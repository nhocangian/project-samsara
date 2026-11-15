package com.samsara.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Máy Quay VHS: chuột phải đưa lên mắt (hình VHS, thấy những thứ mắt thường không thấy), phím V nhìn đêm hồng ngoại,
 * phím R thay pin. Phần lớn logic nằm ở phía client ({@code client/CamcorderClientManager}).
 *
 * <p>Hệ thống ba cuộn băng ký ức cũ (giết bò / zombie / enderman để mở, phím B để xem) đã cất vào
 * {@code _trash/tapes_removed/}: kịch bản chưa chốt cách dùng băng.
 */
public class CamcorderItem extends Item {
    public CamcorderItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            // consume: không vung tay; HeldItemRendererMixin chặn luôn hoạt ảnh "cất/rút lại item" của vanilla,
            // để chỉ còn động tác đưa máy quay lên mắt
            com.samsara.client.CamcorderClientManager.toggleAiming(user);
            return TypedActionResult.consume(stack);
        }
        return TypedActionResult.success(stack, false);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("samsara.camcorder_item.rec_00_14_09"));
        tooltip.add(Text.translatable("samsara.camcorder_item.mot_chiec_may_quay"));
        tooltip.add(Text.translatable("samsara.camcorder_item.chuot_phai_dua_may"));
        tooltip.add(Text.translatable("samsara.camcorder_item.phim_v_bat_tat"));
        tooltip.add(Text.translatable("samsara.camcorder_item.phim_r_nap_pin"));
        tooltip.add(Text.translatable("samsara.camcorder_item.cam_bien_emf_tu"));
    }
}
