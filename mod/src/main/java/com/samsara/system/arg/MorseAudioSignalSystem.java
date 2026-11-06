package com.samsara.system.arg;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ARG Feature 2: Tần Số Hư Vô (The 0.00Hz Morse Signal).
 * Khi người chơi ở độ sâu âm (Y <= -30) trong bóng tối tuyệt đối (Light = 0):
 * Phát ra tín hiệu âm thanh và hạt quang học nhấp nháy theo Mã Morse Quốc Tế:
 * "I AM YOU" (··  ·-  --  -·--  ---  ··-)
 */
public class MorseAudioSignalSystem {
    // Mã morse đại diện cho "I AM YOU"
    // 1 = dot (ngắn), 3 = dash (dài), 0 = khoảng lặng
    private static final int[] MORSE_PATTERN = {
            // I (··)
            1, 0, 1, 0, 0, 0,
            // A (·-)
            1, 0, 3, 0, 0, 0,
            // M (--)
            3, 0, 3, 0, 0, 0, 0, 0, 0, 0,
            // Y (-·--)
            3, 0, 1, 0, 3, 0, 3, 0, 0, 0,
            // O (---)
            3, 0, 3, 0, 3, 0, 0, 0,
            // U (··-)
            1, 0, 1, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final Map<UUID, Integer> STEP_MAP = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(MorseAudioSignalSystem::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 4 != 0) return; // Nhịp xung cơ sở mỗi 0.2s

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Điều kiện: Dưới tầng sâu Y <= -30 và không có ánh sáng
            if (player.getY() <= -30) {
                int light = player.getServerWorld().getLightLevel(LightType.BLOCK, player.getBlockPos());
                if (light == 0) {
                    UUID uuid = player.getUuid();
                    int step = STEP_MAP.getOrDefault(uuid, 0);

                    int signalType = MORSE_PATTERN[step % MORSE_PATTERN.length];

                    if (signalType > 0) {
                        float pitch = (signalType == 1) ? 1.6f : 0.8f;

                        // Tiếng gõ lách cách như sóng radio cổ
                        player.getServerWorld().playSound(
                                null,
                                player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value(),
                                SoundCategory.AMBIENT,
                                0.35f,
                                pitch
                        );

                        // Nếu người chơi đang dùng Kính Viễn Vọng (Spyglass) nhìn vào bóng tối:
                        // Các hạt phân tử phát sáng sẽ nhấp nháy đồng bộ
                        if (player.isUsingItem() && player.getActiveItem().isOf(Items.SPYGLASS)) {
                            Vec3d look = player.getRotationVec(1.0f).multiply(4.0);
                            player.getServerWorld().spawnParticles(
                                    ParticleTypes.GLOW,
                                    player.getX() + look.x,
                                    player.getEyeY() + look.y,
                                    player.getZ() + look.z,
                                    signalType == 3 ? 6 : 2,
                                    0.1, 0.1, 0.1,
                                    0.01
                            );
                        }
                    }

                    STEP_MAP.put(uuid, (step + 1) % MORSE_PATTERN.length);
                }
            }
        }
    }
}
