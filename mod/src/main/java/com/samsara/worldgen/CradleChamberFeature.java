package com.samsara.worldgen;

import com.mojang.serialization.Codec;
import com.samsara.registry.SamsaraBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Generates the deterministic 7x7x7 Cradle Chamber at Y = -58.
 * Constructed from Bedrock and Crying Obsidian, housing the Cradle Altar.
 */
public class CradleChamberFeature extends Feature<DefaultFeatureConfig> {

    public CradleChamberFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();

        // Pin the chamber strictly at Bedrock elevation Y = -58
        int centerX = origin.getX();
        int centerZ = origin.getZ();
        int floorY = -59;

        // 7x7x7 Structure Bounds: X from -3 to +3, Y from 0 to 6, Z from -3 to +3
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y <= 6; y++) {
                    BlockPos currentPos = new BlockPos(centerX + x, floorY + y, centerZ + z);
                    boolean isEdge = (x == -3 || x == 3 || z == -3 || z == 3 || y == 0 || y == 6);

                    if (isEdge) {
                        // Alternate between Bedrock and Crying Obsidian
                        if ((x + y + z) % 2 == 0) {
                            world.setBlockState(currentPos, Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);
                        } else {
                            world.setBlockState(currentPos, Blocks.BEDROCK.getDefaultState(), 2);
                        }
                    } else {
                        // Interior hollow space
                        world.setBlockState(currentPos, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }

        // Place central altar: The Cradle at Y = -58
        BlockPos cradlePos = new BlockPos(centerX, floorY + 1, centerZ);
        world.setBlockState(cradlePos, SamsaraBlocks.CRADLE.getDefaultState(), 2);

        // Surrounding 4 directional pedestals for ambient ritual lighting
        world.setBlockState(cradlePos.north(2), Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);
        world.setBlockState(cradlePos.south(2), Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);
        world.setBlockState(cradlePos.east(2), Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);
        world.setBlockState(cradlePos.west(2), Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);

        world.setBlockState(cradlePos.north(2).up(), Blocks.SOUL_FIRE.getDefaultState(), 2);
        world.setBlockState(cradlePos.south(2).up(), Blocks.SOUL_FIRE.getDefaultState(), 2);
        world.setBlockState(cradlePos.east(2).up(), Blocks.SOUL_FIRE.getDefaultState(), 2);
        world.setBlockState(cradlePos.west(2).up(), Blocks.SOUL_FIRE.getDefaultState(), 2);

        return true;
    }
}
