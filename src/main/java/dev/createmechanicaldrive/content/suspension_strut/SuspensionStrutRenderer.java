package dev.createmechanicaldrive.content.suspension_strut;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.createmechanicaldrive.client.SableContraptionDragTransform;
import dev.createmechanicaldrive.client.SuspensionStrutRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SuspensionStrutRenderer
        extends SmartBlockEntityRenderer<SuspensionStrutBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public SuspensionStrutRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    protected void renderSafe(
            SuspensionStrutBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        Level level = blockEntity.getLevel();
        BlockPos blockPos = blockEntity.getBlockPos();
        if (level == null || level.getBlockEntity(blockPos) != blockEntity) {
            return;
        }

        BlockState state = level.getBlockState(blockPos);
        if (!(state.getBlock() instanceof SuspensionStrutBlock)
                || !state.hasProperty(SuspensionStrutBlock.FACING)) {
            return;
        }
        Direction facing = state.getValue(SuspensionStrutBlock.FACING);
        Vec3 localFacing = directionVector(facing);
        Vec3 localOrientation =
                SuspensionStrutRenderHelper.orientationReference(facing);
        Vec3 ownAttachment = SuspensionStrutRenderHelper.attachmentPoint(
                new Vec3(0.5D, 0.5D, 0.5D),
                localFacing
        );

        SuspensionStrutRenderHelper.renderJoint(
                blockRenderer,
                ownAttachment,
                localFacing,
                localOrientation,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                1.0F,
                1.0F,
                1.0F
        );

        if (!blockEntity.isController() || !blockEntity.hasLink()) {
            return;
        }

        renderStrut(
                blockEntity,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                localFacing,
                localOrientation
        );
    }

    private void renderStrut(
            SuspensionStrutBlockEntity blockEntity,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            Vec3 localFacing,
            Vec3 localOrientation
    ) {
        Level level = blockEntity.getLevel();
        BlockPos partnerPos = blockEntity.getPartnerPos();
        BlockPos partnerTransformPos = blockEntity.getLinkedTransformPos();
        if (level == null
                || partnerPos == null
                || partnerTransformPos == null) {
            return;
        }

        Direction partnerFacing = blockEntity.getLinkedFacing();
        if (partnerFacing == null) {
            return;
        }
        if (blockEntity.isCompactLink()
                && !level.getBlockState(partnerTransformPos).isFaceSturdy(
                level,
                partnerTransformPos,
                partnerFacing
        )) {
            return;
        }
        Vec3 partnerOrientation =
                SuspensionStrutRenderHelper.orientationReference(partnerFacing);
        BlockPos originPos = blockEntity.getBlockPos();
        Vec3 startCenter = Vec3.atCenterOf(originPos);
        Vec3 partnerWorldCenter = linkedWorldCenter(
                blockEntity,
                level,
                partnerTransformPos,
                partnerFacing
        );
        Vec3 endCenter = SableContraptionDragTransform.worldPositionToLocal(
                level,
                originPos,
                partnerWorldCenter
        );

        Vec3 partnerDirection;
        if (endCenter == null) {
            startCenter = SableContraptionDragTransform.renderWorldCenter(level, originPos);
            endCenter = partnerWorldCenter;
            localFacing = renderWorldDirection(level, originPos, localFacing);
            localOrientation = renderWorldDirection(
                    level,
                    originPos,
                    localOrientation
            );
            partnerDirection = renderWorldDirection(
                    level,
                    partnerTransformPos,
                    directionVector(partnerFacing)
            );
            partnerOrientation = renderWorldDirection(
                    level,
                    partnerTransformPos,
                    partnerOrientation
            );
        } else {
            partnerDirection = renderDirectionRelativeTo(
                    level,
                    originPos,
                    partnerTransformPos,
                    directionVector(partnerFacing)
            );
            partnerOrientation = renderDirectionRelativeTo(
                    level,
                    originPos,
                    partnerTransformPos,
                    partnerOrientation
            );
        }

        Vec3 origin = Vec3.atLowerCornerOf(originPos);
        Vec3 start = SuspensionStrutRenderHelper.attachmentPoint(
                startCenter,
                localFacing
        ).subtract(origin);
        Vec3 end = SuspensionStrutRenderHelper.attachmentPoint(
                endCenter,
                partnerDirection
        ).subtract(origin);

        if (blockEntity.isCompactLink()) {
            SuspensionStrutRenderHelper.renderJoint(
                    blockRenderer,
                    end,
                    partnerDirection,
                    partnerOrientation,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    1.0F,
                    1.0F,
                    1.0F
            );
        }

        SuspensionStrutRenderHelper.renderStrutBetween(
                blockRenderer,
                start,
                end,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                1.0F,
                1.0F,
                1.0F,
                blockEntity.getRestLength(),
                localOrientation,
                partnerOrientation
        );
    }

    @Override
    public AABB getRenderBoundingBox(SuspensionStrutBlockEntity blockEntity) {
        BlockPos originPos = blockEntity.getBlockPos();
        BlockPos partnerPos = blockEntity.getPartnerPos();
        BlockPos partnerTransformPos = blockEntity.getLinkedTransformPos();
        Level level = blockEntity.getLevel();

        if (partnerPos == null) {
            return new AABB(originPos).inflate(0.5D);
        }

        if (level == null || partnerTransformPos == null) {
            return AABB.encapsulatingFullBlocks(originPos, partnerPos)
                    .inflate(1.0D);
        }

        Direction partnerFacing = blockEntity.getLinkedFacing();
        if (partnerFacing == null) {
            return new AABB(originPos).inflate(0.5D);
        }
        Vec3 partnerWorldCenter = linkedWorldCenter(
                blockEntity,
                level,
                partnerTransformPos,
                partnerFacing
        );
        Vec3 start = Vec3.atCenterOf(originPos);
        Vec3 end = SableContraptionDragTransform.worldPositionToLocal(
                level,
                originPos,
                partnerWorldCenter
        );

        if (end == null) {
            start = SableContraptionDragTransform.renderWorldCenter(level, originPos);
            end = partnerWorldCenter;
        }

        return new AABB(start, end).inflate(1.0D);
    }

    @Override
    public boolean shouldRenderOffScreen(SuspensionStrutBlockEntity blockEntity) {
        return true;
    }

    private static Vec3 renderWorldDirection(
            Level level,
            BlockPos pos,
            Vec3 direction
    ) {
        Vec3 transformed = SableContraptionDragTransform.localNormalToWorld(
                level,
                pos,
                direction
        );
        return transformed == null ? direction : transformed.normalize();
    }

    private static Vec3 renderDirectionRelativeTo(
            Level level,
            BlockPos originPos,
            BlockPos targetPos,
            Vec3 direction
    ) {
        Vec3 world = renderWorldDirection(level, targetPos, direction);
        Vec3 local = SableContraptionDragTransform.worldNormalToLocal(
                level,
                originPos,
                world
        );
        return local == null ? world : local.normalize();
    }

    private static Vec3 directionVector(Direction direction) {
        return Vec3.atLowerCornerOf(direction.getNormal());
    }

    private static Vec3 linkedWorldCenter(
            SuspensionStrutBlockEntity blockEntity,
            Level level,
            BlockPos partnerTransformPos,
            Direction partnerFacing
    ) {
        Vec3 center = SableContraptionDragTransform.renderWorldCenter(
                level,
                partnerTransformPos
        );
        if (!blockEntity.isCompactLink()) {
            return center;
        }
        return center.add(renderWorldDirection(
                level,
                partnerTransformPos,
                directionVector(partnerFacing)
        ));
    }
}
