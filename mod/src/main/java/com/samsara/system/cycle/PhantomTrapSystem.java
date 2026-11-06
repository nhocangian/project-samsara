package com.samsara.system.cycle;

import com.samsara.registry.SamsaraSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.List;

/**
 * Bẫy Sinh Tồn: Phantom vs Giấc Ngủ (Master Production Document, mục II.2).
 *
 * Phantom không phải quái vật ăn thịt mà là tàn dư linh hồn kiếp cũ, lao xuống để ép người chơi nhắm mắt.
 *  - Không ngủ: mỗi đêm thức trắng, bầy Phantom quanh người chơi đông hơn, to hơn, kêu bằng âm tua ngược
 *    và thì thầm "ngủ đi...".
 *  - Bấm ngủ: thoát Phantom ngay lập tức (banishPhantoms), nhưng sleep_count += 1.
 *
 * Số đêm thức = thống kê vanilla TIME_SINCE_REST / 24000 (vanilla cũng dùng số này cho Phantom).
 * Lệnh test: /samsara test phantom_wave [đêm].
 */
public class PhantomTrapSystem {
    /** Chu kỳ kiểm tra một đợt Phantom (tick). */
    private static final int WAVE_INTERVAL = 600;
    /** Bán kính đếm Phantom đang bám theo người chơi. */
    private static final double SWARM_RADIUS = 64.0;

    private static final String[] WHISPERS = {
            "samsara.phantom_trap_system.ngu_di",
            "samsara.phantom_trap_system.nham_mat_lai_di",
            "samsara.phantom_trap_system.chiec_giuong_van_con",
            "samsara.phantom_trap_system.cau_met_roi",
            "samsara.phantom_trap_system.ngu_di_ngu_di",
    };

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PhantomTrapSystem::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % WAVE_INTERVAL != 0) return;
        ServerWorld overworld = server.getOverworld();
        if (overworld.getDifficulty() == Difficulty.PEACEFUL) return;
        if (!overworld.isNight()) return;

        for (ServerPlayerEntity player : overworld.getPlayers()) {
            if (player.isSpectator() || player.isCreative() || player.isSleeping()) continue;
            spawnWave(player, nightsAwake(player));
        }
    }

    /** Số đêm người chơi đã thức trắng liên tiếp. */
    public static int nightsAwake(ServerPlayerEntity player) {
        int ticks = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));
        return ticks / 24000;
    }

    /** Đặt số đêm thức (lệnh test). */
    public static void setNightsAwake(ServerPlayerEntity player, int nights) {
        player.getStatHandler().setStat(player, Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST), Math.max(0, nights) * 24000);
    }

    /** Kích thước bầy mong muốn theo số đêm thức. */
    public static int targetSwarmSize(int nights) {
        return switch (nights) {
            case 0 -> 0;
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 6;
            case 4 -> 9;
            default -> 12;
        };
    }

    /**
     * Thả thêm Phantom tới khi bầy quanh người chơi đủ số lượng mong muốn.
     * @return số Phantom vừa được thả
     */
    public static int spawnWave(ServerPlayerEntity player, int nights) {
        int target = targetSwarmSize(nights);
        if (target <= 0) return 0;

        ServerWorld world = player.getServerWorld();
        if (world.getRegistryKey() != World.OVERWORLD) return 0;

        Box area = player.getBoundingBox().expand(SWARM_RADIUS);
        int existing = world.getEntitiesByClass(PhantomEntity.class, area, PhantomEntity::isAlive).size();
        int toSpawn = Math.min(target - existing, 4); // tối đa 4 con mỗi đợt để bầy dày dần lên
        if (toSpawn <= 0) return 0;

        Random random = world.getRandom();
        int spawned = 0;
        for (int i = 0; i < toSpawn; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 10.0 + random.nextDouble() * 14.0;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            int surface = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
            int y = Math.max(surface, player.getBlockY()) + 18 + random.nextInt(10);
            BlockPos pos = new BlockPos(x, Math.min(y, world.getTopY() - 2), z);
            if (!world.isAir(pos) || !world.isAir(pos.up())) continue;

            PhantomEntity phantom = EntityType.PHANTOM.create(world);
            if (phantom == null) continue;
            phantom.refreshPositionAndAngles(pos, 0.0f, 0.0f);
            phantom.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null, null);
            // Đêm thức càng nhiều, tàn hồn càng phình to
            phantom.setPhantomSize(Math.min(nights - 1, 5));
            world.spawnEntity(phantom);
            spawned++;
        }

        if (spawned > 0) {
            // Tiếng Phantom tua ngược vọng xuống từ bầu trời
            world.playSound(null, player.getX(), player.getY() + 12.0, player.getZ(),
                    SamsaraSounds.PHANTOM_REVERSED, SoundCategory.HOSTILE, 1.4f, 0.55f + random.nextFloat() * 0.1f);
            if (nights >= 2) {
                player.sendMessage(Text.translatable(WHISPERS[random.nextInt(WHISPERS.length)]), true);
            }
        }

        // Thức trắng tới đêm thứ 3: <System> cài máy quay vào Hotbar (Video 4)
        if (nights >= 3) {
            CamcorderGiftSystem.tryGift(player, "samsara.phantom_trap_system.system_cau_khong_chiu");
        }
        return spawned;
    }

    /** Ngủ dậy: mọi Phantom quanh người chơi tan thành tro bụi. */
    public static void banishPhantoms(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Box area = player.getBoundingBox().expand(SWARM_RADIUS * 1.5);
        List<PhantomEntity> phantoms = world.getEntitiesByClass(PhantomEntity.class, area, PhantomEntity::isAlive);
        for (PhantomEntity phantom : phantoms) {
            world.spawnParticles(ParticleTypes.SOUL, phantom.getX(), phantom.getY(), phantom.getZ(), 12, 0.6, 0.3, 0.6, 0.02);
            phantom.discard();
        }
    }
}
