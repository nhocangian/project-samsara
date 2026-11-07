"""
Cửa Ký Ức (chương II của The Womb, "Những Cánh Cửa"): block cửa đứng trơ trọi trên mặt nước, kèm khung.
Tham chiếu: biển cửa panel màu phấn (hồng / xám xanh / trắng) đứng trong nước nông, một cánh cửa trắng lớn hé mở.

Sinh ra (chạy từ thư mục mod/:  python tools/generate_memory_door.py):
  textures/block/memory_door_<màu>_bottom.png   16x16  nửa dưới cánh cửa (2 ô panel dưới, tay nắm)
  textures/block/memory_door_<màu>_top.png      16x16  nửa trên (2 ô panel cao, đỉnh vòm)
  textures/block/memory_door_<màu>_frame.png    16x16  khung cửa
  textures/block/memory_door_knob.png           16x16  tay nắm đồng
  models/block/memory_door_<màu>_<lower|upper>[_ajar|_open].json
  blockstates/memory_door_<màu>.json
  textures/block/memory_door_curtain.png        16x32  màn sáng gần như đục sau cánh cửa lớn (che vũ trụ bên ngoài)
  textures/block/womb_veil.png + model + blockstate   Vách Hư Không: tường và trần đen tuyền của căn phòng bao
                                                      Bến Đò và biển cửa (đen tuyền nên không lộ góc, không lộ ánh sáng)

Model mặc định quay mặt về NAM (người chơi đi từ Bến Đò lên phía bắc nhìn thấy mặt trước).
Cánh cửa dày 2 px ở giữa block (z 7..9); khung thò ra ngoài block 2 px mỗi bên, dầm trên cao 2 px trên nửa trên.
ajar = 0 đóng, 1 hé 22.5°, 2 hé 45° (xoay quanh bản lề phía tây, cánh mở về phía bắc, tức ra xa người nhìn).
"""
import json
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "samsara")
TEX = os.path.join(ROOT, "textures", "block")
MODELS = os.path.join(ROOT, "models", "block")
STATES = os.path.join(ROOT, "blockstates")

COLORS = {
    #          nền cánh cửa         khung
    "pink":  ((0xE8, 0xBE, 0xC2), (0xDD, 0xAD, 0xB2)),
    "blue":  ((0xAE, 0xBB, 0xCB), (0x9D, 0xAB, 0xBE)),
    "white": ((0xEF, 0xEE, 0xEA), (0xE2, 0xE1, 0xDC)),
}


def shade(c, k):
    return tuple(max(0, min(255, int(round(v * k)))) for v in c)


def jitter(rng, c, amount=0.025):
    return shade(c, 1.0 + rng.uniform(-amount, amount))


def panel(img, rng, base, x0, y0, x1, y1, arch=False):
    """Ô panel lõm: mép trên-trái tối (bóng), mép dưới-phải sáng, lòng hơi tối hơn nền."""
    inner = shade(base, 0.955)
    dark = shade(base, 0.86)
    light = shade(base, 1.06)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if arch and y == y0 and (x == x0 or x == x1):
                continue
            edge_top = y == y0 or (arch and y == y0 + 1 and (x == x0 or x == x1))
            if edge_top or x == x0:
                c = dark
            elif y == y1 or x == x1:
                c = light
            else:
                c = jitter(rng, inner, 0.015)
            img.putpixel((x, y), c + (255,))


def door_textures(name, base, frame):
    rng = random.Random(sum(map(ord, name)) * 131)
    full = Image.new("RGBA", (16, 32))
    for y in range(32):
        for x in range(16):
            full.putpixel((x, y), jitter(rng, base, 0.018) + (255,))
    # Viền cánh cửa
    for y in range(32):
        full.putpixel((0, y), shade(base, 0.92) + (255,))
        full.putpixel((15, y), shade(base, 0.9) + (255,))
    for x in range(16):
        full.putpixel((x, 0), shade(base, 1.04) + (255,))
        full.putpixel((x, 31), shade(base, 0.88) + (255,))
    # Hai ô panel cao phía trên (đỉnh bo), hai ô panel thấp phía dưới
    panel(full, rng, base, 3, 3, 7, 16, arch=True)
    panel(full, rng, base, 9, 3, 13, 16, arch=True)
    panel(full, rng, base, 3, 20, 7, 28)
    panel(full, rng, base, 9, 20, 13, 28)
    full.crop((0, 0, 16, 16)).save(os.path.join(TEX, f"memory_door_{name}_top.png"))
    full.crop((0, 16, 16, 32)).save(os.path.join(TEX, f"memory_door_{name}_bottom.png"))

    fr = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = jitter(rng, frame, 0.02)
            if x in (0, 15) or y in (0, 15):
                c = shade(frame, 0.93)
            fr.putpixel((x, y), c + (255,))
    fr.save(os.path.join(TEX, f"memory_door_{name}_frame.png"))


def knob_texture():
    img = Image.new("RGBA", (16, 16))
    rng = random.Random(7)
    for y in range(16):
        for x in range(16):
            k = 1.0 - 0.25 * (x + y) / 30.0
            img.putpixel((x, y), jitter(rng, shade((0xC9, 0xA5, 0x5E), k), 0.04) + (255,))
    img.save(os.path.join(TEX, "memory_door_knob.png"))


def curtain_texture():
    """Màn sáng: trắng ấm, gần như đục ở giữa, mép mờ dần."""
    w, h = 16, 32
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            ex = min(x + 0.5, w - x - 0.5) / (w / 2)
            ey = min(y + 0.5, h - y - 0.5) / (h / 2)
            a = min(1.0, min(ex, ey) * 3.0)
            img.putpixel((x, y), (255, 252, 244, int(240 * a)))
    img.save(os.path.join(TEX, "memory_door_curtain.png"))


VEIL_COLOR = (0x04, 0x03, 0x06)


def veil_block():
    img = Image.new("RGBA", (16, 16), VEIL_COLOR + (255,))
    img.save(os.path.join(TEX, "womb_veil.png"))
    write_json(os.path.join(MODELS, "womb_veil.json"), {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "textures": {"particle": "samsara:block/womb_veil", "all": "samsara:block/womb_veil"},
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
            f: {"texture": "#all", "cullface": f, "shade": False}
            for f in ("north", "south", "east", "west", "up", "down")}}],
    })
    write_json(os.path.join(STATES, "womb_veil.json"), {"variants": {"": {"model": "samsara:block/womb_veil"}}})


def box(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), uv=None, rotation=None):
    e = {"from": frm, "to": to, "faces": {}}
    for f in faces:
        # UV luôn khai báo rõ: khối thò ra ngoài block (khung cửa) mà để Minecraft tự suy UV thì đọc nhầm texture bên cạnh
        face = {"texture": tex, "uv": uv[f] if uv and f in uv else [0, 0, 16, 16]}
        e["faces"][f] = face
    if rotation:
        e["rotation"] = rotation
    return e


def door_model(name, half, angle):
    tex = "#bottom" if half == "lower" else "#top"
    rot = {"origin": [0, 8, 7], "axis": "y", "angle": angle} if angle else None
    elements = []
    # Cánh cửa: mặt trước/sau dùng cả texture 16x16, cạnh dùng dải 2 px
    elements.append(box([0, 0, 7], [16, 16, 9], tex, uv={
        "north": [16, 0, 0, 16], "south": [0, 0, 16, 16],
        "east": [14, 0, 16, 16], "west": [0, 0, 2, 16],
        "up": [0, 0, 16, 2], "down": [0, 14, 16, 16]}, rotation=rot))
    if half == "lower":
        # Tay nắm hai mặt, cao ~0.9 block, phía không có bản lề
        elements.append(box([12.5, 13, 9], [13.5, 14, 10], "#knob", rotation=rot))
        elements.append(box([12.5, 13, 6], [13.5, 14, 7], "#knob", rotation=rot))
    # Khung: hai thanh đứng thò ra ngoài block, sâu 4 px (z 6..10)
    elements.append(box([-2, 0, 6], [0, 16, 10], "#frame"))
    elements.append(box([16, 0, 6], [18, 16, 10], "#frame"))
    if half == "upper":
        elements.append(box([-2, 16, 6], [18, 18, 10], "#frame"))
    return {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "textures": {
            "particle": f"samsara:block/memory_door_{name}_frame",
            "bottom": f"samsara:block/memory_door_{name}_bottom",
            "top": f"samsara:block/memory_door_{name}_top",
            "frame": f"samsara:block/memory_door_{name}_frame",
            "knob": "samsara:block/memory_door_knob",
        },
        "elements": elements,
    }


AJAR = {0: ("", 0), 1: ("_ajar", 22.5), 2: ("_open", 45)}
FACING_Y = {"south": 0, "west": 90, "north": 180, "east": 270}


def write_json(path, data):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    for d in (TEX, MODELS, STATES):
        os.makedirs(d, exist_ok=True)
    knob_texture()
    curtain_texture()
    veil_block()
    for name, (base, frame) in COLORS.items():
        door_textures(name, base, frame)
        variants = {}
        for half in ("lower", "upper"):
            for ajar, (suffix, angle) in AJAR.items():
                model_name = f"memory_door_{name}_{half}{suffix}"
                write_json(os.path.join(MODELS, model_name + ".json"), door_model(name, half, angle))
                for facing, y in FACING_Y.items():
                    v = {"model": f"samsara:block/{model_name}"}
                    if y:
                        v["y"] = y
                    variants[f"ajar={ajar},facing={facing},half={half}"] = v
        write_json(os.path.join(STATES, f"memory_door_{name}.json"), {"variants": variants})
    print("ok")


if __name__ == "__main__":
    main()
