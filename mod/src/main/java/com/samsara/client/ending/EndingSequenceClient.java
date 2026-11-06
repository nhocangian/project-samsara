package com.samsara.client.ending;

import com.samsara.mixin.ChatScreenAccessor;
import com.samsara.system.ending.EndingServer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Đoạn kết phía CLIENT (Video 9: The_Egg_Dialogue). Toàn bộ chạy theo một dòng thời gian (tick client):
 *
 *  1. CINEMATIC (15 giây, máy chậm + cắt cảnh, HUD ẩn, khoá di chuyển):
 *     toàn cảnh xa thấp (người chơi nhỏ bé trước Quả Trứng và vũ trụ) -> sát vai nhìn mặt nước gợn
 *     -> góc thấp bên cạnh ngước theo bóng phản chiếu trồi lên -> về lại mắt nhân vật.
 *  2. DIALOGUE: đoạn thoại tự động (EndingDialogue). Nhân vật chính tự gõ vào ô chat; <System> trả lời chữ vàng.
 *     Ở 4 câu then chốt, bóng phản chiếu "nhảy" kích thước (tim Warden + rung màn hình). Câu cuối: Gargantua là mắt nó.
 *  3. CLOSING: chớp mắt nhắm lại, trứng nở (chớp trắng) -> màn NO SIGNAL -> câu kết -> về menu chính (NoSignalScreen).
 *
 * Server chỉ gửi gói ENDING_START (EndingServer). Lệnh test: /samsara test ending.
 */
@Environment(EnvType.CLIENT)
public class EndingSequenceClient {
    private enum Phase { NONE, CINEMATIC, DIALOGUE, CLOSING }

    public static final int CINEMATIC_TICKS = 300;

    // Các cảnh quay (tick bắt đầu); giữa các cảnh là CẮT CẢNH có nháy đen ngắn, không bay camera.
    private static final int SHOT_CLOSE = 100;   // cảnh 2: sát vai người chơi, nhìn mặt nước
    private static final int SHOT_RISE = 150;    // cảnh 3: góc thấp bên cạnh, bóng phản chiếu trồi lên
    private static final int SHOT_RETURN = 270;  // cảnh 4: về lại mắt nhân vật
    private static final int RISE_START = 160;
    private static final int RISE_END = 250;
    private static final int[] CUTS = {SHOT_CLOSE, SHOT_RISE, SHOT_RETURN};
    private static final int CLOSING_TICKS = 90;

    private static final Random RANDOM = Random.create();

    private static Phase phase = Phase.NONE;
    private static int phaseTicks = 0;

    // Bối cảnh
    private static Vec3d eggPos = Vec3d.ZERO;
    private static Vec3d feet = Vec3d.ZERO;
    private static Vec3d forward = new Vec3d(0, 0, -1);
    private static Vec3d right = new Vec3d(1, 0, 0);

    // Bóng phản chiếu
    private static float reflectionScale = 1.0f;
    private static float prevReflectionScale = 1.0f;
    private static float targetScale = 1.0f;
    private static boolean eyeRevealed = false;

    // Quả trứng nở
    private static float hatch = 0.0f;
    private static float hatchTarget = 0.0f;

    // Rung màn hình
    private static int shakeTicks = 0;

    // Đoạn thoại
    private static List<EndingDialogue.Step> script = List.of();
    private static int stepIndex = 0;
    private static int stepTicks = 0;
    private static int typedChars = 0;
    private static boolean stepDone = false;

    private static Perspective savedPerspective = Perspective.FIRST_PERSON;
    private static boolean savedHudHidden = false;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(EndingServer.ENDING_START, (client, handler, buf, sender) -> {
            Vec3d egg = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3d player = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
            client.execute(() -> start(egg, player));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(EndingSequenceClient::reset));
        ClientTickEvents.END_CLIENT_TICK.register(EndingSequenceClient::tick);
        HudRenderCallback.EVENT.register(EndingSequenceClient::renderOverlay);
        MirrorSelfRenderer.register();
    }

    // ==========================================
    // TRẠNG THÁI CÔNG KHAI (renderer / mixin đọc)
    // ==========================================

    public static boolean isActive() { return phase != Phase.NONE; }
    public static boolean isMovementLocked() { return phase != Phase.NONE; }
    public static boolean isCinematic() { return phase == Phase.CINEMATIC; }

    public static float getHatchProgress(float tickDelta) { return phase == Phase.NONE ? 0.0f : hatch; }

    public static float getReflectionScale(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevReflectionScale, reflectionScale);
    }

    public static boolean isEyeRevealed() { return eyeRevealed; }

    /** Bóng phản chiếu có đang hiện không (từ lúc trồi lên trong cinematic tới hết đoạn kết). */
    public static boolean isReflectionVisible() {
        return (phase == Phase.CINEMATIC && phaseTicks >= RISE_START) || phase == Phase.DIALOGUE || phase == Phase.CLOSING;
    }

    /** Độ trồi lên khỏi mặt nước (0 = chìm hẳn, 1 = đứng thẳng). */
    public static float getRise(float tickDelta) {
        if (phase != Phase.CINEMATIC) return 1.0f;
        float t = (phaseTicks + tickDelta - RISE_START) / (float) (RISE_END - RISE_START);
        return smooth(MathHelper.clamp(t, 0.0f, 1.0f));
    }

    /** Vị trí chân của bóng phản chiếu: càng lớn càng lùi xa ra sau Quả Trứng. */
    public static Vec3d getReflectionPos(float tickDelta) {
        float s = getReflectionScale(tickDelta);
        double dist;
        if (s <= 2.0f) dist = 3.0;
        else if (s <= 4.0f) dist = MathHelper.lerp((s - 2.0f) / 2.0f, 3.0, 10.0);
        else if (s <= 12.0f) dist = MathHelper.lerp((s - 4.0f) / 8.0f, 10.0, 24.0);
        else dist = MathHelper.lerp((s - 12.0f) / (EndingDialogue.EYE_REVEAL_SCALE - 12.0f), 24.0, 72.0);
        Vec3d p = feet.add(forward.multiply(dist));

        // Khi lớn và lùi ra khỏi hồ gương lơ lửng, chân nó hạ dần xuống mặt nước/mặt đất thật bên dưới:
        // ở câu cuối nó đứng từ biển sâu vươn lên ngang trời thay vì lơ lửng giữa không trung.
        // Chân hạ đều theo kích thước (luôn giữ đầu cao hơn người chơi), tối đa tới mặt nước ở xa phía dưới.
        if (s > 3.5f) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                Vec3d far = feet.add(forward.multiply(72.0));
                int ground = client.world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,
                        MathHelper.floor(far.x), MathHelper.floor(far.z));
                if (ground > client.world.getBottomY() && ground - 1.0 < feet.y) {
                    double y = Math.max(ground - 1.0, feet.y - 1.08 * (s - 3.5f));
                    p = new Vec3d(p.x, Math.min(feet.y, y), p.z);
                }
            }
        }
        return p;
    }

    /** Hướng bóng phản chiếu quay mặt (nhìn về người chơi). */
    public static float getReflectionYaw() {
        Vec3d back = forward.multiply(-1);
        return (float) (MathHelper.atan2(back.z, back.x) * (180.0 / Math.PI)) - 90.0f;
    }

    // ==========================================
    // BẮT ĐẦU / KẾT THÚC
    // ==========================================

    public static void start(Vec3d egg, Vec3d playerFeet) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        reset();
        eggPos = egg;
        feet = playerFeet;
        Vec3d f = new Vec3d(egg.x - playerFeet.x, 0, egg.z - playerFeet.z);
        forward = f.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, -1) : f.normalize();
        right = new Vec3d(-forward.z, 0, forward.x);

        script = EndingDialogue.script();
        savedPerspective = client.options.getPerspective();
        savedHudHidden = client.options.hudHidden;
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        client.options.hudHidden = true;
        if (client.currentScreen != null) client.setScreen(null);
        enter(Phase.CINEMATIC);
    }

    private static void reset() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (phase == Phase.CINEMATIC && client.options != null) {
            client.options.setPerspective(savedPerspective);
            client.options.hudHidden = savedHudHidden;
        }
        phase = Phase.NONE;
        phaseTicks = 0;
        reflectionScale = prevReflectionScale = targetScale = 1.0f;
        eyeRevealed = false;
        hatch = hatchTarget = 0.0f;
        shakeTicks = 0;
        stepIndex = 0;
        stepTicks = 0;
        typedChars = 0;
        stepDone = false;
    }

    private static void enter(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    // ==========================================
    // TICK
    // ==========================================

    private static void tick(MinecraftClient client) {
        if (phase == Phase.NONE) return;
        if (client.player == null || client.world == null) {
            reset();
            return;
        }
        phaseTicks++;
        if (shakeTicks > 0) shakeTicks--;

        prevReflectionScale = reflectionScale;
        reflectionScale += (targetScale - reflectionScale) * 0.12f;
        hatch += (hatchTarget - hatch) * 0.05f;

        switch (phase) {
            case CINEMATIC -> tickCinematic(client);
            case DIALOGUE -> tickDialogue(client);
            case CLOSING -> tickClosing(client);
            default -> { }
        }
    }

    private static void tickCinematic(MinecraftClient client) {
        // Mặt nước gợn sóng và sủi bọt nơi bóng phản chiếu trồi lên
        if (phaseTicks >= SHOT_CLOSE + 20 && phaseTicks <= RISE_END) {
            Vec3d p = getReflectionPos(0);
            for (int i = 0; i < 3; i++) {
                client.world.addParticle(i == 0 ? ParticleTypes.SPLASH : ParticleTypes.BUBBLE_POP,
                        p.x + (RANDOM.nextDouble() - 0.5) * 1.6, feet.y + 0.1, p.z + (RANDOM.nextDouble() - 0.5) * 1.6,
                        0, 0.05, 0);
            }
            if (phaseTicks % 20 == 0) {
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.8f));
            }
        }
        if (phaseTicks == SHOT_RETURN) {
            // Cảnh cuối đặt máy trong mắt nhân vật: về góc nhìn thứ nhất để không thấy bên trong đầu
            client.options.setPerspective(Perspective.FIRST_PERSON);
        }
        if (phaseTicks == RISE_START) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.AMBIENT_UNDERWATER_EXIT, 0.6f, 0.9f));
        }

        if (phaseTicks >= CINEMATIC_TICKS) {
            client.options.setPerspective(Perspective.FIRST_PERSON);
            client.options.hudHidden = savedHudHidden;
            // Trả quyền nhìn cho người chơi, mặt đối mặt với bóng phản chiếu
            Vec3d face = getReflectionPos(1).add(0, 1.6, 0);
            Vec3d eye = client.player.getEyePos();
            Vec3d d = face.subtract(eye);
            client.player.setYaw((float) (MathHelper.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90.0f);
            client.player.setPitch((float) -(MathHelper.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * (180.0 / Math.PI)));
            enter(Phase.DIALOGUE);
        }
    }

    // ---------------- Đoạn thoại ----------------

    private static void tickDialogue(MinecraftClient client) {
        if (stepIndex >= script.size()) {
            enter(Phase.CLOSING);
            return;
        }
        EndingDialogue.Step step = script.get(stepIndex);
        stepTicks++;

        switch (step.kind()) {
            case WAIT -> {
                if (stepTicks >= step.ticks()) nextStep();
            }
            case HATCH -> {
                hatchTarget = step.value();
                nextStep();
            }
            case JUMP -> {
                if (stepTicks == 1) {
                    targetScale = step.value();
                    shakeTicks = 14;
                    client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.55f, 1.0f));
                    client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), 0.5f, 0.6f));
                    if (step.value() >= EndingDialogue.EYE_REVEAL_SCALE) eyeRevealed = true;
                }
                if (stepTicks >= 40) nextStep();
            }
            case SYSTEM -> {
                // <System> "suy nghĩ" một nhịp rồi trả lời; thời gian đọc tỉ lệ với độ dài câu
                if (stepTicks == 30) {
                    client.inGameHud.getChatHud().addMessage(Text.literal("§e<System> " + step.text()));
                    client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.4f));
                }
                if (stepTicks >= 30 + 40 + (int) (step.text().length() * 1.3f)) nextStep();
            }
            case ME -> tickTyping(client, step);
        }
    }

    /** Nhân vật chính tự gõ vào ô chat (khoảng 20 ký tự/giây), dừng một nhịp rồi "gửi". */
    private static void tickTyping(MinecraftClient client, EndingDialogue.Step step) {
        String text = step.text();
        if (stepTicks == 1) {
            client.setScreen(new ChatScreen(""));
        }
        if (!stepDone && stepTicks > 6) {
            if (typedChars < text.length()) {
                typedChars = Math.min(text.length(), typedChars + 1 + (RANDOM.nextFloat() < 0.25f ? 1 : 0));
                if (client.currentScreen instanceof ChatScreen chat) {
                    ((ChatScreenAccessor) chat).samsara$getChatField().setText(text.substring(0, typedChars));
                }
                if (RANDOM.nextFloat() < 0.5f) {
                    client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.8f, 0.04f));
                }
            } else {
                stepDone = true;
                stepTicks = 0;
            }
            return;
        }
        if (stepDone && stepTicks == 12) {
            if (client.currentScreen instanceof ChatScreen) client.setScreen(null);
            client.inGameHud.getChatHud().addMessage(Text.literal("<" + displayName(client, step.name()) + "> " + text));
        }
        if (stepDone && stepTicks >= 40) nextStep();
    }

    private static String displayName(MinecraftClient client, EndingDialogue.NameMode mode) {
        return switch (mode) {
            case NAM -> "Nam_1998";
            case GLITCH -> "N§ka§rm_§k1§r9§k9§r8";
            case REAL -> client.player != null ? client.player.getName().getString() : "Nam_1998";
        };
    }

    private static void nextStep() {
        stepIndex++;
        stepTicks = 0;
        typedChars = 0;
        stepDone = false;
    }

    // ---------------- Nhắm mắt, trứng nở ----------------

    private static void tickClosing(MinecraftClient client) {
        hatchTarget = 1.0f;
        if (phaseTicks == 60) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_GLASS_BREAK, 0.4f, 1.0f));
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 0.8f));
            shakeTicks = 20;
        }
        if (phaseTicks >= CLOSING_TICKS) {
            phase = Phase.NONE; // giữ nguyên thế giới phía sau; NoSignalScreen lo phần còn lại
            client.setScreen(new NoSignalScreen());
        }
    }

    // ==========================================
    // CAMERA (gọi từ CameraEndingMixin)
    // ==========================================

    /** Vị trí + điểm nhìn của camera cinematic; null nếu không ở pha cinematic. */
    public static Vec3d[] cinematicCamera(float tickDelta, Vec3d playerEye) {
        if (phase != Phase.CINEMATIC) return null;
        float t = phaseTicks + tickDelta;
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d eggTop = eggPos.add(0, 6.0, 0);
        Vec3d refl = getReflectionPos(tickDelta);

        // Mọi cảnh đều chuyển động rất chậm (đẩy / nâng vài block trong nhiều giây), giữa các cảnh là cắt cảnh.
        if (t < SHOT_CLOSE) {
            // CẢNH 1 - TOÀN CẢNH: máy đặt xa phía sau, thấp sát mặt nước, ngửa lên.
            // Người chơi là một chấm nhỏ trước Quả Trứng khổng lồ và bầu trời vũ trụ phía trên.
            float k = ease(t / SHOT_CLOSE);
            Vec3d from = feet.subtract(forward.multiply(26.0)).add(right.multiply(7.0)).add(up.multiply(0.8));
            Vec3d to = feet.subtract(forward.multiply(23.0)).add(right.multiply(6.0)).add(up.multiply(1.6));
            Vec3d lookFrom = eggTop.add(up.multiply(9.0));
            Vec3d lookTo = eggTop.add(up.multiply(6.0));
            return new Vec3d[]{from.lerp(to, k), lookFrom.lerp(lookTo, k)};
        }
        if (t < SHOT_RISE) {
            // CẢNH 2 - SÁT VAI: ngay sau lưng người chơi, nhìn xuống mặt nước phía trước đang bắt đầu gợn.
            float k = ease((t - SHOT_CLOSE) / (SHOT_RISE - SHOT_CLOSE));
            Vec3d base = playerEye.subtract(forward.multiply(1.7)).add(right.multiply(0.7)).add(up.multiply(0.25));
            Vec3d water = feet.add(forward.multiply(3.0));
            return new Vec3d[]{base.add(forward.multiply(0.4 * k)), water.add(up.multiply(0.2))};
        }
        if (t < SHOT_RETURN) {
            // CẢNH 3 - BÓNG TRỒI LÊN: máy thấp sát mặt nước bên cạnh, từ từ ngước lên theo cái bóng.
            float k = ease((t - SHOT_RISE) / (SHOT_RETURN - SHOT_RISE));
            Vec3d base = feet.add(forward.multiply(1.6)).add(right.multiply(2.6)).add(up.multiply(0.25));
            Vec3d pos = base.subtract(right.multiply(0.5 * k)).add(up.multiply(0.35 * k));
            float rise = getRise(tickDelta);
            Vec3d look = refl.add(up.multiply(0.3 + 1.4 * rise));
            return new Vec3d[]{pos, look};
        }
        // CẢNH 4 - VỀ MẮT: đứng yên trong mắt nhân vật, nhìn thẳng vào bóng phản chiếu.
        return new Vec3d[]{playerEye, refl.add(up.multiply(1.6))};
    }

    /** Độ lệch rung màn hình (block). */
    public static Vec3d shakeOffset() {
        if (shakeTicks <= 0) return Vec3d.ZERO;
        double a = shakeTicks / 14.0 * 0.12;
        return new Vec3d((RANDOM.nextDouble() - 0.5) * a, (RANDOM.nextDouble() - 0.5) * a, (RANDOM.nextDouble() - 0.5) * a);
    }

    /** Easing rất mềm cho chuyển động máy chậm (không có cú giật ở đầu / cuối cảnh). */
    private static float ease(float x) {
        x = MathHelper.clamp(x, 0.0f, 1.0f);
        return x * x * x * (x * (x * 6.0f - 15.0f) + 10.0f);
    }

    private static float smooth(float x) {
        return x * x * (3.0f - 2.0f * x);
    }

    // ==========================================
    // LỚP PHỦ (viền đen cinematic, chớp mắt, chớp trắng)
    // ==========================================

    private static void renderOverlay(DrawContext ctx, float tickDelta) {
        if (phase == Phase.NONE) return;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        if (phase == Phase.CINEMATIC) {
            // Viền đen điện ảnh trượt vào / ra
            float t = phaseTicks + tickDelta;
            float k = Math.min(1.0f, Math.min(t / 20.0f, (CINEMATIC_TICKS - t) / 20.0f));
            int bar = (int) (h * 0.11f * Math.max(0.0f, k));
            ctx.fill(0, 0, w, bar, 0xFF000000);
            ctx.fill(0, h - bar, w, h, 0xFF000000);

            // Mở đầu từ màn đen; mỗi lần cắt cảnh nháy đen thật ngắn
            float black = 1.0f - MathHelper.clamp(t / 30.0f, 0.0f, 1.0f);
            for (int cut : CUTS) {
                float d = Math.abs(t - cut);
                if (d < 5.0f) black = Math.max(black, 1.0f - d / 5.0f);
            }
            if (black > 0.0f) {
                ctx.fill(0, 0, w, h, ((int) (black * 255) << 24));
            }
        }

        if (phase == Phase.CLOSING) {
            float t = phaseTicks + tickDelta;
            // Hai lần chớp mắt nặng trĩu rồi nhắm hẳn
            float close;
            if (t < 14) close = (float) Math.sin(t / 14.0 * Math.PI) * 0.7f;
            else if (t < 30) close = (float) Math.sin((t - 14) / 16.0 * Math.PI) * 0.85f;
            else close = MathHelper.clamp((t - 30) / 25.0f, 0.0f, 1.0f);
            int lid = (int) (h * 0.5f * close);
            ctx.fill(0, 0, w, lid, 0xFF000000);
            ctx.fill(0, h - lid, w, h, 0xFF000000);
            if (t >= 60) {
                float white = 1.0f - MathHelper.clamp((t - 60) / 30.0f, 0.0f, 1.0f);
                ctx.fill(0, 0, w, h, ((int) (white * 255) << 24) | 0xFFF4E0);
            }
        }
    }
}
