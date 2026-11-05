package com.mbolt.casino.block.decor;

import com.mbolt.casino.block.HorizontalShapes;
import com.mbolt.casino.entity.SeatEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class DiningChairBlock extends FacingDecorBlock implements Seat {
    public static final MapCodec<DiningChairBlock> CODEC = simpleCodec(DiningChairBlock::new);

    public DiningChairBlock(Properties properties) {
        super(properties, new HorizontalShapes(new double[]{2, 0, 2, 14, 9, 14},
                new double[]{2, 7, 13, 14, 21, 15}));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public double seatHeight() {
        return 9.0 / 16;
    }

    @Override
    public double seatForwardOffset() {
        return 2.5 / 16;
    }

    @Override
    public Direction seatFacing(BlockState state, Player player) {
        return state.getValue(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        return SeatEntity.sit(level, pos, state, player);
    }
}
