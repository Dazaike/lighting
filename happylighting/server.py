from __future__ import annotations

import http.server
import json
import logging
import mimetypes
import os
import queue
import socketserver
import sys
import threading
from pathlib import Path

from .service import HappyLightingService

_LOG = logging.getLogger("lumora.server")


def _get_web_dist_dir() -> Path:
    if getattr(sys, "frozen", False) and hasattr(sys, "_MEIPASS"):
        return Path(sys._MEIPASS) / "web" / "dist"
    return Path(__file__).resolve().parent.parent / "web" / "dist"


class LumoraHttpServer:
    def __init__(self, service: HappyLightingService) -> None:
        self.service = service
        self.dist_dir = _get_web_dist_dir()
        self.clients: set[queue.Queue[dict]] = set()
        self.lock = threading.Lock()

        # Connect to service state changes
        self.service.state_changed.connect(self._on_state_changed)

        server_self = self

        class LumoraHandler(http.server.BaseHTTPRequestHandler):
            def log_message(self, format, *args):
                _LOG.debug(format, *args)

            def _send_json(self, status_code: int, payload: dict):
                body = json.dumps(payload).encode("utf-8")
                self.send_response(status_code)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.send_header("Access-Control-Allow-Origin", "*")
                self.send_header("Access-Control-Allow-Headers", "Content-Type")
                self.end_headers()
                self.wfile.write(body)

            def do_OPTIONS(self):
                self.send_response(204)
                self.send_header("Access-Control-Allow-Origin", "*")
                self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                self.send_header("Access-Control-Allow-Headers", "Content-Type")
                self.end_headers()

            def do_GET(self):
                parsed_path = self.path.split("?")[0]

                if parsed_path == "/api/state":
                    self._send_json(200, server_self.service.state.as_dict())
                    return

                if parsed_path == "/api/events":
                    self.send_response(200)
                    self.send_header("Content-Type", "text/event-stream")
                    self.send_header("Cache-Control", "no-cache")
                    self.send_header("Connection", "keep-alive")
                    self.send_header("Access-Control-Allow-Origin", "*")
                    self.end_headers()

                    client_q: queue.Queue[dict] = queue.Queue()
                    with server_self.lock:
                        server_self.clients.add(client_q)

                    # Send initial state immediately
                    initial_msg = f"data: {json.dumps(server_self.service.state.as_dict())}\n\n"
                    try:
                        self.wfile.write(initial_msg.encode("utf-8"))
                        self.wfile.flush()
                        while True:
                            try:
                                state_data = client_q.get(timeout=20.0)
                                msg = f"data: {json.dumps(state_data)}\n\n"
                                self.wfile.write(msg.encode("utf-8"))
                                self.wfile.flush()
                            except queue.Empty:
                                # Keep-alive comment
                                self.wfile.write(b": keepalive\n\n")
                                self.wfile.flush()
                    except (BrokenPipeError, ConnectionResetError, OSError):
                        pass
                    finally:
                        with server_self.lock:
                            server_self.clients.discard(client_q)
                    return

                # Static file serving from web/dist/
                rel_path = parsed_path.lstrip("/")
                if not rel_path or rel_path == "index.html":
                    file_path = server_self.dist_dir / "index.html"
                else:
                    file_path = (server_self.dist_dir / rel_path).resolve()

                if file_path.exists() and file_path.is_file() and str(file_path).startswith(str(server_self.dist_dir)):
                    mime, _ = mimetypes.guess_type(str(file_path))
                    mime = mime or "application/octet-stream"
                    content = file_path.read_bytes()

                    self.send_response(200)
                    self.send_header("Content-Type", mime)
                    self.send_header("Content-Length", str(len(content)))
                    self.send_header("Access-Control-Allow-Origin", "*")
                    self.end_headers()
                    self.wfile.write(content)
                else:
                    # Single-Page-App fallback
                    index_path = server_self.dist_dir / "index.html"
                    if index_path.exists():
                        content = index_path.read_bytes()
                        self.send_response(200)
                        self.send_header("Content-Type", "text/html")
                        self.send_header("Content-Length", str(len(content)))
                        self.end_headers()
                        self.wfile.write(content)
                    else:
                        self.send_response(404)
                        self.end_headers()

            def do_POST(self):
                content_len = int(self.headers.get("Content-Length", 0))
                body = self.rfile.read(content_len) if content_len > 0 else b"{}"
                try:
                    data = json.loads(body.decode("utf-8")) if body else {}
                except Exception:
                    data = {}

                path = self.path.split("?")[0]

                if path == "/api/scan":
                    server_self.service.scan_devices()
                    self._send_json(200, {"ok": True})
                elif path == "/api/connect":
                    addr = data.get("address")
                    server_self.service.connect_device(addr)
                    self._send_json(200, {"ok": True})
                elif path == "/api/disconnect":
                    server_self.service.disconnect_device()
                    self._send_json(200, {"ok": True})
                elif path == "/api/reset":
                    server_self.service.reset_and_reconnect()
                    self._send_json(200, {"ok": True})
                elif path == "/api/color":
                    r = int(data.get("r", 255))
                    g = int(data.get("g", 255))
                    b = int(data.get("b", 255))
                    server_self.service.set_color((r, g, b))
                    self._send_json(200, {"ok": True})
                elif path == "/api/brightness":
                    val = int(data.get("brightness", 100))
                    server_self.service.set_brightness(val)
                    self._send_json(200, {"ok": True})
                elif path == "/api/turn_on":
                    server_self.service.turn_on()
                    self._send_json(200, {"ok": True})
                elif path == "/api/turn_off":
                    server_self.service.turn_off()
                    self._send_json(200, {"ok": True})
                else:
                    self.send_response(404)
                    self.end_headers()

        self.server = socketserver.ThreadingTCPServer(("127.0.0.1", 0), LumoraHandler)
        self.port = self.server.server_address[1]
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        _LOG.info("Lumora HTTP/SSE server running on port %d", self.port)

    def _on_state_changed(self, state: dict):
        with self.lock:
            for q in list(self.clients):
                q.put(state)

    def shutdown(self):
        try:
            self.server.shutdown()
        except Exception:
            pass
