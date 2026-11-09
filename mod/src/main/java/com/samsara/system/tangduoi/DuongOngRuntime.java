package com.samsara.system.tangduoi;

import com.samsara.block.DenChopBlock;
import com.samsara.block.DenOngBlock;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.registry.SamsaraParticles;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tầng Dưới, tầng 1: đếm số lần đi sai ở từng ngã (qua vạch trên đường vòng) và cánh hoa trôi theo dòng nước.
 */
public final class DuongOngRuntime {
    private DuongOngRuntime() {}

    private static final class State {
        final int[] wrong = new int[DuongOngBuilder.JUNCTIONS];
        /** Đã tới ngã tư kể từ lần đếm trước: tránh đếm nhiều lần khi đứng trên vạch hay đi ngược. */
        final boolean[] armed = new boolean[DuongOngBuilder.JUNCTIONS];
        double anchorX;
        double anchorZ;
        int idleTicks = 0;
        /** Đèn đã tắt vì đứng yên, kèm trạng thái gốc để bật lại. */
        final Map<BlockPos, BlockState> dimmed = new HashMap<>();
    }

    private static final Random RANDOM = Random.create();
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Map<UUID, Integer> SLEEPING = new HashMap<>();
    private static final int SLEEP_TICKS = 90;
    private static final double GATE_HALF_WIDTH = 4.0;
    private record Relit(BlockState state, long restoreAt) {}

    private static final Map<BlockPos, Relit> RELIT = new HashMap<>();
    private static final int RELIGHT_PER_TICK = 4;
    private static final int RELIGHT_DELAY = 3;
    private static final double RELIGHT_RADIUS = 80.0;
    private static final int IDLE_START = 400;
    private static final int IDLE_STEP = 40;
    private static final double IDLE_RADIUS = 36.0;
    private static final double SURFACE = DuongOngBuilder.ORIGIN.getY() + 0.93;

    private static int teleportTicks = 0;
    private static ServerPlayerEntity pendingPlayer = null;
    private static ServerWorld pendingWorld = null;
    private static double[] pendingTp = null;

    /** Bước vào vết rách: cắt đen, dựng tầng nếu chưa có, rồi đặt người chơi vào căn hầm giống hệt. */
    public static void enter(ServerPlayerEntity player) {
        ServerWorld world = player.getServer().getWorld(SamsaraDimensions.TANG_DUOI_WORLD_KEY);
        if (world == null) return;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 110, 0, false, false, false));
        if (!world.getBlockState(TangDuoiRoom.arrival().down()).isOf(net.minecraft.block.Blocks.STONE_BRICKS)
                && !world.getBlockState(TangDuoiRoom.arrival().down()).isOf(net.minecraft.block.Blocks.MOSSY_STONE_BRICKS)) {
            DuongOngBuilder.buildTest(world);
        }
        reset();
        BlockPos a = TangDuoiRoom.arrival();
        teleportLater(player, world, a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 180.0f, 0.0f, 60);
    }

    public static void startSleep(ServerPlayerEntity player) {
        if (SLEEPING.containsKey(player.getUuid())) return;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 120, 0, false, false, false));
        SLEEPING.put(player.getUuid(), SLEEP_TICKS);
    }

    /** Ngủ ở phòng cuối: mắt tối dần, một câu của System, rồi tỉnh dậy ở đầu tầng 2. */
    private static void tickSleep(ServerWorld world) {
        for (Iterator<Map.Entry<UUID, Integer>> it = SLEEPING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Integer> e = it.next();
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(e.getKey());
            if (p == null) {
                it.remove();
                continue;
            }
            int left = e.getValue() - 1;
            e.setValue(left);
            if (left == 40) {
                p.sendMessage(Text.literal("§e<System> ").append(Text.translatable("samsara.tang_duoi.mot_lop_nua").formatted(Formatting.YELLOW)), false);
            }
            if (left <= 0) {
                it.remove();
                LoiDiNhapRuntime.enter(p);
            }
        }
    }

    public static void register() {
        // Giường ở Tầng Dưới: dùng được bất kể giờ, không cộng số giấc ngủ, không bị đánh thức
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.getRegistryKey() != SamsaraDimensions.TANG_DUOI_WORLD_KEY) return ActionResult.PASS;
            if (!(world.getBlockState(hit.getBlockPos()).getBlock() instanceof BedBlock)) return ActionResult.PASS;
            if (player instanceof ServerPlayerEntity sp) startSleep(sp);
            return ActionResult.SUCCESS;
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey() != SamsaraDimensions.TANG_DUOI_WORLD_KEY) return;
            if (teleportTicks > 0 && --teleportTicks == 0) finishTeleport();
            restoreRelit(world);
            tickSleep(world);
            for (ServerPlayerEntity player : world.getPlayers()) {
                tickGates(world, player);
                tickIdleDim(world, player);
                tickRelight(world, player);
                if (TangDuoiRoom.isDeepInRoom(player.getX(), player.getZ())) TangDuoiRoom.sealDoor(world);
                if (world.getTime() % 10 == 0) tickPetals(world, player);
            }
        });
    }

    /** Dựng lại tầng thì quên hết số lần sai. */
    public static void reset() {
        STATES.clear();
    }

    /**
     * Đưa người chơi tới sau {@code ticks} tick. Dựng xong phải chờ ánh sáng tính xong rồi mới đưa người chơi tới:
     * chunk gửi cho client trong lúc còn đang tính sáng sẽ tối mãi (server thì đúng, client giữ dữ liệu cũ).
     */
    public static void teleportLater(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch, int ticks) {
        pendingPlayer = player;
        pendingWorld = world;
        pendingTp = new double[]{x, y, z, yaw, pitch};
        teleportTicks = ticks;
    }

    private static void finishTeleport() {
        if (pendingPlayer != null && !pendingPlayer.isRemoved()) {
            pendingPlayer.teleport(pendingWorld, pendingTp[0], pendingTp[1], pendingTp[2], (float) pendingTp[3], (float) pendingTp[4]);
        }
        pendingPlayer = null;
    }

    private static void tickGates(ServerWorld world, ServerPlayerEntity player) {
        State state = STATES.computeIfAbsent(player.getUuid(), k -> new State());
        double ox = DuongOngBuilder.ORIGIN.getX();
        double oz = DuongOngBuilder.ORIGIN.getZ();
        for (int plot = 0; plot < DuongOngBuilder.JUNCTIONS; plot++) {
            double x = player.getX() - ox - DuongOngBuilder.CENTER[plot][0];
            double z = player.getZ() - oz - DuongOngBuilder.CENTER[plot][1];
            if (x * x + z * z < 36.0) {
                state.armed[plot] = true;
                continue;
            }
            if (!state.armed[plot]) continue;
            List<int[]> gates = DuongOngBuilder.wrongGates(plot);
            for (int[] gate : gates) {
                double gx = gate[0] + 0.5;
                double gz = gate[1] + 0.5;
                if (Math.abs(x - gx) <= 0.9 && Math.abs(z - gz) <= GATE_HALF_WIDTH) {
                    state.armed[plot] = false;
                    onWrong(world, player, plot, ++state.wrong[plot]);
                    break;
                }
            }
        }
    }

    /**
     * Sửa ô tối do ánh sáng chưa cập nhật: giống đặt rồi đập một block bên cạnh, nhưng làm với chính đèn.
     * Tắt đèn rồi vài tick sau mới bật lại (cùng một tick thì bộ tính sáng không thấy khác biệt gì),
     * ở khoảng cách còn xa nên người chơi hầu như không kịp thấy.
     */
    private static void tickRelight(ServerWorld world, ServerPlayerEntity player) {
        int done = 0;
        for (Iterator<BlockPos> it = DuongOngBuilder.RELIGHT.iterator(); it.hasNext() && done < RELIGHT_PER_TICK; ) {
            BlockPos p = it.next();
            if (player.squaredDistanceTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5) > RELIGHT_RADIUS * RELIGHT_RADIUS) continue;
            it.remove();
            BlockState lamp = world.getBlockState(p);
            if (lamp.getLuminance() <= 0) continue;
            RELIT.put(p, new Relit(lamp, world.getTime() + RELIGHT_DELAY));
            world.setBlockState(p, SamsaraBlocks.GACH_MEN_O.getDefaultState(), 2 | 16);
            done++;
        }
    }

    private static void restoreRelit(ServerWorld world) {
        for (Iterator<Map.Entry<BlockPos, Relit>> it = RELIT.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Relit> e = it.next();
            if (world.getTime() < e.getValue().restoreAt) continue;
            world.setBlockState(e.getKey(), e.getValue().state, 2 | 16);
            it.remove();
        }
    }

    /** Đứng yên quá {@link #IDLE_START} tick: đèn xa nhất phía sau tắt, rồi đến bóng tiếp theo; đi tiếp thì sáng lại. */
    private static void tickIdleDim(ServerWorld world, ServerPlayerEntity player) {
        State state = STATES.computeIfAbsent(player.getUuid(), k -> new State());
        double mx = player.getX() - state.anchorX;
        double mz = player.getZ() - state.anchorZ;
        if (mx * mx + mz * mz > 2.25) {
            state.anchorX = player.getX();
            state.anchorZ = player.getZ();
            state.idleTicks = 0;
            for (Map.Entry<BlockPos, BlockState> e : state.dimmed.entrySet()) {
                if (world.getBlockState(e.getKey()).isOf(SamsaraBlocks.DEN_CHOP)) world.setBlockState(e.getKey(), e.getValue(), 3);
            }
            state.dimmed.clear();
            return;
        }
        int t = ++state.idleTicks - IDLE_START;
        if (t < 0 || t % IDLE_STEP != 0) return;
        Vec3d look = player.getRotationVec(1.0f);
        BlockPos pick = null;
        double best = -1.0;
        for (BlockPos p : DuongOngBuilder.LAMPS) {
            double dx = p.getX() + 0.5 - player.getX();
            double dz = p.getZ() + 0.5 - player.getZ();
            double d = dx * dx + dz * dz;
            if (d > IDLE_RADIUS * IDLE_RADIUS || d <= best || dx * look.x + dz * look.z >= 0.0) continue;
            if (!world.getBlockState(p).isOf(SamsaraBlocks.DEN_HUYNH_QUANG)) continue;
            best = d;
            pick = p;
        }
        if (pick == null) return;
        BlockState original = world.getBlockState(pick);
        state.dimmed.put(pick, original);
        world.setBlockState(pick, SamsaraBlocks.DEN_CHOP.getDefaultState().with(DenOngBlock.AXIS, original.get(DenOngBlock.AXIS))
                .with(DenChopBlock.LIT, false), 3);
    }

    private static void onWrong(ServerWorld world, ServerPlayerEntity player, int plot, int n) {
        BlockPos before = DuongOngBuilder.before(plot);
        switch (n) {
            case 2 -> DuongOngBuilder.killEntryLamp(world, plot);
            case 3 -> DuongOngBuilder.placeSign(world, DuongOngBuilder.wrongSignPos(plot), Direction.WEST,
                    "samsara.tang_duoi.di_vong_1", "samsara.tang_duoi.di_vong_2");
            case 4 -> world.playSound(null, before.add(0, 2, 22), SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.BLOCKS, 1.6f, 0.7f);
            case 5 -> {
                player.sendMessage(Text.translatable("samsara.tang_duoi.hoa_khong_nhin"), true);
                DuongOngBuilder.lightCorrectBranch(world, plot);
            }
            default -> { }
        }
    }

    /** Cánh hoa vàng trôi trên mặt nước dọc lối đúng, gần người chơi. */
    private static void tickPetals(ServerWorld world, ServerPlayerEntity player) {
        double ox = DuongOngBuilder.ORIGIN.getX() + 0.5;
        double oz = DuongOngBuilder.ORIGIN.getZ() + 0.5;
        for (int plot = 0; plot < DuongOngBuilder.JUNCTIONS; plot++) {
            int[][] route = DuongOngBuilder.route(plot);
            for (int i = 0; i + 1 < route.length; i++) {
                int len = Math.abs(route[i + 1][0] - route[i][0]) + Math.abs(route[i + 1][1] - route[i][1]);
                if (len == 0) continue;
                double ux = Integer.signum(route[i + 1][0] - route[i][0]);
                double uz = Integer.signum(route[i + 1][1] - route[i][1]);
                double t = RANDOM.nextDouble() * len;
                double spread = (RANDOM.nextDouble() - 0.5) * 3.5;
                double px = ox + route[i][0] + ux * t + uz * spread;
                double pz = oz + route[i][1] + uz * t + ux * spread;
                if (player.squaredDistanceTo(px, SURFACE, pz) > 30 * 30) continue;
                world.spawnParticles(player, SamsaraParticles.CANH_HOA, false, px, SURFACE, pz, 0, ux, 0.0, uz, 0.05);
            }
        }
    }
}
