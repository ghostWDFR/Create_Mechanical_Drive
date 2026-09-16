package dev.createmechanicaldrive.content.tank_transmission;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public class TankTransmissionSteeringBlockEntity
        extends SplitShaftBlockEntity {

    public static final int MIN_STEERING_ANGLE = -360;
    public static final int MAX_STEERING_ANGLE = 360;
    public static final int DEFAULT_STEERING_ANGLE = 180;

    private static final String STEERING_ANGLE_TAG =
            "SteeringAngle";

    private static final String CONTROL_INPUT_ANGLE_TAG =
            "ControlInputAngle";

    private static final String VISUAL_SHAFT_ANGLE_TAG =
            "VisualShaftAngle";

    private static final float STEERING_RATIO_STEP =
            1.0F / 32.0F;

    private ScrollValueBehaviour maxSteeringAngle;

    private float controlInputAngle = 0.0F;

    private float currentSteeringAngle = 0.0F;

    private float visualShaftAngle = 0.0F;

    private static final float EPSILON =
            0.001F;

    public TankTransmissionSteeringBlockEntity(
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

        maxSteeringAngle =
                new KineticScrollValueBehaviour(
                        Component.translatable(
                                "mechanical_drive.tank_transmission.max_steering_angle"
                        ),
                        this,
                        new SteeringAngleValueBoxTransform()
                                .fromSide(Direction.UP)
                );

        maxSteeringAngle
                .between(
                        MIN_STEERING_ANGLE,
                        MAX_STEERING_ANGLE
                )
                .withCallback(
                        value -> onMaxSteeringAngleChanged()
                );

        maxSteeringAngle.value =
                DEFAULT_STEERING_ANGLE;

        behaviours.add(maxSteeringAngle);
    }

    private static class SteeringAngleValueBoxTransform
            extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(
                    8.0F,
                    8.0F,
                    14.51F
            );
        }

        @Override
        protected boolean isSideActive(
                BlockState state,
                Direction direction
        ) {
            return direction == Direction.UP;
        }

        @Override
        public void rotate(
                LevelAccessor level,
                BlockPos pos,
                BlockState state,
                PoseStack poseStack
        ) {
            super.rotate(
                    level,
                    pos,
                    state,
                    poseStack
            );

            poseStack.mulPose(
                    Axis.ZP.rotationDegrees(90.0F)
            );
        }

        @Override
        public float getScale() {
            return 0.5F;
        }
    }

    public float getSteeringAngle() {
        return currentSteeringAngle;
    }

    public void setPonderSteeringAngle(
            float angle
    ) {
        float limit =
                getSteeringLimit();

        currentSteeringAngle =
                Mth.clamp(
                        angle,
                        -limit,
                        limit
                );

        controlInputAngle =
                currentSteeringAngle;

        setChanged();
        sendData();
    }

    public float getVisualShaftAngle() {
        return visualShaftAngle;
    }

    public int getSteeringLimit() {
        if (maxSteeringAngle == null) {
            return DEFAULT_STEERING_ANGLE;
        }

        return Math.abs(
                Mth.clamp(
                        maxSteeringAngle.getValue(),
                        MIN_STEERING_ANGLE,
                        MAX_STEERING_ANGLE
                )
        );
    }

    private float getSteeringDirectionMultiplier() {
        if (maxSteeringAngle == null) {
            return 1.0F;
        }

        return maxSteeringAngle.getValue() < 0
                ? -1.0F
                : 1.0F;
    }

    public float getSteeringMultiplier() {
        float limit =
                getSteeringLimit();

        if (limit <= 0.0F) {
            return 0.0F;
        }

        float steering =
                Mth.clamp(
                        currentSteeringAngle / limit,
                        -1.0F,
                        1.0F
                );

        float absolute =
                Math.abs(steering);

        if (absolute < 0.01F) {
            return 0.0F;
        }

        float curved =
                absolute * absolute;

        return Math.copySign(
                curved,
                steering
        );
    }

    private float quantizeMultiplier(
            float multiplier
    ) {
        float quantized =
                Math.round(
                        multiplier
                                / STEERING_RATIO_STEP
                )
                        * STEERING_RATIO_STEP;

        if (Math.abs(quantized) < 0.0001F) {
            return 0.0F;
        }

        return quantized;
    }

    public float getDesiredLeftMultiplier() {
        float steering =
                getSteeringMultiplier();

        float amount =
                Math.abs(steering);

        float multiplier;

        if (steering > 0.0F) {
            multiplier =
                    1.0F
                            + Math.min(
                            amount,
                            1.0F - amount
                    );
        } else {
            multiplier =
                    1.0F
                            - amount * 2.0F;
        }

        return quantizeMultiplier(
                multiplier
        );
    }

    public float getDesiredRightMultiplier() {
        float steering =
                getSteeringMultiplier();

        float amount =
                Math.abs(steering);

        float multiplier;

        if (steering < 0.0F) {
            multiplier =
                    1.0F
                            + Math.min(
                            amount,
                            1.0F - amount
                    );
        } else {
            multiplier =
                    1.0F
                            - amount * 2.0F;
        }

        return quantizeMultiplier(
                multiplier
        );
    }

    private Direction getControlInputSide() {
        if (level == null) {
            return null;
        }

        Direction facing =
                getBlockState().getValue(
                        TankTransmissionSteeringBlock.FACING
                );

        if (getControlSpeedFromSide(facing) != 0.0F) {
            return facing;
        }

        Direction back =
                facing.getOpposite();

        if (getControlSpeedFromSide(back) != 0.0F) {
            return back;
        }

        return null;
    }

    private float getLogicalControlSpeed(
            Direction inputSide
    ) {
        float speed =
                getControlSpeedFromSide(inputSide);

        return KineticBlockEntity.convertToDirection(
                speed,
                inputSide
        );
    }

    private float getControlSpeedFromSide(
            Direction side
    ) {
        BlockPos neighbourPos =
                worldPosition.relative(side);

        BlockState neighbourState =
                level.getBlockState(neighbourPos);

        if (!(level.getBlockEntity(neighbourPos)
                instanceof KineticBlockEntity kinetic)) {
            return 0.0F;
        }

        if (kinetic instanceof TankTransmissionSteeringControlPassThrough passThrough
                && passThrough.mechanicalDrive$isTankSteeringControlPassThrough()
                && worldPosition.equals(
                passThrough.mechanicalDrive$getTankSteeringControlPassThroughSource()
        )) {
            return 0.0F;
        }

        if (!(neighbourState.getBlock()
                instanceof KineticBlock kineticBlock)) {
            return 0.0F;
        }

        Direction towardSteering =
                side.getOpposite();

        if (!kineticBlock.hasShaftTowards(
                level,
                neighbourPos,
                neighbourState,
                towardSteering
        )) {
            return 0.0F;
        }

        return kinetic.getSpeed();
    }

    public static float getControlPassThroughSpeed(
            KineticBlockEntity output
    ) {
        return getControlPassThrough(output).speed();
    }

    public static ControlPassThrough getControlPassThrough(
            KineticBlockEntity output
    ) {
        if (output.getLevel() == null) {
            return ControlPassThrough.EMPTY;
        }

        for (Direction directionToSteering : Direction.values()) {
            BlockPos steeringPos =
                    output.getBlockPos()
                            .relative(directionToSteering);

            if (!(
                    output.getLevel()
                            .getBlockEntity(steeringPos)
                            instanceof TankTransmissionSteeringBlockEntity steering
            )) {
                continue;
            }

            BlockState steeringState =
                    steering.getBlockState();

            if (steeringState.getValue(
                    TankTransmissionSteeringBlock.VERTICAL
            )) {
                continue;
            }

            Direction outputSide =
                    directionToSteering.getOpposite();

            if (!isControlSide(
                    steeringState,
                    outputSide
            )) {
                continue;
            }

            if (!hasShaftTowards(
                    output,
                    directionToSteering
            )) {
                continue;
            }

            Direction inputSide =
                    outputSide.getOpposite();

            float inputSpeed =
                    getControlSpeedFromSide(
                            steering,
                            inputSide,
                            output.getBlockPos()
                    );

            if (Math.abs(inputSpeed) > EPSILON) {
                return new ControlPassThrough(
                        inputSpeed,
                        steering.getBlockPos()
                );
            }
        }

        return ControlPassThrough.EMPTY;
    }

    private static float getControlSpeedFromSide(
            TankTransmissionSteeringBlockEntity steering,
            Direction side,
            BlockPos excludedOutputPos
    ) {
        if (steering.level == null) {
            return 0.0F;
        }

        BlockPos neighbourPos =
                steering.worldPosition.relative(side);

        if (neighbourPos.equals(excludedOutputPos)) {
            return 0.0F;
        }

        BlockState neighbourState =
                steering.level.getBlockState(neighbourPos);

        if (!(steering.level.getBlockEntity(neighbourPos)
                instanceof KineticBlockEntity kinetic)) {
            return 0.0F;
        }

        if (kinetic instanceof TankTransmissionSteeringControlPassThrough passThrough
                && passThrough.mechanicalDrive$isTankSteeringControlPassThrough()
                && steering.worldPosition.equals(
                passThrough.mechanicalDrive$getTankSteeringControlPassThroughSource()
        )) {
            return 0.0F;
        }

        if (!(neighbourState.getBlock()
                instanceof KineticBlock kineticBlock)) {
            return 0.0F;
        }

        Direction towardSteering =
                side.getOpposite();

        if (!kineticBlock.hasShaftTowards(
                steering.level,
                neighbourPos,
                neighbourState,
                towardSteering
        )) {
            return 0.0F;
        }

        return kinetic.getSpeed();
    }

    private static boolean hasShaftTowards(
            KineticBlockEntity blockEntity,
            Direction direction
    ) {
        BlockState state =
                blockEntity.getBlockState();

        if (!(state.getBlock()
                instanceof KineticBlock kineticBlock)) {
            return false;
        }

        return kineticBlock.hasShaftTowards(
                blockEntity.getLevel(),
                blockEntity.getBlockPos(),
                state,
                direction
        );
    }

    private static boolean isControlSide(
            BlockState state,
            Direction side
    ) {
        Direction facing =
                state.getValue(
                        TankTransmissionSteeringBlock.FACING
                );

        return side == facing
                || side == facing.getOpposite();
    }

    public record ControlPassThrough(
            float speed,
            BlockPos sourceSteeringPos
    ) {
        public static final ControlPassThrough EMPTY =
                new ControlPassThrough(
                        0.0F,
                        null
                );
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) {
            return;
        }

        Direction inputSide =
                getControlInputSide();

        if (inputSide == null) {
            return;
        }

        float rawSpeed =
                getControlSpeedFromSide(inputSide);

        float logicalSpeed =
                getLogicalControlSpeed(inputSide);

        float logicalAngleDelta =
                KineticBlockEntity.convertToAngular(
                        logicalSpeed
                );

        controlInputAngle += logicalAngleDelta;

        float steeringAngleDelta =
                logicalAngleDelta
                        * getSteeringDirectionMultiplier();

        Direction.Axis axis =
                getBlockState().getValue(
                        TankTransmissionSteeringBlock.AXIS
                );

        Direction positiveAxisDirection =
                Direction.get(
                        Direction.AxisDirection.POSITIVE,
                        axis
                );

        float visualSpeed =
                KineticBlockEntity.convertToDirection(
                        rawSpeed,
                        positiveAxisDirection
                );

        float visualAngleDelta =
                KineticBlockEntity.convertToAngular(
                        visualSpeed
                );

        visualShaftAngle += visualAngleDelta;

        float limit =
                getSteeringLimit();

        float newSteeringAngle =
                Mth.clamp(
                        currentSteeringAngle
                                + steeringAngleDelta,
                        -limit,
                        limit
                );

        if (Math.abs(
                newSteeringAngle
                        - currentSteeringAngle
        ) < 0.001F) {
            return;
        }

        currentSteeringAngle =
                newSteeringAngle;

        onSteeringAngleChanged();
    }

    private void onMaxSteeringAngleChanged() {
        if (level == null || level.isClientSide) {
            return;
        }

        float limit =
                getSteeringLimit();

        float newSteeringAngle =
                Mth.clamp(
                        currentSteeringAngle,
                        -limit,
                        limit
                );

        if (Math.abs(
                newSteeringAngle
                        - currentSteeringAngle
        ) < 0.001F) {
            return;
        }

        currentSteeringAngle =
                newSteeringAngle;

        onSteeringAngleChanged();
    }

    private void onSteeringAngleChanged() {
        if (level == null || level.isClientSide) {
            return;
        }

        setChanged();
        sendData();
    }

    @Override
    public float getRotationSpeedModifier(
            Direction direction
    ) {
        return 1.0F;
    }

    public float getVisualSpeedForSide(
            Direction side
    ) {
        return getSpeed();
    }

    @Override
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean isPlayerSneaking
    ) {

        CreateLang.builder()
                .add(
                        Component.translatable(
                                "block.mechanical_drive.tank_transmission_steering"
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
                                "tooltip.mechanical_drive.tank_transmission.steering_angle",
                                Component.literal(
                                        formatSteeringAngle(
                                                currentSteeringAngle
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

    private static String formatSteeringAngle(
            float angle
    ) {
        return String.format(
                Locale.ROOT,
                "%.1f\u00B0",
                angle
        );
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        tag.putFloat(
                CONTROL_INPUT_ANGLE_TAG,
                controlInputAngle
        );

        tag.putFloat(
                STEERING_ANGLE_TAG,
                currentSteeringAngle
        );

        tag.putFloat(
                VISUAL_SHAFT_ANGLE_TAG,
                visualShaftAngle
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

        if (tag.contains(CONTROL_INPUT_ANGLE_TAG)) {
            controlInputAngle =
                    tag.getFloat(
                            CONTROL_INPUT_ANGLE_TAG
                    );
        } else if (tag.contains(STEERING_ANGLE_TAG)) {
            controlInputAngle =
                    tag.getFloat(
                            STEERING_ANGLE_TAG
                    );
        }

        if (tag.contains(STEERING_ANGLE_TAG)) {
            currentSteeringAngle =
                    tag.getFloat(
                            STEERING_ANGLE_TAG
                    );
        } else {
            float limit =
                    getSteeringLimit();

            currentSteeringAngle =
                    Mth.clamp(
                            controlInputAngle,
                            -limit,
                            limit
                    );
        }

        if (tag.contains(VISUAL_SHAFT_ANGLE_TAG)) {
            visualShaftAngle =
                    tag.getFloat(
                            VISUAL_SHAFT_ANGLE_TAG
                    );
        }
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }
}
