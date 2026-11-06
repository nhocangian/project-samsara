import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

# ==========================================
# MASTER COLOR PALETTE (RGB + Alpha)
# ==========================================
WHITE        = [255, 255, 255, 255]
LIGHT_GOLD   = [255, 248, 160, 255]
WARM_YELLOW  = [255, 225,  70, 255]
SOLAR_GOLD   = [255, 190,  25, 255]
AMBER_GOLD   = [255, 150,  15, 255]
FIERY_ORANGE = [255, 100,  10, 255]
NEON_ORANGE  = [255,  65,   5, 255]
DEEP_AMBER   = [215,  45,   8, 255]
DARK_AMBER   = [160,  30,   6, 255]
CRIMSON_DUST = [110,  18,   5, 220]
DEEP_CRIMSON = [ 75,  10,   4, 200]
VOID_INNER   = [ 20,   8,  16, 250]
VOID_BLACK   = [  5,   2,   8, 255]

# ==============================================================================
# STAGE 1: Fractured Square Sun (Mặt Trời Rạn Nứt & Mắt Hư Không)
# ==============================================================================
def generate_stage1():
    W, H = 44, 44
    grid = np.zeros((H, W, 4), dtype=np.uint8)
    cx, cy = 21.5, 21.5

    def grain(x, y):
        n = (x * 49297 + y * 93821) ^ 0x3f5a1
        return ((n ^ (n >> 7)) & 0xFF) / 255.0

    # 1. Square Sun body (24x24 from x=10..33, y=10..33)
    for y in range(H):
        for x in range(W):
            if 10 <= x <= 33 and 10 <= y <= 33:
                edge_dist = min(x - 10, 33 - x, y - 10, 33 - y)
                g = grain(x, y)
                if edge_dist == 0:
                    grid[y, x] = FIERY_ORANGE
                elif edge_dist == 1:
                    grid[y, x] = SOLAR_GOLD if g > 0.3 else AMBER_GOLD
                elif edge_dist == 2:
                    grid[y, x] = WARM_YELLOW if g > 0.4 else SOLAR_GOLD
                else:
                    if g > 0.75:
                        grid[y, x] = WHITE
                    elif g > 0.35:
                        grid[y, x] = LIGHT_GOLD
                    else:
                        grid[y, x] = WARM_YELLOW

    # 2. Central nascent singularity eye (pitch black void)
    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            r = np.sqrt(dx*dx + dy*dy)
            if r <= 3.8:
                grid[y, x] = VOID_BLACK
            elif r <= 4.8:
                grid[y, x] = VOID_INNER
            elif r <= 5.8:
                grid[y, x] = WHITE

    # 3. Fractal hairline fractures tearing across the square sun
    cracks = [
        [(22, 17), (23, 16), (24, 14), (26, 13), (28, 11), (30, 10), (32, 9), (33, 8)],
        [(21, 26), (19, 27), (18, 29), (16, 30), (14, 31), (12, 32), (10, 33)],
        [(17, 20), (16, 18), (14, 17), (13, 15), (11, 14), (10, 12)],
        [(26, 23), (27, 25), (29, 26), (31, 28), (32, 30), (33, 31)],
        [(17, 22), (15, 23), (13, 23), (11, 22), (10, 22)],
        [(26, 20), (28, 20), (30, 21), (32, 21), (33, 20)],
        [(22, 27), (23, 29), (23, 31), (22, 33)]
    ]
    for branch in cracks:
        for px, py in branch:
            if 0 <= px < W and 0 <= py < H:
                grid[py, px] = WHITE
                for nx, ny in [(px-1, py), (px+1, py), (px, py-1), (px, py+1)]:
                    if 0 <= nx < W and 0 <= ny < H and 10 <= nx <= 33 and 10 <= ny <= 33:
                        if not np.array_equal(grid[ny, nx], VOID_BLACK) and not np.array_equal(grid[ny, nx], WHITE):
                            grid[ny, nx] = NEON_ORANGE

    return grid

# ==============================================================================
# STAGE 2: Face-On Cosmic Eye (Con Mắt Hư Không / AnasAbdin Style) - Image 2
# ==============================================================================
def generate_stage2():
    W, H = 46, 46
    grid = np.zeros((H, W, 4), dtype=np.uint8)
    cx, cy = 22.5, 22.5

    def hash_pt(x, y, seed=0):
        n = (x * 374761393 + y * 668265263 + seed * 1013904223) ^ 0x5bf03635
        n = (n ^ (n >> 13)) * 1274126177
        return (n & 0x7fffffff) / float(0x7fffffff)

    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            r = np.sqrt(dx*dx + dy*dy)

            # Central Black Hole Event Horizon
            if r <= 7.5:
                grid[y, x] = VOID_BLACK
                continue
            elif r <= 8.5:
                grid[y, x] = VOID_INNER
                continue

            # Core Photon Ring (bright golden-white circle)
            if 8.5 < r <= 9.6:
                grid[y, x] = WHITE
            elif 9.6 < r <= 10.8:
                grid[y, x] = LIGHT_GOLD
            elif 10.8 < r <= 12.0:
                grid[y, x] = SOLAR_GOLD
            elif 12.0 < r <= 13.2:
                grid[y, x] = FIERY_ORANGE
            elif 13.2 < r <= 14.5:
                grid[y, x] = DEEP_AMBER

            # Concentric Orbiting Dust Tracks (exact geometric arcs matching Image 2!)
            tracks = [
                (10.2, 11.2, 101, 0.65, WHITE, LIGHT_GOLD),
                (13.2, 14.2, 202, 0.48, WHITE, LIGHT_GOLD),
                (15.2, 16.2, 303, 0.42, LIGHT_GOLD, SOLAR_GOLD),
                (17.2, 18.2, 404, 0.48, SOLAR_GOLD, FIERY_ORANGE),
                (19.2, 20.2, 505, 0.52, WARM_YELLOW, DEEP_AMBER),
                (21.0, 22.0, 606, 0.60, AMBER_GOLD, DARK_AMBER)
            ]
            for r_min, r_max, seed, thresh, col_hi, col_lo in tracks:
                if r_min <= r <= r_max:
                    h = hash_pt(x, y, seed)
                    if h > thresh:
                        grid[y, x] = col_hi if h > (thresh + (1.0 - thresh) * 0.7) else col_lo

    return grid

# ==============================================================================
# STAGE 3: Relativistic Plasma Vortex / Neon Flare - Image 3
# ==============================================================================
def generate_stage3():
    W, H = 50, 42
    grid = np.zeros((H, W, 4), dtype=np.uint8)
    cx, cy = 24.5, 20.5

    tilt_rad = math.radians(-22.0)
    cos_t = math.cos(tilt_rad)
    sin_t = math.sin(tilt_rad)

    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            u = dx * cos_t - dy * sin_t
            v = dx * sin_t + dy * cos_t

            # Central Black Hole Shadow
            shadow_d = (u / 8.0)**2 + (v / 6.5)**2
            if shadow_d <= 0.85:
                grid[y, x] = VOID_BLACK
                continue
            elif shadow_d <= 1.0:
                grid[y, x] = VOID_INNER
                continue

            r = math.sqrt(dx*dx + dy*dy)
            angle = math.atan2(dy, dx)

            # 1. Swirling Fiery Plasma Vortex Dome
            if v <= 0.5:
                r_arc = math.sqrt((u / 14.0)**2 + (v / 12.0)**2)
                if 0.62 <= r_arc <= 1.25 and abs(u) <= 17.5:
                    swirl = math.sin(angle * 2.5 + r * 1.8)
                    doppler = (-u / 17.5) * 0.5 + 0.5
                    
                    if r_arc <= 0.75:
                        grid[y, x] = WHITE if doppler > 0.4 else LIGHT_GOLD
                    elif r_arc <= 0.90:
                        grid[y, x] = LIGHT_GOLD if swirl > -0.2 else SOLAR_GOLD
                    elif r_arc <= 1.05:
                        grid[y, x] = SOLAR_GOLD if doppler > 0.3 else FIERY_ORANGE
                    elif r_arc <= 1.18:
                        grid[y, x] = FIERY_ORANGE if doppler > 0.5 else DEEP_AMBER
                    else:
                        grid[y, x] = DEEP_AMBER if doppler > 0.6 else DARK_AMBER

            # 2. Lower swirling arc
            if v >= -0.5:
                r_arc_low = math.sqrt((u / 13.0)**2 + (v / 10.5)**2)
                if 0.65 <= r_arc_low <= 1.15 and abs(u) <= 15.0:
                    swirl = math.cos(angle * 2.0 - r * 1.5)
                    if r_arc_low <= 0.78:
                        grid[y, x] = LIGHT_GOLD
                    elif r_arc_low <= 0.92:
                        grid[y, x] = SOLAR_GOLD if swirl > -0.1 else FIERY_ORANGE
                    elif r_arc_low <= 1.05:
                        grid[y, x] = FIERY_ORANGE
                    else:
                        grid[y, x] = DEEP_AMBER

            # 3. Dynamic Accretion Disk with Long Sharp Wings
            disk_thick = 3.2 * max(0.0, 1.0 - (abs(u) / 24.5)**1.3)
            if abs(v) <= disk_thick and abs(u) <= 24.5:
                d_norm = abs(v) / max(0.01, disk_thick)
                if u < 0:
                    if d_norm <= 0.35:
                        grid[y, x] = WHITE
                    elif d_norm <= 0.65:
                        grid[y, x] = LIGHT_GOLD
                    elif d_norm <= 0.85:
                        grid[y, x] = SOLAR_GOLD
                    else:
                        grid[y, x] = FIERY_ORANGE
                    if u <= -21.0:
                        grid[y, x] = SOLAR_GOLD if d_norm <= 0.5 else FIERY_ORANGE
                else:
                    if d_norm <= 0.25:
                        grid[y, x] = LIGHT_GOLD
                    elif d_norm <= 0.50:
                        grid[y, x] = SOLAR_GOLD
                    elif d_norm <= 0.75:
                        grid[y, x] = FIERY_ORANGE
                    else:
                        grid[y, x] = DEEP_AMBER if u < 18.0 else DARK_AMBER
                    if u >= 20.0:
                        grid[y, x] = DEEP_AMBER if d_norm <= 0.4 else DARK_AMBER

    # Front blazing accretion band passing in front of the lower shadow
    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            u = dx * cos_t - dy * sin_t
            v = dx * sin_t + dy * cos_t
            if 0.0 <= v <= 2.6 and abs(u) <= 8.2:
                d_norm = v / 2.6
                if d_norm <= 0.45:
                    grid[y, x] = WHITE
                elif d_norm <= 0.75:
                    grid[y, x] = LIGHT_GOLD if u < 2.0 else SOLAR_GOLD
                else:
                    grid[y, x] = SOLAR_GOLD if u < 2.0 else FIERY_ORANGE

    return grid

# ==============================================================================
# STAGE 4: Cinematic Fine-Ring Gargantua - Image 1
# ==============================================================================
def generate_stage4():
    W, H = 50, 44
    grid = np.zeros((H, W, 4), dtype=np.uint8)
    cx, cy = 24.5, 21.5

    tilt_rad = math.radians(-13.5)
    cos_t = math.cos(tilt_rad)
    sin_t = math.sin(tilt_rad)

    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            u = dx * cos_t - dy * sin_t
            v = dx * sin_t + dy * cos_t

            # Central Black Hole Shadow
            shadow_d = (u / 9.6)**2 + (v / 8.2)**2
            if shadow_d <= 0.86:
                grid[y, x] = VOID_BLACK
                continue
            elif shadow_d <= 1.0:
                grid[y, x] = VOID_INNER
                continue

            # Upper Gravitational Lensing Arc (Concentric 1px threads!)
            if v <= 0.8:
                r_arc = math.sqrt((u / 16.5)**2 + (v / 14.2)**2)
                if 0.65 <= r_arc <= 1.06 and abs(u) <= 18.0:
                    if 0.67 <= r_arc < 0.73:
                        grid[y, x] = WHITE
                    elif 0.73 <= r_arc < 0.79:
                        grid[y, x] = LIGHT_GOLD
                    elif 0.79 <= r_arc < 0.85:
                        grid[y, x] = SOLAR_GOLD
                    elif 0.85 <= r_arc < 0.91:
                        grid[y, x] = FIERY_ORANGE
                    elif 0.91 <= r_arc < 0.97:
                        grid[y, x] = DEEP_AMBER
                    elif 0.97 <= r_arc <= 1.04:
                        grid[y, x] = DARK_AMBER

            # Lower Gravitational Lensing Arc
            if v >= -0.8:
                r_arc_low = math.sqrt((u / 16.5)**2 + (v / 14.2)**2)
                if 0.65 <= r_arc_low <= 1.06 and abs(u) <= 18.0:
                    if 0.67 <= r_arc_low < 0.73:
                        grid[y, x] = WHITE
                    elif 0.73 <= r_arc_low < 0.79:
                        grid[y, x] = LIGHT_GOLD
                    elif 0.79 <= r_arc_low < 0.85:
                        grid[y, x] = SOLAR_GOLD
                    elif 0.85 <= r_arc_low < 0.91:
                        grid[y, x] = FIERY_ORANGE
                    elif 0.91 <= r_arc_low < 0.97:
                        grid[y, x] = DEEP_AMBER
                    elif 0.97 <= r_arc_low <= 1.04:
                        grid[y, x] = DARK_AMBER

            # Equatorial Accretion Disk
            disk_thick = 2.5 * max(0.0, 1.0 - (abs(u) / 24.5)**1.6)
            if abs(v) <= disk_thick and abs(u) <= 24.5:
                d_norm = abs(v) / max(0.01, disk_thick)
                if u < 0:
                    if d_norm <= 0.30:
                        grid[y, x] = WHITE
                    elif d_norm <= 0.60:
                        grid[y, x] = LIGHT_GOLD
                    elif d_norm <= 0.85:
                        grid[y, x] = SOLAR_GOLD
                    else:
                        grid[y, x] = FIERY_ORANGE
                else:
                    if d_norm <= 0.25:
                        grid[y, x] = LIGHT_GOLD
                    elif d_norm <= 0.55:
                        grid[y, x] = SOLAR_GOLD
                    elif d_norm <= 0.80:
                        grid[y, x] = FIERY_ORANGE
                    else:
                        grid[y, x] = DEEP_AMBER

                if abs(u) >= 21.0:
                    grid[y, x] = DEEP_AMBER if abs(u) < 23.0 else DARK_AMBER

    # Front crossing of accretion disk
    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            u = dx * cos_t - dy * sin_t
            v = dx * sin_t + dy * cos_t
            if 0.0 <= v <= 2.2 and abs(u) <= 9.6:
                d_norm = v / 2.2
                if d_norm <= 0.35:
                    grid[y, x] = WHITE if u < 0 else LIGHT_GOLD
                elif d_norm <= 0.70:
                    grid[y, x] = LIGHT_GOLD if u < 0 else SOLAR_GOLD
                else:
                    grid[y, x] = SOLAR_GOLD if u < 0 else FIERY_ORANGE

    return grid

# ==============================================================================
# TRUE TEXTURE-CONTOUR BLOOM (Quầng sáng ôm sát cấu trúc hình thể thực của Texture)
# ==============================================================================
def apply_texture_contour_bloom(canvas):
    """
    Tạo quầng sáng tự nhiên ôm sát chính xác từng đường viền và hình khối của Texture:
    - Mặt trời vuông -> Quầng sáng tỏa ra theo khối vuông bo tròn mềm mại
    - Hố đen đĩa bồi tụ -> Quầng sáng kéo dài theo cánh kim cương và vành thấu kính
    - Lõi hư không -> Giữ nguyên 100% độ đen thẳm tuyệt đối, không bị loang mờ
    """
    t_arr = np.array(canvas, dtype=np.float32) / 255.0
    tex_rgb = t_arr[:, :, :3]
    tex_a   = t_arr[:, :, 3:4]
    
    tex_lum = 0.299 * tex_rgb[:, :, 0:1] + 0.587 * tex_rgb[:, :, 1:2] + 0.114 * tex_rgb[:, :, 2:3]
    is_void = np.clip((0.20 - tex_lum) / 0.15, 0.0, 1.0) * np.clip(tex_a / 0.7, 0.0, 1.0)
    
    # Nguồn phát xạ: Chỉ các điểm ảnh sáng của texture mới phát sáng!
    emissive = tex_rgb * tex_a * (1.0 - is_void) * np.clip((tex_lum - 0.05) / 0.35, 0.0, 1.0)
    em_img = Image.fromarray((emissive * 255.0).astype(np.uint8), mode='RGB')
    
    # 3 tầng lan tỏa tự nhiên theo hình dạng texture:
    # 1. Hào quang ôm sát viền hình khối (bán kính 8px)
    # 2. Quầng sáng vàng kim trung gian (bán kính 22px)
    # 3. Quầng khí quyển mềm mại loang xa (bán kính 48px)
    b_tight = np.array(em_img.filter(ImageFilter.GaussianBlur(radius=8)), dtype=np.float32) / 255.0
    b_mid   = np.array(em_img.filter(ImageFilter.GaussianBlur(radius=22)), dtype=np.float32) / 255.0
    b_wide  = np.array(em_img.filter(ImageFilter.GaussianBlur(radius=48)), dtype=np.float32) / 255.0
    
    c_white  = np.array([1.0, 0.98, 0.90])
    c_gold   = np.array([1.0, 0.82, 0.30])
    c_amber  = np.array([0.98, 0.50, 0.12])
    
    bloom_rgb = b_tight * 1.4 * c_white + \
                b_mid   * 1.1 * c_gold + \
                b_wide  * 0.6 * c_amber
    
    bloom_lum = 0.299 * bloom_rgb[:, :, 0:1] + 0.587 * bloom_rgb[:, :, 1:2] + 0.114 * bloom_rgb[:, :, 2:3]
    bloom_a = np.clip(bloom_lum * 1.5, 0.0, 1.0)
    
    # Mặt nạ viền góc để bảo đảm alpha = 0 ở r >= 240px
    H, W = 512, 512
    y, x = np.ogrid[:H, :W]
    r = np.sqrt((x - 255.5)**2 + (y - 255.5)**2) / 512.0
    quad_mask = np.clip((0.48 - r) / 0.14, 0.0, 1.0)[:, :, np.newaxis]
    bloom_a *= quad_mask
    
    # Khuếch đại nhẹ các pixel sáng của texture gốc
    boosted_tex = tex_rgb * (1.15 + 0.85 * tex_lum)
    
    # Hòa trộn: giữ trọn vẹn chi tiết texture, quầng sáng chỉ tỏa ra ngoài và tuyệt đối không xâm lấn lõi đen
    comb_rgb = boosted_tex * tex_a * (1.0 - is_void)
    comb_rgb += bloom_rgb * (1.0 - tex_a) * (1.0 - is_void)
    comb_rgb += tex_rgb * is_void
    
    comb_a = np.clip(tex_a + bloom_a * (1.0 - is_void), 0.0, 1.0) * quad_mask
    comb_a = np.maximum(comb_a, is_void)
    
    return Image.fromarray((np.clip(np.concatenate([comb_rgb, comb_a], axis=2), 0.0, 1.0) * 255.0).astype(np.uint8), mode='RGBA')

def assemble_base(grid, scale=8, canvas_size=512):
    h, w, _ = grid.shape
    im_small = Image.fromarray(grid, mode='RGBA')
    im_upscaled = im_small.resize((w * scale, h * scale), Image.NEAREST)

    canvas = Image.new('RGBA', (canvas_size, canvas_size), (0, 0, 0, 0))
    pos_x = (canvas_size - w * scale) // 2
    pos_y = (canvas_size - h * scale) // 2
    canvas.paste(im_upscaled, (pos_x, pos_y), im_upscaled)
    return canvas

def main():
    print("Generating all 4 celestial stages with True Texture-Contour Bloom...")
    g1 = generate_stage1()
    g2 = generate_stage2()
    g3 = generate_stage3()
    g4 = generate_stage4()

    # Step 1: Pixel Art Base Grid upscaled
    b1 = assemble_base(g1, scale=9)
    b2 = assemble_base(g2, scale=8)
    b3 = assemble_base(g3, scale=8)
    b4 = assemble_base(g4, scale=8)

    # Step 2: Apply physical multi-scale Bloom that conforms to each texture's exact shape
    t1 = apply_texture_contour_bloom(b1)
    t2 = apply_texture_contour_bloom(b2)
    t3 = apply_texture_contour_bloom(b3)
    t4 = apply_texture_contour_bloom(b4)

    # Save to mod assets
    base_path = "src/main/resources/assets/samsara/textures/environment"
    t1.save(f"{base_path}/sun_stage1.png")
    t2.save(f"{base_path}/sun_stage2.png")
    t3.save(f"{base_path}/sun_stage3.png")
    t4.save(f"{base_path}/sun_stage4.png")
    print("Saved sun_stage1.png to sun_stage4.png with Texture-Contour Bloom successfully!")

if __name__ == '__main__':
    main()
