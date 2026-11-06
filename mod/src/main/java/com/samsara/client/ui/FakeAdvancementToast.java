package com.samsara.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Toast "Thành tựu đã đạt được!" giả, vẽ giống hệt toast thành tựu vanilla (khung, biểu tượng, chữ vàng),
 * nhưng tên thành tựu bị nhiễu ký tự trong nửa giây đầu và thỉnh thoảng giật lại sau đó.
 */
@Environment(EnvType.CLIENT)
public class FakeAdvancementToast implements Toast {
    private static final long DURATION_MS = 5000L;

    private final String title;
    private final ItemStack icon;

    public FakeAdvancementToast(String title, ItemStack icon) {
        this.title = title;
        this.icon = icon;
    }

    @Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        context.drawTexture(TEXTURE, 0, 0, 0, 0, getWidth(), getHeight());
        TextRenderer text = manager.getClient().textRenderer;

        context.drawText(text, Text.translatable("advancements.toast.task"), 30, 7, 0xFFFF00, false);
        // Tên bị nhiễu lúc mới hiện, sau đó thỉnh thoảng giật lại một khung
        boolean glitch = startTime < 600L || (startTime / 90L) % 23 == 0;
        Text name = glitch ? Text.translatable(title).formatted(net.minecraft.util.Formatting.OBFUSCATED) : Text.translatable(title);
        context.drawText(text, name, 30, 18, 0xFFFFFF, false);

        context.drawItemWithoutEntity(icon, 8, 8);
        return startTime >= DURATION_MS ? Visibility.HIDE : Visibility.SHOW;
    }
}
