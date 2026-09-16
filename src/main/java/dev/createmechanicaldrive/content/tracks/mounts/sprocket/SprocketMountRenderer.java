package dev.createmechanicaldrive.content.tracks.mounts.sprocket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerRenderer;
import dev.createmechanicaldrive.content.tracks.chain.TrackAssemblyRenderer;
import dev.createmechanicaldrive.content.tracks.chain.TrackKineticVisuals;
import dev.createmechanicaldrive.content.tracks.wheels.sprocket.SprocketWheelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SprocketMountRenderer
        extends KineticBlockEntityRenderer<SprocketMountBlockEntity> {
    private static final float MOUNT_THICKNESS = 3.0F / 16.0F;

    public SprocketMountRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(
            SprocketMountBlockEntity mount,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack attachment = mount.getAttachment();
        if (!SprocketMountAttachmentShapes.supports(attachment)) {
            return;
        }

        BlockState state = mount.getBlockState();
        Direction output = state.getValue(
                SprocketMountBlock.HORIZONTAL_FACING
        );

        int attachmentLight = getAttachmentLight(
                mount,
                output,
                packedLight
        );

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(new Quaternionf().rotationTo(
                new Vector3f(0.0F, 0.0F, -1.0F),
                new Vector3f(
                        output.getStepX(),
                        output.getStepY(),
                        output.getStepZ()
                )
        ));
        poseStack.translate(0.0F, 0.0F, -MOUNT_THICKNESS);
        poseStack.mulPose(Axis.ZP.rotation(
                TrackKineticVisuals.sprocketAngle(mount, partialTicks)
        ));

        ShaftMarkerRenderer.renderInWheelMount(
                attachment,
                state,
                poseStack,
                buffers,
                attachmentLight
        );
        SprocketWheelRenderer.renderInMount(
                attachment,
                state,
                poseStack,
                buffers,
                attachmentLight
        );
        poseStack.popPose();

        TrackAssemblyRenderer.render(
                mount,
                partialTicks,
                poseStack,
                buffers,
                attachmentLight
        );
    }

    private static int getAttachmentLight(
            SprocketMountBlockEntity mount,
            Direction output,
            int fallbackLight
    ) {
        Level level = mount.getLevel();
        if (level == null) {
            return fallbackLight;
        }

        BlockPos pos = mount.getBlockPos();

        int centerLight = LevelRenderer.getLightColor(
                level,
                pos
        );

        int frontLight = LevelRenderer.getLightColor(
                level,
                pos.relative(output)
        );

        return maxPackedLight(
                fallbackLight,
                centerLight,
                frontLight
        );
    }

    private static int maxPackedLight(
            int first,
            int... rest
    ) {
        int result = first;

        for (int packedLight : rest) {
            int blockLight = Math.max(
                    result & 0xFFFF,
                    packedLight & 0xFFFF
            );

            int skyLight = Math.max(
                    result >>> 16,
                    packedLight >>> 16
            );

            result = blockLight | (skyLight << 16);
        }

        return result;
    }
}
