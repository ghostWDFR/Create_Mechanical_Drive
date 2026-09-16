package dev.createmechanicaldrive.content.stirling_engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class StirlingEngineFlywheelRenderer
        extends KineticBlockEntityRenderer<
        StirlingEngineFlywheelBlockEntity
        > {

    private static final PartialModel INNER =
            PartialModel.of(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/stirling_engine/stirling_engine_flywheel_inner"
                    )
            );

    private static final PartialModel OUTER =
            PartialModel.of(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/stirling_engine/stirling_engine_flywheel_outer"
                    )
            );

    public StirlingEngineFlywheelRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);
    }

    @Override
    protected void renderSafe(
            StirlingEngineFlywheelBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        Direction.Axis axis =
                blockEntity
                        .getBlockState()
                        .getValue(
                                StirlingEngineFlywheelBlock.AXIS
                        );

        float innerAngle =
                KineticBlockEntityRenderer
                        .getAngleForBe(
                                blockEntity,
                                blockEntity.getBlockPos(),
                                axis
                        );

        float outerAngle =
                blockEntity
                        .getFlywheelAngle(
                                partialTicks
                        );

        renderPart(
                INNER,
                blockEntity,
                axis,
                innerAngle,
                poseStack,
                buffer,
                packedLight
        );

        renderPart(
                OUTER,
                blockEntity,
                axis,
                outerAngle,
                poseStack,
                buffer,
                packedLight
        );
    }

    private static void renderPart(
            PartialModel model,
            StirlingEngineFlywheelBlockEntity blockEntity,
            Direction.Axis axis,
            float angle,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        SuperByteBuffer rendered =
                CachedBuffers
                        .partial(
                                model,
                                blockEntity.getBlockState()
                        )
                        .reset();

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
                        angle
                )
        );

        poseStack.translate(
                -0.5F,
                -0.5F,
                -0.5F
        );

        rendered
                .light(
                        packedLight
                )
                .renderInto(
                        poseStack,
                        buffer.getBuffer(
                                RenderType.cutout()
                        )
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
}