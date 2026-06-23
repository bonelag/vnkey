import os
import sys
import unicodedata

def remove_accents(input_str):
    if not input_str:
        return ""
    s = unicodedata.normalize('NFD', input_str)
    s = ''.join([c for c in s if unicodedata.category(c) != 'Mn'])
    s = s.replace('đ', 'd').replace('Đ', 'D')
    return s.lower()

def main():
    path = r"f:\Code\modapk\vnkey\dict_vi\list-syllable-special-words.txt"
    if not os.path.exists(path):
        print("Not found")
        return
        
    syllables = set()
    with open(path, 'r', encoding='utf-16') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            parts = line.split('\t')
            if parts:
                word = parts[0]
                # Split multi-syllable words by space
                for syl in word.split():
                    syllables.add(syl.lower())
                    
    print(f"Total unique accented syllables: {len(syllables)}")
    
    clean_syllables = set()
    for syl in syllables:
        clean_syllables.add(remove_accents(syl))
        
    print(f"Total unique clean syllables: {len(clean_syllables)}")
    
    # Sort and write to a output file to see what they look like
    sorted_clean = sorted(list(clean_syllables))
    print("Writing to clean_syllables.txt...")
    with open(r"f:\Code\modapk\vnkey\dict_vi\clean_syllables.txt", "w", encoding="utf-8") as f:
        for s in sorted_clean:
            f.write(s + "\n")
            
if __name__ == '__main__':
    main()
