# TẦNG DƯỚI — Tổng quan bốn tầng

*Thiết kế đã bàn với người dùng ngày 09/11/2026. Tầng 1 đã dựng; tầng 2–4 mới là đề xuất, sẽ bàn chi tiết từng tầng rồi mới dựng (chốt tầng nào dựng tầng đó).*

**Ý đồ:** bước chuyển giữa Ký Ức Của Nam và The Womb, tước dần ký ức của Nam từng lớp. Mỗi tầng là một save cũ hơn, những save này sẽ gặp lại trên các cánh cửa ở chương II của The Womb. Không có quái đuổi theo, không có cái chết (ngoại lệ do người dùng chốt 10/11/2026: Ophanim ở tầng 2 chỉ nhìn rồi núp, không gây hại); thua chỉ bị đưa lại chỗ cũ. The Wanderer không xuất hiện ở đây: Dreamcore mạnh nhất khi vắng người.

**Nền chung (đã dựng):** Đường Ống gạch men, nước đục, đèn huỳnh quang, đèn chập chờn và đèn hỏng, bọ quanh đèn, nước rỉ, glitch và Khối Mã 5–10% bề mặt, Khối Chưa Tải (càng xuống càng nhiều). Mỗi tầng có luật chơi riêng; cuối tầng là chiếc giường đỏ với bảng *"đừng bấm ngủ"*, chữ càng xuống càng non nớt. Mỗi tầng trả lại một mảnh nhật ký.

## Tầng 1. Nhà Nam 05/2018: "Hoa Quay Lưng" (ĐÃ DỰNG)
Xem `tang_duoi_01_nha_nam_2018.md`. Bản dựng khác đề xuất ở hai điểm: đường vòng là ống thật (không dịch chuyển), và căn hầm đầu tầng có ổ khoá 0518 thay cho mảng tường bong.

## Tầng 2. Sáng tạo-KHÔNG XÓA 08/2016: "Lối đi nháp"
Bể nước sâu không thấy đáy; lối qua làm bằng **Khối Nháp**, block chỉ thấy qua máy quay. Phải vừa đi vừa ngắm máy quay, tầm nhìn hẹp vì zoom. Rơi thì nổi lên ở đầu bể. Càng về cuối các bảng *"KHÔNG XÓA"* càng nhiều và viết càng gấp. Nhà ở tầng này: đồ đạc xếp thành hàng thử nghiệm, nhiều phòng chưa xây xong. Nếu phải cắt bớt tầng thì cắt tầng này trước.

## Tầng 3. Thế giới của em 04/2012: "Trốn tìm" (tầng cảm xúc chính, phải giữ)
Nhà do đứa em xây: vụng về, bảng hiệu sai chính tả, hai cái giường, một cái vẫn được dọn sẵn. Đồ đạc to gấp đôi, người chơi thấy mình như đứa trẻ. Đứa em đếm từ 1 đến 100 trong chat; phải tìm đủ ba món đồ nó giấu theo mấy tấm bảng. Hết giờ: *"anh chậm quá. lại nha"* và đếm lại. Tìm đủ thì phòng ngủ mở. Qua máy quay, kẻ ngồi trốn trong góc mặc skin của chính người chơi. Gài cho câu của `<System>` ở Video 9: người em trai đang đợi ở nhà cũng là cậu.

## Tầng 4. Alpha 1.2.6 2010: "Đêm đầu tiên" (phải giữ)
Mất sạch túi đồ và máy quay (đây là ngoại lệ duy nhất của quy tắc máy quay không vứt được; cần thêm vào `CamcorderKeepSystem`). Một mảnh đất nhỏ, Khối Chưa Tải ăn dần từ ngoài vào, vài cái cây và đúng ba con cừu quay mặt về phía người chơi. Bản Alpha chưa có giường (Beta 1.3 mới có): người chơi tự chặt cây, lấy len, tự chế giường, vật phẩm hiện tooltip *"chưa có trong phiên bản này"*. Tự tay làm ra thứ bị cấm từ đầu, ngủ, rồi tỉnh dậy ở Bến Đò.

## Khi dựng tầng sau
Giường cuối tầng 1 hiện dẫn thẳng tới Bến Đò (`DuongOngRuntime.tickSleep`); đổi sang đầu tầng 2 khi có.
