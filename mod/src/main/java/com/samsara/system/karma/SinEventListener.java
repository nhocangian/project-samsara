package com.samsara.system.karma;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Listens to entity death events and charges karmic debt to the aggressor.
 */
public class SinEventListener {

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (damageSource.getAttacker() instanceof ServerPlayerEntity player) {
                int points = 0;

                if (entity instanceof VillagerEntity) {
                    points = 25; // Betrayal of innocent kindred
                } else if (entity instanceof PassiveEntity) {
                    points = 15; // Wanton slaughter of gentle creatures
                } else if (entity instanceof Monster) {
                    points = 5;  // Violence against the corrupted forms of oneself
                }

                if (points > 0) {
                    SinDebtManager.get(player.getServerWorld()).addSinDebt(player, points);
                }
            }
        });
    }
}
