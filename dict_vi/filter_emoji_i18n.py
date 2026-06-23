#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Lọc emoji_i18n.jsondlgz chỉ giữ từ khóa tìm emoji cho tiếng Anh (en) + tiếng Việt (vi),
loại bỏ ~90 ngôn ngữ khác để giảm dung lượng APK.

Định dạng file (gzip, mỗi ngôn ngữ 2 dòng):
    #<lang>
    {<json tên emoji>}
"""
import gzip
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.normpath(os.path.join(HERE, "..", "java", "res", "raw", "emoji_i18n.jsondlgz"))
KEEP = {"en", "vi"}


def main():
    if not os.path.isfile(SRC):
        print(f"ERROR: not found {SRC}", file=sys.stderr)
        return 1

    with gzip.open(SRC, "rb") as f:
        raw = f.read()

    # tách theo dòng nhưng giữ cặp (#lang, json)
    lines = raw.split(b"\n")
    out_lines = []
    i = 0
    kept = []
    while i < len(lines):
        line = lines[i]
        if line.startswith(b"#"):
            lang = line[1:].strip().decode("utf-8", "replace")
            json_line = lines[i + 1] if i + 1 < len(lines) else b""
            if lang in KEEP:
                out_lines.append(line)
                out_lines.append(json_line)
                kept.append(lang)
            i += 2
        else:
            # dòng lạ (vd dòng trống cuối) — bỏ qua
            i += 1

    payload = b"\n".join(out_lines) + b"\n"

    old_size = os.path.getsize(SRC)
    with gzip.open(SRC, "wb") as f:
        f.write(payload)
    new_size = os.path.getsize(SRC)

    print(f"Kept languages: {kept}")
    print(f"Size: {old_size/1e6:.2f}MB -> {new_size/1e6:.2f}MB (decompressed {len(payload)/1e6:.2f}MB)")
    if not all(k in kept for k in KEEP):
        print(f"WARNING: missing some of {KEEP} in source!", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
