package com.samsara.block;

import com.samsara.block.entity.KhoiNhapBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

/**
 * Tầng Dưới, tầng 2: Khối Nháp. Có va chạm thật nhưng mắt thường không thấy; KhoiNhapRenderer chỉ vẽ khi ngắm máy quay.
 * Không có khung chọn (rê tâm ngắm không lộ), và ngập nước để mặt bể phía trên vẫn liền, không thủng lỗ theo hình lối đi.
 */
public class KhoiNhapBlock extends BlockWithEntity implements Waterloggable {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    /** false: ván giả của đoạn 4, mắt thường thấy nhưng dẫm là rơi. */
    private final boolean solid;

    public KhoiNhapBlock(Settings settings, boolean solid) {
        super(settings);
        this.solid = solid;
        setDefaultState(getDefaultState().with(WATERLOGGED, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (!solid) return VoxelShapes.empty();
        // Không ngập nước = giường nháp ở phòng cuối, thấp như giường thật
        return state.get(WATERLOGGED) ? VoxelShapes.fullCube() : VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 0.5625, 1.0);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        return state;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new KhoiNhapBlockEntity(pos, state);
    }
}
