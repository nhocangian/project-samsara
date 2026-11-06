package com.samsara.system.narrative;

import com.samsara.item.FracturedSlateItem;
import com.samsara.registry.SamsaraItems;
import com.samsara.registry.SamsaraSounds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

/**
 * Handles karmic imprint onto Fractured Slates upon taking life.
 */
public class SlateNarrativeHandler {

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (damageSource.getAttacker() instanceof ServerPlayerEntity player) {
                String unlockKey = null;
                String unlockQuote = null;

                if (entity instanceof CowEntity) {
                    unlockKey = FracturedSlateItem.NBT_COW;
                    unlockQuote = "samsara.slate_narrative_handler.cau_co_nhan_ra";
                } else if (entity instanceof ZombieEntity) {
                    unlockKey = FracturedSlateItem.NBT_ZOMBIE;
                    unlockQuote = "samsara.slate_narrative_handler.cau_da_chon_bao";
                } else if (entity instanceof EndermanEntity) {
                    unlockKey = FracturedSlateItem.NBT_ENDERMAN;
                    unlockQuote = "samsara.slate_narrative_handler.dung_nhin_vao_mat";
                }

                if (unlockKey != null) {
                    boolean unlockedAny = false;
                    for (int i = 0; i < player.getInventory().size(); i++) {
                        ItemStack stack = player.getInventory().getStack(i);
                        if (stack.isOf(SamsaraItems.FRACTURED_SLATE)) {
                            NbtCompound nbt = stack.getOrCreateNbt();
                            if (!nbt.getBoolean(unlockKey)) {
                                nbt.putBoolean(unlockKey, true);
                                unlockedAny = true;
                            }
                        }
                    }

                    if (unlockedAny) {
                        player.sendMessage(Text.translatable("samsara.slate_narrative_handler.phien_da_rung_len"), true);
                        player.sendMessage(Text.translatable(unlockQuote), false);
                        player.getServerWorld().playSound(
                                null,
                                player.getX(), player.getY(), player.getZ(),
                                SamsaraSounds.SLATE_WHISPER,
                                SoundCategory.PLAYERS,
                                1.0f,
                                0.8f
                        );
                    }
                }
            }
        });
    }
}
