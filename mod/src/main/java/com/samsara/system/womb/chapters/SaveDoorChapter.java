package com.samsara.system.womb.chapters;

import com.samsara.entity.EchoDummyEntity;
import com.samsara.registry.SamsaraEntities;
import com.samsara.system.cycle.NamGhostSystem;
import com.samsara.system.womb.WombRoute;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Chương 2 - Hành lang cửa Save File (giữa Bến Đò và Khổng Thần Steve 2009).
 *
 * Hai bên tuyến là những cầu thang đá nổi trồi lên khỏi mặt nước, dẫn tới một cánh cửa đứng giữa hư không.
 * Mỗi cánh cửa là một save file Nam từng tạo; biển tên ghi tên thế giới và lần chơi cuối.
 * Càng đi về phía Quả Trứng, thời gian càng lùi: cửa mới (gỗ sồi, 2021) -> cửa cổ (đá cuội, Alpha 2009),
 * vừa khớp với xác Steve 2009 ở chặng kế tiếp. Có cửa hé ánh vàng, có cửa đóng chặt, có bậc thang gãy.
 * Hàng xa hơn là "rừng cửa" không tên, cao hơn, chỉ để thấy bóng dáng trên nền trời.
 * Qua máy quay, vài cánh cửa có bóng Nam_1998 đứng chờ.
 */
public final class SaveDoorChapter {
    private SaveDoorChapter() {}

    public static final String GHOST_TAG = "samsara_door_ghost";

    /** Đoạn tuyến (tính theo block từ Bến Đò) dành cho chương này. */
    private static final double START = 18.0;
    private static final double END = 98.0;

    /** Save file theo thứ tự từ mới nhất (gần bến) tới cũ nhất (gần Steve 2009). */
    private static final String[][] SAVES = {
            {"New World (14)", "03/2021"},
            {"Survival - đừng ngủ", "02/2021"},
            {"New World (13)", "11/2020"},
            {"server với Tuấn", "07/2020"},
            {"Hardcore", "01/2020"},
            {"Nhà Nam", "05/2018"},
            {"Sáng tạo-KHÔNG XÓA", "08/2016"},
            {"Copy of New World", "12/2015"},
            {"Survival 2014", "06/2014"},
            {"hầm mỏ", "09/2013"},
            {"Thế giới của em", "04/2012"},
            {"New World", "10/2011"},
            {"Beta 1.7.3", "2011"},
            {"World2", "2010"},
            {"Alpha 1.2.6", "2010"},
            {"World1", "2009"},
    };

    public static void build(ServerWorld world, BlockPos origin) {
        Random random = Random.create(origin.asLong() ^ 0x5A7E_D00DL);
        removeGhosts(world, origin);

        // Hàng gần: các cửa có tên, so le hai bên tuyến
        for (int i = 0; i < SAVES.length; i++) {
            double t = SAVES.length == 1 ? 0.0 : i / (double) (SAVES.length - 1);
            double along = MathHelper.lerp(t, START, END);
            int side = i % 2 == 0 ? 1 : -1;
            double lateral = 11.0 + random.nextDouble() * 5.0;
            int height = 4 + random.nextInt(3);
            Door door = new Door(SAVES[i][0], SAVES[i][1], (float) t, height, side, lateral,
                    random.nextFloat() < 0.35f, i % 4 == 1);
            buildDoorAt(world, origin, along, door, random);
        }

        // Hàng xa: "rừng cửa" không tên, cao và thưa, chỉ để thấy bóng trên nền trời
        for (int i = 0; i < 12; i++) {
            double along = START + random.nextDouble() * (END - START + 20.0);
            int side = random.nextBoolean() ? 1 : -1;
            double lateral = 26.0 + random.nextDouble() * 22.0;
            int height = 8 + random.nextInt(10);
            float age = (float) MathHelper.clamp((along - START) / (END - START), 0.0, 1.0);
            Door door = new Door("New World (" + (2 + random.nextInt(40)) + ")", null, age, height, side, lateral,
                    random.nextFloat() < 0.2f, false);
            buildDoorAt(world, origin, along, door, random);
        }
    }

    private record Door(String name, String date, float age, int height, int side, double lateral,
                        boolean open, boolean ghost) {}

    /** Vật liệu theo tuổi: mới = gỗ sồi; giữa = vân sam/rêu; cổ = đá cuội như thời Alpha. */
    private record Palette(BlockState step, BlockState landing, BlockState frame, BlockState door) {
        static Palette forAge(float age) {
            if (age < 0.35f) {
                return new Palette(Blocks.OAK_STAIRS.getDefaultState(), Blocks.OAK_PLANKS.getDefaultState(),
                        Blocks.STRIPPED_OAK_LOG.getDefaultState(), Blocks.OAK_DOOR.getDefaultState());
            }
            if (age < 0.7f) {
                return new Palette(Blocks.MOSSY_STONE_BRICK_STAIRS.getDefaultState(), Blocks.MOSSY_STONE_BRICKS.getDefaultState(),
                        Blocks.SPRUCE_LOG.getDefaultState(), Blocks.SPRUCE_DOOR.getDefaultState());
            }
            return new Palette(Blocks.COBBLESTONE_STAIRS.getDefaultState(), Blocks.MOSSY_COBBLESTONE.getDefaultState(),
                    Blocks.OAK_PLANKS.getDefaultState(), Blocks.OAK_DOOR.getDefaultState());
        }
    }

    private static void buildDoorAt(ServerWorld world, BlockPos origin, double along, Door door, Random random) {
        Vec3d routePoint = pointAlong(origin, along);
        Vec3d tangent = WombRoute.tangentNear(origin, routePoint);
        Vec3d normal = new Vec3d(-tangent.z, 0, tangent.x).multiply(door.side());
        Direction out = Direction.getFacing(normal.x, 0, normal.z); // từ tuyến hướng ra cửa
        Direction toRoute = out.getOpposite();
        Direction across = out.rotateYClockwise();

        int water = WombRoute.SURFACE_Y;
        int top = water + door.height();           // mặt sàn chiếu nghỉ
        BlockPos landing = BlockPos.ofFloored(routePoint.add(normal.multiply(door.lateral()))).withY(top - 1);
        Palette p = Palette.forAge(door.age());

        // Chiếu nghỉ 3x3 lơ lửng
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                world.setBlockState(landing.offset(out, a).offset(across, b), p.landing(), 2);
            }
        }

        // Cầu thang nổi đi xuống mặt nước, mỗi bậc rộng 2; cửa càng cổ càng nhiều bậc gãy
        BlockState step = p.step().with(StairsBlock.FACING, out);
        // Bậc i thấp hơn chiếu nghỉ i block; bậc cuối chìm ngang mặt nước (chân người chơi ở y = water - 1)
        for (int i = 1; i <= door.height(); i++) {
            BlockPos s = landing.offset(toRoute, 1 + i).down(i);
            if (i > 1 && i < door.height() && random.nextFloat() < door.age() * 0.35f) continue; // bậc gãy
            BlockState st = step.with(StairsBlock.WATERLOGGED, s.getY() < water);
            world.setBlockState(s, st, 2);
            world.setBlockState(s.offset(across, 1), st, 2);
        }

        // Khung cửa ở mép sau chiếu nghỉ, mặt cửa quay về tuyến
        BlockPos doorBase = landing.offset(out, 1).up();
        for (int y = 0; y <= 2; y++) {
            world.setBlockState(doorBase.offset(across, -1).up(y), p.frame(), 2);
            world.setBlockState(doorBase.offset(across, 1).up(y), p.frame(), 2);
        }
        world.setBlockState(doorBase.up(2), p.frame(), 2);

        BlockState doorState = p.door()
                .with(DoorBlock.FACING, toRoute)
                .with(DoorBlock.HINGE, DoorHinge.LEFT)
                .with(DoorBlock.OPEN, door.open());
        world.setBlockState(doorBase, doorState.with(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2);
        world.setBlockState(doorBase.up(), doorState.with(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);

        // Phía sau cửa: hư không; cửa hé thì có ánh vàng hắt ra
        BlockPos behind = doorBase.offset(out, 1);
        BlockState glow = door.open() ? Blocks.OCHRE_FROGLIGHT.getDefaultState() : Blocks.AIR.getDefaultState();
        world.setBlockState(behind, glow, 2);
        world.setBlockState(behind.up(), glow, 2);

        // Biển tên save file trên đầu khung
        if (door.name() != null) {
            BlockPos signPos = doorBase.up(2).offset(toRoute, 1);
            world.setBlockState(signPos, Blocks.DARK_OAK_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, toRoute), 2);
            if (world.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
                SignText text = sign.getFrontText()
                        .withMessage(0, Text.literal(door.name()))
                        .withMessage(1, door.date() != null ? Text.translatable("samsara.spire_chapter.choi_lan_cuoi") : Text.empty())
                        .withMessage(2, Text.literal(door.date() != null ? door.date() : ""))
                        .withMessage(3, Text.literal("§8Nam_1998"))
                        .withGlowing(door.open());
                sign.setText(text, true);
            }
        }

        // Bóng Nam_1998 đứng chờ trước cửa (chỉ thấy qua máy quay)
        if (door.ghost()) {
            EchoDummyEntity ghost = SamsaraEntities.ECHO_DUMMY.create(world);
            if (ghost != null) {
                BlockPos stand = landing.up();
                float yaw = toRoute.asRotation();
                ghost.refreshPositionAndAngles(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, yaw, 0.0f);
                ghost.setHeadYaw(yaw);
                ghost.setBodyYaw(yaw);
                ghost.setPlayerUuid(NamGhostSystem.NAM_UUID);
                ghost.setPermanent(true);
                ghost.setCustomName(Text.literal(NamGhostSystem.GHOST_NAME));
                ghost.setCustomNameVisible(true);
                ghost.addCommandTag(GHOST_TAG);
                world.spawnEntity(ghost);
            }
        }
    }

    /** Điểm trên tuyến cách Bến Đò {@code distance} block (đo dọc tuyến). */
    public static Vec3d pointAlong(BlockPos origin, double distance) {
        List<Vec3d> pts = WombRoute.polyline(origin);
        double left = distance;
        for (int i = 0; i < pts.size() - 1; i++) {
            double len = pts.get(i).distanceTo(pts.get(i + 1));
            if (left <= len) return pts.get(i).lerp(pts.get(i + 1), left / len);
            left -= len;
        }
        return pts.get(pts.size() - 1);
    }

    private static void removeGhosts(ServerWorld world, BlockPos origin) {
        Vec3d a = pointAlong(origin, START);
        Vec3d b = pointAlong(origin, END + 20.0);
        Box area = new Box(a, b).expand(60.0, 40.0, 60.0);
        world.getEntitiesByClass(EchoDummyEntity.class, area, e -> e.getCommandTags().contains(GHOST_TAG))
                .forEach(EchoDummyEntity::discard);
    }
}
