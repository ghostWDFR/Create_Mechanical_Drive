package dev.createmechanicaldrive.content.tracks.wheels.support;

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

public final class SupportWheelRenderer {
    private static final float AXLE_OFFSET = 4.0F / 16.0F;
    private static final ResourceLocation MODEL_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/wheels/support_wheel/support_wheel"
            );
    private static final PartialModel MODEL = PartialModel.of(MODEL_RESOURCE);

    private SupportWheelRenderer() {
    }

    public static ModelResourceLocation modelLocation() {
        return ModelResourceLocation.standalone(MODEL_RESOURCE);
    }

    public static void renderInTorsionMount(
            ItemStack stack,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            float wheelAngle
    ) {
        if (!SupportWheelItem.isSupportWheel(stack)) {
            return;
        }

        poseStack.pushPose();
        // Move the model's 8,8,8 centre onto the requested 8,8,12 axle.
        poseStack.translate(0.0F, 0.0F, AXLE_OFFSET);
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotation(wheelAngle));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        CachedBuffers.partial(MODEL, state)
                .light(packedLight)
                .renderInto(
                        poseStack,
                        buffers.getBuffer(RenderType.cutout())
                );
        poseStack.popPose();
    }
}
