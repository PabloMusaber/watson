"""Deepgram-based STT and TTS for push-to-talk audio."""
from __future__ import annotations

import io
import logging
import queue
import threading
import wave

import numpy as np
import sounddevice as sd
from deepgram import DeepgramClient

logger = logging.getLogger(__name__)

_STT_SAMPLE_RATE = 16_000
_TTS_SAMPLE_RATE = 24_000


def _to_wav_bytes(audio: np.ndarray, sample_rate: int) -> bytes:
    """Wrap float32 [-1, 1] mono audio as an in-memory 16-bit PCM WAV file.

    transcribe_file() has no sample_rate kwarg, so raw PCM would be ambiguous;
    a WAV header makes the format self-describing.
    """
    pcm16 = (np.clip(audio, -1.0, 1.0) * 32767.0).astype(np.int16)
    buf = io.BytesIO()
    with wave.open(buf, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sample_rate)
        w.writeframes(pcm16.tobytes())
    return buf.getvalue()


class DeepgramTranscriber:
    """Push-to-talk transcription via Deepgram's prerecorded REST API."""

    def __init__(self, api_key: str, model: str) -> None:
        self._client = DeepgramClient(api_key=api_key)
        self._model = model

    def transcribe(self, audio: np.ndarray) -> str | None:
        wav_bytes = _to_wav_bytes(audio, _STT_SAMPLE_RATE)
        try:
            response = self._client.listen.v1.media.transcribe_file(
                request=wav_bytes,
                model=self._model,
            )
            text = response.results.channels[0].alternatives[0].transcript.strip()
            return text or None
        except Exception:
            logger.exception("Deepgram transcription failed")
            return None


class DeepgramTts:
    """Text-to-speech via Deepgram, played back through sounddevice."""

    def __init__(self, api_key: str, model: str) -> None:
        self._client = DeepgramClient(api_key=api_key)
        self._model = model
        self._stop_event = threading.Event()
        self._is_speaking = threading.Event()
        self._queue: queue.Queue[str | None] = queue.Queue()
        self._worker_thread = threading.Thread(target=self._worker, daemon=True)
        self._worker_thread.start()

    @property
    def is_speaking(self) -> bool:
        return self._is_speaking.is_set()

    def speak(self, text: str) -> None:
        """Queue text for playback, cancelling any in-progress speech."""
        self._stop_event.set()
        while not self._queue.empty():
            try:
                self._queue.get_nowait()
            except queue.Empty:
                break
        self._queue.put(text)

    def stop(self) -> None:
        """Cancel current playback. Safe to call from any thread."""
        self._stop_event.set()

    def _worker(self) -> None:
        while True:
            text = self._queue.get()
            if text is None:
                break
            self._speak_now(text)

    def _speak_now(self, text: str) -> None:
        self._stop_event.clear()
        self._is_speaking.set()
        try:
            chunks = self._client.speak.v1.audio.generate(
                text=text,
                model=self._model,
                container="none",
                encoding="linear16",
                sample_rate=_TTS_SAMPLE_RATE,
            )
            pcm_bytes = b"".join(chunks)
            if self._stop_event.is_set():
                return
            pcm = np.frombuffer(pcm_bytes, dtype=np.int16)
            sd.play(pcm, _TTS_SAMPLE_RATE)
            sd.wait()
        except Exception:
            logger.exception("Deepgram TTS playback failed")
        finally:
            sd.stop()
            self._is_speaking.clear()
