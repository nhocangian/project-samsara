package com.samsara.mixin;

import com.samsara.client.WombTransitionClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FOV kéo dãn / co giật khi bị hút vào The Womb (chỉ góc nhìn thế giới, không áp cho tay cầm). */
@Mixin(GameRenderer.class)
public class GameRendererFovMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void samsara$wombStretchFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (changingFov && WombTransitionClient.isActive()) {
            cir.setReturnValue(WombTransitionClient.modifyFov(cir.getReturnValue(), tickDelta));
        }
    }
}
