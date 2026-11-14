"""Nhập texture "bản Alpha" cho tầng 4 "Đêm đầu tiên" từ các gói có sẵn, thay cho texture tự vẽ trước đây.

Nguồn (theo thứ tự ưu tiên, giống cách xếp gói của chính Golden Days):
  1. Golden Days Alpha (PoeticRainbow), file zip anh tải về đặt ở gốc repo: cỏ, lá, rêu...
  2. Programmer Art (gói texture cũ của Mojang, có sẵn trong asset của Minecraft 1.20.1): đá, cuội, gỗ, cát...
  3. Texture vanilla trong minecraft-client.jar: khối mà thời Alpha trông như bây giờ (đất, len).
Lưu ý: texture Golden Days là của tác giả gói; kiểm tra quyền trước khi phát hành mod công khai.

Mỗi texture ghi ra là `<tên>_s.png` (bản đồ specular của Iris) cạnh texture vanilla cùng tên. Shader world6 đọc atlas
specular làm "mặt Alpha" của khối (xem `shader/.../lib/dem_dau_tien_tear.glsl`). Quy ước kênh alpha:
  255 = pixel có màu, 2 = trong suốt nhưng "khối này có bản Alpha", không có file = khối của bản mới (ẩn ngoài vết rách).

Golden Days tô sẵn màu cỏ/lá vào texture, còn Minecraft vẫn nhân thêm màu biome. Biome dem_dau_tien giữ màu cỏ/lá
đồng bằng của 1.20.1 (để cỏ bản mới trong vết rách đúng màu), nên script chia ngược texture Golden Days cho màu đó.
Kết quả có thể vượt 255, nên ghi một nửa giá trị kèm alpha = 254; shader thấy alpha 254 thì nhân đôi lại.
"""
import io
import json
import zipfile
from pathlib import Path

from PIL import Image

TOOLS = Path(__file__).resolve().parent
MOD = TOOLS.parent
REPO = MOD.parent
OUT = MOD / "src/main/resources/assets/minecraft/textures/block"
BIOME = MOD / "src/main/resources/data/samsara/worldgen/biome/dem_dau_tien.json"
LOOM = Path.home() / ".gradle/caches/fabric-loom"

GOLDEN = next(REPO.glob("golden-days-alpha-*.zip"))
PROGRAMMER = LOOM / "assets/objects/14/1480c062cb7ff8ef635875401f959af86b6d01dd"  # minecraft/resourcepacks/programmer_art.zip
CLIENT = LOOM / "1.20.1/minecraft-client.jar"

BLOCK = "assets/minecraft/textures/block/"

# tên texture -> cách tô màu của Minecraft ("grass", "foliage" hoặc None)
TEXTURES = {
    "grass_block_top": "grass",
    "grass_block_side": None,
    "grass_block_side_overlay": "grass",
    "dirt": None,
    "stone": None,
    "cobblestone": None,
    "oak_log": None,
    "oak_log_top": None,
    "oak_leaves": "foliage",
    "oak_planks": None,
    "sand": None,
    "gravel": None,
    "white_wool": None,
    "crafting_table_top": None,
    "crafting_table_side": None,
    "crafting_table_front": None,
    "dandelion": None,
    "poppy": None,
    "coal_ore": None,
    "iron_ore": None,
    # Đồ người chơi hay làm / đặt trong đêm đầu tiên: phải có bản Alpha, không thì ra ngoài vết rách sẽ vô hình
    "torch": None,
    "furnace_front": None,
    "furnace_side": None,
    "furnace_top": None,
    "oak_door_top": None,
    "oak_door_bottom": None,
    "ladder": None,
    "oak_sapling": None,
}
# Công trình của bản mới trong vết rách cũng có "bản cũ" bên dưới: lúc vết rách co lại sát vào thì lộ ra bản cũ của
# chính nó (nhà gỗ cũ, cây sồi cũ) thay vì biến mất. tên texture mới -> texture Alpha dùng làm bản cũ (màu giữ nguyên,
# vì các khối này không được tô màu biome). Khối không có ở đây (đèn lồng, cánh hoa...) thì ẩn hẳn ngoài vết rách.
# Khối Chưa Tải ăn dần mép đảo
MOD_BLOCKS = ["khoi_chua_tai"]

OLD_SELF = {
    "mud_bricks": "oak_planks",
    "spruce_planks": "oak_planks",
    "barrel_side": "oak_planks",
    "barrel_top": "oak_planks",
    "barrel_bottom": "oak_planks",
    "cherry_log": "oak_log",
    "cherry_log_top": "oak_log_top",
    "cherry_leaves": "oak_leaves",
}

# Màu cỏ / lá đồng bằng của 1.20.1, cũng là màu của biome dem_dau_tien
TINTS = {"grass": (145, 189, 89), "foliage": (119, 171, 47)}


def load(name):
    for src in (GOLDEN, PROGRAMMER, CLIENT):
        with zipfile.ZipFile(src) as z:
            try:
                data = z.read(BLOCK + name + ".png")
            except KeyError:
                continue
        im = Image.open(io.BytesIO(data)).convert("RGBA")
        if im.height > im.width:  # texture động: lấy khung đầu
            im = im.crop((0, 0, im.width, im.width))
        return im, src.name
    raise FileNotFoundError(name)


def finish(im, tint=None):
    out = Image.new("RGBA", im.size)
    px, dst = im.load(), out.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a < 128:
                dst[x, y] = (0, 0, 0, 2)
                continue
            if tint:
                r, g, b = (min(255, c * 255 // t // 2) for c, t in zip((r, g, b), tint))
                dst[x, y] = (r, g, b, 254)
            else:
                dst[x, y] = (r, g, b, 255)
    return out


def main():
    loaded = {n: load(n) for n in TEXTURES}
    OUT.mkdir(parents=True, exist_ok=True)
    for name, kind in TEXTURES.items():
        im, src = loaded[name]
        # Chỉ chia ngược texture đã tô sẵn màu (của Golden Days); texture xám thì để Minecraft tô như thường
        tint = TINTS[kind] if kind and src == GOLDEN.name else None
        finish(im, tint).save(OUT / f"{name}_s.png")
        label = "programmer_art" if src == PROGRAMMER.name else src
        print(f"{name:28s} <- {label}")
    # Khối của mod dùng trên đảo: bản Alpha chính là texture của nó (để không bị ẩn ngoài vết rách)
    for name in MOD_BLOCKS:
        src = MOD / f"src/main/resources/assets/samsara/textures/block/{name}.png"
        out = MOD / f"src/main/resources/assets/samsara/textures/block/{name}_s.png"
        finish(Image.open(src).convert("RGBA")).save(out)
        print(f"{name:28s} <- samsara (itself)")
    for name, base in OLD_SELF.items():
        finish(load(base)[0]).save(OUT / f"{name}_s.png")
        print(f"{name:28s} <- old {base}")
    biome = json.loads(BIOME.read_text(encoding="utf8"))
    to_int = lambda c: (c[0] << 16) | (c[1] << 8) | c[2]
    biome["effects"]["grass_color"] = to_int(TINTS["grass"])
    biome["effects"]["foliage_color"] = to_int(TINTS["foliage"])
    BIOME.write_text(json.dumps(biome, indent=2) + "\n", encoding="utf8")


if __name__ == "__main__":
    main()
