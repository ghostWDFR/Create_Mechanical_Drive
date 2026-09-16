package dev.createmechanicaldrive.content.steering_wheel;

import dev.createmechanicaldrive.CreateMechanicalDriveClient;
import dev.createmechanicaldrive.client.SteeringWheelClientInput;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.data.ModelData;

public class SteeringWheelRenderer
        implements BlockEntityRenderer<SteeringWheelBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public SteeringWheelRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            SteeringWheelBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Direction facing =
                blockEntity.getBlockState()
                        .getValue(
                                SteeringWheelBlock.FACING
                        );

        float angle;

        if (SteeringWheelClientInput.isDragging(
                blockEntity.getBlockPos()
        )) {
            angle =
                    SteeringWheelClientInput
                            .getDraggingAngle();
        } else {
            angle =
                    blockEntity
                            .getRenderAngle(
                                    partialTick
                            );
        }

        angle =
                Mth.wrapDegrees(
                        angle
                );

        SteeringWheelColor color =
                blockEntity.getBlockState()
                        .getValue(
                                SteeringWheelBlock.COLOR
                        );

        ModelResourceLocation modelLocation =
                switch (color) {
                    case BLACK ->
                            CreateMechanicalDriveClient
                                    .STEERING_WHEEL_MODEL;

                    case BLUE ->
                            CreateMechanicalDriveClient
                                    .STEERING_WHEEL_BLUE_MODEL;

                    case GRAY ->
                            CreateMechanicalDriveClient
                                    .STEERING_WHEEL_GRAY_MODEL;

                    case GREEN ->
                            CreateMechanicalDriveClient
                                    .STEERING_WHEEL_GREEN_MODEL;

                    case RED ->
                            CreateMechanicalDriveClient
                                    .STEERING_WHEEL_RED_MODEL;

                    case YELLOW ->
                            CreateMechanicalDriveClient
                                    .STEERING_WHEEL_YELLOW_MODEL;
                };

        BakedModel wheelModel =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(
                                modelLocation
                        );

        RenderType renderType =
                RenderType.cutout();

        VertexConsumer buffer =
                bufferSource.getBuffer(
                        renderType
                );

        poseStack.pushPose();

        poseStack.translate(
                0.5F,
                0.5F,
                0.5F
        );

        rotateToFacing(
                poseStack,
                facing
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        angle
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
                        buffer,
                        null,
                        wheelModel,
                        1.0F,
                        1.0F,
                        1.0F,
                        packedLight,
                        packedOverlay,
                        ModelData.EMPTY,
                        renderType
                );

        poseStack.popPose();
    }

    private static void rotateToFacing(
            PoseStack poseStack,
            Direction facing
    ) {
        switch (facing) {
            case NORTH -> {
            }

            case EAST ->
                    poseStack.mulPose(
                            Axis.YP.rotationDegrees(
                                    -90.0F
                            )
                    );

            case SOUTH ->
                    poseStack.mulPose(
                            Axis.YP.rotationDegrees(
                                    180.0F
                            )
                    );

            case WEST ->
                    poseStack.mulPose(
                            Axis.YP.rotationDegrees(
                                    90.0F
                            )
                    );

            case UP ->
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(
                                    90.0F
                            )
                    );

            case DOWN ->
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(
                                    -90.0F
                            )
                    );
        }
    }
}