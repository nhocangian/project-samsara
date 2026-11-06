package com.samsara.system.cycle;

import com.samsara.registry.SamsaraItems;
import com.samsara.registry.SamsaraSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

/**
 * "Chiếc Máy Quay Khẩn Cấp" (Video 4): máy quay VHS là cơ chế quan sát do <System> tự gài vào
 * ô số 3 của Hotbar, khi người chơi thức trắng tới đêm thứ 3 hoặc khi thế giới đạt Giai đoạn 2.
 * Mỗi người chơi chỉ nhận một lần (EggCycleState.camcorderGiven).
 * Lệnh test: /samsara test camcorder_gift.
 */
public class CamcorderGiftSystem {
    /** Ô số 3 của Hotbar (chỉ số 0-based). */
    public static final int HOTBAR_SLOT = 2;

    public static void register() {
        // Thế giới đã ở Giai đoạn 2+ (vd. save cũ) mà người chơi chưa nhận máy quay
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (EggCycleState.get(server).getStage() >= 2) {
                tryGift(player, "samsara.camcorder_gift_system.system_cau_quay_lai");
            }
        }));
    }

    /**
     * Cài máy quay vào ô số 3 nếu người chơi chưa từng nhận.
     * @return true nếu vừa cài máy quay
     */
    public static boolean tryGift(ServerPlayerEntity player, String systemLine) {
        EggCycleState state = EggCycleState.get(player.getServer());
        if (state.wasCamcorderGiven(player.getUuid())) return false;
        state.markCamcorderGiven(player.getUuid());

        PlayerInventory inv = player.getInventory();
        // Đã tự tìm được máy quay thì <System> không cần can thiệp
        if (inv.containsAny(stack -> stack.isOf(SamsaraItems.FRACTURED_SLATE))) return false;

        give(player);
        player.sendMessage(Text.translatable(systemLine), false);
        com.samsara.system.ui.UiHorrorSystem.advancement(player, "camcorder", "samsara.advancement.mat_thu_ba", SamsaraItems.FRACTURED_SLATE);
        player.sendMessage(Text.translatable("samsara.camcorder_gift_system.o_so_3_tren"), true);
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SamsaraSounds.SYSTEM_STATIC, SoundCategory.PLAYERS, 0.9f, 0.6f);
        return true;
    }

    /** Đặt máy quay vào ô số 3; món đồ đang ở đó được dời sang ô trống khác (hoặc rơi xuống đất). */
    public static void give(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        ItemStack displaced = inv.getStack(HOTBAR_SLOT);
        inv.setStack(HOTBAR_SLOT, new ItemStack(SamsaraItems.FRACTURED_SLATE));
        if (!displaced.isEmpty() && !inv.insertStack(displaced)) {
            player.dropItem(displaced, false);
        }
        inv.markDirty();
    }
}
