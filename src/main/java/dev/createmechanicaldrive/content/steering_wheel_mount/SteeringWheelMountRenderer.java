package dev.createmechanicaldrive.content.steering_wheel_mount;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffsets;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2d;

public class SteeringWheelMountRenderer extends KineticBlockEntityRenderer<SteeringWheelMountBlockEntity> {
    private static final PartialModel TELE_OUTER = offroadModel("tele_outer");
    private static final PartialModel TELE_INNER = offroadModel("tele_inner");
    private static final PartialModel TELE_MOUNT = offroadModel("mount");
    private static final PartialModel SPRING_UPPER = offroadModel("spring_upper");
    private static final PartialModel SPRING_MIDDLE = offroadModel("spring_middle");
    private static final PartialModel SPRING_LOWER = offroadModel("spring_lower");

    public SteeringWheelMountRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    private static PartialModel offroadModel(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(
                "offroad",
                "block/wheel_mount/" + name
        ));
    }

    @Override
    protected void renderSafe(
            SteeringWheelMountBlockEntity mount,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        BlockState state = getRenderedBlockState(mount);
        RenderType rotatingType = getRenderType(mount, state);
        renderRotatingBuffer(
                mount,
                getRotatedModel(mount, state),
                poseStack,
                buffers.getBuffer(rotatingType),
                light
        );
        renderSteeringShaft(mount, state, poseStack, buffers, rotatingType, light);
        FilteringRenderer.renderOnBlockEntity(mount, partialTicks, poseStack, buffers, light, overlay);

        VertexConsumer cutout = buffers.getBuffer(RenderType.cutoutMipped());
        Direction wheelSide = mount.getBlockState()
                .getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction renderDirection = wheelSide.getOpposite();
        double lateralOffset = WheelMountOffsets.lateral(
                mount, partialTicks, wheelSide
        );
        double longitudinalOffset = WheelMountOffsets.longitudinal(
                mount, partialTicks
        );
        double wheelVertical = -mount.getLerpedExtension(partialTicks)
                + WheelMountOffsets.height(mount, partialTicks);
        double linkageAngle = Math.atan2(wheelVertical + 0.375D, 0.75D);
        double linkageLength = new Vector2d(wheelVertical + 0.375D, 0.75D).length();
        double springAngle = Math.atan2(wheelVertical - 0.4375D + 0.125D, 0.1875D);
        double springLength = new Vector2d(wheelVertical - 0.4375D + 0.125D, 0.1875D).length();

        poseStack.pushPose();
        ((PoseTransformStack) ((PoseTransformStack) TransformStack.of(poseStack)
                .center())
                .rotateYDegrees(AngleHelper.horizontalAngle(renderDirection))
                .rotateXDegrees(AngleHelper.verticalAngle(renderDirection)))
                .uncenter();

        poseStack.translate(lateralOffset, 0.0D, longitudinalOffset);
        renderTelescopingLinkage(state, poseStack, cutout, light, linkageAngle, linkageLength);
        renderWheel(mount, state, partialTicks, poseStack, buffers, cutout, light, overlay, wheelVertical, renderDirection);
        renderSpring(state, poseStack, cutout, light, springAngle, springLength);
        poseStack.popPose();
    }

    private static void renderSteeringShaft(
            SteeringWheelMountBlockEntity mount,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffers,
            RenderType renderType,
            int light
    ) {
        Direction firstSide = mount.getSteeringShaftDirection();
        Direction.Axis axis = firstSide.getAxis();
        float angle = getSteeringShaftAngle(mount, axis);
        renderSteeringShaftHalf(
                mount, state, firstSide, axis, angle,
                poseStack, buffers, renderType, light
        );
        renderSteeringShaftHalf(
                mount, state, firstSide.getOpposite(), axis, angle,
                poseStack, buffers, renderType, light
        );
    }

    private static void renderSteeringShaftHalf(
            SteeringWheelMountBlockEntity mount,
            BlockState state,
            Direction side,
            Direction.Axis axis,
            float angle,
            PoseStack poseStack,
            MultiBufferSource buffers,
            RenderType renderType,
            int light
    ) {
        SuperByteBuffer shaft = CachedBuffers.partialFacing(
                AllPartialModels.SHAFT_HALF,
                state,
                side
        ).reset();
        KineticBlockEntityRenderer.kineticRotationTransform(
                shaft,
                mount,
                axis,
                angle,
                light
        ).renderInto(poseStack, buffers.getBuffer(renderType));
    }

    private static float getSteeringShaftAngle(
            SteeringWheelMountBlockEntity mount,
            Direction.Axis axis
    ) {
        Level level = mount.getLevel();
        float renderTime = level == null ? 0.0F : AnimationTickHolder.getRenderTime(level);
        float offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(
                mount,
                mount.getBlockPos(),
                axis
        );
        return ((renderTime * mount.getSteeringInputVisualSpeed() * 3.0F / 10.0F + offset) % 360.0F)
                / 180.0F * Mth.PI;
    }

    private static void renderTelescopingLinkage(
            BlockState state,
            PoseStack poseStack,
            VertexConsumer buffer,
            int light,
            double angle,
            double length
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0D, -0.375D, 0.0D);
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.XP.rotation((float) angle));
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        CachedBuffers.partial(TELE_OUTER, state).light(light).renderInto(poseStack, buffer);
        poseStack.translate(0.0D, 0.0D, -(length - 1.0D));
        CachedBuffers.partial(TELE_INNER, state).light(light).renderInto(poseStack, buffer);
        poseStack.popPose();
    }

    private static void renderWheel(
            SteeringWheelMountBlockEntity mount,
            BlockState state,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            VertexConsumer cutout,
            int light,
            int overlay,
            double verticalPosition,
            Direction renderDirection
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0D, verticalPosition, 0.25D);
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.rotateAround(Axis.YP.rotation((float) mount.getLerpedYaw(partialTicks)), 0.0F, 0.0F, -1.0F);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        CachedBuffers.partial(TELE_MOUNT, state).light(light).renderInto(poseStack, cutout);

        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.translate(0.0D, 0.0D, -1.625D);
        double directionSign = renderDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0D : -1.0D;
        double axisSign = renderDirection.getAxis() == Direction.Axis.X ? 1.0D : -1.0D;
        poseStack.mulPose(Axis.ZP.rotation((float) (-mount.getLerpedWheelAngle(partialTicks) * directionSign * axisSign)));

        ItemStack wheelStack = mount.getWheel();
        ShaftMarkerRenderer.renderInWheelMount(
                wheelStack,
                state,
                poseStack,
                buffers,
                light
        );
        TireLike wheel = wheelStack.get(OffroadDataComponents.TIRE);
        if (wheel != null) {
            Vec3 rotation = wheel.rotation();
            poseStack.mulPose(Axis.XP.rotation((float) Math.toRadians(rotation.x)));
            poseStack.mulPose(Axis.YP.rotation((float) Math.toRadians(rotation.y)));
            poseStack.mulPose(Axis.ZP.rotation((float) Math.toRadians(rotation.z)));
            poseStack.translate(wheel.offset().x, wheel.offset().y, wheel.offset().z);

            if (wheel.model().isPresent()) {
                SuperByteBuffer wheelBuffer = CachedBuffers.partial(PartialModel.of(wheel.model().get()), state);
                wheelBuffer.light(light)
                        .translate(-0.5F, 0.0F, -0.5F)
                        .renderInto(poseStack, cutout);
            } else {
                Minecraft.getInstance().getItemRenderer().renderStatic(
                        wheelStack,
                        ItemDisplayContext.NONE,
                        light,
                        overlay,
                        poseStack,
                        buffers,
                        mount.getLevel(),
                        0
                );
            }
        }
        poseStack.popPose();
    }

    private static void renderSpring(
            BlockState state,
            PoseStack poseStack,
            VertexConsumer buffer,
            int light,
            double angle,
            double length
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.9375D, 0.0625D);
        poseStack.mulPose(Axis.XP.rotation((float) angle + (float) (Math.PI / 2.0D)));
        poseStack.translate(-0.5D, -0.9375D, -0.0625D);

        float flexibleSpan = (float) length - 0.25F;
        CachedBuffers.partial(SPRING_UPPER, state).light(light).renderInto(poseStack, buffer);
        CachedBuffers.partial(SPRING_MIDDLE, state)
                .light(light)
                .translate(0.0F, 0.8125F, 0.0F)
                .scale(1.0F, flexibleSpan / 0.875F, 1.0F)
                .translateBack(0.0F, 0.8125F, 0.0F)
                .renderInto(poseStack, buffer);
        CachedBuffers.partial(SPRING_LOWER, state)
                .light(light)
                .translate(0.0D, -(flexibleSpan - 0.875D), 0.0D)
                .renderInto(poseStack, buffer);
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 512;
    }

    @Override
    protected SuperByteBuffer getRotatedModel(SteeringWheelMountBlockEntity mount, BlockState state) {
        return CachedBuffers.partialFacing(
                AllPartialModels.SHAFT_HALF,
                mount.getBlockState(),
                mount.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite()
        );
    }
}
