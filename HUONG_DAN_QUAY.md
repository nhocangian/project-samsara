# Hướng dẫn quay video (Project Samsara)

Bản ghi nhớ nhanh trước mỗi buổi quay. Chi tiết kỹ thuật ở `HANDOFF.md`, kịch bản từng cảnh ở `scripts/master_production_document_project_samsara_revision.md`.

## 1. Mở game đúng vai

| Video | Vai | Lệnh mở game |
|---|---|---|
| 1, 2, 3 | Người em | `start_samsara.bat <tên người em>` |
| 4 trở đi | Nam | `start_samsara.bat Nam_1998` |

- Không ghi tên thì game tự đặt tên ngẫu nhiên kiểu `Player123` (lộ trên chat và trên tấm biển mang tên mình).
- Mỗi tên là một người chơi riêng trong world (túi đồ, tiến trình riêng).

## 2. World để quay

- Tạo **world mới**, nên chọn seed có chỗ xuất hiện ở **đồng bằng**: khu nhà của Nam tự dựng quanh spawn lần đầu vào game (nhà gỗ, ruộng, chuồng, hầm mỏ, góc phù phép, cổng địa ngục, Bia Mộ). Lần đầu vào sẽ khựng vài giây, chỉ một lần.
- Dùng một world cho cả loạt video (căn nhà, Bia Mộ, khu vực đều là một).

## 3. Trước khi bật OBS: gõ lệnh chuẩn bị

| Lệnh | Dùng cho | Trạng thái được đặt |
|---|---|---|
| `/samsara quay video1` | Video 1 `Visiting_Brother_World` | Stage 4 (7 lần ngủ), sáng sớm. Máy quay sẵn ở ô số 3. Bia Mộ dựng lại, rương còn nguyên nhật ký. Đứng ở spawn, nhìn về căn nhà. |
| `/samsara quay video2` | Video 2 `The_Found_Journal` | Stage 4, sáng sớm. Cầm sẵn nhật ký (ô 1) và máy quay (ô 3). Đứng trong nhà. |
| `/samsara quay video3` | Video 3 `The_Unknown_Observer` | Stage 4, giữa buổi sáng. Cầm nhật ký và máy quay. Đứng ở spawn. |
| `/samsara quay video4` | Video 4 `Observed_Nam_POV` | Stage 2 (3 lần ngủ), chiều muộn. **Chưa có máy quay**, chưa thức đêm nào. Đứng trước cửa nhà. Máy quay tự tới sau đêm thức trắng thứ ba. |
| `/samsara quay tat` | Sau khi quay xong | Tắt chế độ quay, mọi thứ chạy lại như thường. |

Lệnh nào cũng: chuyển về sinh tồn, hồi đầy máu và đói, dọn sạch túi đồ, trời quang, **xoá khung chat** (chữ của lệnh không lọt vào hình), và bật **chế độ quay**: các hiện tượng ngẫu nhiên đứng yên, chỉ xảy ra khi bấm phím đạo diễn. Hiệu ứng của stage (block bay lên trời, nước xám, Phantom) vẫn chạy như kịch bản.

## 4. Phím đạo diễn (bấm lúc đang quay, không hiện chữ gì)

Mặc định ở bàn phím số bên phải (numpad); đổi trong **Tuỳ chọn → Điều khiển → Phím → nhóm "Samsara: đạo diễn"**.

| Phím | Cảnh | Ghi chú |
|---|---|---|
| Numpad 1 | Bước chân dội lại sau lưng | Đang ở Overworld |
| Numpad 2 | Quái đang đuổi đứng khựng nhìn mình | Cần quái ở gần |
| Numpad 3 | Hướng dương ngoài tầm mắt quay về phía mình | Cần hướng dương gần (trước lán đất có 5 cây) |
| Numpad 4 | Cả đàn con vật quay lại nhìn | Cần con vật gần (chuồng) |
| Numpad 5 | Khối lỗi phiên bản (một khối nhá texture Alpha) | Khối trong tầm nhìn cách 10–28 khối |
| Numpad 6 | Biển mang tên mình cắm sau lưng | Cần đất trống sau lưng |
| Numpad 7 | Tiếng cuốc đáp lại trong vách đá | Đang ở dưới hang (dưới Y 50) |
| Numpad 8 | Đuốc tự có ở đoạn hang phía trước | Đang ở dưới hang |
| Numpad 9 | Đường hầm nhánh lạ mở trong vách | Đang ở dưới hang |
| Numpad 0 | `Nam_1998 joined the game` + *"em đang ở đâu"* | Video 3 cảnh 6 (cần Stage 3 trở lên mới có câu chat) |
| Numpad . | `<System>` cài máy quay vào ô số 3 | Video 4 cảnh 4, nếu không muốn chờ đêm thứ ba |

## 5. Quy trình một buổi quay

1. `start_samsara.bat <tên>` (đúng vai).
2. Vào world quay, gõ `/samsara quay videoN`.
3. Bật OBS, quay theo kịch bản; bấm phím đạo diễn ở cảnh cần.
4. Quay xong: `/samsara quay tat` (hoặc chuẩn bị video kế tiếp).

## 6. Lưu ý

- Mắt thường luôn thấy mặt trời bình thường; hố đen chỉ hiện khi **ngắm qua máy quay** (chuột phải) hoặc trong ảnh chụp F2.
- Video 1 không đụng tới máy quay, không có Nam_1998 trong chat (chế độ quay đã chặn lượt ngẫu nhiên).
- Hầm mỏ của Nam xuống tới Y -50 (lối xuống có mái đá cuội ở góc tây nam khu nhà, cầu thang xoắn): chỗ hợp để quay các cảnh dưới hang.
