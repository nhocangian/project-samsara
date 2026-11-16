package com.samsara;

import com.samsara.registry.SamsaraBlockEntities;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.registry.SamsaraEntities;
import com.samsara.registry.SamsaraItems;
import com.samsara.registry.SamsaraSounds;
import com.samsara.system.additions.MetempsychosisManager;
import com.samsara.system.additions.PhantomAudioEchoSystem;
import com.samsara.system.echo.EchoResidueManager;
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

        // 4 Psychological Strategy Additions
        com.samsara.system.sleep.OutOfBodySleepGlitch.register();
        com.samsara.system.cycle.HostileHesitationSystem.register();
        // Overworld "ngờ ngợ": dấu vết người cũ quanh spawn, hoa quay theo người, con vật nhìn
        com.samsara.system.overworld.NamBase.register();
        com.samsara.system.overworld.NamTraces.register();
        com.samsara.system.overworld.OverworldSunflowerGaze.register();
        com.samsara.system.overworld.AnimalStare.register();
        com.samsara.system.overworld.SelfSign.register();
        com.samsara.system.overworld.VersionGlitch.register();
        // Hang động: tiếng cuốc đáp lại, đuốc ở phía trước, đường hầm nhánh của người khác
        com.samsara.system.overworld.CavePickEcho.register();
        com.samsara.system.overworld.CaveTorchAhead.register();
        com.samsara.system.overworld.CaveBranchTunnel.register();
        // Phím đạo diễn: gọi các hiện tượng trên đúng lúc quay video, không để lại dấu vết trên khung hình
        com.samsara.system.overworld.DirectorCues.register();
        com.samsara.system.overworld.FilmingSetup.register();

        // ARG Ciphers & Metaphysical Enigmas
        com.samsara.system.arg.SelfRealizationLecternSystem.register();

        // Emergent Psychological Subsystems
        PhantomAudioEchoSystem.register();
        MetempsychosisManager.register();

        // Worldgen Registration
        com.samsara.worldgen.SpawnCairnGenerator.register();
        Registry.register(Registries.FEATURE, new Identifier(MOD_ID, "sunflower_field"),
                new com.samsara.worldgen.memory.SunflowerFieldFeature(DefaultFeatureConfig.CODEC));

        // Megalithic Architectures & Supernatural Phenomena
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
        com.samsara.system.tangduoi.LoiDiNhapRuntime.register();
        com.samsara.system.trontim.TronTimRuntime.register();
        com.samsara.system.demdautien.DemDauTienRuntime.register();
        com.samsara.system.tangduoi.OKhoaHam.register();
        com.samsara.system.cycle.CamcorderKeepSystem.register();
        com.samsara.command.SamsaraTestCommands.register();

        LOGGER.info("[Project: Samsara] The cycle is primed. Awaken.");
    }
}
