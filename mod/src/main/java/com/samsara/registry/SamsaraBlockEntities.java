package com.samsara.registry;

import com.samsara.block.entity.CradleBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registry for Project: Samsara Block Entities.
 */
public class SamsaraBlockEntities {
    public static final Identifier CRADLE_BE_ID = new Identifier("samsara", "cradle_block_entity");
    public static final Identifier PRIMORDIAL_EGG_BE_ID = new Identifier("samsara", "primordial_egg_block_entity");

    public static BlockEntityType<CradleBlockEntity> CRADLE_BLOCK_ENTITY;
    public static BlockEntityType<com.samsara.block.entity.PrimordialEggBlockEntity> PRIMORDIAL_EGG_BE;

    public static void registerBlockEntities() {
        CRADLE_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                CRADLE_BE_ID,
                FabricBlockEntityTypeBuilder.create(CradleBlockEntity::new, SamsaraBlocks.CRADLE).build()
        );

        PRIMORDIAL_EGG_BE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                PRIMORDIAL_EGG_BE_ID,
                FabricBlockEntityTypeBuilder.create(com.samsara.block.entity.PrimordialEggBlockEntity::new, SamsaraBlocks.PRIMORDIAL_EGG).build()
        );
    }
}
