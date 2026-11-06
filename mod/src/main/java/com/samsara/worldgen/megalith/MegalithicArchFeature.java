package com.samsara.worldgen.megalith;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * The Megalithic Archway (Vòm Cổng Cự Thạch Khổng Lồ - Tham chiếu Ảnh 5).
 * A brutalist, monumental parabolic arch spanning 240 blocks across and 250 blocks high.
 * So vast that a Woodland Mansion looks tiny beneath its span.
 */
public class MegalithicArchFeature {

    public static boolean generate(ServerWorld world, int centerX, int centerZ) {
        int groundY = 62;
        int apexY = 316;
        int halfSpanX = 115;      // Bề rộng nhịp vòm = 230 blocks
        int halfDepthZ = 46;      // Chiều sâu vòm = 92 blocks
        int archThickness = 24;   // Độ dày phiến đá vòm

        BlockState smoothBasalt = Blocks.SMOOTH_BASALT.getDefaultState();
        BlockState deepslateTiles = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState polishedBlackstone = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState calcite = Blocks.CALCITE.getDefaultState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // 1. Quét theo trục X và Z để tạo hình vòm Parabol nguyên khối
        for (int x = -halfSpanX; x <= halfSpanX; x++) {
            float xRelOuter = (float) Math.abs(x) / (float) halfSpanX;
            // Phương trình parabol ngoài: y = apex - (apex - ground) * (x / halfSpan)^2
            int outerY = (int) (apexY - (apexY - groundY) * (xRelOuter * xRelOuter));

            // Phương trình parabol trong (lòng vòm thông suốt)
            int innerSpanX = halfSpanX - archThickness;
            int innerApexY = apexY - archThickness;
            int innerY = groundY - 1;
            if (Math.abs(x) < innerSpanX) {
                float xRelInner = (float) Math.abs(x) / (float) innerSpanX;
                innerY = (int) (innerApexY - (innerApexY - groundY) * (xRelInner * xRelInner));
            }

            for (int z = -halfDepthZ; z <= halfDepthZ; z++) {
                float zRel = (float) Math.abs(z) / (float) halfDepthZ;
                // Bo vát cạnh theo chiều Z ở các mép ngoài (Beveled Outer Edges)
                int zDrop = (int) (Math.pow(zRel, 3.0) * 14.0);

                int topLimit = outerY - zDrop;
                int bottomLimit = Math.max(groundY, innerY + 1);

                if (topLimit < bottomLimit) continue;

                for (int y = bottomLimit; y <= topLimit; y++) {
                    mutable.set(centerX + x, y, centerZ + z);

                    // Phân tầng vật liệu tạo khối kiến trúc hùng vĩ
                    BlockState mat;
                    boolean isOuterShell = (y >= topLimit - 2 || y <= bottomLimit + 2 || Math.abs(z) >= halfDepthZ - 2 || Math.abs(x) >= halfSpanX - 2);

                    if (y > 280) {
                        // Đỉnh vòm chạm tầng mây có viền Calcite sáng loáng
                        mat = (isOuterShell && (x + y + z) % 5 == 0) ? calcite : smoothBasalt;
                    } else if (isOuterShell) {
                        mat = ((x + y) % 11 == 0) ? polishedBlackstone : deepslateTiles;
                    } else {
                        mat = smoothBasalt;
                    }

                    // Điểm xuyết các mạch năng lượng huyền bí Crying Obsidian dọc theo sống vòm
                    if (Math.abs(z) == 0 && Math.abs(x) % 18 == 0 && y > groundY + 10) {
                        mat = cryingObsidian;
                    }

                    world.setBlockState(mutable, mat, 2);
                }
            }
        }

        // 2. Lõi treo bí ẩn ở đỉnh vòm (Suspended Monolith Core under the Apex)
        int coreY = apexY - archThickness - 18;
        for (int dy = -12; dy <= 12; dy++) {
            int rad = (int) (7.0f * (1.0f - (float) Math.abs(dy) / 14.0f));
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if (dx * dx + dz * dz <= rad * rad) {
                        mutable.set(centerX + dx, coreY + dy, centerZ + dz);
                        BlockState mat = ((dx + dy + dz) % 4 == 0) ? cryingObsidian : polishedBlackstone;
                        if (dx == 0 && dz == 0 && Math.abs(dy) < 4) {
                            mat = seaLantern;
                        }
                        world.setBlockState(mutable, mat, 2);
                    }
                }
            }
        }

        return true;
    }
}
