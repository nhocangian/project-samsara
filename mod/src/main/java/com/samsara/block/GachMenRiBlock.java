package com.samsara.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Gạch trần đang rỉ nước (Tầng Dưới): thỉnh thoảng một giọt rơi xuống mặt nước bên dưới. */
public class GachMenRiBlock extends Block {
    public GachMenRiBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextFloat() > 0.45f) return;
        world.addParticle(com.samsara.registry.SamsaraParticles.GIOT_NUOC,
                pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() - 0.05, pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                com.samsara.system.tangduoi.DuongOngBuilder.ORIGIN.getY() + 0.93, 0, 0);
    }
}
