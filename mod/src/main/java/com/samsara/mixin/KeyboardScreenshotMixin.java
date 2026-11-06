package com.samsara.mixin;

import com.samsara.client.ScreenshotSunClient;
import net.minecraft.client.Keyboard;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.util.function.Consumer;

/** F2: chuyển việc chụp màn hình cho {@link ScreenshotSunClient} (ảnh chụp thấy mặt trời thật). */
@Mixin(Keyboard.class)
public class KeyboardScreenshotMixin {
    @Redirect(method = "onKey", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/util/ScreenshotRecorder;saveScreenshot(Ljava/io/File;Lnet/minecraft/client/gl/Framebuffer;Ljava/util/function/Consumer;)V"))
    private void samsara$deferScreenshot(File dir, Framebuffer fb, Consumer<Text> callback) {
        ScreenshotSunClient.requestScreenshot(dir, fb, callback);
    }
}
