package dev.createmechanicaldrive.content.tracks.mounts.idler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerRenderer;
import dev.createmechanicaldrive.content.tracks.chain.TrackAssemblyManager;
import dev.createmechanicaldrive.content.tracks.chain.TrackKineticVisuals;
import dev.createmechanicaldrive.content.tracks.wheels.idler.IdlerWheelRenderer;
import dev.createmechanicaldrive.client.SableContraptionDragTransform;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class IdlerMountRenderer
        extends SafeBlockEntityRenderer<IdlerMountBlockEntity> {
    private static final float MOUNT_THICKNESS = 3.0F / 16.0F;
    private static final ResourceLocation AXLE_MODEL_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/mounts/idler/idler_mount_axle"
            );
    private static final PartialModel AXLE_MODEL =
            PartialModel.of(AXLE_MODEL_RESOURCE);

    public IdlerMountRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }

    public static ModelResourceLocation axleModelLocation() {
        return ModelResourceLocation.standalone(AXLE_MODEL_RESOURCE);
    }

    @Override
    protected void renderSafe(
            IdlerMountBlockEntity mount,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = mount.getBlockState();
        Direction output = state.getValue(
                IdlerMountBlock.HORIZONTAL_FACING
        );
        ItemStack attachment = mount.getAttachment();

        int renderLight = getRenderLight(
                mount,
                output,
                packedLight
        );

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);

        Direction lateral = output.getClockWise();
        double axleOffset = mount.getAxleOffset();
        poseStack.translate(
                lateral.getStepX() * axleOffset,
                0.0D,
                lateral.getStepZ() * axleOffset
        );
        poseStack.mulPose(new Quaternionf().rotationTo(
                new Vector3f(0.0F, 0.0F, -1.0F),
                new Vector3f(
                        output.getStepX(),
                        output.getStepY(),
                        output.getStepZ()
                )
        ));
        poseStack.translate(0.0D, 0.0D, -MOUNT_THICKNESS);

        renderAxle(
                state,
                poseStack,
                buffers,
                renderLight
        );

        ShaftMarkerRenderer.renderInWheelMount(
                attachment,
                state,
                poseStack,
                buffers,
                renderLight
        );

        var trackOwner = TrackAssemblyManager.owner(
                mount.getLevel(),
                mount
        );
        float wheelAngle = trackOwner == null
                ? 0.0F
                : TrackKineticVisuals.idlerWheelAngle(
                        trackOwner,
                        partialTicks
                );
        IdlerWheelRenderer.renderInMount(
                attachment,
                state,
                poseStack,
                buffers,
                renderLight,
                wheelAngle
        );
        poseStack.popPose();
    }

    private static void renderAxle(
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, MOUNT_THICKNESS);
        poseStack.translate(0.0F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        CachedBuffers.partial(AXLE_MODEL, state)
                .light(packedLight)
                .renderInto(
                        poseStack,
                        buffers.getBuffer(RenderType.cutout())
                );
        poseStack.popPose();
    }

    private static int getRenderLight(
            IdlerMountBlockEntity mount,
            Direction output,
            int fallbackLight
    ) {
        Level level = mount.getLevel();
        if (level == null) {
            return fallbackLight;
        }

        BlockPos pos = mount.getBlockPos();

        if (SableSubLevelHelper.getSubLevel(level, pos) == null) {
            return maxPackedLight(
                    fallbackLight,
                    LevelRenderer.getLightColor(level, pos),
                    LevelRenderer.getLightColor(
                            level,
                            pos.relative(output)
                    )
            );
        }

        Vec3 worldCenter =
                SableContraptionDragTransform.renderWorldCenter(
                        level,
                        pos
                );

        if (worldCenter == null) {
            return fallbackLight;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;

        if (clientLevel == null) {
            return fallbackLight;
        }

        BlockPos worldPos = BlockPos.containing(
                worldCenter.x,
                worldCenter.y,
                worldCenter.z
        );

        return maxPackedLight(
                fallbackLight,
                LevelRenderer.getLightColor(
                        clientLevel,
                        worldPos
                ),
                LevelRenderer.getLightColor(
                        clientLevel,
                        worldPos.above()
                ),
                LevelRenderer.getLightColor(
                        clientLevel,
                        worldPos.north()
                ),
                LevelRenderer.getLightColor(
                        clientLevel,
                        worldPos.south()
                ),
                LevelRenderer.getLightColor(
                        clientLevel,
                        worldPos.east()
                ),
                LevelRenderer.getLightColor(
                        clientLevel,
                        worldPos.west()
                )
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
