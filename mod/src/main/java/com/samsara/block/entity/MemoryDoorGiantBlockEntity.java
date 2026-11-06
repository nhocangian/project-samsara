package com.samsara.block.entity;

import com.samsara.registry.SamsaraBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** Chỉ để gắn MemoryDoorGiantRenderer vào block cửa khổng lồ; không lưu dữ liệu gì. */
public class MemoryDoorGiantBlockEntity extends BlockEntity {
    public MemoryDoorGiantBlockEntity(BlockPos pos, BlockState state) {
        super(SamsaraBlockEntities.MEMORY_DOOR_GIANT_BE, pos, state);
    }
}
