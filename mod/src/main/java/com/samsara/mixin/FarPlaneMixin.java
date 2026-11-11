package com.samsara.mixin;

import com.samsara.registry.SamsaraDimensions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tầng 2 (Lối đi nháp): các khối khổng lồ cao tới cả nghìn block, xa hơn mặt phẳng cắt mặc định (4 lần tầm nhìn chunk).
 * Chỉ trong chiều này, đẩy mặt phẳng cắt ra xa để khối không bị xén mất một mảng.
 */
@Mixin(GameRenderer.class)
public class FarPlaneMixin {
    private static final float LOI_DI_NHAP_FAR = 6000.0f;

    @Inject(method = "getFarPlaneDistance", at = @At("RETURN"), cancellable = true)
    private void samsara$farPlaneForGiants(CallbackInfoReturnable<Float> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.world.getRegistryKey() == SamsaraDimensions.LOI_DI_NHAP_WORLD_KEY) {
            cir.setReturnValue(Math.max(cir.getReturnValue(), LOI_DI_NHAP_FAR));
        }
    }
}
