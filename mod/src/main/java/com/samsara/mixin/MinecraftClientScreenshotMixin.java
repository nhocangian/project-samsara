package com.samsara.mixin;

import com.samsara.client.ScreenshotSunClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Khung hình chụp F2: chụp trước khi lên màn hình, rồi hiện khung cũ thay vào (xem {@link ScreenshotSunClient}). */
@Mixin(MinecraftClient.class)
public class MinecraftClientScreenshotMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(II)V"))
    private void samsara$captureBeforeScreen(Framebuffer fb, int width, int height) {
        ScreenshotSunClient.drawToScreen(fb, width, height);
    }
}
