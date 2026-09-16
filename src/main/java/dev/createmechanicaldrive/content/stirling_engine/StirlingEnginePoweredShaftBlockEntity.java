package dev.createmechanicaldrive.content.stirling_engine;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class StirlingEnginePoweredShaftBlockEntity extends GeneratingKineticBlockEntity {
    private static final float GENERATED_RPM = 256.0F;
    private static final float GENERATED_SU = 256.0F;
    private static final String RUNNING_TAG = "Running";
    private static final String ROTATION_SIGN_TAG = "RotationSign";
    private static final String SPEED_MULTIPLIER_TAG = "SpeedMultiplier";

    private boolean running;
    private int rotationSign = 1;
    private float speedMultiplier = 1.0F;

    public StirlingEnginePoweredShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = level.getBlockState(worldPosition);
        if (!state.is(dev.createmechanicaldrive.CreateMechanicalDrive.STIRLING_ENGINE_POWERED_SHAFT.get())) {
            return;
        }

        if (!hasValidOutput(level, worldPosition, state)) {
            level.setBlock(
                    worldPosition,
                    StirlingEnginePoweredShaftBlock.getShaftEquivalent(state),
                    3
            );
            return;
        }

        refreshGeneratedState();
    }

    public void refreshGeneratedState() {
        if (level == null || level.isClientSide) {
            return;
        }

        StirlingEngineHeaterBlockEntity heater = getHeater(level, worldPosition, getBlockState());
        boolean canRun = heater != null && heater.isHeated();
        boolean shouldRun = running && canRun;
        int nextRotationSign = rotationSign;
        float nextSpeedMultiplier = canRun ? heater.getSpeedMultiplier() : 1.0F;

        if (canRun && !running) {
            float startSpeed = getSpeed();
            if (Math.abs(startSpeed) > 0.001F) {
                shouldRun = true;
                nextRotationSign = startSpeed < 0.0F ? -1 : 1;
            }
        }

        if (nextSpeedMultiplier <= 0.001F) {
            shouldRun = false;
        }

        if (shouldRun == running
                && nextRotationSign == rotationSign
                && Math.abs(nextSpeedMultiplier - speedMultiplier) < 0.001F) {
            return;
        }

        running = shouldRun;
        rotationSign = nextRotationSign == 0 ? 1 : nextRotationSign;
        speedMultiplier = nextSpeedMultiplier;
        updateGeneratedRotation();
        setChanged();
        sendData();
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = running ? GENERATED_SU / GENERATED_RPM : 0.0F;
        lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public float getGeneratedSpeed() {
        return running ? GENERATED_RPM * speedMultiplier * rotationSign : 0.0F;
    }

    static boolean hasHeatedOutput(LevelReader level, BlockPos pos, BlockState state) {
        return getHeater(level, pos, state) != null;
    }

    static boolean hasValidOutput(LevelReader level, BlockPos pos, BlockState state) {
        return findAssembly(level, pos, state) != null;
    }

    private static StirlingEngineHeaterBlockEntity getHeater(LevelReader level, BlockPos pos, BlockState state) {
        Assembly assembly = findAssembly(level, pos, state);
        if (assembly == null || level.hasNeighborSignal(assembly.corePos())) {
            return null;
        }

        if (level.getBlockEntity(assembly.heaterPos()) instanceof StirlingEngineHeaterBlockEntity heater
                && heater.isHeated()) {
            return heater;
        }

        return null;
    }

    private static Assembly findAssembly(LevelReader level, BlockPos pos, BlockState state) {
        Direction.Axis shaftAxis = state.getValue(StirlingEnginePoweredShaftBlock.AXIS);

        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == shaftAxis) {
                continue;
            }

            BlockPos outputPos = pos.relative(direction.getOpposite(), 2);
            BlockState outputState = level.getBlockState(outputPos);

            if (!outputState.is(dev.createmechanicaldrive.CreateMechanicalDrive.STIRLING_ENGINE_OUTPUT.get())) {
                continue;
            }

            if (StirlingEngineOutputBlock.getShaftPos(outputState, outputPos).equals(pos)) {
                BlockPos corePos = outputPos.relative(outputState.getValue(StirlingEngineOutputBlock.FACING).getOpposite());
                BlockState coreState = level.getBlockState(corePos);

                if (!StirlingEngineCoreBlock.isValidAssembly(level, corePos, coreState)) {
                    continue;
                }

                return new Assembly(
                        corePos,
                        corePos.relative(coreState.getValue(StirlingEngineCoreBlock.FACING).getOpposite())
                );
            }
        }

        return null;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putBoolean(RUNNING_TAG, running);
        tag.putInt(ROTATION_SIGN_TAG, rotationSign);
        tag.putFloat(SPEED_MULTIPLIER_TAG, speedMultiplier);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        running = tag.getBoolean(RUNNING_TAG);
        rotationSign = tag.getInt(ROTATION_SIGN_TAG);
        speedMultiplier = tag.contains(SPEED_MULTIPLIER_TAG) ? tag.getFloat(SPEED_MULTIPLIER_TAG) : 1.0F;
        if (rotationSign == 0) {
            rotationSign = 1;
        }
        super.read(tag, registries, clientPacket);
    }

    private record Assembly(BlockPos corePos, BlockPos heaterPos) {
    }
}
