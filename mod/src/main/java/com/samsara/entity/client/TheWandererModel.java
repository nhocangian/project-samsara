package com.samsara.entity.client;

import com.samsara.entity.TheWandererEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * The Wanderer - "Lữ Khách Vô Danh" (theo concept art). Cao ~2.5 block (40 unit).
 *
 * Kỹ thuật dựng giống Blockbench: khối đặc tối bên trong (đầu, mũ, thân, tay) được phủ bởi nhiều VÒNG VẠT VẢI
 * (tấm phẳng dày 0, texture có mép dưới rách trong suốt) chồng lên nhau như vảy, mỗi vòng xoè ra ngoài nhiều hơn
 * vòng trên:
 *   mũ (khoác xuống vai) -> áo choàng vai -> vạt giữa -> vạt dưới gần chạm đất; tay áo cũng có vòng vạt riêng.
 * Mặt ẩn sâu trong mũ, chỉ còn 2 mắt sáng; cẳng tay gầy, móng dài lộ ra dưới tay áo.
 * Không có hoạt ảnh bước chân: đung đưa như cây sậy, cổ lệch, thỉnh thoảng giật đầu; các vạt lay nhẹ.
 *
 * Toạ độ: mặt đất y = 24 như vanilla; phần trên xoay quanh điểm chạm đất (torso, pivot y = 24),
 * toạ độ con của torso là tương đối: 0 = mặt đất, -40 = đỉnh mũ. Texture 256x128 (tools/generate_wanderer.py).
 */
public class TheWandererModel extends EntityModel<TheWandererEntity> {
    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final List<ModelPart> panels = new ArrayList<>();
    private final List<Float> panelBasePitch = new ArrayList<>();
    private final List<Float> panelBaseRoll = new ArrayList<>();
    /** Mức bị gió nâng của từng vạt: vạt dưới bay nhiều nhất, mũ gần như yên. */
    private final List<Float> panelLift = new ArrayList<>();
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    /** Độ mờ (0..1) do renderer đặt mỗi khung hình. */
    public float fade = 1.0f;

    /** Một vòng vạt: kích thước tấm + các biến thể UV trong atlas. */
    private record Ring(int w, int h, int[] uvU, int uvV) {}

    private static final Ring HOOD = new Ring(4, 6, new int[]{0, 8, 16}, 32);
    private static final Ring CAPE = new Ring(4, 10, new int[]{24, 32, 40}, 32);
    private static final Ring MID = new Ring(4, 14, new int[]{48, 56, 64}, 32);
    private static final Ring HEM = new Ring(5, 17, new int[]{72, 82, 92, 102}, 32);
    private static final Ring SLEEVE = new Ring(3, 10, new int[]{112, 118, 124}, 32);

    public TheWandererModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.head = torso.getChild("head");
        this.rightArm = torso.getChild("right_arm");
        this.leftArm = torso.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        collect(torso, -1.0f);
        collect(head, 0.15f);
        collect(rightArm, 0.4f);
        collect(leftArm, 0.4f);
    }

    /** @param lift mức gió nâng; âm = tính theo độ cao (vạt càng thấp càng bay) */
    private void collect(ModelPart parent, float lift) {
        for (int i = 0; parent.hasChild("panel_" + i); i++) {
            ModelPart p = parent.getChild("panel_" + i);
            panels.add(p);
            panelBasePitch.add(p.pitch);
            panelBaseRoll.add(p.roll);
            panelLift.add(lift >= 0 ? lift : MathHelper.clamp((p.pivotY + 32.0f) / 14.0f, 0.0f, 1.0f));
        }
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        Random random = Random.create(1998L);

        root.addChild("right_leg", ModelPartBuilder.create().uv(0, 16).cuboid(-1.0f, 0.0f, -1.0f, 2, 10, 2),
                ModelTransform.pivot(-1.5f, 14.0f, 0.0f));
        root.addChild("left_leg", ModelPartBuilder.create().uv(0, 16).mirrored().cuboid(-1.0f, 0.0f, -1.0f, 2, 10, 2),
                ModelTransform.pivot(1.5f, 14.0f, 0.0f));

        ModelPartData torso = root.addChild("torso",
                ModelPartBuilder.create().uv(72, 0).cuboid(-3.5f, -32.0f, -2.0f, 7, 18, 4),
                ModelTransform.pivot(0.0f, 24.0f, 0.0f));

        ModelPartData head = torso.addChild("head", ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-4.0f, -8.0f, -4.0f, 8, 8, 8)
                        .uv(32, 0).cuboid(-5.0f, -9.5f, -5.5f, 10, 10, 10),
                ModelTransform.pivot(0.0f, -32.0f, -0.5f));

        ModelPartData rightArm = torso.addChild("right_arm", arm(), ModelTransform.pivot(-5.5f, -30.0f, 0.0f));
        ModelPartData leftArm = torso.addChild("left_arm", arm(), ModelTransform.pivot(5.5f, -30.0f, 0.0f));
        for (ModelPartData a : new ModelPartData[]{rightArm, leftArm}) {
            for (int f = 0; f < 3; f++) {
                a.addChild("claw_" + f, ModelPartBuilder.create().uv(120, 0).cuboid(-0.5f, 0.0f, -0.5f, 1, 5, 1),
                        ModelTransform.of(-0.7f + f * 0.7f, 19.0f, -0.4f, -0.12f, 0.0f, (f - 1) * 0.08f));
            }
        }

        int[] torsoIdx = {0};
        int[] headIdx = {0};
        // Mũ trùm: một vòng vạt quanh nửa dưới mũ khoác xuống vai (chừa mặt trước), và một lớp vạt trên đỉnh lưng mũ
        ring(head, headIdx, HOOD, 11, -3.5f, 5.6f, 6.0f, 0.18f, true, random);
        ring(head, headIdx, HOOD, 7, -8.5f, 5.4f, 5.9f, 0.10f, true, random);
        // Áo choàng vai, vạt giữa, vạt dưới: mỗi vòng to hơn và xoè hơn vòng trên
        ring(torso, torsoIdx, CAPE, 14, -31.0f, 5.6f, 4.0f, 0.22f, false, random);
        ring(torso, torsoIdx, MID, 16, -25.0f, 5.4f, 3.9f, 0.16f, false, random);
        ring(torso, torsoIdx, HEM, 18, -18.0f, 5.8f, 4.3f, 0.20f, false, random);
        // Tay áo
        for (ModelPartData a : new ModelPartData[]{rightArm, leftArm}) {
            ring(a, new int[]{0}, SLEEVE, 6, 3.0f, 2.6f, 2.6f, 0.15f, false, random);
        }
        return TexturedModelData.of(data, 256, 128);
    }

    private static ModelPartBuilder arm() {
        return ModelPartBuilder.create()
                .uv(96, 0).cuboid(-2.0f, -1.0f, -2.0f, 4, 12, 4)
                .uv(112, 0).cuboid(-1.0f, 11.0f, -1.0f, 2, 8, 2);
    }

    /**
     * Một vòng {@code count} vạt quanh trục đứng: tâm ở (0, y, 0), bán kính rx (ngang) / rz (trước-sau).
     * Mỗi vạt quay mặt ra ngoài, xoè ra theo {@code flare} rad (+ ngẫu nhiên), lệch thêm 2-6 độ.
     * {@code openFront}: bỏ các vạt che trước mặt (cho mũ trùm).
     */
    private static void ring(ModelPartData parent, int[] idx, Ring ring, int count, float y, float rx, float rz,
                             float flare, boolean openFront, Random random) {
        for (int i = 0; i < count; i++) {
            float a = (float) (i * Math.PI * 2.0 / count) + (random.nextFloat() - 0.5f) * 0.25f;
            // a = 0 hướng ra sau lưng (+z); a = PI là trước mặt (-z)
            if (openFront && Math.abs(MathHelper.wrapDegrees(a * MathHelper.DEGREES_PER_RADIAN - 180.0f)) < 50.0f) continue;
            float x = MathHelper.sin(a) * rx;
            float z = MathHelper.cos(a) * rz;
            int uvU = ring.uvU()[random.nextInt(ring.uvU().length)];
            float pitch = flare + (random.nextFloat() - 0.5f) * 0.12f;
            float roll = tilt(random);
            float yJitter = (random.nextFloat() - 0.5f) * 1.5f;
            parent.addChild("panel_" + idx[0]++, ModelPartBuilder.create().uv(uvU, ring.uvV())
                            .cuboid(-ring.w() / 2.0f, 0.0f, 0.0f, ring.w(), ring.h(), 0),
                    ModelTransform.of(x, y + yJitter, z, pitch, a, roll));
        }
    }

    /** Lệch ngẫu nhiên 2-6 độ, hai chiều. */
    private static float tilt(Random random) {
        float deg = 2.0f + random.nextFloat() * 4.0f;
        return (random.nextBoolean() ? deg : -deg) * MathHelper.RADIANS_PER_DEGREE;
    }

    @Override
    public void setAngles(TheWandererEntity entity, float limbAngle, float limbDistance, float t, float headYaw, float headPitch) {
        torso.roll = MathHelper.sin(t * 0.03f) * 0.035f;
        torso.pitch = 0.06f + MathHelper.sin(t * 0.021f) * 0.02f;

        head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.6f;
        head.pitch = 0.14f + headPitch * MathHelper.RADIANS_PER_DEGREE * 0.5f;
        int phase = (int) t % 97;
        head.roll = phase < 3 ? 0.42f : 0.12f;

        if (entity.areHandsRaised()) {
            rightArm.pitch = -MathHelper.PI * 0.85f;
            leftArm.pitch = -MathHelper.PI * 0.85f;
            rightArm.roll = -0.2f;
            leftArm.roll = 0.2f;
        } else {
            rightArm.pitch = 0.05f + MathHelper.sin(t * 0.025f) * 0.03f;
            leftArm.pitch = 0.05f + MathHelper.sin(t * 0.025f + 1.7f) * 0.03f;
            rightArm.roll = 0.08f;
            leftArm.roll = -0.08f;
        }

        // Lơ lửng: không chạm đất, không có chân; nhấp nhô rất chậm
        rightLeg.visible = false;
        leftLeg.visible = false;
        torso.pivotY = 24.0f - 2.0f - MathHelper.sin(t * 0.045f) * 0.8f;

        // Một luồng gió vô hình nâng tà áo: vạt dưới xoè và lượn thành sóng chạy vòng quanh thân
        for (int i = 0; i < panels.size(); i++) {
            ModelPart p = panels.get(i);
            float lift = panelLift.get(i);
            float wave = MathHelper.sin(t * 0.05f + p.yaw * 2.0f);
            float flutter = MathHelper.sin(t * 0.13f + i * 0.7f);
            p.pitch = panelBasePitch.get(i) + lift * (0.22f + 0.14f * wave) + flutter * (0.02f + 0.03f * lift);
            p.roll = panelBaseRoll.get(i) + lift * 0.06f * MathHelper.sin(t * 0.09f + p.yaw * 3.0f);
        }
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha * fade);
    }
}
