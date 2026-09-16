package dev.createmechanicaldrive.content.tracks.wheels.drive;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class DriveWheelRenderer {
    private static final ResourceLocation MODEL_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/wheels/drive_wheel/drive_wheel"
            );
    private static final PartialModel MODEL = PartialModel.of(MODEL_RESOURCE);
    private static final ResourceLocation BIG_MODEL_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/wheels/drive_wheel/drive_wheel_big"
            );
    private static final PartialModel BIG_MODEL =
            PartialModel.of(BIG_MODEL_RESOURCE);
    private static final ResourceLocation BIG_INNER_MODEL_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/wheels/drive_wheel/drive_wheel_big_inner"
            );
    private static final PartialModel BIG_INNER_MODEL =
            PartialModel.of(BIG_INNER_MODEL_RESOURCE);
    private static final ResourceLocation BIG_OUTER_MODEL_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/wheels/drive_wheel/drive_wheel_big_outer"
            );
    private static final PartialModel BIG_OUTER_MODEL =
            PartialModel.of(BIG_OUTER_MODEL_RESOURCE);

    private DriveWheelRenderer() {
    }

    public static ModelResourceLocation modelLocation() {
        return ModelResourceLocation.standalone(MODEL_RESOURCE);
    }

    public static ModelResourceLocation bigModelLocation() {
        return ModelResourceLocation.standalone(BIG_MODEL_RESOURCE);
    }

    public static ModelResourceLocation bigInnerModelLocation() {
        return ModelResourceLocation.standalone(BIG_INNER_MODEL_RESOURCE);
    }

    public static ModelResourceLocation bigOuterModelLocation() {
        return ModelResourceLocation.standalone(BIG_OUTER_MODEL_RESOURCE);
    }

    public static void renderOnTorsionArm(
            ItemStack stack,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            float wheelAngle
    ) {
        renderOnTorsionArm(
                stack,
                state,
                poseStack,
                buffers,
                packedLight,
                wheelAngle,
                BigWheelLayer.SINGLE
        );
    }

    public static void renderOnTorsionArm(
            ItemStack stack,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            float wheelAngle,
            BigWheelLayer bigWheelLayer
    ) {
        if (!DriveWheelItem.isDriveWheel(stack)
                && !BigDriveWheelItem.isBigDriveWheel(stack)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotation(wheelAngle));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        CachedBuffers.partial(
                        BigDriveWheelItem.isBigDriveWheel(stack)
                                ? bigModel(bigWheelLayer)
                                : MODEL,
                        state
                )
                .light(packedLight)
                .renderInto(
                        poseStack,
                        buffers.getBuffer(RenderType.cutout())
                );
        poseStack.popPose();
    }

    private static PartialModel bigModel(BigWheelLayer layer) {
        return switch (layer) {
            case INNER -> BIG_INNER_MODEL;
            case OUTER -> BIG_OUTER_MODEL;
            case SINGLE -> BIG_MODEL;
        };
    }

    public enum BigWheelLayer {
        SINGLE,
        INNER,
        OUTER
    }
}
