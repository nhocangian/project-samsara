package com.samsara.system.overworld;

import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.womb.WanderChaseSystem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Khối lỗi phiên bản (Overworld): thỉnh thoảng một khối tự nhiên trong tầm nhìn (khúc gỗ trên cây, khối đá cuội trong
 * vách...) hiện texture Alpha trong 1-2 giây, rung nhẹ và nhoè màu như băng từ đọc lệch, rồi trở lại bình thường. Đi
 * lại gần (dưới 6 khối) thì nó thôi ngay. Mắt thường: từ Stage 2, vài phút một lần. Qua máy quay đang ngắm: từ Stage 1,
 * chừng nửa phút một lần ("thứ ghi hình thấy sự thật"). Gieo trước cho tầng 4 Tầng Dưới, nơi cả hòn đảo là bản Alpha.
 *
 * <p>Server chọn khối (tia nhìn lệch ngẫu nhiên trong nón 30°, khoảng cách 10-28 khối, chỉ các khối có texture Alpha
 * trong {@link #TEXTURES}) rồi gửi {@link #PACKET} (vị trí, số tick, khối) cho đúng người đó; phía client
 * ({@code client/overworld/VersionGlitchRenderer}) vẽ một khối Alpha phủ lên. Không đổi khối thật trong thế giới.
 */
public final class VersionGlitch {
    private VersionGlitch() {}

    public static final Identifier PACKET = new Identifier("samsara", "version_glitch");

    /**
     * Khối có bản Alpha: {mặt bên, mặt trên/dưới}, tên texture trong {@code samsara:textures/overworld/version_glitch/}
     * (sinh từ bản Alpha của tầng 4 bằng {@code mod/tools/make_glitch_textures.py}).
     */
    public static final Map<Block, String[]> TEXTURES = new HashMap<>();
    static {
        TEXTURES.put(Blocks.STONE, new String[]{"stone", "stone"});
        TEXTURES.put(Blocks.COBBLESTONE, new String[]{"cobblestone", "cobblestone"});
        TEXTURES.put(Blocks.DIRT, new String[]{"dirt", "dirt"});
        TEXTURES.put(Blocks.GRAVEL, new String[]{"gravel", "gravel"});
        TEXTURES.put(Blocks.SAND, new String[]{"sand", "sand"});
        TEXTURES.put(Blocks.OAK_LOG, new String[]{"oak_log", "oak_log_top"});
        TEXTURES.put(Blocks.OAK_PLANKS, new String[]{"oak_planks", "oak_planks"});
        TEXTURES.put(Blocks.COAL_ORE, new String[]{"coal_ore", "coal_ore"});
        TEXTURES.put(Blocks.IRON_ORE, new String[]{"iron_ore", "iron_ore"});
        TEXTURES.put(Blocks.CRAFTING_TABLE, new String[]{"crafting_table_side", "crafting_table_top"});
        TEXTURES.put(Blocks.GRASS_BLOCK, new String[]{"grass_block_side", "grass_block_top"});
        TEXTURES.put(Blocks.OAK_LEAVES, new String[]{"oak_leaves", "oak_leaves"});
    }

    private static final int MIN_DIST = 10;
    private static final int MAX_DIST = 28;
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(VersionGlitch::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 10 != 0 || FilmingState.scripted(server)) return;
        int stage = EggCycleState.get(server).getStage();
        if (stage < 1) return;
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) return;
        long now = server.getTicks();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            boolean aiming = WanderChaseSystem.isAiming(player.getUuid());
            if (!aiming && stage < 2) continue;
            long next = NEXT.getOrDefault(player.getUuid(), now + (aiming ? 200 : 2400));
            NEXT.putIfAbsent(player.getUuid(), next);
            if (now < next) continue;
            if (trigger(world, player)) {
                int gap = aiming ? 400 + world.random.nextInt(400) : 2400 + world.random.nextInt(2400);
                NEXT.put(player.getUuid(), now + gap);
            }
        }
    }

    /** Tìm một khối hợp lệ trong tầm nhìn và gửi cho người chơi. */
    public static boolean trigger(ServerWorld world, ServerPlayerEntity player) {
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        for (int attempt = 0; attempt < 12; attempt++) {
            Vec3d dir = look.add((world.random.nextDouble() - 0.5) * 0.55, (world.random.nextDouble() - 0.5) * 0.4,
                    (world.random.nextDouble() - 0.5) * 0.55).normalize();
            BlockHitResult hit = world.raycast(new RaycastContext(eye, eye.add(dir.multiply(MAX_DIST)),
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
            if (hit.getType() != HitResult.Type.BLOCK) continue;
            BlockPos pos = hit.getBlockPos();
            if (pos.getSquaredDistance(eye) < MIN_DIST * MIN_DIST) continue;
            Block block = world.getBlockState(pos).getBlock();
            if (!TEXTURES.containsKey(block)) continue;
            send(player, pos, 24 + world.random.nextInt(18), block);
            return true;
        }
        return false;
    }

    private static void send(ServerPlayerEntity player, BlockPos pos, int ticks, Block block) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeVarInt(ticks);
        buf.writeIdentifier(Registries.BLOCK.getId(block));
        ServerPlayNetworking.send(player, PACKET, buf);
    }

    /** Lệnh test: một khối Alpha ngay trong tầm nhìn (bỏ qua stage và thời gian chờ; gần cũng được). */
    public static boolean testNow(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d eye = player.getEyePos();
        BlockHitResult hit = world.raycast(new RaycastContext(eye, eye.add(player.getRotationVec(1.0f).multiply(MAX_DIST)),
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Block block = world.getBlockState(hit.getBlockPos()).getBlock();
            if (TEXTURES.containsKey(block)) {
                send(player, hit.getBlockPos(), 60, block);
                return true;
            }
        }
        return trigger(world, player);
    }
}
