package com.samsara.system.tangduoi;

import com.samsara.registry.SamsaraBlocks;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.womb.WombTravel;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tầng Dưới, tầng 2: rơi nước thì nổi lên ở điểm gần nhất, đoạn 3 tự xoá sau lưng, giường cuối dẫn tới Bến Đò.
 * Không có cái chết, không mất máu.
 */
public final class LoiDiNhapRuntime {
    private LoiDiNhapRuntime() {}

    private static final class State {
        int checkpoint = 0;
        final int[] falls = new int[LoiDiNhapBuilder.SECTIONS];
        int returnIn = 0;
        int age = 0;
        int revealed = -1;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Map<UUID, Integer> SLEEPING = new HashMap<>();
    private static final Map<UUID, Integer> WELCOME = new HashMap<>();
    private static final int SLEEP_TICKS = 90;
    private static final double SURFACE = LoiDiNhapBuilder.ORIGIN.getY() + 0.5;
    /** Ô của đoạn 3 cách người chơi bao nhiêu ô (theo thứ tự lối đi) thì bị xoá. */
    private static final int ERASE_BEHIND = 3;

    /** Từ giường cuối tầng 1 sang: tỉnh dậy trong căn phòng nháp. */
    public static void enter(ServerPlayerEntity player) {
        ServerWorld world = player.getServer().getWorld(SamsaraDimensions.LOI_DI_NHAP_WORLD_KEY);
        if (world == null) return;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 110, 0, false, false, false));
        if (!LoiDiNhapBuilder.isBuilt(world)) LoiDiNhapBuilder.build(world);
        STATES.remove(player.getUuid());
        BlockPos a = LoiDiNhapBuilder.arrival();
        DuongOngRuntime.teleportLater(player, world, a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 180.0f, 0.0f, 60);
        WELCOME.put(player.getUuid(), 110);
    }

    public static void reset() {
        STATES.clear();
    }

    public static void startSleep(ServerPlayerEntity player) {
        if (SLEEPING.containsKey(player.getUuid())) return;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 120, 0, false, false, false));
        SLEEPING.put(player.getUuid(), SLEEP_TICKS);
    }

    /** Lệnh test: coi như vừa rơi xuống nước. */
    public static void forceFall(ServerPlayerEntity player) {
        fall(player.getServerWorld(), player, STATES.computeIfAbsent(player.getUuid(), k -> new State()));
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.getRegistryKey() != SamsaraDimensions.LOI_DI_NHAP_WORLD_KEY) return ActionResult.PASS;
            if (!(world.getBlockState(hit.getBlockPos()).getBlock() instanceof BedBlock)) return ActionResult.PASS;
            if (player instanceof ServerPlayerEntity sp) {
                if (LoiDiNhapBuilder.isFinalBed(hit.getBlockPos())) startSleep(sp);
                else sp.sendMessage(Text.translatable("samsara.tang_duoi2.giuong_thu"), true);
            }
            return ActionResult.SUCCESS;
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey() != SamsaraDimensions.LOI_DI_NHAP_WORLD_KEY) return;
            tickSleep(world);
            for (ServerPlayerEntity player : world.getPlayers()) tickPlayer(world, player);
        });
    }

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
                p.sendMessage(Text.literal("§e<System> ").append(Text.translatable("samsara.tang_duoi2.mong_hon").formatted(Formatting.YELLOW)), false);
            }
            if (left <= 0) {
                it.remove();
                EggCycleState.get(p.getServer()).markTangDuoiCleared(p.getUuid());
                WombTravel.teleportToTheWomb(p);
            }
        }
    }

    private static void tickPlayer(ServerWorld world, ServerPlayerEntity player) {
        UUID id = player.getUuid();
        State state = STATES.computeIfAbsent(id, k -> new State());
        Integer welcome = WELCOME.get(id);
        if (welcome != null) {
            if (welcome <= 1) {
                WELCOME.remove(id);
                player.sendMessage(Text.translatable("samsara.tang_duoi2.doi_che_do").formatted(Formatting.GRAY, Formatting.ITALIC), false);
            } else {
                WELCOME.put(id, welcome - 1);
            }
        }

        if (state.returnIn > 0 && --state.returnIn == 0) respawn(player, state);
        if (state.returnIn > 0) return;

        // Đã tới điểm nào thì giữ điểm đó
        if (player.isOnGround()) {
            int cp = LoiDiNhapBuilder.checkpointAt(player.getZ() - LoiDiNhapBuilder.ORIGIN.getZ());
            if (cp > state.checkpoint) state.checkpoint = cp;
        }
        tickGiants(world, player, state);

        if (player.getY() < SURFACE) {
            fall(world, player, state);
            return;
        }
        tickErase(world, player, state);
        if (world.getTime() % 12 == 0 && state.checkpoint < LoiDiNhapBuilder.SECTIONS
                && state.falls[state.checkpoint] >= 5) {
            hint(world, player, state.checkpoint);
        }
    }

    /** Mỗi đảo tới được thì thêm khối khổng lồ; chờ một lúc sau khi vào để thực thể đã lưu kịp tải, tránh nhân đôi. */
    private static void tickGiants(ServerWorld world, ServerPlayerEntity player, State state) {
        if (++state.age < 80) return;
        int step = LoiDiNhapBuilder.isFinalRoom(player.getZ()) ? LoiDiNhapKhoi.LAST : state.checkpoint;
        while (state.revealed < step) {
            state.revealed++;
            LoiDiNhapKhoi.reveal(world, player, state.revealed, state.revealed == step);
        }
    }

    /** Tối mắt một nhịp rồi nổi lên ở điểm gần nhất; rơi nhiều lần trong một đoạn thì đoạn đó hiện mờ. */
    private static void fall(ServerWorld world, ServerPlayerEntity player, State state) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false, false));
        state.returnIn = 18;
        int section = Math.min(state.checkpoint, LoiDiNhapBuilder.SECTIONS - 1);
        int n = ++state.falls[section];
        if (n == 3) LoiDiNhapBuilder.placeRedoSign(world, state.checkpoint);
    }

    private static void respawn(ServerPlayerEntity player, State state) {
        ServerWorld world = player.getServerWorld();
        if (state.checkpoint == LoiDiNhapBuilder.ERASE_SECTION) LoiDiNhapBuilder.restoreErasing(world);
        BlockPos p = LoiDiNhapBuilder.checkpoint(state.checkpoint);
        player.setVelocity(0, 0, 0);
        player.fallDistance = 0.0f;
        player.teleport(world, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 180.0f, 0.0f);
    }

    /** Đoạn 3: ô đã đi qua biến mất sau lưng. Còn đứng trên đảo trước đó thì đoạn phải đầy đủ. */
    private static void tickErase(ServerWorld world, ServerPlayerEntity player, State state) {
        if (state.checkpoint != LoiDiNhapBuilder.ERASE_SECTION) return;
        List<BlockPos> tiles = LoiDiNhapBuilder.section(LoiDiNhapBuilder.ERASE_SECTION);
        int nearest = -1;
        double best = 2.25;
        for (int i = 0; i < tiles.size(); i++) {
            BlockPos t = tiles.get(i);
            double d = player.squaredDistanceTo(t.getX() + 0.5, player.getY(), t.getZ() + 0.5);
            if (d < best) {
                best = d;
                nearest = i;
            }
        }
        if (nearest < 0) {
            double z = player.getZ() - LoiDiNhapBuilder.ORIGIN.getZ();
            if (z > LoiDiNhapBuilder.ERASE_START_Z && world.getTime() % 20 == 0) LoiDiNhapBuilder.restoreErasing(world);
            return;
        }
        for (int i = nearest - ERASE_BEHIND; i >= 0; i--) {
            BlockPos t = tiles.get(i);
            if (!world.getBlockState(t).isOf(SamsaraBlocks.KHOI_NHAP)) break;
            world.setBlockState(t, Blocks.WATER.getDefaultState(), 2 | 16);
            world.playSound(null, t, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.BLOCKS, 0.5f, 0.5f);
        }
    }

    private static void hint(ServerWorld world, ServerPlayerEntity player, int section) {
        for (BlockPos t : LoiDiNhapBuilder.section(section)) {
            if (player.squaredDistanceTo(t.getX() + 0.5, t.getY() + 1.0, t.getZ() + 0.5) > 40 * 40) continue;
            world.spawnParticles(player, ParticleTypes.SMOKE, false, t.getX() + 0.5, t.getY() + 1.05, t.getZ() + 0.5, 1, 0.2, 0.0, 0.2, 0.0);
        }
    }
}
