package com.samsara.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Ô đang rê chuột của màn hình túi đồ / bàn chế tạo (tooltip giường ở tầng 4 "Đêm đầu tiên"). */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("focusedSlot")
    Slot samsara$getFocusedSlot();
}
