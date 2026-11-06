package com.samsara.worldgen.ruins;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * Phế Tích Đại Xác Tàu Cự Thạch Đắm (True Colossal Sunken Dreadnought Keel Ruin).
 * Quy mô siêu cự đại:
 * - Chiều dài 240 blocks (Z từ -120 đến +120).
 * - Chiều cao từ đáy biển sâu Y = 20 vút thẳng lên Y = 318 (sát trần xây dựng tối đa!).
 * - Bề rộng nở ra tới 90 blocks ở vòm trên.
 * - Thân tàu bị xé toạc làm đôi bởi vết nứt địa chấn rộng 18 blocks để lộ lòng giáo đường rỗng bên trong.
 * - Phân tầng vật liệu tự nhiên (không còn vân sọc đường chéo!).
 */
public class SunkenDreadnoughtRuin {

    public static boolean generate(ServerWorld world, int centerX, int groundY, int centerZ) {
        Random rand = new Random(centerX * 8191L + centerZ * 4919L);
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        BlockState blackstone = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState crackedDeepslate = Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState smoothBasalt = Blocks.SMOOTH_BASALT.getDefaultState();
        BlockState calcite = Blocks.CALCITE.getDefaultState();
        BlockState diorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState moss = Blocks.MOSS_BLOCK.getDefaultState();
        BlockState copperCatwalk = Blocks.CUT_COPPER.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();

        int halfLength = 140; // Chiều dài = 280 blocks
        int seabedY = Math.max(15, groundY - 45);
        int topY = 540; // Vượt trần Y=320 cũ, vươn cao tới Y = 540!

        for (int z = -halfLength; z <= halfLength; z++) {
            float zRel = (float) z / halfLength;
            float zProfile = 1.0f - (zRel * zRel);
            if (zProfile <= 0.0f) continue;
            float zScale = (float) Math.sqrt(zProfile);

            // Vết nứt địa chấn xé toạc thân tàu ở giữa (Z từ -12 đến +12)
            boolean isFractureZone = (Math.abs(z) <= 10);

            for (int y = seabedY; y <= topY; y++) {
                float hProg = (float) (y - seabedY) / (topY - seabedY);
                // Bề rộng nở dần theo chiều cao (nửa chiều rộng từ 5 lên tới 45 blocks = 90 blocks bề ngang!)
                int halfWidth = (int) ((4.5f + Math.pow(hProg, 1.35) * 42.0f) * zScale);
                if (halfWidth < 3) halfWidth = 3;

                // Gân chịu lực dọc thân mỗi 18 blocks
                boolean isRib = (Math.abs(z) % 18 == 0);
                int currentMax = halfWidth + (isRib ? 3 : 0);

                // Phân tầng vật liệu tự nhiên theo độ cao
                BlockState wallMat;
                if (y < seabedY + 40) {
                    wallMat = (y % 3 == 0) ? blackstone : crackedDeepslate;
                } else if (y < seabedY + 120) {
                    wallMat = (y % 4 == 0) ? smoothBasalt : diorite;
                } else {
                    wallMat = calcite;
                }

                for (int x = -currentMax; x <= currentMax; x++) {
                    int absX = Math.abs(x);

                    // Xé toạc thân tàu ở khu vực vết nứt
                    if (isFractureZone && rand.nextFloat() < 0.75f) {
                        mutable.set(centerX + x, y, centerZ + z);
                        if (absX >= currentMax - 2) {
                            world.setBlockState(mutable, cryingObsidian, 2);
                        }
                        continue;
                    }

                    mutable.set(centerX + x, y, centerZ + z);

                    if (absX >= currentMax - 2) {
                        // Vỏ ngoài dày 3 blocks
                        BlockState finalMat = wallMat;
                        if (y < groundY + 4 && rand.nextFloat() < 0.35f) {
                            finalMat = moss;
                        } else if (isFractureZone) {
                            finalMat = cryingObsidian;
                        }

                        world.setBlockState(mutable, finalMat, 2);
                    } else {
                        // Lòng rỗng bên trong với các tầng sàn catwalk
                        if (y == groundY + 30 || y == groundY + 80 || y == groundY + 140 || y == groundY + 200) {
                            if (absX <= 4) {
                                world.setBlockState(mutable, copperCatwalk, 2);
                            } else if (y <= groundY || !world.isAir(mutable)) {
                                world.setBlockState(mutable, air, 2);
                            }
                        } else if (y <= groundY || !world.isAir(mutable)) {
                            world.setBlockState(mutable, air, 2);
                        }
                    }
                }
            }
        }

        // Lõi tế đàn Lodestone trung tâm
        BlockPos altarPos = new BlockPos(centerX, groundY + 4, centerZ);
        world.setBlockState(altarPos, Blocks.LODESTONE.getDefaultState(), 2);
        world.setBlockState(altarPos.up(), seaLantern, 2);
        world.setBlockState(altarPos.north(), cryingObsidian, 2);
        world.setBlockState(altarPos.south(), cryingObsidian, 2);

        return true;
    }
}
