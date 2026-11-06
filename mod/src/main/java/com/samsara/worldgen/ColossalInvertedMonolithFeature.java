package com.samsara.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * The Colossal Inverted Monolith (Đại Khối Kiến Trúc Lộn Ngược Xẻ Đôi Đại Dương v2 - Cosmic Scale).
 * Reference: Megalithic inverted terraced dreadnought wedge descending from the stratosphere into the ocean.
 * Height: ~300 blocks (from Y = 20 seabed to Y = 318 sky build limit).
 * Length: ~360 blocks (along Z, spanning past the horizon into the fog).
 * Width: Flares from 8 blocks at waterline to ~150 blocks near the sky ceiling.
 */
public class ColossalInvertedMonolithFeature extends Feature<DefaultFeatureConfig> {

    public ColossalInvertedMonolithFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        return generateMonolithStructure(world, origin.getX(), origin.getZ());
    }

    public static boolean generateMonolithStructure(StructureWorldAccess world, int centerX, int centerZ) {
        int waterLevel = 62;
        int seabedY = 20;
        int topY = 540; // Vươn cao vượt qua Y=320 cũ lên tận Y = 540!
        int halfLength = 220; // Tổng chiều dài = 440 blocks dọc theo trục Z!

        BlockState blackstone = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState deepslateTiles = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState smoothBasalt = Blocks.SMOOTH_BASALT.getDefaultState();
        BlockState calcite = Blocks.CALCITE.getDefaultState();
        BlockState diorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState stoneBricks = Blocks.STONE_BRICKS.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockState tintedGlass = Blocks.TINTED_GLASS.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState copperCatwalk = Blocks.CUT_COPPER.getDefaultState();
        BlockState moss = Blocks.MOSS_BLOCK.getDefaultState();
        BlockState oxidizedCopper = Blocks.OXIDIZED_COPPER.getDefaultState();
        BlockState darkPrismarine = Blocks.DARK_PRISMARINE.getDefaultState();

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // 1. TẠO HÌNH KHỐI NÊM LỘN NGƯỢC KHỔNG LỒ (Inverted Terraced Wedge)
        for (int z = -halfLength; z <= halfLength; z++) {
            float zRel = (float) z / (float) halfLength;
            float zProfile = 1.0f - (zRel * zRel);
            if (zProfile <= 0.0f) continue;
            float zScale = (float) Math.sqrt(zProfile);

            // Sườn nêm từ đáy biển rãnh sâu (Y=20) lên đỉnh bầu trời (Y=318)
            for (int y = seabedY; y <= topY; y++) {
                float hProg = (float) (y - waterLevel) / (float) (topY - waterLevel);
                if (hProg < 0.0f) hProg = 0.0f;

                // Chiều rộng bậc thang nở dần về phía trên (tối đa ~75 block mỗi bên = 150 blocks bề ngang!)
                int halfWidth = (int) ((4.0f + Math.pow(hProg, 1.30) * 70.0f) * zScale);
                if (halfWidth < 2) halfWidth = 2;

                // Gân tăng cứng dọc thân (Vertical Structural Ribs mỗi 20 blocks Z)
                boolean isRibZ = (Math.abs(z) % 20 == 0);
                int ribExtra = isRibZ ? 3 : 0;

                // Phân tầng vật liệu theo độ cao hùng vĩ
                BlockState primaryWall;
                BlockState accentWall;

                if (y < 75) {
                    // Tầng chân nước (Waterline Keel) - Đen tuyền, huyền bí
                    primaryWall = (y % 2 == 0) ? blackstone : deepslateTiles;
                    accentWall = cryingObsidian;
                } else if (y < 155) {
                    // Tầng thân dưới (Lower Terraced Hull) - Đá bazan và đá hoa xám
                    primaryWall = (y % 3 == 0) ? smoothBasalt : stoneBricks;
                    accentWall = diorite;
                } else if (y < 240) {
                    // Tầng thân trên (Upper Flank) - Calcite trắng và đá nhẵn
                    primaryWall = (y % 2 == 0) ? calcite : diorite;
                    accentWall = smoothBasalt;
                } else {
                    // Tầng đỉnh vòm mây (Sky Crown) - Calcite sáng chói, đá phát sáng và rêu ký sinh
                    primaryWall = calcite;
                    accentWall = ((z + y) % 7 == 0) ? moss : diorite;
                }

                // Hàng cửa sổ phát sáng barcode dài vô tận (Waterline Slits)
                boolean isSlitWindow = (y >= 66 && y <= 72 && Math.abs(z) % 4 == 0);

                for (int x = -halfWidth - ribExtra; x <= halfWidth + ribExtra; x++) {
                    int absX = Math.abs(x);
                    int currentMax = halfWidth + (isRibZ ? 3 : 0);

                    mutable.set(centerX + x, y, centerZ + z);

                    if (absX >= currentMax - 1 && absX <= currentMax) {
                        // Vỏ ngoài cấu trúc (Outer Shell 2-block thickness)
                        if (isSlitWindow && absX == currentMax) {
                            world.setBlockState(mutable, tintedGlass, 2);
                        } else if (isSlitWindow && absX == currentMax - 1) {
                            world.setBlockState(mutable, seaLantern, 2);
                        } else if ((x + y + z) % 19 == 0 && y > 60) {
                            world.setBlockState(mutable, accentWall, 2);
                        } else {
                            world.setBlockState(mutable, primaryWall, 2);
                        }
                    } else if (absX < currentMax - 1) {
                        // LÒNG TRONG RỖNG KHỔNG LỒ (Hollow Cathedral Interior)
                        // Bố trí sàn cầu catwalk mỗi 40 block độ cao
                        if (y == 75 || y == 125 || y == 175 || y == 230 || y == 285) {
                            if (absX <= 3) {
                                world.setBlockState(mutable, copperCatwalk, 2);
                            } else if (absX == currentMax - 2) {
                                world.setBlockState(mutable, copperCatwalk, 2);
                            } else if (y <= waterLevel || !world.isAir(mutable)) {
                                world.setBlockState(mutable, air, 2);
                            }
                        } else if (y == topY) {
                            // Mái trần trên cùng (Top Crown Roof)
                            world.setBlockState(mutable, ((x + z) % 2 == 0) ? blackstone : cryingObsidian, 2);
                        } else {
                            if (y <= waterLevel || !world.isAir(mutable)) {
                                world.setBlockState(mutable, air, 2);
                            }
                        }
                    }
                }
            }
        }

        // 2. TẠO CÁC KHỐI LỒI BẤT THƯỜNG TRÊN THÂN (Protruding Biomass / Sensor Pods - Tham chiếu Ảnh 1)
        buildHangingNodule(world, centerX, centerZ - 55, 145, 15, darkPrismarine, oxidizedCopper, -1);
        buildHangingNodule(world, centerX, centerZ + 65, 195, 18, oxidizedCopper, moss, -1);
        buildHangingNodule(world, centerX, centerZ + 15, 230, 16, darkPrismarine, cryingObsidian, 1);

        // 3. TẠO RÃNH TÁCH NƯỚC VÀ LỐI VÀO DƯỚI NƯỚC (Water Trench & Entrance Altar)
        for (int z = -12; z <= 12; z++) {
            for (int x = -4; x <= 4; x++) {
                for (int y = seabedY + 2; y <= waterLevel + 4; y++) {
                    mutable.set(centerX + x, y, centerZ + z);
                    if (Math.abs(x) == 4 || Math.abs(z) == 12 || y == seabedY + 2) {
                        world.setBlockState(mutable, cryingObsidian, 2);
                    } else {
                        world.setBlockState(mutable, air, 2);
                    }
                }
            }
        }

        // 4. BÀN THỜ ARG & LÕI NĂNG LƯỢNG TRUNG TÂM (Central Lodestone Altar at Y = 76)
        BlockPos altarPos = new BlockPos(centerX, 76, centerZ);
        world.setBlockState(altarPos, Blocks.LODESTONE.getDefaultState(), 2);
        world.setBlockState(altarPos.up(), Blocks.BEACON.getDefaultState(), 2);
        world.setBlockState(altarPos.north(3), cryingObsidian, 2);
        world.setBlockState(altarPos.south(3), cryingObsidian, 2);
        world.setBlockState(altarPos.east(3), cryingObsidian, 2);
        world.setBlockState(altarPos.west(3), cryingObsidian, 2);

        return true;
    }

    private static void buildHangingNodule(StructureWorldAccess world, int centerX, int centerZ, int centerY, int radius, BlockState matA, BlockState matB, int side) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int rSq = radius * radius;

        float hProg = (float) (centerY - 62) / (float) (318 - 62);
        int halfWidth = (int) (4.0f + Math.pow(hProg, 1.30) * 70.0f);
        int noduleCenterX = centerX + side * (halfWidth - 2);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq <= rSq) {
                        mutable.set(noduleCenterX + dx, centerY + dy, centerZ + dz);
                        BlockState mat = ((dx + dy + dz) % 3 == 0) ? matB : matA;
                        world.setBlockState(mutable, mat, 2);
                    }
                }
            }
        }
    }
}
