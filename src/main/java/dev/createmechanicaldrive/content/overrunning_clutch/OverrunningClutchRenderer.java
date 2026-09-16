package dev.createmechanicaldrive.content.overrunning_clutch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.createmechanicaldrive.CreateMechanicalDriveClient;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class OverrunningClutchRenderer
        extends KineticBlockEntityRenderer<
        OverrunningClutchBlockEntity
        > {

    private static final float HALF_SHAFT_SCALE =
            0.46F;

    private static final float HALF_SHAFT_OFFSET =
            0.27F;

    private final BlockRenderDispatcher blockRenderer;

    public OverrunningClutchRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);

        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    protected void renderSafe(
            OverrunningClutchBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state =
                blockEntity.getBlockState();

        Direction.Axis axis =
                state.getValue(
                        OverrunningClutchBlock.AXIS
                );

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

        BlockState shaftState =
                shaft(axis);

        RenderType renderType =
                getRenderType(
                        blockEntity,
                        shaftState
                );

        int light =
                getShaftLight(
                        blockEntity,
                        axis,
                        packedLight
                );

        renderShaftHalf(
                blockEntity,
                shaftState,
                axis,
                positive,
                blockEntity.getVisualSpeedForSide(
                        positive
                ),
                renderType,
                poseStack,
                buffer,
                light
        );

        renderShaftHalf(
                blockEntity,
                shaftState,
                axis,
                negative,
                blockEntity.getVisualSpeedForSide(
                        negative
                ),
                renderType,
                poseStack,
                buffer,
                light
        );

        renderInnerPart(
                CreateMechanicalDriveClient
                        .OVERRUNNING_CLUTCH_INNER_SIDES_MODEL,
                blockEntity,
                axis,
                blockEntity.getVisualSpeedForSide(
                        positive
                ),
                poseStack,
                buffer,
                light,
                packedOverlay
        );

        renderInnerPart(
                CreateMechanicalDriveClient
                        .OVERRUNNING_CLUTCH_INNER_MIDDLE_MODEL,
                blockEntity,
                axis,
                blockEntity.getVisualSpeedForSide(
                        negative
                ),
                poseStack,
                buffer,
                light,
                packedOverlay
        );
    }

    private void renderShaftHalf(
            OverrunningClutchBlockEntity blockEntity,
            BlockState shaftState,
            Direction.Axis axis,
            Direction side,
            float speed,
            RenderType renderType,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light
    ) {
        SuperByteBuffer shaft =
                getRotatedModel(
                        blockEntity,
                        shaftState
                )
                        .reset();

        poseStack.pushPose();

        transformPoseToHalf(
                poseStack,
                axis,
                side
        );

        KineticBlockEntityRenderer
                .kineticRotationTransform(
                        shaft,
                        blockEntity,
                        axis,
                        getAngleForSpeed(
                                blockEntity,
                                axis,
                                speed
                        ),
                        light
                )
                .renderInto(
                        poseStack,
                        buffer.getBuffer(
                                renderType
                        )
                );

        poseStack.popPose();
    }

    private void renderInnerPart(
            ModelResourceLocation modelLocation,
            OverrunningClutchBlockEntity blockEntity,
            Direction.Axis axis,
            float speed,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int packedOverlay
    ) {
        BakedModel model =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(
                                modelLocation
                        );

        RenderType renderType =
                RenderType.cutout();

        VertexConsumer vertexConsumer =
                buffer.getBuffer(
                        renderType
                );

        poseStack.pushPose();

        poseStack.translate(
                0.5F,
                0.5F,
                0.5F
        );

        alignModelAxis(
                poseStack,
                axis
        );

        poseStack.mulPose(
                Axis.YP.rotation(
                        getAngleForSpeed(
                                blockEntity,
                                axis,
                                speed
                        )
                )
        );

        poseStack.translate(
                -0.5F,
                -0.5F,
                -0.5F
        );

        blockRenderer
                .getModelRenderer()
                .renderModel(
                        poseStack.last(),
                        vertexConsumer,
                        null,
                        model,
                        1.0F,
                        1.0F,
                        1.0F,
                        light,
                        packedOverlay,
                        ModelData.EMPTY,
                        renderType
                );

        poseStack.popPose();
    }

    private static void alignModelAxis(
            PoseStack poseStack,
            Direction.Axis axis
    ) {
        switch (axis) {
            case X ->
                    poseStack.mulPose(
                            Axis.ZP.rotationDegrees(
                                    -90.0F
                            )
                    );

            case Z ->
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(
                                    90.0F
                            )
                    );

            case Y -> {
            }
        }
    }

    private static void transformPoseToHalf(
            PoseStack poseStack,
            Direction.Axis axis,
            Direction side
    ) {
        float offset =
                side.getAxisDirection()
                        == Direction.AxisDirection.POSITIVE
                        ? HALF_SHAFT_OFFSET
                        : -HALF_SHAFT_OFFSET;

        poseStack.translate(
                axis == Direction.Axis.X
                        ? offset
                        : 0.0F,
                axis == Direction.Axis.Y
                        ? offset
                        : 0.0F,
                axis == Direction.Axis.Z
                        ? offset
                        : 0.0F
        );

        poseStack.translate(
                0.5F,
                0.5F,
                0.5F
        );

        switch (axis) {
            case X ->
                    poseStack.scale(
                            HALF_SHAFT_SCALE,
                            1.0F,
                            1.0F
                    );

            case Y ->
                    poseStack.scale(
                            1.0F,
                            HALF_SHAFT_SCALE,
                            1.0F
                    );

            case Z ->
                    poseStack.scale(
                            1.0F,
                            1.0F,
                            HALF_SHAFT_SCALE
                    );
        }

        poseStack.translate(
                -0.5F,
                -0.5F,
                -0.5F
        );
    }

    private static float getAngleForSpeed(
            OverrunningClutchBlockEntity blockEntity,
            Direction.Axis axis,
            float speed
    ) {
        Level level =
                blockEntity.getLevel();

        float renderTime =
                level == null
                        ? 0.0F
                        : AnimationTickHolder
                        .getRenderTime(level);

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
                )
                        % 360.0F
        )
                / 180.0F
                * Mth.PI;
    }

    private static int getShaftLight(
            OverrunningClutchBlockEntity blockEntity,
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
        int result =
                first;

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
                    block
                            | sky << 16;
        }

        return result;
    }
}
