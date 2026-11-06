package com.samsara.system.cycle;

import com.samsara.worldgen.memory.NamHouseBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World "Chu Kỳ #N": sau màn kết, client tạo một world mới (NewCycleClient) và đặt {@link #pendingCycle}
 * trước khi server tích hợp khởi động (cùng JVM). Khi server mới chạy:
 *  - ghi số chu kỳ vào EggCycleState,
 *  - dựng nhà Nam (bản không có vết rách) cách điểm spawn 12 block về phía bắc, cửa quay về điểm spawn,
 *  - lần đầu vào: tiêu đề "Chu Kỳ #N" + một dòng của <System>.
 * World cũ không bị đụng tới.
 * Lệnh test: /samsara test new_cycle [house|intro].
 */
public final class NewCycleSystem {
    private NewCycleSystem() {}

    /** Đặt bởi client ngay trước khi tạo world mới; 0 = không có. */
    public static volatile int pendingCycle = 0;

    /** Nhà nằm phía bắc điểm spawn: cửa (z + 4) cách chỗ xuất hiện ~8 block. */
    private static final int HOUSE_OFFSET_Z = -12;
    private static final int INTRO_DELAY_TICKS = 60;

    private static final Map<UUID, Integer> INTRO_AT = new HashMap<>();

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            INTRO_AT.clear();
            int cycle = pendingCycle;
            pendingCycle = 0;
            if (cycle <= 1) return;
            EggCycleState state = EggCycleState.get(server);
            state.setCycle(cycle);
            if (!state.isCycleHouseBuilt()) {
                buildHouse(server);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            EggCycleState state = EggCycleState.get(server);
            if (state.getCycle() > 1 && state.markUiFlag(handler.player.getUuid(), "cycle_intro")) {
                INTRO_AT.put(handler.player.getUuid(), server.getTicks() + INTRO_DELAY_TICKS);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (INTRO_AT.isEmpty()) return;
            int now = server.getTicks();
            INTRO_AT.entrySet().removeIf(e -> {
                if (now < e.getValue()) return false;
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
                if (player != null) showIntro(player, EggCycleState.get(server).getCycle());
                return true;
            });
        });
    }

    /** Dựng nhà Nam gần điểm spawn của Overworld. */
    public static BlockPos buildHouse(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        BlockPos ground = buildHouseFacing(overworld, overworld.getSpawnPos());
        EggCycleState.get(server).setCycleHouseBuilt(true);
        return ground;
    }

    /** Dựng nhà sao cho cửa quay về {@code standing} (nhà nằm 12 block về phía bắc). Trả về vị trí mặt sàn. */
    public static BlockPos buildHouseFacing(ServerWorld world, BlockPos standing) {
        BlockPos column = standing.add(0, 0, HOUSE_OFFSET_Z);
        world.getChunk(column);
        BlockPos ground = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column).down();
        NamHouseBuilder.buildForNewCycle(world, ground);
        return ground;
    }

    public static void showIntro(ServerPlayerEntity player, int cycle) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(30, 80, 40));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("samsara.new_cycle.subtitle")));
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("samsara.new_cycle.title", cycle)));
        player.sendMessage(Text.translatable("samsara.new_cycle.system"), false);
        player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.AMBIENT, 0.6f, 0.5f);
    }
}
