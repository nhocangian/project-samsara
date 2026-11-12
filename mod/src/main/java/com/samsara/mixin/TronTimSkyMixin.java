package com.samsara.mixin;

import com.samsara.client.trontim.TronTimFallback;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tầng 3 không có shaderpack Samsara: không vẽ trời (trăng, sao) và mây; nền là màu sương đen của biome. */
@Mixin(WorldRenderer.class)
public class TronTimSkyMixin {
    @Inject(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            at = @At("HEAD"), cancellable = true)
    private void samsara$noSky(CallbackInfo ci) {
        if (TronTimFallback.active()) ci.cancel();
    }

    @Inject(method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FDDD)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void samsara$noClouds(CallbackInfo ci) {
        if (TronTimFallback.active()) ci.cancel();
    }
}
