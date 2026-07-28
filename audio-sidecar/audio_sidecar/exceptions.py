"""Custom exception hierarchy for audio-sidecar."""
from __future__ import annotations


class AudioSidecarError(Exception):
    """Base exception for all audio-sidecar errors."""


class ConfigError(AudioSidecarError):
    """Raised when configuration is missing, malformed, or invalid."""


class DeviceNotFoundError(AudioSidecarError):
    """Raised when a required input device cannot be found or opened."""


class AudioDeviceError(AudioSidecarError):
    """Raised when the audio capture device fails to open or operate."""
