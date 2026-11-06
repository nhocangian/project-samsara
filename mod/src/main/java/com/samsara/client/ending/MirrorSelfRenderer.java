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
 * Bóng phản chiếu của chính người chơi trong đoạn kết - hình dạng duy nhất của <System> (Video 9, cảnh 04-05).
 * Vẽ thuần phía client (không phải entity), theo 3 lớp:
 *  1. Thân làm bằng vũ trụ: model người chơi vẽ bằng lớp cổng End (trời sao chuyển động trong dáng người).
 *  2. Skin của chính bạn phủ mờ lên trên, để vẫn nhận ra đó là mình ("Họ cũng chính là cậu").
 *  3. Viền sáng tím mờ (model phồng nhẹ, lớp năng lượng kiểu creeper tích điện) để dáng người vẫn đọc được
 *     trên nền trời đen khi đã lớn tới x45.
 * Đầu bắt chước hướng nhìn của người chơi như tấm gương, rồi trễ nửa nhịp từ câu đầu của <System>
 * (EndingSequenceClient.getMirrorLook). Mỗi lần nhảy cỡ: vài tick nhiễu băng VHS (rung ngang, tách màu, mất hình).
 * Ở x45, texture hố đen Gargantua (sun_stage4) hiện đúng chỗ con mắt, và vị trí được canh để con mắt
 * trùng Gargantua trên trời The Womb.
 */
@Environment(EnvType.CLIENT)
public class MirrorSelfRenderer {
    private static PlayerEntityModel<AbstractClientPlayerEntity> wideModel;
    private static PlayerEntityModel<AbstractClientPlayerEntity> slimModel;
    private static final Identifier AURA_TEXTURE = new Identifier("textures/entity/creeper/creeper_armor.png");
    private static final net.minecraft.util.math.random.Random RANDOM = net.minecraft.util.math.random.Random.create();

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
        // Đứng thẳng, tay buông; đầu nhìn theo hướng nhìn của bạn như tấm gương (có thể trễ nửa nhịp)
        float[] look = EndingSequenceClient.getMirrorLook(tickDelta);
        m.head.pitch = 0.08f + look[1];
        m.head.roll = (float) Math.sin(time * 0.02f) * 0.06f;
        m.head.yaw = look[0];
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
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - EndingSequenceClient.getReflectionYaw(tickDelta)));
        matrices.scale(-scale, -scale, scale);
        matrices.translate(0.0, -1.501, 0.0);

        int glitch = EndingSequenceClient.getGlitchTicks();
        boolean dropout = glitch > 0 && RANDOM.nextFloat() < 0.25f;      // mất hình một khung như băng nhảy track
        if (glitch > 0) {
            // Rung ngang theo dòng quét
            matrices.translate((RANDOM.nextFloat() - 0.5f) * 0.35f, 0.0, 0.0);
        }
        float flicker = 0.9f + (float) Math.sin(time * 0.7f) * 0.05f;

        if (!dropout) {
            // 1. Thân là vũ trụ: trời sao chuyển động bên trong dáng người
            m.render(matrices, consumers.getBuffer(RenderLayer.getEndPortal()), 0xF000F0, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);
            consumers.draw(RenderLayer.getEndPortal());

            // 2. Skin của chính bạn, mờ, ám tím
            RenderLayer skinLayer = RenderLayer.getEntityTranslucent(skin);
            m.render(matrices, consumers.getBuffer(skinLayer), 0xF000F0, OverlayTexture.DEFAULT_UV, 0.45f, 0.38f, 0.62f, 0.30f * flicker);
            consumers.draw(skinLayer);

            // 3. Viền sáng: model phồng nhẹ quanh tâm thân, lớp năng lượng trôi chậm
            matrices.push();
            matrices.translate(0.0, 0.75, 0.0);
            matrices.scale(1.06f, 1.04f, 1.06f);
            matrices.translate(0.0, -0.75, 0.0);
            float drift = time * 0.004f;
            RenderLayer aura = RenderLayer.getEnergySwirl(AURA_TEXTURE, drift % 1.0f, (drift * 0.7f) % 1.0f);
            m.render(matrices, consumers.getBuffer(aura), 0xF000F0, OverlayTexture.DEFAULT_UV, 0.30f, 0.16f, 0.50f, 1.0f);
            consumers.draw(aura);
            matrices.pop();
        }

        if (glitch > 0 && !dropout) {
            // Tách màu đỏ / lam lệch hai bên như đầu từ đọc lệch
            RenderLayer ghost = RenderLayer.getEntityTranslucent(skin);
            float off = 0.08f + RANDOM.nextFloat() * 0.12f;
            matrices.push();
            matrices.translate(off, 0.0, 0.0);
            m.render(matrices, consumers.getBuffer(ghost), 0xF000F0, OverlayTexture.DEFAULT_UV, 1.0f, 0.15f, 0.2f, 0.35f);
            consumers.draw(ghost);
            matrices.translate(-2 * off, 0.0, 0.0);
            m.render(matrices, consumers.getBuffer(ghost), 0xF000F0, OverlayTexture.DEFAULT_UV, 0.15f, 0.8f, 1.0f, 0.35f);
            consumers.draw(ghost);
            matrices.pop();
        }

        if (EndingSequenceClient.isEyeRevealed() && !dropout) {
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
