package dev.createmechanicaldrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.createmechanicaldrive.CreateMechanicalDriveClient;
import dev.simulated_team.simulated.index.SimRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SuspensionStrutRenderHelper {
    public static final double BALL_RADIUS = 3.0D / 16.0D;
    public static final double FIXED_UPPER_SHAFT_LENGTH = 5.0D / 16.0D;
    private static final double DYNAMIC_SHAFT_END_INSET =
            BALL_RADIUS + FIXED_UPPER_SHAFT_LENGTH;
    private static final float SPRING_HALF_WIDTH = 2.75F / 16.0F;
    private static final ResourceLocation SPRING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "simulated",
                    "textures/block/spring/spring.png"
            );

    private SuspensionStrutRenderHelper() {
    }

    public static Vec3 attachmentPoint(
            Vec3 blockCenter,
            Vec3 facing
    ) {
        return blockCenter.subtract(facing.normalize().scale(0.5D));
    }

    public static void renderJoint(
            BlockRenderDispatcher blockRenderer,
            Vec3 attachmentPoint,
            Vec3 facing,
            Vec3 orientationReference,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue
    ) {
        if (facing.lengthSqr() < 1.0E-8D) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(
                attachmentPoint.x,
                attachmentPoint.y,
                attachmentPoint.z
        );
        rotateToAveragedOrientation(
                poseStack,
                facing.normalize(),
                orientationReference,
                orientationReference
        );
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        renderModel(
                blockRenderer,
                getModel(CreateMechanicalDriveClient.SUSPENSION_STRUT_JOINT_MODEL),
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
        poseStack.popPose();
    }

    public static void renderStrutBetween(
            BlockRenderDispatcher blockRenderer,
            Vec3 start,
            Vec3 end,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            double configuredLength,
            Vec3 startOrientation,
            Vec3 endOrientation
    ) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }

        Vec3 direction = delta.scale(1.0D / length);

        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);
        rotateToAveragedOrientation(
                poseStack,
                direction,
                startOrientation,
                endOrientation
        );

        renderCenteredPart(
                blockRenderer,
                CreateMechanicalDriveClient.SUSPENSION_STRUT_BALL_MODEL,
                0.0D,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
        renderCenteredPart(
                blockRenderer,
                CreateMechanicalDriveClient.SUSPENSION_STRUT_BALL_MODEL,
                length,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );

        renderDynamicSpring(
                BALL_RADIUS,
                Math.max(0.0D, length - BALL_RADIUS * 2.0D),
                configuredLength,
                poseStack,
                buffer,
                packedLight,
                red,
                green,
                blue
        );

        renderScaledPart(
                blockRenderer,
                CreateMechanicalDriveClient.SUSPENSION_STRUT_SHAFT_MODEL,
                BALL_RADIUS,
                Math.max(0.0D, length - DYNAMIC_SHAFT_END_INSET - BALL_RADIUS),
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );

        poseStack.pushPose();
        poseStack.translate(
                -0.5D,
                length - 1.0D,
                -0.5D
        );
        renderModel(
                blockRenderer,
                getModel(CreateMechanicalDriveClient.SUSPENSION_STRUT_UPPER_SHAFT_MODEL),
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void renderDynamicSpring(
            double offset,
            double length,
            double configuredLength,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            float red,
            float green,
            float blue
    ) {
        if (length <= 1.0E-4D) {
            return;
        }

        VertexConsumer vertices = buffer.getBuffer(
                SimRenderTypes.spring(SPRING_TEXTURE)
        );

        int tintRed = colorChannel(red);
        int tintGreen = colorChannel(green);
        int tintBlue = colorChannel(blue);
        int tintAlpha = red < 1.0F || green < 1.0F || blue < 1.0F
                ? 255
                : 0;

        double repetitions = Math.max(1.0E-4D, configuredLength);
        double segmentHeight = length / repetitions;
        int fullSegments = (int) Math.floor(repetitions);
        float bottom = (float) offset;

        for (int segment = 0; segment < fullSegments; segment++) {
            float top = segment == fullSegments - 1
                    && repetitions == fullSegments
                    ? (float) (offset + length)
                    : (float) (bottom + segmentHeight);
            renderSpringTube(
                    poseStack,
                    vertices,
                    bottom,
                    top,
                    1.0F,
                    packedLight,
                    tintRed,
                    tintGreen,
                    tintBlue,
                    tintAlpha
            );
            bottom = top;
        }

        float fractionalSegment = (float) (repetitions - fullSegments);
        if (fractionalSegment > 1.0E-4F) {
            renderSpringTube(
                    poseStack,
                    vertices,
                    bottom,
                    (float) (offset + length),
                    fractionalSegment,
                    packedLight,
                    tintRed,
                    tintGreen,
                    tintBlue,
                    tintAlpha
            );
        }
    }

    private static void renderSpringTube(
            PoseStack poseStack,
            VertexConsumer vertices,
            float bottom,
            float top,
            float textureLength,
            int packedLight,
            int tintRed,
            int tintGreen,
            int tintBlue,
            int tintAlpha
    ) {
        float halfWidth = SPRING_HALF_WIDTH;

        renderSpringSide(
                poseStack,
                vertices,
                -halfWidth, bottom, -halfWidth,
                -halfWidth, top, -halfWidth,
                halfWidth, top, -halfWidth,
                halfWidth, bottom, -halfWidth,
                0.0F, 0.0F, -1.0F,
                textureLength,
                packedLight,
                tintRed,
                tintGreen,
                tintBlue,
                tintAlpha
        );
        renderSpringSide(
                poseStack,
                vertices,
                halfWidth, bottom, -halfWidth,
                halfWidth, top, -halfWidth,
                halfWidth, top, halfWidth,
                halfWidth, bottom, halfWidth,
                1.0F, 0.0F, 0.0F,
                textureLength,
                packedLight,
                tintRed,
                tintGreen,
                tintBlue,
                tintAlpha
        );
        renderSpringSide(
                poseStack,
                vertices,
                halfWidth, bottom, halfWidth,
                halfWidth, top, halfWidth,
                -halfWidth, top, halfWidth,
                -halfWidth, bottom, halfWidth,
                0.0F, 0.0F, 1.0F,
                textureLength,
                packedLight,
                tintRed,
                tintGreen,
                tintBlue,
                tintAlpha
        );
        renderSpringSide(
                poseStack,
                vertices,
                -halfWidth, bottom, halfWidth,
                -halfWidth, top, halfWidth,
                -halfWidth, top, -halfWidth,
                -halfWidth, bottom, -halfWidth,
                -1.0F, 0.0F, 0.0F,
                textureLength,
                packedLight,
                tintRed,
                tintGreen,
                tintBlue,
                tintAlpha
        );
    }

    private static void renderSpringSide(
            PoseStack poseStack,
            VertexConsumer vertices,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            float dx,
            float dy,
            float dz,
            float normalX,
            float normalY,
            float normalZ,
            float textureLength,
            int packedLight,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        float outerMinU = 0.0F;
        float outerMaxU = 7.0F / 16.0F;
        springVertex(poseStack, vertices, ax, ay, az, outerMinU, 0.0F,
                normalX, normalY, normalZ, packedLight, red, green, blue, alpha);
        springVertex(poseStack, vertices, bx, by, bz, outerMinU, textureLength,
                normalX, normalY, normalZ, packedLight, red, green, blue, alpha);
        springVertex(poseStack, vertices, cx, cy, cz, outerMaxU, textureLength,
                normalX, normalY, normalZ, packedLight, red, green, blue, alpha);
        springVertex(poseStack, vertices, dx, dy, dz, outerMaxU, 0.0F,
                normalX, normalY, normalZ, packedLight, red, green, blue, alpha);

        float innerMinU = 15.0F / 16.0F;
        float innerMaxU = 8.0F / 16.0F;
        springVertex(poseStack, vertices, dx, dy, dz, innerMinU, 0.0F,
                -normalX, -normalY, -normalZ, packedLight, red, green, blue, alpha);
        springVertex(poseStack, vertices, cx, cy, cz, innerMinU, textureLength,
                -normalX, -normalY, -normalZ, packedLight, red, green, blue, alpha);
        springVertex(poseStack, vertices, bx, by, bz, innerMaxU, textureLength,
                -normalX, -normalY, -normalZ, packedLight, red, green, blue, alpha);
        springVertex(poseStack, vertices, ax, ay, az, innerMaxU, 0.0F,
                -normalX, -normalY, -normalZ, packedLight, red, green, blue, alpha);
    }

    private static void springVertex(
            PoseStack poseStack,
            VertexConsumer vertices,
            float x,
            float y,
            float z,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ,
            int packedLight,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        vertices.addVertex(poseStack.last().pose(), x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setLight(packedLight)
                .setNormal(poseStack.last(), normalX, normalY, normalZ);
    }

    private static int colorChannel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
    }

    private static void renderCenteredPart(
            BlockRenderDispatcher blockRenderer,
            ModelResourceLocation model,
            double offset,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue
    ) {
        poseStack.pushPose();
        poseStack.translate(-0.5D, offset - 0.5D, -0.5D);
        renderModel(
                blockRenderer,
                getModel(model),
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
        poseStack.popPose();
    }

    private static void renderScaledPart(
            BlockRenderDispatcher blockRenderer,
            ModelResourceLocation model,
            double offset,
            double length,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue
    ) {
        if (length <= 1.0E-4D) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(-0.5D, offset, -0.5D);
        poseStack.scale(1.0F, (float) length, 1.0F);
        renderModel(
                blockRenderer,
                getModel(model),
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
        poseStack.popPose();
    }

    private static BakedModel getModel(ModelResourceLocation location) {
        return Minecraft.getInstance().getModelManager().getModel(location);
    }

    public static Vec3 orientationReference(Direction facing) {
        Quaternionf orientation = new Quaternionf().rotationTo(
                new Vector3f(0.0F, 1.0F, 0.0F),
                Vec3.atLowerCornerOf(facing.getNormal()).toVector3f()
        );
        Vector3f reference = new Vector3f(1.0F, 0.0F, 0.0F);
        orientation.transform(reference);
        return new Vec3(reference.x, reference.y, reference.z).normalize();
    }

    private static void rotateToAveragedOrientation(
            PoseStack poseStack,
            Vec3 direction,
            Vec3 startOrientation,
            Vec3 endOrientation
    ) {
        Vec3 axis = direction.normalize();
        Quaternionf shaftRotation = new Quaternionf().rotationTo(
                new Vector3f(0.0F, 1.0F, 0.0F),
                axis.toVector3f()
        );
        Vector3f baseVector = new Vector3f(1.0F, 0.0F, 0.0F);
        shaftRotation.transform(baseVector);
        Vec3 base = new Vec3(baseVector.x, baseVector.y, baseVector.z)
                .normalize();

        Vec3 start = projectOntoCrossSection(startOrientation, axis);
        Vec3 end = projectOntoCrossSection(endOrientation, axis);
        if (start == null && end == null) {
            poseStack.mulPose(shaftRotation);
            return;
        }
        if (start == null) {
            start = end;
        } else if (end == null) {
            end = start;
        }

        double startRoll = rollAngle(base, start, axis);
        double endRoll = rollAngle(base, end, axis);
        double shortestDifference = Math.atan2(
                Math.sin(endRoll - startRoll),
                Math.cos(endRoll - startRoll)
        );
        float averagedRoll = (float) (startRoll + shortestDifference * 0.5D);

        poseStack.mulPose(shaftRotation);
        poseStack.mulPose(new Quaternionf().rotateY(averagedRoll));
    }

    private static Vec3 projectOntoCrossSection(
            Vec3 orientation,
            Vec3 axis
    ) {
        if (orientation == null || orientation.lengthSqr() < 1.0E-8D) {
            return null;
        }

        Vec3 projected = orientation.subtract(axis.scale(orientation.dot(axis)));
        return projected.lengthSqr() < 1.0E-8D
                ? null
                : projected.normalize();
    }

    private static double rollAngle(
            Vec3 base,
            Vec3 reference,
            Vec3 axis
    ) {
        return Math.atan2(
                axis.dot(base.cross(reference)),
                base.dot(reference)
        );
    }

    private static void rotateToDirection(
            PoseStack poseStack,
            Vec3 direction
    ) {
        poseStack.mulPose(
                new Quaternionf().rotationTo(
                        new Vector3f(0.0F, 1.0F, 0.0F),
                        direction.toVector3f()
                )
        );
    }

    private static void renderModel(
            BlockRenderDispatcher blockRenderer,
            BakedModel model,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue
    ) {
        VertexConsumer vertices = buffer.getBuffer(RenderType.cutout());
        if (red < 1.0F || green < 1.0F || blue < 1.0F) {
            vertices = new TintedVertexConsumer(vertices, red, green, blue);
        }

        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                vertices,
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
    }

    private static final class TintedVertexConsumer implements VertexConsumer {
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
            this.delegate = delegate;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(
                    multiply(red, this.red),
                    multiply(green, this.green),
                    multiply(blue, this.blue),
                    alpha
            );
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }

        private static int multiply(int channel, float multiplier) {
            return Math.max(0, Math.min(255, Math.round(channel * multiplier)));
        }
    }
}
