package dev.createmechanicaldrive.content.shaft_distributor;

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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

public class ShaftDistributorRenderer
        extends KineticBlockEntityRenderer<ShaftDistributorBlockEntity> {

    private static final PartialModel FRAME_MODEL =
            PartialModel.of(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/distributor_shaft/shaft_distributor_frame"
                    )
            );
    private static final PartialModel FOUR_WAY_FRAME_MODEL =
            PartialModel.of(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/distributor_shaft/shaft_distributor_double_frame"
                    )
            );
    private static final PartialModel INPUT_SHAFT_MODEL =
            PartialModel.of(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/distributor_shaft/shaft_distributor_input_shaft"
                    )
            );

    private static final PartialModel HORIZONTAL_SHAFT_MODEL =
            PartialModel.of(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/distributor_shaft/shaft_distributor_horizontal_shaft"
                    )
            );

    private final BlockRenderDispatcher blockRenderer;

    public ShaftDistributorRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);
        blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    protected void renderSafe(
            ShaftDistributorBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state =
                blockEntity.getBlockState();

        Direction inputSide =
                state.getValue(ShaftDistributorBlock.FACING);

        Direction outputSide =
                ShaftDistributorBlock.getOutputDirection(state);

        boolean fourWay =
                state.getBlock() instanceof FourWayShaftDistributorBlock;

        Quaternionf modelRotation =
                getModelRotation(
                        inputSide,
                        outputSide
                );

        int renderLight =
                getRenderLight(
                        blockEntity,
                        packedLight
                );

        renderFrame(
                fourWay ? FOUR_WAY_FRAME_MODEL : FRAME_MODEL,
                blockEntity,
                modelRotation,
                poseStack,
                buffer,
                renderLight,
                packedOverlay
        );
        renderPart(
                INPUT_SHAFT_MODEL,
                blockEntity,
                modelRotation,
                inputSide.getAxis(),
                Axis.ZP,
                ShaftDistributorBlock.getInputAxisSign(state),
                blockEntity.getVisualSpeedForSide(inputSide),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        if (fourWay) {
            Direction oppositeInputSide =
                    inputSide.getOpposite();

            renderPart(
                    INPUT_SHAFT_MODEL,
                    blockEntity,
                    getModelRotation(oppositeInputSide, outputSide),
                    oppositeInputSide.getAxis(),
                    Axis.ZP,
                    -ShaftDistributorBlock.getInputAxisSign(state),
                    blockEntity.getVisualSpeedForSide(oppositeInputSide),
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );
        }

        renderPart(
                HORIZONTAL_SHAFT_MODEL,
                blockEntity,
                modelRotation,
                outputSide.getAxis(),
                Axis.XP,
                ShaftDistributorBlock.getOutputAxisSign(state),
                blockEntity.getVisualSpeedForSide(outputSide),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
    }

    private void renderFrame(
            PartialModel model,
            ShaftDistributorBlockEntity blockEntity,
            Quaternionf modelRotation,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state =
                blockEntity.getBlockState();

        RenderType renderType =
                getRenderType(
                        blockEntity,
                        state
                );

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(modelRotation);
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        if (blockEntity.getLevel() != null
                && SableSubLevelHelper.getSubLevel(
                blockEntity.getLevel(),
                blockEntity.getBlockPos()
        ) != null) {
            BakedModel bakedModel = model.get();
            VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
            blockRenderer.getModelRenderer().renderModel(
                    poseStack.last(),
                    vertexConsumer,
                    state,
                    bakedModel,
                    1.0F,
                    1.0F,
                    1.0F,
                    packedLight,
                    packedOverlay,
                    ModelData.EMPTY,
                    renderType
            );
        } else {
            CachedBuffers.partial(model, state)
                    .reset()
                    .light(packedLight)
                    .renderInto(
                            poseStack,
                            buffer.getBuffer(renderType)
                    );
        }

        poseStack.popPose();
    }

    private void renderPart(
            PartialModel model,
            ShaftDistributorBlockEntity blockEntity,
            Quaternionf modelRotation,
            Direction.Axis worldAxis,
            Axis rotationAxis,
            float axisSign,
            float speed,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state =
                blockEntity.getBlockState();

        RenderType renderType =
                getRenderType(
                        blockEntity,
                        state
                );

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(modelRotation);
        poseStack.mulPose(
                rotationAxis.rotation(
                        getAngleForSpeed(
                                blockEntity,
                                worldAxis,
                                axisSign,
                                speed
                        )
                )
        );
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        int shaftLight =
                getShaftLight(
                        blockEntity,
                        worldAxis,
                        packedLight
                );

        CachedBuffers.partial(model, state)
                .reset()
                .light(shaftLight)
                .renderInto(
                        poseStack,
                        buffer.getBuffer(renderType)
                );

        poseStack.popPose();
    }

    private static Quaternionf getModelRotation(
            Direction inputSide,
            Direction outputSide
    ) {
        float outputX =
                outputSide.getStepX();
        float outputY =
                outputSide.getStepY();
        float outputZ =
                outputSide.getStepZ();

        float localZWorldX =
                -inputSide.getStepX();
        float localZWorldY =
                -inputSide.getStepY();
        float localZWorldZ =
                -inputSide.getStepZ();

        float localYWorldX =
                localZWorldY * outputZ
                        - localZWorldZ * outputY;
        float localYWorldY =
                localZWorldZ * outputX
                        - localZWorldX * outputZ;
        float localYWorldZ =
                localZWorldX * outputY
                        - localZWorldY * outputX;

        Matrix3f basis =
                new Matrix3f()
                        .setColumn(
                                0,
                                outputX,
                                outputY,
                                outputZ
                        )
                        .setColumn(
                                1,
                                localYWorldX,
                                localYWorldY,
                                localYWorldZ
                        )
                        .setColumn(
                                2,
                                localZWorldX,
                                localZWorldY,
                                localZWorldZ
                        );

        return new Quaternionf()
                .setFromNormalized(basis);
    }

    private static float getAngleForSpeed(
            ShaftDistributorBlockEntity blockEntity,
            Direction.Axis worldAxis,
            float axisSign,
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
                                worldAxis
                        );

        return (((renderTime * speed * 3.0F / 10.0F + offset)
                * axisSign) % 360.0F)
                / 180.0F
                * Mth.PI;
    }

    private static int getShaftLight(
            ShaftDistributorBlockEntity blockEntity,
            Direction.Axis axis,
            int fallbackLight
    ) {
        Level level =
                blockEntity.getLevel();

        if (level == null) {
            return fallbackLight;
        }

        if (SableSubLevelHelper.getSubLevel(
                level,
                blockEntity.getBlockPos()
        ) != null) {
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

    private static int getRenderLight(
            ShaftDistributorBlockEntity blockEntity,
            int fallbackLight
    ) {
        Level level =
                blockEntity.getLevel();

        if (level == null) {
            return fallbackLight;
        }

        BlockPos localPos =
                blockEntity.getBlockPos();

        if (SableSubLevelHelper.getSubLevel(level, localPos) == null) {
            return maxPackedLight(
                    fallbackLight,
                    LevelRenderer.getLightColor(level, localPos)
            );
        }

        Vec3 worldCenter =
                SableSubLevelHelper.getWorldCenter(level, localPos);

        BlockPos worldPos =
                BlockPos.containing(worldCenter);

        int result =
                maxPackedLight(
                        fallbackLight,
                        LevelRenderer.getLightColor(level, worldPos)
                );

        for (Direction direction : Direction.values()) {
            result = maxPackedLight(
                    result,
                    LevelRenderer.getLightColor(
                            level,
                            worldPos.relative(direction)
                    )
            );
        }

        return result;
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
