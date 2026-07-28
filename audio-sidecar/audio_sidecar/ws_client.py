"""Reconnecting WebSocket client."""
from __future__ import annotations

import logging
import threading
import time
from collections.abc import Callable

from websocket import WebSocketApp

from audio_sidecar.config import WebSocketConfig

logger = logging.getLogger(__name__)


class WebSocketClient:
    """Maintains a persistent, auto-reconnecting WebSocket connection.

    Set ``on_message`` before calling ``start()`` to handle inbound messages.
    """

    def __init__(self, config: WebSocketConfig) -> None:
        self._config = config
        self._ws: WebSocketApp | None = None
        self._ws_open = threading.Event()
        self._stop_flag = threading.Event()
        self.on_message: Callable[[str], None] | None = None

    @property
    def is_connected(self) -> bool:
        return self._ws_open.is_set()

    def start(self) -> None:
        """Spawn the reconnecting WebSocket thread."""
        self._stop_flag.clear()
        thread = threading.Thread(target=self._run_forever, name="ws", daemon=True)
        thread.start()

    def stop(self) -> None:
        """Signal shutdown and close the current connection."""
        self._stop_flag.set()
        if self._ws is not None:
            self._ws.close()

    def send(self, message: str) -> bool:
        """Send a message. Returns False (without raising) if not connected."""
        if not self._ws_open.is_set() or self._ws is None:
            return False
        try:
            self._ws.send(message)
            return True
        except Exception:
            logger.warning("WebSocket send failed")
            return False

    def _run_forever(self) -> None:
        while not self._stop_flag.is_set():
            try:
                self._ws = WebSocketApp(
                    self._config.url,
                    on_open=self._on_open,
                    on_close=self._on_close,
                    on_message=self._on_message,
                    on_error=self._on_error,
                )
                self._ws.run_forever(ping_interval=20)
            except Exception:
                logger.warning("WebSocket connection error")
                self._ws_open.clear()
            if self._stop_flag.is_set():
                break
            logger.info("reconnecting in %ds", self._config.reconnect_seconds)
            time.sleep(self._config.reconnect_seconds)

    def _on_open(self, ws: WebSocketApp) -> None:
        logger.info("WebSocket connected: %s", self._config.url)
        self._ws_open.set()

    def _on_close(self, ws: WebSocketApp, code: int, msg: str) -> None:
        logger.info("WebSocket closed (code=%s, msg=%s)", code, msg)
        self._ws_open.clear()

    def _on_message(self, ws: WebSocketApp, message: str) -> None:
        if self.on_message is not None:
            try:
                self.on_message(message)
            except Exception:
                logger.exception("error in WebSocket message handler")

    def _on_error(self, ws: WebSocketApp, error: Exception) -> None:
        logger.warning("WebSocket error: %s", error)
