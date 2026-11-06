package com.samsara.client.ending;

import com.samsara.system.celestial.SolarFractureManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Bóng phản chiếu của chính người chơi trong đoạn kết - hình dạng của <System>.
 * Vẽ thuần phía client (không phải entity): model người chơi với skin của chính bạn, nhuộm tối tím
 * gần như bóng đen không mặt, trồi lên từ mặt nước rồi "nhảy" kích thước theo đoạn thoại.
 * Ở lần nhảy cuối, texture hố đen Gargantua (sun_stage4) hiện đúng chỗ con mắt.
 */
@Environment(EnvType.CLIENT)
public class MirrorSelfRenderer {
    private static PlayerEntityModel<AbstractClientPlayerEntity> wideModel;
    private static PlayerEntityModel<AbstractClientPlayerEntity> slimModel;

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(MirrorSelfRenderer::render);
    }

    private static PlayerEntityModel<AbstractClientPlayerEntity> model(boolean slim) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (wideModel == null) {
            wideModel = new PlayerEntityModel<>(client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER), false);
            slimModel = new PlayerEntityModel<>(client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_SLIM), true);
        }
        return slim ? slimModel : wideModel;
    }

    private static void render(WorldRenderContext context) {
        if (!EndingSequenceClient.isReflectionVisible()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        float tickDelta = context.tickDelta();
        float scale = EndingSequenceClient.getReflectionScale(tickDelta);
        float rise = EndingSequenceClient.getRise(tickDelta);
        Vec3d pos = EndingSequenceClient.getReflectionPos(tickDelta);
        Vec3d cam = context.camera().getPos();
        float time = client.player.age + tickDelta;

        boolean slim = "slim".equals(client.player.getModel());
        PlayerEntityModel<AbstractClientPlayerEntity> m = model(slim);
        m.child = false;
        m.sneaking = false;
        m.riding = false;
        m.setVisible(true);
        // Đứng thẳng, tay buông, đầu hơi nghiêng như đang quan sát
        m.head.pitch = 0.08f;
        m.head.roll = (float) Math.sin(time * 0.02f) * 0.06f;
        m.head.yaw = 0.0f;
        m.body.pitch = 0.0f;
        m.rightArm.pitch = 0.0f;
        m.leftArm.pitch = 0.0f;
        m.rightArm.roll = 0.04f;
        m.leftArm.roll = -0.04f;
        m.rightLeg.pitch = 0.0f;
        m.leftLeg.pitch = 0.0f;
        m.hat.copyTransform(m.head);
        m.jacket.copyTransform(m.body);
        m.rightSleeve.copyTransform(m.rightArm);
        m.leftSleeve.copyTransform(m.leftArm);
        m.rightPants.copyTransform(m.rightLeg);
        m.leftPants.copyTransform(m.leftLeg);

        Identifier skin = client.player.getSkinTexture();
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();

        matrices.push();
        double sinkY = (1.0 - rise) * -2.2 * scale;
        matrices.translate(pos.x - cam.x, pos.y - cam.y + sinkY, pos.z - cam.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - EndingSequenceClient.getReflectionYaw()));
        matrices.scale(-scale, -scale, scale);
        matrices.translate(0.0, -1.501, 0.0);

        // Bóng tối tím, gần như không thấy mặt; nhấp nháy rất nhẹ như tín hiệu băng
        float flicker = 0.9f + (float) Math.sin(time * 0.7f) * 0.05f;
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityTranslucent(skin));
        m.render(matrices, vc, 0xF000F0, OverlayTexture.DEFAULT_UV, 0.16f, 0.13f, 0.24f, 0.93f * flicker);

        if (EndingSequenceClient.isEyeRevealed()) {
            renderEye(matrices, consumers, time);
        }
        matrices.pop();
        consumers.draw();
    }

    /**
     * Gargantua ở vị trí con mắt: một tấm hố đen (sun_stage4) dán ngay trước mặt đầu.
     * Toạ độ model: đầu là khối 8x8x8 px quanh trục cổ, mặt trước ở z = -4 px.
     */
    private static void renderEye(MatrixStack matrices, VertexConsumerProvider consumers, float time) {
        matrices.push();
        float s = 7.0f / 16.0f;                       // tấm mắt rộng ~7 px trên mặt 8 px
        matrices.translate(0.0, -4.0 / 16.0, -4.05 / 16.0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(time * 0.6f));
        Matrix4f mat = matrices.peek().getPositionMatrix();
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getEyes(SolarFractureManager.SUN_STAGE_4));
        float h = s / 2.0f;
        vertex(vc, mat, -h, -h, 0, 0);
        vertex(vc, mat, -h, h, 0, 1);
        vertex(vc, mat, h, h, 1, 1);
        vertex(vc, mat, h, -h, 1, 0);
        matrices.pop();
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, float x, float y, float u, float v) {
        vc.vertex(mat, x, y, 0.0f).color(255, 255, 255, 255).texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(0.0f, 0.0f, -1.0f).next();
    }
}
