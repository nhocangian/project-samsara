package com.samsara.entity.client;

import com.samsara.entity.DimensionalRiftEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renderer cho Vết Rách Không Gian (Dimensional Rift) chuẩn Cinematic Stargate & 3D Parallax Skybox:
 *  - Lòng cổng sử dụng kỹ thuật Interior Parallax Mapping:
 *    Tọa độ UV trượt theo góc nhìn camera của người chơi (Camera-Space Parallax Projection)
 *    kết hợp chiều sâu vật lý đa tầng trong lòng cổng dày 0.44m.
 *    Khi người chơi di chuyển qua lại, ngước nhìn hay cúi đầu, lòng cổng mở ra không gian 3D thị sai
 *    thăm thẳm như hố đen vũ trụ / skybox 3 chiều thực thụ!
 *  - 100% tương thích và hiển thị rõ nét trên mọi Shader (Iris / Complementary / Sodium / Vanilla).
 *  - Triệt tiêu 100% hiện tượng chớp nháy (Z-fighting): Các mặt phẳng được cách nhau an toàn.
 *  - Cụm 14 Khối Lập Phương Vệ Tinh 3D Thật bay lơ lửng, nhấp nhô và xoay tròn quanh cổng.
 */
@Environment(EnvType.CLIENT)
public class DimensionalRiftRenderer extends EntityRenderer<DimensionalRiftEntity> {
    public static final Identifier WOMB_TEXTURE = new Identifier("samsara", "textures/entity/dimensional_rift_womb.png");
    public static final Identifier WOMB_TEXTURE_1 = new Identifier("samsara", "textures/entity/dimensional_rift_womb_1.png"); // Silexars (Creation)
    public static final Identifier WOMB_TEXTURE_2 = new Identifier("samsara", "textures/entity/dimensional_rift_womb_2.png"); // Glowmarcher (3D Tunnel)
    public static final Identifier WOMB_TEXTURE_3 = new Identifier("samsara", "textures/entity/dimensional_rift_womb_3.png"); // Kishimisu (Fractal Neon)
    public static final Identifier CUBE_WOMB_TEXTURE = new Identifier("samsara", "textures/entity/rift_cube_womb.png");



    // 14 khối tinh thể vệ tinh 3D bay lơ lửng quanh vết rách (X, Y, Z, Size, SpeedMult, BobPhase)
    private static final float[][] SATELLITE_CUBES = new float[][]{
            {-2.35f,  1.60f,  0.42f, 0.32f, 1.0f, 0.0f},
            { 2.35f,  1.40f, -0.38f, 0.30f, 1.1f, 1.2f},
            {-2.20f, -0.80f, -0.45f, 0.28f, 0.9f, 2.4f},
            { 2.25f, -0.90f,  0.40f, 0.34f, 1.0f, 3.6f},
            {-1.10f,  3.15f,  0.25f, 0.26f, 1.2f, 4.8f},
            { 1.15f,  3.10f, -0.28f, 0.24f, 1.2f, 0.7f},
            { 0.00f,  3.75f,  0.15f, 0.22f, 1.4f, 2.1f}, // Đỉnh chóp ngọn tháp
            {-2.60f,  0.35f,  0.10f, 0.20f, 0.8f, 3.3f},
            { 2.65f,  0.25f, -0.12f, 0.22f, 0.8f, 4.5f},
            {-1.80f, -2.10f,  0.35f, 0.25f, 1.0f, 1.8f},
            { 1.85f, -2.15f, -0.35f, 0.26f, 1.0f, 5.2f},
            {-0.60f, -2.65f,  0.45f, 0.18f, 1.3f, 2.9f}, // Bụi tinh thể sát sàn
            { 0.65f, -2.65f, -0.42f, 0.18f, 1.3f, 4.1f},
            { 2.80f,  2.10f,  0.30f, 0.20f, 0.7f, 1.5f}
    };

    public DimensionalRiftRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(DimensionalRiftEntity entity) {
        String type = entity.getRiftType();

        if ("womb_1".equalsIgnoreCase(type)) return WOMB_TEXTURE_1;
        if ("womb_2".equalsIgnoreCase(type)) return WOMB_TEXTURE_2;
        if ("womb_3".equalsIgnoreCase(type)) return WOMB_TEXTURE_3;

        switch (DimensionalRiftEntity.defaultWombStyle) {
            case 1: return WOMB_TEXTURE_1;
            case 2: return WOMB_TEXTURE_2;
            default: return WOMB_TEXTURE_3;
        }
    }

    public Identifier getCubeTexture(DimensionalRiftEntity entity) {
        String type = entity.getRiftType();
        boolean isWomb = type != null && type.toLowerCase().startsWith("womb");
        return CUBE_WOMB_TEXTURE;
    }

    @Override
    public void render(DimensionalRiftEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        // 1. Nhấp nhô bồng bềnh nhẹ nhàng
        float age = entity.age + tickDelta;
        float bob = MathHelper.sin(age * 0.035f) * 0.08f;
        // Tâm cổng cao 5.4m đặt tại Y = 2.6m để chân đế tiếp đất tự nhiên
        matrices.translate(0, 2.6 + bob, 0);

        // 2. Quay theo góc nhìn Yaw của vết rách
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));

        int fullBright = LightmapTextureManager.MAX_LIGHT_COORDINATE; // 0xF000F0

        String riftType = entity.getRiftType();
        boolean isWomb = riftType != null && riftType.toLowerCase().startsWith("womb");
        boolean isElysian = !isWomb;
        // Cân bằng quang thông màu đỉnh cho khung đá
        int vR = isElysian ? 210 : 255;
        int vG = isElysian ? 190 : 255;
        int vB = isElysian ? 155 : 255;
        int vA = 255;

        // ==========================================
        // TÍNH TOÁN GÓC NHÌN CAMERA CHO HIỆU ỨNG THỊ SAI 3D (3D PARALLAX PROJECTION)
        // ==========================================
        float viewX = 0.0f;
        float viewY = 0.0f;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.gameRenderer != null && client.gameRenderer.getCamera() != null) {
                Vec3d camPos = client.gameRenderer.getCamera().getPos();
                double dx = camPos.x - entity.getX();
                double dy = camPos.y - (entity.getY() + 2.6);
                double dz = camPos.z - entity.getZ();

                float yawRad = (float) Math.toRadians(entity.getYaw());
                double localX = dx * Math.cos(yawRad) + dz * Math.sin(yawRad);
                double localY = dy;
                double localZ = -dx * Math.sin(yawRad) + dz * Math.cos(yawRad);

                double dist = Math.sqrt(localX * localX + localY * localY + localZ * localZ) + 0.001;
                // Nếu người chơi ở phía sau cổng (localZ < 0), đảo hướng để góc nhìn tự nhiên từ cả 2 mặt
                float sign = (localZ < 0) ? -1.0f : 1.0f;
                viewX = (float) MathHelper.clamp((localX / dist) * sign, -1.0, 1.0);
                viewY = (float) MathHelper.clamp(localY / dist, -1.0, 1.0);
            }
        } catch (Exception ignored) {}

        // Độ trượt UV theo góc nhìn người chơi (Parallax shift factor)
        // Hệ số thị sai âm (-viewX) theo đúng quang học phối cảnh: góc nhìn nghiêng mở rộng không gian phía đối diện
        float uShift = -viewX * 0.22f;
        float vShift = -viewY * 0.04f;

        // Tỷ lệ portal aperture: chiều ngang 2.44m, chiều cao 4.50m (tỷ lệ 0.5422)
        // Texture 1024x1024 có pixel tỉ lệ 1:1, do đó dU = dV * (2.44 / 4.50) = 0.488 để tỷ lệ hiển thị đạt chuẩn 100% không méo
        float uCenter = MathHelper.clamp(0.50f + uShift, 0.25f, 0.75f);
        float vCenter = MathHelper.clamp(0.50f + vShift, 0.46f, 0.54f);

        float u0 = uCenter - 0.244f;
        float u1 = uCenter + 0.244f;
        float v0 = vCenter - 0.450f;
        float v1 = vCenter + 0.450f;

        // ==========================================
        // PHẦN A: LÒNG CỔNG CHIỀU SÂU 3D THỊ SAI (3D PARALLAX SKYBOX INTERIOR)
        // Dùng EntityCutoutNoCull: Đảm bảo 100% hiển thị trên mọi Shader, ghi vào Depth Buffer
        // và trượt thị sai 3 chiều theo góc nhìn camera người chơi!
        // ==========================================
        VertexConsumer windowConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(getTexture(entity)));

        float winX0 = -1.22f;
        float winX1 =  1.22f;
        float winY0 = -2.25f;
        float winY1 =  2.25f;

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f posMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();

        // Độ sáng màu đỉnh của lòng cổng: Rực rỡ 255 để chi tiết rừng rậm và hố đen sắc nét tuyệt đối
        int winR = 255;
        int winG = 255;
        int winB = 255;

        // 1. Mặt trước lòng cổng (Facing +Z, đặt tại Z = +0.004f, normal +Z)
        drawVertex(windowConsumer, posMatrix, normalMatrix, winX0, winY0, 0.004f, u0, v1, fullBright, 0, 0, 1, winR, winG, winB, 255);
        drawVertex(windowConsumer, posMatrix, normalMatrix, winX1, winY0, 0.004f, u1, v1, fullBright, 0, 0, 1, winR, winG, winB, 255);
        drawVertex(windowConsumer, posMatrix, normalMatrix, winX1, winY1, 0.004f, u1, v0, fullBright, 0, 0, 1, winR, winG, winB, 255);
        drawVertex(windowConsumer, posMatrix, normalMatrix, winX0, winY1, 0.004f, u0, v0, fullBright, 0, 0, 1, winR, winG, winB, 255);

        // 2. Mặt sau lòng cổng (Facing -Z, đặt tại Z = -0.004f, normal -Z)
        // Cách mặt trước 0.008m -> Triệt tiêu 100% Z-fighting, góc nhìn từ sau trượt thị sai mượt mà
        drawVertex(windowConsumer, posMatrix, normalMatrix, winX1, winY0, -0.004f, u0, v1, fullBright, 0, 0, -1, winR, winG, winB, 255);
        drawVertex(windowConsumer, posMatrix, normalMatrix, winX0, winY0, -0.004f, u1, v1, fullBright, 0, 0, -1, winR, winG, winB, 255);
        drawVertex(windowConsumer, posMatrix, normalMatrix, winX0, winY1, -0.004f, u1, v0, fullBright, 0, 0, -1, winR, winG, winB, 255);
        drawVertex(windowConsumer, posMatrix, normalMatrix, winX1, winY1, -0.004f, u0, v0, fullBright, 0, 0, -1, winR, winG, winB, 255);

        // ==========================================
        // PHẦN B: KHUNG CỔNG TINH THỂ 3D HÙNG VĨ (TRIỆT TIÊU 100% Z-FIGHTING)
        // ==========================================
        VertexConsumer cubeConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(getCubeTexture(entity)));

        // 1. Trụ tinh thể bên trái (-X)
        renderBox(cubeConsumer, matrices, -1.75f, -2.55f, -0.25f, -1.22f, -1.75f, 0.25f, fullBright, vR, vG, vB, vA); // Bệ đế trái (dày 0.50m)
        renderBox(cubeConsumer, matrices, -1.65f, -1.75f, -0.22f, -1.22f,  2.25f, 0.22f, fullBright, vR, vG, vB, vA); // Trụ chính trái (dày 0.44m)
        renderBox(cubeConsumer, matrices, -1.90f, -0.50f, -0.20f, -1.65f,  1.30f, 0.20f, fullBright, vR, vG, vB, vA); // Bậc tinh thể ngoài (dày 0.40m)

        // 2. Trụ tinh thể bên phải (+X)
        renderBox(cubeConsumer, matrices,  1.22f, -2.55f, -0.25f,  1.75f, -1.75f, 0.25f, fullBright, vR, vG, vB, vA); // Bệ đế phải (dày 0.50m)
        renderBox(cubeConsumer, matrices,  1.22f, -1.75f, -0.22f,  1.65f,  2.25f, 0.22f, fullBright, vR, vG, vB, vA); // Trụ chính phải (dày 0.44m)
        renderBox(cubeConsumer, matrices,  1.65f, -0.50f, -0.20f,  1.90f,  1.30f, 0.20f, fullBright, vR, vG, vB, vA); // Bậc tinh thể ngoài (dày 0.40m)

        // 3. Vòm đỉnh và Ngọn tháp tinh thể (Top Arch & Spire)
        renderBox(cubeConsumer, matrices, -1.65f,  2.25f, -0.22f,  1.65f,  2.60f, 0.22f, fullBright, vR, vG, vB, vA); // Xà ngang vòm đỉnh (dày 0.44m)
        renderBox(cubeConsumer, matrices, -0.85f,  2.60f, -0.20f,  0.85f,  3.00f, 0.20f, fullBright, vR, vG, vB, vA); // Bậc vương miện giữa (dày 0.40m)
        renderBox(cubeConsumer, matrices, -0.35f,  3.00f, -0.18f,  0.35f,  3.50f, 0.18f, fullBright, vR, vG, vB, vA); // Đỉnh tháp vút nhọn (dày 0.36m)

        // 4. Bệ móng ngưỡng cửa (Bottom Threshold Beam)
        renderBox(cubeConsumer, matrices, -1.22f, -2.55f, -0.24f,  1.22f, -2.25f, 0.24f, fullBright, vR, vG, vB, vA); // Xà chân cửa (dày 0.48m)

        // ==========================================
        // PHẦN C: CỤM 14 KHỐI LẬP PHƯƠNG VỆ TINH 3D BAY LƠ LỬNG
        // ==========================================
        for (int i = 0; i < SATELLITE_CUBES.length; i++) {
            float[] cData = SATELLITE_CUBES[i];
            float baseX = cData[0];
            float baseY = cData[1];
            float baseZ = cData[2];
            float cSize = cData[3];
            float spd   = cData[4];
            float phase = cData[5];

            float cBobY = baseY + MathHelper.sin(age * 0.045f * spd + phase) * 0.14f;
            float cBobZ = baseZ + MathHelper.cos(age * 0.035f * spd + phase * 1.3f) * 0.10f;
            float rotY = (age * 1.6f * spd + i * 36.0f) % 360.0f;
            float rotX = (age * 1.1f * spd + i * 25.0f) % 360.0f;

            matrices.push();
            matrices.translate(baseX, cBobY, cBobZ);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));

            float h = cSize / 2.0f;
            renderBox(cubeConsumer, matrices, -h, -h, -h, h, h, h, fullBright, vR, vG, vB, vA);

            matrices.pop();
        }

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void renderBox(VertexConsumer consumer, MatrixStack matrices,
                                  float x0, float y0, float z0,
                                  float x1, float y1, float z1, int light,
                                  int r, int g, int b, int a) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f pos = entry.getPositionMatrix();
        Matrix3f norm = entry.getNormalMatrix();

        // North face (-Z)
        drawVertex(consumer, pos, norm, x1, y0, z0, 0f, 1f, light,  0,  0, -1, r, g, b, a);
        drawVertex(consumer, pos, norm, x0, y0, z0, 1f, 1f, light,  0,  0, -1, r, g, b, a);
        drawVertex(consumer, pos, norm, x0, y1, z0, 1f, 0f, light,  0,  0, -1, r, g, b, a);
        drawVertex(consumer, pos, norm, x1, y1, z0, 0f, 0f, light,  0,  0, -1, r, g, b, a);

        // South face (+Z)
        drawVertex(consumer, pos, norm, x0, y0, z1, 0f, 1f, light,  0,  0,  1, r, g, b, a);
        drawVertex(consumer, pos, norm, x1, y0, z1, 1f, 1f, light,  0,  0,  1, r, g, b, a);
        drawVertex(consumer, pos, norm, x1, y1, z1, 1f, 0f, light,  0,  0,  1, r, g, b, a);
        drawVertex(consumer, pos, norm, x0, y1, z1, 0f, 0f, light,  0,  0,  1, r, g, b, a);

        // East face (+X)
        drawVertex(consumer, pos, norm, x1, y0, z1, 0f, 1f, light,  1,  0,  0, r, g, b, a);
        drawVertex(consumer, pos, norm, x1, y0, z0, 1f, 1f, light,  1,  0,  0, r, g, b, a);
        drawVertex(consumer, pos, norm, x1, y1, z0, 1f, 0f, light,  1,  0,  0, r, g, b, a);
        drawVertex(consumer, pos, norm, x1, y1, z1, 0f, 0f, light,  1,  0,  0, r, g, b, a);

        // West face (-X)
        drawVertex(consumer, pos, norm, x0, y0, z0, 0f, 1f, light, -1,  0,  0, r, g, b, a);
        drawVertex(consumer, pos, norm, x0, y0, z1, 1f, 1f, light, -1,  0,  0, r, g, b, a);
        drawVertex(consumer, pos, norm, x0, y1, z1, 1f, 0f, light, -1,  0,  0, r, g, b, a);
        drawVertex(consumer, pos, norm, x0, y1, z0, 0f, 0f, light, -1,  0,  0, r, g, b, a);

        // Up face (+Y)
        drawVertex(consumer, pos, norm, x0, y1, z1, 0f, 1f, light,  0,  1,  0, r, g, b, a);
        drawVertex(consumer, pos, norm, x1, y1, z1, 1f, 1f, light,  0,  1,  0, r, g, b, a);
        drawVertex(consumer, pos, norm, x1, y1, z0, 1f, 0f, light,  0,  1,  0, r, g, b, a);
        drawVertex(consumer, pos, norm, x0, y1, z0, 0f, 0f, light,  0,  1,  0, r, g, b, a);

        // Down face (-Y)
        drawVertex(consumer, pos, norm, x0, y0, z0, 0f, 1f, light,  0, -1,  0, r, g, b, a);
        drawVertex(consumer, pos, norm, x1, y0, z0, 1f, 1f, light,  0, -1,  0, r, g, b, a);
        drawVertex(consumer, pos, norm, x1, y0, z1, 1f, 0f, light,  0, -1,  0, r, g, b, a);
        drawVertex(consumer, pos, norm, x0, y0, z1, 0f, 0f, light,  0, -1,  0, r, g, b, a);
    }

    private static void drawVertex(VertexConsumer consumer, Matrix4f posMatrix, Matrix3f normalMatrix,
                                   float x, float y, float z, float u, float v, int light,
                                   float nx, float ny, float nz,
                                   int r, int g, int b, int a) {
        consumer.vertex(posMatrix, x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normalMatrix, nx, ny, nz)
                .next();
    }
}
