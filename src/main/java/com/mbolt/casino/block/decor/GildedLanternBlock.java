package com.mbolt.casino.block.decor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A large square brass lantern for posts, pylons and ceilings. Placed like a vanilla lantern: standing on a
 * block, or hanging when placed under one.
 */
public final class GildedLanternBlock extends LanternBlock {
    private static final VoxelShape STANDING_SHAPE = Block.box(4, 0, 4, 12, 14, 12);
    private static final VoxelShape HANGING_SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public GildedLanternBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<LanternBlock> codec() {
        return MapCodec.<LanternBlock>unit(this);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? HANGING_SHAPE : STANDING_SHAPE;
    }
}
