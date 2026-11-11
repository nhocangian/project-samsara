package com.samsara.client.tangduoi;

import com.samsara.registry.SamsaraDimensions;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Tầng 2: các đàn chim bay vòng quanh những khối khổng lồ, chỉ là hình bóng đen vỗ cánh ở rất xa. Vẽ phía client, không phải
 * thực thể. Mỗi con là một tấm quay về phía camera; texture trắng có hai hình cánh (nửa trái, nửa phải), mỗi con tô một sắc xám.
 */
public final class LoiDiNhapBirds {
    private LoiDiNhapBirds() {}

    private static final Identifier TEXTURE = new Identifier("samsara", "textures/misc/chim_xa.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    /** Đàn: tâm x, y, z (toạ độ thế giới), bán kính vòng, số con, tốc độ (rad/giây, âm là bay ngược chiều). */
    private static final double[][] FLOCKS = {
            {-300, 215, -430, 165, 11, 0.055},
            {60, 300, -300, 120, 9, -0.065},
            {-420, 260, -670, 130, 8, 0.05},
            {-40, 250, -560, 190, 7, -0.04},
            {640, 420, -900, 260, 14, 0.03},
    };
    private static final float HALF_W = 3.4f;
    private static final float HALF_H = 1.7f;

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(LoiDiNhapBirds::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.world.getRegistryKey() != SamsaraDimensions.LOI_DI_NHAP_WORLD_KEY) return;
        if (context.consumers() == null || context.matrixStack() == null) return;

        Camera camera = context.camera();
        Vec3d cam = camera.getPos();
        double time = (client.world.getTime() + context.tickDelta()) / 20.0;
        Vector3f right = new Vector3f(camera.getDiagonalPlane()).normalize();
        Vector3f up = new Vector3f(camera.getVerticalPlane()).normalize();

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = context.consumers().getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        for (int f = 0; f < FLOCKS.length; f++) {
            double[] flock = FLOCKS[f];
            int count = (int) flock[4];
            for (int i = 0; i < count; i++) {
                // Mỗi con lệch vòng và độ cao một chút, cả đàn đi thành một dải
                double phase = time * flock[5] + f * 1.9 + i * 0.075;
                double radius = flock[3] + Math.sin(i * 2.4 + f) * 16.0;
                double x = flock[0] + Math.cos(phase) * radius - cam.x;
                double y = flock[1] + Math.sin(i * 1.3 + time * 0.4) * 7.0 + Math.sin(i * 3.7) * 9.0 - cam.y;
                double z = flock[2] + Math.sin(phase) * radius - cam.z;
                boolean wingsUp = Math.sin(time * 5.2 + i * 1.9 + f) > 0.0;
                float u0 = wingsUp ? 0.0f : 0.5f;
                // Mỗi con một sắc từ gần trắng tới gần đen
                int shade = 30 + (int) (Math.abs(Math.sin(i * 12.9898 + f * 78.233) * 43758.5453) % 1.0 * 200);
                bird(vc, m, (float) x, (float) y, (float) z, right, up, u0, shade);
            }
        }
        matrices.pop();
    }

    private static void bird(VertexConsumer vc, Matrix4f m, float x, float y, float z, Vector3f right, Vector3f up, float u0, int shade) {
        float rx = right.x * HALF_W;
        float ry = right.y * HALF_W;
        float rz = right.z * HALF_W;
        float ux = up.x * HALF_H;
        float uy = up.y * HALF_H;
        float uz = up.z * HALF_H;
        vertex(vc, m, x - rx + ux, y - ry + uy, z - rz + uz, u0, 0f, shade);
        vertex(vc, m, x - rx - ux, y - ry - uy, z - rz - uz, u0, 1f, shade);
        vertex(vc, m, x + rx - ux, y + ry - uy, z + rz - uz, u0 + 0.5f, 1f, shade);
        vertex(vc, m, x + rx + ux, y + ry + uy, z + rz + uz, u0 + 0.5f, 0f, shade);
    }

    /** Alpha 254 là dấu cho shader world4 biết đây là chim, không phải khối khổng lồ (không lát gạch, không sương theo độ cao). */
    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int shade) {
        vc.vertex(m, x, y, z).color(shade, shade, shade, 254).texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV).light(FULL_BRIGHT).normal(0, 1, 0).next();
    }
}
