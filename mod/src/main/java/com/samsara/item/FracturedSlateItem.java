package com.samsara.item;

import com.samsara.registry.SamsaraSounds;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Fractured Slate: A dynamic ARG narrative artifact.
 * Its surface shifts dynamically as karmic blood is spilled across cycles.
 */
public class FracturedSlateItem extends Item {
    public static final String NBT_COW = "samsara_cow_slain";
    public static final String NBT_ZOMBIE = "samsara_zombie_slain";
    public static final String NBT_ENDERMAN = "samsara_enderman_slain";

    public FracturedSlateItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return false;
        return nbt.getBoolean(NBT_COW) || nbt.getBoolean(NBT_ZOMBIE) || nbt.getBoolean(NBT_ENDERMAN);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            // consume: không vung tay; HeldItemRendererMixin chặn luôn hoạt ảnh "cất/rút lại item" của vanilla,
            // để chỉ còn động tác đưa máy quay lên mắt
            if (user.isSneaking()) {
                com.samsara.client.CamcorderClientManager.cycleTapePlayback(user);
            } else {
                com.samsara.client.CamcorderClientManager.toggleAiming(user);
            }
            return TypedActionResult.consume(stack);
        }

        if (!world.isClient) {
            // Phía server chỉ xử lý âm thanh khi cần
            if (user.isSneaking()) {
                world.playSound(
                        null,
                        user.getX(), user.getY(), user.getZ(),
                        SamsaraSounds.SLATE_WHISPER,
                        SoundCategory.PLAYERS,
                        0.8f,
                        0.9f + (world.random.nextFloat() * 0.2f)
                );
            }
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        if (world.isClient || !(entity instanceof PlayerEntity player)) return;

        // Chỉ rung khi người chơi đang cầm trên tay chính hoặc tay phụ
        boolean isHeld = player.getMainHandStack() == stack || player.getOffHandStack() == stack;
        if (!isHeld) return;

        if (world.getTime() % 80 != 0) return; // Kiểm tra mỗi 4 giây

        NbtCompound nbt = stack.getOrCreateNbt();
        boolean cowUnlocked = nbt.getBoolean(NBT_COW);
        boolean zombieUnlocked = nbt.getBoolean(NBT_ZOMBIE);
        boolean endermanUnlocked = nbt.getBoolean(NBT_ENDERMAN);

        net.minecraft.util.math.Box box = player.getBoundingBox().expand(8.0);

        if (!cowUnlocked && !world.getEntitiesByClass(net.minecraft.entity.passive.CowEntity.class, box, e -> true).isEmpty()) {
            player.sendMessage(Text.translatable("samsara.fractured_slate_item.camera_nhieu_hat_cam"), true);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SamsaraSounds.SLATE_WHISPER, SoundCategory.PLAYERS, 0.7f, 1.4f);
        } else if (!zombieUnlocked && !world.getEntitiesByClass(net.minecraft.entity.mob.ZombieEntity.class, box, e -> true).isEmpty()) {
            player.sendMessage(Text.translatable("samsara.fractured_slate_item.camera_nhieu_hat_cam_2"), true);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SamsaraSounds.SLATE_WHISPER, SoundCategory.PLAYERS, 0.7f, 1.2f);
        } else if (!endermanUnlocked && !world.getEntitiesByClass(net.minecraft.entity.mob.EndermanEntity.class, box, e -> true).isEmpty()) {
            player.sendMessage(Text.translatable("samsara.fractured_slate_item.camera_rung_giat_du"), true);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SamsaraSounds.SLATE_WHISPER, SoundCategory.PLAYERS, 0.7f, 0.9f);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        boolean cowUnlocked = nbt != null && nbt.getBoolean(NBT_COW);
        boolean zombieUnlocked = nbt != null && nbt.getBoolean(NBT_ZOMBIE);
        boolean endermanUnlocked = nbt != null && nbt.getBoolean(NBT_ENDERMAN);

        tooltip.add(Text.translatable("samsara.fractured_slate_item.rec_00_14_09"));
        tooltip.add(Text.translatable("samsara.fractured_slate_item.mot_chiec_may_quay"));
        tooltip.add(Text.translatable("samsara.fractured_slate_item.chuot_phai_dua_may"));
        tooltip.add(Text.translatable("samsara.fractured_slate_item.phim_v_bat_tat"));
        tooltip.add(Text.translatable("samsara.fractured_slate_item.phim_r_nap_pin"));
        tooltip.add(Text.translatable("samsara.fractured_slate_item.phim_b_shift_chuot"));
        tooltip.add(Text.translatable("samsara.fractured_slate_item.cam_bien_emf_tu"));
        tooltip.add(Text.empty());

        // Inscription 1: Cow
        if (cowUnlocked) {
            tooltip.add(Text.translatable("samsara.fractured_slate_item.tape_01_vet_tu"));
        } else {
            tooltip.add(Text.translatable("samsara.fractured_slate_item.tape_01_hu_hong"));
        }

        // Inscription 2: Zombie
        if (zombieUnlocked) {
            tooltip.add(Text.translatable("samsara.fractured_slate_item.tape_02_ban_ghi"));
        } else {
            tooltip.add(Text.translatable("samsara.fractured_slate_item.tape_02_hu_hong"));
        }

        // Inscription 3: Enderman
        if (endermanUnlocked) {
            tooltip.add(Text.translatable("samsara.fractured_slate_item.tape_03_tan_so"));
        } else {
            tooltip.add(Text.translatable("samsara.fractured_slate_item.tape_03_hu_hong"));
        }

        // Endgame Clue when complete
        if (cowUnlocked && zombieUnlocked && endermanUnlocked) {
            tooltip.add(Text.empty());
            tooltip.add(Text.translatable("samsara.fractured_slate_item.ban_ghi_hoan_tat"));
        }
    }
}
