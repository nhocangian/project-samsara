package com.samsara.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.samsara.entity.DimensionalRiftEntity;
import com.samsara.registry.SamsaraItems;
import com.samsara.system.arg.PalimpsestJournalItem;
import com.samsara.system.womb.WombTravel;
import com.samsara.system.arg.WombPhantomSkiffManager;
import com.samsara.system.cycle.CamcorderGiftSystem;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.cycle.PhantomTrapSystem;
import com.samsara.system.cycle.StageAnomalySystem;
import com.samsara.system.cycle.WombTransitionSystem;
import com.samsara.system.cycle.CollapseAnomalySystem;
import com.samsara.system.cycle.NamGhostSystem;
import com.samsara.system.memory.NamMemoryManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.util.List;

/**
 * Hệ thống Lệnh Kiểm Thử (Debug & Test Commands) cho Project: Samsara.
 * QUY TẮC CỐT LÕI THEO YÊU CẦU CỦA USER:
 *  - Mỗi khi thêm 1 tính năng mới vào mod -> LUÔN tạo kèm 1 lệnh test tương ứng dưới /samsara test <tính_năng>.
 *  - Nếu tính năng đó bị loại bỏ (trực tiếp hoặc gián tiếp) -> XÓA NGAY lệnh test tương ứng.
 */
public class SamsaraTestCommands {

    /** Server -> client: đặt pin máy quay 50% để bấm R xem nắp băng mở (/samsara test camcorder_model). */
    public static final net.minecraft.util.Identifier CAMCORDER_MODEL_TEST = new net.minecraft.util.Identifier("samsara", "camcorder_model_test");

    public static void register() {
        CommandRegistrationCallback.EVENT.register(SamsaraTestCommands::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                         CommandRegistryAccess registryAccess,
                                         CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("samsara")
                // 1. Phím tắt dịch chuyển giữa các chiều không gian
                .then(CommandManager.literal("womb")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player != null) {
                                WombTravel.teleportToTheWomb(player);
                            }
                            return 1;
                        }))
                .then(CommandManager.literal("nam")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player != null) {
                                NamMemoryManager.arrive(player, false);
                            }
                            return 1;
                        }))
                .then(CommandManager.literal("overworld")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player != null) {
                                WombTravel.teleportToOverworld(player);
                            }
                            return 1;
                        }))

                // 2. Hệ thống kiểm thử tính năng (/samsara test ...)
                .then(CommandManager.literal("test")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ctx -> showTestHelp(ctx.getSource()))
                        .then(CommandManager.literal("help")
                                .executes(ctx -> showTestHelp(ctx.getSource())))

                        // Test: Con Thuyền Tiền Kiếp (The Phantom Skiff)
                        .then(CommandManager.literal("skiff")
                                .executes(ctx -> testSkiff(ctx.getSource())))

                        // Test: Cổng Vết Rách sang The Womb (Womb Rift Portal)
                        .then(CommandManager.literal("womb_rift")
                                .executes(ctx -> testWombRift(ctx.getSource())))

                        // Test: Đổi kiểu lòng cổng The Womb (1: Silexars, 2: Glowmarcher, 3: Kishimisu)
                        .then(CommandManager.literal("womb_style")
                                .then(CommandManager.argument("style", IntegerArgumentType.integer(1, 3))
                                        .executes(ctx -> testWombStyle(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "style")))))

                        // Test: Đặt giai đoạn Mặt Trời Biến Chuyển Hố Đen (/samsara test sun_stage <0-4>)
                        .then(CommandManager.literal("sun_stage")
                                .then(CommandManager.argument("stage", IntegerArgumentType.integer(0, 4))
                                        .executes(ctx -> testSunStage(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "stage")))))

                        // Test: Chu kỳ Vỏ Trứng - xem trạng thái (/samsara test cycle)
                        .then(CommandManager.literal("cycle")
                                .executes(ctx -> testCycleStatus(ctx.getSource())))

                        // Test: Đặt số lần ngủ, giai đoạn đi theo (/samsara test sleep_count <n>)
                        .then(CommandManager.literal("sleep_count")
                                .then(CommandManager.argument("sleeps", IntegerArgumentType.integer(0, 64))
                                        .executes(ctx -> testSleepCount(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "sleeps")))))

                        // Test: Bẫy Phantom - đặt số đêm thức và thả một đợt (/samsara test phantom_wave [đêm])
                        .then(CommandManager.literal("phantom_wave")
                                .executes(ctx -> testPhantomWave(ctx.getSource(), 3))
                                .then(CommandManager.argument("nights", IntegerArgumentType.integer(0, 10))
                                        .executes(ctx -> testPhantomWave(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "nights")))))

                        // Test: <System> cài máy quay vào ô số 3 (/samsara test camcorder_gift)
                        .then(CommandManager.literal("camcorder_gift")
                                .executes(ctx -> testCamcorderGift(ctx.getSource())))

                        // Test: Bị hút vào The Womb kèm hiệu ứng không gian kéo dãn (/samsara test womb_transition)
                        .then(CommandManager.literal("womb_transition")
                                .executes(ctx -> testWombTransition(ctx.getSource())))

                        // Test: Dựng lại nhà Nam ở chiều Ký Ức Của Nam (/samsara test nam_house)
                        .then(CommandManager.literal("nam_house")
                                .executes(ctx -> testNamHouse(ctx.getSource())))

                        // Test: Trồng lại hoa hướng dương quay đúng hướng (/samsara test nam_field [bán kính chunk])
                        // Test: Xem trước bóng phản chiếu / <System> (/samsara test mirror <cỡ|off>)
                        .then(CommandManager.literal("mirror")
                                .executes(ctx -> testMirror(ctx.getSource(), 1.0f))
                                .then(CommandManager.literal("off").executes(ctx -> testMirror(ctx.getSource(), 0.0f)))
                                .then(CommandManager.argument("scale", IntegerArgumentType.integer(1, 45))
                                        .executes(ctx -> testMirror(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "scale")))))

                        // Test: Hoa hướng dương quay ngay về phía bạn (/samsara test sunflower_gaze)
                        .then(CommandManager.literal("sunflower_gaze")
                                .executes(ctx -> testSunflowerGaze(ctx.getSource())))
                        .then(CommandManager.literal("nam_field")
                                .executes(ctx -> testNamField(ctx.getSource(), 8))
                                .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 16))
                                        .executes(ctx -> testNamField(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))

                        // Test: Tuyến hành hương trong The Womb (/samsara test womb_route [here|rebuild|tp <chặng>])
                        .then(CommandManager.literal("womb_route")
                                .executes(ctx -> testWombRoute(ctx.getSource()))
                                .then(CommandManager.literal("here")
                                        .executes(ctx -> testWombRouteHere(ctx.getSource())))
                                .then(CommandManager.literal("rebuild")
                                        .executes(ctx -> testWombRouteRebuild(ctx.getSource())))
                                .then(CommandManager.literal("titans")
                                        .executes(ctx -> testWombTitans(ctx.getSource())))
                                .then(CommandManager.literal("doors")
                                        .executes(ctx -> testWombDoors(ctx.getSource())))
                                .then(CommandManager.literal("spire")
                                        .executes(ctx -> testWombSpire(ctx.getSource())))
                                .then(CommandManager.literal("final_door")
                                        .executes(ctx -> testWombFinalDoor(ctx.getSource())))
                                .then(CommandManager.literal("dock")
                                        .executes(ctx -> testWombDock(ctx.getSource())))
                                .then(CommandManager.literal("tp")
                                        .then(CommandManager.argument("stop", IntegerArgumentType.integer(0, 4))
                                                .executes(ctx -> testWombRouteTp(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "stop"))))))

                        // Test: Ảnh chụp F2 thấy mặt trời thật (/samsara test screenshot_sun)
                        .then(CommandManager.literal("screenshot_sun").executes(ctx -> testScreenshotSun(ctx.getSource())))

                        // Test: Khối Mã (mã nhị phân rơi, các khối cạnh nhau nối liền) (/samsara test code_block)
                        .then(CommandManager.literal("code_block").executes(ctx -> testCodeBlock(ctx.getSource())))

                        // Test: Kinh dị tầng giao diện (/samsara test ui <join|leave|say|archive|advancement|rename|reset>)
                        .then(CommandManager.literal("ui")
                                .then(CommandManager.literal("join").executes(ctx -> testUi(ctx.getSource(), "join")))
                                .then(CommandManager.literal("leave").executes(ctx -> testUi(ctx.getSource(), "leave")))
                                .then(CommandManager.literal("say").executes(ctx -> testUi(ctx.getSource(), "say")))
                                .then(CommandManager.literal("archive").executes(ctx -> testUi(ctx.getSource(), "archive")))
                                .then(CommandManager.literal("advancement").executes(ctx -> testUi(ctx.getSource(), "advancement")))
                                .then(CommandManager.literal("rename").executes(ctx -> testUi(ctx.getSource(), "rename")))
                                .then(CommandManager.literal("reset").executes(ctx -> testUi(ctx.getSource(), "reset")))
                                .then(CommandManager.literal("ending_menu_on").executes(ctx -> testUi(ctx.getSource(), "ending_menu_on")))
                                .then(CommandManager.literal("ending_menu_off").executes(ctx -> testUi(ctx.getSource(), "ending_menu_off"))))

                        // Test: Máy quay 3D khi cầm: LCD lật, đèn REC, nắp băng khi thay pin (/samsara test camcorder_model)
                        .then(CommandManager.literal("camcorder_model")
                                .executes(ctx -> testCamcorderModel(ctx.getSource())))

                        // Test: Phantom đứng yên trước mặt để ngắm "Tàn Hồn Phantom" qua máy quay (/samsara test soul_phantom)
                        .then(CommandManager.literal("soul_phantom")
                                .executes(ctx -> testSoulPhantom(ctx.getSource())))

                        // Test: Dựng một The Wanderer đứng yên trước mặt để ngắm model (/samsara test wanderer)
                        .then(CommandManager.literal("wanderer")
                                .executes(ctx -> testWandererPose(ctx.getSource())))

                        // Test: The Wanderer bám đuôi trên tuyến (/samsara test wanderer_chase [start|stop])
                        .then(CommandManager.literal("wanderer_chase")
                                .executes(ctx -> testWandererChase(ctx.getSource(), true))
                                .then(CommandManager.literal("start")
                                        .executes(ctx -> testWandererChase(ctx.getSource(), true)))
                                .then(CommandManager.literal("stop")
                                        .executes(ctx -> testWandererChase(ctx.getSource(), false))))

                        // Test: Tiêu đề chương của tuyến hành hương (/samsara test chapter_title <1-4>)
                        .then(CommandManager.literal("chapter_title")
                                .then(CommandManager.argument("chapter", IntegerArgumentType.integer(1, 4))
                                        .executes(ctx -> testChapterTitle(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "chapter")))))

                        // Test: Dựng bệ + Quả Trứng Nguyên Thủy trước mặt (/samsara test egg)
                        .then(CommandManager.literal("egg")
                                .executes(ctx -> testEgg(ctx.getSource())))

                        // Test: Chạy ngay đoạn kết (cinematic + đối thoại <System> + NO SIGNAL) (/samsara test ending)
                        .then(CommandManager.literal("ending")
                                .executes(ctx -> testEnding(ctx.getSource())))

                        // Test: World "Chu Kỳ #N" (/samsara test new_cycle <house|intro|cairn>)
                        .then(CommandManager.literal("new_cycle")
                                .then(CommandManager.literal("house").executes(ctx -> testNewCycle(ctx.getSource(), "house")))
                                .then(CommandManager.literal("intro").executes(ctx -> testNewCycle(ctx.getSource(), "intro")))
                                .then(CommandManager.literal("cairn").executes(ctx -> testNewCycle(ctx.getSource(), "cairn"))))

                        // Test: Vòng bóng ma Nam_1998 quanh người chơi (/samsara test nam_ghosts)
                        .then(CommandManager.literal("nam_ghosts")
                                .executes(ctx -> testNamGhosts(ctx.getSource())))

                        // Test: Dị tượng theo giai đoạn (/samsara test anomaly <sheep|music|lift|reverse>)
                        .then(CommandManager.literal("anomaly")
                                .then(CommandManager.literal("lift")
                                        .executes(ctx -> testAnomalyLift(ctx.getSource(), 4))
                                        .then(CommandManager.argument("stage", IntegerArgumentType.integer(3, 4))
                                                .executes(ctx -> testAnomalyLift(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "stage")))))
                                .then(CommandManager.literal("sheep")
                                        .executes(ctx -> testAnomalySheep(ctx.getSource())))
                                .then(CommandManager.literal("music")
                                        .executes(ctx -> testAnomalyMusic(ctx.getSource())))
                                .then(CommandManager.literal("reverse")
                                        .executes(ctx -> testAnomalyReverse(ctx.getSource(), "familiar"))
                                        .then(CommandManager.literal("phantom")
                                                .executes(ctx -> testAnomalyReverse(ctx.getSource(), "phantom")))
                                        .then(CommandManager.literal("cave")
                                                .executes(ctx -> testAnomalyReverse(ctx.getSource(), "cave")))))

                        // Test: Cấp Túi Đồ Nghề Điều Tra ARG (Kit: Camcorder, Pin, Sách #0, La Bàn, Thuốc Nhìn Đêm)
                        .then(CommandManager.literal("kit")
                                .executes(ctx -> testInvestigatorKit(ctx.getSource())))

                        // Test: Sinh Trụ Cự Thạch Cổ thử nghiệm cơ chế Quét Quang Học (Megalith Optical Scan)
                        .then(CommandManager.literal("megalith")
                                .executes(ctx -> testMegalithScan(ctx.getSource())))

                        // Test: Dọn dẹp sạch sẽ toàn bộ Cổng & Nhãn Chữ dư thừa trong phạm vi
                        .then(CommandManager.literal("cleanup")
                                .executes(ctx -> testCleanup(ctx.getSource())))
                )
        );
    }

    private static int showTestHelp(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§6================ §e[PROJECT: SAMSARA TEST SUITE] §6================"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test skiff     §7- Triệu hồi Con Thuyền Tiền Kiếp & Rương trôi dạt"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test womb_rift §7- Mở Cổng Vết Rách về The Womb (Bình Nguyên)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test womb_style <1|2|3> §7- Đổi kiểu lòng cổng (1: Silexars, 2: Tunnel 3D, 3: Neon)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test sun_stage <0-4> §7- Đặt giai đoạn Mặt Trời (0: Vanilla, 1-4: Hố Đen Gargantua)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test cycle    §7- Xem trạng thái chu kỳ Vỏ Trứng (số lần ngủ, giai đoạn, đêm thức)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test sleep_count <n> §7- Đặt số lần ngủ (giai đoạn đi theo; 7+ = vào The Womb ở lần ngủ tới)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test phantom_wave [đêm] §7- Đặt số đêm thức & thả một đợt Phantom"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test camcorder_gift §7- <System> cài máy quay vào ô số 3"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test anomaly <sheep|music> §7- Cừu nhìn về Bia Mộ / Nhạc nền bị giật"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test anomaly reverse [phantom|cave] §7- Âm tua ngược thật: âm quen sau lưng (Stage 3) / tiếng Phantom / tiếng hang"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test anomaly lift [3|4] §7- Block bị hút lên trời (3: cát/sỏi, 4: mọi block)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test womb_transition §7- Bị hút đi sau giấc ngủ thứ 7 (tới Ký Ức Của Nam)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test nam_house §7- Dựng lại nhà Nam ở cánh đồng hướng dương"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test nam_field [r] §7- Trồng lại hoa hướng dương quay về đường mòn (bán kính r chunk)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test code_block §7- Tường + sàn Khối Mã trước mặt"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test womb_route [here|rebuild|dock|doors|titans|spire|final_door|tp <0-4>] §7- Tuyến hành hương The Womb: xem / dựng tại chỗ / dựng lại / chỉ bến đò / chỉ biển cửa (và tới đó) / chỉ khổng thần / chỉ tháp / trước cánh cửa cuối / tới chặng"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test screenshot_sun §7- Chụp màn hình như F2: ảnh thấy mặt trời hố đen (Stage 1+)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test ui <join|leave|say|archive|advancement|rename|reset|ending_menu_on|ending_menu_off> §7- Kinh dị tầng giao diện (archive = 3 dòng Video 3 của Chu Kỳ ≥2) (F3 lạ & tên cửa sổ & nút Rời giường từ Stage 1, menu nhiễu ở Stage 4)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test camcorder_model §7- Máy quay 3D: LCD lật, đèn REC, nắp băng khi thay pin (pin về 50%)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test soul_phantom §7- Phantom đứng yên: qua máy quay là \"Tàn Hồn Phantom\""), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test wanderer §7- Dựng The Wanderer đứng yên trước mặt để ngắm model (qua máy quay)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test wanderer_chase [start|stop] §7- The Wanderer bám đuôi trên tuyến (chỉ thấy qua máy quay)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test chapter_title <1-4> §7- Hiện tiêu đề chương của tuyến hành hương"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test egg     §7- Dựng bệ + Quả Trứng Nguyên Thủy trước mặt"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test ending  §7- Chạy đoạn kết: cinematic, đối thoại <System>, NO SIGNAL (sẽ lưu, thoát và TẠO WORLD MỚI \"Chu Kỳ #N\"!)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test mirror <1-45|off> §7- Xem trước bóng phản chiếu (<System>) trước mặt, đổi cỡ để thấy cú nhảy"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test sunflower_gaze §7- Hoa hướng dương quanh bạn quay ngay về phía bạn (trong Ký Ức Của Nam)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test new_cycle <house|intro|cairn> §7- Nhà Nam của world chu kỳ mới (dựng 12 block phía bắc bạn) / tiêu đề lần đầu vào / Bia Mộ chu kỳ mới (3 block phía bắc)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test nam_ghosts §7- Vòng bóng ma Nam_1998 (chỉ thấy qua máy quay)"), false);
        source.sendFeedback(() -> Text.literal("§d/samsara nam            §7- Dịch chuyển tới Ký Ức Của Nam (cánh đồng hướng dương)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test cleanup  §7- Dọn sạch toàn bộ cổng và nhãn chữ dư thừa xung quanh"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test kit       §7- Nhận bộ đồ điều tra (Máy quay, Pin, Sách #0, La bàn)"), false);
        source.sendFeedback(() -> Text.literal("§b/samsara test megalith  §7- Dựng Bia Đá Cổ thử nghiệm Quét Quang Học (Camcorder)"), false);
        source.sendFeedback(() -> Text.literal("§d/samsara womb           §7- Dịch chuyển tức thời vào Biển Gương The Womb"), false);
        source.sendFeedback(() -> Text.literal("§d/samsara overworld      §7- Dịch chuyển tức thời về Overworld"), false);
        source.sendFeedback(() -> Text.literal("§6================================================================"), false);
        return 1;
    }

    private static int testSkiff(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ServerWorld world = player.getServerWorld();

        WombPhantomSkiffManager.spawnTestSkiff(world, player);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã triệu hồi Con Thuyền Tiền Kiếp phía trước bạn. Hãy tiến lại gần trong bán kính 18 block để xem thuyền tan biến và để lại rương di vật!"), true);
        return 1;
    }

    private static int testInvestigatorKit(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ServerWorld world = player.getServerWorld();

        // 1. Máy quay (Phiến Đá Vỡ Nứt / Fractured Slate - Outlast Camcorder)
        player.getInventory().insertStack(new ItemStack(SamsaraItems.FRACTURED_SLATE));
        // 2. 4x Pin máy quay
        player.getInventory().insertStack(new ItemStack(SamsaraItems.CAMCORDER_BATTERY, 4));
        // 3. Sách Nhật Ký Chu Kỳ #0 (Palimpsest #0)
        player.getInventory().insertStack(PalimpsestJournalItem.createDecodedBook());
        // 4. La Bàn Tiền Kiếp chỉ về Quả Trứng
        player.getInventory().insertStack(WombPhantomSkiffManager.createEchoCompass(world, player));

        // 5. Thuốc nhìn trong đêm & áp dụng hiệu ứng Night Vision
        ItemStack potion = new ItemStack(Items.POTION);
        PotionUtil.setPotion(potion, Potions.LONG_NIGHT_VISION);
        player.getInventory().insertStack(potion);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 6000, 0, false, false, true));

        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã cấp toàn bộ túi đồ nghề điều tra ARG (Máy quay, 4x Pin, Sách Palimpsest #0, La bàn tiền kiếp, Thuốc & hiệu ứng Nhìn trong đêm)!"), false);
        return 1;
    }

    private static int testMegalithScan(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ServerWorld world = player.getServerWorld();

        BlockPos center = player.getBlockPos().offset(player.getHorizontalFacing(), 5);
        int baseY = Math.max(62, player.getBlockY());

        // Dựng một trụ bia đá cổ cao 8 block với hoa văn cự thạch
        world.setBlockState(center.down(), Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 2);
        world.setBlockState(center, Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState(), 2);
        world.setBlockState(center.up(1), Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);
        world.setBlockState(center.up(2), Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 2);
        world.setBlockState(center.up(3), Blocks.REINFORCED_DEEPSLATE.getDefaultState(), 2);
        world.setBlockState(center.up(4), Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);
        world.setBlockState(center.up(5), Blocks.SEA_LANTERN.getDefaultState(), 2);
        world.setBlockState(center.up(6), Blocks.AMETHYST_CLUSTER.getDefaultState(), 2);

        world.playSound(null, center, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1.5f, 0.6f);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã dựng Trụ Bia Đá Cổ tại [" + center.getX() + ", " + baseY + ", " + center.getZ() + "]. Cầm Máy Quay bật chế độ Night Vision nhìn thẳng vào bia đá để kiểm tra Quét Quang Học!"), true);
        return 1;
    }

    private static int testCleanup(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ServerWorld world = player.getServerWorld();
        BlockPos pPos = player.getBlockPos();

        // Quét và xóa tất cả rift entity và text display trong phạm vi 64 block xung quanh người chơi
        Box area = new Box(pPos).expand(64, 128, 64);
        List<DimensionalRiftEntity> rifts = world.getEntitiesByClass(DimensionalRiftEntity.class, area, e -> true);
        List<DisplayEntity.TextDisplayEntity> texts = world.getEntitiesByClass(DisplayEntity.TextDisplayEntity.class, area, e -> true);
        int rCount = rifts.size();
        int tCount = texts.size();
        rifts.forEach(Entity::discard);
        texts.forEach(Entity::discard);

        source.sendFeedback(() -> Text.literal("§e[CLEANUP] §aĐã dọn dẹp sạch sẽ " + rCount + " Cổng Rạn Nứt và " + tCount + " Nhãn Chữ trong bán kính 64 block!"), true);
        return 1;
    }

    private static int testWombRift(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ServerWorld world = player.getServerWorld();

        BlockPos targetPos = player.getBlockPos().offset(player.getHorizontalFacing(), 4);
        float yaw = player.getYaw() + 180.0f;
        String styleType = "womb_" + DimensionalRiftEntity.defaultWombStyle;
        DimensionalRiftEntity rift = new DimensionalRiftEntity(world, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, yaw, styleType);
        world.spawnEntity(rift);

        world.playSound(null, targetPos, SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.BLOCKS, 1.2f, 0.9f);
        world.playSound(null, targetPos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0f, 1.2f);

        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã triệu hồi Cổng Vết Rách về The Womb (Kiểu " + DimensionalRiftEntity.defaultWombStyle + ") tại [" + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ() + "]. Dùng §b/samsara test womb_style <1|2|3> §ađể chuyển đổi 3 kiểu lòng cổng!"), true);
        return 1;
    }

    private static int testWombStyle(ServerCommandSource source, int style) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ServerWorld world = player.getServerWorld();

        DimensionalRiftEntity.defaultWombStyle = style;
        String newType = "womb_" + style;

        Box searchBox = new Box(player.getBlockPos()).expand(64, 128, 64);
        List<DimensionalRiftEntity> rifts = world.getEntitiesByClass(DimensionalRiftEntity.class, searchBox,
                r -> r.getRiftType() != null && r.getRiftType().toLowerCase().startsWith("womb"));
        for (DimensionalRiftEntity rift : rifts) {
            rift.setRiftType(newType);
        }

        String styleName;
        switch (style) {
            case 1:
                styleName = "Kiểu 1: Silexars (Creation - Điểm kỳ dị sắc sai quang phổ)";
                break;
            case 2:
                styleName = "Kiểu 2: Glowmarcher (Đường hầm giun 3D đa chiều uốn lượn)";
                break;
            case 3:
                styleName = "Kiểu 3: Kishimisu (Ma trận Fractal Neon huyền ảo)";
                break;
            default:
                styleName = "Kiểu " + style;
                break;
        }

        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_CLUSTER_STEP, SoundCategory.PLAYERS, 1.2f, 1.5f);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã chuyển lòng cổng Bình Nguyên sang §6" + styleName + " §a(đã đồng bộ " + rifts.size() + " cổng xung quanh)!"), true);
        return 1;
    }

    private static int testSunStage(ServerCommandSource source, int stage) {
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = (player != null) ? player.getServerWorld() : source.getServer().getOverworld();

        com.samsara.system.celestial.SolarFractureManager.setStage(stage, world);

        String stageDesc;
        switch (stage) {
            case 0: stageDesc = "Giai đoạn 0: Mặt Trời Vanilla bình thường (chưa ngủ lần nào)"; break;
            case 1: stageDesc = "Giai đoạn 1: Mặt Trời Rạn Nứt & Mắt Hư Không (Nascent Singularity & Fractures)"; break;
            case 2: stageDesc = "Giai đoạn 2: Con Mắt Hư Không & Bụi Sao Đồng Tâm (Face-On Singularity / AnasAbdin Style)"; break;
            case 3: stageDesc = "Giai đoạn 3: Bão Plasma Xoáy Cuộn & Cánh Lửa Nghiêng (Relativistic Plasma Vortex Flare)"; break;
            case 4: stageDesc = "Giai đoạn 4: Siêu Hố Đen Gargantua Nghệ Thuật (Fine-Ring Dithering & 3D Accretion Disk)"; break;
            default: stageDesc = "Giai đoạn " + stage; break;
        }

        if (world != null) {
            world.playSound(null, source.getPosition().x, source.getPosition().y, source.getPosition().z,
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.AMBIENT, 1.2f, 0.6f);
        }

        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã đặt Mặt Trời sang §6" + stageDesc + "§a! Đưa Máy Quay lên mắt (chuột phải) rồi ngước nhìn Mặt Trời để quan sát — mắt thường chỉ thấy Mặt Trời Vanilla."), true);
        return 1;
    }

    // ==========================================
    // EGG CYCLE (Giai đoạn 1 của lộ trình)
    // ==========================================

    private static int testCycleStatus(ServerCommandSource source) {
        EggCycleState state = EggCycleState.get(source.getServer());
        ServerPlayerEntity player = source.getPlayer();
        source.sendFeedback(() -> Text.literal("§e[CYCLE] §fSố lần ngủ: §6" + state.getSleeps()
                + " §f| Giai đoạn: §6" + state.getStage() + "/4"), false);
        if (player != null) {
            int nights = PhantomTrapSystem.nightsAwake(player);
            source.sendFeedback(() -> Text.literal("§e[CYCLE] §fĐêm thức: §6" + nights
                    + " §f(bầy mục tiêu " + PhantomTrapSystem.targetSwarmSize(nights) + ")"
                    + " §f| Đã nhận máy quay: §6" + state.wasCamcorderGiven(player.getUuid())
                    + " §f| Đã vào The Womb: §6" + state.isAwakened(player.getUuid())), false);
        }
        return 1;
    }

    private static int testSleepCount(ServerCommandSource source, int sleeps) {
        ServerWorld world = source.getServer().getOverworld();
        com.samsara.system.celestial.SolarFractureManager.setSleeps(sleeps, world);
        ServerPlayerEntity player = source.getPlayer();
        if (player != null && sleeps < EggCycleState.FINAL_SLEEP) {
            // Cho phép thử lại giấc ngủ thứ 7
            EggCycleState.get(source.getServer()).clearAwakened(player.getUuid());
        }
        int stage = EggCycleState.get(source.getServer()).getStage();
        String hint = sleeps == EggCycleState.FINAL_SLEEP - 1 ? "§a. Ngủ thêm 1 đêm = giấc ngủ thứ 7."
                : stage >= 4 ? "§a. Ngủ qua đêm để bị kéo vào The Womb." : "§a.";
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã đặt số lần ngủ = §6" + sleeps + " §a-> Giai đoạn §6" + stage + "/4" + hint), true);
        return 1;
    }

    private static int testPhantomWave(ServerCommandSource source, int nights) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        PhantomTrapSystem.setNightsAwake(player, nights);
        int spawned = PhantomTrapSystem.spawnWave(player, nights);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐêm thức = §6" + nights + "§a, đã thả §6" + spawned
                + " §aPhantom (bầy mục tiêu " + PhantomTrapSystem.targetSwarmSize(nights) + "). Ngủ để xua tan bầy."), true);
        return 1;
    }

    private static int testCamcorderGift(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        CamcorderGiftSystem.give(player);
        EggCycleState.get(source.getServer()).markCamcorderGiven(player.getUuid());
        player.sendMessage(Text.literal("§e<System> §fCầm lấy. Nhìn qua nó đi."), false);
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                com.samsara.registry.SamsaraSounds.SYSTEM_STATIC, SoundCategory.PLAYERS, 0.9f, 0.6f);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã cài máy quay vào ô số 3 (món đồ cũ ở ô đó được dời sang ô trống)."), true);
        return 1;
    }

    private static int testWombTransition(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        if (WombTransitionSystem.isInTransition(player)) return 0;
        // Không đứng trên giường thì The Womb dựng một chiếc giường đỏ mặc định
        WombTransitionSystem.begin(player, player.getBlockPos());
        source.sendFeedback(() -> Text.literal("§e[TEST] §aBắt đầu bị hút vào The Womb (" + WombTransitionSystem.TRANSITION_TICKS / 20.0 + " giây)."), true);
        return 1;
    }

    private static int testNamHouse(ServerCommandSource source) {
        ServerWorld world = NamMemoryManager.getWorld(source.getServer());
        if (world == null) return 0;
        com.samsara.worldgen.memory.NamHouseBuilder.build(world);
        EggCycleState.get(source.getServer()).setNamHouseBuilt(true);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã dựng lại nhà Nam ở Ký Ức Của Nam. Dùng §d/samsara nam §ađể tới đó."), true);
        return 1;
    }

    private static int testNamField(ServerCommandSource source, int radius) {
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = NamMemoryManager.getWorld(source.getServer());
        if (player == null || world == null) return 0;
        if (player.getServerWorld() != world) {
            source.sendFeedback(() -> Text.literal("§c[TEST] Hãy đứng trong Ký Ức Của Nam (/samsara nam) trước."), false);
            return 0;
        }
        int chunks = com.samsara.worldgen.memory.SunflowerFieldFeature.replant(world,
                player.getChunkPos().x, player.getChunkPos().z, radius);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã trồng lại hoa hướng dương ở §6" + chunks + " §achunk quanh bạn."), true);
        return 1;
    }

    private static int testWombRoute(ServerCommandSource source) {
        BlockPos origin = EggCycleState.get(source.getServer()).getRouteOrigin();
        if (origin == null) {
            source.sendFeedback(() -> Text.literal("§e[ROUTE] §cChưa có tuyến. Vào The Womb (/samsara womb) để tự dựng, hoặc đứng trong The Womb và dùng §b/samsara test womb_route here§c."), false);
            return 0;
        }
        ServerPlayerEntity player = source.getPlayer();
        source.sendFeedback(() -> Text.literal("§e[ROUTE] §fGốc tuyến: §6" + origin.getX() + ", " + origin.getZ()), false);
        for (com.samsara.system.womb.WombRoute.Stop stop : com.samsara.system.womb.WombRoute.Stop.values()) {
            BlockPos p = stop.pos(origin);
            String dist = player != null && player.getWorld().getRegistryKey() == com.samsara.registry.SamsaraDimensions.THE_WOMB_WORLD_KEY
                    ? " §8(cách " + (int) Math.sqrt(player.getBlockPos().getSquaredDistance(p)) + " block)" : "";
            source.sendFeedback(() -> Text.literal("§b " + stop.ordinal() + ". §f" + stop.title + " §7@ " + p.getX() + ", " + p.getZ() + dist), false);
        }
        return 1;
    }

    private static int testWombRouteHere(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        if (player.getWorld().getRegistryKey() != com.samsara.registry.SamsaraDimensions.THE_WOMB_WORLD_KEY) {
            source.sendFeedback(() -> Text.literal("§c[ROUTE] Hãy đứng trong The Womb (/samsara womb) trước."), false);
            return 0;
        }
        BlockPos origin = player.getBlockPos().withY(com.samsara.system.womb.WombRoute.SURFACE_Y);
        EggCycleState.get(source.getServer()).setRouteOrigin(origin);
        com.samsara.system.womb.WombRouteManager.build(player.getServerWorld(), origin);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã dựng tuyến hành hương bắt đầu từ chỗ bạn đứng, chạy về phía bắc ~400 block."), true);
        return testWombRoute(source);
    }

    private static int testWombRouteRebuild(ServerCommandSource source) {
        BlockPos origin = EggCycleState.get(source.getServer()).getRouteOrigin();
        ServerWorld womb = source.getServer().getWorld(com.samsara.registry.SamsaraDimensions.THE_WOMB_WORLD_KEY);
        if (origin == null || womb == null) return testWombRoute(source);
        com.samsara.system.womb.WombRouteManager.build(womb, origin);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã dựng lại tuyến hành hương."), true);
        return 1;
    }

    /** Dựng lại biển cửa (chương II) và đưa người chơi tới đầu hành lang, nhìn về phía bắc. */
    private static int testWombDoors(ServerCommandSource source) {
        BlockPos origin = EggCycleState.get(source.getServer()).getRouteOrigin();
        ServerWorld womb = source.getServer().getWorld(com.samsara.registry.SamsaraDimensions.THE_WOMB_WORLD_KEY);
        if (origin == null || womb == null) return testWombRoute(source);
        com.samsara.system.womb.chapters.SaveDoorChapter.build(womb, origin);
        com.samsara.system.womb.chapters.DockChapter.build(womb, origin);
        com.samsara.system.womb.chapters.TitanChapter.build(womb, origin);
        ServerPlayerEntity player = source.getPlayer();
        if (player != null) {
            player.teleport(womb, origin.getX() + 0.5, com.samsara.system.womb.WombRoute.SURFACE_Y, origin.getZ() - 10.5, 180.0f, 0.0f);
        }
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã dựng lại căn phòng cửa và Bến Đò. Nhìn về phía bắc; ngắm máy quay vào cửa sát tuyến để thấy tên save; cánh cửa trắng ở tường bắc là lối ra."), true);
        return 1;
    }

    private static int testWombTitans(ServerCommandSource source) {
        BlockPos origin = EggCycleState.get(source.getServer()).getRouteOrigin();
        ServerWorld womb = source.getServer().getWorld(com.samsara.registry.SamsaraDimensions.THE_WOMB_WORLD_KEY);
        if (origin == null || womb == null) return testWombRoute(source);
        com.samsara.system.womb.chapters.TitanChapter.build(womb, origin);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã dựng lại 3 khổng thần. Tới xem bằng §b/samsara test womb_route tp 1§a / §b2§a / §b3§a."), true);
        return 1;
    }

    private static int testScreenshotSun(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        int stage = EggCycleState.get(source.getServer()).getStage();
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                com.samsara.system.celestial.SolarFractureManager.SCREENSHOT_SUN_TEST,
                net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã chụp màn hình (Stage " + stage + "). "
                + (stage >= 1 ? "Nhìn về phía mặt trời rồi mở thư mục screenshots: ảnh có hố đen, mắt vẫn thấy mặt trời thường."
                              : "§cStage 0: ảnh chụp như thường. Dùng §b/samsara test sleep_count 1§c trước.")), false);
        return 1;
    }

    private static int testUi(ServerCommandSource source, String what) {
        var server = source.getServer();
        ServerPlayerEntity player = source.getPlayer();
        switch (what) {
            case "join" -> com.samsara.system.ui.UiHorrorSystem.ghostJoin(server, 20 * 60);
            case "leave" -> com.samsara.system.ui.UiHorrorSystem.ghostLeave(server);
            case "say" -> com.samsara.system.ui.UiHorrorSystem.ghostSay(server, com.samsara.system.ui.UiHorrorSystem.GHOST_LINE);
            case "archive" -> com.samsara.system.ui.UiHorrorSystem.ghostJoin(server, 0, true);
            case "advancement" -> {
                if (player == null) return 0;
                com.samsara.system.ui.UiHorrorSystem.advancement(player, "test", "Đừng Rời Mắt", Items.ENDER_EYE, true);
            }
            case "rename" -> {
                String name = com.samsara.system.ui.UiHorrorSystem.levelNameFor(server);
                String shown = name != null ? name : "(giữ nguyên - cần Stage 3+)";
                source.sendFeedback(() -> Text.literal("§e[TEST] §aKhi thoát game, tên thế giới sẽ thành: §6" + shown
                        + "§a. Đổi lại trong danh sách thế giới (Edit) nếu muốn."), false);
                return 1;
            }
            case "reset" -> {
                if (player != null) EggCycleState.get(server).clearUiFlags(player.getUuid());
                EggCycleState.get(server).clearUiFlags(com.samsara.system.cycle.NamGhostSystem.NAM_UUID);
                source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã xoá các mốc thành tựu giả đã hiện và cờ 3 dòng Chu Kỳ #2 (sẽ hiện lại khi chạm mốc)."), false);
                return 1;
            }
            case "ending_menu_on", "ending_menu_off" -> {
                if (player == null) return 0;
                boolean on = what.endsWith("on");
                com.samsara.system.ui.UiHorrorSystem.sendEndingMenuFlag(player, on);
                source.sendFeedback(() -> Text.literal("§e[TEST] §aMenu chính kiểu \"sau đoạn kết\": " + (on ? "BẬT" : "TẮT")
                        + "§a. Thoát ra menu chính để xem."), false);
                return 1;
            }
            default -> { return 0; }
        }
        return 1;
    }

    private static int testMirror(ServerCommandSource source, float scale) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        net.minecraft.network.PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeFloat(scale);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, com.samsara.system.ending.EndingServer.MIRROR_PREVIEW, buf);
        source.sendFeedback(() -> Text.literal(scale <= 0 ? "§e[TEST] §aĐã tắt bóng phản chiếu."
                : "§e[TEST] §aBóng phản chiếu cỡ §fx" + (int) scale + "§a. Gõ lại với cỡ khác để thấy cú nhảy (1, 2, 4, 12, 45); "
                + "sau 3 giây nó bắt đầu trễ nửa nhịp. Ở x45 trong The Womb: mắt trùng Gargantua + tín hiệu shader."), false);
        return 1;
    }

    private static int testSunflowerGaze(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        if (player.getServerWorld().getRegistryKey() != com.samsara.registry.SamsaraDimensions.NAM_MEMORY_WORLD_KEY) {
            source.sendFeedback(() -> Text.literal("§e[TEST] §cPhải đứng trong Ký Ức Của Nam (/samsara nam)."), false);
            return 0;
        }
        int n = com.samsara.system.memory.SunflowerGazeSystem.turnToward(player.getServerWorld(), player.getX(), player.getZ(), 48, 1.0f);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã quay §f" + n + "§a bông hoa về phía bạn. Đi lại để thấy hoa quay theo."), false);
        return 1;
    }

    private static int testNewCycle(ServerCommandSource source, String what) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        if (what.equals("house")) {
            BlockPos ground = com.samsara.system.cycle.NewCycleSystem.buildHouseFacing(player.getServerWorld(), player.getBlockPos());
            source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã dựng nhà Nam (bản chu kỳ mới, không có vết rách) ở §f"
                    + ground.toShortString() + "§a, cửa quay về phía bạn. Rương hầm để trống."), false);
        } else if (what.equals("cairn")) {
            BlockPos at = player.getBlockPos().north(3);
            com.samsara.worldgen.SpawnCairnGenerator.buildCairn(player.getServerWorld(), at, false);
            source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã dựng Bia Mộ bản chu kỳ mới (rương chỉ có nhật ký, không có hoa khô) ở §f"
                    + at.toShortString()), false);
        } else {
            int cycle = Math.max(2, EggCycleState.get(source.getServer()).getCycle());
            com.samsara.system.cycle.NewCycleSystem.showIntro(player, cycle);
        }
        return 1;
    }

    private static int testCamcorderModel(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        if (!player.getMainHandStack().isOf(SamsaraItems.FRACTURED_SLATE)) {
            ItemStack old = player.getMainHandStack().copy();
            player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(SamsaraItems.FRACTURED_SLATE));
            if (!old.isEmpty()) player.getInventory().offerOrDrop(old);
        }
        player.getInventory().offerOrDrop(new ItemStack(SamsaraItems.CAMCORDER_BATTERY, 2));
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, CAMCORDER_MODEL_TEST,
                net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã đặt máy quay vào tay, pin còn 50% và thêm 2 pin dự phòng. "
                + "Đổi ô rồi cầm lại để xem màn LCD lật mở; bấm §bR §ađể xem nắp băng bật lên; §bF5 §ađể xem góc thứ ba."), false);
        return 1;
    }

    private static int testSoulPhantom(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ServerWorld world = player.getServerWorld();
        net.minecraft.util.math.Vec3d look = player.getRotationVector().multiply(1, 0, 1);
        if (look.lengthSquared() < 1.0E-4) look = new net.minecraft.util.math.Vec3d(0, 0, 1);
        net.minecraft.util.math.Vec3d at = player.getPos().add(look.normalize().multiply(6.0)).add(0, 2.0, 0);
        net.minecraft.entity.mob.PhantomEntity phantom = net.minecraft.entity.EntityType.PHANTOM.create(world);
        if (phantom == null) return 0;
        phantom.refreshPositionAndAngles(at.x, at.y, at.z, player.getYaw() + 180.0f, 0.0f);
        phantom.setBodyYaw(player.getYaw() + 180.0f);
        phantom.setAiDisabled(true);
        phantom.setPhantomSize(2);
        world.spawnEntity(phantom);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aMột Phantom đứng yên trước mặt bạn (không AI). Mắt thường thấy Phantom thường; đưa máy quay lên mắt để thấy hình dạng thật. Dùng §b/kill @e[type=phantom] §ađể dọn."), true);
        return 1;
    }

    private static int testWandererPose(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ServerWorld world = player.getServerWorld();
        net.minecraft.util.math.Vec3d look = player.getRotationVector().multiply(1, 0, 1);
        if (look.lengthSquared() < 1.0E-4) look = new net.minecraft.util.math.Vec3d(0, 0, 1);
        net.minecraft.util.math.Vec3d at = player.getPos().add(look.normalize().multiply(5.0));
        com.samsara.entity.TheWandererEntity w = com.samsara.registry.SamsaraEntities.THE_WANDERER.create(world);
        if (w == null) return 0;
        w.setPinned(true);
        w.refreshPositionAndAngles(at.x, player.getY(), at.z, player.getYaw() + 180.0f, 0.0f);
        w.setHeadYaw(player.getYaw() + 180.0f);
        w.setBodyYaw(player.getYaw() + 180.0f);
        world.spawnEntity(w);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aThe Wanderer đứng trước mặt bạn 5 block (2 phút). Chỉ thấy khi đưa máy quay lên mắt."), true);
        return 1;
    }

    private static int testWandererChase(ServerCommandSource source, boolean start) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        if (!start) {
            com.samsara.system.womb.WanderChaseSystem.setEnabled(player, false);
            source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã tắt The Wanderer bám đuôi cho bạn (bật lại bằng §b/samsara test wanderer_chase start§a)."), true);
            return 1;
        }
        if (player.getWorld().getRegistryKey() != com.samsara.registry.SamsaraDimensions.THE_WOMB_WORLD_KEY) {
            source.sendFeedback(() -> Text.literal("§c[TEST] Hãy đứng trên tuyến hành hương trong The Womb (vd. §b/samsara test womb_route tp 1§c)."), false);
            return 0;
        }
        boolean ok = com.samsara.system.womb.WanderChaseSystem.startNow(player);
        source.sendFeedback(() -> Text.literal(ok
                ? "§e[TEST] §aThe Wanderer đứng sau lưng bạn ~20 block. Đừng nhìn qua máy quay để thấy nó tiến lại; ngắm nó qua ống kính thì nó đứng sững."
                : "§c[TEST] Chưa có tuyến hành hương."), true);
        return ok ? 1 : 0;
    }

    private static int testChapterTitle(ServerCommandSource source, int chapter) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        com.samsara.system.womb.WombPacingSystem.showTitle(player, chapter - 1);
        return 1;
    }

    private static int testWombDock(ServerCommandSource source) {
        BlockPos origin = EggCycleState.get(source.getServer()).getRouteOrigin();
        ServerWorld womb = source.getServer().getWorld(com.samsara.registry.SamsaraDimensions.THE_WOMB_WORLD_KEY);
        if (origin == null || womb == null) return testWombRoute(source);
        com.samsara.system.womb.chapters.DockChapter.build(womb, origin);
        com.samsara.system.womb.WombCurrentSystem.syncAll(source.getServer());
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã dựng lại Bến Đò (thuyền chờ ở đầu bến). Tới đó bằng §b/samsara test womb_route tp 0§a, ngồi thuyền để dòng chảy đẩy đi; đưa máy quay lên mắt để thấy thuyền bóng ma."), true);
        return 1;
    }

    private static int testWombSpire(ServerCommandSource source) {
        BlockPos origin = EggCycleState.get(source.getServer()).getRouteOrigin();
        ServerWorld womb = source.getServer().getWorld(com.samsara.registry.SamsaraDimensions.THE_WOMB_WORLD_KEY);
        if (origin == null || womb == null) return testWombRoute(source);
        com.samsara.system.womb.chapters.SpireChapter.build(womb, origin);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã dựng lại Tháp Singularity & Hồ Gương. Tới chân cầu thang bằng §b/samsara test womb_route tp 4§a."), true);
        return 1;
    }

    /** Đóng lại cánh cửa cuối rồi đưa người chơi lên cầu, đứng trước cửa nhìn về phía hồ. */
    private static int testWombFinalDoor(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        BlockPos origin = EggCycleState.get(source.getServer()).getRouteOrigin();
        ServerWorld womb = source.getServer().getWorld(com.samsara.registry.SamsaraDimensions.THE_WOMB_WORLD_KEY);
        if (player == null || origin == null || womb == null) return testWombRoute(source);
        BlockPos door = com.samsara.system.womb.chapters.SpireChapter.finalDoor(origin);
        for (BlockPos p : new BlockPos[]{door, door.up()}) {
            net.minecraft.block.BlockState st = womb.getBlockState(p);
            if (st.isOf(com.samsara.registry.SamsaraBlocks.MEMORY_DOOR_FINAL)) {
                womb.setBlockState(p, st.with(com.samsara.block.MemoryDoorFinalBlock.OPEN, false), 3);
            }
        }
        player.teleport(womb, door.getX() + 0.5, door.getY(), door.getZ() + 2.5, 180.0f, 0.0f);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐứng trước cánh cửa cuối (đã đóng lại). Nhấp chuột phải để mở; đưa máy quay lên mắt để đọc nhãn."), true);
        return 1;
    }

    /** Dựng một bức tường 5x3 và một mảng sàn 3x3 bằng Khối Mã trước mặt người chơi để xem mã nối liền. */
    private static int testCodeBlock(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ServerWorld world = player.getServerWorld();
        net.minecraft.util.math.Direction f = player.getHorizontalFacing();
        net.minecraft.util.math.Direction side = f.rotateYClockwise();
        BlockPos base = player.getBlockPos().offset(f, 4);
        net.minecraft.block.BlockState code = com.samsara.registry.SamsaraBlocks.WOMB_CODE.getDefaultState();
        for (int a = -2; a <= 2; a++) {
            for (int y = 0; y < 3; y++) {
                BlockPos p = base.offset(side, a).up(y);
                world.setBlockState(p, com.samsara.block.WombCodeBlock.at(code, p), 3);
            }
        }
        for (int a = -1; a <= 1; a++) {
            for (int b = 1; b <= 3; b++) {
                BlockPos p = player.getBlockPos().offset(side, a).offset(f, b).down();
                world.setBlockState(p, com.samsara.block.WombCodeBlock.at(code, p), 3);
            }
        }
        for (int a = -3; a <= 3; a++) {
            for (int y = -1; y <= 3; y++) {
                for (int b = 0; b <= 5; b++) {
                    com.samsara.block.WombCodeBlock.refresh(world, player.getBlockPos().offset(side, a).offset(f, b).up(y));
                }
            }
        }
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã dựng tường và sàn Khối Mã trước mặt."), false);
        return 1;
    }

    private static int testWombRouteTp(ServerCommandSource source, int index) {
        ServerPlayerEntity player = source.getPlayer();
        BlockPos origin = EggCycleState.get(source.getServer()).getRouteOrigin();
        ServerWorld womb = source.getServer().getWorld(com.samsara.registry.SamsaraDimensions.THE_WOMB_WORLD_KEY);
        if (player == null || origin == null || womb == null) return testWombRoute(source);
        com.samsara.system.womb.WombRoute.Stop stop = com.samsara.system.womb.WombRoute.Stop.byIndex(index);
        BlockPos p = stop.pos(origin);
        // Tháp: đứng trên bến ở chân đại cầu thang, nhìn về phía bắc
        if (stop == com.samsara.system.womb.WombRoute.Stop.SPIRE) p = com.samsara.system.womb.chapters.SpireChapter.dock(origin);
        player.teleport(womb, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 180.0f, 0.0f);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã tới chặng §6" + stop.ordinal() + ". " + stop.title), true);
        return 1;
    }

    /** Dựng bệ để trứng nằm cách người chơi ~8 block phía trước; người chơi đứng ở mép hồ. */
    private static com.samsara.entity.PrimordialEggEntity buildEggInFront(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        net.minecraft.util.math.Vec3d look = player.getRotationVector().multiply(1, 0, 1);
        if (look.lengthSquared() < 1.0E-4) look = new net.minecraft.util.math.Vec3d(0, 0, 1);
        look = look.normalize();
        BlockPos center = BlockPos.ofFloored(player.getX() + look.x * 8.0, player.getY(), player.getZ() + look.z * 8.0);
        return com.samsara.system.ending.EggAltarManager.build(world, center, null);
    }

    private static int testEgg(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        buildEggInFront(player);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã dựng bệ + Quả Trứng Nguyên Thủy trước mặt. Bước vào trong bán kính 6 block quanh trứng sẽ kích hoạt đoạn kết (mỗi người chơi 1 lần; dùng §b/samsara test ending §ađể xem lại)."), true);
        return 1;
    }

    private static int testEnding(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        EggCycleState.get(source.getServer()).clearEndingSeen(player.getUuid());
        Box area = player.getBoundingBox().expand(24.0);
        List<com.samsara.entity.PrimordialEggEntity> eggs = player.getServerWorld()
                .getEntitiesByClass(com.samsara.entity.PrimordialEggEntity.class, area, e -> true);
        com.samsara.entity.PrimordialEggEntity egg = eggs.isEmpty() ? buildEggInFront(player) : eggs.get(0);
        if (egg == null) return 0;
        com.samsara.system.ending.EndingServer.start(player, egg);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aBắt đầu đoạn kết. Cuối đoạn kết game sẽ lưu và thoát về menu chính."), true);
        return 1;
    }

    private static int testNamGhosts(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        int count = NamGhostSystem.spawnRing(player);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã gọi §6" + count + " §abóng ma Nam_1998 quanh bạn (30 giây). Đưa máy quay lên mắt để thấy chúng"
                + (count == 0 ? " §c(chỉ hoạt động ở Overworld)" : "") + "§a."), true);
        return 1;
    }

    private static int testAnomalyLift(ServerCommandSource source, int stage) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        int lifted = CollapseAnomalySystem.liftNear(player, stage, stage >= 4 ? 12 : 6);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã hút §6" + lifted + " §ablock lên trời theo luật Giai đoạn " + stage
                + (lifted == 0 ? " §c(không tìm thấy block phù hợp; Giai đoạn 3 cần cát/sỏi trên mặt đất)" : "") + "§a."), true);
        return 1;
    }

    private static int testAnomalySheep(ServerCommandSource source) {
        StageAnomalySystem.startSheepEpisode(source.getServer());
        int stage = EggCycleState.get(source.getServer()).getStage();
        String warn = stage < 1 ? " §c(cần Giai đoạn 1+: /samsara test sleep_count 1)" : "";
        source.sendFeedback(() -> Text.literal("§e[TEST] §aBầy cừu trong bán kính 48 block đứng im nhìn về Bia Mộ trong 30 giây." + warn), true);
        return 1;
    }

    private static int testAnomalyReverse(ServerCommandSource source, String which) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        switch (which) {
            case "phantom" -> player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket(
                    net.minecraft.registry.Registries.SOUND_EVENT.getEntry(com.samsara.registry.SamsaraSounds.PHANTOM_REVERSED),
                    net.minecraft.sound.SoundCategory.HOSTILE, player.getX(), player.getY() + 8, player.getZ(), 1.4f, 0.6f, player.getRandom().nextLong()));
            case "cave" -> player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket(
                    net.minecraft.registry.Registries.SOUND_EVENT.getEntry(com.samsara.registry.SamsaraSounds.CAVE_REVERSED),
                    net.minecraft.sound.SoundCategory.AMBIENT, player.getX(), player.getY(), player.getZ(), 1.0f, 0.9f, player.getRandom().nextLong()));
            default -> com.samsara.system.cycle.ReversedEchoSystem.play(player);
        }
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã phát âm tua ngược: §f" + which
                + ("familiar".equals(which) ? " §7(sau lưng bạn 3–5 block)" : "")), false);
        return 1;
    }

    private static int testAnomalyMusic(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        StageAnomalySystem.sendMusicStutter(player);
        source.sendFeedback(() -> Text.literal("§e[TEST] §aĐã làm âm thanh giật như băng kẹt đầu đọc."), true);
        return 1;
    }
}
