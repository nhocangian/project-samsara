package com.samsara.client.ending;

import com.samsara.SamsaraMod;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.cycle.NewCycleSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.GameRules;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelStorage;

/**
 * Sau màn NO SIGNAL: "Đằng sau nút 'Lưu & Thoát'... luôn luôn là nút 'Tạo thế giới mới'."
 * Ghi lại thiết lập của world vừa chơi (chế độ, độ khó, cheat, datapack) trước khi thoát, rồi tạo ngay
 * world mới "Chu Kỳ #N+1" với seed ngẫu nhiên. World cũ chỉ được lưu và đóng, không bị sửa.
 * Nhà Nam ở điểm spawn do NewCycleSystem (phía server) dựng.
 */
@Environment(EnvType.CLIENT)
public final class NewCycleClient {
    private NewCycleClient() {}

    public record Plan(int cycle, LevelInfo oldInfo, DataConfiguration dataConfig) {}

    /** Gọi TRƯỚC khi thoát world (cần server tích hợp còn chạy). Null nếu không phải chơi đơn. */
    public static Plan capture(MinecraftClient client) {
        IntegratedServer server = client.getServer();
        if (server == null) return null;
        try {
            SaveProperties props = server.getSaveProperties();
            int cycle = EggCycleState.get(server).getCycle() + 1;
            return new Plan(cycle, props.getLevelInfo(), props.getDataConfiguration());
        } catch (Exception e) {
            SamsaraMod.LOGGER.warn("[Samsara] Không đọc được thiết lập world để tạo chu kỳ mới", e);
            return null;
        }
    }

    /** Gọi SAU khi đã thoát hẳn world cũ. Trả về false nếu không tạo được (khi đó về menu chính như cũ). */
    public static boolean start(MinecraftClient client, Plan plan) {
        try {
            LevelStorage storage = client.getLevelStorage();
            String name = I18n.translate("samsara.new_cycle.world_name", plan.cycle());
            String dir = "Chu Ky " + plan.cycle();
            for (int i = 2; storage.levelExists(dir); i++) {
                dir = "Chu Ky " + plan.cycle() + " (" + i + ")";
            }
            LevelInfo old = plan.oldInfo();
            LevelInfo info = new LevelInfo(name, old.getGameMode(), old.isHardcore(), old.getDifficulty(),
                    old.areCommandsAllowed(), new GameRules(), plan.dataConfig());
            GeneratorOptions generator = new GeneratorOptions(GeneratorOptions.getRandomSeed(), true, false);

            NewCycleSystem.pendingCycle = plan.cycle();
            client.createIntegratedServerLoader().createAndStart(dir, info, generator,
                    registries -> registries.get(RegistryKeys.WORLD_PRESET).entryOf(WorldPresets.DEFAULT)
                            .value().createDimensionsRegistryHolder());
            return true;
        } catch (Exception e) {
            NewCycleSystem.pendingCycle = 0;
            SamsaraMod.LOGGER.error("[Samsara] Không tạo được world chu kỳ mới", e);
            return false;
        }
    }
}
