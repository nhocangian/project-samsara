package com.samsara.mixin;

import com.samsara.system.demdautien.DemDauTienClock;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.SheepEntityModel;
import net.minecraft.client.render.entity.model.SheepWoolEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tầng 4 "Đêm đầu tiên": cừu đã bị đánh (trụi lông) vẫn vẽ lớp lông, nhưng tô màu đánh dấu r = 255, g = b = 253 để
 * shader world6 chỉ cho lớp lông hiện bên trong vết rách nhỏ quanh con cừu ("bên kia" vết rách nó vẫn còn lông).
 */
@Mixin(net.minecraft.client.render.entity.feature.SheepWoolFeatureRenderer.class)
public abstract class SheepWoolFeatureRendererMixin extends FeatureRenderer<SheepEntity, SheepEntityModel<SheepEntity>> {
    @Shadow @Final private static Identifier SKIN;
    @Shadow @Final private SheepWoolEntityModel<SheepEntity> model;

    protected SheepWoolFeatureRendererMixin(FeatureRendererContext<SheepEntity, SheepEntityModel<SheepEntity>> context) {
        super(context);
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/passive/SheepEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void samsara$woolBeyondTear(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, SheepEntity sheep,
                                        float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                                        float headYaw, float headPitch, CallbackInfo ci) {
        if (!sheep.isSheared() || sheep.isInvisible() || !DemDauTienClock.isFloor(sheep.getWorld())) return;
        render(getContextModel(), model, SKIN, matrices, vertexConsumers, light, sheep, limbAngle, limbDistance,
                animationProgress, headYaw, headPitch, tickDelta, 1.0f, 253f / 255f, 253f / 255f);
        ci.cancel();
    }
}
