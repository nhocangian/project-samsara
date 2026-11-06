"""
Tạo texture cho Quả Trứng Nguyên Thủy (PrimordialEggEntity / PrimordialEggRenderer).

Model trứng được ghép từ nhiều khối vuông lớn (giống trứng Rồng Ender phóng to); mọi khối dùng
chung UV gốc (0, 0), nên texture chỉ cần là một tấm "vật liệu" phủ kín, đủ lớn cho khối to nhất.

  primordial_egg.png        512x256  Vỏ kim loại sẫm: nhiễu pixel tím than, vân tấm thép, đinh tán.
  primordial_egg_cracks.png 512x256  CHỈ các đường nứt màu hổ phách (nền trong suốt);
                                     game vẽ lớp này phát sáng (RenderLayer.getEyes).

Chạy từ thư mục mod/:  python tools/generate_primordial_egg.py
Kết quả trong src/main/resources/assets/samsara/textures/entity/
         tools/preview_primordial_egg.png (hai lớp chồng lên nhau, phóng to 2x)
"""
import os
import random

import numpy as np
from PIL import Image

W, H = 512, 256
SEED = 1998
rng = random.Random(SEED)
np_rng = np.random.default_rng(SEED)

# ---------------- Vỏ kim loại ----------------
base = np.zeros((H, W, 4), dtype=np.float32)
palette = np.array([
    (22, 18, 28), (28, 23, 36), (34, 28, 44), (40, 33, 52), (26, 22, 33),
], dtype=np.float32)
idx = np_rng.choice(len(palette), size=(H, W), p=[0.28, 0.30, 0.18, 0.08, 0.16])
base[..., :3] = palette[idx]
base[..., 3] = 255

# Vân tấm thép: các đường ngang tối mỗi 16 px, đinh tán sáng ở giao điểm
for y in range(0, H, 16):
    base[y, :, :3] *= 0.6
for y in range(8, H, 16):
    for x in range(8, W, 16):
        base[y, x, :3] = (70, 60, 86)

# ---------------- Đường nứt hổ phách ----------------
cracks = np.zeros((H, W, 4), dtype=np.float32)
AMBER = [(255, 176, 64), (255, 140, 40), (255, 214, 120)]


def crack(x, y, steps):
    """Đi ngẫu nhiên theo pixel (chủ yếu dọc), rẽ nhánh thỉnh thoảng."""
    for _ in range(steps):
        if 0 <= x < W and 0 <= y < H:
            cracks[y, x, :3] = rng.choice(AMBER)
            cracks[y, x, 3] = 255
            # Vầng sáng mờ hai bên
            for dx in (-1, 1):
                if 0 <= x + dx < W and cracks[y, x + dx, 3] == 0:
                    cracks[y, x + dx, :3] = (200, 90, 30)
                    cracks[y, x + dx, 3] = 90
        y += rng.choice((1, 1, 1, 0, -1))
        x += rng.choice((-1, 0, 0, 1))
        if rng.random() < 0.03:
            crack(x, y, steps // 3)


for _ in range(70):
    crack(rng.randrange(W), rng.randrange(H), rng.randrange(20, 70))

# Tối vỏ dưới đường nứt cho cảm giác sâu
mask = cracks[..., 3] > 200
base[mask, :3] = (12, 8, 14)

out_dir = os.path.join("src", "main", "resources", "assets", "samsara", "textures", "entity")
os.makedirs(out_dir, exist_ok=True)
base_img = Image.fromarray(base.clip(0, 255).astype(np.uint8), "RGBA")
crack_img = Image.fromarray(cracks.clip(0, 255).astype(np.uint8), "RGBA")
base_img.save(os.path.join(out_dir, "primordial_egg.png"))
crack_img.save(os.path.join(out_dir, "primordial_egg_cracks.png"))

preview = base_img.copy()
preview.alpha_composite(crack_img)
preview.crop((0, 0, 256, 128)).resize((512, 256), Image.NEAREST).save(os.path.join("tools", "preview_primordial_egg.png"))
print("Đã ghi primordial_egg.png, primordial_egg_cracks.png và tools/preview_primordial_egg.png")
