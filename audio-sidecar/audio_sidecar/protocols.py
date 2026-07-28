"""Structural interfaces (Protocols) for audio-sidecar components.

These mirror Java interfaces: concrete classes satisfy them implicitly
without declaring `implements` — Python's type checker verifies structural
compatibility at type-check time.
"""
from __future__ import annotations

from typing import Protocol

import numpy as np


class TranscriberProtocol(Protocol):
    def transcribe(self, audio: np.ndarray) -> str | None: ...


class TtsProtocol(Protocol):
    @property
    def is_speaking(self) -> bool: ...

    def speak(self, text: str) -> None: ...

    def stop(self) -> None: ...


class WebSocketClientProtocol(Protocol):
    @property
    def is_connected(self) -> bool: ...

    def send(self, message: str) -> bool: ...

    def start(self) -> None: ...

    def stop(self) -> None: ...
