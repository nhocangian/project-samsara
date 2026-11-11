"""Sinh texture đàn chim ở xa của tầng 2 (assets/samsara/textures/misc/chim_xa.png).

Hai hình bóng trắng cạnh nhau: nửa trái cánh giơ lên, nửa phải cánh hạ xuống. Sửa ở đây rồi chạy lại, đừng sửa PNG.
"""
from pathlib import Path

from PIL import Image, ImageDraw

W, H = 64, 16
SS = 4
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/samsara/textures/misc/chim_xa.png"
OUT.parent.mkdir(parents=True, exist_ok=True)
INK = (255, 255, 255, 255)   # trắng: màu từng con do LoiDiNhapBirds tô qua màu đỉnh

img = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))
d = ImageDraw.Draw(img)


def wing(cx, tip_dy):
    """Thân giữa khung, hai cánh mảnh chéo về hai bên."""
    y = 8 * SS
    tip = y + tip_dy * SS
    d.line([(cx - 14 * SS, tip), (cx - 6 * SS, y - tip_dy * SS // 3), (cx, y)], fill=INK, width=3 * SS, joint="curve")
    d.line([(cx + 14 * SS, tip), (cx + 6 * SS, y - tip_dy * SS // 3), (cx, y)], fill=INK, width=3 * SS, joint="curve")
    d.ellipse([cx - 2 * SS, y - 2 * SS, cx + 2 * SS, y + 2 * SS], fill=INK)


wing(16 * SS, -5)
wing(48 * SS, 5)
img.resize((W, H), Image.LANCZOS).save(OUT)
print(OUT)
