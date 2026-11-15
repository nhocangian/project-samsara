package com.samsara.registry;

import com.samsara.item.CamcorderItem;
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
    public static final Identifier CAMCORDER_ID = new Identifier("samsara", "camcorder");
    public static final Identifier PALIMPSEST_ID = new Identifier("samsara", "palimpsest_journal");
    public static final Identifier BATTERY_ID = new Identifier("samsara", "camcorder_battery");

    public static final Item CAMCORDER = new CamcorderItem(
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

    public static void registerItems() {
        Registry.register(Registries.ITEM, CAMCORDER_ID, CAMCORDER);
        Registry.register(Registries.ITEM, BATTERY_ID, CAMCORDER_BATTERY);
        Registry.register(Registries.ITEM, PALIMPSEST_ID, PALIMPSEST_JOURNAL);

        // Add to creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(CAMCORDER);
            entries.add(CAMCORDER_BATTERY);
            entries.add(PALIMPSEST_JOURNAL);
        });
    }
}
