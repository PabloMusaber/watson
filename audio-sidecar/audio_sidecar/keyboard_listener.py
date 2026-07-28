"""Push-to-talk keyboard listener using Linux evdev."""
from __future__ import annotations

import glob
import logging
import select
import threading
from collections.abc import Callable

import evdev

from audio_sidecar.config import KeyboardConfig
from audio_sidecar.exceptions import ConfigError, DeviceNotFoundError

logger = logging.getLogger(__name__)


def _key_code(key_name: str) -> int | None:
    return getattr(evdev.ecodes, key_name, None)


def _find_keyboards(key_code: int) -> list[evdev.InputDevice]:
    devices = []
    for path in glob.glob("/dev/input/event*"):
        try:
            dev = evdev.InputDevice(path)
            caps = dev.capabilities()
            if evdev.ecodes.EV_KEY in caps and key_code in caps[evdev.ecodes.EV_KEY]:
                devices.append(dev)
        except (PermissionError, OSError):
            pass
    if not devices:
        raise DeviceNotFoundError(f"No keyboard found with key code {key_code}")
    return devices


class KeyboardListener:
    """Listens for push-to-talk key events on Linux via evdev."""

    def __init__(self, config: KeyboardConfig) -> None:
        self._config = config
        self._stop_flag = threading.Event()
        self._thread: threading.Thread | None = None

    def start(
        self,
        on_press: Callable[[], None],
        on_release: Callable[[], None],
    ) -> None:
        """Spawn the keyboard listener thread."""
        self._stop_flag.clear()
        self._thread = threading.Thread(
            target=self._listen,
            args=(on_press, on_release),
            name="kbd",
            daemon=True,
        )
        self._thread.start()

    def stop(self) -> None:
        """Signal the listener thread to exit."""
        self._stop_flag.set()

    def check(self) -> bool:
        """Return True if at least one keyboard device is readable for the configured key.

        Exits immediately without loading any heavy model; safe to call as a CLI check.
        """
        key_name = self._config.push_to_talk_key
        code = _key_code(key_name)
        if code is None:
            logger.error("unknown key name: %s", key_name)
            return False
        try:
            devices = _find_keyboards(code)
        except DeviceNotFoundError as e:
            logger.error("%s", e)
            return False
        for dev in devices:
            logger.info("found device: %s (%s)", dev.path, dev.name)
        return True

    def _open_devices(self, code: int) -> list[evdev.InputDevice]:
        if self._config.device:
            return [evdev.InputDevice(self._config.device)]
        return _find_keyboards(code)

    def watch(self) -> None:
        """Print all key events from the configured device until Ctrl-C."""
        key_name = self._config.push_to_talk_key
        code = _key_code(key_name)
        if code is None:
            raise ConfigError(f"Unknown key name: {key_name}")

        devices = self._open_devices(code)
        device = devices[0]
        logger.info("watching %s (%s) — Ctrl-C to stop", device.path, device.name)
        try:
            for event in device.read_loop():
                if event.type == evdev.ecodes.EV_KEY:
                    kev = evdev.categorize(event)
                    marker = " <= configured" if event.code == code else ""
                    logger.info("key %s: %s%s", kev.keystate, kev.keycode, marker)
        except KeyboardInterrupt:
            pass

    def _listen(
        self,
        on_press: Callable[[], None],
        on_release: Callable[[], None],
    ) -> None:
        key_name = self._config.push_to_talk_key
        code = _key_code(key_name)
        if code is None:
            logger.error("unknown key name: %s — keyboard listener not started", key_name)
            return

        try:
            devices = self._open_devices(code)
        except (DeviceNotFoundError, ConfigError) as e:
            logger.error("%s", e)
            self._stop_flag.set()
            return

        logger.info(
            "keyboard listener ready (key=%s, devices=%d)",
            key_name,
            len(devices),
        )
        fds = {dev.fd: dev for dev in devices}

        while not self._stop_flag.is_set():
            try:
                readable, _, _ = select.select(fds.keys(), [], [], 0.2)
            except Exception:
                logger.exception("keyboard select error")
                self._stop_flag.set()
                break

            for fd in readable:
                dev = fds[fd]
                try:
                    for event in dev.read():
                        if event.type != evdev.ecodes.EV_KEY:
                            continue
                        if event.code == code:
                            if event.value == 1:
                                logger.debug("key down: %s", dev.path)
                                on_press()
                            elif event.value == 0:
                                logger.debug("key up: %s", dev.path)
                                on_release()
                except OSError:
                    logger.exception("keyboard read error: %s", dev.path)
                    self._stop_flag.set()
                    break
