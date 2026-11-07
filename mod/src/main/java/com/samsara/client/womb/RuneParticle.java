package com.samsara.client.womb;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/**
 * Ký tự phù phép (bộ chữ của bàn phù phép vanilla, minecraft:sga_a..z) màu đỏ, tự phát sáng, bay chậm lên rồi tan.
 * Vừa bay lên vừa trôi dần về phía bắc (về Gargantua, như mọi thứ trong chương III). Cứ vài tick lại đổi sang một
 * ký tự khác, lắc nhẹ sang hai bên. Chỉ để tạo không khí, không mang nghĩa.
 */
public class RuneParticle extends SpriteBillboardParticle {
    private final SpriteProvider sprites;
    private final float phase;
    private int nextSwap;
    private final double driftZ;

    protected RuneParticle(ClientWorld world, double x, double y, double z, SpriteProvider sprites) {
        super(world, x, y, z);
        this.sprites = sprites;
        setSprite(sprites.getSprite(random));
        maxAge = 90 + random.nextInt(70);
        scale = 0.10f + random.nextFloat() * 0.12f;
        red = 1.0f;
        green = 0.10f + random.nextFloat() * 0.12f;
        blue = 0.06f;
        alpha = 0.0f;
        velocityY = 0.025 + random.nextFloat() * 0.03;
        driftZ = -velocityY * (0.5 + random.nextFloat() * 0.4);   // trôi dần về Gargantua (phía bắc)
        velocityX = 0.0;
        velocityZ = driftZ;
        velocityMultiplier = 1.0f;
        gravityStrength = 0.0f;
        collidesWithWorld = false;
        phase = random.nextFloat() * 100.0f;
        nextSwap = 6 + random.nextInt(14);
    }

    @Override
    public void tick() {
        super.tick();
        float fadeIn = MathHelper.clamp(age / 15.0f, 0.0f, 1.0f);
        float fadeOut = MathHelper.clamp((maxAge - age) / 25.0f, 0.0f, 1.0f);
        alpha = 0.9f * fadeIn * fadeOut;
        velocityX = MathHelper.sin((age + phase) * 0.08f) * 0.01;
        velocityZ = driftZ + MathHelper.cos((age + phase) * 0.06f) * 0.01;
        if (--nextSwap <= 0) {
            setSprite(sprites.getSprite(random));
            nextSwap = 6 + random.nextInt(14);
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getBrightness(float tint) {
        return LightmapTextureManager.MAX_LIGHT_COORDINATE;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new RuneParticle(world, x, y, z, sprites);
        }
    }
}
