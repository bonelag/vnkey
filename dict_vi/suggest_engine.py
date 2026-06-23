import unicodedata

# Bản đồ ánh xạ nguyên âm tiếng Việt có dấu sang tổ hợp phím Telex
ACCENTS_TELEX = {
    'á': 'as', 'ắ': 'aws', 'ấ': 'aas', 'é': 'es', 'ế': 'ees', 'í': 'is', 'ó': 'os', 'ố': 'oos', 'ớ': 'ows', 'ú': 'us', 'ứ': 'uws', 'ý': 'ys',
    'à': 'af', 'ằ': 'awf', 'ầ': 'aaf', 'è': 'ef', 'ề': 'eef', 'ì': 'if', 'ò': 'of', 'ồ': 'oof', 'ờ': 'owf', 'ù': 'uf', 'ừ': 'uwf', 'ỳ': 'yf',
    'ả': 'ar', 'ẳ': 'awr', 'ẩ': 'aar', 'ẻ': 'er', 'ể': 'eer', 'ỉ': 'ir', 'ỏ': 'or', 'ổ': 'oor', 'ở': 'owr', 'ủ': 'ur', 'ử': 'uwr', 'ỷ': 'yr',
    'ã': 'ax', 'ẵ': 'awx', 'ẫ': 'aax', 'ẽ': 'ex', 'ễ': 'eex', 'ĩ': 'ix', 'õ': 'ox', 'ỗ': 'oox', 'ỡ': 'owx', 'ũ': 'ux', 'ữ': 'uwx', 'ỹ': 'yx',
    'ạ': 'aj', 'ặ': 'awj', 'ậ': 'aaj', 'ẹ': 'ej', 'ệ': 'eej', 'ị': 'ij', 'ọ': 'oj', 'ộ': 'ooj', 'ợ': 'owj', 'ụ': 'uj', 'ự': 'uwj', 'ỵ': 'yj',
    'đ': 'dd', 'Đ': 'dd',
}

BASE_CHARS_TELEX = {
    'â': 'aa', 'ă': 'aw', 'ê': 'ee', 'ô': 'oo', 'ơ': 'ow', 'ư': 'uw',
    'Â': 'aa', 'Ă': 'aw', 'Ê': 'ee', 'Ô': 'oo', 'Ơ': 'ow', 'Ư': 'uw',
}

def remove_accents(input_str):
    """
    Loại bỏ dấu tiếng Việt (ví dụ: 'chọn' -> 'chon').
    """
    if not input_str:
        return ""
    # Chuẩn hóa NFD để tách các ký tự dấu
    s = unicodedata.normalize('NFD', input_str)
    # Loại bỏ các ký tự dấu (Unicode category Mn)
    s = ''.join([c for c in s if unicodedata.category(c) != 'Mn'])
    # Thay thế đ/Đ
    s = s.replace('đ', 'd').replace('Đ', 'D')
    return s.lower()

def to_telex(word):
    """
    Chuyển đổi một từ tiếng Việt sang dạng Telex thô.
    Ví dụ: 'chó' -> 'chos', 'chọn' -> 'chojn' (hoặc 'chonj' tùy theo vị trí dấu).
    Ở đây ta sử dụng cơ chế chuyển đổi trực tiếp ký tự để dễ dàng so khớp tiền tố.
    """
    telex = ''
    for char in word:
        if char in ACCENTS_TELEX:
            telex += ACCENTS_TELEX[char]
        elif char in BASE_CHARS_TELEX:
            telex += BASE_CHARS_TELEX[char]
        else:
            telex += char.lower()
    return telex

class SuggestEngine:
    def __init__(self, dictionary_words):
        """
        dictionary_words: Danh sách các tuple (word, frequency)
        """
        # Tiền xử lý từ điển để lưu trữ dạng không dấu và dạng Telex giúp tìm kiếm nhanh hơn
        self.dict_data = []
        for word, freq in dictionary_words:
            word_clean = remove_accents(word)
            word_telex = to_telex(word)
            self.dict_data.append({
                'word': word,
                'freq': freq,
                'clean': word_clean,
                'telex': word_telex
            })

    def get_suggestions(self, user_input, max_suggestions=5):
        """
        Gợi ý từ dựa trên input của người dùng.
        """
        if not user_input:
            return []
            
        user_input_clean = remove_accents(user_input)
        user_input_telex = to_telex(user_input)
        
        matches = []
        for item in self.dict_data:
            word = item['word']
            word_clean = item['clean']
            word_telex = item['telex']
            freq = item['freq']
            
            # Kiểm tra xem từ trong từ điển có khớp tiền tố với input không (ở cả dạng Telex và không dấu)
            # Ví dụ: gõ 'cho' -> khớp với 'chọn' (Telex: chojn hoặc clean: chon)
            is_match = word_telex.startswith(user_input_telex) or word_clean.startswith(user_input_clean)
            
            if is_match:
                # Không gợi ý lại chính xác từ thô mà người dùng đang gõ (nếu từ trong từ điển trùng khít)
                # để nhường chỗ cho các từ có dấu/từ gợi ý khác
                if word.lower() == user_input.lower():
                    continue
                    
                # 1. Tính toán khoảng cách gõ thêm (Telex distance)
                extra_keys = len(word_telex) - len(user_input_telex)
                
                # 2. Cơ chế phạt (Penalty): 
                # Nếu từ gợi ý chứa chữ 'w' (ơ, ư, ă) nhưng input của người dùng không gõ 'w',
                # ta sẽ phạt khoảng cách phím để ưu tiên các từ có gốc nguyên âm o/ô hơn.
                # Ví dụ: gõ 'cho' thì 'chọn' (chojn) hoặc 'chó' (chos) sẽ đứng trước 'chơi' (chowi).
                penalty = 0
                if 'w' in word_telex and 'w' not in user_input_telex:
                    penalty += 3
                    
                # 3. Điểm số xếp hạng cuối cùng (càng thấp càng ưu tiên)
                score = extra_keys + penalty
                
                matches.append((word, freq, score))
                
        # Sắp xếp danh sách:
        # - Ưu tiên hàng đầu: Điểm số gõ phím 'score' thấp nhất (phím gõ thêm ít nhất).
        # - Ưu tiên thứ hai: Tần suất sử dụng 'freq' cao nhất.
        # - Ưu tiên thứ ba: Độ dài từ ngắn nhất.
        matches.sort(key=lambda x: (x[2], -x[1], len(x[0])))
        
        # Trả về danh sách các từ gợi ý độc nhất
        result = []
        seen = set()
        for w, f, s in matches:
            if w.lower() not in seen:
                result.append(w)
                seen.add(w.lower())
                if len(result) >= max_suggestions:
                    break
                    
        return result

if __name__ == '__main__':
    # Test thử thuật toán gợi ý
    test_dict = [
        ('chơi', 155),
        ('chọn', 155),
        ('cho', 155),
        ('chó', 153),
        ('chôn', 132),
        ('chồng', 155),
        ('chống', 154),
        ('chỗ', 154),
        ('chờ', 153),
        ('chợ', 152),
    ]
    engine = SuggestEngine(test_dict)
    
    # In kết quả kiểm tra với input 'cho'
    print("Suggestions for 'cho':", engine.get_suggestions('cho', 5))
    # Kết quả mong muốn: chọn, chó, chôn (hoặc tương tự theo thứ tự điểm số)
