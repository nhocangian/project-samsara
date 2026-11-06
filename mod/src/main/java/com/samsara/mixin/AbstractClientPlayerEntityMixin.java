package com.samsara.mixin;

import com.samsara.client.CamcorderClientManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin thu phóng góc nhìn (Camera FOV Zoom) mượt mà khi ngắm qua ống kính máy quay Outlast.
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
    private void samsara$camcorderZoom(CallbackInfoReturnable<Float> cir) {
        float progress = CamcorderClientManager.getAimProgress();
        if (progress > 0.001f) {
            // Thu phóng mượt mà từ 1.0x xuống 0.74x (zoom quang học x1.35)
            float zoomFactor = 1.0f - (0.26f * progress);
            cir.setReturnValue(cir.getReturnValue() * zoomFactor);
        }
    }
}
