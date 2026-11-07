"""
Lớp vỏ glitch (Bến Đò, The Womb): chính texture của vật bị "xé" như dữ liệu save đang hỏng.
Tham chiếu của người dùng (D:/c/*.gif): lát ngang bị xô lệch, tách kênh màu đỏ / xanh, vạch quét CRT, đổi kênh màu.
Không đặt như block thường: DockDrift phủ nó lên vật thể bằng block display phóng nhỉnh hơn khối gốc.

Sinh ra (chạy từ thư mục mod/:  python tools/generate_glitch.py):
  textures/block/womb_glitch_<0..7>.png (+ .png.mcmeta)   dải 32xN khung (texture gốc phóng 2 lần); khung 0 trong suốt
      chất liệu = biến thể / 2: 0 giấy dán tường (nam_wallpaper_0), 1 ván sồi, 2 len đỏ (giường), 3 gỗ vân sam bóc vỏ;
      mỗi chất liệu 2 nhịp (biến thể chẵn / lẻ) để các tấm cùng loại không giật cùng lúc.
  Mỗi khung giật: các đoạn ngang dài ngắn khác nhau, mép lởm chởm, xô lệch, mỗi đoạn một kiểu màu (tách kênh, đổi kênh,
  âm bản, phủ neon hồng / xanh lơ / vàng / xanh lá, sáng gắt), vạch quét tối; vài khối chữ nhật xô lệch kiểu datamosh;
  vài vạch neon mảnh đứt quãng. (Bản trước mỗi lát chạy hết bề ngang nên ra sọc đều: người dùng chê.)
  models/block/womb_glitch_<0..7>.json, blockstates/womb_glitch.json, tools/preview_glitch.png
Texture vanilla đọc thẳng từ minecraft-client.jar trong cache của Loom.
"""
import glob
import io
import json
import os
import random
import zipfile

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "samsara")
TEX = os.path.join(ROOT, "textures", "block")
MODELS = os.path.join(ROOT, "models", "block")
STATES = os.path.join(ROOT, "blockstates")
FRAMES = 14


def vanilla(name):
    jars = glob.glob(os.path.expanduser("~/.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar"))
    with zipfile.ZipFile(jars[0]) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")


def bases():
    return [Image.open(os.path.join(TEX, "nam_wallpaper_0.png")).convert("RGBA"),
            vanilla("oak_planks"), vanilla("red_wool"), vanilla("stripped_spruce_log")]


def clamp(v):
    return max(0, min(255, int(v)))


RES = 32                     # 32 px mỗi khung (texture gốc 16 px phóng 2 lần) để vệt glitch mảnh hơn
NEON = [(0xFF, 0x2E, 0xD0), (0x22, 0xF2, 0xFF), (0xFF, 0xE6, 0x3A), (0x5C, 0xFF, 0x6E), (0xF4, 0xF4, 0xF4)]


def recolor(rgb, mode, rng_tint):
    r, g, b = rgb
    if mode == "swap":
        return b, r, g
    if mode == "swap2":
        return g, b, r
    if mode == "invert":
        return 255 - r, 255 - g, 255 - b
    if mode == "tint":
        t = rng_tint
        lum = (r + g + b) / 3 / 255
        return t[0] * (0.45 + lum), t[1] * (0.45 + lum), t[2] * (0.45 + lum)
    if mode == "pop":
        return r * 1.5, g * 1.5, b * 1.5
    return r * 1.2, g * 1.2, b * 1.2


def glitch_frame(base, rng):
    """Các đoạn ngang dài ngắn khác nhau, lệch nhau, mỗi đoạn một kiểu màu; vài khối xô lệch kiểu datamosh;
    vài vạch mảnh neon đứt quãng. Phần còn lại trong suốt."""
    src = base.resize((RES, RES), Image.NEAREST)
    img = Image.new("RGBA", (RES, RES), (0, 0, 0, 0))

    def sample(x, y, dx, split):
        r = src.getpixel(((x - dx + split) % RES, y))[0]
        g = src.getpixel(((x - dx) % RES, y))[1]
        b = src.getpixel(((x - dx - split) % RES, y))[2]
        return r, g, b

    modes = ["split", "split", "swap", "swap2", "invert", "tint", "tint", "pop"]
    # Đoạn ngang: dài ngắn ngẫu nhiên
    for _ in range(rng.randint(3, 9)):
        y0 = rng.randrange(RES)
        h = rng.choice((1, 1, 1, 2, 2, 3, 4, 6))
        x0 = rng.randrange(RES)
        w = rng.choice((3, 5, 8, 12, 18, 26, RES))
        dx = rng.randint(-10, 10)
        split = rng.choice((1, 2, 2, 3))
        mode = rng.choice(modes)
        tint = rng.choice(NEON)
        for y in range(y0, min(RES, y0 + h)):
            dark = 0.7 if y % 2 else 1.0
            jitter = rng.randint(-2, 2)          # mép đoạn lởm chởm theo từng dòng
            for x in range(x0 + jitter, min(RES, x0 + w + jitter)):
                if x < 0:
                    continue
                c = recolor(sample(x, y, dx, split), mode, tint)
                img.putpixel((x, y), (clamp(c[0] * dark), clamp(c[1] * dark), clamp(c[2] * dark), 255))
    # Khối xô lệch (datamosh): một mảng chữ nhật dịch cả khối
    for _ in range(rng.randint(0, 2)):
        bw, bh = rng.randint(4, 14), rng.randint(3, 10)
        bx, by = rng.randrange(RES - bw), rng.randrange(RES - bh)
        dx, dy = rng.randint(-6, 6), rng.randint(-3, 3)
        mode = rng.choice(modes)
        tint = rng.choice(NEON)
        for y in range(by, by + bh):
            for x in range(bx, bx + bw):
                c = recolor(src.getpixel(((x - dx) % RES, (y - dy) % RES))[:3], mode, tint)
                img.putpixel((x, y), (clamp(c[0]), clamp(c[1]), clamp(c[2]), 255))
    # Vạch mảnh neon đứt quãng
    for _ in range(rng.randint(0, 3)):
        y = rng.randrange(RES)
        col = rng.choice(NEON)
        x = rng.randrange(RES)
        while x < RES:
            run = rng.randint(1, 6)
            for xx in range(x, min(RES, x + run)):
                img.putpixel((xx, y), col + (255,))
            x += run + rng.randint(1, 8)
    return img


def sequence(rng):
    frames = []
    for _ in range(16):
        frames.append({"index": 0, "time": rng.randint(12, 70)})
        for _ in range(rng.randint(2, 6)):
            frames.append({"index": rng.randint(1, FRAMES - 1), "time": rng.randint(1, 3)})
    return frames


def write_json(path, data):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    for d in (TEX, MODELS, STATES):
        os.makedirs(d, exist_ok=True)
    srcs = bases()
    count = len(srcs) * 2           # mỗi chất liệu 2 nhịp khác nhau: biến thể = chất liệu * 2 + k
    preview = Image.new("RGBA", (64 * FRAMES, 64 * count), (30, 30, 30, 255))
    variants = {}
    for v in range(count):
        base = srcs[v // 2]
        rng = random.Random(500 + v)
        strip = Image.new("RGBA", (RES, RES * FRAMES), (0, 0, 0, 0))
        preview.alpha_composite(base.resize((64, 64), Image.NEAREST), (0, v * 64))
        for i in range(1, FRAMES):
            f = glitch_frame(base, rng)
            strip.paste(f, (0, RES * i))
            preview.alpha_composite(f.resize((64, 64), Image.NEAREST), (i * 64, v * 64))
        name = f"womb_glitch_{v}"
        strip.save(os.path.join(TEX, name + ".png"))
        write_json(os.path.join(TEX, name + ".png.mcmeta"), {"animation": {"interpolate": False, "frames": sequence(rng)}})
        write_json(os.path.join(MODELS, name + ".json"), {
            "parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": f"samsara:block/{name}", "all": f"samsara:block/{name}"},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                          "faces": {fc: {"texture": "#all", "uv": [0, 0, 16, 16]}
                                    for fc in ("north", "south", "east", "west", "up", "down")}}]})
        variants[f"variant={v}"] = {"model": f"samsara:block/{name}"}
    write_json(os.path.join(STATES, "womb_glitch.json"), {"variants": variants})
    preview.save(os.path.join(HERE, "preview_glitch.png"))
    print("ok")


if __name__ == "__main__":
    main()
