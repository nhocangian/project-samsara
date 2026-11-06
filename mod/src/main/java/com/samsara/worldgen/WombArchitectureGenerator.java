package com.samsara.worldgen;

import com.samsara.block.PrimordialEggBlock;
import com.samsara.entity.EchoDummyEntity;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.registry.SamsaraEntities;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Kiến tạo kiến trúc phi thực và các khối hình học kỳ dị lơ lửng trong Cõi Thai Nghén (The Womb).
 * - Kim Tự Tháp Đảo Ngược (Inverted Void Pyramid) lơ lửng trên tầng không.
 * - Vòng Tròn Thiên Thể Luân Hồi (Ouroboros Concentric Gyroscope Rings).
 * - Các khối tinh thể đa diện lơ lửng (Floating Octahedral Obelisks).
 * - Con đường đá nứt gãy xuyên qua khoảng hư không nối từ Giường đến Đại Trứng.
 */
public class WombArchitectureGenerator {

    public static void generateWombSanctuary(ServerWorld world, BlockPos center, BlockState bedState) {
        // 1. Con đường đá nứt gãy lơ lửng (Floating Fractured Causeway)
        buildFracturedCauseway(world, center);

        // 2. Chiếc giường cô độc tại tâm điểm xuất phát
        placeStrandedBed(world, center, bedState);

        // 3. Đại Trứng Hắc Thạch tại đài tế phía Bắc (Z + 36)
        BlockPos eggCenter = center.add(0, 2, 36);
        buildEggMonolithAltar(world, eggCenter);

        // 4. KIẾN TRÚC HÌNH HỌC LƠ LỬNG TRÊN TRỜI (Floating Cosmic Geometries):
        // A. Kim Tự Tháp Đảo Ngược (Inverted Pyramid) treo lơ lửng ngay phía trên Đại Trứng
        buildInvertedPyramid(world, eggCenter.add(0, 32, 0), 8);

        // B. Vòng Tròn Luân Hồi (Ouroboros Gyroscope Rings) lơ lửng ở cao độ Y = 95
        buildOuroborosRings(world, center.add(0, 28, 18), 11);

        // C. 4 Khối Đa Diện Thần Bí (Floating Octahedrons) treo lơ lửng ở 4 hướng vệ tinh
        buildFloatingOctahedron(world, center.add(-20, 22, 10), 4);
        buildFloatingOctahedron(world, center.add(20, 22, 10), 4);
        buildFloatingOctahedron(world, center.add(-20, 26, 30), 4);
        buildFloatingOctahedron(world, center.add(20, 26, 30), 4);

        // 5. Các phế tích ký ức và bóng ma nhân chứng
        spawnAtmosphericDetails(world, center);
    }

    private static void buildFracturedCauseway(ServerWorld world, BlockPos center) {
        // Tạo những phiến đá hắc thạch trôi nổi trên khoảng không hư vô
        for (int z = -6; z <= 42; z++) {
            int width = (z < 6 || z > 30) ? 6 : 3;
            for (int x = -width; x <= width; x++) {
                // Tạo các vết rách nứt ngẫu nhiên giữa các tảng đá
                if ((x * z + z * 3) % 7 == 0) continue;

                BlockPos p = center.add(x, 0, z);
                // Dọn dẹp khoảng không
                for (int y = 1; y <= 35; y++) {
                    world.setBlockState(p.up(y), Blocks.AIR.getDefaultState(), 2);
                }

                BlockState stone = ((x + z) % 3 == 0) ? Blocks.CRYING_OBSIDIAN.getDefaultState() :
                        ((x + z) % 2 == 0) ? Blocks.POLISHED_BASALT.getDefaultState() :
                                Blocks.OBSIDIAN.getDefaultState();
                world.setBlockState(p, stone, 2);
                world.setBlockState(p.down(), Blocks.BEDROCK.getDefaultState(), 2);
            }
        }
    }

    private static void placeStrandedBed(ServerWorld world, BlockPos center, BlockState bedState) {
        BlockState placedBed = bedState.getBlock() instanceof BedBlock ? bedState : Blocks.RED_BED.getDefaultState();
        world.setBlockState(center.up(), placedBed, 2);
        if (placedBed.contains(BedBlock.PART)) {
            world.setBlockState(center.up().offset(Direction.SOUTH), placedBed.with(BedBlock.PART, net.minecraft.block.enums.BedPart.FOOT), 2);
        }
    }

    private static void buildEggMonolithAltar(ServerWorld world, BlockPos altarPos) {
        // Đài tế lễ nhiều tầng
        for (int r = 5; r >= 1; r--) {
            int yOffset = 5 - r;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r) {
                        BlockPos p = altarPos.add(dx, yOffset - 3, dz);
                        world.setBlockState(p, Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);
                    }
                }
            }
        }

        // Đặt Đại Trứng Hắc Thạch tại đỉnh đài tế
        world.setBlockState(altarPos, SamsaraBlocks.PRIMORDIAL_EGG.getDefaultState(), 2);

        // 4 ngọn lửa linh hồn bao quanh
        world.setBlockState(altarPos.north(3), Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);
        world.setBlockState(altarPos.north(3).up(), Blocks.SOUL_FIRE.getDefaultState(), 2);
        world.setBlockState(altarPos.south(3), Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);
        world.setBlockState(altarPos.south(3).up(), Blocks.SOUL_FIRE.getDefaultState(), 2);
        world.setBlockState(altarPos.east(3), Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);
        world.setBlockState(altarPos.east(3).up(), Blocks.SOUL_FIRE.getDefaultState(), 2);
        world.setBlockState(altarPos.west(3), Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);
        world.setBlockState(altarPos.west(3).up(), Blocks.SOUL_FIRE.getDefaultState(), 2);
    }

    /**
     * Kiến trúc 1: Kim Tự Tháp Đảo Ngược Lơ Lửng (Inverted Void Pyramid)
     * Đáy phẳng ở trên cao, chóp nhọn chĩa thẳng xuống Đại Trứng.
     */
    private static void buildInvertedPyramid(ServerWorld world, BlockPos apex, int height) {
        for (int y = 0; y < height; y++) {
            int radius = y + 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Viền ngoài kim tự tháp rỗng
                    boolean isEdge = (Math.abs(dx) == radius || Math.abs(dz) == radius);
                    if (isEdge || y == height - 1) {
                        BlockPos p = apex.add(dx, y, dz);
                        world.setBlockState(p, ((dx + dz) % 2 == 0) ? Blocks.CRYING_OBSIDIAN.getDefaultState() : Blocks.POLISHED_BLACKSTONE.getDefaultState(), 2);
                    }
                }
            }
        }
        // Điểm phát quang ở chóp nhọn chĩa xuống
        world.setBlockState(apex, Blocks.SHROOMLIGHT.getDefaultState(), 2);
    }

    /**
     * Kiến trúc 2: Vòng Tròn Thiên Thể Luân Hồi (Ouroboros Concentric Rings)
     * Treo lơ lửng thẳng đứng giữa bầu trời.
     */
    private static void buildOuroborosRings(ServerWorld world, BlockPos ringCenter, int radius) {
        for (int angle = 0; angle < 360; angle += 6) {
            double rad = Math.toRadians(angle);
            // Vòng tròn đứng trên mặt phẳng X - Y
            int x = (int) Math.round(Math.cos(rad) * radius);
            int y = (int) Math.round(Math.sin(rad) * radius);
            BlockPos p1 = ringCenter.add(x, y, 0);
            world.setBlockState(p1, (angle % 30 == 0) ? Blocks.CRYING_OBSIDIAN.getDefaultState() : Blocks.OBSIDIAN.getDefaultState(), 2);

            // Vòng tròn nghiêng lồng vào nhau (Concentric Gyroscope Ring)
            int z2 = (int) Math.round(Math.cos(rad) * (radius - 3));
            int y2 = (int) Math.round(Math.sin(rad) * (radius - 3));
            BlockPos p2 = ringCenter.add(0, y2, z2);
            world.setBlockState(p2, Blocks.POLISHED_BLACKSTONE.getDefaultState(), 2);
        }
    }

    /**
     * Kiến trúc 3: Khối Đa Diện Tinh Thể Lơ Lửng (Floating Octahedron)
     */
    private static void buildFloatingOctahedron(ServerWorld world, BlockPos pos, int r) {
        for (int dy = -r; dy <= r; dy++) {
            int layerR = r - Math.abs(dy);
            for (int dx = -layerR; dx <= layerR; dx++) {
                for (int dz = -layerR; dz <= layerR; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= layerR) {
                        BlockPos p = pos.add(dx, dy, dz);
                        world.setBlockState(p, (Math.abs(dx) == layerR || Math.abs(dz) == layerR) ?
                                Blocks.CRYING_OBSIDIAN.getDefaultState() : Blocks.BLACKSTONE.getDefaultState(), 2);
                    }
                }
            }
        }
        // Lõi phát sáng bên trong
        world.setBlockState(pos, Blocks.SEA_LANTERN.getDefaultState(), 2);
    }

    private static void spawnAtmosphericDetails(ServerWorld world, BlockPos center) {
        // Tàn tích lò nung & rương cổ
        BlockPos chestPos = center.add(4, 1, -2);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
        if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setStack(0, new ItemStack(Items.WITHER_ROSE));
            chest.setStack(13, new ItemStack(Items.WOODEN_PICKAXE));
        }

        // Đèn linh hồn dẫn lối dọc cây cầu nứt
        for (int z = 4; z <= 36; z += 8) {
            world.setBlockState(center.add(-3, 1, z), Blocks.SOUL_LANTERN.getDefaultState(), 2);
            world.setBlockState(center.add(3, 1, z), Blocks.SOUL_LANTERN.getDefaultState(), 2);
        }

        // HÀNG BÓNG MA VĨNH CỬU CÚI ĐẦU TIỄN ĐƯA (12 Bản thể linh hồn kiếp trước)
        // Dọc 2 bên cầu nối
        for (int z = 6; z <= 30; z += 6) {
            spawnBowingEcho(world, center.add(-4, 1, z), 80.0f);
            spawnBowingEcho(world, center.add(4, 1, z), -80.0f);
        }

        // Xung quanh đài tế Đại Trứng
        spawnBowingEcho(world, center.add(-3, 3, 36), 110.0f);
        spawnBowingEcho(world, center.add(3, 3, 36), -110.0f);
        spawnBowingEcho(world, center.add(0, 3, 41), 180.0f);
    }

    private static void spawnBowingEcho(ServerWorld world, BlockPos pos, float yaw) {
        EchoDummyEntity dummy = SamsaraEntities.ECHO_DUMMY.create(world);
        if (dummy != null) {
            dummy.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 35.0f);
            dummy.setCrouchingState(false);
            dummy.setPermanent(true); // VĨNH CỬU KHÔNG BIẾN MẤT
            dummy.setBowing(true);    // ĐẦU CÚI TÔN NGHIÊM
            world.spawnEntity(dummy);
        }
    }
}
