package dev.createmechanicaldrive.content.dog_clutch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class DogClutchRenderer
        extends KineticBlockEntityRenderer<
        DogClutchBlockEntity
        > {

    public DogClutchRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);
    }

    @Override
    protected void renderSafe(
            DogClutchBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState blockState =
                blockEntity.getBlockState();

        Direction facing =
                blockState.getValue(
                        DogClutchBlock.FACING
                );

        Direction.Axis axis =
                facing.getAxis();

        float angle =
                KineticBlockEntityRenderer
                        .getAngleForBe(
                                blockEntity,
                                blockEntity.getBlockPos(),
                                axis
                        );

        renderShaft(
                blockEntity,
                axis,
                angle,
                poseStack,
                buffer,
                packedLight
        );

        renderGear(
                blockEntity,
                facing,
                axis,
                angle,
                partialTicks,
                poseStack,
                buffer,
                packedLight
        );
    }

    private void renderShaft(
            DogClutchBlockEntity blockEntity,
            Direction.Axis axis,
            float angle,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        BlockState shaftState =
                KineticBlockEntityRenderer
                        .shaft(axis);

        RenderType renderType =
                getRenderType(
                        blockEntity,
                        shaftState
                );

        SuperByteBuffer shaft =
                getRotatedModel(
                        blockEntity,
                        shaftState
                )
                        .reset();

        KineticBlockEntityRenderer
                .kineticRotationTransform(
                        shaft,
                        blockEntity,
                        axis,
                        angle,
                        packedLight
                )
                .renderInto(
                        poseStack,
                        buffer.getBuffer(
                                renderType
                        )
                );
    }

    private void renderGear(
            DogClutchBlockEntity blockEntity,
            Direction facing,
            Direction.Axis axis,
            float angle,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        BlockState cogState =
                AllBlocks.COGWHEEL
                        .getDefaultState()
                        .setValue(
                                BlockStateProperties.AXIS,
                                Direction.Axis.Y
                        );

        RenderType renderType =
                getRenderType(
                        blockEntity,
                        cogState
                );

        SuperByteBuffer cog =
                CachedBuffers
                        .partial(
                                AllPartialModels
                                        .SHAFTLESS_COGWHEEL,
                                cogState
                        )
                        .reset();

        float offset =
                blockEntity.getGearOffset(
                        partialTicks
                );

        Direction shiftDirection =
                facing.getOpposite();

        poseStack.pushPose();

        poseStack.translate(
                shiftDirection.getStepX()
                        * offset,
                shiftDirection.getStepY()
                        * offset,
                shiftDirection.getStepZ()
                        * offset
        );

        poseStack.translate(
                0.5F,
                0.5F,
                0.5F
        );

        if (axis == Direction.Axis.X) {
            poseStack.mulPose(
                    Axis.ZP.rotationDegrees(
                            -90.0F
                    )
            );
        } else if (
                axis == Direction.Axis.Z
        ) {
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            90.0F
                    )
            );
        }

        poseStack.translate(
                -0.5F,
                -0.5F,
                -0.5F
        );

        KineticBlockEntityRenderer
                .kineticRotationTransform(
                        cog,
                        blockEntity,
                        Direction.Axis.Y,
                        angle,
                        packedLight
                )
                .renderInto(
                        poseStack,
                        buffer.getBuffer(
                                renderType
                        )
                );

        poseStack.popPose();
    }
}