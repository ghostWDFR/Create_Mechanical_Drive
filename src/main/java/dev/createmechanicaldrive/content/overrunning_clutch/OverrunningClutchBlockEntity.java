package dev.createmechanicaldrive.content.overrunning_clutch;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class OverrunningClutchBlockEntity
        extends SplitShaftBlockEntity {

    private static final float EPSILON =
            0.001F;

    private ScrollOptionBehaviour<OverrunningClutchDirection>
            directionBehaviour;

    public OverrunningClutchBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(
                type,
                pos,
                state
        );
    }

    @Override
    public void addBehaviours(
            List<BlockEntityBehaviour> behaviours
    ) {
        super.addBehaviours(behaviours);

        directionBehaviour =
                new ScrollOptionBehaviour<>(
                        OverrunningClutchDirection.class,
                        Component.translatable(
                                "mechanical_drive.overrunning_clutch.direction"
                        ),
                        this,
                        new DirectionValueBoxTransform()
                );

        directionBehaviour.value =
                0;

        directionBehaviour.withCallback(
                this::onDirectionChanged
        );

        behaviours.add(
                directionBehaviour
        );
    }

    private void onDirectionChanged(
            int value
    ) {
        if (level == null
                || level.isClientSide) {
            return;
        }

        rebuildKinetics();

        setChanged();
        sendData();
    }

    public void rebuildKinetics() {
        if (
                level == null
                        || level.isClientSide
        ) {
            return;
        }

        RotationPropagator.handleRemoved(
                level,
                worldPosition,
                this
        );

        RotationPropagator.handleAdded(
                level,
                worldPosition,
                this
        );
    }

    public OverrunningClutchDirection getSelectedDirection() {
        if (directionBehaviour == null) {
            return OverrunningClutchDirection.CLOCKWISE;
        }

        return directionBehaviour.get();
    }

    public boolean isLockedByRedstone() {
        BlockState state =
                getBlockState();

        return state.hasProperty(
                OverrunningClutchBlock.POWERED
        )
                && state.getValue(
                OverrunningClutchBlock.POWERED
        );
    }

    public void setPonderDirection(
            OverrunningClutchDirection direction
    ) {
        if (directionBehaviour == null) {
            return;
        }

        directionBehaviour.setValue(
                direction
                        == OverrunningClutchDirection.CLOCKWISE
                        ? 0
                        : 1
        );

        setChanged();
    }

    public boolean isSpeedAllowed(
            float speed
    ) {
        if (isLockedByRedstone()) {
            return true;
        }

        if (
                Math.abs(
                        speed
                ) < EPSILON
        ) {
            return true;
        }

        boolean positive =
                speed > 0.0F;

        return getSelectedDirection()
                == OverrunningClutchDirection.CLOCKWISE
                ? positive
                : !positive;
    }

    @Override
    public float getRotationSpeedModifier(
            Direction side
    ) {
        if (isLockedByRedstone()) {
            return 1.0F;
        }

        if (side.getAxis()
                != getBlockState().getValue(OverrunningClutchBlock.AXIS)) {
            return 0.0F;
        }

        if (!hasSource()) {
            return 1.0F;
        }

        if (getSourceFacing() == side) {
            return 1.0F;
        }

        return isSpeedAllowed(getTheoreticalSpeed())
                ? 1.0F
                : 0.0F;
    }

    public float getVisualSpeedForSide(
            Direction side
    ) {
        if (
                side.getAxis()
                        != getBlockState()
                        .getValue(
                                OverrunningClutchBlock.AXIS
                        )
        ) {
            return 0.0F;
        }

        if (hasSource()) {
            float ownSpeed =
                    getSpeed();

            if (Math.abs(ownSpeed) >= EPSILON) {
                if (isLockedByRedstone()
                        || getSourceFacing() == side
                        || isSpeedAllowed(ownSpeed)) {
                    return ownSpeed;
                }
            }
        }

        KineticBlockEntity neighbour =
                getNeighbouringKinetic(
                        side
                );

        if (neighbour != null) {
            return neighbour.getSpeed();
        }

        return 0.0F;
    }

    private KineticBlockEntity getNeighbouringKinetic(
            Direction side
    ) {
        if (level == null) {
            return null;
        }

        BlockPos neighbourPos =
                worldPosition.relative(
                        side
                );

        BlockState neighbourState =
                level.getBlockState(
                        neighbourPos
                );

        if (!(
                neighbourState.getBlock()
                        instanceof IRotate rotate
        )) {
            return null;
        }

        if (!rotate.hasShaftTowards(
                level,
                neighbourPos,
                neighbourState,
                side.getOpposite()
        )) {
            return null;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(
                        neighbourPos
                );

        if (!(
                blockEntity
                        instanceof KineticBlockEntity kinetic
        )) {
            return null;
        }

        return kinetic;
    }

    private static class DirectionValueBoxTransform
            extends ValueBoxTransform {

        @Override
        public Vec3 getLocalOffset(
                LevelAccessor level,
                BlockPos pos,
                BlockState state
        ) {
            Direction side =
                    getRenderSide(
                            state
                    );

            if (side == null) {
                return null;
            }

            return getLocationForSide(
                    side
            );
        }

        @Override
        public void rotate(
                LevelAccessor level,
                BlockPos pos,
                BlockState state,
                PoseStack poseStack
        ) {
            Direction side =
                    getRenderSide(
                            state
                    );

            if (side == null) {
                return;
            }

            poseStack.mulPose(
                    com.mojang.math.Axis.YP.rotationDegrees(
                            AngleHelper.horizontalAngle(side)
                                    + 180.0F
                    )
            );

            if (side == Direction.UP) {
                poseStack.mulPose(
                        com.mojang.math.Axis.XP.rotationDegrees(
                                90.0F
                        )
                );
            } else if (side == Direction.DOWN) {
                poseStack.mulPose(
                        com.mojang.math.Axis.XP.rotationDegrees(
                                270.0F
                        )
                );
            }
        }

        @Override
        public boolean testHit(
                LevelAccessor level,
                BlockPos pos,
                BlockState state,
                Vec3 localHit
        ) {
            for (Direction side : Direction.values()) {
                if (!isSideActive(state, side)) {
                    continue;
                }

                if (localHit.distanceTo(
                        getLocationForSide(side)
                ) < scale / 2.0F) {
                    return true;
                }
            }

            return false;
        }

        private Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(
                    8.0F,
                    8.0F,
                    14.51F
            );
        }

        private Vec3 getLocationForSide(
                Direction side
        ) {
            Vec3 location =
                    getSouthLocation();

            location =
                    VecHelper.rotateCentered(
                            location,
                            AngleHelper.horizontalAngle(side),
                            Direction.Axis.Y
                    );

            return VecHelper.rotateCentered(
                    location,
                    AngleHelper.verticalAngle(side),
                    Direction.Axis.X
            );
        }

        private Direction getRenderSide(
                BlockState state
        ) {
            if (isSideActive(state, Direction.UP)) {
                return Direction.UP;
            }

            if (isSideActive(state, Direction.NORTH)) {
                return Direction.NORTH;
            }

            return null;
        }

        private boolean isSideActive(
                BlockState state,
                Direction direction
        ) {
            if (
                    !state.hasProperty(
                            OverrunningClutchBlock.AXIS
                    )
            ) {
                return false;
            }

            Direction.Axis shaftAxis =
                    state.getValue(
                            OverrunningClutchBlock.AXIS
                    );

            Direction.Axis localZAxis =
                    shaftAxis == Direction.Axis.Y
                            ? Direction.Axis.Z
                            : Direction.Axis.Y;

            return direction.getAxis()
                    == localZAxis;
        }

        @Override
        public float getScale() {
            return 0.5F;
        }
    }
}
