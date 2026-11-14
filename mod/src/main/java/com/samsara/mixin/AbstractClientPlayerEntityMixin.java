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
        // Tầng 3 "Trốn tìm": cảnh cuối khoá chân người chơi bằng hiệu ứng chậm, mà chậm thì game thu hẹp góc nhìn
        // (trông như zoom vào). Ở tầng này giữ góc nhìn cố định; trong cảnh cuối mở rộng hơn một chút
        if (com.samsara.client.trontim.EchoRenderer.inFloor(net.minecraft.client.MinecraftClient.getInstance())) {
            cir.setReturnValue(com.samsara.client.trontim.EchoRenderer.inFinale() ? 1.12f : 1.0f);
            return;
        }
        // Tầng 4 "Đêm đầu tiên": cảnh cuối cũng khoá chân bằng hiệu ứng chậm; giữ góc nhìn, không để zoom vào
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player != null && com.samsara.system.demdautien.DemDauTienClock.isFloor(mc.world)
                && mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS)) {
            cir.setReturnValue(1.0f);
            return;
        }
        float progress = CamcorderClientManager.getAimProgress();
        if (progress > 0.001f) {
            // Thu phóng mượt mà từ 1.0x xuống 0.74x (zoom quang học x1.35)
            float zoomFactor = 1.0f - (0.26f * progress);
            cir.setReturnValue(cir.getReturnValue() * zoomFactor);
        }
    }
}
