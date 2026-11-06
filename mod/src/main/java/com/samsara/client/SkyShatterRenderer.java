package com.samsara.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.samsara.item.FracturedSlateItem;
import com.samsara.registry.SamsaraItems;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Custom Shader Renderer: Bầu Trời Thủy Tinh Vỡ Nát (Cinematic Sky Fracture & Glass Shatter).
 * Thay thế bầu trời cũ bằng hệ thống vết nứt không gian đa tầng phát quang:
 * 1. Mạng lưới nứt vỡ không gian có độ dày thể tích (Volumetric Fracture Ribbons) tỏa sắc quang phổ.
 * 2. 240+ mảnh gương phản chiếu 3D đa giác rơi lộn nhào với độ bóng gương (Specular Glint) khi trời vỡ.
 */
public class SkyShatterRenderer {
    private static final List<PolygonalGlassShard> SHARDS = new ArrayList<>();
    private static boolean wasTriggered = false;
    private static float shakeIntensity = 0.0f;

    public static class PolygonalGlassShard {
        float x, y, z;
        float vx, vy, vz;
        float rotX, rotY, rotZ;
        float vRotX, vRotY, vRotZ;
        float size;
        float r, g, b, alpha;
        int sides;

        public PolygonalGlassShard(float x, float y, float z, Random rand) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = (rand.nextFloat() - 0.5f) * 0.6f;
            this.vy = -0.4f - rand.nextFloat() * 0.7f;
            this.vz = (rand.nextFloat() - 0.5f) * 0.6f;
            this.rotX = rand.nextFloat() * 360f;
            this.rotY = rand.nextFloat() * 360f;
            this.rotZ = rand.nextFloat() * 360f;
            this.vRotX = (rand.nextFloat() - 0.5f) * 22f;
            this.vRotY = (rand.nextFloat() - 0.5f) * 22f;
            this.vRotZ = (rand.nextFloat() - 0.5f) * 22f;
            this.size = 2.5f + rand.nextFloat() * 5.0f;
            this.sides = rand.nextBoolean() ? 3 : 4; // Tam giác hoặc tứ giác kính

            // Màu phản chiếu bầu trời: tím huyền bí, cyan vũ trụ và trắng bạc
            float colorSeed = rand.nextFloat();
            if (colorSeed < 0.4f) {
                this.r = 0.25f; this.g = 0.85f; this.b = 1.0f; // Cyan
            } else if (colorSeed < 0.8f) {
                this.r = 0.85f; this.g = 0.35f; this.b = 1.0f; // Violet
            } else {
                this.r = 1.0f; this.g = 0.95f; this.b = 0.75f; // Gold-white
            }
            this.alpha = 0.8f + rand.nextFloat() * 0.2f;
        }

        public void update() {
            x += vx;
            y += vy;
            z += vz;
            rotX += vRotX;
            rotY += vRotY;
            rotZ += vRotZ;
            alpha = Math.max(0.0f, alpha - 0.007f);
        }
    }

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(SkyShatterRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (!player.clientWorld.getRegistryKey().getValue().getPath().equals("overworld")) {
            return;
        }

        boolean slateCompleted = isSlateFullyCompleted(player);
        if (!slateCompleted && SHARDS.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.multiply(context.camera().getRotation());

        // Hiệu ứng rung chấn nhẹ khi bầu trời vỡ
        if (shakeIntensity > 0.001f) {
            float shakeX = (player.getRandom().nextFloat() - 0.5f) * shakeIntensity;
            float shakeY = (player.getRandom().nextFloat() - 0.5f) * shakeIntensity;
            matrices.translate(shakeX, shakeY, 0);
            shakeIntensity *= 0.92f;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Matrix4f model = matrices.peek().getPositionMatrix();

        long worldTime = player.clientWorld.getTimeOfDay();
        float pulse = MathHelper.sin(worldTime * 0.1f) * 0.2f + 0.8f;

        // 1. MẠNG LƯỚI RẠN NỨT THỂ TÍCH (Volumetric Fracture Web)
        if (slateCompleted && SHARDS.isEmpty()) {
            buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

            Random crackRand = new Random(8888L);
            int branches = 20;
            float skyR = 85.0f;

            for (int b = 0; b < branches; b++) {
                float baseAngle = (float) b / branches * MathHelper.TAU;
                float curX = 0, curY = skyR, curZ = 0;

                for (int s = 0; s < 7; s++) {
                    float dev = (crackRand.nextFloat() - 0.5f) * 0.5f;
                    float angle = baseAngle + dev;
                    float nextY = curY - (10.0f + crackRand.nextFloat() * 5.0f);
                    float r = MathHelper.sqrt(Math.max(0, skyR * skyR - nextY * nextY));
                    float nextX = MathHelper.cos(angle) * r;
                    float nextZ = MathHelper.sin(angle) * r;

                    float thick = (1.5f - (s * 0.18f)) * pulse;
                    // Dải nứt rạn có bề dày và ánh sáng quang sai
                    buffer.vertex(model, curX - thick, curY, curZ).color(0.9f, 0.4f, 1.0f, 0.9f * pulse).next();
                    buffer.vertex(model, curX + thick, curY, curZ).color(0.2f, 0.9f, 1.0f, 0.9f * pulse).next();
                    buffer.vertex(model, nextX - thick, nextY, nextZ).color(0.9f, 0.4f, 1.0f, 0.9f * pulse).next();
                    buffer.vertex(model, nextX + thick, nextY, nextZ).color(0.2f, 0.9f, 1.0f, 0.9f * pulse).next();

                    curX = nextX; curY = nextY; curZ = nextZ;
                }
            }
            tessellator.draw();
        }

        // 2. KÍCH HOẠT VỠ VỤN THỦY TINH ĐỘT NGỘT (Shatter Trigger)
        if (slateCompleted && !wasTriggered && player.isSleeping()) {
            wasTriggered = true;
            shakeIntensity = 0.35f;
            spawnGlassShards();
            player.playSound(SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.AMBIENT, 2.5f, 0.5f);
            player.playSound(SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.AMBIENT, 2.5f, 1.3f);
            player.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.AMBIENT, 1.0f, 1.8f);
        }

        // 3. VẼ CÁC MẢNH KÍNH 3D ĐA GIÁC ĐANG RƠI (Falling Polygonal Shards)
        if (!SHARDS.isEmpty()) {
            buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

            for (int i = SHARDS.size() - 1; i >= 0; i--) {
                PolygonalGlassShard shard = SHARDS.get(i);
                shard.update();

                if (shard.alpha <= 0.01f || shard.y < -35.0f) {
                    SHARDS.remove(i);
                    continue;
                }

                float sz = shard.size;
                float sx = shard.x;
                float sy = shard.y;
                float szz = shard.z;

                // Ánh phản quang lấp lánh (Specular glint)
                float glint = MathHelper.sin(shard.rotX * 0.1f) * 0.3f + 0.7f;
                float fr = Math.min(1.0f, shard.r * glint);
                float fg = Math.min(1.0f, shard.g * glint);
                float fb = Math.min(1.0f, shard.b * glint);

                if (shard.sides == 3) {
                    buffer.vertex(model, sx, sy + sz, szz).color(1.0f, 1.0f, 1.0f, shard.alpha).next();
                    buffer.vertex(model, sx - sz, sy - sz * 0.6f, szz + sz * 0.4f).color(fr, fg, fb, shard.alpha).next();
                    buffer.vertex(model, sx + sz, sy - sz * 0.6f, szz - sz * 0.4f).color(fr * 0.8f, fg * 0.8f, fb * 0.8f, shard.alpha).next();
                } else {
                    // Tứ giác kính
                    buffer.vertex(model, sx - sz * 0.7f, sy + sz * 0.7f, szz).color(1.0f, 1.0f, 1.0f, shard.alpha).next();
                    buffer.vertex(model, sx - sz, sy - sz, szz).color(fr, fg, fb, shard.alpha).next();
                    buffer.vertex(model, sx + sz, sy - sz * 0.5f, szz).color(fr * 0.7f, fg * 0.7f, fb * 0.7f, shard.alpha).next();

                    buffer.vertex(model, sx - sz * 0.7f, sy + sz * 0.7f, szz).color(1.0f, 1.0f, 1.0f, shard.alpha).next();
                    buffer.vertex(model, sx + sz, sy - sz * 0.5f, szz).color(fr * 0.7f, fg * 0.7f, fb * 0.7f, shard.alpha).next();
                    buffer.vertex(model, sx + sz * 0.8f, sy + sz, szz).color(fr, fg, fb, shard.alpha).next();
                }
            }
            tessellator.draw();
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static void spawnGlassShards() {
        SHARDS.clear();
        Random rand = new Random();
        for (int i = 0; i < 240; i++) {
            float angle = rand.nextFloat() * MathHelper.TAU;
            float r = rand.nextFloat() * 70.0f;
            float x = MathHelper.cos(angle) * r;
            float z = MathHelper.sin(angle) * r;
            float y = 45.0f + rand.nextFloat() * 40.0f;
            SHARDS.add(new PolygonalGlassShard(x, y, z, rand));
        }
    }

    private static boolean isSlateFullyCompleted(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(SamsaraItems.FRACTURED_SLATE)) {
                NbtCompound nbt = stack.getNbt();
                if (nbt != null &&
                        nbt.getBoolean(FracturedSlateItem.NBT_COW) &&
                        nbt.getBoolean(FracturedSlateItem.NBT_ZOMBIE) &&
                        nbt.getBoolean(FracturedSlateItem.NBT_ENDERMAN)) {
                    return true;
                }
            }
        }
        return false;
    }
}
