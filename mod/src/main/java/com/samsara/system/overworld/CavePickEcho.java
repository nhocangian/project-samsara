package com.samsara.system.overworld;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tiếng cuốc đáp lại (hang động Overworld): người chơi đào đá dưới lòng đất rồi dừng tay; vài giây sau, trong vách đá
 * cách 5-8 khối vang lên tiếng cuốc gõ đá đúng nhịp người chơi vừa đào (cùng số khối, cùng khoảng cách giữa các khối,
 * đúng loại đá), như có ai đang đào song song ở bên kia. Người chơi đào tiếp thì tiếng đó im ngay. Chỉ người đó nghe.
 * Mỗi người ít nhất {@link #COOLDOWN} tick một lần, xác suất mỗi lần dừng tay {@link #CHANCE}; phím đạo diễn gọi ngay.
 */
public final class CavePickEcho {
    private CavePickEcho() {}

    private static final int COOLDOWN = 4800;
    private static final float CHANCE = 0.2f;
    /** Dừng tay chừng này tick thì coi như đã dừng đào. */
    private static final int PAUSE = 50;
    /** Mỗi khối: vài nhát gõ cách nhau rồi tiếng vỡ. */
    private static final int HITS = 4;
    private static final int HIT_GAP = 5;

    private record Dig(long tick, BlockSoundGroup sound) {}

    private record Note(long at, SoundEvent sound, float volume, float pitch) {}

    private static final Map<UUID, Deque<Dig>> DIGS = new HashMap<>();
    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final Map<UUID, List<Note>> PLAYING = new HashMap<>();
    private static final Map<UUID, Vec3d> SOURCE = new HashMap<>();

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (!(player instanceof ServerPlayerEntity sp) || !Caves.underground(sp)) return;
            if (!isRock(state)) return;
            // Đang phát mà người chơi đào tiếp: im ngay
            PLAYING.remove(sp.getUuid());
            Deque<Dig> q = DIGS.computeIfAbsent(sp.getUuid(), id -> new ArrayDeque<>());
            q.addLast(new Dig(sp.getServer().getTicks(), state.getSoundGroup()));
            while (q.size() > 6) q.removeFirst();
        });
        ServerTickEvents.END_SERVER_TICK.register(CavePickEcho::tick);
    }

    static boolean isRock(BlockState s) {
        return s.isIn(BlockTags.BASE_STONE_OVERWORLD) || s.isIn(BlockTags.COAL_ORES) || s.isIn(BlockTags.IRON_ORES)
                || s.isIn(BlockTags.COPPER_ORES) || s.isOf(Blocks.COBBLESTONE) || s.isOf(Blocks.COBBLED_DEEPSLATE);
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTicks();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID id = player.getUuid();
            List<Note> notes = PLAYING.get(id);
            if (notes != null) {
                Vec3d src = SOURCE.get(id);
                for (Note n : notes) {
                    if (n.at == now) send(player, src, n);
                }
                if (notes.isEmpty() || notes.get(notes.size() - 1).at <= now) PLAYING.remove(id);
                continue;
            }
            Deque<Dig> q = DIGS.get(id);
            if (q == null || q.size() < 3) continue;
            if (now - q.peekLast().tick < PAUSE) continue;
            // Đã dừng tay: thử một lần cho đợt đào này
            List<Dig> pattern = new ArrayList<>(q);
            q.clear();
            if (!Caves.underground(player) || now < NEXT.getOrDefault(id, (long) COOLDOWN)) continue;
            if (FilmingState.scripted(server) || player.getRandom().nextFloat() >= CHANCE) continue;
            play(player, pattern, now + 30);
        }
    }

    /** Lên lịch phát lại nhịp đào (bắt đầu lúc {@code start}) từ một điểm trong vách đá gần người chơi. */
    private static boolean play(ServerPlayerEntity player, List<Dig> pattern, long start) {
        Vec3d src = rockSpot(player);
        if (src == null) return false;
        List<Note> notes = new ArrayList<>();
        long t = start;
        long prev = pattern.get(0).tick;
        for (Dig d : pattern) {
            t += Math.max(HITS * HIT_GAP + 4, d.tick - prev);
            prev = d.tick;
            for (int h = 0; h < HITS; h++) notes.add(new Note(t + (long) h * HIT_GAP, d.sound.getHitSound(), 0.55f, 0.85f));
            notes.add(new Note(t + (long) HITS * HIT_GAP, d.sound.getBreakSound(), 0.7f, 0.85f));
        }
        PLAYING.put(player.getUuid(), notes);
        SOURCE.put(player.getUuid(), src);
        NEXT.put(player.getUuid(), start + COOLDOWN);
        return true;
    }

    /** Một điểm nằm trong đá đặc, cách 5-8 khối về một bên (lệch 60-120° so với hướng nhìn). */
    private static Vec3d rockSpot(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d look = player.getRotationVec(1.0f).multiply(1, 0, 1).normalize();
        for (int i = 0; i < 16; i++) {
            double side = world.random.nextBoolean() ? 1 : -1;
            double a = Math.toRadians(60 + world.random.nextDouble() * 60) * side;
            Vec3d dir = new Vec3d(look.x * Math.cos(a) - look.z * Math.sin(a), 0, look.x * Math.sin(a) + look.z * Math.cos(a));
            double dist = 5 + world.random.nextDouble() * 3;
            Vec3d p = player.getEyePos().add(dir.multiply(dist)).add(0, world.random.nextDouble() * 2 - 1.5, 0);
            BlockPos bp = BlockPos.ofFloored(p);
            if (world.getBlockState(bp).isOpaqueFullCube(world, bp)) return Vec3d.ofCenter(bp);
        }
        return null;
    }

    private static void send(ServerPlayerEntity player, Vec3d src, Note n) {
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(n.sound), SoundCategory.BLOCKS,
                src.x, src.y, src.z, n.volume, n.pitch * (0.97f + player.getRandom().nextFloat() * 0.06f), player.getRandom().nextLong()));
    }

    /** Phím đạo diễn: phát ngay nhịp đào gần nhất của người chơi (chưa đào gì thì một nhịp 4 khối đá). */
    public static boolean cue(ServerPlayerEntity player) {
        if (player.getWorld().getRegistryKey() != World.OVERWORLD) return false;
        List<Dig> pattern = new ArrayList<>(DIGS.getOrDefault(player.getUuid(), new ArrayDeque<>()));
        if (pattern.size() < 2) {
            pattern.clear();
            long t = 0;
            for (int i = 0; i < 4; i++) {
                pattern.add(new Dig(t, BlockSoundGroup.STONE));
                t += 26 + player.getRandom().nextInt(10);
            }
        }
        return play(player, pattern, player.getServer().getTicks() + 10);
    }
}
