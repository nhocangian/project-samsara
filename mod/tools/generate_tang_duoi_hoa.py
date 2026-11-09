"""
Tầng Dưới: hoa hướng dương khô (ngã 3) và cánh hoa (hạt trôi trên nước). Chạy từ thư mục mod/:
    python tools/generate_tang_duoi_hoa.py [đường dẫn minecraft-client.jar]

Hoa khô: lấy texture và model hoa hướng dương vanilla trong jar, đổi sang nâu héo.
"""
import colorsys
import glob
import io
import json
import os
import random
import sys
import zipfile

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "samsara")
TEX = os.path.join(ROOT, "textures", "block")
MODELS = os.path.join(ROOT, "models", "block")
STATES = os.path.join(ROOT, "blockstates")
PART_TEX = os.path.join(ROOT, "textures", "particle")
PART = os.path.join(ROOT, "particles")
NAMES = ["sunflower_back", "sunflower_front", "sunflower_top", "sunflower_bottom"]


def wilt(img):
    img = img.convert("RGBA")
    p = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = p[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            # Kéo về nâu xám, tối đi, giữ lại các đốm sáng tối để còn nhận ra hình hoa
            h = 0.08 + (h - 0.08) * 0.12
            s = min(s * 0.55, 0.5)
            v = v * 0.5
            nr, ng, nb = colorsys.hsv_to_rgb(h, s, v)
            p[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    return img


def main():
    jar = sys.argv[1] if len(sys.argv) > 1 else glob.glob(
        os.path.expanduser("~/.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar"))[0]
    with zipfile.ZipFile(jar) as z:
        for n in NAMES:
            data = z.read(f"assets/minecraft/textures/block/{n}.png")
            wilt(Image.open(io.BytesIO(data))).save(os.path.join(TEX, n.replace("sunflower", "hoa_kho") + ".png"))
        for part in ("sunflower_top", "sunflower_bottom"):
            model = json.loads(z.read(f"assets/minecraft/models/block/{part}.json"))
            for k, v in list(model["textures"].items()):
                if "sunflower" in v:
                    model["textures"][k] = "samsara:block/" + v.split("/")[-1].replace("sunflower", "hoa_kho")
            with open(os.path.join(MODELS, part.replace("sunflower", "hoa_kho") + ".json"), "w", encoding="utf8", newline="\n") as f:
                json.dump(model, f, indent=2)
                f.write("\n")

    variants = {}
    for facing, rot in [("east", 0), ("south", 90), ("west", 180), ("north", 270)]:
        for half, m in [("lower", "hoa_kho_bottom"), ("upper", "hoa_kho_top")]:
            v = {"model": "samsara:block/" + m}
            if rot:
                v["y"] = rot
            variants[f"facing={facing},half={half}"] = v
    with open(os.path.join(STATES, "hoa_kho.json"), "w", encoding="utf8", newline="\n") as f:
        json.dump({"variants": variants}, f, indent=2)
        f.write("\n")

    # Cánh hoa: 3 biến thể 8x8
    os.makedirs(PART_TEX, exist_ok=True)
    os.makedirs(PART, exist_ok=True)
    names = []
    for i in range(3):
        r = random.Random(5100 + i)
        img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        p = img.load()
        for y in range(1, 7):
            w = {1: 1, 2: 2, 3: 3, 4: 3, 5: 2, 6: 1}[y] + r.choice([0, 0, 1])
            x0 = 4 - w // 2
            for x in range(x0, x0 + w):
                t = r.uniform(-14, 14)
                p[x, y] = (int(238 + t), int(186 + t), int(46 + t), 255)
        name = f"canh_hoa_{i}"
        img.save(os.path.join(PART_TEX, name + ".png"))
        names.append("samsara:" + name)
    with open(os.path.join(PART, "canh_hoa.json"), "w", encoding="utf8", newline="\n") as f:
        json.dump({"textures": names}, f, indent=2)
        f.write("\n")


if __name__ == "__main__":
    main()
