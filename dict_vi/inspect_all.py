import os

def main():
    dict_dir = r"f:\Code\modapk\vnkey\dict_vi"
    for name in os.listdir(dict_dir):
        path = os.path.join(dict_dir, name)
        if os.path.isfile(path) and (name.endswith(".txt") or name.endswith(".dict") or name.endswith(".combined")):
            print(f"File: {name}, Size: {os.path.getsize(path)}")
            # read first 3 lines if possible
            if name.endswith(".txt") or name.endswith(".combined"):
                try:
                    with open(path, 'r', encoding='utf-16') as f:
                        lines = [f.readline().strip() for _ in range(3)]
                    print("  UTF-16:", [l.encode('utf-8') for l in lines])
                except Exception:
                    pass
                try:
                    with open(path, 'r', encoding='utf-8') as f:
                        lines = [f.readline().strip() for _ in range(3)]
                    print("  UTF-8 :", [l.encode('utf-8') for l in lines])
                except Exception:
                    pass

if __name__ == '__main__':
    main()
