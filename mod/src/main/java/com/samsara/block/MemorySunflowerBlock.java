package com.samsara.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Hoa hướng dương của chiều "Ký Ức Của Nam".
 * Giống hệt hoa hướng dương vanilla (dùng lại model & texture), nhưng mặt hoa xoay được theo FACING.
 * Hoa vanilla luôn quay về hướng đông; ở đây SunflowerFieldFeature cho mọi bông hoa nhìn về
 * nơi người chơi tỉnh dậy, nên từ đầu con đường mòn cả màn hình là cánh hoa vàng.
 * Phá ra vẫn rơi hoa hướng dương vanilla.
 */
public class MemorySunflowerBlock extends TallPlantBlock {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    public MemorySunflowerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(FACING, Direction.SOUTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        return state == null ? null : state.with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
}
