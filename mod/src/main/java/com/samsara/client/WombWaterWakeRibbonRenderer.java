package com.samsara.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.samsara.registry.SamsaraDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.*;

/**
 * Custom Boat Wake Ribbon & Organic Oar Ripple Renderer (Đạt chuẩn Minecraft Pixel-Art & Fluid Dynamics):
 * 1. Dải rẽ nước phủ texture bọt nước tự nhiên (water_wake.png):
 *    - Viền ngoài bọt nước trắng răng cưa hữu cơ uốn lượn, lòng dải nước sủi bọt khí li ti.
 *    - Tự động mở rộng hình chữ V (V-taper) và vuốt mỏng đầu nối tại sống thuyền.
 *    - Bắt ánh sáng vàng kim nhẹ từ Hắc Lỗ Gargantua khi rẽ sóng.
 * 2. Vòng sóng khua mái chèo phủ texture hữu cơ (oar_ripple.png):
 *    - Không còn là các vòng tròn vector toán học hoàn hảo mà là các gợn sóng bọt nước răng cưa tự nhiên.
 *    - Có đốm bọt nước ở tâm mái chèo và vòng sóng phụ đồng tâm mở rộng dần rồi tan biến.
 * 3. Tối ưu hiệu năng 100%, không còn hạt particle thừa thãi gây lag.
 */
public class WombWaterWakeRibbonRenderer {

    private static final Identifier WAKE_TEXTURE = new Identifier("samsara", "textures/misc/water_wake.png");
    private static final Identifier RIPPLE_TEXTURE = new Identifier("samsara", "textures/misc/oar_ripple.png");

    public static class WakeNode {
        double x, z;
        double sideX, sideZ;
        double waterY;
        float age;
        float maxAge;
        float baseWidth;
        float vCoord;

        public WakeNode(double x, double z, double sideX, double sideZ, double waterY, float maxAge, float baseWidth, float vCoord) {
            this.x = x;
            this.z = z;
            this.sideX = sideX;
            this.sideZ = sideZ;
            this.waterY = waterY;
            this.age = 0f;
            this.maxAge = maxAge;
            this.baseWidth = baseWidth;
            this.vCoord = vCoord;
        }
    }

    public static class OarRipple {
        double x, z;
        double waterY;
        float age;
        float maxAge;
        float startRadius;
        float endRadius;
        float rotation; // Góc xoay ngẫu nhiên giúp các vòng không bị đồng điệu

        public OarRipple(double x, double z, double waterY) {
            this.x = x;
            this.z = z;
            this.waterY = waterY;
            this.age = 0f;
            this.maxAge = 38f; // Tồn tại khoảng 1.9 giây
            this.startRadius = 0.35f;
            this.endRadius = 1.75f;
            this.rotation = (float) (Math.random() * Math.PI * 2);
        }
    }

    private static class BoatWakeData {
        final List<WakeNode> nodes = new ArrayList<>();
        final List<OarRipple> ripples = new ArrayList<>();
        int leftOarTicks = 0;
        int rightOarTicks = 0;
        double lastSpawnX = 0;
        double lastSpawnZ = 0;
        double accumulatedDist = 0;
        boolean hasLastPos = false;
    }

    private static final Map<UUID, BoatWakeData> boatWakes = new HashMap<>();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(WombWaterWakeRibbonRenderer::tick);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(WombWaterWakeRibbonRenderer::render);
    }

    private static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            boatWakes.clear();
            return;
        }

        boolean isWomb = world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY);
        Set<UUID> activeBoatUuids = new HashSet<>();

        // Kiểm tra phương tiện người chơi đang ngồi
        Entity vehicle = client.player.getVehicle();
        if (vehicle instanceof BoatEntity playerBoat) {
            updateBoat(world, playerBoat, isWomb);
            activeBoatUuids.add(playerBoat.getUuid());
        }

        // Kiểm tra các thuyền khác xung quanh
        for (Entity entity : world.getEntities()) {
            if (entity instanceof BoatEntity boat && entity != vehicle) {
                updateBoat(world, boat, isWomb);
                activeBoatUuids.add(boat.getUuid());
            }
        }

        // Cập nhật tuổi thọ và dọn dẹp các mắt dải rẽ nước & vòng sóng
        Iterator<Map.Entry<UUID, BoatWakeData>> it = boatWakes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BoatWakeData> entry = it.next();
            BoatWakeData data = entry.getValue();

            for (int i = data.nodes.size() - 1; i >= 0; i--) {
                WakeNode node = data.nodes.get(i);
                node.age += 1.0f;
                if (node.age >= node.maxAge) {
                    data.nodes.remove(i);
                }
            }

            for (int i = data.ripples.size() - 1; i >= 0; i--) {
                OarRipple ripple = data.ripples.get(i);
                ripple.age += 1.0f;
                if (ripple.age >= ripple.maxAge) {
                    data.ripples.remove(i);
                }
            }

            if (!activeBoatUuids.contains(entry.getKey()) && data.nodes.isEmpty() && data.ripples.isEmpty()) {
                it.remove();
            }
        }
    }

    private static void updateBoat(ClientWorld world, BoatEntity boat, boolean isWomb) {
        double boatY = boat.getY();
        if (isWomb && (boatY < 59.0 || boatY > 64.0)) return;

        BoatWakeData data = boatWakes.computeIfAbsent(boat.getUuid(), k -> new BoatWakeData());

        float yaw = boat.getYaw();
        double rad = Math.toRadians(yaw);
        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);
        double rightX = -Math.cos(rad);
        double rightZ = -Math.sin(rad);

        double dx = boat.getX() - boat.prevX;
        double dz = boat.getZ() - boat.prevZ;
        double moveDistSq = dx * dx + dz * dz;
        boolean paddling = boat.isPaddleMoving(0) || boat.isPaddleMoving(1);
        boolean moving = moveDistSq > 0.0001 || paddling || boat.getVelocity().horizontalLengthSquared() > 0.0001;

        double waterY = isWomb ? 62.025 : Math.max(boatY + 0.35, Math.floor(boatY) + 0.92);

        if (moving) {
            double sternDist = 1.05;
            double sternX = boat.getX() - forwardX * sternDist;
            double sternZ = boat.getZ() - forwardZ * sternDist;

            double distFromLast = data.hasLastPos ? Math.hypot(sternX - data.lastSpawnX, sternZ - data.lastSpawnZ) : 999.0;

            if (distFromLast > 4.0 && data.hasLastPos) {
                data.nodes.clear();
                distFromLast = 999.0;
            }

            // Tần suất cắm mắt lưới (0.24 block đảm bảo đường cong cực kỳ mượt mà)
            if (distFromLast >= 0.24 || !data.hasLastPos) {
                double step = data.hasLastPos ? distFromLast : 0.24;
                data.accumulatedDist += step;
                float vCoord = (float) (data.accumulatedDist / 2.3);

                // Độ rộng ban đầu thích ứng theo tốc độ thuyền
                double currentSpeed = Math.sqrt(moveDistSq);
                float speedBonus = (float) MathHelper.clamp(currentSpeed * 2.5, 0.0, 0.4);
                float initialWidth = 1.35f + speedBonus;

                data.nodes.add(new WakeNode(sternX, sternZ, rightX, rightZ, waterY, 68f, initialWidth, vCoord));
                data.lastSpawnX = sternX;
                data.lastSpawnZ = sternZ;
                data.hasLastPos = true;
            }

            // Vòng sóng nước khua mái chèo (Oar Ripple Rings)
            if (boat.isPaddleMoving(0)) {
                data.leftOarTicks++;
                if (data.leftOarTicks % 9 == 1) {
                    double oarLX = boat.getX() - forwardX * 0.20 - rightX * 1.25;
                    double oarLZ = boat.getZ() - forwardZ * 0.20 - rightZ * 1.25;
                    data.ripples.add(new OarRipple(oarLX, oarLZ, waterY));
                }
            } else {
                data.leftOarTicks = 0;
            }

            if (boat.isPaddleMoving(1)) {
                data.rightOarTicks++;
                if (data.rightOarTicks % 9 == 5) { // Lệch pha nhịp nhàng
                    double oarRX = boat.getX() - forwardX * 0.20 + rightX * 1.25;
                    double oarRZ = boat.getZ() - forwardZ * 0.20 + rightZ * 1.25;
                    data.ripples.add(new OarRipple(oarRX, oarRZ, waterY));
                }
            } else {
                data.rightOarTicks = 0;
            }
        } else {
            data.hasLastPos = false;
        }
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (boatWakes.isEmpty()) return;

        Vec3d camPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        matrices.push();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-3.0f, -3.0f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Matrix4f model = matrices.peek().getPositionMatrix();

        // =========================================================================
        // 1. VẼ DẢI RẼ NƯỚC UỐN LƯỢN (TEXTURED WAKE RIBBON)
        // =========================================================================
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, WAKE_TEXTURE);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

        buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        for (BoatWakeData data : boatWakes.values()) {
            List<WakeNode> nodes = data.nodes;
            int nSize = nodes.size();
            if (nSize < 2) continue;

            for (int i = 0; i < nSize - 1; i++) {
                WakeNode n1 = nodes.get(i);
                WakeNode n2 = nodes.get(i + 1);

                float p1 = n1.age / n1.maxAge;
                float p2 = n2.age / n2.maxAge;

                // Taper làm mượt điểm nối ngay sau thuyền
                float tailFade1 = MathHelper.clamp((1.0f - p1) * 1.15f, 0.0f, 1.0f);
                float tailFade2 = MathHelper.clamp((1.0f - p2) * 1.15f, 0.0f, 1.0f);
                float headFade1 = i < 2 ? (i + 1) * 0.5f : 1.0f;
                float headFade2 = (i + 1) < 2 ? (i + 2) * 0.5f : 1.0f;

                float alpha1 = tailFade1 * headFade1;
                float alpha2 = tailFade2 * headFade2;

                // Dải nước mở rộng theo hình chữ V loang mềm mại
                float w1 = n1.baseWidth * (1.0f + p1 * 0.85f);
                float w2 = n2.baseWidth * (1.0f + p2 * 0.85f);
                float hw1 = w1 * 0.5f;
                float hw2 = w2 * 0.5f;

                float lx1 = (float)(n1.x - n1.sideX * hw1 - camPos.x);
                float lz1 = (float)(n1.z - n1.sideZ * hw1 - camPos.z);
                float rx1 = (float)(n1.x + n1.sideX * hw1 - camPos.x);
                float rz1 = (float)(n1.z + n1.sideZ * hw1 - camPos.z);
                float y1  = (float)(n1.waterY - camPos.y);

                float lx2 = (float)(n2.x - n2.sideX * hw2 - camPos.x);
                float lz2 = (float)(n2.z - n2.sideZ * hw2 - camPos.z);
                float rx2 = (float)(n2.x + n2.sideX * hw2 - camPos.x);
                float rz2 = (float)(n2.z + n2.sideZ * hw2 - camPos.z);
                float y2  = (float)(n2.waterY - camPos.y);

                float v1 = n1.vCoord;
                float v2 = n2.vCoord;

                // Ánh sáng trắng bọt nước với chút ấm vàng nhẹ từ Hắc Lỗ
                float rCol = 1.0f;
                float gCol = 0.98f;
                float bCol = 0.95f;

                // Tam giác 1: TopLeft -> TopRight -> BottomRight
                buffer.vertex(model, lx1, y1, lz1).texture(0.0f, v1).color(rCol, gCol, bCol, alpha1).next();
                buffer.vertex(model, rx1, y1, rz1).texture(1.0f, v1).color(rCol, gCol, bCol, alpha1).next();
                buffer.vertex(model, rx2, y2, rz2).texture(1.0f, v2).color(rCol, gCol, bCol, alpha2).next();

                // Tam giác 2: TopLeft -> BottomRight -> BottomLeft
                buffer.vertex(model, lx1, y1, lz1).texture(0.0f, v1).color(rCol, gCol, bCol, alpha1).next();
                buffer.vertex(model, rx2, y2, rz2).texture(1.0f, v2).color(rCol, gCol, bCol, alpha2).next();
                buffer.vertex(model, lx2, y2, lz2).texture(0.0f, v2).color(rCol, gCol, bCol, alpha2).next();
            }
        }
        tessellator.draw();

        // =========================================================================
        // 2. VẼ VÒNG SÓNG KHUA MÁI CHÈO HỮU CƠ (TEXTURED OAR RIPPLES)
        // =========================================================================
        RenderSystem.setShaderTexture(0, RIPPLE_TEXTURE);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        for (BoatWakeData data : boatWakes.values()) {
            for (OarRipple ripple : data.ripples) {
                float progress = ripple.age / ripple.maxAge;
                float radius = ripple.startRadius + (ripple.endRadius - ripple.startRadius) * progress;
                float ringAlpha = (1.0f - progress) * 0.90f;
                float y = (float)(ripple.waterY - camPos.y + 0.002); // Nằm nhẹ trên dải rẽ nước

                float cx = (float)(ripple.x - camPos.x);
                float cz = (float)(ripple.z - camPos.z);

                // Áp dụng góc xoay ngẫu nhiên giúp các đốm bọt nước mái chèo trông đa dạng
                float cosR = MathHelper.cos(ripple.rotation);
                float sinR = MathHelper.sin(ripple.rotation);

                float dx1 = (-radius) * cosR - (-radius) * sinR;
                float dz1 = (-radius) * sinR + (-radius) * cosR;

                float dx2 = (radius) * cosR - (-radius) * sinR;
                float dz2 = (radius) * sinR + (-radius) * cosR;

                float dx3 = (radius) * cosR - (radius) * sinR;
                float dz3 = (radius) * sinR + (radius) * cosR;

                float dx4 = (-radius) * cosR - (radius) * sinR;
                float dz4 = (-radius) * sinR + (radius) * cosR;

                // Tam giác 1: TopLeft -> TopRight -> BottomRight
                buffer.vertex(model, cx + dx1, y, cz + dz1).texture(0.0f, 0.0f).color(1f, 1f, 1f, ringAlpha).next();
                buffer.vertex(model, cx + dx2, y, cz + dz2).texture(1.0f, 0.0f).color(1f, 1f, 1f, ringAlpha).next();
                buffer.vertex(model, cx + dx3, y, cz + dz3).texture(1.0f, 1.0f).color(1f, 1f, 1f, ringAlpha).next();

                // Tam giác 2: TopLeft -> BottomRight -> BottomLeft
                buffer.vertex(model, cx + dx1, y, cz + dz1).texture(0.0f, 0.0f).color(1f, 1f, 1f, ringAlpha).next();
                buffer.vertex(model, cx + dx3, y, cz + dz3).texture(1.0f, 1.0f).color(1f, 1f, 1f, ringAlpha).next();
                buffer.vertex(model, cx + dx4, y, cz + dz4).texture(0.0f, 1.0f).color(1f, 1f, 1f, ringAlpha).next();
            }
        }
        tessellator.draw();

        RenderSystem.polygonOffset(0.0f, 0.0f);
        RenderSystem.disablePolygonOffset();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
