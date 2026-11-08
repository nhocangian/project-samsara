package com.samsara.entity.client;

import com.samsara.client.ending.EndingSequenceClient;
import com.samsara.entity.PrimordialEggEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Vẽ Quả Trứng Nguyên Thủy bằng các khối vuông xếp tầng (pixel art 3D, như trứng Rồng Ender phóng to).
 *  - Mỗi tầng là 2 khối vuông (thẳng + xoay 45°) để mặt cắt gần tròn mà vẫn "khối".
 *  - Hai lớp: vỏ kim loại (sáng theo ánh sáng thế giới) + đường nứt hổ phách phát sáng (getEyes).
 *  - Nhịp tim 60/phút: "thình-thịch" phồng nhẹ; khi đoạn kết bắt đầu nở, nhịp nhanh và nứt sáng hơn.
 *  - 7 mảnh vỏ bay vòng quanh; mảnh thứ 7 vừa vỡ, phát sáng.
 */
public class PrimordialEggRenderer extends EntityRenderer<PrimordialEggEntity> {
    private static final Identifier SHELL = new Identifier("samsara", "textures/entity/primordial_egg.png");
    private static final Identifier CRACKS = new Identifier("samsara", "textures/entity/primordial_egg_cracks.png");

    /** Chiều cao trứng theo pixel model (16 px = 1 block). */
    private static final int HEIGHT_PX = 96;
    private static final int LAYERS = 16;
    private static final float MAX_HALF_WIDTH_PX = 30.0f;

    private final ModelPart egg;
    private final ModelPart shard;

    public PrimordialEggRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.egg = createEgg().createModel();
        this.shard = createShard().createModel();
        this.shadowRadius = 0.0f;
    }

    /** Bán kính (px) của trứng ở độ cao tỉ lệ t (0 = đáy, 1 = đỉnh): đầu nhọn phía trên. */
    private static float halfWidthAt(float t) {
        float u = t * 2.0f - 1.0f;                // -1..1
        float stretch = u > 0 ? 1.0f : 0.82f;     // nửa trên dài hơn -> đầu nhọn
        float v = u * stretch;
        return MAX_HALF_WIDTH_PX * (float) Math.sqrt(Math.max(0.0, 1.0 - v * v)) * (1.0f - 0.18f * Math.max(0, u));
    }

    private static TexturedModelData createEgg() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        float layerH = (float) HEIGHT_PX / LAYERS;
        for (int i = 0; i < LAYERS; i++) {
            float t = (i + 0.5f) / LAYERS;
            float r = Math.max(4.0f, Math.round(halfWidthAt(t)));
            float y0 = -(i + 1) * layerH; // model Y hướng xuống, nên lên trên là âm
            ModelPartBuilder box = ModelPartBuilder.create().uv(0, 0)
                    .cuboid(-r, y0, -r, r * 2, layerH, r * 2, Dilation.NONE);
            root.addChild("layer_" + i, box, ModelTransform.NONE);
            // Khối xoay 45° nhỏ hơn một chút: bo tròn góc nhìn
            float r2 = r * 0.86f;
            root.addChild("layer_r_" + i, ModelPartBuilder.create().uv(0, 0)
                            .cuboid(-r2, y0, -r2, r2 * 2, layerH, r2 * 2, Dilation.NONE),
                    ModelTransform.of(0, 0, 0, 0, (float) Math.PI / 4.0f, 0));
        }
        return TexturedModelData.of(data, 512, 256);
    }

    private static TexturedModelData createShard() {
        ModelData data = new ModelData();
        data.getRoot().addChild("shard", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-5.0f, -7.0f, -1.5f, 10.0f, 14.0f, 3.0f), ModelTransform.NONE);
        return TexturedModelData.of(data, 512, 256);
    }

    @Override
    public boolean shouldRender(PrimordialEggEntity entity, Frustum frustum, double x, double y, double z) {
        return true; // trứng to hơn hitbox nhiều; không để bị cắt khỏi khung nhìn
    }

    /** Cỡ vỏ trứng (người chơi chọn theo ảnh ở khung x45, đứng cách 6 block); thu nhỏ quanh tâm để trứng vẫn giữa vòng hố đen. */
    private static final float EGG_SCALE = 0.92f;
    private static final float EGG_CENTER_Y = 3.0f;

    @Override
    public void render(PrimordialEggEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        float time = entity.age + tickDelta;
        float hatch = EndingSequenceClient.getHatchProgress(tickDelta); // 0..1 trong đoạn kết

        // Nhịp tim: 1 nhịp/giây (nhanh dần khi nở), dạng "thình-thịch"
        float bpm = 60.0f + hatch * 90.0f;
        float beat = (time / 20.0f * bpm / 60.0f) % 1.0f;
        float pulse = (float) (Math.exp(-beat * 14.0) + 0.55 * Math.exp(-Math.max(0, beat - 0.22) * 14.0) * (beat > 0.22 ? 1 : 0));
        float scale = EGG_SCALE * (1.0f + pulse * (0.025f + hatch * 0.05f));
        float shake = hatch > 0 ? (float) Math.sin(time * 3.1f) * hatch * 0.06f : 0.0f;

        matrices.push();
        matrices.translate(shake, Math.sin(time * 0.05) * 0.15 + EGG_CENTER_Y * (1.0f - EGG_SCALE), -shake);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 0.4f));
        matrices.scale(scale, -scale, -scale); // lật trục model (Y xuống) về trục thế giới

        egg.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(SHELL)), light, OverlayTexture.DEFAULT_UV);
        float glow = MathHelper.clamp(0.55f + pulse * 0.45f + hatch * 0.6f, 0.0f, 1.0f);
        egg.render(matrices, consumers.getBuffer(RenderLayer.getEyes(CRACKS)), 0xF000F0, OverlayTexture.DEFAULT_UV,
                glow, glow, glow, 1.0f);
        matrices.pop();

        // 7 mảnh vỏ bay quanh (7 giấc ngủ)
        for (int i = 0; i < 7; i++) {
            float a = time * 0.6f + i * (360.0f / 7.0f);
            float bob = (float) Math.sin(time * 0.04f + i) * 0.4f;
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(a));
            matrices.translate(4.6f + hatch * 3.0f, 3.0f + bob + (i % 3) * 0.6f, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(time * (1.5f + i * 0.3f)));
            matrices.scale(1.0f, -1.0f, -1.0f);
            shard.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(SHELL)), light, OverlayTexture.DEFAULT_UV);
            if (i == 6) {
                shard.render(matrices, consumers.getBuffer(RenderLayer.getEyes(CRACKS)), 0xF000F0, OverlayTexture.DEFAULT_UV,
                        1.0f, 1.0f, 1.0f, 1.0f);
            }
            matrices.pop();
        }
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    @Override
    public Identifier getTexture(PrimordialEggEntity entity) {
        return SHELL;
    }
}
