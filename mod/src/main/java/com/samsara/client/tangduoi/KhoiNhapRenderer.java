package com.samsara.client.tangduoi;

import com.samsara.block.entity.KhoiNhapBlockEntity;
import com.samsara.client.CamcorderClientManager;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.system.tangduoi.LoiDiNhapBuilder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.joml.Matrix4f;

/**
 * Khối Nháp chỉ hiện khi đã ngắm hẳn máy quay: viền ô đen như viền chọn block, bên trong rỗng nên thấy nước ngay dưới chân.
 * Vẽ bằng các thanh hình học, không dựa vào độ trong suốt của texture (shader các chiều xử lý alpha của entity khác nhau).
 */
public class KhoiNhapRenderer implements BlockEntityRenderer<KhoiNhapBlockEntity> {
    private static final Identifier TEXTURE = new Identifier("samsara", "textures/block/khoi_nhap.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    @Override
    public void render(KhoiNhapBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider buffers, int light, int overlay) {
        if (be.getWorld() == null) return;
        boolean aimed = CamcorderClientManager.isFullyAimed();
        // Ván giả: có trong mắt, không có trong máy quay
        if (be.getCachedState().isOf(SamsaraBlocks.VAN_GIA)) {
            if (aimed) return;
            net.minecraft.client.MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                    net.minecraft.block.Blocks.OAK_PLANKS.getDefaultState(), matrices, buffers, light, overlay);
            return;
        }
        if (!aimed || LoiDiNhapBuilder.isStaticZone(be.getPos().getZ() + 0.5)) return;
        BlockPos pos = be.getPos();
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = buffers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        if (!be.getCachedState().get(com.samsara.block.KhoiNhapBlock.WATERLOGGED)) {
            drawBed(vc, m, be, pos);
            return;
        }
        float t = 1.003f;
        float w = 0.05f;
        // Bốn cạnh mặt trên
        quad(vc, m, 0, t, 0, 0, t, 1, w, t, 1, w, t, 0);
        quad(vc, m, 1 - w, t, 0, 1 - w, t, 1, 1, t, 1, 1, t, 0);
        quad(vc, m, 0, t, 0, 0, t, w, 1, t, w, 1, t, 0);
        quad(vc, m, 0, t, 1 - w, 0, t, 1, 1, t, 1, 1, t, 1 - w);
        // Mép lộ ra ở rìa lối đi, để nhìn ngang vẫn thấy
        float b = 0.86f;
        if (open(be, pos, Direction.NORTH)) quad(vc, m, 1, 1, 0, 1, b, 0, 0, b, 0, 0, 1, 0);
        if (open(be, pos, Direction.SOUTH)) quad(vc, m, 0, 1, 1, 0, b, 1, 1, b, 1, 1, 1, 1);
        if (open(be, pos, Direction.WEST)) quad(vc, m, 0, 1, 0, 0, b, 0, 0, b, 1, 0, 1, 1);
        if (open(be, pos, Direction.EAST)) quad(vc, m, 1, 1, 1, 1, b, 1, 1, b, 0, 1, 1, 0);
    }

    /** Khung dây của giường: toàn nét mảnh (mảng đặc bị shader làm thành khối sáng). Ô phía bắc là đầu giường, có gối và thành giường. */
    private static void drawBed(VertexConsumer vc, Matrix4f m, KhoiNhapBlockEntity be, BlockPos pos) {
        float top = 0.5625f;
        float low = 0.08f;
        float w = 0.05f;
        boolean head = open(be, pos, Direction.NORTH);
        boolean foot = open(be, pos, Direction.SOUTH);
        for (float y : new float[]{top, low}) {
            quad(vc, m, 0, y, 0, 0, y, 1, w, y, 1, w, y, 0);
            quad(vc, m, 1 - w, y, 0, 1 - w, y, 1, 1, y, 1, 1, y, 0);
            if (head) quad(vc, m, 0, y, 0, 0, y, w, 1, y, w, 1, y, 0);
            if (foot) quad(vc, m, 0, y, 1 - w, 0, y, 1, 1, y, 1, 1, y, 1 - w);
        }
        if (head) {
            // Gối
            quad(vc, m, 0.08f, top, 0.45f, 0.08f, top, 0.45f + w, 0.92f, top, 0.45f + w, 0.92f, top, 0.45f);
            // Thành đầu giường: hai cột cao và một thanh ngang
            post(vc, m, w / 2, w / 2, low, 0.95f, w);
            post(vc, m, 1 - w / 2, w / 2, low, 0.95f, w);
            quad(vc, m, 0, 0.95f, 0, 0, 0.95f - w, 0, 1, 0.95f - w, 0, 1, 0.95f, 0);
        }
        if (foot) {
            post(vc, m, w / 2, 1 - w / 2, low, top, w);
            post(vc, m, 1 - w / 2, 1 - w / 2, low, top, w);
        }
    }

    /** Một nét đứng: hai mảnh mỏng bắt chéo, nhìn hướng nào cũng thấy. */
    private static void post(VertexConsumer vc, Matrix4f m, float x, float z, float y0, float y1, float w) {
        float h = w / 2;
        quad(vc, m, x - h, y1, z, x - h, y0, z, x + h, y0, z, x + h, y1, z);
        quad(vc, m, x, y1, z - h, x, y0, z - h, x, y0, z + h, x, y1, z + h);
    }

    private static boolean open(KhoiNhapBlockEntity be, BlockPos pos, Direction d) {
        return !be.getWorld().getBlockState(pos.offset(d)).isOf(SamsaraBlocks.KHOI_NHAP);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        vertex(vc, m, x0, y0, z0);
        vertex(vc, m, x1, y1, z1);
        vertex(vc, m, x2, y2, z2);
        vertex(vc, m, x3, y3, z3);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z) {
        vc.vertex(m, x, y, z).color(255, 255, 255, 255).texture(0.5f, 0.5f)
                .overlay(OverlayTexture.DEFAULT_UV).light(FULL_BRIGHT).normal(0, 1, 0).next();
    }

    @Override
    public int getRenderDistance() {
        return 96;
    }
}
