package dev.createmechanicaldrive.content.stirling_engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class StirlingEngineOutputRenderer extends SafeBlockEntityRenderer<StirlingEngineOutputBlockEntity> {
    public StirlingEngineOutputRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(
            StirlingEngineOutputBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        StirlingEnginePoweredShaftBlockEntity shaft = blockEntity.getShaft();
        if (shaft == null) {
            return;
        }

        Float targetAngle = blockEntity.getTargetAngle();
        if (targetAngle == null) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        Direction facing = StirlingEngineOutputBlock.getConnectedDirection(state);
        Direction.Axis facingAxis = facing.getAxis();
        Direction.Axis shaftAxis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);
        Direction.Axis defaultShaftAxis = facingAxis == Direction.Axis.X
                ? Direction.Axis.Z
                : Direction.Axis.X;
        boolean roll90 = shaftAxis != defaultShaftAxis;
        float angle = targetAngle;

        float piston = 0.375F * Mth.sin(angle)
                - Mth.sqrt(Mth.square(0.875F) - Mth.square(0.375F) * Mth.square(Mth.cos(angle)));
        float linkageDistance = Mth.sqrt(Mth.square(piston - 0.375F * Mth.sin(angle)));
        float linkageAngle = (float) Math.acos(linkageDistance / 0.875F)
                * (Mth.cos(angle) >= 0.0F ? 1.0F : -1.0F);

        VertexConsumer solid = buffer.getBuffer(RenderType.solid());

        transformed(AllPartialModels.ENGINE_PISTON, state, facing, roll90)
                .translate(0.0F, piston + 1.25F, 0.0F)
                .light(packedLight)
                .renderInto(poseStack, solid);

        transformed(AllPartialModels.ENGINE_LINKAGE, state, facing, roll90)
                .center()
                .translate(0.0F, 1.0F, 0.0F)
                .uncenter()
                .translate(0.0F, piston + 1.25F, 0.0F)
                .translate(0.0F, 0.25F, 0.5F)
                .rotateX(linkageAngle)
                .translate(0.0F, -0.25F, -0.5F)
                .light(packedLight)
                .renderInto(poseStack, solid);

        if (blockEntity.shouldRenderConnector()) {
            transformed(AllPartialModels.ENGINE_CONNECTOR, state, facing, roll90)
                    .translate(0.0F, 2.0F, 0.0F)
                    .center()
                    .rotateX(-(angle + Mth.HALF_PI))
                    .uncenter()
                    .light(packedLight)
                    .renderInto(poseStack, solid);
        }
    }

    private SuperByteBuffer transformed(
            PartialModel partial,
            BlockState state,
            Direction facing,
            boolean roll90
    ) {
        return CachedBuffers.partial(partial, state)
                .center()
                .rotateYDegrees(AngleHelper.horizontalAngle(facing))
                .rotateXDegrees(AngleHelper.verticalAngle(facing) + 90.0F)
                .rotateYDegrees(roll90 ? -90.0F : 0.0F)
                .uncenter();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
