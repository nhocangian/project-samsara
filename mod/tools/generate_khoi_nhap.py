"""Sinh texture Khối Nháp: một màu đen đặc, KhoiNhapRenderer dùng để vẽ các thanh viền ô (như viền chọn block)."""
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/samsara/textures/block/khoi_nhap.png"
Image.new("RGBA", (16, 16), (8, 8, 10, 255)).save(OUT)
print(OUT)
