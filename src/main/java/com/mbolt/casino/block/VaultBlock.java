package com.mbolt.casino.block;

import com.mbolt.casino.CasinoText;
import com.mbolt.casino.block.entity.OwnedBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.game.SlotRounds;
import com.mbolt.casino.game.RouletteRounds;
import com.mbolt.casino.inventory.PlayerItems;
import com.mbolt.casino.inventory.ResourceItems;
import com.mbolt.casino.menu.VaultMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;

/** The casino bank. Only its owner opens it; a vault nobody owns is claimed with a manager key. */
public final class VaultBlock extends CasinoBlock {
    public static final MapCodec<VaultBlock> CODEC = simpleCodec(VaultBlock::new);

    public VaultBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VaultBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void openMenu(ServerPlayer player, BlockPos pos, OwnedBlockEntity blockEntity) {
        if (!blockEntity.hasOwner()) {
            player.displayClientMessage(CasinoText.message("vault_unclaimed"), true);
            return;
        }
        if (!blockEntity.isOwner(player)) {
            player.displayClientMessage(CasinoText.message("owner_only", blockEntity.ownerName()), true);
            return;
        }
        var provider = new SimpleMenuProvider((id, inventory, viewer) -> new VaultMenu(id, inventory, pos),
                getName());
        player.openMenu(provider, buf -> buf.writeBlockPos(pos));
    }

    /** A removed vault settles its rounds and drops everything it stored, exactly once. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof VaultBlockEntity vault) {
            SlotRounds.get(server.getServer()).vaultRemoved(server.getServer(), vault);
            RouletteRounds.get(server.getServer()).vaultRemoved(server.getServer(), vault);
            var stored = new LinkedHashMap<>(vault.ledger().resources());
            vault.ledger().clearResources();
            vault.ledgerChanged();
            stored.forEach((id, count) -> ResourceItems.item(id)
                    .ifPresent(item -> PlayerItems.dropAt(level, pos, item, count)));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
