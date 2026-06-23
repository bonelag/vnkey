import os
import sys
import struct

# Thiết lập encoding cho console để tránh lỗi hiển thị tiếng Việt trên Windows
sys.stdout.reconfigure(encoding='utf-8')

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

def read_forward_link(data, offset):
    """
    Đọc forward link ở cuối group (PtNodeArray).
    """
    b1 = data[offset]
    b2 = data[offset+1]
    b3 = data[offset+2]
    pos_start = offset
    offset += 3
    if (b1 & 0x80) != 0:
        val = -(((b1 & 0x7F) << 16) + (b2 << 8) + b3)
    else:
        val = ((b1 & 0x7F) << 16) + (b2 << 8) + b3
    if val == 0:
        return None, offset
    return pos_start + val, offset

def traverse_trie_collect(data, start_offset, current_prefix='', words_list=None, node_map=None):
    """
    Duyệt Trie lần 1 để:
    1. Thu thập map node_start -> word
    2. Lưu danh sách từ kèm tần suất và dữ liệu bigram thô
    """
    if words_list is None:
        words_list = []
    if node_map is None:
        node_map = {}
        
    offset = start_offset
    if offset >= len(data):
        return words_list, node_map
        
    node_count, offset = read_node_count(data, offset)
    children_to_visit = []
    
    for _ in range(node_count):
        if offset >= len(data):
            break
        node_start = offset # Vị trí byte Flags của Node
        
        flags = data[offset]
        offset += 1
        
        # Đọc ký tự/chuỗi ký tự của Node
        if flags & 0x20:  # FLAG_HAS_MULTIPLE_CHARS
            node_str, offset = read_string(data, offset)
        else:
            char_val, offset = read_char(data, offset)
            node_str = chr(char_val)
            
        node_prefix = current_prefix + node_str
        
        # Đăng ký vị trí node_start vào node_map
        node_map[node_start] = node_prefix
        
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
            children_addr = pos_addr + val
            
        # Nhảy qua danh sách shortcut nếu có
        if (flags & 0x10) and (flags & 0x08):  # FLAG_IS_TERMINAL & FLAG_HAS_SHORTCUT_TARGETS
            shortcut_list_size = (data[offset] << 8) + data[offset+1]
            offset += 2 + shortcut_list_size
            
        # Đọc và lưu trữ thông tin bigram thô
        bigrams_raw = []
        if (flags & 0x10) and (flags & 0x04):  # FLAG_IS_TERMINAL & FLAG_HAS_BIGRAMS
            while True:
                bg_flags = data[offset]
                offset += 1
                bg_addr_type = bg_flags & 0x30
                bg_addr_val = None
                pos_bg_addr = offset
                if bg_addr_type == 0x10:
                    bg_addr_val = data[offset]
                    offset += 1
                elif bg_addr_type == 0x20:
                    bg_addr_val = (data[offset] << 8) + data[offset+1]
                    offset += 2
                elif bg_addr_type == 0x30:
                    bg_addr_val = (data[offset] << 16) + (data[offset+1] << 8) + data[offset+2]
                    offset += 3
                    
                if bg_addr_val is not None:
                    # Kiểm tra dấu âm/dương của offset bigram
                    if bg_flags & 0x40:  # FLAG_BIGRAM_ATTR_OFFSET_NEGATIVE
                        bg_addr_val = -bg_addr_val
                    bigrams_raw.append((bg_flags, pos_bg_addr, bg_addr_val))
                    
                if not (bg_flags & 0x80):  # FLAG_BIGRAM_SHORTCUT_ATTR_HAS_NEXT
                    break
                    
        # Nếu là từ hợp lệ (không có cờ FLAG_IS_NOT_A_WORD)
        if freq is not None and not (flags & 0x02):
            words_list.append({
                'word': node_prefix,
                'freq': freq,
                'bigrams_raw': bigrams_raw
            })
            
        # Lưu địa chỉ con
        if children_addr is not None:
            children_to_visit.append((children_addr, node_prefix))
            
    # Duyệt đệ quy các group con theo DFS
    for child_addr, child_prefix in children_to_visit:
        traverse_trie_collect(data, child_addr, child_prefix, words_list, node_map)
            
    return words_list, node_map

def reverse_dict_to_combined(input_dict_path, output_combined_path):
    """
    Giải mã ngược file từ điển nhị phân (.dict) về dạng file thô .combined chuẩn của AOSP LatinIME.
    """
    if not os.path.exists(input_dict_path):
        print(f"Lỗi: Không tìm thấy file nhị phân tại: {input_dict_path}")
        return False
        
    print(f"Đang đọc và phân tích file nhị phân: {os.path.basename(input_dict_path)}...")
    
    try:
        data = open(input_dict_path, 'rb').read()
        
        # 1. Đọc kích thước header
        header_size = struct.unpack('>I', data[8:12])[0]
        print(f"-> Header size: {header_size} byte")
        
        # 2. Đọc siêu dữ liệu (Attributes) của Header
        header_bytes = data[12:header_size]
        attributes = []
        current_attr = []
        for b in header_bytes:
            if b in (0x1F, 0x00):
                if current_attr:
                    attr_str = ''.join(chr(c) if 32 <= c < 127 or c > 127 else '.' for c in current_attr)
                    attributes.append(attr_str)
                    current_attr = []
            else:
                current_attr.append(b)
                
        # Tạo từ điển thuộc tính header
        header_map = {}
        for i in range(0, len(attributes) - 1, 2):
            if i + 1 < len(attributes):
                header_map[attributes[i]] = attributes[i+1]
                
        print("-> Thông tin Header:")
        for k, v in header_map.items():
            print(f"   + {k}: {v}")
            
        # Tạo dòng đầu tiên định dạng: dictionary=key=value,key=value...
        header_parts = []
        for key in ['dictionary', 'locale', 'description', 'date', 'version']:
            if key in header_map:
                header_parts.append(f"{key}={header_map[key]}")
        header_line = ",".join(header_parts)
        
        # 3. Quét lần 1: Duyệt Trie để thu thập danh sách từ và map địa chỉ Node
        print("-> Đang quét cấu trúc Trie dữ liệu (Lần 1: Thu thập địa chỉ Node)...")
        words_list, node_map = traverse_trie_collect(data, header_size)
        print(f"-> Đã quét xong. Tổng số node từ vựng trích xuất: {len(words_list)}")
        
        # 4. Giải mã các bigram thô bằng cách tra cứu trong node_map
        print("-> Đang giải mã các địa chỉ Bigram sang chữ viết tương ứng...")
        decoded_words = []
        for item in words_list:
            word = item['word']
            freq = item['freq']
            bigrams = []
            
            for bg_flags, pos_bg_addr, bg_addr_val in item['bigrams_raw']:
                # Tính địa chỉ tuyệt đối của Node bigram
                target_addr = pos_bg_addr + bg_addr_val
                # Tra cứu ngược từ trong node_map
                bg_word = node_map.get(target_addr)
                if bg_word:
                    bg_freq = bg_flags & 0x0F # Tần suất của bigram (4-bit thấp)
                    bigrams.append((bg_word, bg_freq))
                    
            decoded_words.append({
                'word': word,
                'freq': freq,
                'bigrams': bigrams
            })
            
        # 5. Sắp xếp danh sách từ theo tần suất giảm dần để tạo file thô tối ưu nhất
        print("-> Đang sắp xếp danh sách từ theo tần suất...")
        decoded_words.sort(key=lambda x: -x['freq'])
        
        # 6. Ghi ra file .combined chuẩn của AOSP LatinIME
        print(f"-> Đang ghi file thô chuẩn ra: {os.path.basename(output_combined_path)}...")
        with open(output_combined_path, 'w', encoding='utf-8') as f:
            # Ghi dòng header
            f.write(f"{header_line}\n")
            
            for item in decoded_words:
                word = item['word']
                freq = item['freq']
                # Định dạng từ chính: có 1 dấu cách ở đầu
                f.write(f" word={word},f={freq}\n")
                
                # Ghi các dòng bigram đi kèm
                for bg_word, bg_freq in item['bigrams']:
                    # Định dạng bigram: có 2 dấu cách ở đầu
                    f.write(f"  bigram={bg_word},f={bg_freq}\n")
                    
        print(f"Thành công! File thô đã được xuất tại: {output_combined_path}")
        return True
        
    except Exception as e:
        print(f"Đã xảy ra lỗi khi tạo file thô: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == '__main__':
    current_dir = os.path.dirname(os.path.abspath(__file__))
    
    input_path = os.path.join(current_dir, 'main_vi.dict')
    output_path = os.path.join(current_dir, 'main_vi_raw.combined')
    
    if len(sys.argv) > 1:
        input_path = sys.argv[1]
    if len(sys.argv) > 2:
        output_path = sys.argv[2]
        
    reverse_dict_to_combined(input_path, output_path)
