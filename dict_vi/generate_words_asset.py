#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sinh asset `vietnamese_words.txt` (word<TAB>freq) cho engine gợi ý tiếng Việt thuần Kotlin.

Nguồn: main_vi.combined (định dạng FUTO/AOSP dictionary combined).
Mỗi dòng từ trong combined có dạng:  " word=chào,f=162,..."
Bỏ qua bigram (dòng "  bigram=...").

Output: java/assets/vietnamese_words.txt  — 1 dòng / từ:  "chào\t162"
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "main_vi.combined")
ASSETS = os.path.normpath(os.path.join(HERE, "..", "java", "assets"))
OUT = os.path.join(ASSETS, "vietnamese_words.txt")
OUT_BG = os.path.join(ASSETS, "vietnamese_bigrams.txt")

WORD_RE = re.compile(r"^\s*word=(.+?),f=(\d+)")
BIGRAM_RE = re.compile(r"^\s*bigram=(.+?),f=(\d+)")


def main():
    if not os.path.isfile(SRC):
        print(f"ERROR: not found {SRC}", file=sys.stderr)
        return 1

    entries = []
    bigrams = []  # (prev_word, next_word, freq)
    cur_word = None
    with open(SRC, "r", encoding="utf-8") as f:
        for line in f:
            mb = BIGRAM_RE.match(line)
            if mb and "bigram=" in line:
                if cur_word is not None:
                    nxt = mb.group(1).strip()
                    if nxt:
                        bigrams.append((cur_word, nxt, int(mb.group(2))))
                continue
            m = WORD_RE.match(line)
            if not m:
                continue
            word = m.group(1).strip()
            freq = int(m.group(2))
            if not word:
                continue
            cur_word = word
            entries.append((word, freq))

    # gộp trùng (lấy freq lớn nhất), giữ thứ tự tần suất giảm dần để engine load có lợi
    best = {}
    for w, fr in entries:
        if w not in best or fr > best[w]:
            best[w] = fr
    merged = sorted(best.items(), key=lambda x: (-x[1], x[0]))

    os.makedirs(ASSETS, exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        for w, fr in merged:
            f.write(f"{w}\t{fr}\n")

    # bigram: prev<TAB>next<TAB>freq  (prev/next thường ngữ cảnh -> ưu tiên ứng viên đúng)
    with open(OUT_BG, "w", encoding="utf-8", newline="\n") as f:
        for prev, nxt, fr in bigrams:
            f.write(f"{prev}\t{nxt}\t{fr}\n")

    print(f"Wrote {len(merged)} words -> {OUT}")
    print(f"Wrote {len(bigrams)} bigrams -> {OUT_BG}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
