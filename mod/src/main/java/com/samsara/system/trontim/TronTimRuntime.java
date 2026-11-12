package com.samsara.system.trontim;

import com.samsara.entity.EmEntity;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.registry.SamsaraEntities;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.womb.WombTravel;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tầng 3 "Trốn tìm": ba vòng. Đứa em trốn và đếm 1..100 trong chat (mỗi giây một số); cứ vài số nó cười một tiếng từ chỗ
 * trốn, tiếng cười tạo sóng vàng (client). Tới sát nó thì nó chạy mất, để lại một món đồ. Hết 100 thì "anh cham qua. lai nha"
 * và đếm lại; món đã nhặt thì giữ. Vòng 3 nó không cười, đếm kẹt ở 47, chỉ còn hộp nhạc trên gác; trong tủ quần áo là
 * kẻ ngồi trốn mang skin người chơi. Tìm thấy thì cửa phòng ngủ mở. Ngủ: tên "em" nhiễu thành tên người chơi, rồi Bến Đò.
 */
public final class TronTimRuntime {
    private TronTimRuntime() {}

    private enum Phase { INTRO, COUNT, FOUND, DONE, SLEEP }

    private static final class State {
        int round = 1;
        Phase phase = Phase.INTRO;
        int timer = 0;
        int count = 0;
        boolean moved = false;
        int crowd = 0;
        UUID em = null;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final int COUNT_TICKS = 20;
    private static final int LAST_NUMBER = 100;
    private static final int STUCK_AT = 47;

    // ==========================================
    // VÀO TẦNG / LỆNH TEST
    // ==========================================

    /** Từ giường cuối tầng 2: tỉnh dậy ở sân trước căn nhà tối đen. */
    public static void enter(ServerPlayerEntity player) {
        ServerWorld world = world(player);
        if (world == null) return;
        TronTimBuilder.build(world);
        start(player, 1);
        BlockPos a = TronTimBuilder.arrival();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0, false, false, false));
        player.teleport(world, a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 180.0f, 0.0f);
    }

    /** Lệnh test: bắt đầu ở vòng {@code round} (1-3), hoặc 4 = đã tìm đủ, cửa phòng ngủ mở. */
    public static void testRound(ServerPlayerEntity player, int round) {
        ServerWorld world = world(player);
        if (world == null) return;
        TronTimBuilder.build(world);
        BlockPos p;
        if (round >= 4) {
            removeEm(world);
            State s = new State();
            s.phase = Phase.DONE;
            STATES.put(player.getUuid(), s);
            TronTimBuilder.openBedroom(world);
            p = TronTimBuilder.bedroom();
        } else {
            start(player, round);
            p = round == 3 ? TronTimBuilder.upstairs() : TronTimBuilder.arrival();
        }
        player.teleport(world, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 180.0f, 0.0f);
    }

    public static void testSleep(ServerPlayerEntity player) {
        State s = STATES.computeIfAbsent(player.getUuid(), k -> new State());
        startSleep(player, s);
    }

    private static ServerWorld world(ServerPlayerEntity player) {
        return player.getServer().getWorld(SamsaraDimensions.TRON_TIM_WORLD_KEY);
    }

    private static void start(ServerPlayerEntity player, int round) {
        ServerWorld world = world(player);
        removeEm(world);
        State s = new State();
        s.round = round;
        s.phase = Phase.INTRO;
        STATES.put(player.getUuid(), s);
    }

    private static void removeEm(ServerWorld world) {
        world.getEntitiesByClass(EmEntity.class, TronTimBuilder.houseBox(), e -> true).forEach(e -> e.discard());
    }

    // ==========================================
    // VÒNG LẶP
    // ==========================================

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey() != SamsaraDimensions.TRON_TIM_WORLD_KEY) return;
            if (world.getPlayers().isEmpty()) return;
            ambient(world);
            tickMusic(world);
            // Chép danh sách: cuối tầng người chơi bị đưa sang chiều khác ngay trong vòng lặp
            for (ServerPlayerEntity player : new java.util.ArrayList<>(world.getPlayers())) tick(world, player);
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.getRegistryKey() != SamsaraDimensions.TRON_TIM_WORLD_KEY) return ActionResult.PASS;
            if (!(world.getBlockState(hit.getBlockPos()).getBlock() instanceof BedBlock)) return ActionResult.PASS;
            if (player instanceof ServerPlayerEntity sp) {
                State s = STATES.get(sp.getUuid());
                if (s != null && s.phase == Phase.DONE && TronTimBuilder.inBedroom(hit.getBlockPos())) startSleep(sp, s);
            }
            return ActionResult.SUCCESS;
        });
    }

    /** Tiếng của căn nhà: xích đu, đồng hồ, vòi nước. Mỗi tiếng là một vòng sóng ở client. */
    private static void ambient(ServerWorld world) {
        long t = world.getTime();
        if (t % 80 == 0) world.playSound(null, TronTimBuilder.SWING, SoundEvents.BLOCK_CHAIN_STEP, SoundCategory.BLOCKS, 0.9f, 0.6f);
        if (t % 30 == 0) world.playSound(null, TronTimBuilder.CLOCK, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.BLOCKS, 0.5f, 1.8f);
        if (t % 47 == 0) world.playSound(null, TronTimBuilder.DRIP, SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_WATER, SoundCategory.BLOCKS, 0.8f, 0.9f);
    }

    private static void tick(ServerWorld world, ServerPlayerEntity player) {
        State s = STATES.get(player.getUuid());
        if (s == null) {
            // Vào lại thế giới giữa chừng: bắt đầu lại vòng 1, căn nhà giữ nguyên
            start(player, 1);
            s = STATES.get(player.getUuid());
        }
        s.timer++;
        switch (s.phase) {
            case INTRO -> tickIntro(world, player, s);
            case COUNT -> tickCount(world, player, s);
            case FOUND -> tickFound(world, player, s);
            case SLEEP -> tickSleep(world, player, s);
            default -> { }
        }
    }

    private static void tickIntro(ServerWorld world, ServerPlayerEntity player, State s) {
        String[] lines = s.round == 1 ? new String[]{"anh_oi", "choi_tron_tim", "em_tron", "dem_toi_100"}
                : s.round == 2 ? new String[]{"em_tron_tiep"} : new String[]{"lan_nay_kho"};
        int i = (s.timer - 40) / 30;
        if (s.timer >= 40 && (s.timer - 40) % 30 == 0 && i < lines.length) em(player, Text.translatable("samsara.tron_tim.em." + lines[i]));
        if (s.timer >= 40 + lines.length * 30 + 20) {
            spawnEm(world, s);
            s.phase = Phase.COUNT;
            s.timer = 0;
            s.count = 0;
            s.moved = false;
        }
    }

    private static void tickCount(ServerWorld world, ServerPlayerEntity player, State s) {
        EmEntity em = em(world, s);
        if (em == null) {
            spawnEm(world, s);
            em = em(world, s);
            if (em == null) return;
        }
        // Tìm thấy?
        double reach = s.round == 3 ? 2.6 : 1.8;
        Vec3d d = player.getPos().subtract(em.getPos());
        if (Math.abs(d.y) < 2.5 && d.x * d.x + d.z * d.z < reach * reach) {
            found(world, player, s, em);
            return;
        }
        if (s.timer % COUNT_TICKS != 0) return;

        // Vòng 3: đếm tới 47 thì kẹt; không cười, chỉ còn hộp nhạc
        if (s.round == 3) {
            if (s.count < STUCK_AT) {
                s.count++;
                em(player, Text.literal(String.valueOf(s.count)));
            } else if ((s.timer / COUNT_TICKS) % 3 == 0) {
                em(player, Text.literal(String.valueOf(STUCK_AT)));
            }
            if ((s.timer / COUNT_TICKS) % 5 == 0) musicBox(world);
            return;
        }

        s.count++;
        em(player, Text.literal(String.valueOf(s.count)));
        int every = s.round == 1 ? 7 : 12;
        if (s.count % every == 0) giggle(world, em);
        // Vòng 2: giữa chừng nó lẻn sang chỗ khác (bước chân nó cũng tạo sóng)
        if (s.round == 2 && !s.moved && s.count == 50) {
            s.moved = true;
            BlockPos b = TronTimBuilder.SPOT_KITCHEN_B;
            em.getNavigation().startMovingTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 1.2);
        }
        if (s.count >= LAST_NUMBER) {
            em(player, Text.translatable("samsara.tron_tim.em.cham_qua"));
            em.discard();
            s.em = null;
            s.phase = Phase.INTRO;
            s.timer = 40 + 30;   // bỏ qua lời mở đầu, chờ một nhịp rồi đếm lại
            if (s.round == 1) s.timer = 40 + 4 * 30;
        }
    }

    private static void found(ServerWorld world, ServerPlayerEntity player, State s, EmEntity em) {
        s.phase = Phase.FOUND;
        s.timer = 0;
        BlockPos spot = em.getBlockPos();
        drop(world, spot, s.round);
        if (s.round == 3) {
            // Kẻ ngồi trốn không chạy: nó ngẩng đầu nhìn
            em.setPitch(-35f);
            em.setHeadYaw(em.getYaw());
            world.playSound(null, spot, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.BLOCKS, 0.4f, 0.5f);
            return;
        }
        em(player, Text.translatable("samsara.tron_tim.em.hihi"));
        em(player, Text.translatable("samsara.tron_tim.em.thay_roi"));
        giggle(world, em);
        BlockPos run = s.round == 1 ? TronTimBuilder.at(10, 0, -10) : TronTimBuilder.at(13, 0, -27);
        em.getNavigation().startMovingTo(run.getX() + 0.5, run.getY(), run.getZ() + 0.5, 1.6);
    }

    private static void tickFound(ServerWorld world, ServerPlayerEntity player, State s) {
        EmEntity em = em(world, s);
        if (s.round == 3) {
            if (s.timer == 50 && em != null) {
                em.discard();
                s.em = null;
            }
            if (s.timer == 70) {
                TronTimBuilder.openBedroom(world);
                world.playSound(null, TronTimBuilder.BEDROOM_DOOR, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.BLOCKS, 1.5f, 0.6f);
                s.phase = Phase.DONE;
            }
            return;
        }
        if (s.timer == 70 && em != null) {
            em.discard();
            s.em = null;
        }
        if (s.timer >= 110) {
            s.round++;
            s.phase = Phase.INTRO;
            s.timer = 0;
        }
    }

    // ==========================================
    // NGỦ
    // ==========================================

    private static void startSleep(ServerPlayerEntity player, State s) {
        if (s.phase == Phase.SLEEP) return;
        s.phase = Phase.SLEEP;
        s.timer = 0;
        s.crowd = 0;
    }

    /** Lúc ngủ: "anh oi" lặp đi lặp lại; mỗi lần có thêm vài người đứng sau lưng, gần hơn, cùng nhìn về người chơi. */
    private static final int[] CALLS = {20, 42, 62, 80, 96, 110, 122};
    private static final int CROWD_MAX = 22;

    /**
     * Tên "em" trong chat nhiễu dần thành tên người chơi. Mỗi tiếng "anh oi" đi kèm một tiếng động khẽ ngay sau lưng,
     * nên sóng tiếng vọng soi ra cả một đám người đứng im (skin của chính người chơi) đang nhìn. Rồi tối hẳn, <System>, Bến Đò.
     */
    private static void tickSleep(ServerWorld world, ServerPlayerEntity player, State s) {
        String name = player.getGameProfile().getName();
        Text line = Text.translatable("samsara.tron_tim.em.anh_oi");
        for (int k = 0; k < CALLS.length; k++) {
            if (s.timer != CALLS[k]) continue;
            MutableText who = k < 2 ? Text.literal("em")
                    : k < 4 ? Text.literal("e").append(Text.literal("m").formatted(Formatting.OBFUSCATED))
                    : k < 6 ? Text.literal(name).formatted(Formatting.OBFUSCATED)
                    : Text.literal(name);
            chat(player, who, line);
            spawnCrowd(world, player, s, k);
            // Tiếng động khẽ sau lưng: sóng của nó soi ra đám đông
            // Tiếng động khẽ ở phía cửa (người chơi quay lưng ra cửa để bấm giường)
            Vec3d toDoor = doorCenter().subtract(player.getPos()).multiply(1, 0, 1).normalize();
            Vec3d back = toDoor.multiply(1.5).add(player.getPos());
            world.playSound(null, back.x, back.y + 1.0, back.z, SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM,
                    SoundCategory.NEUTRAL, 0.5f, 0.55f + 0.05f * k);
        }
        // Cả đám luôn quay mặt về người chơi
        for (EmEntity e : world.getEntitiesByClass(EmEntity.class, TronTimBuilder.houseBox(), EmEntity::isCrowd)) {
            double dx = player.getX() - e.getX(), dz = player.getZ() - e.getZ();
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            e.setYaw(yaw);
            e.setHeadYaw(yaw);
            e.setBodyYaw(yaw);
        }
        switch (s.timer) {
            case 140 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 120, 0, false, false, false));
            case 170 -> player.sendMessage(Text.literal("§e<System> ")
                    .append(Text.translatable("samsara.tron_tim.system").formatted(Formatting.YELLOW)), false);
            case 225 -> {
                STATES.remove(player.getUuid());
                removeEm(world);
                EggCycleState.get(player.getServer()).markTangDuoiCleared(player.getUuid());
                WombTravel.teleportToTheWomb(player);
            }
            default -> { }
        }
    }

    private static Vec3d doorCenter() {
        BlockPos d = TronTimBuilder.BEDROOM_DOOR;
        return new Vec3d(d.getX() + 1.0, d.getY(), d.getZ() + 0.5);
    }

    /** Thêm vài người từ phía cửa phòng ngủ (ngoài ô cửa trước, rồi lấn vào trong), mỗi đợt đứng gần người chơi hơn. */
    private static void spawnCrowd(ServerWorld world, ServerPlayerEntity player, State s, int wave) {
        int add = Math.min(2 + wave, CROWD_MAX - s.crowd);
        // Đám đông kéo vào từ phía cửa phòng ngủ: đợt đầu đứng ngoài ô cửa, mỗi đợt tiến vào gần người chơi hơn
        Vec3d door = doorCenter();
        double doorDist = Math.sqrt(player.squaredDistanceTo(door.x, player.getY(), door.z));
        double baseYaw = Math.atan2(door.z - player.getZ(), door.x - player.getX());
        double dist = doorDist + 2.5 - wave * (doorDist + 1.0) / 6.0;
        java.util.Random random = new java.util.Random(player.getUuid().getLeastSignificantBits() + wave);
        int placed = 0;
        for (int tries = 0; tries < add * 12 && placed < add; tries++) {
            // Ưu tiên sau lưng; thử mãi không được thì lan dần ra hai bên (phòng ngủ chật, nhiều tường và đồ đạc)
            double spread = Math.PI * Math.min(1.6, 0.5 + tries * 0.04);
            double a = baseYaw + (random.nextDouble() - 0.5) * spread;
            double r = Math.max(1.6, dist + (random.nextDouble() - 0.5) * 3.0);
            double x = player.getX() + Math.cos(a) * r, z = player.getZ() + Math.sin(a) * r;
            BlockPos at = BlockPos.ofFloored(x, player.getY(), z);
            if (!world.getBlockState(at).isAir() || !world.getBlockState(at.up()).isAir()) continue;
            EmEntity e = SamsaraEntities.EM.create(world);
            if (e == null) continue;
            e.refreshPositionAndAngles(x, player.getY(), z, 0f, 0f);
            e.setCrowd(true);
            world.spawnEntity(e);
            s.crowd++;
            placed++;
        }
    }

    // ==========================================
    // ĐỨA EM
    // ==========================================

    private static EmEntity em(ServerWorld world, State s) {
        if (s.em == null) return null;
        return world.getEntity(s.em) instanceof EmEntity e && e.isAlive() ? e : null;
    }

    private static void spawnEm(ServerWorld world, State s) {
        removeEm(world);
        EmEntity e = SamsaraEntities.EM.create(world);
        if (e == null) return;
        BlockPos p = s.round == 1 ? TronTimBuilder.SPOT_TABLE : s.round == 2 ? TronTimBuilder.SPOT_KITCHEN_A : TronTimBuilder.SPOT_WARDROBE;
        float yaw = s.round == 3 ? 0f : 180f;   // kẻ ngồi trốn quay mặt ra cửa tủ (nam)
        e.refreshPositionAndAngles(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, s.round == 3 ? 20f : 0f);
        e.setHeadYaw(yaw);
        e.setBodyYaw(yaw);
        e.setHider(s.round == 3);
        world.spawnEntity(e);
        s.em = e.getUuid();
    }

    /** Tiếng cười khúc khích từ chỗ trốn (âm allay, cao). Client tô vòng sóng của nó màu vàng ấm. */
    private static void giggle(ServerWorld world, EmEntity em) {
        world.playSound(null, em.getX(), em.getY() + 1.0, em.getZ(), SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.NEUTRAL, 1.0f, 1.35f);
    }

    /** Hộp nhạc trên gác: năm nốt chuông, cách nhau 6 tick. */
    private static void musicBox(ServerWorld world) {
        float[] pitches = {1.0f, 1.19f, 1.33f, 1.19f, 0.89f};
        for (int i = 0; i < pitches.length; i++) MUSIC_QUEUE.put(world.getTime() + 1 + i * 6L, pitches[i]);
    }

    private static final Map<Long, Float> MUSIC_QUEUE = new HashMap<>();

    private static void tickMusic(ServerWorld world) {
        if (MUSIC_QUEUE.isEmpty()) return;
        Float pitch = MUSIC_QUEUE.remove(world.getTime());
        if (pitch != null) world.playSound(null, TronTimBuilder.MUSIC, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 0.7f, pitch);
        MUSIC_QUEUE.keySet().removeIf(t -> t < world.getTime());
    }

    /** Món đồ đứa em để lại chỗ nó vừa núp. */
    private static void drop(ServerWorld world, BlockPos spot, int round) {
        ItemStack stack = switch (round) {
            case 1 -> new ItemStack(Items.DANDELION);
            case 2 -> new ItemStack(Items.COOKIE);
            default -> new ItemStack(Items.PAPER);
        };
        stack.setCustomName(Text.translatable("samsara.tron_tim.mon." + round).formatted(Formatting.WHITE));
        ItemEntity item = new ItemEntity(world, spot.getX() + 0.5, spot.getY() + 0.2, spot.getZ() + 0.5, stack, 0, 0, 0);
        item.setNeverDespawn();
        world.spawnEntity(item);
    }

    // ==========================================
    // CHAT
    // ==========================================

    private static void em(ServerPlayerEntity player, Text message) {
        chat(player, Text.literal("em"), message);
    }

    private static void chat(ServerPlayerEntity player, MutableText name, Text message) {
        player.sendMessage(Text.literal("<").append(name).append("> ").append(message), false);
    }
}
