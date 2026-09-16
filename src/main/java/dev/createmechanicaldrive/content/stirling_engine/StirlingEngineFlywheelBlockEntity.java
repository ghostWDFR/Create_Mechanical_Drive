package dev.createmechanicaldrive.content.stirling_engine;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class StirlingEngineFlywheelBlockEntity
        extends KineticBlockEntity {

    public static final float MINIMUM_RPM =
            64.0F;

    private static final float DRIVE_ACCELERATION =
            0.6F;

    private static final float COAST_DECELERATION =
            0.35F;

    private static final float ACCELERATION_RESPONSE =
            0.03F;

    private static final String FLYWHEEL_SPEED_TAG =
            "FlywheelSpeed";

    private static final String FLYWHEEL_ACCELERATION_TAG =
            "FlywheelAcceleration";

    private float flywheelSpeed;

    private float flywheelAcceleration;

    private float clientAngle;

    private float previousClientAngle;

    private float previousEfficiency =
            -1.0F;

    public StirlingEngineFlywheelBlockEntity(
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
    public void tick() {
        super.tick();

        float targetSpeed =
                getSpeed();

        float difference =
                targetSpeed
                        - flywheelSpeed;

        float desiredAcceleration;

        if (
                Math.abs(difference) < 0.001F
        ) {
            desiredAcceleration =
                    0.0F;
        } else if (
                Math.abs(targetSpeed) < 0.001F
        ) {
            desiredAcceleration =
                    -Math.signum(flywheelSpeed)
                            * COAST_DECELERATION;

            if (
                    Math.signum(flywheelAcceleration)
                            == Math.signum(flywheelSpeed)
            ) {
                flywheelAcceleration =
                        0.0F;
            }
        } else {
            desiredAcceleration =
                    Math.signum(difference)
                            * DRIVE_ACCELERATION;
        }

        flywheelAcceleration =
                Mth.approach(
                        flywheelAcceleration,
                        desiredAcceleration,
                        ACCELERATION_RESPONSE
                );

        float nextSpeed =
                flywheelSpeed
                        + flywheelAcceleration;

        if (
                difference > 0.0F
                        && nextSpeed > targetSpeed
        ) {
            nextSpeed =
                    targetSpeed;

            flywheelAcceleration =
                    0.0F;
        } else if (
                difference < 0.0F
                        && nextSpeed < targetSpeed
        ) {
            nextSpeed =
                    targetSpeed;

            flywheelAcceleration =
                    0.0F;
        }

        flywheelSpeed =
                nextSpeed;

        if (
                Math.abs(targetSpeed) < 0.001F
                        && Math.abs(flywheelSpeed) < 0.01F
        ) {
            flywheelSpeed =
                    0.0F;

            flywheelAcceleration =
                    0.0F;
        }

        if (
                level != null
                        && level.isClientSide
        ) {
            previousClientAngle =
                    clientAngle;

            clientAngle +=
                    KineticBlockEntity
                            .convertToAngular(
                                    flywheelSpeed
                            );

            clientAngle =
                    Mth.wrapDegrees(
                            clientAngle
                    );
        }

        if (
                level == null
                        || level.isClientSide
        ) {
            return;
        }

        float efficiency =
                getEfficiency();

        if (
                Math.abs(
                        efficiency
                                - previousEfficiency
                ) >= 0.01F
        ) {
            previousEfficiency =
                    efficiency;

            refreshFlywheelNetwork();

            setChanged();
            sendData();
        }
    }

    public float getFlywheelAngle(
            float partialTicks
    ) {
        float difference =
                Mth.wrapDegrees(
                        clientAngle
                                - previousClientAngle
                );

        float angleDegrees =
                previousClientAngle
                        + difference
                        * partialTicks;

        return angleDegrees
                * Mth.DEG_TO_RAD;
    }

    public float getEfficiency() {
        float shaftSpeed =
                Math.abs(
                        getSpeed()
                );

        float outerSpeed =
                Math.abs(
                        flywheelSpeed
                );

        if (
                shaftSpeed < MINIMUM_RPM
                        || outerSpeed < MINIMUM_RPM
        ) {
            return 0.0F;
        }

        if (
                shaftSpeed <= MINIMUM_RPM
        ) {
            return outerSpeed >= shaftSpeed
                    ? 1.0F
                    : 0.0F;
        }

        return Mth.clamp(
                (outerSpeed - MINIMUM_RPM)
                        / (shaftSpeed - MINIMUM_RPM),
                0.0F,
                1.0F
        );
    }

    public Direction.Axis getAxis() {
        return getBlockState()
                .getValue(
                        StirlingEngineFlywheelBlock.AXIS
                );
    }

    @Override
    public void remove() {
        KineticNetwork network =
                level != null
                        && !level.isClientSide
                        && hasNetwork()
                        ? getOrCreateNetwork()
                        : null;

        super.remove();

        if (
                network != null
                        && !network.members.isEmpty()
        ) {
            network.updateNetwork();
        }
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied =
                0.0F;

        return 0.0F;
    }

    private void refreshFlywheelNetwork() {
        if (!hasNetwork()) {
            return;
        }

        KineticNetwork network =
                getOrCreateNetwork();

        if (network == null) {
            return;
        }

        network.updateNetwork();
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.write(
                tag,
                registries,
                clientPacket
        );

        tag.putFloat(
                FLYWHEEL_SPEED_TAG,
                flywheelSpeed
        );

        tag.putFloat(
                FLYWHEEL_ACCELERATION_TAG,
                flywheelAcceleration
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

        flywheelSpeed =
                tag.getFloat(
                        FLYWHEEL_SPEED_TAG
                );

        flywheelAcceleration =
                tag.getFloat(
                        FLYWHEEL_ACCELERATION_TAG
                );
    }
}