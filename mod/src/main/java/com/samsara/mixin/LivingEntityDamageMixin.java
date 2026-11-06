package com.samsara.mixin;

import com.samsara.system.karma.SinDebtManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept living entity damage and track cruelty before death occurs.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"))
    private void samsara$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;

        if (source.getAttacker() instanceof ServerPlayerEntity player) {
            if (target instanceof PassiveEntity && amount >= 1.0f) {
                // Inflicting pain onto defenseless creatures accumulates micro-debt
                SinDebtManager.get(player.getServerWorld()).addSinDebt(player, 1);
            }
        }
    }
}
