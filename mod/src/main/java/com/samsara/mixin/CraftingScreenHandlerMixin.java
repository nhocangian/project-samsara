package com.samsara.mixin;

import com.samsara.system.demdautien.DemDauTienClock;
import com.samsara.system.demdautien.DemDauTienRuntime;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Tầng 4 "Đêm đầu tiên": thời Alpha chưa có giường. Công thức giường chỉ chạy ở bàn chế tạo đang nằm trong vết rách
 * (chỗ đó là bản 1.20.1); ngoài ra ô kết quả để trống (client hiện tooltip "chưa có trong phiên bản này").
 */
@Mixin(CraftingScreenHandler.class)
public class CraftingScreenHandlerMixin {
    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private static void samsara$alphaBed(ScreenHandler handler, World world, PlayerEntity player,
                                         RecipeInputInventory craftingInventory, CraftingResultInventory resultInventory,
                                         CallbackInfo ci) {
        if (world.isClient || !DemDauTienClock.isFloor(world) || world.getServer() == null) return;
        Optional<CraftingRecipe> recipe = world.getServer().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, craftingInventory, world);
        if (recipe.isEmpty() || !recipe.get().getOutput(world.getRegistryManager()).isIn(ItemTags.BEDS)) return;
        if (handler instanceof CraftingScreenHandlerAccessor acc) {
            BlockPos table = acc.samsara$getContext().get((w, pos) -> pos).orElse(null);
            if (table != null && DemDauTienRuntime.tableInTear(world, table)) return;
        }
        resultInventory.setStack(0, ItemStack.EMPTY);
        handler.setPreviousTrackedSlot(0, ItemStack.EMPTY);
        if (player instanceof ServerPlayerEntity sp) {
            sp.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 0, ItemStack.EMPTY));
        }
        ci.cancel();
    }
}
