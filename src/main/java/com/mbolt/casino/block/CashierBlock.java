package com.mbolt.casino.block;

import com.mbolt.casino.block.entity.CashierBlockEntity;
import com.mbolt.casino.block.entity.OwnedBlockEntity;
import com.mbolt.casino.menu.CashierMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Exchanges resources for chips and back; any player may use it. */
public final class CashierBlock extends CasinoBlock {
    public static final MapCodec<CashierBlock> CODEC = simpleCodec(CashierBlock::new);
    /** The counter and the booth wall with the teller window, as in models/cashier.bbmodel. */
    private static final HorizontalShapes SHAPES = new HorizontalShapes(
            new double[]{0, 0, 0, 16, 9, 16},
            new double[]{1, 9, 3, 15, 16, 5});

    public CashierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CashierBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void openMenu(ServerPlayer player, BlockPos pos, OwnedBlockEntity blockEntity) {
        var provider = new SimpleMenuProvider((id, inventory, viewer) -> new CashierMenu(id, inventory, pos),
                getName());
        player.openMenu(provider, buf -> buf.writeBlockPos(pos));
    }
}
