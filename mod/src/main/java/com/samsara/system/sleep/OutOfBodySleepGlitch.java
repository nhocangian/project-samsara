package com.samsara.system.sleep;

import com.samsara.entity.EchoDummyEntity;
import com.samsara.registry.SamsaraEntities;
import com.samsara.registry.SamsaraSounds;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Tính năng 1: Cơ chế Giấc Mơ Tách Hồn (The Sleep Out-of-Body Glitch) - Giai đoạn 1.
 * Khi ngủ, người chơi có 25% xác suất cảm thấy linh hồn phân ly khỏi thể xác:
 * - Một bóng ma (EchoDummyEntity) xuất hiện ở góc phòng lặng lẽ nhìn chiếc giường.
 * - Khi tỉnh dậy, bên cạnh gối rơi ra một đóa hoa hồng khô héo mà người chơi chưa từng đặt.
 */
public class OutOfBodySleepGlitch {
    private static final String NBT_OUT_OF_BODY_ACTIVE = "samsara_out_of_body_active";

    public static void register() {
        // Bắt đầu ngủ
        EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            ServerWorld world = player.getServerWorld();

            // 25% xác suất kích hoạt hiện tượng tách hồn trong Giai đoạn 1
            if (world.random.nextFloat() < 0.25f) {
                player.getCommandTags().add(NBT_OUT_OF_BODY_ACTIVE);

                // Triệu hồi bóng ma ở góc phòng (cách giường 3 block)
                BlockPos ghostPos = sleepingPos.add(
                        world.random.nextBoolean() ? 3 : -3,
                        0,
                        world.random.nextBoolean() ? 3 : -3
                );

                EchoDummyEntity ghost = SamsaraEntities.ECHO_DUMMY.create(world);
                if (ghost != null) {
                    ghost.refreshPositionAndAngles(
                            ghostPos.getX() + 0.5,
                            ghostPos.getY(),
                            ghostPos.getZ() + 0.5,
                            (float) Math.toDegrees(Math.atan2(sleepingPos.getZ() - ghostPos.getZ(), sleepingPos.getX() - ghostPos.getX())) - 90.0f,
                            0.0f
                    );
                    ghost.setPlayerUuid(player.getUuid());
                    ghost.setCrouchingState(false);
                    world.spawnEntity(ghost);
                }

                // Thông điệp rợn ngợp
                player.sendMessage(Text.translatable("samsara.out_of_body_sleep_glitch.ban_cam_giac_co"), true);
                world.playSound(null, sleepingPos, SamsaraSounds.SLATE_WHISPER, SoundCategory.PLAYERS, 0.7f, 0.5f);

                // Kích hoạt hiệu ứng Temporal Desync & Spectral Ghosting trên shader
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.BLINDNESS,
                        90, // 4.5s
                        0,
                        false,
                        false,
                        false
                ));
            }
        });

        // Thức dậy
        EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;

            if (player.getCommandTags().remove(NBT_OUT_OF_BODY_ACTIVE)) {
                ServerWorld world = player.getServerWorld();

                // Dư ảnh khi tỉnh thức
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.BLINDNESS,
                        25,
                        0,
                        false,
                        false,
                        false
                ));

                // Xuất hiện đóa hoa khô rơi bên cạnh gối
                ItemStack witheredFlower = new ItemStack(Items.POPPY);
                witheredFlower.setCustomName(Text.translatable("samsara.out_of_body_sleep_glitch.doa_hoa_tan_roi"));

                ItemEntity itemEntity = new ItemEntity(
                        world,
                        sleepingPos.getX() + 0.5,
                        sleepingPos.getY() + 0.6,
                        sleepingPos.getZ() + 0.5,
                        witheredFlower
                );
                itemEntity.setToDefaultPickupDelay();
                world.spawnEntity(itemEntity);

                player.sendMessage(Text.translatable("samsara.out_of_body_sleep_glitch.ben_canh_goi_co"), false);
            }
        });
    }
}
