"""
Texture máy quay 3D khi cầm (CamcorderItemModel / CamcorderItemRenderer).

  camcorder.png   64x64   (assets/samsara/textures/item/camcorder.png)

Mỗi khối dùng bố cục UV hộp kiểu entity (u, v, dx, dy, dz). Toạ độ entity: model vẽ qua scale(-1,-1,1),
nên trong ảnh: hàng trên của mỗi mặt bên = phía TRÊN thật của máy; mặt "minX" = bên PHẢI máy (phía dây đeo),
mặt "maxX" = bên TRÁI (phía màn LCD), "minZ" = mặt trước (ống kính), "maxZ" = mặt sau.

  thân máy    (  0, 0)  6x5x9     nắp băng   (  0,24) 5x1x5
  nòng kính   ( 32, 0)  4x4x3     kính ngắm  ( 22,24) 2x2x2
  loa che     ( 48, 0)  5x5x1     cao su mắt ( 32,24) 3x3x1
  màn LCD     ( 32, 8)  1x4x5     dây đeo    (  0,32) 1x3x6
  mặt màn     ( 46, 8)  0x3x4     pin        ( 16,32) 4x3x1
  micro       (  0,16)  2x1x3     đèn REC    ( 12,16) 1x1x1 (trắng, renderer tô màu)
  đèn IR      ( 16,16)  1x1x1     cuộn băng  ( 20,16) 4x0x3

Chạy từ thư mục mod/:  python tools/generate_camcorder.py
"""
import os
import random

from PIL import Image

W, H = 64, 64
rng = random.Random(1998)

PLASTIC = [(0x1C, 0x1D, 0x21), (0x22, 0x23, 0x28), (0x27, 0x29, 0x2E)]
DARK = (0x0E, 0x0E, 0x11)
TRIM = (0x8C, 0x90, 0x98)
TRIM_DARK = (0x5A, 0x5E, 0x66)
GREY = [(0x3A, 0x3C, 0x42), (0x44, 0x46, 0x4D)]
RUBBER = [(0x12, 0x12, 0x14), (0x18, 0x18, 0x1B)]
GLASS = [(0x10, 0x14, 0x24), (0x18, 0x1E, 0x36), (0x22, 0x2A, 0x4A)]
GLINT = (0x9A, 0xA8, 0xE0)
FABRIC = [(0x20, 0x1A, 0x16), (0x2A, 0x22, 0x1C)]
STITCH = (0x6A, 0x60, 0x54)
RED_DOT = (0xC0, 0x18, 0x18)

img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()


def put(x, y, c, a=255):
    px[x, y] = tuple(c[:3]) + (a,)


def fill(x0, y0, w, h, palette):
    for x in range(x0, x0 + w):
        for y in range(y0, y0 + h):
            put(x, y, rng.choice(palette) if isinstance(palette, list) else palette)


def faces(u, v, dx, dy, dz):
    """Vùng UV các mặt: top, bottom, right(minX), front(minZ), left(maxX), back(maxZ) -> (x, y, w, h)."""
    return {
        "top": (u + dz, v, dx, dz),
        "bottom": (u + dz + dx, v, dx, dz),
        "right": (u, v + dz, dz, dy),
        "front": (u + dz, v + dz, dx, dy),
        "left": (u + dz + dx, v + dz, dz, dy),
        "back": (u + 2 * dz + dx, v + dz, dx, dy),
    }


def box(u, v, dx, dy, dz, palette):
    for (x, y, w, h) in faces(u, v, dx, dy, dz).values():
        fill(x, y, w, h, palette)
    return faces(u, v, dx, dy, dz)


# ---------------------------------------------------------------- thân máy
f = box(0, 0, 6, 5, 9, PLASTIC)
# mặt trên: khe tản nhiệt chạy dọc phía sau, nút zoom bạc phía trước
x, y, w, h = f["top"]
for yy in range(y + 5, y + h - 1):
    for xx in (x + 1, x + 3):
        put(xx, yy, DARK)
put(x + 4, y + 1, TRIM)
put(x + 4, y + 2, TRIM_DARK)
# hai bên: sọc viền bạc ở mép dưới
for side in ("left", "right"):
    x, y, w, h = f[side]
    for xx in range(x, x + w):
        put(xx, y + h - 2, TRIM_DARK if (xx % 3) else TRIM)
# bên phải: vân bám tay dưới dây đeo
x, y, w, h = f["right"]
for xx in range(x + 2, x + w - 2):
    for yy in range(y + 1, y + 3):
        if (xx + yy) % 2 == 0:
            put(xx, yy, RUBBER[0])
# bên trái: hõm chứa màn LCD + lỗ loa
x, y, w, h = f["left"]
for xx in range(x + 1, x + 6):
    for yy in range(y, y + 4):
        put(xx, yy, DARK)
for xx in range(x + 7, x + 9):
    put(xx, y + 1, RUBBER[0])
    put(xx, y + 2, RUBBER[0])
# mặt trước: viền tối quanh ống kính
x, y, w, h = f["front"]
fill(x + 1, y, 4, 4, DARK)
# mặt sau: tối hơn, chấm đèn sạc
x, y, w, h = f["back"]
fill(x, y, w, h, RUBBER)
put(x + 5, y + 4, (0x30, 0x60, 0x30))

# ---------------------------------------------------------------- nòng kính: vòng lấy nét có gân
f = box(32, 0, 4, 4, 3, PLASTIC)
for side in ("left", "right", "top", "bottom"):
    x, y, w, h = f[side]
    for xx in range(x, x + w):
        for yy in range(y, y + h):
            put(xx, yy, RUBBER[1] if ((xx if side in ("left", "right") else yy) % 2) else (0x30, 0x31, 0x36))

# ---------------------------------------------------------------- loa che + mặt kính
f = box(48, 0, 5, 5, 1, RUBBER)
x, y, w, h = f["front"]
fill(x, y, w, h, DARK)
fill(x + 1, y + 1, 3, 3, GLASS)
put(x + 2, y + 2, GLASS[0])
put(x + 1, y + 1, GLINT)
put(x + 3, y + 1, TRIM_DARK)       # chấm phản xạ thứ hai mờ

# ---------------------------------------------------------------- màn LCD (vỏ)
f = box(32, 8, 1, 4, 5, GREY)
x, y, w, h = f["left"]               # mặt ngoài khi gập
for xx in range(x, x + w):
    put(xx, y, TRIM_DARK)
put(x + 1, y + 2, TRIM)
put(x + 2, y + 2, TRIM)
x, y, w, h = f["right"]              # mặt trong: viền đen quanh màn
fill(x, y, w, h, DARK)

# ---------------------------------------------------------------- mặt màn: ảnh nhiễu xám có vạch quét (renderer tô màu)
f = faces(46, 8, 0, 3, 4)
for key in ("right", "left"):
    x, y, w, h = f[key]
    for xx in range(x, x + w):
        for yy in range(y, y + h):
            g = 150 + int(rng.gauss(0, 30)) - (25 if (yy - y) % 2 else 0)
            g = max(60, min(235, g))
            put(xx, yy, (g, g, g))
    put(x, y, RED_DOT)               # chấm REC ở góc màn

# ---------------------------------------------------------------- micro: lưới
f = box(0, 16, 2, 1, 3, GREY)
x, y, w, h = f["top"]
for xx in range(x, x + w):
    for yy in range(y, y + h):
        put(xx, yy, DARK if (xx + yy) % 2 else GREY[1])
x, y, w, h = f["front"]
fill(x, y, w, h, DARK)

# ---------------------------------------------------------------- đèn REC / IR: trắng để tô màu
box(12, 16, 1, 1, 1, [(0xF0, 0xF0, 0xF0)])
box(16, 16, 1, 1, 1, [(0xE0, 0xE0, 0xE0)])

# ---------------------------------------------------------------- cuộn băng (thấy khi mở nắp)
f = faces(20, 16, 4, 0, 3)
for key in ("top", "bottom"):
    x, y, w, h = f[key]
    fill(x, y, w, h, DARK)
    for rx in (x, x + 3):
        put(rx, y + 1, (0xD8, 0xD4, 0xC8))
    put(x + 1, y + 1, (0x3A, 0x28, 0x1A))      # dải băng nâu giữa hai cuộn
    put(x + 2, y + 1, (0x3A, 0x28, 0x1A))
    put(x + 1, y + 2, (0xB0, 0xA8, 0x90))      # nhãn băng
    put(x + 2, y + 2, (0xB0, 0xA8, 0x90))

# ---------------------------------------------------------------- nắp băng: xám nhạt, gân, mũi tên EJECT
f = box(0, 24, 5, 1, 5, GREY)
x, y, w, h = f["top"]
for yy in range(y, y + h):
    put(x, yy, TRIM_DARK)
    put(x + w - 1, yy, TRIM_DARK)
put(x + 2, y + 1, TRIM)
put(x + 1, y + 2, TRIM)
put(x + 2, y + 2, TRIM)
put(x + 3, y + 2, TRIM)
x, y, w, h = f["bottom"]
fill(x, y, w, h, DARK)

# ---------------------------------------------------------------- kính ngắm + cao su mắt
box(22, 24, 2, 2, 2, PLASTIC)
f = box(32, 24, 3, 3, 1, RUBBER)
x, y, w, h = f["back"]
fill(x, y, w, h, RUBBER[0])
put(x + 1, y + 1, (0x14, 0x22, 0x18))           # thị kính lờ mờ xanh

# ---------------------------------------------------------------- dây đeo vải có đường chỉ
f = box(0, 32, 1, 3, 6, FABRIC)
for side in ("left", "right", "top", "bottom"):
    x, y, w, h = f[side]
    for xx in range(x, x + w):
        if xx % 2 == 0:
            put(xx, y, STITCH)
            put(xx, y + h - 1, STITCH)

# ---------------------------------------------------------------- pin: nhãn xanh
f = box(16, 32, 4, 3, 1, RUBBER)
x, y, w, h = f["back"]
for xx in range(x, x + w):
    put(xx, y + 1, (0x2C, 0x5A, 0x3A))
put(x, y + 2, TRIM_DARK)

out = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "samsara", "textures", "item", "camcorder.png")
img.save(out)
img.resize((W * 8, H * 8), Image.NEAREST).save(os.path.join(os.path.dirname(__file__), "preview_camcorder.png"))
print("wrote", os.path.normpath(out))
