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
    /** Gói S2C (float): xem trước bóng phản chiếu ở cỡ cho trước, 0 = tắt (lệnh test mirror). */
    public static final Identifier MIRROR_PREVIEW = new Identifier("samsara", "mirror_preview");

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

        // Hồ Gương ở Tháp: đặt người chơi đúng 6 block phía nam (đo lại theo ảnh đã duyệt: 4.5 block làm quả trứng che hết Gargantua) Quả Trứng (chỗ chụp ảnh tham chiếu), nhìn về bắc (lúc cắt sang cảnh toàn cảnh
        // nên không lộ). Ở khoảng này, ngước lên Gargantua thì quả trứng nằm gọn trong vòng tròn của hố đen (người dùng
        // chọn khung này, 08/11/2026); đứng sát hơn thì quả trứng to quá vòng tròn.
        net.minecraft.util.math.BlockPos altar = EggCycleState.get(player.getServer()).getEggAltarPos();
        if (player.getServerWorld().getRegistryKey() == com.samsara.registry.SamsaraDimensions.THE_WOMB_WORLD_KEY
                && altar != null && Math.abs(altar.getX() + 0.5 - egg.getX()) < 2 && Math.abs(altar.getZ() + 0.5 - egg.getZ()) < 2) {
            player.networkHandler.requestTeleport(egg.getX(), altar.getY() - 1.0, egg.getZ() + 6.0, 180.0f, 0.0f);
        }

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
