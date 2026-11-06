package com.samsara.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Lớp phủ giao diện Outlast Camcorder Viewfinder HUD khi người chơi đưa máy quay lên ngắm.
 * Bố cục được căn chỉnh pixel-perfect, không chồng lấn chữ:
 * - Góc trên trái: ● REC (hoặc ▶ PLAY khi phát băng) & chế độ quay / timecode.
 * - Góc trên phải: Cột pin Outlast [||||] căn lề phải chuẩn xác & pin dự phòng [R].
 * - Trung tâm: Khung chữ thập [ + ], 4 góc ngắm và Hộp phụ đề ký ức VCR (Phím B).
 * - Cạnh dưới: Thước đo sóng điện từ EMF & Cảnh báo dị thường khi bóng ma áp sát.
 * - Góc dưới cùng: Phím tắt điều khiển gọn gàng.
 */
public class CamcorderOverlay implements HudRenderCallback {

    public static void register() {
        HudRenderCallback.EVENT.register(new CamcorderOverlay());
    }

    /**
     * Nhiễu băng VHS khi bóng ma lại gần (thay cho các vạch trắng đặc trước đây). Tất cả đều mờ, không có trắng tinh:
     *  - Dải "tracking" lăn chậm từ trên xuống: một vệt tối chứa các mẩu nhiễu ngắn xám, lệch màu đỏ/xanh hai bên.
     *  - Hạt nhiễu li ti xám (không phải chấm trắng), càng gần càng dày.
     *  - Thỉnh thoảng một đường rách ngang mảnh màu tối, và một cái chớp mất tín hiệu rất ngắn.
     * Cường độ theo {@code strength} (0..1).
     */
    private static void drawVhsInterference(DrawContext ctx, int width, int height, float strength, long time, float tickDelta) {
        float t = time + tickDelta;
        java.util.Random r = new java.util.Random(time * 7919L + (long) (tickDelta * 3.0f));

        // 1. Dải tracking lăn
        int bandH = (int) (10 + 22 * strength);
        int span = height + bandH * 2;
        int bandY = (int) ((t * 1.6f) % span) - bandH;
        ctx.fill(0, bandY, width, bandY + bandH, ((int) (45 * strength) << 24));
        for (int y = bandY; y < bandY + bandH; y++) {
            if (y < 0 || y >= height) continue;
            int segments = 2 + r.nextInt(3 + (int) (strength * 6));
            for (int i = 0; i < segments; i++) {
                int x = r.nextInt(width);
                int len = 6 + r.nextInt(40);
                int g = 70 + r.nextInt(90);
                int a = (int) ((40 + r.nextInt(70)) * strength);
                int gray = (g << 16) | (g << 8) | g;
                ctx.fill(x, y, x + len, y + 1, (a << 24) | gray);
                ctx.fill(x - 2, y, x - 2 + len, y + 1, ((a / 3) << 24) | 0xFF3070);   // lệch đỏ
                ctx.fill(x + 2, y, x + 2 + len, y + 1, ((a / 3) << 24) | 0x30E0FF);   // lệch xanh
            }
        }

        // 2. Hạt nhiễu xám
        int grains = (int) (140 * strength);
        for (int i = 0; i < grains; i++) {
            int x = r.nextInt(width);
            int y = r.nextInt(height);
            int g = 90 + r.nextInt(110);
            int a = 20 + r.nextInt((int) (30 + 50 * strength));
            ctx.fill(x, y, x + 1, y + 1, (a << 24) | (g << 16) | (g << 8) | g);
        }

        // 3. Đường rách ngang mảnh, màu tối, lệch màu nhẹ
        if (r.nextFloat() < strength * 0.18f) {
            int y = r.nextInt(height);
            int x0 = r.nextInt(width / 2);
            int x1 = x0 + width / 3 + r.nextInt(width / 2);
            ctx.fill(x0, y, x1, y + 1, 0x70000000);
            ctx.fill(x0 + 3, y + 1, x1 + 3, y + 2, 0x2830E0FF);
        }

        // 4. Chớp mất tín hiệu rất ngắn
        if (r.nextFloat() < strength * 0.04f) {
            ctx.fill(0, 0, width, height, 0x30000000);
        }
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        // Chỉ hiển thị HUD máy quay khi người chơi đã đưa máy quay lên mặt hoàn toàn
        if (!CamcorderClientManager.isFullyAimed()) return;

        int width = drawContext.getScaledWindowWidth();
        int height = drawContext.getScaledWindowHeight();
        TextRenderer textRenderer = client.textRenderer;
        long time = client.world.getTime();

        int activeTape = CamcorderClientManager.getActiveTape();
        int playbackTicks = CamcorderClientManager.getPlaybackTicks();
        float ghostStrength = CamcorderClientManager.getGhostProximityStrength();

        int cx = width / 2;
        int cy = height / 2;

        // =========================================================================
        // HIỆU ỨNG NHIỄU SÓNG TỪ TRƯỜNG KHI BÓNG MA LẠI GẦN (MAGNETIC STATIC GLITCH)
        // =========================================================================
        if (ghostStrength > 0.05f) {
            drawVhsInterference(drawContext, width, height, ghostStrength, time, tickDelta);
        }

        // =========================================================================
        // 1. GÓC TRÊN TRÁI (TOP-LEFT): REC / PLAY & CHẾ ĐỘ QUAY (Safe margin x = 42)
        // Chừa khoảng trống an toàn x = 0..36 cho icon VoiceChat hoặc mod khác
        // Dòng 1: y = 14 | Dòng 2: y = 26 | Dòng 3: y = 38
        // =========================================================================
        long seconds = (time / 20) % 60;
        long minutes = (time / 1200) % 60;
        long hours = (time / 72000) % 24;
        String timecode = String.format("TC %02d:%02d:%02d:14", hours, minutes, seconds);

        int leftX = 42;

        if (activeTape > 0) {
            // Đang phát lại băng từ VCR
            boolean playBlink = (time % 16) < 11;
            int playColor = playBlink ? 0xFF55FFFF : 0x6655FFFF;
            drawContext.drawTextWithShadow(textRenderer, "▶ PLAY", leftX, 14, playColor);
            drawContext.drawTextWithShadow(textRenderer, "TAPE #0" + activeTape, leftX + 46, 14, 0xFFFFFFFF);
            drawContext.drawTextWithShadow(textRenderer, "◄◄ VCR REWIND  SP", leftX, 26, 0xFF88CCEE);
            drawContext.drawTextWithShadow(textRenderer, "24. OCT 1993", leftX, 38, 0xFF555555);
        } else {
            // Chế độ ghi hình bình thường (● REC)
            boolean recBlink = (time % 20) < 12;
            int recColor = recBlink ? 0xFFFF2222 : 0x44FF2222;
            drawContext.drawTextWithShadow(textRenderer, "●", leftX, 14, recColor);
            drawContext.drawTextWithShadow(textRenderer, "REC", leftX + 10, 14, 0xFFFFFFFF);
            drawContext.drawTextWithShadow(textRenderer, "SP", leftX + 34, 14, 0xFFAAAAAA);

            if (CamcorderClientManager.isNightVision()) {
                drawContext.drawTextWithShadow(textRenderer, "[IR NIGHT VISION ON]", leftX + 56, 14, 0xFF44FF66);
                drawContext.drawTextWithShadow(textRenderer, "GAIN: +18dB (INFRARED)", leftX, 26, 0xFF22AA44);
            } else {
                drawContext.drawTextWithShadow(textRenderer, "AUTO WB", leftX + 56, 14, 0xFF888888);
                drawContext.drawTextWithShadow(textRenderer, timecode, leftX, 26, 0xFF666666);
            }
            drawContext.drawTextWithShadow(textRenderer, "24. OCT 1993", leftX, 38, 0xFF555555);
        }

        // =========================================================================
        // 2. GÓC TRÊN PHẢI (TOP-RIGHT): CỘT PIN OUTLAST & PHÍM TẮT ĐIỀU KHIỂN
        // Căn lề phải chuẩn xác (margin = 24px)
        // Dòng 1: y = 14 | Dòng 2: y = 26 | Dòng 3: y = 38
        // =========================================================================
        float batt = CamcorderClientManager.getBatteryLevel();
        int spares = CamcorderClientManager.getSpareBatteryCount(client.player);

        String barStr;
        int barColor;
        if (batt >= 75.0f) {
            barStr = "||||";
            barColor = 0xFF44FF66;
        } else if (batt >= 50.0f) {
            barStr = "||| ";
            barColor = 0xFF88EE44;
        } else if (batt >= 25.0f) {
            barStr = "||  ";
            barColor = 0xFFFFBB22;
        } else if (batt > 0.0f) {
            barStr = "|   ";
            boolean lowBlink = (time % 14) < 8;
            barColor = lowBlink ? 0xFFFF3333 : 0x44FF3333;
        } else {
            barStr = "EMPTY";
            boolean emptyBlink = (time % 10) < 5;
            barColor = emptyBlink ? 0xFFFF1111 : 0x33FF1111;
        }

        String fullBattText = "BATT [" + barStr + "]";
        int battWidth = textRenderer.getWidth(fullBattText);
        int battX = width - battWidth - 24;

        drawContext.drawTextWithShadow(textRenderer, "BATT [", battX, 14, 0xFFFFFFFF);
        drawContext.drawTextWithShadow(textRenderer, barStr, battX + textRenderer.getWidth("BATT ["), 14, barColor);
        drawContext.drawTextWithShadow(textRenderer, "]", battX + textRenderer.getWidth("BATT [" + barStr), 14, 0xFFFFFFFF);

        String spareText = "SPARE: x" + spares + " [R]";
        int spareWidth = textRenderer.getWidth(spareText);
        int spareColor = spares > 0 ? 0xFFAAAAAA : 0xFFFF6666;
        drawContext.drawTextWithShadow(textRenderer, spareText, width - spareWidth - 24, 26, spareColor);

        // Phím tắt điều khiển đặt gọn gàng ở góc trên phải, không đè lên thanh Hotbar ở đáy!
        String keysHint = net.minecraft.client.resource.language.I18n.translate("samsara.camcorder_overlay.v_nhin_dem_b");
        int keysWidth = textRenderer.getWidth(keysHint);
        drawContext.drawTextWithShadow(textRenderer, keysHint, width - keysWidth - 24, 38, 0xFF666666);

        // =========================================================================
        // 3. ĐỈNH TRUNG TÂM (TOP-CENTER): THƯỚC ĐO SÓNG ĐIỆN TỪ EMF
        // Đặt ở đỉnh màn hình giữa 2 cụm trái/phải, không chạm bất kỳ thanh máu hay hotbar nào!
        // =========================================================================
        if (ghostStrength > 0.05f) {
            int emfBars = (int) (ghostStrength * 10.0f);
            StringBuilder emfVisual = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                if (i < emfBars) emfVisual.append("■");
                else emfVisual.append("□");
            }

            int emfColor;
            String emfStatus;
            if (ghostStrength < 0.35f) {
                emfColor = 0xFF44FF66; // Xanh: Ở xa (>14 blocks)
                emfStatus = "WEAK DISTORTION";
            } else if (ghostStrength < 0.70f) {
                emfColor = 0xFFFFBB22; // Vàng: Đang tiếp cận (8-14 blocks)
                emfStatus = "APPROACHING";
            } else {
                boolean dangerBlink = (time % 6) < 4;
                emfColor = dangerBlink ? 0xFFFF2222 : 0x55FF2222; // Đỏ: RẤT GẦN (<8 blocks)
                emfStatus = "CRITICAL PROXIMITY!";
            }

            String emfLine = "EMF [" + emfVisual + "] " + emfStatus;
            int emfWidth = textRenderer.getWidth(emfLine);
            drawContext.drawTextWithShadow(textRenderer, emfLine, cx - (emfWidth / 2), 14, emfColor);

            if (ghostStrength >= 0.35f) {
                String warningText = net.minecraft.client.resource.language.I18n.translate("samsara.camcorder_overlay.di_thuong_tu_truong");
                int warnWidth = textRenderer.getWidth(warningText);
                drawContext.drawTextWithShadow(textRenderer, warningText, cx - (warnWidth / 2), 25, 0xFFFFAA33);
            }
        }

        // =========================================================================
        // 4. KHUNG CHỮ THẬP [ + ] & 4 GÓC NGẮM (CAMCORDER BRACKETS)
        // =========================================================================
        drawContext.drawText(textRenderer, "+", cx - 3, cy - 4, 0x55FFFFFF, false);
        // Nhãn save file khi ngắm vào cánh cửa có tên ở chương II của The Womb
        com.samsara.client.womb.SaveDoorLabels.draw(drawContext, textRenderer, cx, cy);

        int bSize = 32;
        int bThick = 1;
        int bColor = 0x44FFFFFF;

        // Góc trên-trái
        drawContext.fill(cx - bSize, cy - bSize, cx - bSize + 10, cy - bSize + bThick, bColor);
        drawContext.fill(cx - bSize, cy - bSize, cx - bSize + bThick, cy - bSize + 10, bColor);

        // Góc trên-phải
        drawContext.fill(cx + bSize - 10, cy - bSize, cx + bSize, cy - bSize + bThick, bColor);
        drawContext.fill(cx + bSize - bThick, cy - bSize, cx + bSize, cy - bSize + 10, bColor);

        // Góc dưới-trái
        drawContext.fill(cx - bSize, cy + bSize - bThick, cx - bSize + 10, cy + bSize, bColor);
        drawContext.fill(cx - bSize, cy + bSize - 10, cx - bSize + bThick, cy + bSize, bColor);

        // Góc dưới-phải
        drawContext.fill(cx + bSize - 10, cy + bSize - bThick, cx + bSize, cy + bSize, bColor);
        drawContext.fill(cx + bSize - bThick, cy + bSize - 10, cx + bSize, cy + bSize, bColor);

        // =========================================================================
        // 5. HỘP PHỤ ĐỀ KÝ ỨC VCR (TAPE PLAYBACK SUBTITLES)
        // Bắt đầu tại (cy + 36) - nằm dưới khung ngắm trung tâm, cách xa hotbar và không bị đè chữ
        // =========================================================================
        if (activeTape > 0) {
            // Dải sóng tracking cuộn màn hình
            int scanY = (int) ((playbackTicks * 2) % height);
            drawContext.fill(0, scanY, width, scanY + 1, 0x3355FFFF);

            String title;
            String quote1;
            String quote2;
            int titleColor;

            if (activeTape == 1) {
                title = net.minecraft.client.resource.language.I18n.translate("samsara.camcorder_overlay.tape_01_vet_tu");
                quote1 = net.minecraft.client.resource.language.I18n.translate("samsara.camcorder_overlay.cau_co_nhan_ra");
                quote2 = net.minecraft.client.resource.language.I18n.translate("samsara.camcorder_overlay.hoi_am_tat_dan");
                titleColor = 0xFFFFAA44;
            } else if (activeTape == 2) {
                title = net.minecraft.client.resource.language.I18n.translate("samsara.camcorder_overlay.tape_02_ban_ghi");
                quote1 = net.minecraft.client.resource.language.I18n.translate("samsara.camcorder_overlay.no_khong_he_tan");
                quote2 = net.minecraft.client.resource.language.I18n.translate("samsara.camcorder_overlay.cau_da_chon_bao");
                titleColor = 0xFFFF5555;
            } else {
                title = net.minecraft.client.resource.language.I18n.translate("samsara.camcorder_overlay.tape_03_tan_so");
                quote1 = net.minecraft.client.resource.language.I18n.translate("samsara.camcorder_overlay.dung_nhin_vao_mat");
                quote2 = net.minecraft.client.resource.language.I18n.translate("samsara.camcorder_overlay.tat_may_quay_di");
                titleColor = 0xFFDD55FF;
            }

            String hint = net.minecraft.client.resource.language.I18n.translate("samsara.camcorder_overlay.phim_b_chuyen_tape");
            int w1 = textRenderer.getWidth(quote1);
            int w2 = textRenderer.getWidth(quote2);
            int titleW = textRenderer.getWidth(title);
            int hintW = textRenderer.getWidth(hint);
            int maxTextW = Math.max(Math.max(w1, w2), Math.max(hintW, titleW));

            int boxW = maxTextW + 36;
            int boxH = 64;
            int boxY = cy + 36;
            int boxX = cx - (boxW / 2);

            // Nền đen bán trong suốt với viền trên cyan VCR và viền dưới tinh tế
            drawContext.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xB0080808);
            drawContext.fill(boxX, boxY, boxX + boxW, boxY + 1, 0xAA55FFFF);
            drawContext.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, 0x3355FFFF);

            // Các dòng chữ giãn cách đều đặn, căn giữa hoàn hảo
            drawContext.drawTextWithShadow(textRenderer, title, cx - (titleW / 2), boxY + 6, titleColor);
            drawContext.drawTextWithShadow(textRenderer, quote1, cx - (w1 / 2), boxY + 20, 0xFFFFFFFF);
            drawContext.drawTextWithShadow(textRenderer, quote2, cx - (w2 / 2), boxY + 33, 0xFFAAAAAA);
            drawContext.drawTextWithShadow(textRenderer, hint, cx - (hintW / 2), boxY + 48, 0xFF55FFFF);
        }

        // =========================================================================
        // 6. GARGANTUA BEACON & SPECTRAL ARG DECODER (CHIỀU KHÔNG GIAN THE WOMB)
        // CHỈ HIỆN KHI BẬT CHẾ ĐỘ NHÌN ĐÊM HỒNG NGOẠI (NIGHT VISION / IR - PHÍM V)
        // =========================================================================
        if (client.world.getRegistryKey().equals(com.samsara.registry.SamsaraDimensions.THE_WOMB_WORLD_KEY)
                && CamcorderClientManager.isNightVision()) {
            float pitch = client.player.getPitch();
            float yaw = MathHelper.wrapDegrees(client.player.getYaw());

            // Nhìn về phía Hắc Lỗ Gargantua ở bầu trời phía Bắc (North: yaw ~ 180 hoặc -180, pitch < -10)
            boolean lookingAtGargantua = (pitch < -12.0f && Math.abs(yaw) > 135.0f);

            if (lookingAtGargantua) {
                // Nhấp nháy sóng mang vô tuyến Gargantua 0.00Hz (Đặt ở phía trên cao, KHÔNG che tâm ngắm)
                boolean carrierBlink = (time % 8) < 5;
                int cColor = carrierBlink ? 0xFF00FFCC : 0x4400FFCC;

                int boxW = 280;
                int boxH = 34;
                int boxX = cx - (boxW / 2);
                int boxY = 46;

                drawContext.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xAA040C14);
                drawContext.fill(boxX, boxY, boxX + boxW, boxY + 1, 0xCC00FFCC);
                drawContext.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, 0x4400FFCC);

                String line1 = "▲ GARGANTUA BEACON [CARRIER: 0.00 Hz // GRAVITATIONAL WAVE]";
                String line2 = "SIGNAL: ··· ·- -- ··· ·- ·-· ·- // [CARRIER ACTIVE]";

                drawContext.drawTextWithShadow(textRenderer, line1, cx - (textRenderer.getWidth(line1) / 2), boxY + 6, cColor);
                drawContext.drawTextWithShadow(textRenderer, line2, cx - (textRenderer.getWidth(line2) / 2), boxY + 18, 0xFFFFAA33);

                // Thêm sọc quét nhiễu tím đặc trưng của Hắc Lỗ
                int gScanY = (int) ((time * 4) % height);
                drawContext.fill(0, gScanY, width, gScanY + 2, 0x33AA00FF);
            } else {
                // Quét quang học chính xác: Chỉ kích hoạt khi tâm ngắm máy quay thực sự nhìn vào Bia Đá / Cự Thạch nổi
                BlockPos targetRuinPos = findTargetRuin(client, tickDelta, 120.0);

                if (targetRuinPos != null) {
                    Vec3d eyePos = client.player.getCameraPosVec(tickDelta);
                    double dist = eyePos.distanceTo(Vec3d.ofCenter(targetRuinPos));
                    int distMeters = (int) Math.round(dist);

                    int ruinCellX = Math.floorDiv(targetRuinPos.getX(), 120);
                    int ruinCellZ = Math.floorDiv(targetRuinPos.getZ(), 120);
                    int cipherIndex = Math.floorMod(ruinCellX * 31 + ruinCellZ * 17, 4);
                    String archiveId = String.format("%02X-%02X", Math.abs(ruinCellX) % 256, Math.abs(ruinCellZ) % 256);

                    String hexString;
                    String checksum;
                    if (cipherIndex == 0) {
                        hexString = "46 49 4E 44 20 59 4F 55 52 20 4F 57 4E 20 47 52 41 56 45";
                        checksum = "CRC-32: 0x8F4A1C";
                    } else if (cipherIndex == 1) {
                        hexString = "44 4F 20 4E 4F 54 20 54 52 55 53 54 20 54 48 45 20 4D 49 52 52 4F 52";
                        checksum = "CRC-32: 0x3E7B92";
                    } else if (cipherIndex == 2) {
                        hexString = "57 45 20 57 45 52 45 20 4E 45 56 45 52 20 42 4F 52 4E";
                        checksum = "CRC-32: 0x5C22D0";
                    } else {
                        hexString = "54 48 45 20 43 59 43 4C 45 20 48 41 53 20 4E 4F 20 45 4E 44";
                        checksum = "CRC-32: 0x1A99FE";
                    }

                    String archTitle = "[OPTICAL LOCK // REMNANT-" + archiveId + " // " + distMeters + "m]";
                    String archCipher = "HEX: " + hexString;
                    String archStatus = "STATUS: ENCRYPTED // " + checksum;

                    // Đo bề rộng thực tế của văn bản để hộp HUD luôn bao phủ hoàn hảo, không bị tràn lề
                    int titleW = textRenderer.getWidth(archTitle);
                    int cipherW = textRenderer.getWidth(archCipher);
                    int statusW = textRenderer.getWidth(archStatus);
                    int maxW = Math.max(Math.max(titleW, cipherW), statusW);

                    int boxW = maxW + 28;
                    int boxH = 42;
                    int boxX = cx - (boxW / 2);
                    int boxY = height - 68; // Đặt ở đáy màn hình trên thanh hotbar, để trống toàn bộ tâm ngắm

                    drawContext.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xCC051008);
                    drawContext.fill(boxX, boxY, boxX + boxW, boxY + 1, 0xCC44FF66);
                    drawContext.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, 0x4444FF66);

                    drawContext.drawTextWithShadow(textRenderer, archTitle, cx - (titleW / 2), boxY + 5, 0xFF44FF66);
                    drawContext.drawTextWithShadow(textRenderer, archCipher, cx - (cipherW / 2), boxY + 17, 0xFFEEEEEE);
                    drawContext.drawTextWithShadow(textRenderer, archStatus, cx - (statusW / 2), boxY + 29, 0xFF77AA77);
                }
            }
        }
    }

    /**
     * Nhận diện các khối kiến trúc cự thạch, bia đá và phế tích cổ xưa trong The Womb.
     * TUYỆT ĐỐI BỎ QUA lớp đá nền tự nhiên (Y <= 61) của đáy biển.
     */
    public static boolean isRuinBlock(BlockPos pos, BlockState state) {
        if (state == null || state.isAir()) return false;
        Block block = state.getBlock();
        String path = Registries.BLOCK.getId(block).getPath();

        // 1. Nước, đá nền bedrock -> Bỏ qua
        if (path.equals("water") || path.equals("bedrock")) {
            return false;
        }

        // 2. Khối xây dựng nhân tạo đặc trưng của phế tích (luôn hợp lệ dù ở độ cao nào)
        boolean isCraftedStructureBlock = path.contains("brick") // polished_blackstone_bricks, deepslate_bricks, stone_bricks
                || path.contains("tiles")       // deepslate_tiles
                || path.contains("chiseled")     // chiseled_polished_blackstone, chiseled_quartz
                || path.contains("reinforced")   // reinforced_deepslate
                || path.contains("lantern")      // sea_lantern, soul_lantern
                || path.contains("prismarine")   // dark_prismarine, prismarine_bricks
                || path.contains("quartz")       // quartz_block, quartz_pillar
                || path.contains("copper")       // cut_copper, oxidized_copper
                || path.contains("chain")        // chain
                || path.contains("chest")        // chest
                || path.contains("lodestone");   // lodestone

        if (isCraftedStructureBlock) {
            return true;
        }

        // 3. Các khối đá tự nhiên nhưng được tạo hình cự thạch (calcite, bone_block, basalt, diorite, crying_obsidian, polished_blackstone, amethyst):
        // CHỈ HỢP LỆ KHI LÀ KIẾN TRÚC NHÔ CAO KHỎI MẶT NƯỚC (Y >= 63)!
        // Đáy biển The Womb luôn nằm ở Y <= 61 (mặt nước Y = 62), nên mọi khối đá tự nhiên ở Y <= 61 đều là đáy biển.
        if (pos.getY() >= 63) {
            return path.contains("blackstone")
                    || path.contains("deepslate")
                    || path.contains("obsidian")
                    || path.contains("calcite")
                    || path.contains("bone_block")
                    || path.contains("diorite")
                    || path.contains("basalt")
                    || path.contains("amethyst");
        }

        return false;
    }

    /**
     * Dò tìm phế tích / cự thạch theo hướng nhìn (Raycast) trực tiếp của người chơi.
     */
    public static BlockPos findTargetRuin(MinecraftClient client, float tickDelta, double maxDistance) {
        if (client.world == null || client.player == null) return null;

        Vec3d eyePos = client.player.getCameraPosVec(tickDelta);
        Vec3d lookVec = client.player.getRotationVec(tickDelta);
        Vec3d endVec = eyePos.add(lookVec.multiply(maxDistance));

        // 1. Raycast chính xuyên qua không gian (FluidHandling.NONE để bỏ qua nước biển)
        BlockHitResult hit = client.world.raycast(new RaycastContext(
                eyePos,
                endVec,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = hit.getBlockPos();
            if (isRuinBlock(hitPos, client.world.getBlockState(hitPos))) {
                return hitPos;
            }
            // Nếu trúng gần một khối kiến trúc nổi (kiểm tra các block từ Y >= 63 trở lên)
            for (int dy = 1; dy <= 3; dy++) {
                BlockPos above = hitPos.up(dy);
                if (above.getY() >= 63 && isRuinBlock(above, client.world.getBlockState(above))) {
                    return above;
                }
            }
        }

        // 2. Quét mẫu dọc theo tia nhìn từ d = 6.0m đến maxDistance (bắt các cột đá cao, xích cự thạch, đỉnh tháp ở Y >= 63)
        for (double d = 6.0; d <= maxDistance; d += 3.0) {
            Vec3d sample = eyePos.add(lookVec.multiply(d));
            BlockPos samplePos = BlockPos.ofFloored(sample);
            if (isRuinBlock(samplePos, client.world.getBlockState(samplePos))) {
                return samplePos;
            }
            BlockPos up = samplePos.up();
            if (isRuinBlock(up, client.world.getBlockState(up))) return up;
        }

        return null;
    }
}
