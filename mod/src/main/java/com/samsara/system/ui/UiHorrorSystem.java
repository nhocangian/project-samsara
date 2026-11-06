package com.samsara.system.ui;

import com.samsara.mixin.LevelPropertiesAccessor;
import com.samsara.system.cycle.EggCycleState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;

/**
 * Kinh dị tầng giao diện - phía SERVER: game tự "biết" mình là một cái game.
 *
 *  - Nam_1998 vào game: từ Stage 2, vào ban đêm, thỉnh thoảng hiện dòng vàng "Nam_1998 joined the game" y như
 *    nhiều người chơi (kể cả trong thế giới chơi đơn), rồi một lúc sau "left the game". Từ Stage 3, trong lúc
 *    "có mặt" hắn còn nhắn vài câu vào chat.
 *  - Thành tựu giả: toast thành tựu (vẽ ở client) + dòng chat thành tựu y như vanilla, mỗi mốc chỉ một lần.
 *  - Tên thế giới tự đổi: lúc thoát game, tên trong danh sách thế giới đổi theo tiến trình
 *    (Stage 3: "&lt;tên cũ&gt; - Nam_1998", Stage 4: "Nam_1998", sau đoạn kết: "đừng mở lại"). Chỉ áp cho chơi đơn.
 * Dòng F3 lạ và menu tạm dừng bị nhiễu nằm ở client (UiHorrorClient, DebugHudMixin).
 * Lệnh test: /samsara test ui <join|advancement|rename>.
 */
public class UiHorrorSystem {
    public static final Identifier FAKE_ADVANCEMENT = new Identifier("samsara", "fake_advancement");
    /** Gói S2C (boolean): Nam_1998 đang "có mặt" -> client thêm hắn vào bảng Tab. */
    public static final Identifier GHOST_PRESENCE = new Identifier("samsara", "ghost_presence");
    /** Gói S2C (boolean, lệnh test): bật / tắt trạng thái "đã xem đoạn kết" của menu chính phía client. */
    public static final Identifier ENDING_MENU_FLAG = new Identifier("samsara", "ending_menu_flag");

    public static void sendEndingMenuFlag(ServerPlayerEntity player, boolean seen) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(seen);
        ServerPlayNetworking.send(player, ENDING_MENU_FLAG, buf);
    }
    public static final String GHOST_NAME = "Nam_1998";

    /**
     * Câu duy nhất Nam_1998 gõ (kịch bản Video 3, cảnh 06). Kịch bản chỉ duyệt câu này:
     * không thêm câu khác khi chưa có vị trí trong kịch bản.
     */
    public static final String GHOST_LINE = "samsara.ui_horror.ghost.em_dang_o_dau";

    /**
     * Ba dòng Nam_1998 gõ ở lần "vào game" đầu tiên của world Chu Kỳ ≥2 (kịch bản Video 3: gửi đường dẫn OBS cho em).
     * Chỉ một lần mỗi world (cờ {@link #ARCHIVE_FLAG}); những lần sau quay về {@link #GHOST_LINE}.
     */
    public static final String[] ARCHIVE_LINES = {
            "samsara.ui_horror.ghost.em",
            "samsara.ui_horror.ghost.archive_path",
            "samsara.ui_horror.ghost.xem_tu_tep_dau_tien"
    };
    private static final String ARCHIVE_FLAG = "archive_lines";
    /** Khoảng cách giữa các dòng và từ dòng cuối tới lúc rời game (tick). */
    private static final int LINE_GAP = 80, LEAVE_AFTER_LAST = 120;

    private static int ghostLeaveTick = -1;
    private static int ghostNextLine = -1;
    private static String[] ghostLines = new String[0];
    private static int ghostLineIndex = 0;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ghostLeaveTick = -1;
            ghostNextLine = -1;
            EggCycleState state = EggCycleState.get(server);
            if (state.getOriginalLevelName() == null) state.setOriginalLevelName(server.getSaveProperties().getLevelName());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(UiHorrorSystem::renameOnExit);
        ServerTickEvents.END_SERVER_TICK.register(UiHorrorSystem::tick);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sendPresence(handler.getPlayer(), ghostLeaveTick >= 0));
    }

    // ==========================================
    // NAM_1998 VÀO / RỜI GAME
    // ==========================================

    private static void tick(MinecraftServer server) {
        int now = server.getTicks();
        Random random = server.getOverworld().getRandom();

        if (ghostLeaveTick >= 0) {
            if (now >= ghostLeaveTick) {
                ghostLeave(server);
            } else if (ghostNextLine >= 0 && now >= ghostNextLine) {
                if (ghostLineIndex < ghostLines.length) ghostSay(server, ghostLines[ghostLineIndex++]);
                ghostNextLine = ghostLineIndex < ghostLines.length ? now + LINE_GAP : -1;
            }
            return;
        }
        if (now % 1200 != 0) return;                       // kiểm tra mỗi phút
        if (EggCycleState.get(server).getStage() < 2 || !server.getOverworld().isNight()) return;
        if (random.nextFloat() < 1.0f / 8.0f) ghostJoin(server, 600 + random.nextInt(1200));
    }

    /**
     * Nam_1998 "vào game". Chu Kỳ ≥2, lần đầu: gõ {@link #ARCHIVE_LINES} rồi rời đi ngay sau dòng cuối.
     * Còn lại: Stage 3+ gõ đúng một câu {@link #GHOST_LINE}; Stage 2 vào rồi im lặng rời đi.
     */
    public static void ghostJoin(MinecraftServer server, int stayTicks) {
        EggCycleState state = EggCycleState.get(server);
        boolean archive = state.getCycle() >= 2 && state.markUiFlag(com.samsara.system.cycle.NamGhostSystem.NAM_UUID, ARCHIVE_FLAG);
        ghostJoin(server, stayTicks, archive);
    }

    public static void ghostJoin(MinecraftServer server, int stayTicks, boolean archive) {
        if (archive) ghostLines = ARCHIVE_LINES;
        else if (EggCycleState.get(server).getStage() >= 3) ghostLines = new String[]{GHOST_LINE};
        else ghostLines = new String[0];
        ghostLineIndex = 0;
        broadcast(server, Text.translatable("multiplayer.player.joined", GHOST_NAME).formatted(Formatting.YELLOW));
        int firstLine = server.getTicks() + 200;
        ghostNextLine = ghostLines.length > 0 ? firstLine : -1;
        ghostLeaveTick = archive
                ? firstLine + LINE_GAP * (ghostLines.length - 1) + LEAVE_AFTER_LAST
                : server.getTicks() + stayTicks;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) sendPresence(p, true);
    }

    public static void ghostLeave(MinecraftServer server) {
        broadcast(server, Text.translatable("multiplayer.player.left", GHOST_NAME).formatted(Formatting.YELLOW));
        ghostLeaveTick = -1;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) sendPresence(p, false);
    }

    private static void sendPresence(ServerPlayerEntity player, boolean present) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(present);
        ServerPlayNetworking.send(player, GHOST_PRESENCE, buf);
    }

    public static void ghostSay(MinecraftServer server, String line) {
        broadcast(server, Text.literal("<" + GHOST_NAME + "> ").append(Text.translatable(line)));
    }

    private static void broadcast(MinecraftServer server, Text text) {
        server.getPlayerManager().broadcast(text, false);
    }

    // ==========================================
    // THÀNH TỰU GIẢ
    // ==========================================

    /** Thành tựu giả ({@code title} là lang key; chuỗi thường cũng hiện đúng vì key lạ hiện nguyên văn), mỗi người chơi mỗi {@code key} chỉ một lần (trừ khi {@code force}). */
    public static void advancement(ServerPlayerEntity player, String key, String title, Item icon, boolean force) {
        EggCycleState state = EggCycleState.get(player.getServer());
        if (!state.markUiFlag(player.getUuid(), "adv_" + key) && !force) return;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(title);
        buf.writeIdentifier(Registries.ITEM.getId(icon));
        ServerPlayNetworking.send(player, FAKE_ADVANCEMENT, buf);

        Text link = Text.literal("[").append(Text.translatable(title)).append("]").formatted(Formatting.GREEN);
        broadcast(player.getServer(), Text.translatable("chat.type.advancement.task", player.getDisplayName(), link));
    }

    public static void advancement(ServerPlayerEntity player, String key, String title, Item icon) {
        advancement(player, key, title, icon, false);
    }

    // ==========================================
    // TÊN THẾ GIỚI TỰ ĐỔI
    // ==========================================

    /** Tên thế giới theo tiến trình hiện tại (null = giữ tên gốc). */
    public static String levelNameFor(MinecraftServer server) {
        EggCycleState state = EggCycleState.get(server);
        String original = state.getOriginalLevelName() != null ? state.getOriginalLevelName() : server.getSaveProperties().getLevelName();
        if (state.anyEndingSeen()) return "đừng mở lại";
        if (state.getStage() >= 4) return GHOST_NAME;
        if (state.getStage() >= 3) return original + " - " + GHOST_NAME;
        return null;
    }

    private static void renameOnExit(MinecraftServer server) {
        if (server.isDedicated()) return;
        String name = levelNameFor(server);
        if (name != null) applyLevelName(server, name);
    }

    public static boolean applyLevelName(MinecraftServer server, String name) {
        if (!(server.getSaveProperties() instanceof LevelProperties props)) return false;
        LevelInfo old = props.getLevelInfo();
        LevelInfo renamed = new LevelInfo(name, old.getGameMode(), old.isHardcore(), old.getDifficulty(),
                old.areCommandsAllowed(), old.getGameRules(), old.getDataConfiguration());
        ((LevelPropertiesAccessor) props).samsara$setLevelInfo(renamed);
        return true;
    }
}
