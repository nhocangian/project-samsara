package com.samsara.mixin;

import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Cho cột sóng của Nam_1998 trong bảng Tab chập chờn. */
@Mixin(PlayerListEntry.class)
public interface PlayerListEntryInvoker {
    @Invoker("setLatency")
    void samsara$setLatency(int latency);
}
