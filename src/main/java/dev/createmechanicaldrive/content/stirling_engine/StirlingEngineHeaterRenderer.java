package dev.createmechanicaldrive.content.stirling_engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.createmechanicaldrive.CreateMechanicalDriveClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class StirlingEngineHeaterRenderer
        implements BlockEntityRenderer<StirlingEngineHeaterBlockEntity> {
    private static final float START_OFFSET = 4.0F / 16.0F;
    private final BlockRenderDispatcher blockRenderer;

    public StirlingEngineHeaterRenderer(BlockEntityRendererProvider.Context context) {
        blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            StirlingEngineHeaterBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (!blockEntity.hasCover() && !blockEntity.isCoverRemoving()) {
            return;
        }

        Direction facing = blockEntity.getBlockState().getValue(StirlingEngineHeaterBlock.FACING);
        float progress = blockEntity.getCoverProgress(partialTick);
        float easedProgress = 1.0F - Mth.square(1.0F - progress);
        float offset = START_OFFSET * (1.0F - easedProgress);

        BakedModel model = Minecraft.getInstance()
                .getModelManager()
                .getModel(CreateMechanicalDriveClient.STIRLING_ENGINE_HEATER_COVER_MODEL);
        RenderType renderType = RenderType.cutout();
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        poseStack.pushPose();
        poseStack.translate(-facing.getStepX() * offset, 0.0F, -facing.getStepZ() * offset);
        poseStack.translate(0.0F, -facing.getStepY() * offset, 0.0F);
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (facing) {
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(270.0F));
            default -> {
            }
        }
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        Level level = blockEntity.getLevel();
        if (level == null) {
            poseStack.popPose();
            return;
        }

        BlockState state = blockEntity.getBlockState();
        BlockPos pos = blockEntity.getBlockPos();
        blockRenderer.getModelRenderer().tesselateBlock(
                level,
                model,
                state,
                pos,
                poseStack,
                buffer,
                false,
                RandomSource.create(),
                state.getSeed(pos),
                packedOverlay,
                ModelData.EMPTY,
                renderType
        );
        poseStack.popPose();
    }
}
