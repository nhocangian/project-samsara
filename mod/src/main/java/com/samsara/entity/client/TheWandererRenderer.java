package com.samsara.entity.client;

import com.samsara.entity.TheWandererEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Renderer cho The Wanderer - "Lữ Khách Vô Danh".
 *  - Chỉ hiện khi nhìn qua ống kính máy quay (mắt thường không thấy cả bóng).
 *  - Hai mắt 1x1 luôn phát sáng (EyesFeatureRenderer, wanderer_eyes.png).
 *  - Mờ dần khi đứng trong ánh sáng block (đuốc, đèn): "fade-out khi gặp ánh sáng".
 */
public class TheWandererRenderer extends MobEntityRenderer<TheWandererEntity, TheWandererModel> {
    public static final Identifier TEXTURE = new Identifier("samsara", "textures/entity/wanderer.png");
    private static final Identifier EYES = new Identifier("samsara", "textures/entity/wanderer_eyes.png");
    private static final RenderLayer EYES_LAYER = RenderLayer.getEyes(EYES);

    public TheWandererRenderer(EntityRendererFactory.Context context) {
        super(context, new TheWandererModel(TheWandererModel.getTexturedModelData().createModel()), 0.0f);
        this.addFeature(new EyesFeatureRenderer<>(this) {
            @Override
            public RenderLayer getEyesTexture() {
                return EYES_LAYER;
            }
        });
    }

    @Override
    public Identifier getTexture(TheWandererEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(TheWandererEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // Thực thể siêu nhiên vô hình trước mắt thường: chỉ thấy (cả bóng) qua máy quay
        boolean throughCam = com.samsara.client.CamcorderClientManager.isFullyAimed();
        if (!throughCam) {
            this.shadowRadius = 0.0f;
            return;
        }
        this.shadowRadius = 0.0f; // lơ lửng: không đổ bóng xuống đất
        int blockLight = LightmapTextureManager.getBlockLightCoordinates(light);
        // Chỉ nhạt đi khi đứng sát nguồn sáng mạnh (block light > 7), và không bao giờ trong suốt quá nửa
        this.model.fade = MathHelper.clamp(1.0f - Math.max(0, blockLight - 7) / 8.0f * 0.45f, 0.55f, 1.0f);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        this.model.fade = 1.0f;
    }

    @Nullable
    @Override
    protected RenderLayer getRenderLayer(TheWandererEntity entity, boolean showBody, boolean translucent, boolean showOutline) {
        // Trong suốt để mũ trùm hở mặt, rìa áo rách và hiệu ứng mờ dần hoạt động
        return RenderLayer.getEntityTranslucent(getTexture(entity));
    }
}
