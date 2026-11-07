"""
Đạo cụ kể chuyện ở Bến Đò (chương I của The Womb, "nơi bạn thức dậy": phòng ngủ của Nam đang tan ra trên mặt nước).

Sinh ra (chạy từ thư mục mod/:  python tools/generate_dock_props.py):
  nam_computer        máy tính CRT + bàn phím; màn hình là ảnh chụp bầu trời có chấm đen giữa mặt trời
                      (ảnh chụp màn hình đêm đầu tiên trong nhật ký). Mặt trước quay về NAM ở model gốc.
  nam_wallpaper       mảng tường phòng ngủ: mặt trước giấy dán tường kem sọc, mặt sau ván gỗ sồi (vách ngoài nhà Nam),
                      trên/dưới lộ lõi vữa và nan gỗ. variant 0 trơn, 1 treo ảnh hai người, 2 vết nắng của tấm poster đã gỡ.
                      Mặt trước quay về NAM ở model gốc.
  wet_footprints      dấu chân ướt in trên sàn (decal mỏng 0.25 px, lớp cutout); ngón chân hướng BẮC ở model gốc.
Model khai báo UV cho mọi mặt (xem HANDOFF: model thò ra ngoài block mà để Minecraft tự suy UV sẽ đọc nhầm atlas).
"""
import json
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "samsara")
TEX = os.path.join(ROOT, "textures", "block")
MODELS = os.path.join(ROOT, "models", "block")
STATES = os.path.join(ROOT, "blockstates")

FULL = [0, 0, 16, 16]
SOUTH_FACING_Y = {"south": 0, "west": 90, "north": 180, "east": 270}
NORTH_FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def write_json(path, data):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + ((c[3],) if len(c) > 3 else ())


def box(frm, to, faces):
    """faces: {mặt: (texture, uv)}; mọi mặt đều có UV rõ ràng."""
    return {"from": frm, "to": to,
            "faces": {f: {"texture": t, "uv": uv} for f, (t, uv) in faces.items()}}


# ------------------------------------------------------------------ máy tính

def computer_textures(rng):
    # Vỏ CRT màu be ngả vàng
    case = Image.new("RGBA", (16, 16))
    base = (0xC9, 0xC0, 0xA6, 255)
    for y in range(16):
        for x in range(16):
            case.putpixel((x, y), shade(base, 0.95 + rng.random() * 0.08))
    for x in range(16):
        case.putpixel((x, 15), shade(base, 0.8))
    case.save(os.path.join(TEX, "nam_computer_case.png"))

    # Mặt trước 32x32: viền be, màn hình bo góc: trời xanh, mặt trời có chấm đen, dải cỏ, đèn nguồn xanh
    front = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            front.putpixel((x, y), shade(base, 0.93 + rng.random() * 0.08))
    sx0, sy0, sx1, sy1 = 4, 3, 28, 22
    for y in range(sy0, sy1):
        for x in range(sx0, sx1):
            corner = (x in (sx0, sx1 - 1)) and (y in (sy0, sy1 - 1))
            if corner:
                continue
            t = (y - sy0) / (sy1 - sy0)
            sky = (int(0x6E + 0x50 * t), int(0x9C + 0x40 * t), int(0xE6 + 0x10 * t), 255)
            px = sky
            if y >= sy1 - 5:
                px = (0x5C, 0x9E, 0x3C, 255) if y < sy1 - 3 else (0x7A, 0x56, 0x36, 255)
            # quét dòng CRT
            if y % 2 == 1:
                px = shade(px, 0.88)
            front.putpixel((x, y), px)
    # Mặt trời vuông kiểu Minecraft, chấm đen ở giữa
    cx, cy = 20, 8
    for y in range(cy - 3, cy + 3):
        for x in range(cx - 3, cx + 3):
            front.putpixel((x, y), (0xFF, 0xF2, 0x9A, 255))
    for y in range(cy - 1, cy + 1):
        for x in range(cx - 1, cx + 1):
            front.putpixel((x, y), (0x05, 0x05, 0x08, 255))
    # Đèn nguồn và nút
    front.putpixel((25, 26), (0x5C, 0xE0, 0x6A, 255))
    front.putpixel((26, 26), (0x5C, 0xE0, 0x6A, 255))
    for x in range(6, 12):
        front.putpixel((x, 26), shade(base, 0.7))
    screen_animation(front, (sx0, sy0, sx1, sy1), rng)

    # Bàn phím: nền be tối, phím ô vuông
    kb = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            key = (x % 2 == 0) and (y % 2 == 0)
            kb.putpixel((x, y), (0xE4, 0xDE, 0xCC, 255) if key else (0x8C, 0x86, 0x74, 255))
    kb.save(os.path.join(TEX, "nam_computer_keyboard.png"))


SCREEN_GLITCH = 6     # khung 1..6: màn hình giật (lát xô lệch, tách màu)
SCREEN_CODE = 8       # khung 7..14: mã nhị phân xanh rơi (tham chiếu GIF code rain của người dùng)


def screen_animation(front, box, rng):
    """Dải khung động cho mặt trước máy tính: khung 0 là ảnh chụp bình thường; thỉnh thoảng giật, đôi khi chạy mã."""
    sx0, sy0, sx1, sy1 = box
    n = 1 + SCREEN_GLITCH + SCREEN_CODE
    strip = Image.new("RGBA", (32, 32 * n))
    strip.paste(front, (0, 0))
    for i in range(1, SCREEN_GLITCH + 1):
        f = front.copy()
        for _ in range(rng.randint(2, 4)):
            y0 = rng.randint(sy0, sy1 - 2)
            h = rng.randint(1, 4)
            dx = rng.choice((-7, -5, -3, 3, 5, 7))
            swap = rng.random() < 0.35
            for y in range(y0, min(sy1, y0 + h)):
                for x in range(sx0, sx1):
                    def px(xx):
                        return front.getpixel((sx0 + (xx - sx0) % (sx1 - sx0), y))
                    r = px(x - dx + 1)[0]
                    g = px(x - dx)[1]
                    b = px(x - dx - 1)[2]
                    if swap:
                        r, g, b = b, r, g
                    k = 0.75 if y % 2 else 1.15
                    f.putpixel((x, y), (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)), 255))
        strip.paste(f, (0, 32 * i))
    # Mã rơi: các cột 2 px, đầu sáng xanh trắng, đuôi xanh lam nhạt dần; mỗi khung rơi xuống 2 px
    cols = [(x, rng.randint(0, 30), rng.randint(5, 12)) for x in range(sx0 + 1, sx1 - 1, 3)]
    for j in range(SCREEN_CODE):
        f = front.copy()
        for y in range(sy0, sy1):
            for x in range(sx0, sx1):
                f.putpixel((x, y), (0x04, 0x06, 0x14, 255))
        for (x, start, length) in cols:
            head = sy0 + (start + j * 2) % 30 - 5
            for t in range(length):
                y = head - t
                if sy0 <= y < sy1 and rng.random() < 0.85:
                    if t == 0:
                        c = (0xC8, 0xF4, 0xFF, 255)
                    else:
                        k = 1.0 - t / length
                        c = (int(0x10 * k), int(0x50 + 0x60 * k), int(0x90 + 0x60 * k), 255)
                    f.putpixel((x, y), c)
                    if rng.random() < 0.4 and x + 1 < sx1:
                        f.putpixel((x + 1, y), c)       # chữ "0" rộng 2 px, "1" rộng 1 px
        strip.paste(f, (0, 32 * (1 + SCREEN_GLITCH + j)))
    strip.save(os.path.join(TEX, "nam_computer_front.png"))
    frames = []
    for k in range(10):
        frames.append({"index": 0, "time": rng.randint(50, 150)})
        if k % 3 == 2:
            for _ in range(2):
                frames += [{"index": 1 + SCREEN_GLITCH + j, "time": 2} for j in range(SCREEN_CODE)]
        else:
            frames += [{"index": rng.randint(1, SCREEN_GLITCH), "time": rng.randint(1, 3)} for _ in range(rng.randint(2, 5))]
    write_json(os.path.join(TEX, "nam_computer_front.png.mcmeta"), {"animation": {"interpolate": False, "frames": frames}})


def computer_model():
    case, front, kb = "#case", "#front", "#keyboard"
    els = [
        # Thân CRT (sâu, mặt trước ở phía nam)
        box([2, 1, 3], [14, 12, 13], {
            "south": (front, FULL), "north": (case, FULL), "east": (case, [0, 4, 10, 15]),
            "west": (case, [0, 4, 10, 15]), "up": (case, [2, 3, 14, 13]), "down": (case, [2, 3, 14, 13])}),
        # Chân đế
        box([5, 0, 5], [11, 1, 11], {f: (case, [5, 5, 11, 11]) for f in ("north", "south", "east", "west", "up", "down")}),
        # Bàn phím phía trước
        box([3, 0, 13.25], [13, 0.75, 15.75], {
            "up": (kb, [3, 6, 13, 9]), "down": (case, [3, 6, 13, 9]), "north": (case, [3, 0, 13, 1]),
            "south": (case, [3, 0, 13, 1]), "east": (case, [0, 0, 3, 1]), "west": (case, [0, 0, 3, 1])}),
    ]
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": "samsara:block/nam_computer_case", "case": "samsara:block/nam_computer_case",
                         "front": "samsara:block/nam_computer_front", "keyboard": "samsara:block/nam_computer_keyboard"},
            "elements": els}


# ------------------------------------------------------------------ giấy dán tường

PAPER = (0xE6, 0xDA, 0xBC, 255)


def wallpaper_base(rng):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = PAPER
            if x % 4 == 0:
                c = shade(c, 0.92)                       # sọc mờ
            c = shade(c, 0.97 + rng.random() * 0.05)
            if y > 11:
                c = shade(c, 1.0 - (y - 11) * 0.03)      # ẩm ố dần xuống chân tường
            img.putpixel((x, y), c)
    for x, y in ((2, 3), (10, 5), (6, 10), (14, 12), (2, 13), (10, 14)):
        img.putpixel((x, y), (0xC9, 0x9A, 0x9A, 255))    # hoa nhỏ
    return img


def wallpaper_textures(rng):
    plain = wallpaper_base(rng)
    plain.save(os.path.join(TEX, "nam_wallpaper_0.png"))

    photo = wallpaper_base(rng)
    frame = (0x5A, 0x3C, 0x24, 255)
    for y in range(3, 11):
        for x in range(4, 12):
            edge = x in (4, 11) or y in (3, 10)
            if edge:
                photo.putpixel((x, y), frame)
            else:
                # ảnh bạc màu: nền trời nhạt, hai bóng người (một lớn, một nhỏ)
                c = (0xB8, 0xC4, 0xC8, 255) if y < 8 else (0x9C, 0xA8, 0x86, 255)
                photo.putpixel((x, y), c)
    for x, y in ((6, 5), (6, 6), (6, 7), (6, 8), (9, 7), (9, 8)):
        photo.putpixel((x, y), (0x4A, 0x44, 0x40, 255))
    photo.putpixel((8, 2), frame)                          # đinh treo
    photo.save(os.path.join(TEX, "nam_wallpaper_1.png"))

    poster = wallpaper_base(rng)
    for y in range(2, 13):
        for x in range(3, 13):
            poster.putpixel((x, y), shade(poster.getpixel((x, y)), 1.07))   # vết nắng sáng hơn quanh chỗ dán
    for x, y in ((3, 2), (12, 2), (3, 12), (12, 12)):
        poster.putpixel((x, y), (0xD8, 0xD0, 0xA0, 255))   # băng dính còn sót
    poster.save(os.path.join(TEX, "nam_wallpaper_2.png"))

    edge = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            lath = (x % 5) in (1, 2)
            c = (0x8E, 0x6E, 0x4C, 255) if lath else (0xCF, 0xC8, 0xBA, 255)
            edge.putpixel((x, y), shade(c, 0.9 + rng.random() * 0.15))
    edge.save(os.path.join(TEX, "nam_wallpaper_edge.png"))


def wallpaper_model(v):
    front, back, edge = f"#front", "#back", "#edge"
    return {"parent": "minecraft:block/block",
            "textures": {"particle": f"samsara:block/nam_wallpaper_{v}", "front": f"samsara:block/nam_wallpaper_{v}",
                         "back": "minecraft:block/oak_planks", "edge": "samsara:block/nam_wallpaper_edge"},
            "elements": [box([0, 0, 0], [16, 16, 16], {
                "south": (front, FULL), "north": (back, FULL), "east": (edge, FULL), "west": (edge, FULL),
                "up": (edge, FULL), "down": (edge, FULL)})]}


# ------------------------------------------------------------------ dấu chân ướt

def footprints_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wet = (0x7A, 0x52, 0x2C, 255)   # gỗ thấm nước sẫm lại (lớp cutout: lớp trong suốt bị shader vẽ như mặt nước, ra đen)

    def foot(x0, y0, mirror):
        # bàn chân dài 6, ngón ở trên (bắc), gót ở dưới
        rows = [(1, 3), (0, 3), (0, 3), (0, 2), (1, 2), (1, 2)]
        for i, (a, b) in enumerate(rows):
            for dx in range(a, b + 1):
                x = x0 + (3 - dx if mirror else dx)
                img.putpixel((x, y0 + i + 1), wet)
        for dx in range(0, 4):                              # ngón chân
            if dx != 1:
                img.putpixel((x0 + (3 - dx if mirror else dx), y0), (0x7A, 0x52, 0x2C, 255))

    foot(3, 1, False)     # chân trái đi trước
    foot(9, 8, True)      # chân phải phía sau
    img.save(os.path.join(TEX, "wet_footprints.png"))


def footprints_model():
    t = "#prints"
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": "samsara:block/wet_footprints", "prints": "samsara:block/wet_footprints"},
            "elements": [box([0, 0, 0], [16, 0.25, 16], {"up": (t, FULL), "down": (t, FULL)})]}


def main():
    for d in (TEX, MODELS, STATES):
        os.makedirs(d, exist_ok=True)
    rng = random.Random(7)

    computer_textures(rng)
    write_json(os.path.join(MODELS, "nam_computer.json"), computer_model())
    write_json(os.path.join(STATES, "nam_computer.json"), {"variants": {
        f"facing={f}": ({"model": "samsara:block/nam_computer", "y": y} if y else {"model": "samsara:block/nam_computer"})
        for f, y in SOUTH_FACING_Y.items()}})

    wallpaper_textures(rng)
    variants = {}
    for v in range(3):
        write_json(os.path.join(MODELS, f"nam_wallpaper_{v}.json"), wallpaper_model(v))
        for f, y in SOUTH_FACING_Y.items():
            m = {"model": f"samsara:block/nam_wallpaper_{v}"}
            if y:
                m["y"] = y
            variants[f"facing={f},variant={v}"] = m
    write_json(os.path.join(STATES, "nam_wallpaper.json"), {"variants": variants})

    footprints_texture()
    write_json(os.path.join(MODELS, "wet_footprints.json"), footprints_model())
    write_json(os.path.join(STATES, "wet_footprints.json"), {"variants": {
        f"facing={f}": ({"model": "samsara:block/wet_footprints", "y": y} if y else {"model": "samsara:block/wet_footprints"})
        for f, y in NORTH_FACING_Y.items()}})

    # Ảnh xem trước
    prev = Image.new("RGBA", (16 * 7 * 4, 32 * 4), (40, 40, 40, 255))
    names = ["nam_computer_front", "nam_computer_case", "nam_wallpaper_0", "nam_wallpaper_1", "nam_wallpaper_2",
             "nam_wallpaper_edge", "wet_footprints"]
    for i, n in enumerate(names):
        im = Image.open(os.path.join(TEX, n + ".png")).convert("RGBA")
        im = im.crop((0, 0, im.width, im.width))      # khung đầu của texture động
        im = im.resize((64, 64) if im.width == 16 else (64, 64), Image.NEAREST)
        prev.alpha_composite(im, (i * 64, 32))
    prev.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "preview_dock_props.png"))
    print("ok")


if __name__ == "__main__":
    main()
