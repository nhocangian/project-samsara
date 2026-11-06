package com.samsara.worldgen.ruins;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.block.Blocks;
import com.samsara.registry.SamsaraBiomes;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.worldgen.ColossalInvertedMonolithFeature;
import com.samsara.worldgen.megalith.*;
import com.samsara.worldgen.ruins.ocean.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.entry.RegistryEntry;
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
import net.minecraft.world.biome.Biome;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Manager for Colossal Megaliths & Ancient Ruins in The Womb (Black Mirror Ocean).
 * Tự sinh 20 phế tích cự thần hoàn toàn tự nhiên theo từng ô lưới 320x320 block.
 * Tự động tạo trước khi người chơi nhìn thấy ở tầm xa ngay khi các chunk khu vực được tải,
 * loại bỏ hoàn toàn hiện tượng công trình đột ngột hiện ra trước mặt hay deadlock nạp chunk.
 */
public class WombRuinManager extends PersistentState {

    public static final String KEY = "samsara_womb_ruins";
    public static final int CELL_SIZE = 320;

    public static final String[] ALL_RUINS = new String[]{
        // 10 Ocean Relics
        "astrolabe", "obelisk", "wings", "temple", "throne",
        "crystals", "torso", "lantern", "sundial", "gateway",
        // 5 Original Colossal Megaliths
        "monolith", "chains", "arch", "titan", "hand",
        // 5 Ancient Ruin Structures
        "shattered_chains", "ribcage", "skull", "broken_arch", "ship"
    };

    private final Set<String> generatedCells = new HashSet<>();
    private static int tickCounter = 0;

    public static WombRuinManager get(ServerWorld world) {
        return world.getServer().getOverworld().getPersistentStateManager().getOrCreate(
                WombRuinManager::fromNbt,
                WombRuinManager::new,
                KEY
        );
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(WombRuinManager::registerCommands);

        // Sinh phế tích ở tầm xa an toàn ngay khi toàn bộ 3x3 chunk xung quanh tâm phế tích đã được nạp
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY)) {
                return;
            }

            tickCounter++;
            if (tickCounter % 20 != 0) return; // Kiểm tra mỗi giây

            WombRuinManager manager = get(world);

            for (ServerPlayerEntity player : world.getPlayers()) {
                BlockPos pPos = player.getBlockPos();
                int playerCellX = Math.floorDiv(pPos.getX(), CELL_SIZE);
                int playerCellZ = Math.floorDiv(pPos.getZ(), CELL_SIZE);

                // Quét bán kính 2 ô lưới xung quanh người chơi
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        int cX = playerCellX + dx;
                        int cZ = playerCellZ + dz;
                        String cellKey = cX + "_" + cZ;

                        if (manager.generatedCells.contains(cellKey)) continue;

                        BlockPos candidatePos = getCandidatePosition(world.getSeed(), cX, cZ);
                        int targetChunkX = candidatePos.getX() >> 4;
                        int targetChunkZ = candidatePos.getZ() >> 4;

                        // Kiểm tra xem cả vùng 3x3 chunk xung quanh công trình đã được nạp hay chưa
                        // Đảm bảo không xảy ra deadlock hay gián đoạn nạp chunk
                        boolean areaLoaded = true;
                        for (int cx = -1; cx <= 1; cx++) {
                            for (int cz = -1; cz <= 1; cz++) {
                                if (!world.isChunkLoaded(targetChunkX + cx, targetChunkZ + cz)) {
                                    areaLoaded = false;
                                    break;
                                }
                            }
                            if (!areaLoaded) break;
                        }

                        // Không sinh phế tích chắn ngang tuyến hành hương (WombRoute)
                        if (com.samsara.system.womb.WombRouteManager.isNearRoute(world, candidatePos.getX(), candidatePos.getZ())) {
                            manager.generatedCells.add(cellKey);
                            manager.markDirty();
                            continue;
                        }

                        if (areaLoaded) {
                            int surfaceY = Math.max(62, world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, candidatePos).getY());
                            String type = getRuinTypeForCell(world, cX, cZ);

                            // Xây dựng phế tích hoàn chỉnh trước khi người chơi lại gần
                            generateRuinByType(world, candidatePos.getX(), surfaceY, candidatePos.getZ(), type);
                            // Phế tích gần tuyến hành hương: dọn lại hành lang nếu nó chìa ra chắn đường
                            com.samsara.system.womb.WombRouteManager.clearCorridorNear(world, candidatePos.getX(), candidatePos.getZ(), 110.0);

                            // Sinh thêm các đặc trưng vi mô của Biome xung quanh phế tích
                            generateBiomeAmbientLandmarks(world, candidatePos.getX(), candidatePos.getZ());

                            manager.generatedCells.add(cellKey);
                            manager.markDirty();
                        }
                    }
                }
            }
        });
    }

    public static BlockPos getCandidatePosition(int cellX, int cellZ) {
        long cellSeed = (cellX * 52148291023L + cellZ * 91283741829L);
        Random r = new Random(cellSeed);
        int localX = 60 + r.nextInt(CELL_SIZE - 120);
        int localZ = 60 + r.nextInt(CELL_SIZE - 120);
        return new BlockPos(cellX * CELL_SIZE + localX, 62, cellZ * CELL_SIZE + localZ);
    }

    public static BlockPos getCandidatePosition(long seed, int cellX, int cellZ) {
        return getCandidatePosition(cellX, cellZ);
    }

    public static String getRuinTypeForCell(ServerWorld world, int cellX, int cellZ) {
        long cellSeed = world.getSeed() ^ (cellX * 817263541L + cellZ * 293847162L);
        Random r = new Random(cellSeed);

        BlockPos pos = getCandidatePosition(world.getSeed(), cellX, cellZ);
        RegistryEntry<Biome> biome = world.getBiome(pos);

        if (biome.matchesKey(SamsaraBiomes.TITAN_GRAVEYARD)) {
            String[] titanPool = new String[]{"titan", "hand", "torso", "shattered_chains", "chains", "arch", "obelisk"};
            return titanPool[r.nextInt(titanPool.length)];
        } else if (biome.matchesKey(SamsaraBiomes.SINGULARITY_SPIRES)) {
            String[] spiresPool = new String[]{"crystals", "gateway", "monolith", "astrolabe", "sundial", "lantern"};
            return spiresPool[r.nextInt(spiresPool.length)];
        } else {
            // Origin Cradle: các đền thờ, đôi cánh và ngai vàng thiêng liêng
            String[] cradlePool = new String[]{"throne", "wings", "temple", "obelisk", "sundial", "astrolabe", "lantern"};
            return cradlePool[r.nextInt(cradlePool.length)];
        }
    }

    public static String getRuinTypeForCell(long seed, int cellX, int cellZ) {
        long cellSeed = seed ^ (cellX * 817263541L + cellZ * 293847162L);
        Random r = new Random(cellSeed);
        return ALL_RUINS[r.nextInt(ALL_RUINS.length)];
    }

    public static boolean generateRuinByType(ServerWorld world, int x, int y, int z, String type) {
        int surfaceY = Math.max(62, y);
        return switch (type.toLowerCase()) {
            // 10 Ocean Relics
            case "astrolabe" -> DrownedAstrolabeRuin.generate(world, x, surfaceY, z);
            case "obelisk" -> WeepingObeliskRuin.generate(world, x, surfaceY, z);
            case "wings" -> SeveredWingsRuin.generate(world, x, surfaceY, z);
            case "temple" -> SubmergedTempleRuin.generate(world, x, surfaceY, z);
            case "throne" -> PrecursorThroneRuin.generate(world, x, surfaceY, z);
            case "crystals" -> SingularityCrystalsRuin.generate(world, x, surfaceY, z);
            case "torso" -> SunkenColossusTorsoRuin.generate(world, x, surfaceY, z);
            case "lantern" -> SoulLanternTowerRuin.generate(world, x, surfaceY, z);
            case "sundial" -> EternitySundialRuin.generate(world, x, surfaceY, z);
            case "gateway" -> BrokenMirrorGatewayRuin.generate(world, x, surfaceY, z);
            // 5 Original Colossal Megaliths
            case "monolith" -> ColossalInvertedMonolithFeature.generateMonolithStructure(world, x, z);
            case "chains" -> HeavenBindingChainsFeature.generate(world, x, z);
            case "arch" -> MegalithicArchFeature.generate(world, x, z);
            case "titan" -> PetrifiedTitanFeature.generate(world, x, z);
            case "hand" -> FossilizedHandFeature.generate(world, x, z);
            // 5 Fallen Titan Relics
            case "shattered_chains" -> ShatteredChainsRuin.generate(world, x, surfaceY, z);
            case "ribcage" -> ColossalRibcageRuin.generate(world, x, surfaceY, z);
            case "skull" -> FallenTitanSkullRuin.generate(world, x, surfaceY, z);
            case "broken_arch" -> BrokenMegalithicArchRuin.generate(world, x, surfaceY, z);
            case "ship" -> SunkenDreadnoughtRuin.generate(world, x, surfaceY, z);
            default -> false;
        };
    }

    public static void generateBiomeAmbientLandmarks(ServerWorld world, int centerX, int centerZ) {
        RegistryEntry<Biome> biome = world.getBiome(new BlockPos(centerX, 62, centerZ));
        Random r = new Random(world.getSeed() ^ ((long) centerX * 341873128712L + (long) centerZ * 132897987541L));

        if (biome.matchesKey(SamsaraBiomes.TITAN_GRAVEYARD)) {
            // Sinh 4 - 6 mẩu xương hoá thạch và mỏm đá basalt nhô lên khỏi mặt nước cạn
            int count = 4 + r.nextInt(3);
            for (int i = 0; i < count; i++) {
                int ox = centerX + (r.nextInt(120) - 60);
                int oz = centerZ + (r.nextInt(120) - 60);
                if (Math.abs(ox - centerX) < 25 && Math.abs(oz - centerZ) < 25) continue;

                int height = 3 + r.nextInt(6);
                boolean isBone = r.nextBoolean();
                BlockPos.Mutable m = new BlockPos.Mutable();
                for (int dy = 0; dy <= height; dy++) {
                    int y = 60 + dy;
                    m.set(ox, y, oz);
                    if (isBone) {
                        world.setBlockState(m, Blocks.BONE_BLOCK.getDefaultState(), 2);
                        if (dy == 1 && r.nextBoolean()) world.setBlockState(m.east(), Blocks.BONE_BLOCK.getDefaultState(), 2);
                    } else {
                        world.setBlockState(m, (dy % 2 == 0) ? Blocks.SMOOTH_BASALT.getDefaultState() : Blocks.BASALT.getDefaultState(), 2);
                    }
                }
            }
        } else if (biome.matchesKey(SamsaraBiomes.SINGULARITY_SPIRES)) {
            // Sinh 4 - 6 cột thạch anh tím và crying obsidian nhô lên khỏi mặt nước cạn
            int count = 4 + r.nextInt(3);
            for (int i = 0; i < count; i++) {
                int ox = centerX + (r.nextInt(120) - 60);
                int oz = centerZ + (r.nextInt(120) - 60);
                if (Math.abs(ox - centerX) < 25 && Math.abs(oz - centerZ) < 25) continue;

                int height = 4 + r.nextInt(7);
                BlockPos.Mutable m = new BlockPos.Mutable();
                for (int dy = 0; dy <= height; dy++) {
                    int y = 60 + dy;
                    m.set(ox, y, oz);
                    if (dy == height) {
                        world.setBlockState(m, Blocks.AMETHYST_CLUSTER.getDefaultState(), 2);
                    } else if (dy > height - 2) {
                        world.setBlockState(m, Blocks.BUDDING_AMETHYST.getDefaultState(), 2);
                    } else if (dy % 3 == 0) {
                        world.setBlockState(m, Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);
                    } else {
                        world.setBlockState(m, Blocks.AMETHYST_BLOCK.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("samsara")
            .then(CommandManager.literal("womb")
                .then(CommandManager.literal("list")
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(() -> Text.literal("§6=== 20 Phế Tích Khổng Lồ trong The Womb ==="), false);
                        for (String r : ALL_RUINS) {
                            ctx.getSource().sendFeedback(() -> Text.literal("§7- §e" + r), false);
                        }
                        return 1;
                    })
                )
                .then(CommandManager.literal("nearest")
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                        ServerWorld world = (ServerWorld) player.getWorld();

                        if (!world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY)) {
                            ctx.getSource().sendError(Text.literal("Lệnh này chỉ dùng được trong The Womb."));
                            return 0;
                        }

                        BlockPos pPos = player.getBlockPos();
                        int pCellX = Math.floorDiv(pPos.getX(), CELL_SIZE);
                        int pCellZ = Math.floorDiv(pPos.getZ(), CELL_SIZE);

                        BlockPos nearestPos = null;
                        double minDistSq = Double.MAX_VALUE;
                        String nearestType = "";

                        for (int dx = -2; dx <= 2; dx++) {
                            for (int dz = -2; dz <= 2; dz++) {
                                int cX = pCellX + dx;
                                int cZ = pCellZ + dz;
                                BlockPos candidate = getCandidatePosition(world.getSeed(), cX, cZ);
                                double dSq = candidate.getSquaredDistance(pPos.getX(), candidate.getY(), pPos.getZ());
                                if (dSq < minDistSq) {
                                    minDistSq = dSq;
                                    nearestPos = candidate;
                                    nearestType = getRuinTypeForCell(world, cX, cZ);
                                }
                            }
                        }

                        if (nearestPos != null) {
                            int dist = (int) Math.sqrt(minDistSq);
                            BlockPos finalNearestPos = nearestPos;
                            String finalNearestType = nearestType;
                            ctx.getSource().sendFeedback(() -> Text.literal("§aPhế tích gần nhất: §e" + finalNearestType + " §atại [" + finalNearestPos.getX() + ", " + finalNearestPos.getZ() + "] (§b" + dist + " block§a)"), false);
                        }
                        return 1;
                    })
                )
                .then(CommandManager.literal("spawn")
                    .then(CommandManager.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String r : ALL_RUINS) builder.suggest(r);
                            builder.suggest("random");
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            ServerWorld world = (ServerWorld) player.getWorld();

                            if (!world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY)) {
                                ctx.getSource().sendError(Text.literal("Chỉ dựng được phế tích The Womb khi đang ở trong The Womb."));
                                return 0;
                            }

                            String type = StringArgumentType.getString(ctx, "type");
                            if (type.equalsIgnoreCase("random")) {
                                type = ALL_RUINS[new Random().nextInt(ALL_RUINS.length)];
                            }

                            BlockPos pos = player.getBlockPos();
                            boolean success = generateRuinByType(world, pos.getX(), pos.getY(), pos.getZ(), type);

                            if (success) {
                                String finalType = type;
                                ctx.getSource().sendFeedback(() -> Text.literal("§aĐã dựng §e" + finalType + " §atại vị trí hiện tại!"), false);
                                world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 2.0f, 0.5f);
                            } else {
                                ctx.getSource().sendError(Text.literal("Không có loại phế tích: " + type));
                            }
                            return success ? 1 : 0;
                        })
                    )
                )
            )
        );
    }

    public static WombRuinManager fromNbt(NbtCompound tag) {
        WombRuinManager manager = new WombRuinManager();
        NbtList list = tag.getList("GeneratedCells", 8);
        for (int i = 0; i < list.size(); i++) {
            manager.generatedCells.add(list.getString(i));
        }
        return manager;
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
