package com.samsara.client;

import com.samsara.registry.SamsaraDimensions;
import com.samsara.system.womb.WombCurrentSystem;
import com.samsara.system.womb.WombRoute;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BoatEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/**
 * Phía CLIENT của tuyến hành hương (Chương 1 - Bến Đò):
 *  - Dòng chảy: khi ngồi thuyền trên tuyến, thuyền bị đẩy chậm về phía Quả Trứng và kéo nhẹ về giữa dòng.
 *    (Thuyền do người chơi lái được client điều khiển, nên lực đẩy phải tác động ở đây.)
 *  - Thuyền bóng ma: qua ống kính máy quay, một chiếc thuyền thứ hai chèo song song ngay bên cạnh,
 *    người chèo là chính bạn (cùng skin, cùng nhịp mái chèo, mờ như bóng). Mắt thường không thấy gì.
 */
@Environment(EnvType.CLIENT)
public class WombRouteClient {
    private static final Identifier BOAT_TEXTURE = new Identifier("textures/entity/boat/dark_oak.png");
    private static final double CURRENT_PUSH = 0.006;
    private static final double CENTER_PULL = 0.0015;
    private static final double ECHO_OFFSET = 6.5;

    private static BlockPos origin = null;
    private static BoatEntityModel boatModel;
    private static PlayerEntityModel<AbstractClientPlayerEntity> playerWide;
    private static PlayerEntityModel<AbstractClientPlayerEntity> playerSlim;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(WombCurrentSystem.ROUTE_SYNC, (client, handler, buf, sender) -> {
            BlockPos o = buf.readBoolean() ? BlockPos.fromLong(buf.readLong()) : null;
            client.execute(() -> origin = o);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            origin = null;
            lastAiming = false;
        });
        ClientTickEvents.END_CLIENT_TICK.register(WombRouteClient::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(WombRouteClient::renderEchoBoat);
    }

    /** Gốc tuyến hành hương do server gửi xuống (null nếu chưa có tuyến). */
    public static BlockPos getOrigin() {
        return origin;
    }

    private static boolean inWomb(MinecraftClient client) {
        return client.world != null && client.world.getRegistryKey() == SamsaraDimensions.THE_WOMB_WORLD_KEY;
    }

    /** Thuyền người chơi đang ngồi, nếu đang trôi trên tuyến (chưa tới chân tháp). */
    private static BoatEntity boatOnRoute(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (origin == null || player == null || !inWomb(client)) return null;
        if (!(player.getVehicle() instanceof BoatEntity boat)) return null;
        if (WombRoute.distanceToRoute(origin, boat.getX(), boat.getZ()) > 10.0) return null;
        BlockPos spire = WombRoute.Stop.SPIRE.pos(origin);
        if (boat.squaredDistanceTo(Vec3d.ofCenter(spire)) < 24.0 * 24.0) return null;
        return boat;
    }

    // ==========================================
    // DÒNG CHẢY
    // ==========================================

    private static boolean lastAiming = false;

    private static void tick(MinecraftClient client) {
        // Báo server khi bắt đầu / thôi ngắm qua máy quay (The Wanderer đứng sững khi bị nhìn qua ống kính)
        boolean aiming = client.player != null && CamcorderClientManager.isFullyAimed();
        if (aiming != lastAiming && client.getNetworkHandler() != null) {
            lastAiming = aiming;
            net.minecraft.network.PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
            buf.writeBoolean(aiming);
            ClientPlayNetworking.send(com.samsara.system.womb.WanderChaseSystem.AIM_PACKET, buf);
        }

        BoatEntity boat = boatOnRoute(client);
        if (boat == null || !boat.isLogicalSideForUpdatingMovement()) return;
        Vec3d pos = boat.getPos();
        Vec3d tangent = WombRoute.tangentNear(origin, pos);
        Vec3d normal = new Vec3d(-tangent.z, 0, tangent.x);
        // Khoảng lệch có dấu so với đường giữa: kéo thuyền về giữa dòng
        double offset = signedOffset(pos, tangent);
        Vec3d push = tangent.multiply(CURRENT_PUSH).add(normal.multiply(-MathHelper.clamp(offset, -4.0, 4.0) * CENTER_PULL));
        boat.addVelocity(push.x, 0.0, push.z);
    }

    private static double signedOffset(Vec3d pos, Vec3d tangent) {
        Vec3d normal = new Vec3d(-tangent.z, 0, tangent.x);
        double best = Double.MAX_VALUE;
        double sign = 0.0;
        var pts = WombRoute.polyline(origin);
        for (int i = 0; i < pts.size() - 1; i++) {
            Vec3d a = pts.get(i);
            Vec3d b = pts.get(i + 1);
            Vec3d ab = b.subtract(a);
            double len2 = ab.x * ab.x + ab.z * ab.z;
            double t = len2 < 1.0E-6 ? 0 : MathHelper.clamp(((pos.x - a.x) * ab.x + (pos.z - a.z) * ab.z) / len2, 0, 1);
            Vec3d c = a.add(ab.multiply(t));
            double dx = pos.x - c.x;
            double dz = pos.z - c.z;
            double d2 = dx * dx + dz * dz;
            if (d2 < best) {
                best = d2;
                sign = dx * normal.x + dz * normal.z;
            }
        }
        return sign;
    }

    // ==========================================
    // THUYỀN BÓNG MA (CHỈ QUA MÁY QUAY)
    // ==========================================

    private static void renderEchoBoat(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!CamcorderClientManager.isFullyAimed()) return;
        BoatEntity boat = boatOnRoute(client);
        if (boat == null || client.player == null) return;
        ensureModels(client);

        float tickDelta = context.tickDelta();
        Vec3d boatPos = boat.getLerpedPos(tickDelta);
        Vec3d tangent = WombRoute.tangentNear(origin, boatPos);
        Vec3d normal = new Vec3d(-tangent.z, 0, tangent.x);
        // Luôn ở phía xa đường giữa hơn, để không đâm vào phao đèn
        double side = signedOffset(boatPos, tangent) > 0 ? -1.0 : 1.0;
        float time = client.player.age + tickDelta;
        Vec3d echo = boatPos.add(normal.multiply(ECHO_OFFSET * side)).add(0, Math.sin(time * 0.08) * 0.04, 0);
        Vec3d cam = context.camera().getPos();
        float boatYaw = boat.getYaw(tickDelta);

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        int light = 0xF000F0;
        float flicker = 0.55f + (float) Math.sin(time * 0.6f) * 0.08f;

        // Thuyền (biến đổi giống BoatEntityRenderer); mái chèo đi theo đúng nhịp chèo của bạn
        matrices.push();
        matrices.translate(echo.x - cam.x, echo.y - cam.y + 0.375, echo.z - cam.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - boatYaw));
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0f));
        boatModel.setAngles(boat, tickDelta, 0.0f, -0.1f, 0.0f, 0.0f);
        boatModel.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(BOAT_TEXTURE)), light,
                OverlayTexture.DEFAULT_UV, 0.55f, 0.55f, 0.75f, flicker);
        matrices.pop();

        // Người chèo: chính bạn, ngồi đúng chỗ bạn ngồi trên thuyền thật
        AbstractClientPlayerEntity player = client.player;
        Vec3d seat = player.getLerpedPos(tickDelta).subtract(boatPos);
        PlayerEntityModel<AbstractClientPlayerEntity> model = "slim".equals(player.getModel()) ? playerSlim : playerWide;
        model.riding = true;
        model.child = false;
        model.sneaking = false;
        model.setVisible(true);
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float headYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw) - bodyYaw;
        model.setAngles(player, 0.0f, 0.0f, time, headYaw, player.getPitch(tickDelta));

        matrices.push();
        matrices.translate(echo.x + seat.x - cam.x, echo.y + seat.y - cam.y, echo.z + seat.z - cam.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
        matrices.scale(-0.9375f, -0.9375f, 0.9375f);
        matrices.translate(0.0, -1.501, 0.0);
        model.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(player.getSkinTexture())), light,
                OverlayTexture.DEFAULT_UV, 0.6f, 0.6f, 0.8f, flicker);
        matrices.pop();
        consumers.draw();
    }

    private static void ensureModels(MinecraftClient client) {
        if (boatModel != null) return;
        boatModel = new BoatEntityModel(client.getEntityModelLoader().getModelPart(EntityModelLayers.createBoat(BoatEntity.Type.DARK_OAK)));
        playerWide = new PlayerEntityModel<>(client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER), false);
        playerSlim = new PlayerEntityModel<>(client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_SLIM), true);
    }
}
