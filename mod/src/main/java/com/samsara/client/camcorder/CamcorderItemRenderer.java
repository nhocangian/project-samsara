package com.samsara.client.camcorder;

import com.samsara.client.CamcorderClientManager;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Vẽ máy quay 3D ở mọi chế độ (tay, góc thứ ba, túi đồ, khung, rơi đất). Biến đổi theo chế độ lấy từ
 * {@code models/item/fractured_slate.json} (parent builtin/entity); hoạt ảnh nâng lên mặt vẫn do
 * {@code HeldItemRendererMixin} đảm nhận (và ẩn hẳn model khi đã ngắm qua kính).
 *
 * Hoạt ảnh:
 * - Màn LCD lật mở mỗi lần cầm máy lên (góc thứ nhất), mặt màn phát sáng: xám xanh bình thường, xanh lục khi bật
 *   hồng ngoại, tắt khi hết pin, chập chờn khi bóng ma lại gần. Góc thứ ba luôn mở; túi đồ / khung / đất gập lại.
 * - Đèn REC nhấp nháy đỏ 1 lần/giây khi còn pin (không nháy trong túi đồ).
 * - Đèn hồng ngoại phía trước sáng tím khi bật nhìn đêm.
 * - Nắp băng bật lên rồi đóng lại khi thay pin (phím R).
 */
public class CamcorderItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    private static final Identifier TEXTURE = new Identifier("samsara", "textures/item/camcorder.png");
    private static final int FULL_BRIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;
    private static final float LCD_OPEN_DEG = 95.0f;
    private static final float LID_OPEN_DEG = 72.0f;

    private CamcorderItemModel model;

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light, int overlay) {
        if (model == null) model = new CamcorderItemModel();
        MinecraftClient client = MinecraftClient.getInstance();
        float delta = client.getTickDelta();

        boolean firstPerson = mode.isFirstPerson();
        boolean thirdPerson = mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND
                || mode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
        boolean powered = CamcorderClientManager.getBatteryLevel() > 0.0f;

        // ---- trạng thái các bộ phận chuyển động
        float lcdOpen = firstPerson ? CamcorderClientManager.getLcdOpen(delta) : (thirdPerson ? 1.0f : 0.0f);
        model.lcd.yaw = easeOutBack(lcdOpen) * LCD_OPEN_DEG * MathHelper.RADIANS_PER_DEGREE;
        float lid = firstPerson ? CamcorderClientManager.getLidOpen(delta) : 0.0f;
        model.lid.pitch = -lid * LID_OPEN_DEG * MathHelper.RADIANS_PER_DEGREE;

        matrices.push();
        matrices.translate(0.5f, 0.5f, 0.5f);
        matrices.scale(-1.0f, -1.0f, 1.0f);

        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));

        // ---- thân (không có màn, không có đèn)
        model.lcdScreen.visible = false;
        model.recLamp.visible = false;
        model.irLamp.visible = false;
        model.root.render(matrices, buffer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);

        // ---- đèn REC
        long time = client.world != null ? client.world.getTime() : 0L;
        boolean animated = firstPerson || thirdPerson;
        boolean recOn = powered && animated && (time / 10L) % 2L == 0L;
        model.recLamp.visible = true;
        if (recOn) {
            model.recLamp.render(matrices, buffer, FULL_BRIGHT, overlay, 1.0f, 0.10f, 0.08f, 1.0f);
        } else {
            model.recLamp.render(matrices, buffer, light, overlay, 0.28f, 0.06f, 0.06f, 1.0f);
        }

        // ---- đèn hồng ngoại
        model.irLamp.visible = true;
        if (firstPerson && CamcorderClientManager.isNightVision() && powered) {
            model.irLamp.render(matrices, buffer, FULL_BRIGHT, overlay, 0.62f, 0.18f, 0.42f, 1.0f);
        } else {
            model.irLamp.render(matrices, buffer, light, overlay, 0.16f, 0.13f, 0.16f, 1.0f);
        }

        // ---- mặt màn LCD (chỉ khi đã mở đủ để thấy)
        if (lcdOpen > 0.05f) {
            model.lcdScreen.visible = true;
            matrices.push();
            model.lcd.rotate(matrices);
            // Lớp có cull: tấm màn dày 0 có 2 mặt trùng nhau, không cull thì 2 mặt đè nhau thành sọc
            VertexConsumer screenBuffer = consumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));
            if (powered) {
                float r = 0.62f, g = 0.72f, b = 0.85f;
                if (firstPerson && CamcorderClientManager.isNightVision()) {
                    r = 0.42f; g = 1.0f; b = 0.48f;
                }
                // Mới mở: màn bật sáng dần
                float on = MathHelper.clamp((lcdOpen - 0.4f) / 0.6f, 0.15f, 1.0f);
                float ghost = firstPerson ? CamcorderClientManager.getGhostProximityStrength() : 0.0f;
                if (ghost > 0.05f && Math.random() < 0.15 + ghost * 0.6) {
                    on *= 0.25f + (float) Math.random() * 0.5f;     // chập chờn khi bóng ma lại gần
                }
                model.lcdScreen.render(matrices, screenBuffer, FULL_BRIGHT, overlay, r * on, g * on, b * on, 1.0f);
            } else {
                model.lcdScreen.render(matrices, screenBuffer, light, overlay, 0.05f, 0.05f, 0.06f, 1.0f);
            }
            matrices.pop();
        }

        matrices.pop();
    }

    /** Lật mở hơi quá rồi bật lại, như bản lề lò xo. */
    private static float easeOutBack(float t) {
        if (t <= 0.0f) return 0.0f;
        if (t >= 1.0f) return 1.0f;
        float c1 = 1.3f, c3 = c1 + 1.0f;
        float u = t - 1.0f;
        return 1.0f + c3 * u * u * u + c1 * u * u;
    }
}
