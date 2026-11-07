package com.samsara.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Tượng Quỳ: tượng thạch cao trắng cỡ người quỳ trên mặt nước (chương III của The Womb). Hàng trăm bức cùng quay
 * về Gargantua; mỗi bức là một lần "bạn" đã chết (máy quay hiện dòng thông báo chết, xem SaveDoorLabels).
 * Chỉ để nhìn: không phá, không item. POSE 0 = quỳ ngửa mặt nhìn lên, 1 = quỳ cúi đầu, 2 = quỳ chắp tay cầu nguyện.
 * Model sinh bởi tools/generate_memory_statue.py.
 */
public class MemoryStatueBlock extends Block {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final IntProperty POSE = IntProperty.of("pose", 0, 2);

    private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 16, 14);

    public MemoryStatueBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(POSE, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, POSE);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}
