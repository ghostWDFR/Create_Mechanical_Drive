package dev.createmechanicaldrive.content.engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.createmechanicaldrive.CreateMechanicalDriveClient;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.List;

public class EngineRenderer
        extends KineticBlockEntityRenderer<EngineBlockEntity> {

    private static final float PIPE_MOVEMENT_DISTANCE =
            0.25F / 16.0F;

    private static final float PIPE_OSCILLATIONS_PER_SECOND =
            2.0F;

    private final BlockRenderDispatcher blockRenderer;

    public EngineRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);

        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    protected void renderSafe(
            EngineBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        renderShaft(
                blockEntity,
                poseStack,
                bufferSource,
                packedLight
        );

        renderPipes(
                blockEntity,
                partialTicks,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
    }

    private void renderShaft(
            EngineBlockEntity blockEntity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Direction.Axis axis =
                blockEntity.getBlockState()
                        .getValue(
                                EngineBlock.FACING
                        )
                        .getAxis();

        renderShaftHalf(
                blockEntity,
                poseStack,
                bufferSource,
                packedLight,
                axis,
                false
        );

        renderShaftHalf(
                blockEntity,
                poseStack,
                bufferSource,
                packedLight,
                axis,
                true
        );
    }

    private void renderShaftHalf(
            EngineBlockEntity blockEntity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            Direction.Axis axis,
            boolean positiveHalf
    ) {
        BlockState shaftState =
                KineticBlockEntityRenderer.shaft(
                        axis
                );

        RenderType renderType =
                getRenderType(
                        blockEntity,
                        shaftState
                );

        SuperByteBuffer shaft =
                getRotatedModel(
                        blockEntity,
                        shaftState
                );

        poseStack.pushPose();

        if (axis == Direction.Axis.X) {
            poseStack.translate(
                    positiveHalf ? 9.0F / 16.0F : 0.0F,
                    0.0F,
                    0.0F
            );

            poseStack.scale(
                    7.0F / 16.0F,
                    1.0F,
                    1.0F
            );
        } else {
            poseStack.translate(
                    0.0F,
                    0.0F,
                    positiveHalf ? 9.0F / 16.0F : 0.0F
            );

            poseStack.scale(
                    1.0F,
                    1.0F,
                    7.0F / 16.0F
            );
        }

        KineticBlockEntityRenderer.renderRotatingBuffer(
                blockEntity,
                shaft,
                poseStack,
                bufferSource.getBuffer(
                        renderType
                ),
                getShaftLight(
                        blockEntity,
                        axis,
                        packedLight
                )
        );

        poseStack.popPose();
    }

    private static float getPipeYOffset(
            EngineBlockEntity blockEntity,
            float partialTicks
    ) {
        if (!blockEntity.isRunning()) {
            return 0.0F;
        }

        Level level =
                blockEntity.getLevel();

        if (level == null) {
            return 0.0F;
        }

        double timeSeconds =
                (level.getGameTime()
                        + partialTicks)
                        / 20.0D;

        double phase =
                timeSeconds
                        * PIPE_OSCILLATIONS_PER_SECOND
                        * Math.PI
                        * 2.0D;

        float wave =
                (float) (
                        (Math.sin(phase)
                                + 1.0D)
                                * 0.5D
                );

        return -PIPE_MOVEMENT_DISTANCE
                * wave;
    }

    private void renderPipes(
            EngineBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Direction facing =
                blockEntity.getBlockState()
                        .getValue(
                                EngineBlock.FACING
                        );

        List<EngineBlockEntity> group =
                blockEntity.getEngineGroup();

        boolean extendedPipes =
                group.size() > 1
                        && !blockEntity.isFirstEngineInGroup();

        boolean heated =
                blockEntity.isRunning();

        BakedModel pipesModel =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(
                                extendedPipes
                                        ? heated
                                        ? CreateMechanicalDriveClient
                                        .ENGINE_PIPES_EXTENDED_HEATED_MODEL
                                        : CreateMechanicalDriveClient
                                        .ENGINE_PIPES_EXTENDED_MODEL
                                        : heated
                                        ? CreateMechanicalDriveClient
                                        .ENGINE_PIPES_HEATED_MODEL
                                        : CreateMechanicalDriveClient
                                        .ENGINE_PIPES_MODEL
                        );

        RenderType renderType =
                RenderType.cutout();

        VertexConsumer buffer =
                bufferSource.getBuffer(
                        renderType
                );

        poseStack.pushPose();

        poseStack.translate(
                0.0F,
                getPipeYOffset(
                        blockEntity,
                        partialTicks
                ),
                0.0F
        );

        rotateToFacing(
                poseStack,
                facing
        );

        blockRenderer
                .getModelRenderer()
                .renderModel(
                        poseStack.last(),
                        buffer,
                        null,
                        pipesModel,
                        1.0F,
                        1.0F,
                        1.0F,
                        packedLight,
                        packedOverlay,
                        ModelData.EMPTY,
                        renderType
                );

        poseStack.popPose();
    }

    private static int getShaftLight(
            EngineBlockEntity blockEntity,
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
                Direction.get(
                        Direction.AxisDirection.NEGATIVE,
                        axis
                );

        int centerLight =
                LevelRenderer.getLightColor(
                        level,
                        pos
                );

        int positiveLight =
                LevelRenderer.getLightColor(
                        level,
                        pos.relative(
                                positive
                        )
                );

        int negativeLight =
                LevelRenderer.getLightColor(
                        level,
                        pos.relative(
                                negative
                        )
                );

        return maxPackedLight(
                fallbackLight,
                centerLight,
                positiveLight,
                negativeLight
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

    private static void rotateToFacing(
            PoseStack poseStack,
            Direction facing
    ) {
        float degrees =
                switch (facing) {
                    case EAST -> 90.0F;
                    case SOUTH -> 180.0F;
                    case WEST -> 270.0F;
                    case UP, DOWN, NORTH -> 0.0F;
                };

        if (degrees == 0.0F) {
            return;
        }

        poseStack.translate(
                0.5F,
                0.0F,
                0.5F
        );

        poseStack.mulPose(
                new Quaternionf(
                        new AxisAngle4f(
                                (float) Math.toRadians(
                                        degrees
                                ),
                                0.0F,
                                1.0F,
                                0.0F
                        )
                )
        );

        poseStack.translate(
                -0.5F,
                0.0F,
                -0.5F
        );
    }
}