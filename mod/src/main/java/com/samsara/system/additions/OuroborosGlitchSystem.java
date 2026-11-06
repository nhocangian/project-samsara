package com.samsara.system.additions;

import com.samsara.item.FracturedSlateItem;
import com.samsara.registry.SamsaraItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Creative Addition 3: Ouroboros Glitch & Retinal Inversion.
 * Subtly gaslights the player's perception:
 * When holding the Fractured Slate and looking into the eyes of mobs,
 * mob names temporarily glitch to the player's own username,
 * reinforcing that all entities are merely past or future iterations of oneself.
 */
public class OuroborosGlitchSystem {
    private static final Map<UUID, Long> LAST_GLITCH_TICK = new HashMap<>();
    private static final int GLITCH_DURATION_TICKS = 200; // Tên bị "nhiễu" trong 10 giây

    /** Tên gốc của quái trước khi bị glitch, để trả lại khi hết hiệu ứng. */
    private record OriginalName(@Nullable Text name, boolean visible, Text glitchName, long restoreTick) {}
    private static final Map<MobEntity, OriginalName> GLITCHED_MOBS = new WeakHashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(OuroborosGlitchSystem::tick);
        // Server tắt giữa lúc glitch -> trả lại tên ngay để tên giả không bị lưu vào save
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> restoreExpiredNames(Long.MAX_VALUE));
    }

    private static void restoreExpiredNames(long currentTick) {
        Iterator<Map.Entry<MobEntity, OriginalName>> it = GLITCHED_MOBS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<MobEntity, OriginalName> entry = it.next();
            MobEntity mob = entry.getKey();
            OriginalName original = entry.getValue();
            if (mob.isRemoved()) {
                it.remove();
                continue;
            }
            if (currentTick < original.restoreTick()) continue;

            // Chỉ trả lại tên nếu chưa ai đổi tên con quái trong lúc glitch (vd. bằng name tag)
            if (original.glitchName().equals(mob.getCustomName())) {
                mob.setCustomName(original.name());
                mob.setCustomNameVisible(original.visible());
            }
            it.remove();
        }
    }

    private static void tick(MinecraftServer server) {
        long currentTick = server.getTicks();
        restoreExpiredNames(currentTick);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ItemStack held = player.getMainHandStack();
            if (!held.isOf(SamsaraItems.FRACTURED_SLATE)) continue;

            UUID uuid = player.getUuid();
            long lastTrigger = LAST_GLITCH_TICK.getOrDefault(uuid, 0L);

            // Cooldown of 45 seconds between micro-glitches
            if (currentTick - lastTrigger < 900) continue;

            // Search for mobs within direct gaze
            Box gazeBox = new Box(player.getBlockPos()).expand(10.0);
            List<MobEntity> nearbyMobs = player.getServerWorld().getEntitiesByClass(MobEntity.class, gazeBox, m -> true);

            for (MobEntity mob : nearbyMobs) {
                // If player is looking directly at the mob
                if (player.canSee(mob) && mob.distanceTo(player) < 6.0 && !GLITCHED_MOBS.containsKey(mob)) {
                    LAST_GLITCH_TICK.put(uuid, currentTick);

                    // Temporarily set mob custom name to player's name (restored after GLITCH_DURATION_TICKS)
                    Text glitchName = Text.literal("§8§o" + player.getName().getString() + "§r");
                    GLITCHED_MOBS.put(mob, new OriginalName(
                            mob.getCustomName(),
                            mob.isCustomNameVisible(),
                            glitchName,
                            currentTick + GLITCH_DURATION_TICKS
                    ));
                    mob.setCustomName(glitchName);
                    mob.setCustomNameVisible(true);

                    player.getServerWorld().playSound(
                            null,
                            mob.getX(), mob.getY(), mob.getZ(),
                            SoundEvents.BLOCK_CONDUIT_AMBIENT,
                            SoundCategory.PLAYERS,
                            0.7f,
                            0.5f
                    );

                    // Eerie subtle chat notification
                    player.sendMessage(Text.translatable("samsara.ouroboros_glitch_system.you_recognize_the_expression"), true);

                    // Kích hoạt micro-glitch trên shader (1.5s reality tear)
                    player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.DARKNESS,
                            30,
                            0,
                            false,
                            false,
                            false
                    ));
                    break;
                }
            }
        }
    }
}
