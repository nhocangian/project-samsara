"""
Hạt và ánh sáng của The Womb.

  textures/particle/womb_mote.png   8x8    chấm trắng mép mềm (tô màu trong MoteParticle): bụi bị hút về Gargantua
  textures/block/titan_halo.png     32x32  vầng hào quang: vòng trắng ngà mép mềm (TitanHaloRenderer)

Chạy từ thư mục mod/:  python tools/generate_womb_particles.py
"""
import os

from PIL import Image

TEX = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "samsara",
                   "textures")
OUT = os.path.join(TEX, "particle")
IVORY = (255, 246, 226)


def halo():
    n = 32
    img = Image.new("RGBA", (n, n))
    for y in range(n):
        for x in range(n):
            dx = (x + 0.5) / n * 2 - 1
            dy = (y + 0.5) / n * 2 - 1
            r = (dx * dx + dy * dy) ** 0.5
            a = max(0.0, 1.0 - abs(r - 0.82) / 0.12) ** 1.5
            img.putpixel((x, y), IVORY + (int(230 * a),))
    img.save(os.path.join(TEX, "block", "titan_halo.png"))


def main():
    os.makedirs(OUT, exist_ok=True)
    n = 8
    img = Image.new("RGBA", (n, n))
    for y in range(n):
        for x in range(n):
            dx = (x + 0.5) / n * 2 - 1
            dy = (y + 0.5) / n * 2 - 1
            a = max(0.0, 1.0 - (dx * dx + dy * dy) ** 0.5) ** 1.6
            img.putpixel((x, y), (255, 255, 255, int(255 * a)))
    img.save(os.path.join(OUT, "womb_mote.png"))
    halo()
    print("ok")


if __name__ == "__main__":
    main()
