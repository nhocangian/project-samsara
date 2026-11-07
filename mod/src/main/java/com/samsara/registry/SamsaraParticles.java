package com.samsara.registry;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Hạt riêng của Project Samsara. */
public final class SamsaraParticles {
    private SamsaraParticles() {}

    /** Ký tự phù phép đỏ phát sáng bay lên (chương III của The Womb), vẽ bởi client/womb/RuneParticle. */
    public static final DefaultParticleType RUNE = FabricParticleTypes.simple(true);

    /** Bụi vàng kem bị hút về Gargantua (ngoài căn phòng cửa), vẽ bởi client/womb/MoteParticle. */
    public static final DefaultParticleType MOTE = FabricParticleTypes.simple(true);

    public static void register() {
        Registry.register(Registries.PARTICLE_TYPE, new Identifier("samsara", "rune"), RUNE);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier("samsara", "womb_mote"), MOTE);
    }
}
