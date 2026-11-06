package com.samsara.mixin;

import com.samsara.system.cycle.StageAnomalySystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stage 2+: cây trồng ở Overworld không còn tự lớn; chỉ lớn khi được ngắm qua máy quay bật IR
 * (xem StageAnomalySystem.growObservedCrops). Phân bón (bone meal) vẫn dùng được.
 */
@Mixin(CropBlock.class)
public class CropBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void samsara$onlyGrowUnderInfrared(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (StageAnomalySystem.shouldBlockCropGrowth(world)) {
            ci.cancel();
        }
    }
}
