package com.samsara.client.womb;

import com.samsara.client.WombRouteClient;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.system.womb.chapters.SaveDoorLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Nhãn save file trên màn hình máy quay: mắt thường chỉ thấy những cánh cửa giống hệt nhau; ngắm máy quay vào một
 * cánh cửa có tên thì dưới tâm ngắm hiện nhãn kiểu băng VHS (tên thế giới, lần chơi cuối). Gọi từ CamcorderOverlay
 * khi đã đưa máy lên mắt. Vị trí cửa tính lại từ gốc tuyến (SaveDoorLayout), không cần gói tin.
 */
public final class SaveDoorLabels {
    private SaveDoorLabels() {}

    private static final double MAX_DISTANCE = 60.0;

    private static String lastKey = null;
    private static int lockedTicks = 0;
    private static long lastTime = -1;

    public static void draw(DrawContext ctx, TextRenderer tr, int cx, int cy) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (!client.world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY)) return;
        BlockPos origin = WombRouteClient.getOrigin();
        if (origin == null) return;

        Vec3d eye = client.gameRenderer.getCamera().getPos();
        Vec3d look = client.player.getRotationVec(1.0f);

        String bestName = null;
        String bestDate = null;
        double bestScore = 0.0;
        for (SaveDoorLayout.NamedDoor door : SaveDoorLayout.named(origin)) {
            double score = aimScore(eye, look, Vec3d.ofBottomCenter(door.pos()).add(0, 1.0, 0), 1.4);
            if (score > bestScore) {
                bestScore = score;
                bestName = door.name();
                bestDate = door.date();
            }
        }
        BlockPos giant = SaveDoorLayout.giant(origin);
        double giantScore = aimScore(eye, look, Vec3d.ofBottomCenter(giant).add(0, 6.0, 0), 6.0);
        if (giantScore > bestScore) {
            bestName = SaveDoorLayout.GIANT_SAVE[0];
            bestDate = SaveDoorLayout.GIANT_SAVE[1];
        }

        long time = client.world.getTime();
        if (time != lastTime) {
            lastTime = time;
            if (bestName != null && bestName.equals(lastKey)) lockedTicks++;
            else lockedTicks = 0;
            lastKey = bestName;
        }
        if (bestName == null) return;

        // Khoá mục tiêu như máy quay lấy nét: chữ chập chờn vài tick đầu rồi đứng yên
        float fade = MathHelper.clamp(lockedTicks / 8.0f, 0.0f, 1.0f);
        if (lockedTicks < 8 && (time % 3) == 0) return;
        int alpha = (int) (60 + 195 * fade);
        String line1 = "▶ " + bestName;
        String line2 = Formatting.strip(I18n.translate("samsara.spire_chapter.choi_lan_cuoi")) + " " + bestDate;
        int w = Math.max(tr.getWidth(line1), tr.getWidth(line2)) + 8;
        int top = cy + 40;
        ctx.fill(cx - w / 2, top - 3, cx + w / 2, top + 21, (alpha / 3) << 24);
        ctx.drawTextWithShadow(tr, line1, cx - tr.getWidth(line1) / 2, top, (alpha << 24) | 0xF0F0F0);
        ctx.drawTextWithShadow(tr, line2, cx - tr.getWidth(line2) / 2, top + 10, (alpha << 24) | 0x9AA6B8);
    }

    /** 0 = không ngắm trúng; càng gần tâm ngắm càng cao. {@code radius} = bán kính "trúng" quanh tâm cửa (block). */
    private static double aimScore(Vec3d eye, Vec3d look, Vec3d target, double radius) {
        Vec3d d = target.subtract(eye);
        double dist = d.length();
        if (dist < 1.0 || dist > MAX_DISTANCE) return 0.0;
        double cos = d.multiply(1.0 / dist).dotProduct(look);
        double tolerance = Math.cos(Math.max(Math.toRadians(2.5), Math.atan2(radius, dist)));
        if (cos < tolerance) return 0.0;
        return cos;
    }
}
