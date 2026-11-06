"""
Tạo 4 giai đoạn Mặt Trời -> Hố Đen dạng PIXEL ART THUẦN (64x64, không blur).

Mã hóa texture theo PREMULTIPLIED ALPHA (WorldRendererMixin vẽ với blend ONE, ONE_MINUS_SRC_ALPHA):
  - Pixel phát sáng (thân, đĩa bồi tụ): RGB = màu, A = 0.9 -> che 90% trời + cộng sáng nhẹ (giữ màu cả ban ngày)
  - Vầng sáng: premultiplied thường (RGB = màu*A), A 0.22-0.55 -> nhuộm trời sang cam, không trắng xóa.
  - Lõi hư không: RGB = 0, A = 255 -> che đen bầu trời thật sự.
  - Vết nứt / bụi tối: RGB tối, A ở giữa -> vừa làm tối vừa nhuốm màu.

Ý tưởng xuyên suốt 4 giai đoạn: bắt đầu từ Mặt Trời VUÔNG của Vanilla rồi biến dạng dần,
để người chơi luôn nhận ra đó vẫn là "mặt trời":
  1. Mặt trời vuông rạn nứt, mắt đen nhỏ ở tâm.
  2. Các góc vuông vỡ vụn, lõi thành hố đen nhìn trực diện với vành quang tử vàng kim.
  3. Bão plasma đỏ cam xoáy xoắn ốc, phụt tia lửa chéo.
  4. Gargantua: đĩa bồi tụ nghiêng + vòng thấu kính hấp dẫn vắt qua đỉnh.

Chạy từ thư mục mod/:  python tools/generate_sun_stages_pixel.py
Kết quả: src/main/resources/assets/samsara/textures/environment/sun_stage{1..4}.png
         tools/preview_sun_stages.png (ảnh xem trước so sánh cũ / mới trên nền trời)
"""
import math
import os
import numpy as np
from PIL import Image

N = 64                 # kích thước THIẾT KẾ (mọi bán kính/tọa độ bên dưới tính theo đơn vị này)
C = (N - 1) / 2.0      # tâm 31.5
# Độ phân giải: mỗi pixel thiết kế được chia thành K x K pixel thật -> texture (64*K) x (64*K).
# K=1 là bản pixel 64x64 cũ; K=2 (mặc định) = 128x128; K=4 = 256x256.
K = int(os.environ.get("SUN_RES", "2"))
FN = N * K
G = max(1, K // 2)      # cỡ cụm cho bụi/hạt nhiễu để vẫn thấy rõ ở độ phân giải cao

# Ma trận Bayer 4x4 cho dithering pixel art
BAYER = (np.array([[0, 8, 2, 10],
                   [12, 4, 14, 6],
                   [3, 11, 1, 9],
                   [15, 7, 13, 5]], dtype=np.float32) + 0.5) / 16.0

# Bảng màu phát sáng (emissive, cộng sáng)
WHITE      = (255, 252, 240)
PALE_GOLD  = (255, 236, 160)
GOLD       = (255, 200, 60)
AMBER      = (255, 150, 30)
ORANGE     = (245, 105, 20)
RED_ORANGE = (220, 60, 15)
DEEP_RED   = (150, 30, 8)
EMBER      = (90, 22, 6)
# Vầng sáng: màu ấm, vẽ bằng tint (nhuộm trời) với độ phủ giảm dần theo vòng
HALO_1     = (255, 185, 70)
HALO_2     = (245, 135, 45)
HALO_3     = (220, 95, 35)
HALO_OPACITY = (0.45, 0.28, 0.14)

# Thân phát sáng che 90% bầu trời, 10% còn lại cộng sáng -> giữ màu cả ban ngày lẫn ban đêm
BODY_OPACITY = 0.9


def hash01(x, y, seed=0):
    n = (x * 374761393 + y * 668265263 + seed * 1013904223) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return (n & 0x7FFFFFFF) / float(0x7FFFFFFF)


class Canvas:
    def __init__(self):
        self.rgb = np.zeros((FN, FN, 3), dtype=np.float32)
        self.a = np.zeros((FN, FN), dtype=np.float32)

    def emit(self, x, y, col, op=BODY_OPACITY):
        """Pixel phát sáng: che phần lớn bầu trời (op) + cộng sáng nhẹ, để màu không bị trời xanh làm trắng xóa."""
        if 0 <= x < FN and 0 <= y < FN:
            self.rgb[y, x] = col
            self.a[y, x] = op

    def tint(self, x, y, col, op):
        """Pixel nhuộm màu kiểu alpha thường (premultiplied): bầu trời ngả dần sang màu col."""
        if 0 <= x < FN and 0 <= y < FN:
            self.rgb[y, x] = np.array(col, dtype=np.float32) * op
            self.a[y, x] = op

    def void(self, x, y, col=(0, 0, 0)):
        """Pixel đen đặc: che hoàn toàn bầu trời."""
        if 0 <= x < FN and 0 <= y < FN:
            self.rgb[y, x] = col
            self.a[y, x] = 1.0

    def stain(self, x, y, col, alpha):
        """Pixel tối bán phần (vết nứt, bụi): làm tối + nhuốm màu."""
        if 0 <= x < FN and 0 <= y < FN:
            self.rgb[y, x] = col
            self.a[y, x] = alpha

    def dither_emit(self, x, y, t, col, op=BODY_OPACITY):
        """Phát sáng với mật độ t (0..1) bằng Bayer dithering."""
        if t > BAYER[y % 4, x % 4]:
            self.emit(x, y, col, op)

    def dither_tint(self, x, y, t, col, op):
        if t > BAYER[y % 4, x % 4]:
            self.tint(x, y, col, op)

    def image(self):
        rgba = np.concatenate([self.rgb, self.a[:, :, None] * 255.0], axis=2)
        return Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), mode="RGBA")


def pixels():
    """(x, y) = pixel thật để ghi; (dx, dy) = tọa độ thiết kế so với tâm."""
    for y in range(FN):
        for x in range(FN):
            yield x, y, (x + 0.5) / K - 0.5 - C, (y + 0.5) / K - 0.5 - C


def superellipse(dx, dy, p):
    return (abs(dx) ** p + abs(dy) ** p) ** (1.0 / p)


def radial_halo(cv, r_in, r_out, cols=(HALO_1, HALO_2, HALO_3), metric=None):
    """
    Vầng sáng pixel kiểu "glow bậc thang": 3 vòng màu ĐẶC, mỗi vòng dày >= 2px, mờ dần ra ngoài.
    Không dùng dithering 1px vì filter VHS của shaderpack (tách kênh RGB + scanline) biến dither
    thành nhiễu ô cờ xanh/tím.
    """
    band_w = min(3, max(2, int(round((r_out - r_in) / 3.0))))  # vòng 2-3px, tổng tối đa 9px
    for x, y, dx, dy in pixels():
        r = metric(dx, dy) if metric else math.hypot(dx, dy)
        if r > r_in:
            band = int((r - r_in) // band_w)
            if band < 3:
                cv.tint(x, y, cols[band], HALO_OPACITY[band])


DUST_COLS = (PALE_GOLD, GOLD, AMBER, ORANGE)


def dust_halo(cv, r_start, step=2.5, cols=DUST_COLS, metric=None, seeds=(3, 4, 5, 6)):
    """
    Vầng "bụi sao" lấp lánh (kiểu Stage 2): 4 vòng đồng tâm, mỗi vòng dày 1px thiết kế, gồm các hạt sáng
    rải ngẫu nhiên (cố định seed), thưa dần ra ngoài. metric cho phép vòng theo hình vuông bo / elip nghiêng.
    """
    thresholds = (0.55, 0.62, 0.70, 0.80)
    for x, y, dx, dy in pixels():
        r = metric(dx, dy) if metric else math.hypot(dx, dy)
        for i in range(4):
            if abs(r - (r_start + i * step)) <= 0.5 and hash01(x // G, y // G, seeds[i]) > thresholds[i]:
                cv.emit(x, y, cols[i])


# ==============================================================================
# STAGE 1: Mặt Trời Vuông Rạn Nứt
# ==============================================================================
def stage1():
    cv = Canvas()
    half = 12  # thân vuông 24x24 (gần tỉ lệ Mặt Trời Vanilla)

    square = lambda dx, dy: superellipse(dx, dy, 6)
    radial_halo(cv, half, half + 7, metric=square)  # bắt đầu sát mép, không hở khe
    dust_halo(cv, half + 1.5, step=2.0, metric=square)  # bụi sao lấp lánh theo khối vuông bo góc

    for x, y, dx, dy in pixels():
        cheb = max(abs(dx), abs(dy))
        if cheb <= half:
            e = half - cheb
            if e < 1:
                cv.emit(x, y, AMBER)
            elif e < 2:
                cv.emit(x, y, GOLD)
            else:
                g = hash01(x // K, y // K, 1)  # hạt nhiễu theo ô thiết kế, đỡ lấm tấm
                cv.emit(x, y, WHITE if g > 0.8 else PALE_GOLD)

    # "Mặt Trời nứt chính tâm, lộ lõi đen":
    #  - MỘT đường nứt chính gãy khúc chạy chéo, lòng đen sâu, chỉ toác rộng ở ngay tâm.
    #  - Vài vết nứt nhánh MẢNH tách ra từ đường chính ở xa tâm (không tỏa từ tâm -> không thành hình chân nhện),
    #    nhọn dần về đầu.
    #  - Mép trắng nóng mảnh + quầng cam mỏng quanh đường chính; không dùng màu đỏ sẫm (bị đục khi qua VHS).
    # Ít khúc, gãy gắt kiểu tia sét
    main = [(8.6, -11.6), (6.2, -8.4), (7.6, -6.6), (3.6, -3.4), (0.0, 0.0),
            (-2.6, 2.4), (-1.4, 4.6), (-5.2, 7.0), (-4.4, 8.8), (-7.6, 11.6)]
    branches = [
        [(6.2, -8.4), (3.8, -9.0), (1.6, -10.6)],
        [(7.6, -6.6), (9.6, -5.6), (10.6, -3.8)],
        [(-2.6, 2.4), (-5.0, 1.8), (-7.4, 3.0)],
        [(-5.2, 7.0), (-7.6, 8.0), (-9.2, 7.4)],
    ]

    def polyline(path):
        lens = [math.hypot(path[i + 1][0] - path[i][0], path[i + 1][1] - path[i][1]) for i in range(len(path) - 1)]
        return path, lens, sum(lens)

    def dist_t(pl, px, py):
        """(khoảng cách tới đường gấp khúc, vị trí t 0..1 dọc đường)."""
        path, lens, total = pl
        best_d, best_t, acc = 1e9, 0.0, 0.0
        for i in range(len(path) - 1):
            (ax, ay), (bx, by) = path[i], path[i + 1]
            vx, vy = bx - ax, by - ay
            k = max(0.0, min(1.0, ((px - ax) * vx + (py - ay) * vy) / (vx * vx + vy * vy)))
            d = math.hypot(px - (ax + vx * k), py - (ay + vy * k))
            if d < best_d:
                best_d, best_t = d, (acc + lens[i] * k) / total
            acc += lens[i]
        return best_d, best_t

    main_pl = polyline(main)
    branch_pls = [polyline(b) for b in branches]

    for x, y, dx, dy in pixels():
        if max(abs(dx), abs(dy)) > half - 0.5:
            continue
        # Đường chính: mảnh ở hai đầu (0.4), toác ~2.3 ở tâm
        d, t = dist_t(main_pl, dx, dy)
        w = 0.35 + 1.15 * math.exp(-((t - 0.5) / 0.07) ** 2)  # lỗ ở tâm nhỏ, nhọn kiểu thấu kính
        edge = d - w  # < 0: trong lòng nứt
        is_main = True
        # Nhánh: lòng 0.3 -> 0.1 ở đầu nhánh
        for bpl in branch_pls:
            bd, bt = dist_t(bpl, dx, dy)
            bw = 0.28 - 0.2 * bt
            if bd - bw < edge:
                edge, is_main = bd - bw, False
        edge_w = 0.55 if is_main else 0.3  # mép nhánh mảnh hơn, không xù
        if edge <= 0:
            cv.void(x, y)                       # lòng nứt đen sâu
        elif edge <= edge_w:
            cv.emit(x, y, WHITE)                # mép trắng nóng
        elif is_main and edge <= 1.3:
            cv.emit(x, y, AMBER)                # quầng cam mỏng quanh đường chính
    return cv


# ==============================================================================
# STAGE 2: Hố Đen Trực Diện, góc vuông vỡ vụn
# ==============================================================================
def stage2():
    cv = Canvas()
    radial_halo(cv, 14, 27)

    # Tàn dư khung vuông cũ đang vỡ ra (mảnh vụn rời rạc)
    for x, y, dx, dy in pixels():
        cheb = max(abs(dx), abs(dy))
        r = math.hypot(dx, dy)
        if 14.5 <= cheb <= 16.5 and r > 15:
            h = hash01(x // G, y // G, 22)
            if h > 0.62:
                cv.emit(x, y, AMBER if h > 0.85 else ORANGE if h > 0.72 else DEEP_RED)

    # Các vệt bụi sao quay quanh
    dust_halo(cv, 16.0)

    # Vành quang tử + thân hố đen
    for x, y, dx, dy in pixels():
        r = math.hypot(dx, dy)
        if r <= 7.6:
            cv.void(x, y)
        elif r <= 8.5:
            cv.void(x, y, (12, 3, 2))
        elif r <= 9.6:
            cv.emit(x, y, WHITE)
        elif r <= 10.7:
            cv.emit(x, y, PALE_GOLD)
        elif r <= 12.0:
            cv.emit(x, y, GOLD)
        elif r <= 13.4:
            cv.emit(x, y, AMBER)
        elif r <= 14.6:
            cv.tint(x, y, ORANGE, 0.6)
    return cv


# ==============================================================================
# STAGE 3: Bão Plasma Xoáy Đỏ Cam
# ==============================================================================
def stage3():
    cv = Canvas()
    radial_halo(cv, 20, 30, cols=((250, 120, 40), (230, 80, 30), (190, 50, 25)))
    dust_halo(cv, 22.0, cols=(GOLD, AMBER, ORANGE, RED_ORANGE))  # bụi plasma đỏ cam

    # Các nhánh xoắn ốc logarit
    for x, y, dx, dy in pixels():
        r = math.hypot(dx, dy)
        if 8.0 < r <= 21.0:
            th = math.atan2(dy, dx)
            s = math.cos(2.0 * th + 4.2 * math.log(r))
            heat = 1.0 - (r - 8.0) / 13.0  # nóng ở trong, nguội ra ngoài
            if s > 0.15:
                if heat > 0.75:
                    col = WHITE if s > 0.7 else PALE_GOLD
                elif heat > 0.5:
                    col = GOLD if s > 0.6 else AMBER
                elif heat > 0.25:
                    col = ORANGE if s > 0.5 else RED_ORANGE
                else:
                    col = RED_ORANGE if s > 0.6 else DEEP_RED
                cv.emit(x, y, col)
            elif s > -0.35 and heat > 0.2:
                cv.tint(x, y, (200, 60, 20), 0.35)

    for x, y, dx, dy in pixels():
        r = math.hypot(dx, dy)
        if r <= 6.6:
            cv.void(x, y)
        elif r <= 7.5:
            cv.void(x, y, (25, 4, 2))
        elif r <= 8.4:
            cv.emit(x, y, PALE_GOLD)
    return cv


# ==============================================================================
# STAGE 4: Gargantua (đĩa nghiêng + thấu kính hấp dẫn)
# ==============================================================================
def stage4():
    cv = Canvas()
    tilt = math.radians(-12)
    ct, st = math.cos(tilt), math.sin(tilt)

    def uv(dx, dy):
        return dx * ct - dy * st, dx * st + dy * ct

    tilted = lambda dx, dy: math.hypot(uv(dx, dy)[0] * 0.85, uv(dx, dy)[1] * 1.15)
    radial_halo(cv, 16, 30, metric=tilted)
    dust_halo(cv, 17.5, metric=tilted)  # bụi sao theo elip nghiêng của đĩa bồi tụ

    SH_U, SH_V = 10.0, 9.0  # bán trục bóng tối

    # Vòng thấu kính (phần đĩa phía sau bị bẻ cong vòng qua đỉnh và đáy)
    for x, y, dx, dy in pixels():
        u, v = uv(dx, dy)
        ra = math.hypot(u / (SH_U * 1.32), v / (SH_V * 1.38))
        if v < 0 and 0.80 <= ra <= 1.0:          # cung trên dày, sáng
            k = (ra - 0.80) / 0.20
            col = WHITE if k < 0.25 else PALE_GOLD if k < 0.5 else GOLD if k < 0.75 else AMBER
            if u > 4:  # phía lùi xa (Doppler) tối hơn
                col = {WHITE: PALE_GOLD, PALE_GOLD: GOLD, GOLD: AMBER, AMBER: ORANGE}[col]
            cv.emit(x, y, col)
        elif v >= 0 and 0.80 <= ra <= 0.88:      # cung dưới mảnh, mờ
            cv.emit(x, y, AMBER if u < 0 else ORANGE)

    # Bóng tối sự kiện + vành quang tử 1px
    for x, y, dx, dy in pixels():
        u, v = uv(dx, dy)
        d = math.hypot(u / SH_U, v / SH_V)
        if d <= 0.9:
            cv.void(x, y)
        elif d <= 1.0:
            cv.emit(x, y, WHITE if u < 0 else PALE_GOLD)

    # Đĩa bồi tụ: phần trước đi ngang qua mặt bóng tối
    L = 30.0
    for x, y, dx, dy in pixels():
        u, v = uv(dx, dy)
        if abs(u) > L:
            continue
        thick = 2.7 * max(0.0, 1.0 - (abs(u) / L) ** 1.6)
        inside_shadow = math.hypot(u / SH_U, v / SH_V) <= 1.0
        # Nửa trên của đĩa nằm sau bóng tối -> bị che, chỉ vẽ nửa dưới khi ở trong bóng
        if inside_shadow and v < -0.2:
            continue
        if abs(v) <= thick:
            dn = abs(v) / max(thick, 0.01)
            near = 1.0 - abs(u) / L
            if u < 0:  # phía lao tới (Doppler beaming) sáng hơn
                col = WHITE if dn < 0.35 else PALE_GOLD if dn < 0.7 else GOLD
            else:
                col = PALE_GOLD if dn < 0.3 else GOLD if dn < 0.65 else AMBER
            if near < 0.25:
                col = AMBER if u < 0 else ORANGE
            if near < 0.1:
                col = DEEP_RED
            cv.emit(x, y, col)
    return cv


# ==============================================================================
# XEM TRƯỚC
# ==============================================================================
SKY_DAY = np.array([120, 167, 255], dtype=np.float32)
SKY_DUSK = np.array([40, 46, 80], dtype=np.float32)


def composite_new(img, sky):
    arr = np.array(img, dtype=np.float32)
    rgb, a = arr[:, :, :3], arr[:, :, 3:4] / 255.0
    return np.clip(rgb + sky * (1.0 - a), 0, 255)


def composite_old(img, sky):
    arr = np.array(img.resize((FN, FN), Image.BILINEAR), dtype=np.float32)
    rgb, a = arr[:, :, :3], arr[:, :, 3:4] / 255.0
    return np.clip(rgb * a + sky * (1.0 - a), 0, 255)


def save_preview(new_imgs, old_imgs, path, scale=6):
    tile = FN * max(1, round(N * scale / FN))  # bội số nguyên của FN để không méo pixel
    pad = 8
    rows = [("old", SKY_DAY), ("new", SKY_DAY), ("new", SKY_DUSK)]
    W = pad + len(new_imgs) * (tile + pad)
    H = pad + len(rows) * (tile + pad)
    sheet = Image.new("RGB", (W, H), (24, 24, 28))
    for ri, (kind, sky) in enumerate(rows):
        for si in range(len(new_imgs)):
            if kind == "old":
                if old_imgs[si] is None:
                    continue
                arr = composite_old(old_imgs[si], sky)
            else:
                arr = composite_new(new_imgs[si], sky)
            t = Image.fromarray(arr.astype(np.uint8), "RGB").resize((tile, tile), Image.NEAREST)
            sheet.paste(t, (pad + si * (tile + pad), pad + ri * (tile + pad)))
    sheet.save(path)


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    out_dir = os.path.join(here, "..", "src", "main", "resources", "assets", "samsara", "textures", "environment")

    old = []
    for i in range(1, 5):
        p = os.path.join(here, "sun_stages_old", f"sun_stage{i}.png")  # bản cũ (blur + alpha blend) để so sánh
        old.append(Image.open(p).convert("RGBA") if os.path.exists(p) else None)

    imgs = [stage1().image(), stage2().image(), stage3().image(), stage4().image()]
    for i, im in enumerate(imgs, start=1):
        im.save(os.path.join(out_dir, f"sun_stage{i}.png"))

    preview = os.path.join(here, "preview_sun_stages.png")
    save_preview(imgs, old, preview)
    print(f"Saved 4 pixel-art sun stages ({FN}x{FN}, premultiplied) and", preview)


if __name__ == "__main__":
    main()
