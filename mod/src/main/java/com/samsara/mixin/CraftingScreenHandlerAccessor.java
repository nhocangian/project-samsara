package com.samsara.mixin;

import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Tầng 4 "Đêm đầu tiên": biết bàn chế tạo nằm đâu (có trong vết rách không) và đọc lại ô chế tạo. */
@Mixin(CraftingScreenHandler.class)
public interface CraftingScreenHandlerAccessor {
    @Accessor("context")
    ScreenHandlerContext samsara$getContext();

    @Accessor("input")
    RecipeInputInventory samsara$getInput();
}
