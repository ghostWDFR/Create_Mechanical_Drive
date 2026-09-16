package dev.createmechanicaldrive.content.steering_wheel;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.createmechanicaldrive.ControlDistance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;


import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SteeringWheelBlockEntity
        extends GeneratingKineticBlockEntity {

    private static final float MIN_GENERATED_RPM =
            2.0F;

    private static final float MAX_GENERATED_RPM =
            32.0F;

    private static final float RPM_PER_DEGREE_PER_TICK =
            10.0F / 3.0F;

    private static final float EPSILON =
            0.001F;

    private static final String CURRENT_ANGLE_TAG =
            "CurrentAngle";

    private static final String TARGET_ANGLE_TAG =
            "TargetAngle";

    private static final String GENERATED_SPEED_TAG =
            "GeneratedSpeed";

    private static final String HELD_TAG =
            "Held";

    private static final String CONTROLLING_PLAYER_TAG =
            "ControllingPlayer";

    private float currentAngle =
            0.0F;

    private float previousAngle =
            0.0F;

    private float targetAngle =
            0.0F;

    private float previousTargetAngle =
            0.0F;

    private float generatedSpeed =
            0.0F;

    private boolean held =
            false;

    @Nullable
    private UUID controllingPlayerId;

    public SteeringWheelBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(
                type,
                pos,
                state
        );

        effects =
                new NoParticleKineticEffectHandler(
                        this
                );
    }

    public void setTargetAngle(
            float targetAngle
    ) {
        if (!Float.isFinite(targetAngle)) {
            return;
        }

        this.targetAngle =
                targetAngle;

        setChanged();
        sendData();
    }

    public void setHeld(
            boolean held,
            @Nullable UUID controllingPlayerId
    ) {
        UUID newControllingPlayerId =
                held
                        ? controllingPlayerId
                        : null;

        if (this.held == held
                && Objects.equals(
                this.controllingPlayerId,
                newControllingPlayerId
        )) {
            return;
        }

        this.held =
                held;

        this.controllingPlayerId =
                newControllingPlayerId;

        if (held) {
            previousTargetAngle =
                    targetAngle;
        }

        if (!held
                && generatedSpeed != 0.0F) {
            generatedSpeed =
                    0.0F;

            updateGeneratedRotation();
        }

        setChanged();
        sendData();
    }

    public boolean isControlledBy(
            UUID playerId
    ) {
        return held
                && Objects.equals(
                controllingPlayerId,
                playerId
        );
    }

    public boolean isControlledByAnother(
            UUID playerId
    ) {
        return held
                && !Objects.equals(
                controllingPlayerId,
                playerId
        );
    }

    private boolean isControllerTooFar() {
        if (level == null
                || level.getServer() == null
                || controllingPlayerId == null) {
            return true;
        }

        ServerPlayer player =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(
                                controllingPlayerId
                        );

        return player == null
                || ControlDistance.isTooFar(
                player,
                worldPosition
        );
    }

    public float getTargetAngle() {
        return targetAngle;
    }

    public float getRenderAngle(
            float partialTick
    ) {
        if (hasSource()
                && Math.abs(getSpeed()) > EPSILON) {
            return Mth.rotLerp(
                    partialTick,
                    previousAngle,
                    currentAngle
            );
        }

        return Mth.wrapDegrees(
                targetAngle
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null) {
            return;
        }

        previousAngle =
                currentAngle;

        if (!level.isClientSide
                && held
                && isControllerTooFar()) {
            setHeld(
                    false,
                    null
            );
        }

        if (hasSource()
                && Math.abs(getSpeed()) > EPSILON) {
            followExternalRotation();

            if (!level.isClientSide
                    && generatedSpeed != 0.0F) {
                generatedSpeed =
                        0.0F;

                updateGeneratedRotation();

                setChanged();
                sendData();
            }

            return;
        }

        if (level.isClientSide) {
            return;
        }

        if (!held) {
            previousTargetAngle =
                    targetAngle;

            if (generatedSpeed != 0.0F) {
                generatedSpeed =
                        0.0F;

                updateGeneratedRotation();

                setChanged();
                sendData();
            }

            return;
        }

        float targetDelta =
                targetAngle
                        - previousTargetAngle;

        previousTargetAngle =
                targetAngle;

        if (Math.abs(targetDelta)
                <= EPSILON) {
            currentAngle =
                    targetAngle;

            if (generatedSpeed != 0.0F) {
                generatedSpeed =
                        0.0F;

                updateGeneratedRotation();

                setChanged();
                sendData();
            }

            return;
        }

        float rpm =
                Math.abs(targetDelta)
                        * RPM_PER_DEGREE_PER_TICK;

        rpm =
                Mth.clamp(
                        rpm,
                        MIN_GENERATED_RPM,
                        MAX_GENERATED_RPM
                );

        float logicalSpeed =
                rpm
                        * Math.signum(
                        targetDelta
                );

        Direction shaftDirection =
                getBlockState()
                        .getValue(
                                SteeringWheelBlock.FACING
                        )
                        .getOpposite();

        float newGeneratedSpeed =
                KineticBlockEntity
                        .convertToDirection(
                                logicalSpeed,
                                shaftDirection
                        );

        if (newGeneratedSpeed
                != generatedSpeed) {
            generatedSpeed =
                    newGeneratedSpeed;

            updateGeneratedRotation();

            setChanged();
            sendData();
        }

        currentAngle =
                targetAngle;
    }

    private void followExternalRotation() {
        Direction shaftDirection =
                getBlockState()
                        .getValue(
                                SteeringWheelBlock.FACING
                        )
                        .getOpposite();

        float logicalSpeed =
                KineticBlockEntity
                        .convertToDirection(
                                getSpeed(),
                                shaftDirection
                        );

        float angleDelta =
                KineticBlockEntity
                        .convertToAngular(
                                logicalSpeed
                        );

        currentAngle +=
                angleDelta;

        targetAngle =
                currentAngle;
    }

    @Override
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        return false;
    }

    @Override
    public float calculateAddedStressCapacity() {
        lastCapacityProvided =
                4.0F;

        return 4.0F;
    }

    @Override
    public float getGeneratedSpeed() {
        return generatedSpeed;
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
                CURRENT_ANGLE_TAG,
                currentAngle
        );

        tag.putFloat(
                TARGET_ANGLE_TAG,
                targetAngle
        );

        tag.putFloat(
                GENERATED_SPEED_TAG,
                generatedSpeed
        );

        if (clientPacket) {
            tag.putBoolean(
                    HELD_TAG,
                    held
            );

            if (controllingPlayerId != null) {
                tag.putUUID(
                        CONTROLLING_PLAYER_TAG,
                        controllingPlayerId
                );
            }
        }
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

        currentAngle =
                tag.getFloat(
                        CURRENT_ANGLE_TAG
                );

        targetAngle =
                tag.getFloat(
                        TARGET_ANGLE_TAG
                );

        generatedSpeed =
                tag.getFloat(
                        GENERATED_SPEED_TAG
                );

        if (clientPacket) {
            held = tag.getBoolean(
                    HELD_TAG
            );

            controllingPlayerId =
                    tag.hasUUID(
                            CONTROLLING_PLAYER_TAG
                    )
                            ? tag.getUUID(
                            CONTROLLING_PLAYER_TAG
                    )
                            : null;
        }
    }
}
