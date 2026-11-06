package com.samsara.mixin;

import com.samsara.system.celestial.SolarFractureManager;
import net.minecraft.client.item.CompassAnglePredicateProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stage 3+: "Kim La Bàn quay tít" ở Overworld (Master Production Document, bảng mục II.3).
 * Kim quay đều rồi thỉnh thoảng giật ngược lại, như bị một lực hấp dẫn khác kéo đi.
 */
@Mixin(CompassAnglePredicateProvider.class)
public class CompassSpinMixin {
    @Inject(method = "unclampedCall", at = @At("HEAD"), cancellable = true)
    private void samsara$spinNeedle(ItemStack stack, ClientWorld world, LivingEntity entity, int seed,
                                    CallbackInfoReturnable<Float> cir) {
        if (SolarFractureManager.clientStage < 3) return;
        World w = world != null ? world : (entity != null ? entity.getWorld() : null);
        if (w == null || w.getRegistryKey() != World.OVERWORLD) return;

        float t = (w.getTime() % 100000L) + (seed & 0xFF);
        float spin = t * 0.045f;
        float jerk = (float) Math.sin(t * 0.21f) * 0.12f;
        float angle = (spin + jerk) % 1.0f;
        cir.setReturnValue(angle < 0 ? angle + 1.0f : angle);
    }
}
