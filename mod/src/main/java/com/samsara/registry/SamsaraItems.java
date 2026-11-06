package com.samsara.registry;

import com.samsara.item.FracturedSlateItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.util.Rarity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registry for Project: Samsara items.
 */
public class SamsaraItems {
    public static final Identifier FRACTURED_SLATE_ID = new Identifier("samsara", "fractured_slate");
    public static final Identifier PALIMPSEST_ID = new Identifier("samsara", "palimpsest_journal");
    public static final Identifier BATTERY_ID = new Identifier("samsara", "camcorder_battery");

    public static final Item FRACTURED_SLATE = new FracturedSlateItem(
            new FabricItemSettings()
                    .maxCount(1)
                    .rarity(Rarity.EPIC)
                    .fireproof()
    );

    public static final Item CAMCORDER_BATTERY = new com.samsara.item.CamcorderBatteryItem(
            new FabricItemSettings()
                    .maxCount(16)
                    .rarity(Rarity.UNCOMMON)
    );

    public static final Item PALIMPSEST_JOURNAL = new com.samsara.system.arg.PalimpsestJournalItem(
            new FabricItemSettings()
    );

    public static final Item CRADLE_ITEM = new BlockItem(
            SamsaraBlocks.CRADLE,
            new FabricItemSettings().rarity(Rarity.EPIC).fireproof()
    );

    public static final Item PRIMORDIAL_EGG_ITEM = new BlockItem(
            SamsaraBlocks.PRIMORDIAL_EGG,
            new FabricItemSettings().rarity(Rarity.EPIC).fireproof()
    );

    public static void registerItems() {
        Registry.register(Registries.ITEM, FRACTURED_SLATE_ID, FRACTURED_SLATE);
        Registry.register(Registries.ITEM, BATTERY_ID, CAMCORDER_BATTERY);
        Registry.register(Registries.ITEM, PALIMPSEST_ID, PALIMPSEST_JOURNAL);
        Registry.register(Registries.ITEM, SamsaraBlocks.CRADLE_ID, CRADLE_ITEM);
        Registry.register(Registries.ITEM, SamsaraBlocks.PRIMORDIAL_EGG_ID, PRIMORDIAL_EGG_ITEM);

        // Add to creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(FRACTURED_SLATE);
            entries.add(CAMCORDER_BATTERY);
            entries.add(PALIMPSEST_JOURNAL);
            entries.add(CRADLE_ITEM);
            entries.add(PRIMORDIAL_EGG_ITEM);
        });
    }
}
