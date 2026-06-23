import os
import sys

def check_file(path):
    print("Checking:", path)
    if not os.path.exists(path):
        print("Not found")
        return
    
    # Try different encodings
    encodings = ['utf-16', 'utf-8', 'latin-1', 'utf-16-le']
    for enc in encodings:
        try:
            with open(path, 'r', encoding=enc) as f:
                lines = [f.readline() for _ in range(10)]
            print(f"Success with {enc}:")
            for l in lines:
                print(repr(l))
            return
        except Exception as e:
            print(f"Failed with {enc}: {e}")

if __name__ == '__main__':
    check_file(r"f:\Code\modapk\vnkey\dict_vi\list-syllable-special-words.txt")
