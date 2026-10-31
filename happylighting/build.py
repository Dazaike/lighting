from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def main() -> None:
    # First ensure the React web app is built
    web_dir = ROOT / "web"
    if (web_dir / "package.json").exists():
        subprocess.run(["pnpm", "run", "build"], cwd=web_dir, check=True, shell=True)

    command = [
        sys.executable,
        "-m",
        "PyInstaller",
        "--noconfirm",
        "--clean",
        "--onefile",
        "--windowed",
        "--splash",
        str(ROOT / "assets" / "splash.png"),
        "--name",
        "Lumora",
        "--icon",
        str(ROOT / "assets" / "app.ico"),
        "--add-data",
        f"{ROOT / 'assets'};assets",
        "--add-data",
        f"{ROOT / 'web' / 'dist'};web/dist",
        str(ROOT / "run.pyw"),
    ]
    subprocess.run(command, cwd=ROOT, check=True)


if __name__ == "__main__":
    main()
