package dev.createmechanicaldrive.content.rigid_wheel_mount;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerRenderer;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffsets;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class RigidWheelMountRenderer
        extends KineticBlockEntityRenderer<RigidWheelMountBlockEntity> {

    private static final PartialModel WHEEL_MOUNT =
            PartialModel.of(
                    ResourceLocation.fromNamespaceAndPath(
                            "offroad",
                            "block/wheel_mount/mount"
                    )
            );
    private static final PartialModel MOUNT_PART =
            PartialModel.of(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/wheel_mounts/rigid_wheel_mount_part"
                    )
            );

    public RigidWheelMountRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);
    }

    @Override
    protected void renderSafe(
            RigidWheelMountBlockEntity mount,
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

        VertexConsumer cutout =
                buffers.getBuffer(
                        RenderType.cutoutMipped()
                );

        Direction wheelSide = mount.getBlockState()
                .getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction renderDirection = wheelSide.getOpposite();

        poseStack.pushPose();

        ((PoseTransformStack)
                ((PoseTransformStack)
                        TransformStack.of(
                                        poseStack
                                )
                                .center())
                        .rotateYDegrees(
                                AngleHelper.horizontalAngle(
                                        renderDirection
                                )
                        )
                        .rotateXDegrees(
                                AngleHelper.verticalAngle(
                                        renderDirection
                                )
                        ))
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
                renderDirection
        );

        poseStack.popPose();
    }

    private static void renderMountPart(
            RigidWheelMountBlockEntity mount,
            BlockState state,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light
    ) {
        if (!WheelMountOffsets.shouldRenderRigidMountPart(mount)) {
            return;
        }

        Direction wheelSide = state.getValue(
                BlockStateProperties.HORIZONTAL_FACING
        );
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

        CachedBuffers.partial(MOUNT_PART, state)
                .light(light)
                .renderInto(
                        poseStack,
                        buffers.getBuffer(RenderType.solid())
                );

        poseStack.popPose();
    }
    private static void renderWheel(
            RigidWheelMountBlockEntity mount,
            BlockState state,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            VertexConsumer cutout,
            int light,
            int overlay,
            Direction renderDirection
    ) {
        poseStack.pushPose();

        poseStack.translate(
                0.0D,
                0.0D,
                0.25D
        );

        CachedBuffers.partial(
                        WHEEL_MOUNT,
                        state
                )
                .light(
                        light
                )
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
                                -mount.getLerpedWheelAngle(
                                        partialTicks
                                )
                                        * directionSign
                                        * axisSign
                        )
                )
        );

        ItemStack wheelStack =
                mount.getWheel();

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
                                        wheel.model()
                                                .get()
                                ),
                                state
                        );

                wheelBuffer
                        .light(
                                light
                        )
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
                Minecraft.getInstance()
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
            RigidWheelMountBlockEntity mount,
            BlockState state
    ) {
        return CachedBuffers.partialFacing(
                AllPartialModels.SHAFT_HALF,
                mount.getBlockState(),
                mount.getBlockState()
                        .getValue(
                                BlockStateProperties.HORIZONTAL_FACING
                        )
                        .getOpposite()
        );
    }
}
