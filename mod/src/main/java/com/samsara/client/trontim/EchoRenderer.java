package com.samsara.client.trontim;

import com.samsara.registry.SamsaraDimensions;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tầng 3 "Trốn tìm": mọi thứ tối đen (shader world5), chỉ thấy được nhờ âm thanh. Mỗi tiếng động tạo một vòng sóng lan ra.
 * Ngay dải sóng, khối hiện đúng texture Minecraft (ngả xám) trong một thoáng, rồi chỉ còn nét viền trắng mờ dần.
 * Người (người chơi khác, zombie...) hiện thành khung đúng dáng Steve, chụp lại tư thế lúc sóng chạm tới; bảng hiện chữ.
 *
 * Nét viền vẽ bằng position_color nên đi qua gbuffers_basic của world5 (giữ màu). Texture và chữ đi qua gbuffers_textured,
 * vốn tô đen mọi thứ, trừ khi màu đỉnh mang dấu tiếng vọng (g = b = 253/255, xem {@link #MARK}).
 * Các mặt đen vẫn ghi độ sâu nên mọi thứ sau tường bị che đúng như thật.
 */
public final class EchoRenderer {
    private EchoRenderer() {}

    /** Sóng lan bao nhiêu block mỗi giây. */
    private static final double SPEED = 13.0;
    /** Nét viền: sáng hẳn trong FLASH giây rồi tắt dần theo hằng số FADE. */
    private static final double FLASH = 0.18;
    private static final double FADE = 0.85;
    /** Texture thật chỉ hiện trong TEXTURE giây sau khi sóng chạm. */
    private static final double TEXTURE = 0.5;
    /** Texture chỉ hiện trong phần này của bán kính sóng; xa hơn chỉ còn viền trắng. */
    private static final double DETAIL = 0.65;
    private static final double LIFE = 3.2;
    private static final float WEAK = 0.16f;
    private static final int MAX_RADIUS = 22;
    /** Dấu tiếng vọng trong màu đỉnh: shader world5 chỉ cho texture sáng khi g = b = 253/255. */
    private static final float MARK = 253f / 255f;

    private record Edge(float x1, float y1, float z1, float x2, float y2, float z2, float dist, boolean strong, boolean figure) {}

    /** Một mặt khối: 4 đỉnh (toạ độ thế giới) và UV trên atlas. */
    private record Face(float[] xyz, float[] uv, float nx, float ny, float nz, float dist) {}

    private record SignEcho(BlockPos pos, BlockState state, Text[] lines, float dist) {}

    /** Đứa em / kẻ ngồi trốn trong tầm sóng: được vẽ lại bằng skin thật trong một thoáng. */
    private record Figure(int id, float dist) {}

    /** Màu sóng của tiếng do đứa em gây ra (tiếng cười, bước chân nó): vàng ấm. */
    private static final float[] WARM = {1.0f, 0.8f, 0.45f};

    private static final class Ping {
        final double start;
        final float radius;
        final float r, g, b;
        final List<Edge> edges = new ArrayList<>();
        final List<Face> faces = new ArrayList<>();
        final List<SignEcho> signs = new ArrayList<>();
        final List<Figure> figures = new ArrayList<>();

        Ping(double start, float radius, float r, float g, float b) {
            this.start = start;
            this.radius = radius;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static final List<Ping> PINGS = new ArrayList<>();
    private static double lastStep = -1;

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(EchoRenderer::render);
        // Khung chọn khối (đi qua gbuffers_basic) sẽ lộ khối đang nhìn: tắt hẳn trong chiều này
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hit) -> !inFloor(MinecraftClient.getInstance()));
        // Máy quay vô dụng ở tầng này: áp lên mắt chỉ ra nhiễu đen và "KHÔNG CÓ TÍN HIỆU"
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!inFloor(client) || !com.samsara.client.CamcorderClientManager.isFullyAimed()) return;
            int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
            ctx.fill(0, 0, w, h, 0xFF050505);
            java.util.Random noise = new java.util.Random(System.nanoTime() / 40_000_000L);
            for (int i = 0; i < 260; i++) {
                int x = noise.nextInt(w), y = noise.nextInt(h), g = 20 + noise.nextInt(50);
                ctx.fill(x, y, x + 1 + noise.nextInt(3), y + 1, 0xFF000000 | g << 16 | g << 8 | g);
            }
            boolean blink = (System.currentTimeMillis() / 600) % 2 == 0;
            if (blink) ctx.drawCenteredTextWithShadow(client.textRenderer, Text.translatable("samsara.tron_tim.mat_tin_hieu"), w / 2, h / 2 - 4, 0xFFDDDDDD);
        });
    }

    public static boolean inFloor(MinecraftClient client) {
        return client.world != null && client.world.getRegistryKey() == SamsaraDimensions.TRON_TIM_WORLD_KEY;
    }

    private static double now() {
        return System.nanoTime() / 1.0e9;
    }

    /** Gọi từ SoundSystem.play: mọi âm có vị trí trong chiều này đều tạo một vòng sóng. */
    public static void onSound(SoundInstance sound) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!inFloor(client) || sound.getSound() == null) return;
        if (sound.isRelative() || sound.getAttenuationType() == SoundInstance.AttenuationType.NONE) return;
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        Vec3d pos = new Vec3d(sound.getX(), sound.getY(), sound.getZ());
        String path = sound.getId().getPath();
        boolean step = path.endsWith(".step") && pos.squaredDistanceTo(player.getPos()) < 4.0;
        float radius;
        if (step) {
            if (player.isSneaking()) return;
            double t = now();
            if (t - lastStep < 0.12) return;
            lastStep = t;
            radius = player.isSprinting() ? 8.0f : 5.5f;
            pos = player.getPos().add(0, 0.1, 0);
        } else {
            radius = Math.min(MAX_RADIUS, 6.0f + 10.0f * sound.getVolume());
        }
        Box near = new Box(pos, pos).expand(2.0);
        boolean em = !client.world.getEntitiesByClass(com.samsara.entity.EmEntity.class, near, e -> true).isEmpty();
        // Chỉ tiếng do người chơi gây ra (gần người chơi) hoặc tiếng của chính đứa em mới làm lộ nó;
        // tiếng của căn nhà (đồng hồ, vòi nước...) soi đồ đạc nhưng không soi người, kẻo trốn như không
        boolean reveal = em || pos.squaredDistanceTo(player.getPos()) < 16.0;
        if (em) ping(pos, radius, WARM[0], WARM[1], WARM[2], true);
        else ping(pos, radius, 1.0f, 1.0f, 1.0f, reveal);
    }

    /** Một vòng sóng từ {@code center}, lan tới {@code radius} block. */
    public static void ping(Vec3d center, float radius, float r, float g, float b) {
        ping(center, radius, r, g, b, true);
    }

    public static void ping(Vec3d center, float radius, float r, float g, float b, boolean revealFigures) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        Ping ping = new Ping(now(), radius, r, g, b);
        scanBlocks(client, client.world, center, radius, ping);
        if (revealFigures) scanEntities(client, client.world, center, radius, ping);
        PINGS.add(ping);
    }

    // ==========================================
    // QUÉT KHỐI: NÉT VIỀN, MẶT CÓ TEXTURE, BẢNG
    // ==========================================

    private static void scanBlocks(MinecraftClient client, ClientWorld world, Vec3d c, float radius, Ping ping) {
        int r = (int) Math.ceil(radius);
        BlockPos origin = BlockPos.ofFloored(c);
        BlockPos.Mutable p = new BlockPos.Mutable();
        Random random = Random.create();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > (r + 1) * (r + 1)) continue;
                    p.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = world.getBlockState(p);
                    if (state.isAir()) continue;
                    VoxelShape shape = state.getOutlineShape(world, p);
                    if (shape.isEmpty()) continue;
                    BlockPos pos = p.toImmutable();
                    if (isFull(world, pos)) fullCubeEdges(world, pos, c, radius, ping.edges);
                    else if (state.getBlock() instanceof net.minecraft.block.AbstractSignBlock) signEdges(state, pos, c, radius, ping.edges);
                    else shapeEdges(shape, pos, c, radius, ping.edges);
                    faces(client, world, state, pos, c, radius, random, ping.faces);
                    if (world.getBlockEntity(pos) instanceof SignBlockEntity sign) {
                        double d = Vec3d.ofCenter(pos).distanceTo(c);
                        if (d <= radius) {
                            Text[] lines = new Text[4];
                            for (int i = 0; i < 4; i++) lines[i] = sign.getFrontText().getMessage(i, false);
                            ping.signs.add(new SignEcho(pos, state, lines, (float) d));
                        }
                    }
                }
            }
        }
    }

    private static boolean isFull(ClientWorld world, BlockPos p) {
        return world.getBlockState(p).isFullCube(world, p);
    }

    /** Các mặt của model khối (đúng texture từng mặt), bỏ mặt bị khối bên cạnh che. */
    private static void faces(MinecraftClient client, ClientWorld world, BlockState state, BlockPos pos, Vec3d c, float radius,
                              Random random, List<Face> out) {
        BakedModel model = client.getBlockRenderManager().getModel(state);
        for (Direction dir : Direction.values()) {
            if (!Block.shouldDrawSide(state, world, pos, dir, pos.offset(dir))) continue;
            random.setSeed(42L);
            for (BakedQuad q : model.getQuads(state, dir, random)) addFace(q, pos, c, radius, out);
        }
        random.setSeed(42L);
        for (BakedQuad q : model.getQuads(state, null, random)) addFace(q, pos, c, radius, out);
    }

    private static void addFace(BakedQuad quad, BlockPos pos, Vec3d c, float radius, List<Face> out) {
        int[] v = quad.getVertexData();
        int stride = v.length / 4;
        float[] xyz = new float[12];
        float[] uv = new float[8];
        double cx = 0, cy = 0, cz = 0;
        for (int i = 0; i < 4; i++) {
            xyz[i * 3] = Float.intBitsToFloat(v[i * stride]) + pos.getX();
            xyz[i * 3 + 1] = Float.intBitsToFloat(v[i * stride + 1]) + pos.getY();
            xyz[i * 3 + 2] = Float.intBitsToFloat(v[i * stride + 2]) + pos.getZ();
            uv[i * 2] = Float.intBitsToFloat(v[i * stride + 4]);
            uv[i * 2 + 1] = Float.intBitsToFloat(v[i * stride + 5]);
            cx += xyz[i * 3] / 4;
            cy += xyz[i * 3 + 1] / 4;
            cz += xyz[i * 3 + 2] / 4;
        }
        double d = Math.sqrt((cx - c.x) * (cx - c.x) + (cy - c.y) * (cy - c.y) + (cz - c.z) * (cz - c.z));
        if (d > radius * DETAIL) return;
        Vector3f n = quad.getFace().getUnitVector();
        out.add(new Face(xyz, uv, n.x, n.y, n.z, (float) d));
    }

    /**
     * Khối đầy: chỉ vẽ cạnh lồi và cạnh lõm thật đậm; cạnh nằm giữa một mặt phẳng liền (sàn, tường) vẽ rất mờ để vẫn
     * đoán được mặt phẳng mà không thành lưới ô vuông chói mắt.
     */
    private static void fullCubeEdges(ClientWorld world, BlockPos p, Vec3d c, float radius, List<Edge> out) {
        Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length; i++) {
            for (int j = i + 1; j < dirs.length; j++) {
                Direction d1 = dirs[i], d2 = dirs[j];
                if (d1.getAxis() == d2.getAxis()) continue;
                boolean e1 = !isFull(world, p.offset(d1));
                boolean e2 = !isFull(world, p.offset(d2));
                if (!e1 && !e2) continue;
                boolean strong;
                if (e1 && e2) {
                    strong = true;
                } else {
                    Direction open = e1 ? d1 : d2;
                    Direction closed = e1 ? d2 : d1;
                    strong = isFull(world, p.offset(closed).offset(open));   // góc lõm
                }
                addCubeEdge(p, d1, d2, c, radius, strong, out);
            }
        }
    }

    /** Cạnh chung của hai mặt {@code d1}, {@code d2} của khối ở {@code p}. */
    private static void addCubeEdge(BlockPos p, Direction d1, Direction d2, Vec3d c, float radius, boolean strong, List<Edge> out) {
        Direction.Axis axis = null;
        for (Direction.Axis a : Direction.Axis.values()) {
            if (a != d1.getAxis() && a != d2.getAxis()) axis = a;
        }
        double[] lo = {p.getX(), p.getY(), p.getZ()};
        double[] hi = {p.getX(), p.getY(), p.getZ()};
        for (Direction d : new Direction[]{d1, d2}) {
            int k = d.getAxis().ordinal();
            double v = d.getDirection() == Direction.AxisDirection.POSITIVE ? lo[k] + 1 : lo[k];
            lo[k] = v;
            hi[k] = v;
        }
        hi[axis.ordinal()] += 1;
        add(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2], c, radius, strong, out);
    }

    /** Khối không đầy (bậc thang, rào, giường, bàn...): vẽ đủ 12 cạnh của từng hộp. */
    /**
     * Bảng: hitbox chỉ là một cột vuông, nên dựng lại đúng hình tấm bảng (và cọc nếu là bảng cắm đất) theo model gốc:
     * tấm 24x12x2, cọc 2x14x2 pixel model, thu nhỏ 2/3, xoay theo hướng bảng như SignBlockEntityRenderer.
     */
    private static void signEdges(BlockState state, BlockPos p, Vec3d c, float radius, List<Edge> out) {
        boolean wall = state.getBlock() instanceof WallSignBlock;
        float degrees = wall ? state.get(WallSignBlock.FACING).asRotation()
                : state.contains(SignBlock.ROTATION) ? state.get(SignBlock.ROTATION) * 22.5f : 0f;
        double rad = Math.toRadians(-degrees);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        double k = 0.6666667 / 16.0;
        double dy = wall ? -0.3125 : 0.0, dz = wall ? -0.4375 : 0.0;
        // {x0, x1, y0, y1, z0, z1} theo pixel model, y đã lật (model vẽ với scale y âm)
        List<double[]> parts = new ArrayList<>();
        parts.add(new double[]{-12, 12, 2, 14, -1, 1});
        if (!wall) parts.add(new double[]{-1, 1, -12, 2, -1, 1});
        for (double[] b : parts) {
            double[][] corner = new double[8][];
            int n = 0;
            for (int iy = 0; iy < 2; iy++) {
                for (int iz = 0; iz < 2; iz++) {
                    for (int ix = 0; ix < 2; ix++) {
                        double lx = b[ix] * k, ly = b[2 + iy] * k + dy, lz = b[4 + iz] * k + dz;
                        double wx = cos * lx + sin * lz, wz = -sin * lx + cos * lz;
                        corner[n++] = new double[]{p.getX() + 0.5 + wx, p.getY() + 0.5 + ly, p.getZ() + 0.5 + wz};
                    }
                }
            }
            int[][] pairs = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
            for (int[] pr : pairs) {
                double[] a = corner[pr[0]], e = corner[pr[1]];
                add(a[0], a[1], a[2], e[0], e[1], e[2], c, radius, true, out);
            }
        }
    }

    private static void shapeEdges(VoxelShape shape, BlockPos p, Vec3d c, float radius, List<Edge> out) {
        for (Box b : shape.getBoundingBoxes()) boxEdges(b.offset(p), c, radius, out);
    }

    private static void boxEdges(Box b, Vec3d c, float radius, List<Edge> out) {
        double x0 = b.minX, y0 = b.minY, z0 = b.minZ, x1 = b.maxX, y1 = b.maxY, z1 = b.maxZ;
        for (double y : new double[]{y0, y1}) {
            add(x0, y, z0, x1, y, z0, c, radius, true, out);
            add(x0, y, z1, x1, y, z1, c, radius, true, out);
            add(x0, y, z0, x0, y, z1, c, radius, true, out);
            add(x1, y, z0, x1, y, z1, c, radius, true, out);
        }
        add(x0, y0, z0, x0, y1, z0, c, radius, true, out);
        add(x1, y0, z0, x1, y1, z0, c, radius, true, out);
        add(x0, y0, z1, x0, y1, z1, c, radius, true, out);
        add(x1, y0, z1, x1, y1, z1, c, radius, true, out);
    }

    private static void add(double x1, double y1, double z1, double x2, double y2, double z2, Vec3d c, float radius,
                            boolean strong, List<Edge> out) {
        add(x1, y1, z1, x2, y2, z2, c, radius, strong, false, out);
    }

    private static void add(double x1, double y1, double z1, double x2, double y2, double z2, Vec3d c, float radius,
                            boolean strong, boolean figure, List<Edge> out) {
        double mx = (x1 + x2) / 2 - c.x, my = (y1 + y2) / 2 - c.y, mz = (z1 + z2) / 2 - c.z;
        double d = Math.sqrt(mx * mx + my * my + mz * mz);
        if (d > radius) return;
        out.add(new Edge((float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, (float) d, strong, figure));
    }

    // ==========================================
    // QUÉT NGƯỜI: KHUNG DÁNG STEVE
    // ==========================================

    private static boolean humanoid(Entity e) {
        return (e instanceof PlayerEntity && e != MinecraftClient.getInstance().player)
                || e instanceof ZombieEntity || e instanceof AbstractSkeletonEntity || e instanceof com.samsara.entity.EmEntity;
    }

    /** Tư thế lúc sóng quét qua được giữ lại như một bóng ma: sáu hộp đầu, thân, hai tay, hai chân (đơn vị pixel, 16 = 1 block). */
    private static void scanEntities(MinecraftClient client, ClientWorld world, Vec3d c, float radius, Ping ping) {
        Box area = new Box(c, c).expand(radius);
        for (Entity e : world.getOtherEntities(null, area, EchoRenderer::humanoid)) {
            if (!(e instanceof LivingEntity living)) continue;
            Vec3d base = e.getPos();
            if (base.distanceTo(c) > radius) continue;
            float body = (float) Math.toRadians(living.bodyYaw);
            float head = (float) Math.toRadians(living.headYaw);
            float scale = living.isBaby() ? 0.5f : 1.0f;
            boolean sitting = e instanceof com.samsara.entity.EmEntity em && em.isHider();
            // {tâm x, đáy y, rộng x, cao y, sâu z, quay theo đầu?, tâm z}
            float[][] parts = sitting ? new float[][]{
                    {-2, 0, 4, 4, 12, 0, 6}, {2, 0, 4, 4, 12, 0, 6},  // chân duỗi ra trước, bó gối
                    {0, 0, 8, 12, 4, 0, 0},                           // thân ngồi bệt
                    {-6, 1, 4, 12, 4, 0, 1}, {6, 1, 4, 12, 4, 0, 1},  // tay ôm gối
                    {0, 12, 8, 8, 8, 1, 0}                            // đầu
            } : new float[][]{
                    {-2, 0, 4, 12, 4, 0, 0}, {2, 0, 4, 12, 4, 0, 0},  // chân
                    {0, 12, 8, 12, 4, 0, 0},                          // thân
                    {-6, 12, 4, 12, 4, 0, 0}, {6, 12, 4, 12, 4, 0, 0}, // tay
                    {0, 24, 8, 8, 8, 1, 0}                            // đầu
            };
            if (e instanceof com.samsara.entity.EmEntity) ping.figures.add(new Figure(e.getId(), (float) base.distanceTo(c)));
            for (float[] part : parts) {
                float yaw = part[5] > 0 ? head : body;
                partEdges(base, yaw, scale, part, c, radius, ping.edges);
            }
        }
    }

    private static void partEdges(Vec3d base, float yaw, float scale, float[] part, Vec3d c, float radius, List<Edge> out) {
        float s = scale / 16f;
        float hx = part[2] / 2, hz = part[4] / 2;
        float[][] corners = new float[8][3];
        int k = 0;
        for (int iy = 0; iy < 2; iy++) {
            for (int iz = 0; iz < 2; iz++) {
                for (int ix = 0; ix < 2; ix++) {
                    float lx = part[0] + (ix == 0 ? -hx : hx);
                    float lz = part[6] + (iz == 0 ? -hz : hz);
                    float ly = part[1] + (iy == 0 ? 0 : part[3]);
                    // Model nhìn về +z khi yaw = 0; xoay quanh trục đứng theo yaw của Minecraft
                    float cos = (float) Math.cos(yaw), sin = (float) Math.sin(yaw);
                    float wx = lx * cos - lz * sin;
                    float wz = lx * sin + lz * cos;
                    corners[k][0] = (float) base.x + wx * s;
                    corners[k][1] = (float) base.y + ly * s;
                    corners[k][2] = (float) base.z + wz * s;
                    k++;
                }
            }
        }
        int[][] pairs = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] pr : pairs) {
            float[] a = corners[pr[0]], b = corners[pr[1]];
            add(a[0], a[1], a[2], b[0], b[1], b[2], c, radius, true, true, out);
        }
    }

    // ==========================================
    // VẼ
    // ==========================================

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!inFloor(client)) {
            PINGS.clear();
            return;
        }
        if (PINGS.isEmpty() || context.consumers() == null || context.matrixStack() == null) return;
        double t = now();
        Camera camera = context.camera();
        Vec3d cam = camera.getPos();
        MatrixStack matrices = context.matrixStack();
        Matrix4f m = matrices.peek().getPositionMatrix();
        Matrix3f normal = matrices.peek().getNormalMatrix();

        for (Iterator<Ping> it = PINGS.iterator(); it.hasNext(); ) {
            if (t - it.next().start > MAX_RADIUS / SPEED + LIFE) it.remove();
        }

        // 1. Texture thật ngay dải sóng
        VertexConsumer tex = context.consumers().getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        for (Ping ping : PINGS) {
            double age = t - ping.start;
            for (Face f : ping.faces) {
                double since = age - f.dist / SPEED;
                // Gần tâm tiếng động: texture rõ và đọng lâu; xa dần thì nhạt và tắt sớm; ngoài DETAIL bán kính chỉ còn nét viền
                double k = 1.0 - f.dist / (ping.radius * DETAIL);
                if (k <= 0) continue;
                double life = TEXTURE * (0.35 + 1.4 * k);
                if (since < 0 || since > life) continue;
                double a = Math.pow(1.0 - since / life, 1.5) * Math.pow(k, 0.8) * 0.95;
                face(tex, m, normal, cam, f, (float) a);
            }
        }

        // 1b. Đứa em / kẻ ngồi trốn: model người chơi với skin thật, hiện một thoáng như texture khối
        net.minecraft.client.render.entity.EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        for (Ping ping : PINGS) {
            double age = t - ping.start;
            for (Figure fig : ping.figures) {
                double since = age - fig.dist / SPEED;
                double life = TEXTURE * 2.0;
                if (since < 0 || since > life) continue;
                Entity entity = client.world.getEntityById(fig.id);
                if (entity == null) continue;
                float delta = context.tickDelta();
                Vec3d pos = entity.getLerpedPos(delta).subtract(cam);
                EmRenderer.echoAlpha = (float) (Math.pow(1.0 - since / life, 1.3) * 0.95);
                EmRenderer.echoBright = 1.0f;
                dispatcher.render(entity, pos.x, pos.y, pos.z, entity.getYaw(delta), delta, matrices, context.consumers(),
                        LightmapTextureManager.MAX_LIGHT_COORDINATE);
                EmRenderer.echoAlpha = 0f;
            }
        }

        // 2. Nét viền
        VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getDebugQuads());
        for (Ping ping : PINGS) {
            double age = t - ping.start;
            for (Edge e : ping.edges) {
                double since = age - e.dist / SPEED;
                if (since < 0) continue;
                double a = since < FLASH ? 1.0 : Math.exp(-(since - FLASH) / FADE);
                // Người thì sáng rõ và nét dày hơn, không nhạt theo khoảng cách: phải nhận ra ngay dáng ai
                if (!e.figure) a *= 1.0 - 0.55 * (e.dist / ping.radius);
                if (!e.strong) a *= WEAK;
                if (a < 0.02) continue;
                strip(lines, m, cam, e, e.figure ? 0.0045f : e.strong ? 0.0025f : 0.0016f, ping.r, ping.g, ping.b, (float) Math.min(1.0, a));
            }
        }

        // 3. Chữ trên bảng: sáng lên khi sóng chạm và tắt chậm như nét viền
        TextRenderer font = client.textRenderer;
        for (Ping ping : PINGS) {
            double age = t - ping.start;
            for (SignEcho s : ping.signs) {
                double since = age - s.dist / SPEED;
                if (since < 0) continue;
                double a = since < FLASH ? 1.0 : Math.exp(-(since - FLASH) / (FADE * 1.6));
                if (a < 0.05) continue;
                signText(font, matrices, context, cam, s, (float) a);
            }
        }
    }

    private static void face(VertexConsumer buf, Matrix4f m, Matrix3f normal, Vec3d cam, Face f, float a) {
        for (int i = 0; i < 4; i++) {
            float x = (float) (f.xyz[i * 3] - cam.x) * 0.997f;
            float y = (float) (f.xyz[i * 3 + 1] - cam.y) * 0.997f;
            float z = (float) (f.xyz[i * 3 + 2] - cam.z) * 0.997f;
            buf.vertex(m, x, y, z).color(1.0f, MARK, MARK, a).texture(f.uv[i * 2], f.uv[i * 2 + 1])
                    .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                    .normal(normal, f.nx, f.ny, f.nz).next();
        }
    }

    /** Một nét mảnh luôn quay mặt về phía máy quay, kéo nhẹ về phía mắt để nằm trên cả mặt khối lẫn texture. */
    private static void strip(VertexConsumer buf, Matrix4f m, Vec3d cam, Edge e, float width, float r, float g, float b, float a) {
        Vector3f p1 = new Vector3f((float) (e.x1 - cam.x), (float) (e.y1 - cam.y), (float) (e.z1 - cam.z)).mul(0.993f);
        Vector3f p2 = new Vector3f((float) (e.x2 - cam.x), (float) (e.y2 - cam.y), (float) (e.z2 - cam.z)).mul(0.993f);
        Vector3f dir = new Vector3f(p2).sub(p1);
        Vector3f mid = new Vector3f(p1).add(p2).mul(0.5f);
        Vector3f side = dir.cross(mid, new Vector3f());
        if (side.lengthSquared() < 1e-8f) return;
        // Độ dày tính theo khoảng cách tới mắt: nét trông mảnh như nhau dù gần hay xa (sát mắt không thành thanh to)
        float dist = Math.max(mid.length(), 0.6f);
        side.normalize().mul(Math.min(width * dist, 0.08f) * 0.5f);
        buf.vertex(m, p1.x - side.x, p1.y - side.y, p1.z - side.z).color(r, g, b, a).next();
        buf.vertex(m, p1.x + side.x, p1.y + side.y, p1.z + side.z).color(r, g, b, a).next();
        buf.vertex(m, p2.x + side.x, p2.y + side.y, p2.z + side.z).color(r, g, b, a).next();
        buf.vertex(m, p2.x - side.x, p2.y - side.y, p2.z - side.z).color(r, g, b, a).next();
    }

    /** Chữ mặt trước của bảng, đặt đúng chỗ như SignBlockEntityRenderer gốc. */
    private static void signText(TextRenderer font, MatrixStack matrices, WorldRenderContext context, Vec3d cam, SignEcho s, float a) {
        matrices.push();
        matrices.translate(s.pos.getX() - cam.x + 0.5, s.pos.getY() - cam.y + 0.5, s.pos.getZ() - cam.z + 0.5);
        boolean wall = s.state.getBlock() instanceof WallSignBlock;
        float degrees = wall ? s.state.get(WallSignBlock.FACING).asRotation()
                : s.state.contains(SignBlock.ROTATION) ? s.state.get(SignBlock.ROTATION) * 22.5f : 0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-degrees));
        if (wall) matrices.translate(0.0, -0.3125, -0.4375);
        matrices.translate(0.0, 0.3333333, 0.0466667 + 0.004);
        float scale = 0.015625f * 0.6666667f * 1.2f;   // to hơn bản gốc một chút cho dễ đọc trong bóng tối
        matrices.scale(scale, -scale, scale);
        int alpha = Math.max(8, (int) (a * 255)) << 24;
        int color = alpha | 0xFDFDFD;   // r = g = b = 253: dấu tiếng vọng
        for (int i = 0; i < 4; i++) {
            OrderedText line = s.lines[i].asOrderedText();
            float x = -font.getWidth(line) / 2f;
            font.draw(line, x, i * 10 - 20, color, false, matrices.peek().getPositionMatrix(), context.consumers(),
                    TextRenderer.TextLayerType.POLYGON_OFFSET, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        }
        matrices.pop();
    }
}
