package com.samsara.mixin;

import com.samsara.client.sound.ReversedSounds;
import net.minecraft.client.sound.SoundLoader;
import net.minecraft.client.sound.StaticSound;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Âm {@code samsara:sounds/reversed/...}: nạp file vanilla tương ứng rồi phát ngược (xem {@link ReversedSounds}).
 * Kết quả vẫn nằm trong bộ nhớ đệm {@code loadedSounds} của game, nên được giải phóng như mọi âm khác.
 */
@Mixin(SoundLoader.class)
public abstract class SoundLoaderReverseMixin {
    @Shadow @Final private ResourceFactory resourceFactory;
    @Shadow @Final private Map<Identifier, CompletableFuture<StaticSound>> loadedSounds;

    @Inject(method = "loadStatic(Lnet/minecraft/util/Identifier;)Ljava/util/concurrent/CompletableFuture;",
            at = @At("HEAD"), cancellable = true)
    private void samsara$loadReversed(Identifier id, CallbackInfoReturnable<CompletableFuture<StaticSound>> cir) {
        Identifier source = ReversedSounds.sourceOf(id);
        if (source == null) return;
        cir.setReturnValue(this.loadedSounds.computeIfAbsent(id, key -> CompletableFuture.supplyAsync(() -> {
            try {
                return ReversedSounds.load(this.resourceFactory, source);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, Util.getMainWorkerExecutor())));
    }
}
