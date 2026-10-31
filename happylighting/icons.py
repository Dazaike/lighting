from __future__ import annotations

import sys
from pathlib import Path
from PySide6.QtGui import (
    QColor,
    QIcon,
    QImage,
    QLinearGradient,
    QPainter,
    QPainterPath,
    QPen,
    QPixmap,
)

def _get_assets_dir() -> Path:
    if getattr(sys, "frozen", False) and hasattr(sys, "_MEIPASS"):
        return Path(sys._MEIPASS) / "assets"
    return Path(__file__).resolve().parent.parent / "assets"


ANDROID_DRAWABLE = (
    Path(__file__).resolve().parent.parent
    / "app"
    / "src"
    / "main"
    / "res"
    / "drawable-nodpi"
    / "lumora_foreground.png"
)


def create_app_icon() -> QIcon:
    """Create the Lumora app icon matching the Android app icon."""
    assets_dir = _get_assets_dir()
    icon_ico = assets_dir / "app.ico"
    icon_png = assets_dir / "lumora_icon.png"

    if icon_ico.exists():
        icon = QIcon(str(icon_ico))
        if not icon.isNull():
            return icon

    if icon_png.exists():
        icon = QIcon(str(icon_png))
        if not icon.isNull():
            return icon

    # Fallback procedural generation matching Android ic_launcher
    size = 256
    pixmap = QPixmap(size, size)
    pixmap.fill(Qt.GlobalColor.transparent)

    painter = QPainter(pixmap)
    painter.setRenderHint(QPainter.RenderHint.Antialiasing)
    painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform)

    padding = 12.0
    rect_size = size - 2 * padding
    radius = 54.0
    path = QPainterPath()
    path.addRoundedRect(QRectF(padding, padding, rect_size, rect_size), radius, radius)

    painter.setClipPath(path)
    gradient = QLinearGradient(padding, padding, padding + rect_size, padding + rect_size)
    gradient.setColorAt(0.0, QColor("#0B0F16"))
    gradient.setColorAt(1.0, QColor("#101722"))
    painter.fillPath(path, gradient)

    if ANDROID_DRAWABLE.exists():
        orig_fg = QImage(str(ANDROID_DRAWABLE))
        cx, cy = 432, 419
        crop_size = 520
        cropped = orig_fg.copy(cx - crop_size // 2, cy - crop_size // 2, crop_size, crop_size)
        painter.setCompositionMode(QPainter.CompositionMode.CompositionMode_Screen)
        emblem_size = 200.0
        dest_pos = (size - emblem_size) / 2.0
        painter.drawImage(QRectF(dest_pos, dest_pos, emblem_size, emblem_size), cropped)

    painter.setCompositionMode(QPainter.CompositionMode.CompositionMode_SourceOver)
    painter.setClipping(False)
    painter.setPen(QPen(QColor(255, 255, 255, 28), 1.5))
    painter.drawPath(path)
    painter.end()

    return QIcon(pixmap)
