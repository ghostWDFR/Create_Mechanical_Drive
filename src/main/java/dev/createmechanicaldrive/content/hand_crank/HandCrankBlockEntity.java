package dev.createmechanicaldrive.content.hand_crank;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class HandCrankBlockEntity
        extends GeneratingKineticBlockEntity {

    private static final String IN_USE_TAG =
            "InUse";

    private static final String BACKWARDS_TAG =
            "Backwards";

    public int inUse =
            0;

    public boolean backwards =
            false;

    private float independentAngle =
            0.0F;

    private float chasingAngularVelocity =
            0.0F;

    public HandCrankBlockEntity(
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

    public void turn(
            boolean backwards
    ) {
        boolean update =
                getGeneratedSpeed() == 0.0F
                        || this.backwards
                        != backwards;

        inUse =
                10;

        this.backwards =
                backwards;

        if (update
                && level != null
                && !level.isClientSide) {
            updateGeneratedRotation();
        }
    }

    @Override
    public float getGeneratedSpeed() {
        if (inUse == 0) {
            return 0.0F;
        }

        return backwards
                ? -HandCrankBlock.ROTATION_SPEED
                : HandCrankBlock.ROTATION_SPEED;
    }

    @Override
    public float calculateAddedStressCapacity() {
        lastCapacityProvided =
                HandCrankBlock.STRESS_CAPACITY;

        return HandCrankBlock.STRESS_CAPACITY;
    }

    public float getIndependentAngle(
            float partialTicks
    ) {
        return independentAngle
                + partialTicks
                * chasingAngularVelocity;
    }

    @Override
    public void tick() {
        super.tick();

        float actualAngularSpeed =
                KineticBlockEntity
                        .convertToAngular(
                                getSpeed()
                        );

        chasingAngularVelocity +=
                (
                        actualAngularSpeed
                                - chasingAngularVelocity
                ) / 4.0F;

        independentAngle +=
                chasingAngularVelocity;

        if (inUse <= 0) {
            return;
        }

        inUse--;

        if (inUse == 0
                && level != null
                && !level.isClientSide) {
            sequenceContext =
                    null;

            updateGeneratedRotation();
        }
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        tag.putInt(
                IN_USE_TAG,
                inUse
        );

        tag.putBoolean(
                BACKWARDS_TAG,
                backwards
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
        inUse =
                tag.getInt(
                        IN_USE_TAG
                );

        backwards =
                tag.getBoolean(
                        BACKWARDS_TAG
                );

        super.read(
                tag,
                registries,
                clientPacket
        );
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void tickAudio() {
        super.tickAudio();

        if (inUse <= 0) {
            return;
        }

        if (AnimationTickHolder.getTicks()
                % 10 != 0) {
            return;
        }

        if (level == null) {
            return;
        }

        AllSoundEvents.CRANKING.playAt(
                level,
                worldPosition,
                inUse / 2.5F,
                0.65F
                        + (10 - inUse)
                        / 10.0F,
                true
        );
    }
}