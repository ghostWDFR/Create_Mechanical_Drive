package dev.createmechanicaldrive.content.dog_clutch;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DogClutchBlockEntity
        extends KineticBlockEntity {

    private static final float DISENGAGED_OFFSET =
            4.0F / 16.0F;

    private static final float ANIMATION_SPEED =
            0.35F;

    private static final String ANIMATION_SEQUENCE_TAG =
            "AnimationSequence";

    private static final String ANIMATION_ENGAGING_TAG =
            "AnimationEngaging";

    private float previousGearOffset;

    private float gearOffset;

    private int animationSequence;

    private boolean animationEngaging;

    public DogClutchBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(
                type,
                pos,
                state
        );

        float initialOffset =
                state.getBlock()
                        instanceof DogClutchEngagedBlock
                        ? 0.0F
                        : DISENGAGED_OFFSET;

        previousGearOffset =
                initialOffset;

        gearOffset =
                initialOffset;
    }

    public void triggerGearAnimation(
            boolean engaging
    ) {
        animationEngaging =
                engaging;

        animationSequence++;

        if (engaging) {
            previousGearOffset =
                    DISENGAGED_OFFSET;

            gearOffset =
                    DISENGAGED_OFFSET;
        } else {
            previousGearOffset =
                    0.0F;

            gearOffset =
                    0.0F;
        }

        sendData();
    }

    @Override
    public void tick() {
        super.tick();

        if (
                level == null
                        || !level.isClientSide
        ) {
            return;
        }

        previousGearOffset =
                gearOffset;

        float targetOffset =
                isEngaged()
                        ? 0.0F
                        : DISENGAGED_OFFSET;

        gearOffset +=
                (
                        targetOffset
                                - gearOffset
                )
                        * ANIMATION_SPEED;

        if (
                Math.abs(
                        targetOffset
                                - gearOffset
                )
                        < 0.001F
        ) {
            gearOffset =
                    targetOffset;
        }
    }

    public boolean isEngaged() {
        return getBlockState()
                .getBlock()
                instanceof DogClutchEngagedBlock;
    }

    public float getGearOffset(
            float partialTicks
    ) {
        return Mth.lerp(
                partialTicks,
                previousGearOffset,
                gearOffset
        );
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        tag.putInt(
                ANIMATION_SEQUENCE_TAG,
                animationSequence
        );

        tag.putBoolean(
                ANIMATION_ENGAGING_TAG,
                animationEngaging
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
        int receivedSequence =
                tag.getInt(
                        ANIMATION_SEQUENCE_TAG
                );

        boolean receivedEngaging =
                tag.getBoolean(
                        ANIMATION_ENGAGING_TAG
                );

        boolean startAnimation =
                clientPacket
                        && receivedSequence
                        != animationSequence;

        animationSequence =
                receivedSequence;

        animationEngaging =
                receivedEngaging;

        super.read(
                tag,
                registries,
                clientPacket
        );

        if (startAnimation) {
            if (animationEngaging) {
                previousGearOffset =
                        DISENGAGED_OFFSET;

                gearOffset =
                        DISENGAGED_OFFSET;
            } else {
                previousGearOffset =
                        0.0F;

                gearOffset =
                        0.0F;
            }
        }
    }
}