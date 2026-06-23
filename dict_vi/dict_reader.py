import os
import struct

def read_char(data, offset):
    """
    Đọc 1 ký tự theo định dạng Char Format của LatinIME v2.
    """
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
    """
    Đọc chuỗi nhiều ký tự (multiple chars) kết thúc bằng terminator 0x1F.
    """
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
    """
    Đọc số lượng node con trong group (PtNodeArray).
    """
    b = data[offset]
    offset += 1
    if (b & 0x80) != 0:
        val = ((b & 0x7F) << 8) + data[offset]
        offset += 1
        return val, offset
    else:
        return b, offset

def traverse_trie(data, start_offset, current_prefix='', words_dict=None):
    """
    Duyệt đệ quy toàn bộ Trie của LatinIME v2 (DFS) để trích xuất từ vựng và tần suất.
    """
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
        
        # Đọc ký tự/chuỗi ký tự của Node
        if flags & 0x20:  # FLAG_HAS_MULTIPLE_CHARS
            node_str, offset = read_string(data, offset)
        else:
            char_val, offset = read_char(data, offset)
            node_str = chr(char_val)
            
        node_prefix = current_prefix + node_str
        
        # Đọc tần suất nếu là Terminal Node
        freq = None
        if flags & 0x10:  # FLAG_IS_TERMINAL
            freq = data[offset]
            offset += 1
            
        # Đọc địa chỉ con
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
            # children_addr là tương đối so với vị trí trường address
            children_addr = pos_addr + val
            
        # Nhảy qua danh sách shortcut nếu có
        if (flags & 0x10) and (flags & 0x08):  # FLAG_IS_TERMINAL & FLAG_HAS_SHORTCUT_TARGETS
            shortcut_list_size = (data[offset] << 8) + data[offset+1]
            offset += 2 + shortcut_list_size
            
        # Nhảy qua danh sách bigram nếu có
        if (flags & 0x10) and (flags & 0x04):  # FLAG_IS_TERMINAL & FLAG_HAS_BIGRAMS
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
                if not (bg_flags & 0x80):  # FLAG_BIGRAM_SHORTCUT_ATTR_HAS_NEXT
                    break
                    
        # Nếu là từ hợp lệ (và không có cờ FLAG_IS_NOT_A_WORD)
        if freq is not None and not (flags & 0x02):
            words_dict.append((node_prefix, freq))
            
        # Lưu lại địa chỉ con để duyệt đệ quy sau khi đọc xong toàn bộ group
        if children_addr is not None:
            children_to_visit.append((children_addr, node_prefix))
            
    # Duyệt đệ quy các group con theo thứ tự DFS
    for child_addr, child_prefix in children_to_visit:
        traverse_trie(data, child_addr, child_prefix, words_dict)
            
    return words_dict

def load_blacklist(filepath):
    """
    Tải danh sách các từ bị cấm (blacklist).
    """
    blacklist = set()
    if not os.path.exists(filepath):
        return blacklist
    
    for encoding in ['utf-8', 'utf-16', 'latin-1']:
        try:
            with open(filepath, 'r', encoding=encoding) as f:
                for line in f:
                    word = line.strip().lower()
                    if word:
                        blacklist.add(word)
            break
        except UnicodeDecodeError:
            continue
            
    return blacklist

def load_dictionary(filepath, blacklist=None):
    """
    Giải mã từ điển nhị phân LatinIME v2 (.dict) và trả về danh sách từ vựng.
    Bỏ qua các từ nằm trong blacklist.
    """
    if blacklist is None:
        blacklist = set()
        
    words = []
    if not os.path.exists(filepath):
        print(f"Lỗi: Không tìm thấy file từ điển tại {filepath}")
        return words
        
    try:
        data = open(filepath, 'rb').read()
        # Đọc header size (4 byte tại offset 8, big endian)
        header_size = struct.unpack('>I', data[8:12])[0]
        
        # Duyệt trie để lấy danh sách các từ
        raw_words = traverse_trie(data, header_size)
        
        # Lọc qua blacklist
        for word, freq in raw_words:
            if word.lower() not in blacklist:
                words.append((word, freq))
                
    except Exception as e:
        print(f"Lỗi khi giải mã file từ điển nhị phân: {e}")
        
    return words

if __name__ == '__main__':
    current_dir = os.path.dirname(os.path.abspath(__file__))
    blacklist_path = os.path.join(current_dir, 'blacklist.dict')
    dict_path = os.path.join(current_dir, 'main_vi.dict')
    
    bl = load_blacklist(blacklist_path)
    print(f"Đã tải {len(bl)} từ trong blacklist.")
    
    dict_words = load_dictionary(dict_path, bl)
    print(f"Đã giải mã thành công {len(dict_words)} từ từ file nhị phân main_vi.dict.")
    if dict_words:
        print("Ví dụ 5 từ đầu tiên:", dict_words[:5])
