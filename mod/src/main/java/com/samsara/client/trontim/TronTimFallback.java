package com.samsara.client.trontim;

import net.minecraft.client.MinecraftClient;

/**
 * Tầng 3 "Trốn tìm" khi không có shaderpack Samsara (tắt Iris, hoặc dùng pack khác): shader world5 không chạy nên căn nhà
 * sẽ sáng trưng. Lúc đó mod tự làm tối: lightmap chỉ còn đúng ô sáng nhất (thứ EchoRenderer vẽ với ánh sáng tối đa),
 * mọi khối và thực thể ở mức sáng 0 thành đen; bỏ vẽ trời và mây. Khi pack Samsara đang chạy thì không đụng gì cả.
 */
public final class TronTimFallback {
    private TronTimFallback() {}

    private static long checkedAt = -1;
    private static boolean samsaraShaders = false;

    /** Đang ở tầng 3 và không có shaderpack Samsara: cần tự làm tối. */
    public static boolean active() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!EchoRenderer.inFloor(client)) return false;
        return !samsaraShadersOn();
    }

    /** Shaderpack Samsara có đang chạy không (hỏi Iris mỗi giây một lần). Tầng 4 cũng dùng để biết khi nào cần bản dự phòng. */
    public static boolean samsaraShadersOn() {
        long now = System.currentTimeMillis();
        if (now - checkedAt > 1000) {
            checkedAt = now;
            samsaraShaders = samsaraPackInUse();
        }
        return samsaraShaders;
    }

    /** Hỏi Iris (qua reflection, vì Iris không bắt buộc) xem có đang bật pack Samsara không. */
    private static boolean samsaraPackInUse() {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            boolean inUse = (Boolean) api.getMethod("isShaderPackInUse").invoke(instance);
            if (!inUse) return false;
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Object name = iris.getMethod("getCurrentPackName").invoke(null);
            return name != null && name.toString().toLowerCase().contains("samsara");
        } catch (Throwable t) {
            return false;
        }
    }
}
