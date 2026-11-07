package com.samsara.client.womb;

import com.samsara.client.WombRouteClient;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.system.womb.WombRoute;
import com.samsara.system.womb.chapters.SaveDoorLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Nhãn VHS dưới tâm ngắm máy quay ở The Womb. Mắt thường không thấy những dòng này.
 *  - Chương II: ngắm vào cánh cửa có tên -> "▶ <tên save>" / "Chơi lần cuối: <ngày>".
 *  - Chương III: ngắm vào khổng thần -> dòng thông báo chết của Minecraft (bản tiếng Việt gốc) / tên save và năm;
 *    ngắm vào một Tượng Quỳ -> một dòng thông báo chết (cố định theo vị trí tượng) / "kiếp #n".
 * Gọi từ CamcorderOverlay khi đã đưa máy lên mắt. Vị trí tính lại từ gốc tuyến, không cần gói tin.
 */
public final class SaveDoorLabels {
    private SaveDoorLabels() {}

    private static final double MAX_DISTANCE = 60.0;
    private static final int STATUE_DEATHS = 16;

    /** Khổng thần: khoá dòng thông báo chết, dòng phụ (tên save và năm), độ cao tâm ngắm so với mặt nước. */
    private static final Object[][] TITANS = {
            {WombRoute.Stop.TITAN_STEVE, "samsara.titan_death.steve", "World1  ·  2009", 14.0},
            {WombRoute.Stop.TITAN_CREEPER, "samsara.titan_death.creeper", "hầm mỏ  ·  09/2013", 4.0},
            {WombRoute.Stop.TITAN_ARSONIST, "samsara.titan_death.arsonist", "server với Tuấn  ·  07/2020", 22.0},
    };

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

        String line1 = null;
        String line2 = null;
        double bestScore = 0.0;

        // Tượng Quỳ: tia ngắm trúng đúng block tượng
        HitResult hit = client.world.raycast(new RaycastContext(eye, eye.add(look.multiply(MAX_DISTANCE)),
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK
                && client.world.getBlockState(bh.getBlockPos()).isOf(SamsaraBlocks.MEMORY_STATUE)) {
            BlockPos p = bh.getBlockPos();
            long h = MathHelper.hashCode(p.getX(), 0, p.getZ());
            line1 = I18n.translate("samsara.statue_death." + Math.floorMod(h, STATUE_DEATHS));
            line2 = I18n.translate("samsara.statue_death.kiep", 1000 + Math.floorMod(h >> 8, 9000));
            bestScore = 2.0; // tượng trúng tia ngắm luôn được ưu tiên
        }

        // Khổng thần
        for (Object[] t : TITANS) {
            BlockPos l = ((WombRoute.Stop) t[0]).landmark(origin);
            Vec3d center = new Vec3d(l.getX() + 0.5, WombRoute.SURFACE_Y + (double) t[3], l.getZ() + 0.5);
            double score = aimScore(eye, look, center, 10.0, 120.0);
            if (score > bestScore) {
                bestScore = score;
                line1 = I18n.translate((String) t[1]);
                line2 = (String) t[2];
            }
        }

        // Cửa có tên (chương II) và cánh cửa trắng
        for (SaveDoorLayout.NamedDoor door : SaveDoorLayout.named(origin)) {
            double score = aimScore(eye, look, Vec3d.ofBottomCenter(door.pos()).add(0, 1.0, 0), 1.4, MAX_DISTANCE);
            if (score > bestScore) {
                bestScore = score;
                line1 = "▶ " + door.name();
                line2 = lastPlayed(door.date());
            }
        }
        BlockPos giant = SaveDoorLayout.giant(origin);
        double giantScore = aimScore(eye, look, Vec3d.ofBottomCenter(giant).add(0, 6.0, 0), 6.0, MAX_DISTANCE);
        if (giantScore > bestScore) {
            line1 = "▶ " + SaveDoorLayout.GIANT_SAVE[0];
            line2 = lastPlayed(SaveDoorLayout.GIANT_SAVE[1]);
        }

        long time = client.world.getTime();
        if (time != lastTime) {
            lastTime = time;
            if (line1 != null && line1.equals(lastKey)) lockedTicks++;
            else lockedTicks = 0;
            lastKey = line1;
        }
        if (line1 == null) return;

        // Khoá mục tiêu như máy quay lấy nét: chữ chập chờn vài tick đầu rồi đứng yên
        float fade = MathHelper.clamp(lockedTicks / 8.0f, 0.0f, 1.0f);
        if (lockedTicks < 8 && (time % 3) == 0) return;
        int alpha = (int) (60 + 195 * fade);
        int w = Math.max(tr.getWidth(line1), tr.getWidth(line2)) + 8;
        int top = cy + 40;
        ctx.fill(cx - w / 2, top - 3, cx + w / 2, top + 21, (alpha / 3) << 24);
        ctx.drawTextWithShadow(tr, line1, cx - tr.getWidth(line1) / 2, top, (alpha << 24) | 0xF0F0F0);
        ctx.drawTextWithShadow(tr, line2, cx - tr.getWidth(line2) / 2, top + 10, (alpha << 24) | 0x9AA6B8);
    }

    private static String lastPlayed(String date) {
        return Formatting.strip(I18n.translate("samsara.spire_chapter.choi_lan_cuoi")) + " " + date;
    }

    /** 0 = không ngắm trúng; càng gần tâm ngắm càng cao. {@code radius} = bán kính "trúng" quanh tâm (block). */
    private static double aimScore(Vec3d eye, Vec3d look, Vec3d target, double radius, double maxDistance) {
        Vec3d d = target.subtract(eye);
        double dist = d.length();
        if (dist < 1.0 || dist > maxDistance) return 0.0;
        double cos = d.multiply(1.0 / dist).dotProduct(look);
        double tolerance = Math.cos(Math.max(Math.toRadians(2.5), Math.atan2(radius, dist)));
        if (cos < tolerance) return 0.0;
        return cos;
    }
}
