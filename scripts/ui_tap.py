#!/usr/bin/env python3
"""Find a UI element by text/content description, then tap or long-press it.

Usage: ui_tap.py <label> [--exact] [--long] [--scroll-y <reference label>]

--scroll-y scrolls the horizontal row containing <reference label> leftwards
until <label> appears (the inbox pill row puts Spam off-screen), then acts.
Exits non-zero when nothing matches, so a capture script can fall back.
"""

import re
import subprocess
import sys
import time
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


def bounds(node: ET.Element) -> list[int]:
    return [int(n) for n in re.findall(r"-?\d+", node.get("bounds", ""))]


def find(root: ET.Element, needle: str, exact: bool) -> ET.Element | None:
    for node in root.iter("node"):
        for attr in ("text", "content-desc"):
            value = node.get(attr) or ""
            hit = value == needle if exact else needle.lower() in value.lower()
            if hit and len(bounds(node)) == 4:
                return node
    return None


def act(node: ET.Element, label: str, long_press: bool) -> None:
    b = bounds(node)
    x = (b[0] + b[2]) // 2
    y = (b[1] + b[3]) // 2
    if long_press:
        subprocess.run(
            ["adb", "shell", "input", "swipe", str(x), str(y), str(x), str(y), "900"],
            check=True,
        )
        print(f"long-pressed {label!r} at ({x}, {y})")
    else:
        subprocess.run(["adb", "shell", "input", "tap", str(x), str(y)], check=True)
        print(f"tapped {label!r} at ({x}, {y})")


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if not args:
        print("usage: ui_tap.py <label> [--exact] [--long] [--scroll-y REF]", file=sys.stderr)
        return 2
    needle = args[0]
    exact = "--exact" in sys.argv
    long_press = "--long" in sys.argv
    scroll_ref = None
    if "--scroll-y" in sys.argv:
        scroll_ref = sys.argv[sys.argv.index("--scroll-y") + 1]

    root = dump()
    node = find(root, needle, exact)

    if node is None and scroll_ref:
        ref = find(root, scroll_ref, exact=False)
        if ref is not None:
            y = (bounds(ref)[1] + bounds(ref)[3]) // 2
            for _ in range(5):
                subprocess.run(
                    ["adb", "shell", "input", "swipe", "950", str(y), "120", str(y), "250"],
                    check=True,
                )
                time.sleep(0.7)
                root = dump()
                node = find(root, needle, exact)
                if node is not None:
                    break

    if node is None:
        print(f"not found: {needle!r}", file=sys.stderr)
        return 1

    act(node, needle, long_press)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
