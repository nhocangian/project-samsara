"""
Texture cho The Wanderer - "Lữ Khách Vô Danh" (theo concept art của dự án).

Áo choàng và mũ được dựng từ nhiều lớp VẠT VẢI (tấm phẳng) chồng lên nhau như vảy, mỗi vạt có
mép dưới rách lởm chởm (pixel trong suốt), nếp gấp dọc, mép trên sáng hơn chỗ vạt trên đè xuống,
và đầu sợi sờn sáng dần. Thân, đầu, tay là khối đặc tối bên trong.

  wanderer.png       256x128
  wanderer_eyes.png  256x128  CHỈ hai con mắt + quầng sáng mát (vẽ phát sáng)

Bảng màu: #0B0B10 #15151D #22222E #343446 #4A4A5E (vải), #9FB4C8 quầng sáng, #E8EEF5 mắt.

Bố cục UV (khớp TheWandererModel):
  đầu        uv(  0, 0) 8x8x8     mặt trước (8..15, 8..15) đen; mắt cao 2 px ở (10,12..13) và (13,12..13)
  mũ         uv( 32, 0) 10x10x10  mặt trước (42..51, 10..19) hở, chừa viền sáng
  thân trong uv( 72, 0) 7x18x4
  tay áo     uv( 96, 0) 4x12x4
  cẳng tay   uv(112, 0) 2x8x2
  móng       uv(120, 0) 1x5x1
  chân       uv(  0,16) 2x10x2
  VẠT VẢI (tấm phẳng w x h x 0 -> footprint 2w x h; nửa phải = mặt ngoài, nửa trái = mặt trong tối hơn):
    mũ     w4 h6   : (0,32) (8,32) (16,32)
    vai    w4 h10  : (24,32) (32,32) (40,32)
    giữa   w4 h14  : (48,32) (56,32) (64,32)
    vạt    w5 h17  : (72,32) (82,32) (92,32) (102,32)
    tay áo w3 h10  : (112,32) (118,32) (124,32)

Chạy từ thư mục mod/:  python tools/generate_wanderer.py
"""
import os
import random

from PIL import Image

W, H = 256, 128
rng = random.Random(1998)

VOID = (0x0B, 0x0B, 0x10)
CLOAK = (0x15, 0x15, 0x1D)
CHAR = (0x22, 0x22, 0x2E)
ASH = (0x34, 0x34, 0x46)
FADE = (0x4A, 0x4A, 0x5E)
GLOW = (0x9F, 0xB4, 0xC8)
EYE = (0xE8, 0xEE, 0xF5)
TONES = [VOID, CLOAK, CHAR, ASH, FADE]

img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()


def tone(level):
    return TONES[max(0, min(len(TONES) - 1, int(round(level))))]


def solid(x0, y0, w, h, base=1.2):
    """Khối đặc bên trong: tối, nếp dọc nhẹ."""
    for x in range(x0, x0 + w):
        col = rng.uniform(-0.5, 0.6)
        for y in range(y0, y0 + h):
            px[x, y] = tone(base + col + rng.uniform(-0.6, 0.6)) + (255,)


def panel(u, v, w, h, jag):
    """Một vạt vải: mặt ngoài ở (u..u+w), mặt trong ở (u+w..u+2w)."""
    lengths = []
    for c in range(w):
        lengths.append(h - rng.randint(0, jag))
    if rng.random() < 0.5:                     # một sợi dài lòi xuống
        lengths[rng.randrange(w)] = h
    for c in range(w):
        fold = rng.choice([0.9, 1.4, 2.0, 2.6])  # mỗi cột là một nếp
        L = lengths[c]
        for y in range(L):
            lvl = fold + rng.uniform(-0.5, 0.5)
            if y == 0:
                lvl += 0.9                          # mép trên sáng: chỗ vạt trên đè lên
            if y >= L - 2:
                lvl += 0.8                          # đầu sợi sờn
            if 0 < y < L - 2 and rng.random() < 0.04:
                continue                            # lỗ thủng nhỏ
            # Tấm phẳng hướng mặt ngoài về +z cục bộ = mặt "south" của cuboid (nửa phải); mặt trong (north) tối hơn
            px[u + 2 * w - 1 - c, v + y] = tone(lvl) + (255,)
            px[u + c, v + y] = tone(lvl - 1.0) + (255,)


# ---------------- Khối đặc ----------------
solid(0, 0, 32, 16)
for x in range(8, 16):
    for y in range(8, 16):
        px[x, y] = VOID + (255,)
solid(32, 0, 40, 20, base=1.6)
for x in range(42, 52):                         # mặt trước mũ: viền sáng, lòng hở
    for y in range(10, 20):
        edge = x in (42, 51) or y == 10
        px[x, y] = (ASH + (255,)) if edge else (0, 0, 0, 0)
solid(72, 0, 22, 22, base=1.0)
solid(96, 0, 16, 16, base=1.4)
solid(112, 0, 8, 10, base=2.4)
for x in range(120, 124):
    for y in range(0, 6):
        px[x, y] = (FADE if y < 4 else ASH) + (255,)
solid(0, 16, 8, 12, base=0.8)

# ---------------- Vạt vải ----------------
for u in (0, 8, 16):
    panel(u, 32, 4, 6, jag=2)
for u in (24, 32, 40):
    panel(u, 32, 4, 10, jag=3)
for u in (48, 56, 64):
    panel(u, 32, 4, 14, jag=5)
for u in (72, 82, 92, 102):
    panel(u, 32, 5, 17, jag=7)
for u in (112, 118, 124):
    panel(u, 32, 3, 10, jag=4)

out_dir = os.path.join("src", "main", "resources", "assets", "samsara", "textures", "entity")
os.makedirs(out_dir, exist_ok=True)
img.save(os.path.join(out_dir, "wanderer.png"))

eyes = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ep = eyes.load()
for ex in (10, 13):
    ep[ex, 12] = EYE + (255,)      # mắt cao 2 pixel, cả hai đều sáng hẳn
    ep[ex, 13] = EYE + (255,)
eyes.save(os.path.join(out_dir, "wanderer_eyes.png"))

preview = Image.new("RGBA", (136, 56), (200, 196, 186, 255))
preview.alpha_composite(img.crop((0, 0, 136, 56)))
preview.resize((136 * 5, 56 * 5), Image.NEAREST).save(os.path.join("tools", "preview_wanderer.png"))
print("Đã ghi wanderer.png, wanderer_eyes.png và tools/preview_wanderer.png")
