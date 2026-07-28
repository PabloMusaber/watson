from __future__ import annotations

import queue
import sys
import threading
import time
import types
import unittest
from unittest.mock import MagicMock

# ---------------------------------------------------------------------------
# Stub out hardware-bound dependencies before importing the package so tests
# run without sounddevice or a physical keyboard attached.
# ---------------------------------------------------------------------------

_evdev = types.ModuleType("evdev")
_evdev.ecodes = types.SimpleNamespace(EV_KEY=1, KEY_F3=61)
_evdev.InputDevice = MagicMock()
sys.modules.setdefault("evdev", _evdev)

_sd = types.ModuleType("sounddevice")
_sd.InputStream = MagicMock()
_sd.PortAudioError = OSError
_sd.CallbackFlags = object
_sd.play = MagicMock()
_sd.wait = MagicMock()
_sd.stop = MagicMock()
sys.modules.setdefault("sounddevice", _sd)

_ws_mod = types.ModuleType("websocket")
_ws_mod.WebSocketApp = MagicMock()
sys.modules.setdefault("websocket", _ws_mod)

# ---------------------------------------------------------------------------
# Safe to import now.
# ---------------------------------------------------------------------------

from audio_sidecar._markers import _START_RECORDING, _STOP_RECORDING  # noqa: E402
from audio_sidecar.ptt_handler import PttHandler  # noqa: E402
from audio_sidecar.sidecar import Sidecar  # noqa: E402

import numpy as np  # noqa: E402


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _make_sidecar() -> Sidecar:
    """Create a bare Sidecar with only the state needed by _process_loop."""
    subject = Sidecar.__new__(Sidecar)
    subject._audio_q = queue.Queue()
    subject._recording_buffer = []
    subject._stop_flag = threading.Event()
    mock_transcriber = MagicMock()
    mock_transcriber.transcribe.return_value = "hello there"
    subject._transcriber = mock_transcriber
    subject._ws = MagicMock()
    subject._ws.send.return_value = True
    subject._tts = MagicMock()
    subject._ptt = PttHandler(subject._recording_buffer, subject._flush_recording)
    return subject


def _run_loop(subject: Sidecar) -> threading.Thread:
    t = threading.Thread(target=subject._process_loop, daemon=True)
    t.start()
    return t


def _wait_for(predicate, timeout: float = 1.0) -> bool:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if predicate():
            return True
        time.sleep(0.01)
    return False


def _stop_loop(subject: Sidecar, thread: threading.Thread) -> None:
    subject._stop_flag.set()
    thread.join(timeout=1.0)
    assert not thread.is_alive(), "process loop did not stop"


# A recording of 0.6s of a 440Hz-ish tone at 16kHz: long enough and loud
# enough to clear both the min-samples and min-RMS guards in _flush_recording.
_LOUD_CHUNK = (np.sin(np.linspace(0, 40, 9_600)) * 0.5).astype(np.float32)


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------


class SidecarProcessingTests(unittest.TestCase):
    def test_transcribes_once_on_stop_recording(self):
        subject = _make_sidecar()
        calls = []
        subject._emit = lambda text: calls.append(text)

        t = _run_loop(subject)
        subject._audio_q.put(_START_RECORDING)
        subject._audio_q.put(_LOUD_CHUNK)
        subject._audio_q.put(_STOP_RECORDING)

        _wait_for(lambda: len(calls) >= 1)
        _stop_loop(subject, t)

        self.assertEqual(len(calls), 1)

    def test_does_not_transcribe_before_stop_recording(self):
        subject = _make_sidecar()
        calls = []
        subject._emit = lambda text: calls.append(text)

        t = _run_loop(subject)
        subject._audio_q.put(_START_RECORDING)
        subject._audio_q.put(_LOUD_CHUNK)

        time.sleep(0.1)
        self.assertEqual(len(calls), 0, "should not transcribe before STOP")

        subject._audio_q.put(_STOP_RECORDING)
        _wait_for(lambda: len(calls) >= 1)
        _stop_loop(subject, t)

        self.assertEqual(len(calls), 1)

    def test_short_recording_is_dropped(self):
        subject = _make_sidecar()
        subject._recording_buffer.append(np.zeros(100, dtype=np.float32))  # < 0.5s
        subject._flush_recording()
        subject._transcriber.transcribe.assert_not_called()

    def test_quiet_recording_is_dropped(self):
        subject = _make_sidecar()
        subject._recording_buffer.append(np.zeros(9_600, dtype=np.float32))  # silent
        subject._flush_recording()
        subject._transcriber.transcribe.assert_not_called()


if __name__ == "__main__":
    unittest.main()
