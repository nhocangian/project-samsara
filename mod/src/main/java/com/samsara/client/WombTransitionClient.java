package com.samsara.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.samsara.system.cycle.WombTransitionSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Hiệu ứng "không gian kéo dãn" khi bị hút vào The Womb (phía client).
 *
 *  Pha HÚT (TRANSITION_PACKET, ~4.5 giây):
 *    - FOV giãn rộng dần như bị kéo căng (tối đa ~170°), rồi ở 15% cuối co giật mạnh vào trong
 *      (dolly zoom) như bị nuốt.
 *    - Hai lớp vệt sao womb_stretch.png (blend cộng) xoay ngược chiều nhau, phóng to dần từ tâm.
 *    - Màn hình tối dần về đen.
 *  Pha ĐẾN (ARRIVE_PACKET, 2 giây): chớp trắng, FOV từ rộng thu về bình thường, vệt sao co lại rồi tắt.
 */
@Environment(EnvType.CLIENT)
public class WombTransitionClient {
    private static final Identifier STRETCH = new Identifier("samsara", "textures/misc/womb_stretch.png");
    private static final int TEX_SIZE = 128;
    private static final int ARRIVE_TICKS = 40;

    private static int transitionTicks = -1;
    private static int transitionDuration = WombTransitionSystem.TRANSITION_TICKS;
    private static int arriveTicks = -1;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(WombTransitionSystem.TRANSITION_PACKET, (client, handler, buf, sender) -> {
            int duration = Math.max(1, buf.readVarInt());
            client.execute(() -> {
                transitionDuration = duration;
                transitionTicks = 0;
                arriveTicks = -1;
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(WombTransitionSystem.ARRIVE_PACKET, (client, handler, buf, sender) ->
                client.execute(() -> {
                    transitionTicks = -1;
                    arriveTicks = 0;
                }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            transitionTicks = -1;
            arriveTicks = -1;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (transitionTicks >= 0) {
                // Giữ màn hình đen nếu gói ARRIVE đến trễ
                transitionTicks = Math.min(transitionTicks + 1, transitionDuration);
            }
            if (arriveTicks >= 0 && ++arriveTicks > ARRIVE_TICKS) {
                arriveTicks = -1;
            }
        });

        HudRenderCallback.EVENT.register(WombTransitionClient::renderOverlay);
    }

    public static boolean isActive() {
        return transitionTicks >= 0 || arriveTicks >= 0;
    }

    /** Tiến trình pha hút, 0..1 (âm nếu không ở pha hút). */
    private static float pullProgress(float tickDelta) {
        if (transitionTicks < 0) return -1.0f;
        return MathHelper.clamp((transitionTicks + tickDelta) / transitionDuration, 0.0f, 1.0f);
    }

    /** Tiến trình pha đến, 0..1 (âm nếu không ở pha đến). */
    private static float arriveProgress(float tickDelta) {
        if (arriveTicks < 0) return -1.0f;
        return MathHelper.clamp((arriveTicks + tickDelta) / ARRIVE_TICKS, 0.0f, 1.0f);
    }

    // ==========================================
    // FOV (gọi từ GameRendererFovMixin)
    // ==========================================

    public static double modifyFov(double fov, float tickDelta) {
        float pull = pullProgress(tickDelta);
        if (pull >= 0.0f) {
            double mult;
            if (pull < 0.85f) {
                // Giãn dần, nhanh dần về cuối, kèm nhịp "thở" rung nhẹ
                float t = pull / 0.85f;
                mult = 1.0 + 1.6 * t * t + Math.sin(pull * 40.0) * 0.03 * t;
            } else {
                // Bị nuốt: co giật mạnh vào trong
                float t = (pull - 0.85f) / 0.15f;
                mult = MathHelper.lerp(t * t, 2.6, 0.25);
            }
            return MathHelper.clamp(fov * mult, 5.0, 170.0);
        }
        float arrive = arriveProgress(tickDelta);
        if (arrive >= 0.0f) {
            float ease = 1.0f - (1.0f - arrive) * (1.0f - arrive);
            return MathHelper.clamp(fov * MathHelper.lerp(ease, 1.9, 1.0), 5.0, 170.0);
        }
        return fov;
    }

    // ==========================================
    // LỚP PHỦ HUD
    // ==========================================

    private static void renderOverlay(DrawContext ctx, float tickDelta) {
        if (!isActive()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        float time = client.player != null ? client.player.age + tickDelta : 0.0f;

        float pull = pullProgress(tickDelta);
        float arrive = arriveProgress(tickDelta);

        float stretchAlpha;
        float stretchScale;
        float black = 0.0f;
        float white = 0.0f;
        if (pull >= 0.0f) {
            stretchAlpha = Math.min(1.0f, pull * 1.6f);
            stretchScale = 0.8f + pull * pull * 4.0f;
            black = pull < 0.55f ? 0.0f : (pull - 0.55f) / 0.45f;
            black = black * black;
        } else {
            stretchAlpha = (1.0f - arrive) * 0.9f;
            stretchScale = 3.0f - arrive * 2.4f;
            white = arrive < 0.5f ? 1.0f - arrive * 2.0f : 0.0f;
        }

        // Hai lớp vệt sao xoay ngược chiều, blend cộng
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        int base = (int) (Math.max(w, h) * 1.2f);
        drawStretchLayer(ctx, w, h, base, stretchScale, time * 1.5f, stretchAlpha);
        drawStretchLayer(ctx, w, h, base, stretchScale * 0.6f, -time * 2.3f + 45.0f, stretchAlpha * 0.7f);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.defaultBlendFunc();

        if (black > 0.0f) {
            ctx.fill(0, 0, w, h, ((int) (black * 255) << 24));
        }
        if (white > 0.0f) {
            ctx.fill(0, 0, w, h, ((int) (white * 255) << 24) | 0xF4ECFF);
        }
        RenderSystem.disableBlend();
    }

    private static void drawStretchLayer(DrawContext ctx, int w, int h, int base, float scale, float angleDeg, float alpha) {
        if (alpha <= 0.01f) return;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, MathHelper.clamp(alpha, 0.0f, 1.0f));
        ctx.getMatrices().push();
        ctx.getMatrices().translate(w / 2.0f, h / 2.0f, 0.0f);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angleDeg));
        ctx.getMatrices().scale(scale, scale, 1.0f);
        ctx.drawTexture(STRETCH, -base / 2, -base / 2, base, base, 0.0f, 0.0f, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
        ctx.getMatrices().pop();
    }
}
