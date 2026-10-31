from __future__ import annotations

import logging
import sys
from logging.handlers import RotatingFileHandler
from pathlib import Path

from PySide6.QtCore import QtMsgType, qInstallMessageHandler

from .settings import APP_DIR

LOG_PATH = APP_DIR / "happylighting.log"

_QT_LEVELS = {
    QtMsgType.QtDebugMsg: logging.DEBUG,
    QtMsgType.QtInfoMsg: logging.INFO,
    QtMsgType.QtWarningMsg: logging.WARNING,
    QtMsgType.QtCriticalMsg: logging.ERROR,
    QtMsgType.QtFatalMsg: logging.CRITICAL,
}


def configure_logging() -> Path:
    """Route app logs, Qt messages, and uncaught tracebacks to a rotating file.

    The app ships windowed (``console=False``, ``run.pyw``), so anything written
    to stderr is discarded. This file is the only place failures survive.
    """
    APP_DIR.mkdir(parents=True, exist_ok=True)
    handler = RotatingFileHandler(LOG_PATH, maxBytes=512_000, backupCount=2, encoding="utf-8")
    handler.setFormatter(
        logging.Formatter("%(asctime)s %(levelname)-7s %(name)s: %(message)s")
    )
    root = logging.getLogger()
    root.setLevel(logging.INFO)
    root.addHandler(handler)
    logging.getLogger("happylighting").setLevel(logging.DEBUG)
    logging.getLogger("bleak").setLevel(logging.INFO)

    qt_log = logging.getLogger("happylighting.qt")

    def _qt_handler(mode, context, message: str) -> None:
        qt_log.log(_QT_LEVELS.get(mode, logging.INFO), "%s", message)

    qInstallMessageHandler(_qt_handler)

    def _excepthook(exc_type, exc, tb) -> None:
        logging.getLogger("happylighting").critical(
            "unhandled exception", exc_info=(exc_type, exc, tb)
        )
        sys.__excepthook__(exc_type, exc, tb)

    sys.excepthook = _excepthook
    return LOG_PATH
