package dev.createmechanicaldrive.content.tracks.wheels.sprocket;

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

public final class SprocketWheelRenderer {
    private static final float TOWARD_MOUNT_OFFSET = 3.0F / 16.0F;
    private static final ResourceLocation MODEL_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/wheels/sprocket/sprocket_wheel"
            );
    private static final PartialModel MODEL = PartialModel.of(MODEL_RESOURCE);

    private SprocketWheelRenderer() {
    }

    public static ModelResourceLocation modelLocation() {
        return ModelResourceLocation.standalone(MODEL_RESOURCE);
    }

    public static void renderInMount(
            ItemStack stack,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        if (!SprocketWheelItem.isSprocketWheel(stack)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, TOWARD_MOUNT_OFFSET);
        poseStack.translate(0.0F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        CachedBuffers.partial(MODEL, state)
                .light(packedLight)
                .renderInto(
                        poseStack,
                        buffers.getBuffer(RenderType.cutout())
                );
        poseStack.popPose();
    }
}
