package com.samsara.system.memory;

import com.samsara.registry.SamsaraDimensions;
import com.samsara.registry.SamsaraSounds;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.worldgen.memory.NamHouseBuilder;
import com.samsara.worldgen.memory.NamMemoryLayout;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/**
 * "Ký Ức Của Nam" (samsara:nam_memory): nơi đầu tiên người chơi bị hút tới sau giấc ngủ thứ 7.
 * Cánh đồng hướng dương phẳng vô tận, một con đường mòn duy nhất dẫn về nhà Nam (Tháng 5/2018).
 * Với shader Samsara (thư mục world2), mặt đất bị bẻ cong ngược lên kiểu "Inception".
 * Lối đi tiếp: vết rách sang The Womb dưới hầm nhà.
 * Lệnh: /samsara nam (dịch chuyển), /samsara test nam_house (dựng lại nhà).
 */
public class NamMemoryManager {

    public static ServerWorld getWorld(net.minecraft.server.MinecraftServer server) {
        return server.getWorld(SamsaraDimensions.NAM_MEMORY_WORLD_KEY);
    }

    /** Dựng nhà Nam nếu chưa có. */
    public static void ensureHouse(ServerWorld world) {
        EggCycleState state = EggCycleState.get(world.getServer());
        if (state.isNamHouseBuilt()) return;
        NamHouseBuilder.build(world);
        state.setNamHouseBuilt(true);
    }

    /**
     * Đưa người chơi tới đầu con đường mòn, nhìn về phía nhà.
     * @param firstArrival true khi đến bằng giấc ngủ thứ 7 (hiện lời dẫn truyện)
     */
    public static boolean arrive(ServerPlayerEntity player, boolean firstArrival) {
        ServerWorld world = getWorld(player.getServer());
        if (world == null) {
            player.sendMessage(Text.translatable("samsara.nam_memory_manager.khong_the_ket_noi"), false);
            return false;
        }
        ensureHouse(world);

        if (player.isSleeping()) player.wakeUp(false, true);
        player.teleport(world, 0.5, NamMemoryLayout.GROUND_Y + 1.0, NamMemoryLayout.ARRIVAL_Z + 0.5, 180.0f, 0.0f);
        player.fallDistance = 0.0f;

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.AMBIENT, 1.0f, 0.6f);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SamsaraSounds.CRADLE_HUM, SoundCategory.AMBIENT, 0.8f, 1.2f);

        if (firstArrival) {
            com.samsara.system.ui.UiHorrorSystem.advancement(player, "home", "samsara.advancement.ve_nha", net.minecraft.item.Items.SUNFLOWER);
            player.sendMessage(Text.translatable("samsara.nam_memory_manager.ban_mo_mat_khong"), false);
            player.sendMessage(Text.translatable("samsara.nam_memory_manager.chi_co_hoa_huong"), false);
            player.sendMessage(Text.translatable("samsara.nam_memory_manager.hay_da_tung"), true);
        } else {
            player.sendMessage(Text.translatable("samsara.nam_memory_manager.ky_uc_cua_nam"), true);
        }
        return true;
    }
}
