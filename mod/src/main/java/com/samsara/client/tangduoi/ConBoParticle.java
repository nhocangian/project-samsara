package com.samsara.client.tangduoi;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/** Con bọ một chấm, bay vòng vòng lộn xộn ngay dưới bóng đèn (Tầng Dưới). */
public class ConBoParticle extends SpriteBillboardParticle {
    private final double cx;
    private final double cy;
    private final double cz;
    private final float speed;
    private final float radius;
    private final float wobble;
    private float phase;

    protected ConBoParticle(ClientWorld world, double x, double y, double z, SpriteProvider sprites) {
        super(world, x, y, z);
        setSprite(sprites.getSprite(random));
        cx = x;
        cy = y;
        cz = z;
        maxAge = 120 + random.nextInt(140);
        scale = 0.018f + random.nextFloat() * 0.012f;
        float grey = random.nextFloat() < 0.3f ? 0.82f + random.nextFloat() * 0.15f : 0.42f + random.nextFloat() * 0.2f;
        red = grey;
        green = grey;
        blue = grey * 1.03f;
        alpha = 0.32f + random.nextFloat() * 0.3f;
        gravityStrength = 0.0f;
        collidesWithWorld = false;
        speed = (random.nextBoolean() ? 1 : -1) * (0.18f + random.nextFloat() * 0.25f);
        radius = 0.25f + random.nextFloat() * 0.65f;
        wobble = 0.5f + random.nextFloat() * 1.5f;
        phase = random.nextFloat() * 6.28f;
        orbit(0);
    }

    private void orbit(float jitter) {
        phase += speed + jitter;
        float r = radius * (1.0f + 0.35f * MathHelper.sin(phase * wobble));
        x = cx + MathHelper.cos(phase) * r;
        z = cz + MathHelper.sin(phase) * r;
        y = cy + 0.25f * MathHelper.sin(phase * wobble * 1.7f);
    }

    @Override
    public void tick() {
        prevPosX = x;
        prevPosY = y;
        prevPosZ = z;
        if (age++ >= maxAge) {
            markDead();
            return;
        }
        // Thỉnh thoảng giật cục đổi nhịp, như bọ đập vào bóng đèn
        orbit(random.nextFloat() < 0.06f ? (random.nextFloat() - 0.5f) * 2.5f : 0.0f);
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
            return new ConBoParticle(world, x, y, z, sprites);
        }
    }
}
