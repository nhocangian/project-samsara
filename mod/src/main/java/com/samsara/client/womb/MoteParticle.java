package com.samsara.client.womb;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * Bụi bị hút về Gargantua: chấm nhỏ màu vàng kem như đĩa bồi tụ, bốc lên từ mặt nước rồi bay theo hướng con mắt
 * trên trời ({@link #GARGANTUA_DIR}), tăng tốc dần và kéo dài thành vệt mảnh dọc hướng bay, rồi tan.
 * Vận tốc ban đầu của gói hạt (vx, vy, vz) chỉ dùng độ lớn = tốc độ xuất phát (chương IV truyền lớn hơn).
 */
public class MoteParticle extends SpriteBillboardParticle {
    /** Phải khớp bhForward của shader world1 và MirrorSelfRenderer.GARGANTUA_DIR. */
    public static final Vec3d GARGANTUA_DIR = new Vec3d(0.0, 0.54, -0.84).normalize();

    private static final double ACCEL = 1.045;
    private static final double MAX_SPEED = 0.55;
    /** Độ dài vệt = tốc độ x hệ số này (block), tối đa {@link #MAX_TAIL}. */
    private static final float STRETCH = 5.0f;
    private static final float MAX_TAIL = 3.0f;

    private final float peakAlpha;

    protected MoteParticle(ClientWorld world, double x, double y, double z, double speed, SpriteProvider sprites) {
        super(world, x, y, z);
        setSprite(sprites.getSprite(random));
        maxAge = 60 + random.nextInt(50);
        scale = 0.025f + random.nextFloat() * 0.03f;
        red = 1.0f;
        green = 0.86f + random.nextFloat() * 0.08f;
        blue = 0.62f + random.nextFloat() * 0.12f;
        peakAlpha = 0.35f + random.nextFloat() * 0.25f;
        alpha = 0.0f;
        // Hướng bay lệch nhẹ quanh hướng Gargantua
        Vec3d dir = GARGANTUA_DIR.add((random.nextDouble() - 0.5) * 0.25, (random.nextDouble() - 0.5) * 0.15,
                (random.nextDouble() - 0.5) * 0.25).normalize().multiply(Math.max(0.01, speed));
        velocityX = dir.x;
        velocityY = dir.y;
        velocityZ = dir.z;
        velocityMultiplier = 1.0f;
        gravityStrength = 0.0f;
        collidesWithWorld = false;
    }

    @Override
    public void tick() {
        double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);
        if (speed < MAX_SPEED) {
            velocityX *= ACCEL;
            velocityY *= ACCEL;
            velocityZ *= ACCEL;
        }
        super.tick();
        float fadeIn = MathHelper.clamp(age / 10.0f, 0.0f, 1.0f);
        float fadeOut = MathHelper.clamp((maxAge - age) / 20.0f, 0.0f, 1.0f);
        alpha = peakAlpha * fadeIn * fadeOut;
    }

    /** Quad kéo dài dọc hướng bay (đầu ở vị trí hạt, đuôi lùi về sau), luôn quay mặt về camera. */
    @Override
    public void buildGeometry(VertexConsumer vc, Camera camera, float tickDelta) {
        Vec3d cam = camera.getPos();
        Vector3f head = new Vector3f(
                (float) (MathHelper.lerp(tickDelta, prevPosX, x) - cam.x),
                (float) (MathHelper.lerp(tickDelta, prevPosY, y) - cam.y),
                (float) (MathHelper.lerp(tickDelta, prevPosZ, z) - cam.z));
        Vector3f dir = new Vector3f((float) velocityX, (float) velocityY, (float) velocityZ);
        float speed = dir.length();
        if (speed < 1.0E-5f) dir.set(0, 1, 0);
        else dir.div(speed);
        float tailLength = Math.min(MAX_TAIL, Math.max(scale * 2.0f, speed * STRETCH));
        Vector3f tail = new Vector3f(dir).mul(-tailLength).add(head);

        Vector3f toCam = new Vector3f(head).negate();
        if (toCam.lengthSquared() < 1.0E-6f) return;
        Vector3f side = new Vector3f(dir).cross(toCam);
        if (side.lengthSquared() < 1.0E-8f) side.set(1, 0, 0);
        side.normalize().mul(scale);

        float u0 = getMinU(), u1 = getMaxU(), v0 = getMinV(), v1 = getMaxV();
        int light = getBrightness(tickDelta);
        vertex(vc, head.x + side.x, head.y + side.y, head.z + side.z, u1, v0, light);
        vertex(vc, head.x - side.x, head.y - side.y, head.z - side.z, u0, v0, light);
        vertex(vc, tail.x - side.x, tail.y - side.y, tail.z - side.z, u0, v1, light);
        vertex(vc, tail.x + side.x, tail.y + side.y, tail.z + side.z, u1, v1, light);
    }

    private void vertex(VertexConsumer vc, float x, float y, float z, float u, float v, int light) {
        vc.vertex(x, y, z).texture(u, v).color(red, green, blue, alpha).light(light).next();
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getBrightness(float tint) {
        return 0xF000F0;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new MoteParticle(world, x, y, z, Math.sqrt(vx * vx + vy * vy + vz * vz), sprites);
        }
    }
}
