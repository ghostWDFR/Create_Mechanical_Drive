package dev.createmechanicaldrive.content.worm_gears;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class WormGearSmallRenderer
        extends KineticBlockEntityRenderer<
        WormGearSmallBlockEntity
        > {

    private static final PartialModel ENCASED_GEAR_MODEL =
            PartialModel.of(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/worm_gear/worm_gear_small_encased"
                    )
            );

    private final BlockRenderDispatcher blockRenderer;

    public WormGearSmallRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);

        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    protected void renderSafe(
            WormGearSmallBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state =
                blockEntity.getBlockState();

        if (state.getValue(WormGearSmallBlock.ENCASED)) {
            Direction.Axis axis =
                    state.getValue(
                            WormGearSmallBlock.AXIS
                    );

            renderCasing(
                    blockEntity,
                    state,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );

            renderEncasedGear(
                    blockEntity,
                    state,
                    axis,
                    poseStack,
                    buffer,
                    packedLight
            );

            return;
        }

        RenderType renderType =
                getRenderType(
                        blockEntity,
                        state
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
    }

    private void renderEncasedGear(
            WormGearSmallBlockEntity blockEntity,
            BlockState state,
            Direction.Axis axis,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        SuperByteBuffer gear =
                CachedBuffers.partial(
                                ENCASED_GEAR_MODEL,
                                state
                        )
                        .reset();

        poseStack.pushPose();

        poseStack.translate(
                0.5F,
                0.5F,
                0.5F
        );

        alignModelAxis(
                poseStack,
                axis
        );

        poseStack.mulPose(
                Axis.YP.rotation(
                        KineticBlockEntityRenderer
                                .getAngleForBe(
                                        blockEntity,
                                        blockEntity.getBlockPos(),
                                        axis
                                )
                )
        );

        poseStack.translate(
                -0.5F,
                -0.5F,
                -0.5F
        );

        gear.light(
                        packedLight
                )
                .renderInto(
                        poseStack,
                        bufferSource.getBuffer(
                                RenderType.cutout()
                        )
                );

        poseStack.popPose();
    }

    private void renderCasing(
            WormGearSmallBlockEntity blockEntity,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BakedModel casingModel =
                blockRenderer.getBlockModel(
                        state
                );

        RenderType renderType =
                RenderType.cutout();

        VertexConsumer vertexConsumer =
                bufferSource.getBuffer(
                        renderType
                );

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
                        null,
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

    private static void alignModelAxis(
            PoseStack poseStack,
            Direction.Axis axis
    ) {
        switch (axis) {
            case X ->
                    poseStack.mulPose(
                            Axis.ZP.rotationDegrees(
                                    -90.0F
                            )
                    );

            case Z ->
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(
                                    90.0F
                            )
                    );

            case Y -> {
            }
        }
    }

}
