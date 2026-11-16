package com.samsara.client.overworld;

import com.samsara.system.overworld.VersionGlitch;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Phía client của khối lỗi phiên bản ({@link VersionGlitch}): vẽ một khối texture Alpha phủ khít lên khối thật trong
 * vài chục tick. Mấy tick đầu chập chờn bật / tắt, suốt thời gian đó rung giật vài phần trăm khối, lâu lâu tách màu
 * (một bóng đỏ lệch một bên, một bóng xanh lệch bên kia, như đầu từ đọc lệch). Người chơi đi lại gần (dưới 6 khối) thì
 * tắt ngay. Vẽ bằng lớp entity nên chạy cả khi có lẫn không có shaderpack.
 */
public final class VersionGlitchRenderer {
    private VersionGlitchRenderer() {}

    private static final Random RANDOM = Random.create();

    private static BlockPos pos;
    private static int ticksLeft;
    private static int total;
    private static Identifier side;
    private static Identifier top;
    private static boolean wasFar;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(VersionGlitch.PACKET, (client, handler, buf, sender) -> {
            BlockPos p = buf.readBlockPos();
            int ticks = buf.readVarInt();
            Block block = Registries.BLOCK.get(buf.readIdentifier());
            client.execute(() -> start(client, p, ticks, block));
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ticksLeft <= 0) return;
            ticksLeft--;
            if (client.player == null || client.world == null) {
                ticksLeft = 0;
                return;
            }
            // Đi lại gần thì thôi (chỉ khi lúc bắt đầu nó ở xa: lệnh test chĩa vào khối sát bên vẫn xem được)
            if (wasFar && client.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) < 36) ticksLeft = 0;
            // Khối thật đã đổi (bị phá...) thì thôi
            if (!VersionGlitch.TEXTURES.containsKey(client.world.getBlockState(pos).getBlock())) ticksLeft = 0;
        });
        WorldRenderEvents.AFTER_ENTITIES.register(VersionGlitchRenderer::render);
    }

    private static void start(MinecraftClient client, BlockPos p, int ticks, Block block) {
        String[] tex = VersionGlitch.TEXTURES.get(block);
        if (tex == null || client.player == null) return;
        pos = p;
        ticksLeft = ticks;
        total = ticks;
        side = new Identifier("samsara", "textures/overworld/version_glitch/" + tex[0] + ".png");
        top = new Identifier("samsara", "textures/overworld/version_glitch/" + tex[1] + ".png");
        wasFar = client.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(p)) >= 64;
    }

    private static void render(WorldRenderContext context) {
        if (ticksLeft <= 0 || pos == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        int age = total - ticksLeft;
        // Mấy tick đầu và cuối chập chờn như tín hiệu chưa bắt được
        if ((age < 6 || ticksLeft < 4) && RANDOM.nextFloat() < 0.45f) return;

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;
        MatrixStack matrices = context.matrixStack();
        Vec3d cam = context.camera().getPos();
        int light = WorldRenderer.getLightmapCoordinates(client.world, pos.up());

        float shake = 0.015f + (RANDOM.nextFloat() < 0.2f ? 0.05f : 0.0f);
        matrices.push();
        matrices.translate(pos.getX() - cam.x + (RANDOM.nextFloat() - 0.5f) * shake,
                pos.getY() - cam.y + (RANDOM.nextFloat() - 0.5f) * shake * 0.5f,
                pos.getZ() - cam.z + (RANDOM.nextFloat() - 0.5f) * shake);
        cube(matrices, consumers, RenderLayer.getEntityCutoutNoCull(side), RenderLayer.getEntityCutoutNoCull(top),
                light, 1f, 1f, 1f, 1f, 0.004f);

        // Tách màu: đỏ lệch một bên, xanh lệch bên kia, theo trục ngang của camera
        if (RANDOM.nextFloat() < 0.6f) {
            float yaw = (float) Math.toRadians(context.camera().getYaw());
            float off = 0.03f + RANDOM.nextFloat() * 0.05f;
            float rx = (float) Math.cos(yaw) * off, rz = (float) Math.sin(yaw) * off;
            matrices.push();
            matrices.translate(rx, 0, rz);
            cube(matrices, consumers, RenderLayer.getEntityTranslucent(side), RenderLayer.getEntityTranslucent(top),
                    light, 1f, 0.2f, 0.25f, 0.45f, 0.012f);
            matrices.translate(-2 * rx, 0, -2 * rz);
            cube(matrices, consumers, RenderLayer.getEntityTranslucent(side), RenderLayer.getEntityTranslucent(top),
                    light, 0.2f, 0.85f, 1f, 0.45f, 0.012f);
            matrices.pop();
        }
        matrices.pop();
    }

    /** Khối lập phương nở ra {@code grow} mỗi phía; mặt trên/dưới dùng texture {@code topLayer}. */
    private static void cube(MatrixStack matrices, VertexConsumerProvider consumers, RenderLayer sideLayer, RenderLayer topLayer,
                             int light, float r, float g, float b, float a, float grow) {
        float lo = -grow, hi = 1 + grow;
        Matrix4f m = matrices.peek().getPositionMatrix();
        Matrix3f n = matrices.peek().getNormalMatrix();
        VertexConsumer vs = consumers.getBuffer(sideLayer);
        face(vs, m, n, Direction.NORTH, lo, hi, light, r, g, b, a);
        face(vs, m, n, Direction.SOUTH, lo, hi, light, r, g, b, a);
        face(vs, m, n, Direction.WEST, lo, hi, light, r, g, b, a);
        face(vs, m, n, Direction.EAST, lo, hi, light, r, g, b, a);
        VertexConsumer vt = consumers.getBuffer(topLayer);
        face(vt, m, n, Direction.UP, lo, hi, light, r, g, b, a);
        face(vt, m, n, Direction.DOWN, lo, hi, light, r, g, b, a);
    }

    private static void face(VertexConsumer vc, Matrix4f m, Matrix3f n, Direction d, float lo, float hi, int light,
                             float r, float g, float b, float a) {
        float[][] q = switch (d) {
            case NORTH -> new float[][]{{hi, hi, lo}, {hi, lo, lo}, {lo, lo, lo}, {lo, hi, lo}};
            case SOUTH -> new float[][]{{lo, hi, hi}, {lo, lo, hi}, {hi, lo, hi}, {hi, hi, hi}};
            case WEST -> new float[][]{{lo, hi, lo}, {lo, lo, lo}, {lo, lo, hi}, {lo, hi, hi}};
            case EAST -> new float[][]{{hi, hi, hi}, {hi, lo, hi}, {hi, lo, lo}, {hi, hi, lo}};
            case UP -> new float[][]{{lo, hi, lo}, {lo, hi, hi}, {hi, hi, hi}, {hi, hi, lo}};
            case DOWN -> new float[][]{{lo, lo, hi}, {lo, lo, lo}, {hi, lo, lo}, {hi, lo, hi}};
        };
        float[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        for (int i = 0; i < 4; i++) {
            vc.vertex(m, q[i][0], q[i][1], q[i][2]).color(r, g, b, a).texture(uv[i][0], uv[i][1])
                    .overlay(OverlayTexture.DEFAULT_UV).light(light)
                    .normal(n, d.getOffsetX(), d.getOffsetY(), d.getOffsetZ()).next();
        }
    }
}
