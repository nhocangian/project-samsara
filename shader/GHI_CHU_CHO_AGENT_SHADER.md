# Ghi chú cho agent làm shader

*Phiên làm mod để lại, 06/11/2026. Đưa file này cho agent shader rồi xoá mục nào đã làm.*

---

## 1. Shader đổi nhẹ khi bóng phản chiếu đạt cỡ cực đại (×45)

**Bối cảnh (đoạn kết, Video 9, cảnh 05 "Gargantua là con mắt"):**
- Đoạn kết diễn ra ở **The Womb** (`samsara:the_womb` = thư mục **`world1`**).
- Bóng phản chiếu của người chơi (hình dạng của `<System>`) nhảy cỡ ×2 → ×4 → ×12 → **×45**.
- Ở ×45, mod canh vị trí để **con mắt của nó nằm đúng chỗ Gargantua trên trời**, hướng `normalize(vec3(0.0, 0.54, -0.84))` (bắc, cao ~33°). Hướng này phải khớp `bhForward` trong `world1/gbuffers_skybasic.fsh`. Nếu shader đổi hướng hố đen thì báo phiên mod để sửa `EndingSequenceClient.GARGANTUA_DIR`.
- Lúc đó trên màn hình là một bóng người khổng lồ thân làm bằng trời sao, viền tím mờ, che gần hết bầu trời phía bắc; texture hố đen (sun_stage4) dán ở chỗ con mắt.

**Tín hiệu mod gửi sang shader: uniform `rainStrength`** (uniform có sẵn của Iris)
- Bình thường = `0.0`. Mọi biome của The Womb không có mưa, nên ở `world1` uniform này **chỉ** dùng làm tín hiệu.
- Khi con mắt lộ ra (cú nhảy lên ×45 xong), mod tăng đều `0 → 1` trong khoảng **2 giây**, rồi giữ ở `1.0` tới hết đoạn kết (nhắm mắt, trứng nở, màn NO SIGNAL).
- Về lại `0` khi thoát world hoặc tắt lệnh xem trước.
- Có thể dùng `wetness` nếu muốn bản đã làm mượt sẵn.
- Chỉ đọc ở `world1`. Các world khác có mưa thật nên đừng dùng `rainStrength` làm tín hiệu ở đó.

**Muốn shader làm gì:** người dùng muốn "tới kích thước cực đại shader sẽ thay đổi một tí". Phần thiết kế để agent shader bàn với người dùng. Vài gợi ý cho hợp cảnh:
- Gargantua "mở mắt": vành bồi tụ sáng hơn hoặc ấm hơn, con ngươi (bóng tối giữa) khẽ co giãn theo nhịp thở chậm.
- Hậu kỳ trong `world1/final.fsh`: viền tối (vignette) dày lên, màu ngả hổ phách, băng VHS hơi xuống cấp (nhoè, sọc nhẹ). Kiềm chế, đúng tinh thần "kinh dị kiềm chế".
- Đừng che hay làm mờ ô chat: đoạn thoại `<System>` vẫn đang chạy trong ô chat lúc đó.

**Thử trong game:**
1. `/samsara womb` (vào The Womb).
2. `/samsara test mirror 12`, rồi `/samsara test mirror 45`: bóng nhảy lên ×45 trước mặt, `rainStrength` tăng lên 1. Quay mặt về phía bắc và ngước lên để thấy con mắt trùng Gargantua.
3. `/samsara test mirror off` để tắt (tín hiệu về 0).
4. Chạy trọn đoạn kết thật: `/samsara test ending` (khoảng 3–4 phút).

## 2. Kiểm tra cách Iris vẽ hai lớp mới trên bóng phản chiếu

Bóng phản chiếu giờ được vẽ bằng hai kiểu vẽ mà shaderpack trước đây chưa gặp trên entity:
- **Thân:** `RenderLayer.getEndPortal()` (lớp cổng End). Không có shader thì thân là trời sao chuyển động trong dáng người. Khi bật shaderpack, cần xem Iris đưa lớp này qua chương trình nào (thường là `gbuffers_block` hoặc chương trình riêng của cổng End) và nó còn ra trời sao không. Nếu ra mảng màu phẳng hoặc đen kịt thì nên xử lý trong shader để thân vẫn là trời sao.
- **Viền sáng:** `RenderLayer.getEnergySwirl(...)` (kiểu creeper tích điện, cộng sáng), màu tím, hơi phồng ra ngoài thân. Kiểm tra nó không bị `gbuffers_entities` làm tối mất.
- Chỗ con mắt vẫn dùng `RenderLayer.getEyes(sun_stage4)`, nên đi qua `gbuffers_spidereyes` → quầng sáng `lib/eye_glow.glsl` như cũ.
