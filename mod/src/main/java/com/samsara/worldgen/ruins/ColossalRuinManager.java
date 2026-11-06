package com.samsara.worldgen.ruins;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Quản lý sinh phế tích cự thần rải rác khắp thế giới Overworld (Colossal Ruin Exploration System).
 * - Tự động phát hiện khi người chơi thám hiểm đến gần các tọa độ phế tích cổ xưa và sinh tự nhiên.
 * - Hỗ trợ lệnh /samsara ruin nearest và /samsara ruin spawn <type>.
 */
public class ColossalRuinManager extends PersistentState {

    public static final String KEY = "samsara_colossal_ruins";
    private static final int CELL_SIZE = 640; // Mỗi ô lưới 640x640 block có một phế tích cự thần
    private static final int TRIGGER_DIST_SQ = 160 * 160; // Kích hoạt khi người chơi cách trong vòng 160 blocks

    private final Set<String> generatedCells = new HashSet<>();
    private static int tickCounter = 0;

    public static ColossalRuinManager get(ServerWorld world) {
        return world.getServer().getOverworld().getPersistentStateManager().getOrCreate(
                ColossalRuinManager::fromNbt,
                ColossalRuinManager::new,
                KEY
        );
    }

    public static void register() {
        // Đăng ký lệnh
        CommandRegistrationCallback.EVENT.register(ColossalRuinManager::registerCommands);

        // Đăng ký vòng lặp thám hiểm mỗi 40 tick (2 giây) kiểm tra vị trí người chơi
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey() != ServerWorld.OVERWORLD) return;

            tickCounter++;
            if (tickCounter % 40 != 0) return;

            ColossalRuinManager manager = get(world);

            for (ServerPlayerEntity player : world.getPlayers()) {
                BlockPos pPos = player.getBlockPos();
                int playerCellX = Math.floorDiv(pPos.getX(), CELL_SIZE);
                int playerCellZ = Math.floorDiv(pPos.getZ(), CELL_SIZE);

                // Kiểm tra ô hiện tại và các ô lân cận
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int cX = playerCellX + dx;
                        int cZ = playerCellZ + dz;
                        String cellKey = cX + "_" + cZ;

                        if (manager.generatedCells.contains(cellKey)) continue;

                        BlockPos ruinPos = getCandidatePosition(world.getSeed(), cX, cZ);
                        double distSq = ruinPos.getSquaredDistance(pPos.getX(), ruinPos.getY(), pPos.getZ());

                        if (distSq <= TRIGGER_DIST_SQ) {
                            // Người chơi đã đến gần vị trí phế tích! Sinh phế tích tự nhiên!
                            int surfaceY = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, ruinPos).getY();
                            if (surfaceY > 50 && surfaceY < 240) {
                                String type = getRuinTypeForCell(world.getSeed(), cX, cZ);
                                generateRuinByType(world, ruinPos.getX(), surfaceY, ruinPos.getZ(), type);

                                manager.generatedCells.add(cellKey);
                                manager.markDirty();

                                // Âm thanh rền vang kỳ quái báo hiệu phát hiện phế tích cổ
                                world.playSound(null, pPos, SoundEvents.AMBIENT_CAVE.value(), SoundCategory.AMBIENT, 2.5f, 0.4f);
                                world.playSound(null, pPos, SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.AMBIENT, 3.0f, 0.5f);
                                player.sendMessage(Text.translatable("samsara.colossal_ruin_manager.the_wind_moans_across"), false);
                            }
                        }
                    }
                }
            }
        });
    }

    private static BlockPos getCandidatePosition(long seed, int cellX, int cellZ) {
        long cellSeed = seed ^ (cellX * 341873128712L + cellZ * 132897987541L);
        Random r = new Random(cellSeed);
        int localX = 120 + r.nextInt(CELL_SIZE - 240);
        int localZ = 120 + r.nextInt(CELL_SIZE - 240);
        return new BlockPos(cellX * CELL_SIZE + localX, 64, cellZ * CELL_SIZE + localZ);
    }

    private static String getRuinTypeForCell(long seed, int cellX, int cellZ) {
        long cellSeed = seed ^ (cellX * 987123L + cellZ * 456123L);
        Random r = new Random(cellSeed);
        String[] types = new String[]{"chains", "ribcage", "skull", "arch", "ship"};
        return types[r.nextInt(types.length)];
    }

    public static boolean generateRuinByType(ServerWorld world, int x, int y, int z, String type) {
        return switch (type.toLowerCase()) {
            case "chains" -> ShatteredChainsRuin.generate(world, x, y, z);
            case "ribcage" -> ColossalRibcageRuin.generate(world, x, y, z);
            case "skull" -> FallenTitanSkullRuin.generate(world, x, y, z);
            case "arch" -> BrokenMegalithicArchRuin.generate(world, x, y, z);
            case "ship" -> SunkenDreadnoughtRuin.generate(world, x, y, z);
            default -> ColossalRibcageRuin.generate(world, x, y, z);
        };
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                         CommandRegistryAccess registryAccess,
                                         CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("samsara")
                .then(CommandManager.literal("ruin")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("nearest")
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) return 0;
                                    ServerWorld w = p.getServerWorld();
                                    BlockPos pPos = p.getBlockPos();
                                    int cX = Math.floorDiv(pPos.getX(), CELL_SIZE);
                                    int cZ = Math.floorDiv(pPos.getZ(), CELL_SIZE);

                                    BlockPos cand = getCandidatePosition(w.getSeed(), cX, cZ);
                                    String type = getRuinTypeForCell(w.getSeed(), cX, cZ);
                                    double dist = Math.sqrt(cand.getSquaredDistance(pPos.getX(), cand.getY(), pPos.getZ()));

                                    ctx.getSource().sendFeedback(() -> Text.literal(String.format(
                                            "§7[Phế Tích Samsara] Di tích gần nhất: §e%s§7 tại §f(%d, ~, %d)§7 (Khoảng cách: §a%.1fm§7)",
                                            type.toUpperCase(), cand.getX(), cand.getZ(), dist
                                    )), false);
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("spawn")
                                .then(CommandManager.argument("type", StringArgumentType.word())
                                        .suggests((c, b) -> {
                                            b.suggest("chains");
                                            b.suggest("ribcage");
                                            b.suggest("skull");
                                            b.suggest("arch");
                                            b.suggest("ship");
                                            b.suggest("random");
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            if (p == null) return 0;
                                            String type = StringArgumentType.getString(ctx, "type").toLowerCase();
                                            if (type.equals("random")) {
                                                String[] types = new String[]{"chains", "ribcage", "skull", "arch", "ship"};
                                                type = types[new Random().nextInt(types.length)];
                                            }
                                            BlockPos pos = p.getBlockPos();
                                            generateRuinByType(p.getServerWorld(), pos.getX(), pos.getY(), pos.getZ(), type);
                                            final String chosen = type;
                                            ctx.getSource().sendFeedback(() -> Text.literal("§8§o[Phế tích cổ '" + chosen.toUpperCase() + "' hiện lên từ chu kỳ bị lãng quên...]§r"), true);
                                            return 1;
                                        })
                                )
                        )
                )
        );
    }

    public static ColossalRuinManager fromNbt(NbtCompound nbt) {
        ColossalRuinManager m = new ColossalRuinManager();
        NbtList list = nbt.getList("GeneratedCells", 8);
        for (int i = 0; i < list.size(); i++) {
            m.generatedCells.add(list.getString(i));
        }
        return m;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (String cell : generatedCells) {
            list.add(NbtString.of(cell));
        }
        nbt.put("GeneratedCells", list);
        return nbt;
    }
}
