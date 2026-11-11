package com.samsara.system.tangduoi;

import com.samsara.registry.SamsaraBlocks;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.womb.WombTravel;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tầng Dưới, tầng 2: rơi nước thì nổi lên ở điểm gần nhất, đoạn 3 tự xoá sau lưng, giường cuối dẫn tới Bến Đò.
 * Không có cái chết, không mất máu.
 */
public final class LoiDiNhapRuntime {
    private LoiDiNhapRuntime() {}

    private static final class State {
        int checkpoint = 0;
        final int[] falls = new int[LoiDiNhapBuilder.SECTIONS];
        int returnIn = 0;
        int age = 0;
        int revealed = -1;
        /** Đoạn 5: tick cuối cùng mỗi ô còn được nhìn thấy. */
        final long[] seen = new long[LoiDiNhapBuilder.section(LoiDiNhapBuilder.LOOK_SECTION).size()];
        boolean lookActive = false;
        int saveVersion = 0;
        int anchor = 0;
        boolean giantsGone = false;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    /** Giờ thế giới lúc giường đặt sai bên nhạt thành khung dây; -1 nếu không có. */
    private static long fadeAt = -1;
    private static final Map<UUID, Integer> SLEEPING = new HashMap<>();
    private static final Map<UUID, Integer> WELCOME = new HashMap<>();
    private static final int SLEEP_TICKS = 90;
    private static final double SURFACE = LoiDiNhapBuilder.ORIGIN.getY() + 0.5;
    /** Ô của đoạn 3 cách người chơi bao nhiêu ô (theo thứ tự lối đi) thì bị xoá. */
    private static final int ERASE_BEHIND = 3;
    /** Đoạn 5: ô nằm trong nón này (độ) quanh hướng ngắm máy quay thì có thật; ra khỏi nón quá GRACE tick thì tan. */
    private static final double LOOK_CONE = 52.0;
    private static final int LOOK_GRACE = 6;
    /** Đoạn 6: mỗi bản nháp đứng yên chừng này tick, báo trước SAVE_WARN tick. */
    private static final int SAVE_HALF = 100;
    private static final int SAVE_WARN = 30;

    /** Từ giường cuối tầng 1 sang: tỉnh dậy trong căn phòng nháp. */
    public static void enter(ServerPlayerEntity player) {
        ServerWorld world = player.getServer().getWorld(SamsaraDimensions.LOI_DI_NHAP_WORLD_KEY);
        if (world == null) return;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 110, 0, false, false, false));
        if (!LoiDiNhapBuilder.isBuilt(world)) LoiDiNhapBuilder.build(world);
        STATES.remove(player.getUuid());
        BlockPos a = LoiDiNhapBuilder.arrival();
        DuongOngRuntime.teleportLater(player, world, a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 180.0f, 0.0f, 60);
        WELCOME.put(player.getUuid(), 110);
    }

    public static void reset() {
        STATES.clear();
    }

    public static void startSleep(ServerPlayerEntity player) {
        if (SLEEPING.containsKey(player.getUuid())) return;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 120, 0, false, false, false));
        SLEEPING.put(player.getUuid(), SLEEP_TICKS);
    }

    /** Lệnh test: coi như vừa rơi xuống nước. */
    public static void forceFall(ServerPlayerEntity player) {
        fall(player.getServerWorld(), player, STATES.computeIfAbsent(player.getUuid(), k -> new State()));
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.getRegistryKey() != SamsaraDimensions.LOI_DI_NHAP_WORLD_KEY) return ActionResult.PASS;
            net.minecraft.item.ItemStack held = player.getStackInHand(hand);
            if (LoiDiNhapBuilder.isBedItem(held)) {
                // Giường của em chỉ đặt được vào một trong hai chỗ trống hai bên giường cuối
                int side = LoiDiNhapBuilder.bedSideAt(hit.getBlockPos());
                if (side == 0) return ActionResult.FAIL;
                if (world instanceof ServerWorld sw && LoiDiNhapBuilder.chosenSide(sw) == 0) {
                    LoiDiNhapBuilder.placeChosenBed(sw, side);
                    if (side != LoiDiNhapBuilder.RIGHT_SIDE) fadeAt = sw.getTime() + 50;
                    if (!player.getAbilities().creativeMode) held.decrement(1);
                }
                return ActionResult.SUCCESS;
            }
            if (!(world.getBlockState(hit.getBlockPos()).getBlock() instanceof BedBlock)) return ActionResult.PASS;
            if (player instanceof ServerPlayerEntity sp) {
                int chosen = LoiDiNhapBuilder.chosenSide(sp.getServerWorld());
                if (LoiDiNhapBuilder.isFinalBed(hit.getBlockPos())) {
                    // Chưa đặt giường em vào phòng thì chưa ngủ được
                    if (chosen == 0) sp.sendMessage(Text.translatable("samsara.tang_duoi2.thieu_giuong"), true);
                    else startSleep(sp);
                }
                else if (chosen == LoiDiNhapBuilder.RIGHT_SIDE && LoiDiNhapBuilder.isChosenBed(hit.getBlockPos(), chosen))
                    sp.sendMessage(Text.translatable("samsara.tang_duoi2.giuong_em"), true);
                else sp.sendMessage(Text.translatable("samsara.tang_duoi2.giuong_thu"), true);
            }
            return ActionResult.SUCCESS;
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey() != SamsaraDimensions.LOI_DI_NHAP_WORLD_KEY) return;
            tickSleep(world);
            if (fadeAt >= 0 && world.getTime() >= fadeAt) {
                fadeAt = -1;
                LoiDiNhapBuilder.fadeWrongBed(world);
            }
            for (ServerPlayerEntity player : world.getPlayers()) tickPlayer(world, player);
        });
    }

    private static void tickSleep(ServerWorld world) {
        for (Iterator<Map.Entry<UUID, Integer>> it = SLEEPING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Integer> e = it.next();
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(e.getKey());
            if (p == null) {
                it.remove();
                continue;
            }
            int left = e.getValue() - 1;
            e.setValue(left);
            if (left == 40) {
                p.sendMessage(Text.literal("§e<System> ").append(Text.translatable("samsara.tang_duoi2.mong_hon").formatted(Formatting.YELLOW)), false);
            }
            if (left == 15 && LoiDiNhapBuilder.chosenSide(p.getServerWorld()) == LoiDiNhapBuilder.RIGHT_SIDE) {
                p.sendMessage(Text.literal("§e<System> ").append(Text.translatable("samsara.tang_duoi2.mong_hon_giu").formatted(Formatting.YELLOW)), false);
            }
            if (left <= 0) {
                it.remove();
                EggCycleState.get(p.getServer()).markTangDuoiCleared(p.getUuid());
                WombTravel.teleportToTheWomb(p);
            }
        }
    }

    private static void tickPlayer(ServerWorld world, ServerPlayerEntity player) {
        UUID id = player.getUuid();
        State state = STATES.computeIfAbsent(id, k -> new State());
        Integer welcome = WELCOME.get(id);
        if (welcome != null) {
            if (welcome <= 1) {
                WELCOME.remove(id);
                player.sendMessage(Text.translatable("samsara.tang_duoi2.doi_che_do").formatted(Formatting.GRAY, Formatting.ITALIC), false);
            } else {
                WELCOME.put(id, welcome - 1);
            }
        }

        if (state.returnIn > 0 && --state.returnIn == 0) respawn(player, state);
        if (state.returnIn > 0) return;

        // Đã tới điểm nào thì giữ điểm đó
        if (player.isOnGround()) {
            int cp = LoiDiNhapBuilder.checkpointAt(player.getZ() - LoiDiNhapBuilder.ORIGIN.getZ());
            if (cp > state.checkpoint) state.checkpoint = cp;
        }
        tickGiants(world, player, state);
        if (world.getTime() % 4 == 0) tickPages(world, player);
        if (world.getTime() % 40 == 0 && LoiDiNhapBuilder.isFinalRoom(player.getZ())) tickBedItem(world, player);

        if (player.getY() < SURFACE) {
            fall(world, player, state);
            return;
        }
        tickErase(world, player, state);
        tickLook(world, player, state);
        tickSave(world, player, state);
        if (world.getTime() % 12 == 0 && state.checkpoint < LoiDiNhapBuilder.SECTIONS
                && state.falls[state.checkpoint] >= 5) {
            hint(world, player, state.checkpoint);
        }
    }

    /**
     * Mỗi đảo tới được thì thêm khối khổng lồ; chờ một lúc sau khi vào để thực thể đã lưu kịp tải rồi dựng lại cả bộ, tránh
     * nhân đôi. Qua đảo 3 thì dời sang điểm neo thứ hai. Bước vào đoạn nhiễu thì tắt hẳn.
     */
    private static void tickGiants(ServerWorld world, ServerPlayerEntity player, State state) {
        if (++state.age < 80) return;
        if (state.giantsGone || LoiDiNhapBuilder.isPastStatic(player.getZ())) {
            if (!state.giantsGone) {
                state.giantsGone = true;
                LoiDiNhapKhoi.vanish(world, player, state.revealed >= 0);
            }
            return;
        }
        int step = Math.min(state.checkpoint, LoiDiNhapKhoi.LAST);
        int anchor = LoiDiNhapKhoi.anchorFor(player.getZ() - LoiDiNhapBuilder.ORIGIN.getZ(), state.anchor);
        if (state.revealed < 0 || anchor != state.anchor) {
            boolean grew = state.revealed >= 0 && step > state.revealed;
            state.anchor = anchor;
            state.revealed = step;
            LoiDiNhapKhoi.rebuild(world, anchor, step);
            if (grew) LoiDiNhapKhoi.placeSound(world, player);
            return;
        }
        while (state.revealed < step) {
            state.revealed++;
            LoiDiNhapKhoi.reveal(world, player, anchor, state.revealed, state.revealed == step);
        }
    }

    /** Bước tới trang xé (chỉ thấy qua máy quay) thì nhặt được: một bản nháp khác của đoạn nhật ký. */
    private static void tickPages(ServerWorld world, ServerPlayerEntity player) {
        for (net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity e : world.getEntitiesByClass(
                net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity.class, player.getBoundingBox().expand(0.8, 1.0, 0.8),
                d -> d.getCommandTags().contains(LoiDiNhapBuilder.PAGE_TAG))) {
            int n = 0;
            for (String tag : e.getCommandTags()) {
                if (tag.startsWith(LoiDiNhapBuilder.PAGE_TAG + "_")) n = Integer.parseInt(tag.substring(LoiDiNhapBuilder.PAGE_TAG.length() + 1));
            }
            e.discard();
            if (n <= 0) continue;
            // Dựng lại tầng lúc chunk đảo chưa tải có thể để lại bản trùng: xoá luôn mọi bản cùng số quanh đó
            String same = LoiDiNhapBuilder.PAGE_TAG + "_" + n;
            world.getEntitiesByClass(net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity.class, e.getBoundingBox().expand(4.0),
                    d -> d.getCommandTags().contains(same)).forEach(d -> d.discard());
            if (hasPage(player, n)) continue;
            player.getInventory().offerOrDrop(LoiDiNhapBuilder.pageBook(n));
            world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 0.8f);
            player.sendMessage(Text.translatable("samsara.tang_duoi2.nhat_trang").formatted(Formatting.GRAY, Formatting.ITALIC), true);
        }
    }

    private static boolean hasPage(ServerPlayerEntity player, int n) {
        String title = LoiDiNhapBuilder.pageBook(n).getNbt().getString(net.minecraft.item.WrittenBookItem.TITLE_KEY);
        net.minecraft.entity.player.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            net.minecraft.item.ItemStack s = inv.getStack(i);
            if (s.isOf(net.minecraft.item.Items.WRITTEN_BOOK) && s.hasNbt()
                    && title.equals(s.getNbt().getString(net.minecraft.item.WrittenBookItem.TITLE_KEY))) return true;
        }
        return false;
    }

    /** Phòng cuối: nếu chưa đặt giường và người chơi chưa cầm, có một chiếc giường nằm trên thảm. */
    private static void tickBedItem(ServerWorld world, ServerPlayerEntity player) {
        if (LoiDiNhapBuilder.chosenSide(world) != 0) return;
        net.minecraft.entity.player.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (LoiDiNhapBuilder.isBedItem(inv.getStack(i))) return;
        }
        if (!world.getEntitiesByClass(net.minecraft.entity.ItemEntity.class, LoiDiNhapBuilder.finalRoomBox(),
                e -> LoiDiNhapBuilder.isBedItem(e.getStack())).isEmpty()) return;
        BlockPos p = LoiDiNhapBuilder.bedItemSpot();
        net.minecraft.entity.ItemEntity item = new net.minecraft.entity.ItemEntity(world, p.getX() + 0.5, p.getY() + 0.1, p.getZ() + 0.5,
                LoiDiNhapBuilder.bedItem(), 0, 0, 0);
        item.setNeverDespawn();
        world.spawnEntity(item);
    }

    /** Tối mắt một nhịp rồi nổi lên ở điểm gần nhất; rơi nhiều lần trong một đoạn thì đoạn đó hiện mờ. */
    private static void fall(ServerWorld world, ServerPlayerEntity player, State state) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false, false));
        state.returnIn = 18;
        int section = Math.min(state.checkpoint, LoiDiNhapBuilder.SECTIONS - 1);
        int n = ++state.falls[section];
        if (n == 3) LoiDiNhapBuilder.placeRedoSign(world, state.checkpoint);
    }

    private static void respawn(ServerPlayerEntity player, State state) {
        ServerWorld world = player.getServerWorld();
        if (state.checkpoint == LoiDiNhapBuilder.ERASE_SECTION) LoiDiNhapBuilder.restoreErasing(world);
        BlockPos p = LoiDiNhapBuilder.checkpoint(state.checkpoint);
        player.setVelocity(0, 0, 0);
        player.fallDistance = 0.0f;
        player.teleport(world, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 180.0f, 0.0f);
    }

    /** Đoạn 3: ô đã đi qua biến mất sau lưng. Còn đứng trên đảo trước đó thì đoạn phải đầy đủ. */
    private static void tickErase(ServerWorld world, ServerPlayerEntity player, State state) {
        if (state.checkpoint != LoiDiNhapBuilder.ERASE_SECTION) return;
        List<BlockPos> tiles = LoiDiNhapBuilder.section(LoiDiNhapBuilder.ERASE_SECTION);
        int nearest = -1;
        double best = 2.25;
        for (int i = 0; i < tiles.size(); i++) {
            BlockPos t = tiles.get(i);
            double d = player.squaredDistanceTo(t.getX() + 0.5, player.getY(), t.getZ() + 0.5);
            if (d < best) {
                best = d;
                nearest = i;
            }
        }
        if (nearest < 0) {
            double z = player.getZ() - LoiDiNhapBuilder.ORIGIN.getZ();
            if (z > LoiDiNhapBuilder.ERASE_START_Z && world.getTime() % 20 == 0) LoiDiNhapBuilder.restoreErasing(world);
            return;
        }
        for (int i = nearest - ERASE_BEHIND; i >= 0; i--) {
            BlockPos t = tiles.get(i);
            if (!world.getBlockState(t).isOf(SamsaraBlocks.KHOI_NHAP)) break;
            world.setBlockState(t, Blocks.WATER.getDefaultState(), 2 | 16);
            world.playSound(null, t, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.BLOCKS, 0.5f, 0.5f);
        }
    }

    /**
     * Đoạn 5 "nhìn chỗ em bước": ô chỉ có thật khi đang được nhìn qua máy quay. Ngẩng lên, nhìn sang bên hay hạ máy quay
     * thì ô dưới chân tan sau vài tick; nhìn lại thì ô vẽ lại. Còn ở đảo 4 (hay đã qua) thì đoạn phải đầy đủ.
     */
    private static void tickLook(ServerWorld world, ServerPlayerEntity player, State state) {
        int index = LoiDiNhapBuilder.LOOK_SECTION;
        List<BlockPos> tiles = LoiDiNhapBuilder.section(index);
        double z = player.getZ() - LoiDiNhapBuilder.ORIGIN.getZ();
        boolean on = state.checkpoint == index && z < LoiDiNhapBuilder.LOOK_START_Z;
        if (!on) {
            if (state.lookActive) {
                state.lookActive = false;
                LoiDiNhapBuilder.restoreSection(world, index);
            }
            return;
        }
        long now = world.getTime();
        if (!state.lookActive) {
            state.lookActive = true;
            java.util.Arrays.fill(state.seen, now);
        }
        boolean aiming = com.samsara.system.womb.WanderChaseSystem.isAiming(player.getUuid());
        net.minecraft.util.math.Vec3d eye = player.getEyePos();
        net.minecraft.util.math.Vec3d look = player.getRotationVec(1.0f);
        double cos = Math.cos(Math.toRadians(LOOK_CONE));
        net.minecraft.block.BlockState draft = SamsaraBlocks.KHOI_NHAP.getDefaultState();
        for (int i = 0; i < tiles.size(); i++) {
            BlockPos t = tiles.get(i);
            if (aiming) {
                net.minecraft.util.math.Vec3d to = new net.minecraft.util.math.Vec3d(t.getX() + 0.5, t.getY() + 1.0, t.getZ() + 0.5).subtract(eye);
                double len = to.length();
                if (len < 0.01 || look.dotProduct(to) / len > cos) state.seen[i] = now;
            }
            boolean present = world.getBlockState(t).isOf(SamsaraBlocks.KHOI_NHAP);
            boolean visible = now - state.seen[i] <= LOOK_GRACE;
            if (visible && !present) {
                world.setBlockState(t, draft, 2 | 16);
            } else if (!visible && present) {
                world.setBlockState(t, Blocks.WATER.getDefaultState(), 2 | 16);
                if (player.squaredDistanceTo(t.getX() + 0.5, t.getY() + 1.0, t.getZ() + 0.5) < 16.0) {
                    world.playSound(null, t, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.BLOCKS, 0.35f, 0.6f);
                }
            }
        }
    }

    /**
     * Đoạn 6 "tự động lưu": cứ SAVE_HALF tick lối đi đổi sang bản nháp kia (chặng 1 và 3 chỉ có ở bản 0, chặng 2 ở bản 1,
     * các ô ngã ba có ở cả hai). Báo trước bằng dòng "Đang tự động lưu..." và tiếng tích tắc. Ô đang đứng thì giữ lại
     * cho tới khi bước khỏi, nên đứng giữa chặng lúc đổi bản chỉ bị kẹt, chờ một nhịp là chặng quay về.
     */
    private static void tickSave(ServerWorld world, ServerPlayerEntity player, State state) {
        if (state.checkpoint != LoiDiNhapBuilder.SAVE_SECTION) return;
        long t = world.getTime();
        int version = (int) ((t / SAVE_HALF) % 2);
        int phase = (int) (t % SAVE_HALF);
        if (phase >= SAVE_HALF - SAVE_WARN) {
            if (phase % 10 == 0) {
                player.sendMessage(Text.translatable("samsara.tang_duoi2.dang_luu").formatted(Formatting.GRAY, Formatting.ITALIC), true);
                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.BLOCKS, 0.5f, 1.6f);
            }
        }
        BlockPos under = BlockPos.ofFloored(player.getX(), LoiDiNhapBuilder.ORIGIN.getY(), player.getZ());
        boolean changed = version != state.saveVersion;
        state.saveVersion = version;
        if (changed) {
            player.sendMessage(Text.translatable("samsara.tang_duoi2.da_luu", version + 1).formatted(Formatting.GRAY, Formatting.ITALIC), true);
            player.playSound(SoundEvents.UI_TOAST_IN, SoundCategory.BLOCKS, 0.8f, 0.7f);
        }
        List<BlockPos> keep = LoiDiNhapBuilder.saveVersion(version);
        net.minecraft.block.BlockState draft = SamsaraBlocks.KHOI_NHAP.getDefaultState();
        for (BlockPos p : LoiDiNhapBuilder.section(LoiDiNhapBuilder.SAVE_SECTION)) {
            boolean want = keep.contains(p) || p.equals(under);
            boolean present = world.getBlockState(p).isOf(SamsaraBlocks.KHOI_NHAP);
            if (want && !present) world.setBlockState(p, draft, 2 | 16);
            else if (!want && present) world.setBlockState(p, Blocks.WATER.getDefaultState(), 2 | 16);
        }
    }

    private static void hint(ServerWorld world, ServerPlayerEntity player, int section) {
        for (BlockPos t : LoiDiNhapBuilder.section(section)) {
            if (player.squaredDistanceTo(t.getX() + 0.5, t.getY() + 1.0, t.getZ() + 0.5) > 40 * 40) continue;
            world.spawnParticles(player, ParticleTypes.SMOKE, false, t.getX() + 0.5, t.getY() + 1.05, t.getZ() + 0.5, 1, 0.2, 0.0, 0.2, 0.0);
        }
    }
}
