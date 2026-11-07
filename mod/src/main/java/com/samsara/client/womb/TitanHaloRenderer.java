package com.samsara.client.womb;

import com.samsara.block.TitanHaloBlock;
import com.samsara.block.entity.TitanHaloBlockEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * Vầng hào quang của khổng thần (chương III): một quad nằm ngang, texture vòng sáng trắng ngà, tâm ở block; đứng
 * yên, một lớp. (Bản trước có thêm một tia sáng billboard rọi từ Gargantua, đã bỏ vì trông như tấm kính giả; ánh sáng
 * lên thân tượng giờ là block ánh sáng thật, xem TitanChapter.bodyLight.)
 */
public class TitanHaloRenderer implements BlockEntityRenderer<TitanHaloBlockEntity> {
    private static final Identifier HALO = new Identifier("samsara", "textures/block/titan_halo.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    @Override
    public void render(TitanHaloBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider buffers, int light, int overlay) {
        int radius = be.getCachedState().contains(TitanHaloBlock.RADIUS) ? be.getCachedState().get(TitanHaloBlock.RADIUS) : 6;
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        Matrix4f m = matrices.peek().getPositionMatrix();

        // Vầng hào quang (lớp này không cull nên thấy được cả từ dưới lên)
        VertexConsumer halo = buffers.getBuffer(RenderLayer.getEntityTranslucentEmissive(HALO));
        vertex(halo, m, -radius, 0, -radius, 0, 0);
        vertex(halo, m, -radius, 0, radius, 0, 1);
        vertex(halo, m, radius, 0, radius, 1, 1);
        vertex(halo, m, radius, 0, -radius, 1, 0);

        matrices.pop();
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, double x, double y, double z, float u, float v) {
        vc.vertex(m, (float) x, (float) y, (float) z).color(255, 255, 255, 255).texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV).light(FULL_BRIGHT).normal(0, 1, 0).next();
    }

    @Override
    public boolean rendersOutsideBoundingBox(TitanHaloBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 256;
    }
}
