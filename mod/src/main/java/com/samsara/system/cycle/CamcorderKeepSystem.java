package com.samsara.system.cycle;

import com.samsara.registry.SamsaraItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Máy quay không vứt được: từ lúc được tặng, ném ra đất hay chết thì vật phẩm trên đất biến mất và máy quay trở lại túi;
 * cất vào rương thì nó tự về túi. (Tầng 4 của Tầng Dưới sẽ chủ động lấy máy quay đi: khi dựng tầng đó cần thêm ngoại lệ ở đây.)
 */
public final class CamcorderKeepSystem {
    private CamcorderKeepSystem() {}

    private static final int INTERVAL = 10;

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity item && item.getStack().isOf(SamsaraItems.FRACTURED_SLATE)) item.discard();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % INTERVAL != 0) return;
            EggCycleState state = EggCycleState.get(server);
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.isAlive() && state.wasCamcorderGiven(player.getUuid())) keep(player);
            }
        });
    }

    private static void keep(ServerPlayerEntity player) {
        boolean inPlayer = player.getInventory().containsAny(s -> s.isOf(SamsaraItems.FRACTURED_SLATE))
                || player.currentScreenHandler.getCursorStack().isOf(SamsaraItems.FRACTURED_SLATE);
        if (inPlayer) return;
        // Đang nằm trong một ô của rương đang mở: lấy lại
        ScreenHandler handler = player.currentScreenHandler;
        for (Slot slot : handler.slots) {
            if (slot.getStack().isOf(SamsaraItems.FRACTURED_SLATE)) slot.setStack(ItemStack.EMPTY);
        }
        CamcorderGiftSystem.give(player);
    }
}
