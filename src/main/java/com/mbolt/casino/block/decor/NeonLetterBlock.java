package com.mbolt.casino.block.decor;

import com.mbolt.casino.network.NeonPickerOpen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * One neon character on a wall. Placing it, or clicking it with an empty hand, opens a small screen to pick the
 * character; a dye changes its color. Rows of these spell any sign.
 */
public final class NeonLetterBlock extends WallDecorBlock {
    public static final IntegerProperty GLYPH = IntegerProperty.create("glyph", 0, NeonGlyph.values().length - 1);
    /** How far from the letter a player may still change it, like opening a container. */
    private static final double REACH = 8;

    public NeonLetterBlock(Properties properties) {
        super(properties, flat(2));
        registerDefaultState(defaultBlockState().setValue(GLYPH, 0).setValue(Neon.COLOR, NeonColor.PINK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GLYPH, Neon.COLOR);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new NeonPickerOpen(pos));
        }
    }

    /** Dyes recolor; another block in hand is placed next to the letter instead of opening the picker. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DyeItem) {
            return Neon.dye(stack, state, level, pos, player);
        }
        if (stack.getItem() instanceof BlockItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new NeonPickerOpen(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Applies a character picked on the client, after checking the player may still reach and edit the letter. */
    public static void choose(Player player, BlockPos pos, int glyph) {
        Level level = player.level();
        if (glyph < 0 || glyph >= NeonGlyph.values().length || !level.isLoaded(pos)
                || !player.canInteractWithBlock(pos, REACH - player.blockInteractionRange())
                || !level.mayInteract(player, pos) || !player.mayBuild()) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof NeonLetterBlock && state.getValue(GLYPH) != glyph) {
            level.setBlock(pos, state.setValue(GLYPH, glyph), Block.UPDATE_ALL);
        }
    }
}
