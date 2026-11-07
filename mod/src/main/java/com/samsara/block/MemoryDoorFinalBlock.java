package com.samsara.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Cánh cửa cuối (chương IV "Tháp Ngược"): một Cửa Ký Ức trắng cỡ người đứng một mình trên cầu, trước Hồ Gương.
 * Cánh cửa duy nhất của The Womb mở được bằng tay (nhấp chuột phải): đóng kín thì chắn lối, mở thì cánh quay hẳn 90°
 * và không còn va chạm. Mắt thường không thấy chữ; qua máy quay hiện "▶ New World / Chơi lần cuối: bây giờ / bạn"
 * (client/womb/SaveDoorLabels). Model dùng chung texture cửa trắng, sinh bởi tools/generate_memory_door.py.
 */
public class MemoryDoorFinalBlock extends Block {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OPEN = Properties.OPEN;

    private static final VoxelShape NS = Block.createCuboidShape(0, 0, 7, 16, 16, 9);
    private static final VoxelShape EW = Block.createCuboidShape(7, 0, 0, 9, 16, 16);

    public MemoryDoorFinalBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.WEST).with(HALF, DoubleBlockHalf.LOWER).with(OPEN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, OPEN);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(FACING).getAxis() == Direction.Axis.Z ? NS : EW;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(OPEN) ? VoxelShapes.empty() : getOutlineShape(state, world, pos, context);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        boolean open = !state.get(OPEN);
        BlockPos other = state.get(HALF) == DoubleBlockHalf.LOWER ? pos.up() : pos.down();
        if (!world.isClient) {
            world.setBlockState(pos, state.with(OPEN, open), 3);
            BlockState o = world.getBlockState(other);
            if (o.isOf(this)) world.setBlockState(other, o.with(OPEN, open), 3);
            world.playSound(null, pos, open ? SoundEvents.BLOCK_WOODEN_DOOR_OPEN : SoundEvents.BLOCK_WOODEN_DOOR_CLOSE,
                    SoundCategory.BLOCKS, 0.8f, 0.6f);
        }
        return ActionResult.success(world.isClient);
    }
}
