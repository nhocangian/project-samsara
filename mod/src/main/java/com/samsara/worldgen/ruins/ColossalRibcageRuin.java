package com.samsara.worldgen.ruins;

import com.samsara.registry.SamsaraItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * Phế Tích Đại Khung Xương Cự Thần Vũ Trụ (Mountain-Scale Leviathan Ruin).
 * Quy mô cự đại đích thực:
 * - Dài 200 blocks (Z từ -100 đến +100).
 * - Rộng 140 blocks (X từ -70 đến +70).
 * - Chiều cao xương sườn vút lên tận Y = 250 - 280 (xuyên thủng tầng mây Y=192!).
 * - Thân xương dày 6 - 8 blocks, đốt sống khổng lồ rộng 20 blocks.
 * - Các dẻ sườn bị gãy đổ rải rác tạo thành cầu xương và tháp đá hóa thạch.
 */
public class ColossalRibcageRuin {

    public static boolean generate(ServerWorld world, int centerX, int groundY, int centerZ) {
        Random rand = new Random(centerX * 7919L + centerZ * 6607L);
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        BlockState bone = Blocks.BONE_BLOCK.getDefaultState();
        BlockState calcite = Blocks.CALCITE.getDefaultState();
        BlockState smoothBasalt = Blocks.SMOOTH_BASALT.getDefaultState();
        BlockState moss = Blocks.MOSS_BLOCK.getDefaultState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState deepslate = Blocks.DEEPSLATE_TILES.getDefaultState();

        int halfLength = 95; // Tổng chiều dài = 190 blocks

        // 1. CỘT SỐNG KHỔNG LỒ RỘNG 20 BLOCKS (Titan Spinal Column)
        for (int z = -halfLength; z <= halfLength; z++) {
            boolean isVertebraJoint = (Math.abs(z) % 18 == 0);
            int vRadius = isVertebraJoint ? 9 : 7;

            for (int dx = -vRadius; dx <= vRadius; dx++) {
                for (int dy = -4; dy <= vRadius * 2; dy++) {
                    int yPos = groundY + dy;
                    if (yPos > 560 || yPos < -60) continue;

                    // Ống tủy khổng lồ rỗng ở giữa (đường kính 8 blocks, đi bộ hoặc chèo thuyền qua được)
                    int distCanalSq = dx * dx + (dy - 4) * (dy - 4);
                    int distOuterSq = dx * dx + (dy - 4) * (dy - 4);

                    if (distOuterSq <= vRadius * vRadius && distCanalSq > 12) {
                        if (rand.nextFloat() < 0.05f) continue; // Vết nứt thời gian
                        mutable.set(centerX + dx, yPos, centerZ + z);

                        BlockState mat = bone;
                        if (dy < 0) mat = deepslate;
                        else if (rand.nextFloat() < 0.25f) mat = calcite;
                        if (yPos < groundY + 3 && rand.nextFloat() < 0.35f) mat = moss;

                        world.setBlockState(mutable, mat, 2);
                    }
                }
            }

            // Mỏm gai sống lưng vươn cao 25 blocks (Dorsal Spine Towers)
            if (isVertebraJoint) {
                int spineHeight = 18 + (int) (Math.sin((float) (z + halfLength) / (halfLength * 2) * Math.PI) * 12.0f);
                for (int dy = vRadius * 2; dy <= vRadius * 2 + spineHeight; dy++) {
                    int yPos = groundY + dy;
                    if (yPos > 318) continue;
                    for (int sx = -2; sx <= 2; sx++) {
                        for (int sz = -2; sz <= 2; sz++) {
                            if (sx * sx + sz * sz <= 4) {
                                mutable.set(centerX + sx, yPos, centerZ + z + sz);
                                world.setBlockState(mutable, bone, 2);
                            }
                        }
                    }
                }
            }
        }

        // 2. TÁM CẶP XƯƠNG SƯỜN KHỔNG LỒ VÒM LÊN TẬN TẦNG MÂY (Titan Rib Arches)
        // Mỗi dẻ sườn vòm cao từ Y=62 lên đến Y=230 - 270! (Bề rộng nhịp 130 blocks!)
        int ribIndex = 0;
        for (int z = -halfLength + 16; z <= halfLength - 16; z += 22) {
            ribIndex++;
            float zProg = (float) (z + halfLength) / (halfLength * 2);

            // Bán kính sườn mở rộng cực đại vươn cao vượt qua Y=320 cũ lên tận Y ~ 380!
            int ribMaxH = (int) (180.0f + Math.sin(zProg * Math.PI) * 140.0f);
            int ribSpanX = (int) (65.0f + Math.sin(zProg * Math.PI) * 35.0f);

            for (int side : new int[]{-1, 1}) {
                boolean isBroken = (ribIndex == 3 && side == 1) || (ribIndex == 6 && side == -1) || (ribIndex == 7 && side == 1);
                float breakFraction = isBroken ? (0.45f + rand.nextFloat() * 0.2f) : 1.0f;
                int maxStep = (int) (160 * breakFraction);

                int ribThickness = 5; // Thân dẻ sườn dày 10 blocks nguyên khối!

                for (int s = 0; s <= maxStep; s++) {
                    float p = (float) s / 160.0f; // 0.0 (chân cột sống) -> 1.0 (chóp vòm ngực)

                    // Quỹ đạo uốn cong hình elip vươn cao
                    int rx = (int) (side * (Math.sin(p * Math.PI * 0.88f) * ribSpanX));
                    int ry = (int) (groundY + 8 + (p * ribMaxH));
                    if (ry > 560) break;

                    for (int dx = -ribThickness; dx <= ribThickness; dx++) {
                        for (int dy = -ribThickness; dy <= ribThickness; dy++) {
                            for (int dz = -ribThickness; dz <= ribThickness; dz++) {
                                if (dx * dx + dy * dy + dz * dz <= ribThickness * ribThickness) {
                                    if (rand.nextFloat() < 0.04f) continue; // Phong hóa thủng lỗ tự nhiên
                                    mutable.set(centerX + rx + dx, ry + dy, centerZ + z + dz);

                                    BlockState mat = (rand.nextFloat() < 0.3f) ? calcite : bone;
                                    if (ry < groundY + 6 && rand.nextFloat() < 0.4f) mat = moss;
                                    if (s == maxStep && isBroken) mat = cryingObsidian; // Vết gãy đứt rỉ năng lượng

                                    world.setBlockState(mutable, mat, 2);
                                }
                            }
                        }
                    }
                }

                // Nếu sườn bị gãy, tạo khối xương khổng lồ rơi cắm xiên xuống đất
                if (isBroken) {
                    int fallenX = centerX + side * (ribSpanX - 6);
                    int fallenZ = centerZ + z + 8;
                    for (int f = 0; f < 35; f++) {
                        int fy = groundY + f;
                        if (fy > 318) break;
                        for (int fdx = -3; fdx <= 3; fdx++) {
                            for (int fdz = -3; fdz <= 3; fdz++) {
                                if (fdx * fdx + fdz * fdz <= 9) {
                                    mutable.set(fallenX + fdx + (f / 3), fy, fallenZ + fdz + (f / 4));
                                    world.setBlockState(mutable, bone, 2);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. TẾ ĐÀN CỰ THẠCH LÒNG NGỰC (Titanic Altar with Skyward Beacon)
        BlockPos altarPos = new BlockPos(centerX, groundY + 2, centerZ);
        for (int ax = -4; ax <= 4; ax++) {
            for (int az = -4; az <= 4; az++) {
                mutable.set(centerX + ax, groundY + 1, centerZ + az);
                world.setBlockState(mutable, cryingObsidian, 2);
            }
        }
        world.setBlockState(altarPos, Blocks.LODESTONE.getDefaultState(), 2);
        world.setBlockState(altarPos.up(), Blocks.BEACON.getDefaultState(), 2);

        // Rương kho báu di vật cổ
        BlockPos chestPos = altarPos.north(2);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
        if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setStack(13, new ItemStack(SamsaraItems.FRACTURED_SLATE));
            chest.setStack(4, new ItemStack(Items.ECHO_SHARD, 5));
            chest.setStack(22, new ItemStack(Items.BONE_BLOCK, 16));
            chest.markDirty();
        }

        return true;
    }
}
