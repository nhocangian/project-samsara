package com.samsara.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** Đèn huỳnh quang chập chờn (Tầng Dưới): sáng khá lâu, rồi tắt vài nhịp ngắn, thỉnh thoảng tắt hẳn một lúc. */
public class DenChopBlock extends DenOngBlock {
    public static final BooleanProperty LIT = Properties.LIT;

    public DenChopBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(LIT, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(LIT);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        boolean lit = !state.get(LIT);
        world.setBlockState(pos, state.with(LIT, lit), 3);
        if (lit) {
            world.playSound(null, pos, com.samsara.registry.SamsaraSounds.DEN_CHOP_TICK, SoundCategory.BLOCKS, 0.4f, 0.9f + random.nextFloat() * 0.2f);
            // Vừa sáng lại: phần lớn là nháy ngắn tiếp, đôi khi giữ sáng khá lâu
            world.scheduleBlockTick(pos, this, random.nextFloat() < 0.55f ? 2 + random.nextInt(5) : 40 + random.nextInt(120));
        } else {
            world.scheduleBlockTick(pos, this, random.nextFloat() < 0.12f ? 40 + random.nextInt(60) : 2 + random.nextInt(4));
        }
    }
}
