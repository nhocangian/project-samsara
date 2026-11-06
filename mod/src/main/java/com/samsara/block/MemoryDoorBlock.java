package com.samsara.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Cửa Ký Ức: cánh cửa panel màu phấn đứng trơ trọi trên mặt nước The Womb (chương II "Những Cánh Cửa").
 * Chỉ để nhìn: không mở, không phá, không có item. Model gồm cả khung (thò ra ngoài block), sinh bởi
 * tools/generate_memory_door.py. AJAR: 0 đóng, 1 hé 22.5°, 2 hé 45° (cánh mở ra xa phía mặt trước).
 * Mặt trước quay theo FACING.
 */
public class MemoryDoorBlock extends Block {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    public static final IntProperty AJAR = IntProperty.of("ajar", 0, 2);

    private static final VoxelShape NS = Block.createCuboidShape(0, 0, 7, 16, 16, 9);
    private static final VoxelShape EW = Block.createCuboidShape(7, 0, 0, 9, 16, 16);

    public MemoryDoorBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.SOUTH).with(HALF, DoubleBlockHalf.LOWER).with(AJAR, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, AJAR);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(FACING).getAxis() == Direction.Axis.Z ? NS : EW;
    }
}
