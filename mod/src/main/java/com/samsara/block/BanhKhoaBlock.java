package com.samsara.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Bánh số đá của ổ khoá ở căn hầm Tầng Dưới: bấm chuột phải để xoay. HINT = đã sai nhiều, vết khắc hiện cả với mắt thường. */
public class BanhKhoaBlock extends Block {
    public static final IntProperty DIGIT = IntProperty.of("digit", 0, 9);
    public static final BooleanProperty HINT = BooleanProperty.of("hint");

    public BanhKhoaBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(DIGIT, 0).with(HINT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(DIGIT, HINT);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            world.setBlockState(pos, state.with(DIGIT, (state.get(DIGIT) + 1) % 10), 2 | 16);
            world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.35f, 0.6f + world.random.nextFloat() * 0.15f);
        }
        return ActionResult.SUCCESS;
    }
}
