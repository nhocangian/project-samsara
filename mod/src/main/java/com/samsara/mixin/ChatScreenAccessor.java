package com.samsara.mixin;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Cho đoạn kết tự "gõ" chữ vào ô chat (EndingSequenceClient). */
@Mixin(ChatScreen.class)
public interface ChatScreenAccessor {
    @Accessor("chatField")
    TextFieldWidget samsara$getChatField();
}
