from __future__ import annotations


def scale_rgb(color: tuple[int, int, int], brightness: int) -> tuple[int, int, int]:
    factor = max(1, min(brightness, 100)) / 100.0
    return tuple(max(0, min(255, round(channel * factor))) for channel in color)
