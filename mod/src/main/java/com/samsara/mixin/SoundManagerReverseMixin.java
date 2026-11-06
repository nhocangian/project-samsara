package com.samsara.mixin;

import com.samsara.client.sound.ReversedSounds;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Khi nạp {@code sounds.json}, game bỏ qua âm nào không có file. Âm tua ngược không có file riêng,
 * nên kiểm tra file gốc thay thế (xem {@link ReversedSounds}).
 */
@Mixin(SoundManager.class)
public abstract class SoundManagerReverseMixin {
    @Inject(method = "isSoundResourcePresent", at = @At("HEAD"), cancellable = true)
    private static void samsara$reversedPresent(Sound sound, Identifier id, ResourceFactory factory,
                                                CallbackInfoReturnable<Boolean> cir) {
        Identifier source = ReversedSounds.sourceOf(sound.getLocation());
        if (source != null) cir.setReturnValue(factory.getResource(source).isPresent());
    }
}
