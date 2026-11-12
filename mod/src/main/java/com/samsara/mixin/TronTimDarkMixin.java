package com.samsara.mixin;

import com.samsara.client.trontim.TronTimFallback;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tầng 3 không có shaderpack Samsara: lightmap đen hết, chỉ giữ ô sáng tối đa (block 15, sky 15) là trắng. Trong chiều này
 * không có nguồn sáng và không có ánh trời, nên mọi khối, thực thể, bàn tay đều đen; chỉ thứ tiếng vọng vẽ với ánh sáng
 * tối đa mới hiện. Có pack Samsara thì {@link TronTimFallback#active()} trả về false, lightmap giữ nguyên.
 */
@Mixin(LightmapTextureManager.class)
public class TronTimDarkMixin {
    @Shadow @Final private NativeImage image;
    @Shadow @Final private NativeImageBackedTexture texture;

    @Inject(method = "update", at = @At("TAIL"))
    private void samsara$darkFloor(float delta, CallbackInfo ci) {
        if (!TronTimFallback.active()) return;
        for (int sky = 0; sky < 16; sky++) {
            for (int block = 0; block < 16; block++) {
                boolean full = sky == 15 && block == 15;
                image.setColor(block, sky, full ? 0xFFFFFFFF : 0xFF000000);
            }
        }
        texture.upload();
    }
}
