Đã chốt các lựa chọn cho cấu trúc mới:

- Video 3 – A: Bóng người đứng ở khoảng cách xa, chỉ quan sát người chơi.
- Ba băng ký ức – B: Không ép cả ba băng phải xuất hiện trong Video 4–6. Chỉ đưa vào khi chúng có vai trò tự nhiên trong diễn biến.
- Chu Kỳ #2 – B: Để phần Chu Kỳ #2 xuất hiện sau outro Channy, như một đoạn hậu kết.

Từ đây, tôi sẽ xem cấu trúc V1.0 là cấu trúc đã được duyệt để triển khai. Những nội dung mới trong bản dưới đây là đề xuất dàn dựng; các câu chữ đã có trong tài liệu nguồn sẽ được giữ nguyên.

---

# PHẦN NỀN — TRIẾT LÝ & CƠ CHẾ

*(Nguyên văn từ tài liệu gốc, mục I–II.)*

## I. TỔNG QUAN TRIẾT LÝ & THẾ GIỚI (CORE LORE)

### 1. Triết Lý Cốt Lõi: The Egg & Luân Hồi (Samsara)
* **Thế giới Minecraft là "Vỏ Trứng" (The Incubator):** Vũ trụ Overworld không phải là thực tại cố định mà là lồng ấp tạm thời nuôi dưỡng một linh hồn. Siêu Hố Đen **Gargantua (`The Womb`)** ngự trị vòm trời là "người mẹ" thu gom ký ức và mở lồng ấp khi chu kỳ kết thúc.
* **Tiểu Tử qua Giấc Ngủ:** Mỗi lần bấm `SLEEP`, nhân vật "chết đi một phần" để nạp dữ liệu về *The Womb*. Mỗi giấc ngủ đập nứt thêm một tầng vỏ trứng (tăng biến đếm ẩn `sleep_count`).
* **Đồng Nhất Thực Thể ("Tất cả là Một"):** Mọi quái vật Phantom, bóng ma qua camera (*The Wanderer*), phế tích cũ, và cả người chơi khác... đều là **chính bạn (Channy / Khán giả)** ở các kiếp sống / Save File khác nhau.

### 2. Hình Thức Thể Hiện & Bản Game
* **Hình thức:** Found Footage / Video Analysis (Nhập vai **Investigator Channy** mổ xẻ một kênh YouTube ẩn danh).
* **Bản Build Nguyền Rủa:** `Minecraft 1.20.1-SAMSARA-REL` — phiên bản Dev Build / Lost Build không tồn tại trên dữ liệu chính thức của Mojang hay Spigot.
* **Cấu trúc ống kính:** 
  * **Video 1 – 3:** Góc nhìn của **Em Nam** khi vào thăm Save World cũ của anh mình và đọc cuốn nhật ký *Palimpsest Journal*.
  * **Video 4 – 9:** Góc nhìn trực tiếp từ các tệp ghi hình **OBS** gốc của **Nam (`Nam_1998`)** để lại.

---

## II. CƠ CHẾ TRONG GAME & HỆ THỐNG BIẾN CHUYỂN

### 1. Chiếc Máy Quay VHS (`Analog Camcorder`)
* **Bản chất:** Là **Cơ Chế Quan Sát** do **Thực Thể Mẹ (`<System>`)** tự gài vào Hotbar khi chỉ số hoảng loạn / giấc ngủ kích hoạt.
* **Tác dụng:** Mắt thường của nhân vật chỉ nhìn thấy hình ảnh Vanilla 16x16 tĩnh lặng. Khi ngắm qua ống kính máy quay (chế độ IR Green Mode), tần số thực tại thật bộc lộ: Hố Đen Gargantua, vết nứt Fractal, và Username thực của các bóng ma.

### 2. Bẫy Sinh Tồn: Phantom vs Giấc Ngủ
* **Phantom:** Không phải quái vật ăn thịt mà là tàn dư linh hồn kiếp cũ bị phân rã, lao xuống tra tấn để **ép người chơi kiệt sức nhắm mắt ngủ**.
* **Thế cờ Lôi Xé:** 
  * *Không ngủ:* Bầy Phantom xuất hiện ngày càng đông, xé rách không gian, phát âm thanh tua ngược.
  * *Bấm ngủ:* Thoát Phantom lập tức nhưng `sleep_count += 1`, gia tốc tận thế. Đêm ngủ thứ 7 (`sleep_count == 7`) làm Hố Đen nuốt chửng vòm trời.

### 3. Tiến Trình Bầu Trời & Chiều Không Gian

| Stage | Visual Mặt Trời / Bầu Trời (Shader 3D) | Dị Tượng Môi Trường & Gameplay |
| :--- | :--- | :--- |
| **Stage 1** | Mặt Trời nứt chính tâm, lộ lõi đen `#050208`. | Tiếng nhạc Vanilla bị giật (stutter). Cừu đứng quay mặt về 1 tọa độ. |
| **Stage 2** | Hố đen trực diện (*Face-on*), vành quang tử vàng kim. | Nước đổi màu xám bạc. Cây trồng chỉ lớn dưới ánh hồng ngoại IR Mode. |
| **Stage 3** | Bão Plasma xoáy đỏ cam xé toạc vòm trời. | Trọng lực bẻ cong: Cát/sỏi rơi ngược lên trời. Kim La Bàn quay tít. |
| **Stage 4** | Hố Đen Gargantua 3D Raymarching hổ phách. | Overworld sụp đổ toàn bộ. Khối block bị hút đứt đoạn lên hư không. |

---

# VIDEO 1 — `Visiting_Brother_World.mp4`

KỊCH BẢN CHI TIẾT · BẢN PHÁT TRIỂN 1.0

Vai trò của video: Giới thiệu thế giới của Nam sau khi anh biến mất, đặt người xem vào góc nhìn của người em trai và gieo những dấu hiệu đầu tiên rằng thế giới này đã trải qua một biến cố bất thường.

Thời lượng đề xuất: 7–9 phút.

Góc nhìn: Footage Minecraft của người em trai. Không có lời dẫn của Channy trong phần footage gốc.

Trạng thái thế giới: Stage 4 — `sleep_count ≥ 7`.

Điều này rất quan trọng: người em không bước vào một thế giới bình thường rồi từ từ chứng kiến nó biến đổi. Thế giới đã ở trong tình trạng bất thường ngay từ đầu. Cậu chỉ chưa hiểu chuyện gì đã xảy ra.

## CẢNH 01 — MỘT THẾ GIỚI ĐÃ BỊ BỎ LẠI

00:00 – 00:50

Thiết lập

Hình ảnh và hành động

1. Video bắt đầu bằng màn hình desktop, sau đó Minecraft được mở lên.
2. Không cần logo, intro hay nhạc nền. Chỉ có tiếng máy tính và âm thanh thao tác chuột rất nhỏ.
3. Người em mở mục Singleplayer.
4. Danh sách thế giới hiện ra. Cậu dừng lại ở save của anh trai.
5. Không thêm hiệu ứng glitch ngay lúc này. Để giao diện Minecraft trông hoàn toàn bình thường.

Tên world: Giữ nguyên tên save gốc nếu tài liệu B đã xác định. Không tự đặt tên mới.

6. Người em chọn world và nhấn Play.
7. Màn hình tải thế giới xuất hiện. Trong vài giây, âm thanh máy tính gần như biến mất.

Âm thanh

- Tiếng click chuột.
- Tiếng quạt máy tính hoặc noise nền rất nhẹ.
- Nhạc Minecraft mặc định có thể bắt đầu rất nhỏ khi world tải xong, nhưng không cần sử dụng một bản nhạc kinh dị riêng.

Mục đích

Mở đầu bằng một hành động rất đời thường: vào thế giới của người thân đã mất tích. Không cố gắng báo trước rằng đây là một câu chuyện kinh dị.

## CẢNH 02 — NƠI MỌI THỨ VẪN CÒN NGUYÊN

00:50 – 02:00

Thiết lập không gian

Hình ảnh và hành động

1. Người em xuất hiện trong Overworld, gần khu vực spawn.
2. Máy quay giữ nguyên góc nhìn thứ nhất. Không cắt cảnh ngay khi vừa vào game.
3. Cậu đứng yên vài giây để quan sát.
4. Trước mặt là căn nhà gỗ của Nam. Các vật dụng vẫn ở vị trí cũ, tạo cảm giác người chủ chỉ vừa rời đi.
5. Người em đi quanh nhà, nhìn qua cửa sổ và kiểm tra những khu vực quen thuộc.
6. Không cần thêm dấu vết đổ nát hay máu me. Căn nhà vẫn nguyên vẹn.

Chi tiết bất thường đầu tiên

Trong lúc người em đi quanh khu vực, camera bắt được một vài block nhỏ đang bị hút lên phía trên.

Đây là hiệu ứng Stage 4 đã được xác định trong tài liệu A.

Cách thể hiện:

- Không cho block bay lên đồng loạt.
- Một vài block ở xa từ từ rời khỏi mặt đất.
- Chúng lơ lửng rồi biến mất khỏi khung hình.
- Người em có thể quay camera theo dõi, nhưng chưa cần phản ứng mạnh.

Âm thanh

- Âm thanh môi trường Minecraft bình thường.
- Tiếng bước chân trên cỏ.
- Tiếng cửa gỗ và các âm thanh tương tác thông thường.
- Khi block bắt đầu bay lên, có thể giảm nhẹ âm thanh môi trường trong thời gian ngắn.

Không sử dụng tiếng gầm, tiếng hét hoặc âm thanh báo động. Hiệu ứng này nên tạo cảm giác sai lệch về vật lý hơn là một cuộc tấn công.

Mục đích

Cho người xem biết thế giới có điều gì đó không ổn, nhưng chưa tiết lộ nguyên nhân. Người em vẫn đang cố gắng khám phá save của anh trai như một người chơi bình thường.

## CẢNH 03 — BIA MỘ Ở SPAWN

02:00 – 03:15

Vật chứng quan trọng

Hình ảnh và hành động

1. Khi đi khỏi căn nhà, người em phát hiện một công trình nhỏ gần khu vực spawn.
2. Đây là Bia Mộ (Spawn Cairn).
3. Camera không lập tức nhìn thẳng vào bảng. Người em đi vòng quanh công trình trước.
4. Cậu tiến lại gần bảng và đọc nội dung.

Nội dung bảng

SIGN · BIA MỘ SPAWN

## [Kẻ Đến Sau]

## Cậu đến muộn rồi

## Chỗ này...

## vừa có người nằm

Lưu ý kỹ thuật: Nội dung trên là bốn dòng bảng đã được chốt. Cần kiểm tra giới hạn ký tự thực tế của Minecraft và cách hiển thị tiếng Việt trong mod; không tự ý đổi câu chữ để vừa bảng.

Dàn dựng

- Đọc từng dòng bằng cách di chuyển camera từ trên xuống.
- Dừng lại lâu hơn một chút ở dòng cuối.
- Không zoom bằng hiệu ứng hậu kỳ quá mạnh.
- Sau khi đọc xong, người em lùi lại một bước và quan sát toàn bộ bia mộ.

Có thể để camera đứng yên khoảng 3–4 giây.

Âm thanh

- Không có nhạc nền.
- Chỉ còn tiếng gió hoặc ambience môi trường.
- Có thể giữ tiếng bước chân cuối cùng rồi để khoảng lặng.

Mục đích

Bia mộ là vật chứng đầu tiên cho thấy có người đã nằm ở đây trước khi người em đến.

Câu `[Kẻ Đến Sau]` cũng tạo một câu hỏi quan trọng: nếu người em là người đến sau, vậy ai đã đến trước?

## CẢNH 04 — CHIẾC RƯƠNG CẠNH BIA MỘ

03:15 – 04:30

Phát hiện nhật ký

Hình ảnh và hành động

1. Người em nhìn thấy chiếc rương đặt cạnh bia mộ.
2. Cậu tiến lại gần và mở rương.
3. Bên trong có hai vật phẩm:
   - Nhật Ký Nam — `Palimpsest Journal`.
   - Hoa khô.
4. Camera giữ lại khung hình rương đang mở đủ lâu để người xem nhận diện cả hai vật phẩm.
5. Người em lấy cuốn nhật ký ra.

Chi tiết cần nhấn mạnh: Hoa khô

Hoa khô không cần có hiệu ứng đặc biệt. Nó nên là một vật phẩm bình thường, được đặt cạnh cuốn nhật ký.

Không giải thích hoa khô có ý nghĩa gì ở thời điểm này. Để nó trở thành một chi tiết mà khán giả có thể nhớ lại khi biết thêm về căn nhà và ký ức của Nam.

Âm thanh

- Tiếng mở rương Minecraft.
- Tiếng lấy vật phẩm.
- Không có âm thanh kỳ lạ khi nhặt nhật ký.

Mục đích

Đây là bước chuyển từ khám phá địa điểm sang khám phá câu chuyện.

Bia mộ nói rằng có người từng nằm ở đây. Cuốn nhật ký là vật chứng đầu tiên có thể giúp người em hiểu chuyện gì đã xảy ra.

## CẢNH 05 — LỜI CẢNH BÁO

04:30 – 06:20

Thông tin cốt lõi

Hình ảnh và hành động

1. Người em mở `Palimpsest Journal`.
2. Camera dừng ở trang đầu tiên.
3. Chưa đọc toàn bộ nhật ký. Video 2 sẽ đảm nhiệm phần khám phá sâu hơn.
4. Ở lớp mực mới, người em nhìn thấy lời nhắn dành cho mình.

Nội dung nhật ký

PALIMPSEST JOURNAL · LỚP MỰC MỚI

# Nếu một ngày em mở file này ra, đừng bấm ngủ.

— anh Nam

Dàn dựng

- Không thêm hiệu ứng chữ xuất hiện từng ký tự.
- Không thêm tiếng thì thầm đọc câu này.
- Giữ nguyên văn bản trên trang sách.
- Sau khi đọc xong, để người em không thao tác trong vài giây.

Sau đó, cậu đóng sách.

Điểm quan trọng

Câu cảnh báo này không được giải thích ngay. Đặc biệt, không cho người em thử ngủ để kiểm tra lời nhắn.

Lúc này, khán giả chỉ biết rằng Nam đã để lại một lời cảnh báo rất cụ thể cho em trai mình.

Âm thanh

- Tiếng lật trang sách.
- Tiếng đóng sách.
- Sau khi đóng sách, để lại một khoảng lặng ngắn.

Mục đích

Đây là câu móc chính của Video 1. Nó biến việc khám phá một thế giới Minecraft thành một câu chuyện có mục đích cá nhân: Nam đã biết điều gì đó và muốn em trai tránh nó.

## CẢNH 06 — THẾ GIỚI KHÔNG MUỐN ĐỨNG YÊN

06:20 – 07:40

Kết thúc video

Hình ảnh và hành động

1. Người em đóng nhật ký và rời khỏi giao diện sách.
2. Camera trở lại góc nhìn thứ nhất.
3. Cậu nhìn về phía căn nhà hoặc khu vực spawn.
4. Những block nhỏ tiếp tục bị hút lên trời.
5. Lần này, một block xuất hiện gần hơn trong khung hình.
6. Người em hướng camera lên để theo dõi nó.
7. Không có hố đen xuất hiện bằng mắt thường. Bầu trời vẫn hiển thị mặt trời Minecraft bình thường.

Quy tắc hình ảnh bắt buộc

- Không để người em nhìn thấy Gargantua bằng mắt thường.
- Không thêm một chấm đen bất thường trên mặt trời ở cảnh này.
- Không để mặt trời biến dạng.
- Không đưa hình ảnh The Womb vào Video 1.
- Hố đen chỉ có thể được phát hiện qua máy quay, theo luật đã chốt.

Kết cảnh

Camera giữ trên bầu trời thêm vài giây.

Người em không nói gì. Sau đó video kết thúc đột ngột.

Âm thanh

- Tiếng môi trường giảm dần.
- Không dùng nhạc cao trào.
- Có thể để một tiếng ambience Minecraft kéo dài qua vài khung hình cuối rồi cắt thẳng sang đen.

END OF FOOTAGE

# Tổng hợp nhịp độ Video 1

| Cảnh | Nội dung                      | Vai trò                  |
| ---- | ----------------------------- | ------------------------ |
| 01   | Mở save của Nam               | Thiết lập góc nhìn       |
| 02   | Khám phá căn nhà và block bay | Giới thiệu Stage 4       |
| 03   | Phát hiện Bia Mộ              | Gieo bí ẩn               |
| 04   | Mở rương cạnh bia mộ          | Giới thiệu vật chứng     |
| 05   | Đọc lời nhắn của Nam          | Đặt câu hỏi trung tâm    |
| 06   | Quan sát thế giới bất thường  | Kết bằng cảm giác bất an |

## Những điều tôi chủ động chưa đưa vào Video 1

1. Chưa đọc lớp mực cũ của nhật ký. Video 2 sẽ có không gian để khám phá `Chu Kỳ #0: Vô Danh`, các ghi chép cũ và những sự kiện liên quan.
2. Chưa cho máy quay ô số 3 hoạt động. Người em chỉ cần phát hiện và đọc nhật ký ở đây. Việc khám phá những gì máy quay có thể nhìn thấy nên được dành cho Video 3.
3. Chưa xuất hiện Nam_1998 trong chat. Chi tiết này sẽ có tác dụng mạnh hơn khi người em đã bắt đầu nghi ngờ có một thứ gì đó đang quan sát mình.
4. Chưa tiết lộ Gargantua. Video 1 phải tuân thủ luật mắt thường và máy quay; không để khán giả hiểu sai cơ chế ngay từ đầu.

## Đánh giá của script doctor

Tôi muốn giữ Video 1 tương đối tĩnh. Sức mạnh của nó nằm ở sự tương phản giữa hai thứ: một thế giới Minecraft vẫn có căn nhà, rương, sách và những vật dụng quen thuộc; nhưng người chủ của thế giới ấy đã để lại một bia mộ cùng lời cảnh báo không được ngủ.

Đặc biệt, câu “Nếu một ngày em mở file này ra, đừng bấm ngủ.” nên là điểm nặng nhất của video. Đừng để các hiệu ứng kinh dị lấn át nó.

Có một điểm cần xác minh trước khi triển khai mod: cách lưu trữ và hiển thị `Palimpsest Journal`. Vì đây là nhật ký hai lớp mực, cần bảo đảm người chơi có thể đọc lớp mực mới ở Video 1 mà không vô tình nhìn thấy toàn bộ lớp cũ trước Video 2.

Bước tiếp theo hợp lý là phát triển Video 2 – `The_Found_Journal.mp4`, nơi người em bắt đầu khám phá lớp mực cũ và nhận ra thế giới này đã có một lịch sử dài hơn những gì cậu tưởng tượng.

# VIDEO 2 — `The_Found_Journal.mp4`

KỊCH BẢN CHI TIẾT · BẢN PHÁT TRIỂN 1.0

Vai trò của video: Tiếp tục footage của người em, mở rộng bí ẩn từ lời cảnh báo của Nam sang lịch sử của thế giới. Khán giả bắt đầu nhận ra rằng những điều bất thường không chỉ vừa mới xảy ra, và Nam có thể đã trải qua nhiều lần biến cố trước khi biến mất.

Thời lượng đề xuất: 8–10 phút.

Góc nhìn: Người em trai của Nam, tiếp nối trực tiếp Video 1.

Trạng thái thế giới: Stage 4, `sleep_count ≥ 7`.

Nguyên tắc: Không có lần ngủ mới trong Video 2. Người em không cần phải ngủ để khám phá nhật ký.

## CẢNH 01 — QUAY LẠI CUỐN NHẬT KÝ

00:00 – 01:10

Tiếp nối Video 1

Hình ảnh và hành động

1. Video mở đầu bằng một đoạn footage ngắn bên trong căn nhà gỗ.
2. Người em đứng trước chiếc bàn hoặc khu vực đặt đồ.
3. Cậu mở lại `Palimpsest Journal`.
4. Lần này, camera không tập trung vào lời cảnh báo ở lớp mực mới nữa.
5. Người em bắt đầu lật các trang tiếp theo, tìm hiểu những ghi chép cũ.

Không cần chuyển cảnh hay dựng lại căn nhà. Đây là cùng một buổi khám phá trong save world của Nam.

Âm thanh

- Âm thanh trong nhà yên tĩnh.
- Tiếng mở sách và lật trang.
- Không có nhạc nền ở đoạn đầu.

Mục đích

Video 1 đặt ra câu hỏi: Tại sao Nam lại cảnh báo em trai đừng ngủ?

Video 2 bắt đầu trả lời bằng cách cho thấy Nam không phải người đầu tiên gặp những chuyện kỳ lạ trong thế giới này.

## CẢNH 02 — CHU KỲ #0: VÔ DANH

01:10 – 03:00

Khám phá lịch sử

Hình ảnh và hành động

1. Người em lật đến phần nhật ký cũ.
2. Camera dừng ở dòng tiêu đề đã bị cạo xước.

PALIMPSEST JOURNAL · LỚP MỰC CŨ

# Chu Kỳ #0: Vô Danh

Một tiêu đề cũ đã bị cạo đi, chỉ còn lại những dấu vết mực và vết xước trên giấy.

3. Người em kéo xuống và đọc những ghi chép theo thứ tự ngày tháng:
   - Ngày 4.
   - Ngày 11.
   - Ngày 19.
   - Ngày 27.
   - Ngày 33.
4. Mỗi ghi chép được giữ trên màn hình đủ lâu để người xem đọc.
5. Không cần cho toàn bộ nội dung các trang xuất hiện cùng lúc. Dùng thao tác lật trang tự nhiên.

Dàn dựng chữ

Các mốc ngày tháng trên là thông tin đã có trong tài liệu A. Nội dung cụ thể của từng ngày cần lấy nguyên văn từ bản nhật ký gốc trong tài liệu B, không tự viết thêm lời ghi chép mới vào đây.

Đặc biệt, cần giữ cảm giác nhật ký có hai lớp lịch sử:

- Lớp mực cũ thuộc về `Chu Kỳ #0: Vô Danh`.
- Lớp mực mới là những ghi chép của Nam dành cho em trai.

Hai lớp không nên bị trình bày như một cuốn nhật ký được viết liên tục bởi cùng một người.

Âm thanh

- Tiếng lật trang sách.
- Tiếng giấy nhẹ.
- Có thể thêm một lớp ambience rất nhỏ, nhưng không nên có âm thanh ma quái mỗi lần chuyển trang.

Mục đích

Đưa khái niệm chu kỳ vào câu chuyện mà chưa giải thích bản chất của nó.

Người xem có thể tự hỏi: Ai đã viết những dòng này? Vì sao tiêu đề lại là Chu Kỳ #0? Và Nam đã biết về những ghi chép này từ khi nào?

## CẢNH 03 — NHỮNG DÒNG CHỮ BỊ CẠO XÓA

03:00 – 04:10

Chi tiết bí ẩn

Hình ảnh và hành động

1. Người em quay lại trang có tiêu đề `Chu Kỳ #0: Vô Danh`.
2. Camera dừng ở vùng giấy bị cạo.
3. Người em di chuyển chuột hoặc góc nhìn để quan sát kỹ hơn.
4. Những vết xước không cho phép đọc được tiêu đề ban đầu.

Dàn dựng

Không nên làm chữ cũ hiện ra bằng hiệu ứng phát sáng hoặc tự động giải mã. Sự thiếu hụt thông tin chính là điều khiến chi tiết này có giá trị.

Đây cũng là nơi có thể cho người xem nhận ra rằng cuốn nhật ký không chỉ là nơi ghi lại sự kiện. Nó đã từng bị sửa đổi, xóa bỏ hoặc viết chồng lên.

Âm thanh

Giữ im lặng gần như hoàn toàn.

Mục đích

Tạo một câu hỏi chưa có lời giải: người viết đã cố xóa điều gì?

Không giải đáp ngay trong Video 2.

## CẢNH 04 — MẶT TRỜI CÓ MỘT CHẤM ĐEN

04:10 – 05:30

Hiện tượng Stage 4

Hình ảnh và hành động

1. Người em đóng nhật ký.
2. Cậu rời khỏi căn nhà và đi ra ngoài.
3. Camera hướng lên bầu trời.
4. Mặt trời Minecraft hiện ra bình thường.
5. Người em đứng nhìn một lúc, sau đó hạ camera xuống.

Quy tắc bắt buộc

Ở cảnh này, mặt trời phải hoàn toàn bình thường đối với mắt thường.

Không có chấm đen hiện ra trực tiếp trong hình ảnh gameplay. Không có biến dạng, không có vòng xoáy hay một vật thể đen che khuất mặt trời.

Chấm đen thuộc về góc nhìn qua máy quay. Vì vậy, không được để người em trực tiếp phát hiện nó ở đây.

Đề xuất dựng hình

Có thể sử dụng một đoạn footage ngắn quay mặt trời bằng góc nhìn bình thường, sau đó cắt sang cảnh tiếp theo mà không giải thích gì.

Channy sẽ có cơ hội đối chiếu hình ảnh này với footage qua máy quay ở những video sau.

Âm thanh

- Tiếng môi trường ngoài trời.
- Tiếng bước chân.
- Không có âm thanh cảnh báo.

Mục đích

Đây là một chi tiết có giá trị khi xem lại toàn bộ chuỗi video: người xem đã nhìn thấy bầu trời bình thường, nhưng chưa biết rằng có một thứ khác đang tồn tại ngoài tầm nhìn của họ.

## CẢNH 05 — BẦY CỪU

05:30 – 07:00

Hiện tượng quen thuộc bị lệch

Hình ảnh và hành động

1. Người em đi ngang qua một khu vực cỏ gần Bia Mộ.
2. Cậu phát hiện một bầy cừu đang đứng thành cụm.
3. Các con cừu đều hướng đầu về phía Bia Mộ.
4. Chúng không di chuyển theo hành vi thông thường.
5. Người em tiến lại gần.
6. Camera lần lượt quan sát các con cừu.
7. Dù người chơi đến gần, bầy cừu vẫn hướng về cùng một vị trí.

Cách thể hiện

- Không để cừu quay đầu đồng loạt như một màn hù dọa.
- Không cho chúng lao đến người chơi.
- Không sử dụng âm thanh cừu bất thường.
- Chỉ cần hành vi đứng yên và cùng nhìn về một hướng.

Nếu muốn tăng cảm giác bất an, có thể để một con cừu ở rìa đàn hơi lệch góc nhìn so với những con còn lại, nhưng cuối cùng nó cũng hướng về Bia Mộ.

Đề xuất bổ sung về animation, không phải lore mới.

Âm thanh

- Tiếng cừu Minecraft rất nhỏ, có thể giữ nguyên âm thanh gốc.
- Không có nhạc nền.
- Khi camera tiến lại gần, âm thanh môi trường vẫn bình thường.

Mục đích

Bầy cừu là một chi tiết bình thường trong Minecraft, nhưng hành vi của chúng khiến khung cảnh trở nên không tự nhiên.

Điều quan trọng là chúng không nhìn người em. Chúng nhìn Bia Mộ.

## CẢNH 06 — LỜI NHẮN CUỐI CÙNG

07:00 – 08:30

Cliffhanger

Hình ảnh và hành động

1. Người em quay lại Bia Mộ.
2. Cậu có thể kiểm tra chiếc rương thêm một lần, nhưng không cần lấy thêm vật phẩm.
3. Camera hướng về phía cuốn nhật ký.
4. Người em quay lại trang cuối cùng của lớp mực cũ.

Nội dung đã chốt

PALIMPSEST JOURNAL · LỚP MỰC CŨ

Lời nhắn cuối:

“Lên thuyền. Theo dải đèn về phía bắc... ĐỪNG ĐỂ NÓ RỜI KHỎI ỐNG KÍNH.”

Dàn dựng

1. Hiển thị toàn bộ lời nhắn.
2. Camera dừng ở cụm từ `ĐỪNG ĐỂ NÓ RỜI KHỎI ỐNG KÍNH`.
3. Không thêm lời giải thích.
4. Người em đóng sách.
5. Cậu nhìn về phía xa, nơi có thể nhìn thấy một phần địa hình hoặc mặt nước nếu bố cục world cho phép.

Âm thanh

- Tiếng đóng sách.
- Một khoảng lặng.
- Có thể kết thúc bằng tiếng môi trường ngoài trời bình thường.

Không thêm tiếng thì thầm hoặc âm thanh báo hiệu có ai đó đang ở gần. Lời nhắn đã đủ tạo ra sự tò mò.

Mục đích

Đây là điểm kết quan trọng nhất của Video 2.

Người em hiện có hai mảnh thông tin:

- Nam từng cảnh báo không được ngủ.
- Một người viết nhật ký trước đó đã cảnh báo phải giữ một thứ gì đó trong ống kính.

Hai lời nhắn chưa được giải thích, nhưng chúng có thể liên quan đến cùng một hiện tượng.

# Tổng hợp nhịp độ Video 2

| Cảnh | Nội dung             | Chức năng                    |
| ---- | -------------------- | ---------------------------- |
| 01   | Mở lại nhật ký       | Nối tiếp Video 1             |
| 02   | Chu Kỳ #0            | Giới thiệu lịch sử trước Nam |
| 03   | Trang bị cạo xóa     | Gợi ý nhật ký có nhiều lớp   |
| 04   | Mặt trời bình thường | Tuân thủ luật quan sát       |
| 05   | Bầy cừu nhìn Bia Mộ  | Tạo bất an                   |
| 06   | Lời nhắn về ống kính | Dẫn sang Video 3             |

## Những điều cần giữ để Video 2 không phá logic

1\. Không cho người em ngủ.

Video 2 không cần có lần ngủ mới. Lời cảnh báo của Nam vẫn còn nguyên giá trị và người em chưa có lý do để bất chấp nó.

2\. Không để người em nhìn thấy Gargantua.

Mắt thường vẫn chỉ thấy mặt trời Minecraft bình thường. Việc phát hiện hố đen phải dành cho góc nhìn qua máy quay.

3\. Không đưa máy quay vào tay người em quá sớm.

Video 2 chỉ kết thúc bằng lời nhắn về ống kính. Video 3 mới là nơi người em bắt đầu hiểu tại sao phải giữ một thứ gì đó trong camera.

4\. Không tiết lộ toàn bộ nội dung nhật ký cũ.

Nếu các ghi chép ngày 4, 11, 19, 27 và 33 có thông tin cụ thể, hãy giữ nguyên chúng theo tài liệu B. Tôi không muốn tự tạo nội dung thay thế rồi vô tình làm thay đổi ý nghĩa của `Chu Kỳ #0`.

## Đánh giá và hướng chuyển tiếp

Tôi đề xuất Video 2 kết thúc bằng lời nhắn về ống kính, sau đó Video 3 mở bằng việc người em tìm cách hiểu câu nói ấy.

Đây là một chuyển tiếp khá tự nhiên:

- Video 1: Nam để lại lời cảnh báo về giấc ngủ.
- Video 2: Nhật ký cũ để lại lời cảnh báo về ống kính.
- Video 3: Người em phát hiện rằng camera có thể nhìn thấy những thứ mắt thường không nhìn thấy.

Điều này giúp máy quay không xuất hiện như một món đồ công nghệ ngẫu nhiên. Nó trở thành một vật chứng đã được báo trước từ hai video trước.

Một lưu ý về kịch bản gốc: Video 2 bản cũ có cảnh người em ngủ lần đầu và thấy mặt trời nứt. Cảnh đó không còn phù hợp với luật hiện tại, vì người em đang ở một world Stage 4 và mặt trời không thể bị nhìn thấy biến dạng bằng mắt thường. Tôi đã loại bỏ cảnh ấy khỏi cấu trúc mới, không thay bằng một lần ngủ khác.

Tiếp theo, Video 3 sẽ là bước chuyển từ khám phá nhật ký sang khám phá thứ đang quan sát người em.

# VIDEO 3 — `The_Unknown_Observer.mp4`

KỊCH BẢN CHI TIẾT · BẢN PHÁT TRIỂN 1.0

Vai trò của video: Người em bắt đầu làm theo lời nhắn trong nhật ký và phát hiện máy quay VHS. Qua ống kính, cậu nhìn thấy một thế giới hoàn toàn khác với những gì mắt thường quan sát được. Lần đầu tiên, sự hiện diện của một người khác trong world được xác nhận.

Thời lượng đề xuất: 9–11 phút.

Góc nhìn: Người em trai của Nam.

Trạng thái thế giới: Stage 4 — `sleep_count ≥ 7`.

Quyết định đã duyệt: Bóng người xuất hiện ở khoảng cách xa và chỉ đứng quan sát. Không tự di chuyển khi camera quay đi, không có màn rượt đuổi.

Chức năng kết thúc: Tạo cầu nối từ footage của người em sang những bản ghi OBS cũ của Nam trong Video 4.

## CẢNH 01 — ĐỪNG ĐỂ NÓ RỜI KHỎI ỐNG KÍNH

00:00 – 01:20

Nối tiếp Video 2

Hình ảnh và hành động

1. Video mở bằng footage tiếp nối cảnh cuối Video 2.
2. Người em vẫn đứng gần Bia Mộ.
3. Cậu nhìn về phía xa, nơi có thể thấy một phần địa hình và đường chân trời.
4. Camera dừng lại vài giây, sau đó quay về phía căn nhà.
5. Người em mở lại nhật ký, nhìn lần nữa vào lời nhắn cuối.

Nội dung giữ nguyên:

> Lên thuyền. Theo dải đèn về phía bắc... ĐỪNG ĐỂ NÓ RỜI KHỎI ỐNG KÍNH.

6. Người em đóng sách.
7. Cậu bắt đầu quay lại căn nhà để kiểm tra những đồ vật còn sót lại.

Âm thanh

- Tiếng môi trường Overworld.
- Tiếng bước chân.
- Tiếng đóng sách.
- Không có âm thanh bí ẩn.

Mục đích

Không cần để người em lập tức hiểu lời nhắn. Cậu chỉ đang cố tìm một chiếc máy quay hoặc một vật dụng có thể liên quan đến câu nói đó.

## CẢNH 02 — VẬT PHẨM TRONG Ô SỐ 3

01:20 – 02:50

Phát hiện máy quay

Hình ảnh và hành động

1. Người em trở về khu vực căn nhà.
2. Cậu mở inventory để kiểm tra những vật phẩm đang mang theo.
3. Một vật phẩm lạ xuất hiện trong ô số 3 của hotbar: máy quay VHS.
4. Người em dừng lại, chuyển sang ô số 3.
5. Cậu cầm máy quay trên tay.

Cơ chế đã chốt

Máy quay VHS tự được `<System>` cài vào ô số 3 khi người chơi thức trắng đến đêm thứ ba hoặc khi đạt Stage 2.

Trong trường hợp này, người em đang ở Stage 4 nên máy quay đã tồn tại trong world trước khi footage Video 3 bắt đầu.

Không cần tạo cảnh máy quay rơi từ trên trời, xuất hiện trong rương hoặc được một nhân vật trao cho người em.

Giao diện

Tên vật phẩm:

`VHS Camera`

Các thông tin về pin, hồng ngoại và chức năng camera sẽ chỉ hiển thị khi người chơi kích hoạt nó.

Âm thanh

- Tiếng mở inventory.
- Tiếng chuyển hotbar.
- Một âm thanh bật thiết bị rất nhẹ khi người em sử dụng máy quay.

Mục đích

Máy quay xuất hiện như một vật phẩm vốn đã thuộc về thế giới này, nhưng người em chưa biết tại sao nó lại nằm trong inventory.

## CẢNH 03 — CẬU KHÔNG CHỊU NGỦ. VẬY THÌ HÃY NHÌN ĐI.

02:50 – 04:20

Thông điệp của System

Hình ảnh và hành động

1. Người em kích hoạt máy quay.
2. Màn hình chuyển sang giao diện camera.
3. Một lớp nhiễu VHS nhẹ xuất hiện.
4. Các thông tin giao diện được hiển thị: pin, chế độ hồng ngoại và khung ngắm.
5. Sau vài giây, một dòng chữ xuất hiện.

CAMERA UI · SYSTEM MESSAGE

Cậu không chịu ngủ. Vậy thì hãy nhìn đi.

Lưu ý về lời thoại

Đây là một trong hai câu `<System>` đã có trong tài liệu A. Giữ nguyên văn, không thêm lời giải thích hoặc câu trả lời.

6. Người em hướng camera quanh căn nhà.
7. Khi chuyển sang chế độ hồng ngoại, các vật thể trong khung hình trở nên rõ nét hơn.
8. Camera từ từ hướng lên bầu trời.

Phát hiện đầu tiên

Qua màn hình máy quay, mặt trời bình thường biến mất khỏi cảm giác quen thuộc của khung cảnh. Thay vào đó, ở phía xa là một vùng tối khổng lồ trên bầu trời: Gargantua.

Không cần cho Gargantua xuất hiện bằng mắt thường. Khung hình camera là nơi duy nhất người xem được phép nhìn thấy nó.

Dàn dựng hình ảnh

- Không zoom quá nhanh.
- Không sử dụng cú giật camera.
- Để người em từ từ điều chỉnh góc nhìn, như thể đang cố xác định mình vừa nhìn thấy thứ gì.
- Giữ Gargantua trong khung hình đủ lâu để người xem nhận diện kích thước và vị trí của nó.

Âm thanh

- Tiếng rè VHS.
- Tiếng thiết bị hoạt động.
- Một lớp âm trầm rất nhẹ khi Gargantua xuất hiện.
- Không sử dụng tiếng hét hoặc âm thanh hù dọa đột ngột.

Mục đích

Đây là lần đầu tiên khán giả hiểu rằng máy quay không chỉ ghi lại hình ảnh. Nó cho phép người chơi nhìn thấy một tầng thực tại khác.

## CẢNH 04 — NHỮNG THỨ KHÔNG CÓ Ở ĐÓ

04:20 – 06:10

Khám phá camera

Hình ảnh và hành động

1. Người em hạ camera khỏi bầu trời.
2. Cậu hướng máy quay về phía căn nhà và khu vực xung quanh.
3. Một số chi tiết trong khung hình trở nên khác biệt so với góc nhìn thông thường.
4. Những bóng tối, hình dạng ở xa và các chi tiết môi trường mà mắt thường không chú ý đến giờ trở nên rõ ràng hơn.
5. Người em thử quay camera quanh khu vực spawn.

Những gì được phép xuất hiện

- Gargantua trong bầu trời.
- Các chi tiết môi trường qua hồng ngoại.
- Những dấu hiệu bất thường vốn thuộc Stage 4.
- Bóng người ở khoảng cách xa, nhưng chưa cần xuất hiện ngay trong cảnh này.

Không thêm một loạt thực thể mới chỉ để lấp đầy khung hình. Camera cần có cảm giác như một công cụ quan sát, không phải một chiếc kính nhìn thấy hàng chục con quái vật.

Âm thanh

- Tiếng rè camera thay đổi nhẹ theo góc quay.
- Âm thanh môi trường vẫn tồn tại bên dưới.
- Không thêm lời thì thầm mới.

Mục đích

Cho người xem thời gian hiểu cơ chế camera trước khi đưa vào hình ảnh quan trọng nhất của video: người quan sát.

## CẢNH 05 — NGƯỜI ĐỨNG Ở ĐẰNG XA

06:10 – 08:00

Cảnh kinh dị chính

Hình ảnh và hành động

1. Người em hướng camera ra xa khỏi căn nhà.
2. Trong một khoảng thời gian ngắn, khung hình chỉ có địa hình bình thường.
3. Sau đó, một bóng người xuất hiện ở rất xa.
4. Nó đứng trên một vị trí cao hơn mặt đất xung quanh.
5. Bóng người không di chuyển.
6. Người em giữ camera hướng về phía nó.

Thiết kế thực thể trong cảnh này

- Chỉ là một hình người tối ở khoảng cách xa.
- Không để người xem nhận diện rõ khuôn mặt.
- Không tạo hình dáng quái vật phức tạp.
- Không để thực thể lao tới hoặc biến mất bằng hiệu ứng.

Quy tắc chuyển động đã duyệt — Phương án A

Bóng người đứng yên và quan sát người chơi.

Không áp dụng cơ chế của The Wanderer vào bóng người này. The Wanderer thuộc The Womb và có luật chuyển động riêng. Hai hiện tượng không được nhập làm một nếu tài liệu A chưa xác nhận chúng là cùng một thực thể.

Dàn dựng

- Giữ camera hướng vào bóng người khoảng 5–7 giây.
- Người em có thể điều chỉnh góc nhìn một chút để kiểm tra xem đó có phải một cấu trúc trong địa hình hay không.
- Bóng người vẫn đứng nguyên vị trí.
- Người em hạ camera xuống.
- Khi nhìn lại bằng mắt thường, bóng người không còn nằm trong tầm nhìn dễ nhận biết.

Không cần xác nhận nó biến mất. Chỉ cần camera không còn hướng về nó.

Âm thanh

- Tiếng rè VHS.
- Không có tiếng bước chân từ phía bóng người.
- Không có âm thanh xuất hiện thực thể.
- Có thể để một khoảng lặng kéo dài.

Mục đích

Đây là lần đầu tiên người em có bằng chứng rằng có một thứ đang quan sát mình.

Điều đáng sợ không phải là nó đuổi theo cậu, mà là nó đã đứng đó từ trước.

## CẢNH 06 — `Nam_1998 joined the game`

08:00 – 09:40

Giao diện bị xâm nhập

Hình ảnh và hành động

1. Người em vẫn đang cầm máy quay.
2. Cậu quay lại phía căn nhà.
3. Một thông báo chat xuất hiện.

MINECRAFT CHAT

`Nam_1998 joined the game`

4. Người em dừng lại.
5. Không có nhân vật nào xuất hiện trong tầm nhìn.
6. Một tin nhắn tiếp theo hiện lên:

CHAT MESSAGE

em đang ở đâu

7. Người em quay camera quanh khu vực.
8. Không có ai ở gần.

Lưu ý

Hai dòng chat trên đã được xác định trong tài liệu A. Không thêm các tin nhắn khác như `anh vẫn ở đây mà` hoặc `sao em lại mở file này` nếu chưa có vị trí cụ thể trong kịch bản được duyệt.

Âm thanh

- Âm thanh thông báo chat Minecraft.
- Tiếng môi trường nền.
- Không có tiếng bước chân hoặc tiếng người nói.

Mục đích

Người xem có thể nhận ra cái tên `Nam_1998` trùng với người anh đã biến mất.

Nhưng vấn đề là: nếu Nam không còn ở đây, ai đang đăng nhập bằng tài khoản đó?

## CẢNH 07 — KHÔNG CÓ AI Ở ĐÓ

09:40 – 10:50

Kết thúc footage

Hình ảnh và hành động

1. Người em quay về phía căn nhà.
2. Camera đi chậm qua khu vực spawn.
3. Ở khoảng cách xa, bóng người ban nãy vẫn đứng ở đó.
4. Người em hướng máy quay về phía nó.
5. Không có chuyển động.
6. Camera giữ nguyên khung hình.
7. Một khoảng nhiễu VHS xuất hiện nhẹ.
8. Người em hạ máy quay xuống.

Không cho bóng người tiến gần hơn. Không có rượt đuổi. Không có cú jump scare.

Kết thúc

Camera hướng về căn nhà một lần cuối.

Người em mở inventory, nhìn vào vật phẩm VHS Camera, rồi đóng lại.

Footage kết thúc bằng một cú cắt đen.

END OF FOOTAGE

# Tổng hợp nhịp độ Video 3

| Cảnh | Nội dung             | Chức năng                  |
| ---- | -------------------- | -------------------------- |
| 01   | Đọc lại lời nhắn     | Nối tiếp Video 2           |
| 02   | Phát hiện VHS Camera | Giới thiệu công cụ         |
| 03   | Gargantua qua camera | Tiết lộ tầng thực tại khác |
| 04   | Quan sát môi trường  | Thiết lập luật camera      |
| 05   | Bóng người ở xa      | Xác nhận có kẻ quan sát    |
| 06   | Nam_1998 đăng nhập   | Đặt câu hỏi về danh tính   |
| 07   | Bóng người vẫn ở đó  | Kết thúc bằng bất an       |

## Chuyển tiếp sang Video 4

Đây là điểm quan trọng trong cấu trúc toàn bộ Project Samsara.

Video 1–3 là footage của người em sau khi Nam biến mất. Video 4–9 là các bản ghi OBS cũ của chính Nam.

Vì vậy, không nên chuyển thẳng từ bóng người sang một cảnh Nam đang chơi Minecraft mà không có dấu hiệu cho khán giả biết rằng thời gian đã thay đổi.

Đề xuất dựng chuyển tiếp:

1. Video 3 kết thúc bằng màn hình đen.
2. Một khoảng nhiễu hình ảnh ngắn xuất hiện.
3. Chuyển sang footage OBS có giao diện máy tính và chất lượng hình ảnh khác biệt.
4. Tên tệp xuất hiện:

`Observed_Nam_POV.mp4`

5\. Sau đó bắt đầu footage của Nam.

Đây là một thay đổi về cách dựng video, không phải một cơ chế mới trong mod.

## Đánh giá của script doctor

Tôi đánh giá Video 3 là video quan trọng nhất trong bộ ba footage đầu tiên vì nó làm thay đổi cách khán giả hiểu về toàn bộ thế giới.

- Video 1: Có điều gì đó đã xảy ra với Nam.
- Video 2: Chuyện này có thể đã xảy ra từ trước Nam.
- Video 3: Có một thứ đang quan sát người em, và nó có thể liên quan đến Nam.

Một điểm tôi muốn giữ nguyên: Bóng người không nên có hành động tấn công. Sự hiện diện của nó đã đủ đáng sợ, đặc biệt khi người xem biết rằng nó vẫn đứng đó dù người em đã nhìn đi nơi khác.

Một điểm cần kiểm tra với phiên bản mod: Cơ chế `<System>` tự cài máy quay ở Stage 2 hoặc sau ba đêm thức trắng. Vì người em đang vào một world Stage 4, cần xác định cách mod xử lý vật phẩm đã được cài trước đó để tránh việc camera tự xuất hiện lặp lại hoặc tạo ra hai bản sao.

Video 3 kết thúc bộ ba footage của người em. Từ Video 4, chúng ta sẽ bắt đầu nhìn thấy những sự kiện này từ chính góc nhìn của Nam, trước khi anh biến mất.

# VIDEO 4 — `Observed_Nam_POV.mp4`

KỊCH BẢN CHI TIẾT · BẢN PHÁT TRIỂN 1.0

Bắt đầu dòng thời gian của Nam

Vai trò của video: Chuyển từ góc nhìn của người em sang những bản ghi OBS cũ của Nam. Khán giả lần đầu chứng kiến quá trình Nam phát hiện những hiện tượng bất thường, hiểu được sự xuất hiện của máy quay VHS và bắt đầu nghi ngờ rằng có một người khác đang ở trong thế giới của mình.

Thời lượng đề xuất: 10–12 phút.

Góc nhìn: Nam (`Nam_1998`).

Trạng thái thế giới khi bắt đầu: `sleep_count = 3`, Stage 2.

Trạng thái khi kết thúc: `sleep_count = 4`, vẫn Stage 2.

Điểm cần giữ thật chặt: Video 4 không phải một đoạn footage tiếp nối Video 3 về mặt thời gian. Đây là bản ghi cũ hơn, được Channy tìm thấy và trình chiếu sau khi xem ba video của người em.

## CẢNH 01 — BẢN GHI CỦA NAM

00:00 – 01:10

Chuyển dòng thời gian

Hình ảnh và hành động

1. Màn hình đen từ cuối Video 3.
2. Một đoạn nhiễu hình VHS rất ngắn.
3. Footage chuyển sang giao diện OBS cũ.
4. Tên tệp xuất hiện:

`Observed_Nam_POV.mp4`

5. Bản ghi bắt đầu. Minecraft đang chạy.
6. Góc nhìn thứ nhất, người chơi là Nam.

Không cần giới thiệu Nam bằng lời dẫn. Người xem sẽ nhận ra đây là một footage khác nhờ giao diện OBS, chất lượng hình ảnh và nhịp chơi.

Âm thanh

- Tiếng quạt máy tính.
- Tiếng click chuột.
- Âm thanh Minecraft.
- Không có nhạc nền mở đầu.

Mục đích

Đưa khán giả từ câu hỏi “Ai đang quan sát người em?” trở về thời điểm trước đó, khi Nam vẫn còn sống trong thế giới này.

## CẢNH 02 — BA ĐÊM THỨC TRẮNG

01:10 – 03:00

Stage 2

Hình ảnh và hành động

1. Nam đang ở gần căn nhà của mình.
2. Cậu kiểm tra inventory, đi quanh khu vực và thực hiện một vài công việc sinh tồn thông thường.
3. Màn đêm buông xuống.
4. Nam không đi ngủ.
5. Footage có thể tua qua một số khoảng thời gian không có hoạt động quan trọng, nhưng phải giữ cảm giác đây là một bản ghi liên tục.
6. Đến đêm thứ ba, Phantom bắt đầu xuất hiện nhiều hơn.

Hiện tượng Stage 2

- Nước chuyển sang màu xám bạc.
- Cây cối có biểu hiện bất thường khi quan sát qua camera hồng ngoại.
- Bóng người mang tên `Nam_1998` bắt đầu xuất hiện quanh nhà vào ban đêm.

Ở thời điểm này, Nam chưa có máy quay VHS nên không được cho thấy các hiện tượng chỉ nhìn thấy qua camera.

Dàn dựng

Nam có thể nhìn thấy nước xám bạc bằng mắt thường. Đây là dấu hiệu rõ ràng đầu tiên cho thấy thế giới đang thay đổi.

Không cần làm toàn bộ mặt nước phát sáng. Chỉ cần màu sắc và phản xạ của nước trở nên sai lệch so với bình thường.

Âm thanh

- Tiếng Phantom.
- Tiếng môi trường ban đêm.
- Tiếng bước chân của Nam.
- Không thêm lời thì thầm nếu chưa đến thời điểm phù hợp.

Mục đích

Cho thấy Nam đã cố gắng thức trắng trong nhiều đêm, và việc không ngủ đang khiến những hiện tượng bất thường tăng lên.

## CẢNH 03 — NHỮNG BÓNG NGƯỜI QUANH NHÀ

03:00 – 04:40

Hiện tượng bất thường

Hình ảnh và hành động

1. Nam đi ra ngoài căn nhà để kiểm tra khu vực xung quanh.
2. Ở rìa tầm nhìn, cậu phát hiện một nhân vật đứng gần hàng cây.
3. Nam quay camera về phía đó.
4. Không có ai ở đó nữa.
5. Cậu tiếp tục đi vòng quanh nhà.
6. Một bóng người khác xuất hiện ở phía đối diện.
7. Nam quay lại. Bóng người vẫn đứng yên ở khoảng cách xa.
8. Khi Nam tiến lại gần, footage có thể cắt sang một góc nhìn khác do OBS ghi lại, nhưng không được để bóng người chạy hoặc lao tới.

Chi tiết quan trọng

Những bóng người này mang tên trên đầu là:

`Nam_1998`

Đây không phải một người chơi lạ có username ngẫu nhiên. Chúng sử dụng chính tên của Nam.

Không cần cho nhiều bóng xuất hiện cùng lúc. Hai hoặc ba lần bắt gặp ở những vị trí khác nhau sẽ hiệu quả hơn một cảnh có cả đám đông đứng vây quanh nhà.

Âm thanh

- Tiếng Phantom ở xa.
- Tiếng bước chân của Nam.
- Âm thanh môi trường ban đêm.
- Không có âm thanh riêng cho lúc bóng người xuất hiện.

Mục đích

Đưa vào một câu hỏi mới: Nếu những bóng người đều mang tên Nam, vậy chúng là ai?

Ở Video 3, người em nhìn thấy một bóng người đứng quan sát mình. Video 4 cho thấy hiện tượng này đã tồn tại từ trước, trong chính thế giới của Nam.

## CẢNH 04 — MÁY QUAY ĐƯỢC CÀI ĐẶT

04:40 – 06:10

System can thiệp

Hình ảnh và hành động

1. Nam trở về nhà.
2. Cậu mở inventory để kiểm tra vật phẩm.
3. Ô số 3 xuất hiện một vật phẩm mới: `VHS Camera`.
4. Nam dừng thao tác.
5. Cậu chuyển sang ô số 3 và cầm máy quay lên.

Cơ chế đã chốt

Máy quay được `<System>` tự cài vào ô số 3 khi người chơi thức trắng tới đêm thứ ba hoặc khi đạt Stage 2.

Trong kịch bản này, Nam đã thức trắng ba đêm và đang ở Stage 2. Chỉ cần một lần cài đặt. Không được tạo hai sự kiện liên tiếp cho cùng một vật phẩm.

Thông điệp của System

Sau khi Nam kích hoạt máy quay, giao diện hiện lên:

CAMERA UI · SYSTEM MESSAGE

Cậu không chịu ngủ. Vậy thì hãy nhìn đi.

Giữ nguyên câu chữ. Không thêm câu trả lời từ Nam, không biến thông điệp thành một đoạn hội thoại.

Dàn dựng

- Nam giữ máy quay trong tay.
- Giao diện camera xuất hiện.
- Khung hình có nhiễu VHS nhẹ.
- Các thông tin pin và chế độ hồng ngoại được hiển thị.
- Nam thử hướng máy quay quanh căn nhà.

Âm thanh

- Tiếng mở inventory.
- Tiếng kích hoạt thiết bị.
- Tiếng rè VHS rất nhẹ.
- Âm thanh nền của Minecraft vẫn còn.

Mục đích

Đây là lần đầu tiên khán giả chứng kiến máy quay được đưa vào câu chuyện từ góc nhìn của Nam. Nhờ đó, sự xuất hiện của nó trong Video 3 không còn là một chi tiết ngẫu nhiên.

## CẢNH 05 — NHÌN THẤY NHỮNG GÌ MẮT THƯỜNG KHÔNG THẤY

06:10 – 08:10

Khám phá cơ chế camera

Hình ảnh và hành động

1. Nam bật chế độ hồng ngoại.
2. Cậu hướng camera về phía mặt nước.
3. Màu nước xám bạc trở nên rõ ràng hơn.
4. Nam quay về phía những hàng cây.
5. Qua camera, cây cối có biểu hiện phát triển hoặc thay đổi khi được quan sát.
6. Nam thử đưa camera ra khỏi khu vực cây rồi hướng trở lại.
7. Sau đó, cậu từ từ ngước lên trời.

Phát hiện Gargantua

Qua máy quay, Nam nhìn thấy Gargantua.

Luật hình ảnh

- Khi Nam bỏ máy quay xuống, bầu trời vẫn có mặt trời Minecraft bình thường.
- Khi nhìn qua camera, Gargantua mới xuất hiện.
- Không để Nam nhìn thấy hố đen bằng mắt thường.
- Không để camera biến mặt trời thành một vật thể khác trong góc nhìn thông thường.

Dàn dựng

Nam có thể di chuyển camera vài lần để kiểm tra xem hình ảnh có bị lỗi hay không. Sau đó cậu giữ máy quay hướng lên trời.

Không cần để Gargantua chuyển động rõ rệt. Kích thước và sự hiện diện của nó đã đủ tạo cảm giác bất thường.

Âm thanh

- Tiếng thiết bị hoạt động.
- Nhiễu VHS.
- Âm trầm rất nhẹ khi Gargantua lọt vào khung hình.
- Không có tiếng nói.

Mục đích

Nam phát hiện ra máy quay có thể nhìn thấy một thứ mà mắt thường không thể nhìn thấy. Đây là bước đầu tiên khiến cậu hiểu rằng thế giới Minecraft này đang che giấu một tầng hiện thực khác.

## CẢNH 06 — NHỮNG TIẾNG THÌ THẦM

08:10 – 09:40

Áp lực tâm lý

Hình ảnh và hành động

1. Nam hạ máy quay và quay về phía căn nhà.
2. Phantom xuất hiện nhiều hơn trên bầu trời.
3. Nam chạy vào trong nhà.
4. Một dòng chữ xuất hiện trên action bar.

ACTION BAR · WHISPER

# ...ngủ đi...

5. Nam dừng lại trong nhà.
6. Sau một khoảng thời gian, một dòng khác xuất hiện:

ACTION BAR · WHISPER

# ...nhắm mắt lại đi...

Quy tắc sử dụng lời thì thầm

Hai câu trên thuộc danh sách lời thì thầm đã chốt. Không cần phát liên tục hoặc biến chúng thành một cuộc hội thoại.

Có thể để các câu xuất hiện cách nhau một khoảng thời gian, trong lúc Nam đang cố gắng hoàn thành việc cuối cùng trước khi đi ngủ.

Các câu còn lại trong danh sách:

- `...chiếc giường vẫn còn ấm...`
- `...cậu mệt rồi...`

Nên giữ lại cho các đoạn sau nếu cần tăng áp lực. Không nhất thiết phải sử dụng hết trong Video 4.

Âm thanh

- Tiếng Phantom.
- Tiếng bước chân.
- Tiếng cửa đóng.
- Action bar không cần có giọng đọc.

Mục đích

Đây là lần đầu tiên người xem cảm nhận được rằng thế giới không chỉ thay đổi về mặt hình ảnh. Nó đang cố thuyết phục Nam đi ngủ.

## CẢNH 07 — LẦN NGỦ THỨ 4

09:40 – 11:30

Chuyển Stage

Hình ảnh và hành động

1. Nam đứng trong căn nhà.
2. Cậu nhìn về phía chiếc giường.
3. Một khoảng dừng ngắn.
4. Nam tiến lại và tương tác với giường.
5. Màn hình chuyển sang hiệu ứng ngủ của Minecraft.
6. Footage cắt sang buổi sáng.

Kết quả

Đây là lần ngủ thứ 4.

WORLD STATE

`sleep_count`\
4

`Stage`\
2

Điểm cần giữ về logic

Lần ngủ thứ 4 không đưa thế giới lên Stage 3.

Theo bảng Stage đã chốt:

| Số lần ngủ | Trạng thái |
| ---------- | ---------- |
| 1–2        | Stage 1    |
| 3–4        | Stage 2    |
| 5–6        | Stage 3    |
| 7 trở lên  | Stage 4    |

Vì vậy, sau khi thức dậy, Nam vẫn ở Stage 2.

Hình ảnh sau khi thức dậy

- Căn nhà vẫn nguyên vẹn.
- Nước vẫn có màu xám bạc.
- Không xuất hiện hiệu ứng cát hoặc sỏi bay ngược.
- Không có block mặt đất bị hút lên trời.
- Bầu trời nhìn bằng mắt thường vẫn bình thường.

Nam cầm lại máy quay, hướng về phía căn nhà và khu vực xung quanh.

Không cần cho thêm một hiện tượng lớn. Việc thế giới vẫn tiếp tục bất thường sau khi ngủ đã đủ tạo cảm giác rằng giấc ngủ không giải quyết được vấn đề.

Âm thanh

- Âm thanh thức dậy thông thường của Minecraft.
- Tiếng bước chân.
- Tiếng môi trường buổi sáng.

Kết thúc footage

Nam đứng nhìn ra ngoài căn nhà.

Một khoảng lặng.

Footage kết thúc.

END OF FOOTAGE

# Tổng hợp nhịp độ Video 4

| Cảnh | Nội dung                | Chức năng                       |
| ---- | ----------------------- | ------------------------------- |
| 01   | Bản ghi OBS của Nam     | Chuyển dòng thời gian           |
| 02   | Ba đêm thức trắng       | Thiết lập Stage 2               |
| 03   | Bóng người quanh nhà    | Tạo bí ẩn về Nam_1998           |
| 04   | Máy quay tự xuất hiện   | Giới thiệu can thiệp của System |
| 05   | Gargantua qua camera    | Xác nhận luật quan sát          |
| 06   | Phantom và lời thì thầm | Tăng áp lực                     |
| 07   | Lần ngủ thứ 4           | Kết thúc ở Stage 2              |

## Những chi tiết tôi chủ động chưa sử dụng

1\. Không đưa băng ký ức vào Video 4.

Theo quyết định B, ba băng không bắt buộc phải xuất hiện lần lượt trong Video 4–6. Video này đã có đủ chức năng giới thiệu máy quay và các hiện tượng Stage 2. Thêm một cuốn băng ở đây có nguy cơ làm loãng trọng tâm.

2\. Chưa sử dụng câu System thứ hai.

`Cậu đã ngủ đủ lâu để bắt đầu nhìn thấy.`

Câu này được giữ lại cho một thời điểm phù hợp hơn, khi nội dung và điều kiện kích hoạt đã được xác định. Không nên sử dụng chỉ để lấp chỗ trống.

3\. Không để Nam phát hiện toàn bộ sự thật.

Nam mới chỉ biết máy quay cho thấy những thứ mắt thường không nhìn thấy. Cậu chưa hiểu Gargantua là gì, chưa biết ý nghĩa của các bóng người và càng chưa hiểu bản chất của những chu kỳ.

## Kiểm tra tính liên tục

Có một điểm quan trọng cần thống nhất cho các video tiếp theo:

- Video 4 bắt đầu với `sleep_count = 3`.
- Nam thức trắng ba đêm và máy quay được cài vào ô số 3.
- Nam ngủ một lần, nâng `sleep_count` lên 4.
- Video 5 phải bắt đầu ở `sleep_count = 4`, vẫn Stage 2.
- Lần ngủ tiếp theo trong Video 5 mới đưa thế giới lên Stage 3.

Không được để hiệu ứng Stage 3 xuất hiện trước lần ngủ thứ 5.

## Chuyển tiếp sang Video 5

Tôi đề xuất Video 5 tập trung vào một sự thay đổi quan trọng: Nam không còn chỉ quan sát các hiện tượng xung quanh căn nhà nữa, mà bắt đầu tìm kiếm những dấu vết có liên quan đến quá khứ của mình.

Trọng tâm sẽ là căn nhà tháng 5/2018 và cối xay gió trắng trong Ký Ức Của Nam.

Tên video đề xuất:

`The_Last_Familiar_Place.mp4`

Điểm kết nên là lần ngủ thứ 5, khi Nam tỉnh dậy và lần đầu tiên nhìn thấy thế giới chuyển sang Stage 3.

Đây sẽ là bước chuyển từ nỗi bất an vì bị quan sát sang cảm giác thế giới đang dần mất đi những quy luật quen thuộc.

# VIDEO 5 — `The_Last_Familiar_Place.mp4`

KỊCH BẢN CHI TIẾT · BẢN PHÁT TRIỂN 1.0

Giai đoạn Stage 2 → Stage 3

Vai trò của video: Nam bắt đầu tìm kiếm một địa điểm quen thuộc trong thế giới của mình. Những hiện tượng bất thường không còn chỉ xuất hiện quanh căn nhà hiện tại. Cậu tìm thấy một căn nhà gỗ gắn với tháng 5/2018, cùng cối xay gió trắng. Đây là lần đầu tiên quá khứ của Nam xuất hiện dưới dạng một địa điểm cụ thể.

Thời lượng đề xuất: 10–12 phút.

Góc nhìn: OBS của Nam (`Nam_1998`).

Trạng thái khi bắt đầu: `sleep_count = 4`, Stage 2.

Trạng thái khi kết thúc: `sleep_count = 5`, Stage 3.

Lưu ý: Không đưa Elysian Highlands trở lại. Căn nhà tháng 5/2018 thuộc Ký Ức Của Nam, được khám phá đầy đủ sau giấc ngủ thứ 7 trong Video 7. Ở Video 5, Nam chỉ tìm thấy một dấu vết hoặc địa điểm liên quan, không được tiết lộ toàn bộ khu vực ký ức trước thời điểm.

## CẢNH 01 — SAU KHI THỨC DẬY

00:00 – 01:20

Nối tiếp Video 4

Hình ảnh và hành động

1. Video mở bằng cảnh Nam đứng trong căn nhà sau lần ngủ thứ 4.
2. Cậu bước ra ngoài.
3. Camera nhìn qua khu vực mặt nước xám bạc.
4. Nam cầm máy quay VHS và bật lên.
5. Cậu quan sát khu vực xung quanh, đặc biệt là những hàng cây.
6. Không có hiện tượng mới ngay lập tức.

Nam bắt đầu kiểm tra lại những nơi từng xuất hiện bóng người.

Âm thanh

- Tiếng môi trường buổi sáng.
- Tiếng bước chân.
- Tiếng máy quay VHS khởi động.

Mục đích

Tạo cảm giác Nam đang cố gắng tìm hiểu các hiện tượng thay vì chỉ phản ứng với chúng. Cậu đã bắt đầu hình thành thói quen sử dụng máy quay để kiểm tra thế giới.

## CẢNH 02 — NHỮNG DẤU VẾT KHÔNG THUỘC VỀ CĂN NHÀ

01:20 – 03:10

Khám phá

Hình ảnh và hành động

1. Nam rời khỏi căn nhà, đi theo một con đường hoặc địa hình tự nhiên ở gần khu vực sinh sống.
2. Cậu phát hiện một số dấu vết địa hình không giống khu vực xung quanh.
3. Camera dừng lại ở một khoảng đất trống, sau đó hướng về phía xa.
4. Nam tiếp tục di chuyển.
5. Một công trình bằng gỗ bắt đầu xuất hiện ở đường chân trời.

Lưu ý về thiết kế địa điểm

Không cần tạo thêm một khu vực hay biome mới. Đây là hành trình tìm đến một địa điểm có sẵn trong cấu trúc ký ức của Nam.

Không thêm bảng chỉ đường, NPC hoặc vật phẩm mới để dẫn đường nếu chưa có trong tài liệu A.

Âm thanh

- Tiếng bước chân trên cỏ.
- Âm thanh môi trường.
- Nhạc Minecraft nền rất nhỏ nếu tự nhiên xuất hiện.

Mục đích

Chuyển trọng tâm từ căn nhà hiện tại sang một địa điểm khác. Khán giả bắt đầu cảm nhận rằng thế giới này chứa nhiều dấu vết của những thời điểm khác nhau.

## CẢNH 03 — CĂN NHÀ THÁNG 5/2018

03:10 – 05:20

Vật chứng ký ức

Hình ảnh và hành động

1. Nam tiến lại gần công trình.
2. Một căn nhà gỗ sồi xuất hiện trong khung hình.
3. Bên cạnh căn nhà là cối xay gió màu trắng.
4. Nam đi chậm lại, quan sát toàn bộ công trình.
5. Cậu tiến tới cửa nhà.
6. Camera nhìn qua cửa sổ, quan sát nội thất.
7. Nam bước vào bên trong.

Những yếu tố đã chốt

- Nhà gỗ sồi.
- Cối xay gió trắng.
- Liên hệ với tháng 5/2018.

Không thêm người sống trong nhà, không thêm một nhân vật bí ẩn hay một cảnh sinh hoạt gia đình mới.

Dàn dựng hình ảnh

Căn nhà nên có cảm giác được bảo tồn, không phải một tòa nhà đổ nát.

Đây là một địa điểm có ý nghĩa cá nhân, vì vậy hãy tránh biến nó thành một phế tích kinh dị thông thường.

Âm thanh

- Tiếng cửa gỗ.
- Tiếng bước chân trên sàn gỗ.
- Âm thanh môi trường nhẹ.

Mục đích

Đây là lần đầu tiên người xem nhìn thấy một địa điểm gắn với quá khứ của Nam. Nó tạo ra một đối trọng với những hiện tượng kỳ lạ: giữa một thế giới đang biến dạng, vẫn có một nơi trông giống như được giữ lại từ một thời điểm cũ.

## CẢNH 04 — MỘT NƠI QUEN THUỘC

05:20 – 07:00

Khoảng lặng

Hình ảnh và hành động

1. Nam đi quanh tầng trệt.
2. Cậu nhìn những khối gỗ, đồ nội thất và các góc trong nhà.
3. Camera dừng lại ở một vài chi tiết.
4. Nam đi ra ngoài và nhìn lại cối xay gió.
5. Cậu bật máy quay VHS, quan sát căn nhà qua khung ngắm.

Đề xuất dàn dựng

Ở cảnh này, tôi muốn tránh một lỗi thường gặp: biến mọi căn nhà xuất hiện trong ARG thành một địa điểm chứa jump scare.

Căn nhà tháng 5/2018 nên được để yên.

Không cần:

- Cửa tự mở.
- Đèn tự tắt.
- Bóng người xuất hiện sau cửa sổ.
- Đồ đạc tự di chuyển.
- Tiếng gọi tên Nam.

Sự bình thường của căn nhà mới là điều khiến nó có giá trị.

Âm thanh

- Tiếng bước chân.
- Tiếng gió ngoài cửa.
- Tiếng máy quay hoạt động.
- Có thể để một khoảng lặng dài khi Nam đứng nhìn cối xay gió.

Mục đích

Khán giả có thời gian cảm nhận rằng Nam đang tìm thấy một thứ gì đó có liên quan đến cuộc sống trước đây của mình, dù cậu chưa hiểu rõ nó.

## CẢNH 05 — THẾ GIỚI BẮT ĐẦU THAY ĐỔI

07:00 – 08:50

Chuyển tiếp Stage

Hình ảnh và hành động

1. Nam rời khỏi căn nhà tháng 5/2018.
2. Cậu đi ra khu vực trống gần đó.
3. Một vài hạt cát bắt đầu bay lên khỏi mặt đất.
4. Nam dừng lại.
5. Cậu nhìn theo những hạt cát đang bay ngược lên trời.
6. Một số block sỏi bắt đầu có hiện tượng tương tự.
7. Nam chuyển sang máy quay VHS để quan sát.

Quan trọng về thời điểm

Cảnh này diễn ra trước lần ngủ thứ 5, vì vậy không được cho hiệu ứng Stage 3 xuất hiện ở đây.

Những hiện tượng cát và sỏi bay ngược lên trời chỉ nên xuất hiện sau khi `sleep_count` đạt 5.

Do đó, cảnh này cần được dời sang sau giấc ngủ thứ 5 ở cuối video. Phần trước đó chỉ nên kết thúc bằng việc Nam rời căn nhà và quay về nơi ngủ.

## CẢNH 05 — LẦN NGỦ THỨ 5

07:00 – 08:30

Chuyển Stage

Hình ảnh và hành động

1. Nam quay về căn nhà chính.
2. Cậu cất một vài vật phẩm.
3. Nam đứng cạnh giường.
4. Cậu dừng lại một lúc trước khi tương tác.
5. Nam nằm xuống.
6. Màn hình chuyển sang hiệu ứng ngủ.
7. Footage chuyển sang buổi sáng.

World state

WORLD STATE · AFTER SLEEP

`sleep_count`\
5

`Stage`\
3

Lưu ý: Không thêm lời thoại mới cho Nam ở thời điểm ngủ. Không để `<System>` nói thêm một câu nếu chưa có trong tài liệu.

## CẢNH 06 — TRỌNG LỰC KHÔNG CÒN ĐÚNG NỮA

08:30 – 10:30

Stage 3

Hình ảnh và hành động

1. Nam tỉnh dậy.
2. Cậu bước ra ngoài.
3. Một vài hạt cát bay lên khỏi mặt đất.
4. Nam dừng lại để quan sát.
5. Các block sỏi bắt đầu bay ngược lên.
6. Camera theo dõi một block sỏi bay lên cao.
7. Nam mở inventory hoặc kiểm tra la bàn.
8. Kim la bàn quay loạn, không còn chỉ về một hướng ổn định.

Hiệu ứng Stage 3 đã chốt

- Cát và sỏi bay ngược lên trời.
- La bàn quay loạn.

Không thêm:

- Nước xám bạc lơ lửng.
- Đất đá bị hút lên toàn bộ bầu trời.
- Gargantua xuất hiện bằng mắt thường.

Âm thanh

- Tiếng block vỡ hoặc chuyển động có thể được dùng rất tiết chế.
- Tiếng bước chân của Nam.
- Tiếng giao diện khi kiểm tra la bàn.
- Một lớp âm thanh môi trường thấp hơn bình thường.

Mục đích

Lần đầu tiên, sự bất thường không chỉ nằm trong tầm nhìn của camera. Quy luật vật lý của thế giới đang thay đổi ngay trước mắt Nam.

## CẢNH 07 — KẾT THÚC BẢN GHI

10:30 – 11:20

Cliffhanger

Hình ảnh và hành động

1. Nam bật máy quay VHS.
2. Cậu hướng camera vào những block cát và sỏi đang bay lên.
3. Nam chuyển góc nhìn sang la bàn.
4. Kim la bàn vẫn quay loạn.
5. Cậu quay lại phía căn nhà.
6. Footage kết thúc khi Nam vẫn đang cầm máy quay.

Không cần thêm bóng người, Phantom hoặc một thông điệp mới ở cuối video. Những thay đổi của Stage 3 đã đủ làm cliffhanger.

END OF FOOTAGE

# Tổng hợp nhịp độ Video 5

| Cảnh | Nội dung                               | Chức năng          |
| ---- | -------------------------------------- | ------------------ |
| 01   | Nam thức dậy sau lần ngủ thứ 4         | Nối tiếp Video 4   |
| 02   | Tìm kiếm địa điểm khác                 | Mở rộng không gian |
| 03   | Căn nhà tháng 5/2018                   | Giới thiệu ký ức   |
| 04   | Quan sát căn nhà và cối xay gió        | Tạo khoảng lặng    |
| 05   | Lần ngủ thứ 5                          | Kích hoạt Stage 3  |
| 06   | Cát, sỏi bay ngược và la bàn quay loạn | Thể hiện Stage 3   |
| 07   | Nam ghi hình hiện tượng                | Cliffhanger        |

## Những điểm cần bảo vệ

1\. Căn nhà tháng 5/2018 không phải Elysian Highlands.

Không sử dụng lại tên địa danh đã bị loại bỏ. Căn nhà và cối xay gió thuộc Ký Ức Của Nam, được khám phá đầy đủ sau giấc ngủ thứ 7.

2\. Không tiết lộ quá sớm nội dung của Video 7.

Video 5 chỉ cho thấy địa điểm và mối liên hệ với tháng 5/2018. Chưa xuất hiện bảng:

`Nơi ở của Nam / Tháng 5/2018 / / đừng bấm ngủ`

Bảng này thuộc cảnh Ký Ức Của Nam sau giấc ngủ thứ 7.

3\. Không ép xuất hiện băng ký ức.

Video 5 chưa cần `TAPE 02`. Tôi muốn giữ các băng như những vật chứng có mục đích, không phải các collectible buộc phải thu thập theo từng video.

4\. Giữ đúng mốc Stage.

Lần ngủ thứ 5 là thời điểm chuyển sang Stage 3. Không có hiệu ứng cát/sỏi bay ngược trước đó.

## Đánh giá của script doctor

Video 5 có nhiệm vụ khá khác Video 4.

Video 4 là lúc Nam phát hiện mình đang bị quan sát. Video 5 là lúc Nam bắt đầu tìm kiếm những thứ thuộc về quá khứ của mình.

Tôi đặc biệt muốn giữ căn nhà tháng 5/2018 thật bình thường. Khi khán giả đến Video 7 và nhận ra đây chính là một phần của Ký Ức Của Nam, họ sẽ nhớ lại rằng địa điểm này đã xuất hiện từ trước, nhưng chưa được giải thích.

Một đề xuất cần để mở: Có thể để một chi tiết kiến trúc của căn nhà tháng 5/2018 xuất hiện lại trong Ký Ức Của Nam ở Video 7, giúp khán giả nhận ra đây là cùng một địa điểm. Đây là liên kết hình ảnh, không phải lore mới.

Video tiếp theo sẽ là:

# VIDEO 6 — `Gravity_Decay.mp4`

Trọng tâm: Nam sống trong Stage 3, hiện tượng trọng lực tiếp tục sai lệch, những lời thì thầm trở nên rõ ràng hơn và cậu bắt đầu tiến gần đến lần ngủ thứ 6.

# VIDEO 6 — `Gravity_Decay.mp4`

KỊCH BẢN CHI TIẾT · BẢN PHÁT TRIỂN 1.0

Giai đoạn Stage 3 · Lần ngủ thứ 5 → 6

Vai trò của video: Đẩy sự bất thường của thế giới lên một cấp độ mới. Nam đã biết máy quay có thể tiết lộ những thứ mắt thường không nhìn thấy, nhưng giờ đây ngay cả những quy luật vật lý bình thường cũng đang sụp đổ. Những hiện tượng này khiến việc sinh tồn trở nên khó khăn và làm Nam ngày càng mất phương hướng.

Thời lượng đề xuất: 10–12 phút.

Góc nhìn: OBS của Nam (`Nam_1998`).

Trạng thái khi bắt đầu: `sleep_count = 5`, Stage 3.

Trạng thái khi kết thúc: `sleep_count = 6`, vẫn Stage 3.

Nguyên tắc dàn dựng: Video này không cần một địa điểm mới hay một thực thể mới. Nỗi kinh dị đến từ việc thế giới quen thuộc dần mất đi những quy luật vốn có.

## CẢNH 01 — MỌI THỨ VẪN ĐANG BAY LÊN

00:00 – 01:30

Nối tiếp Video 5

Hình ảnh và hành động

1. Video mở bằng footage tiếp nối cuối Video 5.
2. Nam vẫn đang cầm máy quay VHS.
3. Trước mặt cậu, một vài block cát và sỏi tiếp tục bay ngược lên trời.
4. Nam hạ máy quay xuống, quan sát bằng mắt thường.
5. Hiện tượng vẫn xảy ra. Đây không phải lỗi hình ảnh của camera.
6. Nam tiến lại gần một vị trí có cát đang bay lên.
7. Cậu đứng quan sát một lúc rồi quay về phía căn nhà.

Dàn dựng hình ảnh

- Không cho tất cả block cùng bay lên một lúc.
- Các block xuất hiện rải rác, ở những vị trí khác nhau.
- Một số block bay chậm, một số đã ở khá cao.
- Không để mặt đất bị hút lên thành những mảng lớn. Hiệu ứng đó thuộc Stage 4.

Âm thanh

- Tiếng bước chân trên cỏ.
- Tiếng môi trường Minecraft.
- Âm thanh chuyển động của block ở mức vừa phải.

Mục đích

Xác nhận rằng Stage 3 vẫn đang tiếp diễn sau Video 5. Nam không thể đơn giản chờ đến sáng để mọi thứ trở lại bình thường.

## CẢNH 02 — LA BÀN KHÔNG CÒN CHỈ ĐƯỜNG

01:30 – 03:00

Phương hướng biến mất

Hình ảnh và hành động

1. Nam mở inventory.
2. Cậu lấy la bàn ra.
3. Camera tập trung vào kim la bàn.
4. Kim quay liên tục, không ổn định ở một hướng.
5. Nam di chuyển vài bước.
6. Cậu dừng lại, nhìn la bàn lần nữa.
7. Kim vẫn quay loạn.

Dàn dựng

Không cần tạo hiệu ứng la bàn vỡ hoặc biến thành một vật phẩm khác. La bàn vẫn hoạt động về mặt hình thức, nhưng không còn giúp Nam xác định phương hướng.

Có thể để Nam thử mở bản đồ hoặc nhìn địa hình xung quanh, nhưng không thêm một cơ chế định vị mới.

Âm thanh

- Tiếng mở inventory.
- Tiếng chuyển vật phẩm.
- Không cần âm thanh đặc biệt khi kim la bàn quay.

Mục đích

Đây là một chi tiết nhỏ nhưng có ý nghĩa lớn: Nam không chỉ mất đi khả năng hiểu thế giới, mà còn mất đi công cụ cơ bản để định hướng trong đó.

## CẢNH 03 — ÂM THANH ĐI NGƯỢC

03:00 – 05:10

Kinh dị âm thanh

Hình ảnh và hành động

1. Nam tiếp tục di chuyển qua khu vực có cát và sỏi.
2. Footage ghi lại một khoảng thời gian tương đối bình thường.
3. Khi Nam dừng lại, âm thanh môi trường bắt đầu có dấu hiệu bất thường.
4. Một số âm thanh được phát ngược.
5. Nam đứng yên, không di chuyển.
6. Âm thanh tiếp tục trong vài giây.
7. Nam bật máy quay VHS và hướng về khu vực xung quanh.

Thiết kế âm thanh

Đây là cảnh nên dành nhiều công sức nhất cho phần hậu kỳ.

Có thể sử dụng các âm thanh Minecraft quen thuộc đã được đảo ngược, chẳng hạn:

- Tiếng bước chân.
- Tiếng tương tác với block.
- Một số âm thanh môi trường.

Không nên đảo ngược tất cả âm thanh cùng lúc. Nếu mọi thứ đều bị đảo ngược, người xem sẽ nhanh chóng nhận ra đây chỉ là một hiệu ứng âm thanh.

Thay vào đó, hãy để một âm thanh quen thuộc xuất hiện theo chiều ngược, trong khi những âm thanh khác vẫn bình thường.

Lời thì thầm

Lời thì thầm đã được xác định trong tài liệu A:

ACTION BAR · WHISPER

# ...chiếc giường vẫn còn ấm...

Có thể cho câu này xuất hiện trên action bar trong lúc Nam đang đứng giữa khu vực trống.

Không thêm giọng nói đọc câu này. Không tạo một cuộc hội thoại giữa Nam và `<System>`.

Mục đích

Ở Video 4, lời thì thầm giống như lời mời gọi đi ngủ. Đến Video 6, nó nên tạo cảm giác như thế giới đang nhắc Nam về chiếc giường, ngay cả khi cậu không muốn nghĩ đến nó.

## CẢNH 04 — MỘT TẦN SỐ KHÔNG RÕ NGUỒN

05:10 – 06:50

Đề xuất mở rộng vật chứng

Ý tưởng cảnh

Đây là vị trí tôi cân nhắc sử dụng `TAPE 03 — Tần Số 14.8 MHz`.

Tuy nhiên, theo quyết định B của bạn, ba băng ký ức không bắt buộc phải xuất hiện trong Video 4–6. Vì vậy, tôi không muốn biến việc tìm thấy cuốn băng thành một nhiệm vụ bắt buộc.

Phương án đề xuất

Nam đang sử dụng máy quay VHS thì phát hiện một tín hiệu âm thanh bất thường.

1. Nam dừng camera.
2. Cậu thử bật và tắt hồng ngoại.
3. Tín hiệu vẫn còn.
4. Footage có thể chuyển sang giao diện camera trong vài giây.
5. Một đoạn nhiễu âm thanh xuất hiện.
6. Nam không tìm được nguồn phát.

Nếu muốn đưa băng ký ức vào video này, có thể để `TAPE 03 — Tần Số 14.8 MHz` xuất hiện như một vật chứng liên quan đến tín hiệu đó.

Nhưng cần xác định rõ cơ chế trước khi đưa vào kịch bản chính thức.

CHƯA CHỐT

TAPE 03 — Tần Số 14.8 MHz

- Vị trí: Video 6.
- Vai trò dự kiến: Liên hệ với tín hiệu âm thanh bất thường.
- Nội dung ghi âm: Chưa được xác định đầy đủ trong tài liệu A.
- Cơ chế xuất hiện: Cần hỏi phiên mod.

Tôi không đề xuất tự viết nội dung băng lúc này, vì việc tạo ra một nguồn phát hoặc một thông điệp mới có thể vô tình bổ sung lore chưa được duyệt.

Mục đích

Tạo một khả năng mở rộng cho hệ thống vật chứng mà không ép buộc video phải có thêm một collectible.

## CẢNH 05 — PHANTOM NGÀY CÀNG NHIỀU

06:50 – 08:40

Áp lực tăng dần

Hình ảnh và hành động

1. Thời gian trong game chuyển sang ban đêm.
2. Nam quay về khu vực căn nhà.
3. Phantom bắt đầu xuất hiện trên bầu trời.
4. Số lượng Phantom tăng dần.
5. Nam cố gắng đi vào nhà.
6. Cậu dừng lại khi nghe thấy tiếng Phantom ở gần.
7. Một dòng chữ xuất hiện trên action bar.

ACTION BAR · WHISPER

# ...cậu mệt rồi...

Dàn dựng

- Không cần để Phantom lao vào người chơi.
- Không tạo một trận chiến sinh tồn.
- Không biến chúng thành một đợt quái vật tấn công thông thường.
- Hãy để chúng xuất hiện trên cao, khiến người xem có cảm giác Nam đang bị bao vây bởi một thứ không thể giải thích.

Âm thanh

- Tiếng Phantom.
- Tiếng môi trường ban đêm.
- Một số âm thanh đảo ngược từ cảnh trước có thể tái xuất hiện rất nhẹ.

Mục đích

Nam đã ngủ ở lần thứ 5, nhưng thế giới vẫn không trở lại bình thường. Những lời thì thầm tiếp tục xuất hiện và Phantom ngày càng đông.

## CẢNH 06 — NAM KHÔNG CÒN MUỐN NHÌN LÊN

08:40 – 10:00

Khoảng lặng trước giấc ngủ

Hình ảnh và hành động

1. Nam vào nhà.
2. Cậu đóng cửa.
3. Footage dừng lại ở góc nhìn hướng về căn phòng.
4. Nam mở inventory, kiểm tra các vật phẩm.
5. Cậu cầm máy quay VHS, nhìn qua khung ngắm một lần nữa.
6. Không có hiện tượng mới.
7. Nam hạ máy quay xuống.
8. Camera quay về phía giường.

Dàn dựng

Đây nên là cảnh ít chuyển động nhất video.

Không cần lời thoại mới. Không cần thêm thông điệp của `<System>`.

Chỉ để người xem thấy Nam đang đứng trước chiếc giường sau một chuỗi hiện tượng bất thường kéo dài.

Âm thanh

- Tiếng Phantom ở xa.
- Tiếng môi trường bên ngoài căn nhà.
- Âm thanh trong nhà yên tĩnh.

Mục đích

Chuẩn bị cho lần ngủ thứ 6. Nam không ngủ vì đã giải quyết được vấn đề. Cậu chỉ tiếp tục chu kỳ mà mình chưa hiểu.

## CẢNH 07 — LẦN NGỦ THỨ 6

10:00 – 11:30

Tiếp tục Stage 3

Hình ảnh và hành động

1. Nam tiến lại gần giường.
2. Camera dừng một lúc trước khi tương tác.
3. Nam nằm xuống.
4. Màn hình chuyển sang hiệu ứng ngủ.
5. Footage chuyển sang buổi sáng.
6. Nam thức dậy.

World state

WORLD STATE · AFTER SLEEP

`sleep_count`\
6

`Stage`\
3

Hình ảnh sau khi thức dậy

- Cát và sỏi vẫn bay ngược lên trời.
- La bàn vẫn quay loạn.
- Thế giới chưa chuyển sang Stage 4.
- Không có hiệu ứng block mặt đất bị hút lên trời.

Nam bật máy quay VHS, hướng lên bầu trời một lần nữa.

Kết thúc footage

Camera dừng ở một khung hình có những block sỏi đang bay lên.

Không cần thêm một thực thể mới.

Màn hình cắt đen.

END OF FOOTAGE

# Tổng hợp nhịp độ Video 6

| Cảnh | Nội dung                           | Chức năng             |
| ---- | ---------------------------------- | --------------------- |
| 01   | Cát và sỏi tiếp tục bay lên        | Nối tiếp Stage 3      |
| 02   | La bàn quay loạn                   | Mất phương hướng      |
| 03   | Âm thanh đảo ngược và lời thì thầm | Tăng kinh dị âm thanh |
| 04   | Tín hiệu 14.8 MHz                  | Đề xuất vật chứng     |
| 05   | Phantom xuất hiện nhiều hơn        | Tăng áp lực           |
| 06   | Nam đứng trước giường              | Chuẩn bị giấc ngủ     |
| 07   | Lần ngủ thứ 6                      | Kết thúc Stage 3      |

## Những điều cần bảo vệ

1\. Không đưa hiệu ứng Stage 4 vào Video 6.

Block mặt đất bị hút lên trời là hiệu ứng của Stage 4, bắt đầu từ lần ngủ thứ 7. Video này chỉ sử dụng cát và sỏi bay ngược.

2\. Không trộn hiệu ứng nước của Stage 2 vào Stage 3.

Nước xám bạc là dấu hiệu Stage 2. Không cần tiếp tục nhấn mạnh hiệu ứng đó trong video này.

3\. Không để Phantom trở thành quái vật chiến đấu.

Chúng là một phần của áp lực tâm lý. Sự hiện diện và số lượng của chúng quan trọng hơn một cảnh đánh nhau.

4\. Không bắt buộc phải thu thập TAPE 03.

Nếu chưa có nội dung và cơ chế rõ ràng, nên để băng này xuất hiện ở một thời điểm khác hoặc loại khỏi Video 6.

## Đánh giá của script doctor

Video 6 nên tạo cảm giác ngột ngạt hơn Video 5, nhưng không nhất thiết phải có nhiều cảnh hơn.

Tôi muốn người xem cảm nhận được ba tầng bất thường:

- Thị giác: Cát và sỏi không còn chịu tác động của trọng lực như bình thường.
- Định hướng: La bàn không còn giúp Nam tìm đường.
- Thính giác: Những âm thanh quen thuộc bị đảo ngược, xen lẫn lời thì thầm.

Ba tầng này cùng phục vụ một ý tưởng: thế giới đang dần mất đi những quy luật giúp con người cảm thấy an toàn.

Và sau khi Nam ngủ lần thứ 6, chúng ta đã đến sát ngưỡng của bước ngoặt lớn nhất trong toàn bộ hành trình.

# VIDEO 7 — `The_7th_Sleep.mp4`

Video tiếp theo cần được xử lý như một chương chuyển tiếp lớn, không đơn thuần là một video về giấc ngủ.

Nó phải bao gồm đầy đủ trình tự đã chốt:

1. Nam ngủ lần thứ 7, thế giới đạt Stage 4.
2. Overworld xuất hiện hiệu ứng block mặt đất bị hút lên trời.
3. Nam tỉnh dậy giữa cánh đồng hướng dương.
4. Những bông hoa đều quay về phía Nam.
5. Cậu tìm thấy căn nhà gỗ và cối xay gió trắng.
6. Bảng `Nơi ở của Nam / Tháng 5/2018 / / đừng bấm ngủ`.
7. Nam xuống hầm, tìm `Trang Xé Cuối Cùng`.
8. Phát hiện đường dẫn OBS được ghi trong nhật ký.
9. Đọc dòng `[DƯỚI HẦM] Anh đi đây`.
10. Phát hiện vết rách dẫn đến The Womb.

Điểm kết đề xuất: Nam bước qua vết rách. Không cắt sang The Womb quá sớm; hãy để Video 7 kết thúc đúng khoảnh khắc cậu rời khỏi thế giới quen thuộc.

# VIDEO 7 — `The_7th_Sleep.mp4`

KỊCH BẢN CHI TIẾT · BẢN PHÁT TRIỂN 1.0

Bước ngoặt cốt truyện · Lần ngủ thứ 7 · Stage 4

Vai trò của video: Đây là điểm kết thúc hành trình của Nam trong Overworld. Sau sáu lần ngủ và hàng loạt hiện tượng bất thường, lần ngủ thứ bảy đưa Nam đến một nơi hoàn toàn khác. Cậu không còn đơn thuần khám phá những dấu hiệu của thế giới đang biến dạng nữa. Cậu bắt đầu tìm thấy câu trả lời về căn nhà, về những ký ức và về con đường mà anh trai đã để lại.

Thời lượng đề xuất: 12–15 phút.

Góc nhìn: OBS của Nam (`Nam_1998`).

Trạng thái khi bắt đầu: `sleep_count = 6`, Stage 3.

Trạng thái sau khi ngủ: `sleep_count = 7`, Stage 4.

Điểm kết: Nam bước qua vết rách dẫn đến The Womb.

## CẢNH 01 — GIẤC NGỦ THỨ BẢY

00:00 – 01:40

Stage 3 → Stage 4

Hình ảnh và hành động

1. Video mở đầu bằng một đoạn footage ngắn nối tiếp Video 6.
2. Nam đứng trong căn nhà, trước chiếc giường.
3. Ngoài cửa sổ, những block cát và sỏi vẫn bay lên.
4. Nam nhìn ra ngoài một lúc.
5. Cậu quay lại, tiến về phía giường.
6. Nam tương tác với giường và bắt đầu ngủ.
7. Màn hình chuyển sang đen.

Âm thanh

- Tiếng môi trường ban đêm.
- Tiếng Phantom ở xa.
- Âm thanh môi trường giảm dần khi Nam nằm xuống.

Chuyển cảnh

Không sử dụng hiệu ứng giật mình hoặc hình ảnh quái vật xuất hiện ngay khi màn hình tối.

Giữ màn hình đen lâu hơn bình thường một chút.

Sau đó, footage xuất hiện trở lại.

## CẢNH 02 — THẾ GIỚI ĐÃ VỠ

01:40 – 03:00

Stage 4

Hình ảnh và hành động

1. Nam thức dậy.
2. Cậu rời khỏi giường.
3. Camera hướng ra cửa sổ.
4. Những block đất, cỏ và đá đang bị hút lên trời.
5. Nam bước ra ngoài.
6. Camera lia chậm qua khu vực xung quanh.
7. Những mảng địa hình nhỏ lơ lửng phía trên mặt đất.
8. Một số block tiếp tục bay lên, để lại khoảng trống trong địa hình.

Dàn dựng

Đây là lần đầu tiên hiệu ứng Stage 4 xuất hiện trong footage của Nam.

Cần tạo cảm giác quy mô lớn hơn những hiện tượng trước:

- Stage 1: Mặt trời có dấu hiệu bất thường.
- Stage 2: Môi trường và những bóng người xung quanh nhà biến đổi.
- Stage 3: Cát và sỏi bay ngược, la bàn mất phương hướng.
- Stage 4: Chính mặt đất bắt đầu bị hút lên trời.

Không biến toàn bộ thế giới thành một đống đổ nát ngay lập tức. Những khu vực vẫn còn nguyên vẹn sẽ khiến các mảng địa hình bị hút lên trở nên bất thường hơn.

Âm thanh

- Tiếng gió mạnh hơn bình thường.
- Tiếng block vỡ hoặc rơi ở khoảng cách xa.
- Không cần nhạc nền dồn dập.

Mục đích

Xác nhận lần ngủ thứ 7 đã đưa thế giới đến Stage 4. Đây là trạng thái mà người em từng nhìn thấy trong Video 1–3.

## CẢNH 03 — TỈNH DẬY Ở MỘT NƠI KHÔNG CÒN BÓNG TỐI

03:00 – 04:30

Không gian ký ức

Hình ảnh và hành động

1. Footage đột ngột chuyển sang một không gian sáng.
2. Nam tỉnh dậy giữa một cánh đồng hướng dương rộng lớn.
3. Không có căn nhà quen thuộc.
4. Không còn những block bị hút lên trời.
5. Không có Phantom.
6. Nam đứng yên, nhìn xung quanh.

Dòng mô tả trong game

SYSTEM MESSAGE · GAME DESCRIPTION

## Bạn mở mắt. Không còn bóng tối, không còn Hố Đen.

Lưu ý: Đây là dòng mô tả trong game, sử dụng đại từ “bạn”, không phải lời thoại của `<System>`.

Chi tiết quan trọng

Những bông hoa hướng dương đều quay về phía Nam.

Nam thử di chuyển sang một vị trí khác.

Các bông hoa vẫn quay về phía cậu.

Nam đứng yên.

Camera từ từ lia ngang qua cánh đồng, cho thấy hiện tượng này không chỉ xảy ra với một vài bông hoa gần người chơi.

Âm thanh

- Tiếng gió nhẹ.
- Tiếng môi trường đồng cỏ.
- Không có tiếng thì thầm.
- Không có âm thanh Phantom.

Mục đích

Đây là khoảng nghỉ đầu tiên thực sự trong cả hành trình của Nam. Nhưng sự yên bình này vẫn có một điểm không tự nhiên: toàn bộ cánh đồng dường như đang chú ý đến cậu.

## CẢNH 04 — CĂN NHÀ THÁNG 5/2018

04:30 – 06:40

Địa điểm ký ức

Hình ảnh và hành động

1. Nam phát hiện một công trình ở phía xa.
2. Cậu bắt đầu đi về phía đó.
3. Khi đến gần, công trình hiện ra là một căn nhà gỗ sồi.
4. Bên cạnh nhà có một cối xay gió màu trắng.
5. Nam đi một vòng quanh căn nhà.
6. Cậu quan sát cửa ra vào, cửa sổ và khu vực xung quanh.
7. Không có dấu hiệu của người đang sinh sống tại đây.
8. Nam tiến đến chiếc bảng đặt gần căn nhà.

Nội dung bảng

SIGN · 4 DÒNG

## Nơi ở của Nam

## Tháng 5/2018

## &#x20;

## đừng bấm ngủ

Dàn dựng

Căn nhà nên được xây dựng với cảm giác quen thuộc, bình thường và có người từng sinh sống.

Không cần tạo ra các hiệu ứng kinh dị ngay khi Nam nhìn thấy nó.

Cối xay gió trắng cũng không nên tự xoay hoặc phát ra âm thanh bất thường nếu tài liệu A không quy định điều đó.

Âm thanh

- Gió thổi qua cánh đồng.
- Tiếng bước chân.
- Âm thanh môi trường yên tĩnh.

Mục đích

Đây là lần đầu Nam tìm thấy một địa điểm có mốc thời gian cụ thể gắn với tên mình. Căn nhà không chỉ là một công trình trong Minecraft. Nó là một phần ký ức mà cậu chưa hiểu vì sao mình lại có mặt ở đây.

## CẢNH 05 — CHIẾC GIƯỜNG VẪN CÒN ẤM

06:40 – 08:00

Chi tiết bất thường

Hình ảnh và hành động

1. Nam mở cửa căn nhà.
2. Cậu bước vào trong.
3. Camera từ từ quan sát nội thất.
4. Không gian bên trong tương đối bình thường.
5. Nam tìm thấy chiếc giường màu đỏ.
6. Camera dừng lại ở chiếc giường trong vài giây.

Chi tiết hình ảnh

Chiếc giường đỏ được giữ nguyên như một vật thể bình thường. Không cần thêm hiệu ứng phát sáng hay biến dạng.

Điểm quan trọng là sự đối lập giữa một căn nhà yên bình và dòng chữ trên bảng bên ngoài:

`đừng bấm ngủ`

Âm thanh

- Tiếng cửa mở.
- Tiếng bước chân trên sàn gỗ.
- Không nhạc nền.

Mục đích

Gợi lại lời cảnh báo xuyên suốt nhật ký Nam. Chiếc giường không còn đơn thuần là một vật dụng để hồi phục trong Minecraft.

Tuy nhiên, cảnh này không nên giải thích ý nghĩa của nó. Câu trả lời sẽ nằm ở những gì Nam tìm thấy bên dưới căn nhà.

## CẢNH 06 — CĂN HẦM

08:00 – 09:30

Phát hiện vật chứng

Hình ảnh và hành động

1. Nam phát hiện lối xuống hầm.
2. Cậu mở lối đi và bước xuống.
3. Không gian bên dưới tối hơn căn nhà phía trên.
4. Camera quan sát khu vực hầm.
5. Nam tìm thấy một vật phẩm mang tên `Trang Xé Cuối Cùng`.
6. Cậu nhặt vật phẩm và mở ra đọc.

Dàn dựng

Căn hầm không cần quá rộng.

Nó nên có cảm giác như một nơi được sử dụng để cất giữ những thứ không muốn để lộ ra bên ngoài.

Không thêm phòng bí mật, đường hầm phụ hay công trình mới nếu chưa được xác nhận trong tài liệu.

Âm thanh

- Tiếng bước chân xuống cầu thang.
- Tiếng cửa hoặc lối đi đóng lại.
- Âm thanh trong hầm trầm và yên tĩnh hơn bên ngoài.

Mục đích

Chuyển từ một không gian ký ức yên bình sang phần lưu trữ những thông tin cuối cùng Nam muốn để lại.

## CẢNH 07 — TRANG XÉ CUỐI CÙNG

09:30 – 11:30

Vật chứng cốt truyện

Hình ảnh và hành động

1. Nam mở `Trang Xé Cuối Cùng`.
2. Camera tập trung vào từng phần nội dung.
3. Nam đọc mục `[CHO EM]`.
4. Camera dừng lại ở đường dẫn OBS.
5. Sau đó Nam tiếp tục đọc mục `[DƯỚI HẦM]`.

Nội dung đã chốt

TRANG XÉ CUỐI CÙNG

[CHO EM]

Anh để lại các đoạn ghi hình ở đây:

`C:/Users/Nam/Videos/OBS/Samsara_Archive/`

Xem từ tệp đầu tiên. Đừng tua.

TRANG XÉ CUỐI CÙNG

[DƯỚI HẦM]

Anh đi đây.

Dàn dựng nội dung

Không nên hiển thị toàn bộ trang sách cùng lúc nếu điều đó khiến chữ quá nhỏ.

Có thể chia thành hai lần đọc:

- Lần đầu: Nam đọc `[CHO EM]`, camera dừng ở đường dẫn.
- Lần hai: Nam đọc `[DƯỚI HẦM]`.

Đường dẫn OBS là một đoạn văn bản được ghi trong game. Nó không có nghĩa là mod đang đọc hoặc truy cập thư mục thật trên máy tính.

Âm thanh

- Tiếng lật trang sách.
- Tiếng môi trường trong hầm.
- Không có giọng đọc tự động.

Mục đích

Đây là một trong những thông tin quan trọng nhất của toàn bộ chuỗi video.

Nam không chỉ để lại nhật ký. Cậu còn để lại một kho ghi hình và một chỉ dẫn rõ ràng cho người sẽ tìm thấy chúng.

Câu `Xem từ tệp đầu tiên. Đừng tua.` cũng tạo ra lý do hợp lý cho việc Channy xem các footage theo thứ tự.

## CẢNH 08 — VẾT RÁCH

11:30 – 13:30

Kết thúc Overworld

Hình ảnh và hành động

1. Sau khi đọc xong trang cuối, Nam đóng sách.
2. Camera từ từ hướng về phía cuối căn hầm.
3. Một vết rách không gian xuất hiện trong khu vực này.
4. Phía bên kia là một khoảng tối không giống Nether hay End.
5. Nam tiến lại gần.
6. Cậu dừng lại trước vết rách.
7. Camera hướng qua khe nứt, ghi lại hình ảnh không gian phía bên kia.
8. Nam bước qua.

Dàn dựng hình ảnh

Vết rách phải mang cảm giác không gian bị xé ra, không phải một cánh cổng Minecraft thông thường.

Không sử dụng:

- Khung obsidian.
- Hiệu ứng portal tím của Nether.
- Hình ảnh End portal.
- Một cấu trúc mới có tên riêng.

Đây là lối dẫn sang The Womb đã được xác định trong lore.

Âm thanh

- Âm thanh môi trường trong hầm giảm dần.
- Một lớp âm trầm xuất hiện khi camera hướng về vết rách.
- Không sử dụng tiếng hét hoặc âm thanh tấn công.

Khoảnh khắc cuối

Nam bước qua.

Camera theo sau cậu trong một khoảnh khắc ngắn.

Màn hình nhiễu mạnh.

Sau đó:

# NO SIGNAL

OBS RECORDING INTERRUPTED

END OF VIDEO 7

# Tổng kết Video 7

| Cảnh | Nội dung              | Chức năng                    |
| ---- | --------------------- | ---------------------------- |
| 01   | Nam ngủ lần thứ 7     | Kích hoạt bước ngoặt         |
| 02   | Stage 4               | Xác nhận thế giới sụp đổ     |
| 03   | Cánh đồng hướng dương | Chuyển sang không gian ký ức |
| 04   | Nhà Nam tháng 5/2018  | Tìm thấy địa điểm quen thuộc |
| 05   | Chiếc giường đỏ       | Gợi lại lời cảnh báo         |
| 06   | Căn hầm               | Tìm vật chứng cuối           |
| 07   | Trang Xé Cuối Cùng    | Để lại chỉ dẫn               |
| 08   | Vết rách              | Dẫn sang The Womb            |

## Kiểm tra tính nhất quán

Đúng timeline

Video bắt đầu ở `sleep_count = 6`, sau đó Nam ngủ lần thứ 7 và bước vào Stage 4.

Đúng lore

Cánh đồng hướng dương, căn nhà, cối xay gió, chiếc giường đỏ, Trang Xé Cuối Cùng và vết rách đều thuộc nội dung đã chốt.

Không phá quy tắc mod

Đường dẫn OBS chỉ là chữ trong sách Minecraft, không có truy cập file thật.

Không tiết lộ The Womb quá sớm

Video chỉ kết thúc khi Nam bước qua vết rách. Hành trình bên kia sẽ dành trọn cho Video 8.

# Chuyển tiếp sang Video 8 — `Journey_To_The_Womb.mp4`

Video 8 cần có cảm giác khác hẳn bảy video trước.

Trước đây, Nam luôn ở trong một thế giới Minecraft có địa hình, nhà cửa, cây cối và những quy luật quen thuộc. The Womb là nơi những thứ đó không còn vận hành theo cách bình thường.

9

Bốn chương đã chốt:

1. Bến Đò: Biển đen, Gargantua, dải phao đèn, giường đỏ chìm dưới nước và con thuyền bóng ma.
2. Những Cánh Cửa: 16 cánh cửa đại diện cho những save file khác nhau.
3. Những Người Khổng Lồ: Những lần chết và các thế giới cũ được lưu giữ trong ký ức.
4. Tháp Ngược: Cánh cửa cuối cùng, hồ gương, Quả Trứng và The Wanderer.

Điểm quan trọng nhất khi viết Video 8: đừng biến The Womb thành một dungeon để Nam vượt qua. Đây là nơi lưu giữ ký ức của những chu kỳ, những thế giới và những phiên bản khác nhau của chính cậu. Mỗi chương nên hé lộ một phần sự thật, thay vì chỉ đưa ra một thử thách để giải quyết.

# VIDEO 8 — `Journey_To_The_Womb.mp4`

KỊCH BẢN CHI TIẾT · BẢN PHÁT TRIỂN 1.0

The Womb · Hành trình qua những ký ức

Vai trò của video: Đây là chương khám phá trung tâm của Project Samsara. Nam đã bước ra khỏi thế giới Minecraft quen thuộc và tiến vào nơi lưu giữ những ký ức của các chu kỳ. Những gì cậu nhìn thấy không phải các thế giới xa lạ, mà là những mảnh đời, những lần tồn tại và những phiên bản khác nhau của chính mình.

Thời lượng đề xuất: 15–18 phút.

Góc nhìn: OBS của Nam (`Nam_1998`).

Trạng thái: Nam đã bước qua vết rách cuối Video 7. Video 8 diễn ra hoàn toàn trong The Womb.

Cấu trúc đã duyệt:

1. Bến Đò.
2. Những Cánh Cửa.
3. Những Người Khổng Lồ.
4. Tháp Ngược.

Nguyên tắc dàn dựng: The Womb không phải dungeon. Không có nhiệm vụ chiến đấu, không có boss fight và không cần giải đố để mở đường. Nam đang đi qua một nơi lưu giữ ký ức. Mỗi chương cần khiến cậu hiểu thêm một chút về bản chất của thế giới.

# CHƯƠNG I — BẾN ĐÒ

00:00 – 04:00

## CẢNH 01 — PHÍA BÊN KIA VẾT RÁCH

00:00 – 01:10

Hình ảnh và hành động

1. Video mở đầu bằng màn hình nhiễu của cuối Video 7.
2. Tín hiệu hình ảnh dần khôi phục.
3. Nam đang đứng trên một bến gỗ nhỏ.
4. Trước mặt cậu là một vùng biển đen trải dài đến tận đường chân trời.
5. Không có đất liền, cây cối hay công trình quen thuộc.
6. Camera từ từ ngước lên bầu trời.

Hình ảnh quan trọng

Phía trên vùng biển là Gargantua.

Nhưng lần này, nó không còn là một chấm đen nhỏ trên bầu trời. Nó hiện ra như một con ngươi khổng lồ đang nhìn xuống vùng biển.

Không cần cho Gargantua chuyển động hoặc phát ra âm thanh. Sự hiện diện của nó đã đủ tạo cảm giác áp đảo.

Âm thanh

- Tiếng nước rất nhỏ.
- Gió trầm và kéo dài.
- Tiếng nhiễu VHS còn sót lại từ đoạn chuyển cảnh.

Dàn dựng

Không sử dụng nhạc nền ngay lập tức. Hãy để người xem có thời gian quan sát cảnh quan mới.

Mục đích

Xác nhận rằng Nam đã bước vào một không gian khác. Gargantua không còn chỉ là một hiện tượng quan sát được qua camera. Nó đang hiện diện ngay phía trên cậu.

## CẢNH 02 — DẢI ĐÈN VỀ PHÍA BẮC

01:10 – 02:20

Hình ảnh và hành động

1. Nam quay camera xuống mặt nước.
2. Phía xa xuất hiện một dải phao đèn.
3. Những chiếc phao tạo thành một con đường kéo dài về phía bắc.
4. Nam nhìn lại phía sau.
5. Bến gỗ vẫn ở đó, nhưng không có đường quay lại rõ ràng.
6. Cậu tiến về phía chiếc thuyền đang neo tại bến.

Chi tiết

Dải đèn này liên hệ trực tiếp với lời nhắn cuối trong nhật ký cũ:

> Lên thuyền. Theo dải đèn về phía bắc... ĐỪNG ĐỂ NÓ RỜI KHỎI ỐNG KÍNH.

Không cần phát lại toàn bộ lời nhắn bằng giọng đọc. Người xem đã biết nội dung này từ Video 2.

Âm thanh

- Tiếng nước vỗ nhẹ vào bến.
- Tiếng gỗ của chiếc thuyền.
- Không thêm lời thì thầm mới.

Mục đích

Tạo sự kết nối giữa nhật ký của Nam và địa điểm hiện tại. Lời nhắn không còn là một câu cảnh báo vô nghĩa.

## CẢNH 03 — CHIẾC GIƯỜNG DƯỚI NƯỚC

02:20 – 03:10

Hình ảnh và hành động

1. Nam bước lên thuyền.
2. Cậu bắt đầu di chuyển theo dải phao đèn.
3. Camera hướng xuống mặt nước.
4. Dưới làn nước đen, Nam nhìn thấy một chiếc giường đỏ.
5. Chiếc giường nằm chìm dưới mặt nước, tách biệt khỏi mọi thứ xung quanh.
6. Nam dừng thuyền để quan sát.
7. Camera giữ nguyên góc nhìn trong vài giây.
8. Sau đó Nam tiếp tục đi.

Dàn dựng

Chiếc giường đỏ là vật thể đã xuất hiện trong những video trước. Không cần biến nó thành một vật thể có chuyển động riêng.

Nó nên được đặt ở vị trí đủ dễ nhận ra nhưng không quá nổi bật.

Âm thanh

- Tiếng thuyền di chuyển.
- Âm thanh nước.
- Một khoảng lặng khi Nam dừng quan sát chiếc giường.

Mục đích

Nhắc lại chủ đề giấc ngủ và những lần Nam đã bước qua các chu kỳ. Chiếc giường xuất hiện ở một nơi không có căn nhà, không có phòng ngủ và không có lý do thông thường để tồn tại.

## CẢNH 04 — CON THUYỀN BÓNG MA

03:10 – 04:00

Phát hiện qua camera

Hình ảnh và hành động

1. Nam tiếp tục đi theo dải đèn.
2. Phía xa xuất hiện một chiếc thuyền khác.
3. Bằng mắt thường, Nam chỉ thấy mặt biển và những chiếc phao.
4. Cậu bật VHS Camera.
5. Qua camera, một chiếc thuyền bóng ma xuất hiện.
6. Nam hướng camera theo chiếc thuyền.
7. Cậu nhận ra hình dáng người ngồi trên đó.

Sự thật cần thể hiện

Con thuyền bóng ma chính là Nam.

Không cần giải thích bằng thoại. Hãy để camera ghi lại hình ảnh chiếc thuyền và người ngồi bên trong.

Đây là một phiên bản khác của chính cậu, không phải một nhân vật mới.

Dàn dựng

- Không cho thuyền bóng ma tấn công.
- Không cho người trên thuyền nói chuyện.
- Không để nó biến mất bằng một hiệu ứng lớn.
- Chỉ cần để Nam quan sát nó trong một khoảng thời gian ngắn.

Mục đích

Đưa ý tưởng “Tất cả là Một” vào trải nghiệm thực tế của Nam. Cậu đang nhìn thấy một phiên bản khác của chính mình, nhưng chưa có đủ thông tin để hiểu điều đó.

# CHƯƠNG II — NHỮNG CÁNH CỬA

04:00 – 08:00

## CẢNH 05 — BẾN ĐẾN

04:00 – 05:00

Hình ảnh và hành động

1. Nam đi hết dải phao đèn.
2. Phía trước xuất hiện một bến gỗ khác.
3. Cậu bước xuống thuyền.
4. Camera quan sát khu vực xung quanh.
5. Trước mặt Nam là một không gian rộng với nhiều cánh cửa.

Dàn dựng

Không cần tạo thêm một công trình trung gian. Chuyển thẳng từ bến đò sang không gian Những Cánh Cửa.

Âm thanh nước dần nhỏ đi, thay bằng tiếng vang không gian.

Mục đích

Chuyển từ ký ức về những lần tồn tại sang nơi lưu giữ các thế giới.

## CẢNH 06 — MƯỜI SÁU CÁNH CỬA

05:00 – 06:30

Hình ảnh và hành động

1. Nam tiến lại gần khu vực các cánh cửa.
2. Camera lia ngang qua toàn bộ không gian.
3. Có tổng cộng 16 cánh cửa.
4. Mỗi cánh cửa mang tên một save file.
5. Nam đi dọc theo hàng cửa và quan sát các nhãn.
6. Camera dừng lại ở một số cái tên quen thuộc.

Các nhãn đã xác định

MỐC MỚI NHẤT

`New World (14) 03/2021`

`Nhà Nam 05/2018`

`Thế giới của em 04/2012`

MỐC CŨ NHẤT

`World1 2009`

Các tên save còn lại cần lấy đúng từ tài liệu B hoặc danh sách đã được xác nhận. Không tự tạo thêm tên world để lấp đủ 16 cánh cửa.

Dàn dựng

Thứ tự cửa đi từ các save mới hơn về những save cũ hơn, kết thúc ở `World1 2009`.

Không cần cho Nam mở từng cửa. Chỉ cần đi dọc hành lang và quan sát.

Âm thanh

- Tiếng bước chân vang nhẹ.
- Âm thanh gió rất nhỏ.
- Không có tiếng người bên trong cửa.

Mục đích

Đây là lần đầu Nam nhìn thấy những thế giới của mình được đặt cạnh nhau như một kho lưu trữ.

Các save file không còn chỉ là những bản đồ Minecraft nằm trên máy tính. Chúng được thể hiện như những cánh cửa dẫn tới các chu kỳ tồn tại khác nhau.

## CẢNH 07 — NHỮNG CÁNH CỬA QUEN THUỘC

06:30 – 08:00

Hình ảnh và hành động

1. Nam dừng trước cánh cửa `Nhà Nam 05/2018`.
2. Camera giữ nguyên ở bảng tên.
3. Nam quay sang cánh cửa `Thế giới của em 04/2012`.
4. Camera dừng lại lâu hơn một chút.
5. Nam nhìn dọc theo hàng cửa còn lại.
6. Cuối cùng, cậu quay về phía con đường dẫn đến chương tiếp theo.

Dàn dựng

Không mở cửa `Nhà Nam 05/2018` để tái hiện lại căn nhà. Người xem vừa trải nghiệm địa điểm đó trong Video 7.

Không mở `Thế giới của em 04/2012` để đưa thêm một thế giới mới chưa có trong cấu trúc.

Điều quan trọng ở đây là tên của các save. Nam đang nhìn thấy những dấu mốc tồn tại của chính mình.

Âm thanh

- Tiếng bước chân dừng lại.
- Một khoảng lặng dài khi camera hướng vào `Thế giới của em 04/2012`.

Mục đích

Gợi ra một câu hỏi mà video chưa cần trả lời: nếu các save đều thuộc về những chu kỳ khác nhau, thì ai là người đã sống trong những thế giới đó?

# CHƯƠNG III — NHỮNG NGƯỜI KHỔNG LỒ

08:00 – 12:30

## CẢNH 08 — STEVE 2009

08:00 – 09:20

Hình ảnh và hành động

1. Nam bước vào khu vực tiếp theo.
2. Không gian mở rộng thành một vùng đất rộng lớn.
3. Ở phía xa là một Steve khổng lồ đang quỳ.
4. Nam tiến lại gần.
5. Camera từ từ ngước lên, cho thấy kích thước của thực thể.
6. Steve vẫn giữ nguyên tư thế quỳ.

Dàn dựng

Steve 2009 đại diện cho một trong những lần tồn tại đầu tiên.

Không cho Steve đứng dậy hoặc di chuyển. Tư thế quỳ là hình ảnh cần giữ lại.

Không thêm khuôn mặt méo mó hay hiệu ứng kinh dị cơ thể. Sự bất thường nằm ở kích thước và tư thế của nhân vật.

Âm thanh

- Gió trầm.
- Tiếng bước chân của Nam.
- Không có nhạc dồn dập.

Mục đích

Đưa người xem đối diện với một phiên bản cổ xưa của Steve, một ký ức đã tồn tại từ những thế giới đầu tiên.

## CẢNH 09 — HỐ NỔ CREEPER 2013

09:20 – 10:30

Hình ảnh và hành động

1. Nam rời khỏi khu vực Steve khổng lồ.
2. Cậu tiến đến một vùng đất có hố nổ Creeper rất lớn.
3. Camera quan sát từ mép hố.
4. Nam đi vòng quanh miệng hố.
5. Những block đất và đá nằm rải rác xung quanh.
6. Không có Creeper xuất hiện.

Dàn dựng

Đây là ký ức về một lần chết trong Minecraft.

Không tái hiện vụ nổ trực tiếp. Không cần thêm cảnh nhân vật bị thương hoặc biến mất.

Chỉ cần một hố nổ lớn, để lại dấu vết của một sự kiện đã xảy ra.

Âm thanh

- Gió.
- Tiếng bước chân trên địa hình.
- Không có tiếng Creeper.

Mục đích

Cho thấy những sự kiện tưởng như rất bình thường trong Minecraft cũng có thể trở thành ký ức được lưu giữ tại The Womb.

## CẢNH 10 — NGƯỜI ĐỐT NHÀ

10:30 – 12:30

Hình ảnh và hành động

1. Nam tiến đến một khu vực có một căn nhà đang cháy.
2. Camera dừng lại ở căn nhà.
3. Một nhân vật khổng lồ đang nâng căn nhà cháy lên.
4. Nam nhìn từ xa.
5. Camera từ từ tiến gần hơn.
6. Nhân vật vẫn giữ nguyên căn nhà trên tay.

Dàn dựng

Đây là một trong những hình ảnh quan trọng nhất của chương Những Người Khổng Lồ.

Căn nhà cháy là một ký ức về một thế giới đã mất.

Không cần giải thích ai đã đốt căn nhà hoặc vì sao nó bị đốt. Không tự đặt tên cho nhân vật khổng lồ.

Hình ảnh phải có cảm giác một ký ức đau buồn đang được lưu giữ, chứ không phải một cảnh chiến đấu.

Âm thanh

- Tiếng lửa cháy nhỏ.
- Gió.
- Âm thanh môi trường trầm.

Mục đích

Đưa chủ đề mất mát trở lại. Những thế giới trong các save file không chỉ lưu giữ thành tựu và công trình. Chúng còn lưu giữ những lần thất bại, những lần chết và những thứ đã bị bỏ lại.

# CHƯƠNG IV — THÁP NGƯỢC

12:30 – 17:30

## CẢNH 11 — CÁNH CỬA CUỐI CÙNG

12:30 – 13:30

Hình ảnh và hành động

1. Nam rời khỏi khu vực Những Người Khổng Lồ.
2. Một cánh cửa đứng tách biệt xuất hiện phía trước.
3. Nam tiến lại gần.
4. Camera hướng vào bảng tên trên cửa.

Nội dung bảng

SIGN · 4 DÒNG

## New World

## Chơi lần cuối:

## bây giờ

## bạn

Dàn dựng

Đây là cánh cửa khác biệt với 15 cánh cửa trước.

Nó không mang tên một save file trong quá khứ. Nó mang mốc thời gian hiện tại.

Dòng cuối cùng là `bạn`.

Không cần giải thích thêm bằng lời thoại.

Âm thanh

- Tiếng bước chân dừng lại.
- Không gian yên tĩnh.

Mục đích

Đưa người xem từ những ký ức đã qua đến chính thế giới hiện tại mà họ đang theo dõi.

## CẢNH 12 — HỒ GƯƠNG

13:30 – 14:40

Hình ảnh và hành động

1. Nam bước qua cánh cửa.
2. Cậu xuất hiện bên cạnh một hồ nước rộng.
3. Mặt hồ phẳng và phản chiếu không gian xung quanh.
4. Nam tiến đến sát mép hồ.
5. Camera hướng xuống mặt nước.
6. Hình ảnh phản chiếu của Nam xuất hiện.

Dàn dựng

Không cần làm phản chiếu có hành vi khác biệt với Nam ở cảnh này.

Chỉ cần mặt nước quá tĩnh và hình ảnh phản chiếu quá rõ, tạo cảm giác đây không phải một hồ nước bình thường.

Âm thanh

- Gió gần như biến mất.
- Tiếng bước chân nhỏ dần.
- Không có tiếng nước chuyển động.

Mục đích

Chuẩn bị cho hình ảnh Quả Trứng và sự đối diện cuối cùng ở Video 9.

## CẢNH 13 — QUẢ TRỨNG

14:40 – 15:50

Hình ảnh và hành động

1. Camera từ từ ngước khỏi mặt hồ.
2. Quả Trứng xuất hiện trong khung hình.
3. Nam đứng yên nhìn nó.
4. Camera quan sát Quả Trứng từ nhiều góc độ.
5. Phía sau là Tháp Ngược.
6. Nam không tương tác với Quả Trứng.

Dàn dựng

Quả Trứng là trung tâm của cảnh này.

Không cho nó nứt hoặc nở trong Video 8. Sự kiện đó thuộc Video 9.

Không thêm ký hiệu, văn tự hoặc cơ chế tương tác mới lên bề mặt Quả Trứng.

Âm thanh

- Âm trầm rất nhẹ.
- Không sử dụng âm thanh nhịp tim.
- Không có giọng nói.

Mục đích

Đặt Nam trước vật thể trung tâm của toàn bộ hành trình. Video 9 sẽ là nơi sự thật được đối diện trực tiếp.

## CẢNH 14 — LỮ KHÁCH VÔ DANH

15:50 – 17:00

Thực thể quan sát

Hình ảnh và hành động

1. Nam đang hướng camera về phía Quả Trứng.
2. Qua khung ngắm VHS, một bóng người xuất hiện ở phía xa.
3. Bóng người đứng đối diện Nam.
4. Nam giữ camera hướng về thực thể.
5. Thực thể đứng yên.
6. Nam hạ camera xuống để quan sát bằng mắt thường.
7. Thực thể không còn xuất hiện trong tầm nhìn thông thường.
8. Nam bật camera lên lần nữa.
9. Lữ Khách Vô Danh xuất hiện ở một vị trí gần hơn.

Quy tắc hành vi đã chốt

- The Wanderer chỉ nhìn thấy qua camera.
- Khi bị nhìn qua ống kính, nó đứng yên.
- Khi không bị quan sát, nó tiến lại gần.
- Không cho nó chạy hoặc tấn công.
- Không cho nó xuất hiện rõ ràng bằng mắt thường.

Dàn dựng

Cảnh này cần được dựng bằng sự đối lập giữa hai lần quan sát:

- Khi Nam nhìn qua camera: thực thể đứng yên.
- Khi Nam không nhìn qua camera: vị trí của thực thể thay đổi.

Không cần giải thích cơ chế. Người xem có thể tự nhận ra quy luật.

Âm thanh

- Tiếng nhiễu camera nhẹ.
- Không có tiếng bước chân của thực thể.
- Không thêm lời thoại.

Mục đích

Đưa The Wanderer trở lại như một thực thể bí ẩn đã được xác định trong lore. Nó không cần trở thành nhân vật phản diện hay có một màn đối đầu riêng.

## CẢNH 15 — TRƯỚC QUẢ TRỨNG

17:00 – 17:30

Hình ảnh và hành động

1. Nam hướng camera về phía Quả Trứng.
2. The Wanderer không còn nằm trong khung hình.
3. Nam đứng trước hồ gương.
4. Camera tiến gần Quả Trứng.
5. Khung hình dừng lại.

Âm thanh

- Âm thanh trầm dần biến mất.
- Còn lại tiếng nhiễu VHS rất nhẹ.

Kết thúc

Màn hình chuyển đen.

# END OF RECORDING

NAM_1998 · OBS

END OF VIDEO 8

# Tổng kết cấu trúc Video 8

| Chương                    | Nội dung                                | Ý nghĩa                                   |
| ------------------------- | --------------------------------------- | ----------------------------------------- |
| I. Bến Đò                 | Gargantua, biển đen, thuyền bóng ma     | Nam nhìn thấy một phiên bản khác của mình |
| II. Những Cánh Cửa        | 16 save file                            | Những chu kỳ và thế giới đã tồn tại       |
| III. Những Người Khổng Lồ | Steve 2009, Creeper 2013, căn nhà cháy  | Những ký ức về các lần chết và mất mát    |
| IV. Tháp Ngược            | Cửa cuối, hồ gương, Quả Trứng, Wanderer | Đưa Nam đến sự thật cuối cùng             |

## Kiểm tra tính nhất quán

Đúng cấu trúc đã duyệt

Không đưa những địa danh đã bị loại bỏ trở lại.

Đúng quy tắc camera

Gargantua và The Wanderer được thể hiện theo đúng giới hạn quan sát đã xác định.

Không thêm nhân vật mới

Những hình ảnh khổng lồ là ký ức của các chu kỳ, không được tự ý gán danh tính mới.

Không giải quyết Quả Trứng quá sớm

Quả Trứng chưa nứt, chưa nở và chưa có cuộc đối thoại. Tất cả dành cho Video 9.

## Những điểm cần giữ nguyên khi dựng

- Bến Đò: Chiếc giường đỏ phải là một chi tiết dễ nhận ra, không biến thành một cảnh tương tác mới.
- Những Cánh Cửa: Đủ 16 cánh cửa. Danh sách nhãn phải lấy từ nguồn đã duyệt, không tự điền tên.
- Những Người Khổng Lồ: Không biến thành boss hoặc thử thách chiến đấu.
- Tháp Ngược: Quả Trứng là trọng tâm, The Wanderer chỉ đóng vai trò quan sát.
- Kết thúc: Không có lời giải thích của Nam. Cậu chỉ đứng trước Quả Trứng và footage kết thúc.

# Tiếp theo: VIDEO 9 — `The_Egg_Dialogue.mp4`

Chương kết · Cần bảo toàn thoại nguyên văn

Video 9 là chương quan trọng nhất về mặt kịch bản. Đây không còn là một cuộc khám phá mà là cuộc đối diện cuối cùng giữa Nam và `<System>`.

Những phần đã chốt cần được giữ nguyên:

1. Cinematic mở đầu tại Quả Trứng.
2. Đoạn thoại `<System>` phải giữ nguyên văn từ tài liệu B.
3. Tên Nam nhiễu dần thành username thật của người chơi.
4. Bóng phản chiếu lớn dần ×45.
5. Gargantua được tiết lộ là con mắt của bóng phản chiếu.
6. Quả Trứng nở.
7. Màn hình `NO SIGNAL`.
8. Outro Channy giữ nguyên nguyên văn.
9. Sau outro mới xuất hiện Chu Kỳ #2.

Lưu ý: nguyên văn đoạn thoại `<System>` và outro Channy đã được chép từ tài liệu gốc vào Video 9 (mục "Nguyên văn đoạn thoại") và Phần II (mục "Nguyên văn outro"). Giữ đúng nguyên văn, không viết lại.

# VIDEO 9 — `The_Egg_Dialogue.mp4`

KỊCH BẢN CHI TIẾT · BẢN PHÁT TRIỂN 1.0

CHƯƠNG KẾT · CHU KỲ #2

Vai trò của video: Khép lại hành trình của Nam và hé lộ bản chất thực sự của Project Samsara. Những điều người xem từng chứng kiến trong tám video trước được kết nối lại: những save file, những lần ngủ, những bóng ma, những người khổng lồ và Gargantua.

Đây không phải một trận chiến cuối cùng. Nam không thể đánh bại `<System>`, cũng không cần tìm cách phá hủy Quả Trứng. Cậu chỉ có thể đối diện với sự thật.

Thời lượng đề xuất: 8–10 phút, chưa tính outro Channy và đoạn hậu kết Chu Kỳ #2.

Góc nhìn: Cinematic trong game, tiếp nối footage OBS của Nam.

Trạng thái: Nam đang đứng trước Quả Trứng tại Tháp Ngược, cuối Video 8.

Ba phần phải phân biệt rõ

1. Video 9: Cuộc đối thoại và sự kiện Quả Trứng nở.
2. Outro Channy: Đoạn kết của Investigator, giữ nguyên văn bản đã duyệt.
3. Chu Kỳ #2: Hậu kết xuất hiện sau outro, không được đưa lên trước.

## CẢNH 01 — TÍN HIỆU TRỞ LẠI

00:00 – 01:00

Hình ảnh và hành động

1. Màn hình đen sau Video 8.
2. Tiếng nhiễu VHS xuất hiện.
3. Footage dần khôi phục.
4. Camera hướng về Quả Trứng.
5. Nam vẫn đứng tại vị trí cuối cùng của Video 8.
6. Camera từ từ tiến gần hơn.
7. Không có The Wanderer trong khung hình.

Âm thanh

- Tiếng nhiễu tín hiệu.
- Âm trầm rất nhẹ.
- Không có tiếng môi trường thông thường.

Dàn dựng

Không mở đầu bằng một cảnh chuyển động lớn. Người xem cần nhận ra đây vẫn là cùng một địa điểm và cùng khoảnh khắc cuối Video 8.

## CẢNH 02 — `<System>` LÊN TIẾNG

01:00 – 03:00

THOẠI NGUYÊN VĂN

Hình ảnh và hành động

1. Camera hướng thẳng vào Quả Trứng.
2. Mọi chuyển động trong khung hình dừng lại.
3. Một dòng chữ của `<System>` xuất hiện.
4. Nam không di chuyển.
5. Cuộc đối thoại bắt đầu.

Quy tắc bắt buộc

Đoạn thoại `<System>` Video 9 là nội dung bất khả xâm phạm. Phải lấy nguyên văn từ tài liệu B.

Không được thay thế câu hỏi của Nam bằng một phiên bản diễn đạt lại.

### Nguyên văn đoạn thoại (từ tài liệu gốc, mục IV)

*(Phông nền vô cực sao. Cửa sổ Chat Vanilla tự động gõ chữ màu vàng)*

> **`<Nam_1998>`** Mọi thứ kết thúc rồi sao? Hố Đen... thế giới Minecraft của tôi bị phá hủy rồi à?
> 
> **`<System>`** Không. Thế giới đó chỉ là một lớp vỏ trứng đã đến lúc phải rạn nứt. Cậu vừa kết thúc một kiếp sống, Nam.
> 
> **`<Nam_1998>`** Tôi đã chết? Tai nạn? Hay tôi bị bầy quái vật trong game giết chết?
> 
> **`<System>`** Cậu chỉ đơn giản là đã kiệt sức và nhắm mắt lại. Đừng lo lắng, đây là một quá trình tự nhiên. Mọi linh hồn đều phải trải qua nó.
> 
> **`<Nam_1998>`** Vậy... gia đình tôi? Em trai tôi? Những người chơi khác trên các Server tôi từng gặp? Cả những bóng ma mang tên tôi qua máy quay VHS nữa? Mọi người đang ở đâu?
> 
> **`<System>`** Không có ai ở đâu cả. Chỉ có cậu.
> 
> **`<Nam_1998>`** Ý bạn là sao?
> 
> **`<System>`** Tất cả họ... đều là cậu. 
> Steve đầu tiên đặt chân vào thế giới Alpha năm 2009 là cậu. 
> Gã người chơi bị nổ chết bởi Creeper trong hầm mỏ năm 2013 là cậu. 
> Người em trai mà cậu luôn nghĩ rằng đang đợi cậu ở nhà... cũng là cậu ở một kiếp sống khác.
> 
> **`<Nam_1998>`** *(Khoảng dừng 5 giây)* Tôi... là tất cả họ? Vậy những phế tích tôi tìm thấy ở Elysian Highlands? Căn nhà gỗ năm 2018?
> 
> **`<System>`** Là do chính cậu xây nên ở kiếp trước. Mỗi khi cậu tạo một Save File mới, cậu đẻ ra một kiếp sống. Mỗi khi cậu phá hủy một công trình của người khác trên mạng, cậu đang tự phá nhà của chính mình. Mỗi khi cậu giúp đỡ một người chơi lạ mặt, cậu đang tự cứu rỗi bản thân ở một thời điểm khác.
> 
> **`<Nam_1998>`** Kể cả kẻ đã tranh cãi với tôi trên Server năm ngoái? Kẻ đã đốt nhà tôi?
> 
> **`<System>`** Đều là cậu. Cả nạn nhân lẫn kẻ thủ ác. Cả người sáng tạo lẫn kẻ phá hủy. Cậu đã sống hàng chục ngàn kiếp trong thế giới lập phương này.
> 
> **`<Nam_1998>`** Vậy... vũ trụ này là gì? Tại sao Minecraft lại tồn tại? Tại sao lại có vòng lặp Samsara này?
> 
> **`<System>`** Vũ trụ này là một lồng ấp. Một Quả Trứng Vũ Trụ. Cậu chưa đủ lớn. Cậu là một thai nhi đang học cách nhận thức thông qua sự luân hồi. Chỉ khi cậu đã sống đủ mọi kiếp sống của từng nhân vật, từng Save File, chịu đựng đủ mọi sự phân rã và nỗi cô độc... tâm thức của cậu mới thực sự trưởng thành.
> 
> **`<Nam_1998>`** Sau khi trưởng thành... tôi sẽ trở thành gì?
> 
> **`<System>`** Cậu sẽ trở thành một Vũ Trụ Mới. Một Thực Thể Sáng Tạo như tôi.
> 
> **`<Nam_1998>`** *(Khoảng dừng dài)* Còn người đang ngồi trước màn hình máy tính theo dõi đoạn băng này thì sao? Người đang xem từng khung hình OBS này của tôi?
> 
> **`<System>`** Họ cũng chính là cậu. Nhưng họ vẫn đang ở trong một lớp vỏ trứng khác, chưa đến lượt thức tỉnh. Bây giờ, Nam... hãy nhắm mắt lại. Quả trứng tiếp theo đã sẵn sàng.

Thiết kế hình ảnh

- Không hiển thị `<System>` như một nhân vật.
- Không tạo avatar hay khuôn mặt cho nó.
- Văn bản xuất hiện như giao diện hệ thống trong game.
- Cách hiển thị bình thản, không đe dọa.

Âm thanh

- Giọng `<System>` nếu tài liệu gốc có quy định giọng đọc thì giữ đúng quy định đó.
- Nếu chỉ có chữ, không tự thêm giọng đọc.
- Không sử dụng âm thanh hù dọa.

Mục đích

Đây là lần đầu sự thật được nói ra trực tiếp. `<System>` không phải một con quái vật đang cố lừa Nam. Nó là một phần của chu kỳ, và cuộc đối thoại cần giữ được cảm giác bình thản, gần gũi như một người mẹ.

## CẢNH 03 — TÊN NAM BIẾN MẤT

03:00 – 04:20

Danh tính bị thay thế

Hình ảnh và hành động

1. Camera giữ nguyên hướng về Quả Trứng.
2. Một dòng thông tin có tên Nam xuất hiện.
3. Tên bắt đầu bị nhiễu.
4. Một vài ký tự biến dạng.
5. Tên `Nam_1998` dần bị thay thế.
6. Cuối cùng, nó trở thành username thật của người chơi đang xem.

Dàn dựng kỹ thuật

Đây là hiệu ứng phá vỡ ranh giới giữa câu chuyện và người xem.

Cần phân biệt:

- Trong nội dung ARG, đây là một hiệu ứng được dựng có chủ đích.
- Không phải mod tự đọc toàn bộ tài khoản hoặc dữ liệu cá nhân của người xem.
- Mod chỉ cần sử dụng username trong game theo giới hạn kỹ thuật đã chốt.

Không tự ý đưa tên thật, email hoặc thông tin máy tính của người xem vào hiệu ứng.

Âm thanh

- Nhiễu kỹ thuật số ngắn.
- Âm thanh bị ngắt quãng khi từng ký tự biến đổi.
- Sau khi tên hoàn tất, mọi âm thanh trở lại yên tĩnh.

Mục đích

Người xem không còn đứng ngoài quan sát Nam. Họ bắt đầu nhận ra rằng mình cũng là một phần của chu kỳ đang được mô tả.

## CẢNH 04 — BÓNG PHẢN CHIẾU

04:20 – 05:50

Hình ảnh và hành động

1. Camera chuyển hướng xuống mặt hồ gương.
2. Hình ảnh phản chiếu của Nam xuất hiện.
3. Bóng phản chiếu bắt đầu lớn dần.
4. Camera vẫn giữ góc nhìn ổn định.
5. Bóng phản chiếu ngày càng chiếm nhiều diện tích mặt hồ.
6. Cuối cùng, kích thước của nó đạt mức lớn gấp 45 lần.

Quy tắc hình ảnh

Hệ số ×45 là thông tin đã chốt.

Không nên biến nó thành một cảnh bóng người khổng lồ bước ra khỏi mặt nước hoặc tấn công Nam. Đây là sự phóng đại của hình ảnh phản chiếu, không phải một trận đối đầu.

Âm thanh

- Âm trầm tăng dần.
- Tiếng nhiễu camera.
- Không có tiếng gầm hoặc tiếng bước chân.

Mục đích

Cho người xem thấy rằng bóng phản chiếu không đơn thuần là hình ảnh của Nam. Nó là thứ có quy mô lớn hơn rất nhiều so với thế giới mà cậu đang đứng trong đó.

## CẢNH 05 — GARGANTUA LÀ CON MẮT

05:50 – 06:50

Tiết lộ cốt lõi

Hình ảnh và hành động

1. Camera từ mặt hồ từ từ ngước lên.
2. Gargantua xuất hiện phía trên.
3. Khung hình chuyển đổi giữa Gargantua và bóng phản chiếu.
4. Người xem nhận ra Gargantua chính là con mắt của bóng phản chiếu.
5. Camera giữ nguyên khung hình rộng để thể hiện quy mô.

Dàn dựng

Không cần thêm một đoạn giải thích bằng chữ.

Hãy để mối liên hệ được truyền đạt bằng hình ảnh:

- Bóng phản chiếu ngày càng lớn.
- Camera hướng lên.
- Gargantua nằm ở vị trí con mắt của nó.

Mục đích

Đây là lời giải cho hình ảnh Gargantua đã xuất hiện từ những video đầu tiên.

Nó không phải một thiên thể ngẫu nhiên hay một cánh cổng độc lập. Nó là con mắt của thực thể phản chiếu khổng lồ.

## CẢNH 06 — QUẢ TRỨNG NỞ

06:50 – 08:00

Cao trào

Hình ảnh và hành động

1. Camera quay trở lại Quả Trứng.
2. Một vết nứt xuất hiện.
3. Ánh sáng từ bên trong bắt đầu lọt ra.
4. Các vết nứt lan rộng.
5. Quả Trứng nở.
6. Ánh sáng phủ kín khung hình.

Dàn dựng

Không cần mô tả thứ bên trong Quả Trứng như một sinh vật có hình dạng cụ thể nếu tài liệu A và B chưa xác định.

Sự kiện quan trọng là Quả Trứng nở, không phải giới thiệu thêm một boss hoặc nhân vật.

Âm thanh

- Âm thanh nứt vỡ trầm.
- Âm thanh tăng dần khi ánh sáng lan rộng.
- Sau đó âm thanh bị cắt đột ngột.

Kết thúc footage

# NO SIGNAL

CONNECTION LOST

END OF NAM'S RECORDING

# PHẦN II — OUTRO CHANNY

Sau Video 9 · Ngoài game

Đây là phần Investigator Channy trở lại.

Lưu ý: Outro Channy là đoạn đã chốt và phải giữ nguyên văn. Không tự viết lại, thêm hoặc cắt câu. Trong phần dựng, chỉ cần đặt nguyên văn outro vào đúng vị trí sau `NO SIGNAL`.

### Nguyên văn outro (từ tài liệu gốc, mục V)

**[GÓC MÁY]:** *Cam quay cận mặt Channy. Đèn phòng tối nhẹ, chỉ có ánh sáng từ màn hình hắt lên mặt. Không nhạc nền, chỉ có tiếng quạt PC rì rầm.*

**CHANNY** *(Giọng trầm, tĩnh lặng, ngộ ra sự thật)*:
> "Đến đây thì... với đoạn thoại với `<System>` ở cuối Video 9, điều đó có nghĩa là...
> 
> Nam là tôi, và là các bạn ở kiếp sống trước.
> Người em trai gửi bức email ẩn danh này... cũng là tôi và là các bạn ở một chu kỳ khác.
> 
> Và quan trọng hơn cả..."

**[HÀNH ĐỘNG]:** *Channy ngước mắt nhìn thẳng trực diện vào ống kính máy quay, dừng lại 3 giây.*

**CHANNY** *(Giọng ấm áp, chân thành)*:
> "Tất cả các bạn... những người đang ngồi trước màn hình theo dõi video này... chúng ta đều là một linh hồn duy nhất đang nằm trong cùng một vỏ trứng, trải qua hàng ngàn kiếp sống dưới những Username khác nhau.
> 
> Các bạn đang xem tôi mổ xẻ một thế giới mà chính các bạn đã từng sống, từng xây, và từng bỏ quên trong những Save File cũ. 
> 
> Kênh YouTube này tồn tại không phải để giải một vụ án mất tích. Nó tồn tại để nhắc chúng ta nhớ rằng: Vòng lặp Samsara vẫn đang chạy."

**[HÀNH ĐỘNG]:** *Channy mỉm cười nhẹ, đưa tay bấm tắt màn hình máy tính. Ánh sáng trên mặt tắt phụt, phòng tối đi.*

**[ÂM THANH / HÌNH ẢNH CẮT CẢNH]:** *Màn hình chuyển sang đen tuyền hoàn toàn. Dòng chữ màu vàng nhẹ tự động hiện ra ở chính giữa màn hình:*

> **"Đằng sau nút 'Lưu & Thoát khỏi thế giới'... luôn luôn là nút 'Tạo thế giới mới'."**

*(Fade out dần trong 3 giây — Hết video)*

Câu kết đã xác nhận:

## “Đằng sau nút 'Lưu & Thoát khỏi thế giới'... luôn luôn là nút 'Tạo thế giới mới'.”

Câu này phải là điểm kết của phần outro, không phải câu kết của toàn bộ Project Samsara.

# PHẦN III — HẬU KẾT: CHU KỲ #2

Sau outro Channy

Vai trò: Đưa người xem quay trở lại Minecraft sau khi câu chuyện tưởng như đã kết thúc.

Không dùng nhạc cao trào. Không giới thiệu một mối đe dọa mới. Đoạn hậu kết cần bình thường đến mức khiến người xem phải tự nhận ra điều bất thường.

## CẢNH 07 — WORLD MỚI

Hình ảnh và hành động

1. Outro Channy kết thúc.
2. Màn hình chuyển sang giao diện Minecraft.
3. World cũ đổi tên thành:

`đừng mở lại`

4. Game tự tạo một world mới.
5. Tên world:

`Chu Kỳ #2`

6. Người chơi xuất hiện tại spawn.
7. Camera quan sát khu vực xung quanh.

Dàn dựng

Không cần giải thích tại sao game tự tạo world mới.

Không thêm thông báo về việc dữ liệu bị xóa hoặc một nhân vật mới xuất hiện.

Đây là một sự kiện hậu kết, không phải một phần gameplay mới.

## CẢNH 08 — CĂN NHÀ VẪN Ở ĐÓ

Hậu kết

Hình ảnh và hành động

1. Người chơi rời spawn.
2. Căn nhà Nam xuất hiện cách spawn 12 block.
3. Căn nhà vẫn ở đó.
4. Không có cối xay gió hay cánh đồng hướng dương xuất hiện trong cảnh này nếu không thuộc world mới đã chốt.
5. Camera hướng về căn nhà.
6. Không có nhân vật nào xuất hiện.

Mục đích

Ngôi nhà vẫn tồn tại sau khi chu kỳ mới bắt đầu. Người chơi có thể nhận ra nó, nhưng không hiểu vì sao mình lại có liên hệ với nơi này.

## CẢNH 09 — BIA MỘ

Hình ảnh kết

Hình ảnh và hành động

1. Camera quay về phía Bia Mộ.
2. Bia Mộ vẫn nằm cạnh spawn.
3. Bảng vẫn ghi:

`[Kẻ Đến Sau]`

`Cậu đến muộn rồi`

`Chỗ này...`

`vừa có người nằm`

4. Người chơi tiến đến rương bên cạnh.
5. Mở rương.
6. Trong rương chỉ còn Nhật Ký Nam.

Hoa khô không còn xuất hiện.

Dàn dựng

Không thêm vật phẩm mới vào rương.

Không để Nhật Ký tự mở.

Không tạo thêm lời nhắn từ Nam.

Sự vắng mặt của những vật phẩm cũ quan trọng hơn việc thêm một thông điệp mới.

## CẢNH 10 — LỜI NHẮN CUỐI

Kết thúc toàn bộ dự án

Hình ảnh và hành động

1. Người chơi đóng rương.
2. Camera quay lại căn nhà.
3. Màn hình giữ nguyên.
4. Một dòng chữ `<System>` xuất hiện.

SYSTEM MESSAGE · IN-GAME

## Ngôi nhà vẫn ở đó. Cậu vẫn không nhớ mình đã xây nó.

Âm thanh

- Không nhạc nền.
- Không tiếng thì thầm.
- Chỉ còn âm thanh môi trường Minecraft.

Kết thúc

Giữ hình ảnh thêm vài giây.

Sau đó cắt đen.

# PROJECT: SAMSARA

CYCLE #2

# Tổng kết cấu trúc kết thúc

| Phần         | Nội dung                                            | Vai trò                           |
| ------------ | --------------------------------------------------- | --------------------------------- |
| Video 9      | Đối thoại, bóng phản chiếu, Gargantua, Quả Trứng nở | Tiết lộ sự thật                   |
| Outro Channy | Lời kết của Investigator                            | Khép lại góc nhìn người làm video |
| Chu Kỳ #2    | World mới, căn nhà, Bia Mộ, Nhật Ký                 | Mở ra chu kỳ tiếp theo            |

## Kiểm tra các quy tắc bất khả xâm phạm

Thoại `<System>`

Không tự viết lại. Phải lấy nguyên văn từ tài liệu B.

Outro Channy

Giữ nguyên bản đã duyệt. Câu kết được đặt sau `NO SIGNAL`.

Chu Kỳ #2

Chỉ xuất hiện sau outro, không chen vào Video 9.

Không thêm lore mới

Không xác định hình dạng bên trong Quả Trứng, không thêm thực thể hay giải thích ngoài những gì đã chốt.

# Tổng kết toàn bộ Project Samsara — Video 1 đến 9

| Video | Tên                       | Trọng tâm                                        |
| ----- | ------------------------- | ------------------------------------------------ |
| 1     | `Visiting_Brother_World`  | Người em tìm thấy thế giới của Nam               |
| 2     | `The_Found_Journal`       | Nhật ký và Chu Kỳ #0                             |
| 3     | `The_Unknown_Observer`    | Camera và bóng người quan sát                    |
| 4     | `Observed_Nam_POV`        | Nam bắt đầu nhìn thấy thế giới qua camera        |
| 5     | `The_Last_Familiar_Place` | Căn nhà tháng 5/2018                             |
| 6     | `Gravity_Decay`           | Trọng lực và phương hướng biến dạng              |
| 7     | `The_7th_Sleep`           | Giấc ngủ thứ bảy và lối vào The Womb             |
| 8     | `Journey_To_The_Womb`     | Hành trình qua những ký ức                       |
| 9     | `The_Egg_Dialogue`        | Sự thật, Quả Trứng và sự khởi đầu của chu kỳ mới |

## Những phần còn cần đối chiếu trước khi khóa kịch bản

1. Thoại `<System>` Video 9: đã chèn nguyên văn (mục "Nguyên văn đoạn thoại" trong Video 9).
2. Outro Channy: đã chép toàn bộ bản chốt (mục "Nguyên văn outro" ở Phần II).
3. 16 nhãn save file: danh sách chính xác (tên + ngày) có trong `hien_trang_trong_mod.md`, mục 4.
4. TAPE 03: Chưa cần thiết phải xuất hiện trong Video 6; cơ chế và nội dung vẫn chưa được xác nhận.
5. Đề xuất sửa thoại nhắc Elysian Highlands: Vẫn chưa áp dụng. Nếu câu đó còn nằm trong thoại `<System>` Video 9, cần xử lý bằng một thay đổi tối thiểu và chờ bạn duyệt trước khi thay.

Một điểm quan trọng: Bản Video 9 ở trên là bản phân cảnh và dàn dựng, chưa phải bản khóa thoại. Tôi sẽ không tự điền những đoạn thoại gốc đang thiếu bằng cách phỏng đoán.