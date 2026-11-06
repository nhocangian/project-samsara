package com.samsara.worldgen.memory;

/**
 * Bố cục cố định của chiều "Ký Ức Của Nam" (samsara:nam_memory), dùng chung cho
 * SunflowerFieldFeature (khi sinh chunk) và NamHouseBuilder (khi dựng nhà).
 *
 *   - Thế giới phẳng: cỏ ở y = GROUND_Y, toàn bộ phủ kín hoa hướng dương.
 *   - Nhà Nam ở quanh (0, 0), cửa quay về phía nam (+Z).
 *   - Một con đường mòn duy nhất rộng 3 block chạy thẳng từ cửa nhà về +Z, tới vô tận.
 *   - Người chơi xuất hiện trên đường mòn tại z = ARRIVAL_Z, nhìn về phía nhà.
 */
public final class NamMemoryLayout {
    private NamMemoryLayout() {}

    /** Mặt cỏ (block cỏ nằm ở y này, hoa mọc ở y + 1 và y + 2). */
    public static final int GROUND_Y = 60;

    /** Tường trước của nhà (cửa nằm ở đây). */
    public static final int HOUSE_FRONT_Z = 4;
    /** Nửa bề rộng đường mòn (|x| <= PATH_HALF_WIDTH). */
    public static final int PATH_HALF_WIDTH = 1;
    /** Điểm xuất hiện trên đường mòn. */
    public static final int ARRIVAL_Z = 110;

    /** Khoảng sân trống quanh nhà và cối xay gió (không có hoa hướng dương). */
    public static boolean isClearing(int x, int z) {
        return x >= -8 && x <= 14 && z >= -8 && z <= 6;
    }

    public static boolean isPath(int x, int z) {
        return Math.abs(x) <= PATH_HALF_WIDTH && z > HOUSE_FRONT_Z;
    }
}
