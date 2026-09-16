package dev.createmechanicaldrive.content.chain_linkage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.createmechanicaldrive.CreateMechanicalDriveClient;
import dev.createmechanicaldrive.client.ChainLinkageRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ChainGearRenderer
        extends KineticBlockEntityRenderer<ChainGearBlockEntity> {
    private static final float TAU =
            (float) (Math.PI * 2.0D);

    private final BlockRenderDispatcher blockRenderer;

    public ChainGearRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);

        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    protected void renderSafe(
            ChainGearBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state =
                blockEntity.getBlockState();

        Direction.Axis axis =
                state.getValue(
                        ChainGearBlock.AXIS
                );

        renderGear(
                blockEntity,
                state,
                axis,
                partialTicks,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        if (blockEntity.hasChainLoop()) {
            renderChainSegment(
                    blockEntity,
                    axis,
                    partialTicks,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );
        }
    }

    private void renderGear(
            ChainGearBlockEntity blockEntity,
            BlockState state,
            Direction.Axis axis,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        RenderType renderType =
                getRenderType(
                        blockEntity,
                        state
                );

        Vec3 ponderOffset =
                ChainGearPonderPositionHelper.renderOffset(
                        blockEntity.getLevel(),
                        blockEntity.getBlockPos(),
                        partialTicks
                );

        poseStack.pushPose();
        poseStack.translate(
                ponderOffset.x,
                ponderOffset.y,
                ponderOffset.z
        );
        renderRotatingBuffer(
                blockEntity,
                getRotatedModel(
                        blockEntity,
                        state
                ),
                poseStack,
                buffer.getBuffer(
                        renderType
                ),
                packedLight
        );
        poseStack.popPose();
    }

    private void renderChainSegment(
            ChainGearBlockEntity blockEntity,
            Direction.Axis axis,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        List<BlockPos> loop =
                blockEntity.getChainLoop();

        if (loop.size() < ChainLinkageValidator.MIN_GEARS) {
            return;
        }

        BakedModel linkModel =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(
                                CreateMechanicalDriveClient
                                        .CHAIN_LINKAGE_MODEL
                        );

        int index =
                blockEntity.getChainIndex();

        if (index < 0
                || index >= loop.size()) {
            return;
        }

        BlockPos origin =
                blockEntity.getBlockPos();

        if (ChainGearPonderPositionHelper.shouldRenderFlexiblePonderChain(
                blockEntity.getLevel(),
                loop,
                partialTicks
        )) {
            ChainLinkageRenderHelper.renderFlexibleTwoGearPart(
                    blockRenderer,
                    linkModel,
                    ChainGearPonderPositionHelper.renderCenter(
                            blockEntity.getLevel(),
                            loop.getFirst(),
                            partialTicks
                    ),
                    ChainGearPonderPositionHelper.renderCenter(
                            blockEntity.getLevel(),
                            loop.getLast(),
                            partialTicks
                    ),
                    axis,
                    ChainGearPonderPositionHelper.axisVector(axis),
                    index,
                    Vec3.atLowerCornerOf(origin),
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    worldSpaceChainTextureOffset(
                            blockEntity,
                            axis,
                            loop
                    )
            );

            return;
        }

        if (blockEntity.needsWorldSpaceChainRender()) {
            if (!blockEntity.isFlexibleTwoGearChainCurrentlyValid()) {
                return;
            }

            int otherIndex =
                    index == 0
                            ? 1
                            : 0;

            ChainLinkageRenderHelper.renderFlexibleTwoGearPart(
                    blockRenderer,
                    linkModel,
                    blockEntity.getLevel(),
                    List.of(
                            origin,
                            loop.get(otherIndex)
                    ),
                    0,
                    axis,
                    Vec3.atLowerCornerOf(origin),
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    worldSpaceChainTextureOffset(
                            blockEntity,
                            axis,
                            loop
                    )
            );

            return;
        }

        ChainLinkageRenderHelper.renderLoopPart(
                blockRenderer,
                linkModel,
                loop,
                index,
                axis,
                Vec3.atLowerCornerOf(origin),
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                -chainTextureOffset(
                        blockEntity,
                        axis,
                        loop
                )
        );
    }
    private static float worldSpaceChainTextureOffset(
            ChainGearBlockEntity blockEntity,
            Direction.Axis axis,
            List<BlockPos> loop
    ) {
        float offset =
                chainTextureOffset(
                        blockEntity,
                        axis,
                        loop
                );

        return axis == Direction.Axis.Z
                ? -offset
                : offset;
    }

    private static float chainTextureOffset(
            ChainGearBlockEntity blockEntity,
            Direction.Axis axis,
            List<BlockPos> loop
    ) {
        float speed =
                blockEntity.getSpeed();

        if (Math.abs(speed) < 0.001F) {
            return 0.0F;
        }

        float angle =
                KineticBlockEntityRenderer
                        .getAngleForBe(
                                blockEntity,
                                blockEntity.getBlockPos(),
                                axis
                        );

        float turns =
                angle / TAU * 8.0F;

        float phase =
                turns - (float) Math.floor(
                        turns
                );

        return phase
                * ChainLinkageRenderHelper.textureMotionSign(
                        loop,
                        axis
                );
    }
}
