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

    // Cửa Ký Ức (chương II của The Womb): ba màu phấn như biển cửa trong tham chiếu, không item, không phá được
    public static final Block MEMORY_DOOR_PINK = memoryDoor();
    public static final Block MEMORY_DOOR_BLUE = memoryDoor();
    public static final Block MEMORY_DOOR_WHITE = memoryDoor();

    /** Cánh cửa trắng khổng lồ hé mở ở cuối hành lang cửa (vẽ bằng MemoryDoorGiantRenderer). */
    public static final Block MEMORY_DOOR_GIANT = new com.samsara.block.MemoryDoorGiantBlock(
            FabricBlockSettings.create().strength(-1.0f, 3600000.0f).noCollision().nonOpaque().dropsNothing()
                    .luminance(state -> 15));

    /** Vách Hư Không: tường và trần đen tuyền của căn phòng bao Bến Đò và biển cửa (chặn cả ánh trời). */
    public static final Block WOMB_VEIL = new Block(FabricBlockSettings.copyOf(Blocks.BEDROCK)
            .strength(-1.0f, 3600000.0f).dropsNothing().mapColor(net.minecraft.block.MapColor.BLACK));

    private static Block memoryDoor() {
        return new com.samsara.block.MemoryDoorBlock(FabricBlockSettings.create()
                .strength(-1.0f, 3600000.0f).nonOpaque().dropsNothing()
                .sounds(net.minecraft.sound.BlockSoundGroup.WOOD));
    }

    public static void registerBlocks() {
        Registry.register(Registries.BLOCK, new Identifier("samsara", "memory_door_pink"), MEMORY_DOOR_PINK);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "memory_door_blue"), MEMORY_DOOR_BLUE);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "memory_door_white"), MEMORY_DOOR_WHITE);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "memory_door_giant"), MEMORY_DOOR_GIANT);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "womb_veil"), WOMB_VEIL);
        Registry.register(Registries.BLOCK, MEMORY_SUNFLOWER_ID, MEMORY_SUNFLOWER);
        Registry.register(Registries.BLOCK, CRADLE_ID, CRADLE);
        Registry.register(Registries.BLOCK, PRIMORDIAL_EGG_ID, PRIMORDIAL_EGG);
    }
}
