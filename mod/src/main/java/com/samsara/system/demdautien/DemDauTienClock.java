package com.samsara.system.demdautien;

import com.samsara.registry.SamsaraDimensions;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Đồng hồ riêng của tầng 4 "Đêm đầu tiên" và phép tính mép vết rách, dùng chung cho server và client.
 *
 * <p>Chiều phụ trong Minecraft dùng chung giờ với Overworld, nên tầng này tự giữ giờ: {@code timeOfDay = start +
 * (gameTime - anchor)}. Server gửi (anchor, start) cho client ({@link #PACKET}); mixin {@code DemDauTienTimeMixin} trả
 * giờ này cho {@code World.getTimeOfDay()} phía client, nên bầu trời và uniform {@code worldTime} của Iris đi theo.
 *
 * <p>Shader ({@code lib/dem_dau_tien_tear.glsl}) tính nhịp thở của vết rách theo {@code worldTime}, server tính theo đúng
 * giờ đó với cùng công thức ({@link #margin}), nên chỗ người chơi thấy vết rách phủ qua cũng là chỗ server cho chế giường.
 * Đổi công thức ở đây thì đổi y hệt trong shader (và ngược lại).
 *
 * <p>Shader còn phải biết con cừu nào đã bị đánh (để mở vết rách nhỏ quanh nó). Thông tin đó đi kèm đồng hồ: số ngày
 * ({@code timeOfDay / 24000}, uniform {@code worldDay}) là bitmask ba con cừu; giờ trong ngày ({@link #hour}) vẫn chạy
 * như thường. Bầu trời và việc ngủ chỉ nhìn giờ trong ngày; số ngày chỉ đổi pha mặt trăng.
 */
public final class DemDauTienClock {
    private DemDauTienClock() {}

    public static final Identifier PACKET = new Identifier("samsara", "dem_dau_tien_clock");

    /** Giờ lúc tỉnh dậy: sáng muộn; tối hẳn ở {@link #NIGHT} (khoảng 9 phút sau). */
    public static final long START = 3000;
    /** Từ giờ này giường cho nằm (ngưỡng ngủ của vanilla). */
    public static final long SLEEP_FROM = 12542;
    /** Tối hẳn: chưa ngủ thì Khối Chưa Tải bắt đầu ăn vào. */
    public static final long NIGHT = 13800;
    /** Cảnh cuối: đồng hồ nhảy tới đây, shader thấy giờ này thì cho vết rách nở ra nuốt đảo rồi trắng xoá. */
    public static final long FINALE = 18000;
    /** Shader: trắng xoá bắt đầu sau từng này tick kể từ {@link #FINALE}. */
    public static final long WHITE_AFTER = 170;

    // Phía client: (anchor, start) nhận từ server; chưa nhận thì coi như giữa trưa
    private static volatile boolean clientSet;
    private static volatile long clientAnchor;
    private static volatile long clientStart;

    public static void setClient(long anchor, long start) {
        clientAnchor = anchor;
        clientStart = start;
        clientSet = true;
    }

    public static long clientTimeOfDay(World world) {
        if (!clientSet) return 6000;
        return clientStart + (world.getTime() - clientAnchor);
    }

    /** Giờ trong ngày (bỏ phần số ngày mang bitmask cừu). */
    public static long hour(long timeOfDay) {
        return Math.floorMod(timeOfDay, 24000L);
    }

    /** Bitmask cừu đã bị đánh, đọc từ số ngày. */
    public static int sheepBits(long timeOfDay) {
        return (int) Math.floorDiv(timeOfDay, 24000L) & 7;
    }

    public static boolean isFloor(World world) {
        return world != null && world.getRegistryKey() == SamsaraDimensions.DEM_DAU_TIEN_WORLD_KEY;
    }

    // ==========================================
    // MÉP VẾT RÁCH (giống tearMargin trong shader, trừ nhiễu nhỏ ±0,3 khối)
    // ==========================================

    private static double breath(int i, double t) {
        return 0.5 + 0.32 * Math.sin(t * 0.55 + i * 2.1) + 0.18 * Math.sin(t * 1.37 + i * 4.3);
    }

    /** Thời gian (giây) mà nhịp thở dùng, từ giờ trong ngày của tầng. */
    public static double seconds(long timeOfDay) {
        return timeOfDay / 20.0;
    }

    /**
     * Khoảng cách (khối) từ điểm (x, y, z) vào trong mép vết rách gần nhất lúc {@code timeOfDay}: dương là ở trong.
     * Shader còn cộng một lớp nhiễu nhỏ (±0,3 khối) và dải dither ±0,3 khối; nơi gọi nên đòi dư ra chừng 0,4 khối.
     */
    public static double margin(double x, double y, double z, long timeOfDay) {
        long hour = hour(timeOfDay);
        double t = seconds(hour);
        double grow = finaleGrowth(hour);
        double best = -1e9;
        int bits = sheepBits(timeOfDay);
        for (int k = 0; k < DemDauTienBuilder.SHEEP.length; k++) {
            if ((bits & (1 << k)) == 0) continue;
            best = Math.max(best, sheepMargin(k, x, y, z, t));
        }
        for (int i = 0; i < DemDauTienBuilder.TEARS.length; i++) {
            double[] tear = DemDauTienBuilder.TEARS[i];
            double b = 0.6 + 0.5 * breath(i, t);
            double dx = x - tear[0], dy = (y - tear[1]) * 1.25, dz = z - tear[2];
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double a = Math.atan2(dz, dx);
            double waves = 0.55 * Math.sin(a * 5.0 - t * 1.1 + i) + 0.35 * Math.sin(a * 3.0 + t * 0.7 + i * 2.0);
            double surge = 0.6 * Math.sin(t * 1.3 + Math.sin(a * 2.0 + i) * 2.2);
            double r = tear[3] * 0.85 * b + waves + surge + grow;
            best = Math.max(best, r - dist);
        }
        return best;
    }

    /**
     * Vết rách nhỏ ở con cừu thứ k (đã bị đánh): một ống đứng bán kính ~1 khối, mép sóng, trôi vòng quanh chỗ con cừu
     * đứng nên cứ quét ngang qua người nó (chỗ vết rách phủ thì thấy còn lông). Giống {@code sheepMargin} trong shader.
     */
    private static double sheepMargin(int k, double x, double y, double z, double t) {
        int[] s = DemDauTienBuilder.SHEEP[k];
        double base = DemDauTienBuilder.ORIGIN.getY();
        if (y < base - 6 || y > base + 8) return -1e9;
        double orbit = t * 0.7 + k * 2.1;
        double cx = s[0] + 0.5 + 0.9 * Math.cos(orbit), cz = s[1] + 0.5 + 0.9 * Math.sin(orbit);
        double dx = x - cx, dz = z - cz;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double a = Math.atan2(dz, dx);
        double r = 1.0 * (0.85 + 0.2 * Math.sin(t * 0.9 + k * 1.7)) + 0.15 * Math.sin(a * 4.0 - t * 1.6 + k)
                + 0.1 * Math.sin(a * 7.0 + t * 1.1 + k * 2.0);
        return r - dist;
    }

    /** Cảnh cuối: bán kính cộng thêm (khối), tăng nhanh dần để nuốt cả đảo trong vài giây. */
    public static double finaleGrowth(long timeOfDay) {
        if (timeOfDay < FINALE) return 0.0;
        double s = (timeOfDay - FINALE) / 20.0;
        return s * s * 1.6;
    }

    /** Điểm có nằm chắc trong vết rách không (đã chừa sai số so với hình người chơi thấy). */
    public static boolean inside(double x, double y, double z, long timeOfDay) {
        return margin(x, y, z, timeOfDay) > 0.4;
    }
}
