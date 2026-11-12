package com.samsara.mixin;

import com.samsara.client.trontim.EchoRenderer;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tầng 3 "Trốn tìm": mỗi âm thanh phát ra tạo một vòng tiếng vọng (xem {@link EchoRenderer}). */
@Mixin(SoundSystem.class)
public class EchoSoundMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("TAIL"))
    private void samsara$echo(SoundInstance sound, CallbackInfo ci) {
        EchoRenderer.onSound(sound);
    }
}
