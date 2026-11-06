package com.samsara.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.samsara.client.CamcorderClientManager;
import com.samsara.system.celestial.SolarFractureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Mixin can thiệp vào WorldRenderer.renderSky:
 *  - Chuyển hướng texture Mặt Trời (sun.png) sang các texture Hố Đen theo từng giai đoạn (sun_stage1..4.png),
 *    CHỈ khi người chơi đang ngắm qua máy quay (CamcorderClientManager.isFullyAimed). Mắt thường luôn thấy Mặt Trời Vanilla.
 *  - Các texture sun_stage được mã hóa PREMULTIPLIED ALPHA (xem tools/generate_sun_stages_pixel.py), nên vẽ với
 *    blend (ONE, ONE_MINUS_SRC_ALPHA): phần thân giữ màu, vầng sáng nhuộm trời, lõi hư không che đen bầu trời.
 *  - Khi mưa, Vanilla làm mờ Mặt Trời qua alpha của shader color; với premultiplied cần nhân cả RGB theo alpha đó,
 *    rồi trả lại shader color gốc trước khi vẽ Mặt Trăng (và khôi phục blend Additive cho Mặt Trăng).
 */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Unique
    private float[] samsara$savedShaderColor = null;

    @ModifyArg(
            method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/util/Identifier;)V"),
            index = 1
    )
    private Identifier samsara$modifySkyTextures(Identifier original) {
        if (original != null && original.getPath().contains("sun")) {
            // Theo Master Production Document: mắt thường chỉ thấy Mặt Trời Vanilla 16x16 tĩnh lặng.
            // Chỉ khi ngắm qua ống kính máy quay (đã đưa lên sát mắt) thì Hố Đen mới lộ ra.
            // Ngoại lệ: khung hình đang được chụp bằng F2 (ảnh chụp thấy sự thật, xem ScreenshotSunClient)
            if (!CamcorderClientManager.isFullyAimed() && !com.samsara.client.ScreenshotSunClient.isCapturing()) {
                return original;
            }
            Identifier stageTexture = SolarFractureManager.getSunTexture();
            if (stageTexture == null) {
                // Giai đoạn 0: giữ nguyên Mặt Trời Vanilla (texture + blend gốc)
                return original;
            }

            // Premultiplied: làm mờ theo mưa phải nhân cả RGB lẫn A
            float[] color = RenderSystem.getShaderColor();
            samsara$savedShaderColor = new float[]{color[0], color[1], color[2], color[3]};
            float fade = color[3];
            RenderSystem.setShaderColor(color[0] * fade, color[1] * fade, color[2] * fade, fade);

            RenderSystem.blendFuncSeparate(
                    GlStateManager.SrcFactor.ONE,
                    GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SrcFactor.ONE,
                    GlStateManager.DstFactor.ZERO
            );
            return stageTexture;
        } else if (original != null && original.getPath().contains("moon")) {
            if (samsara$savedShaderColor != null) {
                float[] c = samsara$savedShaderColor;
                RenderSystem.setShaderColor(c[0], c[1], c[2], c[3]);
                samsara$savedShaderColor = null;
            }
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SrcFactor.SRC_ALPHA,
                    GlStateManager.DstFactor.ONE,
                    GlStateManager.SrcFactor.ONE,
                    GlStateManager.DstFactor.ZERO
            );
        }
        return original;
    }
}
