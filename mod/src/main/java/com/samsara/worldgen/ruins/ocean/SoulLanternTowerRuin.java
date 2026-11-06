package com.samsara.worldgen.ruins.ocean;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * 8. Soul Lantern Spire (Tháp Hải Đăng Ngọn Lửa Hồn / Pharos of the Void).
 * A towering, weathered 48-block high octagonal lighthouse standing alone in the black sea.
 * Capped by an open lantern cupola burning with eternal soul flame and sea lantern lenses.
 */
public class SoulLanternTowerRuin {

    public static boolean generate(ServerWorld world, int centerX, int surfaceY, int centerZ) {
        int waterLevel = Math.max(62, surfaceY);
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockState soulSoil = Blocks.SOUL_SOIL.getDefaultState();
        BlockState soulFire = Blocks.SOUL_FIRE.getDefaultState();
        BlockState tintedGlass = Blocks.TINTED_GLASS.getDefaultState();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        int towerHeight = 46;

        // 1. Submerged Foundation Base (radius 7, Y = waterLevel - 6 to waterLevel)
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                if (dx * dx + dz * dz <= 49) {
                    for (int y = waterLevel - 6; y <= waterLevel; y++) {
                        mutable.set(centerX + dx, y, centerZ + dz);
                        world.setBlockState(mutable, ((dx + dz) % 3 == 0) ? chiseled : bricks, 2);
                    }
                }
            }
        }

        // 2. Tapered Octagonal Tower Body (Y = waterLevel to waterLevel + 36)
        for (int y = 1; y <= 36; y++) {
            int worldY = waterLevel + y;
            int r = (y < 12) ? 5 : (y < 26) ? 4 : 3;

            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int d2 = dx * dx + dz * dz;
                    // Hollow octagonal wall
                    if (d2 <= r * r && d2 >= (r - 1) * (r - 1)) {
                        mutable.set(centerX + dx, worldY, centerZ + dz);
                        // Vertical conduit seams of crying obsidian
                        BlockState mat;
                        if ((dx == 0 || dz == 0) && y % 3 == 0) {
                            mat = crying;
                        } else if (y % 6 == 0) {
                            mat = chiseled;
                        } else {
                            mat = bricks;
                        }
                        world.setBlockState(mutable, mat, 2);
                    }
                }
            }

            // Central beacon conduit line
            if (y % 7 == 0) {
                mutable.set(centerX, worldY, centerZ);
                world.setBlockState(mutable, crying, 2);
            }
        }

        // 3. Widened Balcony / Gallery (Y = waterLevel + 37)
        int balconyY = waterLevel + 37;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (dx * dx + dz * dz <= 25) {
                    mutable.set(centerX + dx, balconyY, centerZ + dz);
                    world.setBlockState(mutable, chiseled, 2);
                }
            }
        }

        // Balcony railing
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 >= 20 && d2 <= 25) {
                    mutable.set(centerX + dx, balconyY + 1, centerZ + dz);
                    world.setBlockState(mutable, bricks, 2);
                }
            }
        }

        // 4. Lantern Cupola (Y = balconyY + 1 to balconyY + 8)
        // 4 corner pillars with tinted glass windows
        for (int h = 1; h <= 6; h++) {
            for (int px : new int[]{-3, 3}) {
                for (int pz : new int[]{-3, 3}) {
                    mutable.set(centerX + px, balconyY + h, centerZ + pz);
                    world.setBlockState(mutable, crying, 2);
                }
            }
            // Glass window panes
            for (int i = -2; i <= 2; i++) {
                mutable.set(centerX + i, balconyY + h, centerZ - 3);
                world.setBlockState(mutable, tintedGlass, 2);
                mutable.set(centerX + i, balconyY + h, centerZ + 3);
                world.setBlockState(mutable, tintedGlass, 2);
                mutable.set(centerX - 3, balconyY + h, centerZ + i);
                world.setBlockState(mutable, tintedGlass, 2);
                mutable.set(centerX + 3, balconyY + h, centerZ + i);
                world.setBlockState(mutable, tintedGlass, 2);
            }
        }

        // Soul Brazier in the center of the lantern
        mutable.set(centerX, balconyY + 1, centerZ);
        world.setBlockState(mutable, seaLantern, 2);
        mutable.set(centerX, balconyY + 2, centerZ);
        world.setBlockState(mutable, soulSoil, 2);
        mutable.set(centerX, balconyY + 3, centerZ);
        world.setBlockState(mutable, soulFire, 2);

        // 5. Conical Pyramid Roof (Y = balconyY + 7 to balconyY + 11)
        for (int step = 0; step <= 4; step++) {
            int roofR = 4 - step;
            int ry = balconyY + 7 + step;
            for (int dx = -roofR; dx <= roofR; dx++) {
                for (int dz = -roofR; dz <= roofR; dz++) {
                    if (dx * dx + dz * dz <= roofR * roofR) {
                        mutable.set(centerX + dx, ry, centerZ + dz);
                        world.setBlockState(mutable, (step == 4) ? crying : bricks, 2);
                    }
                }
            }
        }

        // Finial pinnacle tip
        mutable.set(centerX, balconyY + 12, centerZ);
        world.setBlockState(mutable, crying, 2);
        mutable.set(centerX, balconyY + 13, centerZ);
        world.setBlockState(mutable, seaLantern, 2);

        return true;
    }
}
