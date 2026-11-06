package com.samsara.system.cycle;

import com.samsara.registry.SamsaraSounds;
import com.samsara.system.memory.NamMemoryManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Chuỗi "bị hút đi" sau giấc ngủ thứ 7: không gian kéo dãn rồi người chơi tỉnh dậy ở
 * chiều "Ký Ức Của Nam" (cánh đồng hướng dương). The Womb chỉ tới được qua vết rách dưới hầm nhà Nam.
 *
 *  1. Thức dậy: client nhận gói TRANSITION_PACKET -> không gian kéo dãn (FOV giãn rộng, vệt sao
 *     toả ra từ tâm, tối dần). Người chơi bị nhấc khỏi giường, không thể di chuyển.
 *  2. Hết TRANSITION_TICKS: dịch chuyển (NamMemoryManager) rồi gửi ARRIVE_PACKET -> chớp trắng,
 *     không gian co lại về bình thường.
 *
 * Lệnh test: /samsara test womb_transition.
 */
public class WombTransitionSystem {
    public static final Identifier TRANSITION_PACKET = new Identifier("samsara", "womb_transition");
    public static final Identifier ARRIVE_PACKET = new Identifier("samsara", "womb_arrive");

    public static final int TRANSITION_TICKS = 90;

    private record Pending(BlockPos bedPos, int startTick) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> PENDING.clear());
        ServerTickEvents.END_SERVER_TICK.register(WombTransitionSystem::tick);
    }

    public static boolean isInTransition(ServerPlayerEntity player) {
        return PENDING.containsKey(player.getUuid());
    }

    public static void begin(ServerPlayerEntity player, BlockPos bedPos) {
        if (PENDING.containsKey(player.getUuid())) return;
        PENDING.put(player.getUuid(), new Pending(bedPos.toImmutable(), player.getServer().getTicks()));

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(TRANSITION_TICKS);
        ServerPlayNetworking.send(player, TRANSITION_PACKET, buf);

        // Bị nhấc lên khỏi giường, chân tay nặng trĩu
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, TRANSITION_TICKS + 10, 10, false, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, TRANSITION_TICKS - 20, 0, false, false, false));

        ServerWorld world = player.getServerWorld();
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.AMBIENT, 0.9f, 0.5f);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SamsaraSounds.CRADLE_HUM, SoundCategory.AMBIENT, 1.2f, 0.5f);
    }

    private static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        int now = server.getTicks();

        Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Pending> entry = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                // Thoát game giữa chừng: cho phép lần ngủ sau thử lại
                EggCycleState.get(server).clearAwakened(entry.getKey());
                it.remove();
                continue;
            }

            int elapsed = now - entry.getValue().startTick();
            ServerWorld world = player.getServerWorld();

            // Bụi không gian bị hút ngược lên quanh người chơi
            if (elapsed % 2 == 0) {
                float p = Math.min(1.0f, elapsed / (float) TRANSITION_TICKS);
                world.spawnParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                        (int) (6 + p * 30), 2.5, 1.5, 2.5, 0.05 + p * 0.2);
            }
            if (elapsed == TRANSITION_TICKS - 25) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.AMBIENT, 1.4f, 0.5f);
            }

            if (elapsed >= TRANSITION_TICKS) {
                it.remove();
                player.removeStatusEffect(StatusEffects.LEVITATION);
                player.removeStatusEffect(StatusEffects.SLOWNESS);
                player.fallDistance = 0.0f;
                NamMemoryManager.arrive(player, true);
                ServerPlayNetworking.send(player, ARRIVE_PACKET, PacketByteBufs.empty());
            }
        }
    }
}
