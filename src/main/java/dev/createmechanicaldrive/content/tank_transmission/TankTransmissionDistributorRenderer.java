package dev.createmechanicaldrive.content.tank_transmission;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class TankTransmissionDistributorRenderer
        extends KineticBlockEntityRenderer<
        TankTransmissionDistributorBlockEntity
        > {

    public TankTransmissionDistributorRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);
    }

    @Override
    protected void renderSafe(
            TankTransmissionDistributorBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state =
                blockEntity.getBlockState();

        Direction inputSide =
                state.getValue(
                        TankTransmissionDistributorBlock.FACING
                );

        Direction leftSide =
                inputSide.getClockWise();

        Direction rightSide =
                inputSide.getCounterClockWise();

        renderShaftHalf(
                blockEntity,
                inputSide,
                blockEntity.getInputVisualSpeed(),
                poseStack,
                buffer,
                packedLight
        );

        renderShaftHalf(
                blockEntity,
                leftSide,
                blockEntity.getLeftVisualSpeed(),
                poseStack,
                buffer,
                packedLight
        );

        renderShaftHalf(
                blockEntity,
                rightSide,
                blockEntity.getRightVisualSpeed(),
                poseStack,
                buffer,
                packedLight
        );
    }

    private void renderShaftHalf(
            TankTransmissionDistributorBlockEntity blockEntity,
            Direction side,
            float speed,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        Direction.Axis axis =
                side.getAxis();

        BlockState shaftState =
                KineticBlockEntityRenderer.shaft(axis);

        RenderType renderType =
                getRenderType(
                        blockEntity,
                        shaftState
                );

        SuperByteBuffer shaft =
                getRotatedModel(
                        blockEntity,
                        shaftState
                ).reset();

        poseStack.pushPose();

        transformPoseToHalf(
                poseStack,
                axis,
                side
        );

        KineticBlockEntityRenderer.kineticRotationTransform(
                shaft,
                blockEntity,
                axis,
                getAngleForSpeed(
                        blockEntity,
                        axis,
                        speed
                ),
                getShaftLight(
                        blockEntity,
                        axis,
                        packedLight
                )
        ).renderInto(
                poseStack,
                buffer.getBuffer(renderType)
        );

        poseStack.popPose();
    }

    private static void transformPoseToHalf(
            PoseStack poseStack,
            Direction.Axis axis,
            Direction side
    ) {
        float offset =
                side.getAxisDirection()
                        == Direction.AxisDirection.POSITIVE
                        ? 0.25F
                        : -0.25F;

        poseStack.translate(
                offsetForAxis(
                        axis,
                        offset,
                        Direction.Axis.X
                ),
                offsetForAxis(
                        axis,
                        offset,
                        Direction.Axis.Y
                ),
                offsetForAxis(
                        axis,
                        offset,
                        Direction.Axis.Z
                )
        );

        poseStack.translate(
                0.5F,
                0.5F,
                0.5F
        );

        switch (axis) {
            case X ->
                    poseStack.scale(
                            0.5F,
                            1.0F,
                            1.0F
                    );

            case Y ->
                    poseStack.scale(
                            1.0F,
                            0.5F,
                            1.0F
                    );

            case Z ->
                    poseStack.scale(
                            1.0F,
                            1.0F,
                            0.5F
                    );
        }

        poseStack.translate(
                -0.5F,
                -0.5F,
                -0.5F
        );
    }

    private static float offsetForAxis(
            Direction.Axis axis,
            float offset,
            Direction.Axis targetAxis
    ) {
        return axis == targetAxis
                ? offset
                : 0.0F;
    }

    private static float getAngleForSpeed(
            TankTransmissionDistributorBlockEntity blockEntity,
            Direction.Axis axis,
            float speed
    ) {
        Level level =
                blockEntity.getLevel();

        float renderTime =
                level == null
                        ? 0.0F
                        : AnimationTickHolder.getRenderTime(level);

        float offset =
                KineticBlockEntityRenderer
                        .getRotationOffsetForPosition(
                                blockEntity,
                                blockEntity.getBlockPos(),
                                axis
                        );

        return (
                (
                        renderTime
                                * speed
                                * 3.0F
                                / 10.0F
                                + offset
                ) % 360.0F
        ) / 180.0F * Mth.PI;
    }

    private static int getShaftLight(
            TankTransmissionDistributorBlockEntity blockEntity,
            Direction.Axis axis,
            int fallbackLight
    ) {
        Level level =
                blockEntity.getLevel();

        if (level == null) {
            return fallbackLight;
        }

        BlockPos pos =
                blockEntity.getBlockPos();

        Direction positive =
                Direction.get(
                        Direction.AxisDirection.POSITIVE,
                        axis
                );

        Direction negative =
                Direction.get(
                        Direction.AxisDirection.NEGATIVE,
                        axis
                );

        int centerLight =
                LevelRenderer.getLightColor(
                        level,
                        pos
                );

        int positiveLight =
                LevelRenderer.getLightColor(
                        level,
                        pos.relative(positive)
                );

        int negativeLight =
                LevelRenderer.getLightColor(
                        level,
                        pos.relative(negative)
                );

        return maxPackedLight(
                fallbackLight,
                centerLight,
                positiveLight,
                negativeLight
        );
    }

    private static int maxPackedLight(
            int first,
            int... rest
    ) {
        int result = first;

        for (int packedLight : rest) {
            int block =
                    Math.max(
                            result & 0xFFFF,
                            packedLight & 0xFFFF
                    );

            int sky =
                    Math.max(
                            result >>> 16,
                            packedLight >>> 16
                    );

            result =
                    block | sky << 16;
        }

        return result;
    }
}