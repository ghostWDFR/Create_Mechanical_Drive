package dev.createmechanicaldrive.content.hand_crank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.createmechanicaldrive.CreateMechanicalDriveClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.util.Mth;

public class HandCrankRenderer
        implements BlockEntityRenderer<HandCrankBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public HandCrankRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            HandCrankBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BakedModel crankModel =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(
                                CreateMechanicalDriveClient
                                        .HAND_CRANK_MODEL
                        );

        BakedModel handleModel =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(
                                CreateMechanicalDriveClient
                                        .HAND_CRANK_HANDLE_MODEL
                        );

        Direction.Axis axis =
                blockEntity
                        .getBlockState()
                        .getValue(
                                HandCrankBlock.AXIS
                        );

        float angle =
                blockEntity.getIndependentAngle(
                        partialTick
                );

        float visualAngle =
                axis == Direction.Axis.Y
                        ? -angle
                        : angle;

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

        applyAxisRotation(
                poseStack,
                axis
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        visualAngle
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
                        crankModel,
                        1.0F,
                        1.0F,
                        1.0F,
                        packedLight,
                        packedOverlay,
                        ModelData.EMPTY,
                        renderType
                );

        poseStack.popPose();

        float handleRadius =
                6.0F / 16.0F;

        float angleRadians =
                visualAngle
                        * Mth.DEG_TO_RAD;

        float handleX =
                -Mth.sin(
                        angleRadians
                )
                        * handleRadius;

        float handleY =
                (
                        Mth.cos(
                                angleRadians
                        )
                                - 1.0F
                )
                        * handleRadius;

        poseStack.pushPose();

        poseStack.translate(
                0.5F,
                0.5F,
                0.5F
        );

        applyAxisRotation(
                poseStack,
                axis
        );

        poseStack.translate(
                handleX,
                handleY,
                0.0F
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
                        handleModel,
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

    private static void applyAxisRotation(
            PoseStack poseStack,
            Direction.Axis axis
    ) {
        switch (axis) {
            case X ->
                    poseStack.mulPose(
                            Axis.YP.rotationDegrees(
                                    90.0F
                            )
                    );

            case Y ->
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(
                                    90.0F
                            )
                    );

            case Z -> {
            }
        }
    }
}