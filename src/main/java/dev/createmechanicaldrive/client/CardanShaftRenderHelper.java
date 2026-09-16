package dev.createmechanicaldrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.createmechanicaldrive.CreateMechanicalDriveClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CardanShaftRenderHelper {
    private static final double SHAFT_END_INSET =
            1.0D / 16.0D;

    private static final double SHAFT_EDGE_CENTER_OFFSET =
            2.0D / 16.0D;

    private static final double SHAFT_EDGE_LENGTH =
            4.0D / 16.0D;

    private static final double CONNECTOR_BEND_OFFSET =
            2.0D / 16.0D;

    private CardanShaftRenderHelper() {
    }

    public static void renderRodBetween(
            BlockRenderDispatcher blockRenderer,
            Vec3 start,
            Vec3 end,
            Vec3 startJointDirection,
            Vec3 endJointDirection,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float rotation
    ) {
        renderRodBetween(
                blockRenderer,
                start,
                end,
                startJointDirection,
                endJointDirection,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                rotation,
                1.0F,
                1.0F,
                1.0F
        );
    }

    public static void renderRodBetween(
            BlockRenderDispatcher blockRenderer,
            Vec3 start,
            Vec3 end,
            Vec3 startJointDirection,
            Vec3 endJointDirection,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float rotation,
            float red,
            float green,
            float blue
    ) {
        Vec3 delta =
                end.subtract(start);

        double length =
                delta.length();

        if (length < 0.001D) {
            return;
        }

        BakedModel shaftModel =
                getModel(
                        CreateMechanicalDriveClient
                                .CARDAN_SHAFT_MODEL
                );
        BakedModel centerModel =
                getModel(
                        CreateMechanicalDriveClient
                                .CARDAN_SHAFT_CENTER_MODEL
                );
        BakedModel edgeModel =
                getModel(
                        CreateMechanicalDriveClient
                                .CARDAN_SHAFT_EDGE_MODEL
                );
        BakedModel connectorModel =
                getModel(
                        CreateMechanicalDriveClient
                                .CARDAN_JOINT_CONNECTOR_MODEL
                );

        Vec3 shaftDirection =
                delta.normalize();
        double shaftInset =
                Math.min(
                        SHAFT_END_INSET,
                        length * 0.5D
                );
        double shaftLength =
                length - shaftInset * 2.0D;
        double edgeInset =
                Math.min(
                        shaftInset
                                + SHAFT_EDGE_CENTER_OFFSET,
                        length * 0.5D
                );

        RenderType renderType =
                RenderType.cutout();

        poseStack.pushPose();
        poseStack.translate(
                start.x,
                start.y,
                start.z
        );
        rotateToDirection(
                poseStack,
                shaftDirection,
                rotation
        );

        poseStack.pushPose();
        poseStack.translate(
                0.0D,
                shaftInset,
                0.0D
        );
        poseStack.scale(
                1.0F,
                (float) shaftLength,
                1.0F
        );
        poseStack.translate(
                -0.5F,
                0.0F,
                -0.5F
        );
        renderModel(
                blockRenderer,
                shaftModel,
                poseStack,
                buffer,
                renderType,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
        poseStack.popPose();

        renderUnscaledPart(
                blockRenderer,
                centerModel,
                length * 0.5D,
                poseStack,
                buffer,
                renderType,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );

        renderShaftEdge(
                blockRenderer,
                edgeModel,
                edgeInset,
                false,
                poseStack,
                buffer,
                renderType,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
        renderShaftEdge(
                blockRenderer,
                edgeModel,
                length
                        - edgeInset
                        - SHAFT_EDGE_LENGTH,
                true,
                poseStack,
                buffer,
                renderType,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
        poseStack.popPose();

        renderOrientedPart(
                blockRenderer,
                connectorModel,
                offsetTowardBend(
                        start,
                        startJointDirection,
                        shaftDirection
                ),
                averageDirection(
                        shaftDirection,
                        startJointDirection
                ),
                rotation,
                poseStack,
                buffer,
                renderType,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
        renderOrientedPart(
                blockRenderer,
                connectorModel,
                offsetTowardBend(
                        end,
                        endJointDirection,
                        shaftDirection.scale(-1.0D)
                ),
                averageDirection(
                        shaftDirection,
                        endJointDirection.scale(-1.0D)
                ),
                rotation,
                poseStack,
                buffer,
                renderType,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
    }

    private static BakedModel getModel(
            ModelResourceLocation location
    ) {
        return Minecraft.getInstance()
                .getModelManager()
                .getModel(location);
    }

    private static void renderUnscaledPart(
            BlockRenderDispatcher blockRenderer,
            BakedModel model,
            double offset,
            PoseStack poseStack,
            MultiBufferSource buffer,
            RenderType renderType,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue
    ) {
        poseStack.pushPose();
        poseStack.translate(
                -0.5D,
                offset - 0.5D,
                -0.5D
        );
        renderModel(
                blockRenderer,
                model,
                poseStack,
                buffer,
                renderType,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
        poseStack.popPose();
    }

    private static void renderOrientedPart(
            BlockRenderDispatcher blockRenderer,
            BakedModel model,
            Vec3 position,
            Vec3 direction,
            float rotation,
            PoseStack poseStack,
            MultiBufferSource buffer,
            RenderType renderType,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue
    ) {
        poseStack.pushPose();
        poseStack.translate(
                position.x,
                position.y,
                position.z
        );
        rotateToDirection(
                poseStack,
                direction,
                rotation
        );
        poseStack.translate(
                -0.5D,
                -0.5D,
                -0.5D
        );
        renderModel(
                blockRenderer,
                model,
                poseStack,
                buffer,
                renderType,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
        poseStack.popPose();
    }

    private static void renderShaftEdge(
            BlockRenderDispatcher blockRenderer,
            BakedModel model,
            double offset,
            boolean flipped,
            PoseStack poseStack,
            MultiBufferSource buffer,
            RenderType renderType,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue
    ) {
        poseStack.pushPose();

        if (flipped) {
            poseStack.translate(
                    0.0D,
                    offset
                            + SHAFT_EDGE_LENGTH,
                    0.0D
            );
            poseStack.mulPose(
                    new Quaternionf()
                            .rotateX(
                                    (float) Math.PI
                            )
            );
            poseStack.translate(
                    -0.5D,
                    0.0D,
                    -0.5D
            );
        } else {
            poseStack.translate(
                    -0.5D,
                    offset,
                    -0.5D
            );
        }

        renderModel(
                blockRenderer,
                model,
                poseStack,
                buffer,
                renderType,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
        poseStack.popPose();
    }

    private static Vec3 averageDirection(
            Vec3 shaftDirection,
            Vec3 jointDirection
    ) {
        if (jointDirection.lengthSqr() < 1.0E-8D) {
            return shaftDirection;
        }

        Vec3 average =
                shaftDirection.add(
                        jointDirection.normalize()
                );

        return average.lengthSqr() < 1.0E-8D
                ? shaftDirection
                : average.normalize();
    }

    private static Vec3 offsetTowardBend(
            Vec3 position,
            Vec3 jointDirection,
            Vec3 shaftDirection
    ) {
        if (jointDirection.lengthSqr() < 1.0E-8D
                || shaftDirection.lengthSqr() < 1.0E-8D) {
            return position;
        }

        Vec3 jointAxis =
                jointDirection.normalize();
        Vec3 shaftAxis =
                shaftDirection.normalize();
        Vec3 bendDirection =
                shaftAxis.subtract(
                        jointAxis.scale(
                                shaftAxis.dot(jointAxis)
                        )
                );

        return position.add(
                bendDirection.scale(
                        CONNECTOR_BEND_OFFSET
                )
        );
    }

    private static void rotateToDirection(
            PoseStack poseStack,
            Vec3 direction,
            float rotation
    ) {
        poseStack.mulPose(
                new Quaternionf()
                        .rotationTo(
                                new Vector3f(
                                        0.0F,
                                        1.0F,
                                        0.0F
                                ),
                                direction.toVector3f()
                        )
        );
        poseStack.mulPose(
                new Quaternionf()
                        .rotateY(rotation)
        );
    }

    private static void renderModel(
            BlockRenderDispatcher blockRenderer,
            BakedModel model,
            PoseStack poseStack,
            MultiBufferSource buffer,
            RenderType renderType,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue
    ) {
        VertexConsumer vertexConsumer =
                buffer.getBuffer(renderType);

        if (red < 1.0F
                || green < 1.0F
                || blue < 1.0F) {
            vertexConsumer =
                    new TintedVertexConsumer(
                            vertexConsumer,
                            red,
                            green,
                            blue
                    );
        }

        BlockShadedModelRenderer.render(
                poseStack.last(),
                vertexConsumer,
                null,
                model,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay,
                renderType
        );
    }

    private static final class TintedVertexConsumer
            implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float red;
        private final float green;
        private final float blue;

        private TintedVertexConsumer(
                VertexConsumer delegate,
                float red,
                float green,
                float blue
        ) {
            this.delegate =
                    delegate;
            this.red =
                    red;
            this.green =
                    green;
            this.blue =
                    blue;
        }

        @Override
        public VertexConsumer addVertex(
                float x,
                float y,
                float z
        ) {
            delegate.addVertex(
                    x,
                    y,
                    z
            );
            return this;
        }

        @Override
        public VertexConsumer setColor(
                int red,
                int green,
                int blue,
                int alpha
        ) {
            delegate.setColor(
                    multiplyColor(
                            red,
                            this.red
                    ),
                    multiplyColor(
                            green,
                            this.green
                    ),
                    multiplyColor(
                            blue,
                            this.blue
                    ),
                    alpha
            );
            return this;
        }

        @Override
        public VertexConsumer setUv(
                float u,
                float v
        ) {
            delegate.setUv(
                    u,
                    v
            );
            return this;
        }

        @Override
        public VertexConsumer setUv1(
                int u,
                int v
        ) {
            delegate.setUv1(
                    u,
                    v
            );
            return this;
        }

        @Override
        public VertexConsumer setUv2(
                int u,
                int v
        ) {
            delegate.setUv2(
                    u,
                    v
            );
            return this;
        }

        @Override
        public VertexConsumer setNormal(
                float x,
                float y,
                float z
        ) {
            delegate.setNormal(
                    x,
                    y,
                    z
            );
            return this;
        }

        private static int multiplyColor(
                int channel,
                float multiplier
        ) {
            return Math.max(
                    0,
                    Math.min(
                            255,
                            Math.round(
                                    channel
                                            * multiplier
                            )
                    )
            );
        }
    }
}
