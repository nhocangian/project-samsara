"""
Thiết kế 4 giai đoạn Mặt Trời -> Hố Đen GIỐNG HỆT generate_4_stages_hd.py (bản đang dùng trước đó),
nhưng vẽ ở LƯỚI MỊN HƠN: mỗi "ô" của lưới gốc 44-50 px được chia thành S x S pixel con.
Các hình (vành, đĩa bồi tụ, vết nứt...) được tính lại theo tọa độ thực nên mép tròn/chéo mượt hơn,
trong khi vầng sáng (Texture-Contour Bloom) và bảng màu dùng lại nguyên bản.

  S = 1  -> trùng với bản cũ (dùng để kiểm tra:  python tools/generate_4_stages_hd_smooth.py --check)
  S = 4  -> mặc định: mỗi pixel texture hiển thị = 2x2 pixel trên canvas 512 (bản cũ là 8x8)

Chạy từ thư mục mod/:  python tools/generate_4_stages_hd_smooth.py
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import generate_4_stages_hd as base  # noqa: E402  (bảng màu + bloom gốc)

WHITE, LIGHT_GOLD, WARM_YELLOW = base.WHITE, base.LIGHT_GOLD, base.WARM_YELLOW
SOLAR_GOLD, AMBER_GOLD, FIERY_ORANGE = base.SOLAR_GOLD, base.AMBER_GOLD, base.FIERY_ORANGE
NEON_ORANGE, DEEP_AMBER, DARK_AMBER = base.NEON_ORANGE, base.DEEP_AMBER, base.DARK_AMBER
VOID_INNER, VOID_BLACK = base.VOID_INNER, base.VOID_BLACK


def fine(W, H, S):
    """Duyệt lưới mịn: trả về (X, Y) pixel con và (x, y) tọa độ thực trong hệ lưới gốc."""
    for Y in range(H * S):
        for X in range(W * S):
            yield X, Y, (X + 0.5) / S - 0.5, (Y + 0.5) / S - 0.5


def seg_dist(px, py, ax, ay, bx, by):
    vx, vy = bx - ax, by - ay
    L = vx * vx + vy * vy
    k = 0.0 if L == 0 else max(0.0, min(1.0, ((px - ax) * vx + (py - ay) * vy) / L))
    return math.hypot(px - (ax + vx * k), py - (ay + vy * k))


# ==============================================================================
# STAGE 1: Fractured Square Sun
# ==============================================================================
def stage1(S):
    W, H = 44, 44
    grid = np.zeros((H * S, W * S, 4), dtype=np.uint8)
    cx, cy = 21.5, 21.5

    def grain(x, y):
        n = (x * 49297 + y * 93821) ^ 0x3f5a1
        return ((n ^ (n >> 7)) & 0xFF) / 255.0

    for X, Y, x, y in fine(W, H, S):
        if 9.5 <= x <= 33.5 and 9.5 <= y <= 33.5:
            edge = int(min(x - 9.5, 33.5 - x, y - 9.5, 33.5 - y))
            # Hạt nhiễu theo ô gốc khi S=1, mịn dần khi S lớn (mỗi ô con một giá trị)
            g = grain(X, Y) if S > 1 else grain(int(round(x)), int(round(y)))
            if edge == 0:
                grid[Y, X] = FIERY_ORANGE
            elif edge == 1:
                grid[Y, X] = SOLAR_GOLD if g > 0.3 else AMBER_GOLD
            elif edge == 2:
                grid[Y, X] = WARM_YELLOW if g > 0.4 else SOLAR_GOLD
            else:
                grid[Y, X] = WHITE if g > 0.75 else LIGHT_GOLD if g > 0.35 else WARM_YELLOW

    for X, Y, x, y in fine(W, H, S):
        r = math.hypot(x - cx, y - cy)
        if r <= 3.8:
            grid[Y, X] = VOID_BLACK
        elif r <= 4.8:
            grid[Y, X] = VOID_INNER
        elif r <= 5.8:
            grid[Y, X] = WHITE

    cracks = [
        [(22, 17), (23, 16), (24, 14), (26, 13), (28, 11), (30, 10), (32, 9), (33, 8)],
        [(21, 26), (19, 27), (18, 29), (16, 30), (14, 31), (12, 32), (10, 33)],
        [(17, 20), (16, 18), (14, 17), (13, 15), (11, 14), (10, 12)],
        [(26, 23), (27, 25), (29, 26), (31, 28), (32, 30), (33, 31)],
        [(17, 22), (15, 23), (13, 23), (11, 22), (10, 22)],
        [(26, 20), (28, 20), (30, 21), (32, 21), (33, 20)],
        [(22, 27), (23, 29), (23, 31), (22, 33)],
    ]
    segs = [(b[i], b[i + 1]) for b in cracks for i in range(len(b) - 1)]

    def crack_dist(x, y):
        return min(seg_dist(x, y, a[0], a[1], b[0], b[1]) for a, b in segs)

    # Lõi vết nứt trắng nóng + viền cam neon. Ở S=1 giữ đúng bề rộng cũ (1 ô + 1 ô mỗi bên);
    # ở lưới mịn thu mảnh lại cho giống nét nứt mảnh của bản cũ thay vì thành dải dày.
    core_w, edge_w = (0.5, 1.5) if S == 1 else (0.3, 0.75)
    dist = np.full((H * S, W * S), 1e9, dtype=np.float32)
    for X, Y, x, y in fine(W, H, S):
        dist[Y, X] = crack_dist(x, y)
    for X, Y, x, y in fine(W, H, S):
        if dist[Y, X] <= core_w:
            grid[Y, X] = WHITE
    for X, Y, x, y in fine(W, H, S):
        d = dist[Y, X]
        if core_w < d <= edge_w and 9.5 <= x <= 33.5 and 9.5 <= y <= 33.5:
            if not np.array_equal(grid[Y, X], VOID_BLACK) and not np.array_equal(grid[Y, X], WHITE):
                grid[Y, X] = NEON_ORANGE
    return grid


# ==============================================================================
# STAGE 2: Face-On Cosmic Eye
# ==============================================================================
def stage2(S):
    W, H = 46, 46
    grid = np.zeros((H * S, W * S, 4), dtype=np.uint8)
    cx, cy = 22.5, 22.5

    def hash_pt(x, y, seed=0):
        n = (x * 374761393 + y * 668265263 + seed * 1013904223) ^ 0x5bf03635
        n = (n ^ (n >> 13)) * 1274126177
        return (n & 0x7fffffff) / float(0x7fffffff)

    tracks = [
        (10.2, 11.2, 101, 0.65, WHITE, LIGHT_GOLD),
        (13.2, 14.2, 202, 0.48, WHITE, LIGHT_GOLD),
        (15.2, 16.2, 303, 0.42, LIGHT_GOLD, SOLAR_GOLD),
        (17.2, 18.2, 404, 0.48, SOLAR_GOLD, FIERY_ORANGE),
        (19.2, 20.2, 505, 0.52, WARM_YELLOW, DEEP_AMBER),
        (21.0, 22.0, 606, 0.60, AMBER_GOLD, DARK_AMBER),
    ]
    for X, Y, x, y in fine(W, H, S):
        r = math.hypot(x - cx, y - cy)
        if r <= 7.5:
            grid[Y, X] = VOID_BLACK
            continue
        elif r <= 8.5:
            grid[Y, X] = VOID_INNER
            continue
        if r <= 9.6:
            grid[Y, X] = WHITE
        elif r <= 10.8:
            grid[Y, X] = LIGHT_GOLD
        elif r <= 12.0:
            grid[Y, X] = SOLAR_GOLD
        elif r <= 13.2:
            grid[Y, X] = FIERY_ORANGE
        elif r <= 14.5:
            grid[Y, X] = DEEP_AMBER
        # Bụi sao: mỗi hạt bụi là một cụm (S//2 x S//2) để vẫn thấy được khi lưới mịn
        hx, hy = (X // max(1, S // 2), Y // max(1, S // 2)) if S > 1 else (int(round(x)), int(round(y)))
        for r_min, r_max, seed, thresh, col_hi, col_lo in tracks:
            if r_min <= r <= r_max:
                h = hash_pt(hx, hy, seed)
                if h > thresh:
                    grid[Y, X] = col_hi if h > (thresh + (1.0 - thresh) * 0.7) else col_lo
    return grid


# ==============================================================================
# STAGE 3: Relativistic Plasma Vortex
# ==============================================================================
def stage3(S):
    W, H = 50, 42
    grid = np.zeros((H * S, W * S, 4), dtype=np.uint8)
    cx, cy = 24.5, 20.5
    t = math.radians(-22.0)
    ct, st = math.cos(t), math.sin(t)

    for X, Y, x, y in fine(W, H, S):
        dx, dy = x - cx, y - cy
        u = dx * ct - dy * st
        v = dx * st + dy * ct
        sd = (u / 8.0) ** 2 + (v / 6.5) ** 2
        if sd <= 0.85:
            grid[Y, X] = VOID_BLACK
            continue
        elif sd <= 1.0:
            grid[Y, X] = VOID_INNER
            continue
        r = math.hypot(dx, dy)
        ang = math.atan2(dy, dx)
        if v <= 0.5:
            ra = math.sqrt((u / 14.0) ** 2 + (v / 12.0) ** 2)
            if 0.62 <= ra <= 1.25 and abs(u) <= 17.5:
                swirl = math.sin(ang * 2.5 + r * 1.8)
                dop = (-u / 17.5) * 0.5 + 0.5
                if ra <= 0.75:
                    grid[Y, X] = WHITE if dop > 0.4 else LIGHT_GOLD
                elif ra <= 0.90:
                    grid[Y, X] = LIGHT_GOLD if swirl > -0.2 else SOLAR_GOLD
                elif ra <= 1.05:
                    grid[Y, X] = SOLAR_GOLD if dop > 0.3 else FIERY_ORANGE
                elif ra <= 1.18:
                    grid[Y, X] = FIERY_ORANGE if dop > 0.5 else DEEP_AMBER
                else:
                    grid[Y, X] = DEEP_AMBER if dop > 0.6 else DARK_AMBER
        if v >= -0.5:
            rl = math.sqrt((u / 13.0) ** 2 + (v / 10.5) ** 2)
            if 0.65 <= rl <= 1.15 and abs(u) <= 15.0:
                swirl = math.cos(ang * 2.0 - r * 1.5)
                if rl <= 0.78:
                    grid[Y, X] = LIGHT_GOLD
                elif rl <= 0.92:
                    grid[Y, X] = SOLAR_GOLD if swirl > -0.1 else FIERY_ORANGE
                elif rl <= 1.05:
                    grid[Y, X] = FIERY_ORANGE
                else:
                    grid[Y, X] = DEEP_AMBER
        thick = 3.2 * max(0.0, 1.0 - (abs(u) / 24.5) ** 1.3)
        if abs(v) <= thick and abs(u) <= 24.5:
            dn = abs(v) / max(0.01, thick)
            if u < 0:
                grid[Y, X] = WHITE if dn <= 0.35 else LIGHT_GOLD if dn <= 0.65 else SOLAR_GOLD if dn <= 0.85 else FIERY_ORANGE
                if u <= -21.0:
                    grid[Y, X] = SOLAR_GOLD if dn <= 0.5 else FIERY_ORANGE
            else:
                grid[Y, X] = LIGHT_GOLD if dn <= 0.25 else SOLAR_GOLD if dn <= 0.50 else FIERY_ORANGE if dn <= 0.75 else (DEEP_AMBER if u < 18.0 else DARK_AMBER)
                if u >= 20.0:
                    grid[Y, X] = DEEP_AMBER if dn <= 0.4 else DARK_AMBER

    for X, Y, x, y in fine(W, H, S):
        dx, dy = x - cx, y - cy
        u = dx * ct - dy * st
        v = dx * st + dy * ct
        if 0.0 <= v <= 2.6 and abs(u) <= 8.2:
            dn = v / 2.6
            if dn <= 0.45:
                grid[Y, X] = WHITE
            elif dn <= 0.75:
                grid[Y, X] = LIGHT_GOLD if u < 2.0 else SOLAR_GOLD
            else:
                grid[Y, X] = SOLAR_GOLD if u < 2.0 else FIERY_ORANGE
    return grid


# ==============================================================================
# STAGE 4: Cinematic Fine-Ring Gargantua
# ==============================================================================
def stage4(S):
    W, H = 50, 44
    grid = np.zeros((H * S, W * S, 4), dtype=np.uint8)
    cx, cy = 24.5, 21.5
    t = math.radians(-13.5)
    ct, st = math.cos(t), math.sin(t)
    ring_cols = [(0.67, 0.73, WHITE), (0.73, 0.79, LIGHT_GOLD), (0.79, 0.85, SOLAR_GOLD),
                 (0.85, 0.91, FIERY_ORANGE), (0.91, 0.97, DEEP_AMBER), (0.97, 1.04, DARK_AMBER)]

    def ring(ra):
        for lo, hi, col in ring_cols:
            if lo <= ra < hi or (hi == 1.04 and ra == 1.04):
                return col
        return None

    for X, Y, x, y in fine(W, H, S):
        dx, dy = x - cx, y - cy
        u = dx * ct - dy * st
        v = dx * st + dy * ct
        sd = (u / 9.6) ** 2 + (v / 8.2) ** 2
        if sd <= 0.86:
            grid[Y, X] = VOID_BLACK
            continue
        elif sd <= 1.0:
            grid[Y, X] = VOID_INNER
            continue
        ra = math.sqrt((u / 16.5) ** 2 + (v / 14.2) ** 2)
        if (v <= 0.8 or v >= -0.8) and 0.65 <= ra <= 1.06 and abs(u) <= 18.0:
            col = ring(ra)
            if col is not None:
                grid[Y, X] = col
        thick = 2.5 * max(0.0, 1.0 - (abs(u) / 24.5) ** 1.6)
        if abs(v) <= thick and abs(u) <= 24.5:
            dn = abs(v) / max(0.01, thick)
            if u < 0:
                grid[Y, X] = WHITE if dn <= 0.30 else LIGHT_GOLD if dn <= 0.60 else SOLAR_GOLD if dn <= 0.85 else FIERY_ORANGE
            else:
                grid[Y, X] = LIGHT_GOLD if dn <= 0.25 else SOLAR_GOLD if dn <= 0.55 else FIERY_ORANGE if dn <= 0.80 else DEEP_AMBER
            if abs(u) >= 21.0:
                grid[Y, X] = DEEP_AMBER if abs(u) < 23.0 else DARK_AMBER

    for X, Y, x, y in fine(W, H, S):
        dx, dy = x - cx, y - cy
        u = dx * ct - dy * st
        v = dx * st + dy * ct
        if 0.0 <= v <= 2.2 and abs(u) <= 9.6:
            dn = v / 2.2
            if dn <= 0.35:
                grid[Y, X] = WHITE if u < 0 else LIGHT_GOLD
            elif dn <= 0.70:
                grid[Y, X] = LIGHT_GOLD if u < 0 else SOLAR_GOLD
            else:
                grid[Y, X] = SOLAR_GOLD if u < 0 else FIERY_ORANGE
    return grid


def assemble(grid, cell_px, canvas_size=512):
    """Đặt lưới mịn vào canvas 512 sao cho tổng kích thước bằng bản cũ (W*scale_cũ)."""
    h, w, _ = grid.shape
    small = Image.fromarray(grid, mode="RGBA").resize((w * cell_px, h * cell_px), Image.NEAREST)
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    canvas.paste(small, ((canvas_size - small.width) // 2, (canvas_size - small.height) // 2), small)
    return canvas


# (hàm stage, độ mịn S, số pixel canvas cho mỗi pixel con) — tích S*cell_px = tỉ lệ cũ (9 cho stage 1, 8 cho còn lại)
CONFIG = [(stage1, 3, 3), (stage2, 4, 2), (stage3, 4, 2), (stage4, 4, 2)]
CONFIG_CHECK = [(stage1, 1, 9), (stage2, 1, 8), (stage3, 1, 8), (stage4, 1, 8)]


def build(config):
    return [base.apply_texture_contour_bloom(assemble(fn(S), cell)) for fn, S, cell in config]


def save_preview(old_imgs, new_imgs, path, scale_to=384):
    sky = np.array([120, 167, 255], dtype=np.float32)
    pad = 8
    W = pad + 4 * (scale_to + pad)
    H = pad + 2 * (scale_to + pad)
    sheet = Image.new("RGB", (W, H), (24, 24, 28))
    for row, imgs in enumerate((old_imgs, new_imgs)):
        for i, im in enumerate(imgs):
            crop = im.crop((64, 64, 448, 448))  # cắt vùng giữa cho dễ nhìn chi tiết
            arr = np.array(crop, dtype=np.float32)
            rgb, a = arr[:, :, :3], arr[:, :, 3:4] / 255.0
            out = np.clip(rgb * a + sky * (1.0 - a), 0, 255).astype(np.uint8)  # alpha blend như trong game
            tile = Image.fromarray(out, "RGB").resize((scale_to, scale_to), Image.NEAREST)
            sheet.paste(tile, (pad + i * (scale_to + pad), pad + row * (scale_to + pad)))
    sheet.save(path)


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    old_dir = os.path.join(here, "sun_stages_old")
    old = [Image.open(os.path.join(old_dir, f"sun_stage{i}.png")).convert("RGBA") for i in range(1, 5)]

    if "--check" in sys.argv:
        imgs = build(CONFIG_CHECK)
        for i, (a, b) in enumerate(zip(old, imgs), start=1):
            diff = np.abs(np.array(a, dtype=np.int16) - np.array(b, dtype=np.int16))
            print(f"stage{i}: max diff {diff.max()}, pixels differing {(diff.max(axis=2) > 2).sum()}")
        return

    imgs = build(CONFIG)
    out_dir = os.path.join(here, "..", "src", "main", "resources", "assets", "samsara", "textures", "environment")
    for i, im in enumerate(imgs, start=1):
        im.save(os.path.join(out_dir, f"sun_stage{i}.png"))
    preview = os.path.join(here, "preview_sun_stages_smooth.png")
    save_preview(old, imgs, preview)
    print("Saved smooth sun stages and", preview)


if __name__ == "__main__":
    main()
