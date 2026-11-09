package com.samsara.system.cycle;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Trạng thái DUY NHẤT của chu kỳ "Vỏ Trứng" trong một thế giới (lưu trong data/ của Overworld).
 * Mọi hệ thống cần biết tiến trình (Mặt Trời, Phantom, máy quay, dị tượng, cổng The Womb) đọc từ đây.
 *
 * Theo Master Production Document:
 *   sleep_count 0 -> Stage 0 (Mặt Trời Vanilla), 1-2 -> Stage 1, 3-4 -> Stage 2, 5-6 -> Stage 3,
 *   7+ (giấc ngủ thứ 7) -> Stage 4: Hố Đen nuốt trời, người chơi bị hút về "Ký Ức Của Nam"
 *   (cánh đồng hướng dương); từ hầm nhà Nam mới có vết rách sang The Womb.
 */
public class EggCycleState extends PersistentState {
    public static final String KEY = "samsara_egg_cycle";
    /** Dữ liệu cũ của SolarFractureManager, chỉ đọc một lần để chuyển sang KEY mới. */
    private static final String LEGACY_SOLAR_KEY = "samsara_solar_fracture";

    public static final int MIN_STAGE = 0;
    public static final int MAX_STAGE = 4;
    public static final int FINAL_SLEEP = 7;

    private int sleeps = 0;
    private int stage = 0;
    /** Ngày (timeOfDay / 24000) của lần ngủ gần nhất đã được tính, để mỗi đêm chỉ tính 1 lần. */
    private long lastCountedDay = -1;
    /** Người chơi đã được <System> cài máy quay vào Hotbar. */
    private final Set<UUID> camcorderGiven = new HashSet<>();
    /** Người chơi đã bị kéo sang chiều Ký Ức Của Nam sau giấc ngủ thứ 7. */
    private final Set<UUID> awakened = new HashSet<>();
    /** Nhà Nam ở chiều Ký Ức Của Nam đã được dựng chưa. */
    private boolean namHouseBuilt = false;
    /** Số thứ tự chu kỳ của world này: 1 = world gốc, 2+ = world "Chu Kỳ #N" tạo sau màn kết. */
    private int cycle = 1;
    /** Đã dựng nhà Nam ở điểm spawn của world chu kỳ mới chưa. */
    private boolean cycleHouseBuilt = false;
    /** Vị trí bệ Quả Trứng trong The Womb (null = chưa dựng). */
    private net.minecraft.util.math.BlockPos eggAltarPos = null;
    /** Gốc tuyến hành hương trong The Womb (nơi người chơi bước vào lần đầu); null = chưa dựng. */
    private net.minecraft.util.math.BlockPos routeOrigin = null;
    /** Người chơi đã xem đoạn kết (đối thoại với <System>). */
    private final Set<UUID> endingSeen = new HashSet<>();
    /** Đã đi hết Tầng Dưới: vết rách dưới hầm nhà Nam dẫn thẳng tới The Womb. */
    private final Set<UUID> tangDuoiCleared = new HashSet<>();

    public static EggCycleState get(MinecraftServer server) {
        PersistentStateManager psm = server.getOverworld().getPersistentStateManager();
        EggCycleState state = psm.get(EggCycleState::fromNbt, KEY);
        if (state == null) {
            state = new EggCycleState();
            LegacyReader reader = psm.get(LegacyReader::fromNbt, LEGACY_SOLAR_KEY);
            if (reader != null) {
                NbtCompound legacy = reader.nbt;
                state.sleeps = Math.max(0, legacy.getInt("Sleeps"));
                state.lastCountedDay = legacy.contains("LastDay") ? legacy.getLong("LastDay") : -1;
                state.stage = legacy.contains("Stage")
                        ? MathHelper.clamp(legacy.getInt("Stage"), MIN_STAGE, MAX_STAGE)
                        : stageForSleeps(state.sleeps);
            }
            psm.set(KEY, state);
            state.markDirty();
        }
        return state;
    }

    /** Giai đoạn tương ứng với số lần ngủ. */
    public static int stageForSleeps(int sleeps) {
        if (sleeps <= 0) return 0;
        if (sleeps <= 2) return 1;
        if (sleeps <= 4) return 2;
        if (sleeps <= 6) return 3;
        return 4;
    }

    /** Số lần ngủ tối thiểu để đạt một giai đoạn (dùng cho lệnh test). */
    public static int sleepsForStage(int stage) {
        return switch (stage) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 5;
            default -> FINAL_SLEEP;
        };
    }

    public int getSleeps() { return sleeps; }
    public int getStage() { return stage; }
    public long getLastCountedDay() { return lastCountedDay; }

    /**
     * Ghi nhận một giấc ngủ qua đêm. Giai đoạn không bao giờ lùi
     * (nếu trước đó đã được đặt cao hơn bằng lệnh test).
     */
    public void countSleep(long day) {
        lastCountedDay = day;
        sleeps++;
        stage = Math.max(stage, stageForSleeps(sleeps));
        markDirty();
    }

    /** Đặt thẳng số lần ngủ (lệnh test). Giai đoạn đi theo số lần ngủ, kể cả khi lùi. */
    public void setSleeps(int value) {
        sleeps = Math.max(0, value);
        stage = stageForSleeps(sleeps);
        markDirty();
    }

    /** Đặt thẳng giai đoạn (lệnh test), không đổi số lần ngủ. */
    public void setStage(int value) {
        stage = MathHelper.clamp(value, MIN_STAGE, MAX_STAGE);
        markDirty();
    }

    public boolean wasCamcorderGiven(UUID uuid) { return camcorderGiven.contains(uuid); }
    public void markCamcorderGiven(UUID uuid) { if (camcorderGiven.add(uuid)) markDirty(); }
    public void clearCamcorderGiven(UUID uuid) { if (camcorderGiven.remove(uuid)) markDirty(); }

    public boolean isAwakened(UUID uuid) { return awakened.contains(uuid); }
    public void markAwakened(UUID uuid) { if (awakened.add(uuid)) markDirty(); }
    public void clearAwakened(UUID uuid) { if (awakened.remove(uuid)) markDirty(); }

    public int getCycle() { return cycle; }
    public void setCycle(int value) { cycle = Math.max(1, value); markDirty(); }
    public boolean isCycleHouseBuilt() { return cycleHouseBuilt; }
    public void setCycleHouseBuilt(boolean built) { cycleHouseBuilt = built; markDirty(); }

    public boolean isNamHouseBuilt() { return namHouseBuilt; }
    public void setNamHouseBuilt(boolean built) { namHouseBuilt = built; markDirty(); }

    public net.minecraft.util.math.BlockPos getEggAltarPos() { return eggAltarPos; }
    public void setEggAltarPos(net.minecraft.util.math.BlockPos pos) { eggAltarPos = pos; markDirty(); }

    /** Cờ "đã xảy ra một lần" cho kinh dị tầng giao diện, dạng "uuid:key" (vd. thành tựu giả). */
    private final Set<String> uiFlags = new HashSet<>();
    /** Tên thế giới ban đầu, trước khi bị đổi khi thoát game. */
    private String originalLevelName = null;

    public boolean markUiFlag(UUID uuid, String key) {
        boolean added = uiFlags.add(uuid + ":" + key);
        if (added) markDirty();
        return added;
    }

    public void clearUiFlags(UUID uuid) {
        if (uiFlags.removeIf(f -> f.startsWith(uuid + ":"))) markDirty();
    }

    public boolean anyEndingSeen() { return !endingSeen.isEmpty(); }

    public String getOriginalLevelName() { return originalLevelName; }
    public void setOriginalLevelName(String name) { originalLevelName = name; markDirty(); }

    public net.minecraft.util.math.BlockPos getRouteOrigin() { return routeOrigin; }
    public void setRouteOrigin(net.minecraft.util.math.BlockPos pos) { routeOrigin = pos; markDirty(); }

    public boolean hasSeenEnding(UUID uuid) { return endingSeen.contains(uuid); }
    public boolean hasClearedTangDuoi(UUID uuid) { return tangDuoiCleared.contains(uuid); }
    public void markTangDuoiCleared(UUID uuid) { if (tangDuoiCleared.add(uuid)) markDirty(); }
    public void clearTangDuoiCleared(UUID uuid) { if (tangDuoiCleared.remove(uuid)) markDirty(); }
    public void markEndingSeen(UUID uuid) { if (endingSeen.add(uuid)) markDirty(); }
    public void clearEndingSeen(UUID uuid) { if (endingSeen.remove(uuid)) markDirty(); }

    // ==========================================
    // PERSISTENCE
    // ==========================================

    public static EggCycleState fromNbt(NbtCompound nbt) {
        EggCycleState s = new EggCycleState();
        s.sleeps = Math.max(0, nbt.getInt("Sleeps"));
        s.stage = MathHelper.clamp(nbt.getInt("Stage"), MIN_STAGE, MAX_STAGE);
        s.lastCountedDay = nbt.contains("LastDay") ? nbt.getLong("LastDay") : -1;
        readUuids(nbt, "CamcorderGiven", s.camcorderGiven);
        readUuids(nbt, "Awakened", s.awakened);
        s.namHouseBuilt = nbt.getBoolean("NamHouseBuilt");
        s.cycle = nbt.contains("Cycle") ? Math.max(1, nbt.getInt("Cycle")) : 1;
        s.cycleHouseBuilt = nbt.getBoolean("CycleHouseBuilt");
        if (nbt.contains("EggAltarPos")) s.eggAltarPos = net.minecraft.util.math.BlockPos.fromLong(nbt.getLong("EggAltarPos"));
        readUuids(nbt, "EndingSeen", s.endingSeen);
        readUuids(nbt, "TangDuoiCleared", s.tangDuoiCleared);
        for (net.minecraft.nbt.NbtElement e : nbt.getList("UiFlags", NbtElement.STRING_TYPE)) s.uiFlags.add(e.asString());
        if (nbt.contains("OriginalLevelName")) s.originalLevelName = nbt.getString("OriginalLevelName");
        if (nbt.contains("RouteOrigin")) s.routeOrigin = net.minecraft.util.math.BlockPos.fromLong(nbt.getLong("RouteOrigin"));
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("Sleeps", sleeps);
        nbt.putInt("Stage", stage);
        nbt.putLong("LastDay", lastCountedDay);
        writeUuids(nbt, "CamcorderGiven", camcorderGiven);
        writeUuids(nbt, "Awakened", awakened);
        nbt.putBoolean("NamHouseBuilt", namHouseBuilt);
        nbt.putInt("Cycle", cycle);
        nbt.putBoolean("CycleHouseBuilt", cycleHouseBuilt);
        if (eggAltarPos != null) nbt.putLong("EggAltarPos", eggAltarPos.asLong());
        writeUuids(nbt, "EndingSeen", endingSeen);
        writeUuids(nbt, "TangDuoiCleared", tangDuoiCleared);
        if (routeOrigin != null) nbt.putLong("RouteOrigin", routeOrigin.asLong());
        NbtList flags = new NbtList();
        for (String f : uiFlags) flags.add(NbtString.of(f));
        nbt.put("UiFlags", flags);
        if (originalLevelName != null) nbt.putString("OriginalLevelName", originalLevelName);
        return nbt;
    }

    private static void readUuids(NbtCompound nbt, String key, Set<UUID> out) {
        NbtList list = nbt.getList(key, NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) {
            try {
                out.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void writeUuids(NbtCompound nbt, String key, Set<UUID> in) {
        NbtList list = new NbtList();
        for (UUID uuid : in) list.add(NbtString.of(uuid.toString()));
        nbt.put(key, list);
    }

    /** Chỉ dùng để đọc NBT thô của dữ liệu cũ; không bao giờ được ghi lại. */
    private static class LegacyReader extends PersistentState {
        final NbtCompound nbt;
        LegacyReader(NbtCompound nbt) { this.nbt = nbt; }
        static LegacyReader fromNbt(NbtCompound nbt) { return new LegacyReader(nbt); }
        @Override public NbtCompound writeNbt(NbtCompound out) { return out.copyFrom(nbt); }
    }
}
