"""
Tầng Dưới / Đường Ống: gạch men trắng ố, gạch glitch, đèn huỳnh quang.
Chạy từ thư mục mod/:  python tools/generate_tang_duoi.py

Sinh ra:
  textures/block/gach_men_o_0..3.png            4 biến thể ố bẩn (blockstate chọn ngẫu nhiên theo vị trí)
  textures/block/gach_men_glitch_0..1.png       texture động: phần lớn thời gian là gạch thường, thỉnh thoảng xé lát, tách màu
  textures/block/den_huynh_quang.png
  models/block/*.json, blockstates/*.json
  tools/preview_tang_duoi.png
"""
import json
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "samsara")
TEX = os.path.join(ROOT, "textures", "block")
MODELS = os.path.join(ROOT, "models", "block")
STATES = os.path.join(ROOT, "blockstates")
SIZE = 32          # 2 x 2 viên gạch mỗi block
TILE = SIZE // 2
BASE = (232, 236, 230)
GROUT = (140, 148, 144)


def clamp(v):
    return max(0, min(255, int(v)))


def tile_image(seed):
    r = random.Random(seed)
    img = Image.new("RGB", (SIZE, SIZE))
    px = img.load()
    # Mỗi viên có độ ố riêng
    tint = {(i, j): r.uniform(-12, 8) for i in range(2) for j in range(2)}
    for y in range(SIZE):
        for x in range(SIZE):
            if x % TILE == 0 or y % TILE == 0:
                c = tuple(clamp(v + r.uniform(-7, 7)) for v in GROUT)
            else:
                t = tint[(x // TILE, y // TILE)] + r.uniform(-3, 3)
                c = (clamp(BASE[0] + t), clamp(BASE[1] + t), clamp(BASE[2] + t * 1.1))
            px[x, y] = c
    # Vệt ố vàng nâu chảy dọc từ ron gạch
    for _ in range(r.randint(2, 5)):
        x = r.randrange(SIZE)
        y = r.choice([1, TILE + 1])
        length = r.randint(3, 13)
        strength = r.uniform(0.12, 0.38)
        for k in range(length):
            yy = y + k
            if yy >= SIZE:
                break
            p = px[x, yy]
            a = strength * (1 - k / length)
            px[x, yy] = (clamp(p[0] * (1 - a) + 122 * a), clamp(p[1] * (1 - a) + 96 * a), clamp(p[2] * (1 - a) + 52 * a))
            if r.random() < 0.18:
                x = (x + r.choice([-1, 1])) % SIZE
    return img


def glitch_frame(tile, r):
    """Xé tile thành lát ngang lệch nhau, tách kênh đỏ / xanh."""
    out = tile.copy()
    y = 0
    while y < SIZE:
        h = r.randint(2, 7)
        if r.random() < 0.65:
            band = tile.crop((0, y, SIZE, min(SIZE, y + h)))
            br, bg, bb = band.split()
            s = r.randint(-11, 11)
            br = br.transform(band.size, Image.AFFINE, (1, 0, s + 3, 0, 1, 0))
            bg = bg.transform(band.size, Image.AFFINE, (1, 0, s, 0, 1, 0))
            bb = bb.transform(band.size, Image.AFFINE, (1, 0, s - 3, 0, 1, 0))
            out.paste(Image.merge("RGB", (br, bg, bb)), (0, y))
        y += h
    return out


def write_json(path, data):
    with open(path, "w", encoding="utf8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def cube(name):
    write_json(os.path.join(MODELS, name + ".json"),
               {"parent": "minecraft:block/cube_all", "textures": {"all": "samsara:block/" + name}})


def weighted(block, names):
    write_json(os.path.join(STATES, block + ".json"),
               {"variants": {"": [{"model": "samsara:block/" + n} for n in names]}})


def axis_states(block, model):
    """Máng đèn nằm ngang qua ống: ống chạy theo z thì xoay model 90 độ."""
    write_json(os.path.join(STATES, block + ".json"), {"variants": {
        "axis=x": {"model": "samsara:block/" + model},
        "axis=z": {"model": "samsara:block/" + model, "y": 90}}})


FONT = {0: ["111", "101", "101", "101", "111"], 1: ["010", "110", "010", "010", "111"], 2: ["111", "001", "111", "100", "111"],
        3: ["111", "001", "111", "001", "111"], 4: ["101", "101", "111", "001", "001"], 5: ["111", "100", "111", "001", "111"],
        6: ["111", "100", "111", "101", "111"], 7: ["111", "001", "010", "010", "010"], 8: ["111", "101", "111", "101", "111"],
        9: ["111", "101", "111", "001", "111"]}


def make_lock():
    """Bánh số đá (khắc số màu sáng trên nền đá) và các vết khắc chỉ thấy qua máy quay."""
    r = random.Random(3300)
    variants = {}
    for d in range(10):
        img = Image.new("RGB", (32, 32))
        p = img.load()
        for y in range(32):
            for x in range(32):
                v = 92 + r.randint(-9, 9)
                if x < 2 or y < 2 or x > 29 or y > 29:
                    v = 54 + r.randint(-5, 5)
                p[x, y] = (v, v, v + 2)
        for row, line in enumerate(FONT[d]):
            for col, ch in enumerate(line):
                if ch == "1":
                    for yy in range(4):
                        for xx in range(4):
                            p[10 + col * 4 + xx, 6 + row * 4 + yy] = (206, 204, 192)
        name = f"banh_khoa_{d}"
        img.save(os.path.join(TEX, name + ".png"))
        write_json(os.path.join(MODELS, name + ".json"), {"parent": "minecraft:block/cube", "textures": {
            "particle": "minecraft:block/stone_bricks", "down": "minecraft:block/stone_bricks", "up": "minecraft:block/stone_bricks",
            "north": "minecraft:block/stone_bricks", "east": "minecraft:block/stone_bricks", "west": "minecraft:block/stone_bricks",
            "south": "samsara:block/" + name}})
        for hint in ("false", "true"):
            variants[f"digit={d},hint={hint}"] = {"model": "samsara:block/" + name}
    write_json(os.path.join(STATES, "banh_khoa.json"), {"variants": variants})

    misc = os.path.join(ROOT, "textures", "misc")
    os.makedirs(misc, exist_ok=True)
    white = (232, 236, 230, 235)

    def blank():
        return Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def stroke(img, x, y0, y1, slant):
        p = img.load()
        for y in range(y0, y1):
            xx = x + int((y - y0) * slant)
            p[xx, y] = white
            p[xx + 1, y] = white

    def tally(n):
        img = blank()
        groups = n // 5
        x = 6
        for g in range(groups):
            for i in range(5):
                if i < 4:
                    stroke(img, x + i * 6, 12, 52, 0.05)
            p = img.load()
            for t in range(24):
                px = x - 2 + t
                py = 42 - int(t * 0.8)
                p[px, py] = white
                p[px + 1, py] = white
            x += 30
        for i in range(n % 5):
            stroke(img, x + i * 6, 12, 52, 0.05)
        return img

    tally(1).save(os.path.join(misc, "khac_1.png"))
    tally(5).save(os.path.join(misc, "khac_5.png"))
    tally(8).save(os.path.join(misc, "khac_8.png"))

    clean = blank()
    p = clean.load()
    for y in range(14, 50):
        for x in range(14, 50):
            edge = x in (14, 49) or y in (14, 49)
            p[x, y] = (236, 240, 234, 210 if edge else 58)
    clean.save(os.path.join(misc, "khac_0.png"))

    arrow = Image.new("RGBA", (128, 32), (0, 0, 0, 0))
    a = arrow.load()
    for x in range(8, 118):
        for t in range(2):
            a[x, 16 + t] = white
    for k in range(12):
        for t in range(2):
            a[118 - k, 16 - k + t] = white
            a[118 - k, 17 + k + t] = white
    arrow.save(os.path.join(misc, "khac_mui_ten.png"))


def main():
    tiles = []
    for i in range(4):
        img = tile_image(1000 + i)
        img.save(os.path.join(TEX, f"gach_men_o_{i}.png"))
        cube(f"gach_men_o_{i}")
        tiles.append(img)
    weighted("gach_men_o", [f"gach_men_o_{i}" for i in range(4)])

    # Gạch rỉ nước: mảng ẩm xám lạnh rất nhẹ, cùng tông với vệt ướt của shader
    wet = tile_image(1010)
    w = wet.load()
    r = random.Random(1010)
    for y in range(SIZE):
        for x in range(SIZE):
            d = ((x - 15) ** 2 + (y - 16) ** 2) ** 0.5
            if d < 12 + r.uniform(-2.5, 2.5):
                k = 0.34 * (1 - d / 15) + 0.05
                p = w[x, y]
                w[x, y] = (clamp(p[0] * (1 - k) + 92 * k), clamp(p[1] * (1 - k) + 104 * k), clamp(p[2] * (1 - k) + 112 * k))
    wet.save(os.path.join(TEX, "gach_men_ri.png"))
    cube("gach_men_ri")
    weighted("gach_men_ri", ["gach_men_ri"])

    # Khung 0 = gạch thường (giữ lâu), các khung sau = glitch ngắn; hai biến thể lệch nhịp
    holds = [(70, 23), (113, 31)]
    previews = []
    for v, (hold_a, hold_b) in enumerate(holds):
        r = random.Random(7700 + v)
        base = tiles[v]
        frames = [base]
        schedule = [{"index": 0, "time": hold_a}]
        for n in range(1, 4):
            frames.append(glitch_frame(base, r))
            schedule.append({"index": n, "time": 2})
        schedule.append({"index": 0, "time": hold_b})
        for n in range(4, 6):
            frames.append(glitch_frame(base, r))
            schedule.append({"index": n, "time": 2})
        strip = Image.new("RGB", (SIZE, SIZE * len(frames)))
        for n, f in enumerate(frames):
            strip.paste(f, (0, n * SIZE))
        strip.save(os.path.join(TEX, f"gach_men_glitch_{v}.png"))
        write_json(os.path.join(TEX, f"gach_men_glitch_{v}.png.mcmeta"), {"animation": {"frames": schedule}})
        cube(f"gach_men_glitch_{v}")
        previews.append(frames[1])
    weighted("gach_men_glitch", ["gach_men_glitch_0", "gach_men_glitch_1"])

    # Đèn lắp âm vào trần: vẽ trên chính nền gạch (cùng độ phân giải, cùng ron) để không lạc lõng.
    X0, X1, Y0, Y1 = 9, 22, 2, 29       # khung đèn dài dọc theo block

    def fixture(seed, diffuser, tube):
        img = tile_image(seed).copy()
        q = img.load()
        for y in range(Y0, Y1 + 1):
            for x in range(X0, X1 + 1):
                if x in (X0, X1) or y in (Y0, Y1):
                    q[x, y] = (163, 170, 168)
                elif 14 <= x <= 17:
                    q[x, y] = tube
                else:
                    q[x, y] = diffuser
        return img

    lamp = fixture(1000, (214, 228, 226), (250, 254, 250))
    lamp.save(os.path.join(TEX, "den_huynh_quang.png"))
    cube("den_huynh_quang")
    axis_states("den_huynh_quang", "den_huynh_quang")

    # Đèn tắt: máng còn nguyên, ống tối, vài xác côn trùng nằm trong máng
    off = fixture(1001, (118, 126, 124), (88, 94, 92))
    q = off.load()
    r = random.Random(77)
    for _ in range(5):
        x, y = r.randint(X0 + 2, X1 - 2), r.randint(Y0 + 3, Y1 - 3)
        q[x, y] = (46, 44, 40)
    off.save(os.path.join(TEX, "den_huynh_quang_tat.png"))
    cube("den_huynh_quang_tat")
    write_json(os.path.join(STATES, "den_chop.json"), {"variants": {
        "axis=x,lit=true": {"model": "samsara:block/den_huynh_quang"},
        "axis=z,lit=true": {"model": "samsara:block/den_huynh_quang", "y": 90},
        "axis=x,lit=false": {"model": "samsara:block/den_huynh_quang_tat"},
        "axis=z,lit=false": {"model": "samsara:block/den_huynh_quang_tat", "y": 90}}})

    # Đèn hỏng: máng vỡ một mảng lởm chởm, ống gãy, muội lan nhẹ ra gạch
    broken = fixture(1002, (112, 120, 118), (80, 86, 84))
    b = broken.load()
    r = random.Random(4242)
    cx, cy = 16, 17
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - cx, (y - cy) * 0.62
            d = (dx * dx + dy * dy) ** 0.5
            if d < 4.0 + r.uniform(-1.3, 1.3) and X0 < x < X1 and Y0 < y < Y1:
                b[x, y] = (15, 14, 13)
            elif d < 11.0:
                k = (1.0 - d / 11.0) * 0.55 * r.uniform(0.75, 1.0)
                p = b[x, y]
                b[x, y] = (clamp(p[0] * (1 - k) + 26 * k), clamp(p[1] * (1 - k) + 24 * k), clamp(p[2] * (1 - k) + 22 * k))
    broken.save(os.path.join(TEX, "den_hong.png"))
    cube("den_hong")
    axis_states("den_hong", "den_hong")

    # Dấu chân trên gạch: cùng hình dấu chân Bến Đò, xám nhạt; cắt alpha nên làm mờ bằng cách bỏ bớt điểm ảnh
    src = Image.open(os.path.join(TEX, "wet_footprints.png")).convert("RGBA")
    prints = Image.new("RGBA", src.size)
    sp, op = src.load(), prints.load()
    for y in range(src.height):
        for x in range(src.width):
            r_, g_, b_, a_ = sp[x, y]
            if a_ and (x + y * 2) % 3 != 0:
                shade = int(150 + (r_ + g_ + b_) / 765 * 22)
                op[x, y] = (shade, shade + 5, shade + 8, a_)
    prints.save(os.path.join(TEX, "dau_chan_gach.png"))

    # Giọt nước: đúng một đường 1 pixel kiểu pixel art
    PART_TEX = os.path.join(ROOT, "textures", "particle")
    drop = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    dp = drop.load()
    for y in range(4, 28):
        dp[16, y] = (255, 255, 255, 215)
    drop.save(os.path.join(PART_TEX, "giot_nuoc.png"))

    # Vòng nước: 6 khung, vòng tròn pixel 1 nét loang dần ra
    names = []
    for i in range(6):
        ring = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        rp = ring.load()
        radius = 3 + i * 2.3
        for y in range(32):
            for x in range(32):
                d = ((x - 15.5) ** 2 + (y - 15.5) ** 2) ** 0.5
                if abs(d - radius) < 0.72:
                    rp[x, y] = (255, 255, 255, 235)
        ring.save(os.path.join(PART_TEX, f"vong_nuoc_{i}.png"))
        names.append(f"samsara:vong_nuoc_{i}")
    write_json(os.path.join(ROOT, "particles", "vong_nuoc.json"), {"textures": names})

    # Khối Chưa Tải: ô tím đen kiểu thiếu texture
    missing = Image.new("RGB", (32, 32))
    mp = missing.load()
    for y in range(32):
        for x in range(32):
            mp[x, y] = (248, 0, 248) if (x // 16 + y // 16) % 2 == 0 else (0, 0, 0)
    missing.save(os.path.join(TEX, "khoi_chua_tai.png"))
    cube("khoi_chua_tai")
    weighted("khoi_chua_tai", ["khoi_chua_tai"])

    make_lock()

    prev = Image.new("RGB", (SIZE * 6, SIZE))
    for i, t in enumerate(tiles + previews):
        prev.paste(t, (i * SIZE, 0))
    prev.resize((SIZE * 24, SIZE * 4), Image.NEAREST).save(os.path.join(HERE, "preview_tang_duoi.png"))


if __name__ == "__main__":
    main()
