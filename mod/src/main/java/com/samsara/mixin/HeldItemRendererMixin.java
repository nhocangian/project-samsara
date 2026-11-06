package com.samsara.mixin;

import com.samsara.client.CamcorderClientManager;
import com.samsara.item.FracturedSlateItem;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Động tác đưa máy quay lên mắt ở góc nhìn thứ nhất (chuột phải).
 *
 * Tính trong không gian tay (sau equip offset của vanilla, trước biến đổi "display" của model):
 * điểm neo là CAO SU MẮT của kính ngắm. Theo tiến độ ngắm, điểm này đi theo đường cong từ chỗ cầm ở hông tới
 * ngay trước mắt: nhấc lên trước, kéo vào giữa và áp sát sau; máy nghiêng nhẹ giữa chừng rồi thẳng lại
 * (khử góc xoay lúc cầm ở hông). Khi đã áp vào mắt ({@code isFullyAimed}) thì ẩn model, màn hình chuyển sang
 * góc nhìn máy quay với shader VHS.
 *
 * DISPLAY_* phải khớp {@code firstperson_righthand} trong models/item/fractured_slate.json
 * (tay trái khai báo giống hệt, Minecraft tự lật trục X).
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    private static final float DISPLAY_TX = -1.2f / 16f, DISPLAY_TY = 3.4f / 16f, DISPLAY_TZ = -1.5f / 16f;
    private static final float DISPLAY_RX = 6f, DISPLAY_RY = -10f, DISPLAY_RZ = 2f;
    private static final float DISPLAY_SCALE = 0.72f;
    /** Tâm mặt sau cao su mắt trong toạ độ item (pixel). */
    private static final float EYE_X = 7f, EYE_Y = 10.5f, EYE_Z = 14f;
    /** Equip offset tay phải của vanilla (HeldItemRenderer.applyEquipOffset). */
    private static final float EQUIP_X = 0.56f, EQUIP_Y = -0.52f, EQUIP_Z = -0.72f;
    /** Đích của cao su mắt trong không gian mắt: ngay trước mắt, hơi thấp một chút. */
    private static final float TARGET_X = 0f, TARGET_Y = -0.015f, TARGET_Z = -0.10f;

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void samsara$raiseCamcorderToFace(
            LivingEntity entity,
            ItemStack item,
            ModelTransformationMode mode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (!(item.getItem() instanceof FracturedSlateItem)) return;
        if (!mode.isFirstPerson()) return;

        // Đã áp vào mắt: ẩn model, nhìn qua kính ngắm
        if (CamcorderClientManager.isFullyAimed()) {
            ci.cancel();
            return;
        }

        float t = CamcorderClientManager.getAimProgress();
        if (t <= 0.001f) return;
        // Tiến độ 0.9 là lúc chuyển sang kính ngắm: lúc đó máy phải vừa chạm mắt
        t = MathHelper.clamp(t / 0.9f, 0.0f, 1.0f);
        float s = mode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND ? -1.0f : 1.0f;

        // Vị trí cao su mắt khi cầm ở hông (không gian tay)
        Quaternionf displayRot = new Quaternionf().rotationXYZ(
                DISPLAY_RX * MathHelper.RADIANS_PER_DEGREE,
                s * DISPLAY_RY * MathHelper.RADIANS_PER_DEGREE,
                s * DISPLAY_RZ * MathHelper.RADIANS_PER_DEGREE);
        Vector3f eye = new Vector3f(EYE_X / 16f - 0.5f, EYE_Y / 16f - 0.5f, EYE_Z / 16f - 0.5f)
                .mul(DISPLAY_SCALE).rotate(displayRot)
                .add(s * DISPLAY_TX, DISPLAY_TY, DISPLAY_TZ);

        // Đích trong không gian tay
        float goalX = TARGET_X - s * EQUIP_X, goalY = TARGET_Y - EQUIP_Y, goalZ = TARGET_Z - EQUIP_Z;

        // Đường cong: nhấc lên trước, kéo vào giữa sau, áp vào mắt cuối cùng
        float up = ease(MathHelper.clamp(t / 0.7f, 0.0f, 1.0f));
        float side = ease(MathHelper.clamp((t - 0.1f) / 0.75f, 0.0f, 1.0f));
        float in = ease(MathHelper.clamp((t - 0.25f) / 0.75f, 0.0f, 1.0f));
        float straighten = ease(t);
        float bump = MathHelper.sin(t * MathHelper.PI);

        matrices.translate(
                MathHelper.lerp(side, eye.x, goalX),
                MathHelper.lerp(up, eye.y, goalY) + 0.05f * bump,   // vồng lên một chút giữa đường
                MathHelper.lerp(in, eye.z, goalZ));
        // Xoay quanh cao su mắt: nghiêng nhẹ giữa chừng, rồi khử góc cầm ở hông để máy nhìn thẳng ra trước
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(s * (-DISPLAY_RZ * straighten - 9.0f * bump)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(s * (-DISPLAY_RY * straighten + 6.0f * bump)));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-DISPLAY_RX * straighten - 4.0f * bump));
        matrices.translate(-eye.x, -eye.y, -eye.z);
    }

    /**
     * Mỗi lần dùng item thành công, MinecraftClient.doItemUse gọi resetEquipProgress: tay hạ xuống rồi nâng lại
     * như rút item mới. Với máy quay thì bỏ qua để không đè lên động tác đưa lên mắt.
     */
    @Inject(method = "resetEquipProgress", at = @At("HEAD"), cancellable = true)
    private void samsara$keepCamcorderSteady(net.minecraft.util.Hand hand, CallbackInfo ci) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player != null && client.player.getStackInHand(hand).getItem() instanceof FracturedSlateItem) {
            ci.cancel();
        }
    }

    private static float ease(float x) {
        return x * x * (3.0f - 2.0f * x);
    }
}
