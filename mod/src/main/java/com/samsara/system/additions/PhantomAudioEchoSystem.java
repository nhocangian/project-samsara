package com.samsara.system.additions;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Creative Addition 1: Phantom Auditory Echo System.
 * Replays delayed spatial auditory cues (footsteps, pickaxe clinks, chest clicks)
 * originating 2-3 blocks directly behind the player's camera.
 * Reinforces the eerie realization that someone is walking in your exact footsteps because they ARE you.
 */
public class PhantomAudioEchoSystem {
    private static final Map<UUID, Deque<QueuedEchoSound>> SOUND_QUEUES = new HashMap<>();

    public record QueuedEchoSound(long executeTick, Vec3d relativeOffset) {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PhantomAudioEchoSystem::tick);
    }

    private static void tick(MinecraftServer server) {
        long currentTick = server.getTicks();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID uuid = player.getUuid();
            Deque<QueuedEchoSound> queue = SOUND_QUEUES.computeIfAbsent(uuid, k -> new ArrayDeque<>());

            // Randomly capture player movement sound to echo if underground
            if (player.getY() < 0 && player.isOnGround() && player.getVelocity().horizontalLengthSquared() > 0.001) {
                if (server.getOverworld().random.nextFloat() < 0.002f) { // Rare eerie occurrence
                    // Schedule playback 35 ticks (~1.75s) into the future
                    Vec3d behindOffset = player.getRotationVec(1.0f).multiply(-2.5);
                    queue.addLast(new QueuedEchoSound(currentTick + 35, behindOffset));
                }
            }

            // Process queued sounds
            while (!queue.isEmpty() && queue.peekFirst().executeTick() <= currentTick) {
                QueuedEchoSound echo = queue.pollFirst();
                Vec3d soundPos = player.getPos().add(echo.relativeOffset());

                // Send discreet sound packet positioned directly behind player
                PlaySoundS2CPacket packet = new PlaySoundS2CPacket(
                        Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_STONE_STEP),
                        SoundCategory.PLAYERS,
                        soundPos.x, soundPos.y, soundPos.z,
                        0.6f, 0.75f,
                        player.getRandom().nextLong()
                );
                player.networkHandler.sendPacket(packet);
            }
        }
    }
}
