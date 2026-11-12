package com.samsara.mixin;

import com.samsara.client.trontim.EchoRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.RecipeToast;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tầng 3 "Trốn tìm": nhặt bông hoa hay tờ giấy không được bật toast "New Recipes Unlocked" giữa bóng tối. */
@Mixin(ToastManager.class)
public class TronTimToastMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void samsara$noRecipeToast(Toast toast, CallbackInfo ci) {
        if (toast instanceof RecipeToast && EchoRenderer.inFloor(MinecraftClient.getInstance())) ci.cancel();
    }
}
