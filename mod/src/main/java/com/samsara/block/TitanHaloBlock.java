package com.samsara.block;

import com.samsara.block.entity.TitanHaloBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * Vầng hào quang của khổng thần (chương III): block vô hình đặt ở tâm vòng sáng trên đầu tượng; TitanHaloRenderer
 * vẽ vòng sáng bán kính RADIUS và một tia sáng từ Gargantua rọi xuyên qua vòng sáng xuống mặt nước.
 */
public class TitanHaloBlock extends BlockWithEntity {
    public static final IntProperty RADIUS = IntProperty.of("radius", 3, 10);

    public TitanHaloBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(RADIUS, 6));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(RADIUS);
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
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TitanHaloBlockEntity(pos, state);
    }
}
