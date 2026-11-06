package com.samsara.entity.client;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.util.math.MathHelper;

/**
 * "Tàn Hồn Phantom" - hình dạng thật của Phantom qua ống kính máy quay (Master Production Document, mục II.2:
 * tàn dư linh hồn kiếp cũ bị phân rã). Một thân người gầy trơ sườn đang bay úp mặt, hai tay dang ra làm
 * xương cánh, màng da rách căng giữa tay và thân; hai chân kéo lê phía sau nối bằng màng đuôi.
 * Cùng khung toạ độ với model Phantom vanilla (mặt hướng -z, cánh dang theo ±x) để dùng chung phép biến đổi.
 */
public class SoulPhantomModel extends EntityModel<PhantomEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart leftOuter;
    private final ModelPart rightWing;
    private final ModelPart rightOuter;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public SoulPhantomModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.leftWing = body.getChild("left_wing");
        this.leftOuter = leftWing.getChild("outer");
        this.rightWing = body.getChild("right_wing");
        this.rightOuter = rightWing.getChild("outer");
        this.leftLeg = body.getChild("left_leg");
        this.rightLeg = body.getChild("right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        ModelPartData body = root.addChild("body", ModelPartBuilder.create()
                        .uv(24, 0).cuboid(-3.0f, -1.5f, -6.0f, 6, 3, 10),
                ModelTransform.of(0.0f, 1.0f, 0.0f, -0.1f, 0.0f, 0.0f));

        body.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0f, -3.0f, -6.0f, 6, 6, 6),
                ModelTransform.of(0.0f, 0.0f, -6.0f, 0.25f, 0.0f, 0.0f));

        // Cánh trái (+x): cánh tay + màng trong; khớp khuỷu -> cẳng tay + 2 nan ngón + màng ngoài
        ModelPartData lw = body.addChild("left_wing", ModelPartBuilder.create()
                        .uv(56, 0).cuboid(0.0f, -0.5f, -0.5f, 7, 1, 1)
                        .uv(0, 32).cuboid(0.0f, 0.0f, 0.0f, 8, 0, 10),
                ModelTransform.pivot(3.0f, -1.0f, -4.0f));
        lw.addChild("outer", ModelPartBuilder.create()
                        .uv(56, 4).cuboid(0.0f, -0.5f, -0.5f, 8, 1, 1)
                        .uv(40, 32).cuboid(0.0f, 0.0f, 0.0f, 9, 0, 8),
                ModelTransform.pivot(7.0f, 0.0f, 0.0f));
        ModelPartData lo = lw.getChild("outer");
        lo.addChild("finger_0", ModelPartBuilder.create().uv(56, 8).cuboid(0.0f, -0.5f, -0.5f, 5, 1, 1),
                ModelTransform.of(8.0f, 0.0f, 0.0f, 0.0f, -0.5f, 0.0f));
        lo.addChild("finger_1", ModelPartBuilder.create().uv(56, 8).cuboid(0.0f, -0.5f, -0.5f, 5, 1, 1),
                ModelTransform.of(6.0f, 0.0f, 0.5f, 0.0f, -1.0f, 0.0f));

        // Cánh phải (-x), đối xứng
        ModelPartData rw = body.addChild("right_wing", ModelPartBuilder.create()
                        .uv(56, 0).mirrored().cuboid(-7.0f, -0.5f, -0.5f, 7, 1, 1)
                        .uv(0, 32).mirrored().cuboid(-8.0f, 0.0f, 0.0f, 8, 0, 10),
                ModelTransform.pivot(-3.0f, -1.0f, -4.0f));
        rw.addChild("outer", ModelPartBuilder.create()
                        .uv(56, 4).mirrored().cuboid(-8.0f, -0.5f, -0.5f, 8, 1, 1)
                        .uv(40, 32).mirrored().cuboid(-9.0f, 0.0f, 0.0f, 9, 0, 8),
                ModelTransform.pivot(-7.0f, 0.0f, 0.0f));
        ModelPartData ro = rw.getChild("outer");
        ro.addChild("finger_0", ModelPartBuilder.create().uv(56, 8).mirrored().cuboid(-5.0f, -0.5f, -0.5f, 5, 1, 1),
                ModelTransform.of(-8.0f, 0.0f, 0.0f, 0.0f, 0.5f, 0.0f));
        ro.addChild("finger_1", ModelPartBuilder.create().uv(56, 8).mirrored().cuboid(-5.0f, -0.5f, -0.5f, 5, 1, 1),
                ModelTransform.of(-6.0f, 0.0f, 0.5f, 0.0f, 1.0f, 0.0f));

        // Hai chân kéo lê phía sau + màng đuôi
        body.addChild("left_leg", ModelPartBuilder.create().uv(0, 16).cuboid(-1.0f, -1.0f, 0.0f, 2, 2, 10),
                ModelTransform.pivot(1.4f, 0.0f, 4.0f));
        body.addChild("right_leg", ModelPartBuilder.create().uv(0, 16).mirrored().cuboid(-1.0f, -1.0f, 0.0f, 2, 2, 10),
                ModelTransform.pivot(-1.4f, 0.0f, 4.0f));
        body.addChild("tail", ModelPartBuilder.create().uv(80, 32).cuboid(-2.0f, 0.0f, 0.0f, 4, 0, 8),
                ModelTransform.pivot(0.0f, 0.0f, 4.0f));

        return TexturedModelData.of(data, 128, 64);
    }

    @Override
    public void setAngles(PhantomEntity entity, float limbAngle, float limbDistance, float animationProgress,
                          float headYaw, float headPitch) {
        // Cùng nhịp vỗ với Phantom vanilla (khớp tiếng vỗ cánh)
        float f = (entity.getWingFlapTickOffset() + animationProgress) * 7.448451f * MathHelper.RADIANS_PER_DEGREE;
        float flap = MathHelper.cos(f) * 16.0f * MathHelper.RADIANS_PER_DEGREE;
        leftWing.roll = flap;
        rightWing.roll = -flap;
        // Khớp khuỷu trễ nhịp một chút: cánh gập lại như tay người đang quẫy
        float elbow = MathHelper.cos(f - 0.7f) * 22.0f * MathHelper.RADIANS_PER_DEGREE;
        leftOuter.roll = elbow;
        rightOuter.roll = -elbow;
        // Đầu ngẩng / gục theo nhịp, chân đá lơ lửng
        head.pitch = 0.25f + MathHelper.sin(f) * 0.08f;
        leftLeg.pitch = MathHelper.sin(f * 0.5f) * 0.18f;
        rightLeg.pitch = -MathHelper.sin(f * 0.5f) * 0.18f;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
