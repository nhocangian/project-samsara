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
 * Tầng 2: các khối khổng lồ ở xa phía bắc, mỗi đảo tới thì có thêm khối mới (một chiếc giường ghép dở trên bệ gạch).
 * Bước vào đoạn nhiễu thì tất cả tắt cùng lúc: phòng cuối chỉ còn trời trống và Ophanim.
 *
 * Thực thể neo gần lộ trình (trong vùng chunk đã tải và tầm gửi thực thể ~128-160 block) và hình được đẩy ra xa bằng
 * translation. Lộ trình dài gần 300 block nên có hai điểm neo: qua đảo 3 thì cả bộ được dựng lại ở điểm neo thứ hai.
 */
public final class LoiDiNhapKhoi {
    private LoiDiNhapKhoi() {}

    private static final String TAG = "samsara_khoi_lon";
    private static final String STEP_TAG = "samsara_khoi_lon_";
    /** Toạ độ các khối viết theo điểm neo 0; điểm neo 1 lùi về bắc, hình được bù lại bằng translation. */
    private static final int[] ANCHOR_Z = {-110, -230};
    /** Đi về bắc qua z này thì đổi sang điểm neo 1, quay về nam qua SWITCH_BACK_Z thì về điểm neo 0. */
    private static final int SWITCH_Z = -165;
    private static final int SWITCH_BACK_Z = -155;
    private static final float CELL = 46.0f;
    private static final float SIZE = 44.0f;
    /** Toạ độ so với điểm neo: tâm chiếc giường ở xa về phía bắc, trên cao. */
    private static final float BED_X = -CELL * 1.5f;
    private static final float BED_Y = 122.0f;
    private static final float BED_Z = -440.0f;
    public static final int LAST = 6;
    private static final float SINK = 10.0f;
    private static final float PEDESTAL = 240.0f;

    private record Khoi(int step, BlockState state, float x, float y, float z, float size, float height) {}

    private static Khoi cell(int step, BlockState state, int col, int row) {
        return new Khoi(step, state, BED_X + col * CELL, BED_Y + row * CELL, BED_Z, SIZE, SIZE);
    }

    /** Khối đứng dưới biển: đáy chìm dưới mặt nước, chỉ khác nhau về kích cỡ. */
    private static Khoi onSea(int step, BlockState state, float x, float z, float size) {
        return new Khoi(step, state, x, size / 2 - SINK, z, size, size);
    }

    /** Trụ vuông đứng dưới biển, cao hơn rộng. */
    private static Khoi tower(BlockState state, float x, float z, float width, float height) {
        return new Khoi(0, state, x, height / 2 - SINK, z, width, height);
    }

    private static final BlockState PLANK = Blocks.OAK_PLANKS.getDefaultState();
    private static final BlockState RED = Blocks.RED_WOOL.getDefaultState();

    private static final Khoi[] KHOI = buildList();

    private static Khoi[] buildList() {
        List<Khoi> out = new ArrayList<>(List.of(
                cell(1, PLANK, 3, 0), cell(1, PLANK, 2, 0),
                cell(2, PLANK, 1, 0), cell(2, PLANK, 0, 0),
                cell(3, RED, 3, 1), cell(3, RED, 2, 1),
                cell(4, RED, 1, 1),
                cell(5, PLANK, -1, 0), cell(5, PLANK, -1, 1),
                cell(6, PLANK, -1, 2)));
        // Vài khối trắng và gạch men to như núi ở rất xa: chân chìm trong sương chân trời
        BlockState tile = SamsaraBlocks.GACH_MEN_O.getDefaultState();
        BlockState white = Blocks.WHITE_CONCRETE.getDefaultState();
        out.add(onSea(0, white, -300f, -320f, 260f));
        // Trụ khổng lồ xa phía trước bên phải: nhìn thẳng thì đỉnh vượt khỏi khung hình, nhưng vẫn thấy cạnh và góc
        out.add(tower(white, 900f, -1000f, 700f, 2600f));
        // Tít chân trời, cả phía sau lưng: chỉ còn là những bóng mờ cao vút trong không khí
        out.add(tower(white, -1400f, -1800f, 400f, 1800f));
        out.add(tower(white, -700f, -2600f, 600f, 3200f));
        out.add(tower(tile, 300f, -3000f, 500f, 1400f));
        out.add(tower(white, 1900f, -2200f, 450f, 2400f));
        out.add(tower(tile, 2600f, -700f, 350f, 1600f));
        out.add(tower(white, -2500f, -400f, 500f, 2200f));
        out.add(tower(tile, -2000f, 1500f, 300f, 1200f));
        out.add(tower(white, 1500f, 2000f, 600f, 2800f));
        // Tận cùng chân trời: nhỏ như cây kim nhưng vẫn nhận ra là tháp
        out.add(tower(white, -3200f, -2600f, 150f, 900f));
        out.add(tower(white, -1800f, -4200f, 120f, 700f));
        out.add(tower(tile, 800f, -4500f, 200f, 1100f));
        out.add(tower(white, 2400f, -3800f, 100f, 500f));
        out.add(tower(white, 3800f, -1600f, 160f, 800f));
        out.add(tower(tile, 4200f, 900f, 90f, 450f));
        out.add(tower(white, -4300f, 600f, 140f, 650f));
        out.add(tower(white, -600f, 4300f, 180f, 1000f));
        out.add(tower(tile, 2800f, 3400f, 110f, 600f));
        out.add(onSea(0, white, -420f, -560f, 150f));
        out.add(onSea(0, tile, -520f, -90f, 200f));
        out.add(onSea(0, tile, -260f, 130f, 120f));
        // Bệ gạch dưới chiếc giường: giường nằm trên mặt bệ, bệ đứng dưới biển
        float pedestalTop = BED_Y - SIZE / 2 - 1f;
        out.add(new Khoi(0, tile, BED_X + CELL, pedestalTop - PEDESTAL / 2, BED_Z, PEDESTAL, PEDESTAL));
        return out.toArray(new Khoi[0]);
    }

    private static BlockPos anchor(int index) {
        return LoiDiNhapBuilder.ORIGIN.add(0, 0, ANCHOR_Z[index]);
    }

    /** Điểm neo nên dùng ở z tương đối {@code relZ}, giữ nguyên điểm neo hiện tại trong khoảng chuyển tiếp. */
    public static int anchorFor(double relZ, int current) {
        if (relZ < SWITCH_Z) return 1;
        if (relZ > SWITCH_BACK_Z) return 0;
        return current;
    }

    private static List<DisplayEntity.BlockDisplayEntity> at(ServerWorld world, int index) {
        return world.getEntitiesByClass(DisplayEntity.BlockDisplayEntity.class, new Box(anchor(index)).expand(4.0),
                e -> e.getCommandTags().contains(TAG));
    }

    /** Xoá khối ở cả hai điểm neo (chỗ nào đang tải). Trả về số khối đã xoá. */
    public static int removeAll(ServerWorld world) {
        int n = 0;
        for (int i = 0; i < ANCHOR_Z.length; i++) {
            for (DisplayEntity.BlockDisplayEntity e : at(world, i)) {
                e.discard();
                n++;
            }
        }
        return n;
    }

    /** Dựng lại cả bộ ở điểm neo {@code index}: mọi khối tới bước {@code upTo}. */
    public static void rebuild(ServerWorld world, int index, int upTo) {
        removeAll(world);
        for (Khoi k : KHOI) {
            if (k.step <= upTo) spawn(world, k, index, STEP_TAG + k.step);
        }
    }

    /** Hiện các khối của bước {@code step} ở điểm neo {@code index}; khối nào đã có thì thôi. Có khối mới thì vang một tiếng đặt block rất trầm. */
    public static void reveal(ServerWorld world, ServerPlayerEntity player, int index, int step, boolean sound) {
        String stepTag = STEP_TAG + step;
        if (at(world, index).stream().anyMatch(e -> e.getCommandTags().contains(stepTag))) return;
        for (Khoi k : KHOI) {
            if (k.step == step) spawn(world, k, index, stepTag);
        }
        if (sound && step > 0) placeSound(world, player);
    }

    public static void placeSound(ServerWorld world, ServerPlayerEntity player) {
        world.playSound(null, player.getX(), player.getY() + 8, player.getZ(), SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 6.0f, 0.3f);
        world.playSound(null, player.getX(), player.getY() + 8, player.getZ(), SoundEvents.ENTITY_IRON_GOLEM_STEP, SoundCategory.BLOCKS, 6.0f, 0.2f);
    }

    /** Bước vào đoạn nhiễu: cả bầu trời khối tắt một lượt, kèm một tiếng trầm như bị xoá. */
    public static void vanish(ServerWorld world, ServerPlayerEntity player, boolean sound) {
        int n = removeAll(world);
        if (!sound || n == 0) return;
        world.playSound(null, player.getX(), player.getY() + 6, player.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 4.0f, 0.5f);
        world.playSound(null, player.getX(), player.getY() + 6, player.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.BLOCKS, 4.0f, 0.5f);
    }

    private static void spawn(ServerWorld world, Khoi k, int index, String stepTag) {
        DisplayEntity.BlockDisplayEntity e = EntityType.BLOCK_DISPLAY.create(world);
        if (e == null) return;
        BlockPos anchor = anchor(index);
        e.refreshPositionAndAngles(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5, 0f, 0f);
        NbtCompound nbt = e.writeNbt(new NbtCompound());
        nbt.put("block_state", NbtHelper.fromBlockState(k.state));
        NbtCompound brightness = new NbtCompound();
        brightness.putInt("block", 15);
        brightness.putInt("sky", 15);
        nbt.put("brightness", brightness);
        nbt.putFloat("view_range", 80.0f);   // x64 block: các trụ ở chân trời cách hơn 3000 block
        nbt.put("transformation", transform(k, ANCHOR_Z[0] - ANCHOR_Z[index]));
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
    private static NbtCompound transform(Khoi k, float shiftZ) {
        NbtCompound c = new NbtCompound();
        c.put("left_rotation", floats(0, 0, 0, 1));
        c.put("right_rotation", floats(0, 0, 0, 1));
        c.put("translation", floats(k.x - k.size / 2, k.y - k.height / 2, k.z + shiftZ - k.size / 2));
        c.put("scale", floats(k.size, k.height, k.size));
        return c;
    }

    private static NbtList floats(float... v) {
        NbtList l = new NbtList();
        for (float f : v) l.add(NbtFloat.of(f));
        return l;
    }
}
