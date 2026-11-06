package com.samsara.worldgen.megalith;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * The Pale Colossus (Cự Thần Bạch Tạng Đứng Trong Tầng Mây - Tham chiếu Ảnh 3).
 * A petrified colossal humanoid titan standing ~240 blocks tall (Y=62 to Y=300).
 * Features an emaciated ribbed torso, a colossal reaching hand hovering near the ground,
 * and a gaping screaming mouth piercing the sky.
 */
public class PetrifiedTitanFeature {

    public static boolean generate(ServerWorld world, int centerX, int centerZ) {
        int baseY = 62;
        BlockState calcite = Blocks.CALCITE.getDefaultState();
        BlockState diorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState boneBlock = Blocks.BONE_BLOCK.getDefaultState();
        BlockState blackstone = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState tintedGlass = Blocks.TINTED_GLASS.getDefaultState();
        BlockState deepslate = Blocks.DEEPSLATE_TILES.getDefaultState();

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // 1. CHÂN TRÁI VÀ CHÂN PHẢI (Legs & Feet: Y=62 to Y=165)
        int legSpacing = 22; // Mỗi chân cách tâm 22 block
        int legLength = 103;

        for (int side : new int[]{-1, 1}) {
            int legX = centerX + side * legSpacing;
            for (int dy = 0; dy <= legLength; dy++) {
                int y = baseY + dy;
                float progress = (float) dy / (float) legLength;

                // Độ dày của chân (Cổ chân r=5, Bắp chân r=7, Đùi r=9)
                int radius = (int) (5.0f + Math.sin(progress * Math.PI) * 2.5f + progress * 2.0f);
                // Đầu gối hơi gập nhẹ về phía trước ở Y = baseY + 50
                int kneeOffsetZ = (int) (Math.sin(progress * Math.PI) * 4.0f);

                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx * dx + dz * dz <= radius * radius) {
                            mutable.set(legX + dx, y, centerZ + dz + kneeOffsetZ);
                            BlockState mat = ((dx + dy + dz) % 7 == 0) ? diorite : calcite;
                            if (dy < 6) mat = deepslate; // Bàn chân cắm vào đất
                            world.setBlockState(mutable, mat, 2);
                        }
                    }
                }
            }
        }

        // 2. KHUNG CHẬU VÀ THÂN MÌNH (Pelvis & Emaciated Ribbed Torso: Y=165 to Y=235)
        int torsoBaseY = baseY + legLength;
        int torsoHeight = 72;

        for (int dy = 0; dy <= torsoHeight; dy++) {
            int y = torsoBaseY + dy;
            float p = (float) dy / (float) torsoHeight;

            // Chiều ngang thân (Hông rộng 32, Eo thon hẹp 22, Ngực mở rộng 44)
            int halfW = (int) (18.0f - Math.sin(p * Math.PI * 0.9f) * 6.0f + p * 12.0f);
            int halfD = (int) (11.0f - Math.sin(p * Math.PI * 0.9f) * 3.0f + p * 4.0f);

            // Gân sườn nhô ra mỗi 6 block dọc theo lồng ngực (Ribs)
            boolean isRibLevel = (dy > 20 && dy < 58 && dy % 6 == 0);
            int ribExtra = isRibLevel ? 2 : 0;

            for (int dx = -halfW - ribExtra; dx <= halfW + ribExtra; dx++) {
                for (int dz = -halfD - ribExtra; dz <= halfD + ribExtra; dz++) {
                    float normX = (float) dx / (float) (halfW + ribExtra);
                    float normZ = (float) dz / (float) (halfD + ribExtra);

                    if (normX * normX + normZ * normZ <= 1.0f) {
                        mutable.set(centerX + dx, y, centerZ + dz);

                        // Hốc rỗng ở giữa bụng/ngực tạo vẻ khô gầy ghê rợn
                        if (Math.abs(normX) < 0.25f && dz < 0 && dy > 15 && dy < 50) {
                            world.setBlockState(mutable, cryingObsidian, 2);
                        } else if (isRibLevel && (normX * normX + normZ * normZ > 0.85f)) {
                            world.setBlockState(mutable, boneBlock, 2);
                        } else {
                            world.setBlockState(mutable, ((dx + dy) % 4 == 0) ? diorite : calcite, 2);
                        }
                    }
                }
            }
        }

        // 3. CÁNH TAY KHỔNG LỒ THÒ XUỐNG ĐẤT (Reaching Hand - Tay phải của Titan, bên trái người nhìn)
        int shoulderY = torsoBaseY + torsoHeight - 6;
        int shoulderX = centerX - 32;

        // Cánh tay buông thõng từ Y=230 xuống tận Y=95 (Dài 135 blocks!)
        for (int armDy = 0; armDy <= 125; armDy++) {
            int y = shoulderY - armDy;
            int r = (int) (6.0f - (armDy / 125.0f) * 1.5f);
            // Cánh tay hơi vươn ra phía trước
            int armZ = centerZ + (int) ((armDy / 125.0f) * 16.0f);

            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r) {
                        mutable.set(shoulderX + dx, y, armZ + dz);
                        world.setBlockState(mutable, calcite, 2);
                    }
                }
            }
        }

        // BÀN TAY VÀ CÁC NGÓN TAY DÀI LÒNG THÒNG (Gargantuan Drooping Hand at Y=95 down to Y=72)
        int wristY = shoulderY - 125;
        int wristZ = centerZ + 16;
        for (int f = -2; f <= 2; f++) {
            int fingerX = shoulderX + f * 4;
            int fingerLen = 22 - Math.abs(f) * 3;
            for (int fDy = 0; fDy <= fingerLen; fDy++) {
                int y = wristY - fDy;
                // Ngón tay khum lại như muốn chộp lấy mặt đất
                int fz = wristZ + (int) Math.sin((float) fDy / fingerLen * Math.PI * 0.7f) * 5;
                for (int fdx = -1; fdx <= 1; fdx++) {
                    for (int fdz = -1; fdz <= 1; fdz++) {
                        mutable.set(fingerX + fdx, y, fz + fdz);
                        BlockState mat = (fDy > fingerLen - 4) ? cryingObsidian : calcite;
                        world.setBlockState(mutable, mat, 2);
                    }
                }
            }
        }

        // Cánh tay trái (Viewer's right: Buông xuôi tự nhiên)
        int shoulderLeftX = centerX + 32;
        for (int armDy = 0; armDy <= 75; armDy++) {
            int y = shoulderY - armDy;
            int r = 5;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r) {
                        mutable.set(shoulderLeftX + dx, y, centerZ + dz);
                        world.setBlockState(mutable, calcite, 2);
                    }
                }
            }
        }

        // 4. CỔ VÀ ĐẦU VỚI MIỆNG HÁ HỐC GÀO THÉT (Head & Gaping Scream Jaw: Y=235 to Y=298)
        int headBaseY = shoulderY + 8;
        int headHeight = 44;
        int headW = 12;
        int headD = 14;

        for (int dy = 0; dy <= headHeight; dy++) {
            int y = headBaseY + dy;
            float hp = (float) dy / (float) headHeight;

            int curW = (int) (headW * Math.sin(hp * Math.PI));
            int curD = (int) (headD * Math.sin(hp * Math.PI));
            if (curW < 3) curW = 3;
            if (curD < 3) curD = 3;

            for (int dx = -curW; dx <= curW; dx++) {
                for (int dz = -curD; dz <= curD; dz++) {
                    float nx = (float) dx / (float) curW;
                    float nz = (float) dz / (float) curD;

                    if (nx * nx + nz * nz <= 1.0f) {
                        mutable.set(centerX + dx, y, centerZ + dz);

                        // MIỆNG HÁ TO KINH HOÀNG (Gaping Screaming Abyss: dy từ 6 đến 26, hướng mặt về Z âm)
                        boolean isMouthZone = (dy >= 6 && dy <= 26 && dz < -curD / 2 && Math.abs(dx) <= curW / 2);
                        // MẮT TRỐNG RỖNG (Hollow Eyes: dy từ 30 đến 34, dx = +-4, dz < -curD / 2)
                        boolean isEyeZone = (dy >= 30 && dy <= 34 && (Math.abs(dx) == 4 || Math.abs(dx) == 5) && dz < -curD / 2);

                        if (isMouthZone) {
                            world.setBlockState(mutable, cryingObsidian, 2);
                        } else if (isEyeZone) {
                            world.setBlockState(mutable, blackstone, 2);
                        } else {
                            world.setBlockState(mutable, ((dx + dy) % 3 == 0) ? diorite : calcite, 2);
                        }
                    }
                }
            }
        }

        return true;
    }
}
