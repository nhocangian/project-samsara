package com.samsara.client.tangduoi;

import com.samsara.client.CamcorderClientManager;
import com.samsara.registry.SamsaraDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Tầng 2: Ophanim, thiên thần "đúng kinh thánh": ba vành lồng nhau đầy mắt, một con mắt lớn ở giữa, sáu đôi cánh.
 * Nó ló ra từ mép trái tòa tháp khổng lồ bên phải (LoiDiNhapKhoi), cao lưng chừng tháp, nhìn xuống người chơi.
 * Chỉ thấy qua máy quay. Chĩa thẳng tâm máy quay vào nó thì nó vụt núp ra sau tháp, một lúc sau lại ló ra.
 * Toàn bộ ở phía client: không phải thực thể, server không biết nó tồn tại.
 *
 * Đỉnh vẽ với alpha 253: shader world4 cho nó nhạt theo không khí, sáng tối theo mặt và chói gần mặt trời như các khối
 * khổng lồ, nhưng không lát gạch (xem gbuffers_entities.fsh; chim dùng 254).
 */
public final class LoiDiNhapAngel {
    private LoiDiNhapAngel() {}

    private static final Identifier EYE = new Identifier("samsara", "textures/misc/thien_than_mat.png");
    private static final Identifier IRIS = new Identifier("samsara", "textures/misc/thien_than_mat_trong.png");
    private static final Identifier RING = new Identifier("samsara", "textures/misc/thien_than_vanh.png");
    private static final Identifier WING = new Identifier("samsara", "textures/misc/thien_than_canh.png");
    /** Toạ độ trên texture cánh (pixel trên 512), khớp với tools/generate_thien_than_canh.py. */
    private static final float TEX = 512f;
    private static final float[] SHOULDER_PX = {30f, 300f};
    private static final float[] WRIST_PX = {150f, 80f};
    private static final float[] TIP_PX = {410f, 40f};
    private static final int FULL_BRIGHT = 0xF000F0;

    /**
     * Mỗi bậc một chỗ đứng (toạ độ thế giới), mỗi bậc gần hơn và nhỏ lại (nhưng to hơn trong mắt người chơi).
     * Bậc 0: sau mép trái tòa tháp bên phải (mặt bắc tháp z -1460, vành xa nhất 630 block quanh tâm: không chạm tháp,
     * hai bề mặt cắt nhau ở xa sẽ chập hình); núp thì lùi ra sau tháp. Bậc 1-2: nhô lên từ biển mây giữa các khối,
     * núp thì chìm xuống mây. Bậc 3 (từ đảo 5): không núp nữa. Bậc 4: ngay sau bức tường bắc dở dang của phòng cuối.
     * Các chỗ đứng đã được chọn để sải cánh không cắm vào tháp hay khối nào (xem LoiDiNhapKhoi).
     */
    private record Stage(Vec3d peek, Vec3d hidden, float scale, boolean hides) {}

    private static final Stage[] STAGES = {
            new Stage(new Vec3d(600, 980, -2150), new Vec3d(1650, 980, -2200), 3.0f, true),
            new Stage(new Vec3d(-450, 400, -1350), new Vec3d(-450, -500, -1350), 1.7f, true),
            new Stage(new Vec3d(150, 300, -900), new Vec3d(150, -450, -900), 1.25f, true),
            new Stage(new Vec3d(330, 230, -720), new Vec3d(330, -400, -720), 0.9f, false),
            new Stage(new Vec3d(130, 200, -490), new Vec3d(130, -300, -490), 0.5f, false),
    };
    private static final int FINAL_STAGE = STAGES.length - 1;
    /** Hạ máy quay chừng này tick thì nó tiến thêm một bậc (nếu đã được phép), như thể đã đi lúc không ai nhìn. */
    private static final int UNSEEN_ADVANCE = 80;

    /** Cỡ của bậc hiện tại: S = 3 thì vành ngoài bán kính 630 block. */
    private static float S = 3.0f;

    private static float ringRadius() {
        return 210f * S;
    }

    /** Chĩa tâm máy quay trong góc này (độ) thì nó núp. */
    private static final double STARE_ANGLE = 7.0;
    private static final int HIDE_TICKS = 10;
    private static final int RETURN_TICKS = 90;
    private static final int COOLDOWN_MIN = 500;
    private static final int COOLDOWN_SPREAD = 400;

    private enum State { PEEK, HIDING, HIDDEN, RETURNING }

    private static State state = State.PEEK;
    private static int timer = 0;
    /** 0 = đang ló ra, 1 = đã núp hẳn. */
    private static float hide = 0f;
    private static float prevHide = 0f;
    /** Chưa tới đảo 2 thì nó chưa xuất hiện; tới rồi thì ở lại cho tới khi rời tầng. */
    private static boolean unlocked = false;
    private static int stage = 0;
    private static int unseen = 0;
    /** Bậc không núp: đang bị nhìn thẳng (để chỉ vang tiếng một lần mỗi lần bị bắt gặp). */
    private static boolean lockedOn = false;
    private static AngelHum hum;
    private static final Random RANDOM = Random.create();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(LoiDiNhapAngel::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(LoiDiNhapAngel::render);
    }

    private static boolean inFloor(MinecraftClient client) {
        return client.player != null && client.world != null
                && client.world.getRegistryKey() == SamsaraDimensions.LOI_DI_NHAP_WORLD_KEY;
    }

    private static Vec3d center(float tickDelta) {
        float h = MathHelper.lerp(tickDelta, prevHide, hide);
        float e = h * h * (3 - 2 * h);
        return STAGES[stage].peek.lerp(STAGES[stage].hidden, e);
    }

    /** Bậc xa nhất được phép theo chỗ người chơi đã tới: đảo 2 / 3 / 4 / 5, phòng cuối. */
    private static int allowedStage(MinecraftClient client) {
        double z = client.player.getZ() - com.samsara.system.tangduoi.LoiDiNhapBuilder.ORIGIN.getZ();
        if (com.samsara.system.tangduoi.LoiDiNhapBuilder.isFinalRoom(client.player.getZ())) return FINAL_STAGE;
        int allowed = 0;
        int[] gates = com.samsara.system.tangduoi.LoiDiNhapBuilder.ANGEL_STAGE_Z;
        for (int i = 1; i < gates.length; i++) {
            if (z <= gates[i]) allowed = i;
        }
        return allowed;
    }

    private static void setStage(int s) {
        stage = s;
        S = STAGES[s].scale;
    }

    private static void tick(MinecraftClient client) {
        if (!inFloor(client)) {
            state = State.PEEK;
            hide = prevHide = 0f;
            unlocked = false;
            setStage(0);
            stopHum();
            return;
        }
        if (!unlocked) {
            unlocked = client.player.getZ() - com.samsara.system.tangduoi.LoiDiNhapBuilder.ORIGIN.getZ()
                    <= com.samsara.system.tangduoi.LoiDiNhapBuilder.ANGEL_FROM_Z;
            if (!unlocked) return;
        }
        prevHide = hide;
        boolean aimed = CamcorderClientManager.isFullyAimed();
        boolean stared = aimed && staredAt(client);
        int allowed = allowedStage(client);
        // Vào phòng cuối: nó đã đứng sẵn sau bức tường (đoạn nhiễu ngay trước đó nên không ai thấy nó tới)
        if (allowed == FINAL_STAGE && stage != FINAL_STAGE) {
            setStage(FINAL_STAGE);
            state = State.PEEK;
            hide = prevHide = 0f;
        }
        // Hạ máy quay một lúc thì nó đã đổi chỗ, gần hơn
        unseen = aimed ? 0 : unseen + 1;
        if (unseen >= UNSEEN_ADVANCE && stage < allowed && state == State.PEEK) {
            setStage(stage + 1);
            unseen = 0;
        }
        if (!STAGES[stage].hides) {
            if (state != State.PEEK) {
                hide = Math.max(0f, hide - 1f / RETURN_TICKS);
                if (hide <= 0f) state = State.PEEK;
            }
            if (stared && !lockedOn) {
                // Không núp nữa: mọi con mắt khoá vào người chơi
                client.player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.4f);
                client.player.playSound(SoundEvents.AMBIENT_CAVE.value(), 1.0f, 0.5f);
            }
            lockedOn = stared;
            tickHum(client, aimed);
            return;
        }
        switch (state) {
            case PEEK -> {
                if (stared) startHiding(client);
            }
            case HIDING -> {
                hide = Math.min(1f, hide + 1f / HIDE_TICKS);
                if (hide >= 1f) {
                    state = State.HIDDEN;
                    timer = COOLDOWN_MIN + RANDOM.nextInt(COOLDOWN_SPREAD);
                }
            }
            case HIDDEN -> {
                if (--timer <= 0) {
                    // Mỗi lần núp xong, nếu người chơi đã đi đủ xa, nó trở lại ở chỗ gần hơn
                    if (stage < allowed) setStage(stage + 1);
                    state = State.RETURNING;
                }
            }
            case RETURNING -> {
                hide = Math.max(0f, hide - 1f / RETURN_TICKS);
                if (stared) startHiding(client);
                else if (hide <= 0f) state = State.PEEK;
            }
        }
        tickHum(client, aimed);
    }

    // ==========================================
    // TIẾNG Ù
    // ==========================================

    /**
     * Tiếng ù trầm chỉ nghe khi đang ngắm máy quay mà nó nằm trong khung hình; bậc càng gần càng to. Bị nhìn thẳng mà
     * núp thì tiếng tắt phụt, nên người chơi học được: có tiếng là nó đang ở đâu đó trong khung hình.
     */
    private static void tickHum(MinecraftClient client, boolean aimed) {
        boolean visible = aimed && state != State.HIDDEN && state != State.HIDING && inFrame(client);
        float target = visible ? 0.22f + 0.12f * stage + (lockedOn ? 0.2f : 0f) : 0f;
        if (hum == null || hum.isDone() || !client.getSoundManager().isPlaying(hum)) {
            if (target <= 0f) return;
            hum = new AngelHum();
            client.getSoundManager().play(hum);
        }
        hum.target = target;
        hum.pitchFor(stage);
    }

    private static boolean inFrame(MinecraftClient client) {
        Vec3d eye = client.player.getCameraPosVec(1.0f);
        Vec3d to = center(1.0f).subtract(eye).normalize();
        double angle = Math.toDegrees(Math.acos(MathHelper.clamp(to.dotProduct(client.player.getRotationVec(1.0f)), -1.0, 1.0)));
        // Tính cả sải cánh: tâm hơi lệch khỏi khung vẫn coi là trong hình
        return angle < 48.0;
    }

    private static void stopHum() {
        if (hum != null) {
            hum.finish();
            hum = null;
        }
    }

    private static void cutHum() {
        if (hum != null) {
            hum.target = 0f;
            hum.cut();
        }
    }

    private static final class AngelHum extends net.minecraft.client.sound.MovingSoundInstance {
        float target = 0f;

        AngelHum() {
            super(SoundEvents.BLOCK_BEACON_AMBIENT, net.minecraft.sound.SoundCategory.AMBIENT, net.minecraft.client.sound.SoundInstance.createRandom());
            this.repeat = true;
            this.repeatDelay = 0;
            this.relative = true;
            this.attenuationType = AttenuationType.NONE;
            this.volume = 0.01f;
            this.pitch = 0.5f;
        }

        void pitchFor(int s) {
            this.pitch = 0.55f - 0.03f * s;
        }

        void cut() {
            this.volume = 0.001f;
        }

        void finish() {
            setDone();
        }

        @Override
        public boolean shouldAlwaysPlay() {
            return true;
        }

        @Override
        public void tick() {
            // Lên chậm, xuống nhanh
            float step = target > volume ? 0.012f : 0.08f;
            volume = target > volume ? Math.min(target, volume + step) : Math.max(Math.max(target, 0.001f), volume - step);
        }
    }

    private static void startHiding(MinecraftClient client) {
        state = State.HIDING;
        cutHum();
        client.player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.9f, 0.45f);
        client.player.playSound(SoundEvents.ENTITY_PHANTOM_FLAP, 1.0f, 0.3f);
    }

    private static boolean staredAt(MinecraftClient client) {
        Vec3d eye = client.player.getCameraPosVec(1.0f);
        Vec3d to = center(1.0f).subtract(eye).normalize();
        Vec3d look = client.player.getRotationVec(1.0f);
        return Math.toDegrees(Math.acos(MathHelper.clamp(to.dotProduct(look), -1.0, 1.0))) < STARE_ANGLE;
    }

    // ==========================================
    // VẼ
    // ==========================================

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!inFloor(client) || !unlocked || !CamcorderClientManager.isFullyAimed()) return;
        if (state == State.HIDDEN || context.consumers() == null || context.matrixStack() == null) return;

        Camera camera = context.camera();
        Vec3d cam = camera.getPos();
        float delta = context.tickDelta();
        double time = (client.world.getTime() + delta) / 20.0;
        Vec3d c = center(delta).add(0, Math.sin(time * 0.35) * 6.0 * S, 0).subtract(cam);
        float hideNow = MathHelper.lerp(delta, prevHide, hide);
        float wingScale = 1f - 0.85f * hideNow * hideNow * (3 - 2 * hideNow);   // núp thì cụp cánh
        Vector3f center = new Vector3f((float) c.x, (float) c.y, (float) c.z);

        // Cánh nằm trong mặt phẳng quay về phía người chơi
        Vector3f facing = new Vector3f(-center.x, 0f, -center.z).normalize();
        Vector3f right = new Vector3f(0f, 1f, 0f).cross(facing).normalize();
        Vector3f up = new Vector3f(0f, 1f, 0f);
        Vector3f camRight = new Vector3f(camera.getDiagonalPlane()).normalize();
        Vector3f camUp = new Vector3f(camera.getVerticalPlane()).normalize();

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        Matrix4f m = matrices.peek().getPositionMatrix();

        List<Vector3f> spots = new ArrayList<>();
        List<Float> sizes = new ArrayList<>();
        VertexConsumer wings = context.consumers().getBuffer(RenderLayer.getEntityCutoutNoCull(WING));
        drawWings(wings, m, center, facing, right, up, time, wingScale, spots, sizes);

        VertexConsumer rings = context.consumers().getBuffer(RenderLayer.getEntityCutoutNoCull(RING));
        Quaternionf[] spins = ringSpins(time);
        for (int k = 0; k < 3; k++) drawRing(rings, m, center, spins[k], ringRadius() - k * 26f * S, 16f * S, 20f * S);

        // Bị bắt gặp thì mọi con mắt nhắm lại; ló ra lại thì mở dần
        float open = state == State.HIDING ? MathHelper.clamp(1f - hideNow * 3f, 0f, 1f)
                : state == State.RETURNING ? MathHelper.clamp((0.6f - hideNow) / 0.3f, 0f, 1f) : 1f;
        open = Math.max(open, 0.05f);
        // Hướng nhìn: lúc ló ra lại thì liếc sang bên rồi mới khoá vào người chơi; bình thường chỉ rung rất khẽ
        float look = state == State.RETURNING ? hideNow * hideNow * (3 - 2 * hideNow) : 0f;
        float gazeX = -0.6f * look + (float) Math.sin(time * 0.7) * 0.05f;
        float gazeY = 0.3f * look + (float) Math.sin(time * 0.53 + 1.3) * 0.04f;

        for (int k = 0; k < 3; k++) {
            float r = ringRadius() - k * 26f * S + 12f * S;
            int count = 16 - k * 2;
            for (int i = 0; i < count; i++) {
                double a = (i + 0.5) / count * Math.PI * 2;
                spots.add(spins[k].transform(new Vector3f((float) (Math.cos(a) * r), (float) (Math.sin(a) * r), 0f)).add(center));
                sizes.add(15f * S);
            }
        }
        // Con mắt lớn ở giữa
        spots.add(new Vector3f(center));
        sizes.add(78f * S);

        VertexConsumer whites = context.consumers().getBuffer(RenderLayer.getEntityCutoutNoCull(EYE));
        for (int i = 0; i < spots.size(); i++) billboard(whites, m, spots.get(i), camRight, camUp, sizes.get(i), sizes.get(i) * open);
        if (open > 0.3f) {
            VertexConsumer irises = context.consumers().getBuffer(RenderLayer.getEntityCutoutNoCull(IRIS));
            for (int i = 0; i < spots.size(); i++) {
                float half = sizes.get(i);
                // Tròng nằm nhích về phía camera để không chập với lòng trắng. Ở hơn 2000 block bộ đệm độ sâu chỉ phân
                // biệt được các mặt cách nhau vài block, nên khoảng nhích phải tăng theo khoảng cách (2%).
                float dist = spots.get(i).length();
                Vector3f toCam = new Vector3f(spots.get(i)).negate().normalize().mul(Math.max(1.5f * S, dist * 0.02f));
                Vector3f p = new Vector3f(spots.get(i)).add(toCam)
                        .add(new Vector3f(camRight).mul(gazeX * half * 0.45f))
                        .add(new Vector3f(camUp).mul(gazeY * half * 0.45f));
                billboard(irises, m, p, camRight, camUp, half * 0.5f, half * 0.5f * Math.min(1f, open));
            }
        }
        matrices.pop();
    }

    private static Quaternionf[] ringSpins(double time) {
        return new Quaternionf[]{
                new Quaternionf().rotateY((float) (time * 0.11)).rotateX((float) Math.toRadians(75)),
                new Quaternionf().rotateX((float) (time * 0.09)).rotateY((float) Math.toRadians(20)),
                new Quaternionf().rotateZ((float) (time * 0.13)).rotateY((float) Math.toRadians(80)).rotateX((float) (time * 0.05)),
        };
    }

    /** Vành tròn: ống vuông chia đoạn, bốn mặt mỗi đoạn. */
    private static void drawRing(VertexConsumer vc, Matrix4f m, Vector3f c, Quaternionf q, float radius, float thick, float height) {
        int segments = 56;
        for (int i = 0; i < segments; i++) {
            double a0 = i / (double) segments * Math.PI * 2;
            double a1 = (i + 1) / (double) segments * Math.PI * 2;
            Vector3f[] p0 = section(c, q, a0, radius, thick, height);
            Vector3f[] p1 = section(c, q, a1, radius, thick, height);
            float u0 = (i % 4) / 4f;
            float u1 = u0 + 0.25f;
            // 0: ngoài-trên, 1: ngoài-dưới, 2: trong-dưới, 3: trong-trên
            quad(vc, m, p0[0], p0[1], p1[1], p1[0], u0, u1, 236);
            quad(vc, m, p0[3], p0[2], p1[2], p1[3], u0, u1, 205);
            quad(vc, m, p0[0], p0[3], p1[3], p1[0], u0, u1, 250);
            quad(vc, m, p0[1], p0[2], p1[2], p1[1], u0, u1, 190);
        }
    }

    private static Vector3f[] section(Vector3f c, Quaternionf q, double a, float radius, float thick, float height) {
        float cos = (float) Math.cos(a);
        float sin = (float) Math.sin(a);
        float ro = radius + thick / 2;
        float ri = radius - thick / 2;
        float hz = height / 2;
        return new Vector3f[]{
                q.transform(new Vector3f(cos * ro, sin * ro, hz)).add(c),
                q.transform(new Vector3f(cos * ro, sin * ro, -hz)).add(c),
                q.transform(new Vector3f(cos * ri, sin * ri, -hz)).add(c),
                q.transform(new Vector3f(cos * ri, sin * ri, hz)).add(c),
        };
    }

    /**
     * Ba đôi cánh thiên nga: một đôi giơ lên, một đôi dang ngang, một đôi chúc xuống, đôi dưới nhỏ hơn. Mỗi chiếc là một
     * tấm vẽ xoay quanh vai trong mặt phẳng quay về phía người chơi; vỗ rất chậm, núp thì cụp lại. Dọc sống cánh có
     * sáu con mắt (đưa vào spots để cùng nhắm, cùng liếc với các mắt khác).
     */
    private static void drawWings(VertexConsumer vc, Matrix4f m, Vector3f c, Vector3f facing, Vector3f right, Vector3f up,
                                  double time, float scale, List<Vector3f> spots, List<Float> sizes) {
        float[] elevations = {22f, -8f, -42f};
        float[] spans = {560f, 490f, 390f};
        float[] lifts = {70f, 0f, -70f};
        for (int pair = 0; pair < 3; pair++) {
            float flap = (float) Math.sin(time * 0.55 + pair * 0.9) * 5f;
            float span = spans[pair] * S * scale;
            for (int side = -1; side <= 1; side += 2) {
                float e = (float) Math.toRadians(elevations[pair] + flap);
                Vector3f out = new Vector3f(right).mul(side);
                Vector3f a = new Vector3f(out).mul((float) Math.cos(e)).add(new Vector3f(up).mul((float) Math.sin(e)));
                Vector3f b = new Vector3f(out).mul((float) -Math.sin(e)).add(new Vector3f(up).mul((float) Math.cos(e)));
                Vector3f shoulder = new Vector3f(c)
                        .add(new Vector3f(facing).mul(-(30f + pair * 22f) * S))
                        .add(new Vector3f(out).mul(70f * S * scale))
                        .add(new Vector3f(up).mul(lifts[pair] * S * scale));
                Vector3f p00 = wingPoint(shoulder, a, b, span, 0f, 0f);
                Vector3f p01 = wingPoint(shoulder, a, b, span, 0f, 1f);
                Vector3f p11 = wingPoint(shoulder, a, b, span, 1f, 1f);
                Vector3f p10 = wingPoint(shoulder, a, b, span, 1f, 0f);
                int shade = 250 - pair * 12;
                vertex(vc, m, p00.x, p00.y, p00.z, 0f, 0f, shade);
                vertex(vc, m, p01.x, p01.y, p01.z, 0f, 1f, shade);
                vertex(vc, m, p11.x, p11.y, p11.z, 1f, 1f, shade);
                vertex(vc, m, p10.x, p10.y, p10.z, 1f, 0f, shade);

                if (scale < 0.5f) continue;
                for (int i = 0; i < 6; i++) {
                    float t = 0.16f + i * 0.14f;
                    float px = bezier(SHOULDER_PX[0], WRIST_PX[0], TIP_PX[0], t);
                    float py = bezier(SHOULDER_PX[1], WRIST_PX[1], TIP_PX[1], t) + 14f;
                    Vector3f p = wingPoint(shoulder, a, b, span, px / TEX, py / TEX);
                    // Nhích về phía camera để không chập với tấm cánh (ở xa bộ đệm độ sâu thô)
                    p.add(new Vector3f(p).negate().normalize().mul(Math.max(2f * S, p.length() * 0.012f)));
                    spots.add(p);
                    sizes.add((13f - i * 0.8f) * S * (span / (340f * S)) * 0.8f);
                }
            }
        }
    }

    private static float bezier(float p0, float p1, float p2, float t) {
        float u = 1f - t;
        return u * u * p0 + 2f * u * t * p1 + t * t * p2;
    }

    /** Điểm (u, v) trên texture cánh, đặt vào thế giới sao cho điểm vai của texture nằm ở {@code shoulder}. */
    private static Vector3f wingPoint(Vector3f shoulder, Vector3f a, Vector3f b, float span, float u, float v) {
        return new Vector3f(shoulder)
                .add(new Vector3f(a).mul((u - SHOULDER_PX[0] / TEX) * span))
                .add(new Vector3f(b).mul((SHOULDER_PX[1] / TEX - v) * span));
    }

    private static void billboard(VertexConsumer vc, Matrix4f m, Vector3f p, Vector3f right, Vector3f up, float halfW, float halfH) {
        Vector3f r = new Vector3f(right).mul(halfW);
        Vector3f u = new Vector3f(up).mul(halfH);
        vertex(vc, m, p.x - r.x + u.x, p.y - r.y + u.y, p.z - r.z + u.z, 0f, 0f, 255);
        vertex(vc, m, p.x - r.x - u.x, p.y - r.y - u.y, p.z - r.z - u.z, 0f, 1f, 255);
        vertex(vc, m, p.x + r.x - u.x, p.y + r.y - u.y, p.z + r.z - u.z, 1f, 1f, 255);
        vertex(vc, m, p.x + r.x + u.x, p.y + r.y + u.y, p.z + r.z + u.z, 1f, 0f, 255);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, Vector3f c, Vector3f d, float u0, float u1, int shade) {
        vertex(vc, m, a.x, a.y, a.z, u0, 0f, shade);
        vertex(vc, m, b.x, b.y, b.z, u0, 1f, shade);
        vertex(vc, m, c.x, c.y, c.z, u1, 1f, shade);
        vertex(vc, m, d.x, d.y, d.z, u1, 0f, shade);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int shade) {
        vc.vertex(m, x, y, z).color(shade, shade, shade, 253).texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV).light(FULL_BRIGHT).normal(0, 1, 0).next();
    }

    /** Lệnh test: đưa nó về chỗ ló ra ngay. */
    public static void resetForTest() {
        forceStage(0);
    }

    /** Lệnh test / ScriptedShots: đặt bậc và cho nó ló ra ngay. */
    public static void forceStage(int s) {
        setStage(MathHelper.clamp(s, 0, FINAL_STAGE));
        state = State.PEEK;
        hide = prevHide = 0f;
        unseen = 0;
    }
}
