package com.samsara.registry;

import com.samsara.entity.EchoDummyEntity;
import com.samsara.entity.TheWandererEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registry for Project: Samsara entities.
 */
public class SamsaraEntities {
    public static final Identifier THE_WANDERER_ID = new Identifier("samsara", "the_wanderer");
    public static final EntityType<TheWandererEntity> THE_WANDERER = Registry.register(
            Registries.ENTITY_TYPE,
            THE_WANDERER_ID,
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, TheWandererEntity::new)
                    .dimensions(EntityDimensions.fixed(0.7f, 2.5f))
                    .fireImmune()
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(1)
                    .build()
    );

    public static final Identifier ECHO_DUMMY_ID = new Identifier("samsara", "echo_dummy");
    public static final EntityType<EchoDummyEntity> ECHO_DUMMY = Registry.register(
            Registries.ENTITY_TYPE,
            ECHO_DUMMY_ID,
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, EchoDummyEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(1)
                    .build()
    );

    public static final Identifier DIMENSIONAL_RIFT_ID = new Identifier("samsara", "dimensional_rift");
    public static final EntityType<com.samsara.entity.DimensionalRiftEntity> DIMENSIONAL_RIFT = Registry.register(
            Registries.ENTITY_TYPE,
            DIMENSIONAL_RIFT_ID,
            FabricEntityTypeBuilder.<com.samsara.entity.DimensionalRiftEntity>create(SpawnGroup.MISC, com.samsara.entity.DimensionalRiftEntity::new)
                    .dimensions(EntityDimensions.fixed(3.0f, 3.0f))
                    .fireImmune()
                    .trackRangeBlocks(128)
                    .trackedUpdateRate(1)
                    .build()
    );

    public static final Identifier PRIMORDIAL_EGG_ID = new Identifier("samsara", "primordial_egg");
    public static final EntityType<com.samsara.entity.PrimordialEggEntity> PRIMORDIAL_EGG = Registry.register(
            Registries.ENTITY_TYPE,
            PRIMORDIAL_EGG_ID,
            FabricEntityTypeBuilder.<com.samsara.entity.PrimordialEggEntity>create(SpawnGroup.MISC, com.samsara.entity.PrimordialEggEntity::new)
                    .dimensions(EntityDimensions.fixed(3.5f, 6.0f))
                    .fireImmune()
                    .trackRangeBlocks(256)
                    .trackedUpdateRate(20)
                    .build()
    );

    public static void registerEntities() {
        FabricDefaultAttributeRegistry.register(THE_WANDERER, TheWandererEntity.createWandererAttributes());
        FabricDefaultAttributeRegistry.register(ECHO_DUMMY, EchoDummyEntity.createDummyAttributes());
    }
}
