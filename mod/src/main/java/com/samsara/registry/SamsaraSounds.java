package com.samsara.registry;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Registry for all Project: Samsara custom sound events.
 */
public class SamsaraSounds {
    public static final Identifier CAVE_REVERSED_ID = new Identifier("samsara", "ambient.cave_reversed");
    public static final SoundEvent CAVE_REVERSED = SoundEvent.of(CAVE_REVERSED_ID);

    public static final Identifier SLATE_WHISPER_ID = new Identifier("samsara", "item.slate_whisper");
    public static final SoundEvent SLATE_WHISPER = SoundEvent.of(SLATE_WHISPER_ID);

    public static final Identifier CRADLE_HUM_ID = new Identifier("samsara", "block.cradle_hum");
    public static final SoundEvent CRADLE_HUM = SoundEvent.of(CRADLE_HUM_ID);

    public static final Identifier PHANTOM_REVERSED_ID = new Identifier("samsara", "entity.phantom_reversed");
    public static final SoundEvent PHANTOM_REVERSED = SoundEvent.of(PHANTOM_REVERSED_ID);

    public static final Identifier SYSTEM_STATIC_ID = new Identifier("samsara", "item.system_static");
    public static final SoundEvent SYSTEM_STATIC = SoundEvent.of(SYSTEM_STATIC_ID);

    public static void registerSounds() {
        Registry.register(Registries.SOUND_EVENT, PHANTOM_REVERSED_ID, PHANTOM_REVERSED);
        Registry.register(Registries.SOUND_EVENT, SYSTEM_STATIC_ID, SYSTEM_STATIC);
        Registry.register(Registries.SOUND_EVENT, CAVE_REVERSED_ID, CAVE_REVERSED);
        Registry.register(Registries.SOUND_EVENT, SLATE_WHISPER_ID, SLATE_WHISPER);
        Registry.register(Registries.SOUND_EVENT, CRADLE_HUM_ID, CRADLE_HUM);
    }
}
