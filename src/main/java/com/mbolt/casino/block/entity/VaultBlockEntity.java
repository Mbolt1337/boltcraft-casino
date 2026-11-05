package com.mbolt.casino.block.entity;

import com.mbolt.casino.economy.VaultLedger;
import com.mbolt.casino.registry.ModBlockEntities;
import com.mbolt.casino.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/** The bank of one casino. The door opens while its owner has the vault menu open. */
public final class VaultBlockEntity extends OwnedBlockEntity implements GeoBlockEntity {
    private static final String DOOR_CONTROLLER = "door";
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("animation.vault.open");
    private static final RawAnimation CLOSE = RawAnimation.begin().thenPlay("animation.vault.close");
    private static final RawAnimation OPEN_IDLE = RawAnimation.begin().thenLoop("animation.vault.open_idle");
    private static final RawAnimation CLOSED_IDLE = RawAnimation.begin().thenLoop("animation.vault.closed");
    private static final float DOOR_VOLUME = 0.7f;
    private static final float DOOR_PITCH = 0.8f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final VaultLedger ledger = new VaultLedger();
    private UUID identity = UUID.randomUUID();
    /** Changes even when a full journal replaces its oldest entry and keeps the same size. */
    private long ledgerRevision;
    /** Server only: players that have the vault menu open. */
    private int viewers;
    private boolean open;

    public VaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VAULT.get(), pos, state);
    }

    public VaultLedger ledger() {
        return ledger;
    }

    public UUID identity() {
        return identity;
    }

    public long ledgerRevision() {
        return ledgerRevision;
    }

    /** Call after every ledger change so the vault is saved with its chunk. */
    public void ledgerChanged() {
        ledgerRevision++;
        setChanged();
    }

    public void startViewing() {
        viewers++;
        if (viewers == 1) {
            setOpen(true);
        }
    }

    public void stopViewing() {
        viewers = Math.max(0, viewers - 1);
        if (viewers == 0) {
            setOpen(false);
        }
    }

    private void setOpen(boolean value) {
        if (level == null || open == value) {
            return;
        }
        open = value;
        triggerAnim(DOOR_CONTROLLER, value ? "open" : "close");
        var sound = value ? ModSounds.VAULT_OPEN.get() : ModSounds.VAULT_CLOSE.get();
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, DOOR_VOLUME, DOOR_PITCH);
        syncToClients();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, DOOR_CONTROLLER, 0,
                state -> state.setAndContinue(open ? OPEN_IDLE : CLOSED_IDLE))
                .triggerableAnim("open", OPEN)
                .triggerableAnim("close", CLOSE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void writeClientState(CompoundTag tag) {
        tag.putBoolean("Open", open);
    }

    @Override
    protected void readClientState(CompoundTag tag) {
        open = tag.getBoolean("Open");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("Identity", identity);
        tag.putLong("LedgerRevision", ledgerRevision);
        VaultLedgerNbt.save(ledger, tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("Identity")) {
            identity = tag.getUUID("Identity");
        }
        ledgerRevision = tag.getLong("LedgerRevision");
        VaultLedgerNbt.load(ledger, tag);
    }
}
