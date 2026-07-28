"""Microphone audio capture via sounddevice."""
from __future__ import annotations

import logging
from collections.abc import Callable

import numpy as np
import sounddevice as sd

from audio_sidecar.config import AudioConfig
from audio_sidecar.exceptions import AudioDeviceError

logger = logging.getLogger(__name__)

_SAMPLE_RATE = 16_000
_BLOCK_SIZE = 512


class AudioCapture:
    """Captures mono microphone audio and delivers float32 chunks via callback.

    Set ``on_chunk`` before calling ``start()``. The callback is invoked from
    the sounddevice audio thread for every captured block.
    """

    def __init__(self, config: AudioConfig) -> None:
        self._config = config
        self._stream: sd.InputStream | None = None
        self.on_chunk: Callable[[np.ndarray], None] | None = None

    def start(self) -> None:
        """Open the input stream. Raises AudioDeviceError on failure."""
        try:
            self._stream = sd.InputStream(
                samplerate=_SAMPLE_RATE,
                channels=1,
                dtype="int16",
                blocksize=_BLOCK_SIZE,
                device=self._config.device,
                callback=self._callback,
            )
            self._stream.start()
            logger.info(
                "audio capture started (device=%s, rate=%d Hz)",
                self._config.device,
                _SAMPLE_RATE,
            )
        except sd.PortAudioError as e:
            raise AudioDeviceError(f"Failed to open audio device: {e}") from e

    def stop(self) -> None:
        """Close the input stream."""
        if self._stream is not None:
            self._stream.stop()
            self._stream.close()
            self._stream = None
            logger.info("audio capture stopped")

    def _callback(
        self,
        indata: np.ndarray,
        frames: int,
        time_info: object,
        status: sd.CallbackFlags,
    ) -> None:
        if status:
            logger.warning("audio callback status: %s", status)
        if self.on_chunk is not None:
            chunk = indata[:, 0].astype(np.float32) / 32768.0
            self.on_chunk(chunk)
