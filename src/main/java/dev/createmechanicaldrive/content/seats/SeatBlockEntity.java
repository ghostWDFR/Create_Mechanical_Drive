package dev.createmechanicaldrive.content.seats;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SeatBlockEntity
        extends BlockEntity {

    private SeatBackPosition backPosition =
            SeatBackPosition.UPRIGHT;

    private float reclineTarget =
            SeatBackPosition.UPRIGHT.recline();

    private float previousAnimatedRecline =
            reclineTarget;

    private float animatedRecline =
            reclineTarget;

    public SeatBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(
                type,
                pos,
                state
        );

        if (state.hasProperty(SeatBlock.BACK_POSITION)) {
            backPosition =
                    state.getValue(
                            SeatBlock.BACK_POSITION
                    );

            reclineTarget =
                    backPosition.recline();

            previousAnimatedRecline =
                    reclineTarget;

            animatedRecline =
                    reclineTarget;
        }
    }

    public void tickAnimation() {
        BlockState state =
                getBlockState();

        if (state.hasProperty(SeatBlock.BACK_POSITION)) {
            SeatBackPosition statePosition =
                    state.getValue(
                            SeatBlock.BACK_POSITION
                    );

            if (backPosition != statePosition) {
                setBackPosition(
                        statePosition
                );
            }
        }

        previousAnimatedRecline =
                animatedRecline;

        animatedRecline +=
                (
                        reclineTarget
                                - animatedRecline
                )
                        * 0.35F;

        if (
                Math.abs(
                        reclineTarget
                                - animatedRecline
                )
                        < 0.001F
        ) {
            animatedRecline =
                    reclineTarget;
        }
    }

    public SeatBackPosition getBackPosition() {
        return backPosition;
    }

    public void setBackPosition(
            SeatBackPosition backPosition
    ) {
        if (this.backPosition == backPosition) {
            return;
        }

        this.backPosition =
                backPosition;

        reclineTarget =
                backPosition.recline();

        setChanged();
    }

    public void setReclineTarget(
            float reclineTarget
    ) {
        this.reclineTarget =
                Mth.clamp(
                        reclineTarget,
                        -1.0F,
                        1.0F
                );
    }

    public float getAnimatedRecline(
            float partialTicks
    ) {
        return Mth.lerp(
                partialTicks,
                previousAnimatedRecline,
                animatedRecline
        );
    }
}
