"""Sidecar orchestrator — wires audio, keyboard, transcription, and WebSocket."""
from __future__ import annotations

import json
import logging
import queue
import threading
from datetime import datetime, timezone

import numpy as np

from audio_sidecar._markers import _START_RECORDING, _STOP_RECORDING
from audio_sidecar.audio_capture import AudioCapture
from audio_sidecar.keyboard_listener import KeyboardListener
from audio_sidecar.protocols import TranscriberProtocol, TtsProtocol, WebSocketClientProtocol
from audio_sidecar.ptt_handler import PttHandler

logger = logging.getLogger(__name__)


class Sidecar:
    """Push-to-talk orchestrator: hold the configured key, speak, release.

    Components are injected via the constructor and wired after construction::

        sidecar = Sidecar(audio, keyboard, transcriber, ws, tts)
        audio.on_chunk = sidecar.handle_audio_chunk
        ws.on_message = sidecar.handle_ws_message
        sidecar.run()
    """

    def __init__(
        self,
        audio: AudioCapture,
        keyboard: KeyboardListener,
        transcriber: TranscriberProtocol,
        ws: WebSocketClientProtocol,
        tts: TtsProtocol,
    ) -> None:
        self._audio = audio
        self._keyboard = keyboard
        self._transcriber = transcriber
        self._ws = ws
        self._tts = tts

        self._recording = False
        self._recording_lock = threading.Lock()
        self._audio_q: queue.Queue[object] = queue.Queue(maxsize=1024)
        self._recording_buffer: list[np.ndarray] = []
        self._stop_flag = threading.Event()
        self._ptt = PttHandler(self._recording_buffer, self._flush_recording)

    # --- Public wiring points ---

    def handle_audio_chunk(self, chunk: np.ndarray) -> None:
        """Receive a float32 audio chunk from AudioCapture."""
        with self._recording_lock:
            if not self._recording:
                return
        try:
            self._audio_q.put_nowait(chunk)
        except queue.Full:
            logger.warning("audio queue full, dropping frame")
            self._clear_audio_queue()

    def handle_ws_message(self, message: str) -> None:
        """Handle an inbound WebSocket message (e.g. a TTS command)."""
        try:
            data = json.loads(message)
        except json.JSONDecodeError:
            logger.warning("invalid WebSocket message: %r", message)
            return
        if data.get("type") == "tts":
            text = data.get("text", "")
            if text:
                self._tts.speak(text)

    # --- Lifecycle ---

    def run(self) -> None:
        """Start all components and block until Ctrl-C or shutdown."""
        self._ws.start()
        self._keyboard.start(on_press=self._start_recording, on_release=self._stop_recording)
        proc_thread = threading.Thread(target=self._process_loop, name="proc", daemon=True)
        proc_thread.start()
        self._audio.start()

        logger.info("sidecar running — hold configured key to speak")
        try:
            while not self._stop_flag.is_set():
                self._stop_flag.wait(timeout=0.5)
        except KeyboardInterrupt:
            logger.info("interrupt received, shutting down")
        finally:
            self.shutdown()

    def shutdown(self) -> None:
        """Stop all components cleanly."""
        self._stop_flag.set()
        self._keyboard.stop()
        self._audio.stop()
        self._ws.stop()
        self._tts.stop()

    # --- Recording control (PTT) ---

    def _start_recording(self) -> None:
        self._tts.stop()
        with self._recording_lock:
            if self._recording:
                return
            self._recording = True
        self._queue_control(_START_RECORDING)
        logger.info("recording started")

    def _stop_recording(self) -> None:
        with self._recording_lock:
            if not self._recording:
                return
            self._recording = False
        self._queue_control(_STOP_RECORDING)
        logger.info("recording stopped")

    def _queue_control(self, marker: object) -> None:
        try:
            self._audio_q.put_nowait(marker)
        except queue.Full:
            self._clear_audio_queue()
            self._audio_q.put_nowait(marker)

    def _clear_audio_queue(self) -> None:
        while not self._audio_q.empty():
            try:
                self._audio_q.get_nowait()
            except queue.Empty:
                break

    # --- Process loop ---

    def _process_loop(self) -> None:
        while not self._stop_flag.is_set():
            try:
                item = self._audio_q.get(timeout=0.1)
            except queue.Empty:
                continue
            self._ptt.process(item)

    # Deepgram (like Whisper) can produce garbage on very short or near-silent
    # buffers, and it's a wasted API call either way — skip both up front.
    _MIN_SAMPLES = 8_000   # 0.5 s at 16 kHz
    _MIN_RMS = 0.01        # ~-40 dBFS

    def _flush_recording(self) -> None:
        if not self._recording_buffer:
            return
        audio = np.concatenate(self._recording_buffer)
        self._recording_buffer.clear()
        if len(audio) < self._MIN_SAMPLES:
            logger.debug("audio too short (%d samples), skipping", len(audio))
            return
        if float(np.sqrt(np.mean(audio ** 2))) < self._MIN_RMS:
            logger.debug("audio too quiet, skipping")
            return
        text = self._transcriber.transcribe(audio)
        if text:
            self._emit(text)

    # --- Emission ---

    def _emit(self, text: str) -> None:
        ts = datetime.now(timezone.utc).isoformat()
        payload = json.dumps({"text": text, "ts": ts})
        logger.info("heard: %s", text)
        if not self._ws.send(payload):
            logger.warning("WebSocket not connected, transcript dropped: %s", text)
