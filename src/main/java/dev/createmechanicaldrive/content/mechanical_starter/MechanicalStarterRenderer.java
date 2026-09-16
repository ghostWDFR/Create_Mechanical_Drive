package dev.createmechanicaldrive.content.mechanical_starter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class MechanicalStarterRenderer
        extends KineticBlockEntityRenderer<MechanicalStarterBlockEntity> {

    private static final ModelResourceLocation BUTTON_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/mechanical_starter/mechanical_starter_button"
                    )
            );

    private final BlockRenderDispatcher blockRenderer;

    public MechanicalStarterRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);

        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    public static ModelResourceLocation buttonModelLocation() {
        return BUTTON_MODEL;
    }

    @Override
    protected void renderSafe(
            MechanicalStarterBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state =
                blockEntity.getBlockState();

        Direction facing =
                state.getValue(
                        MechanicalStarterBlock.FACING
                );

        renderButton(
                blockEntity,
                facing,
                partialTicks,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderShaft(
                blockEntity,
                facing,
                poseStack,
                bufferSource,
                packedLight
        );
    }

    private void renderButton(
            MechanicalStarterBlockEntity blockEntity,
            Direction facing,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BakedModel model =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(BUTTON_MODEL);

        VertexConsumer buffer =
                bufferSource.getBuffer(
                        RenderType.cutout()
                );

        float press =
                blockEntity.getButtonPress(
                        partialTicks
                ) * 2.0F / 16.0F;

        poseStack.pushPose();
        poseStack.translate(
                0.5F,
                0.5F,
                0.5F
        );
        applyModelFacing(
                poseStack,
                facing
        );
        poseStack.translate(
                -0.5F,
                -0.5F - press,
                -0.5F
        );

        blockRenderer.getModelRenderer()
                .renderModel(
                        poseStack.last(),
                        buffer,
                        null,
                        model,
                        1.0F,
                        1.0F,
                        1.0F,
                        packedLight,
                        packedOverlay,
                        ModelData.EMPTY,
                        RenderType.cutout()
                );

        poseStack.popPose();
    }

    private static void applyModelFacing(
            PoseStack poseStack,
            Direction facing
    ) {
        switch (facing) {
            case UP -> {
            }

            case DOWN -> poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            180.0F
                    )
            );

            case NORTH -> poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            -90.0F
                    )
            );

            case SOUTH -> poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            90.0F
                    )
            );

            case WEST -> poseStack.mulPose(
                    Axis.ZP.rotationDegrees(
                            90.0F
                    )
            );

            case EAST -> poseStack.mulPose(
                    Axis.ZP.rotationDegrees(
                            -90.0F
                    )
            );
        }
    }

    private void renderShaft(
            MechanicalStarterBlockEntity blockEntity,
            Direction facing,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Direction shaftDirection =
                facing.getOpposite();

        Direction.Axis axis =
                facing.getAxis();

        BlockState shaftState =
                shaft(axis);

        SuperByteBuffer shaft =
                getRotatedModel(
                        blockEntity,
                        shaftState
                ).reset();

        poseStack.pushPose();
        transformToHalfShaft(
                poseStack,
                axis,
                shaftDirection
        );

        kineticRotationTransform(
                shaft,
                blockEntity,
                axis,
                getAngleForBe(
                        blockEntity,
                        blockEntity.getBlockPos(),
                        axis
                ),
                packedLight
        ).renderInto(
                poseStack,
                bufferSource.getBuffer(
                        getRenderType(
                                blockEntity,
                                shaftState
                        )
                )
        );

        poseStack.popPose();
    }

    private static void transformToHalfShaft(
            PoseStack poseStack,
            Direction.Axis axis,
            Direction direction
    ) {
        float offset =
                direction.getAxisDirection()
                        .getStep() * 0.25F;

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
            case X -> poseStack.scale(
                    0.5F,
                    1.0F,
                    1.0F
            );
            case Y -> poseStack.scale(
                    1.0F,
                    0.5F,
                    1.0F
            );
            case Z -> poseStack.scale(
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
}
