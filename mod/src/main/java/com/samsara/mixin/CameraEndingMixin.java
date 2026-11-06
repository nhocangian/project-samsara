package com.samsara.mixin;

import com.samsara.client.ending.EndingSequenceClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Đoạn kết: đặt camera theo đường bay cinematic (góc nhìn thứ 3) và rung màn hình khi bóng phản chiếu lớn lên.
 */
@Mixin(Camera.class)
public abstract class CameraEndingMixin {
    @Shadow protected abstract void setPos(Vec3d pos);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow public abstract Vec3d getPos();

    @Inject(method = "update", at = @At("TAIL"))
    private void samsara$endingCamera(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
                                      float tickDelta, CallbackInfo ci) {
        if (!EndingSequenceClient.isActive()) return;

        Vec3d eye = focusedEntity.getCameraPosVec(tickDelta);
        Vec3d[] shot = EndingSequenceClient.cinematicCamera(tickDelta, eye);
        if (shot != null) {
            Vec3d d = shot[1].subtract(shot[0]);
            float yaw = (float) (MathHelper.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90.0f;
            float pitch = (float) -(MathHelper.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * (180.0 / Math.PI));
            this.setRotation(yaw, pitch);
            this.setPos(shot[0]);
        }
        Vec3d shake = EndingSequenceClient.shakeOffset();
        if (shake != Vec3d.ZERO) {
            this.setPos(this.getPos().add(shake));
        }
    }
}
