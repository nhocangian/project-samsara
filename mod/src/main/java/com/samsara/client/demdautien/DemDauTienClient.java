package com.samsara.client.demdautien;

import com.samsara.mixin.CraftingScreenHandlerAccessor;
import com.samsara.mixin.HandledScreenAccessor;
import com.samsara.system.demdautien.DemDauTienClock;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.Optional;

/**
 * Tầng 4 "Đêm đầu tiên", phía client: nhận đồng hồ của tầng; ở bàn chế tạo ngoài vết rách, xếp đúng công thức giường
 * mà ô kết quả trống thì rê chuột vào hiện "chưa có trong phiên bản này".
 */
public final class DemDauTienClient {
    private DemDauTienClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DemDauTienClock.PACKET, (client, handler, buf, sender) -> {
            long anchor = buf.readLong();
            long start = buf.readLong();
            client.execute(() -> DemDauTienClock.setClient(anchor, start));
        });

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof CraftingScreen crafting)) return;
            ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, delta) -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.world == null || !DemDauTienClock.isFloor(mc.world)) return;
                Slot slot = ((HandledScreenAccessor) crafting).samsara$getFocusedSlot();
                if (slot == null || !(slot.inventory instanceof CraftingResultInventory) || slot.hasStack()) return;
                var input = ((CraftingScreenHandlerAccessor) crafting.getScreenHandler()).samsara$getInput();
                Optional<CraftingRecipe> recipe = mc.world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, input, mc.world);
                if (recipe.isEmpty() || !recipe.get().getOutput(mc.world.getRegistryManager()).isIn(ItemTags.BEDS)) return;
                ctx.drawTooltip(mc.textRenderer, Text.translatable("samsara.dem_dau_tien.chua_co"), mouseX, mouseY);
            });
        });
    }
}
