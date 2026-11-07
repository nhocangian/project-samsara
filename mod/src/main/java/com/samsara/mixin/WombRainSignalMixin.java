package com.samsara.mixin;

import com.samsara.client.ending.EndingSequenceClient;
import com.samsara.registry.SamsaraDimensions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ở The Womb, rainStrength của Iris là kênh tín hiệu "con mắt mở" (bóng phản chiếu ×45) cho shader world1 (thiên hà
 * phóng ra từ Gargantua). The Womb dùng chung thời tiết với Overworld, nên khi Overworld mưa shader tưởng nhầm là
 * tín hiệu. Phía client, mưa ở The Womb luôn bằng đúng tín hiệu của đoạn kết.
 */
@Mixin(World.class)
public abstract class WombRainSignalMixin {
    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void samsara$wombRain(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld world && world.getRegistryKey() == SamsaraDimensions.THE_WOMB_WORLD_KEY) {
            cir.setReturnValue(EndingSequenceClient.shaderSignal());
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void samsara$wombThunder(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld world && world.getRegistryKey() == SamsaraDimensions.THE_WOMB_WORLD_KEY) {
            cir.setReturnValue(0.0f);
        }
    }
}
