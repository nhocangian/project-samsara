package com.samsara;

import com.samsara.client.AuroraSkyRenderer;
import com.samsara.entity.client.EchoDummyRenderer;
import com.samsara.entity.client.TheWandererRenderer;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.registry.SamsaraEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

@Environment(EnvType.CLIENT)
public class SamsaraClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Hoa hướng dương ký ức: cắt alpha như hoa vanilla
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                com.samsara.registry.SamsaraBlocks.MEMORY_SUNFLOWER, net.minecraft.client.render.RenderLayer.getCutout());

        // Register custom entity renderers
        EntityRendererRegistry.register(SamsaraEntities.THE_WANDERER, TheWandererRenderer::new);
        EntityRendererRegistry.register(SamsaraEntities.ECHO_DUMMY, EchoDummyRenderer::new);
        EntityRendererRegistry.register(SamsaraEntities.DIMENSIONAL_RIFT, com.samsara.entity.client.DimensionalRiftRenderer::new);

        // Register custom aurora skybox for The Womb dimension (when shaders are off)
        DimensionRenderingRegistry.registerSkyRenderer(SamsaraDimensions.THE_WOMB_WORLD_KEY, new AuroraSkyRenderer());

        // Register custom atmospheric post-vignette overlay for The Womb
        com.samsara.client.WombAtmosphereOverlay.register();

        // Register client-side water wake ribbon renderer (Phong cách Wakes Ribbon & Oar Ripples)
        com.samsara.client.WombWaterWakeRibbonRenderer.register();

        // Register client-side cosmic dust reverse drift towards Gargantua black hole in Origin Cradle
        com.samsara.client.WombCradleDriftSystem.register();

        // Register client-side water wake system for swimming entities in The Womb
        com.samsara.client.WombClientWaterWakeSystem.register();

        // Receive the Solar Fracture (black-hole sun) stage from the server
        com.samsara.client.SolarStageClientSync.register();

        // Dị tượng phía client theo giai đoạn (nhạc giật, nước xám bạc, báo trạng thái IR)
        com.samsara.client.StageAnomalyClient.register();

        // Hiệu ứng không gian kéo dãn khi bị hút vào The Womb (giấc ngủ thứ 7)
        com.samsara.client.WombTransitionClient.register();

        // Đoạn kết: Quả Trứng Nguyên Thủy, cinematic, bóng phản chiếu, đối thoại <System>, NO SIGNAL
        EntityRendererRegistry.register(SamsaraEntities.PRIMORDIAL_EGG, com.samsara.entity.client.PrimordialEggRenderer::new);
        com.samsara.client.ending.EndingSequenceClient.register();

        // Tuyến hành hương: dòng chảy đẩy thuyền, thuyền bóng ma chèo song song (qua máy quay)
        com.samsara.client.WombRouteClient.register();
        com.samsara.client.WombAmbienceClient.register();

        // Kinh dị tầng giao diện: toast thành tựu giả, menu tạm dừng bị nhiễu
        com.samsara.client.ui.UiHorrorClient.register();
        // Ảnh chụp F2 thấy mặt trời thật (hố đen theo stage)
        com.samsara.client.ScreenshotSunClient.register();
        // Cánh cửa trắng khổng lồ cuối hành lang cửa (chương II của The Womb)
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.samsara.registry.SamsaraBlockEntities.MEMORY_DOOR_GIANT_BE, ctx -> new com.samsara.client.womb.MemoryDoorGiantRenderer());

        // Phantom qua ống kính máy quay: "Tàn Hồn Phantom" (mắt thường vẫn thấy Phantom vanilla)
        EntityRendererRegistry.register(net.minecraft.entity.EntityType.PHANTOM, com.samsara.entity.client.SoulPhantomRenderer::new);

        // Register Outlast Camcorder aim, night vision, and HUD overlay
        com.samsara.client.CamcorderClientManager.init();
        // Máy quay 3D (LCD lật, đèn REC, nắp băng) cho mọi chế độ hiển thị item
        net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(
                com.samsara.registry.SamsaraItems.FRACTURED_SLATE, new com.samsara.client.camcorder.CamcorderItemRenderer());
        com.samsara.client.CamcorderOverlay.register();
    }
}
