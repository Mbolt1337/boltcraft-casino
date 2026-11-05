package com.mbolt.casino.block;

import com.mbolt.casino.advancement.CasinoEvent;
import com.mbolt.casino.block.entity.OwnedBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.item.ManagerKeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Common behavior of the vault, the cashier and the games: they face the player who places them, belong to that
 * player, let the manager key act on them and open their menu on a plain right click.
 */
public abstract class CasinoBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** The block entity position of any casino block part; other blocks map to themselves. */
    public static BlockPos basePos(BlockState state, BlockPos pos) {
        return state.getBlock() instanceof CasinoBlock block ? block.blockEntityPos(state, pos) : pos;
    }

    protected CasinoBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** Opens the block's menu; called on the server for the block entity position. */
    protected abstract void openMenu(ServerPlayer player, BlockPos pos, OwnedBlockEntity blockEntity);

    /** The position holding the block entity; multi-block games point their other parts at it. */
    protected BlockPos blockEntityPos(BlockState state, BlockPos pos) {
        return pos;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof OwnedBlockEntity owned) {
            owned.setOwner(player);
            if (owned instanceof VaultBlockEntity && player instanceof ServerPlayer serverPlayer) {
                CasinoEvent.OWN_VAULT.award(serverPlayer);
            }
        }
    }

    /** The manager key skips the menu so that its own click handling runs. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ManagerKeyItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos entityPos = blockEntityPos(state, pos);
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(entityPos) instanceof OwnedBlockEntity owned) {
            openMenu(serverPlayer, entityPos, owned);
        }
        return InteractionResult.CONSUME;
    }

    /** Fixtures of a generated building drop nothing; see {@link OwnedBlockEntity#isFixture()}. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof OwnedBlockEntity owned
                && owned.isFixture()) {
            return List.of();
        }
        return super.getDrops(state, params);
    }

    /** Mobs such as the wither may not break protected casino blocks. */
    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return Protection.canBeDestroyedByWorld(level, blockEntityPos(state, pos));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }
}
