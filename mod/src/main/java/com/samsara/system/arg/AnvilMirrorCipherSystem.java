package com.samsara.system.arg;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * ARG Feature 4: Sự Nghịch Đảo Danh Tính (The Anvil & Mirror Cipher).
 * Khi người chơi cầm một vật phẩm có tên là Tên Chính Mình Viết Đảo Ngược (ví dụ: Steve -> evetS)
 * hoặc mang tên "SAMSARA":
 * Món đồ hoạt động như một "Thấu Kính Luân Hồi", tiết lộ khoảng cách kiếp sống của mọi sinh vật trước mặt.
 */
public class AnvilMirrorCipherSystem {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AnvilMirrorCipherSystem::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 10 != 0) return; // Quét mỗi 0.5s

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ItemStack main = player.getMainHandStack();
            ItemStack off = player.getOffHandStack();

            boolean hasLens = isMirrorLens(main, player) || isMirrorLens(off, player);
            if (!hasLens) continue;

            // Kiểm tra sinh vật trong tầm ngắm của hồng tâm
            HitResult hit = player.raycast(6.0, 1.0f, false);
            // Raycast thực thể trong tầm nhìn
            MobEntity targetMob = findLookedMob(player, 6.0);

            if (targetMob != null) {
                int cycleId = Math.abs((targetMob.getUuid().hashCode() ^ player.getUuid().hashCode()) % 499) + 1;
                String relationText;

                if (targetMob instanceof CowEntity) {
                    relationText = "samsara.anvil_mirror.self";
                } else if (targetMob instanceof ZombieEntity) {
                    relationText = "samsara.anvil_mirror.corpse";
                } else if (targetMob instanceof VillagerEntity) {
                    relationText = "samsara.anvil_mirror.villager";
                } else if (targetMob instanceof EndermanEntity) {
                    relationText = "samsara.anvil_mirror.hunter";
                } else {
                    relationText = "samsara.anvil_mirror.soul";
                }

                player.sendMessage(Text.translatable(relationText, cycleId), true);
            }
        }
    }

    private static boolean isMirrorLens(ItemStack stack, ServerPlayerEntity player) {
        if (stack.isEmpty() || !stack.hasCustomName()) return false;

        String name = stack.getName().getString().trim();
        String playerName = player.getName().getString();
        String reversedName = new StringBuilder(playerName).reverse().toString();

        return name.equalsIgnoreCase(reversedName) ||
                name.equalsIgnoreCase("SAMSARA") ||
                name.equalsIgnoreCase("OUROBOROS") ||
                name.equalsIgnoreCase("MIRROR");
    }

    private static MobEntity findLookedMob(ServerPlayerEntity player, double maxDist) {
        for (MobEntity mob : player.getServerWorld().getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(maxDist), m -> m.isAlive())) {
            if (player.canSee(mob) && mob.distanceTo(player) <= maxDist) {
                // Kiểm tra góc nhìn
                double dot = player.getRotationVec(1.0f).normalize().dotProduct(mob.getPos().subtract(player.getEyePos()).normalize());
                if (dot > 0.94) {
                    return mob;
                }
            }
        }
        return null;
    }
}
