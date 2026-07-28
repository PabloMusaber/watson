"""Typed configuration dataclasses and TOML loader."""
from __future__ import annotations

import tomllib
from dataclasses import dataclass, field
from pathlib import Path

from audio_sidecar.exceptions import ConfigError


@dataclass(frozen=True)
class AudioConfig:
    device: int | None = None


@dataclass(frozen=True)
class KeyboardConfig:
    push_to_talk_key: str = "KEY_F3"
    device: str | None = None


@dataclass(frozen=True)
class DeepgramConfig:
    # api_key is read from the DEEPGRAM_API_KEY env var, not from this file.
    stt_model: str = "nova-3"
    tts_model: str = "aura-2-asteria-en"


@dataclass(frozen=True)
class WebSocketConfig:
    url: str = "ws://127.0.0.1:8080/ws/utterances"
    reconnect_seconds: int = 2


@dataclass(frozen=True)
class AppConfig:
    audio: AudioConfig = field(default_factory=AudioConfig)
    keyboard: KeyboardConfig = field(default_factory=KeyboardConfig)
    deepgram: DeepgramConfig = field(default_factory=DeepgramConfig)
    websocket: WebSocketConfig = field(default_factory=WebSocketConfig)


def load_config(path: Path) -> AppConfig:
    """Load and validate config from a TOML file. Raises ConfigError on failure."""
    try:
        with open(path, "rb") as f:
            raw = tomllib.load(f)
    except FileNotFoundError as e:
        raise ConfigError(f"Config file not found: {path}") from e
    except tomllib.TOMLDecodeError as e:
        raise ConfigError(f"Invalid TOML in {path}: {e}") from e

    try:
        return AppConfig(
            audio=AudioConfig(**raw.get("audio", {})),
            keyboard=KeyboardConfig(**raw.get("keyboard", {})),
            deepgram=DeepgramConfig(**raw.get("deepgram", {})),
            websocket=WebSocketConfig(**raw.get("websocket", {})),
        )
    except TypeError as e:
        raise ConfigError(f"Config error: {e}") from e
