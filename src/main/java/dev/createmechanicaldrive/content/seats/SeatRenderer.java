package dev.createmechanicaldrive.content.seats;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.createmechanicaldrive.CreateMechanicalDriveClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class SeatRenderer
        implements BlockEntityRenderer<SeatBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public SeatRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            SeatBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Direction facing =
                blockEntity
                        .getBlockState()
                        .getValue(
                                SeatBlock.FACING
                        );

        float recline =
                blockEntity.getAnimatedRecline(
                        partialTick
                );

        SeatColor color =
                blockEntity
                        .getBlockState()
                        .getValue(
                                SeatBlock.COLOR
                        );

        BakedModel rearModel =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(
                                rearModel(
                                        color
                                )
                        );

        RenderType renderType =
                RenderType.cutout();

        VertexConsumer buffer =
                new AmbientOcclusionVertexConsumer(
                        bufferSource.getBuffer(
                                renderType
                        )
                );

        poseStack.pushPose();

        rotateToFacing(
                poseStack,
                facing
        );

        poseStack.translate(
                8.0F / 16.0F,
                2.0F / 16.0F,
                10.0F / 16.0F
        );

        poseStack.mulPose(
                new Quaternionf()
                        .rotationX(
                                (float) Math.toRadians(
                                        SeatBackPosition
                                                .angleDegrees(
                                                        recline
                                                )
                                )
                        )
        );

        poseStack.translate(
                -8.0F / 16.0F,
                -2.0F / 16.0F,
                -10.0F / 16.0F
        );

        blockRenderer
                .getModelRenderer()
                .renderModel(
                        poseStack.last(),
                        buffer,
                        null,
                        rearModel,
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

    private static ModelResourceLocation rearModel(
            SeatColor color
    ) {
        return switch (color) {
            case BLACK ->
                    CreateMechanicalDriveClient.SEAT_REAR_MODEL;
            case BLUE ->
                    CreateMechanicalDriveClient.SEAT_REAR_BLUE_MODEL;
            case GRAY ->
                    CreateMechanicalDriveClient.SEAT_REAR_GRAY_MODEL;
            case GREEN ->
                    CreateMechanicalDriveClient.SEAT_REAR_GREEN_MODEL;
            case RED ->
                    CreateMechanicalDriveClient.SEAT_REAR_RED_MODEL;
            case YELLOW ->
                    CreateMechanicalDriveClient.SEAT_REAR_YELLOW_MODEL;
        };
    }

    private static void rotateToFacing(
            PoseStack poseStack,
            Direction facing
    ) {
        float degrees =
                switch (facing) {
                    case WEST -> 90.0F;
                    case NORTH -> 180.0F;
                    case EAST -> 270.0F;
                    case UP, DOWN, SOUTH -> 0.0F;
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
                                (float) Math.toRadians(
                                        degrees
                                ),
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

    private static class AmbientOcclusionVertexConsumer
            extends VertexConsumerWrapper {
        private static final float SHADE_SCALE =
                0.78F;

        private AmbientOcclusionVertexConsumer(
                VertexConsumer parent
        ) {
            super(parent);
        }

        @Override
        public void addVertex(
                float x,
                float y,
                float z,
                int color,
                float u,
                float v,
                int packedOverlay,
                int packedLight,
                float normalX,
                float normalY,
                float normalZ
        ) {
            parent.addVertex(
                    x,
                    y,
                    z,
                    shadeColor(
                            color,
                            normalX,
                            normalY,
                            normalZ
                    ),
                    u,
                    v,
                    packedOverlay,
                    packedLight,
                    normalX,
                    normalY,
                    normalZ
            );
        }

        private static int shadeColor(
                int color,
                float normalX,
                float normalY,
                float normalZ
        ) {
            float shade =
                    faceShade(
                            normalX,
                            normalY,
                            normalZ
                    );

            return FastColor.ARGB32.color(
                    FastColor.ARGB32.alpha(
                            color
                    ),
                    shadeChannel(
                            FastColor.ARGB32.red(
                                    color
                            ),
                            shade
                    ),
                    shadeChannel(
                            FastColor.ARGB32.green(
                                    color
                            ),
                            shade
                    ),
                    shadeChannel(
                            FastColor.ARGB32.blue(
                                    color
                            ),
                            shade
                    )
            );
        }

        private static int shadeChannel(
                int value,
                float shade
        ) {
            return Mth.clamp(
                    Math.round(
                            value
                                    * shade
                    ),
                    0,
                    255
            );
        }

        private static float faceShade(
                float normalX,
                float normalY,
                float normalZ
        ) {
            float length =
                    Mth.sqrt(
                            normalX
                                    * normalX
                                    + normalY
                                    * normalY
                                    + normalZ
                                    * normalZ
            );

            if (length < 1.0E-5F) {
                return 0.62F;
            }

            float x =
                    normalX / length;

            float y =
                    normalY / length;

            float z =
                    normalZ / length;

            float verticalShade =
                    y >= 0.0F
                            ? y * 1.0F
                            : -y * 0.5F;

            float sideShade =
                    Math.abs(
                            x
                    )
                            * 0.6F
                            + Math.abs(
                            z
                    )
                            * 0.8F;

            return Mth.clamp(
                    (
                            verticalShade
                                    + sideShade
                    )
                            * SHADE_SCALE,
                    0.38F,
                    0.82F
            );
        }
    }
}
