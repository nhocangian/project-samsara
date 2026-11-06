# Samsara Cosmic Shaders

Shaderpack Iris (GLSL 1.20) cho mod **Project: Samsara** (Minecraft 1.20.1, Fabric, Iris 1.7.6 + Sodium 0.5.13).
Phong cách: máy quay VHS / found footage, bầu trời vũ trụ cho The Womb, thế giới bẻ cong kiểu "Inception" cho Ký Ức Của Nam.

## Cấu trúc

```
shaders.properties          Tuỳ chọn chung (profile, screen)
shaders/
  dimension.properties      Gán chiều không gian -> thư mục chương trình
  item.properties           Gán item của mod -> heldItemId (1 = máy quay, 2 = Palimpsest Journal)
  lib/inception_curve.glsl  Hàm bẻ cong thế giới (dùng trong world2)
  *.vsh / *.fsh             Bộ gốc (bản vũ trụ; hiện không chiều nào dùng trực tiếp vì world0 bắt "*")
  world0/                   Overworld + mọi chiều không liệt kê (gồm Elysian Highlands)
  world1/                   The Womb (samsara:the_womb) + The End
  world-1/                  Nether
  world2/                   Ký Ức Của Nam (samsara:nam_memory): bản sao world0 + bẻ cong Inception
```

`dimension.properties`:
```
dimension.world2=samsara:nam_memory
dimension.world1=samsara:the_womb minecraft:the_end minecraft:end
dimension.world-1=minecraft:the_nether minecraft:nether
dimension.world0=minecraft:overworld *
```
Iris dùng NGUYÊN BỘ chương trình trong thư mục của chiều đó, nên mỗi thư mục `worldN/` phải đủ file.

## Các chương trình (giống nhau ở mọi thư mục)

| Chương trình | Việc làm |
|---|---|
| `gbuffers_terrain` | Block. Giữ màu vanilla (albedo × màu đỉnh × lightmap), chỉ thêm sương mù xa che ranh giới chunk. |
| `gbuffers_water` | Nước (world0 có hiệu ứng riêng). Màu nước lấy từ màu đỉnh, nên màu xám bạc Stage 2 của mod hiện được. |
| `gbuffers_entities`, `gbuffers_block` | Thực thể và block entity. |
| `gbuffers_hand` | Tay cầm (không bẻ cong). |
| `gbuffers_skybasic` | Bầu trời thủ tục (sao, cực quang, vết nứt trời). |
| `gbuffers_skytextured` | Mặt trời / mặt trăng. Mặt trời hố đen là texture do mod vẽ (WorldRendererMixin, blend ONE, ONE_MINUS_SRC_ALPHA, premultiplied alpha). |
| `final` | Hậu kỳ toàn màn hình: VHS khi cầm máy quay (`heldItemId == 1`), IR xanh lục khi `nightVision > 0`, glitch khi nhìn The Wanderer (`darknessFactor`), tách màu khi xuất hồn, khúc xạ dưới nước. |

## Quầng sáng mắt / vật phát sáng (mọi thư mục)

- `gbuffers_spidereyes` (lớp `RenderLayer.getEyes` của Minecraft) vẽ màu như vanilla vào `colortex0` và ghi thêm độ sáng
  vào **`colortex4`** (`/* DRAWBUFFERS:04 */`). Thứ dùng lớp này: mắt The Wanderer, khe nứt Quả Trứng, mắt Gargantua
  của bóng phản chiếu, mắt nhện / Enderman / Phantom.
- `lib/eye_glow.glsl` -> `eyeGlow(uv)`: làm loang `colortex4` (12 hướng x 2 vòng trên mipmap) thành quầng ánh lạnh
  `#9FB4C8`, `final.fsh` cộng vào ảnh (cả nhánh xuất thẳng lẫn nhánh hiệu ứng VHS). Chỉnh độ mạnh: `EYE_GLOW_STRENGTH`.
- `colortex4` được chọn để không đụng `colortex1/2` mà bloom của `world2` đang dùng.

## Bẻ cong Inception (world2)

`lib/inception_curve.glsl` đổi toạ độ đỉnh sang không gian người chơi (`gbufferModelViewInverse`),
nâng `y` thêm `khoảng_cách_ngang² × INCEPTION_CURVE` (kèm nhịp "thở" chậm theo `frameTimeCounter`),
rồi đổi ngược lại (`gbufferModelView`). Được gọi trong `world2/gbuffers_{terrain,water,entities,block}.vsh`:

```glsl
#include "/lib/inception_curve.glsl"
vec4 curvedView = inceptionCurve(gl_ModelViewMatrix * gl_Vertex);
gl_Position = gl_ProjectionMatrix * curvedView;
viewPos = curvedView.xyz;
```

- `INCEPTION_CURVE` mặc định `0.0045` (64 block xa -> cong lên ~18 block). Tăng để bát sâu hơn.
- Sương mù trong `world2/*.fsh` được đẩy ra xa (`smoothstep(far*0.80, far*1.10, dist)`) để thấy độ cong.
- Bầu trời và tay cầm không bị bẻ cong.

## Bố cục chiều Ký Ức Của Nam (để chỉnh shader cho khớp)

- Thế giới phẳng, mặt cỏ ở y = 60, luôn là giữa trưa (`fixed_time` 6000), không mưa.
- Nhà Nam ở quanh (0, 0); con đường mòn rộng 3 block chạy thẳng về +Z; người chơi xuất hiện ở (0, 61, 110) nhìn về phía bắc (-Z).
- Phủ kín block `samsara:memory_sunflower` (model hoa hướng dương vanilla, xoay về phía nam).

## Ràng buộc với mod

- `item.properties` phải giữ `item.1 = samsara:fractured_slate` (máy quay) vì `final.fsh` dựa vào nó.
- Mặt trời hố đen chỉ hiện khi đang ngắm qua máy quay; logic này nằm trong mod, không nằm trong shader.
- Trong môi trường dev, `mod/run/shaderpacks/Samsara-Cosmic-Shaders` là junction tới thư mục này: sửa xong chỉ cần tải lại shader trong game (phím R trong màn hình Shader Packs, hoặc tắt/bật shader).
