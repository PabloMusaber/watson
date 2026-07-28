"""Push-to-talk state machine."""
from __future__ import annotations

import logging
from collections.abc import Callable

import numpy as np

from audio_sidecar._markers import _START_RECORDING, _STOP_RECORDING

logger = logging.getLogger(__name__)


class PttHandler:
    """Manages PTT recording state: clears buffer on key-down, flushes on key-up."""

    def __init__(self, recording_buffer: list, flush_fn: Callable[[], None]) -> None:
        self._buffer = recording_buffer
        self._flush = flush_fn
        self._accepting = False

    def process(self, item: object) -> None:
        if item is _START_RECORDING:
            self._buffer.clear()
            self._accepting = True
        elif item is _STOP_RECORDING:
            self._accepting = False
            self._flush()
        elif isinstance(item, np.ndarray):
            if self._accepting:
                self._buffer.append(item)
        else:
            logger.debug("unexpected queue item: %r", item)

    @property
    def is_accepting(self) -> bool:
        return self._accepting
