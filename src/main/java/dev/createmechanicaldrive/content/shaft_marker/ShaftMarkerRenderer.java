package dev.createmechanicaldrive.content.shaft_marker;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

public class ShaftMarkerRenderer
        extends KineticBlockEntityRenderer<ShaftMarkerBlockEntity> {
    private static final Map<DyeColor, PartialModel> MODELS = createModels();

    public ShaftMarkerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    public static ModelResourceLocation modelLocation(DyeColor color) {
        return ModelResourceLocation.standalone(
                modelResourceLocation(color)
        );
    }

    private static ResourceLocation modelResourceLocation(DyeColor color) {
        return ResourceLocation.fromNamespaceAndPath(
                CreateMechanicalDrive.MOD_ID,
                "block/shaft_marker/shaft_marker_" + color.getName()
        );
    }

    private static Map<DyeColor, PartialModel> createModels() {
        Map<DyeColor, PartialModel> models = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            models.put(color, PartialModel.of(modelResourceLocation(color)));
        }
        return models;
    }

    public static void renderInWheelMount(
            ItemStack stack,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        if (!ShaftMarkerItem.isMarker(stack)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        CachedBuffers.partial(
                MODELS.get(ShaftMarkerItem.getColor(stack)),
                state
        ).light(packedLight).renderInto(
                poseStack,
                buffers.getBuffer(RenderType.cutout())
        );
        poseStack.popPose();
    }

    public static void renderAlongPositiveY(
            ItemStack stack,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        if (!ShaftMarkerItem.isMarker(stack)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -0.5F);
        CachedBuffers.partial(
                MODELS.get(ShaftMarkerItem.getColor(stack)),
                state
        ).light(packedLight).renderInto(
                poseStack,
                buffers.getBuffer(RenderType.cutout())
        );
        poseStack.popPose();
    }

    @Override
    protected void renderSafe(
            ShaftMarkerBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(ShaftMarkerBlock.FACING);
        DyeColor color = state.getValue(ShaftMarkerBlock.COLOR);
        Direction.Axis axis = facing.getAxis();
        SuperByteBuffer marker = CachedBuffers.partial(
                MODELS.get(color),
                state
        ).reset();

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        rotateAroundPositiveAxis(
                poseStack,
                axis,
                KineticBlockEntityRenderer.getAngleForBe(
                        blockEntity,
                        blockEntity.getBlockPos(),
                        axis
                )
        );
        poseStack.mulPose(new Quaternionf().rotationTo(
                new Vector3f(0.0F, 1.0F, 0.0F),
                new Vector3f(
                        facing.getStepX(),
                        facing.getStepY(),
                        facing.getStepZ()
                )
        ));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        marker.light(packedLight).renderInto(
                poseStack,
                bufferSource.getBuffer(RenderType.cutout())
        );
        poseStack.popPose();
    }

    private static void rotateAroundPositiveAxis(
            PoseStack poseStack,
            Direction.Axis axis,
            float angle
    ) {
        switch (axis) {
            case X -> poseStack.mulPose(Axis.XP.rotation(angle));
            case Y -> poseStack.mulPose(Axis.YP.rotation(angle));
            case Z -> poseStack.mulPose(Axis.ZP.rotation(angle));
        }
    }
}
