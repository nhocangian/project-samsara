package com.samsara.mixin;

import com.samsara.system.celestial.SolarFractureManager;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Từ Stage 1, tiêu đề cửa sổ game đổi "Minecraft 1.20.1" thành "Minecraft 1.20.1-SAMSARA-REL":
 * bản build nguyền rủa không tồn tại trong tài liệu (mục I.2). Rất khẽ: chỉ ai nhìn thanh tiêu đề mới thấy.
 */
@Mixin(MinecraftClient.class)
public class WindowTitleMixin {
    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void samsara$cursedBuild(CallbackInfoReturnable<String> cir) {
        if (SolarFractureManager.clientStage < 1) return;
        String version = SharedConstants.getGameVersion().getName();
        String title = cir.getReturnValue();
        if (title != null && title.contains(version) && !title.contains("SAMSARA")) {
            cir.setReturnValue(title.replace(version, version + "-SAMSARA-REL"));
        }
    }
}
