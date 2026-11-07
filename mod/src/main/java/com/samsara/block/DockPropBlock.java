package com.samsara.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Đạo cụ kể chuyện ở Bến Đò (máy tính của Nam, dấu chân ướt): chỉ để nhìn, không item, không phá.
 * Model sinh bởi tools/generate_dock_props.py; FACING xoay model.
 */
public class DockPropBlock extends Block {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    private final VoxelShape shape;

    public DockPropBlock(Settings settings, VoxelShape shape) {
        super(settings);
        this.shape = shape;
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shape;
    }
}
