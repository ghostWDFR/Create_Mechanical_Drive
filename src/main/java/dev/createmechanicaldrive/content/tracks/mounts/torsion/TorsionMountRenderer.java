package dev.createmechanicaldrive.content.tracks.mounts.torsion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.client.SableContraptionDragTransform;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerRenderer;
import dev.createmechanicaldrive.content.tracks.chain.TrackAssemblyManager;
import dev.createmechanicaldrive.content.tracks.chain.TrackKineticVisuals;
import dev.createmechanicaldrive.content.tracks.mounts.long_torsion.LongTorsionMountBlock;
import dev.createmechanicaldrive.content.tracks.wheels.drive.BigDriveWheelItem;
import dev.createmechanicaldrive.content.tracks.wheels.drive.DriveWheelRenderer;
import dev.createmechanicaldrive.content.tracks.wheels.support.SupportWheelRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TorsionMountRenderer
        extends SafeBlockEntityRenderer<TorsionMountBlockEntity> {
    private static final ResourceLocation MOUNT_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/mounts/torsion_mount/torsion_mount"
            );
    private static final ResourceLocation REVERSED_MOUNT_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/mounts/torsion_mount/torsion_mount_reversed"
            );
    private static final ResourceLocation ARM_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/mounts/torsion_mount/torsion_bar"
            );
    private static final ResourceLocation EMPTY_ARM_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/mounts/torsion_mount/torsion_bar_empty"
            );
    private static final ResourceLocation REVERSED_ARM_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/mounts/torsion_mount/torsion_bar_reversed"
            );
    private static final ResourceLocation REVERSED_EMPTY_ARM_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/mounts/torsion_mount/torsion_bar_empty_reversed"
            );
    private static final PartialModel MOUNT_MODEL =
            PartialModel.of(MOUNT_RESOURCE);
    private static final PartialModel REVERSED_MOUNT_MODEL =
            PartialModel.of(REVERSED_MOUNT_RESOURCE);
    private static final PartialModel ARM_MODEL =
            PartialModel.of(ARM_RESOURCE);
    private static final PartialModel EMPTY_ARM_MODEL =
            PartialModel.of(EMPTY_ARM_RESOURCE);
    private static final PartialModel REVERSED_ARM_MODEL =
            PartialModel.of(REVERSED_ARM_RESOURCE);
    private static final PartialModel REVERSED_EMPTY_ARM_MODEL =
            PartialModel.of(REVERSED_EMPTY_ARM_RESOURCE);
    private static final ResourceLocation LONG_MOUNT_RESOURCE = model(
            "long_torsion_mount"
    );
    private static final ResourceLocation LONG_REVERSED_MOUNT_RESOURCE = model(
            "long_torsion_mount_inverted"
    );
    private static final ResourceLocation LONG_ARM_RESOURCE = model(
            "long_torsion_bar"
    );
    private static final ResourceLocation LONG_EMPTY_ARM_RESOURCE = model(
            "long_torsion_bar_empty"
    );
    private static final ResourceLocation LONG_REVERSED_ARM_RESOURCE = model(
            "long_torsion_bar_inverted"
    );
    private static final ResourceLocation LONG_REVERSED_EMPTY_ARM_RESOURCE =
            model("long_torsion_bar_empty_inverted");
    private static final PartialModel LONG_MOUNT_MODEL =
            PartialModel.of(LONG_MOUNT_RESOURCE);
    private static final PartialModel LONG_REVERSED_MOUNT_MODEL =
            PartialModel.of(LONG_REVERSED_MOUNT_RESOURCE);
    private static final PartialModel LONG_ARM_MODEL =
            PartialModel.of(LONG_ARM_RESOURCE);
    private static final PartialModel LONG_EMPTY_ARM_MODEL =
            PartialModel.of(LONG_EMPTY_ARM_RESOURCE);
    private static final PartialModel LONG_REVERSED_ARM_MODEL =
            PartialModel.of(LONG_REVERSED_ARM_RESOURCE);
    private static final PartialModel LONG_REVERSED_EMPTY_ARM_MODEL =
            PartialModel.of(LONG_REVERSED_EMPTY_ARM_RESOURCE);

    private static ResourceLocation model(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                CreateMechanicalDrive.MOD_ID,
                "block/tracks/mounts/long_torsion_mount/" + name
        );
    }

    public TorsionMountRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }

    public static ModelResourceLocation armModelLocation() {
        return ModelResourceLocation.standalone(ARM_RESOURCE);
    }

    public static ModelResourceLocation mountModelLocation() {
        return ModelResourceLocation.standalone(MOUNT_RESOURCE);
    }

    public static ModelResourceLocation reversedMountModelLocation() {
        return ModelResourceLocation.standalone(REVERSED_MOUNT_RESOURCE);
    }

    public static ModelResourceLocation emptyArmModelLocation() {
        return ModelResourceLocation.standalone(EMPTY_ARM_RESOURCE);
    }

    public static ModelResourceLocation reversedArmModelLocation() {
        return ModelResourceLocation.standalone(REVERSED_ARM_RESOURCE);
    }

    public static ModelResourceLocation reversedEmptyArmModelLocation() {
        return ModelResourceLocation.standalone(
                REVERSED_EMPTY_ARM_RESOURCE
        );
    }

    public static ModelResourceLocation longMountModelLocation() {
        return ModelResourceLocation.standalone(LONG_MOUNT_RESOURCE);
    }

    public static ModelResourceLocation longReversedMountModelLocation() {
        return ModelResourceLocation.standalone(
                LONG_REVERSED_MOUNT_RESOURCE
        );
    }

    public static ModelResourceLocation longArmModelLocation() {
        return ModelResourceLocation.standalone(LONG_ARM_RESOURCE);
    }

    public static ModelResourceLocation longEmptyArmModelLocation() {
        return ModelResourceLocation.standalone(LONG_EMPTY_ARM_RESOURCE);
    }

    public static ModelResourceLocation longReversedArmModelLocation() {
        return ModelResourceLocation.standalone(
                LONG_REVERSED_ARM_RESOURCE
        );
    }

    public static ModelResourceLocation longReversedEmptyArmModelLocation() {
        return ModelResourceLocation.standalone(
                LONG_REVERSED_EMPTY_ARM_RESOURCE
        );
    }

    @Override
    protected void renderSafe(
            TorsionMountBlockEntity mount,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = mount.getBlockState();
        Direction facing = state.getValue(
                TorsionMountBlock.HORIZONTAL_FACING
        );
        ItemStack attachment = mount.getAttachment();
        boolean reversed = state.getValue(TorsionMountBlock.REVERSED);
        boolean longVariant = state.getBlock()
                instanceof LongTorsionMountBlock;
        float side = reversed
                ? -1.0F
                : 1.0F;
        float armAngle = mount.getLerpedArmAngle(partialTicks);
        int renderLight = getRenderLight(mount, facing, packedLight);
        var trackOwner = TrackAssemblyManager.owner(
                mount.getLevel(),
                mount
        );

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(facing)));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        // Reversed geometry is baked separately. A negative runtime scale
        // would reverse face winding and normals, while mirroring around the
        // arm pivot would also move the body into the neighbouring block.
        CachedBuffers.partial(
                        longVariant
                                ? (reversed
                                        ? LONG_REVERSED_MOUNT_MODEL
                                        : LONG_MOUNT_MODEL)
                                : (reversed
                                        ? REVERSED_MOUNT_MODEL
                                        : MOUNT_MODEL),
                        state
                )
                .light(renderLight)
                .renderInto(
                        poseStack,
                        buffers.getBuffer(RenderType.cutout())
                );

        float supportWheelAngle = trackOwner == null
                ? 0.0F
                : TrackKineticVisuals.supportWheelAngle(
                        trackOwner,
                        partialTicks
                );
        SupportWheelRenderer.renderInTorsionMount(
                mount.getSupportWheel(),
                state,
                poseStack,
                buffers,
                renderLight,
                supportWheelAngle
        );

        float pivotX = reversed ? 1.0F : 0.0F;
        poseStack.translate(pivotX, 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotation(side * armAngle));
        poseStack.translate(-pivotX, -0.5F, 0.0F);

        CachedBuffers.partial(
                        armModel(longVariant, reversed, attachment.isEmpty()),
                        state
                )
                .light(renderLight)
                .renderInto(
                        poseStack,
                        buffers.getBuffer(RenderType.cutout())
                );

        ShaftMarkerRenderer.renderAlongPositiveY(
                attachment,
                state,
                poseStack,
                buffers,
                renderLight
        );
        float wheelAngle = mount.getLerpedWheelAngle(partialTicks);
        if (trackOwner != null
                && TorsionMountAttachments.hasSuspensionWheel(attachment)) {
            wheelAngle = TrackKineticVisuals.driveWheelAngle(
                    trackOwner,
                    side * armAngle,
                    TorsionMountAttachments.wheelRadius(attachment),
                    partialTicks
            );
        }
        if (longVariant) {
            // Both wheel models are centred at model-space x=8. The long arm
            // axle is eight pixels farther from that centre: x=16 normally
            // and x=0 for the inverted arm.
            poseStack.translate(side * 0.5F, 0.0F, 0.0F);
        }
        DriveWheelRenderer.BigWheelLayer bigWheelLayer =
                DriveWheelRenderer.BigWheelLayer.SINGLE;
        if (longVariant
                && BigDriveWheelItem.isBigDriveWheel(attachment)
                && LongTorsionMountBlock.hasAdjacentBigWheel(mount)) {
            bigWheelLayer = LongTorsionMountBlock.rendersOuterWheel(mount)
                    ? DriveWheelRenderer.BigWheelLayer.OUTER
                    : DriveWheelRenderer.BigWheelLayer.INNER;
        }
        DriveWheelRenderer.renderOnTorsionArm(
                attachment,
                state,
                poseStack,
                buffers,
                renderLight,
                wheelAngle,
                bigWheelLayer
        );
        poseStack.popPose();
    }

    private static PartialModel armModel(
            boolean longVariant,
            boolean reversed,
            boolean empty
    ) {
        if (longVariant) {
            if (reversed) {
                return empty
                        ? LONG_REVERSED_EMPTY_ARM_MODEL
                        : LONG_REVERSED_ARM_MODEL;
            }
            return empty ? LONG_EMPTY_ARM_MODEL : LONG_ARM_MODEL;
        }
        if (reversed) {
            return empty ? REVERSED_EMPTY_ARM_MODEL : REVERSED_ARM_MODEL;
        }
        return empty ? EMPTY_ARM_MODEL : ARM_MODEL;
    }

    private static float yawFor(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> throw new IllegalArgumentException(
                    "Torsion mount cannot face " + facing
            );
        };
    }

    private static int getRenderLight(
            TorsionMountBlockEntity mount,
            Direction facing,
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
                    LevelRenderer.getLightColor(level, pos.relative(facing))
            );
        }

        Vec3 worldCenter = SableContraptionDragTransform.renderWorldCenter(
                level,
                pos
        );
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;
        if (worldCenter == null || clientLevel == null) {
            return fallbackLight;
        }

        BlockPos worldPos = BlockPos.containing(
                worldCenter.x,
                worldCenter.y,
                worldCenter.z
        );
        return maxPackedLight(
                fallbackLight,
                LevelRenderer.getLightColor(clientLevel, worldPos),
                LevelRenderer.getLightColor(clientLevel, worldPos.above()),
                LevelRenderer.getLightColor(clientLevel, worldPos.north()),
                LevelRenderer.getLightColor(clientLevel, worldPos.south()),
                LevelRenderer.getLightColor(clientLevel, worldPos.east()),
                LevelRenderer.getLightColor(clientLevel, worldPos.west())
        );
    }

    private static int maxPackedLight(int first, int... rest) {
        int result = first;
        for (int packedLight : rest) {
            int blockLight = Math.max(result & 0xFFFF, packedLight & 0xFFFF);
            int skyLight = Math.max(result >>> 16, packedLight >>> 16);
            result = blockLight | (skyLight << 16);
        }
        return result;
    }

    @Override
    public int getViewDistance() {
        return 512;
    }
}
