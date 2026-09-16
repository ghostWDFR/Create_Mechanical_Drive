package dev.createmechanicaldrive.content.gear_reducer;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GearReducerKinetics {

    private GearReducerKinetics() {
    }

    public static GearReducerBlockEntity findDrivingReducer(
            KineticBlockEntity output
    ) {
        if (
                output.hasSource()
                        && !(output instanceof GearReducerBlockEntity)
        ) {
            return null;
        }

        Level level =
                output.getLevel();

        if (level == null) {
            return null;
        }

        BlockPos outputPos =
                output.getBlockPos();

        for (
                Direction direction
                : Direction.values()
        ) {
            BlockPos reducerPos =
                    outputPos.relative(
                            direction
                    );

            BlockEntity blockEntity =
                    level.getBlockEntity(
                            reducerPos
                    );

            if (!(
                    blockEntity
                            instanceof GearReducerBlockEntity reducer
            )) {
                continue;
            }

            if (!reducer.isReductionMode()) {
                continue;
            }

            if (
                    !reducer.getBlockPos()
                            .relative(
                                    reducer.getInputDirection()
                            )
                            .equals(outputPos)
            ) {
                continue;
            }

            if (!hasShaftTowards(
                    output,
                    direction
            )) {
                continue;
            }

            return reducer;
        }

        return null;
    }

    public static boolean isVirtualReductionOutput(
            KineticBlockEntity output
    ) {
        return findDrivingReducer(output) != null;
    }

    public static float getOutputSpeed(
            KineticBlockEntity output
    ) {
        GearReducerBlockEntity reducer =
                findDrivingReducer(output);

        if (reducer == null) {
            return 0.0F;
        }

        return reducer.getReductionOutputSpeed();
    }

    public static Direction getVirtualReductionSourceFacing(
            KineticBlockEntity output
    ) {
        GearReducerBlockEntity reducer =
                findDrivingReducer(output);

        if (reducer == null) {
            return null;
        }

        BlockPos offset =
                reducer.getBlockPos()
                        .subtract(
                                output.getBlockPos()
                        );

        return Direction.getNearest(
                offset.getX(),
                offset.getY(),
                offset.getZ()
        );
    }

    public static boolean shouldSeparateReductionConnection(
            KineticBlockEntity first,
            KineticBlockEntity second
    ) {
        if (
                first instanceof GearReducerBlockEntity reducer
                        && isReducedOutputNeighbour(
                        reducer,
                        second
                )
        ) {
            return true;
        }

        return second instanceof GearReducerBlockEntity reducer
                && isReducedOutputNeighbour(
                reducer,
                first
        );
    }

    public static int getReductionStageDepth(
            KineticBlockEntity kinetic
    ) {
        if (kinetic.getLevel() == null) {
            return 0;
        }

        KineticBlockEntity current =
                kinetic;

        int depth =
                0;

        for (int i = 0; i < 32; i++) {
            if (current.network == null) {
                break;
            }

            BlockPos rootPos =
                    BlockPos.of(
                            current.network
                    );

            if (!(
                    current.getLevel()
                            .getBlockEntity(rootPos)
                            instanceof KineticBlockEntity root
            )) {
                break;
            }

            GearReducerBlockEntity reducer =
                    findDrivingReducer(root);

            if (reducer == null) {
                break;
            }

            depth++;

            if (!reducer.hasNetwork()) {
                break;
            }

            BlockPos upstreamRootPos =
                    BlockPos.of(
                            reducer.network
                    );

            if (!(
                    current.getLevel()
                            .getBlockEntity(upstreamRootPos)
                            instanceof KineticBlockEntity upstreamRoot
            )) {
                break;
            }

            if (upstreamRoot == root) {
                break;
            }

            current =
                    upstreamRoot;
        }

        return depth;
    }

    public static float getCapacityMultiplierForStage(
            int stage,
            int selectedMultiplier
    ) {
        if (stage <= 1) {
            return selectedMultiplier;
        }

        if (stage == 2) {
            return Math.max(
                    1.0F,
                    selectedMultiplier / 2.0F
            );
        }

        if (stage == 3 && selectedMultiplier >= 4) {
            return 1.5F;
        }

        return 1.0F;
    }

    public static float getOutputActualCapacity(
            KineticBlockEntity output
    ) {
        GearReducerBlockEntity reducer =
                findDrivingReducer(output);

        if (
                reducer == null
                        || !reducer.hasNetwork()
        ) {
            return 0.0F;
        }

        float inputCapacity =
                reducer.getOrCreateNetwork()
                        .calculateCapacity();

        int outputStage =
                getReductionStageDepth(
                        reducer
                )
                        + 1;

        float multiplier =
                getCapacityMultiplierForStage(
                        outputStage,
                        reducer.getMultiplier()
                );

        return inputCapacity
                * multiplier;
    }

    public static float getOutputBaseCapacity(
            KineticBlockEntity output
    ) {
        float speed =
                Math.abs(
                        getOutputSpeed(output)
                );

        if (speed == 0.0F) {
            return 0.0F;
        }

        return getOutputActualCapacity(output)
                / speed;
    }

    private static boolean isReducedOutputNeighbour(
            GearReducerBlockEntity reducer,
            KineticBlockEntity other
    ) {
        if (!reducer.isReductionMode()) {
            return false;
        }

        Direction reducedOutputSide =
                reducer.getInputDirection();

        if (!reducer.getBlockPos()
                .relative(
                        reducedOutputSide
                )
                .equals(
                        other.getBlockPos()
                )) {
            return false;
        }

        return !other.hasSource()
                || other instanceof GearReducerBlockEntity;
    }

    private static boolean hasShaftTowards(
            KineticBlockEntity kinetic,
            Direction sideToReducer
    ) {
        Level level =
                kinetic.getLevel();

        if (level == null) {
            return false;
        }

        BlockState state =
                kinetic.getBlockState();

        if (!(
                state.getBlock()
                        instanceof IRotate rotate
        )) {
            return false;
        }

        return rotate.hasShaftTowards(
                level,
                kinetic.getBlockPos(),
                state,
                sideToReducer
        );
    }
}
