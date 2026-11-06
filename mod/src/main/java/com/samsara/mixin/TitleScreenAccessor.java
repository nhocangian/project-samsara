package com.samsara.mixin;

import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Đổi dòng chữ vàng nhảy ở menu chính sau đoạn kết. */
@Mixin(TitleScreen.class)
public interface TitleScreenAccessor {
    @Accessor("splashText")
    void samsara$setSplashText(SplashTextRenderer splash);
}
