package com.samsara.mixin;

import com.samsara.entity.TheWandererEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to track when the client camera locks onto The Wanderer for subtle visual heartbeat feedback.
 */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void samsara$onClientTick(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        HitResult hit = player.clientWorld.raycast(new net.minecraft.world.RaycastContext(
                player.getCameraPosVec(1.0f),
                player.getCameraPosVec(1.0f).add(player.getRotationVec(1.0f).multiply(24.0)),
                net.minecraft.world.RaycastContext.ShapeType.VISUAL,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                player
        ));

        // Subtle camera tremors could be triggered here when gazing into The Wanderer
    }
}
