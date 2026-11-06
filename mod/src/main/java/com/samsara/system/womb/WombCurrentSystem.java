package com.samsara.system.womb;

import com.samsara.registry.SamsaraDimensions;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.womb.chapters.DockChapter;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Phía SERVER của dòng chảy trên tuyến hành hương:
 *  - Gửi gốc tuyến (ROUTE_SYNC) cho client để client tự đẩy thuyền theo dòng chảy và vẽ thuyền bóng ma
 *    (thuyền do người chơi lái được client điều khiển, nên dòng chảy phải chạy ở client).
 *  - Giữ cho Bến Đò luôn có một chiếc thuyền chờ khi người chơi ở gần.
 */
public class WombCurrentSystem {
    public static final Identifier ROUTE_SYNC = new Identifier("samsara", "womb_route_sync");

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer()));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) -> sync(player));
        ServerTickEvents.END_SERVER_TICK.register(WombCurrentSystem::tick);
    }

    public static void sync(ServerPlayerEntity player) {
        BlockPos origin = EggCycleState.get(player.getServer()).getRouteOrigin();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(origin != null);
        if (origin != null) buf.writeLong(origin.asLong());
        ServerPlayNetworking.send(player, ROUTE_SYNC, buf);
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) sync(p);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 100 != 0) return;
        ServerWorld womb = server.getWorld(SamsaraDimensions.THE_WOMB_WORLD_KEY);
        BlockPos origin = EggCycleState.get(server).getRouteOrigin();
        if (womb == null || origin == null) return;
        Vec3d dock = DockChapter.mooring(origin);
        for (ServerPlayerEntity player : womb.getPlayers()) {
            if (player.hasVehicle()) continue;
            if (player.getPos().squaredDistanceTo(dock) < 30.0 * 30.0) {
                DockChapter.ensureBoat(womb, origin);
                return;
            }
        }
    }
}
