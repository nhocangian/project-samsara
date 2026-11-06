package com.samsara.worldgen;

import com.samsara.registry.SamsaraItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;

/**
 * Tạo kiến trúc "Bia Mộ Vô Danh" (The Nameless Cairn) ngay gần điểm Spawn của thế giới.
 * Đảm bảo người chơi vừa vào thế giới là thấy ngay điểm khởi đầu câu chuyện.
 */
public class SpawnCairnGenerator extends PersistentState {
    public static final String KEY = "samsara_spawn_cairn";
    private boolean generated = false;
    /** Vị trí cột bia; null với thế giới tạo trước khi trường này tồn tại. */
    private BlockPos cairnPos = null;

    public static SpawnCairnGenerator get(ServerWorld world) {
        return world.getServer().getOverworld().getPersistentStateManager().getOrCreate(
                SpawnCairnGenerator::fromNbt,
                SpawnCairnGenerator::new,
                KEY
        );
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerWorld overworld = server.getOverworld();
            SpawnCairnGenerator state = get(overworld);

            if (!state.generated) {
                BlockPos spawnPos = overworld.getSpawnPos();
                BlockPos surfacePos = overworld.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawnPos.add(3, 0, 3));

                // Chu Kỳ #2 trở đi: rương chỉ còn cuốn nhật ký, đóa hoa khô không còn nữa
                boolean firstCycle = com.samsara.system.cycle.EggCycleState.get(server).getCycle() <= 1;
                buildCairn(overworld, surfacePos, firstCycle);
                state.generated = true;
                state.cairnPos = surfacePos;
                state.markDirty();
            }
        });
    }

    public static void buildCairn(ServerWorld world, BlockPos origin, boolean withDriedFlower) {
        // Nền đá 3x3 rêu phong
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos floor = origin.add(x, -1, z);
                world.setBlockState(floor, (x + z) % 2 == 0 ? Blocks.MOSSY_COBBLESTONE.getDefaultState() : Blocks.CRACKED_STONE_BRICKS.getDefaultState(), 2);
            }
        }

        // Cột bia mộ đá
        world.setBlockState(origin, Blocks.CHISELED_STONE_BRICKS.getDefaultState(), 2);
        world.setBlockState(origin.up(), Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState(), 2);

        // Đặt biển chỉ dẫn
        BlockPos signPos = origin.north();
        world.setBlockState(signPos, Blocks.OAK_SIGN.getDefaultState(), 2);
        if (world.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            SignText front = sign.getFrontText()
                    .withMessage(0, Text.translatable("samsara.spawn_cairn_generator.ke_den_sau"))
                    .withMessage(1, Text.translatable("samsara.spawn_cairn_generator.cau_den_muon_roi"))
                    .withMessage(2, Text.translatable("samsara.spawn_cairn_generator.cho_nay"))
                    .withMessage(3, Text.translatable("samsara.spawn_cairn_generator.vua_co_nguoi_nam"));
            sign.setText(front, true);
        }

        // Đặt rương chứa di vật đầu tiên
        BlockPos chestPos = origin.south();
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
        if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            // Tàn thư Palimpsest
            chest.setStack(13, new ItemStack(SamsaraItems.PALIMPSEST_JOURNAL));
            // Máy quay KHÔNG nằm ở đây: <System> tự cài nó vào Hotbar (CamcorderGiftSystem)
            // Đóa hoa tàn (chỉ ở chu kỳ đầu)
            if (!withDriedFlower) return;
            ItemStack rose = new ItemStack(Items.WITHER_ROSE);
            rose.setCustomName(Text.translatable("samsara.spawn_cairn_generator.doa_hoa_kho_tu"));
            chest.setStack(14, rose);
        }
    }

    /** Vị trí Bia Mộ Vô Danh, nơi bầy cừu quay mặt về (Stage 1). */
    public static BlockPos getCairnPos(ServerWorld world) {
        SpawnCairnGenerator state = get(world);
        if (state.cairnPos != null) return state.cairnPos;
        ServerWorld overworld = world.getServer().getOverworld();
        return overworld.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, overworld.getSpawnPos().add(3, 0, 3));
    }

    public static SpawnCairnGenerator fromNbt(NbtCompound nbt) {
        SpawnCairnGenerator gen = new SpawnCairnGenerator();
        gen.generated = nbt.getBoolean("Generated");
        if (nbt.contains("CairnPos")) gen.cairnPos = BlockPos.fromLong(nbt.getLong("CairnPos"));
        return gen;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("Generated", this.generated);
        if (this.cairnPos != null) nbt.putLong("CairnPos", this.cairnPos.asLong());
        return nbt;
    }
}
