package com.samsara.block;

import com.samsara.block.entity.PrimordialEggBlockEntity;
import com.samsara.item.FracturedSlateItem;
import com.samsara.registry.SamsaraBlockEntities;
import com.samsara.registry.SamsaraItems;
import com.samsara.registry.SamsaraSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Primordial Egg (Đại Trứng Hắc Thạch):
 * Central monolith of The Womb dimension.
 * Merging with this egg concludes the soul's training and restarts the cycle.
 */
public class PrimordialEggBlock extends BlockWithEntity {
    public static final BooleanProperty HATCHING = BooleanProperty.of("hatching");

    public PrimordialEggBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(HATCHING, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HATCHING);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PrimordialEggBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, SamsaraBlockEntities.PRIMORDIAL_EGG_BE, PrimordialEggBlockEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof PrimordialEggBlockEntity eggBe)) {
            return ActionResult.PASS;
        }

        if (eggBe.isHatching()) {
            return ActionResult.CONSUME;
        }

        ItemStack held = player.getStackInHand(hand);
        if (held.isOf(SamsaraItems.FRACTURED_SLATE)) {
            NbtCompound nbt = held.getNbt();
            boolean complete = nbt != null &&
                    nbt.getBoolean(FracturedSlateItem.NBT_COW) &&
                    nbt.getBoolean(FracturedSlateItem.NBT_ZOMBIE) &&
                    nbt.getBoolean(FracturedSlateItem.NBT_ENDERMAN);

            if (complete) {
                if (!world.isClient) {
                    eggBe.startHatching(player);
                    world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.BLOCKS, 1.2f, 0.5f);
                    world.playSound(null, pos, SamsaraSounds.CRADLE_HUM, SoundCategory.BLOCKS, 1.0f, 0.6f);
                }
                return ActionResult.SUCCESS;
            } else {
                if (!world.isClient) {
                    player.sendMessage(Text.translatable("samsara.primordial_egg_block.phien_da_chua_thu"), true);
                }
                return ActionResult.SUCCESS;
            }
        } else {
            if (!world.isClient) {
                player.sendMessage(Text.translatable("samsara.primordial_egg_block.mot_khoi_cau_hac"), true);
            }
            return ActionResult.SUCCESS;
        }
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.2;
        double y = pos.getY() + 1.0 + random.nextDouble() * 1.5;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.2;

        world.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0.0, 0.05, 0.0);
        if (random.nextFloat() < 0.2f) {
            world.addParticle(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 0.0, 0.02, 0.0);
        }
    }
}
