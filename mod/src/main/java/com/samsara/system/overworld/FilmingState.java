package com.samsara.system.overworld;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/**
 * Chế độ quay: khi bật (lệnh {@code /samsara quay <video>}), các hiện tượng ngẫu nhiên đứng yên, chỉ xảy ra khi người
 * quay bấm phím đạo diễn ({@link DirectorCues}); hiệu ứng của stage (block bay, nước xám, Phantom...) vẫn chạy như
 * thường. Để quay đúng kịch bản mà không bị một hiện tượng tự bật giữa cảnh (ví dụ Nam_1998 vào game ở Video 1). Lưu
 * trong world; {@code /samsara quay tat} để tắt.
 *
 * <p>Thêm {@link #suppressCamcorderGift}: chặn lượt tự tặng máy quay (Video 4: Nam chưa có máy quay tới cảnh 4; phím
 * đạo diễn "cài máy quay" vẫn tặng được).
 */
public final class FilmingState extends PersistentState {
    private static final String KEY = "samsara_filming";
    private boolean scripted;
    private boolean noGift;

    private static FilmingState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(FilmingState::fromNbt, FilmingState::new, KEY);
    }

    /** Đang quay theo kịch bản: hiện tượng ngẫu nhiên không tự xảy ra. */
    public static boolean scripted(MinecraftServer server) {
        return server != null && get(server).scripted;
    }

    public static boolean suppressCamcorderGift(MinecraftServer server) {
        return server != null && get(server).noGift;
    }

    public static void set(MinecraftServer server, boolean scripted, boolean noGift) {
        FilmingState s = get(server);
        s.scripted = scripted;
        s.noGift = noGift;
        s.markDirty();
    }

    private static FilmingState fromNbt(NbtCompound nbt) {
        FilmingState s = new FilmingState();
        s.scripted = nbt.getBoolean("Scripted");
        s.noGift = nbt.getBoolean("NoGift");
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("Scripted", scripted);
        nbt.putBoolean("NoGift", noGift);
        return nbt;
    }
}
