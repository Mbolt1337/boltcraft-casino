package com.mbolt.casino.item;

import com.mbolt.casino.CasinoText;
import com.mbolt.casino.advancement.CasinoEvent;
import com.mbolt.casino.block.CasinoBlock;
import com.mbolt.casino.block.entity.LinkedBlockEntity;
import com.mbolt.casino.block.entity.OwnedBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.block.entity.VaultLink;
import com.mbolt.casino.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The owner's tool: click a vault to remember it (claiming it if nobody owns it), then click a cashier or a game
 * to link it to that vault.
 */
public final class ManagerKeyItem extends Item {
    public ManagerKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = CasinoBlock.basePos(level.getBlockState(context.getClickedPos()), context.getClickedPos());
        if (player == null || !(level.getBlockEntity(pos) instanceof OwnedBlockEntity target)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Component result = switch (target) {
            case VaultBlockEntity vault -> rememberVault(context.getItemInHand(), player, vault);
            case LinkedBlockEntity linked -> link(context.getItemInHand(), player, (ServerLevel) level, linked);
            default -> CasinoText.message("key_unsupported");
        };
        player.displayClientMessage(result, true);
        return InteractionResult.CONSUME;
    }

    private static Component rememberVault(ItemStack key, Player player, VaultBlockEntity vault) {
        boolean claimed = false;
        if (!vault.hasOwner()) {
            vault.setOwner(player);
            claimed = true;
            if (player instanceof ServerPlayer serverPlayer) {
                // A vault without an owner is one left in a casino found in the world.
                CasinoEvent.OWN_VAULT.award(serverPlayer);
                CasinoEvent.CLAIM_ABANDONED.award(serverPlayer);
            }
        } else if (!vault.isOwner(player)) {
            return CasinoText.message("owner_only", vault.ownerName());
        }
        key.set(ModDataComponents.LINKED_VAULT.get(), VaultLink.of(vault));
        return CasinoText.message(claimed ? "vault_claimed" : "vault_selected");
    }

    private static Component link(ItemStack key, Player player, ServerLevel level, LinkedBlockEntity target) {
        VaultLink link = key.get(ModDataComponents.LINKED_VAULT.get());
        if (link == null) {
            return CasinoText.message("key_no_vault");
        }
        var vault = link.resolve(level);
        if (vault.isEmpty()) {
            return CasinoText.message("vault_unavailable");
        }
        if (!vault.get().isOwner(player)) {
            return CasinoText.message("owner_only", vault.get().ownerName());
        }
        if (target.hasOwner() && !target.isOwner(player)) {
            return CasinoText.message("owner_only", target.ownerName());
        }
        if (!target.canRelink()) {
            return CasinoText.message("busy_relink");
        }
        if (!target.hasOwner()) {
            target.setOwner(player);
        }
        target.linkTo(vault.get());
        if (player instanceof ServerPlayer serverPlayer) {
            CasinoEvent.LINK.award(serverPlayer);
        }
        return CasinoText.message("linked");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        VaultLink link = stack.get(ModDataComponents.LINKED_VAULT.get());
        if (link == null) {
            tooltip.add(CasinoText.message("key_tooltip_empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        BlockPos pos = link.pos().pos();
        tooltip.add(CasinoText.message("key_tooltip_vault", pos.getX(), pos.getY(), pos.getZ())
                .withStyle(ChatFormatting.GOLD));
    }
}
