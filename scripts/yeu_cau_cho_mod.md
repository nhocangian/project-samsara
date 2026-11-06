# YÊU CẦU CHO PHIÊN LÀM MOD

Các thay đổi đã được người dùng duyệt, cần dựng trong game. Làm xong thì cập nhật `hien_trang_trong_mod.md`.

---

## 1. Đoạn thoại Video 9: `<System>` gọi Nam là "cậu"

- **Trạng thái:** ĐÃ LÀM (06/11/2026). 17 dòng `<System>` trong `vi_vn.json` đã đổi; mốc ×45 là bước riêng, không dò theo câu chữ.

- **Ưu tiên:** Vừa
- **Duyệt:** 06/11/2026
- **Mô tả:** Trong đoạn thoại tự gõ trong ô chat ở đoạn kết (quanh Quả Trứng), mọi chữ **"bạn" / "Bạn"** trong lời `<System>` đổi thành **"cậu" / "Cậu"**. Cho khớp với mọi câu `<System>` khác trong game (máy quay, Chu Kỳ #2).
- **Không đổi gì khác:** câu chữ, trình tự, nhịp, các mốc hiệu ứng giữ nguyên. Lời của Nam giữ nguyên, kể cả câu *"Ý bạn là sao?"* (Nam nói với `<System>`).
- **Chữ nguyên văn:** mục "Nguyên văn đoạn thoại" trong Video 9 của `master_production_document_project_samsara_revision.md`.
- **Lưu ý mốc ×45:** câu mốc *"Họ cũng chính là bạn."* giờ là **"Họ cũng chính là cậu."** Nếu code dò mốc theo nội dung câu thì sửa cả chỗ dò.
- **Thứ cần thay trong `hien_trang_trong_mod.md`:** mục 5, dòng nhắc mốc *"Họ cũng chính là bạn."*

---

## 2–5. Bốn việc từ `de_xuat/2026-11-06_cai_thien_master.md` mục D

- **Trạng thái:** ĐÃ LÀM (07/11/2026). Người dùng duyệt cả 4 ngày 07/11/2026.
- **2. Trang nhật ký [ĐÊM ĐẦU TIÊN]** viết lại: *"Mắt anh nhìn thì mặt trời vẫn bình thường. Nhưng ảnh chụp màn hình thì có một chấm đen ở chính giữa."*
- **3. Ảnh chụp F2** từ Stage 1 ghi được mặt trời hố đen; mắt vẫn thấy vanilla. Lệnh `/samsara test screenshot_sun`.
- **4. Chu Kỳ ≥2:** lần đầu Nam_1998 vào game gõ 3 dòng Video 3 (`em` / đường dẫn OBS / `xem từ tệp đầu tiên. đừng tua`) rồi rời đi. Lệnh `/samsara test ui archive`.
- **5. Màn NO SIGNAL:** `2021-03-14 03-29-51.mkv   00:03:42:09` thay cho `Nam_1998_LOG_09.mp4`.
