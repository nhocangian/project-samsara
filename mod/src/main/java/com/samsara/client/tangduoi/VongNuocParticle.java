package com.samsara.client.tangduoi;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.Vec3d;

/** Vòng nước loang ra chỗ giọt nước rơi xuống (Tầng Dưới): một mặt phẳng nằm ngang, vẽ theo từng khung hình. */
public class VongNuocParticle extends SpriteBillboardParticle {
    private static final float HALF = 0.42f;
    private final SpriteProvider sprites;

    protected VongNuocParticle(ClientWorld world, double x, double y, double z, SpriteProvider sprites) {
        super(world, x, y, z);
        this.sprites = sprites;
        maxAge = 14;
        gravityStrength = 0.0f;
        collidesWithWorld = false;
        velocityX = velocityY = velocityZ = 0.0;
        red = 0.8f;
        green = 0.83f;
        blue = 0.84f;
        setSpriteForAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteForAge(sprites);
        alpha = 0.7f * (1.0f - (float) age / maxAge);
    }

    @Override
    public void buildGeometry(VertexConsumer consumer, Camera camera, float tickDelta) {
        Vec3d cam = camera.getPos();
        float cx = (float) (x - cam.x);
        float cy = (float) (y - cam.y);
        float cz = (float) (z - cam.z);
        float u0 = getMinU();
        float u1 = getMaxU();
        float v0 = getMinV();
        float v1 = getMaxV();
        int light = getBrightness(tickDelta);
        // Vẽ cả hai mặt: nhìn từ trên hay sát mặt nước đều thấy
        quad(consumer, cx - HALF, cy, cz - HALF, cx - HALF, cy, cz + HALF, cx + HALF, cy, cz + HALF, cx + HALF, cy, cz - HALF, u0, u1, v0, v1, light);
        quad(consumer, cx + HALF, cy, cz - HALF, cx + HALF, cy, cz + HALF, cx - HALF, cy, cz + HALF, cx - HALF, cy, cz - HALF, u1, u0, v0, v1, light);
    }

    private void quad(VertexConsumer c, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
                      float x3, float y3, float z3, float u0, float u1, float v0, float v1, int light) {
        c.vertex(x0, y0, z0).texture(u0, v0).color(red, green, blue, alpha).light(light).next();
        c.vertex(x1, y1, z1).texture(u0, v1).color(red, green, blue, alpha).light(light).next();
        c.vertex(x2, y2, z2).texture(u1, v1).color(red, green, blue, alpha).light(light).next();
        c.vertex(x3, y3, z3).texture(u1, v0).color(red, green, blue, alpha).light(light).next();
    }

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
            return new VongNuocParticle(world, x, y, z, sprites);
        }
    }
}
