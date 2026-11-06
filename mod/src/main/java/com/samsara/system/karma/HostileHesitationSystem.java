package com.samsara.system.karma;

import com.samsara.item.FracturedSlateItem;
import com.samsara.registry.SamsaraItems;
import com.samsara.registry.SamsaraSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Tính năng 2: Sự Ngập Ngừng của Quái Vật (The Hesitation & Cessation) - Giai đoạn 2.
 * Khi người chơi đã giải mã >= 2 mảnh ký ức hoặc mang nghiệp chướng cao:
 * Quái vật bắt đầu cảm nhận được sự cộng hưởng cùng một linh hồn.
 * Chúng không còn lao vào mù quáng mà ngập ngừng, hạ vũ khí, rên rỉ đau đớn trước khi tấn công.
 */
public class HostileHesitationSystem {
    private static final String TAG_HESITATING = "samsara_hesitating";
    private static final int HESITATION_TICKS = 40; // 2 giây khựng lại
    private static final int COOLDOWN_TICKS = 200;  // 10 giây trước khi có thể ngập ngừng lại

    /** Quái đang ngập ngừng -> tick server mà trạng thái kết thúc. */
    private static final Map<HostileEntity, Long> HESITATION_END = new WeakHashMap<>();
    /** Quái vừa hết ngập ngừng -> tick server mà nó được phép ngập ngừng lần nữa. */
    private static final Map<HostileEntity, Long> COOLDOWN_END = new WeakHashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(HostileHesitationSystem::tick);
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTicks();

        // Mỗi tick: giữ quái đang ngập ngừng đứng yên, và gỡ trạng thái khi hết 2 giây
        Iterator<Map.Entry<HostileEntity, Long>> it = HESITATION_END.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<HostileEntity, Long> entry = it.next();
            HostileEntity mob = entry.getKey();
            if (mob.isRemoved() || !mob.isAlive()) {
                it.remove();
                continue;
            }
            if (now >= entry.getValue()) {
                // Bản năng quái tính ép buộc trở lại
                mob.getCommandTags().remove(TAG_HESITATING);
                COOLDOWN_END.put(mob, now + COOLDOWN_TICKS);
                it.remove();
            } else {
                mob.setTarget(null);
                mob.getNavigation().stop();
                if (mob instanceof SkeletonEntity skeleton) {
                    skeleton.setAttacking(false);
                }
            }
        }

        if (now % 10 != 0) return; // Kiểm tra kích hoạt mỗi 0.5s

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!hasResonance(player)) continue;

            Box range = new Box(player.getBlockPos()).expand(8.0);
            List<HostileEntity> hostiles = player.getServerWorld().getEntitiesByClass(HostileEntity.class, range, h -> h.getTarget() == player);

            for (HostileEntity mob : hostiles) {
                if (HESITATION_END.containsKey(mob)) continue;
                if (now < COOLDOWN_END.getOrDefault(mob, 0L)) continue;
                // Tag còn sót lại từ phiên bản cũ (trước khi có cơ chế hết hạn) -> dọn đi
                mob.getCommandTags().remove(TAG_HESITATING);

                // 25% xác suất quái vật khựng lại vì nhận ra người chơi
                if (player.getServerWorld().random.nextFloat() < 0.25f) {
                    mob.getCommandTags().add(TAG_HESITATING);
                    HESITATION_END.put(mob, now + HESITATION_TICKS);
                    mob.setTarget(null);
                    mob.getNavigation().stop();

                    // Zombie hạ tay, đứng yên nhìn người chơi
                    if (mob instanceof ZombieEntity zombie) {
                        player.getServerWorld().spawnParticles(
                                ParticleTypes.SMOKE,
                                zombie.getX(), zombie.getY() + 1.8, zombie.getZ(),
                                5, 0.2, 0.2, 0.2, 0.01
                        );
                        player.getServerWorld().playSound(
                                null, zombie.getX(), zombie.getY(), zombie.getZ(),
                                SamsaraSounds.CAVE_REVERSED, SoundCategory.HOSTILE, 0.5f, 1.2f
                        );
                    }

                    // Creeper rên rỉ buồn bã thay vì xì khói phát nổ
                    if (mob instanceof CreeperEntity creeper) {
                        player.getServerWorld().playSound(
                                null, creeper.getX(), creeper.getY(), creeper.getZ(),
                                SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 0.6f, 1.5f
                        );
                    }

                    // Skeleton chần chừ hạ cung
                    if (mob instanceof SkeletonEntity skeleton) {
                        skeleton.setAttacking(false);
                    }
                }
            }
        }
    }

    private static boolean hasResonance(ServerPlayerEntity player) {
        int unlocked = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(SamsaraItems.FRACTURED_SLATE)) {
                NbtCompound nbt = stack.getNbt();
                if (nbt != null) {
                    if (nbt.getBoolean(FracturedSlateItem.NBT_COW)) unlocked++;
                    if (nbt.getBoolean(FracturedSlateItem.NBT_ZOMBIE)) unlocked++;
                    if (nbt.getBoolean(FracturedSlateItem.NBT_ENDERMAN)) unlocked++;
                }
            }
        }
        int debt = SinDebtManager.get(player.getServerWorld()).getSinDebt(player.getUuid());
        return unlocked >= 2 || debt >= 30;
    }
}
