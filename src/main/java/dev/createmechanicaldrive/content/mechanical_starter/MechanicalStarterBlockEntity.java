package dev.createmechanicaldrive.content.mechanical_starter;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MechanicalStarterBlockEntity
        extends GeneratingKineticBlockEntity {

    private static final String BUTTON_TICKS_TAG =
            "ButtonTicks";

    private static final String IMPULSE_TICKS_TAG =
            "ImpulseTicks";

    private static final String REVERSED_TAG =
            "Reversed";

    private int buttonTicks;
    private int impulseTicks;
    private boolean reversed;

    public MechanicalStarterBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    public void activate(
            boolean reversed
    ) {
        if (buttonTicks > 0) {
            return;
        }

        buttonTicks =
                MechanicalStarterBlock.IMPULSE_TICKS;

        this.reversed =
                reversed;

        if (level == null
                || level.isClientSide) {
            return;
        }

        if (Math.abs(getSpeed()) > 0.001F
                && impulseTicks <= 0) {
            setChanged();
            sendData();
            return;
        }

        boolean wasGenerating =
                impulseTicks > 0;

        impulseTicks =
                MechanicalStarterBlock.IMPULSE_TICKS;

        if (!wasGenerating) {
            updateGeneratedRotation();
        }

        setChanged();
        sendData();
    }

    @Override
    public float getGeneratedSpeed() {
        if (impulseTicks <= 0) {
            return 0.0F;
        }

        Direction facing =
                getBlockState()
                        .getValue(
                                MechanicalStarterBlock.FACING
                        );

        float clockwiseSign =
                -facing.getAxisDirection()
                        .getStep();

        return MechanicalStarterBlock.ROTATION_SPEED
                * clockwiseSign
                * (reversed ? -1.0F : 1.0F);
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity =
                getGeneratedSpeed() == 0.0F
                        ? 0.0F
                        : MechanicalStarterBlock.STRESS_CAPACITY;

        lastCapacityProvided =
                capacity;

        return capacity;
    }

    public float getButtonPress(
            float partialTicks
    ) {
        return Mth.clamp(
                (
                        buttonTicks
                                - partialTicks
                ) / MechanicalStarterBlock.IMPULSE_TICKS,
                0.0F,
                1.0F
        );
    }

    @Override
    public void tick() {
        super.tick();

        boolean buttonReleased =
                buttonTicks == 1;

        if (buttonTicks > 0) {
            buttonTicks--;
        }

        if (buttonReleased
                && level != null
                && !level.isClientSide) {
            level.playSound(
                    null,
                    worldPosition,
                    net.minecraft.sounds.SoundEvents
                            .STONE_BUTTON_CLICK_OFF,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    0.25F,
                    0.7F
            );
        }

        if (level == null
                || level.isClientSide
                || impulseTicks <= 0) {
            return;
        }

        impulseTicks--;

        if (impulseTicks > 0) {
            return;
        }

        sequenceContext =
                null;

        updateGeneratedRotation();
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
                BUTTON_TICKS_TAG,
                buttonTicks
        );

        tag.putInt(
                IMPULSE_TICKS_TAG,
                impulseTicks
        );

        tag.putBoolean(
                REVERSED_TAG,
                reversed
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
        buttonTicks =
                tag.getInt(
                        BUTTON_TICKS_TAG
                );

        impulseTicks =
                tag.getInt(
                        IMPULSE_TICKS_TAG
                );

        reversed =
                tag.getBoolean(
                        REVERSED_TAG
                );

        super.read(
                tag,
                registries,
                clientPacket
        );
    }
}
