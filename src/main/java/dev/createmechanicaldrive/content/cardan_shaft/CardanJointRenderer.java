package dev.createmechanicaldrive.content.cardan_shaft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.createmechanicaldrive.client.CardanShaftRenderHelper;
import dev.createmechanicaldrive.client.SableContraptionDragTransform;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CardanJointRenderer
        extends KineticBlockEntityRenderer<CardanJointBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public CardanJointRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);

        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public AABB getRenderBoundingBox(
            CardanJointBlockEntity blockEntity
    ) {
        BlockPos originPos =
                blockEntity.getBlockPos();
        BlockPos linkedPos =
                blockEntity.getLinkedPos();
        Level level =
                blockEntity.getLevel();

        if (linkedPos == null) {
            return new AABB(
                    originPos
            );
        }

        if (level == null) {
            return AABB.encapsulatingFullBlocks(
                    originPos,
                    linkedPos
            )
                    .inflate(0.5D);
        }

        Vec3 linkedWorldCenter =
                SableContraptionDragTransform.renderWorldCenter(
                        level,
                        linkedPos
                );
        Vec3 start =
                Vec3.atCenterOf(
                        originPos
                );
        Vec3 end =
                SableContraptionDragTransform.worldPositionToLocal(
                        level,
                        originPos,
                        linkedWorldCenter
                );

        if (end == null) {
            start =
                    SableContraptionDragTransform.renderWorldCenter(
                            level,
                            originPos
                    );
            end =
                    linkedWorldCenter;
        }

        return new AABB(
                start,
                end
        )
                .inflate(0.5D);
    }

    @Override
    public boolean shouldRenderOffScreen(
            CardanJointBlockEntity blockEntity
    ) {
        return true;
    }

    @Override
    protected void renderSafe(
            CardanJointBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state =
                blockEntity.getBlockState();
        float breakWarning =
                blockEntity.getBreakWarningProgress();
        float red =
                1.0F;
        float green =
                1.0F - breakWarning;
        float blue =
                1.0F - breakWarning;

        renderHead(
                blockEntity,
                state,
                poseStack,
                buffer,
                packedLight,
                red,
                green,
                blue
        );

        if (blockEntity.isController()
                && blockEntity.hasLink()) {
            renderRod(
                    blockEntity,
                    partialTicks,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    red,
                    green,
                    blue
            );
        }
    }

    private void renderHead(
            CardanJointBlockEntity blockEntity,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            float red,
            float green,
            float blue
    ) {
        Direction.Axis axis =
                state.getValue(CardanJointBlock.FACING)
                        .getAxis();

        SuperByteBuffer head =
                getRotatedModel(
                        blockEntity,
                        state
                )
                        .reset();

        KineticBlockEntityRenderer
                .kineticRotationTransform(
                        head,
                        blockEntity,
                        axis,
                        getAngle(
                                blockEntity,
                                axis
                        ),
                        packedLight
                )
                .color(
                        Math.round(red * 255.0F),
                        Math.round(green * 255.0F),
                        Math.round(blue * 255.0F),
                        255
                )
                .renderInto(
                        poseStack,
                        buffer.getBuffer(
                                getRenderType(
                                        blockEntity,
                                        state
                                )
                        )
                );
    }

    private void renderRod(
            CardanJointBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue
    ) {
        Level level =
                blockEntity.getLevel();

        BlockPos linkedPos =
                blockEntity.getLinkedPos();

        if (level == null
                || linkedPos == null
                || !blockEntity.isLinkCurrentlyValid()) {
            return;
        }

        BlockPos originPos =
                blockEntity.getBlockPos();

        Direction facing =
                blockEntity.getBlockState()
                        .getValue(CardanJointBlock.FACING);
        Direction linkedFacing =
                level.getBlockState(linkedPos)
                        .getValue(CardanJointBlock.FACING);

        Vec3 startJointDirection =
                directionVector(facing);
        Vec3 endJointDirection =
                directionVector(linkedFacing);

        CardanJointBlockEntity linkedJoint =
                level.getBlockEntity(linkedPos)
                        instanceof CardanJointBlockEntity joint
                        ? joint
                        : null;

        if (linkedJoint != null
                && (blockEntity.hasPonderRenderTransform()
                || linkedJoint.hasPonderRenderTransform())) {
            Vec3 ownOffset =
                    blockEntity.getPonderRenderOffset(
                            partialTicks
                    );
            Vec3 linkedOffset =
                    linkedJoint.getPonderRenderOffset(
                            partialTicks
                    );
            Vec3 start =
                    Vec3.atCenterOf(
                            originPos
                    );
            Vec3 end =
                    Vec3.atCenterOf(
                            linkedPos
                    )
                            .add(
                                    linkedOffset.subtract(
                                            ownOffset
                                    )
                            );
            startJointDirection =
                    VecHelper.rotate(
                            startJointDirection,
                            blockEntity.getPonderRenderYaw(
                                    partialTicks
                            ),
                            Direction.Axis.Y
                    );
            endJointDirection =
                    VecHelper.rotate(
                            endJointDirection,
                            linkedJoint.getPonderRenderYaw(
                                    partialTicks
                            ),
                            Direction.Axis.Y
                    );

            renderRodGeometry(
                    blockEntity,
                    facing,
                    start,
                    end,
                    startJointDirection,
                    endJointDirection,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    red,
                    green,
                    blue
            );
            return;
        }

        Vec3 linkedWorldCenter =
                SableContraptionDragTransform.renderWorldCenter(
                        level,
                        linkedPos
                );

        Vec3 start =
                Vec3.atCenterOf(
                        originPos
                );
        Vec3 end =
                SableContraptionDragTransform.worldPositionToLocal(
                        level,
                        originPos,
                        linkedWorldCenter
                );

        if (end == null) {
            start =
                    SableContraptionDragTransform.renderWorldCenter(
                            level,
                            originPos
                    );
            end =
                    linkedWorldCenter;

            startJointDirection =
                    renderWorldDirection(
                            level,
                            originPos,
                            startJointDirection
                    );
            endJointDirection =
                    renderWorldDirection(
                            level,
                            linkedPos,
                            endJointDirection
                    );
        } else {
            endJointDirection =
                    renderDirectionRelativeTo(
                            level,
                            originPos,
                            linkedPos,
                            endJointDirection
                    );
        }

        renderRodGeometry(
                blockEntity,
                facing,
                start,
                end,
                startJointDirection,
                endJointDirection,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                red,
                green,
                blue
        );
    }

    private void renderRodGeometry(
            CardanJointBlockEntity blockEntity,
            Direction facing,
            Vec3 start,
            Vec3 end,
            Vec3 startJointDirection,
            Vec3 endJointDirection,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue
    ) {
        Vec3 origin =
                Vec3.atLowerCornerOf(
                        blockEntity.getBlockPos()
                );
        Direction.Axis axis =
                facing.getAxis();
        Vec3 delta =
                end.subtract(
                        start
                );
        float rotation =
                getAngle(
                        blockEntity,
                        axis
                )
                        * getRodRotationSign(
                                axis,
                                delta
                        );

        CardanShaftRenderHelper.renderRodBetween(
                blockRenderer,
                start.subtract(origin),
                end.subtract(origin),
                startJointDirection,
                endJointDirection,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                rotation,
                red,
                green,
                blue
        );
    }

    private static Vec3 directionVector(
            Direction direction
    ) {
        return Vec3.atLowerCornerOf(
                direction.getNormal()
        );
    }

    private static Vec3 renderWorldDirection(
            Level level,
            BlockPos pos,
            Vec3 direction
    ) {
        Vec3 transformed =
                SableContraptionDragTransform.localNormalToWorld(
                        level,
                        pos,
                        direction
                );

        return transformed == null
                ? direction
                : transformed;
    }

    private static Vec3 renderDirectionRelativeTo(
            Level level,
            BlockPos originPos,
            BlockPos directionPos,
            Vec3 direction
    ) {
        Vec3 worldDirection =
                renderWorldDirection(
                        level,
                        directionPos,
                        direction
                );
        Vec3 localDirection =
                SableContraptionDragTransform.worldNormalToLocal(
                        level,
                        originPos,
                        worldDirection
                );

        return localDirection == null
                ? worldDirection
                : localDirection;
    }

    private static float getRodRotationSign(
            Direction.Axis axis,
            Vec3 delta
    ) {
        if (delta.lengthSqr() < 1.0E-8D) {
            return 1.0F;
        }

        Vec3 positiveAxis =
                Vec3.atLowerCornerOf(
                        Direction.get(
                                Direction.AxisDirection.POSITIVE,
                                axis
                        ).getNormal()
                );

        return positiveAxis.dot(
                delta.normalize()
        ) < 0.0D
                ? -1.0F
                : 1.0F;
    }

    private static float getAngle(
            CardanJointBlockEntity blockEntity,
            Direction.Axis axis
    ) {
        Level level =
                blockEntity.getLevel();

        float renderTime =
                level == null
                        ? 0.0F
                        : AnimationTickHolder
                        .getRenderTime(level);

        float offset =
                KineticBlockEntityRenderer
                        .getRotationOffsetForPosition(
                                blockEntity,
                                blockEntity.getBlockPos(),
                                axis
                        );

        return (
                (
                        renderTime
                                * blockEntity.getSpeed()
                                * 3.0F
                                / 10.0F
                                + offset
                )
                        % 360.0F
        )
                / 180.0F
                * Mth.PI;
    }
}
