package com.samsara.block.entity;

import com.samsara.block.PrimordialEggBlock;
import com.samsara.registry.SamsaraBlockEntities;
import com.samsara.registry.SamsaraSounds;
import com.samsara.system.karma.SinDebtManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Handles the final awakening and cycle reboot at The Primordial Egg.
 */
public class PrimordialEggBlockEntity extends BlockEntity {
    private boolean hatching = false;
    private int hatchingTicks = 0;
    public static final int TOTAL_HATCH_TICKS = 250;

    public PrimordialEggBlockEntity(BlockPos pos, BlockState state) {
        super(SamsaraBlockEntities.PRIMORDIAL_EGG_BE, pos, state);
    }

    public boolean isHatching() {
        return hatching;
    }

    public void startHatching(PlayerEntity triggerPlayer) {
        this.hatching = true;
        this.hatchingTicks = 0;
        if (world != null && !world.isClient) {
            world.setBlockState(pos, getCachedState().with(PrimordialEggBlock.HATCHING, true));
        }
        markDirty();
    }

    public static void tick(World world, BlockPos pos, BlockState state, PrimordialEggBlockEntity entity) {
        if (world.isClient) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        if (entity.hatching) {
            entity.hatchingTicks++;
            int t = entity.hatchingTicks;

            Box searchBox = new Box(pos).expand(32.0);
            List<ServerPlayerEntity> players = serverWorld.getEntitiesByClass(ServerPlayerEntity.class, searchBox, p -> true);

            for (ServerPlayerEntity player : players) {
                // Stabilize player movement during ascension
                player.setVelocity(Vec3d.ZERO);
                player.velocityModified = true;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 255, false, false, false));
            }

            // Staged revelation dialogue
            if (t == 30) {
                broadcast(players, "samsara.primordial_egg_block_entity.cau_da_nem_du", serverWorld, pos);
            } else if (t == 90) {
                broadcast(players, "samsara.primordial_egg_block_entity.cau_da_mang_du", serverWorld, pos);
            } else if (t == 150) {
                broadcast(players, "samsara.primordial_egg_block_entity.moi_sinh_linh_cau", serverWorld, pos);
            } else if (t == 210) {
                broadcast(players, "samsara.primordial_egg_block_entity.qua_trung_nut_vo", serverWorld, pos);
                serverWorld.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 1.5f, 0.5f);
            } else if (t >= TOTAL_HATCH_TICKS) {
                ServerWorld overworld = serverWorld.getServer().getOverworld();
                BlockPos spawnPos = overworld.getSpawnPos();

                for (ServerPlayerEntity player : players) {
                    // Massive XP burst
                    ExperienceOrbEntity.spawn(serverWorld, player.getPos(), 2500);

                    // Totem animation
                    serverWorld.sendEntityStatus(player, (byte) 35);
                    serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.2f, 0.7f);

                    // Reset Karmic Sin Debt
                    SinDebtManager.get(overworld).addSinDebt(player, -9999);

                    // Set dawn at Overworld
                    overworld.setTimeOfDay(1000);

                    // Teleport back to dawn spawn
                    player.teleport(
                            overworld,
                            spawnPos.getX() + 0.5,
                            spawnPos.getY() + 1.0,
                            spawnPos.getZ() + 0.5,
                            0.0f,
                            0.0f
                    );

                    // Conclude with transcendent awakening prompt
                    Text reason = Text.translatable("samsara.primordial_egg_block_entity.qua_trung_da_nut");
                    player.networkHandler.disconnect(reason);
                }

                entity.hatching = false;
                entity.hatchingTicks = 0;
                serverWorld.setBlockState(pos, state.with(PrimordialEggBlock.HATCHING, false));
                entity.markDirty();
            }
        }
    }

    private static void broadcast(List<ServerPlayerEntity> players, String text, ServerWorld world, BlockPos pos) {
        Text msg = Text.translatable(text);
        for (ServerPlayerEntity player : players) {
            player.sendMessage(msg, false); // Chat
            player.sendMessage(msg, true);  // Actionbar
        }
        world.playSound(null, pos, SamsaraSounds.CAVE_REVERSED, SoundCategory.AMBIENT, 1.0f, 0.9f);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.hatching = nbt.getBoolean("Hatching");
        this.hatchingTicks = nbt.getInt("HatchingTicks");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("Hatching", this.hatching);
        nbt.putInt("HatchingTicks", this.hatchingTicks);
    }
}
