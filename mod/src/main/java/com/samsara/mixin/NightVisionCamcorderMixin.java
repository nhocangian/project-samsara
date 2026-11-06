package com.samsara.mixin;

import com.samsara.client.CamcorderClientManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Khi đang cầm máy quay, độ "nhìn đêm" (vanilla + uniform {@code nightVision} của Iris) chỉ theo công tắc IR
 * của máy quay (phím V), bỏ qua thuốc Nhìn Đêm. Trước đây shader bật chế độ hồng ngoại xanh bất cứ khi nào
 * người chơi cầm máy quay mà đang có hiệu ứng Nhìn Đêm từ nguồn khác (vd. /samsara test kit), trông như IR tự bật.
 */
@Mixin(GameRenderer.class)
public class NightVisionCamcorderMixin {
    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
    private static void samsara$camcorderControlsNightVision(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity != client.player || !CamcorderClientManager.isHoldingCamcorder(client.player)) return;
        boolean ir = CamcorderClientManager.isNightVision() && CamcorderClientManager.getBatteryLevel() > 0.0f;
        cir.setReturnValue(ir ? 1.0f : 0.0f);
    }
}
