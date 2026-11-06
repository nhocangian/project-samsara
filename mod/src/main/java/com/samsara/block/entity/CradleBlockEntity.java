package com.samsara.block.entity;

import com.samsara.block.CradleBlock;
import com.samsara.registry.SamsaraBlockEntities;
import com.samsara.registry.SamsaraSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Handles the state, inventory, and endgame narrative sequence of The Cradle.
 */
public class CradleBlockEntity extends BlockEntity {
    private final DefaultedList<ItemStack> sacrifices = DefaultedList.ofSize(4, ItemStack.EMPTY);

    private boolean sequenceActive = false;
    private int sequenceTicks = 0;
    public static final int TOTAL_SEQUENCE_TICKS = 240; // 12 seconds @ 20 tps

    public CradleBlockEntity(BlockPos pos, BlockState state) {
        super(SamsaraBlockEntities.CRADLE_BLOCK_ENTITY, pos, state);
    }

    public boolean hasItem(int slot) {
        if (slot >= 0 && slot < sacrifices.size()) {
            return !sacrifices.get(slot).isEmpty();
        }
        return false;
    }

    public void insertSacrifice(int slot, ItemStack stack) {
        if (slot >= 0 && slot < sacrifices.size()) {
            sacrifices.set(slot, stack);
            markDirty();
            checkActivation();
        }
    }

    public int getInsertedCount() {
        int count = 0;
        for (ItemStack stack : sacrifices) {
            if (!stack.isEmpty()) count++;
        }
        return count;
    }

    public boolean isSequenceActive() {
        return sequenceActive;
    }

    private void checkActivation() {
        if (sequenceActive) return;
        if (getInsertedCount() == 4) {
            this.sequenceActive = true;
            this.sequenceTicks = 0;
            if (world != null && !world.isClient) {
                world.setBlockState(pos, getCachedState().with(CradleBlock.ACTIVE, true));
                world.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 1.0f, 0.5f);
                world.playSound(null, pos, SamsaraSounds.CRADLE_HUM, SoundCategory.BLOCKS, 1.0f, 0.6f);
            }
            markDirty();
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, CradleBlockEntity entity) {
        if (world.isClient) {
            if (entity.sequenceActive) {
                // Ambient vortex particles on client
                double px = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 1.5;
                double py = pos.getY() + 1.2 + world.random.nextDouble() * 1.5;
                double pz = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 1.5;
                world.addParticle(ParticleTypes.REVERSE_PORTAL, px, py, pz, 0, 0.05, 0);
            }
            return;
        }

        if (!(world instanceof ServerWorld serverWorld)) return;

        if (entity.sequenceActive) {
            entity.sequenceTicks++;
            int t = entity.sequenceTicks;

            // Target players within a 24-block radius of the cradle
            Box bounds = new Box(pos).expand(24.0);
            List<ServerPlayerEntity> players = serverWorld.getEntitiesByClass(ServerPlayerEntity.class, bounds, p -> true);

            for (ServerPlayerEntity player : players) {
                // Lock movement completely
                player.setVelocity(Vec3d.ZERO);
                player.velocityModified = true;

                // Restrict movement via status effects
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 255, false, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 255, false, false, false));
            }

            // Staged Dialogue and Audio Sequence
            if (t == 30) {
                broadcastDialogue(players, "samsara.cradle_block_entity.why_did_you_think", serverWorld, pos);
            } else if (t == 90) {
                broadcastDialogue(players, "samsara.cradle_block_entity.every_blade_you_swung", serverWorld, pos);
            } else if (t == 150) {
                broadcastDialogue(players, "samsara.cradle_block_entity.every_creature_you_bled", serverWorld, pos);
            } else if (t == 210) {
                broadcastDialogue(players, "samsara.cradle_block_entity.the_cycle_has_broken", serverWorld, pos);
                serverWorld.playSound(null, pos, SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.BLOCKS, 1.2f, 0.5f);
            } else if (t >= TOTAL_SEQUENCE_TICKS) {
                // Final Awakening Resolution
                for (ServerPlayerEntity player : players) {
                    // Massive XP burst
                    ExperienceOrbEntity.spawn(serverWorld, player.getPos(), 1500);

                    // Totem animation and screen effect
                    serverWorld.sendEntityStatus(player, (byte) 35);
                    serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 0.8f);

                    // Gentle disconnection packet with the Egg conclusion
                    Text kickReason = Text.translatable("samsara.cradle_block_entity.the_seal_has_shattered");

                    player.networkHandler.disconnect(kickReason);
                }

                // Reset sequence state
                entity.sequenceActive = false;
                entity.sequenceTicks = 0;
                for (int i = 0; i < entity.sacrifices.size(); i++) {
                    entity.sacrifices.set(i, ItemStack.EMPTY);
                }
                serverWorld.setBlockState(pos, state.with(CradleBlock.ACTIVE, false));
                entity.markDirty();
            }
        }
    }

    private static void broadcastDialogue(List<ServerPlayerEntity> players, String message, ServerWorld world, BlockPos pos) {
        Text dialogue = Text.translatable(message);
        for (ServerPlayerEntity player : players) {
            player.sendMessage(dialogue, false); // Chat
            player.sendMessage(dialogue, true);  // Action bar
        }
        world.playSound(null, pos, SamsaraSounds.CAVE_REVERSED, SoundCategory.AMBIENT, 1.0f, 0.8f);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.sequenceActive = nbt.getBoolean("SequenceActive");
        this.sequenceTicks = nbt.getInt("SequenceTicks");

        NbtList list = nbt.getList("Sacrifices", 10);
        for (int i = 0; i < list.size() && i < sacrifices.size(); i++) {
            NbtCompound itemNbt = list.getCompound(i);
            int slot = itemNbt.getByte("Slot") & 255;
            if (slot < sacrifices.size()) {
                sacrifices.set(slot, ItemStack.fromNbt(itemNbt));
            }
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("SequenceActive", this.sequenceActive);
        nbt.putInt("SequenceTicks", this.sequenceTicks);

        NbtList list = new NbtList();
        for (int i = 0; i < sacrifices.size(); i++) {
            if (!sacrifices.get(i).isEmpty()) {
                NbtCompound itemNbt = new NbtCompound();
                itemNbt.putByte("Slot", (byte) i);
                sacrifices.get(i).writeNbt(itemNbt);
                list.add(itemNbt);
            }
        }
        nbt.put("Sacrifices", list);
    }
}
