package com.samsara.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.samsara.registry.SamsaraBiomes;
import com.samsara.registry.SamsaraDimensions;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import org.joml.Matrix4f;

/**
 * Custom Post-Processing Screen Atmosphere Overlay cho The Womb.
 * Tự động chuyển đổi màu viền màn hình (Amniotic Cosmic Vignette) tương ứng với từng Biome:
 * - The Origin Cradle: Lam đậm vũ trụ & Cyan ánh sao (Cosmic Navy / Star Cyan)
 * - The Titan Graveyard: Khói xám tro tàn & Than cự thần (Ashen Charcoal / Smog)
 * - The Singularity Spires: Tím neon rực rỡ & Hồng thạch anh kỳ dị (Neon Amethyst / Violet)
 */
public class WombAtmosphereOverlay implements HudRenderCallback {

    public static void register() {
        HudRenderCallback.EVENT.register(new WombAtmosphereOverlay());
    }

    // Giá trị màu hiện tại (lerp mềm mại qua thời gian)
    private static float curR1 = 0.05f, curG1 = 0.25f, curB1 = 0.65f;
    private static float curR2 = 0.10f, curG2 = 0.45f, curB2 = 0.75f;

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        // Chỉ hiển thị khi đang ở chiều không gian The Womb
        if (!client.world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY)) {
            return;
        }

        BlockPos pos = client.player.getBlockPos();
        RegistryEntry<Biome> biomeEntry = client.world.getBiome(pos);

        // Xác định mục tiêu màu theo Biome hiện tại
        float targetR1, targetG1, targetB1;
        float targetR2, targetG2, targetB2;

        if (biomeEntry.matchesKey(SamsaraBiomes.TITAN_GRAVEYARD)) {
            // Ashen Charcoal / Desolate Fog
            targetR1 = 0.22f; targetG1 = 0.22f; targetB1 = 0.24f;
            targetR2 = 0.32f; targetG2 = 0.33f; targetB2 = 0.35f;
        } else if (biomeEntry.matchesKey(SamsaraBiomes.SINGULARITY_SPIRES)) {
            // Neon Violet / Amethyst Singularity
            targetR1 = 0.58f; targetG1 = 0.08f; targetB1 = 0.78f;
            targetR2 = 0.82f; targetG2 = 0.14f; targetB2 = 0.92f;
        } else {
            // Origin Cradle: Deep Cosmic Mirror Navy
            targetR1 = 0.04f; targetG1 = 0.22f; targetB1 = 0.58f;
            targetR2 = 0.09f; targetG2 = 0.48f; targetB2 = 0.78f;
        }

        // Lerp mượt mà (0.05 mỗi frame)
        curR1 = MathHelper.lerp(0.05f, curR1, targetR1);
        curG1 = MathHelper.lerp(0.05f, curG1, targetG1);
        curB1 = MathHelper.lerp(0.05f, curB1, targetB1);
        curR2 = MathHelper.lerp(0.05f, curR2, targetR2);
        curG2 = MathHelper.lerp(0.05f, curG2, targetG2);
        curB2 = MathHelper.lerp(0.05f, curB2, targetB2);

        int width = drawContext.getScaledWindowWidth();
        int height = drawContext.getScaledWindowHeight();
        float time = (client.world.getTimeOfDay() + tickDelta) * 0.03f;

        // Nhịp thở của thai nghén vũ trụ
        float pulse = MathHelper.sin(time) * 0.10f + 0.32f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();

        // Vẽ 4 dải Gradient viền màn hình (Cosmic Vignette)
        // 1. Viền trên (Top)
        buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, 0, 0, 0).color(curR1, curG1, curB1, pulse).next();
        buffer.vertex(matrix, width, 0, 0).color(curR2, curG2, curB2, pulse).next();
        buffer.vertex(matrix, 0, height * 0.28f, 0).color(curR1, curG1, curB1, 0.0f).next();
        buffer.vertex(matrix, width, height * 0.28f, 0).color(curR2, curG2, curB2, 0.0f).next();
        tessellator.draw();

        // 2. Viền dưới (Bottom)
        buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, 0, height * 0.72f, 0).color(curR2, curG2, curB2, 0.0f).next();
        buffer.vertex(matrix, width, height * 0.72f, 0).color(curR1, curG1, curB1, 0.0f).next();
        buffer.vertex(matrix, 0, height, 0).color(curR2, curG2, curB2, pulse).next();
        buffer.vertex(matrix, width, height, 0).color(curR1, curG1, curB1, pulse).next();
        tessellator.draw();

        // 3. Viền trái (Left)
        buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, 0, 0, 0).color(curR1, curG1, curB1, pulse).next();
        buffer.vertex(matrix, width * 0.18f, 0, 0).color(curR1, curG1, curB1, 0.0f).next();
        buffer.vertex(matrix, 0, height, 0).color(curR2, curG2, curB2, pulse).next();
        buffer.vertex(matrix, width * 0.18f, height, 0).color(curR2, curG2, curB2, 0.0f).next();
        tessellator.draw();

        // 4. Viền phải (Right)
        buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, width * 0.82f, 0, 0).color(curR2, curG2, curB2, 0.0f).next();
        buffer.vertex(matrix, width, 0, 0).color(curR2, curG2, curB2, pulse).next();
        buffer.vertex(matrix, width * 0.82f, height, 0).color(curR1, curG1, curB1, 0.0f).next();
        buffer.vertex(matrix, width, height, 0).color(curR1, curG1, curB1, pulse).next();
        tessellator.draw();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
