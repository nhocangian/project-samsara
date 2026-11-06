package com.samsara.system.karma;

import com.samsara.entity.TheWandererEntity;
import com.samsara.registry.SamsaraEntities;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player sin debt, karmic accumulation, and Wanderer peripheral manifestation.
 * Fully persisted inside the active world save folder via Minecraft's PersistentState.
 */
public class SinDebtManager extends PersistentState {
    public static final String KEY = "samsara_sin_debt";
    public static final int THRESHOLD = 50; // Points required to provoke an appearance

    private final Map<UUID, Integer> sinDebts = new HashMap<>();

    public static SinDebtManager get(ServerWorld world) {
        return world.getServer().getOverworld().getPersistentStateManager().getOrCreate(
                SinDebtManager::fromNbt,
                SinDebtManager::new,
                KEY
        );
    }

    public int getSinDebt(UUID playerUuid) {
        return sinDebts.getOrDefault(playerUuid, 0);
    }

    public void addSinDebt(ServerPlayerEntity player, int points) {
        UUID uuid = player.getUuid();
        int current = getSinDebt(uuid);
        // Không cho nợ nghiệp xuống dưới 0 (vd. Primordial Egg xóa nợ bằng -9999),
        // nếu không người chơi sẽ "tích lũy công đức" vô hạn và The Wanderer không bao giờ xuất hiện lại.
        int updated = Math.max(0, current + points);
        sinDebts.put(uuid, updated);
        markDirty();

        if (updated >= THRESHOLD) {
            boolean spawned = attemptSpawnWanderer(player);
            if (spawned) {
                // Deduct threshold after successful manifestation
                sinDebts.put(uuid, updated - THRESHOLD);
                markDirty();
            }
        }
    }

    /**
     * Spawns The Wanderer at the player's periphery (16 to 24 blocks away).
     */
    private boolean attemptSpawnWanderer(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        double angle = player.getYaw() + 180 + (world.random.nextDouble() - 0.5) * 90.0; // Peripheral / behind
        double rad = Math.toRadians(angle);
        double distance = 16.0 + world.random.nextDouble() * 8.0; // 16 - 24 blocks

        double targetX = player.getX() + Math.cos(rad) * distance;
        double targetZ = player.getZ() + Math.sin(rad) * distance;

        BlockPos targetPos = new BlockPos(
                MathHelper.floor(targetX),
                MathHelper.floor(player.getY()),
                MathHelper.floor(targetZ)
        );

        // Find valid floor
        BlockPos surfacePos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, targetPos);
        if (Math.abs(surfacePos.getY() - player.getBlockY()) > 10) {
            surfacePos = targetPos;
        }

        // Verify spawn clearance
        if (!world.getBlockState(surfacePos).isAir() || !world.getBlockState(surfacePos.up()).isAir()) {
            return false;
        }

        TheWandererEntity wanderer = SamsaraEntities.THE_WANDERER.create(world);
        if (wanderer == null) return false;

        wanderer.refreshPositionAndAngles(
                targetPos.getX() + 0.5,
                surfacePos.getY(),
                targetPos.getZ() + 0.5,
                (float) Math.toDegrees(Math.atan2(player.getZ() - targetPos.getZ(), player.getX() - targetPos.getX())) - 90.0f,
                0.0f
        );

        return world.spawnEntity(wanderer);
    }

    public static SinDebtManager fromNbt(NbtCompound nbt) {
        SinDebtManager manager = new SinDebtManager();
        NbtCompound debtsTag = nbt.getCompound("Debts");
        for (String key : debtsTag.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                manager.sinDebts.put(uuid, debtsTag.getInt(key));
            } catch (IllegalArgumentException ignored) {}
        }
        return manager;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound debtsTag = new NbtCompound();
        for (Map.Entry<UUID, Integer> entry : sinDebts.entrySet()) {
            debtsTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("Debts", debtsTag);
        return nbt;
    }
}
