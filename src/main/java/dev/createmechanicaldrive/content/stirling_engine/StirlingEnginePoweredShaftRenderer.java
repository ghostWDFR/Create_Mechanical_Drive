package dev.createmechanicaldrive.content.stirling_engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class StirlingEnginePoweredShaftRenderer
        extends KineticBlockEntityRenderer<StirlingEnginePoweredShaftBlockEntity> {

    public StirlingEnginePoweredShaftRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(
            StirlingEnginePoweredShaftBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        Direction.Axis axis =
                blockEntity.getBlockState()
                        .getValue(StirlingEnginePoweredShaftBlock.AXIS);

        boolean hasValidOutput = blockEntity.getLevel() != null
                && StirlingEnginePoweredShaftBlockEntity.hasValidOutput(
                        blockEntity.getLevel(),
                        blockEntity.getBlockPos(),
                        blockEntity.getBlockState()
                );

        BlockState renderedState =
                (hasValidOutput
                        ? AllBlocks.POWERED_SHAFT.getDefaultState()
                        : AllBlocks.SHAFT.getDefaultState())
                        .setValue(
                                net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,
                                axis
                        );

        SuperByteBuffer shaft =
                CachedBuffers.block(
                        KineticBlockEntityRenderer.KINETIC_BLOCK,
                        renderedState
                );

        KineticBlockEntityRenderer.kineticRotationTransform(
                shaft,
                blockEntity,
                axis,
                KineticBlockEntityRenderer.getAngleForBe(
                        blockEntity,
                        blockEntity.getBlockPos(),
                        axis
                ),
                packedLight
        );

        shaft.renderInto(
                poseStack,
                buffer.getBuffer(RenderType.solid())
        );
    }
}
