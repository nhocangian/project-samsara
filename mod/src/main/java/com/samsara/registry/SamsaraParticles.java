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

    /** Cánh hoa hướng dương trôi trên mặt nước ở Tầng Dưới, vẽ bởi client/tangduoi/CanhHoaParticle. */
    public static final DefaultParticleType CANH_HOA = FabricParticleTypes.simple(true);

    /** Bọ bay quanh đèn ở Tầng Dưới, vẽ bởi client/tangduoi/ConBoParticle. */
    public static final DefaultParticleType CON_BO = FabricParticleTypes.simple(true);

    /** Giọt nước xám rỉ từ trần Tầng Dưới, vẽ bởi client/tangduoi/GiotNuocParticle. */
    public static final DefaultParticleType GIOT_NUOC = FabricParticleTypes.simple(true);

    /** Vòng nước loang ở chỗ giọt nước rơi (Tầng Dưới), vẽ bởi client/tangduoi/VongNuocParticle. */
    public static final DefaultParticleType VONG_NUOC = FabricParticleTypes.simple(true);

    public static void register() {
        Registry.register(Registries.PARTICLE_TYPE, new Identifier("samsara", "vong_nuoc"), VONG_NUOC);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier("samsara", "giot_nuoc"), GIOT_NUOC);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier("samsara", "con_bo"), CON_BO);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier("samsara", "canh_hoa"), CANH_HOA);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier("samsara", "rune"), RUNE);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier("samsara", "womb_mote"), MOTE);
    }
}
