package com.samsara.client.tangduoi;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/** Cánh hoa hướng dương trôi chậm trên mặt nước theo dòng chảy (Tầng Dưới). */
public class CanhHoaParticle extends SpriteBillboardParticle {
    protected CanhHoaParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
        super(world, x, y, z);
        setSprite(sprites.getSprite(random));
        velocityX = vx;
        velocityY = vy;
        velocityZ = vz;
        velocityMultiplier = 1.0f;
        gravityStrength = 0.0f;
        collidesWithWorld = false;
        maxAge = 90 + random.nextInt(70);
        scale = 0.09f + random.nextFloat() * 0.07f;
        angle = prevAngle = random.nextFloat() * 6.28f;
    }

    @Override
    public void tick() {
        super.tick();
        prevAngle = angle;
        angle += 0.012f;
        float k = Math.min(Math.min(age / 12.0f, (maxAge - age) / 18.0f), 1.0f);
        alpha = Math.max(0.0f, k);
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
            return new CanhHoaParticle(world, x, y, z, vx, vy, vz, sprites);
        }
    }
}
