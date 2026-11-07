"""
Khối Mã (womb_code): block đen có mã nhị phân xanh rơi kiểu matrix (Bến Đò: căn phòng đang "rã" thành dữ liệu).
Không entity, không renderer: chỉ texture động trong atlas và model của chính block.

1. Liền mạch giữa các Khối Mã: texture 256x256 = 4x4 block, 64 px mỗi block, lặp liền ở mọi mép. Thuộc tính TX, TY, TZ
   (0..3) = toạ độ thế giới mod 4; mỗi mặt chỉ lấy đúng ô 1/4 x 1/4 theo toạ độ của mặt đó (UV 4 đơn vị).
   Chiều u: nam +x, bắc -x, đông -z, tây +z, trên/dưới +x / +z; chiều v của mặt đứng ngược y.
2. Viền tan vào khối bên cạnh (connected texture, người dùng yêu cầu): khi cạnh Khối Mã là một block khác, model
   mọc thêm một dải mỏng (nhô 0.25 px) phủ lên mặt của block đó, mã thưa dần thành các đốm lởm chởm rồi tắt.
   Blockstate multipart: 64 phần thân theo TX/TY/TZ + 6 phần viền theo NORTH..DOWN.
     womb_code_fringe_h   viền ngang (tan dần theo u), dùng cho đông/nam/tây/bắc (model hướng đông, xoay y)
     womb_code_fringe_v   viền dọc (tan dần lên trên theo v), dùng cho trên (lật v cho dưới)
3. Animation: lưới chữ 0/1 (phông 3x5 trong ô 4x8 px) đứng yên, độ sáng chạy dọc xuống thành vệt (đầu trắng xanh,
   đuôi xanh lam tắt dần), 32 khung x 2 tick, tuần hoàn liền.

Sinh ra (chạy từ thư mục mod/:  python tools/generate_code_block.py):
  textures/block/womb_code.png, womb_code_fringe_h.png, womb_code_fringe_v.png (+ .png.mcmeta)
  models/block/womb_code_<tx>_<ty>_<tz>.json, womb_code_fringe_{side,up,down}.json, blockstates/womb_code.json
  tools/preview_code_block.png
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

PX = 64                  # px mỗi block
P = 4                    # chu kỳ ghép (block)
SIZE = PX * P            # 256
CW, CH = 4, 8            # ô chữ
FRAMES = 32
FRINGE = 8               # bề rộng viền (đơn vị model, 16 = 1 block)

BG = (0x02, 0x03, 0x07)
DIM = (0x07, 0x12, 0x2A)
ZERO = ["###", "#.#", "#.#", "#.#", "###"]
ONE = [".#.", "##.", ".#.", ".#.", "###"]


def write_json(path, data):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def lit(k):
    if k >= 0.999:
        return (0xC8, 0xF0, 0xFF)
    return (int(0x08 + 0x20 * k), int(0x24 + 0x78 * k), int(0x52 + 0xA8 * k))


def code_strip(w, h, seed):
    """Dải khung mã rơi w x h, tuần hoàn liền theo cả hai chiều."""
    rng = random.Random(seed)
    cols, rows = w // CW, h // CH
    glyph = [[rng.random() < 0.5 for _ in range(cols)] for _ in range(rows)]
    drops = [[(rng.randrange(rows), rng.randint(5, 14)) for _ in range(rng.choice((1, 1, 2)))] for _ in range(cols)]
    step = rows / FRAMES                   # ô đi được mỗi khung (tuần hoàn đúng 1 vòng sau FRAMES khung)
    strip = Image.new("RGBA", (w, h * FRAMES), BG + (255,))
    for f in range(FRAMES):
        for c in range(cols):
            for r in range(rows):
                k = 0.0
                for phase, length in drops[c]:
                    head = (phase + f * step) % rows
                    d = (head - r) % rows
                    if d < 1.0:
                        k = 1.0
                    elif d < length:
                        k = max(k, 1.0 - d / length)
                color = lit(k) if k > 0 else DIM
                pat = ZERO if glyph[r][c] else ONE
                for gy, line in enumerate(pat):
                    for gx, ch in enumerate(line):
                        if ch == "#":
                            strip.putpixel((c * CW + gx, f * h + r * CH + gy + 1), color + (255,))
    return strip


def fringe(strip, w, h, along_u, seed):
    """Mặt nạ tan lởm chởm: đặc ở mép nối (u = 0 hoặc v = cuối), thưa dần thành đốm theo khối 2x2 px, tắt ở giữa."""
    rng = random.Random(seed)
    depth = w * FRINGE // 16                     # số px của dải viền
    mask = [[False] * w for _ in range(h)]
    for by in range(0, h, 2):
        for bx in range(0, w, 2):
            d = bx if along_u else (h - 1 - by)  # khoảng cách tới mép nối
            t = d / depth
            if t >= 1.0:
                continue
            keep = rng.random() < (1.0 - t) ** 1.1 + 0.1 * (t < 0.15)
            if keep:
                for yy in range(by, by + 2):
                    for xx in range(bx, bx + 2):
                        mask[yy][xx] = True
    out = Image.new("RGBA", strip.size, (0, 0, 0, 0))
    frames = strip.height // h
    for f in range(frames):
        for y in range(h):
            for x in range(w):
                if mask[y][x]:
                    out.putpixel((x, f * h + y), strip.getpixel((x, f * h + y)))
    return out


def uv(uq, vq):
    w = 16 / P
    return [uq * w, vq * w, (uq + 1) * w, (vq + 1) * w]


def body_model(tx, ty, tz):
    vy = P - 1 - ty
    faces = {"south": uv(tx, vy), "north": uv(P - 1 - tx, vy), "east": uv(P - 1 - tz, vy),
             "west": uv(tz, vy), "up": uv(tx, tz), "down": uv(tx, P - 1 - tz)}
    return {"parent": "minecraft:block/block",
            "textures": {"particle": "samsara:block/womb_code", "code": "samsara:block/womb_code"},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                          "faces": {fc: {"texture": "#code", "uv": u, "cullface": fc} for fc, u in faces.items()}}]}


def fringe_models():
    F, E = FRINGE, 0.25
    tex = {"particle": "samsara:block/womb_code", "h": "samsara:block/womb_code_fringe_h",
           "v": "samsara:block/womb_code_fringe_v"}
    side = {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": tex, "elements": [
        # Hướng đông: phủ lên mặt trên và hai mặt bên (bắc, nam) của khối bên cạnh, đặc ở x = 16
        {"from": [16, 16, 0], "to": [16 + F, 16 + E, 16], "faces": {"up": {"texture": "#h", "uv": [0, 0, F, 16]}}},
        {"from": [16, 0, -E], "to": [16 + F, 16, 0], "faces": {"north": {"texture": "#h", "uv": [F, 0, 0, 16]}}},
        {"from": [16, 0, 16], "to": [16 + F, 16, 16 + E], "faces": {"south": {"texture": "#h", "uv": [0, 0, F, 16]}}},
    ]}
    up_els, down_els = [], []
    for name, frm, to in (("north", [0, 0, -E], [16, 0, 0]), ("south", [0, 0, 16], [16, 0, 16 + E]),
                          ("west", [-E, 0, 0], [0, 0, 16]), ("east", [16, 0, 0], [16 + E, 0, 16])):
        up_els.append({"from": [frm[0], 16, frm[2]], "to": [to[0], 16 + F, to[2]],
                       "faces": {name: {"texture": "#v", "uv": [0, 16 - F, 16, 16]}}})
        down_els.append({"from": [frm[0], -F, frm[2]], "to": [to[0], 0, to[2]],
                         "faces": {name: {"texture": "#v", "uv": [0, 16, 16, 16 - F]}}})
    up = {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": tex, "elements": up_els}
    down = {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": tex, "elements": down_els}
    return side, up, down


def main():
    for d in (TEX, MODELS, STATES):
        os.makedirs(d, exist_ok=True)
    meta = {"animation": {"frametime": 2, "interpolate": False}}

    body = code_strip(SIZE, SIZE, 42)
    body.save(os.path.join(TEX, "womb_code.png"))
    write_json(os.path.join(TEX, "womb_code.png.mcmeta"), meta)

    small = code_strip(PX, PX, 7)
    fringe(small, PX, PX, True, 11).save(os.path.join(TEX, "womb_code_fringe_h.png"))
    fringe(small, PX, PX, False, 12).save(os.path.join(TEX, "womb_code_fringe_v.png"))
    for n in ("womb_code_fringe_h", "womb_code_fringe_v"):
        write_json(os.path.join(TEX, n + ".png.mcmeta"), meta)

    side, up, down = fringe_models()
    write_json(os.path.join(MODELS, "womb_code_fringe_side.json"), side)
    write_json(os.path.join(MODELS, "womb_code_fringe_up.json"), up)
    write_json(os.path.join(MODELS, "womb_code_fringe_down.json"), down)

    parts = []
    for tx in range(P):
        for ty in range(P):
            for tz in range(P):
                name = f"womb_code_{tx}_{ty}_{tz}"
                write_json(os.path.join(MODELS, name + ".json"), body_model(tx, ty, tz))
                parts.append({"when": {"tx": str(tx), "ty": str(ty), "tz": str(tz)},
                              "apply": {"model": f"samsara:block/{name}"}})
    for d, y in (("east", 0), ("south", 90), ("west", 180), ("north", 270)):
        a = {"model": "samsara:block/womb_code_fringe_side"}
        if y:
            a["y"] = y
        parts.append({"when": {d: "true"}, "apply": a})
    parts.append({"when": {"up": "true"}, "apply": {"model": "samsara:block/womb_code_fringe_up"}})
    parts.append({"when": {"down": "true"}, "apply": {"model": "samsara:block/womb_code_fringe_down"}})
    write_json(os.path.join(STATES, "womb_code.json"), {"multipart": parts})

    # Xem trước: một ô thân (64 px) + hai viền, phóng 3 lần
    prev = Image.new("RGBA", (PX * 3 * 3, PX * 3), (60, 45, 30, 255))
    prev.alpha_composite(body.crop((0, 0, PX, PX)).resize((PX * 3, PX * 3), Image.NEAREST), (0, 0))
    for i, n in enumerate(("womb_code_fringe_h", "womb_code_fringe_v")):
        im = Image.open(os.path.join(TEX, n + ".png")).crop((0, 0, PX, PX)).resize((PX * 3, PX * 3), Image.NEAREST)
        prev.alpha_composite(im, (PX * 3 * (i + 1), 0))
    prev.save(os.path.join(HERE, "preview_code_block.png"))
    print("ok")


if __name__ == "__main__":
    main()
