package com.samsara.mixin;

import com.samsara.client.ending.EndingSequenceClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Khoá di chuyển trong suốt đoạn kết (người chơi chỉ còn xoay được góc nhìn). */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void samsara$lockDuringEnding(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (!EndingSequenceClient.isMovementLocked()) return;
        this.movementForward = 0.0f;
        this.movementSideways = 0.0f;
        this.pressingForward = false;
        this.pressingBack = false;
        this.pressingLeft = false;
        this.pressingRight = false;
        this.jumping = false;
        this.sneaking = false;
    }
}
