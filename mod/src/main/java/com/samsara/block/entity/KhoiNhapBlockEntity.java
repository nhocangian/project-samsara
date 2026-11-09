package com.samsara.block.entity;

import com.samsara.registry.SamsaraBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** Chỉ để gắn KhoiNhapRenderer vào Khối Nháp; không lưu dữ liệu gì. */
public class KhoiNhapBlockEntity extends BlockEntity {
    public KhoiNhapBlockEntity(BlockPos pos, BlockState state) {
        super(SamsaraBlockEntities.KHOI_NHAP_BE, pos, state);
    }
}
