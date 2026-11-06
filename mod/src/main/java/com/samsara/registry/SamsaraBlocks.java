package com.samsara.registry;

import com.samsara.block.CradleBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registry for Project: Samsara blocks.
 */
public class SamsaraBlocks {
    public static final Identifier CRADLE_ID = new Identifier("samsara", "cradle");
    public static final Identifier PRIMORDIAL_EGG_ID = new Identifier("samsara", "primordial_egg");

    // The Cradle block: Unbreakable, high blast resistance, light emission when active
    public static final Block CRADLE = new CradleBlock(
            FabricBlockSettings.copyOf(Blocks.BEDROCK)
                    .strength(-1.0f, 3600000.0f)
                    .luminance(state -> state.get(CradleBlock.ACTIVE) ? 12 : 3)
                    .emissiveLighting((state, world, pos) -> state.get(CradleBlock.ACTIVE))
    );

    // The Primordial Egg: Monolithic altar in The Womb
    public static final Block PRIMORDIAL_EGG = new com.samsara.block.PrimordialEggBlock(
            FabricBlockSettings.copyOf(Blocks.CRYING_OBSIDIAN)
                    .strength(-1.0f, 3600000.0f)
                    .luminance(state -> state.get(com.samsara.block.PrimordialEggBlock.HATCHING) ? 15 : 8)
                    .emissiveLighting((state, world, pos) -> true)
    );

    public static final Identifier MEMORY_SUNFLOWER_ID = new Identifier("samsara", "memory_sunflower");

    // Hoa hướng dương xoay được hướng của chiều Ký Ức Của Nam (không có item riêng)
    public static final Block MEMORY_SUNFLOWER = new com.samsara.block.MemorySunflowerBlock(
            FabricBlockSettings.copyOf(Blocks.SUNFLOWER)
    );

    public static void registerBlocks() {
        Registry.register(Registries.BLOCK, MEMORY_SUNFLOWER_ID, MEMORY_SUNFLOWER);
        Registry.register(Registries.BLOCK, CRADLE_ID, CRADLE);
        Registry.register(Registries.BLOCK, PRIMORDIAL_EGG_ID, PRIMORDIAL_EGG);
    }
}
