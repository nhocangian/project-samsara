"""
Tạo texture "không gian kéo dãn" cho hiệu ứng bị hút vào The Womb (WombTransitionClient).

Pixel art 128x128 (lọc nearest trong game, nên mép pixel vẫn sắc như Minecraft):
  - Các vệt sao bị kéo dãn toả ra từ tâm (hệ toạ độ cực, chia thành nhiều "tia").
  - Tâm trong suốt: đó là lỗ mà người chơi đang rơi vào.
  - Bảng màu hổ phách / tím của Gargantua (Stage 4), thỉnh thoảng có vệt trắng.
Texture dùng alpha thường (KHÔNG premultiplied); game vẽ nó với blend cộng (SRC_ALPHA, ONE).

Chạy từ thư mục mod/:  python tools/generate_womb_stretch.py
Kết quả: src/main/resources/assets/samsara/textures/misc/womb_stretch.png
         tools/preview_womb_stretch.png (ảnh xem trước trên nền đen, phóng to 4x)
"""
import math
import os
import random

import numpy as np
from PIL import Image

N = 128
C = (N - 1) / 2.0
RAYS = 150
SEED = 1998  # Nam_1998

PALETTE = [
    (255, 176, 64),   # hổ phách
    (255, 128, 40),   # cam đĩa bồi tụ
    (176, 96, 255),   # tím cực quang The Womb
    (120, 70, 220),   # tím đậm
    (255, 240, 220),  # trắng nóng
]

random.seed(SEED)
rays = []
for _ in range(RAYS):
    color = random.choices(PALETTE, weights=[5, 3, 4, 3, 1])[0]
    rays.append({
        "start": random.uniform(0.18, 0.75),   # bán kính bắt đầu (tỉ lệ so với cạnh)
        "length": random.uniform(0.25, 0.9),
        "intensity": random.uniform(0.35, 1.0),
        "color": color,
    })

img = np.zeros((N, N, 4), dtype=np.float32)
max_r = C
for y in range(N):
    for x in range(N):
        dx, dy = x - C, y - C
        r = math.hypot(dx, dy) / max_r
        if r < 0.12:
            continue
        theta = (math.atan2(dy, dx) + math.pi) / (2 * math.pi)
        ray = rays[int(theta * RAYS) % RAYS]
        t = (r - ray["start"]) / ray["length"]
        if t <= 0.0 or t >= 1.0:
            continue
        # Sáng dần rồi tắt ở đuôi, như vệt sao bị kéo dãn
        a = ray["intensity"] * math.sin(t * math.pi) ** 0.7
        # Mép ngoài texture mờ dần để khi phóng to không thấy viền vuông
        a *= max(0.0, min(1.0, (1.0 - r) * 4.0 + 0.3)) if r > 0.85 else 1.0
        # Lượng tử hoá alpha thành vài nấc -> cảm giác pixel art
        a = round(a * 5) / 5
        if a <= 0:
            continue
        img[y, x, :3] = ray["color"]
        img[y, x, 3] = a * 255

out_dir = os.path.join("src", "main", "resources", "assets", "samsara", "textures", "misc")
os.makedirs(out_dir, exist_ok=True)
tex = Image.fromarray(img.clip(0, 255).astype(np.uint8), "RGBA")
tex.save(os.path.join(out_dir, "womb_stretch.png"))

preview = Image.new("RGBA", (N, N), (5, 2, 8, 255))
preview.alpha_composite(tex)
preview.resize((N * 4, N * 4), Image.NEAREST).save(os.path.join("tools", "preview_womb_stretch.png"))
print("Đã ghi womb_stretch.png và tools/preview_womb_stretch.png")
