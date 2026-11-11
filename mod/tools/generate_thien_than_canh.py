"""Sinh texture một chiếc cánh thiên nga cho Ophanim (assets/samsara/textures/misc/thien_than_canh.png).

Cánh phải, nhìn từ phía trước: vai ở bên trái (SHOULDER), sống cánh cong lên tới khuỷu (WRIST) rồi ra đầu mút (TIP).
Lông sơ cấp dài mọc ở đoạn khuỷu - đầu mút, lông thứ cấp rủ xuống dọc đoạn vai - khuỷu, hai hàng lông phủ xếp lớp
gần sống cánh. LoiDiNhapAngel đọc toạ độ vai và đường sống cánh (để gắn mắt) từ các hằng ở đây, nên đổi hình thì sửa
cả bên Java. Sửa ở đây rồi chạy lại, đừng sửa PNG.
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

SIZE = 512
SS = 2
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/samsara/textures/misc/thien_than_canh.png"

SHOULDER = (30, 300)
WRIST = (150, 80)
TIP = (410, 40)

WHITE = (252, 249, 249)
EDGE = (205, 194, 200)
SHADOW = (226, 216, 222)


def bezier(t):
    """Sống cánh: vai -> khuỷu -> đầu mút."""
    (x0, y0), (x1, y1), (x2, y2) = SHOULDER, WRIST, TIP
    u = 1 - t
    return (u * u * x0 + 2 * u * t * x1 + t * t * x2, u * u * y0 + 2 * u * t * y1 + t * t * y2)


def feather(d, base, angle, length, width, fill=WHITE):
    """Một chiếc lông: gốc ở base, chĩa theo angle (độ, 0 = sang phải, 90 = xuống), đầu tròn."""
    a = math.radians(angle)
    dx, dy = math.cos(a), math.sin(a)
    px, py = -dy, dx
    pts = []
    n = 24
    for k in range(n + 1):
        t = k / n
        half = width * (math.sin(math.pi * min(1.0, 0.10 + t * 0.92)) ** 0.4) * (1 - 0.12 * t)
        pts.append((base[0] + dx * length * t + px * half, base[1] + dy * length * t + py * half))
    for k in range(n, -1, -1):
        t = k / n
        half = width * (math.sin(math.pi * min(1.0, 0.10 + t * 0.92)) ** 0.4) * (1 - 0.12 * t)
        pts.append((base[0] + dx * length * t - px * half, base[1] + dy * length * t - py * half))
    pts = [(x * SS, y * SS) for x, y in pts]
    d.polygon(pts, fill=fill + (255,), outline=EDGE + (255,))
    d.line([(base[0] * SS, base[1] * SS), ((base[0] + dx * length * 0.9) * SS, (base[1] + dy * length * 0.9) * SS)],
           fill=SHADOW + (255,), width=SS)


def main():
    img = Image.new("RGBA", (SIZE * SS, SIZE * SS), WHITE + (0,))
    d = ImageDraw.Draw(img)

    # Lông thứ cấp: rủ xuống dọc đoạn vai - khuỷu, vẽ từ khuỷu về vai để chiếc gần vai nằm trên
    for k in range(16, -1, -1):
        t = 0.06 + k / 16 * 0.40
        base = bezier(t)
        feather(d, base, 96 - k * 1.2, 120 + (16 - k) * 3.5, 20)
    # Lông sơ cấp: dài, chĩa xuống chéo ra ngoài, xoè dần về đầu mút
    for k in range(13, -1, -1):
        t = 0.48 + k / 13 * 0.50
        base = bezier(t)
        feather(d, base, 72 - k * 6.0, 215 - k * 10, 21)
    # Hai hàng lông phủ xếp lớp gần sống cánh
    for row, (length, width, drop) in enumerate(((120, 20, 26), (70, 18, 8))):
        for k in range(22, -1, -1):
            t = 0.04 + k / 22 * 0.9
            x, y = bezier(t)
            feather(d, (x, y + drop), 92 - k * 1.8, length - k * 1.2, width, SHADOW if row == 0 else WHITE)
    # Sống cánh dày, bo tròn
    pts = [bezier(i / 40) for i in range(41)]
    d.line([(x * SS, y * SS) for x, y in pts], fill=WHITE + (255,), width=26 * SS, joint="curve")
    d.line([(x * SS, (y + 10) * SS) for x, y in pts], fill=EDGE + (255,), width=2 * SS)

    img = img.resize((SIZE, SIZE), Image.LANCZOS).filter(ImageFilter.SMOOTH)
    img.save(OUT)
    print(OUT)


main()
