package dev.createmechanicaldrive.content.rigid_link;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.createmechanicaldrive.client.RigidLinkRenderHelper;
import dev.createmechanicaldrive.client.SableContraptionDragTransform;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class RigidLinkJointRenderer
        extends SmartBlockEntityRenderer<RigidLinkJointBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public RigidLinkJointRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public boolean shouldRenderOffScreen(RigidLinkJointBlockEntity blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(
            RigidLinkJointBlockEntity blockEntity
    ) {
        AABB bounds =
                new AABB(
                        blockEntity.getBlockPos()
                ).inflate(
                        0.5D
                );

        Level level =
                blockEntity.getLevel();

        if (level == null) {
            return bounds;
        }

        BlockPos originPos =
                blockEntity.getBlockPos();

        for (RigidLinkJointBlockEntity.Connection link :
                blockEntity.getValidRenderLinks()) {

            RigidLinkJointBlockEntity other =
                    blockEntity.resolve(
                            link
                    );

            if (other == null) {
                continue;
            }

            if (blockEntity.hasPonderRenderOffset()
                    || other.hasPonderRenderOffset()) {

                Vec3 relativeOffset =
                        other.getPonderRenderOffset(
                                        1.0F
                                )
                                .subtract(
                                        blockEntity
                                                .getPonderRenderOffset(
                                                        1.0F
                                                )
                                );

                Vec3 endpoint =
                        Vec3.atCenterOf(
                                        link.pos()
                                )
                                .add(
                                        relativeOffset
                                );

                bounds =
                        bounds.minmax(
                                new AABB(
                                        endpoint,
                                        endpoint
                                ).inflate(
                                        0.5D
                                )
                        );

                continue;
            }

            Vec3 linkedWorld =
                    SableContraptionDragTransform
                            .renderWorldCenter(
                                    level,
                                    link.pos()
                            );

            Vec3 linkedLocal =
                    SableContraptionDragTransform
                            .worldPositionToLocal(
                                    level,
                                    originPos,
                                    linkedWorld
                            );

            Vec3 endpoint =
                    linkedLocal == null
                            ? linkedWorld
                            : linkedLocal;

            bounds =
                    bounds.minmax(
                            new AABB(
                                    endpoint,
                                    endpoint
                            ).inflate(
                                    0.5D
                            )
                    );
        }

        return bounds;
    }

    @Override
    protected void renderSafe(RigidLinkJointBlockEntity blockEntity,
                              float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight,
                              int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        BlockPos originPos = blockEntity.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(originPos);

        List<RigidLinkJointBlockEntity.Connection> validLinks =
                blockEntity.getValidRenderLinks();

        if (!validLinks.isEmpty()) {
            Direction facing =
                    blockEntity.getBlockState()
                            .getValue(
                                    RigidLinkJointBlock.FACING
                            );

            RigidLinkJointBlockEntity.Connection connectorLink =
                    validLinks.getFirst();

            RigidLinkJointBlockEntity.LinkType connectorType =
                    connectorLink.linkType();

            Vec3 connectorDirection = null;

            if (connectorType
                    == RigidLinkJointBlockEntity.LinkType.LIMITED) {

                RigidLinkJointBlockEntity other =
                        blockEntity.resolve(
                                connectorLink
                        );

                if (other != null
                        && (
                        blockEntity.hasPonderRenderOffset()
                                || other.hasPonderRenderOffset()
                )) {

                    connectorDirection =
                            Vec3.atCenterOf(
                                            connectorLink.pos()
                                    )
                                    .add(
                                            other.getPonderRenderOffset(
                                                            partialTicks
                                                    )
                                                    .subtract(
                                                            blockEntity
                                                                    .getPonderRenderOffset(
                                                                            partialTicks
                                                                    )
                                                    )
                                    )
                                    .subtract(
                                            Vec3.atCenterOf(
                                                    originPos
                                            )
                                    );

                } else {
                    Vec3 linkedWorld =
                            SableContraptionDragTransform
                                    .renderWorldCenter(
                                            level,
                                            connectorLink.pos()
                                    );

                    Vec3 linkedLocal =
                            SableContraptionDragTransform
                                    .worldPositionToLocal(
                                            level,
                                            originPos,
                                            linkedWorld
                                    );

                    connectorDirection =
                            linkedLocal == null
                                    ? linkedWorld.subtract(
                                    SableContraptionDragTransform
                                            .renderWorldCenter(
                                                    level,
                                                    originPos
                                            )
                            )
                                    : linkedLocal.subtract(
                                    Vec3.atCenterOf(
                                            originPos
                                    )
                            );
                }
            }

            RigidLinkRenderHelper.renderConnector(
                    blockRenderer,
                    facing,
                    connectorType,
                    connectorDirection,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    1.0F,
                    1.0F,
                    1.0F
            );
        }

        for (RigidLinkJointBlockEntity.Connection link : validLinks) {
            if (!link.controller()) {
                continue;
            }
            RigidLinkJointBlockEntity other = blockEntity.resolve(link);
            if (other == null || !blockEntity.references(other, link)) {
                continue;
            }

            Vec3 start = Vec3.atCenterOf(originPos);
            Vec3 end;
            if (blockEntity.hasPonderRenderOffset()
                    || other.hasPonderRenderOffset()) {
                end = Vec3.atCenterOf(link.pos())
                        .add(other.getPonderRenderOffset(partialTicks)
                                .subtract(blockEntity.getPonderRenderOffset(partialTicks)));
            } else {
                Vec3 linkedWorld = SableContraptionDragTransform.renderWorldCenter(level, link.pos());
                end = SableContraptionDragTransform.worldPositionToLocal(
                        level, originPos, linkedWorld);
                if (end == null) {
                    start = SableContraptionDragTransform.renderWorldCenter(level, originPos);
                    end = linkedWorld;
                }
            }

            RigidLinkRenderHelper.renderBetween(blockRenderer,
                    start.subtract(origin), end.subtract(origin),
                    poseStack, buffer, packedLight, packedOverlay,
                    1.0F, 1.0F, 1.0F);
        }
    }
}
