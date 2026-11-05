package com.mbolt.casino.entity;

import com.mbolt.casino.CasinoText;
import com.mbolt.casino.block.decor.Seat;
import com.mbolt.casino.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Invisible, transient riding anchor. Furniture remains an ordinary block; no player inventory is touched. */
public final class SeatEntity extends Entity {
    // PlayerRenderer scales the vanilla 16-pixel limbs by 15/16. The bent knee is six pixels from the hip.
    private static final double KNEE_FORWARD = 6.0 / 16 * 15 / 16;
    private static final double LEG_HALF_DEPTH = 2.0 / 16 * 15 / 16;
    private static final double LEGS_HALF_WIDTH = 4.0 / 16 * 15 / 16;
    private BlockPos furniture = BlockPos.ZERO;
    private BlockState furnitureState;
    private Vec3 entryPosition = Vec3.ZERO;

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    public static InteractionResult sit(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.isShiftKeyDown() || player.isPassenger() || player.isSpectator() || player.isSleeping()
                || !player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (!(state.getBlock() instanceof Seat surface) || player.distanceToSqr(pos.getCenter()) > 6 * 6) {
            return InteractionResult.PASS;
        }
        var existing = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos).inflate(.1),
                seat -> seat.furniture.equals(pos));
        for (SeatEntity seat : existing) {
            if (seat.isVehicle()) {
                player.displayClientMessage(CasinoText.message("seat_occupied"), true);
                return InteractionResult.CONSUME;
            }
            seat.discard();
        }
        Direction facing = surface.seatFacing(state, player);
        Vec3 cushion = Vec3.atBottomCenterOf(pos).add(facing.getStepX() * surface.seatForwardOffset(),
                surface.seatHeight(), facing.getStepZ() * surface.seatForwardOffset());
        if (!hasRoom(level, player, cushion, facing)) {
            player.displayClientMessage(CasinoText.message("seat_blocked"), true);
            return InteractionResult.CONSUME;
        }
        SeatEntity seat = ModEntities.SEAT.get().create(server);
        if (seat == null) {
            return InteractionResult.PASS;
        }
        seat.furniture = pos.immutable();
        seat.furnitureState = state;
        seat.entryPosition = player.position();
        seat.moveTo(cushion.x, cushion.y, cushion.z, facing.toYRot(), 0);
        server.addFreshEntity(seat);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.teleport(player.getX(), player.getY(), player.getZ(), facing.toYRot(),
                    player.getXRot());
        }
        if (!player.startRiding(seat, true)) {
            seat.discard();
            return InteractionResult.PASS;
        }
        player.setYRot(facing.toYRot());
        player.setYHeadRot(facing.toYRot());
        seat.positionRider(player);
        return InteractionResult.CONSUME;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide) {
            return;
        }
        BlockState current = level().getBlockState(furniture);
        boolean replaced = furnitureState != null && (current.getBlock() != furnitureState.getBlock()
                || current.hasProperty(HorizontalDirectionalBlock.FACING)
                && current.getValue(HorizontalDirectionalBlock.FACING)
                != furnitureState.getValue(HorizontalDirectionalBlock.FACING));
        boolean blocked = getFirstPassenger() instanceof Player player
                && !hasRoom(level(), player, position(), getDirection());
        if (!isVehicle() || replaced || blocked) {
            ejectPassengers();
            discard();
        }
    }

    private static boolean hasRoom(Level level, Player player, Vec3 cushion, Direction facing) {
        AABB body = new AABB(cushion.x - .3, cushion.y + .05, cushion.z - .3,
                cushion.x + .3, cushion.y + 1.35, cushion.z + .3);
        Vec3 knee = cushion.add(facing.getStepX() * KNEE_FORWARD, 0, facing.getStepZ() * KNEE_FORWARD);
        double halfX = facing.getAxis() == Direction.Axis.X ? LEG_HALF_DEPTH : LEGS_HALF_WIDTH;
        double halfZ = facing.getAxis() == Direction.Axis.Z ? LEG_HALF_DEPTH : LEGS_HALF_WIDTH;
        AABB shins = new AABB(knee.x - halfX, knee.y - .25, knee.z - halfZ,
                knee.x + halfX, knee.y + .17, knee.z + halfZ);
        return level.noCollision(player, body) && level.noCollision(player, shins);
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        return position();
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        passenger.setYBodyRot(getYRot());
        float relative = Mth.wrapDegrees(passenger.getYRot() - getYRot());
        float clamped = Mth.clamp(relative, -85, 85);
        passenger.yRotO += clamped - relative;
        passenger.setYRot(passenger.getYRot() + clamped - relative);
        passenger.setYHeadRot(passenger.getYRot());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Direction facing = getDirection();
        Direction[] exits = {facing, facing.getClockWise(), facing.getCounterClockWise(), facing.getOpposite()};
        for (int distance = 1; distance <= 2; distance++) {
            for (Direction direction : exits) {
                BlockPos candidate = furniture.relative(direction, distance);
                for (int height : new int[]{0, 1, -1}) {
                    Vec3 safe = DismountHelper.findSafeDismountLocation(passenger.getType(), level(),
                            candidate.offset(0, height, 0), true);
                    if (safe != null) {
                        return safe;
                    }
                }
            }
        }
        Vec3 above = Vec3.atBottomCenterOf(furniture.above());
        if (level().noCollision(passenger, passenger.getDimensions(passenger.getPose()).makeBoundingBox(above))) {
            return above;
        }
        return entryPosition;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && !isVehicle();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
