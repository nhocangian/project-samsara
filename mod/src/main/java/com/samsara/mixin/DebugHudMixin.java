package com.samsara.mixin;

import com.samsara.system.celestial.SolarFractureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Dòng F3 lạ (kinh dị tầng giao diện), chỉ hiện khi vỏ trứng đã bắt đầu nứt (Stage 1+):
 *  - Trái: số lớp vỏ đã nứt, và "số người đang xem" lớn hơn số người thật trong thế giới.
 *  - Phải: tên tệp save của người khác, ngày chơi cuối 05/2018.
 * Trông như thông số kỹ thuật bình thường, chỉ người để ý mới thấy.
 */
@Mixin(DebugHud.class)
public class DebugHudMixin {
    @Inject(method = "getLeftText", at = @At("RETURN"))
    private void samsara$leftLines(CallbackInfoReturnable<List<String>> cir) {
        int stage = SolarFractureManager.clientStage;
        if (stage < 1) return;
        List<String> lines = cir.getReturnValue();
        MinecraftClient client = MinecraftClient.getInstance();
        int players = client.getNetworkHandler() != null ? client.getNetworkHandler().getPlayerList().size() : 1;
        lines.add("");
        lines.add("Shell: " + stage + "/4 cracked");
        if (stage >= 2) lines.add("Viewers: " + (players + 1) + " (" + players + " present)");
    }

    @Inject(method = "getRightText", at = @At("RETURN"))
    private void samsara$rightLines(CallbackInfoReturnable<List<String>> cir) {
        if (SolarFractureManager.clientStage < 3) return;
        List<String> lines = cir.getReturnValue();
        lines.add("");
        lines.add("Save: Nam_1998/level.dat");
        lines.add("Last played: 05/2018");
    }
}
