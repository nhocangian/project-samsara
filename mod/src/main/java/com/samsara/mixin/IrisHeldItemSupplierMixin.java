package com.samsara.mixin;

import com.samsara.client.CamcorderClientManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin can thiệp trực tiếp vào Iris Shader HeldItemSupplier:
 * Đảm bảo biến uniform 'heldItemId' / 'heldItemId2' trong Shader Iris chỉ nhận ID của máy quay (ID 1)
 * KHI VÀ CHỈ KHI máy quay đã hoàn tất việc đưa lên ngang mặt (isFullyAimed).
 * Trong quá trình đang nâng lên, shader vẫn là màu bình thường; ngay khi chạm vào mắt thì shader mới chuyển sang VHS!
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.IdMapUniforms$HeldItemSupplier", remap = false)
public class IrisHeldItemSupplierMixin {

    @Inject(method = "getIntID", at = @At("RETURN"), cancellable = true, require = 0)
    private void samsara$controlHeldItemId(CallbackInfoReturnable<Integer> cir) {
        int originalId = cir.getReturnValue();
        // ID 1 là samsara:fractured_slate (Outlast Camcorder)
        if (originalId == 1) {
            if (!CamcorderClientManager.isFullyAimed()) {
                cir.setReturnValue(0);
            }
        }
    }
}
