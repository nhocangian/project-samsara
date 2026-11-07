package com.samsara.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.Direction;

/**
 * Mảng tường phòng ngủ của Nam (Bến Đò): mặt trước (FACING) là giấy dán tường kem sọc, mặt sau là ván gỗ sồi
 * (vách ngoài nhà Nam), cạnh lộ lõi vữa và nan gỗ. VARIANT: 0 trơn, 1 treo ảnh hai người, 2 vết nắng của tấm poster đã gỡ.
 * Model sinh bởi tools/generate_dock_props.py.
 */
public class NamWallpaperBlock extends Block {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final IntProperty VARIANT = IntProperty.of("variant", 0, 2);

    public NamWallpaperBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.SOUTH).with(VARIANT, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, VARIANT);
    }
}
