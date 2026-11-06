package com.samsara.mixin;

import com.samsara.client.ui.UiHorrorClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Lúc Nam_1998 "vào game", hắn có mặt trong danh sách người chơi (phím Tab) và trong gợi ý tên khi gõ lệnh.
 * Danh sách có hơn 1 người nên bảng Tab hiện cả khi đang chơi đơn.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class GhostPlayerListMixin {
    @Inject(method = "getListedPlayerListEntries", at = @At("RETURN"), cancellable = true)
    private void samsara$addGhost(CallbackInfoReturnable<Collection<PlayerListEntry>> cir) {
        PlayerListEntry ghost = UiHorrorClient.getGhostEntry();
        if (ghost == null) return;
        List<PlayerListEntry> list = new ArrayList<>(cir.getReturnValue());
        list.add(ghost);
        cir.setReturnValue(list);
    }
}
