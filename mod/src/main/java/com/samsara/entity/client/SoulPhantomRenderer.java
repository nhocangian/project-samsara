package com.samsara.entity.client;

import com.samsara.client.CamcorderClientManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PhantomEntityRenderer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Renderer thay cho Phantom vanilla:
 *  - Mắt thường: vẽ đúng Phantom vanilla (uỷ quyền cho PhantomEntityRenderer).
 *  - Qua ống kính máy quay: "Tàn Hồn Phantom" (SoulPhantomModel) - thân người gầy với cánh màng da,
 *    mắt phát sáng (lớp mắt -> quầng sáng shader). Dùng cùng phép biến đổi như renderer vanilla
 *    (hướng thân, góc chúc, phóng to theo kích thước Phantom) nên bay và lượn y hệt.
 */
public class SoulPhantomRenderer extends EntityRenderer<PhantomEntity> {
    private static final Identifier TEXTURE = new Identifier("samsara", "textures/entity/soul_phantom.png");
    private static final Identifier EYES = new Identifier("samsara", "textures/entity/soul_phantom_eyes.png");

    private final PhantomEntityRenderer vanilla;
    private final SoulPhantomModel model;

    public SoulPhantomRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.vanilla = new PhantomEntityRenderer(ctx);
        this.model = new SoulPhantomModel(SoulPhantomModel.getTexturedModelData().createModel());
        this.shadowRadius = 0.75f;
    }

    @Override
    public boolean shouldRender(PhantomEntity entity, Frustum frustum, double x, double y, double z) {
        return vanilla.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(PhantomEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        if (!CamcorderClientManager.isFullyAimed()) {
            vanilla.render(entity, yaw, tickDelta, matrices, consumers, light);
            return;
        }

        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw);
        float age = entity.age + tickDelta;

        matrices.push();
        // Giống LivingEntityRenderer + PhantomEntityRenderer: quay thân, chúc theo pitch, lật trục, phóng theo kích thước
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getPitch(tickDelta)));
        matrices.scale(-1.0f, -1.0f, 1.0f);
        float size = 1.0f + 0.15f * entity.getPhantomSize();
        matrices.scale(size, size, size);
        matrices.translate(0.0f, 1.3125f, 0.1875f);
        matrices.translate(0.0f, -1.501f, 0.0f);

        model.setAngles(entity, 0.0f, 0.0f, age, 0.0f, 0.0f);
        int overlay = OverlayTexture.getUv(0.0f, entity.hurtTime > 0 || entity.deathTime > 0);
        model.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)), light, overlay,
                1.0f, 1.0f, 1.0f, 1.0f);
        model.render(matrices, consumers.getBuffer(RenderLayer.getEyes(EYES)), 0xF000F0, OverlayTexture.DEFAULT_UV,
                1.0f, 1.0f, 1.0f, 1.0f);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    @Override
    public Identifier getTexture(PhantomEntity entity) {
        return TEXTURE;
    }
}
