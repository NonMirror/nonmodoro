#!/usr/bin/env python3
"""Measure the "ink budget" of an illustration.

The reference illustrations are pure black ink on a fully transparent ground,
so the only thing that makes them feel busy or calm is how much of the canvas
the ink actually covers.  This prints that number (alpha-weighted, so anti-
aliased edges count for what they are) plus the ink bounding box, so the three
redrawn illustrations can be held to the same budget.

usage: inkstats.py a.png [b.png ...]
"""
import sys
from PIL import Image

REF = {"idle": 15.96, "focus": 16.05, "break": 14.26}
TARGET = 15.5          # the three redrawn illustrations all aim for this


def stats(path):
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    a = im.getchannel("A")
    px = list(a.getdata())
    total = w * h
    ink = sum(px) / 255.0 / total * 100.0          # alpha-weighted coverage
    solid = sum(1 for v in px if v > 200) / total * 100.0
    clear = sum(1 for v in px if v < 16) / total * 100.0
    bbox = a.getbbox()
    if bbox:
        bx = (bbox[0] / w * 100, bbox[1] / h * 100,
              bbox[2] / w * 100, bbox[3] / h * 100)
    else:
        bx = (0, 0, 0, 0)
    return dict(path=path, size=(w, h), ink=ink, solid=solid, clear=clear, bbox=bx)


def main(paths):
    print(f"{'file':<34}{'size':>11}{'ink%':>8}{'solid%':>8}{'clear%':>8}"
          f"{'bbox (x0 y0 x1 y1, % of canvas)':>34}")
    for p in paths:
        try:
            s = stats(p)
        except Exception as e:                     # noqa: BLE001
            print(f"{p:<34}  !! {e}")
            continue
        w, h = s["size"]
        bb = " ".join(f"{v:5.1f}" for v in s["bbox"])
        print(f"{p.split('/')[-1]:<34}{f'{w}x{h}':>11}{s['ink']:>8.2f}"
              f"{s['solid']:>8.2f}{s['clear']:>8.2f}{bb:>34}")
    print()
    print("reference budget: ink 15.4-17.3%, solid ~15%, clear 75-81%")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    main(sys.argv[1:])
