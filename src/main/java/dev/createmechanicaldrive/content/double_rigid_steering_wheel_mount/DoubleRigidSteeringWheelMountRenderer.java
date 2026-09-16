package dev.createmechanicaldrive.content.double_rigid_steering_wheel_mount;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerRenderer;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.double_rigid_wheel_mount.DoubleRigidWheelMountBlockEntity;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffsets;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
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

public class DoubleRigidSteeringWheelMountRenderer
        extends KineticBlockEntityRenderer<DoubleRigidSteeringWheelMountBlockEntity> {

    private static final PartialModel WHEEL_MOUNT =
            offroadModel("mount");

    private static final PartialModel STEERING_MOUNT_PART =
            PartialModel.of(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/wheel_mounts/rigid_steering_mount_part"
                    )
            );
    private static final PartialModel RIGID_MOUNT_PART =
            PartialModel.of(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/wheel_mounts/rigid_wheel_mount_part"
                    )
            );

    public DoubleRigidSteeringWheelMountRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);
    }

    private static PartialModel offroadModel(
            String name
    ) {
        return PartialModel.of(
                ResourceLocation.fromNamespaceAndPath(
                        "offroad",
                        "block/wheel_mount/" + name
                )
        );
    }

    @Override
    protected void renderSafe(
            DoubleRigidSteeringWheelMountBlockEntity mount,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        BlockState state =
                getRenderedBlockState(
                        mount
                );

        RenderType rotatingType =
                getRenderType(
                        mount,
                        state
                );

        renderMountPart(
                mount,
                state,
                partialTicks,
                poseStack,
                buffers,
                light
        );

        renderRotatingBuffer(
                mount,
                getRotatedModel(
                        mount,
                        state
                ),
                poseStack,
                buffers.getBuffer(
                        rotatingType
                ),
                light
        );

        if (mount instanceof DoubleRigidWheelMountBlockEntity) {
            renderThroughDriveShaft(
                    mount,
                    state,
                    poseStack,
                    buffers,
                    rotatingType,
                    light
            );
        } else {
            renderSteeringShaft(
                    mount,
                    state,
                    poseStack,
                    buffers,
                    rotatingType,
                    light
            );
        }

        FilteringRenderer.renderOnBlockEntity(
                mount,
                partialTicks,
                poseStack,
                buffers,
                light,
                overlay
        );

        VertexConsumer cutout =
                buffers.getBuffer(
                        RenderType.cutoutMipped()
                );

        Direction facing = state.getValue(
                BlockStateProperties.HORIZONTAL_FACING
        );

        renderWheelAssembly(
                mount,
                state,
                partialTicks,
                poseStack,
                buffers,
                cutout,
                light,
                overlay,
                facing,
                mount.getWheel(0),
                mount.getLerpedWheelAngle(partialTicks)
        );

        renderWheelAssembly(
                mount,
                state,
                partialTicks,
                poseStack,
                buffers,
                cutout,
                light,
                overlay,
                facing.getOpposite(),
                mount.getWheel(1),
                mount.getLerpedSecondWheelAngle(partialTicks)
        );
    }

    private static void renderMountPart(
            DoubleRigidSteeringWheelMountBlockEntity mount,
            BlockState state,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light
    ) {
        if (!WheelMountOffsets.shouldRenderRigidMountPart(mount)) {
            return;
        }

        PartialModel mountPart =
                mount instanceof DoubleRigidWheelMountBlockEntity
                        ? RIGID_MOUNT_PART
                        : STEERING_MOUNT_PART;

        Direction firstSide = state.getValue(
                BlockStateProperties.HORIZONTAL_FACING
        );

        renderMountPartSide(
                mount,
                state,
                partialTicks,
                poseStack,
                buffers,
                light,
                mountPart,
                firstSide
        );
        renderMountPartSide(
                mount,
                state,
                partialTicks,
                poseStack,
                buffers,
                light,
                mountPart,
                firstSide.getOpposite()
        );
    }

    private static void renderMountPartSide(
            DoubleRigidSteeringWheelMountBlockEntity mount,
            BlockState state,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            PartialModel mountPart,
            Direction wheelSide
    ) {
        Direction renderDirection = wheelSide.getOpposite();

        poseStack.pushPose();
        ((PoseTransformStack) ((PoseTransformStack) TransformStack.of(poseStack)
                .center())
                .rotateYDegrees(AngleHelper.horizontalAngle(renderDirection))
                .rotateXDegrees(AngleHelper.verticalAngle(renderDirection)))
                .uncenter();

        poseStack.translate(
                WheelMountOffsets.lateral(mount, partialTicks, wheelSide),
                WheelMountOffsets.height(mount, partialTicks),
                WheelMountOffsets.longitudinal(mount, partialTicks)
        );

        CachedBuffers.partial(mountPart, state)
                .light(light)
                .renderInto(
                        poseStack,
                        buffers.getBuffer(RenderType.solid())
                );

        poseStack.popPose();
    }
    private static void renderWheelAssembly(
            DoubleRigidSteeringWheelMountBlockEntity mount,
            BlockState state,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            VertexConsumer cutout,
            int light,
            int overlay,
            Direction wheelSide,
            ItemStack wheelStack,
            float wheelAngle
    ) {
        Direction renderDirection = wheelSide.getOpposite();

        poseStack.pushPose();
        ((PoseTransformStack) ((PoseTransformStack) TransformStack.of(poseStack)
                .center())
                .rotateYDegrees(AngleHelper.horizontalAngle(renderDirection))
                .rotateXDegrees(AngleHelper.verticalAngle(renderDirection)))
                .uncenter();

        poseStack.translate(
                WheelMountOffsets.lateral(mount, partialTicks, wheelSide),
                WheelMountOffsets.height(mount, partialTicks),
                WheelMountOffsets.longitudinal(mount, partialTicks)
        );

        renderWheel(
                mount,
                state,
                partialTicks,
                poseStack,
                buffers,
                cutout,
                light,
                overlay,
                renderDirection,
                wheelStack,
                wheelAngle
        );

        poseStack.popPose();
    }

    private static void renderSteeringShaft(
            DoubleRigidSteeringWheelMountBlockEntity mount,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffers,
            RenderType renderType,
            int light
    ) {
        Direction firstSide =
                mount.getSteeringShaftDirection();

        Direction.Axis axis =
                firstSide.getAxis();

        float angle =
                getSteeringShaftAngle(
                        mount,
                        axis
                );

        renderSteeringShaftHalf(
                mount,
                state,
                firstSide,
                axis,
                angle,
                poseStack,
                buffers,
                renderType,
                light
        );

    }

    private void renderThroughDriveShaft(
            DoubleRigidSteeringWheelMountBlockEntity mount,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffers,
            RenderType renderType,
            int light
    ) {
        Direction oppositeSide = DoubleRigidSteeringWheelMountBlock
                .getDriveShaftDirection(state)
                .getOpposite();

        renderRotatingBuffer(
                mount,
                CachedBuffers.partialFacing(
                        AllPartialModels.SHAFT_HALF,
                        state,
                        oppositeSide
                ),
                poseStack,
                buffers.getBuffer(renderType),
                light
        );
    }

    private static void renderSteeringShaftHalf(
            DoubleRigidSteeringWheelMountBlockEntity mount,
            BlockState state,
            Direction side,
            Direction.Axis axis,
            float angle,
            PoseStack poseStack,
            MultiBufferSource buffers,
            RenderType renderType,
            int light
    ) {
        SuperByteBuffer shaft =
                CachedBuffers.partialFacing(
                        AllPartialModels.SHAFT_HALF,
                        state,
                        side
                ).reset();

        KineticBlockEntityRenderer
                .kineticRotationTransform(
                        shaft,
                        mount,
                        axis,
                        angle,
                        light
                )
                .renderInto(
                        poseStack,
                        buffers.getBuffer(
                                renderType
                        )
                );
    }

    private static float getSteeringShaftAngle(
            DoubleRigidSteeringWheelMountBlockEntity mount,
            Direction.Axis axis
    ) {
        Level level =
                mount.getLevel();

        float renderTime =
                level == null
                        ? 0.0F
                        : AnimationTickHolder
                        .getRenderTime(
                                level
                        );

        float offset =
                KineticBlockEntityRenderer
                        .getRotationOffsetForPosition(
                                mount,
                                mount.getBlockPos(),
                                axis
                        );

        return (
                (
                        renderTime
                                * mount.getSteeringInputVisualSpeed()
                                * 3.0F
                                / 10.0F
                                + offset
                )
                        % 360.0F
        )
                / 180.0F
                * Mth.PI;
    }

    private static void renderWheel(
            DoubleRigidSteeringWheelMountBlockEntity mount,
            BlockState state,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            VertexConsumer cutout,
            int light,
            int overlay,
            Direction renderDirection,
            ItemStack wheelStack,
            float wheelAngle
    ) {
        poseStack.pushPose();

        poseStack.translate(
                0.0D,
                0.0D,
                0.25D
        );

        poseStack.translate(
                0.5D,
                0.5D,
                0.5D
        );

        poseStack.rotateAround(
                Axis.YP.rotation(
                        (float) mount.getLerpedYaw(
                                partialTicks
                        )
                ),
                0.0F,
                0.0F,
                -1.0F
        );

        poseStack.translate(
                -0.5D,
                -0.5D,
                -0.5D
        );

        CachedBuffers.partial(
                        WHEEL_MOUNT,
                        state
                )
                .light(light)
                .renderInto(
                        poseStack,
                        cutout
                );

        poseStack.translate(
                0.5D,
                0.5D,
                0.5D
        );

        poseStack.translate(
                0.0D,
                0.0D,
                -1.625D
        );

        double directionSign =
                renderDirection.getAxisDirection()
                        == Direction.AxisDirection.POSITIVE
                        ? 1.0D
                        : -1.0D;

        double axisSign =
                renderDirection.getAxis()
                        == Direction.Axis.X
                        ? 1.0D
                        : -1.0D;

        poseStack.mulPose(
                Axis.ZP.rotation(
                        (float) (
                                -wheelAngle
                                        * directionSign
                                        * axisSign
                        )
                )
        );

        ShaftMarkerRenderer.renderInWheelMount(
                wheelStack,
                state,
                poseStack,
                buffers,
                light
        );

        TireLike wheel =
                wheelStack.get(
                        OffroadDataComponents.TIRE
                );

        if (wheel != null) {
            Vec3 rotation =
                    wheel.rotation();

            poseStack.mulPose(
                    Axis.XP.rotation(
                            (float) Math.toRadians(
                                    rotation.x
                            )
                    )
            );

            poseStack.mulPose(
                    Axis.YP.rotation(
                            (float) Math.toRadians(
                                    rotation.y
                            )
                    )
            );

            poseStack.mulPose(
                    Axis.ZP.rotation(
                            (float) Math.toRadians(
                                    rotation.z
                            )
                    )
            );

            poseStack.translate(
                    wheel.offset().x,
                    wheel.offset().y,
                    wheel.offset().z
            );

            if (wheel.model().isPresent()) {
                SuperByteBuffer wheelBuffer =
                        CachedBuffers.partial(
                                PartialModel.of(
                                        wheel.model().get()
                                ),
                                state
                        );

                wheelBuffer
                        .light(light)
                        .translate(
                                -0.5F,
                                0.0F,
                                -0.5F
                        )
                        .renderInto(
                                poseStack,
                                cutout
                        );
            } else {
                Minecraft
                        .getInstance()
                        .getItemRenderer()
                        .renderStatic(
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

    @Override
    public int getViewDistance() {
        return 512;
    }

    @Override
    protected SuperByteBuffer getRotatedModel(
            DoubleRigidSteeringWheelMountBlockEntity mount,
            BlockState state
    ) {
        return CachedBuffers.partialFacing(
                AllPartialModels.SHAFT_HALF,
                mount.getBlockState(),
                DoubleRigidSteeringWheelMountBlock.getDriveShaftDirection(
                        state
                )
        );
    }
}
