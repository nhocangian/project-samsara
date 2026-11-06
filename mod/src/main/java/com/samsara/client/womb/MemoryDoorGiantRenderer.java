package com.samsara.client.womb;

import com.samsara.block.MemoryDoorGiantBlock;
import com.samsara.block.entity.MemoryDoorGiantBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Cánh cửa trắng khổng lồ cuối hành lang cửa: cùng kiểu với Cửa Ký Ức (texture memory_door_white_*), phóng to
 * {@link #SCALE} lần. Cánh cửa hé 70° ra xa người nhìn (đủ khe cho thuyền đi giữa tuyến), đứng yên.
 * Sau đầu cánh cửa là một màn sáng tĩnh gần như đục (che vũ trụ bên ngoài căn phòng). Không dùng lớp cộng sáng /
 * nhịp thở: các lớp sáng chồng nhau gây chớp. Vệt sáng trên mặt nước là đèn biển dưới đáy (SaveDoorChapter).
 * Toạ độ dưới đây tính theo cánh cửa 1 x 2 block trước khi phóng to; gốc ở chân cửa, giữa lòng cửa.
 */
public class MemoryDoorGiantRenderer implements BlockEntityRenderer<MemoryDoorGiantBlockEntity> {
    public static final float SCALE = 6.0f;

    private static final Identifier LEAF_BOTTOM = new Identifier("samsara", "textures/block/memory_door_white_bottom.png");
    private static final Identifier LEAF_TOP = new Identifier("samsara", "textures/block/memory_door_white_top.png");
    private static final Identifier FRAME = new Identifier("samsara", "textures/block/memory_door_white_frame.png");
    private static final Identifier KNOB = new Identifier("samsara", "textures/block/memory_door_knob.png");
    private static final Identifier CURTAIN = new Identifier("samsara", "textures/block/memory_door_curtain.png");

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float P = 1.0f / 16.0f;

    @Override
    public void render(MemoryDoorGiantBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider buffers, int light, int overlay) {
        Direction facing = be.getCachedState().contains(MemoryDoorGiantBlock.FACING)
                ? be.getCachedState().get(MemoryDoorGiantBlock.FACING) : Direction.SOUTH;

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(facing.asRotation()));
        matrices.scale(SCALE, SCALE, SCALE);

        // Khung: hai thanh đứng + dầm trên (sâu 4 px, thò 2 px ngoài lòng cửa)
        VertexConsumer frame = buffers.getBuffer(RenderLayer.getEntitySolid(FRAME));
        box(matrices, frame, -0.5f - 2 * P, 0, -2 * P, -0.5f, 2, 2 * P);
        box(matrices, frame, 0.5f, 0, -2 * P, 0.5f + 2 * P, 2, 2 * P);
        box(matrices, frame, -0.5f - 2 * P, 2, -2 * P, 0.5f + 2 * P, 2 + 2 * P, 2 * P);

        // Cánh cửa: bản lề phía tây, mặt sau (bắc) lòng cửa; mở ra xa người nhìn
        float angle = 70.0f;
        matrices.push();
        matrices.translate(-0.5f, 0.0f, -P);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
        box(matrices, buffers.getBuffer(RenderLayer.getEntitySolid(LEAF_BOTTOM)), 0, 0, 0, 1, 1, 2 * P);
        box(matrices, buffers.getBuffer(RenderLayer.getEntitySolid(LEAF_TOP)), 0, 1, 0, 1, 2, 2 * P);
        VertexConsumer knob = buffers.getBuffer(RenderLayer.getEntitySolid(KNOB));
        box(matrices, knob, 12.5f * P, 13 * P, 2 * P, 13.5f * P, 14 * P, 3 * P);
        box(matrices, knob, 12.5f * P, 13 * P, -P, 13.5f * P, 14 * P, 0);
        matrices.pop();

        // Màn sáng gần như đục sau đầu cánh cửa: từ trong phòng nhìn qua khe chỉ thấy ánh sáng, không thấy vũ trụ
        VertexConsumer curtain = buffers.getBuffer(RenderLayer.getEntityTranslucentEmissive(CURTAIN));
        quadCurtain(matrices.peek().getPositionMatrix(), curtain, -1.6f, -0.1f, 1.6f, 2.6f, -1.1f);

        matrices.pop();
    }

    /** Hộp có đủ 6 mặt, UV phủ cả texture mỗi mặt (mặt trước/sau dùng đúng tỉ lệ của texture cánh cửa). */
    private static void box(MatrixStack matrices, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        Matrix3f n = matrices.peek().getNormalMatrix();
        // nam (+z) và bắc (-z)
        face(m, n, vc, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, 0, 1, 1, 0);
        face(m, n, vc, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, 0, 1, 1, 0);
        // đông (+x) và tây (-x): dải mép 2 px
        face(m, n, vc, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, 14 * P, 1, 1, 0);
        face(m, n, vc, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, 0, 1, 2 * P, 0);
        // trên và dưới
        face(m, n, vc, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, 0, 2 * P, 1, 0);
        face(m, n, vc, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, 0, 1, 1, 14 * P);
    }

    /** 4 đỉnh ngược chiều kim đồng hồ khi nhìn từ phía pháp tuyến; (u0,v0) góc dưới-trái, (u1,v1) góc trên-phải. */
    private static void face(Matrix4f m, Matrix3f n, VertexConsumer vc,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, float u0, float v0, float u1, float v1) {
        vertex(m, n, vc, ax, ay, az, u0, v0, nx, ny, nz);
        vertex(m, n, vc, bx, by, bz, u1, v0, nx, ny, nz);
        vertex(m, n, vc, cx, cy, cz, u1, v1, nx, ny, nz);
        vertex(m, n, vc, dx, dy, dz, u0, v1, nx, ny, nz);
    }

    private static void vertex(Matrix4f m, Matrix3f n, VertexConsumer vc, float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        vc.vertex(m, x, y, z).color(255, 255, 255, 255).texture(u, v).overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHT).normal(n, nx, ny, nz).next();
    }

    /**
     * Màn sáng đứng, một quad duy nhất (lớp này không cull nên vẫn thấy từ hai phía; vẽ hai quad trùng mặt phẳng
     * sẽ tranh độ sâu và chớp). Độ đục lấy từ alpha của texture.
     */
    private static void quadCurtain(Matrix4f m, VertexConsumer vc, float x0, float y0, float x1, float y1, float z) {
        curtainVertex(m, vc, x0, y0, z, 0, 1);
        curtainVertex(m, vc, x1, y0, z, 1, 1);
        curtainVertex(m, vc, x1, y1, z, 1, 0);
        curtainVertex(m, vc, x0, y1, z, 0, 0);
    }

    private static void curtainVertex(Matrix4f m, VertexConsumer vc, float x, float y, float z, float u, float v) {
        vc.vertex(m, x, y, z).color(255, 255, 255, 255).texture(u, v).overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHT).normal(0, 0, 1).next();
    }

    @Override
    public boolean rendersOutsideBoundingBox(MemoryDoorGiantBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 256;
    }
}
