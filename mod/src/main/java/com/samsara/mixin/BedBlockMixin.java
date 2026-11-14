package com.samsara.mixin;

import com.samsara.system.demdautien.DemDauTienRuntime;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tầng 4 "Đêm đầu tiên": giường đặt ngoài vết rách thì tự gãy (thời Alpha chưa có giường). */
@Mixin(BedBlock.class)
public class BedBlockMixin {
    @Inject(method = "onPlaced", at = @At("TAIL"))
    private void samsara$alphaBed(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack, CallbackInfo ci) {
        DemDauTienRuntime.onBedPlaced(world, pos);
    }
}
