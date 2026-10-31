from __future__ import annotations

import logging
from pathlib import Path
import webview

from .icons import create_app_icon
from .server import LumoraHttpServer
from .service import HappyLightingService

_LOG = logging.getLogger("lumora.ui")


def run_ui(service: HappyLightingService, server: LumoraHttpServer) -> None:
    """Launch the Lumora window using Windows native Edge WebView2 for instant startup."""
    window = webview.create_window(
        title="Lumora",
        url=f"http://127.0.0.1:{server.port}/",
        width=440,
        height=750,
        min_size=(380, 560),
        background_color="#090a0f",
    )

    def on_closed():
        _LOG.info("Window closed, shutting down server and BLE service")
        server.shutdown()
        service.shutdown()

    window.events.closed += on_closed
    webview.start(gui="edgechromium")


class MainWindow:
    """Compatibility interface for testing and embedding."""
    def __init__(self, service: HappyLightingService, server: LumoraHttpServer | None = None) -> None:
        self.service = service
        self.server = server or LumoraHttpServer(service)
        self._title = "Lumora"

    def windowTitle(self) -> str:
        return self._title

    def windowIcon(self):
        return create_app_icon()

    def _apply_state(self, state: dict) -> None:
        pass

    def show(self) -> None:
        pass
