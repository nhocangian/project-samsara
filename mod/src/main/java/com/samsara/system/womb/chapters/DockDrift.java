package com.samsara.system.womb.chapters;

import com.samsara.block.NamWallpaperBlock;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.womb.WombRoute;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Đồ đạc và mảng tường phòng ngủ của Nam trôi lơ lửng quanh Bến Đò, dựng bằng thực thể hiển thị (block display /
 * item display) để nghiêng góc bất kỳ, mỏng như tấm vách và chuyển động mượt: mỗi vật trôi lên xuống và xoay rất chậm
 * như chìm trong nước không trọng lực. Gần phòng thì thấp và còn nguyên; càng về phía cánh cửa trắng càng cao, nhỏ, thưa.
 *
 * Mỗi thực thể mang tag {@link #TAG} và một tag tham số "samsara_drift:yaw:pitch:roll:spin:bob:sx:sy:sz:item"
 * (góc độ, tốc độ xoay độ/giây, biên độ trôi block, cỡ; item = 1 nếu là item display, đã căn giữa sẵn).
 * {@link #register()} cập nhật biến đổi mỗi {@link #STEP} tick, có nội suy, chỉ khi có người chơi ở gần.
 */
public final class DockDrift {
    private DockDrift() {}

    public static final String TAG = "samsara_dock_drift";
    private static final String PARAMS = "samsara_drift:";
    private static final int STEP = 40;
    private static final double NEAR = 140.0;

    // ==========================================
    // DỰNG
    // ==========================================

    public static void build(ServerWorld world, BlockPos origin) {
        removeAll(world, origin);
        buildGlitches(world, origin);
        Random r = Random.create(origin.asLong() ^ 0xD21F7L);
        int fy = WombRoute.SURFACE_Y;           // chân người chơi đứng ở y 62
        BlockState wall = SamsaraBlocks.NAM_WALLPAPER.getDefaultState().with(NamWallpaperBlock.FACING, Direction.SOUTH);

        // Mảng tường mỏng: gần phòng thấp và to, xa dần thì cao, nhỏ, vỡ vụn
        for (int i = 0; i < 11; i++) {
            double along = 3 + i * 5.5 + r.nextDouble() * 3;
            double side = (i % 2 == 0 ? 1 : -1) * (3.5 + r.nextDouble() * 15);
            double y = fy + 4.5 + i * 0.9 + r.nextDouble() * 2.5;
            float k = (float) Math.max(0.45, 1.0 - i * 0.055);
            int variant = i == 2 ? 1 : i == 6 ? 2 : 0;
            spawnBlock(world, origin.getX() + side, y, origin.getZ() - along, wall.with(NamWallpaperBlock.VARIANT, variant),
                    r.nextFloat() * 360f, sign(r) * (12 + r.nextFloat() * 25), sign(r) * (8 + r.nextFloat() * 22),
                    sign(r) * (1.0f + r.nextFloat() * 2.5f), 0.15f + r.nextFloat() * 0.2f,
                    (2.2f + r.nextFloat()) * k, (1.8f + r.nextFloat()) * k, 0.18f, false);
        }
        // Một tấm cửa sổ kính vỡ khỏi tường
        spawnBlock(world, origin.getX() - 7.5, fy + 7.0, origin.getZ() - 9.0, Blocks.GLASS.getDefaultState(),
                40f, 18f, -10f, 1.5f, 0.2f, 1.2f, 1.2f, 0.06f, false);

        // Ván sàn gãy bong khỏi mảnh sàn, lơ lửng ngay mép phòng
        for (int i = 0; i < 7; i++) {
            double a = Math.toRadians(200 + i * 25 + r.nextDouble() * 10);
            double x = origin.getX() + 0.5 + Math.cos(a) * (6.5 + r.nextDouble() * 2);
            double z = origin.getZ() + 4.5 + Math.sin(a) * (6.0 + r.nextDouble() * 2);
            spawnBlock(world, x, fy + 0.6 + r.nextDouble() * 2.2, z, Blocks.OAK_PLANKS.getDefaultState(),
                    r.nextFloat() * 360f, sign(r) * r.nextFloat() * 25, sign(r) * r.nextFloat() * 15,
                    sign(r) * (2f + r.nextFloat() * 3f), 0.1f + r.nextFloat() * 0.1f, 1.0f, 0.12f, 0.24f, false);
        }

        // Đồ đạc trôi khỏi phòng
        // Sách rơi khỏi kệ (kệ ở x +4, z 3..4)
        for (int i = 0; i < 6; i++) {
            spawnItem(world, origin.getX() + 4.5 + r.nextDouble() * 2.5 - 1.5, fy + 1.8 + i * 0.55 + r.nextDouble() * 0.5,
                    origin.getZ() + 2.5 + r.nextDouble() * 3, new ItemStack(Items.BOOK),
                    r.nextFloat() * 360f, sign(r) * r.nextFloat() * 60, sign(r) * r.nextFloat() * 40,
                    sign(r) * (4f + r.nextFloat() * 6f), 0.12f, 0.55f);
        }
        // Gối bay lên khỏi giường (giường ở x -3, z 2..3)
        spawnBlock(world, origin.getX() - 2.5, fy + 1.6, origin.getZ() + 2.4, Blocks.WHITE_WOOL.getDefaultState(),
                20f, 8f, -6f, 2f, 0.15f, 0.75f, 0.22f, 0.45f, false);
        // Chăn đỏ bay lơ lửng
        spawnBlock(world, origin.getX() - 4.0, fy + 3.2, origin.getZ() + 5.0, Blocks.RED_WOOL.getDefaultState(),
                -30f, 14f, 9f, 1.2f, 0.2f, 1.6f, 0.08f, 1.3f, false);
        // Chiếc ghế thứ hai và một ngăn kéo
        spawnBlock(world, origin.getX() + 6.5, fy + 3.5, origin.getZ() - 2.0,
                Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST),
                35f, -20f, 25f, 2.5f, 0.2f, 0.8f, 0.8f, 0.8f, false);
        spawnBlock(world, origin.getX() + 5.5, fy + 5.0, origin.getZ() + 6.5, Blocks.SPRUCE_PLANKS.getDefaultState(),
                10f, 30f, -15f, 2f, 0.2f, 0.8f, 0.35f, 0.6f, false);
        // Khung ảnh, đồng hồ, chậu hoa hướng dương, giấy vở bay cao dần về phía cửa trắng
        spawnItem(world, origin.getX() - 6.0, fy + 4.2, origin.getZ() + 1.0, new ItemStack(Items.PAINTING),
                70f, 15f, 10f, 1.5f, 0.15f, 0.9f);
        spawnItem(world, origin.getX() + 2.5, fy + 4.5, origin.getZ() - 5.0, new ItemStack(Items.CLOCK),
                0f, 25f, 0f, 3f, 0.18f, 0.6f);
        spawnItem(world, origin.getX() - 3.5, fy + 5.5, origin.getZ() - 12.0, new ItemStack(Items.SUNFLOWER),
                0f, 10f, 20f, 1.5f, 0.2f, 0.9f);
        spawnItem(world, origin.getX() + 1.0, fy + 2.4, origin.getZ() + 0.5, new ItemStack(Items.WRITABLE_BOOK),
                45f, 70f, 0f, 3f, 0.12f, 0.5f);
        for (int i = 0; i < 8; i++) {
            spawnItem(world, origin.getX() + sign(r) * (1.5 + r.nextDouble() * 9), fy + 2.5 + i * 0.9 + r.nextDouble() * 1.5,
                    origin.getZ() + 3 - i * 4.5 - r.nextDouble() * 3, new ItemStack(Items.PAPER),
                    r.nextFloat() * 360f, sign(r) * (30 + r.nextFloat() * 50), sign(r) * r.nextFloat() * 30,
                    sign(r) * (6f + r.nextFloat() * 8f), 0.2f + r.nextFloat() * 0.15f, 0.5f);
        }
    }

    /**
     * Lớp vỏ glitch phủ lên vài vật thể của căn phòng: block display của womb_glitch đúng khuôn (outline) của block bên
     * dưới, phóng nhỉnh 0.012 block mỗi phía để không trùng mặt phẳng (gây chớp). Chỉ phủ khi block đó thực sự có
     * (sàn mép vỡ, tường cao ngẫu nhiên, ván bến thiếu tấm). Texture là chính chất liệu của vật bị xé lát nên biến thể
     * theo chất liệu. Không có tag tham số nên không chuyển động. Màn hình máy tính tự giật bằng texture động riêng.
     */
    private static void buildGlitches(ServerWorld world, BlockPos origin) {
        int y = WombRoute.SURFACE_Y;
        int[][] targets = {
                // x, y, z (tương đối gốc tuyến, y so với mặt nước), biến thể: 0 giấy dán tường 1 ván 2 giường 3 gỗ khung
                {-3, 0, 2, 2}, {-3, 0, 3, 2},                       // giường
                {-5, 1, 4, 0}, {-5, 0, 3, 0},                       // tường tây (mảng còn treo ảnh)
                {-1, 0, 8, 0}, {-2, 0, 8, 0},                       // tường nam (khoảng 0..2 đã thành Khối Mã)
                {1, 0, -2, 3}, {1, 1, -2, 3}, {1, 2, -2, 3},        // khung cửa phòng ngủ
                {-1, -1, 0, 1}, {-2, -1, 1, 1},                     // sàn
                {0, -1, -9, 1}, {-1, -1, -9, 1}, {0, -1, -10, 1},   // ván bến
        };
        float pad = 0.012f;
        int n = 0;
        for (int[] t : targets) {
            int variant = t[3] * 2 + (n++ % 2);   // xen kẽ 2 nhịp của cùng chất liệu
            BlockPos pos = origin.withY(y).add(t[0], t[1], t[2]);
            BlockState st = world.getBlockState(pos);
            if (st.isAir() || st.isOf(Blocks.WATER) || st.isOf(Blocks.LIGHT) || st.isOf(SamsaraBlocks.WOMB_CODE)) continue;
            net.minecraft.util.shape.VoxelShape shape = st.getOutlineShape(world, pos);
            if (shape.isEmpty()) continue;
            Box bb = shape.getBoundingBox();
            DisplayEntity.BlockDisplayEntity e = EntityType.BLOCK_DISPLAY.create(world);
            if (e == null) continue;
            e.refreshPositionAndAngles(pos.getX() + bb.minX, pos.getY() + bb.minY, pos.getZ() + bb.minZ, 0f, 0f);
            NbtCompound nbt = e.writeNbt(new NbtCompound());
            nbt.put("block_state", NbtHelper.fromBlockState(SamsaraBlocks.WOMB_GLITCH.getDefaultState()
                    .with(com.samsara.block.WombGlitchBlock.VARIANT, variant)));
            NbtCompound brightness = new NbtCompound();
            brightness.putInt("block", 13);
            brightness.putInt("sky", 0);
            nbt.put("brightness", brightness);
            NbtCompound tr = new NbtCompound();
            tr.put("left_rotation", floats(0, 0, 0, 1));
            tr.put("right_rotation", floats(0, 0, 0, 1));
            tr.put("translation", floats(-pad, -pad, -pad));
            tr.put("scale", floats((float) bb.getXLength() + 2 * pad, (float) bb.getYLength() + 2 * pad,
                    (float) bb.getZLength() + 2 * pad));
            nbt.put("transformation", tr);
            e.readNbt(nbt);
            e.addCommandTag(TAG);
            world.spawnEntity(e);
        }
    }

    private static float sign(Random r) {
        return r.nextBoolean() ? 1f : -1f;
    }

    private static void removeAll(ServerWorld world, BlockPos origin) {
        world.getEntitiesByClass(DisplayEntity.class, area(origin), e -> e.getCommandTags().contains(TAG))
                .forEach(Entity::discard);
    }

    private static Box area(BlockPos origin) {
        return new Box(origin.getX() - 50, WombRoute.SURFACE_Y - 4, origin.getZ() - 90,
                origin.getX() + 50, WombRoute.SURFACE_Y + 40, origin.getZ() + 30);
    }

    private static void spawnBlock(ServerWorld world, double x, double y, double z, BlockState state,
                                   float yaw, float pitch, float roll, float spin, float bob,
                                   float sx, float sy, float sz, boolean unused) {
        DisplayEntity.BlockDisplayEntity e = EntityType.BLOCK_DISPLAY.create(world);
        if (e == null) return;
        NbtCompound extra = new NbtCompound();
        extra.put("block_state", NbtHelper.fromBlockState(state));
        spawn(world, e, x, y, z, extra, new float[]{yaw, pitch, roll, spin, bob, sx, sy, sz}, false);
    }

    private static void spawnItem(ServerWorld world, double x, double y, double z, ItemStack stack,
                                  float yaw, float pitch, float roll, float spin, float bob, float scale) {
        DisplayEntity.ItemDisplayEntity e = EntityType.ITEM_DISPLAY.create(world);
        if (e == null) return;
        NbtCompound extra = new NbtCompound();
        extra.put("item", stack.writeNbt(new NbtCompound()));
        extra.putString("item_display", "fixed");
        spawn(world, e, x, y, z, extra, new float[]{yaw, pitch, roll, spin, bob, scale, scale, scale}, true);
    }

    private static void spawn(ServerWorld world, DisplayEntity e, double x, double y, double z, NbtCompound extra,
                              float[] p, boolean item) {
        e.refreshPositionAndAngles(x, y, z, 0f, 0f);
        NbtCompound nbt = e.writeNbt(new NbtCompound());
        nbt.copyFrom(extra);
        NbtCompound brightness = new NbtCompound();
        brightness.putInt("block", 9);   // 12 làm giấy dán tường cháy trắng dưới shader
        brightness.putInt("sky", 0);
        nbt.put("brightness", brightness);
        nbt.putFloat("view_range", 2.0f);
        nbt.put("transformation", transform(p, item, phase(x, z), world.getTime() / 20.0));
        e.readNbt(nbt);
        e.addCommandTag(TAG);
        StringBuilder tag = new StringBuilder(PARAMS);
        for (float v : p) tag.append(String.format(java.util.Locale.ROOT, "%.3f:", v));
        tag.append(item ? 1 : 0);
        e.addCommandTag(tag.toString());
        world.spawnEntity(e);
    }

    // ==========================================
    // CHUYỂN ĐỘNG
    // ==========================================

    private static double phase(double x, double z) {
        return (Math.floorMod((long) (x * 31 + z * 17), 628L)) / 100.0;
    }

    /** Biến đổi tại thời điểm {@code t} giây: xoay quanh trục đứng + nghiêng cố định, trôi lên xuống, xoay quanh tâm vật. */
    private static NbtCompound transform(float[] p, boolean item, double phase, double t) {
        float yaw = (float) Math.toRadians(p[0] + p[3] * t);
        float pitch = (float) Math.toRadians(p[1] + 4.0 * Math.sin(t * 0.21 + phase));
        float roll = (float) Math.toRadians(p[2] + 4.0 * Math.cos(t * 0.17 + phase));
        Quaternionf q = new Quaternionf().rotateY(yaw).rotateX(pitch).rotateZ(roll);
        Vector3f scale = new Vector3f(p[5], p[6], p[7]);
        float bob = (float) (p[4] * Math.sin(t * 0.35 + phase));
        Vector3f translation = item ? new Vector3f(0, bob, 0)
                : q.transform(new Vector3f(scale).mul(-0.5f)).add(0, bob, 0);   // xoay quanh tâm khối
        NbtCompound c = new NbtCompound();
        c.put("left_rotation", floats(q.x, q.y, q.z, q.w));
        c.put("right_rotation", floats(0, 0, 0, 1));
        c.put("translation", floats(translation.x, translation.y, translation.z));
        c.put("scale", floats(scale.x, scale.y, scale.z));
        return c;
    }

    private static NbtList floats(float... v) {
        NbtList l = new NbtList();
        for (float f : v) l.add(NbtFloat.of(f));
        return l;
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % STEP != 0 || !world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY)) return;
            BlockPos origin = EggCycleState.get(world.getServer()).getRouteOrigin();
            if (origin == null) return;
            boolean near = world.getPlayers().stream().anyMatch(pl -> pl.squaredDistanceTo(
                    origin.getX(), origin.getY(), origin.getZ() - 20) < NEAR * NEAR);
            if (!near) return;
            double t = (world.getTime() + STEP) / 20.0;   // vị trí đích khi nội suy xong
            List<DisplayEntity> list = world.getEntitiesByClass(DisplayEntity.class, area(origin),
                    e -> e.getCommandTags().contains(TAG));
            for (DisplayEntity e : list) {
                float[] p = null;
                boolean item = false;
                for (String tag : e.getCommandTags()) {
                    if (!tag.startsWith(PARAMS)) continue;
                    String[] s = tag.substring(PARAMS.length()).split(":");
                    p = new float[8];
                    for (int i = 0; i < 8; i++) p[i] = Float.parseFloat(s[i]);
                    item = s[8].equals("1");
                }
                if (p == null) continue;
                NbtCompound nbt = e.writeNbt(new NbtCompound());
                nbt.put("transformation", transform(p, item, phase(e.getX(), e.getZ()), t));
                nbt.putInt("interpolation_duration", STEP);
                nbt.putInt("start_interpolation", 0);
                e.readNbt(nbt);
            }
        });
    }
}
