package com.samsara.system.additions;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Đếm lặng lẽ số lần chết và chỗ chết đầu tiên của mỗi người chơi (lưu trong world). Không hiện gì cho người chơi;
 * dữ liệu để dành cho câu đố / bảng F3 về sau.
 */
public class MetempsychosisManager extends PersistentState {
    public static final String KEY = "samsara_metempsychosis";

    private final Map<UUID, Integer> incarnationCycles = new HashMap<>();
    private final Map<UUID, BlockPos> lastDeathCoordinates = new HashMap<>();
    private final Map<UUID, BlockPos> firstDeathCoordinates = new HashMap<>();
    private final Map<UUID, String> firstDeathCauses = new HashMap<>();
    private final Map<UUID, Long> firstDeathTimestamps = new HashMap<>();

    public static MetempsychosisManager get(ServerWorld world) {
        return world.getServer().getOverworld().getPersistentStateManager().getOrCreate(
                MetempsychosisManager::fromNbt,
                MetempsychosisManager::new,
                KEY
        );
    }

    public static void register() {
        // Track player death
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                MetempsychosisManager manager = get(player.getServerWorld());
                UUID uuid = player.getUuid();

                int currentCycle = manager.incarnationCycles.getOrDefault(uuid, 1);
                manager.incarnationCycles.put(uuid, currentCycle + 1);
                manager.lastDeathCoordinates.put(uuid, player.getBlockPos());

                // Ghi nhận cái chết ĐẦU TIÊN của người chơi để phục vụ Nghi Thức Tọa Độ Gương trong The Womb
                if (!manager.firstDeathCoordinates.containsKey(uuid)) {
                    manager.firstDeathCoordinates.put(uuid, player.getBlockPos());
                    manager.firstDeathCauses.put(uuid, damageSource.getName());
                    manager.firstDeathTimestamps.put(uuid, System.currentTimeMillis());
                }

                manager.markDirty();
            }
        });

        // Hồi sinh: không báo gì (người dùng bỏ dòng "[Chu kỳ #n] Thân xác tái tạo"); chỉ lặng lẽ đếm số lần chết
    }

    public BlockPos getFirstDeathCoordinates(UUID uuid) {
        return firstDeathCoordinates.getOrDefault(uuid, new BlockPos(128, 64, 128));
    }

    public boolean hasDiedOnce(UUID uuid) {
        return firstDeathCoordinates.containsKey(uuid);
    }

    public String getFirstDeathCause(UUID uuid) {
        return firstDeathCauses.getOrDefault(uuid, "Hư Vô Không Xác Định (Chu kỳ #0)");
    }

    public long getFirstDeathTimestamp(UUID uuid) {
        return firstDeathTimestamps.getOrDefault(uuid, System.currentTimeMillis());
    }

    public int getCycle(UUID uuid) {
        return incarnationCycles.getOrDefault(uuid, 1);
    }

    public static MetempsychosisManager fromNbt(NbtCompound nbt) {
        MetempsychosisManager manager = new MetempsychosisManager();
        NbtCompound cycles = nbt.getCompound("Cycles");
        for (String k : cycles.getKeys()) {
            manager.incarnationCycles.put(UUID.fromString(k), cycles.getInt(k));
        }

        NbtCompound firstDeaths = nbt.getCompound("FirstDeaths");
        for (String k : firstDeaths.getKeys()) {
            NbtCompound dTag = firstDeaths.getCompound(k);
            UUID uuid = UUID.fromString(k);
            manager.firstDeathCoordinates.put(uuid, new BlockPos(dTag.getInt("X"), dTag.getInt("Y"), dTag.getInt("Z")));
            manager.firstDeathCauses.put(uuid, dTag.getString("Cause"));
            manager.firstDeathTimestamps.put(uuid, dTag.getLong("Time"));
        }
        return manager;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound cycles = new NbtCompound();
        for (Map.Entry<UUID, Integer> entry : incarnationCycles.entrySet()) {
            cycles.putInt(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("Cycles", cycles);

        NbtCompound firstDeaths = new NbtCompound();
        for (Map.Entry<UUID, BlockPos> entry : firstDeathCoordinates.entrySet()) {
            UUID uuid = entry.getKey();
            BlockPos pos = entry.getValue();
            NbtCompound dTag = new NbtCompound();
            dTag.putInt("X", pos.getX());
            dTag.putInt("Y", pos.getY());
            dTag.putInt("Z", pos.getZ());
            dTag.putString("Cause", firstDeathCauses.getOrDefault(uuid, "unknown"));
            dTag.putLong("Time", firstDeathTimestamps.getOrDefault(uuid, 0L));
            firstDeaths.put(uuid.toString(), dTag);
        }
        nbt.put("FirstDeaths", firstDeaths);

        return nbt;
    }
}
