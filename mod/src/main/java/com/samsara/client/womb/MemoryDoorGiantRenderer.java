package com.samsara.client.womb;

import com.samsara.block.MemoryDoorGiantBlock;
import com.samsara.block.entity.MemoryDoorGiantBlockEntity;
import com.samsara.registry.SamsaraBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
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
 * Cánh cửa khổng lồ (cánh cửa trắng ở tường căn phòng cửa, các cánh cửa trên khổng thần): cùng kiểu với Cửa Ký Ức
 * (texture memory_door_<màu>_*), phóng to
 * {@link #SCALE} lần. Cánh cửa hé 70° ra xa người nhìn (đủ khe cho thuyền đi giữa tuyến), đứng yên.
 * Sau đầu cánh cửa là một màn sáng tĩnh gần như đục (che vũ trụ bên ngoài căn phòng). Không dùng lớp cộng sáng /
 * nhịp thở: các lớp sáng chồng nhau gây chớp. Vệt sáng trên mặt nước là đèn biển dưới đáy (SaveDoorChapter).
 * Toạ độ dưới đây tính theo cánh cửa 1 x 2 block trước khi phóng to; gốc ở chân cửa, giữa lòng cửa.
 */
public class MemoryDoorGiantRenderer implements BlockEntityRenderer<MemoryDoorGiantBlockEntity> {
    public static final float SCALE = 6.0f;

    private static final Identifier KNOB = new Identifier("samsara", "textures/block/memory_door_knob.png");
    private static final Identifier CURTAIN = new Identifier("samsara", "textures/block/memory_door_curtain.png");

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float P = 1.0f / 16.0f;

    /** Ánh sáng dùng cho lần vẽ hiện tại (vẽ trên render thread, lần lượt từng cánh cửa). */
    private static int currentLight = FULL_BRIGHT;

    private static Identifier tex(MemoryDoorGiantBlock.Color color, String part) {
        return new Identifier("samsara", "textures/block/memory_door_" + color.asString() + "_" + part + ".png");
    }

    @Override
    public void render(MemoryDoorGiantBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider buffers, int light, int overlay) {
        BlockState state = be.getCachedState();
        if (!state.contains(MemoryDoorGiantBlock.FACING)) return;
        Direction facing = state.get(MemoryDoorGiantBlock.FACING);
        MemoryDoorGiantBlock.Color color = state.get(MemoryDoorGiantBlock.COLOR);
        boolean lit = state.get(MemoryDoorGiantBlock.LIT);
        boolean flat = state.get(MemoryDoorGiantBlock.FLAT);
        // Cánh cửa nằm trong tường Vách Hư Không của căn phòng: tự phát sáng và có màn sáng che vũ trụ.
        // Cửa ngoài trời (trên khổng thần): nhận ánh sáng thật của thế giới, không màn sáng.
        boolean inWall = inRoomWall(be, facing);
        boolean curtainOn = lit && inWall && state.get(MemoryDoorGiantBlock.CURTAIN);
        currentLight = inWall || be.getWorld() == null ? FULL_BRIGHT
                : WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().up(flat ? 1 : 6));

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(facing.asRotation()));
        matrices.scale(SCALE, SCALE, SCALE);

        if (flat) {
            // Nằm ngửa, lệch một góc, không khung: cánh cửa bị thổi bay khỏi khung
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(17.0f));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(90.0f));
            matrices.translate(-0.5f, -1.0f, 0.0f);
            leaf(matrices, buffers, color);
            matrices.pop();
            return;
        }

        // Khung: hai thanh đứng + dầm trên (sâu 4 px, thò 2 px ngoài lòng cửa)
        VertexConsumer frame = buffers.getBuffer(RenderLayer.getEntitySolid(tex(color, "frame")));
        box(matrices, frame, -0.5f - 2 * P, 0, -2 * P, -0.5f, 2, 2 * P);
        box(matrices, frame, 0.5f, 0, -2 * P, 0.5f + 2 * P, 2, 2 * P);
        box(matrices, frame, -0.5f - 2 * P, 2, -2 * P, 0.5f + 2 * P, 2 + 2 * P, 2 * P);

        // Cánh cửa: bản lề bên trái (nhìn từ mặt trước), mặt sau lòng cửa; cửa sáng thì hé 70° ra xa người nhìn
        matrices.push();
        matrices.translate(-0.5f, 0.0f, -P);
        if (lit) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0f));
        leaf(matrices, buffers, color);
        matrices.pop();

        if (curtainOn) {
            // Màn sáng gần như đục sau đầu cánh cửa: nhìn qua khe chỉ thấy ánh sáng
            VertexConsumer curtain = buffers.getBuffer(RenderLayer.getEntityTranslucentEmissive(CURTAIN));
            quadCurtain(matrices.peek().getPositionMatrix(), curtain, -1.6f, -0.1f, 1.6f, 2.6f, -1.1f);
        }

        matrices.pop();
    }

    /** Hai bên lòng cửa (cách tâm 5 block theo bề ngang cửa) có Vách Hư Không không. */
    private static boolean inRoomWall(MemoryDoorGiantBlockEntity be, Direction facing) {
        if (be.getWorld() == null) return false;
        Direction across = facing.rotateYClockwise();
        BlockPos p = be.getPos().up(2);
        return be.getWorld().getBlockState(p.offset(across, 5)).isOf(SamsaraBlocks.WOMB_VEIL)
                || be.getWorld().getBlockState(p.offset(across, -5)).isOf(SamsaraBlocks.WOMB_VEIL);
    }

    /** Cánh cửa 1 x 2 (gốc ở góc dưới-trái-sau), dày 2 px, tay nắm hai mặt. */
    private static void leaf(MatrixStack matrices, VertexConsumerProvider buffers, MemoryDoorGiantBlock.Color color) {
        box(matrices, buffers.getBuffer(RenderLayer.getEntitySolid(tex(color, "bottom"))), 0, 0, 0, 1, 1, 2 * P);
        box(matrices, buffers.getBuffer(RenderLayer.getEntitySolid(tex(color, "top"))), 0, 1, 0, 1, 2, 2 * P);
        VertexConsumer knob = buffers.getBuffer(RenderLayer.getEntitySolid(KNOB));
        box(matrices, knob, 12.5f * P, 13 * P, 2 * P, 13.5f * P, 14 * P, 3 * P);
        box(matrices, knob, 12.5f * P, 13 * P, -P, 13.5f * P, 14 * P, 0);
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
                .light(currentLight).normal(n, nx, ny, nz).next();
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
