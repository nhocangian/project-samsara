"""
Tượng Quỳ (chương III của The Womb, "Những Người Khổng Lồ"): tượng thạch cao trắng cỡ người, quỳ trên mặt nước,
hàng trăm bức cùng quay về Gargantua. Mỗi bức là một lần "bạn" đã chết.

Sinh ra (chạy từ thư mục mod/:  python tools/generate_memory_statue.py):
  textures/block/memory_statue.png          16x16  thạch cao trắng ngà, vết rạn nhỏ
  models/block/memory_statue_up.json        quỳ thẳng, ngửa mặt nhìn lên (pose 0)
  models/block/memory_statue_bowed.json     quỳ, cúi đầu, lưng hơi gập (pose 1)
  models/block/memory_statue_pray.json      quỳ, cúi đầu, hai tay đưa ra trước ngực chắp lại (pose 2)
  blockstates/memory_statue.json

Model mặc định quay mặt về NAM (+z) như Cửa Ký Ức; blockstate xoay theo FACING. Tỉ lệ người chơi thu 0.9:
quỳ cao ~1.9 block nhờ đầu thò lên trên block (model cho phép tới y = 32).
"""
import json
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "samsara")
TEX = os.path.join(ROOT, "textures", "block")
MODELS = os.path.join(ROOT, "models", "block")
STATES = os.path.join(ROOT, "blockstates")

BASE = (0xE9, 0xE6, 0xDF)


def texture():
    rng = random.Random(2009)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 1.0 + rng.uniform(-0.035, 0.035)
            img.putpixel((x, y), tuple(min(255, int(c * k)) for c in BASE) + (255,))
    # vài vết rạn
    for _ in range(3):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randint(3, 6)):
            img.putpixel((x % 16, y % 16), tuple(int(c * 0.82) for c in BASE) + (255,))
            x += rng.choice((-1, 0, 1))
            y += 1
    img.save(os.path.join(TEX, "memory_statue.png"))


def box(frm, to, rotation=None):
    e = {"from": frm, "to": to,
         "faces": {f: {"texture": "#all"} for f in ("north", "south", "east", "west", "up", "down")}}
    if rotation:
        e["rotation"] = rotation
    return e


def statue(pose):
    """Mặt trước = +z. Gối chạm đất ở phía trước (z ~ 9..12.5), cẳng chân nằm ra sau."""
    bowed = pose == "bowed"
    pray = pose == "pray"
    lean = {"origin": [8, 13, 10], "axis": "x", "angle": 22.5} if bowed else None
    head_rot = ({"origin": [8, 24, 10], "axis": "x", "angle": 22.5} if (bowed or pray)
                else {"origin": [8, 24, 10], "axis": "x", "angle": -22.5})
    el = [
        # cẳng chân nằm ngang ra sau
        box([4.5, 0, 1.5], [7.9, 3.4, 12.5]),
        box([8.1, 0, 1.5], [11.5, 3.4, 12.5]),
        # đùi dựng đứng trên gối
        box([4.5, 3.4, 9], [7.9, 13, 12.5]),
        box([8.1, 3.4, 9], [11.5, 13, 12.5]),
        # thân
        box([4.5, 13, 8.5], [11.5, 24, 12], lean),
        # tay buông dọc thân; chắp tay: hai tay xoay 45° ra trước từ vai, khép vào giữa ngực
        *([box([5, 14, 8.5], [8, 23.5, 12], {"origin": [8, 23, 10.25], "axis": "x", "angle": -45}),
           box([8, 14, 8.5], [11, 23.5, 12], {"origin": [8, 23, 10.25], "axis": "x", "angle": -45})] if pray else
          [box([1, 13, 8.5], [4.5, 23.5, 12], lean),
           box([11.5, 13, 8.5], [15, 23.5, 12], lean)]),
        # đầu
        box([4.5, 24, 6.75] if not bowed else [4.5, 22.5, 8.5], [11.5, 31, 13.75] if not bowed else [11.5, 29.5, 15.5], head_rot),
    ]
    return {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "textures": {"particle": "samsara:block/memory_statue", "all": "samsara:block/memory_statue"},
        "elements": el,
    }


FACING_Y = {"south": 0, "west": 90, "north": 180, "east": 270}


def write_json(path, data):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    for d in (TEX, MODELS, STATES):
        os.makedirs(d, exist_ok=True)
    texture()
    variants = {}
    for pose_id, pose in ((0, "up"), (1, "bowed"), (2, "pray")):
        write_json(os.path.join(MODELS, f"memory_statue_{pose}.json"), statue(pose))
        for facing, y in FACING_Y.items():
            v = {"model": f"samsara:block/memory_statue_{pose}"}
            if y:
                v["y"] = y
            variants[f"facing={facing},pose={pose_id}"] = v
    write_json(os.path.join(STATES, "memory_statue.json"), {"variants": variants})
    print("ok")


if __name__ == "__main__":
    main()
