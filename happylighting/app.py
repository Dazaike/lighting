from __future__ import annotations

import logging
import sys

from .logging_setup import configure_logging
from .server import LumoraHttpServer
from .service import HappyLightingService
from .ui import run_ui

try:
    import pyi_splash
except ImportError:
    pyi_splash = None


def _update_splash(text: str) -> None:
    if pyi_splash and pyi_splash.is_alive():
        pyi_splash.update_text(text)


def _close_splash() -> None:
    if pyi_splash and pyi_splash.is_alive():
        pyi_splash.close()


def main() -> None:
    log_path = configure_logging()
    logging.getLogger("happylighting").info("Lumora starting — log at %s", log_path)
    _update_splash("Starting Lumora...")
    service = HappyLightingService()
    server = LumoraHttpServer(service)
    service.startup()
    _close_splash()
    run_ui(service, server)


if __name__ == "__main__":
    main()
