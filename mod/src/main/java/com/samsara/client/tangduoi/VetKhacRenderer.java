package com.samsara.client.tangduoi;

import com.samsara.block.BanhKhoaBlock;
import com.samsara.client.CamcorderClientManager;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.system.tangduoi.DuongOngBuilder;
import com.samsara.system.tangduoi.OKhoaHam;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Vết khắc trên tường căn hầm Tầng Dưới: chỉ hiện khi ngắm qua máy quay (hoặc mờ mờ với mắt thường sau khi đã sai 8 lần).
 * Bốn góc cho bốn chữ số của mã, mũi tên khắc dưới ổ khoá chỉ thứ tự đọc.
 */
@Environment(EnvType.CLIENT)
public final class VetKhacRenderer {
    private VetKhacRenderer() {}

    private record Decal(String name, double x, double y, double z, double w, double h, boolean north) {}

    private static final double NORTH_FACE = 42.01;
    private static final double SOUTH_FACE = 49.99;
    private static final Decal[] DECALS = {
            new Decal("khac_0", -5.0, 2.0, NORTH_FACE, 1.6, 1.6, true),
            new Decal("khac_5", 3.4, 2.5, NORTH_FACE, 1.6, 1.6, true),
            new Decal("khac_1", 3.4, 2.0, SOUTH_FACE, 1.6, 1.6, false),
            new Decal("khac_8", -5.0, 2.0, SOUTH_FACE, 1.6, 1.6, false),
            new Decal("khac_mui_ten", -2.0, 1.0, NORTH_FACE, 4.0, 1.0, true),
    };

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(VetKhacRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || ctx.consumers() == null) return;
        if (client.world.getRegistryKey() != SamsaraDimensions.TANG_DUOI_WORLD_KEY) return;
        BlockState dial = client.world.getBlockState(OKhoaHam.dial(0));
        if (!(dial.getBlock() instanceof BanhKhoaBlock)) return;
        Vec3d o = Vec3d.of(DuongOngBuilder.ORIGIN);
        if (client.player.squaredDistanceTo(o.x, o.y + 2, o.z + 46) > 400.0) return;

        float aim = CamcorderClientManager.getAimProgress();
        float alpha = aim > 0.5f ? 0.95f : dial.get(BanhKhoaBlock.HINT) ? 0.4f : 0.0f;
        if (alpha <= 0.0f) return;

        Vec3d cam = ctx.camera().getPos();
        MatrixStack m = ctx.matrixStack();
        m.push();
        m.translate(o.x - cam.x, o.y - cam.y, o.z - cam.z);
        Matrix4f pos = m.peek().getPositionMatrix();
        Matrix3f normal = m.peek().getNormalMatrix();
        for (Decal d : DECALS) {
            VertexConsumer c = ctx.consumers().getBuffer(RenderLayer.getEntityTranslucent(new Identifier("samsara", "textures/misc/" + d.name + ".png")));
            float x0 = (float) d.x;
            float x1 = (float) (d.x + d.w);
            float y0 = (float) d.y;
            float y1 = (float) (d.y + d.h);
            float z = (float) d.z;
            // Nhìn từ trong hầm: tường bắc x tăng về phải, tường nam x tăng về trái
            float uL = d.north ? 0f : 1f;
            float uR = d.north ? 1f : 0f;
            vertex(c, pos, normal, x0, y1, z, uL, 0f, alpha);
            vertex(c, pos, normal, x0, y0, z, uL, 1f, alpha);
            vertex(c, pos, normal, x1, y0, z, uR, 1f, alpha);
            vertex(c, pos, normal, x1, y1, z, uR, 0f, alpha);
        }
        m.pop();
    }

    private static void vertex(VertexConsumer c, Matrix4f pos, Matrix3f normal, float x, float y, float z, float u, float v, float a) {
        c.vertex(pos, x, y, z).color(1f, 1f, 1f, a).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0)
                .normal(normal, 0f, 0f, 1f).next();
    }
}
