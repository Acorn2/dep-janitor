#!/usr/bin/env python3
"""Generate Dep Janitor app icons from a single square PNG source.

Usage:
  python3 scripts/generate_app_icons.py

Input:
  app-desktop/src/main/resources/icons/source/dep-janitor-1024.png

Outputs:
  app-desktop/src/main/resources/icons/runtime/dep-janitor-<size>.png
  app-desktop/src/main/resources/icons/windows/dep-janitor.ico
  app-desktop/src/main/resources/icons/macos/dep-janitor.icns
"""

from __future__ import annotations

import os
import shutil
import struct
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ICONS_ROOT = ROOT / "app-desktop" / "src" / "main" / "resources" / "icons"
SOURCE = ICONS_ROOT / "source" / "dep-janitor-1024.png"
RUNTIME_DIR = ICONS_ROOT / "runtime"
WINDOWS_DIR = ICONS_ROOT / "windows"
MACOS_DIR = ICONS_ROOT / "macos"

PNG_SIZES = [16, 24, 32, 48, 64, 128, 256, 512, 1024]
ICO_SIZES = [16, 24, 32, 48, 64, 128, 256]
ICNS_MAP = {
    "icon_16x16.png": 16,
    "icon_16x16@2x.png": 32,
    "icon_32x32.png": 32,
    "icon_32x32@2x.png": 64,
    "icon_128x128.png": 128,
    "icon_128x128@2x.png": 256,
    "icon_256x256.png": 256,
    "icon_256x256@2x.png": 512,
    "icon_512x512.png": 512,
    "icon_512x512@2x.png": 1024,
}


def ensure_exists(path: Path) -> None:
    if not path.exists():
        raise SystemExit(f"Source icon not found: {path}\nPlease place your master image there first.")


def ensure_tool(name: str) -> str:
    path = shutil.which(name)
    if not path:
        raise SystemExit(f"Required tool not found: {name}")
    return path


def run(*args: str) -> None:
    result = subprocess.run(args, capture_output=True, text=True)
    if result.returncode != 0:
        raise SystemExit(
            f"Command failed: {' '.join(args)}\nSTDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}"
        )


def generate_png(size: int, out_path: Path, sips_bin: str) -> None:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    run(sips_bin, "-z", str(size), str(size), str(SOURCE), "--out", str(out_path))


def build_ico(png_files: list[Path], out_path: Path) -> None:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    blobs = [path.read_bytes() for path in png_files]
    count = len(blobs)
    header = struct.pack("<HHH", 0, 1, count)
    entries = []
    offset = 6 + 16 * count

    for path, blob in zip(png_files, blobs):
        size = int(path.stem.split("-")[-1])
        width = 0 if size >= 256 else size
        height = 0 if size >= 256 else size
        entry = struct.pack(
            "<BBBBHHII",
            width,
            height,
            0,
            0,
            1,
            32,
            len(blob),
            offset,
        )
        entries.append(entry)
        offset += len(blob)

    out_path.write_bytes(header + b"".join(entries) + b"".join(blobs))


def build_icns(sips_bin: str, iconutil_bin: str, out_path: Path) -> None:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="dep-janitor-iconset-") as tmp:
        iconset = Path(tmp) / "dep-janitor.iconset"
        iconset.mkdir(parents=True, exist_ok=True)
        for name, size in ICNS_MAP.items():
            generate_png(size, iconset / name, sips_bin)
        run(iconutil_bin, "-c", "icns", str(iconset), "-o", str(out_path))


def main() -> None:
    ensure_exists(SOURCE)
    sips_bin = ensure_tool("sips")
    iconutil_bin = ensure_tool("iconutil")

    generated_pngs: list[Path] = []
    for size in PNG_SIZES:
        out = RUNTIME_DIR / f"dep-janitor-{size}.png"
        generate_png(size, out, sips_bin)
        generated_pngs.append(out)

    ico_pngs = [RUNTIME_DIR / f"dep-janitor-{size}.png" for size in ICO_SIZES]
    build_ico(ico_pngs, WINDOWS_DIR / "dep-janitor.ico")
    build_icns(sips_bin, iconutil_bin, MACOS_DIR / "dep-janitor.icns")

    print("Generated runtime PNGs:")
    for path in generated_pngs:
        print(f" - {path.relative_to(ROOT)}")
    print(f"Generated Windows ICO: {(WINDOWS_DIR / 'dep-janitor.ico').relative_to(ROOT)}")
    print(f"Generated macOS ICNS: {(MACOS_DIR / 'dep-janitor.icns').relative_to(ROOT)}")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(130)
