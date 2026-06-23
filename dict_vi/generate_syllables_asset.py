import os
import unicodedata

def remove_accents(input_str):
    if not input_str:
        return ""
    s = unicodedata.normalize('NFD', input_str)
    s = ''.join([c for c in s if unicodedata.category(c) != 'Mn'])
    s = s.replace('đ', 'd').replace('Đ', 'D')
    return s.lower()

def extract_from_txt(path, syllables):
    if not os.path.exists(path):
        return
    with open(path, 'r', encoding='utf-16') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            parts = line.split('\t')
            if parts:
                word = parts[0]
                for syl in word.split():
                    syllables.add(syl.lower())

def extract_from_combined(path, syllables):
    if not os.path.exists(path):
        return
    with open(path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line.startswith('word='):
                # Format: word=không,f=175
                word_part = line.split(',')[0]
                word = word_part.split('=')[1]
                for syl in word.split():
                    syllables.add(syl.lower())
            elif line.startswith('bigram='):
                # Format: bigram=có,f=8
                word_part = line.split(',')[0]
                word = word_part.split('=')[1]
                for syl in word.split():
                    syllables.add(syl.lower())

def main():
    syllables = set()
    
    # Extract from both files
    extract_from_txt(r"f:\Code\modapk\vnkey\dict_vi\list-syllable-special-words.txt", syllables)
    extract_from_combined(r"f:\Code\modapk\vnkey\dict_vi\main_vi.combined", syllables)
    
    print(f"Total unique accented syllables: {len(syllables)}")
    
    clean_syllables = set()
    for syl in syllables:
        # Keep only strings with alphabetic characters
        clean_syl = remove_accents(syl)
        if clean_syl.isalpha():
            clean_syllables.add(clean_syl)
        
    print(f"Total unique clean syllables: {len(clean_syllables)}")
    
    # Create the assets folder if it doesn't exist
    assets_dir = r"f:\Code\modapk\vnkey\java\assets"
    os.makedirs(assets_dir, exist_ok=True)
    
    # Write to assets/vietnamese_syllables.txt (separated by spaces for compact file size)
    sorted_clean = sorted(list(clean_syllables))
    output_path = os.path.join(assets_dir, "vietnamese_syllables.txt")
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(" ".join(sorted_clean))
        
    print(f"Successfully generated {output_path}")

if __name__ == '__main__':
    main()
