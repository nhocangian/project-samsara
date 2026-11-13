package com.samsara.system.trontim;

import com.samsara.entity.EmEntity;
import com.samsara.registry.SamsaraEntities;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Cảnh cuối tầng 3: lúc <System> nói, căn nhà và đồ đạc biến mất, chỉ còn giường, người chơi và đám người trước mặt, đứng
 * trên một mẩu đất giữa hư không. Xung quanh là rễ cây khổng lồ uốn vòm trên đầu và những bậc đất cỏ lơ lửng; người chơi
 * chỉ thấy chúng qua mấy đợt sóng lớn (EchoRenderer nhận qua gói {@link #WAVE}). Đợt thứ tư soi ra rất nhiều người đứng
 * trên các bậc đất, gần có xa có, cùng nhìn về người chơi; đợt cuối thu từ ngoài vào tâm rồi tối hẳn.
 */
public final class TronTimFinale {
    private TronTimFinale() {}

    /** Gói server → client: một đợt sóng lớn (tâm, bán kính, tốc độ, độ tắt, thu vào hay lan ra, tiếng). */
    public static final Identifier WAVE = new Identifier("samsara", "tron_tim_wave");

    private static final int FLAGS = 2 | 16;
    /** Tâm của cảnh: phòng ngủ cũ. */
    private static final BlockPos CENTER = TronTimBuilder.at(-7, 0, -34);
    /** Không dựng gì gần tâm hơn khoảng này: đợt sóng đầu chỉ được thấy mẩu đất dưới chân và khoảng trống. */
    private static final double CLEAR_RADIUS = 15.0;
    private static final long SEED = 0x2012_0404L;
    /** Khối đánh dấu "cảnh cuối đã dựng" (sâu dưới tâm, không ai thấy). */
    private static final BlockPos SENTINEL = CENTER.down(40);

    private static final List<Vec3d> CROWD_SPOTS = new ArrayList<>();
    /** Mặt các bậc đất (tâm, bán kính) để dồn người lên những bậc người chơi nhìn thấy được. */
    private static final List<int[]> TERRACES = new ArrayList<>();
    /** Việc dựng cảnh xếp hàng, mỗi tick làm vài việc (một đoạn rễ, một bậc đất) để server không khựng. */
    private static final java.util.ArrayDeque<Runnable> JOBS = new java.util.ArrayDeque<>();
    private static final int CHUNK_RADIUS = 11;
    private static boolean ticketed = false;
    /** Cảnh cuối đang chạy: căn nhà đã biến mất, tiếng của nó (xích đu, đồng hồ, vòi nước) phải im. */
    private static boolean active = false;
    /** Tối đa bấy nhiêu người đứng trên các bậc đất ở đợt sóng thứ tư. */
    private static final int MULTITUDE = 230;

    public static boolean isActive() {
        return active;
    }

    /**
     * Lúc bấm giường: xin server sinh sẵn các chunk của cảnh cuối (ở nền, không chặn), để lúc dựng không phải sinh chunk
     * ngay trên luồng chính (lần đầu thử: khựng 5 giây).
     */
    public static void prepare(ServerWorld world) {
        if (ticketed) return;
        ticketed = true;
        net.minecraft.util.math.ChunkPos c = new net.minecraft.util.math.ChunkPos(CENTER);
        for (int x = -CHUNK_RADIUS; x <= CHUNK_RADIUS; x++) {
            for (int z = -CHUNK_RADIUS; z <= CHUNK_RADIUS; z++) {
                net.minecraft.util.math.ChunkPos p = new net.minecraft.util.math.ChunkPos(c.x + x, c.z + z);
                world.getChunkManager().addTicket(net.minecraft.server.world.ChunkTicketType.FORCED, p, 1, p);
            }
        }
    }

    /** Rời tầng: trả các chunk đã xin. */
    public static void release(ServerWorld world) {
        active = false;
        if (!ticketed) return;
        ticketed = false;
        JOBS.clear();
        net.minecraft.util.math.ChunkPos c = new net.minecraft.util.math.ChunkPos(CENTER);
        for (int x = -CHUNK_RADIUS; x <= CHUNK_RADIUS; x++) {
            for (int z = -CHUNK_RADIUS; z <= CHUNK_RADIUS; z++) {
                net.minecraft.util.math.ChunkPos p = new net.minecraft.util.math.ChunkPos(c.x + x, c.z + z);
                world.getChunkManager().removeTicket(net.minecraft.server.world.ChunkTicketType.FORCED, p, 1, p);
            }
        }
    }

    /** Mỗi tick: làm vài việc dựng cảnh đang chờ. */
    public static void tick() {
        for (int i = 0; i < 2 && !JOBS.isEmpty(); i++) JOBS.poll().run();
    }

    /** Lúc <System> nói: xoá nhà (trừ giường), đặt mẩu đất dưới chân, giữ người chơi đứng yên; cảnh xung quanh dựng dần. */
    public static void begin(ServerWorld world, ServerPlayerEntity player) {
        prepare(world);
        active = true;
        vanishHouse(world);
        // Nét của những tiếng "anh oi" trước đó vẽ theo căn nhà cũ: xoá ngay, kẻo một góc nhà "ma" còn lơ lửng
        wave(player, 0f, 1f, 0f, false, 3);
        JOBS.clear();
        landscapeJobs(world, false);
        world.setBlockState(SENTINEL, Blocks.BARRIER.getDefaultState(), FLAGS);
        island(world, player.getBlockPos().down(), false);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 900, 255, false, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 900, 200, false, false, false));
        for (EmEntity e : world.getEntitiesByClass(EmEntity.class, area(), EmEntity::isCrowd)) e.setNoGravity(true);
    }

    /** Lệnh dựng lại tầng: gỡ cảnh cuối (nếu đã chạy) bằng chính bộ sinh, đặt không khí thay cho khối. */
    public static void clear(ServerWorld world) {
        // Chỉ gỡ khi cảnh đã từng được dựng (khối đánh dấu dưới tâm); không thì khỏi chạm vào cả trăm chunk xa
        if (!world.getBlockState(SENTINEL).isOf(Blocks.BARRIER)) return;
        world.setBlockState(SENTINEL, Blocks.AIR.getDefaultState(), FLAGS);
        JOBS.clear();
        landscapeJobs(world, true);
        while (!JOBS.isEmpty()) JOBS.poll().run();
    }

    public static Box area() {
        return new Box(CENTER).expand(170, 120, 170);
    }

    private static void vanishHouse(ServerWorld world) {
        BlockState air = Blocks.AIR.getDefaultState();
        for (int x = -26; x <= 26; x++) {
            for (int z = -50; z <= 24; z++) {
                for (int y = -1; y <= 24; y++) {
                    BlockPos p = TronTimBuilder.at(x, y, z);
                    BlockState s = world.getBlockState(p);
                    if (s.isAir() || s.getBlock() instanceof BedBlock) continue;
                    world.setBlockState(p, air, FLAGS);
                }
            }
        }
    }

    /** Mẩu đất cỏ dưới chân người chơi (và dưới giường), thuôn nhỏ dần xuống dưới. */
    private static void island(ServerWorld world, BlockPos top, boolean erase) {
        disc(world, top, 7, 4, erase);
        for (int x = -12; x <= -2; x += 8) disc(world, TronTimBuilder.at(x, -1, -37), 2, 2, erase);
    }

    // ==========================================
    // BỘ SINH CẢNH: RỄ CÂY VÀ BẬC ĐẤT LƠ LỬNG
    // ==========================================

    private static void landscapeJobs(ServerWorld world, boolean erase) {
        Random random = Random.create(SEED);
        if (!erase) {
            CROWD_SPOTS.clear();
            TERRACES.clear();
        }
        BlockState wood = erase ? Blocks.AIR.getDefaultState() : Blocks.DARK_OAK_WOOD.getDefaultState();

        // Rễ / thân cây khổng lồ: mọc từ dưới hư không lên, uốn thành vòm trên đầu rồi cắm xuống phía bên kia
        int trunks = 10;
        for (int i = 0; i < trunks; i++) {
            double a = i * Math.PI * 2 / trunks + (random.nextDouble() - 0.5) * 0.35;
            double b = a + Math.PI * (0.55 + random.nextDouble() * 0.5) * (random.nextBoolean() ? 1 : -1);
            double r0 = 60 + random.nextDouble() * 45, r1 = 60 + random.nextDouble() * 50;
            Vec3d p0 = offset(a, r0, -64);
            Vec3d p3 = offset(b, r1, -60);
            double peak = 45 + random.nextDouble() * 40;
            Vec3d p1 = offset(a, r0 * 0.75, peak + 15);
            Vec3d p2 = offset(b, r1 * 0.75, peak + 15);
            JOBS.add(() -> curve(world, wood, p0, p1, p2, p3, 5.0, 2.8, erase));
            // Ba nhánh tách ra từ thân, vươn xa
            for (int k = 0; k < 3; k++) {
                double t = 0.2 + random.nextDouble() * 0.6;
                Vec3d from = bezier(p0, p1, p2, p3, t);
                double c = random.nextDouble() * Math.PI * 2;
                Vec3d to = from.add(Math.cos(c) * 42, (random.nextDouble() - 0.6) * 40, Math.sin(c) * 42);
                Vec3d mid = from.lerp(to, 0.5).add(0, 14, 0);
                JOBS.add(() -> curve(world, wood, from, mid, mid, to, 2.6, 1.1, erase));
            }
        }

        // Bậc đất cỏ lơ lửng, gần có xa có, vài cái có bậc thứ hai cao hơn
        for (int i = 0; i < 72; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double d = 20 + random.nextDouble() * 105;
            int y = (int) Math.round(-40 + random.nextDouble() * 68);
            int radius = 3 + (int) (random.nextDouble() * (d > 60 ? 8 : 5));
            BlockPos top = BlockPos.ofFloored(offset(a, d, y));
            int thick = 3 + random.nextInt(3);
            JOBS.add(() -> disc(world, top, radius, thick, erase));
            spots(top, radius, random);
            if (!erase) TERRACES.add(new int[]{top.getX(), top.getY(), top.getZ(), radius});
            if (random.nextFloat() < 0.4f) {
                BlockPos step = top.add((int) (Math.cos(a + 1.2) * radius), 2, (int) (Math.sin(a + 1.2) * radius));
                JOBS.add(() -> disc(world, step, Math.max(2, radius - 2), 2, erase));
                spots(step, Math.max(2, radius - 2), random);
                if (!erase) TERRACES.add(new int[]{step.getX(), step.getY(), step.getZ(), Math.max(2, radius - 2)});
            }
        }
    }

    private static Vec3d offset(double angle, double dist, double y) {
        return new Vec3d(CENTER.getX() + 0.5 + Math.cos(angle) * dist, CENTER.getY() + y, CENTER.getZ() + 0.5 + Math.sin(angle) * dist);
    }

    private static Vec3d bezier(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
        double u = 1 - t;
        return p0.multiply(u * u * u).add(p1.multiply(3 * u * u * t)).add(p2.multiply(3 * u * t * t)).add(p3.multiply(t * t * t));
    }

    /** Một đường cong Bezier dày, bán kính từ {@code rEnd} ở hai đầu thuôn về {@code rMid} ở giữa. */
    private static void curve(ServerWorld world, BlockState state, Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double rEnd, double rMid, boolean erase) {
        double length = p0.distanceTo(p1) + p1.distanceTo(p2) + p2.distanceTo(p3);
        int steps = (int) (length / 1.2);
        for (int s = 0; s <= steps; s++) {
            double t = (double) s / steps;
            double r = rEnd + (rMid - rEnd) * Math.sin(Math.PI * t);
            Vec3d c = bezier(p0, p1, p2, p3, t);
            if (c.distanceTo(Vec3d.ofCenter(CENTER)) < CLEAR_RADIUS + r) continue;
            sphere(world, state, c, r, erase);
        }
    }

    private static void sphere(ServerWorld world, BlockState state, Vec3d c, double r, boolean erase) {
        int ri = (int) Math.ceil(r);
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -ri; dy <= ri; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) continue;
                    m.set(c.x + dx, c.y + dy, c.z + dz);
                    if (m.getY() < world.getBottomY() + 1 || m.getY() >= world.getTopY()) continue;
                    BlockState cur = world.getBlockState(m);
                    if (erase ? !cur.isAir() : cur.isAir()) world.setBlockState(m, state, FLAGS);
                }
            }
        }
    }

    /** Đĩa đất cỏ: mặt cỏ, bên dưới đất thu nhỏ dần thành mỏm nhọn. */
    private static void disc(ServerWorld world, BlockPos top, int radius, int thick, boolean erase) {
        BlockState grass = erase ? Blocks.AIR.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();
        BlockState dirt = erase ? Blocks.AIR.getDefaultState() : Blocks.DIRT.getDefaultState();
        for (int d = 0; d <= thick; d++) {
            double r = radius * (1.0 - (double) d / (thick + 1)) + 0.5;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > r * r) continue;
                    BlockPos p = top.add(dx, -d, dz);
                    if (p.getY() < world.getBottomY() + 1) continue;
                    world.setBlockState(p, d == 0 ? grass : dirt, FLAGS);
                }
            }
        }
    }

    private static void spots(BlockPos top, int radius, Random random) {
        int n = 2 + radius / 2;
        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * Math.max(0.5, radius - 1.2);
            CROWD_SPOTS.add(new Vec3d(top.getX() + 0.5 + Math.cos(a) * r, top.getY() + 1, top.getZ() + 0.5 + Math.sin(a) * r));
        }
    }

    // ==========================================
    // ĐÁM ĐÔNG LỚN VÀ SÓNG
    // ==========================================

    /** Đợt sóng thứ tư: rất nhiều người đứng trên các bậc đất và quanh mép mẩu đất, cùng nhìn về người chơi. */
    public static void spawnMultitude(ServerWorld world, ServerPlayerEntity player) {
        // Người chơi đứng yên một chỗ: chỉ đặt người ở chỗ nhìn thẳng thấy được từ mắt (không khuất sau rễ hay bậc đất),
        // và dồn người lên những bậc đất nhìn thấy được, càng gần càng dày
        Vec3d eye = player.getEyePos();
        Random random = Random.create(SEED + 7);
        List<Vec3d> spots = new ArrayList<>();
        for (int[] t : TERRACES) {
            int r = t[3];
            int tries = 6 + r * r;
            int want = Math.max(2, r * r / 3);
            int placed = 0;
            for (int i = 0; i < tries && placed < want; i++) {
                double a = random.nextDouble() * Math.PI * 2, d = Math.sqrt(random.nextDouble()) * Math.max(0.5, r - 1.0);
                Vec3d s = new Vec3d(t[0] + 0.5 + Math.cos(a) * d, t[1] + 1, t[2] + 0.5 + Math.sin(a) * d);
                if (!world.getBlockState(BlockPos.ofFloored(s).down()).isOf(Blocks.GRASS_BLOCK)) continue;
                if (!visible(world, player, eye, s)) continue;
                spots.add(s);
                placed++;
            }
        }
        if (spots.size() > MULTITUDE) {
            List<Vec3d> kept = new ArrayList<>();
            double step = (double) spots.size() / MULTITUDE;
            for (int i = 0; i < MULTITUDE; i++) kept.add(spots.get((int) (i * step)));
            spots = kept;
        }
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + 0.3;
            spots.add(player.getPos().add(Math.cos(a) * 5.5, 0, Math.sin(a) * 5.5));
        }
        for (Vec3d s : spots) {
            EmEntity e = SamsaraEntities.EM.create(world);
            if (e == null) continue;
            double dx = player.getX() - s.x, dz = player.getZ() - s.z;
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            e.refreshPositionAndAngles(s.x, s.y, s.z, yaw, 0f);
            e.setHeadYaw(yaw);
            e.setBodyYaw(yaw);
            e.setCrowd(true);
            e.setNoGravity(true);
            world.spawnEntity(e);
        }
    }

    /** Từ mắt người chơi nhìn thẳng tới đầu và chân chỗ đứng có bị khối nào che không. */
    private static boolean visible(ServerWorld world, ServerPlayerEntity player, Vec3d eye, Vec3d spot) {
        for (double h : new double[]{1.6, 0.4}) {
            Vec3d target = spot.add(0, h, 0);
            net.minecraft.util.hit.BlockHitResult hit = world.raycast(new net.minecraft.world.RaycastContext(eye, target,
                    net.minecraft.world.RaycastContext.ShapeType.COLLIDER, net.minecraft.world.RaycastContext.FluidHandling.NONE, player));
            if (hit.getType() == net.minecraft.util.hit.HitResult.Type.MISS || hit.getPos().squaredDistanceTo(target) < 0.5) return true;
        }
        return false;
    }

    /** Gửi một đợt sóng lớn cho client người chơi. {@code sound}: 0 không tiếng, 1 tiếng chuông trầm, 2 tiếng thu vào, 3 = chỉ xoá mọi nét đang hiện. */
    public static void wave(ServerPlayerEntity player, float radius, float speed, float fade, boolean reverse, int sound) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(player.getX());
        buf.writeDouble(player.getY() + 0.5);
        buf.writeDouble(player.getZ());
        buf.writeFloat(radius);
        buf.writeFloat(speed);
        buf.writeFloat(fade);
        buf.writeBoolean(reverse);
        buf.writeByte(sound);
        ServerPlayNetworking.send(player, WAVE, buf);
    }
}
