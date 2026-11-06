package com.samsara.system.ending;

import com.samsara.entity.PrimordialEggEntity;
import com.samsara.system.cycle.EggCycleState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Phía SERVER của đoạn kết (Video 9: The_Egg_Dialogue).
 * Server chỉ quyết định KHI NÀO đoạn kết bắt đầu; toàn bộ cinematic, bóng phản chiếu, đoạn thoại
 * và màn "NO SIGNAL" chạy ở client (EndingSequenceClient) vì đây là trải nghiệm riêng của từng người.
 *
 * Gói ENDING_START: vị trí trứng (3 double) + vị trí chân người chơi (3 double).
 * Lệnh test: /samsara test ending (bỏ qua giới hạn một lần).
 */
public class EndingServer {
    public static final Identifier ENDING_START = new Identifier("samsara", "ending_start");

    /** Người chơi đang trong đoạn kết ở phiên server này (tránh kích hoạt lặp). */
    private static final Set<UUID> RUNNING = new HashSet<>();

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> RUNNING.clear());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RUNNING.remove(handler.getPlayer().getUuid()));
    }

    public static boolean isRunning(ServerPlayerEntity player) {
        return RUNNING.contains(player.getUuid());
    }

    /** Gọi từ PrimordialEggEntity khi người chơi tới gần. Mỗi người chơi chỉ một lần. */
    public static void tryStart(ServerPlayerEntity player, PrimordialEggEntity egg) {
        EggCycleState state = EggCycleState.get(player.getServer());
        if (RUNNING.contains(player.getUuid()) || state.hasSeenEnding(player.getUuid())) return;
        start(player, egg);
    }

    public static void start(ServerPlayerEntity player, PrimordialEggEntity egg) {
        RUNNING.add(player.getUuid());
        EggCycleState.get(player.getServer()).markEndingSeen(player.getUuid());

        // Bất tử trong lúc diễn (không ai muốn chết đuối giữa đoạn thoại)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 60 * 5, 4, false, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 20 * 60 * 5, 0, false, false, false));

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(egg.getX());
        buf.writeDouble(egg.getY());
        buf.writeDouble(egg.getZ());
        buf.writeDouble(player.getX());
        buf.writeDouble(player.getY());
        buf.writeDouble(player.getZ());
        ServerPlayNetworking.send(player, ENDING_START, buf);
    }
}
