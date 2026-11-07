package com.samsara.block;

import com.samsara.block.entity.MemoryDoorGiantBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Cánh cửa khổng lồ (Cửa Ký Ức phóng to, vẽ bởi MemoryDoorGiantRenderer). Block đặt ở chân cửa; bản thân block vô
 * hình, không va chạm.
 *  - COLOR: trắng / hồng / xanh, cùng texture với Cửa Ký Ức.
 *  - LIT: cánh hé 70° (cánh cửa trắng ở tường căn phòng cửa, cánh cửa trước Steve 2009); tắt = cánh đóng.
 *  - CURTAIN: màn sáng gần như đục sau đầu cánh cửa, chỉ để che vũ trụ bên ngoài căn phòng cửa (chỉ cánh cửa ở tường
 *    phòng cần; ở ngoài trời nó thành một mảng sáng lơ lửng vô duyên).
 *  - FLAT: nằm ngửa, không khung, như bị thổi bay khỏi khung (đáy Hố Nổ 2013).
 *  - BURNING: lửa linh hồn và khói bốc dọc mép cửa (trên tay Kẻ Đốt Nhà), chỉ là hạt phía client.
 * FACING = hướng mặt trước. WATERLOGGED để đặt được dưới nước mà không tạo túi khí.
 */
public class MemoryDoorGiantBlock extends BlockWithEntity implements Waterloggable {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final EnumProperty<Color> COLOR = EnumProperty.of("color", Color.class);
    public static final BooleanProperty LIT = BooleanProperty.of("lit");
    public static final BooleanProperty CURTAIN = BooleanProperty.of("curtain");
    public static final BooleanProperty FLAT = BooleanProperty.of("flat");
    public static final BooleanProperty BURNING = BooleanProperty.of("burning");
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    /** Hệ số phóng to (khớp MemoryDoorGiantRenderer.SCALE). */
    public static final float SCALE = 6.0f;

    public enum Color implements StringIdentifiable {
        WHITE("white"), PINK("pink"), BLUE("blue");

        private final String name;

        Color(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
    }

    public MemoryDoorGiantBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.SOUTH).with(COLOR, Color.WHITE).with(LIT, true)
                .with(CURTAIN, true).with(FLAT, false).with(BURNING, false).with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLOR, LIT, CURTAIN, FLAT, BURNING, WATERLOGGED);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    /** Cửa cháy: lửa linh hồn và khói dọc mép cửa (toạ độ theo cửa đứng, mặt trước theo FACING). */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(BURNING) || state.get(FLAT)) return;
        Direction facing = state.get(FACING);
        Direction right = facing.rotateYCounterclockwise();
        double half = SCALE * 0.5;
        double height = SCALE * 2.0;
        for (int i = 0; i < 6; i++) {
            // Chọn một điểm trên mép trái / phải / trên của cửa
            double side;
            double y;
            int edge = random.nextInt(4);
            if (edge == 0) { side = -half; y = random.nextDouble() * height; }
            else if (edge == 1) { side = half; y = random.nextDouble() * height; }
            else { side = (random.nextDouble() * 2 - 1) * half; y = height; }
            double x = pos.getX() + 0.5 + right.getOffsetX() * side + facing.getOffsetX() * 0.4;
            double z = pos.getZ() + 0.5 + right.getOffsetZ() * side + facing.getOffsetZ() * 0.4;
            world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, pos.getY() + y, z, 0.0, 0.02 + random.nextDouble() * 0.03, 0.0);
            if (random.nextInt(3) == 0) {
                world.addParticle(ParticleTypes.LARGE_SMOKE, x, pos.getY() + y + 0.5, z, 0.0, 0.05, 0.0);
            }
        }
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MemoryDoorGiantBlockEntity(pos, state);
    }
}
