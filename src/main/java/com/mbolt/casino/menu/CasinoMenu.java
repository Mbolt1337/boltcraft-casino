package com.mbolt.casino.menu;

import com.mbolt.casino.block.entity.OwnedBlockEntity;
import com.mbolt.casino.network.MenuNotice;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * A slotless casino screen backed by a block entity. The server builds the screen state every tick and sends it
 * only when it differs from what the client already has; actions arrive as payloads and are re-validated here.
 *
 * @param <B> block entity behind the menu
 * @param <S> state payload shown by the screen
 */
public abstract class CasinoMenu<B extends OwnedBlockEntity, S extends CustomPacketPayload>
        extends AbstractContainerMenu {
    private static final double MAX_DISTANCE_SQUARED = 6 * 6;
    private static final int ACTION_COOLDOWN_TICKS = 4;

    protected final Player player;
    protected final BlockPos pos;
    @Nullable
    protected final B blockEntity;
    private final Class<S> stateType;
    /** Server: the state last sent. Client: the state last received. */
    @Nullable
    private S state;
    private long nextActionTick;
    @Nullable
    private MenuNotice notice;
    private long noticeTick;

    protected CasinoMenu(MenuType<?> type, int containerId, Inventory inventory, BlockPos pos,
                         Class<B> blockEntityType, Class<S> stateType) {
        super(type, containerId);
        this.player = inventory.player;
        this.pos = pos.immutable();
        this.stateType = stateType;
        var found = player.level().getBlockEntity(pos);
        this.blockEntity = blockEntityType.isInstance(found) ? blockEntityType.cast(found) : null;
    }

    /** Server: what the screen should show right now. */
    protected abstract S buildState();

    /** Additional server-side condition for keeping the menu open, such as vault ownership. */
    protected boolean mayView(Player viewer) {
        return true;
    }

    public BlockPos pos() {
        return pos;
    }

    @Nullable
    public B blockEntity() {
        return blockEntity;
    }

    @Nullable
    public S state() {
        return state;
    }

    /** Client: accepts a state payload if it is the type this menu shows. */
    public boolean offerState(Object payload) {
        if (!stateType.isInstance(payload)) {
            return false;
        }
        state = stateType.cast(payload);
        return true;
    }

    @Nullable
    public MenuNotice notice() {
        return notice;
    }

    /** Client game time when the current notice arrived. */
    public long noticeTick() {
        return noticeTick;
    }

    public void acceptNotice(MenuNotice value) {
        notice = value.text().getString().isEmpty() ? null : value;
        noticeTick = player.level().getGameTime();
    }

    @Override
    public boolean stillValid(Player viewer) {
        if (viewer.level().isClientSide) {
            return true;
        }
        return viewer == player
                && blockEntity != null
                && !blockEntity.isRemoved()
                && viewer.level().getBlockEntity(pos) == blockEntity
                && viewer.distanceToSqr(pos.getCenter()) <= MAX_DISTANCE_SQUARED
                && mayView(viewer);
    }

    @Override
    public ItemStack quickMoveStack(Player viewer, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (player instanceof ServerPlayer serverPlayer && stillValid(player)) {
            S next = buildState();
            if (!next.equals(state)) {
                state = next;
                PacketDistributor.sendToPlayer(serverPlayer, next);
            }
        }
    }

    /** Server: whether an action request may run now; ignores bursts of repeated clicks or packets. */
    protected boolean tryStartAction() {
        if (!(player instanceof ServerPlayer) || !stillValid(player)) {
            return false;
        }
        long now = player.level().getGameTime();
        if (now < nextActionTick) {
            return false;
        }
        nextActionTick = now + ACTION_COOLDOWN_TICKS;
        return true;
    }

    protected void sendNotice(Component text, boolean success) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new MenuNotice(containerId, text, success));
        }
    }

    /** Sends the current state immediately instead of waiting for the next tick. */
    protected void sendStateNow() {
        broadcastChanges();
    }
}
