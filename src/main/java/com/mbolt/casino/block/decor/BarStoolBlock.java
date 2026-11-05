package com.mbolt.casino.block.decor;

import com.mbolt.casino.entity.SeatEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The existing bar stool, now usable as a seat without changing its approved model. */
public final class BarStoolBlock extends ShapedDecorBlock implements Seat {
    public static final MapCodec<BarStoolBlock> CODEC = simpleCodec(BarStoolBlock::new);

    public BarStoolBlock(Properties properties) {
        super(properties, Block.box(3, 0, 3, 13, 14, 13));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public double seatHeight() {
        return 14.0 / 16;
    }

    @Override
    public double seatForwardOffset() {
        return 2.0 / 16;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        return SeatEntity.sit(level, pos, state, player);
    }
}
