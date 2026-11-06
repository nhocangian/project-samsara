package com.samsara.system.echo;

import com.samsara.entity.EchoDummyEntity;
import com.samsara.registry.SamsaraEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.item.AxeItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages periodic recording and manifestation of the Player Echo Residue.
 */
public class EchoResidueManager {
    private static final Map<UUID, PlayerActionBuffer> BUFFERS = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(EchoResidueManager::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        long currentTick = server.getTicks();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID uuid = player.getUuid();
            PlayerActionBuffer buffer = BUFFERS.computeIfAbsent(uuid, k -> new PlayerActionBuffer());
            buffer.record(player, currentTick);

            // Evaluate spawn trigger every 1200 ticks (1 minute)
            if (currentTick % 1200 == 0) {
                evaluateEchoTrigger(player, buffer, currentTick);
            }
        }
    }

    private static void evaluateEchoTrigger(ServerPlayerEntity player, PlayerActionBuffer buffer, long currentTick) {
        ServerWorld world = player.getServerWorld();

        // 1% chance per minute
        if (world.random.nextFloat() > 0.01f) {
            return;
        }

        boolean belowZero = player.getY() < 0;
        boolean choppingAloneAtNight = isChoppingAloneAtNight(player, world);

        if (belowZero || choppingAloneAtNight) {
            ActionSnapshot snapshot = buffer.getSnapshot60SecondsAgo(currentTick);
            if (snapshot != null) {
                spawnEchoDummy(world, player, snapshot);
            }
        }
    }

    private static boolean isChoppingAloneAtNight(ServerPlayerEntity player, ServerWorld world) {
        // Night check
        long timeOfDay = world.getTimeOfDay() % 24000;
        boolean isNight = timeOfDay >= 13000 && timeOfDay <= 23000;
        if (!isNight) return false;

        // Alone check: no other players within 64 blocks
        Box aloneBox = new Box(player.getBlockPos()).expand(64.0);
        List<ServerPlayerEntity> nearby = world.getEntitiesByClass(ServerPlayerEntity.class, aloneBox, p -> p != player);
        if (!nearby.isEmpty()) return false;

        // Chopping check: holding axe or near logs
        boolean holdingAxe = player.getMainHandStack().getItem() instanceof AxeItem;
        if (holdingAxe) return true;

        BlockPos pos = player.getBlockPos();
        for (BlockPos testPos : BlockPos.iterate(pos.add(-3, -1, -3), pos.add(3, 3, 3))) {
            BlockState state = world.getBlockState(testPos);
            if (state.isIn(BlockTags.LOGS)) {
                return true;
            }
        }

        return false;
    }

    private static void spawnEchoDummy(ServerWorld world, ServerPlayerEntity sourcePlayer, ActionSnapshot snapshot) {
        EchoDummyEntity dummy = SamsaraEntities.ECHO_DUMMY.create(world);
        if (dummy == null) return;

        dummy.refreshPositionAndAngles(
                snapshot.position().x,
                snapshot.position().y,
                snapshot.position().z,
                snapshot.yaw(),
                snapshot.pitch()
        );
        dummy.setPlayerUuid(sourcePlayer.getUuid());
        dummy.setCrouchingState(snapshot.isCrouching());
        dummy.setSwingingState(snapshot.isSwinging());

        world.spawnEntity(dummy);
    }

    public static void onPlayerDisconnect(UUID uuid) {
        BUFFERS.remove(uuid);
    }
}
