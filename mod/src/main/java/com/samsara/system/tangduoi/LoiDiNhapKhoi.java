package com.samsara.system.tangduoi;

import com.samsara.registry.SamsaraBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * Tầng 2: các khối khổng lồ ở xa phía bắc, mỗi đảo tới thì có thêm khối mới. Cuối tầng chúng ghép thành một chiếc giường.
 *
 * Thực thể neo ở giữa lộ trình (trong vùng chunk đã tải suốt tầng) và hình được đẩy ra xa bằng translation, vì thực thể
 * ở đúng chỗ khối đứng sẽ nằm ngoài tầm tải chunk.
 */
public final class LoiDiNhapKhoi {
    private LoiDiNhapKhoi() {}

    private static final String TAG = "samsara_khoi_lon";
    private static final String STEP_TAG = "samsara_khoi_lon_";
    private static final BlockPos ANCHOR = LoiDiNhapBuilder.ORIGIN.add(0, 0, -110);
    private static final float CELL = 46.0f;
    private static final float SIZE = 44.0f;
    /** Toạ độ so với điểm neo: tâm chiếc giường ở xa về phía bắc, trên cao. */
    private static final float BED_X = -CELL * 1.5f;
    private static final float BED_Y = 122.0f;
    private static final float BED_Z = -440.0f;
    public static final int LAST = 5;
    private static final float SINK = 10.0f;
    private static final float PEDESTAL = 240.0f;

    private record Khoi(int step, BlockState state, float x, float y, float z, float size) {}

    private static Khoi cell(int step, BlockState state, int col, int row) {
        return new Khoi(step, state, BED_X + col * CELL, BED_Y + row * CELL, BED_Z, SIZE);
    }

    /** Khối đứng dưới biển: đáy chìm dưới mặt nước, chỉ khác nhau về kích cỡ. */
    private static Khoi onSea(int step, BlockState state, float x, float z, float size) {
        return new Khoi(step, state, x, size / 2 - SINK, z, size);
    }

    private static final BlockState PLANK = Blocks.OAK_PLANKS.getDefaultState();
    private static final BlockState RED = Blocks.RED_WOOL.getDefaultState();
    private static final BlockState WHITE = Blocks.WHITE_WOOL.getDefaultState();

    private static final Khoi[] KHOI = buildList();

    private static Khoi[] buildList() {
        List<Khoi> out = new ArrayList<>(List.of(
                cell(1, PLANK, 3, 0), cell(1, PLANK, 2, 0),
                cell(2, PLANK, 1, 0), cell(2, PLANK, 0, 0),
                cell(3, RED, 3, 1), cell(3, RED, 2, 1),
                cell(4, RED, 1, 1), cell(4, PLANK, -1, 0), cell(4, PLANK, -1, 1),
                cell(5, WHITE, 0, 1), cell(5, PLANK, -1, 2)));
        // Vài khối trắng và gạch men to như núi ở rất xa: chân chìm trong sương chân trời
        BlockState tile = SamsaraBlocks.GACH_MEN_O.getDefaultState();
        BlockState white = Blocks.WHITE_CONCRETE.getDefaultState();
        out.add(onSea(0, white, -300f, -320f, 260f));
        out.add(onSea(0, white, 320f, -270f, 200f));
        out.add(onSea(0, white, 280f, -530f, 150f));
        out.add(onSea(0, tile, -520f, -90f, 200f));
        out.add(onSea(0, tile, 560f, -10f, 120f));
        // Bệ gạch dưới chiếc giường: giường nằm trên mặt bệ, bệ đứng dưới biển
        float pedestalTop = BED_Y - SIZE / 2 - 1f;
        out.add(new Khoi(0, tile, BED_X + CELL, pedestalTop - PEDESTAL / 2, BED_Z, PEDESTAL));
        return out.toArray(new Khoi[0]);
    }

    private static Box area() {
        return new Box(ANCHOR).expand(4.0);
    }

    private static List<DisplayEntity.BlockDisplayEntity> all(ServerWorld world) {
        return world.getEntitiesByClass(DisplayEntity.BlockDisplayEntity.class, area(), e -> e.getCommandTags().contains(TAG));
    }

    public static void removeAll(ServerWorld world) {
        all(world).forEach(e -> e.discard());
    }

    /** Hiện các khối của bước {@code step}; khối nào đã có thì thôi. Có khối mới thì vang một tiếng đặt block rất trầm. */
    public static void reveal(ServerWorld world, ServerPlayerEntity player, int step, boolean sound) {
        List<DisplayEntity.BlockDisplayEntity> have = all(world);
        String stepTag = STEP_TAG + step;
        if (have.stream().anyMatch(e -> e.getCommandTags().contains(stepTag))) return;
        for (Khoi k : KHOI) {
            if (k.step == step) spawn(world, k, stepTag);
        }
        if (sound && step > 0) {
            world.playSound(null, player.getX(), player.getY() + 8, player.getZ(), SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 6.0f, 0.3f);
            world.playSound(null, player.getX(), player.getY() + 8, player.getZ(), SoundEvents.ENTITY_IRON_GOLEM_STEP, SoundCategory.BLOCKS, 6.0f, 0.2f);
        }
    }

    private static void spawn(ServerWorld world, Khoi k, String stepTag) {
        DisplayEntity.BlockDisplayEntity e = EntityType.BLOCK_DISPLAY.create(world);
        if (e == null) return;
        e.refreshPositionAndAngles(ANCHOR.getX() + 0.5, ANCHOR.getY(), ANCHOR.getZ() + 0.5, 0f, 0f);
        NbtCompound nbt = e.writeNbt(new NbtCompound());
        nbt.put("block_state", NbtHelper.fromBlockState(k.state));
        NbtCompound brightness = new NbtCompound();
        brightness.putInt("block", 15);
        brightness.putInt("sky", 15);
        nbt.put("brightness", brightness);
        nbt.putFloat("view_range", 30.0f);
        nbt.put("transformation", transform(k));
        e.readNbt(nbt);
        e.addCommandTag(TAG);
        e.addCommandTag(stepTag);
        e.addCommandTag(TAG + "_i" + index(k));
        world.spawnEntity(e);
    }

    private static int index(Khoi k) {
        for (int i = 0; i < KHOI.length; i++) {
            if (KHOI[i] == k) return i;
        }
        return 0;
    }

    /** Đứng yên, không xoay: phải trông nặng. */
    private static NbtCompound transform(Khoi k) {
        NbtCompound c = new NbtCompound();
        c.put("left_rotation", floats(0, 0, 0, 1));
        c.put("right_rotation", floats(0, 0, 0, 1));
        c.put("translation", floats(k.x - k.size / 2, k.y - k.size / 2, k.z - k.size / 2));
        c.put("scale", floats(k.size, k.size, k.size));
        return c;
    }

    private static NbtList floats(float... v) {
        NbtList l = new NbtList();
        for (float f : v) l.add(NbtFloat.of(f));
        return l;
    }
}
