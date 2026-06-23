import os
import sys
import msvcrt
from dict_reader import load_blacklist, load_dictionary
from suggest_engine import SuggestEngine

# Kích hoạt ANSI escape codes trên Windows Console
os.system('')

# Reconfigure stdout sang UTF-8 để hiển thị tiếng Việt có dấu
sys.stdout.reconfigure(encoding='utf-8')

def get_current_word(text):
    """
    Lấy từ cuối cùng đang được gõ trong câu.
    """
    if not text:
        return ""
    parts = text.split(' ')
    return parts[-1]

def replace_last_word(text, new_word):
    """
    Thay thế từ cuối cùng đang được gõ bằng từ gợi ý mới và thêm khoảng trắng.
    """
    parts = text.split(' ')
    if len(parts) <= 1:
        return new_word + " "
    return " ".join(parts[:-1]) + " " + new_word + " "

def main():
    current_dir = os.path.dirname(os.path.abspath(__file__))
    blacklist_path = os.path.join(current_dir, 'blacklist.dict')
    dict_path = os.path.join(current_dir, 'main_vi.dict')
    
    print("Đang tải dữ liệu từ điển tiếng Việt (vui lòng đợi trong giây lát)...")
    bl = load_blacklist(blacklist_path)
    dict_words = load_dictionary(dict_path, bl)
    
    if not dict_words:
        print("Lỗi: Không tải được từ điển.")
        return
        
    engine = SuggestEngine(dict_words)
    
    # Xóa màn hình chuẩn bị giao diện
    os.system('cls' if os.name == 'nt' else 'clear')
    
    print("=== CHƯƠNG TRÌNH ĐOÁN VÀ GỢI Ý TỪ TIẾNG VIỆT (LABAN KEY MOCK) ===")
    print("Hướng dẫn: Gõ phím để nhập chữ. Nhấn phím số 1-5 để chọn từ gợi ý tương ứng.")
    print("Nhấn Backspace để xóa. Nhấn Enter để xuống dòng. Nhấn ESC để thoát chương trình.")
    print("-" * 70)
    print() # Dòng trống dự phòng cho dòng Gợi ý
    print() # Dòng trống dự phòng cho dòng Input
    
    input_text = ""
    suggestions = []
    
    # Biến trạng thái để vẽ lại giao diện
    # Ta đang ở dòng Input (dòng dưới), dòng Gợi ý ở ngay phía trên 1 dòng
    first_render = True
    
    while True:
        # Lấy từ cuối cùng đang được gõ
        current_word = get_current_word(input_text)
        
        # Lấy các gợi ý
        if current_word:
            suggestions = engine.get_suggestions(current_word, max_suggestions=5)
        else:
            suggestions = []
            
        # 1. Vẽ giao diện bằng ANSI escape codes
        # Di chuyển lên 1 dòng để ghi đè dòng gợi ý
        sys.stdout.write("\033[F\r\033[K")
        
        # Tạo chuỗi gợi ý
        sug_str = "Gợi ý: "
        if suggestions:
            sug_str += "  ".join([f"[{i+1}] {word}" for i, word in enumerate(suggestions)])
        else:
            sug_str += "(Không có gợi ý)"
        sys.stdout.write(sug_str + "\n")
        
        # Ghi đè dòng Input
        sys.stdout.write("\r\033[K")
        sys.stdout.write(f"Input: {input_text}")
        sys.stdout.flush()
        
        # 2. Bắt sự kiện phím gõ từ msvcrt
        # getwch() trả về ký tự Unicode gõ vào console mà không chờ nhấn Enter
        ch = msvcrt.getwch()
        
        # Xử lý phím Esc để thoát
        if ch == '\x1b':
            print("\n\nĐã thoát chương trình.")
            break
            
        # Xử lý phím Backspace để xóa ký tự
        elif ch in ('\x08', '\x7f', '\b'):
            if input_text:
                input_text = input_text[:-1]
                
        # Xử lý phím số 1-5 để chọn từ gợi ý
        elif ch in ('1', '2', '3', '4', '5'):
            idx = int(ch) - 1
            if idx < len(suggestions):
                input_text = replace_last_word(input_text, suggestions[idx])
                
        # Xử lý phím Enter
        elif ch == '\r' or ch == '\n':
            # Xuống dòng, in ra câu hoàn chỉnh và chuẩn bị nhập câu mới
            sys.stdout.write("\n\n")
            print(f"-> Câu đã nhập: {input_text.strip()}")
            print("-" * 50)
            input_text = ""
            suggestions = []
            print() # Dòng trống dự phòng cho dòng Gợi ý
            print() # Dòng trống dự phòng cho dòng Input
            
        # Xử lý các ký tự thường khác (phím chữ, dấu cách, ký tự đặc biệt)
        else:
            # Chỉ nhận các ký tự in ra được
            if ch.isprintable() or ch == ' ':
                input_text += ch

if __name__ == '__main__':
    main()
