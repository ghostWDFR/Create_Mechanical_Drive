package dev.createmechanicaldrive.content.angle_gear;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;

public class AngleGearRenderer
        extends KineticBlockEntityRenderer<AngleGearBlockEntity> {

    private static final PartialModel INPUT_SHAFT_MODEL =
            PartialModel.of(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/distributor_shaft/shaft_distributor_input_shaft"
                    )
            );

    private final BlockRenderDispatcher blockRenderer;

    public AngleGearRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);

        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    protected void renderSafe(
            AngleGearBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state =
                blockEntity.getBlockState();

        if (state.getValue(AngleGearBlock.ENCASED)) {
            renderCasing(
                    blockEntity,
                    state,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );
        }

        for (Direction side : Direction.values()) {
            if (!AngleGearBlock.hasGearTowards(state, side)) {
                continue;
            }

            renderGear(
                    blockEntity,
                    state,
                    side,
                    poseStack,
                    buffer,
                    packedLight
            );
        }
    }

    private void renderGear(
            AngleGearBlockEntity blockEntity,
            BlockState state,
            Direction side,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        SuperByteBuffer rendered =
                CachedBuffers.partial(
                                INPUT_SHAFT_MODEL,
                                state
                        )
                        .reset();

        RenderType renderType =
                getRenderType(
                        blockEntity,
                        state
                );

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(getSideRotation(side));
        poseStack.mulPose(
                Axis.ZP.rotation(
                        getAngleForSpeed(
                                blockEntity,
                                side,
                                blockEntity.getVisualSpeedForSide(side)
                        )
                )
        );
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        rendered
                .light(
                        getShaftLight(
                                blockEntity,
                                side.getAxis(),
                                packedLight
                        )
                )
                .renderInto(
                        poseStack,
                        buffer.getBuffer(renderType)
                );

        poseStack.popPose();
    }

    private static Quaternionf getSideRotation(
            Direction side
    ) {
        return switch (side) {
            case NORTH -> new Quaternionf();
            case SOUTH -> Axis.YP.rotationDegrees(180.0F);
            case EAST -> Axis.YP.rotationDegrees(-90.0F);
            case WEST -> Axis.YP.rotationDegrees(90.0F);
            case UP -> Axis.XP.rotationDegrees(90.0F);
            case DOWN -> Axis.XP.rotationDegrees(-90.0F);
        };
    }

    private static float getAngleForSpeed(
            AngleGearBlockEntity blockEntity,
            Direction side,
            float speed
    ) {
        Level level =
                blockEntity.getLevel();

        float renderTime =
                level == null
                        ? 0.0F
                        : AnimationTickHolder.getRenderTime(level);

        float offset =
                KineticBlockEntityRenderer
                        .getRotationOffsetForPosition(
                                blockEntity,
                                blockEntity.getBlockPos(),
                                side.getAxis()
                        );

        float axisSign =
                side.getAxisDirection() == Direction.AxisDirection.POSITIVE
                        ? -1.0F
                        : 1.0F;

        return (((renderTime * speed * 3.0F / 10.0F + offset)
                * axisSign) % 360.0F)
                / 180.0F
                * Mth.PI;
    }

    private void renderCasing(
            AngleGearBlockEntity blockEntity,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BakedModel casingModel =
                blockRenderer.getBlockModel(state);

        RenderType renderType =
                RenderType.cutout();

        VertexConsumer vertexConsumer =
                bufferSource.getBuffer(renderType);

        Level level =
                blockEntity.getLevel();

        if (level != null
                && SableSubLevelHelper.getSubLevel(
                        level,
                        blockEntity.getBlockPos()
                ) == null) {
            BlockPos pos =
                    blockEntity.getBlockPos();

            blockRenderer
                    .getModelRenderer()
                    .tesselateBlock(
                            level,
                            casingModel,
                            state,
                            pos,
                            poseStack,
                            vertexConsumer,
                            true,
                            RandomSource.create(),
                            state.getSeed(pos),
                            packedOverlay,
                            ModelData.EMPTY,
                            renderType
                    );

            return;
        }

        blockRenderer
                .getModelRenderer()
                .renderModel(
                        poseStack.last(),
                        vertexConsumer,
                        state,
                        casingModel,
                        1.0F,
                        1.0F,
                        1.0F,
                        packedLight,
                        packedOverlay,
                        ModelData.EMPTY,
                        renderType
                );
    }

    private static int getShaftLight(
            AngleGearBlockEntity blockEntity,
            Direction.Axis axis,
            int fallbackLight
    ) {
        Level level =
                blockEntity.getLevel();

        if (level == null) {
            return fallbackLight;
        }

        BlockPos pos =
                blockEntity.getBlockPos();

        Direction positive =
                Direction.get(
                        Direction.AxisDirection.POSITIVE,
                        axis
                );

        Direction negative =
                positive.getOpposite();

        return maxPackedLight(
                fallbackLight,
                LevelRenderer.getLightColor(level, pos),
                LevelRenderer.getLightColor(level, pos.relative(positive)),
                LevelRenderer.getLightColor(level, pos.relative(negative))
        );
    }

    private static int maxPackedLight(
            int first,
            int... rest
    ) {
        int result =
                first;

        for (int packedLight : rest) {
            int block =
                    Math.max(
                            result & 0xFFFF,
                            packedLight & 0xFFFF
                    );

            int sky =
                    Math.max(
                            result >>> 16,
                            packedLight >>> 16
                    );

            result =
                    block | sky << 16;
        }

        return result;
    }
}
