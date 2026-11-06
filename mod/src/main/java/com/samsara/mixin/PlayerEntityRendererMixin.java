package com.samsara.mixin;

import com.samsara.client.CamcorderClientManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin tạo dáng cầm máy quay đưa lên mặt ở góc nhìn thứ ba (F5 / người chơi khác nhìn vào):
 * Khi chế độ ngắm được kích hoạt, tay cầm máy quay sẽ giơ lên ngang tầm mắt theo tư thế SPYGLASS.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "getArmPose", at = @At("RETURN"), cancellable = true)
    private static void samsara$getCamcorderArmPose(
            AbstractClientPlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<BipedEntityModel.ArmPose> cir
    ) {
        if (CamcorderClientManager.isAiming(player, hand)) {
            cir.setReturnValue(BipedEntityModel.ArmPose.SPYGLASS);
        }
    }
}
