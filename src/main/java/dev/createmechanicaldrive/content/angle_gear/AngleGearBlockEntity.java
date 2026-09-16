package dev.createmechanicaldrive.content.angle_gear;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AngleGearBlockEntity
        extends SplitShaftBlockEntity {

    private Direction ponderSourceFacing;

    public AngleGearBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    @Override
    public float getRotationSpeedModifier(
            Direction direction
    ) {
        BlockState state =
                getBlockState();

        if (!AngleGearBlock.hasGearTowards(state, direction)) {
            return 0.0F;
        }

        Direction sourceFacing =
                hasSource()
                        ? getSourceFacing()
                        : null;

        if (sourceFacing != null
                && !AngleGearBlock.hasGearTowards(state, sourceFacing)) {
            return 0.0F;
        }

        if (sourceFacing == null) {
            return 1.0F;
        }

        if (!areInternallyConnected(
                state,
                sourceFacing,
                direction
        )) {
            return 0.0F;
        }

        return getRotationModifier(sourceFacing, direction);
    }

    private static float getCanonicalSideModifier(
            Direction direction
    ) {
        return switch (direction) {
            case WEST, SOUTH, UP -> 1.0F;
            case EAST, NORTH, DOWN -> -1.0F;
        };
    }

    private static float getRotationModifier(
            Direction source,
            Direction target
    ) {
        return getCanonicalSideModifier(target)
                / getCanonicalSideModifier(source)
                * getVerticalPairModifier(source, target);
    }

    private static float getVerticalPairModifier(
            Direction source,
            Direction target
    ) {
        boolean sourceVertical =
                source.getAxis() == Direction.Axis.Y;

        boolean targetVertical =
                target.getAxis() == Direction.Axis.Y;

        if (!sourceVertical && !targetVertical) {
            return 1.0F;
        }

        Direction horizontal = sourceVertical ? target : source;
        return horizontal.getAxis() == Direction.Axis.Z
                ? -1.0F
                : 1.0F;
    }

    @Override
    public List<BlockPos> addPropagationLocations(
            IRotate rotate,
            BlockState state,
            List<BlockPos> neighbours
    ) {
        for (Direction direction : Direction.values()) {
            if (AngleGearBlock.hasGearTowards(state, direction)) {
                addIfMissing(
                        neighbours,
                        worldPosition.relative(direction)
                );
            }
        }

        return neighbours;
    }

    public float getVisualSpeedForSide(
            Direction side
    ) {
        BlockState state =
                getBlockState();

        if (!AngleGearBlock.hasGearTowards(state, side)) {
            return 0.0F;
        }

        Direction sourceFacing =
                ponderSourceFacing != null
                        ? ponderSourceFacing
                        : hasSource()
                        ? getSourceFacing()
                        : null;

        if (sourceFacing == null) {
            return getSpeed();
        }

        if (!AngleGearBlock.hasGearTowards(state, sourceFacing)) {
            return 0.0F;
        }

        if (!areInternallyConnected(
                state,
                sourceFacing,
                side
        )) {
            return 0.0F;
        }

        return getSpeed()
                * getRotationModifier(sourceFacing, side);
    }

    public void setPonderSourceFacing(
            Direction ponderSourceFacing
    ) {
        this.ponderSourceFacing =
                ponderSourceFacing;
    }

    private static boolean areInternallyConnected(
            BlockState state,
            Direction source,
            Direction target
    ) {
        if (!AngleGearBlock.hasGearTowards(state, source)
                || !AngleGearBlock.hasGearTowards(state, target)) {
            return false;
        }

        if (source == target) {
            return true;
        }

        if (source.getAxis() != target.getAxis()) {
            return true;
        }

        for (Direction bridge : Direction.values()) {
            if (bridge.getAxis() != source.getAxis()
                    && AngleGearBlock.hasGearTowards(state, bridge)) {
                return true;
            }
        }

        return false;
    }

    private static void addIfMissing(
            List<BlockPos> positions,
            BlockPos pos
    ) {
        if (!positions.contains(pos)) {
            positions.add(pos);
        }
    }

    public void rebuildKinetics() {
        if (level == null || level.isClientSide) {
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

        setChanged();
        sendData();
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }
}
