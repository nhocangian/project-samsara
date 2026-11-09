package com.samsara.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/** Đèn trần của Đường Ống; AXIS là chiều chạy của ống, máng đèn luôn nằm ngang qua ống. */
public class DenOngBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;

    public DenOngBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(AXIS, Direction.Axis.Z));
    }

    /** Khoảng một phần ba số đèn thường có bọ bay quanh (đèn hỏng / chập chờn thì không). */
    @Override
    public void randomDisplayTick(BlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos,
                                  net.minecraft.util.math.random.Random random) {
        if (getClass() != DenOngBlock.class || Math.floorMod(pos.getX() + pos.getZ(), 3) != 0) return;
        world.addParticle(com.samsara.registry.SamsaraParticles.CON_BO,
                pos.getX() + 0.5, pos.getY() - 0.55, pos.getZ() + 0.5, 0, 0, 0);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}
