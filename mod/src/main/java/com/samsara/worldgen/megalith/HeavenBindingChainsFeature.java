package com.samsara.worldgen.megalith;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * The Heaven-Binding Chains (Tứ Đại Xích Xiềng Trói Bầu Trời - Tham chiếu Ảnh 1).
 * 4 gargantuan interlocking stone/deepslate chain pillars extending from the earth
 * straight up through the clouds to Y = 318, anchoring the cosmos.
 */
public class HeavenBindingChainsFeature {

    public static boolean generate(ServerWorld world, int centerX, int centerZ) {
        int baseY = 62;
        int topY = 318;

        // Tọa độ gốc của 4 trụ xích lớn tạo phối cảnh hội tụ lên trời (Perspective Convergence)
        // Hai xích gần (trái/phải) và hai xích xa (ở giữa) đúng như Ảnh 1
        generateSingleChain(world, centerX - 42, baseY, centerZ - 25, -0.06f, 0.08f, topY);
        generateSingleChain(world, centerX + 42, baseY, centerZ - 25, 0.06f, 0.08f, topY);
        generateSingleChain(world, centerX - 14, baseY, centerZ + 35, -0.02f, -0.04f, topY);
        generateSingleChain(world, centerX + 14, baseY, centerZ + 35, 0.02f, -0.04f, topY);

        return true;
    }

    /**
     * Dựng một cột xích khổng lồ với các mắt xích đan lồng vào nhau (Interlocking Torus Loops).
     */
    private static void generateSingleChain(ServerWorld world, int startX, int startY, int startZ, float driftX, float driftZ, int targetTopY) {
        BlockState deepslateBricks = Blocks.DEEPSLATE_BRICKS.getDefaultState();
        BlockState polishedDeepslate = Blocks.POLISHED_DEEPSLATE.getDefaultState();
        BlockState crackedDeepslate = Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState smoothBasalt = Blocks.SMOOTH_BASALT.getDefaultState();
        BlockState calcite = Blocks.CALCITE.getDefaultState();
        BlockState ironBlock = Blocks.IRON_BLOCK.getDefaultState();

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // 1. Chân đế mỏ neo khổng lồ dưới mặt đất (Ground Anchor Cradle)
        int anchorRadius = 14;
        for (int dx = -anchorRadius; dx <= anchorRadius; dx++) {
            for (int dz = -anchorRadius; dz <= anchorRadius; dz++) {
                int distSq = dx * dx + dz * dz;
                if (distSq <= anchorRadius * anchorRadius) {
                    for (int y = startY - 12; y <= startY + 4; y++) {
                        mutable.set(startX + dx, y, startZ + dz);
                        BlockState mat = (distSq > (anchorRadius - 2) * (anchorRadius - 2)) ? smoothBasalt : deepslateBricks;
                        world.setBlockState(mutable, mat, 2);
                    }
                }
            }
        }

        // 2. Chuỗi mắt xích khổng lồ (Interlocking Chain Links)
        // Mỗi mắt xích dài 22 block, rộng 14 block, độ dày vành xích là 3 block.
        // Bước nhảy giữa các mắt xích (pitch) là 16 block để 2 mắt xích đan lồng xuyên qua nhau 6 block.
        int linkPitch = 16;
        int linkOuterLength = 22; // Nửa chiều dài theo trục đứng = 11
        int linkOuterWidth = 13;  // Nửa chiều rộng = 6
        int beamThickness = 3;    // Độ dày thân mắt xích

        int linkIndex = 0;
        for (int cy = startY + 8; cy <= targetTopY; cy += linkPitch) {
            float progress = (float) (cy - startY) / (float) (targetTopY - startY);
            int curX = (int) (startX + (cy - startY) * driftX);
            int curZ = (int) (startZ + (cy - startY) * driftZ);

            boolean isOrientationX = (linkIndex % 2 == 0); // Mắt chẵn nằm dọc trục X, mắt lẻ nằm dọc trục Z

            // Dựng hình xuyến chữ nhật bo góc (Rounded Rectangular Hollow Torus)
            for (int dy = -linkOuterLength / 2; dy <= linkOuterLength / 2; dy++) {
                int absY = Math.abs(dy);
                int yPos = cy + dy;
                if (yPos > 319) continue;

                // Bo cong đầu mắt xích ở 2 đỉnh trên và dưới
                int widthLimit = linkOuterWidth;
                if (absY > (linkOuterLength / 2 - 3)) {
                    widthLimit -= (absY - (linkOuterLength / 2 - 3));
                }

                int innerLimit = widthLimit - beamThickness;
                int innerYLimit = (linkOuterLength / 2) - beamThickness;

                for (int dPerp = -widthLimit; dPerp <= widthLimit; dPerp++) {
                    int absPerp = Math.abs(dPerp);
                    for (int dThick = -beamThickness; dThick <= beamThickness; dThick++) {
                        int absThick = Math.abs(dThick);

                        // Kiểm tra xem vị trí này có nằm trong vành xích hay rơi vào lỗ rỗng bên trong
                        boolean isInsideHole = (absY < innerYLimit && absPerp < innerLimit);
                        boolean isWithinOuterShell = (absPerp <= widthLimit && absThick <= beamThickness);

                        if (isWithinOuterShell && !isInsideHole) {
                            int bx = isOrientationX ? (curX + dPerp) : (curX + dThick);
                            int bz = isOrientationX ? (curZ + dThick) : (curZ + dPerp);

                            mutable.set(bx, yPos, bz);

                            // Chọn vật liệu theo độ cao và vân xích
                            BlockState mat;
                            if (progress > 0.85f && (dy + dPerp) % 4 == 0) {
                                // Tầng mây cao bị đóng băng/hóa thạch màu trắng
                                mat = calcite;
                            } else if ((dy + dPerp + dThick) % 17 == 0) {
                                mat = cryingObsidian;
                            } else if ((dy + dPerp) % 5 == 0) {
                                mat = crackedDeepslate;
                            } else if (absThick == beamThickness && absPerp == widthLimit) {
                                mat = polishedDeepslate;
                            } else {
                                mat = deepslateBricks;
                            }

                            world.setBlockState(mutable, mat, 2);
                        }
                    }
                }
            }

            linkIndex++;
        }
    }
}
