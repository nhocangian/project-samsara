package com.samsara;

import com.samsara.registry.SamsaraBlockEntities;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.registry.SamsaraEntities;
import com.samsara.registry.SamsaraItems;
import com.samsara.registry.SamsaraSounds;
import com.samsara.system.additions.MetempsychosisManager;
import com.samsara.system.additions.OuroborosGlitchSystem;
import com.samsara.system.additions.PhantomAudioEchoSystem;
import com.samsara.system.echo.EchoResidueManager;
import com.samsara.system.karma.SinEventListener;
import com.samsara.system.narrative.SlateNarrativeHandler;
import com.samsara.worldgen.ColossalInvertedMonolithFeature;
import com.samsara.worldgen.CradleChamberFeature;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SamsaraMod implements ModInitializer {
    public static final String MOD_ID = "samsara";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Feature<DefaultFeatureConfig> CRADLE_CHAMBER_FEATURE = new CradleChamberFeature(DefaultFeatureConfig.CODEC);
    public static final Feature<DefaultFeatureConfig> COLOSSAL_MONOLITH_FEATURE = new ColossalInvertedMonolithFeature(DefaultFeatureConfig.CODEC);

    @Override
    public void onInitialize() {
        LOGGER.info("[Project: Samsara] Initializing cycle mechanisms...");

        // Registries
        SamsaraSounds.registerSounds();
        SamsaraBlocks.registerBlocks();
        SamsaraBlockEntities.registerBlockEntities();
        com.samsara.registry.SamsaraParticles.register();
        SamsaraItems.registerItems();
        SamsaraEntities.registerEntities();

        // Core Narrative & Horror Subsystems
        EchoResidueManager.register();
        SinEventListener.register();
        SlateNarrativeHandler.register();

        // 4 Psychological Strategy Additions
        com.samsara.system.sleep.OutOfBodySleepGlitch.register();
        com.samsara.system.karma.HostileHesitationSystem.register();
        com.samsara.system.karma.VillagerConfinementSystem.register();
        com.samsara.system.narrative.SilentConvergenceSystem.register();

        // ARG Ciphers & Metaphysical Enigmas
        com.samsara.system.arg.MorseAudioSignalSystem.register();
        com.samsara.system.arg.SelfRealizationLecternSystem.register();
        com.samsara.system.arg.AnvilMirrorCipherSystem.register();

        // Emergent Psychological Subsystems
        PhantomAudioEchoSystem.register();
        MetempsychosisManager.register();
        OuroborosGlitchSystem.register();

        // Worldgen Registration
        com.samsara.worldgen.SpawnCairnGenerator.register();
        Registry.register(Registries.FEATURE, new Identifier(MOD_ID, "cradle_chamber"), CRADLE_CHAMBER_FEATURE);
        Registry.register(Registries.FEATURE, new Identifier(MOD_ID, "colossal_monolith"), COLOSSAL_MONOLITH_FEATURE);
        Registry.register(Registries.FEATURE, new Identifier(MOD_ID, "sunflower_field"),
                new com.samsara.worldgen.memory.SunflowerFieldFeature(DefaultFeatureConfig.CODEC));

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_STRUCTURES,
                RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(MOD_ID, "cradle_chamber"))
        );

        // Megalithic Architectures & Supernatural Phenomena
        com.samsara.worldgen.megalith.MegalithManager.register();
        com.samsara.worldgen.ruins.ColossalRuinManager.register();
        com.samsara.worldgen.ruins.WombRuinManager.register();
        com.samsara.system.water.WombWaterWakeSystem.register();

        // The Womb ARG Features
        com.samsara.system.arg.WombPhantomSkiffManager.register();

        // Celestial Solar Fracture & Twin Moon System
        com.samsara.system.celestial.SolarFractureManager.register();

        // Egg Cycle: bẫy Phantom, máy quay do <System> cài vào Hotbar, dị tượng theo giai đoạn
        com.samsara.system.cycle.PhantomTrapSystem.register();
        com.samsara.system.cycle.CamcorderGiftSystem.register();
        com.samsara.system.cycle.StageAnomalySystem.register();
        com.samsara.system.cycle.WombTransitionSystem.register();
        com.samsara.system.cycle.CollapseAnomalySystem.register();
        com.samsara.system.cycle.NamGhostSystem.register();
        com.samsara.system.cycle.ReversedEchoSystem.register();
        com.samsara.system.memory.SunflowerGazeSystem.register();

        // Đoạn kết: Quả Trứng Nguyên Thủy & đối thoại với <System>
        com.samsara.system.ending.EndingServer.register();
        // World "Chu Kỳ #N" tạo sau màn kết: nhà Nam ở điểm spawn, tiêu đề chu kỳ
        com.samsara.system.cycle.NewCycleSystem.register();

        // Kinh dị tầng giao diện: Nam_1998 vào game, thành tựu giả, tên thế giới tự đổi
        com.samsara.system.ui.UiHorrorSystem.register();

        // Tuyến hành hương The Womb: dòng chảy, thuyền ở Bến Đò
        com.samsara.system.womb.WombCurrentSystem.register();
        com.samsara.system.womb.chapters.DockDrift.register();
        com.samsara.system.womb.WanderChaseSystem.register();
        com.samsara.system.womb.WombPacingSystem.register();

        // Project: Samsara Test & Debug Suite
        com.samsara.system.tangduoi.DuongOngRuntime.register();
        com.samsara.system.tangduoi.OKhoaHam.register();
        com.samsara.system.cycle.CamcorderKeepSystem.register();
        com.samsara.command.SamsaraTestCommands.register();

        LOGGER.info("[Project: Samsara] The cycle is primed. Awaken.");
    }
}
