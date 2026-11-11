package com.samsara.mixin;

import com.samsara.client.CamcorderClientManager;
import com.samsara.registry.SamsaraDimensions;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tầng 2 (Lối đi nháp): trang xé trên các đảo là item display, chỉ thấy khi đang ngắm máy quay. Trong chiều này không
 * có item display nào khác nên không cần phân biệt thêm.
 */
@Mixin(EntityRenderDispatcher.class)
public class TrangXeRenderMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void samsara$hidePagesUnlessAimed(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof DisplayEntity.ItemDisplayEntity
                && entity.getWorld().getRegistryKey() == SamsaraDimensions.LOI_DI_NHAP_WORLD_KEY
                && !CamcorderClientManager.isFullyAimed()) {
            cir.setReturnValue(false);
        }
    }
}
