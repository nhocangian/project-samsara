package com.samsara.mixin;

import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Đọc bản nhạc nền đang phát, để StageAnomalyClient làm nó "giật" ở Stage 1+. */
@Mixin(MusicTracker.class)
public interface MusicTrackerAccessor {
    @Accessor("current")
    SoundInstance samsara$getCurrent();
}
