# Báo Cáo Kỹ Thuật: Phân Tích Định Dạng File `.dict` Của Laban Key Và Cơ Chế Gợi Ý Từ

Tài liệu này giải thích chi tiết cấu trúc nhị phân của các file từ điển `.dict` trong ứng dụng Laban Key (phát triển dựa trên bàn phím Android LatinIME), cách thức giải mã cấu trúc Trie và thuật toán gợi ý từ tiếng Việt thời gian thực.

---

## 1. Phân Tích Định Dạng File `.dict` Nhị Phân (LatinIME v2)

Các file từ điển chính của Laban Key như `main_vi.dict` và `base_char_vi.dict` được đóng gói dưới định dạng **Trie nhị phân nén của Android LatinIME phiên bản 2**. 

Cấu trúc file gồm 2 phần chính: **Header** và **Trie Dữ liệu**.

### A. Cấu trúc Header (90 byte đầu tiên)
Header lưu trữ siêu dữ liệu (metadata) của từ điển dưới dạng các cặp khóa-giá trị (Key-Value) phân tách bởi ký tự `\x1f`:
- **Magic Number (4 byte):** `0x9BC13AFE` (hoặc byte đầu là `\x9B\xC1\x3A\xFE`).
- **Format Version (2 byte):** `0x0002` (phiên bản định dạng 2).
- **Flags (2 byte):** Các cờ cấu hình.
- **Header Size (4 byte - Big Endian):** Độ dài của toàn bộ header (thường là `90` byte, tương ứng giá trị hex `0x0000005A`). Phần Trie dữ liệu sẽ bắt đầu ngay sau offset này.
- **Key-Value Attributes:** Danh sách chuỗi văn bản lưu thông tin như ngày tạo (`date`), ngôn ngữ (`locale`), phiên bản (`version`), mô tả (`description`).

---

### B. Cấu trúc Trie Dữ Liệu
Dữ liệu từ vựng được lưu trữ dưới dạng một cây Trie (Prefix Tree). Cây này được biểu diễn tuyến tính trong file nhị phân thông qua các **Nhóm Node (PtNodeArray)** lồng nhau.

#### 1. Cấu trúc Nhóm Node (PtNodeArray)
Mỗi nhóm node con bắt đầu bằng trường độ dài:
- **Node Count (1 hoặc 2 byte):** Số lượng node con trong nhóm này.
  - Nếu byte đầu tiên có bit cao nhất được set (`b & 0x80 != 0`), số lượng node chiếm 2 byte: `node_count = ((b & 0x7F) << 8) + next_byte`.
  - Ngược lại, chiếm 1 byte: `node_count = b`.
- **PtNodes:** Chuỗi các Node con nằm liên tục ngay sau đó.

#### 2. Cấu trúc Chi tiết của một Node (PtNode)
Mỗi Node đại diện cho một hoặc nhiều ký tự của từ và chứa thông tin liên kết:

| Tên trường | Kích thước | Điều kiện xuất hiện | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| **Flags** | 1 byte | Luôn có | Các cờ thuộc tính của Node (Xem chi tiết bên dưới). |
| **Characters** | 1 hoặc nhiều byte | Luôn có | Ký tự hoặc chuỗi ký tự của Node (Xem định dạng Char Format bên dưới). |
| **Frequency** | 1 byte | Nếu cờ `FLAG_IS_TERMINAL` được set | Tần suất sử dụng của từ kết thúc tại Node này (0-255). |
| **Children Address** | 1, 2, hoặc 3 byte | Nếu cờ địa chỉ con khác `NOADDRESS` | Offset tương đối trỏ tới nhóm node con tiếp theo. |
| **Shortcut List** | Biến đổi | Nếu terminal và có cờ `FLAG_HAS_SHORTCUT_TARGETS` | Nhảy qua bằng cách đọc 2 byte kích thước ở đầu. |
| **Bigram List** | Biến đổi | Nếu terminal và có cờ `FLAG_HAS_BIGRAMS` | Danh sách các từ đi kèm. Nhảy qua bằng cách đọc flags từng phần tử. |

#### 3. Ý nghĩa các bit trong byte `Flags` của Node:
- **Bit 7 - 6 (`0xC0`):** Loại địa chỉ con (`MASK_CHILDREN_ADDRESS_TYPE`):
  - `0x00`: Không có địa chỉ con (nút lá hoàn toàn).
  - `0x40`: Địa chỉ con dài 1 byte.
  - `0x80`: Địa chỉ con dài 2 byte.
  - `0xC0`: Địa chỉ con dài 3 byte.
- **Bit 5 (`0x20`):** `FLAG_HAS_MULTIPLE_CHARS`. Nếu bằng 1, Node chứa một chuỗi ký tự (ví dụ: `TM`, `nh`, `SL`) kết thúc bằng byte `0x1F`. Nếu bằng 0, Node chỉ chứa đúng 1 ký tự.
- **Bit 4 (`0x10`):** `FLAG_IS_TERMINAL`. Bằng 1 nghĩa là đường đi từ gốc đến đây tạo thành một từ hoàn chỉnh có nghĩa.
- **Bit 3 (`0x08`):** `FLAG_HAS_SHORTCUT_TARGETS`. Node có danh sách từ viết tắt/shortcut.
- **Bit 2 (`0x04`):** `FLAG_HAS_BIGRAMS`. Node có chứa liên kết từ đi kèm (Bigram).
- **Bit 1 (`0x02`):** `FLAG_IS_NOT_A_WORD`. Từ này bị vô hiệu hóa (không gợi ý).
- **Bit 0 (`0x01`):** `FLAG_IS_POSSIBLY_OFFENSIVE`. Từ nhạy cảm.

#### 4. Định dạng Mã hóa Ký tự (Char Format)
Các ký tự Unicode (bao gồm ký tự tiếng Việt dựng sẵn) được mã hóa tối ưu để tiết kiệm dung lượng:
- Đọc byte đầu tiên `b`:
  - Nếu `b == 0x1F`: Đây là ký tự kết thúc (terminator) của chuỗi nhiều ký tự.
  - Nếu 3 bit đầu là `0` (`b < 0x20`): Đây là ký tự Unicode 3-byte. Giá trị = `((b & 0x1F) << 16) + (byte_tiep_theo << 8) + byte_tiep_theo_nữa`.
  - Ngược lại (`b >= 0x20`): Đây là ký tự ASCII / ISO-Latin-1 thông thường dài 1 byte, giá trị chính là `b`.

---

## 2. Thuật Toán Giải Mã (Trie Traversal)

Để trích xuất tất cả các từ trong file nhị phân `.dict` sang bộ nhớ Python, chúng ta sử dụng thuật toán duyệt theo chiều sâu (DFS) trên file nhị phân:

1. Đọc kích thước header từ byte thứ 8-11 của file. Đặt con trỏ đọc `offset = header_size`.
2. Hàm đệ quy `traverse_trie(data, offset, current_prefix)`:
   - Đọc `node_count` tại `offset`.
   - Với mỗi node con trong nhóm:
     - Đọc byte `flags`.
     - Giải mã ký tự hoặc chuỗi ký tự theo đúng định dạng **Char Format**, nối vào `current_prefix`.
     - Nếu node là terminal, lưu cặp `(current_prefix, frequency)` vào danh sách từ điển.
     - Tính toán địa chỉ con: `children_addr = pos_addr + offset_val` (trong đó `pos_addr` là vị trí bắt đầu của trường địa chỉ con, và `offset_val` là giá trị 1, 2 hoặc 3 byte đọc được).
     - Nhảy qua các trường `shortcut` và `bigram` (nếu có) để đưa con trỏ đọc đến vị trí của Node tiếp theo một cách chính xác.
     - Lưu `children_addr` vào danh sách chờ duyệt.
   - Sau khi đọc hết tất cả các node trong nhóm hiện tại, duyệt đệ quy tiếp các địa chỉ con đã lưu theo thứ tự DFS để đảm bảo tính tuần tự của file nhị phân.

---

## 3. Giải Thuật Gợi Ý Từ Tiếng Việt Thời Gian Thực

Bàn phím tiếng Việt (Laban Key) cần gợi ý từ cực nhanh ngay khi người dùng gõ phím không dấu hoặc gõ Telex thô (ví dụ gõ `cho` gợi ý `chó`, `chọn`, `chôn`). Script Python mô phỏng giải thuật này như sau:

1. **Telex hóa (Telex encoding):** Chuyển đổi các từ tiếng Việt có dấu trong từ điển về dạng mã Telex thô (ví dụ: `chọn` -> `chojn` hoặc `chonj`, `chó` -> `chos`, `chôn` -> `choon`).
2. **So khớp tiền tố (Prefix Matching):** Lọc các từ trong từ điển mà dạng Telex hoặc dạng không dấu của chúng bắt đầu bằng chuỗi Telex/không dấu của input từ người dùng.
3. **Tính điểm xếp hạng (Ranking & Scoring):**
   - **Khoảng cách gõ phím (Telex distance):** Tính bằng `len(word_telex) - len(input_telex)`. Khoảng cách này càng nhỏ (tức số phím cần gõ thêm ít nhất) thì từ đó càng được ưu tiên đứng trước.
   - **Hình phạt phím phụ (Penalty):** Nếu từ gợi ý chứa phím thay đổi gốc nguyên âm (như phím `w` để tạo `ơ, ư, ă`) nhưng input chưa có phím `w`, ta sẽ cộng thêm điểm phạt (penalty = +4) để đẩy các từ này xuống phía sau. Điều này giúp gõ `cho` sẽ ưu tiên đoán `chó`, `chọn`, `chôn` (gốc nguyên âm `o/ô`) đứng trước các từ `chơi`, `chợ` (gốc nguyên âm `ơ` dùng phím `w`).
   - **Tần suất (Frequency):** Đối với các từ có cùng điểm khoảng cách gõ phím, từ nào có tần suất lớn hơn trong từ điển nhị phân sẽ được xếp lên trước.

---

## 4. Hướng Dẫn Sử Dụng Bộ Script Python Đi Kèm

Bộ script Python được xây dựng để bạn chạy kiểm thử trực tiếp trên terminal Windows:

1.  **[dict_reader.py](file:///f:/Code/modapk/vnkey/dict_vi/dict_reader.py):** Chứa toàn bộ logic đọc file nhị phân, giải mã cấu trúc Trie và tải dữ liệu từ điển.
2.  **[suggest_engine.py](file:///f:/Code/modapk/vnkey/dict_vi/suggest_engine.py):** Chứa logic Telex hóa, tính khoảng cách gõ phím, phạt phím phụ và xếp hạng từ gợi ý.
3.  **[interactive_cli.py](file:///f:/Code/modapk/vnkey/dict_vi/interactive_cli.py):** Chương trình chính chạy giao diện dòng lệnh tương tác thời gian thực 2 dòng (Gợi ý và Input).

### Lệnh Chạy Chương Trình:
Mở Terminal (Command Prompt hoặc PowerShell) trên Windows và chạy lệnh:

```powershell
python f:\Code\modapk\vnkey\dict_vi\interactive_cli.py
```

*Ứng dụng sẽ tự động giải mã file `main_vi.dict`, nạp 31.813 từ tiếng Việt vào bộ nhớ và sẵn sàng cho bạn gõ nhập liệu tương tác trực tiếp.*
