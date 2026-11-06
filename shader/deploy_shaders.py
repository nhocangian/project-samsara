import os
import shutil
import zipfile

src = r"D:\c\shaderpacks\Samsara-Cosmic-Shaders"
dest_folder = r"C:\Users\chann\AppData\Roaming\PrismLauncher\instances\1.20.1\minecraft\shaderpacks\Samsara-Cosmic-Shaders"
dest_zip = r"C:\Users\chann\AppData\Roaming\PrismLauncher\instances\1.20.1\minecraft\shaderpacks\Samsara-Cosmic-Shaders.zip"

print(f"Deploying shaderpack from {src}...")

# 1. Copy folder (use copytree with dirs_exist_ok=True to avoid PermissionError when Minecraft is running)
shutil.copytree(src, dest_folder, dirs_exist_ok=True)
print("Folder synced successfully.")

# 2. Build compliant ZIP with directory entries and forward slashes
if os.path.exists(dest_zip):
    os.remove(dest_zip)

with zipfile.ZipFile(dest_zip, "w", zipfile.ZIP_DEFLATED) as z:
    zInfo = zipfile.ZipInfo("shaders/")
    z.writestr(zInfo, "")
    for root, dirs, files in os.walk(src):
        for d in dirs:
            abs_d = os.path.join(root, d)
            rel_d = os.path.relpath(abs_d, src).replace("\\", "/") + "/"
            if rel_d != "shaders/":
                zInfo = zipfile.ZipInfo(rel_d)
                z.writestr(zInfo, "")
        for f in files:
            abs_f = os.path.join(root, f)
            rel_f = os.path.relpath(abs_f, src).replace("\\", "/")
            z.write(abs_f, rel_f)

print("ZIP archive created with valid forward-slash directory entries.")

# Verify
with zipfile.ZipFile(dest_zip, "r") as z:
    assert "shaders/" in z.namelist(), "ERROR: shaders/ entry missing!"
    print(f"Verified {len(z.namelist())} entries in {dest_zip}. shaders/ confirmed present!")
