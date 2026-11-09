package com.samsara.client.tangduoi;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/** Giọt nước xám trong rỉ từ trần (Tầng Dưới): đọng lại một lúc rồi rơi; vận tốc X của gói hạt là độ cao mặt nước. */
public class GiotNuocParticle extends SpriteBillboardParticle {
    private final double surface;
    private final int hang;

    protected GiotNuocParticle(ClientWorld world, double x, double y, double z, double surface, SpriteProvider sprites) {
        super(world, x, y, z);
        setSprite(sprites.getSprite(random));
        this.surface = surface;
        hang = 25 + random.nextInt(35);
        maxAge = 200;
        scale = 0.02f;
        red = 0.72f;
        green = 0.75f;
        blue = 0.74f;
        alpha = 0.55f;
        gravityStrength = 0.0f;
        collidesWithWorld = false;
        velocityX = velocityY = velocityZ = 0.0;
    }

    @Override
    public void tick() {
        if (age < hang) {
            // Đọng ở trần: vệt ngắn, lớn dần
            scale = 0.02f + 0.05f * age / hang;
        } else {
            // Rơi: vệt dài ra theo tốc độ
            gravityStrength = 0.7f;
            scale = Math.min(0.28f, scale + 0.03f);
        }
        super.tick();
        if (y <= surface) {
            world.addParticle(com.samsara.registry.SamsaraParticles.VONG_NUOC, x, surface + 0.02, z, 0, 0, 0);
            world.playSound(x, y, z, SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundCategory.BLOCKS,
                    0.18f, 0.9f + random.nextFloat() * 0.3f, false);
            markDead();
        }
    }

    /** Không nhận ánh sáng block (ánh sáng vanilla ngả vàng, làm hạt xám thành màu nâu). */
    @Override
    protected int getBrightness(float tint) {
        return 0xF000F0;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new GiotNuocParticle(world, x, y, z, vx, sprites);
        }
    }
}
