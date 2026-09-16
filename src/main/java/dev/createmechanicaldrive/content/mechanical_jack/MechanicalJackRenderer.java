package dev.createmechanicaldrive.content.mechanical_jack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.CreateMechanicalDriveClient;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MechanicalJackRenderer
        extends KineticBlockEntityRenderer<MechanicalJackBlockEntity> {

    private static final PartialModel INPUT_SHAFT_MODEL =
            PartialModel.of(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/mechanical_jack/mechanical_jack_input_shaft"
                    )
            );

    private final BlockRenderDispatcher blockRenderer;

    public MechanicalJackRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);

        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    protected void renderSafe(
            MechanicalJackBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Direction facing =
                blockEntity
                        .getBlockState()
                        .getValue(
                                MechanicalJackBlock.FACING
                        );

        Direction mount =
                blockEntity
                        .getBlockState()
                        .getValue(
                                MechanicalJackBlock.MOUNT
                        );

        if (blockEntity.hasFullBlockCollision()
                && blockEntity.getLevel() != null) {
            renderStaticBlock(
                    blockEntity,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    facing,
                    mount
            );

            return;
        }

        float shaftExtension =
                blockEntity
                        .getInterpolatedExtension(
                                partialTick
                        );

        Vec3 shaftOffset =
                new Vec3(
                        mount.getStepX() * shaftExtension,
                        mount.getStepY() * shaftExtension,
                        mount.getStepZ() * shaftExtension
                );

        Vec3 topOffset =
                MechanicalJackRenderOffsetHelper
                        .getRenderedHeadOffset(
                                blockEntity,
                                mount,
                                partialTick
                        );

        if (topOffset == null) {
            topOffset =
                    shaftOffset;
        }

        BakedModel base =
                getModel(
                        CreateMechanicalDriveClient
                                .MECHANICAL_JACK_BASE_MODEL
                );


        BakedModel shaft1 =
                getModel(
                        CreateMechanicalDriveClient
                                .MECHANICAL_JACK_SHAFT_1_MODEL
                );

        BakedModel shaft2 =
                getModel(
                        CreateMechanicalDriveClient
                                .MECHANICAL_JACK_SHAFT_2_MODEL
                );

        BakedModel top =
                getModel(
                        CreateMechanicalDriveClient
                                .MECHANICAL_JACK_TOP_MODEL
                );

        VertexConsumer buffer =
                bufferSource
                        .getBuffer(
                                RenderType.cutout()
                        );

        renderPart(
                base,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                facing,
                mount,
                Vec3.ZERO
        );

        renderInputShaft(
                blockEntity,
                poseStack,
                bufferSource,
                packedLight,
                facing,
                mount
        );

        renderPart(
                shaft1,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                facing,
                mount,
                shaftOffset.scale(0.375D)
        );

        renderPart(
                shaft2,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                facing,
                mount,
                shaftOffset.scale(0.625D)
        );

        renderPart(
                top,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                facing,
                mount,
                topOffset
        );
    }

    private static float getInputShaftAngle(
            MechanicalJackBlockEntity blockEntity,
            Direction inputSide
    ) {
        Direction.Axis inputAxis =
                inputSide.getAxis();

        float angle =
                KineticBlockEntityRenderer
                        .getAngleForBe(
                                blockEntity,
                                blockEntity
                                        .getBlockPos()
                                        .relative(
                                                inputSide
                                        ),
                                inputAxis
                        );

        if (inputSide == Direction.NORTH
                || inputSide == Direction.WEST) {
            angle = -angle;
        }

        return angle;
    }

    private void renderStaticBlock(
            MechanicalJackBlockEntity blockEntity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            Direction facing,
            Direction mount
    ) {
        BlockState state =
                blockEntity.getBlockState();

        Direction inputSide =
                MechanicalJackBlock
                        .getInputSide(
                                state
                        );

        float inputAngle =
                getInputShaftAngle(
                        blockEntity,
                        inputSide
                );

        VertexConsumer buffer =
                bufferSource
                        .getBuffer(
                                RenderType.cutout()
                        );

        renderStaticPart(
                blockEntity,
                getModel(
                        CreateMechanicalDriveClient
                                .MECHANICAL_JACK_BASE_MODEL
                ),
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                facing,
                mount,
                0.0F,
                Vec3.ZERO
        );

        renderStaticPart(
                blockEntity,
                getModel(
                        CreateMechanicalDriveClient
                                .MECHANICAL_JACK_INPUT_SHAFT_MODEL
                ),
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                facing,
                mount,
                inputAngle,
                Vec3.ZERO
        );

        renderStaticPart(
                blockEntity,
                getModel(
                        CreateMechanicalDriveClient
                                .MECHANICAL_JACK_SHAFT_1_MODEL
                ),
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                facing,
                mount,
                0.0F,
                Vec3.ZERO
        );

        renderStaticPart(
                blockEntity,
                getModel(
                        CreateMechanicalDriveClient
                                .MECHANICAL_JACK_SHAFT_2_MODEL
                ),
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                facing,
                mount,
                0.0F,
                Vec3.ZERO
        );

        renderStaticPart(
                blockEntity,
                getModel(
                        CreateMechanicalDriveClient
                                .MECHANICAL_JACK_TOP_MODEL
                ),
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                facing,
                mount,
                0.0F,
                Vec3.ZERO
        );
    }

    private void renderInputShaft(
            MechanicalJackBlockEntity blockEntity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            Direction facing,
            Direction mount
    ) {
        BlockState state =
                blockEntity.getBlockState();

        Direction inputSide =
                MechanicalJackBlock
                        .getInputSide(
                                state
                        );

        Direction.Axis inputAxis =
                inputSide.getAxis();

        SuperByteBuffer inputShaft =
                CachedBuffers
                        .partial(
                                INPUT_SHAFT_MODEL,
                                state
                        )
                        .reset();

        poseStack.pushPose();

        poseStack.translate(
                0.5F,
                0.5F,
                0.5F
        );

        applyMountRotation(
                poseStack,
                mount
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        rotationForFacing(
                                facing
                        )
                )
        );

        poseStack.translate(
                -0.5F,
                -0.5F,
                -0.5F
        );

        float inputAngle =
                getInputShaftAngle(
                        blockEntity,
                        inputSide
                );

        KineticBlockEntityRenderer
                .kineticRotationTransform(
                        inputShaft,
                        blockEntity,
                        Direction.Axis.Z,
                        inputAngle,
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

    private void renderStaticPart(
            MechanicalJackBlockEntity blockEntity,
            BakedModel model,
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            Direction facing,
            Direction mount,
            float localZRotation,
            Vec3 offset
    ) {
        poseStack.pushPose();

        poseStack.translate(
                offset.x,
                offset.y,
                offset.z
        );

        poseStack.translate(
                0.5F,
                0.5F,
                0.5F
        );

        applyMountRotation(
                poseStack,
                mount
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        rotationForFacing(
                                facing
                        )
                )
        );

        if (localZRotation != 0.0F) {
            poseStack.mulPose(
                    Axis.ZP.rotationDegrees(
                            localZRotation
                    )
            );
        }

        poseStack.translate(
                -0.5F,
                -0.5F,
                -0.5F
        );

        renderModelWithDirectionalShade(
                blockEntity.getLevel(),
                model,
                poseStack.last(),
                buffer,
                packedLight,
                packedOverlay,
                facing,
                mount
        );

        poseStack.popPose();
    }

    private static void renderModelWithDirectionalShade(
            Level level,
            BakedModel model,
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            Direction facing,
            Direction mount
    ) {
        RandomSource random =
                RandomSource.create();

        for (Direction side : Direction.values()) {
            random.setSeed(
                    42L
            );

            renderQuadListWithDirectionalShade(
                    level,
                    model.getQuads(
                            null,
                            side,
                            random,
                            ModelData.EMPTY,
                            RenderType.cutout()
                    ),
                    pose,
                    buffer,
                    packedLight,
                    packedOverlay,
                    facing,
                    mount
            );
        }

        random.setSeed(
                42L
        );

        renderQuadListWithDirectionalShade(
                level,
                model.getQuads(
                        null,
                        null,
                        random,
                        ModelData.EMPTY,
                        RenderType.cutout()
                ),
                pose,
                buffer,
                packedLight,
                packedOverlay,
                facing,
                mount
        );
    }

    private static void renderQuadListWithDirectionalShade(
            Level level,
            java.util.List<BakedQuad> quads,
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            Direction facing,
            Direction mount
    ) {
        for (BakedQuad quad : quads) {
            Direction worldDirection =
                    transformDirection(
                            quad.getDirection(),
                            facing,
                            mount
                    );

            float shade =
                    quad.isShade() && level != null
                            ? level.getShade(
                            worldDirection,
                            true
                    )
                            : 1.0F;

            buffer.putBulkData(
                    pose,
                    quad,
                    shade,
                    shade,
                    shade,
                    1.0F,
                    packedLight,
                    packedOverlay
            );
        }
    }

    private void renderPart(
            BakedModel model,
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            Direction facing,
            Direction mount,
            Vec3 offset
    ) {
        poseStack.pushPose();

        poseStack.translate(
                offset.x,
                offset.y,
                offset.z
        );

        poseStack.translate(
                0.5F,
                0.5F,
                0.5F
        );

        applyMountRotation(
                poseStack,
                mount
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        rotationForFacing(
                                facing
                        )
                )
        );

        poseStack.translate(
                -0.5F,
                -0.5F,
                -0.5F
        );

        blockRenderer
                .getModelRenderer()
                .renderModel(
                        poseStack.last(),
                        buffer,
                        null,
                        model,
                        1.0F,
                        1.0F,
                        1.0F,
                        packedLight,
                        packedOverlay,
                        ModelData.EMPTY,
                        RenderType.cutout()
                );

        poseStack.popPose();
    }

    private static void applyMountRotation(
            PoseStack poseStack,
            Direction mount
    ) {
        switch (mount) {
            case DOWN ->
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(
                                    180.0F
                            )
                    );

            case NORTH ->
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(
                                    -90.0F
                            )
                    );

            case SOUTH ->
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(
                                    90.0F
                            )
                    );

            case EAST ->
                    poseStack.mulPose(
                            Axis.ZP.rotationDegrees(
                                    -90.0F
                            )
                    );

            case WEST ->
                    poseStack.mulPose(
                            Axis.ZP.rotationDegrees(
                                    90.0F
                            )
                    );

            case UP -> {
            }
        }
    }

    private static Direction transformDirection(
            Direction direction,
            Direction facing,
            Direction mount
    ) {
        return rotateDirectionForMount(
                rotateDirectionForFacing(
                        direction,
                        facing
                ),
                mount
        );
    }

    private static Direction rotateDirectionForFacing(
            Direction direction,
            Direction facing
    ) {
        return switch (facing) {
            case EAST ->
                    switch (direction) {
                        case NORTH -> Direction.EAST;
                        case EAST -> Direction.SOUTH;
                        case SOUTH -> Direction.WEST;
                        case WEST -> Direction.NORTH;
                        default -> direction;
                    };

            case SOUTH ->
                    switch (direction) {
                        case NORTH -> Direction.SOUTH;
                        case EAST -> Direction.WEST;
                        case SOUTH -> Direction.NORTH;
                        case WEST -> Direction.EAST;
                        default -> direction;
                    };

            case WEST ->
                    switch (direction) {
                        case NORTH -> Direction.WEST;
                        case EAST -> Direction.NORTH;
                        case SOUTH -> Direction.EAST;
                        case WEST -> Direction.SOUTH;
                        default -> direction;
                    };

            default -> direction;
        };
    }

    private static Direction rotateDirectionForMount(
            Direction direction,
            Direction mount
    ) {
        if (mount == Direction.UP) {
            return direction;
        }

        return switch (mount) {
            case DOWN ->
                    switch (direction) {
                        case NORTH -> Direction.SOUTH;
                        case SOUTH -> Direction.NORTH;
                        case EAST -> Direction.EAST;
                        case WEST -> Direction.WEST;
                        case UP -> Direction.DOWN;
                        case DOWN -> Direction.UP;
                    };

            case NORTH ->
                    switch (direction) {
                        case NORTH -> Direction.DOWN;
                        case SOUTH -> Direction.UP;
                        case EAST -> Direction.EAST;
                        case WEST -> Direction.WEST;
                        case UP -> Direction.NORTH;
                        case DOWN -> Direction.SOUTH;
                    };

            case SOUTH ->
                    switch (direction) {
                        case NORTH -> Direction.UP;
                        case SOUTH -> Direction.DOWN;
                        case EAST -> Direction.EAST;
                        case WEST -> Direction.WEST;
                        case UP -> Direction.SOUTH;
                        case DOWN -> Direction.NORTH;
                    };

            case EAST ->
                    switch (direction) {
                        case NORTH -> Direction.NORTH;
                        case SOUTH -> Direction.SOUTH;
                        case EAST -> Direction.DOWN;
                        case WEST -> Direction.UP;
                        case UP -> Direction.EAST;
                        case DOWN -> Direction.WEST;
                    };

            case WEST ->
                    switch (direction) {
                        case NORTH -> Direction.NORTH;
                        case SOUTH -> Direction.SOUTH;
                        case EAST -> Direction.UP;
                        case WEST -> Direction.DOWN;
                        case UP -> Direction.WEST;
                        case DOWN -> Direction.EAST;
                    };

            default -> direction;
        };
    }

    private static BakedModel getModel(
            net.minecraft.client.resources.model.ModelResourceLocation location
    ) {
        return Minecraft
                .getInstance()
                .getModelManager()
                .getModel(
                        location
                );
    }

    private static float rotationForFacing(
            Direction facing
    ) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }
}