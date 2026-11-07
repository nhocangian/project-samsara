package com.samsara.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

import java.util.Map;

/**
 * Khối Mã: block đen có mã nhị phân xanh rơi kiểu matrix (Bến Đò, căn phòng đang rã thành dữ liệu). Không entity,
 * không renderer: chỉ texture động và model của chính block (tools/generate_code_block.py).
 *  - Liền mạch giữa các Khối Mã: texture 4x4 block lặp liền; TX/TY/TZ (toạ độ thế giới mod 4) chọn ô texture cho mặt.
 *  - Viền tan vào block bên cạnh (connected texture): NORTH..DOWN = bên đó là một block khác (rắn, không phải Khối Mã);
 *    model mọc thêm dải mã thưa dần phủ lên mặt block đó. Tự cập nhật khi hàng xóm đổi.
 * Đặt bằng code thì dùng {@link #at(BlockState, BlockPos)} rồi {@link #refresh(WorldAccess, BlockPos)} sau khi xong cả mảng.
 */
public class WombCodeBlock extends Block {
    public static final IntProperty TX = IntProperty.of("tx", 0, 3);
    public static final IntProperty TY = IntProperty.of("ty", 0, 3);
    public static final IntProperty TZ = IntProperty.of("tz", 0, 3);
    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty WEST = Properties.WEST;
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty DOWN = Properties.DOWN;
    private static final Map<Direction, BooleanProperty> SIDES = Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST,
            Direction.SOUTH, SOUTH, Direction.WEST, WEST, Direction.UP, UP, Direction.DOWN, DOWN);

    public WombCodeBlock(Settings settings) {
        super(settings);
        BlockState s = getDefaultState().with(TX, 0).with(TY, 0).with(TZ, 0);
        for (BooleanProperty p : SIDES.values()) s = s.with(p, false);
        setDefaultState(s);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(TX, TY, TZ, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    /** Trạng thái đúng ô texture cho vị trí {@code pos} (để mã nối liền với các Khối Mã bên cạnh). */
    public static BlockState at(BlockState state, BlockPos pos) {
        return state.with(TX, Math.floorMod(pos.getX(), 4)).with(TY, Math.floorMod(pos.getY(), 4))
                .with(TZ, Math.floorMod(pos.getZ(), 4));
    }

    /** Bên {@code dir} có phải một block khác để viền mã tan lên không. */
    private static boolean blends(BlockView world, BlockPos pos, Direction dir) {
        BlockPos n = pos.offset(dir);
        BlockState s = world.getBlockState(n);
        if (s.isAir() || s.getBlock() instanceof WombCodeBlock || s.isOf(Blocks.WATER) || s.isOf(Blocks.LIGHT)) return false;
        return s.isSideSolidFullSquare(world, n, dir.getOpposite());
    }

    private static BlockState withSides(BlockState state, BlockView world, BlockPos pos) {
        for (Map.Entry<Direction, BooleanProperty> e : SIDES.entrySet()) {
            state = state.with(e.getValue(), blends(world, pos, e.getKey()));
        }
        return state;
    }

    /** Tính lại viền của Khối Mã tại {@code pos} (gọi sau khi dựng xong một mảng bằng code). */
    public static void refresh(WorldAccess world, BlockPos pos) {
        BlockState s = world.getBlockState(pos);
        if (s.getBlock() instanceof WombCodeBlock) {
            BlockState n = withSides(s, world, pos);
            if (n != s) world.setBlockState(pos, n, 2);
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return withSides(at(getDefaultState(), ctx.getBlockPos()), ctx.getWorld(), ctx.getBlockPos());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return state.with(SIDES.get(direction), blends(world, pos, direction));
    }
}
