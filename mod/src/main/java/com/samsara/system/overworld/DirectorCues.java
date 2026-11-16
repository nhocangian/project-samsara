package com.samsara.system.overworld;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.samsara.system.additions.PhantomAudioEchoSystem;
import com.samsara.system.cycle.HostileHesitationSystem;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Predicate;

/**
 * Phím đạo diễn: gọi các hiện tượng Overworld đúng lúc đang quay video mà không để lại dấu vết trên khung hình (không
 * dòng chat, không gõ lệnh). Phía client ({@code client/overworld/DirectorKeys}) có một phím cho mỗi cảnh (mặc định
 * phím số bên phải bàn phím, đổi được trong Điều khiển), bấm thì gửi {@link #PACKET}. Cũng gọi được bằng lệnh
 * {@code /samsara cue <tên>} (không in gì; dùng từ khối lệnh hoặc bảng điều khiển). Chỉ người có quyền lệnh (op) dùng
 * được. Ngoài chế độ quay ({@link FilmingState}) các hiện tượng vẫn tự xảy ra ngẫu nhiên; trong chế độ quay chỉ
 * xảy ra khi bấm phím.
 */
public final class DirectorCues {
    private DirectorCues() {}

    public static final Identifier PACKET = new Identifier("samsara", "director_cue");

    /** Tên cảnh, đúng thứ tự phím (numpad 1..9, rồi 0 và dấu chấm). */
    public static final List<String> CUES = List.of("echo_steps", "hesitate", "sunflower_turn", "animal_stare",
            "version_glitch", "self_sign", "pick_echo", "torch_ahead", "branch_tunnel", "nam_join", "camcorder_gift");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(PACKET, (server, player, handler, buf, sender) -> {
            String cue = buf.readString(64);
            server.execute(() -> run(player, cue));
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
                CommandManager.literal("samsara").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.literal("cue")
                                .then(CommandManager.argument("ten", StringArgumentType.word())
                                        .suggests((ctx, b) -> CommandSource.suggestMatching(CUES, b))
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            return p != null && run(p, StringArgumentType.getString(ctx, "ten")) ? 1 : 0;
                                        })))));
    }

    /** Gọi một cảnh; trả về false nếu lúc này không làm được (ví dụ không đứng dưới hang). Không in gì. */
    public static boolean run(ServerPlayerEntity player, String cue) {
        if (!player.hasPermissionLevel(2)) return false;
        Predicate<ServerPlayerEntity> action = switch (cue) {
            case "echo_steps" -> p -> { PhantomAudioEchoSystem.testNow(p); return true; };
            case "hesitate" -> p -> HostileHesitationSystem.testNow(p) > 0;
            case "sunflower_turn" -> p -> OverworldSunflowerGaze.cue(p) > 0;
            case "animal_stare" -> p -> AnimalStare.testNow(p) > 0;
            case "version_glitch" -> p -> VersionGlitch.trigger(p.getServerWorld(), p);
            case "self_sign" -> SelfSign::cue;
            case "pick_echo" -> CavePickEcho::cue;
            case "torch_ahead" -> CaveTorchAhead::cue;
            case "branch_tunnel" -> CaveBranchTunnel::cue;
            // Video 3: "Nam_1998 joined the game", từ Stage 3 gõ "em đang ở đâu", rồi rời đi sau ~30 giây
            case "nam_join" -> p -> { com.samsara.system.ui.UiHorrorSystem.ghostJoin(p.getServer(), 600, false); return true; };
            // Video 4 cảnh 4: <System> cài máy quay vào ô số 3 ("Cậu không chịu ngủ. Vậy thì hãy nhìn đi.")
            case "camcorder_gift" -> p -> {
                com.samsara.system.cycle.EggCycleState.get(p.getServer()).clearCamcorderGiven(p.getUuid());
                return com.samsara.system.cycle.CamcorderGiftSystem.tryGift(p, "samsara.phantom_trap_system.system_cau_khong_chiu");
            };
            default -> null;
        };
        return action != null && action.test(player);
    }
}
