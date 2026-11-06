package com.samsara.registry;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/**
 * Registry keys for Project: Samsara custom biomes in The Womb:
 * 1. Origin Cradle (Vùng Nôi Nguyên Thủy - Biển cạn 1 block, bụi hư không bay ngược về Hắc Lỗ)
 * 2. Titan Graveyard (Bãi Tha Ma Cự Thần - Biển sâu, tro bụi u tối, tàn tích xương thần)
 * 3. Singularity Spires (Rạn Pha Lê Dị Thường - Biển sâu, tinh thể tím phát quang chọc trời)
 */
public class SamsaraBiomes {
    public static final Identifier ORIGIN_CRADLE_ID = new Identifier("samsara", "origin_cradle");
    public static final Identifier TITAN_GRAVEYARD_ID = new Identifier("samsara", "titan_graveyard");
    public static final Identifier SINGULARITY_SPIRES_ID = new Identifier("samsara", "singularity_spires");
    public static final Identifier THE_WOMB_ID = new Identifier("samsara", "the_womb");

    public static final RegistryKey<Biome> ORIGIN_CRADLE = RegistryKey.of(RegistryKeys.BIOME, ORIGIN_CRADLE_ID);
    public static final RegistryKey<Biome> TITAN_GRAVEYARD = RegistryKey.of(RegistryKeys.BIOME, TITAN_GRAVEYARD_ID);
    public static final RegistryKey<Biome> SINGULARITY_SPIRES = RegistryKey.of(RegistryKeys.BIOME, SINGULARITY_SPIRES_ID);
    public static final RegistryKey<Biome> THE_WOMB = RegistryKey.of(RegistryKeys.BIOME, THE_WOMB_ID);
}
