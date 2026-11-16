package com.samsara.system.overworld;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.samsara.registry.SamsaraItems;
import com.samsara.system.celestial.SolarFractureManager;
import com.samsara.system.cycle.CamcorderGiftSystem;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.cycle.PhantomTrapSystem;
import com.samsara.worldgen.SpawnCairnGenerator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;

import java.util.List;

/**
 * Lệnh chuẩn bị quay: {@code /samsara quay <video1|video2|video3|video4|tat>}. Chạy trước khi bật OBS: đặt thế giới
 * đúng trạng thái kịch bản của video đó, bật chế độ quay ({@link FilmingState}: hiện tượng ngẫu nhiên chỉ xảy ra khi
 * bấm phím đạo diễn), dọn túi đồ, đặt người chơi vào chỗ bắt đầu, rồi xoá khung chat để không lộ lệnh trên hình.
 * <ul>
 *   <li>video1 / video2 / video3: góc nhìn người em, Stage 4 (7 lần ngủ), máy quay đã nằm sẵn ở ô số 3.
 *       video1 dựng lại Bia Mộ với rương còn nguyên nhật ký, bắt đầu ở spawn nhìn về căn nhà; video2 cầm sẵn nhật ký,
 *       đứng trong nhà; video3 cầm nhật ký, bắt đầu ở spawn.</li>
 *   <li>video4: bản ghi cũ của Nam, Stage 2 (3 lần ngủ), chưa có máy quay, chưa thức đêm nào; máy quay tự được cài
 *       sau đêm thức trắng thứ ba (hoặc bấm phím đạo diễn "cài máy quay"). Bắt đầu trước cửa nhà, chiều muộn.</li>
 *   <li>tat: tắt chế độ quay (hiện tượng ngẫu nhiên chạy lại như thường).</li>
 * </ul>
 * Tên nhân vật (Nam_1998 hay tên người em) đặt lúc chạy game: {@code start_samsara.bat <tên>} (xem HANDOFF).
 */
public final class FilmingSetup {
    private FilmingSetup() {}

    /** Gói S2C: client xoá khung chat. */
    public static final Identifier CLEAR_CHAT = new Identifier("samsara", "clear_chat");
    private static final List<String> VIDEOS = List.of("video1", "video2", "video3", "video4", "tat");

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
                CommandManager.literal("samsara").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.literal("quay")
                                .then(CommandManager.argument("video", StringArgumentType.word())
                                        .suggests((ctx, b) -> CommandSource.suggestMatching(VIDEOS, b))
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            return p != null && setup(p, StringArgumentType.getString(ctx, "video")) ? 1 : 0;
                                        })))));
    }

    public static boolean setup(ServerPlayerEntity player, String video) {
        MinecraftServer server = player.getServer();
        ServerWorld world = server.getOverworld();
        if ("tat".equals(video)) {
            FilmingState.set(server, false, false);
            clearChat(player);
            return true;
        }
        boolean nam = "video4".equals(video);
        if (!VIDEOS.contains(video)) return false;

        FilmingState.set(server, true, nam);
        SolarFractureManager.setSleeps(nam ? 3 : 7, world);
        world.setTimeOfDay(switch (video) {
            case "video1", "video2" -> 1000;
            case "video3" -> 4000;
            default -> 9000;
        });
        world.setWeather(12000, 0, false, false);

        // Người chơi: sinh tồn, đầy máu / đói, không hiệu ứng, túi đồ trống
        player.changeGameMode(GameMode.SURVIVAL);
        player.clearStatusEffects();
        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(5.0f);
        player.extinguish();
        player.getInventory().clear();
        player.currentScreenHandler.sendContentUpdates();

        EggCycleState state = EggCycleState.get(server);
        if (nam) {
            // Nam chưa có máy quay, chưa thức đêm nào: máy quay tới sau đêm thức trắng thứ ba
            state.clearCamcorderGiven(player.getUuid());
            PhantomTrapSystem.setNightsAwake(player, 0);
        } else {
            state.markCamcorderGiven(player.getUuid());
            CamcorderGiftSystem.give(player);
            if (!"video1".equals(video)) player.getInventory().setStack(0, new ItemStack(SamsaraItems.PALIMPSEST_JOURNAL));
        }
        player.getInventory().selectedSlot = 0;

        // Video 1: Bia Mộ và rương dựng lại nguyên vẹn (nhật ký + đóa hoa khô)
        if ("video1".equals(video)) {
            BlockPos cairn = SpawnCairnGenerator.getCairnPos(world);
            if (cairn != null) SpawnCairnGenerator.buildCairn(world, cairn, state.getCycle() <= 1);
        }

        // Chỗ bắt đầu
        BlockPos at;
        float yaw;
        if ("video2".equals(video)) {
            at = NamBase.houseSpot(world, true);
            yaw = 180f;
        } else if (nam) {
            at = NamBase.houseSpot(world, false);
            yaw = 0f;
        } else {
            at = null;
            yaw = 0f;
        }
        if (at == null) {
            BlockPos spawn = world.getSpawnPos();
            at = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawn);
            yaw = 0f;   // nhìn về phía nam: căn nhà của Nam
        }
        player.teleport(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 5f);
        clearChat(player);
        return true;
    }

    private static void clearChat(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, CLEAR_CHAT, PacketByteBufs.empty());
    }
}
