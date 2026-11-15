package com.samsara.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registry for Project: Samsara Block Entities.
 */
public class SamsaraBlockEntities {
    public static BlockEntityType<com.samsara.block.entity.MemoryDoorGiantBlockEntity> MEMORY_DOOR_GIANT_BE;

    public static BlockEntityType<com.samsara.block.entity.TitanHaloBlockEntity> TITAN_HALO_BE;

    public static BlockEntityType<com.samsara.block.entity.KhoiNhapBlockEntity> KHOI_NHAP_BE;

    public static void registerBlockEntities() {
        KHOI_NHAP_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("samsara", "khoi_nhap"),
                FabricBlockEntityTypeBuilder.create(com.samsara.block.entity.KhoiNhapBlockEntity::new, SamsaraBlocks.KHOI_NHAP, SamsaraBlocks.VAN_GIA).build());
        TITAN_HALO_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("samsara", "titan_halo"),
                FabricBlockEntityTypeBuilder.create(com.samsara.block.entity.TitanHaloBlockEntity::new, SamsaraBlocks.TITAN_HALO).build());
        MEMORY_DOOR_GIANT_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("samsara", "memory_door_giant"),
                FabricBlockEntityTypeBuilder.create(com.samsara.block.entity.MemoryDoorGiantBlockEntity::new, SamsaraBlocks.MEMORY_DOOR_GIANT).build());
    }
}
