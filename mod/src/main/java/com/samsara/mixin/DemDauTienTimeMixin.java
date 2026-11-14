package com.samsara.mixin;

import com.samsara.system.demdautien.DemDauTienClock;
import com.samsara.system.demdautien.DemDauTienRuntime;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tầng 4 "Đêm đầu tiên" tự giữ giờ (chiều phụ vốn dùng chung giờ với Overworld): bầu trời, ánh sáng, uniform worldTime
 * của Iris và việc ngủ đều đi theo đồng hồ riêng ({@link DemDauTienClock}).
 */
@Mixin(World.class)
public abstract class DemDauTienTimeMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void samsara$floorClock(CallbackInfoReturnable<Long> cir) {
        World self = (World) (Object) this;
        if (!DemDauTienClock.isFloor(self)) return;
        cir.setReturnValue(self.isClient
                ? DemDauTienClock.clientTimeOfDay(self)
                : DemDauTienRuntime.timeOfDay(self));
    }
}
