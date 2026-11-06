package com.samsara.client;

import com.samsara.registry.SamsaraBiomes;
import com.samsara.registry.SamsaraDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Hệ thống hiển thị hiệu ứng môi trường & thông báo chuyển Biome trong The Womb:
 * 1. Thông báo Actionbar điện ảnh (Cinematic Biome Title) & âm thanh chào mừng khi bước qua ranh giới Biome.
 * 2. Hạt môi trường đặc trưng của từng quần xã:
 *    - The Origin Cradle: Bụi sao thiên hà & Hư không bay ngược lên trời về phía Hắc Lỗ Gargantua.
 *    - The Titan Graveyard: Bão tro tàn trắng xám rơi dày đặc như ngày tận thế.
 *    - The Singularity Spires: Bụi sao thạch anh, giọt lệ tím & tia chớp không-thời gian lấp lánh.
 */
public class WombCradleDriftSystem {

    private static final Random RAND = new Random();
    private static RegistryKey<Biome> lastBiomeKey = null;

    // Bụi sao thiên hà vàng kim & tím huyền bí
    private static final DustParticleEffect GOLD_STAR_DUST =
            new DustParticleEffect(new Vector3f(1.0f, 0.92f, 0.65f), 0.85f);
    private static final DustParticleEffect VOID_STAR_DUST =
            new DustParticleEffect(new Vector3f(0.75f, 0.45f, 1.0f), 0.90f);

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientWorld world = client.world;
            PlayerEntity player = client.player;
            if (world == null || player == null) return;

            // Chỉ kích hoạt trong chiều không gian The Womb
            if (!world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY)) {
                lastBiomeKey = null;
                return;
            }

            BlockPos pos = player.getBlockPos();
            RegistryEntry<Biome> biomeEntry = world.getBiome(pos);
            RegistryKey<Biome> currentBiomeKey = biomeEntry.getKey().orElse(null);

            // 1. KIỂM TRA CHUYỂN BIOME & PHÁT THÔNG BÁO ĐIỆN ẢNH
            if (currentBiomeKey != null && !currentBiomeKey.equals(lastBiomeKey)) {
                lastBiomeKey = currentBiomeKey;
                handleBiomeTransition(client, player, currentBiomeKey);
            }

            // 2. SINH HẠT MÔI TRƯỜNG ĐẶC TRƯNG THEO BIOME
            if (biomeEntry.matchesKey(SamsaraBiomes.TITAN_GRAVEYARD)) {
                spawnTitanGraveyardParticles(world, player);
            } else if (biomeEntry.matchesKey(SamsaraBiomes.SINGULARITY_SPIRES)) {
                spawnSingularitySpiresParticles(world, player);
            } else {
                spawnOriginCradleParticles(world, player);
            }
        });
    }

    private static void handleBiomeTransition(MinecraftClient client, PlayerEntity player, RegistryKey<Biome> biomeKey) {
        if (biomeKey.equals(SamsaraBiomes.TITAN_GRAVEYARD)) {
            player.sendMessage(Text.translatable("samsara.womb_cradle_drift_system.the_titan_graveyard_tro"), true);
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.AMBIENT_BASALT_DELTAS_MOOD.value(), 0.8f, 0.6f));
        } else if (biomeKey.equals(SamsaraBiomes.SINGULARITY_SPIRES)) {
            player.sendMessage(Text.translatable("samsara.womb_cradle_drift_system.the_singularity_spires_tinh"), true);
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.85f));
        } else if (biomeKey.equals(SamsaraBiomes.ORIGIN_CRADLE)) {
            player.sendMessage(Text.translatable("samsara.womb_cradle_drift_system.the_origin_cradle_vung"), true);
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), 0.6f, 1.8f));
        }
    }

    /**
     * Vùng Nôi Nguyên Thủy: Bụi sao bay ngược chiều trọng lực hướng về Hắc Lỗ Gargantua (North / Upwards).
     */
    private static void spawnOriginCradleParticles(ClientWorld world, PlayerEntity player) {
        int count = 2 + RAND.nextInt(3);
        for (int i = 0; i < count; i++) {
            double range = 20.0;
            double px = player.getX() + (RAND.nextDouble() - 0.5) * range;
            double pz = player.getZ() + (RAND.nextDouble() - 0.5) * range;
            double py = 62.05 + RAND.nextDouble() * 2.5;

            double vx = (RAND.nextDouble() - 0.5) * 0.008;
            double vy = 0.034 + RAND.nextDouble() * 0.024;
            double vz = -0.054 - RAND.nextDouble() * 0.028;

            float choice = RAND.nextFloat();
            if (choice < 0.40f) {
                world.addParticle(ParticleTypes.REVERSE_PORTAL, px, py, pz, vx, vy, vz);
            } else if (choice < 0.70f) {
                world.addParticle(ParticleTypes.END_ROD, px, py, pz, vx * 0.7, vy * 0.8, vz * 0.7);
            } else if (choice < 0.85f) {
                world.addParticle(GOLD_STAR_DUST, px, py, pz, vx, vy, vz);
            } else {
                world.addParticle(VOID_STAR_DUST, px, py, pz, vx, vy, vz);
            }
        }
    }

    /**
     * Nghĩa Địa Cự Thần: Mưa tro tàn xám trắng rơi phủ ngập tràn không gian.
     */
    private static void spawnTitanGraveyardParticles(ClientWorld world, PlayerEntity player) {
        int count = 4 + RAND.nextInt(4);
        for (int i = 0; i < count; i++) {
            double range = 24.0;
            double px = player.getX() + (RAND.nextDouble() - 0.5) * range;
            double pz = player.getZ() + (RAND.nextDouble() - 0.5) * range;
            double py = player.getY() + 3.0 + RAND.nextDouble() * 8.0;

            double vx = (RAND.nextDouble() - 0.5) * 0.03;
            double vy = -0.04 - RAND.nextDouble() * 0.04;
            double vz = (RAND.nextDouble() - 0.5) * 0.03;

            if (RAND.nextBoolean()) {
                world.addParticle(ParticleTypes.ASH, px, py, pz, vx, vy, vz);
            } else {
                world.addParticle(ParticleTypes.WHITE_ASH, px, py, pz, vx, vy, vz);
            }
        }
    }

    /**
     * Tháp Kỳ Dị: Các tinh thể tím lơ lửng, lệ hắc diện thạch & tia chớp không gian.
     */
    private static void spawnSingularitySpiresParticles(ClientWorld world, PlayerEntity player) {
        int count = 3 + RAND.nextInt(3);
        for (int i = 0; i < count; i++) {
            double range = 18.0;
            double px = player.getX() + (RAND.nextDouble() - 0.5) * range;
            double pz = player.getZ() + (RAND.nextDouble() - 0.5) * range;
            double py = 62.05 + RAND.nextDouble() * 4.0;

            float choice = RAND.nextFloat();
            if (choice < 0.45f) {
                world.addParticle(ParticleTypes.PORTAL, px, py, pz, (RAND.nextDouble() - 0.5) * 0.3, 0.1, (RAND.nextDouble() - 0.5) * 0.3);
            } else if (choice < 0.75f) {
                world.addParticle(ParticleTypes.END_ROD, px, py, pz, (RAND.nextDouble() - 0.5) * 0.02, 0.02, (RAND.nextDouble() - 0.5) * 0.02);
            } else {
                world.addParticle(ParticleTypes.FALLING_OBSIDIAN_TEAR, px, py + 2.0, pz, 0, -0.05, 0);
            }
        }
    }
}
