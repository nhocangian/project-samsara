package com.samsara.system.womb;

import com.samsara.registry.SamsaraDimensions;
import com.samsara.system.cycle.EggCycleState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Nhịp độ hành trình: mỗi khi người chơi bước sang một chương mới của tuyến hành hương,
 * một dòng tiêu đề hiện chậm giữa màn hình như tên chương của một cuốn băng.
 * Chỉ hiện khi đi tới (không hiện lại khi bị ném lùi), reset khi rời The Womb.
 * Âm thanh theo chương nằm ở phía client (WombAmbienceClient).
 */
public class WombPacingSystem {
    /** Mốc bắt đầu của mỗi chương, tính theo quãng đường dọc tuyến từ Bến Đò. */
    public static final double DOORS_START = 16.0;
    public static final double TITANS_START = 100.0;
    public static final double SPIRE_MARGIN = 45.0; // chương 4 bắt đầu khi còn cách cuối tuyến chừng này

    private static final String[][] CHAPTERS = {
            {"samsara.womb_pacing_system.i_ben_do", "samsara.womb_pacing_system.noi_ban_thuc_day"},
            {"samsara.womb_pacing_system.ii_nhung_canh_cua", "samsara.womb_pacing_system.moi_canh_cua_la"},
            {"samsara.womb_pacing_system.iii_nhung_nguoi_khong", "samsara.womb_pacing_system.nhung_lan_ban_da"},
            {"samsara.womb_pacing_system.iv_thap_nguoc", "samsara.womb_pacing_system.lan_cuoi_cung"},
    };

    private static final Map<UUID, Integer> SHOWN = new HashMap<>();

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SHOWN.clear());
        ServerTickEvents.END_SERVER_TICK.register(WombPacingSystem::tick);
    }

    /** Chương (0..3) tại quãng đường {@code progress}. */
    public static int chapterAt(BlockPos origin, double progress) {
        if (progress >= WombRoute.length(origin) - SPIRE_MARGIN) return 3;
        if (progress >= TITANS_START) return 2;
        if (progress >= DOORS_START) return 1;
        return 0;
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 10 != 0) return;
        ServerWorld womb = server.getWorld(SamsaraDimensions.THE_WOMB_WORLD_KEY);
        BlockPos origin = EggCycleState.get(server).getRouteOrigin();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (womb == null || origin == null || player.getWorld() != womb) {
                SHOWN.remove(player.getUuid());
                continue;
            }
            if (WombRoute.distanceToRoute(origin, player.getX(), player.getZ()) > 40.0) continue;
            double progress = WombRoute.progress(origin, player.getX(), player.getZ());
            int chapter = chapterAt(origin, progress);
            int shown = SHOWN.getOrDefault(player.getUuid(), -1);
            if (chapter > shown) {
                SHOWN.put(player.getUuid(), chapter);
                showTitle(player, chapter);
            }
        }
    }

    public static void showTitle(ServerPlayerEntity player, int chapter) {
        String[] c = CHAPTERS[Math.max(0, Math.min(CHAPTERS.length - 1, chapter))];
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(30, 70, 40));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable(c[1])));
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable(c[0])));
    }
}
