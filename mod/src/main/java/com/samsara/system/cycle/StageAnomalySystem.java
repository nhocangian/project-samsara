package com.samsara.system.cycle;

import com.samsara.worldgen.SpawnCairnGenerator;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Dị tượng môi trường phía SERVER theo giai đoạn (Master Production Document, bảng mục II.3).
 *
 *  Stage 1+ : "Cừu đứng quay mặt về 1 tọa độ" - thỉnh thoảng toàn bộ bầy cừu quanh người chơi
 *             đứng bất động, đồng loạt quay đầu nhìn về Bia Mộ Vô Danh (Spawn Cairn) trong 30 giây.
 *  Stage 2+ : "Cây trồng chỉ lớn dưới ánh hồng ngoại IR Mode" - random tick của CropBlock bị chặn
 *             ở Overworld (CropBlockMixin); cây chỉ lớn khi có người ngắm vào qua máy quay bật IR.
 *
 * Phần phía client (nhạc giật, nước xám bạc) nằm ở com.samsara.client.StageAnomalyClient.
 * Lệnh test: /samsara test anomaly <sheep|music>.
 */
public class StageAnomalySystem {
    /** Gói C2S: client báo đang ngắm máy quay với IR bật (boolean). */
    public static final Identifier IR_OBSERVE_PACKET = new Identifier("samsara", "camcorder_ir");
    /** Gói S2C (rỗng): bắt client làm nhạc nền giật ngay (lệnh test). */
    public static final Identifier MUSIC_STUTTER_PACKET = new Identifier("samsara", "music_stutter");

    private static final int SHEEP_EPISODE_TICKS = 600;
    /** Xác suất bắt đầu cơn "cừu nhìn" mỗi giây (~1 lần / 5 phút). */
    private static final float SHEEP_EPISODE_CHANCE = 1.0f / 300.0f;
    private static final double SHEEP_RADIUS = 48.0;

    private static final double IR_RANGE = 12.0;
    private static final double IR_CONE_DOT = 0.85;
    private static final float IR_GROWTH_CHANCE = 0.2f;

    private static long sheepEpisodeEnd = -1;
    private static final Set<UUID> IR_OBSERVERS = new HashSet<>();

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(IR_OBSERVE_PACKET, (server, player, handler, buf, responseSender) -> {
            boolean observing = buf.readBoolean();
            server.execute(() -> {
                if (observing) IR_OBSERVERS.add(player.getUuid());
                else IR_OBSERVERS.remove(player.getUuid());
            });
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> IR_OBSERVERS.remove(handler.getPlayer().getUuid()));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            sheepEpisodeEnd = -1;
            IR_OBSERVERS.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(StageAnomalySystem::tick);
    }

    private static void tick(MinecraftServer server) {
        int stage = EggCycleState.get(server).getStage();
        ServerWorld overworld = server.getOverworld();
        long now = server.getTicks();

        // ---------------- Stage 1: cừu nhìn về Bia Mộ ----------------
        if (stage >= 1) {
            if (now >= sheepEpisodeEnd && now % 20 == 0 && overworld.random.nextFloat() < SHEEP_EPISODE_CHANCE) {
                startSheepEpisode(server);
            }
            if (now < sheepEpisodeEnd) {
                holdSheepStare(overworld);
            }
        } else {
            sheepEpisodeEnd = -1;
        }

        // ---------------- Stage 2: cây chỉ lớn dưới IR ----------------
        if (stage >= 2 && now % 10 == 0) {
            for (UUID uuid : IR_OBSERVERS) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                if (player != null && player.getWorld().getRegistryKey() == World.OVERWORLD) {
                    growObservedCrops(player);
                }
            }
        }
    }

    /** Có nên chặn random tick của cây trồng ở vị trí này không (gọi từ CropBlockMixin). */
    public static boolean shouldBlockCropGrowth(ServerWorld world) {
        return world.getRegistryKey() == World.OVERWORLD && EggCycleState.get(world.getServer()).getStage() >= 2;
    }

    // ==========================================
    // CỪU NHÌN VỀ BIA MỘ
    // ==========================================

    public static void sendMusicStutter(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, MUSIC_STUTTER_PACKET, PacketByteBufs.empty());
    }

    public static void startSheepEpisode(MinecraftServer server) {
        sheepEpisodeEnd = server.getTicks() + SHEEP_EPISODE_TICKS;
    }

    private static void holdSheepStare(ServerWorld world) {
        BlockPos cairn = SpawnCairnGenerator.getCairnPos(world);
        double tx = cairn.getX() + 0.5;
        double ty = cairn.getY() + 1.0;
        double tz = cairn.getZ() + 0.5;

        for (ServerPlayerEntity player : world.getPlayers()) {
            for (SheepEntity sheep : world.getEntitiesByClass(SheepEntity.class,
                    player.getBoundingBox().expand(SHEEP_RADIUS), SheepEntity::isAlive)) {
                sheep.getNavigation().stop();
                // Slowness cực mạnh, không hạt, không icon: con cừu không thể nhúc nhích
                sheep.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 10, false, false, false));

                double dx = tx - sheep.getX();
                double dz = tz - sheep.getZ();
                float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
                sheep.setYaw(yaw);
                sheep.setBodyYaw(yaw);
                sheep.setHeadYaw(yaw);
                sheep.getLookControl().lookAt(tx, ty, tz, 180.0f, 90.0f);
            }
        }
    }

    // ==========================================
    // CÂY TRỒNG DƯỚI ÁNH HỒNG NGOẠI
    // ==========================================

    private static void growObservedCrops(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f).normalize();
        BlockPos center = player.getBlockPos();
        int r = (int) IR_RANGE;

        for (BlockPos pos : BlockPos.iterate(center.add(-r, -4, -r), center.add(r, 4, r))) {
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof CropBlock crop)) continue;
            if (crop.isMature(state)) continue;

            Vec3d to = Vec3d.ofCenter(pos).subtract(eye);
            double dist = to.length();
            if (dist > IR_RANGE || dist < 0.5) continue;
            if (to.normalize().dotProduct(look) < IR_CONE_DOT) continue;
            if (world.random.nextFloat() >= IR_GROWTH_CHANCE) continue;

            world.setBlockState(pos, crop.withAge(crop.getAge(state) + 1), 2);
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                    3, 0.25, 0.2, 0.25, 0.0);
        }
    }
}
