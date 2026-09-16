package dev.createmechanicaldrive.content.tank_transmission;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

public class TankTransmissionDistributorBlockEntity
        extends SplitShaftBlockEntity {

    private static final float STRESS_IMPACT =
            6.0F;

    private static final String LEFT_VISUAL_MULTIPLIER_TAG =
            "LeftVisualMultiplier";

    private static final String RIGHT_VISUAL_MULTIPLIER_TAG =
            "RightVisualMultiplier";

    private float leftVisualMultiplier = 1.0F;
    private float rightVisualMultiplier = 1.0F;

    public TankTransmissionDistributorBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null
                || level.isClientSide) {
            return;
        }

        restoreInputSource();
        refreshFromSteering();
    }

    public void refreshFromSteering() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state =
                getBlockState();

        if (!state.hasProperty(
                TankTransmissionDistributorBlock.FACING
        )) {
            return;
        }

        Direction facing =
                state.getValue(
                        TankTransmissionDistributorBlock.FACING
                );

        Direction leftSide =
                facing.getClockWise();

        Direction rightSide =
                facing.getCounterClockWise();

        float newLeftMultiplier = 1.0F;
        float newRightMultiplier = 1.0F;

        TankTransmissionSteeringBlockEntity verticalSteering =
                getVerticalSteeringAbove(
                        worldPosition
                );

        if (verticalSteering == null) {
            verticalSteering =
                    getVerticalSteeringThroughHousing(
                            leftSide
                    );
        }

        if (verticalSteering == null) {
            verticalSteering =
                    getVerticalSteeringThroughHousing(
                            rightSide
                    );
        }

        if (verticalSteering != null) {
            newLeftMultiplier =
                    verticalSteering.getDesiredLeftMultiplier();

            newRightMultiplier =
                    verticalSteering.getDesiredRightMultiplier();

            setOutputMultipliers(
                    newLeftMultiplier,
                    newRightMultiplier
            );

            return;
        }

        TankTransmissionSteeringBlockEntity leftSteering =
                getCompatibleSteering(
                        leftSide,
                        facing
                );

        if (leftSteering != null) {
            newLeftMultiplier =
                    leftSteering.getDesiredLeftMultiplier();

            newRightMultiplier =
                    leftSteering.getDesiredRightMultiplier();

            setOutputMultipliers(
                    newLeftMultiplier,
                    newRightMultiplier
            );

            return;
        }

        TankTransmissionSteeringBlockEntity rightSteering =
                getCompatibleSteering(
                        rightSide,
                        facing
                );

        if (rightSteering != null) {
            newLeftMultiplier =
                    rightSteering.getDesiredLeftMultiplier();

            newRightMultiplier =
                    rightSteering.getDesiredRightMultiplier();

            setOutputMultipliers(
                    newLeftMultiplier,
                    newRightMultiplier
            );

            return;
        }

        setOutputMultipliers(
                1.0F,
                1.0F
        );
    }

    private TankTransmissionSteeringBlockEntity
    getVerticalSteeringAbove(
            BlockPos basePos
    ) {
        BlockPos steeringPos =
                basePos.above();

        if (!level.isLoaded(steeringPos)) {
            return null;
        }

        BlockState steeringState =
                level.getBlockState(steeringPos);

        if (!steeringState.is(
                CreateMechanicalDrive
                        .TANK_TRANSMISSION_STEERING
                        .get()
        )) {
            return null;
        }

        if (!steeringState.hasProperty(
                TankTransmissionSteeringBlock.VERTICAL
        )) {
            return null;
        }

        if (!steeringState.getValue(
                TankTransmissionSteeringBlock.VERTICAL
        )) {
            return null;
        }

        if (level.getBlockEntity(steeringPos)
                instanceof TankTransmissionSteeringBlockEntity steering) {
            return steering;
        }

        return null;
    }

    private TankTransmissionSteeringBlockEntity
    getVerticalSteeringThroughHousing(
            Direction side
    ) {
        BlockPos currentPos =
                worldPosition.relative(side);

        while (level.isLoaded(currentPos)) {
            BlockState currentState =
                    level.getBlockState(currentPos);

            if (!currentState.is(
                    CreateMechanicalDrive
                            .TANK_TRANSMISSION_HOUSING
                            .get()
            )) {
                return null;
            }

            if (!currentState.hasProperty(
                    TankTransmissionHousingBlock.AXIS
            )) {
                return null;
            }

            if (currentState.getValue(
                    TankTransmissionHousingBlock.AXIS
            ) != side.getAxis()) {
                return null;
            }

            TankTransmissionSteeringBlockEntity steering =
                    getVerticalSteeringAbove(
                            currentPos
                    );

            if (steering != null) {
                return steering;
            }

            currentPos =
                    currentPos.relative(side);
        }

        return null;
    }

    private TankTransmissionSteeringBlockEntity
    getCompatibleSteering(
            Direction side,
            Direction facing
    ) {
        BlockPos currentPos =
                worldPosition.relative(side);

        while (level.isLoaded(currentPos)) {
            BlockState currentState =
                    level.getBlockState(currentPos);

            if (currentState.is(
                    CreateMechanicalDrive
                            .TANK_TRANSMISSION_STEERING
                            .get()
            )) {
                if (!currentState.hasProperty(
                        TankTransmissionSteeringBlock.FACING
                )) {
                    return null;
                }

                if (currentState.getValue(
                        TankTransmissionSteeringBlock.FACING
                ).getAxis() != facing.getAxis()) {
                    return null;
                }

                if (level.getBlockEntity(currentPos)
                        instanceof TankTransmissionSteeringBlockEntity steering) {
                    return steering;
                }

                return null;
            }

            if (!currentState.is(
                    CreateMechanicalDrive
                            .TANK_TRANSMISSION_HOUSING
                            .get()
            )) {
                return null;
            }

            if (!currentState.hasProperty(
                    TankTransmissionHousingBlock.AXIS
            )) {
                return null;
            }

            if (currentState.getValue(
                    TankTransmissionHousingBlock.AXIS
            ) != side.getAxis()) {
                return null;
            }

            currentPos =
                    currentPos.relative(side);
        }

        return null;
    }

    private void setOutputMultipliers(
            float leftMultiplier,
            float rightMultiplier
    ) {
        leftMultiplier =
                Math.round(
                        leftMultiplier * 32.0F
                )
                        / 32.0F;

        rightMultiplier =
                Math.round(
                        rightMultiplier * 32.0F
                )
                        / 32.0F;

        if (Float.compare(
                leftVisualMultiplier,
                leftMultiplier
        ) == 0
                && Float.compare(
                rightVisualMultiplier,
                rightMultiplier
        ) == 0) {
            return;
        }

        RotationPropagator.handleRemoved(
                level,
                worldPosition,
                this
        );

        leftVisualMultiplier =
                leftMultiplier;

        rightVisualMultiplier =
                rightMultiplier;

        restoreInputSource();

        RotationPropagator.handleAdded(
                level,
                worldPosition,
                this
        );

        networkDirty = true;

        setChanged();
        sendData();
    }

    private void restoreInputSource() {
        if (level == null
                || level.isClientSide) {
            return;
        }

        Direction inputSide =
                getBlockState().getValue(
                        TankTransmissionDistributorBlock.FACING
                );

        BlockPos inputPos =
                worldPosition.relative(
                        inputSide
                );

        if (!(level.getBlockEntity(inputPos)
                instanceof KineticBlockEntity input)) {
            return;
        }

        float inputSpeed =
                input.getTheoreticalSpeed();

        if (input instanceof SplitShaftBlockEntity splitShaft) {
            Direction directionFromInput =
                    inputSide.getOpposite();

            inputSpeed *=
                    splitShaft.getRotationSpeedModifier(
                            directionFromInput
                    );
        }

        if (Math.abs(inputSpeed) < 0.001F) {
            return;
        }

        if (!hasSource()
                || getSourceFacing() != inputSide) {
            setSource(
                    inputPos
            );
        }

        setSpeed(
                inputSpeed
        );
    }

    @Override
    public float getRotationSpeedModifier(
            Direction direction
    ) {
        Direction inputSide =
                getBlockState().getValue(
                        TankTransmissionDistributorBlock.FACING
                );

        if (!hasSource()) {
            return 1.0F;
        }

        Direction sourceFacing =
                getSourceFacing();

        if (direction == sourceFacing) {
            return 1.0F;
        }

        if (sourceFacing != inputSide) {
            return 1.0F;
        }

        if (direction == inputSide.getClockWise()) {
            return getEffectiveOutputMultiplier(
                    leftVisualMultiplier,
                    getTheoreticalSpeed()
            );
        }

        if (direction == inputSide.getCounterClockWise()) {
            return getEffectiveOutputMultiplier(
                    rightVisualMultiplier,
                    getTheoreticalSpeed()
            );
        }

        return 1.0F;
    }

    private float getEffectiveOutputMultiplier(
            float selectedMultiplier,
            float inputSpeed
    ) {
        if (Math.abs(inputSpeed) < 0.001F) {
            return selectedMultiplier;
        }

        float maxSpeed =
                AllConfigs.server()
                        .kinetics
                        .maxRotationSpeed
                        .get();

        float maxMultiplier =
                maxSpeed
                        / Math.abs(inputSpeed);

        return Math.copySign(
                Math.min(
                        Math.abs(selectedMultiplier),
                        maxMultiplier
                ),
                selectedMultiplier
        );
    }

    public float getVisualSpeedForSide(
            Direction side
    ) {
        BlockState state =
                getBlockState();

        Direction inputSide =
                state.getValue(
                        TankTransmissionDistributorBlock.FACING
                );

        float inputSpeed =
                getSpeed();

        if (side == inputSide) {
            return inputSpeed;
        }

        Direction leftSide =
                inputSide.getClockWise();

        Direction rightSide =
                inputSide.getCounterClockWise();

        if (side == leftSide) {
            return inputSpeed
                    * getEffectiveOutputMultiplier(
                            leftVisualMultiplier,
                            inputSpeed
                    );
        }

        if (side == rightSide) {
            return inputSpeed
                    * getEffectiveOutputMultiplier(
                            rightVisualMultiplier,
                            inputSpeed
                    );
        }

        return inputSpeed;
    }

    public float getInputVisualSpeed() {
        BlockState state = getBlockState();

        Direction input =
                state.getValue(
                        TankTransmissionDistributorBlock.FACING
                );

        return getVisualSpeedForSide(input);
    }

    public float getLeftVisualSpeed() {
        BlockState state = getBlockState();

        Direction input =
                state.getValue(
                        TankTransmissionDistributorBlock.FACING
                );

        return getVisualSpeedForSide(
                input.getClockWise()
        );
    }

    public float getRightVisualSpeed() {
        BlockState state = getBlockState();

        Direction input =
                state.getValue(
                        TankTransmissionDistributorBlock.FACING
                );

        return getVisualSpeedForSide(
                input.getCounterClockWise()
        );
    }

    public float getLeftVisualMultiplier() {
        return leftVisualMultiplier;
    }

    public float getRightVisualMultiplier() {
        return rightVisualMultiplier;
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied =
                STRESS_IMPACT;

        return STRESS_IMPACT;
    }

    public void setPonderOutputMultipliers(
            float leftMultiplier,
            float rightMultiplier
    ) {
        leftVisualMultiplier =
                Math.round(
                        leftMultiplier * 32.0F
                )
                        / 32.0F;

        rightVisualMultiplier =
                Math.round(
                        rightMultiplier * 32.0F
                )
                        / 32.0F;

        setChanged();
        sendData();
    }

    @Override
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        float stressImpact =
                calculateStressApplied();

        float stressUsage =
                Math.abs(getSpeed())
                        * stressImpact;

        CreateLang.builder()
                .add(
                        Component.translatable(
                                "block.mechanical_drive.tank_transmission_distributor"
                        ).append(
                                ":"
                        )
                )
                .style(
                        ChatFormatting.WHITE
                )
                .forGoggles(
                        tooltip
                );

        CreateLang.builder()
                .add(
                        Component.translatable(
                                "tooltip.mechanical_drive.tank_transmission.stress_impact",
                                Component.literal(
                                        formatStress(
                                                stressUsage
                                        )
                                ).withStyle(
                                        ChatFormatting.AQUA
                                )
                        ).withStyle(
                                ChatFormatting.GRAY
                        )
                )
                .forGoggles(
                        tooltip
                );

        return true;
    }

    private static String formatStress(
            float value
    ) {
        return String.format(
                Locale.ROOT,
                "%.0f su",
                value
        );
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        tag.putFloat(
                LEFT_VISUAL_MULTIPLIER_TAG,
                leftVisualMultiplier
        );

        tag.putFloat(
                RIGHT_VISUAL_MULTIPLIER_TAG,
                rightVisualMultiplier
        );

        super.write(
                tag,
                registries,
                clientPacket
        );
    }

    @Override
    protected void read(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        if (tag.contains(
                LEFT_VISUAL_MULTIPLIER_TAG
        )) {
            leftVisualMultiplier =
                    tag.getFloat(
                            LEFT_VISUAL_MULTIPLIER_TAG
                    );
        }

        if (tag.contains(
                RIGHT_VISUAL_MULTIPLIER_TAG
        )) {
            rightVisualMultiplier =
                    tag.getFloat(
                            RIGHT_VISUAL_MULTIPLIER_TAG
                    );
        }

        super.read(
                tag,
                registries,
                clientPacket
        );
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }
}
