package dev.createmechanicaldrive.content.stirling_engine;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class StirlingEngineOutputBlockEntity extends BlockEntity {
    private float previousSoundAngle;

    public StirlingEngineOutputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick() {
        if (level != null && level.isClientSide) {
            tickSound();
            return;
        }

        refreshPoweredShaft();
    }

    void refreshPoweredShaft() {
        if (level == null || level.isClientSide) {
            return;
        }

        StirlingEngineOutputBlock.updatePoweredShaft(
                level,
                worldPosition,
                getBlockState()
        );
    }

    private void tickSound() {
        if (level == null) {
            return;
        }

        StirlingEnginePoweredShaftBlockEntity shaft = getShaft();
        if (shaft == null || Math.abs(shaft.getSpeed()) < 0.001F) {
            return;
        }

        Direction.Axis shaftAxis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);
        float angle = -KineticBlockEntityRenderer.getAngleForBe(
                shaft,
                shaft.getBlockPos(),
                shaftAxis
        );
        float soundAngle = AngleHelper.deg(angle)
                + (angle < 0.0F ? -105.0F : 285.0F);
        soundAngle %= 360.0F;

        if (!crossedPressureStroke(soundAngle)) {
            previousSoundAngle = soundAngle;
            return;
        }

        Direction facing = StirlingEngineOutputBlock.getConnectedDirection(getBlockState());
        Vec3 soundPos = Vec3.atCenterOf(worldPosition)
                .add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.5D));
        float volume = 0.1F;
        float pitch = 0.75F + level.random.nextFloat() * 0.1F;

        AllSoundEvents.STEAM.playAt(
                level,
                soundPos,
                volume,
                pitch,
                false
        );
        previousSoundAngle = soundAngle;
    }

    @Nullable
    public Float getTargetAngle() {
        StirlingEnginePoweredShaftBlockEntity shaft = getShaft();
        if (shaft == null) {
            return null;
        }

        BlockState state = getBlockState();
        Direction facing = StirlingEngineOutputBlock.getConnectedDirection(state);
        Direction.Axis facingAxis = facing.getAxis();
        Direction.Axis shaftAxis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);

        if (shaftAxis == facingAxis) {
            return null;
        }

        float angle = KineticBlockEntityRenderer.getAngleForBe(
                shaft,
                shaft.getBlockPos(),
                shaftAxis
        );

        if (shaftAxis.isHorizontal()
                && (facingAxis == Direction.Axis.X
                ^ facing.getAxisDirection() == AxisDirection.POSITIVE)) {
            angle *= -1.0F;
        }

        return angle;
    }

    public boolean shouldRenderConnector() {
        if (level == null) {
            return false;
        }

        StirlingEnginePoweredShaftBlockEntity shaft = getShaft();
        if (shaft == null) {
            return false;
        }

        Direction facing = StirlingEngineOutputBlock.getConnectedDirection(getBlockState());
        BlockPos oppositePos = worldPosition.relative(facing, 4);

        if (!(level.getBlockEntity(oppositePos) instanceof StirlingEngineOutputBlockEntity opposite)) {
            return true;
        }

        StirlingEnginePoweredShaftBlockEntity oppositeShaft = opposite.getShaft();

        if (oppositeShaft != shaft) {
            return true;
        }

        return facing.getAxisDirection() == AxisDirection.POSITIVE;
    }

    private boolean crossedPressureStroke(float soundAngle) {
        if (soundAngle >= 0.0F) {
            return previousSoundAngle > 180.0F && soundAngle < 180.0F;
        }

        return previousSoundAngle < -180.0F && soundAngle > -180.0F;
    }

    @Nullable
    public StirlingEnginePoweredShaftBlockEntity getShaft() {
        if (level == null) {
            return null;
        }

        BlockState state = getBlockState();
        BlockPos shaftPos = StirlingEngineOutputBlock.getShaftPos(
                state,
                worldPosition
        );

        if (!(level.getBlockEntity(shaftPos) instanceof StirlingEnginePoweredShaftBlockEntity shaft)) {
            return null;
        }

        if (!StirlingEngineOutputBlock.isShaftValid(
                state,
                shaft.getBlockState()
        )) {
            return null;
        }

        return shaft;
    }
}

