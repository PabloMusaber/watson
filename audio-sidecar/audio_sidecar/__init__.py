"""audio-sidecar: push-to-talk microphone -> Deepgram -> WebSocket daemon."""
from audio_sidecar.config import AppConfig, load_config
from audio_sidecar.sidecar import Sidecar

__all__ = ["AppConfig", "load_config", "Sidecar"]
