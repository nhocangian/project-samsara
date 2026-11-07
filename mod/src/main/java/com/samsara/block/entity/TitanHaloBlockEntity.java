package com.samsara.block.entity;

import com.samsara.registry.SamsaraBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** Chỉ để gắn TitanHaloRenderer vào block hào quang; không lưu dữ liệu gì. */
public class TitanHaloBlockEntity extends BlockEntity {
    public TitanHaloBlockEntity(BlockPos pos, BlockState state) {
        super(SamsaraBlockEntities.TITAN_HALO_BE, pos, state);
    }
}
