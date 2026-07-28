"""Command-line entry point — constructs and wires all components."""
from __future__ import annotations

import argparse
import logging
import os
import sys
from pathlib import Path

from dotenv import load_dotenv

from audio_sidecar.audio_capture import AudioCapture
from audio_sidecar.config import AppConfig, load_config
from audio_sidecar.deepgram_client import DeepgramTranscriber, DeepgramTts
from audio_sidecar.exceptions import AudioSidecarError
from audio_sidecar.keyboard_listener import KeyboardListener
from audio_sidecar.sidecar import Sidecar
from audio_sidecar.ws_client import WebSocketClient

logger = logging.getLogger(__name__)

_DEFAULT_CONFIG = Path("config.toml")


def main() -> None:
    args = _parse_args()
    _setup_logging(args.log_level)
    load_dotenv()

    try:
        config = load_config(args.config)
    except AudioSidecarError as e:
        logger.error("%s", e)
        sys.exit(1)

    keyboard = KeyboardListener(config.keyboard)

    if args.check_keyboard:
        sys.exit(0 if keyboard.check() else 1)

    if args.watch_keyboard:
        try:
            keyboard.watch()
        except AudioSidecarError as e:
            logger.error("%s", e)
            sys.exit(1)
        sys.exit(0)

    api_key = os.environ.get("DEEPGRAM_API_KEY")
    if not api_key:
        logger.error("DEEPGRAM_API_KEY is not set (put it in audio-sidecar/.env)")
        sys.exit(1)

    _run_sidecar(config, api_key)


def _run_sidecar(config: AppConfig, api_key: str) -> None:
    transcriber = DeepgramTranscriber(api_key, config.deepgram.stt_model)
    tts = DeepgramTts(api_key, config.deepgram.tts_model)
    keyboard = KeyboardListener(config.keyboard)
    audio = AudioCapture(config.audio)
    ws = WebSocketClient(config.websocket)

    sidecar = Sidecar(audio=audio, keyboard=keyboard, transcriber=transcriber, ws=ws, tts=tts)

    # Wire callbacks after all components are constructed.
    audio.on_chunk = sidecar.handle_audio_chunk
    ws.on_message = sidecar.handle_ws_message

    sidecar.run()


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="audio-sidecar",
        description="Push-to-talk microphone → Deepgram → WebSocket daemon",
    )
    parser.add_argument(
        "--config",
        type=Path,
        default=_DEFAULT_CONFIG,
        metavar="FILE",
        help=f"Path to config TOML (default: {_DEFAULT_CONFIG})",
    )
    parser.add_argument(
        "--check-keyboard",
        action="store_true",
        help="Test keyboard device detection and exit",
    )
    parser.add_argument(
        "--watch-keyboard",
        action="store_true",
        help="Print key events from the configured device and exit",
    )
    parser.add_argument(
        "--log-level",
        default="INFO",
        choices=["DEBUG", "INFO", "WARNING", "ERROR"],
        metavar="LEVEL",
        help="Log verbosity: DEBUG, INFO, WARNING, ERROR (default: INFO)",
    )
    return parser.parse_args()


def _setup_logging(level: str) -> None:
    logging.basicConfig(
        format="%(asctime)s %(levelname)-8s %(name)s: %(message)s",
        datefmt="%H:%M:%S",
        level=getattr(logging, level),
    )
