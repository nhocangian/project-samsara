package com.samsara.item;

import com.samsara.client.CamcorderClientManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pin Máy Quay Outlast (Camcorder Lithium Battery):
 * Dùng để cấp nguồn cho chế độ Đèn Hồng Ngoại Ban Đêm (IR Night Vision).
 * Nhấn phím 'R' khi cầm máy quay để nạp lại 100% pin.
 */
public class CamcorderBatteryItem extends Item {

    public CamcorderBatteryItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack batteryStack = user.getStackInHand(hand);

        if (world.isClient) {
            // Kiểm tra xem người chơi có đang cầm máy quay ở tay còn lại hoặc trong túi không
            if (CamcorderClientManager.isHoldingCamcorder(user)) {
                if (CamcorderClientManager.getBatteryLevel() >= 99.0f) {
                    user.sendMessage(Text.translatable("samsara.camcorder_battery_item.pin_may_quay_van"), true);
                } else {
                    CamcorderClientManager.reloadBattery(user);
                    if (!user.getAbilities().creativeMode) {
                        batteryStack.decrement(1);
                    }
                    return TypedActionResult.success(batteryStack, true);
                }
            } else {
                user.sendMessage(Text.translatable("samsara.camcorder_battery_item.ban_can_cam_may"), true);
            }
        }

        return TypedActionResult.pass(batteryStack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("samsara.camcorder_battery_item.pin_lithium_3_7v"));
        tooltip.add(Text.translatable("samsara.camcorder_battery_item.dung_de_cap_nguon"));
        tooltip.add(Text.translatable("samsara.camcorder_battery_item.phim_r_nhan_de"));
        tooltip.add(Text.translatable("samsara.camcorder_battery_item.mot_khoi_pin_con"));
    }
}
