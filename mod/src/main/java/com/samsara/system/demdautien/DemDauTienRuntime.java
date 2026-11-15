package com.samsara.system.demdautien;

import com.samsara.registry.SamsaraBlocks;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.womb.WombTravel;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Tầng 4 "Đêm đầu tiên": luật chơi (thiết kế: {@code scripts/de_xuat/tang_duoi_04_dem_dau_tien.md}).
 * <ul>
 *   <li>Vào tầng: cất túi đồ (cả máy quay) vào {@link EggCycleState}, tỉnh dậy trắng tay giữa đảo, chat
 *       "Minecraft Alpha v1.2.6". Đồng hồ riêng ({@link DemDauTienClock}) chạy từ sáng muộn tới tối hẳn (~9 phút).</li>
 *   <li>Ba con cừu luôn nhìn theo người chơi; đánh thì rơi 1 len và thành cừu trụi lông, quanh nó mở một vết rách nhỏ
 *       (bên trong vẫn thấy nó còn lông); Khối Chưa Tải ăn vào một vòng.</li>
 *   <li>Giường chỉ chế được ở bàn chế tạo nằm trong vết rách ({@code CraftingScreenHandlerMixin}), chỉ đặt được trong
 *       vết rách ({@code BedBlockMixin} → {@link #onBedPlaced}), chỉ nằm được khi trời tối.</li>
 *   <li>Ngủ: vết rách nở ra nuốt đảo (shader đọc đồng hồ), trắng xoá, trả túi đồ, sang Bến Đò.</li>
 *   <li>Tối hẳn mà chưa ngủ, rơi khỏi đảo hay chết: Khối Chưa Tải ăn tới chân, làm lại tầng từ đầu.</li>
 * </ul>
 * Giả định một người chơi ở tầng này một lúc (đồng hồ chung cho cả chiều).
 */
public final class DemDauTienRuntime {
    private DemDauTienRuntime() {}

    /** Bề rộng (khối) mỗi vòng Khối Chưa Tải ăn vào. */
    private static final int RING = 2;
    private static final int NIGHT_RING = 3;
    /** Số cột xử lý mỗi tick khi một vòng đang ăn vào. */
    private static final int COLUMNS_PER_TICK = 160;

    private static boolean active;
    private static long anchor;
    private static long start = DemDauTienClock.START;
    /** Cừu đã bị đánh (bit k = con thứ k trong {@link DemDauTienBuilder#SHEEP}); đi kèm đồng hồ để shader biết. */
    private static int sheared;
    /** Tổng bán kính (khối) đã bị ăn từ mép đảo vào. */
    private static int eaten;
    private static final Deque<int[]> COLUMNS = new ArrayDeque<>();
    private static final List<long[]> BED_BREAKS = new ArrayList<>();   // {pos, tick}
    private static int nightTimer;
    private static int failTimer = -1;
    private static int finaleTimer = -1;
    private static BlockPos finaleBed;

    // ==========================================
    // ĐỒNG HỒ
    // ==========================================

    /** Giờ trong ngày của tầng phía server ({@code DemDauTienTimeMixin} trả cho World.getTimeOfDay). */
    public static long timeOfDay(World world) {
        if (!active) return DemDauTienClock.START;
        return start + (world.getTime() - anchor) + sheared * 24000L;
    }

    /** Giờ trong ngày của tầng (không kèm bitmask cừu). */
    private static long hour(World world) {
        return DemDauTienClock.hour(timeOfDay(world));
    }

    private static void setClock(ServerWorld world, long startTime) {
        active = true;
        anchor = world.getTime();
        start = startTime;
        for (ServerPlayerEntity p : world.getPlayers()) sendClock(p);
    }

    private static void sendClock(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeLong(anchor);
        buf.writeLong(start + sheared * 24000L);
        ServerPlayNetworking.send(player, DemDauTienClock.PACKET, buf);
    }

    // ==========================================
    // VÀO / LÀM LẠI / RA
    // ==========================================

    /** Vào tầng (từ giường cuối tầng 3, hoặc lệnh test). */
    public static void enter(ServerPlayerEntity player) {
        ServerWorld world = DemDauTienBuilder.world(player);
        if (world == null) return;
        EggCycleState state = EggCycleState.get(player.getServer());
        if (!state.hasDemDauTienStash(player.getUuid())) {
            NbtList inv = new NbtList();
            player.getInventory().writeNbt(inv);
            state.stashDemDauTien(player.getUuid(), inv);
        }
        restart(world, player);
    }

    /** Dựng lại đảo, trả trời về sáng muộn, người chơi trắng tay ở giữa đảo. */
    private static void restart(ServerWorld world, ServerPlayerEntity player) {
        COLUMNS.clear();
        BED_BREAKS.clear();
        eaten = 0;
        sheared = 0;
        nightTimer = 0;
        failTimer = -1;
        finaleTimer = -1;
        finaleBed = null;
        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, islandBox(), e -> true)) item.discard();
        DemDauTienBuilder.build(world);
        player.getInventory().clear();
        player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);
        player.removeStatusEffect(StatusEffects.SLOWNESS);
        player.removeStatusEffect(StatusEffects.JUMP_BOOST);
        BlockPos s = DemDauTienBuilder.spawn();
        player.teleport(world, s.getX() + 0.5, s.getY(), s.getZ() + 0.5, 30.0f, 0.0f);
        player.fallDistance = 0;
        setClock(world, DemDauTienClock.START);
        player.sendMessage(Text.translatable("samsara.dem_dau_tien.phien_ban").formatted(Formatting.GRAY), false);
    }

    private static void leave(ServerWorld world, ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.SLOWNESS);
        player.removeStatusEffect(StatusEffects.JUMP_BOOST);
        player.removeStatusEffect(StatusEffects.BLINDNESS);
        player.getInventory().clear();
        player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
        NbtList inv = EggCycleState.get(player.getServer()).takeDemDauTienStash(player.getUuid());
        if (inv != null) player.getInventory().readNbt(inv);
        EggCycleState.get(player.getServer()).markTangDuoiCleared(player.getUuid());
        active = false;
        finaleTimer = -1;
        WombTravel.teleportToTheWomb(player);
    }

    private static void fail(ServerPlayerEntity player) {
        if (failTimer >= 0 || finaleTimer >= 0) return;
        failTimer = 0;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0, false, false, false));
    }

    // ==========================================
    // ĐĂNG KÝ SỰ KIỆN
    // ==========================================

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (DemDauTienClock.isFloor(world)) tick(world);
        });

        // Đánh cừu: rơi 1 len, cừu trụi lông, quanh nó mở một vết rách nhỏ (bên kia vẫn còn lông); đảo co lại một vòng
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!DemDauTienClock.isFloor(world) || !(entity instanceof SheepEntity sheep)) return ActionResult.PASS;
            if (world.isClient) return ActionResult.PASS;
            if (sheep.isSheared()) return ActionResult.SUCCESS;
            world.spawnEntity(new ItemEntity(world, sheep.getX(), sheep.getY() + 0.5, sheep.getZ(), new ItemStack(Items.WHITE_WOOL)));
            sheep.setSheared(true);
            int k = nearestSheepSpot(sheep);
            if (k >= 0) {
                sheared |= 1 << k;
                for (ServerPlayerEntity p : ((ServerWorld) world).getPlayers()) sendClock(p);
            }
            queueRing((ServerWorld) world, RING);
            return ActionResult.SUCCESS;
        });

        // Giường: chưa tối thì báo như vanilla; tối rồi thì nằm xuống, vào cảnh cuối
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!DemDauTienClock.isFloor(world)) return ActionResult.PASS;
            BlockState st = world.getBlockState(hit.getBlockPos());
            if (!(st.getBlock() instanceof BedBlock)) return ActionResult.PASS;
            if (world.isClient) return ActionResult.SUCCESS;
            ServerPlayerEntity sp = (ServerPlayerEntity) player;
            if (finaleTimer >= 0 || failTimer >= 0) return ActionResult.SUCCESS;
            if (hour(world) < DemDauTienClock.SLEEP_FROM) {
                sp.sendMessage(Text.translatable("block.minecraft.bed.no_sleep"), true);
                return ActionResult.SUCCESS;
            }
            BlockPos foot = st.get(BedBlock.PART) == net.minecraft.block.enums.BedPart.HEAD
                    ? hit.getBlockPos().offset(st.get(BedBlock.FACING).getOpposite()) : hit.getBlockPos();
            beginFinale((ServerWorld) world, sp, foot);
            return ActionResult.SUCCESS;
        });

        // Chết ở tầng này (rơi khỏi đảo, đói...): không chết thật, làm lại tầng
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player) || !DemDauTienClock.isFloor(player.getWorld())) return true;
            player.setHealth(1.0f);
            fail(player);
            return false;
        });

        // Vào lại game khi đang ở tầng này: đồng hồ đã mất, làm lại tầng
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (!DemDauTienClock.isFloor(player.getWorld())) return;
            server.execute(() -> restart((ServerWorld) player.getWorld(), player));
        });
    }

    // ==========================================
    // MỖI TICK
    // ==========================================

    private static void tick(ServerWorld world) {
        List<ServerPlayerEntity> players = new ArrayList<>(world.getPlayers());
        if (players.isEmpty()) return;
        if (!active) {
            // Vào chiều này bằng cách khác (lệnh /tp...): bắt đầu tầng cho người đầu tiên
            enter(players.get(0));
            return;
        }
        ServerPlayerEntity player = players.get(0);
        long now = world.getTime();

        eatColumns(world);
        breakBeds(world, now);
        faceSheep(world, player);

        // Bàn chế tạo đang mở: vết rách thở nên tính lại ô kết quả thường xuyên
        if (now % 5 == 0) {
            for (ServerPlayerEntity p : players) {
                if (p.currentScreenHandler instanceof CraftingScreenHandler h) {
                    h.onContentChanged(((com.samsara.mixin.CraftingScreenHandlerAccessor) h).samsara$getInput());
                }
            }
        }

        if (finaleTimer >= 0) {
            tickFinale(world, player);
            return;
        }
        if (failTimer >= 0) {
            if (++failTimer == 30) restart(world, player);
            return;
        }
        if (player.getY() < DemDauTienBuilder.ORIGIN.getY() - 40) {
            fail(player);
            return;
        }
        // Tối hẳn mà chưa ngủ: Khối Chưa Tải ăn nhanh vào tới chân
        if (hour(world) >= DemDauTienClock.NIGHT) {
            if (COLUMNS.isEmpty() && ++nightTimer >= 12) {
                nightTimer = 0;
                queueRing(world, NIGHT_RING);
            }
            int px = MathHelper.floor(player.getX()), pz = MathHelper.floor(player.getZ());
            double d = Math.sqrt((double) px * px + (double) pz * pz);
            if (d >= DemDauTienBuilder.edge(px, pz) - eaten - 1 || eaten > DemDauTienBuilder.RADIUS) fail(player);
        }
    }

    private static void faceSheep(ServerWorld world, ServerPlayerEntity player) {
        for (SheepEntity sheep : world.getEntitiesByClass(SheepEntity.class, islandBox(), e -> true)) {
            double dx = player.getX() - sheep.getX(), dz = player.getZ() - sheep.getZ();
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            sheep.setYaw(yaw);
            sheep.setHeadYaw(yaw);
            sheep.setBodyYaw(yaw);
            double dy = player.getEyeY() - sheep.getEyeY();
            sheep.setPitch((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        }
    }

    private static int nearestSheepSpot(SheepEntity sheep) {
        int best = -1;
        double bestD = 9.0;
        for (int k = 0; k < DemDauTienBuilder.SHEEP.length; k++) {
            int[] s = DemDauTienBuilder.SHEEP[k];
            double dx = sheep.getX() - (s[0] + 0.5), dz = sheep.getZ() - (s[1] + 0.5);
            double d = dx * dx + dz * dz;
            if (d < bestD) {
                bestD = d;
                best = k;
            }
        }
        return best;
    }

    private static Box islandBox() {
        int r = DemDauTienBuilder.RADIUS + 6;
        BlockPos o = DemDauTienBuilder.ORIGIN;
        return new Box(-r, o.getY() - 40, -r, r, o.getY() + 30, r);
    }

    // ==========================================
    // KHỐI CHƯA TẢI ĂN VÀO
    // ==========================================

    /** Thêm một vòng rộng {@code width} khối vào hàng đợi (xử lý dần mỗi tick). */
    private static void queueRing(ServerWorld world, int width) {
        int from = eaten, to = eaten + width;
        eaten = to;
        int r = DemDauTienBuilder.RADIUS + 1;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double d = Math.sqrt((double) x * x + (double) z * z);
                double e = DemDauTienBuilder.edge(x, z);
                if (d <= e && d > e - to && d <= e - from + 0.0001) COLUMNS.add(new int[]{x, z});
            }
        }
    }

    private static void eatColumns(ServerWorld world) {
        BlockState unloaded = SamsaraBlocks.KHOI_CHUA_TAI.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();
        int top0 = DemDauTienBuilder.ORIGIN.getY();
        for (int n = 0; n < COLUMNS_PER_TICK && !COLUMNS.isEmpty(); n++) {
            int[] c = COLUMNS.poll();
            int top = DemDauTienBuilder.height(c[0], c[1]);
            // Mặt đảo và mọi thứ mọc trên đó (cây, hoa, đồ người chơi đặt) thành Khối Chưa Tải
            for (int y = top - 5; y <= top0 + 16; y++) {
                BlockPos p = new BlockPos(c[0], y, c[1]);
                BlockState st = world.getBlockState(p);
                if (st.isAir() || st.isOf(SamsaraBlocks.KHOI_CHUA_TAI)) continue;
                if (st.getBlock() instanceof BedBlock) continue;
                world.setBlockState(p, st.isOpaqueFullCube(world, p) || st.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)
                        ? unloaded : air, 2 | 16);
            }
        }
    }

    // ==========================================
    // GIƯỜNG
    // ==========================================

    /** Bàn chế tạo ở {@code table} có đang nằm trong vết rách không (cho chế giường). */
    public static boolean tableInTear(World world, BlockPos table) {
        return DemDauTienClock.inside(table.getX() + 0.5, table.getY() + 0.5, table.getZ() + 0.5, timeOfDay(world));
    }

    /** Gọi từ BedBlock.onPlaced: đặt ngoài vết rách thì giường hiện một nhịp rồi tự gãy, rơi lại thành vật phẩm. */
    public static void onBedPlaced(World world, BlockPos foot) {
        if (world.isClient || !DemDauTienClock.isFloor(world)) return;
        if (DemDauTienClock.inside(foot.getX() + 0.5, foot.getY() + 0.3, foot.getZ() + 0.5, timeOfDay(world))) return;
        BED_BREAKS.add(new long[]{foot.asLong(), world.getTime() + 4});
    }

    private static void breakBeds(ServerWorld world, long now) {
        for (int i = BED_BREAKS.size() - 1; i >= 0; i--) {
            long[] b = BED_BREAKS.get(i);
            if (now < b[1]) continue;
            BED_BREAKS.remove(i);
            BlockPos foot = BlockPos.fromLong(b[0]);
            BlockState st = world.getBlockState(foot);
            if (!(st.getBlock() instanceof BedBlock)) continue;
            BlockPos head = foot.offset(st.get(BedBlock.FACING));
            ItemStack drop = new ItemStack(st.getBlock().asItem());
            world.setBlockState(head, Blocks.AIR.getDefaultState(), 3);
            world.setBlockState(foot, Blocks.AIR.getDefaultState(), 3);
            world.spawnEntity(new ItemEntity(world, foot.getX() + 0.5, foot.getY() + 0.3, foot.getZ() + 0.5, drop));
        }
    }

    // ==========================================
    // CẢNH CUỐI
    // ==========================================

    private static void beginFinale(ServerWorld world, ServerPlayerEntity player, BlockPos foot) {
        finaleTimer = 0;
        finaleBed = foot;
        COLUMNS.clear();
        player.sleep(foot);
    }

    private static void tickFinale(ServerWorld world, ServerPlayerEntity player) {
        finaleTimer++;
        if (finaleTimer == 50) {
            // Tỉnh dậy giữa đêm: ba vết rách cùng nở ra nuốt cả đảo (shader đọc đồng hồ nhảy tới FINALE)
            player.wakeUp(false, false);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 400, 255, false, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 400, 128, false, false, false));
            setClock(world, DemDauTienClock.FINALE);
        }
        if (finaleTimer == 50 + DemDauTienClock.WHITE_AFTER + 50) leave(world, player);
    }

    // ==========================================
    // LỆNH TEST
    // ==========================================

    /** Nhảy đồng hồ tới sát giờ được ngủ. */
    public static void testNight(ServerPlayerEntity player) {
        ServerWorld world = DemDauTienBuilder.world(player);
        if (world == null || !active) return;
        setClock(world, DemDauTienClock.SLEEP_FROM - 100);
    }

    /**
     * Thử câu đố bằng kịch bản chụp: "ban" đặt bàn chế tạo sát gốc cây anh đào (giữa vết rách lớn nhất) và đưa người
     * chơi tới nhìn vào nó; "cho_giuong" đưa người chơi tới nhìn xuống chỗ đặt giường cạnh đó (dọn cánh hoa ở chỗ giường).
     */
    public static void testSpot(ServerPlayerEntity player, boolean table) {
        ServerWorld world = DemDauTienBuilder.world(player);
        if (world == null) return;
        double[] t = DemDauTienBuilder.TEARS[0];
        BlockPos stand, target;
        Vec3d look;
        if (table) {
            target = DemDauTienBuilder.ground(t, 1, 1);
            world.setBlockState(target, Blocks.CRAFTING_TABLE.getDefaultState());
            stand = DemDauTienBuilder.ground(t, 1, 3);
            look = Vec3d.ofCenter(target);
        } else {
            target = DemDauTienBuilder.ground(t, -1, 2).down();
            for (int dz = 1; dz <= 3; dz++) world.setBlockState(DemDauTienBuilder.ground(t, -1, dz), Blocks.AIR.getDefaultState());
            stand = DemDauTienBuilder.ground(t, -1, 4);
            look = Vec3d.ofCenter(target).add(0, 0.5, 0);
        }
        Vec3d eye = new Vec3d(stand.getX() + 0.5, stand.getY() + player.getStandingEyeHeight(), stand.getZ() + 0.5);
        Vec3d d = look.subtract(eye);
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * 180 / Math.PI) - 90f;
        float pitch = (float) (-MathHelper.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI);
        player.teleport(world, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, yaw, pitch);
    }

    /** Đưa đồ để chế giường ngay (3 len, 3 ván, bàn chế tạo). */
    public static void testItems(ServerPlayerEntity player) {
        player.giveItemStack(new ItemStack(Items.WHITE_WOOL, 3));
        player.giveItemStack(new ItemStack(Items.OAK_PLANKS, 8));
        player.giveItemStack(new ItemStack(Items.CRAFTING_TABLE));
    }
}
