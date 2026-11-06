package com.samsara.worldgen.megalith;

import com.mojang.brigadier.CommandDispatcher;
import com.samsara.worldgen.ColossalInvertedMonolithFeature;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Manager for Colossal Megalithic Architectures & Supernatural Phenomena.
 * Registers subcommands:
 *  - /samsara structure monolith (The 350-block Inverted Wedge v2)
 *  - /samsara structure chains   (The Heaven-Binding Chains - Image 1)
 *  - /samsara structure arch     (The Megalithic Archway - Image 5)
 *  - /samsara structure titan    (The Pale Screaming Colossus - Image 3)
 *  - /samsara structure hand     (The Fossilized Crawling Hand - Image 2)
 */
public class MegalithManager {

    public static void register() {
        CommandRegistrationCallback.EVENT.register(MegalithManager::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                         CommandRegistryAccess registryAccess,
                                         CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("samsara")
                .then(CommandManager.literal("structure")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("monolith")
                                .executes(ctx -> spawnMegalith(ctx.getSource(), "monolith")))
                        .then(CommandManager.literal("chains")
                                .executes(ctx -> spawnMegalith(ctx.getSource(), "chains")))
                        .then(CommandManager.literal("arch")
                                .executes(ctx -> spawnMegalith(ctx.getSource(), "arch")))
                        .then(CommandManager.literal("titan")
                                .executes(ctx -> spawnMegalith(ctx.getSource(), "titan")))
                        .then(CommandManager.literal("hand")
                                .executes(ctx -> spawnMegalith(ctx.getSource(), "hand")))
                )
                // Giữ alias cũ /samsara monolith
                .then(CommandManager.literal("monolith")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ctx -> spawnMegalith(ctx.getSource(), "monolith")))
        );
    }

    private static int spawnMegalith(ServerCommandSource source, String type) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        BlockPos pos = player.getBlockPos();
        var world = player.getServerWorld();

        // Âm thanh rung chuyển mặt đất tần số cực trầm
        world.playSound(null, pos, SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.AMBIENT, 10.0f, 0.5f);
        world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.AMBIENT, 8.0f, 0.4f);

        switch (type.toLowerCase()) {
            case "monolith" -> {
                source.sendFeedback(() -> Text.literal("§8§o[Bầu trời rên rỉ khi một cự thần cao 350 block chìm dần xuống vực đại dương...]§r"), true);
                ColossalInvertedMonolithFeature.generateMonolithStructure(world, pos.getX(), pos.getZ());
            }
            case "chains" -> {
                source.sendFeedback(() -> Text.literal("§8§o[Những sợi xích khổng lồ xuyên qua tầng bình lưu, trói trời vào đất...]§r"), true);
                HeavenBindingChainsFeature.generate(world, pos.getX(), pos.getZ());
            }
            case "arch" -> {
                source.sendFeedback(() -> Text.literal("§8§o[Một cổng vòm thô ráp với tỉ lệ bất khả cong vút trên những tầng mây...]§r"), true);
                MegalithicArchFeature.generate(world, pos.getX(), pos.getZ());
            }
            case "titan" -> {
                source.sendFeedback(() -> Text.literal("§8§o[Một cự thần trắng hoá đá đâm thẳng lên trời, miệng há ra trong tiếng thét câm lặng...]§r"), true);
                PetrifiedTitanFeature.generate(world, pos.getX(), pos.getZ());
            }
            case "hand" -> {
                source.sendFeedback(() -> Text.literal("§8§o[Bàn tay hoá thạch của một đấng sáng tạo bị lãng quên cào lên từ mặt đất rỉ máu...]§r"), true);
                FossilizedHandFeature.generate(world, pos.getX(), pos.getZ());
            }
        }

        return 1;
    }
}
