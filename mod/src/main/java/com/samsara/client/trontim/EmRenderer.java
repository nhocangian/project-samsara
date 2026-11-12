package com.samsara.client.trontim;

import com.samsara.entity.EmEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Đứa em / kẻ ngồi trốn. Bình thường không vẽ gì: chỉ hiện trong lượt vẽ tiếng vọng của EchoRenderer (khi {@link #echoAlpha} > 0),
 * với màu đỉnh mang dấu tiếng vọng để shader world5 cho texture skin sáng lên.
 */
public class EmRenderer extends LivingEntityRenderer<EmEntity, PlayerEntityModel<EmEntity>> {
    /** Đặt bởi EchoRenderer quanh lời gọi vẽ; 0 thì không vẽ. */
    static float echoAlpha = 0f;
    static float echoBright = 1f;

    public EmRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.0f);
    }

    @Override
    public void render(EmEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (echoAlpha <= 0f) return;
        float bright = echoBright, alpha = echoAlpha;
        super.render(entity, yaw, tickDelta, matrices, layer -> new Marked(consumers.getBuffer(layer), bright, alpha), light);
    }

    /** Tư thế ngồi (chân duỗi trước) chỉ đặt ở client, ngay trước khi model xếp dáng; không đụng tới logic cưỡi của server. */
    @Override
    protected void scale(EmEntity entity, MatrixStack matrices, float amount) {
        super.scale(entity, matrices, amount);
        this.model.riding = entity.isHider();
    }

    /** Ngồi bệt dưới sàn tủ: hạ cả người xuống để hông chạm sàn. */
    @Override
    protected void setupTransforms(EmEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta) {
        super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta);
        if (entity.isHider()) matrices.translate(0.0, -0.7, 0.0);
    }

    @Override
    protected RenderLayer getRenderLayer(EmEntity entity, boolean showBody, boolean translucent, boolean showOutline) {
        return RenderLayer.getEntityTranslucent(getTexture(entity));
    }

    @Override
    public Identifier getTexture(EmEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity.usesPlayerSkin() && client.player != null) return client.player.getSkinTexture();
        return DefaultSkinHelper.getTexture();
    }

    @Override
    protected boolean hasLabel(EmEntity entity) {
        return false;
    }

    /** Thay màu đỉnh bằng dấu tiếng vọng (r = độ sáng, g = b = 253/255), nhân độ trong. */
    private record Marked(VertexConsumer inner, float bright, float alpha) implements VertexConsumer {
        private static final int MARK = 253;

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            inner.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int a) {
            inner.color((int) (bright * 255), MARK, MARK, (int) (a * alpha));
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            inner.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            inner.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            inner.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            inner.normal(x, y, z);
            return this;
        }

        @Override
        public void next() {
            inner.next();
        }

        @Override
        public void fixedColor(int red, int green, int blue, int a) {
            inner.fixedColor((int) (bright * 255), MARK, MARK, (int) (a * alpha));
        }

        @Override
        public void unfixColor() {
            inner.unfixColor();
        }
    }
}
