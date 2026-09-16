package dev.createmechanicaldrive.content.gearbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CarGearboxInputRenderer extends KineticBlockEntityRenderer<CarGearboxInputBlockEntity> {
    public CarGearboxInputRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(
            CarGearboxInputBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        Direction.Axis axis = blockEntity.getBlockState().getValue(CarGearboxInputBlock.AXIS);
        BlockState shaftState = KineticBlockEntityRenderer.shaft(axis);
        RenderType renderType = getRenderType(blockEntity, shaftState);
        SuperByteBuffer shaft = getRotatedModel(blockEntity, shaftState);

        KineticBlockEntityRenderer.renderRotatingBuffer(
                blockEntity,
                shaft,
                poseStack,
                buffer.getBuffer(renderType),
                getShaftLight(blockEntity, axis, packedLight)
        );
    }

    private static int getShaftLight(CarGearboxInputBlockEntity blockEntity, Direction.Axis axis, int fallbackLight) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return fallbackLight;
        }

        BlockPos pos = blockEntity.getBlockPos();
        Direction positive = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        Direction negative = Direction.get(Direction.AxisDirection.NEGATIVE, axis);

        int centerLight = LevelRenderer.getLightColor(level, pos);
        int positiveLight = LevelRenderer.getLightColor(level, pos.relative(positive));
        int negativeLight = LevelRenderer.getLightColor(level, pos.relative(negative));

        return maxPackedLight(fallbackLight, centerLight, positiveLight, negativeLight);
    }

    private static int maxPackedLight(int first, int... rest) {
        int result = first;
        for (int packedLight : rest) {
            int block = Math.max(result & 0xFFFF, packedLight & 0xFFFF);
            int sky = Math.max(result >>> 16, packedLight >>> 16);
            result = block | sky << 16;
        }
        return result;
    }
}
