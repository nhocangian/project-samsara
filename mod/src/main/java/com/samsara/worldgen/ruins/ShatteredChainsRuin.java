package com.samsara.worldgen.ruins;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * Phế Tích Đại Xích Trói Trời Đứt Đoạn (True Colossal Shattered Sky-Chain Ruin).
 * Quy mô cự đại chọc thủng tầng mây:
 * - Vươn cao từ mặt đất/biển lên tận Y = 318 (sát trần xây dựng thế giới).
 * - Mắt xích khổng lồ dài 48 blocks, rộng 28 blocks, thân xích dày 6 blocks nguyên khối.
 * - Đứt gãy dữ dội ở độ cao Y ~ 240 trong tầng mây, rỉ năng lượng Crying Obsidian.
 * - Dưới mặt đất là hố va chạm rộng 60 blocks với mắt xích đứt lìa cắm nghiêng vào núi đá.
 */
public class ShatteredChainsRuin {

    public static boolean generate(ServerWorld world, int centerX, int startY, int centerZ) {
        Random rand = new Random(centerX * 3137L + centerZ * 8831L);
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        BlockState deepslate = Blocks.DEEPSLATE_BRICKS.getDefaultState();
        BlockState crackedDeepslate = Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState smoothBasalt = Blocks.SMOOTH_BASALT.getDefaultState();
        BlockState tuff = Blocks.TUFF.getDefaultState();
        BlockState moss = Blocks.MOSS_BLOCK.getDefaultState();

        // 1. HỐ VA CHẠM CỰ ĐẠI RỘNG 60 BLOCKS (Giant 60-Block Impact Crater)
        int craterRadius = 32;
        for (int dx = -craterRadius; dx <= craterRadius; dx++) {
            for (int dz = -craterRadius; dz <= craterRadius; dz++) {
                int distSq = dx * dx + dz * dz;
                if (distSq <= craterRadius * craterRadius) {
                    float p = 1.0f - (float) Math.sqrt(distSq) / craterRadius;
                    int depth = (int) (p * 8.0f);
                    for (int dy = -depth; dy <= 2; dy++) {
                        mutable.set(centerX + dx, startY + dy, centerZ + dz);
                        if (dy == -depth) {
                            world.setBlockState(mutable, ((dx + dz) % 3 == 0) ? cryingObsidian : tuff, 2);
                        } else if (dy < 0) {
                            world.setBlockState(mutable, (rand.nextFloat() < 0.35f) ? moss : crackedDeepslate, 2);
                        }
                    }
                }
            }
        }

        // 2. CỘT XÍCH CỰ ĐẠI VƯƠN LÊN TẬN Y=318 (Giant Interlocking Sky Chain)
        // Mỗi mắt xích dài 46 blocks, rộng 26 blocks, bước nhảy pitch = 34 blocks
        int linkPitch = 34;
        int linkOuterLength = 46;
        int linkOuterWidth = 26;
        int beamThickness = 6; // Thân mắt xích dày 6 blocks!

        int linkCount = 13; // 13 mắt xích vươn cao vượt qua Y=320 lên tận Y ~ 520!
        float driftAngleX = 0.12f;
        float driftAngleZ = 0.08f;

        for (int i = 0; i < linkCount; i++) {
            int cy = startY + 18 + i * linkPitch;
            if (cy > 550) break;

            int curX = centerX - 18 + (int) (i * linkPitch * driftAngleX);
            int curZ = centerZ - 12 + (int) (i * linkPitch * driftAngleZ);

            boolean isOrientationX = (i % 2 == 0);
            boolean isTopSeveredLink = (i == linkCount - 1 || cy > 480);

            for (int dy = -linkOuterLength / 2; dy <= linkOuterLength / 2; dy++) {
                int absY = Math.abs(dy);
                int yPos = cy + dy;
                if (yPos > 570 || yPos < -60) continue;

                // Mắt xích trên cùng bị xé toạc dữ dội ở đỉnh
                if (isTopSeveredLink && dy > 4 && rand.nextFloat() < 0.65f) continue;

                int widthLimit = linkOuterWidth / 2;
                if (absY > (linkOuterLength / 2 - 6)) {
                    widthLimit -= (absY - (linkOuterLength / 2 - 6));
                }
                int innerLimit = widthLimit - beamThickness;
                int innerYLimit = (linkOuterLength / 2) - beamThickness;

                for (int dPerp = -widthLimit; dPerp <= widthLimit; dPerp++) {
                    int absPerp = Math.abs(dPerp);
                    for (int dThick = -beamThickness; dThick <= beamThickness; dThick++) {
                        int absThick = Math.abs(dThick);

                        boolean isInsideHole = (absY < innerYLimit && absPerp < innerLimit);
                        boolean isWithinOuter = (absPerp <= widthLimit && absThick <= beamThickness);

                        if (isWithinOuter && !isInsideHole) {
                            if (rand.nextFloat() < 0.04f) continue; // Phong hóa thủng lỗ

                            int bx = isOrientationX ? (curX + dPerp) : (curX + dThick);
                            int bz = isOrientationX ? (curZ + dThick) : (curZ + dPerp);
                            mutable.set(bx, yPos, bz);

                            BlockState mat;
                            if (isTopSeveredLink && absY > 8) {
                                mat = cryingObsidian; // Vết gãy xé rỉ tím ngắt
                            } else if (yPos < startY + 12 && rand.nextFloat() < 0.35f) {
                                mat = moss;
                            } else if (rand.nextFloat() < 0.3f) {
                                mat = crackedDeepslate;
                            } else {
                                mat = smoothBasalt;
                            }

                            world.setBlockState(mutable, mat, 2);
                        }
                    }
                }
            }
        }

        // 3. MẮT XÍCH RƠI ĐỨT LÌA KHỔNG LỒ NẰM NGHIÊNG TRÊN ĐẤT (Fallen 46-Block Link)
        int fallenX = centerX + 38;
        int fallenZ = centerZ + 28;
        for (int dy = -20; dy <= 20; dy++) {
            for (int dx = -14; dx <= 14; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    int absDy = Math.abs(dy);
                    int absDx = Math.abs(dx);
                    int absDz = Math.abs(dz);

                    boolean hole = (absDy < 13 && absDx < 7);
                    if (!hole && absDy <= 20 && absDx <= 14 && absDz <= 5) {
                        if (rand.nextFloat() < 0.08f) continue;

                        int bx = fallenX + dx + (dy / 2);
                        int by = startY + dy + 15;
                        int bz = fallenZ + dz + (dx / 3);
                        if (by > 318 || by < -60) continue;

                        mutable.set(bx, by, bz);
                        BlockState mat = (rand.nextFloat() < 0.35f) ? moss : ((dy > 8) ? cryingObsidian : crackedDeepslate);
                        world.setBlockState(mutable, mat, 2);
                    }
                }
            }
        }

        return true;
    }
}
