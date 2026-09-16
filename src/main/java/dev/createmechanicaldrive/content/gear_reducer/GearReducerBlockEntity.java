package dev.createmechanicaldrive.content.gear_reducer;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GearReducerBlockEntity
        extends SplitShaftBlockEntity {

    private static final float BASE_STRESS_IMPACT =
            2.0F;

    private static final String MULTIPLIER_TAG =
            "Multiplier";

    private List<ScrollValueBehaviour> multiplierBehaviours;

    private boolean syncingMultiplier;

    private int multiplier = 1;

    public GearReducerBlockEntity(
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

        multiplierBehaviours =
                new ArrayList<>();

        for (
                Direction side
                : Direction.values()
        ) {
            ScrollValueBehaviour behaviour =
                    new GearRatioScrollValueBehaviour(
                            Component.translatable(
                                    "mechanical_drive.gear_reducer.multiplier"
                            ),
                            this,
                            new MultiplierValueBoxTransform()
                                    .fromSide(side)
                    );

            behaviour
                    .between(
                            1,
                            4
                    )
                    .withFormatter(
                            value ->
                                    value + "x"
                    )
                    .withCallback(
                            this::onMultiplierChanged
                    );

            behaviour.setValue(
                    multiplier
            );

            multiplierBehaviours.add(
                    behaviour
            );

            behaviours.add(
                    behaviour
            );
        }
    }

    private void onMultiplierChanged(
            int value
    ) {
        int newMultiplier =
                Math.max(
                        1,
                        Math.min(
                                4,
                                value
                        )
                );

        multiplier =
                newMultiplier;

        if (syncingMultiplier) {
            return;
        }

        syncingMultiplier =
                true;

        for (
                ScrollValueBehaviour behaviour
                : multiplierBehaviours
        ) {
            if (
                    behaviour.getValue()
                            != newMultiplier
            ) {
                behaviour.setValue(
                        newMultiplier
                );
            }
        }

        syncingMultiplier =
                false;

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

        updateStressImpactInNetwork();

        setChanged();
        sendData();
    }

    public int getMultiplier() {
        return multiplier;
    }

    public void setPonderMultiplier(
            int value
    ) {
        multiplier =
                Math.max(
                        1,
                        Math.min(
                                4,
                                value
                        )
                );

        if (
                multiplierBehaviours
                        != null
        ) {
            syncingMultiplier =
                    true;

            for (
                    ScrollValueBehaviour behaviour
                    : multiplierBehaviours
            ) {
                behaviour.setValue(
                        multiplier
                );
            }

            syncingMultiplier =
                    false;
        }

        setChanged();
    }

    public Direction getOutputDirection() {
        BlockState state =
                getBlockState();

        if (
                !state.hasProperty(
                        GearReducerBlock.FACING
                )
        ) {
            return Direction.UP;
        }

        return state.getValue(
                GearReducerBlock.FACING
        );
    }

    public Direction getEffectiveSourceFacing() {
        if (hasSource()) {
            return getSourceFacing();
        }

        GearReducerBlockEntity drivingReducer =
                GearReducerKinetics.findDrivingReducer(
                        this
                );

        if (drivingReducer == null) {
            return null;
        }

        BlockPos offset =
                drivingReducer.getBlockPos()
                        .subtract(
                                getBlockPos()
                        );

        return Direction.getNearest(
                offset.getX(),
                offset.getY(),
                offset.getZ()
        );
    }

    public Direction getInputDirection() {
        return getOutputDirection()
                .getOpposite();
    }

    public boolean isReductionMode() {
        if (
                getBlockState().getValue(
                        GearReducerBlock.POWERED
                )
        ) {
            return false;
        }

        if (getMultiplier() <= 1) {
            return false;
        }

        Direction sourceFacing =
                getEffectiveSourceFacing();

        if (sourceFacing == null) {
            return false;
        }

        return sourceFacing
                == getOutputDirection();
    }

    public float getReductionOutputSpeed() {
        if (!isReductionMode()) {
            return 0.0F;
        }

        return getSpeed()
                / getReductionMultiplier();
    }

    public float getReductionMultiplier() {
        if (
                getBlockState().getValue(
                        GearReducerBlock.POWERED
                )
        ) {
            return 1.0F;
        }

        return getMultiplier();
    }

    public float getEffectiveMultiplier(
            float sourceSpeed
    ) {
        if (
                getBlockState().getValue(
                        GearReducerBlock.POWERED
                )
        ) {
            return 1.0F;
        }

        float selectedMultiplier =
                getMultiplier();

        if (sourceSpeed == 0.0F) {
            return selectedMultiplier;
        }

        float maxSpeed =
                AllConfigs.server()
                        .kinetics
                        .maxRotationSpeed
                        .get();

        return Math.min(
                selectedMultiplier,
                maxSpeed
                        / Math.abs(
                        sourceSpeed
                )
        );
    }

    @Override
    public float getRotationSpeedModifier(
            Direction direction
    ) {
        Direction sourceFacing =
                getEffectiveSourceFacing();

        if (sourceFacing == null) {
            return 1.0F;
        }

        Direction input =
                getInputDirection();

        Direction output =
                getOutputDirection();

        float effectiveMultiplier =
                getEffectiveMultiplier(
                        getTheoreticalSpeed()
                );

        if (
                sourceFacing == input
                        && direction == output
        ) {
            return effectiveMultiplier;
        }

        if (
                sourceFacing == output
                        && direction == input
        ) {
            return 1.0F
                    / getReductionMultiplier();
        }

        return 1.0F;
    }

    public float getVisualSpeedForSide(
            Direction side
    ) {
        float speed =
                getSpeed();

        Direction sourceFacing =
                getEffectiveSourceFacing();

        if (sourceFacing == null) {
            return speed;
        }

        Direction input =
                getInputDirection();

        Direction output =
                getOutputDirection();

        float effectiveMultiplier =
                getEffectiveMultiplier(
                        Math.abs(speed)
                );

        if (sourceFacing == input) {
            if (side == input) {
                return speed;
            }

            if (side == output) {
                return speed
                        * effectiveMultiplier;
            }
        }

        if (sourceFacing == output) {
            if (side == output) {
                return speed;
            }

            if (side == input) {
                return speed
                        / getReductionMultiplier();
            }
        }

        return speed;
    }

    @Override
    public float calculateStressApplied() {
        float selectedMultiplier =
                getBlockState().getValue(
                        GearReducerBlock.POWERED
                )
                        ? 1.0F
                        : getMultiplier();

        if (selectedMultiplier == 1.0F) {
            lastStressApplied = 0.0F;
            return 0.0F;
        }

        if (isReductionMode()) {
            float speed =
                    Math.abs(
                            getTheoreticalSpeed()
                    );

            if (speed <= 0.0F) {
                lastStressApplied = 0.0F;
                return 0.0F;
            }

            float impact =
                    32.0F / speed;

            lastStressApplied =
                    impact;

            return impact;
        }

        float impact =
                BASE_STRESS_IMPACT
                        * selectedMultiplier
                        * selectedMultiplier;

        lastStressApplied =
                impact;

        return impact;
    }

    @Override
    public void onSpeedChanged(
            float previousSpeed
    ) {
        super.onSpeedChanged(
                previousSpeed
        );

        updateStressImpactInNetwork();
    }

    private void updateStressImpactInNetwork() {
        if (
                level == null
                        || level.isClientSide
                        || !hasNetwork()
        ) {
            return;
        }

        KineticNetwork kineticNetwork =
                getOrCreateNetwork();

        if (kineticNetwork == null) {
            return;
        }

        kineticNetwork.updateStressFor(
                this,
                calculateStressApplied()
        );

        networkDirty =
                true;
    }

    public void onRedstoneChanged() {
        if (
                level == null
                        || level.isClientSide
        ) {
            return;
        }

        updateStressImpactInNetwork();

        setChanged();
        sendData();
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        tag.putInt(
                MULTIPLIER_TAG,
                multiplier
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

        int receivedMultiplier =
                tag.contains(
                        MULTIPLIER_TAG
                )
                        ? tag.getInt(
                        MULTIPLIER_TAG
                )
                        : 1;

        multiplier =
                Math.max(
                        1,
                        Math.min(
                                4,
                                receivedMultiplier
                        )
                );

        if (
                multiplierBehaviours
                        != null
        ) {
            syncingMultiplier =
                    true;

            for (
                    ScrollValueBehaviour behaviour
                    : multiplierBehaviours
            ) {
                behaviour.setValue(
                        multiplier
                );
            }

            syncingMultiplier =
                    false;
        }
    }

    private static class GearRatioScrollValueBehaviour
            extends ScrollValueBehaviour {

        public GearRatioScrollValueBehaviour(
                Component label,
                GearReducerBlockEntity blockEntity,
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
                    3,
                    1,
                    List.of(
                            Component.translatable(
                                    "mechanical_drive.gear_reducer.multiplier"
                            )
                    ),
                    new ValueSettingsFormatter(
                            settings ->
                                    Component.literal(
                                            (
                                                    settings.value()
                                                            + 1
                                            )
                                                    + "x"
                                    )
                    )
            );
        }

        @Override
        public ValueSettings getValueSettings() {
            return new ValueSettings(
                    0,
                    getValue()
                            - 1
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
                            + 1
            );

            playFeedbackSound(
                    this
            );
        }
    }

    private static class MultiplierValueBoxTransform
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
            if (
                    !state.hasProperty(
                            GearReducerBlock.FACING
                    )
            ) {
                return false;
            }

            Direction facing =
                    state.getValue(
                            GearReducerBlock.FACING
                    );

            return direction.getAxis()
                    != facing.getAxis();
        }

        @Override
        public float getScale() {
            return 0.5F;
        }
    }
}
