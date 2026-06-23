import struct
import sys
import unicodedata

sys.stdout.reconfigure(encoding='utf-8')

def read_char(data, offset):
    b = data[offset]
    offset += 1
    if b == 0x1F:
        return 0x1F, offset
    elif (b & 0xE0) == 0:
        val = ((b & 0x1F) << 16) + (data[offset] << 8) + data[offset+1]
        offset += 2
        return val, offset
    else:
        return b, offset

def read_string(data, offset):
    chars = []
    while True:
        b = data[offset]
        if b == 0x1F:
            offset += 1
            break
        val, offset = read_char(data, offset)
        chars.append(chr(val))
    return ''.join(chars), offset

def read_node_count(data, offset):
    b = data[offset]
    offset += 1
    if (b & 0x80) != 0:
        val = ((b & 0x7F) << 8) + data[offset]
        offset += 1
        return val, offset
    else:
        return b, offset

def traverse_trie(data, start_offset, current_prefix='', words_dict=None):
    if words_dict is None:
        words_dict = []
    
    offset = start_offset
    if offset >= len(data):
        return words_dict
        
    node_count, offset = read_node_count(data, offset)
    children_to_visit = []
    
    for _ in range(node_count):
        if offset >= len(data):
            break
        flags = data[offset]
        offset += 1
        
        if flags & 0x20:
            node_str, offset = read_string(data, offset)
        else:
            char_val, offset = read_char(data, offset)
            node_str = chr(char_val)
            
        node_prefix = current_prefix + node_str
        
        freq = None
        if flags & 0x10:
            freq = data[offset]
            offset += 1
            
        addr_type = flags & 0xC0
        children_addr = None
        if addr_type != 0:
            pos_addr = offset
            if addr_type == 0x40:
                val = data[offset]
                offset += 1
            elif addr_type == 0x80:
                val = (data[offset] << 8) + data[offset+1]
                offset += 2
            else:
                val = (data[offset] << 16) + (data[offset+1] << 8) + data[offset+2]
                offset += 3
            children_addr = pos_addr + val
            
        if (flags & 0x10) and (flags & 0x08):
            shortcut_list_size = (data[offset] << 8) + data[offset+1]
            offset += 2 + shortcut_list_size
            
        if (flags & 0x10) and (flags & 0x04):
            while True:
                bg_flags = data[offset]
                offset += 1
                bg_addr_type = bg_flags & 0x30
                if bg_addr_type == 0x10:
                    offset += 1
                elif bg_addr_type == 0x20:
                    offset += 2
                elif bg_addr_type == 0x30:
                    offset += 3
                if not (bg_flags & 0x80):
                    break
                    
        if freq is not None and not (flags & 0x02):
            words_dict.append((node_prefix, freq))
            
        if children_addr is not None:
            children_to_visit.append((children_addr, node_prefix))
            
    for child_addr, child_prefix in children_to_visit:
        traverse_trie(data, child_addr, child_prefix, words_dict)
            
    return words_dict

def load_dict(filepath):
    data = open(filepath, 'rb').read()
    header_size = struct.unpack('>I', data[8:12])[0]
    return traverse_trie(data, header_size)

if __name__ == '__main__':
    words = load_dict('f:/Code/modapk/vnkey/dict_vi/main_vi.dict')
    print("Tổng số từ load được:", len(words))
    
    # Thử nghiệm gợi ý cho 'cho'
    accents = {
        'á': 'as', 'ắ': 'aws', 'ấ': 'aas', 'é': 'es', 'ế': 'ees', 'í': 'is', 'ó': 'os', 'ố': 'oos', 'ớ': 'ows', 'ú': 'us', 'ứ': 'uws', 'ý': 'ys',
        'à': 'af', 'ằ': 'awf', 'ầ': 'aaf', 'è': 'ef', 'ề': 'eef', 'ì': 'if', 'ò': 'of', 'ồ': 'oof', 'ờ': 'owf', 'ù': 'uf', 'ừ': 'uwf', 'ỳ': 'yf',
        'ả': 'ar', 'ẳ': 'awr', 'ẩ': 'aar', 'ẻ': 'er', 'ể': 'eer', 'ỉ': 'ir', 'ỏ': 'or', 'ổ': 'oor', 'ở': 'owr', 'ủ': 'ur', 'ử': 'uwr', 'ỷ': 'yr',
        'ã': 'ax', 'ẵ': 'awx', 'ẫ': 'aax', 'ẽ': 'ex', 'ễ': 'eex', 'ĩ': 'ix', 'õ': 'ox', 'ỗ': 'oox', 'ỡ': 'owx', 'ũ': 'ux', 'ữ': 'uwx', 'ỹ': 'yx',
        'ạ': 'aj', 'ặ': 'awj', 'ậ': 'aaj', 'ẹ': 'ej', 'ệ': 'eej', 'ị': 'ij', 'ọ': 'oj', 'ộ': 'ooj', 'ợ': 'owj', 'ụ': 'uj', 'ự': 'uwj', 'ỵ': 'yj',
    }
    base_chars = {'â': 'aa', 'ă': 'aw', 'ê': 'ee', 'ô': 'oo', 'ơ': 'ow', 'ư': 'uw', 'đ': 'dd'}
    
    def to_telex(word):
        telex = ''
        for char in word.lower():
            if char in accents: telex += accents[char]
            elif char in base_chars: telex += base_chars[char]
            else: telex += char
        return telex
        
    def remove_accents(input_str):
        s = unicodedata.normalize('NFD', input_str)
        s = ''.join([c for c in s if unicodedata.category(c) != 'Mn'])
        return s.replace('đ', 'd').replace('Đ', 'D').lower()

    user_input = 'cho'
    user_input_telex = to_telex(user_input)
    user_input_clean = remove_accents(user_input)

    matches = []
    for word, freq in words:
        word_telex = to_telex(word)
        word_clean = remove_accents(word)
        if word_telex.startswith(user_input_telex) or word_clean.startswith(user_input_clean):
            if word.lower() == user_input.lower():
                continue
            extra_keys = len(word_telex) - len(user_input_telex)
            penalty = 0
            if 'w' in word_telex and 'w' not in user_input_telex:
                penalty += 4
            score = extra_keys + penalty
            matches.append((word, freq, score))

    matches.sort(key=lambda x: (x[2], -x[1], len(x[0])))
    
    print('Gợi ý cho "cho":')
    for w, f, s in matches[:10]:
        print(f'- {w} (freq: {f}, score: {s})')
