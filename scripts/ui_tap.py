#!/usr/bin/env python3
"""Tap the first UI element whose text or content description matches.

Usage: ui_tap.py "Some label" [--exact]

Dumps the current uiautomator hierarchy, finds the node, taps its centre and
prints what it tapped. Exits non-zero when nothing matches, so a capture
script can fall back to coordinates.
"""

import re
import subprocess
import sys
import xml.etree.ElementTree as ET

REMOTE = "/sdcard/window_dump.xml"
LOCAL = "/tmp/window_dump.xml"


def dump() -> ET.Element:
    subprocess.run(
        ["adb", "shell", "uiautomator", "dump", REMOTE],
        check=True,
        capture_output=True,
        text=True,
    )
    subprocess.run(["adb", "pull", REMOTE, LOCAL], check=True, capture_output=True)
    return ET.parse(LOCAL).getroot()


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: ui_tap.py <label> [--exact]", file=sys.stderr)
        return 2
    needle = sys.argv[1]
    exact = "--exact" in sys.argv
    root = dump()

    for node in root.iter("node"):
        for attr in ("text", "content-desc"):
            value = node.get(attr) or ""
            hit = value == needle if exact else needle.lower() in value.lower()
            if not hit:
                continue
            bounds = [int(n) for n in re.findall(r"-?\d+", node.get("bounds", ""))]
            if len(bounds) != 4:
                continue
            x = (bounds[0] + bounds[2]) // 2
            y = (bounds[1] + bounds[3]) // 2
            subprocess.run(["adb", "shell", "input", "tap", str(x), str(y)], check=True)
            print(f"tapped {value!r} at ({x}, {y})")
            return 0

    print(f"not found: {needle!r}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
