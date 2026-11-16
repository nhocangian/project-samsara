"""Texture cho "khối lỗi phiên bản" ở Overworld (system/overworld/VersionGlitch, client/overworld/VersionGlitchRenderer).

Đọc các texture Alpha `<tên>_s.png` mà `import_alpha_textures.py` đã nhập cho tầng 4 (quy ước kênh alpha của shader:
255 = màu thường, 254 = màu cỏ/lá đã chia ngược cho màu biome và ghi nửa giá trị, 2 = trong suốt) rồi ghi ra bản vẽ
thẳng được, không cần shader: `assets/samsara/textures/overworld/version_glitch/<tên>.png`.
  - alpha 254: nhân đôi rồi nhân lại màu biome đồng bằng (ra đúng màu Golden Days gốc), alpha 255;
  - alpha 2: trong suốt hẳn (alpha 0, lớp cutout bỏ đi);
  - còn lại giữ nguyên.
Chạy lại sau mỗi lần chạy `import_alpha_textures.py`. Không sửa PNG bằng tay.
"""
from pathlib import Path

from PIL import Image

MOD = Path(__file__).resolve().parent.parent
SRC = MOD / "src/main/resources/assets/minecraft/textures/block"
OUT = MOD / "src/main/resources/assets/samsara/textures/overworld/version_glitch"

# Màu biome đồng bằng, giống TINTS trong import_alpha_textures.py
GRASS = (145, 189, 89)
FOLIAGE = (119, 171, 47)
TINT = {"grass_block_top": GRASS, "oak_leaves": FOLIAGE}

NAMES = ["stone", "cobblestone", "dirt", "gravel", "sand", "oak_log", "oak_log_top", "oak_planks", "coal_ore",
         "iron_ore", "crafting_table_side", "crafting_table_top", "grass_block_top", "grass_block_side", "oak_leaves"]


def convert(name):
    im = Image.open(SRC / f"{name}_s.png").convert("RGBA")
    px = im.load()
    tint = TINT.get(name)
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 2:
                px[x, y] = (0, 0, 0, 0)
            elif a == 254:
                t = tint or (255, 255, 255)
                px[x, y] = tuple(min(255, c * 2 * k // 255) for c, k in zip((r, g, b), t)) + (255,)
    return im


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name in NAMES:
        convert(name).save(OUT / f"{name}.png")
        print(name)


if __name__ == "__main__":
    main()
