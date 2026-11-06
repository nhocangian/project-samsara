package com.samsara.system.womb.chapters;

import com.samsara.system.womb.WombRoute;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Bố cục "căn phòng cửa" (chương I + II), chỉ tính toán: dùng chung cho server dựng và client hiện nhãn qua máy quay.
 *
 * Bến Đò và biển cửa nằm trong một căn phòng khổng lồ bằng Vách Hư Không đen tuyền (tường bốn phía + trần), nên
 * người mới tới chỉ thấy bóng tối và cửa, không thấy bầu trời hay Gargantua. Lối ra duy nhất là cánh cửa trắng khổng
 * lồ ở tường bắc, ngay trên tuyến; bước qua nó mới thấy vũ trụ.
 *
 * Đoạn tuyến Bến Đò -> Steve 2009 chạy thẳng về phía bắc (-Z) ở x = origin.x, nên lưới cửa thẳng trục. "along" =
 * quãng đường dọc tuyến tính từ Bến Đò (âm = phía nam bến). Mọi cánh cửa quay mặt về phía nam.
 */
public final class SaveDoorLayout {
    private SaveDoorLayout() {}

    // ---------- Căn phòng ----------
    /** Tường nam (along âm) và tường bắc (= chỗ cánh cửa khổng lồ). */
    public static final int ROOM_SOUTH = -40;
    public static final int GIANT_ALONG = 93;
    public static final int ROOM_NORTH = GIANT_ALONG;
    public static final int ROOM_HALF_WIDTH = 100;
    /** Trần cao hơn mặt nước bao nhiêu block. */
    public static final int ROOM_HEIGHT = 40;
    /** Lỗ ở tường bắc cho cánh cửa khổng lồ (theo block, tính từ tuyến / từ mặt nước). */
    public static final int GATE_HALF_WIDTH = 3;
    public static final int GATE_HEIGHT = 13;

    // ---------- Lưới cửa ----------
    public static final int ROW = 3;
    public static final int COL = 3;
    /** Hàng đầu (phía nam) và hàng cuối (sát tường bắc). */
    public static final int FIELD_FIRST = ROOM_SOUTH + 4;
    public static final int FIELD_LAST = GIANT_ALONG - 5;
    /** Cột gần tuyến nhất và xa nhất. */
    public static final int INNER = 6;
    public static final int OUTER = ROOM_HALF_WIDTH - 5;
    /** Chừa trống quanh Bến Đò (theo |dx| và along). */
    public static final int DOCK_HALF_WIDTH = 4;
    public static final int DOCK_SOUTH = -12;
    public static final int DOCK_NORTH = 12;

    /** Mốc chương II (khớp WombPacingSystem.DOORS_START): chỉ để tham chiếu. */
    public static final int FIELD_START = 16;

    /**
     * Save file theo thứ tự từ mới nhất (cạnh Bến Đò) tới cũ hơn, mỗi 6 block dọc tuyến một cửa, so le đông/tây,
     * ở cột sát tuyến. Mắt thường không thấy tên: chỉ nhãn VHS trên máy quay.
     * World1 (2009), thế giới đầu tiên, là cánh cửa khổng lồ: đi qua nó là gặp Steve 2009.
     */
    public static final String[][] SAVES = {
            {"New World (14)", "03/2021"},
            {"Survival - đừng ngủ", "02/2021"},
            {"New World (13)", "11/2020"},
            {"server với Tuấn", "07/2020"},
            {"Hardcore", "01/2020"},
            {"Nhà Nam", "05/2018"},
            {"Sáng tạo-KHÔNG XÓA", "08/2016"},
            {"Copy of New World", "12/2015"},
            {"Survival 2014", "06/2014"},
            {"hầm mỏ", "09/2013"},
            {"Thế giới của em", "04/2012"},
            {"New World", "10/2011"},
            {"Beta 1.7.3", "2011"},
            {"World2", "2010"},
            {"Alpha 1.2.6", "2010"},
    };
    public static final String[] GIANT_SAVE = {"World1", "2009"};
    private static final int NAMED_FIRST = 0;
    private static final int NAMED_STEP = 6;

    public record NamedDoor(BlockPos pos, String name, String date) {}

    /** Chân cửa (nửa dưới) tại quãng đường {@code along}, lệch {@code dx} block so với tuyến. */
    public static BlockPos doorPos(BlockPos origin, int along, int dx) {
        return new BlockPos(origin.getX() + dx, WombRoute.SURFACE_Y, origin.getZ() - along);
    }

    public static List<NamedDoor> named(BlockPos origin) {
        List<NamedDoor> out = new ArrayList<>();
        for (int i = 0; i < SAVES.length; i++) {
            int along = NAMED_FIRST + i * NAMED_STEP;
            out.add(new NamedDoor(doorPos(origin, along, namedSide(i) * INNER), SAVES[i][0], SAVES[i][1]));
        }
        return out;
    }

    private static int namedSide(int i) {
        return i % 2 == 0 ? 1 : -1;
    }

    public static boolean isNamed(int along, int dx) {
        if (along < NAMED_FIRST || (along - NAMED_FIRST) % NAMED_STEP != 0) return false;
        int i = (along - NAMED_FIRST) / NAMED_STEP;
        return i < SAVES.length && dx == namedSide(i) * INNER;
    }

    /** Ô lưới (along, dx) có được đặt cửa không (chừa hành lang tuyến và Bến Đò). */
    public static boolean isDoorCell(int along, int dx) {
        int ax = Math.abs(dx);
        if (ax < INNER || ax > OUTER) return false;
        return !(ax <= DOCK_HALF_WIDTH + 4 && along >= DOCK_SOUTH && along <= DOCK_NORTH) || isNamed(along, dx);
    }

    /** Chân cánh cửa khổng lồ (giữa tuyến, trong tường bắc). */
    public static BlockPos giant(BlockPos origin) {
        return new BlockPos(origin.getX(), WombRoute.SURFACE_Y, origin.getZ() - GIANT_ALONG);
    }

    /** (x, z) có nằm trong căn phòng (nới thêm {@code margin} block) không. */
    public static boolean insideRoom(BlockPos origin, int x, int z, int margin) {
        int dx = x - origin.getX();
        int along = origin.getZ() - z;
        return Math.abs(dx) <= ROOM_HALF_WIDTH + margin && along >= ROOM_SOUTH - margin && along <= ROOM_NORTH + margin;
    }
}
