package com.samsara.registry;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

/**
 * Registry keys for Project: Samsara custom dimensions.
 * "The Womb" (Cõi Thai Nghén) is the liminal cosmic realm of obsidian plains and auroras.
 */
public class SamsaraDimensions {
    public static final Identifier THE_WOMB_ID = new Identifier("samsara", "the_womb");
    /** "Ký Ức Của Nam": cánh đồng hướng dương phẳng, nơi đến đầu tiên sau giấc ngủ thứ 7. */
    public static final Identifier NAM_MEMORY_ID = new Identifier("samsara", "nam_memory");

    public static final RegistryKey<World> NAM_MEMORY_WORLD_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            NAM_MEMORY_ID
    );

    public static final RegistryKey<World> THE_WOMB_WORLD_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            THE_WOMB_ID
    );

    public static final RegistryKey<DimensionType> THE_WOMB_DIM_TYPE_KEY = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            THE_WOMB_ID
    );


}
