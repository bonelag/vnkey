import os

def main():
    path = r"f:\Code\modapk\vnkey\dict_vi\list-syllable-special-words.txt"
    if not os.path.exists(path):
        print("Not found")
        return
        
    unique_words = set()
    total_lines = 0
    with open(path, 'r', encoding='utf-16') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            total_lines += 1
            parts = line.split('\t')
            if parts:
                unique_words.add(parts[0])
                
    print(f"Total lines: {total_lines}")
    print(f"Unique words: {len(unique_words)}")
    
    # Print 20 words
    sample = list(unique_words)[:50]
    print("Sample words:")
    # print safely
    print([w.encode('utf-8') for w in sample])

if __name__ == '__main__':
    main()
