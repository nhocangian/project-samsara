package com.samsara.system.karma;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tính năng 3: Phản Đòn Bóc Lột Dân Làng (The Villager Confinement Horror) - Giai đoạn 2.
 * Người chơi Minecraft có thói quen nhốt dân làng trong ô 1x1 để ép giá hoặc farm golem.
 * Nếu dân làng bị giam trong không gian chật hẹp:
 * - Mắt rỉ ra hạt nước mắt Crying Obsidian.
 * - Khi giao dịch, tên nghề nghiệp biến thành: "Kiếp trước ngươi cũng từng nhốt ta vào chiếc hộp này."
 * - Gia tăng nợ tội lỗi (Sin Debt) cho người chơi.
 */
public class VillagerConfinementSystem {
    private static final Map<UUID, BlockPos> LAST_POSITIONS = new HashMap<>();
    private static final Map<UUID, Integer> CONFINED_TICKS = new HashMap<>();

    public static void register() {
        // Giám sát sự giam cầm của dân làng theo thời gian
        ServerTickEvents.END_SERVER_TICK.register(VillagerConfinementSystem::tick);

        // Can thiệp khi người chơi click chuột phải để giao dịch
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(entity instanceof VillagerEntity villager) || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            int confined = CONFINED_TICKS.getOrDefault(villager.getUuid(), 0);
            if (confined >= 300) { // Đã bị nhốt trên 15 giây liên tục
                // Đổi tên quở trách
                villager.setCustomName(Text.translatable("samsara.villager_confinement_system.cau_cung_tung_bo"));
                villager.setCustomNameVisible(true);

                // Phát tiếng thở dài não nề
                world.playSound(
                        null, villager.getX(), villager.getY(), villager.getZ(),
                        SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL,
                        1.0f, 0.55f
                );

                // Cộng thêm 5 nợ nghiệp chướng vì hành vi bóc lột đồng loại
                SinDebtManager.get(serverPlayer.getServerWorld()).addSinDebt(serverPlayer, 5);
                serverPlayer.sendMessage(Text.translatable("samsara.villager_confinement_system.mot_cam_giac_toi"), true);
            }

            return ActionResult.PASS;
        });
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 40 != 0) return; // Quét mỗi 2s

        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                world.getEntitiesByClass(VillagerEntity.class, player.getBoundingBox().expand(16.0), v -> true).forEach(villager -> {
                    UUID uuid = villager.getUuid();
                    BlockPos currentPos = villager.getBlockPos();
                    BlockPos lastPos = LAST_POSITIONS.get(uuid);

                    if (lastPos != null && lastPos.equals(currentPos)) {
                        int ticks = CONFINED_TICKS.getOrDefault(uuid, 0) + 40;
                        CONFINED_TICKS.put(uuid, ticks);

                        // Nếu bị nhốt liên tục > 300 ticks (15s): mắt rỉ nước mắt tím
                        if (ticks >= 300) {
                            world.spawnParticles(
                                    ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                                    villager.getX(), villager.getY() + 1.6, villager.getZ(),
                                    3, 0.1, 0.1, 0.1, 0.01
                            );
                        }
                    } else {
                        LAST_POSITIONS.put(uuid, currentPos);
                        CONFINED_TICKS.put(uuid, 0);
                    }
                });
            }
        }
    }
}
