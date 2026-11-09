package com.samsara.block;

import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Đèn huỳnh quang đã hỏng hẳn (Tầng Dưới): không sáng, thỉnh thoảng bắn một chùm tia lửa xuống mặt nước. */
public class DenHongBlock extends DenOngBlock {
    public DenHongBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextFloat() > 0.35f) return;
        int n = 3 + random.nextInt(4);
        for (int i = 0; i < n; i++) {
            world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() - 0.05, pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                    (random.nextDouble() - 0.5) * 0.8, -0.3 - random.nextDouble() * 0.5, (random.nextDouble() - 0.5) * 0.8);
        }
        // Một giọt lửa cam rơi xuống, không khói; không phải chùm nào cũng có
        if (random.nextBoolean()) {
            world.addParticle(ParticleTypes.FALLING_LAVA, pos.getX() + 0.5, pos.getY() - 0.1, pos.getZ() + 0.5, 0, 0, 0);
        }
        world.playSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, com.samsara.registry.SamsaraSounds.DEN_HONG_ZAP,
                SoundCategory.BLOCKS, 0.7f, 0.9f + random.nextFloat() * 0.25f, false);
    }
}
