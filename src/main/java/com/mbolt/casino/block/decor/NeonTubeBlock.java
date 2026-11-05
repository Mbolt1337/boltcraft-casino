package com.mbolt.casino.block.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/** Neon tube for outlines: bends round corners and branches into the tubes next to it on the same wall. */
public final class NeonTubeBlock extends WallConnectingBlock {
    public NeonTubeBlock(Properties properties) {
        super(properties, flat(2.5));
        registerDefaultState(defaultBlockState().setValue(Neon.COLOR, NeonColor.PINK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(Neon.COLOR);
    }

    /** A tube ends at its middle where nothing continues it; a tube on its own lies level. */
    @Override
    protected BlockState arms(BlockState state, boolean up, boolean down, boolean left, boolean right) {
        boolean alone = !up && !down && !left && !right;
        return withArms(state, up, down, left || alone, right || alone);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return Neon.dye(stack, state, level, pos, player);
    }
}
