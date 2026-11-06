package com.samsara.mixin;

import com.samsara.client.StageAnomalyClient;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stage 2+: "Nước đổi màu xám bạc" ở Overworld (chỉ phía client).
 * Đổi tận gốc màu nước của biome thay vì FluidRenderHandler, vì Sodium dùng bộ màu nước riêng
 * (BiomeColors.WATER_COLOR -> Biome.getWaterColor) và bỏ qua màu của FluidRenderHandler.
 */
@Mixin(Biome.class)
public class BiomeWaterColorMixin {
    @Inject(method = "getWaterColor", at = @At("HEAD"), cancellable = true)
    private void samsara$silverWater(CallbackInfoReturnable<Integer> cir) {
        if (StageAnomalyClient.shouldSilverWater()) {
            cir.setReturnValue(StageAnomalyClient.SILVER_WATER);
        }
    }

    @Inject(method = "getWaterFogColor", at = @At("HEAD"), cancellable = true)
    private void samsara$silverWaterFog(CallbackInfoReturnable<Integer> cir) {
        if (StageAnomalyClient.shouldSilverWater()) {
            cir.setReturnValue(StageAnomalyClient.SILVER_WATER_FOG);
        }
    }
}
