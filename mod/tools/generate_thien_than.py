"""Sinh texture cho Ophanim của tầng 2 (assets/samsara/textures/misc/thien_than_*.png).

  mat.png        lòng trắng hồng nhạt có gân máu
  mat_trong.png  tròng xanh có vân, con ngươi đen, một đốm sáng (lớp riêng để dời theo hướng nhìn)
  vanh.png  bề mặt vành: ngà ánh hồng vàng, có đường chạm khắc
  long.png       một chiếc lông vũ trắng to bản (chiều dọc là chiều dài lông)
Sửa ở đây rồi chạy lại, đừng sửa PNG.
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/samsara/textures/misc"
OUT.mkdir(parents=True, exist_ok=True)
random.seed(1998)


def eye():
    """Hai lớp: lòng trắng (mat.png) và tròng + con ngươi (mat_trong.png) để tròng dời được theo hướng nhìn."""
    s = 128
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = s / 2
    d.ellipse([4, 4, s - 4, s - 4], fill=(246, 228, 226, 255))
    for _ in range(16):
        a = random.uniform(0, math.tau)
        r0, r1 = s * 0.46, s * 0.26
        pts = [(c + math.cos(a) * r0, c + math.sin(a) * r0)]
        for k in range(1, 4):
            rr = r0 + (r1 - r0) * k / 3
            aa = a + random.uniform(-0.12, 0.12)
            pts.append((c + math.cos(aa) * rr, c + math.sin(aa) * rr))
        d.line(pts, fill=(214, 120, 128, 255), width=1)
    img.save(OUT / "thien_than_mat.png")

    iris = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(iris)
    ri = s * 0.48
    d.ellipse([c - ri, c - ri, c + ri, c + ri], fill=(52, 112, 196, 255))
    for k in range(64):
        a = k / 64 * math.tau
        col = (96, 166, 230, 255) if k % 2 else (28, 66, 146, 255)
        d.line([(c + math.cos(a) * ri * 0.35, c + math.sin(a) * ri * 0.35),
                (c + math.cos(a) * ri * 0.97, c + math.sin(a) * ri * 0.97)], fill=col, width=2)
    rp = s * 0.20
    d.ellipse([c - rp, c - rp, c + rp, c + rp], fill=(6, 6, 10, 255))
    d.ellipse([c + 10, c - rp * 1.0, c + 24, c - rp * 0.35], fill=(255, 255, 255, 255))
    iris.save(OUT / "thien_than_mat_trong.png")


def ring():
    w, h = 64, 16
    img = Image.new("RGBA", (w, h), (236, 214, 200, 255))
    d = ImageDraw.Draw(img)
    for x in range(w):
        for y in range(h):
            n = random.randint(-8, 8)
            r, g, b, _ = img.getpixel((x, y))
            img.putpixel((x, y), (r + n, g + n, b + n, 255))
    d.line([(0, 3), (w, 3)], fill=(200, 160, 140, 255))
    d.line([(0, h - 4), (w, h - 4)], fill=(200, 160, 140, 255))
    for x in range(4, w, 8):
        d.ellipse([x - 2, h / 2 - 2, x + 2, h / 2 + 2], outline=(196, 150, 128, 255))
    img.save(OUT / "thien_than_vanh.png")


def feather():
    """Lông to bản, đầu tròn, phiến lông có vân mềm."""
    w, h = 64, 128
    # Nền trong suốt cùng màu lông: làm mịn trên nền đen sẽ để lại viền đen quanh mỗi chiếc lông
    img = Image.new("RGBA", (w, h), (252, 248, 248, 0))
    d = ImageDraw.Draw(img)
    pts = []
    n = 60
    for k in range(n + 1):
        t = k / n
        half = (w / 2 - 2) * (math.sin(math.pi * min(1.0, 0.15 + t * 0.9)) ** 0.55) * (1 - 0.25 * t)
        pts.append((w / 2 + half, t * (h - 1)))
    for k in range(n, -1, -1):
        t = k / n
        half = (w / 2 - 2) * (math.sin(math.pi * min(1.0, 0.15 + t * 0.9)) ** 0.55) * (1 - 0.25 * t)
        pts.append((w / 2 - half, t * (h - 1)))
    # Viền xám rất nhạt để các chiếc lông chồng lên nhau vẫn tách ra được
    d.polygon(pts, fill=(252, 248, 248, 255), outline=(206, 194, 200, 255))
    for y in range(4, h - 4, 4):
        d.line([(w / 2, y), (6, y + 10)], fill=(236, 226, 230, 255))
        d.line([(w / 2, y), (w - 7, y + 10)], fill=(236, 226, 230, 255))
    d.line([(w / 2, 0), (w / 2, h - 8)], fill=(218, 196, 202, 255), width=2)
    img.filter(ImageFilter.SMOOTH_MORE).save(OUT / "thien_than_long.png")


eye()
ring()
feather()
print(OUT)
