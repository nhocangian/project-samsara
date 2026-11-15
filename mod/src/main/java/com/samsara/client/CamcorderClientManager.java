package com.samsara.client;

import com.samsara.entity.EchoDummyEntity;
import com.samsara.entity.TheWandererEntity;
import com.samsara.item.CamcorderItem;
import com.samsara.registry.SamsaraItems;
import com.samsara.registry.SamsaraSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Quản lý trạng thái máy quay cầm tay (item samsara:camcorder):
 * - Timing chuẩn xác: Nâng máy quay lên mặt trước rồi mới bật kính ngắm và shader VHS.
 * - Hệ thống Pin (Battery System): Tiêu hao pin khi nhìn đêm, phím 'R' thay pin.
 * - Hệ thống Nhiễu sóng Từ trường khi bóng ma lại gần (Magnetic Proximity Glitch):
 *   + Quét The Wanderer và Echo Dummy trong bán kính 20 blocks.
 *   + Rung giật màn hình, sọc tĩnh điện, rè radio khi ma áp sát.
 */
public class CamcorderClientManager {
    private static boolean aiming = false;
    private static boolean nightVision = false;
    private static float aimProgress = 0.0f;
    private static float prevAimProgress = 0.0f;
    private static boolean engagedSoundPlayed = false;

    // Quản lý dung lượng pin (0.0f -> 100.0f)
    private static float batteryLevel = 100.0f;
    private static int lowBatteryWarningTimer = 0;


    // Cường độ nhiễu sóng từ trường khi bóng ma lại gần (0.0f -> 1.0f)
    private static float ghostProximityStrength = 0.0f;
    private static int staticAudioCooldown = 0;

    // Hoạt ảnh model 3D (CamcorderItemRenderer): màn LCD lật mở khi cầm lên, nắp băng bật khi thay pin
    private static float lcdOpen = 0.0f;
    private static float prevLcdOpen = 0.0f;
    private static int lcdHoldTicks = 0;
    private static final int LCD_DELAY_TICKS = 14;
    private static final float LCD_OPEN_TICKS = 16.0f;
    private static final int LID_DURATION = 28;
    private static int lidTicks = 0;

    public static KeyBinding nightVisionKey;
    public static KeyBinding reloadKey;

    public static void init() {
        // Đăng ký phím tắt Night Vision (Mặc định: Phím V)
        nightVisionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.samsara.night_vision",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.samsara"
        ));

        // Đăng ký phím tắt Thay Pin Nhanh (Mặc định: Phím R)
        reloadKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.samsara.reload_battery",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories.samsara"
        ));

        // Lệnh /samsara test camcorder_model: pin về 50% để thử thay pin (nắp băng)
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.samsara.command.SamsaraTestCommands.CAMCORDER_MODEL_TEST,
                (client, handler, buf, sender) -> client.execute(() -> batteryLevel = 50.0f));

        // Lắng nghe tick phía client
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerEntity player = client.player;
            if (player == null || client.world == null) {
                if (aiming || nightVision || aimProgress > 0.0f) {
                    reset();
                }
                return;
            }

            // Cập nhật hoạt ảnh nội suy chuyển động đưa máy quay lên mặt
            prevAimProgress = aimProgress;
            if (aiming) {
                aimProgress = Math.min(1.0f, aimProgress + 0.14f); // ~0.35 giây để đưa lên mắt
                if (aimProgress >= 0.90f && !engagedSoundPlayed) {
                    engagedSoundPlayed = true;
                    player.playSound(SoundEvents.BLOCK_DISPENSER_DISPENSE, 0.4f, 2.0f);
                }
            } else {
                aimProgress = Math.max(0.0f, aimProgress - 0.20f);
                engagedSoundPlayed = false;
            }

            // Kiểm tra xem người chơi còn đang cầm máy quay trên tay không
            boolean holding = isHoldingCamcorder(player);

            prevLcdOpen = lcdOpen;
            if (holding) {
                // Chờ tay đưa máy lên xong rồi mới lật màn, lật trong ~0.8 giây
                lcdHoldTicks++;
                if (lcdHoldTicks == LCD_DELAY_TICKS) {
                    player.playSound(SoundEvents.BLOCK_LEVER_CLICK, 0.25f, 1.9f);
                }
                if (lcdHoldTicks > LCD_DELAY_TICKS) {
                    lcdOpen = Math.min(1.0f, lcdOpen + 1.0f / LCD_OPEN_TICKS);
                }
            } else {
                lcdHoldTicks = 0;
                lcdOpen = 0.0f;
                prevLcdOpen = 0.0f;
            }
            if (lidTicks > 0) lidTicks--;

            if (aiming && !holding) {
                setAiming(false, player);
            }

            // =========================================================================
            // XỬ LÝ PHÍM THAY PIN (RELOAD KEY - 'R')
            // =========================================================================
            while (reloadKey.wasPressed()) {
                if (holding) {
                    reloadBattery(player);
                }
            }

            // =========================================================================
            // XỬ LÝ PHÍM BẬT/TẮT HỒNG NGOẠI (NIGHT VISION - 'V')
            // =========================================================================
            while (nightVisionKey.wasPressed()) {
                if (holding) {
                    toggleNightVision(player);
                } else {
                    player.sendMessage(Text.translatable("samsara.camcorder_client_manager.ban_can_cam_may"), true);
                }
            }

            // =========================================================================
            // QUÉT THỰC THỂ DỊ THƯỜNG SIÊU NHIÊN & TÍNH TOÁN NHIỄU SÓNG TỪ TRƯỜNG
            // =========================================================================
            Box searchBox = player.getBoundingBox().expand(20.0);
            List<TheWandererEntity> wanderers = client.world.getEntitiesByClass(TheWandererEntity.class, searchBox, e -> true);
            List<EchoDummyEntity> echoes = client.world.getEntitiesByClass(EchoDummyEntity.class, searchBox, e -> true);

            double minDist = 999.0;
            for (TheWandererEntity w : wanderers) {
                double d = player.distanceTo(w);
                if (d < minDist) minDist = d;
            }
            for (EchoDummyEntity e : echoes) {
                double d = player.distanceTo(e);
                if (d < minDist) minDist = d;
            }

            if (minDist <= 20.0) {
                float targetStrength = (float) Math.max(0.0, 1.0 - (minDist / 20.0));
                ghostProximityStrength = MathHelper.lerp(0.18f, ghostProximityStrength, targetStrength);
            } else {
                ghostProximityStrength = Math.max(0.0f, ghostProximityStrength - 0.04f);
            }

            // Xử lý hiệu ứng âm thanh rè tĩnh điện / máy rung khi bóng ma áp sát
            if (ghostProximityStrength > 0.06f && (holding || aiming)) {
                staticAudioCooldown++;
                int interval = (int) MathHelper.lerp(ghostProximityStrength, 40, 6);
                if (staticAudioCooldown >= interval) {
                    staticAudioCooldown = 0;
                    // Tiếng rè tĩnh điện radio
                    player.playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.15f * ghostProximityStrength, 1.8f + (player.getRandom().nextFloat() * 0.4f));
                    if (ghostProximityStrength > 0.4f) {
                        player.playSound(SoundEvents.BLOCK_BEACON_AMBIENT, 0.35f * ghostProximityStrength, 1.9f);
                    }
                }

                // Khi áp sát rất gần (< 8 block) và đang ngắm máy quay: giật nhẹ góc nhìn
                if (isFullyAimed() && ghostProximityStrength > 0.55f) {
                    float jitterPitch = (player.getRandom().nextFloat() - 0.5f) * ghostProximityStrength * 0.35f;
                    float jitterYaw = (player.getRandom().nextFloat() - 0.5f) * ghostProximityStrength * 0.35f;
                    player.setPitch(player.getPitch() + jitterPitch);
                    player.setYaw(player.getYaw() + jitterYaw);
                }
            }

            // =========================================================================
            // TIÊU HAO PIN KHI BẬT ĐÈN HỒNG NGOẠI BAN ĐÊM (IR NIGHT VISION)
            // =========================================================================
            if (nightVision && holding && isFullyAimed()) {
                // Tiêu hao ~1.6% pin mỗi giây (tương đương 60 giây dùng liên tục)
                batteryLevel = Math.max(0.0f, batteryLevel - 0.08f);

                // Cảnh báo pin yếu (<20%)
                if (batteryLevel > 0.0f && batteryLevel <= 20.0f) {
                    lowBatteryWarningTimer++;
                    if (lowBatteryWarningTimer % 80 == 0) { // Mỗi 4 giây bíp 1 lần
                        player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 0.5f, 2.0f);
                        player.sendMessage(Text.translatable("samsara.camcorder_client_manager.pin_yeu_cot_pin"), true);
                    }
                }

                // Sập nguồn khi pin về 0
                if (batteryLevel <= 0.0f) {
                    batteryLevel = 0.0f;
                    setNightVision(false, player);
                    player.playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.8f);
                    player.sendMessage(Text.translatable("samsara.camcorder_client_manager.het_pin_den_hong"), true);
                } else {
                    // Duy trì hiệu ứng nhìn đêm
                    player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.NIGHT_VISION,
                            120,
                            0,
                            false,
                            false,
                            false
                    ));
                }
            } else if (!nightVision && player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                StatusEffectInstance effect = player.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (effect != null && effect.getDuration() <= 140) {
                    player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                }
            }
        });
    }

    public static boolean isHoldingCamcorder(PlayerEntity player) {
        if (player == null) return false;
        return player.getMainHandStack().getItem() instanceof CamcorderItem
                || player.getOffHandStack().getItem() instanceof CamcorderItem;
    }

    public static ItemStack getHeldCamcorder(PlayerEntity player) {
        if (player == null) return ItemStack.EMPTY;
        if (player.getMainHandStack().getItem() instanceof CamcorderItem) return player.getMainHandStack();
        if (player.getOffHandStack().getItem() instanceof CamcorderItem) return player.getOffHandStack();
        return ItemStack.EMPTY;
    }

    public static boolean isAiming() {
        return aiming;
    }

    public static boolean isFullyAimed() {
        return aiming && aimProgress >= 0.90f;
    }

    public static boolean isAiming(PlayerEntity player, Hand hand) {
        if (!aiming || player == null) return false;
        return player.getStackInHand(hand).getItem() instanceof CamcorderItem;
    }

    public static float getAimProgress(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevAimProgress, aimProgress);
    }

    public static float getAimProgress() {
        MinecraftClient client = MinecraftClient.getInstance();
        float tickDelta = client.getTickDelta();
        return getAimProgress(tickDelta);
    }

    public static boolean isNightVision() {
        return nightVision;
    }

    public static float getBatteryLevel() {
        return batteryLevel;
    }

    public static void setBatteryLevel(float level) {
        batteryLevel = MathHelper.clamp(level, 0.0f, 100.0f);
    }

    public static float getGhostProximityStrength() {
        return ghostProximityStrength;
    }

    public static float getLcdOpen(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevLcdOpen, lcdOpen);
    }

    /** Độ mở nắp băng 0..1: bật nhanh, giữ, rồi đóng lại. */
    public static float getLidOpen(float tickDelta) {
        if (lidTicks <= 0) return 0.0f;
        float t = MathHelper.clamp((LID_DURATION - lidTicks + tickDelta) / LID_DURATION, 0.0f, 1.0f);
        float a;
        if (t < 0.22f) a = t / 0.22f;
        else if (t < 0.62f) a = 1.0f;
        else a = 1.0f - (t - 0.62f) / 0.38f;
        a = MathHelper.clamp(a, 0.0f, 1.0f);
        return a * a * (3.0f - 2.0f * a);
    }

    public static int getSpareBatteryCount(PlayerEntity player) {
        if (player == null) return 0;
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(SamsaraItems.CAMCORDER_BATTERY)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static void reloadBattery(PlayerEntity player) {
        if (player == null) return;
        int spares = getSpareBatteryCount(player);
        if (spares <= 0) {
            player.playSound(SoundEvents.BLOCK_DISPENSER_FAIL, 0.7f, 1.4f);
            player.sendMessage(Text.translatable("samsara.camcorder_client_manager.khong_co_pin_du"), true);
            return;
        }

        if (batteryLevel >= 99.0f) {
            player.sendMessage(Text.translatable("samsara.camcorder_client_manager.pin_may_quay_van"), true);
            return;
        }

        // Tiêu hao 1 viên pin từ túi đồ
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(SamsaraItems.CAMCORDER_BATTERY)) {
                stack.decrement(1);
                break;
            }
        }

        batteryLevel = 100.0f;
        lidTicks = LID_DURATION;
        // Âm thanh mở nắp và chốt lò xo nạp pin mới Outlast
        player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.4f);
        player.playSound(SoundEvents.BLOCK_TRIPWIRE_CLICK_ON, 0.8f, 2.0f);
        player.sendMessage(Text.translatable("samsara.camcorder_client_manager.batt_da_nap_pin"), true);
    }

    public static void toggleAiming(PlayerEntity player) {
        setAiming(!aiming, player);
    }

    public static void setAiming(boolean state, PlayerEntity player) {
        if (aiming == state) return;
        aiming = state;

        if (player != null) {
            if (aiming) {
                player.playSound(SoundEvents.ITEM_SPYGLASS_USE, 0.85f, 1.2f);
            } else {
                player.playSound(SoundEvents.ITEM_SPYGLASS_STOP_USING, 0.85f, 1.1f);
                if (nightVision) {
                    setNightVision(false, player);
                }
            }
        }
    }

    public static void toggleNightVision(PlayerEntity player) {
        if (!nightVision && batteryLevel <= 0.0f) {
            if (player != null) {
                player.playSound(SoundEvents.BLOCK_DISPENSER_FAIL, 0.8f, 1.8f);
            }
            return;
        }
        setNightVision(!nightVision, player);
    }

    public static void setNightVision(boolean state, PlayerEntity player) {
        nightVision = state;
        if (player != null) {
            if (nightVision) {
                player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 0.75f, 2.0f);
            } else {
                player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.75f, 1.9f);
                player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        }
    }

    public static void reset() {
        aiming = false;
        nightVision = false;
        aimProgress = 0.0f;
        prevAimProgress = 0.0f;
        engagedSoundPlayed = false;
        ghostProximityStrength = 0.0f;
        lcdOpen = 0.0f;
        prevLcdOpen = 0.0f;
        lcdHoldTicks = 0;
        lidTicks = 0;
    }
}
