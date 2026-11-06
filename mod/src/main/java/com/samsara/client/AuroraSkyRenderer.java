package com.samsara.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Custom Cosmic Sky Shader Renderer cho Cõi Thai Nghén (The Womb).
 * 1. "Con Mắt Hư Vô" (The Singularity Vortex) xoáy tròn tại thiên đỉnh bầu trời.
 * 2. 3 Tầng Dải Cực Quang Đa Tần Số (Triple-Harmonic Aurora Ribbons) uốn lượn liên tục.
 * 3. Tinh Vân Bụi Sao Vũ Trụ (Cosmic Starfield & Nebula Dust) nhấp nháy khắp vòm trời.
 * 4. Quầng sáng đường chân trời (Atmospheric Horizon Radial Glow).
 */
public class AuroraSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
    private static final int STAR_COUNT = 300;
    private static final float[] STAR_X = new float[STAR_COUNT];
    private static final float[] STAR_Y = new float[STAR_COUNT];
    private static final float[] STAR_Z = new float[STAR_COUNT];
    private static final float[] STAR_SIZE = new float[STAR_COUNT];
    private static final float[] STAR_PHASE = new float[STAR_COUNT];
    private static boolean starsInitialized = false;

    private static void initStars() {
        if (starsInitialized) return;
        Random rand = new Random(777L);
        for (int i = 0; i < STAR_COUNT; i++) {
            float u = rand.nextFloat();
            float v = rand.nextFloat();
            float theta = u * MathHelper.TAU;
            float phi = (float) Math.acos(2.0f * v - 1.0f);
            float r = 90.0f;

            STAR_X[i] = r * MathHelper.sin(phi) * MathHelper.cos(theta);
            STAR_Y[i] = Math.abs(r * MathHelper.cos(phi)) + 5.0f; // Nửa bán cầu trên
            STAR_Z[i] = r * MathHelper.sin(phi) * MathHelper.sin(theta);
            STAR_SIZE[i] = 0.6f + rand.nextFloat() * 1.4f;
            STAR_PHASE[i] = rand.nextFloat() * MathHelper.TAU;
        }
        starsInitialized = true;
    }

    @Override
    public void render(WorldRenderContext context) {
        initStars();

        MatrixStack matrices = context.matrixStack();
        float tickDelta = context.tickDelta();
        long worldTime = context.world().getTimeOfDay();
        float time = (worldTime + tickDelta) * 0.015f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        matrices.push();
        matrices.multiply(context.camera().getRotation());
        Matrix4f model = matrices.peek().getPositionMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 1. VẼ TINH VÂN BỤI SAO VŨ TRỤ (Cosmic Starfield)
        buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < STAR_COUNT; i++) {
            float twinkle = MathHelper.sin(time * 2.0f + STAR_PHASE[i]) * 0.4f + 0.6f;
            float s = STAR_SIZE[i];
            float x = STAR_X[i];
            float y = STAR_Y[i];
            float z = STAR_Z[i];

            // Màu sao lấp lánh tím, lam ngọc, và trắng vàng
            float sr = (i % 3 == 0) ? 0.9f : 0.4f;
            float sg = (i % 3 == 1) ? 0.85f : 0.6f;
            float sb = 1.0f;
            float sa = twinkle * 0.75f;

            buffer.vertex(model, x, y + s, z).color(sr, sg, sb, sa).next();
            buffer.vertex(model, x - s, y - s, z).color(sr, sg, sb, sa).next();
            buffer.vertex(model, x + s, y - s, z).color(sr, sg, sb, sa).next();
        }
        tessellator.draw();

        // 2. VẼ HẮC LỖ KERR-NEWMAN & ĐĨA BỒI TỤ TƯƠNG ĐỐI TÍNH (Kerr-Newman Black Hole & Accretion Disk)
        // Tâm Chân Trời Sự Kiện (Event Horizon Shadow)
        float bhAltitude = 65.0f;
        float bhZ = -55.0f;
        float bhX = 0.0f;

        // Vành đai đĩa bồi tụ ngoài & trong (Accretion Disk with Relativistic Doppler asymmetry)
        buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        int diskSegments = 48;
        float rInner = 9.0f;
        float rOuter = 26.0f;

        for (int i = 0; i <= diskSegments; i++) {
            float angle = (float) i / diskSegments * MathHelper.TAU;
            float cosA = MathHelper.cos(angle + time * 0.8f);
            float sinA = MathHelper.sin(angle + time * 0.8f);

            // Góc nghiêng 25 độ của đĩa
            float dx = cosA;
            float dy = sinA * 0.38f;
            float dz = sinA * 0.92f;

            // Tọa độ vành trong và vành ngoài
            float xIn = bhX + dx * rInner;
            float yIn = bhAltitude + dy * rInner;
            float zIn = bhZ + dz * rInner;

            float xOut = bhX + dx * rOuter;
            float yOut = bhAltitude + dy * rOuter;
            float zOut = bhZ + dz * rOuter;

            // Hiệu ứng Doppler Beaming tương đối tính:
            // Phía quay về phía mắt (cosA < 0) sáng chói cyan-trắng, phía quay đi (cosA > 0) tối đỏ
            float doppler = -cosA * 0.5f + 0.5f; // 1.0 ở bên trái (tiến về), 0.0 ở bên phải (lùi xa)
            float vr = MathHelper.lerp(doppler, 0.95f, 0.45f);
            float vg = MathHelper.lerp(doppler, 0.35f, 0.90f);
            float vb = MathHelper.lerp(doppler, 0.45f, 1.0f);
            float alpha = MathHelper.lerp(doppler, 0.35f, 0.90f);

            buffer.vertex(model, xIn, yIn, zIn).color(vr, vg, vb, alpha).next();
            buffer.vertex(model, xOut, yOut, zOut).color(vr * 0.8f, vg * 0.8f, vb * 0.8f, 0.0f).next();
        }
        tessellator.draw();

        // Tâm hắc lỗ đen tuyền che khuất ánh sáng (Event Horizon Sphere)
        buffer.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(model, bhX, bhAltitude, bhZ).color(0.01f, 0.0f, 0.02f, 1.0f).next();
        int horizonSegs = 32;
        float rHorizon = 7.5f;
        for (int i = 0; i <= horizonSegs; i++) {
            float hAngle = (float) i / horizonSegs * MathHelper.TAU;
            float hx = bhX + MathHelper.cos(hAngle) * rHorizon;
            float hy = bhAltitude + MathHelper.sin(hAngle) * rHorizon;
            buffer.vertex(model, hx, hy, bhZ).color(0.01f, 0.0f, 0.02f, 1.0f).next();
        }
        tessellator.draw();

        // 3. VẼ 3 TẦNG DẢI CỰC QUANG UỐN LƯỢN (Triple Harmonic Auroras)
        renderAuroraBand(buffer, model, time, 85.0f, 18.0f, 32.0f, 0.2f, 0.85f, 0.95f, 0.45f); // Tầng Lam Ngọc (Cyan)
        renderAuroraBand(buffer, model, time * 0.8f + 1.5f, 75.0f, 12.0f, 26.0f, 0.85f, 0.25f, 0.95f, 0.40f); // Tầng Tím Hư Vô (Violet)
        renderAuroraBand(buffer, model, time * 1.2f + 3.0f, 65.0f, 8.0f, 20.0f, 0.95f, 0.75f, 0.25f, 0.35f); // Tầng Hoàng Kim (Golden)

        matrices.pop();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void renderAuroraBand(BufferBuilder buffer, Matrix4f model, float t, float radius, float baseH, float ribbonH, float r, float g, float b, float alpha) {
        buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        int segments = 60;

        for (int i = 0; i <= segments; i++) {
            float angle = (float) i / segments * MathHelper.TAU;
            float x = MathHelper.cos(angle) * radius;
            float z = MathHelper.sin(angle) * radius;

            // Sóng hài bậc 2 tạo độ gợn dập dờn như dải lụa
            float wave1 = MathHelper.sin(angle * 4.0f + t) * 9.0f;
            float wave2 = MathHelper.cos(angle * 2.0f - t * 0.7f) * 6.0f;
            float yBottom = baseH + wave1 + wave2;
            float yTop = yBottom + ribbonH + MathHelper.sin(angle * 3.0f + t * 0.5f) * 5.0f;

            // Chuyển màu theo góc uốn lượn
            float lum = MathHelper.sin(angle * 2.0f + t) * 0.2f + 0.8f;

            buffer.vertex(model, x, yBottom, z).color(r * lum, g * lum, b * lum, 0.05f).next();
            buffer.vertex(model, x, yTop, z).color(r * lum, g * lum, b * lum, alpha).next();
        }
        tessellatorDraw(buffer);
    }

    private static void tessellatorDraw(BufferBuilder buffer) {
        Tessellator.getInstance().draw();
    }
}
