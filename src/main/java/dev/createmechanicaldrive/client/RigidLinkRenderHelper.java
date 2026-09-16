package dev.createmechanicaldrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.createmechanicaldrive.CreateMechanicalDriveClient;
import dev.createmechanicaldrive.content.rigid_link.RigidLinkJointBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RigidLinkRenderHelper {
    private static final double DETAIL_LENGTH = 4.0D / 16.0D;
    private static final double DETAIL_START_CENTER_INSET = 2.0D / 16.0D;
    private static final double DETAIL_END_CENTER_INSET = 1.0D / 16.0D;

    private RigidLinkRenderHelper() {
    }

    public static void renderBetween(BlockRenderDispatcher blockRenderer,
                                     Vec3 start, Vec3 end,
                                     PoseStack poseStack, MultiBufferSource buffer,
                                     int packedLight, int packedOverlay,
                                     float red, float green, float blue) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.001D) {
            return;
        }
        Vec3 direction = delta.normalize();
        BakedModel shaft = model(CreateMechanicalDriveClient.RIGID_LINK_SHAFT_MODEL);
        BakedModel detail = model(CreateMechanicalDriveClient.RIGID_LINK_SHAFT_DETAIL_MODEL);
        RenderType renderType = RenderType.cutout();

        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);
        rotateToDirection(poseStack, direction);

        poseStack.pushPose();
        poseStack.scale(1.0F, (float) length, 1.0F);
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        renderModel(blockRenderer, shaft, poseStack, buffer, renderType,
                packedLight, packedOverlay, red, green, blue);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, DETAIL_START_CENTER_INSET, 0.0D);
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        renderModel(blockRenderer, detail, poseStack, buffer, renderType,
                packedLight, packedOverlay, red, green, blue);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, length - DETAIL_END_CENTER_INSET, 0.0D);
        poseStack.mulPose(new Quaternionf().rotateX((float) Math.PI));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        renderModel(blockRenderer, detail, poseStack, buffer, renderType,
                packedLight, packedOverlay, red, green, blue);
        poseStack.popPose();
        poseStack.popPose();
    }

    public static void renderConnector(BlockRenderDispatcher blockRenderer,
                                       Direction facing,
                                       RigidLinkJointBlockEntity.LinkType linkType,
                                       Vec3 linkDirection,
                                       PoseStack poseStack,
                                       MultiBufferSource buffer,
                                       int packedLight, int packedOverlay,
                                       float red, float green, float blue) {
        BakedModel connector = model(linkType == RigidLinkJointBlockEntity.LinkType.LIMITED
                ? CreateMechanicalDriveClient.RIGID_LINK_LIMITED_CONNECTOR_MODEL
                : CreateMechanicalDriveClient.RIGID_LINK_CONNECTOR_MODEL);
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        if (linkType == RigidLinkJointBlockEntity.LinkType.LIMITED
                && linkDirection != null && linkDirection.lengthSqr() > 1.0E-8D) {
            rotateLimitedConnector(poseStack, facing, linkDirection);
        } else {
            rotateToDirection(poseStack,
                    Vec3.atLowerCornerOf(facing.getNormal()));
        }
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        renderModel(blockRenderer, connector, poseStack, buffer,
                RenderType.cutout(), packedLight, packedOverlay,
                red, green, blue);
        poseStack.popPose();
    }

    private static void rotateLimitedConnector(PoseStack poseStack,
                                               Direction facing,
                                               Vec3 linkDirection) {
        Vec3 shaftAxis = linkDirection.normalize();
        Vec3 jointAxis = Vec3.atLowerCornerOf(facing.getNormal());
        jointAxis = jointAxis.subtract(
                shaftAxis.scale(jointAxis.dot(shaftAxis)));
        if (jointAxis.lengthSqr() < 1.0E-8D) {
            rotateToDirection(poseStack, shaftAxis);
            return;
        }
        jointAxis = jointAxis.normalize();

        Quaternionf shaftRotation = new Quaternionf().rotationTo(
                new Vector3f(0.0F, 1.0F, 0.0F), shaftAxis.toVector3f());
        Vector3f currentCrossAxisVector = new Vector3f(1.0F, 0.0F, 0.0F);
        shaftRotation.transform(currentCrossAxisVector);
        Vec3 currentCrossAxis = new Vec3(currentCrossAxisVector.x,
                currentCrossAxisVector.y, currentCrossAxisVector.z).normalize();
        double rollSin = shaftAxis.dot(currentCrossAxis.cross(jointAxis));
        double rollCos = currentCrossAxis.dot(jointAxis);

        poseStack.mulPose(shaftRotation);
        poseStack.mulPose(new Quaternionf().rotateY(
                (float) Math.atan2(rollSin, rollCos)));
    }

    private static BakedModel model(net.minecraft.client.resources.model.ModelResourceLocation location) {
        return Minecraft.getInstance().getModelManager().getModel(location);
    }

    private static void rotateToDirection(PoseStack poseStack, Vec3 direction) {
        poseStack.mulPose(new Quaternionf().rotationTo(
                new Vector3f(0.0F, 1.0F, 0.0F), direction.toVector3f()));
    }

    private static void renderModel(BlockRenderDispatcher blockRenderer,
                                    BakedModel model, PoseStack poseStack,
                                    MultiBufferSource buffer, RenderType renderType,
                                    int packedLight, int packedOverlay,
                                    float red, float green, float blue) {
        BlockShadedModelRenderer.render(
                poseStack.last(),
                buffer.getBuffer(renderType),
                null,
                model,
                red,
                green,
                blue,
                packedLight,
                packedOverlay,
                renderType
        );
    }
}
