import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont

# ==========================================
# COLOR PALETTE (RGB + Alpha)
# ==========================================
WHITE        = [255, 255, 255, 255]
LIGHT_GOLD   = [255, 246, 150, 255]
WARM_YELLOW  = [255, 230,  85, 255]
SOLAR_GOLD   = [255, 200,  35, 255]
AMBER_GOLD   = [255, 165,  20, 255]
FIERY_ORANGE = [255, 120,  15, 255]
NEON_ORANGE  = [255,  85,  10, 255]
DEEP_AMBER   = [220,  65,  10, 255]
DARK_AMBER   = [170,  45,   8, 255]
CRIMSON_DUST = [120,  28,   6, 230]
VOID_INNER   = [ 20,   8,  16, 250]
VOID_BLACK   = [  5,   2,   8, 255]

def create_circular_mask(canvas_size=512, r_start=180, r_end=215):
    """Creates a smoothstep circular fade to ensure 0 alpha at texture boundaries"""
    arr = np.zeros((canvas_size, canvas_size), dtype=np.float32)
    cx, cy = (canvas_size - 1) / 2.0, (canvas_size - 1) / 2.0
    y, x = np.ogrid[:canvas_size, :canvas_size]
    r = np.sqrt((x - cx)**2 + (y - cy)**2)
    
    # 1.0 inside r_start, 0.0 outside r_end, smoothstep in between
    t = np.clip((r - r_start) / (r_end - r_start), 0.0, 1.0)
    smooth = 1.0 - (t * t * (3.0 - 2.0 * t))
    return smooth

# ==============================================================================
# STAGE 1: Fractured Square Sun (Mặt Trời Rạn Nứt & Mắt Kỷ Dị)
# ==============================================================================
def generate_stage1():
    W, H = 36, 36
    grid = np.zeros((H, W, 4), dtype=np.uint8)
    cx, cy = 17.5, 17.5

    # 1. Square sun base (22x22 from x=7..28, y=7..28)
    for y in range(H):
        for x in range(W):
            if 7 <= x <= 28 and 7 <= y <= 28:
                # Distance from square boundary
                edge_dist = min(x - 7, 28 - x, y - 7, 28 - y)
                if edge_dist == 0:
                    grid[y, x] = FIERY_ORANGE
                elif edge_dist == 1:
                    grid[y, x] = SOLAR_GOLD
                elif edge_dist == 2:
                    grid[y, x] = WARM_YELLOW
                else:
                    grid[y, x] = LIGHT_GOLD

    # 2. Outer square corona glow (1-2 px soft halo)
    for y in range(H):
        for x in range(W):
            if not (7 <= x <= 28 and 7 <= y <= 28):
                dx = max(0, 7 - x, x - 28)
                dy = max(0, 7 - y, y - 28)
                dist = np.sqrt(dx*dx + dy*dy)
                if dist <= 3.5:
                    alpha = int(220 * (1.0 - dist / 3.5))
                    grid[y, x] = [255, 175, 30, alpha]

    # 3. Central nascent singularity eye (opening at center)
    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            r = np.sqrt(dx*dx + dy*dy)
            if r <= 3.2:
                grid[y, x] = VOID_BLACK
            elif r <= 4.2:
                grid[y, x] = VOID_INNER
            elif r <= 5.0:
                grid[y, x] = WHITE

    # 4. Jagged hairline fractures propagating outward
    cracks = [
        # Top-Right crack
        [(18, 14), (19, 13), (20, 11), (22, 10), (23, 8), (25, 7)],
        # Bottom-Left crack
        [(17, 21), (15, 22), (14, 24), (12, 25), (10, 27), (8, 28)],
        # Top-Left branching crack
        [(14, 16), (13, 14), (11, 13), (10, 11), (8, 10), (7, 9)],
        # Bottom-Right crack
        [(21, 19), (22, 21), (24, 22), (26, 24), (27, 26), (28, 27)],
        # Lateral micro fractures
        [(14, 18), (11, 18), (9, 17), (7, 18)],
        [(21, 17), (24, 17), (26, 18), (28, 17)]
    ]
    for branch in cracks:
        for px, py in branch:
            if 0 <= px < W and 0 <= py < H:
                grid[py, px] = WHITE
                # Border pixels of cracks in intense fiery neon orange
                for nx, ny in [(px-1, py), (px+1, py), (px, py-1), (px, py+1)]:
                    if 0 <= nx < W and 0 <= ny < H and 7 <= nx <= 28 and 7 <= ny <= 28:
                        if not np.array_equal(grid[ny, nx], VOID_BLACK) and not np.array_equal(grid[ny, nx], WHITE):
                            grid[ny, nx] = NEON_ORANGE

    return grid

# ==============================================================================
# STAGE 2: Face-On Cosmic Eye (Con Mắt Hư Không / AnasAbdin Style) - Image 2
# ==============================================================================
def generate_stage2():
    W, H = 40, 40
    grid = np.zeros((H, W, 4), dtype=np.uint8)
    cx, cy = 19.5, 19.5

    # Deterministic pseudo-random noise for stellar dust / dithering sparkles
    def hash2d(x, y):
        n = (x * 374761393 + y * 668265263) ^ 0x5bf03635
        n = (n ^ (n >> 13)) * 1274126177
        return (n & 0x7fffffff) / float(0x7fffffff)

    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            r = np.sqrt(dx*dx + dy*dy)
            angle = math.atan2(dy, dx)
            h = hash2d(x, y)

            # Central Black Hole Event Horizon
            if r <= 6.8:
                grid[y, x] = VOID_BLACK
            elif r <= 7.8:
                grid[y, x] = VOID_INNER
            # Brilliant Core Photon Ring
            elif r <= 8.8:
                # Alternating white-gold highlights
                if (int(r * 2) + int((angle + math.pi) * 3)) % 2 == 0 or r <= 8.3:
                    grid[y, x] = WHITE
                else:
                    grid[y, x] = LIGHT_GOLD
            elif r <= 10.2:
                grid[y, x] = SOLAR_GOLD
            elif r <= 11.5:
                grid[y, x] = FIERY_ORANGE
            # Concentric Dust Rings & Sparkle Bands (AnasAbdin style)
            # Band 1: Dense inner dust & orbit highlights (12.2 - 13.8)
            elif 12.2 <= r <= 13.8:
                if h > 0.40:
                    if h > 0.88:
                        grid[y, x] = LIGHT_GOLD
                    elif h > 0.65:
                        grid[y, x] = AMBER_GOLD
                    else:
                        grid[y, x] = DEEP_AMBER
            # Gap (13.8 - 14.8): Darker, subtle sparse dust
            elif 14.0 <= r <= 14.8:
                if h > 0.80:
                    grid[y, x] = DARK_AMBER
            # Band 2: Intermediate Keplerian orbital stream (15.0 - 16.8)
            elif 15.0 <= r <= 16.8:
                if h > 0.42:
                    if h > 0.86:
                        grid[y, x] = SOLAR_GOLD
                    elif h > 0.65:
                        grid[y, x] = DEEP_AMBER
                    else:
                        grid[y, x] = DARK_AMBER
            # Band 3: Outer stellar dust & sparkling stars (17.2 - 19.0)
            elif 17.2 <= r <= 19.0:
                if h > 0.58:
                    if h > 0.92:
                        grid[y, x] = WARM_YELLOW
                    elif h > 0.78:
                        grid[y, x] = DARK_AMBER
                    else:
                        grid[y, x] = CRIMSON_DUST

    return grid

# ==============================================================================
# STAGE 3: Relativistic Plasma Vortex (Bão Plasma Xoáy Cuộn / Neon Flare) - Image 3
# ==============================================================================
def generate_stage3():
    W, H = 42, 36
    grid = np.zeros((H, W, 4), dtype=np.uint8)
    cx, cy = 20.5, 17.5

    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            # Elliptical distortion
            r_ellip = np.sqrt((dx / 6.2)**2 + (dy / 5.2)**2)
            r = np.sqrt(dx*dx + dy*dy)
            angle = math.atan2(dy, dx)

            # Central Black Hole Shadow
            if r_ellip <= 0.88:
                grid[y, x] = VOID_BLACK
                continue
            elif r_ellip <= 1.05:
                grid[y, x] = VOID_INNER
                continue

            # Swirling spiral dynamic: angle warped by logarithmic spiral
            spiral_angle = angle + 1.9 * math.log(max(0.1, r / 5.0))
            wave = (math.sin(spiral_angle * 2.0) + 1.0) * 0.5
            
            # Relativistic Doppler factor: approaching (left, dx < 0) vs receding (right, dx > 0)
            doppler = (-dx / 20.0)

            # 1. Swirling inner relativistic vortex (r between 5.5 and 11.0)
            if 5.5 <= r <= 11.5:
                factor = wave + doppler * 0.5
                if factor > 0.90:
                    grid[y, x] = WHITE
                elif factor > 0.70:
                    grid[y, x] = LIGHT_GOLD
                elif factor > 0.45:
                    grid[y, x] = SOLAR_GOLD
                elif factor > 0.20:
                    grid[y, x] = FIERY_ORANGE
                else:
                    grid[y, x] = DEEP_AMBER

            # 2. Sweeping Fiery Wings (Image 3 dynamic flare wings)
            # Left upper wing: arched over the top
            if -18.0 <= dx <= -4.0 and -11.0 <= dy <= 2.0:
                wing_dist = abs(dy - (-0.4 * dx - 5.0))
                if wing_dist <= 2.2:
                    if wing_dist <= 0.9:
                        grid[y, x] = LIGHT_GOLD if dx > -12.0 else SOLAR_GOLD
                    else:
                        grid[y, x] = FIERY_ORANGE

            # Right lower wing: long swept-back relativistic plasma jet/wing
            if 4.0 <= dx <= 20.0 and -2.0 <= dy <= 7.0:
                wing_dist = abs(dy - (0.28 * dx - 0.5))
                if wing_dist <= 2.5 * (1.0 - (dx - 4.0) / 18.0):
                    if dx <= 11.0 and wing_dist <= 0.8:
                        grid[y, x] = SOLAR_GOLD
                    elif dx <= 15.0:
                        grid[y, x] = FIERY_ORANGE
                    elif dx <= 18.5:
                        grid[y, x] = DEEP_AMBER
                    else:
                        grid[y, x] = DARK_AMBER

            # Outer swirling filament streamers
            if 11.5 < r <= 16.5 and wave > 0.72:
                if doppler > 0.0:
                    grid[y, x] = SOLAR_GOLD if wave > 0.85 else FIERY_ORANGE
                else:
                    grid[y, x] = DEEP_AMBER if wave > 0.85 else DARK_AMBER

    return grid

# ==============================================================================
# STAGE 4: Cinematic Fine-Ring Gargantua (Siêu Hố Đen Nghệ Thuật) - Image 1
# ==============================================================================
def generate_stage4():
    W, H = 42, 36
    grid = np.zeros((H, W, 4), dtype=np.uint8)
    cx, cy = 20.5, 17.5

    # 15 degree tilt angle
    tilt_rad = math.radians(-14.0)
    cos_t = math.cos(tilt_rad)
    sin_t = math.sin(tilt_rad)

    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            # Rotated coordinates (u: along accretion disk, v: perpendicular)
            u = dx * cos_t - dy * sin_t
            v = dx * sin_t + dy * cos_t

            # Central Black Hole Shadow (slightly oblate)
            shadow_d = (u / 8.6)**2 + (v / 7.6)**2
            if shadow_d <= 0.86:
                grid[y, x] = VOID_BLACK
                continue
            elif shadow_d <= 1.0:
                grid[y, x] = VOID_INNER
                continue

            # Upper Gravitational Lensing Arc (bent behind & over the singularity)
            if v <= 0.8:
                r_arc = math.sqrt((u / 14.5)**2 + (v / 12.8)**2)
                if 0.65 <= r_arc <= 1.05 and abs(u) <= 15.5:
                    # Fine concentric 1px spectral ringlets (Image 1 Interstellar style!)
                    if 0.67 <= r_arc < 0.74:
                        grid[y, x] = WHITE
                    elif 0.74 <= r_arc < 0.81:
                        grid[y, x] = LIGHT_GOLD
                    elif 0.81 <= r_arc < 0.88:
                        grid[y, x] = SOLAR_GOLD
                    elif 0.88 <= r_arc < 0.95:
                        grid[y, x] = FIERY_ORANGE
                    elif 0.95 <= r_arc <= 1.02:
                        grid[y, x] = DEEP_AMBER

            # Lower Gravitational Lensing Arc (bent beneath the singularity)
            if v >= -0.8:
                r_arc_low = math.sqrt((u / 14.5)**2 + (v / 12.8)**2)
                if 0.65 <= r_arc_low <= 1.05 and abs(u) <= 15.5:
                    if 0.67 <= r_arc_low < 0.74:
                        grid[y, x] = WHITE
                    elif 0.74 <= r_arc_low < 0.81:
                        grid[y, x] = LIGHT_GOLD
                    elif 0.81 <= r_arc_low < 0.88:
                        grid[y, x] = SOLAR_GOLD
                    elif 0.88 <= r_arc_low < 0.95:
                        grid[y, x] = FIERY_ORANGE
                    elif 0.95 <= r_arc_low <= 1.02:
                        grid[y, x] = DEEP_AMBER

            # Equatorial Accretion Disk (crossing through front and sides)
            disk_thick = 2.4 * max(0.0, 1.0 - (abs(u) / 20.0)**1.6)
            if abs(v) <= disk_thick and abs(u) <= 20.0:
                # Doppler boosting: approaching side (u < 0) brighter than receding (u > 0)
                d_norm = abs(v) / max(0.01, disk_thick)
                if u < 0:
                    # Approaching side (brilliant)
                    if d_norm <= 0.35:
                        grid[y, x] = WHITE
                    elif d_norm <= 0.65:
                        grid[y, x] = LIGHT_GOLD
                    elif d_norm <= 0.85:
                        grid[y, x] = SOLAR_GOLD
                    else:
                        grid[y, x] = FIERY_ORANGE
                else:
                    # Receding side (warmer amber/orange)
                    if d_norm <= 0.25:
                        grid[y, x] = LIGHT_GOLD
                    elif d_norm <= 0.55:
                        grid[y, x] = SOLAR_GOLD
                    elif d_norm <= 0.80:
                        grid[y, x] = FIERY_ORANGE
                    else:
                        grid[y, x] = DEEP_AMBER

                # Tapered diamond tips at wings
                if abs(u) >= 17.0:
                    grid[y, x] = DARK_AMBER if abs(u) >= 18.5 else DEEP_AMBER

    # Final touch: Front crossing of the accretion disk across the bottom of the shadow
    # To give authentic 3D Interstellar perspective, the front disk passes OVER the shadow!
    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            u = dx * cos_t - dy * sin_t
            v = dx * sin_t + dy * cos_t
            # Front equator (v between 0.0 and 2.2, inside shadow region)
            if 0.0 <= v <= 2.2 and abs(u) <= 8.5:
                d_norm = v / 2.2
                if d_norm <= 0.4:
                    grid[y, x] = WHITE if u < 0 else LIGHT_GOLD
                elif d_norm <= 0.75:
                    grid[y, x] = SOLAR_GOLD
                else:
                    grid[y, x] = FIERY_ORANGE

    return grid

# ==============================================================================
# UPSCALING & INTEGRATION
# ==============================================================================
def assemble_texture(grid, scale=10, canvas_size=512):
    """Upscales pixel art grid with NEAREST and pastes onto 512x512 canvas with smooth alpha falloff"""
    h, w, _ = grid.shape
    im_small = Image.fromarray(grid, mode='RGBA')
    im_upscaled = im_small.resize((w * scale, h * scale), Image.NEAREST)

    canvas = Image.new('RGBA', (canvas_size, canvas_size), (0, 0, 0, 0))
    pos_x = (canvas_size - w * scale) // 2
    pos_y = (canvas_size - h * scale) // 2
    canvas.paste(im_upscaled, (pos_x, pos_y), im_upscaled)

    # Apply circular falloff mask to ensure strict 0 alpha at r >= 215
    mask = create_circular_mask(canvas_size=canvas_size, r_start=180, r_end=215)
    c_arr = np.array(canvas)
    c_arr[:, :, 3] = (c_arr[:, :, 3].astype(np.float32) * mask).astype(np.uint8)
    return Image.fromarray(c_arr, mode='RGBA')

def main():
    print("Generating all 4 celestial stages...")
    g1 = generate_stage1()
    g2 = generate_stage2()
    g3 = generate_stage3()
    g4 = generate_stage4()

    t1 = assemble_texture(g1, scale=10)
    t2 = assemble_texture(g2, scale=10)
    t3 = assemble_texture(g3, scale=10)
    t4 = assemble_texture(g4, scale=10)

    # Save to mod assets
    base_path = "src/main/resources/assets/samsara/textures/environment"
    t1.save(f"{base_path}/sun_stage1.png")
    t2.save(f"{base_path}/sun_stage2.png")
    t3.save(f"{base_path}/sun_stage3.png")
    t4.save(f"{base_path}/sun_stage4.png")
    print("Saved sun_stage1.png to sun_stage4.png successfully!")

    # Create a nice 4-stage preview collage for verification
    preview = Image.new('RGBA', (1024, 320), (12, 10, 20, 255))
    draw = ImageDraw.Draw(preview)

    stages = [
        (t1, "Stage 1: Mặt Trời Rạn Nứt", "Minecraft Square + Singularity Eye"),
        (t2, "Stage 2: Con Mắt Hư Không", "Face-On M87* & Dust Sparkles (Img 2)"),
        (t3, "Stage 3: Bão Plasma Xoáy Cuộn", "Relativistic Plasma Vortex (Img 3)"),
        (t4, "Stage 4: Siêu Hố Đen Gargantua", "Fine-Ring Accretion & 3D Tilt (Img 1)")
    ]

    for i, (tex, title, subtitle) in enumerate(stages):
        thumb = tex.resize((230, 230), Image.BILINEAR)
        px = 16 + i * 252
        preview.paste(thumb, (px, 20), thumb)
        draw.text((px + 10, 260), title, fill=(255, 220, 100))
        draw.text((px + 10, 280), subtitle, fill=(180, 180, 200))

    preview_path = "C:/Users/chann/.gemini/antigravity/brain/c1c06a3b-1512-4737-a40b-9f1afba36f42/all_4_stages_pixel_preview.png"
    preview.save(preview_path)
    print(f"Saved preview collage to {preview_path}")

if __name__ == '__main__':
    main()
