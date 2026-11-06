package com.samsara.block;

import com.samsara.block.entity.CradleBlockEntity;
import com.samsara.registry.SamsaraBlockEntities;
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
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Cradle Block: The central altar at Y = -58 in the Bedrock Chamber.
 * Accepts 4 directional karmic sacrifices:
 * - Rotten Flesh (the decayed vessel)
 * - Ender Pearl (the gaze into distance)
 * - Ghast Tear (the lament of sorrow)
 * - Golden Apple (the illusion of life)
 */
public class CradleBlock extends BlockWithEntity {
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    public CradleBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(ACTIVE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CradleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, SamsaraBlockEntities.CRADLE_BLOCK_ENTITY, CradleBlockEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof CradleBlockEntity cradle)) {
            return ActionResult.PASS;
        }

        if (cradle.isSequenceActive()) {
            // Cannot interact during awakening ritual
            return ActionResult.CONSUME;
        }

        ItemStack held = player.getStackInHand(hand);

        if (!held.isEmpty()) {
            boolean accepted = false;
            int slot = -1;

            if (held.isOf(Items.ROTTEN_FLESH) && !cradle.hasItem(0)) {
                slot = 0;
                accepted = true;
            } else if (held.isOf(Items.ENDER_PEARL) && !cradle.hasItem(1)) {
                slot = 1;
                accepted = true;
            } else if (held.isOf(Items.GHAST_TEAR) && !cradle.hasItem(2)) {
                slot = 2;
                accepted = true;
            } else if (held.isOf(Items.GOLDEN_APPLE) && !cradle.hasItem(3)) {
                slot = 3;
                accepted = true;
            }

            if (accepted) {
                if (!world.isClient) {
                    cradle.insertSacrifice(slot, held.split(1));
                    world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1.0f, 0.8f);
                    world.playSound(null, pos, SamsaraSounds.CRADLE_HUM, SoundCategory.BLOCKS, 0.8f, 1.0f);
                    player.sendMessage(Text.translatable("samsara.cradle_block.accepts", slot + 1), true);
                }
                return ActionResult.SUCCESS;
            }
        }

        // Status query
        if (!world.isClient) {
            int count = cradle.getInsertedCount();
            player.sendMessage(Text.translatable("samsara.cradle_block.holds", count), true);
        }

        return ActionResult.SUCCESS;
    }
}
