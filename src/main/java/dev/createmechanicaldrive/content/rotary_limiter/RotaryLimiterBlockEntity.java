package dev.createmechanicaldrive.content.rotary_limiter;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class RotaryLimiterBlockEntity
        extends GeneratingKineticBlockEntity {

    public static final int ANGLE_STEP = 5;

    public static final int MIN_ANGLE_DEGREES = 5;
    public static final int MAX_ANGLE_DEGREES = 360;
    public static final int DEFAULT_ANGLE_DEGREES = 45;

    private static final int MIN_ANGLE_STEP =
            MIN_ANGLE_DEGREES / ANGLE_STEP;

    private static final int MAX_ANGLE_STEP =
            MAX_ANGLE_DEGREES / ANGLE_STEP;

    private static final int DEFAULT_ANGLE_STEP =
            DEFAULT_ANGLE_DEGREES / ANGLE_STEP;

    private static final float OUTPUT_RPM =
            16.0F;

    private static final float START_DEADBAND =
            5.0F;

    private static final float STOP_EPSILON =
            0.1F;

    private static final float EPSILON =
            0.001F;

    private static final String MAX_ANGLE_STEP_TAG =
            "MaxAngleStep";

    private static final String TARGET_ANGLE_TAG =
            "TargetAngle";

    private static final String OUTPUT_ANGLE_TAG =
            "OutputAngle";

    private static final String GENERATED_SPEED_TAG =
            "GeneratedSpeed";

    private static final String INPUT_CAPACITY_TAG =
            "InputCapacity";

    private int maxAngleStep =
            DEFAULT_ANGLE_STEP;

    private float targetAngle =
            0.0F;

    private float outputAngle =
            0.0F;

    private float generatedSpeed =
            0.0F;

    private float rememberedInputCapacity =
            0.0F;

    private List<ScrollValueBehaviour>
            angleBehaviours;

    private boolean syncingAngle;

    public RotaryLimiterBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(
            List<BlockEntityBehaviour> behaviours
    ) {
        super.addBehaviours(behaviours);

        angleBehaviours =
                new ArrayList<>();

        for (Direction side : Direction.values()) {
            ScrollValueBehaviour behaviour =
                    new AngleScrollValueBehaviour(
                            Component.translatable(
                                    "mechanical_drive.rotary_limiter.max_angle"
                            ),
                            this,
                            new AngleValueBoxTransform()
                                    .fromSide(side)
                    );

            behaviour
                    .between(
                            MIN_ANGLE_STEP,
                            MAX_ANGLE_STEP
                    )
                    .withFormatter(
                            value ->
                                    value
                                            * ANGLE_STEP
                                            + "\u00B0"
                    )
                    .withCallback(
                            this::onMaxAngleStepChanged
                    );

            behaviour.setValue(
                    maxAngleStep
            );

            angleBehaviours.add(
                    behaviour
            );

            behaviours.add(
                    behaviour
            );
        }
    }

    private void onMaxAngleStepChanged(
            int value
    ) {
        int newValue =
                Mth.clamp(
                        value,
                        MIN_ANGLE_STEP,
                        MAX_ANGLE_STEP
                );

        maxAngleStep =
                newValue;

        if (!syncingAngle) {
            syncingAngle =
                    true;

            for (ScrollValueBehaviour behaviour
                    : angleBehaviours) {

                if (behaviour.getValue()
                        != newValue) {
                    behaviour.setValue(
                            newValue
                    );
                }
            }

            syncingAngle =
                    false;
        }

        float limit =
                getMaxAngleDegrees();

        targetAngle =
                Mth.clamp(
                        targetAngle,
                        -limit,
                        limit
                );

        if (level != null
                && !level.isClientSide) {
            setChanged();
            sendData();
        }
    }

    public int getMaxAngleDegrees() {
        return maxAngleStep
                * ANGLE_STEP;
    }

    public Direction getOutputDirection() {
        return RotaryLimiterBlock
                .getOutputDirection(
                        getBlockState()
                );
    }

    public Direction getInputDirection() {
        return RotaryLimiterBlock
                .getInputDirection(
                        getBlockState()
                );
    }

    public boolean isInputNeighbour(
            BlockPos neighbourPos
    ) {
        return worldPosition
                .relative(
                        getInputDirection()
                )
                .equals(
                        neighbourPos
                );
    }

    private float getRawInputSpeed() {
        if (level == null) {
            return 0.0F;
        }

        Direction input =
                getInputDirection();

        BlockPos neighbourPos =
                worldPosition.relative(
                        input
                );

        BlockState neighbourState =
                level.getBlockState(
                        neighbourPos
                );

        if (!(level.getBlockEntity(neighbourPos)
                instanceof KineticBlockEntity kinetic)) {
            return 0.0F;
        }

        if (!(neighbourState.getBlock()
                instanceof KineticBlock kineticBlock)) {
            return 0.0F;
        }

        if (!kineticBlock.hasShaftTowards(
                level,
                neighbourPos,
                neighbourState,
                input.getOpposite()
        )) {
            return 0.0F;
        }

        return kinetic.getSpeed();
    }

    private float getInputNetworkCapacity() {
        if (level == null) {
            return 0.0F;
        }

        Direction input =
                getInputDirection();

        BlockPos neighbourPos =
                worldPosition.relative(
                        input
                );

        if (!(level.getBlockEntity(neighbourPos)
                instanceof KineticBlockEntity kinetic)) {
            return 0.0F;
        }

        if (!kinetic.hasNetwork()) {
            return 0.0F;
        }

        return kinetic
                .getOrCreateNetwork()
                .calculateCapacity();
    }

    private float getLogicalInputSpeed() {
        return getRawInputSpeed();
    }

    public float getInputVisualSpeed() {
        return getRawInputSpeed();
    }

    private void updateRememberedInputCapacity() {
        float inputSpeed =
                getRawInputSpeed();

        if (Math.abs(inputSpeed)
                < EPSILON) {
            return;
        }

        float inputCapacity =
                getInputNetworkCapacity();

        if (inputCapacity <= 0.0F) {
            return;
        }

        if (Math.abs(
                rememberedInputCapacity
                        - inputCapacity
        ) < EPSILON) {
            return;
        }

        rememberedInputCapacity =
                inputCapacity;

        updateOutputCapacityInNetwork();

        setChanged();
        sendData();
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null
                || level.isClientSide) {
            return;
        }

        updateRememberedInputCapacity();

        updateTargetFromInput();

        advanceOutputAngle();

        updateOutputSpeed();
    }

    private void updateTargetFromInput() {
        float inputSpeed =
                getLogicalInputSpeed();

        if (Math.abs(inputSpeed)
                < EPSILON) {
            return;
        }

        float inputAngleDelta =
                KineticBlockEntity
                        .convertToAngular(
                                inputSpeed
                        );

        float limit =
                getMaxAngleDegrees();

        float newTarget =
                Mth.clamp(
                        targetAngle
                                + inputAngleDelta,
                        -limit,
                        limit
                );

        if (Math.abs(
                newTarget
                        - targetAngle
        ) < EPSILON) {
            return;
        }

        targetAngle =
                newTarget;

        setChanged();
        sendData();
    }

    private void advanceOutputAngle() {
        if (Math.abs(generatedSpeed)
                < EPSILON) {
            return;
        }

        float angleDelta =
                KineticBlockEntity
                        .convertToAngular(
                                generatedSpeed
                        );

        float previousError =
                targetAngle
                        - outputAngle;

        float newOutput =
                outputAngle
                        + angleDelta;

        float newError =
                targetAngle
                        - newOutput;

        if (Math.signum(previousError)
                != Math.signum(newError)) {
            outputAngle =
                    targetAngle;
        } else {
            outputAngle =
                    newOutput;
        }

        setChanged();
        sendData();
    }

    private void updateOutputSpeed() {
        float error =
                targetAngle
                        - outputAngle;

        float absError =
                Math.abs(error);

        boolean currentlyMoving =
                Math.abs(generatedSpeed)
                        > EPSILON;

        if (!currentlyMoving
                && absError < START_DEADBAND) {
            setGeneratedSpeed(
                    0.0F
            );
            return;
        }

        if (currentlyMoving
                && absError <= STOP_EPSILON) {
            outputAngle =
                    targetAngle;

            setGeneratedSpeed(
                    0.0F
            );
            return;
        }

        if (absError <= STOP_EPSILON) {
            setGeneratedSpeed(
                    0.0F
            );
            return;
        }

        float rpmNeededForOneTick =
                absError
                        / 0.3F;

        float rpm =
                Math.min(
                        OUTPUT_RPM,
                        rpmNeededForOneTick
                );

        float newGeneratedSpeed =
                Math.copySign(
                        rpm,
                        error
                );

        setGeneratedSpeed(
                newGeneratedSpeed
        );
    }

    private void updateOutputCapacityInNetwork() {
        if (level == null
                || level.isClientSide
                || !hasNetwork()) {
            return;
        }

        getOrCreateNetwork()
                .updateCapacityFor(
                        this,
                        calculateAddedStressCapacity()
                );

        networkDirty =
                true;
    }

    private void setGeneratedSpeed(
            float newSpeed
    ) {
        if (Math.abs(
                newSpeed
                        - generatedSpeed
        ) < EPSILON) {
            return;
        }

        generatedSpeed =
                newSpeed;

        updateGeneratedRotation();

        updateOutputCapacityInNetwork();

        setChanged();
        sendData();
    }

    @Override
    public float getGeneratedSpeed() {
        return generatedSpeed;
    }

    public void setPonderOutputSpeed(
            float speed
    ) {
        generatedSpeed =
                speed;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float outputSpeed =
                Math.abs(
                        getGeneratedSpeed()
                );

        if (outputSpeed < EPSILON
                || rememberedInputCapacity <= 0.0F) {
            lastCapacityProvided =
                    0.0F;

            return 0.0F;
        }

        float baseCapacity =
                rememberedInputCapacity
                        / outputSpeed;

        lastCapacityProvided =
                baseCapacity;

        return baseCapacity;
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        tag.putInt(
                MAX_ANGLE_STEP_TAG,
                maxAngleStep
        );

        tag.putFloat(
                TARGET_ANGLE_TAG,
                targetAngle
        );

        tag.putFloat(
                OUTPUT_ANGLE_TAG,
                outputAngle
        );

        tag.putFloat(
                GENERATED_SPEED_TAG,
                generatedSpeed
        );

        tag.putFloat(
                INPUT_CAPACITY_TAG,
                rememberedInputCapacity
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
        super.read(
                tag,
                registries,
                clientPacket
        );

        rememberedInputCapacity =
                tag.getFloat(
                        INPUT_CAPACITY_TAG
                );

        maxAngleStep =
                Mth.clamp(
                        tag.contains(
                                MAX_ANGLE_STEP_TAG
                        )
                                ? tag.getInt(
                                MAX_ANGLE_STEP_TAG
                        )
                                : DEFAULT_ANGLE_STEP,
                        MIN_ANGLE_STEP,
                        MAX_ANGLE_STEP
                );

        targetAngle =
                tag.getFloat(
                        TARGET_ANGLE_TAG
                );

        outputAngle =
                tag.getFloat(
                        OUTPUT_ANGLE_TAG
                );

        generatedSpeed =
                tag.getFloat(
                        GENERATED_SPEED_TAG
                );

        if (angleBehaviours != null) {
            syncingAngle =
                    true;

            for (ScrollValueBehaviour behaviour
                    : angleBehaviours) {
                behaviour.setValue(
                        maxAngleStep
                );
            }

            syncingAngle =
                    false;
        }
    }

    private static class AngleScrollValueBehaviour
            extends ScrollValueBehaviour {

        public AngleScrollValueBehaviour(
                Component label,
                RotaryLimiterBlockEntity blockEntity,
                ValueBoxTransform slot
        ) {
            super(
                    label,
                    blockEntity,
                    slot
            );
        }

        @Override
        public ValueSettingsBoard createBoard(
                Player player,
                BlockHitResult hitResult
        ) {
            return new ValueSettingsBoard(
                    label,

                    MAX_ANGLE_STEP - MIN_ANGLE_STEP,
                    1,

                    List.of(
                            Component.translatable(
                                    "mechanical_drive.rotary_limiter.max_angle"
                            )
                    ),

                    new ValueSettingsFormatter(
                            settings ->
                                    Component.literal(
                                            (
                                                    settings.value()
                                                            + MIN_ANGLE_STEP
                                            )
                                                    * ANGLE_STEP
                                                    + "\u00B0"
                                    )
                    )
            );
        }

        @Override
        public ValueSettings getValueSettings() {
            return new ValueSettings(
                    0,
                    getValue()
                            - MIN_ANGLE_STEP
            );
        }

        @Override
        public void setValueSettings(
                Player player,
                ValueSettings valueSetting,
                boolean ctrlDown
        ) {
            setValue(
                    valueSetting.value()
                            + MIN_ANGLE_STEP
            );

            playFeedbackSound(
                    this
            );
        }
    }

    private static class AngleValueBoxTransform
            extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(
                    8.0F,
                    8.0F,
                    15.51F
            );
        }

        @Override
        protected boolean isSideActive(
                BlockState state,
                Direction direction
        ) {
            if (!state.hasProperty(
                    RotaryLimiterBlock.FACING
            )) {
                return false;
            }

            Direction facing =
                    state.getValue(
                            RotaryLimiterBlock.FACING
                    );

            return direction.getAxis()
                    != facing.getAxis();
        }

        @Override
        public float getScale() {
            return 0.5F;
        }
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }
}