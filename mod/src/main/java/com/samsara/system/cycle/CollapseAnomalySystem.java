package com.samsara.system.cycle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Dị tượng "trọng lực bẻ cong" ở Overworld (Master Production Document, bảng mục II.3).
 *
 *  Stage 3: cát / sỏi quanh người chơi thỉnh thoảng rơi NGƯỢC lên trời (Video 6: Gravity_Decay).
 *  Stage 4: Overworld sụp đổ - bất kỳ block tự nhiên nào trên mặt đất cũng có thể bị hút đứt lên hư không.
 *  (La bàn quay tít ở Stage 3+ nằm ở CompassSpinMixin phía client.)
 *
 * Block bị hút lên là FallingBlockEntity không trọng lực, được đẩy lên mỗi tick rồi tan biến trên cao.
 * Lệnh test: /samsara test anomaly lift.
 */
public class CollapseAnomalySystem {
    private static final int RADIUS = 20;
    private static final int RISE_HEIGHT = 48;
    private static final int MAX_LIFETIME = 400;

    private record Lifted(FallingBlockEntity entity, double startY, int bornTick) {}

    private static final List<Lifted> LIFTED = new ArrayList<>();

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> LIFTED.clear());
        ServerTickEvents.END_SERVER_TICK.register(CollapseAnomalySystem::tick);
    }

    private static void tick(MinecraftServer server) {
        tickLifted(server.getTicks());

        int stage = EggCycleState.get(server).getStage();
        if (stage < 3) return;
        int interval = stage >= 4 ? 10 : 30;
        if (server.getTicks() % interval != 0) return;

        ServerWorld overworld = server.getOverworld();
        for (ServerPlayerEntity player : overworld.getPlayers()) {
            if (player.isSpectator()) continue;
            liftNear(player, stage, stage >= 4 ? 2 : 1);
        }
    }

    /**
     * Thử hút tối đa {@code count} block quanh người chơi theo luật của giai đoạn.
     * @return số block đã bị hút lên
     */
    public static int liftNear(ServerPlayerEntity player, int stage, int count) {
        ServerWorld world = player.getServerWorld();
        if (world.getRegistryKey() != World.OVERWORLD) return 0;
        Random random = world.getRandom();
        int lifted = 0;
        for (int attempt = 0; attempt < count * 8 && lifted < count; attempt++) {
            int x = player.getBlockX() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
            int z = player.getBlockZ() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
            if (Math.abs(x - player.getBlockX()) <= 2 && Math.abs(z - player.getBlockZ()) <= 2) continue;
            BlockPos pos = new BlockPos(x, world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1, z);
            BlockState state = world.getBlockState(pos);
            if (!canLift(state, stage)) continue;
            lift(world, pos, state);
            lifted++;
        }
        return lifted;
    }

    private static boolean canLift(BlockState state, int stage) {
        if (state.isAir() || !state.getFluidState().isEmpty()) return false;
        Block block = state.getBlock();
        if (stage < 4) return block instanceof FallingBlock; // cát, sỏi, bột bê tông...
        if (block instanceof BlockWithEntity || block instanceof BedBlock) return false;
        if (state.contains(Properties.DOUBLE_BLOCK_HALF)) return false;
        if (block == Blocks.BEDROCK || state.getHardness(null, BlockPos.ORIGIN) < 0) return false;
        return true;
    }

    private static void lift(ServerWorld world, BlockPos pos, BlockState state) {
        FallingBlockEntity falling = FallingBlockEntity.spawnFromBlock(world, pos, state);
        falling.setNoGravity(true);
        falling.dropItem = false;
        falling.setVelocity(0.0, 0.08, 0.0);
        falling.velocityModified = true;
        LIFTED.add(new Lifted(falling, pos.getY(), world.getServer().getTicks()));

        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                8, 0.3, 0.3, 0.3, 0.02);
        world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.BLOCKS, 0.4f, 0.5f);
    }

    private static void tickLifted(int now) {
        if (LIFTED.isEmpty()) return;
        Iterator<Lifted> it = LIFTED.iterator();
        while (it.hasNext()) {
            Lifted lifted = it.next();
            FallingBlockEntity e = lifted.entity();
            if (e.isRemoved()) {
                it.remove();
                continue;
            }
            // Tăng tốc dần lên trời, hơi lắc ngang như bị hút xoáy
            int age = now - lifted.bornTick();
            double speed = Math.min(0.06 + age * 0.004, 0.6);
            Vec3d v = e.getVelocity();
            e.setVelocity(v.x * 0.8 + Math.sin(age * 0.3) * 0.01, speed, v.z * 0.8 + Math.cos(age * 0.3) * 0.01);
            e.velocityModified = true;
            e.timeFalling = 1; // không cho vanilla tự rơi xuống / tự đặt block

            if (e.getY() - lifted.startY() > RISE_HEIGHT || age > MAX_LIFETIME) {
                if (e.getWorld() instanceof ServerWorld sw) {
                    sw.spawnParticles(ParticleTypes.SMOKE, e.getX(), e.getY(), e.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
                }
                e.discard();
                it.remove();
            }
        }
    }
}
