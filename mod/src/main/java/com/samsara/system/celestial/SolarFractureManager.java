package com.samsara.system.celestial;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import com.samsara.system.cycle.CamcorderGiftSystem;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.cycle.PhantomTrapSystem;
import com.samsara.system.cycle.WombTransitionSystem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Quản lý Hệ thống Mặt Trời Biến Chuyển Hố Đen (Solar Singularity Progression).
 * Theo Master Production Document (scripts/master_production_document_project_samsara_revision.md):
 * mỗi lần ngủ qua đêm "đập nứt thêm một tầng vỏ trứng" (sleep_count += 1), và giấc ngủ thứ 7
 * làm Hố Đen nuốt chửng vòm trời.
 *
 *   sleep_count 0     -> Giai đoạn 0: Mặt Trời Vanilla bình thường (thế giới mới).
 *   sleep_count 1-2   -> Giai đoạn 1: Mặt Trời Rạn Nứt, lộ lõi đen (lần ngủ đầu tiên).
 *   sleep_count 3-4   -> Giai đoạn 2: Hố đen trực diện (Face-on), vành quang tử vàng kim.
 *   sleep_count 5-6   -> Giai đoạn 3: Bão Plasma xoáy đỏ cam.
 *   sleep_count 7+    -> Giai đoạn 4: Siêu Hố Đen Gargantua.
 *
 * sleep_count là của cả THẾ GIỚI và chỉ tăng tối đa 1 lần mỗi đêm (nhiều người chơi cùng ngủ không bị cộng dồn).
 * Trạng thái được lưu trong {@link EggCycleState}; class này lo phần đồng bộ & sự kiện thức dậy.
 * Hỗ trợ lệnh kiểm thử: /samsara test sun_stage <0-4>.
 */
public class SolarFractureManager {
    public static final Identifier SUN_STAGE_1 = new Identifier("samsara", "textures/environment/sun_stage1.png");
    public static final Identifier SUN_STAGE_2 = new Identifier("samsara", "textures/environment/sun_stage2.png");
    public static final Identifier SUN_STAGE_3 = new Identifier("samsara", "textures/environment/sun_stage3.png");
    public static final Identifier SUN_STAGE_4 = new Identifier("samsara", "textures/environment/sun_stage4.png");

    /** Gói tin S2C mang giai đoạn Mặt Trời hiện tại (VarInt 0-4). */
    public static final Identifier SUN_STAGE_PACKET = new Identifier("samsara", "sun_stage");

    public static final int MIN_STAGE = EggCycleState.MIN_STAGE;
    public static final int MAX_STAGE = EggCycleState.MAX_STAGE;

    // Trạng thái phía CLIENT (chỉ được cập nhật qua gói tin SUN_STAGE_PACKET).
    // Tách riêng khỏi EggCycleState (phía server) để máy chủ riêng (dedicated) cũng hiển thị đúng,
    // và để client không ghi đè dữ liệu server trong chế độ chơi đơn.
    public static volatile int clientStage = 0;

    public static void register() {
        // Nạp sẵn trạng thái chu kỳ của thế giới đang mở (và chuyển dữ liệu cũ nếu có)
        ServerLifecycleEvents.SERVER_STARTED.register(EggCycleState::get);

        // Gửi giai đoạn hiện tại cho người chơi vừa vào server
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendStage(handler.getPlayer()));

        // Lắng nghe sự kiện người chơi thức dậy sau giấc ngủ
        EntitySleepEvents.STOP_SLEEPING.register((livingEntity, sleepingPos) -> {
            if (livingEntity instanceof ServerPlayerEntity player) {
                onPlayerWakeUp(player, sleepingPos);
            }
        });
    }

    private static void sendStage(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(EggCycleState.get(player.getServer()).getStage());
        ServerPlayNetworking.send(player, SUN_STAGE_PACKET, buf);
    }

    public static void broadcastStage(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendStage(player);
        }
    }

    /**
     * Texture dùng để vẽ Mặt Trời phía client.
     * Trả về null ở giai đoạn 0 -> giữ nguyên Mặt Trời Vanilla.
     */
    @Nullable
    /** Gói S2C (lệnh test screenshot_sun): client chụp màn hình như bấm F2. */
    public static final Identifier SCREENSHOT_SUN_TEST = new Identifier("samsara", "screenshot_sun");

    public static Identifier getSunTexture() {
        switch (clientStage) {
            case 1: return SUN_STAGE_1;
            case 2: return SUN_STAGE_2;
            case 3: return SUN_STAGE_3;
            case 4: return SUN_STAGE_4;
            default: return null;
        }
    }

    /** Lệnh test: đặt thẳng giai đoạn. */
    public static void setStage(int stage, ServerWorld world) {
        if (world == null) return;
        EggCycleState.get(world.getServer()).setStage(stage);
        broadcastStage(world.getServer());
    }

    /** Lệnh test: đặt thẳng số lần ngủ (giai đoạn đi theo). */
    public static void setSleeps(int sleeps, ServerWorld world) {
        if (world == null) return;
        EggCycleState.get(world.getServer()).setSleeps(sleeps);
        broadcastStage(world.getServer());
    }

    public static void onPlayerWakeUp(ServerPlayerEntity player, BlockPos bedPos) {
        ServerWorld world = player.getServerWorld();
        // Chỉ tính khi đã ngủ qua đêm thành công: ServerWorld đặt giờ về đúng sáng hôm sau (timeOfDay % 24000 == 0)
        // rồi đánh thức người chơi trong CÙNG tick. KHÔNG dùng world.isDay(): ambientDarkness chỉ được tính lại
        // sau khi người chơi đã được đánh thức, nên lúc này nó vẫn là giá trị ban đêm (giấc ngủ không bao giờ được đếm).
        if (world.getTimeOfDay() % 24000L >= 100L) return;
        MinecraftServer server = world.getServer();
        EggCycleState state = EggCycleState.get(server);

        // Ngủ là thoát khỏi bầy Phantom ngay lập tức (cái giá: vỏ trứng nứt thêm một tầng)
        PhantomTrapSystem.banishPhantoms(player);

        // Mỗi đêm chỉ tính 1 lần cho cả thế giới
        long day = world.getTimeOfDay() / 24000L;
        if (day != state.getLastCountedDay()) {
            int oldStage = state.getStage();
            state.countSleep(day);
            int newStage = state.getStage();
            if (newStage != oldStage) {
                broadcastStage(server);
            }

            // Hiệu ứng âm thanh huyền bí khi thức giấc
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.AMBIENT, 1.2f, 0.8f);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.AMBIENT, 0.8f, 0.6f);

            Text message = newStage > oldStage
                    ? Text.translatable("samsara.solar_fracture_manager.stage_up", newStage)
                    : Text.translatable("samsara.solar_fracture_manager.sleep", state.getSleeps());
            boolean overlay = newStage <= oldStage;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                p.sendMessage(message, overlay);
            }
            if (newStage > oldStage) {
                String[] titles = {"", "samsara.solar_fracture_manager.vet_nut_dau_tien", "samsara.solar_fracture_manager.con_mat_hu_khong", "samsara.solar_fracture_manager.trong_luc_be_cong", "samsara.solar_fracture_manager.giac_ngu_thu_bay"};
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    com.samsara.system.ui.UiHorrorSystem.advancement(p, "stage_" + newStage, titles[newStage],
                            net.minecraft.item.Items.RED_BED);
                }
            }
        }

        // Từ Giai đoạn 2, <System> cài máy quay vào Hotbar nếu người chơi chưa có
        if (state.getStage() >= 2) {
            CamcorderGiftSystem.tryGift(player, "samsara.solar_fracture_manager.system_cau_da_ngu");
        }

        // Giấc ngủ thứ 7: Hố Đen nuốt trời, chiếc giường trôi vào The Womb
        if (state.getStage() >= EggCycleState.MAX_STAGE && !state.isAwakened(player.getUuid())
                && world.getRegistryKey() == World.OVERWORLD) {
            state.markAwakened(player.getUuid());
            // Hiệu ứng không gian kéo dãn vài giây rồi mới dịch chuyển (WombTransitionSystem)
            WombTransitionSystem.begin(player, bedPos);
        }
    }
}
