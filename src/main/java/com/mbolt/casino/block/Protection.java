package com.mbolt.casino.block;

import com.mbolt.casino.CasinoText;
import com.mbolt.casino.block.entity.OwnedBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.config.CasinoServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Who may break casino blocks. Two independent rules:
 * <ul>
 *     <li>with {@code protect_blocks}, only the owner and operators break owned blocks;</li>
 *     <li>a vault whose chips are still in circulation is never broken except by an operator, otherwise the owner
 *     would take the resources while the chips stay spendable at other casinos.</li>
 * </ul>
 */
public final class Protection {
    private static final int OPERATOR_LEVEL = 2;

    private Protection() {
    }

    /** Why the player may not break the block, or null when they may. */
    @Nullable
    public static Component breakRefusal(Player player, OwnedBlockEntity block) {
        if (player.hasPermissions(OPERATOR_LEVEL)) {
            return null;
        }
        if (block instanceof VaultBlockEntity vault && vault.ledger().hasLiabilities()) {
            return CasinoText.message("vault_has_liabilities", CasinoText.chips(vault.ledger().outstanding()));
        }
        if (CasinoServerConfig.PROTECT_BLOCKS.get() && block.hasOwner() && !block.isOwner(player)) {
            return CasinoText.message("owner_only", block.ownerName());
        }
        return null;
    }

    /** Explosions, mobs and other world effects. */
    public static boolean canBeDestroyedByWorld(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof OwnedBlockEntity block)) {
            return true;
        }
        if (block instanceof VaultBlockEntity vault && vault.ledger().hasLiabilities()) {
            return false;
        }
        return !CasinoServerConfig.PROTECT_BLOCKS.get();
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        BlockPos pos = CasinoBlock.basePos(event.getState(), event.getPos());
        if (!(event.getLevel().getBlockEntity(pos) instanceof OwnedBlockEntity block)) {
            return;
        }
        Component refusal = breakRefusal(event.getPlayer(), block);
        if (refusal != null) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(refusal, true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> {
            BlockPos base = CasinoBlock.basePos(event.getLevel().getBlockState(pos), pos);
            return !canBeDestroyedByWorld(event.getLevel(), base);
        });
    }
}
