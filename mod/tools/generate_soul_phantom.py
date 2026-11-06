"""
Texture "Tàn Hồn Phantom": hình dạng thật của Phantom khi nhìn qua ống kính máy quay (SoulPhantomRenderer).
Một thân người gầy trơ xương, da tái xám xanh, hai tay dang làm xương cánh căng màng da rách.

  soul_phantom.png       128x64
  soul_phantom_eyes.png  128x64  CHỈ hai mắt (vẽ phát sáng -> quầng sáng shader)

Bố cục UV (khớp SoulPhantomModel):
  đầu         uv(  0, 0) 6x6x6      mặt trước (6..11, 6..11): hốc mắt tối, mắt ở (7,8) và (10,8)
  thân        uv( 24, 0) 6x3x10
  cánh tay    uv( 56, 0) 7x1x1
  cẳng tay    uv( 56, 4) 8x1x1
  ngón (nan)  uv( 56, 8) 5x1x1
  chân        uv(  0,16) 2x2x10
  màng trong  uv(  0,32) 8x0x10   (mặt trên 10..18, mặt dưới 18..26; hàng 32..41)
  màng ngoài  uv( 40,32) 9x0x8    (mặt trên 48..57, mặt dưới 57..66; hàng 32..39)
  màng đuôi   uv( 80,32) 4x0x8    (mặt trên 88..92, mặt dưới 92..96)

Chạy từ thư mục mod/:  python tools/generate_soul_phantom.py
"""
import os
import random

from PIL import Image

W, H = 128, 64
rng = random.Random(2013)

SKIN = [(0x4A, 0x4E, 0x5C), (0x5E, 0x63, 0x72), (0x74, 0x7B, 0x8A), (0x8E, 0x97, 0xA6)]
BONE = (0xB4, 0xBC, 0xC8)
DARK = (0x14, 0x14, 0x1C)
MEMBRANE = [(0x1A, 0x18, 0x24), (0x24, 0x20, 0x30), (0x30, 0x2A, 0x3E)]
VEIN = (0x4A, 0x3C, 0x5E)
EYE = (0xE8, 0xEE, 0xF5)

img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()


def skin(x0, y0, w, h, rib=False):
    for x in range(x0, x0 + w):
        for y in range(y0, y0 + h):
            c = SKIN[min(3, max(0, int(rng.gauss(1.4, 0.8))))]
            if rib and (y - y0) % 2 == 0 and rng.random() < 0.7:
                c = BONE if rng.random() < 0.4 else SKIN[3]       # xương sườn lộ ra
            px[x, y] = c + (255,)


def membrane(x0, y0, w, h, jag):
    """Màng da sẫm, gân tím, lỗ thủng; các hàng cuối (mép sau) rách lởm chởm."""
    cut = [h - rng.randint(0, jag) for _ in range(w)]
    for x in range(x0, x0 + w):
        for y in range(y0, y0 + h):
            if y - y0 >= cut[x - x0] or rng.random() < 0.05:
                continue
            c = rng.choice(MEMBRANE)
            if (x + 2 * (y - y0)) % 7 == 0:
                c = VEIN
            px[x, y] = c + (200,)


skin(0, 0, 24, 12)
for x in range(6, 12):                  # mặt: hốc mắt tối, gò má xương
    for y in range(6, 12):
        px[x, y] = (DARK if y in (7, 8, 9) and x in (6, 7, 8, 9, 10, 11) else SKIN[0]) + (255,)
for x in (7, 8, 9, 10):
    px[x, 11] = DARK + (255,)           # miệng há
skin(24, 0, 32, 13, rib=True)
for (u, v, w) in ((56, 0, 16), (56, 4, 18), (56, 8, 12)):
    for x in range(u, u + w):
        for y in range(v, v + 2):
            px[x, y] = BONE + (255,)
skin(0, 16, 24, 12)
membrane(0, 32, 36, 10, jag=4)
membrane(40, 32, 34, 8, jag=4)
membrane(80, 32, 24, 8, jag=3)

out = os.path.join("src", "main", "resources", "assets", "samsara", "textures", "entity")
os.makedirs(out, exist_ok=True)
img.save(os.path.join(out, "soul_phantom.png"))

eyes = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ep = eyes.load()
for ex in (7, 10):
    ep[ex, 8] = EYE + (255,)
eyes.save(os.path.join(out, "soul_phantom_eyes.png"))
print("Đã ghi soul_phantom.png và soul_phantom_eyes.png")
