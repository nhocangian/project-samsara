package com.samsara.system.ending;

import com.samsara.entity.PrimordialEggEntity;
import com.samsara.registry.SamsaraEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Bệ Quả Trứng trong The Womb. Hiện đặt ở cuối tuyến hành hương (WombRouteManager, chặng SPIRE);
 * Giai đoạn 3b-4 sẽ thay bằng đỉnh Tháp Singularity.
 *  - Hồ nước nông với biểu tượng khảm dưới đáy (người chơi đứng trong nước),
 *    và một hàng đá đặt cách quãng dẫn từ nơi đến tới bệ.
 *  - 07/11/2026: cùng ngôn ngữ thạch cao trắng với The Womb: đáy hồ vẫn đen để mặt nước soi rõ, con dấu và viền hồ
 *    bằng thạch cao trắng (vòng và lõi là đèn biển, sáng lên dưới làn nước như phù văn), mặt dưới bệ (thấy khi đứng dưới nhìn lên) trắng.
 * Lệnh test: /samsara test egg (dựng bệ + trứng ngay trước mặt).
 */
public final class EggAltarManager {
    private EggAltarManager() {}

    /** Khoảng cách của bệ trứng KIỂU CŨ (trước khi có tuyến hành hương), dùng để chuyển thế giới cũ. */
    public static final int DISTANCE_NORTH = 96;
    private static final int RADIUS = 9;

    /**
     * Biểu tượng khảm dưới đáy nước: nền hắc diện thạch đen, lõi và các vòng đồng tâm thạch anh trắng,
     * 7 tia thạch cao toả ra (7 giấc ngủ). Nhìn xuyên qua lớp nước nông như một con dấu.
     */
    private static BlockState sigilAt(int dx, int dz, double d, BlockState obsidian, BlockState crying, BlockState amethyst) {
        if (d < 2.5) return crying;
        if (Math.abs(d - 5.0) < 0.5 || d > RADIUS - 0.7) return crying;
        double angle = Math.atan2(dz, dx);
        double sector = (angle + Math.PI) / (2.0 * Math.PI) * 7.0;
        double offset = Math.abs(sector - Math.round(sector));
        if (d >= 2.5 && d <= RADIUS - 1 && offset * d < 0.45) return amethyst;
        return obsidian;
    }

    /** Dựng bệ + trứng tại {@code center} (center.y = mặt nước nơi người chơi đứng). Có thể gọi lại để dựng lại. */
    public static PrimordialEggEntity build(ServerWorld world, BlockPos center, BlockPos pathFrom) {
        for (int cx = (center.getX() - 24) >> 4; cx <= (center.getX() + 24) >> 4; cx++) {
            for (int cz = (center.getZ() - 24) >> 4; cz <= (center.getZ() + 24) >> 4; cz++) {
                world.getChunk(cx, cz);
            }
        }

        BlockState obsidian = Blocks.OBSIDIAN.getDefaultState();
        BlockState crying = Blocks.SEA_LANTERN.getDefaultState();        // vòng & lõi con dấu phát sáng dưới nước
        BlockState amethyst = Blocks.CALCITE.getDefaultState();          // 7 tia
        BlockState underside = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState water = Blocks.WATER.getDefaultState();
        // center.y = mặt nước (chỗ chân người chơi). Nước nông 1 block ở y-1, biểu tượng khảm ở đáy y-2.
        // Không có gì nhô lên khỏi mặt nước: người chơi đứng TRONG nước, nên bóng phản chiếu trồi lên được ngay trước mặt.
        int y = center.getY();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > RADIUS + 0.3) continue;
                world.setBlockState(center.add(dx, -3, dz), underside, 2);
                world.setBlockState(center.add(dx, -2, dz), sigilAt(dx, dz, d, obsidian, crying, amethyst), 2);
                world.setBlockState(center.add(dx, -1, dz), water, 2);
                for (int dy = 0; dy <= 12; dy++) {
                    world.setBlockState(center.add(dx, dy, dz), Blocks.AIR.getDefaultState(), 2);
                }
            }
        }
        // Viền giữ nước (ngang mặt nước): ở mặt biển thì chìm lẫn vào nước, khi bệ lơ lửng trên cao thì
        // giữ cho hồ không tràn thành thác
        for (int dx = -RADIUS - 2; dx <= RADIUS + 2; dx++) {
            for (int dz = -RADIUS - 2; dz <= RADIUS + 2; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= RADIUS + 0.3 || d > RADIUS + 1.6) continue;
                world.setBlockState(center.add(dx, -3, dz), underside, 2);
                world.setBlockState(center.add(dx, -2, dz), underside, 2);
                world.setBlockState(center.add(dx, -1, dz), amethyst, 2);
            }
        }

        // Không còn beacon (07/11/2026): chùm sáng vanilla không dừng được ở mũi tháp, nó xuyên lên tận trời và
        // trông như bắn ra từ giữa Gargantua.

        // Hàng đá dẫn đường từ nơi đến tới bệ
        if (pathFrom != null) {
            int sx = pathFrom.getX();
            int sz = pathFrom.getZ();
            int steps = (int) Math.ceil(Math.hypot(center.getX() - sx, center.getZ() - sz) / 4.0);
            for (int i = 1; i < steps; i++) {
                double t = i / (double) steps;
                int px = (int) Math.round(sx + (center.getX() - sx) * t);
                int pz = (int) Math.round(sz + (center.getZ() - sz) * t);
                if (Math.hypot(px - center.getX(), pz - center.getZ()) <= RADIUS + 1) break;
                world.setBlockState(new BlockPos(px, y, pz), i % 3 == 0 ? crying : obsidian, 2);
            }
        }

        // Trứng: xoá trứng cũ quanh bệ rồi đặt trứng mới lơ lửng giữa bệ
        Box area = new Box(center).expand(RADIUS + 4, 12, RADIUS + 4);
        world.getEntitiesByClass(PrimordialEggEntity.class, area, e -> true).forEach(PrimordialEggEntity::discard);
        PrimordialEggEntity egg = SamsaraEntities.PRIMORDIAL_EGG.create(world);
        if (egg != null) {
            egg.refreshPositionAndAngles(center.getX() + 0.5, y + 2.0, center.getZ() + 0.5, 0.0f, 0.0f);
            world.spawnEntity(egg);
        }
        return egg;
    }
}
