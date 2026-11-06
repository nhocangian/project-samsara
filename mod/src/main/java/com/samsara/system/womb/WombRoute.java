package com.samsara.system.womb;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Tuyến hành hương cố định trong The Womb (Giai đoạn 3b), tính từ nơi người chơi bước vào lần đầu (origin),
 * chạy về phía bắc (-Z) tới Tháp Singularity. Biển ở đây chỉ ngập mắt cá chân (mặt nước ở y = 61..62)
 * nên thuyền lướt được trên toàn tuyến.
 *
 *   DOCK            Bến Đò - nơi đến, có thuyền (3b-2)
 *   TITAN_STEVE     Xác khổng thần 1: Steve Alpha 2009 (3b-3)
 *   TITAN_CREEPER   Xác khổng thần 2: gã bị Creeper nổ chết 2013 (3b-3)
 *   TITAN_ARSONIST  Xác khổng thần 3: kẻ đốt nhà trên server (3b-3)
 *   SPIRE           Tháp Singularity + Quả Trứng (3b-4; hiện tạm là bệ trứng)
 *
 * Đường đi uốn nhẹ trái phải giữa các chặng để không thấy hết đích từ đầu.
 * Mọi toạ độ chỉ phụ thuộc origin, nên chỉ cần lưu origin (EggCycleState.routeOrigin).
 */
public final class WombRoute {
    private WombRoute() {}

    public static final int SURFACE_Y = 62;

    public enum Stop {
        DOCK("Bến Đò", 0, 0, 0),
        TITAN_STEVE("Khổng Thần: Steve 2009", 0, -110, 1),
        TITAN_CREEPER("Khổng Thần: Hố Nổ 2013", 28, -200, -1),
        TITAN_ARSONIST("Khổng Thần: Kẻ Đốt Nhà", -24, -290, 1),
        SPIRE("Tháp Singularity", 0, -400, 0);

        public final String title;
        public final int dx;
        public final int dz;
        /** Phía đặt công trình so với tuyến (+1 = đông, -1 = tây, 0 = ngay trên tuyến). */
        public final int side;

        Stop(String title, int dx, int dz, int side) {
            this.title = title;
            this.dx = dx;
            this.dz = dz;
            this.side = side;
        }

        /** Điểm trên tuyến của chặng này. */
        public BlockPos pos(BlockPos origin) {
            return new BlockPos(origin.getX() + dx, SURFACE_Y, origin.getZ() + dz);
        }

        /** Nơi dựng công trình của chặng (xác khổng thần đặt lệch sang bên tuyến). */
        public BlockPos landmark(BlockPos origin) {
            return pos(origin).add(side * 20, 0, 0);
        }

        public static Stop byIndex(int i) {
            Stop[] all = values();
            return all[MathHelper.clamp(i, 0, all.length - 1)];
        }
    }

    /** Các điểm gãy của tuyến (y = SURFACE_Y). */
    public static List<Vec3d> polyline(BlockPos origin) {
        List<Vec3d> pts = new ArrayList<>();
        for (Stop s : Stop.values()) {
            BlockPos p = s.pos(origin);
            pts.add(new Vec3d(p.getX() + 0.5, SURFACE_Y, p.getZ() + 0.5));
        }
        return pts;
    }

    /** Tổng chiều dài tuyến (block). */
    public static double length(BlockPos origin) {
        List<Vec3d> pts = polyline(origin);
        double total = 0.0;
        for (int i = 0; i < pts.size() - 1; i++) total += pts.get(i).distanceTo(pts.get(i + 1));
        return total;
    }

    /** Điểm trên tuyến cách Bến Đò {@code distance} block (đo dọc tuyến). */
    public static Vec3d pointAlong(BlockPos origin, double distance) {
        List<Vec3d> pts = polyline(origin);
        double left = Math.max(0.0, distance);
        for (int i = 0; i < pts.size() - 1; i++) {
            double len = pts.get(i).distanceTo(pts.get(i + 1));
            if (left <= len) return pts.get(i).lerp(pts.get(i + 1), left / len);
            left -= len;
        }
        return pts.get(pts.size() - 1);
    }

    /** Quãng đường dọc tuyến (từ Bến Đò) tới điểm trên tuyến gần (x, z) nhất. */
    public static double progress(BlockPos origin, double x, double z) {
        List<Vec3d> pts = polyline(origin);
        double best = Double.MAX_VALUE;
        double bestProgress = 0.0;
        double walked = 0.0;
        for (int i = 0; i < pts.size() - 1; i++) {
            Vec3d a = pts.get(i);
            Vec3d b = pts.get(i + 1);
            double abx = b.x - a.x;
            double abz = b.z - a.z;
            double len2 = abx * abx + abz * abz;
            double len = Math.sqrt(len2);
            double t = len2 < 1.0E-6 ? 0.0 : MathHelper.clamp(((x - a.x) * abx + (z - a.z) * abz) / len2, 0.0, 1.0);
            double cx = a.x + abx * t - x;
            double cz = a.z + abz * t - z;
            double d = cx * cx + cz * cz;
            if (d < best) {
                best = d;
                bestProgress = walked + t * len;
            }
            walked += len;
        }
        return bestProgress;
    }

    /** Quãng đường dọc tuyến tới một chặng. */
    public static double progressOf(BlockPos origin, Stop stop) {
        BlockPos p = stop.pos(origin);
        return progress(origin, p.getX() + 0.5, p.getZ() + 0.5);
    }

    /** Lấy mẫu đều dọc tuyến, mỗi {@code step} block một điểm (kèm điểm đầu). */
    public static List<Vec3d> sample(BlockPos origin, double step) {
        List<Vec3d> pts = polyline(origin);
        List<Vec3d> out = new ArrayList<>();
        out.add(pts.get(0));
        double carry = 0.0;
        for (int i = 0; i < pts.size() - 1; i++) {
            Vec3d a = pts.get(i);
            Vec3d b = pts.get(i + 1);
            double len = a.distanceTo(b);
            double d = step - carry;
            while (d <= len) {
                out.add(a.lerp(b, d / len));
                d += step;
            }
            carry = len - (d - step);
        }
        return out;
    }

    /** Hướng tuyến (đã chuẩn hoá, ngang) tại điểm gần {@code p} nhất. */
    public static Vec3d tangentNear(BlockPos origin, Vec3d p) {
        List<Vec3d> pts = polyline(origin);
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < pts.size() - 1; i++) {
            double d = distanceToSegmentSq(p, pts.get(i), pts.get(i + 1));
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        Vec3d dir = pts.get(best + 1).subtract(pts.get(best));
        return new Vec3d(dir.x, 0, dir.z).normalize();
    }

    /** Khoảng cách ngang từ (x, z) tới tuyến. */
    public static double distanceToRoute(BlockPos origin, double x, double z) {
        List<Vec3d> pts = polyline(origin);
        Vec3d p = new Vec3d(x, SURFACE_Y, z);
        double best = Double.MAX_VALUE;
        for (int i = 0; i < pts.size() - 1; i++) {
            best = Math.min(best, distanceToSegmentSq(p, pts.get(i), pts.get(i + 1)));
        }
        return Math.sqrt(best);
    }

    private static double distanceToSegmentSq(Vec3d p, Vec3d a, Vec3d b) {
        double abx = b.x - a.x;
        double abz = b.z - a.z;
        double len2 = abx * abx + abz * abz;
        double t = len2 < 1.0E-6 ? 0.0 : MathHelper.clamp(((p.x - a.x) * abx + (p.z - a.z) * abz) / len2, 0.0, 1.0);
        double cx = a.x + abx * t - p.x;
        double cz = a.z + abz * t - p.z;
        return cx * cx + cz * cz;
    }
}
