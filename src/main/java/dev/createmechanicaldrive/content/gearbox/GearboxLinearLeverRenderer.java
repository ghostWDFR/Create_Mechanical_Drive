package dev.createmechanicaldrive.content.gearbox;

import dev.createmechanicaldrive.CreateMechanicalDriveClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class GearboxLinearLeverRenderer
        implements BlockEntityRenderer<GearboxLinearLeverBlockEntity> {

    private static final float MAX_TILT_RADIANS =
            (float) Math.toRadians(24.0);

    private final BlockRenderDispatcher blockRenderer;

    public GearboxLinearLeverRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            GearboxLinearLeverBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        float linear = blockEntity.getAnimatedLinear(partialTick);

        Direction facing = blockEntity
                .getBlockState()
                .getValue(GearboxLinearLeverBlock.FACING);

        BakedModel handleModel = Minecraft.getInstance()
                .getModelManager()
                .getModel(
                        CreateMechanicalDriveClient
                                .GEARBOX_LEVER_LINEAR_HANDLE_MODEL
                );

        RenderType renderType = RenderType.cutout();

        VertexConsumer buffer =
                bufferSource.getBuffer(renderType);

        poseStack.pushPose();

        rotateToFacing(poseStack, facing);

        poseStack.translate(
                8.0F / 16.0F,
                2.0F / 16.0F,
                8.0F / 16.0F
        );

        if (facing.getAxis() == Direction.Axis.Z) {linear = -linear;
        }

        float xRotation = -linear * MAX_TILT_RADIANS;

        poseStack.mulPose(
                new Quaternionf().rotationX(xRotation)
        );

        poseStack.translate(
                -8.0F / 16.0F,
                -2.0F / 16.0F,
                -8.0F / 16.0F
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

    private static void rotateToFacing(
            PoseStack poseStack,
            Direction facing
    ) {
        float degrees = switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            case UP, DOWN, NORTH -> 0.0F;
        };

        if (degrees == 0.0F) {
            return;
        }

        poseStack.translate(
                0.5F,
                0.0F,
                0.5F
        );

        poseStack.mulPose(
                new Quaternionf(
                        new AxisAngle4f(
                                (float) Math.toRadians(degrees),
                                0.0F,
                                1.0F,
                                0.0F
                        )
                )
        );

        poseStack.translate(
                -0.5F,
                0.0F,
                -0.5F
        );
    }
}