package dev.createmechanicaldrive.content.tracks.chain;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.tracks.mounts.sprocket.SprocketMountBlockEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TrackAssemblyRenderer {
    private static final ResourceLocation NARROW_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/segments/track_single_narrow"
            );
    private static final ResourceLocation WIDE_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "block/tracks/segments/track_single_wide"
            );
    private static final PartialModel NARROW_MODEL =
            PartialModel.of(NARROW_RESOURCE);
    private static final PartialModel WIDE_MODEL =
            PartialModel.of(WIDE_RESOURCE);

    private TrackAssemblyRenderer() {
    }

    public static ModelResourceLocation narrowModelLocation() {
        return ModelResourceLocation.standalone(NARROW_RESOURCE);
    }

    public static ModelResourceLocation wideModelLocation() {
        return ModelResourceLocation.standalone(WIDE_RESOURCE);
    }

    public static void render(
            SprocketMountBlockEntity mount,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        TrackAssembly assembly = mount.getTrackAssembly();
        if (assembly == null || assembly.linkCount() < 1) {
            return;
        }

        BlockState state = mount.getBlockState();
        TrackPathResolver.Resolved resolved = TrackPathResolver.resolve(
                mount,
                partialTick,
                mount.getLerpedTrackSag(partialTick),
                true
        );
        if (resolved == null) {
            return;
        }
        Direction facing = resolved.facing();
        Direction rolling = resolved.rolling();
        TrackPath path = resolved.path();
        PartialModel model = assembly.type() == TrackType.NARROW
                ? NARROW_MODEL
                : WIDE_MODEL;
        // A physical chain cannot gain or lose a link as suspension travel
        // changes its current spline length. The count is fixed at assembly;
        // only the spacing along the deformed path is redistributed.
        int renderedLinkCount = assembly.linkCount();
        double spacing = path.length() / renderedLinkCount;
        float longitudinalScale = (float) (
                spacing / TrackType.LINK_MODEL_LENGTH
        );
        double phase = TrackKineticVisuals.stableTrackPhase(
                mount,
                partialTick,
                spacing
        );
        Vector3f localAxis = new Vector3f(1.0F, 0.0F, 0.0F);
        Vector3f worldAxis = new Vector3f(
                facing.getStepX(),
                0.0F,
                facing.getStepZ()
        );

        for (int index = 0; index < renderedLinkCount; index++) {
            TrackPath.Sample sample = path.sample(phase + index * spacing);
            poseStack.pushPose();
            poseStack.translate(
                    0.5D + rolling.getStepX() * sample.u(),
                    0.5D + sample.y(),
                    0.5D + rolling.getStepZ() * sample.u()
            );
            poseStack.mulPose(new Quaternionf().rotationTo(
                    localAxis,
                    worldAxis
            ));
            poseStack.mulPose(Axis.XP.rotation((float) -Math.atan2(
                    sample.tangentY(),
                    sample.tangentU()
            )));
            // The model's raised/toothed side is +Y. A track needs that side
            // facing the wheels, not away from them.
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            // Opposite vehicle sides traverse their loops in opposite world
            // directions. Correct the model handedness so the asymmetric end
            // of every link points the same way on both tracks. Rotating
            // around local Y preserves the inward-facing toothed side.
            if (facing.getAxisDirection()
                    == Direction.AxisDirection.NEGATIVE) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            // Suspension changes the spline length while the physical link
            // count stays fixed. Stretch only along the tangent so adjacent
            // links continue to meet without gaps or overlap.
            poseStack.scale(1.0F, 1.0F, longitudinalScale);
            poseStack.translate(-0.5D, -0.5D, -0.5D);
            CachedBuffers.partial(model, state)
                    .light(packedLight)
                    .renderInto(
                            poseStack,
                            buffers.getBuffer(RenderType.cutout())
                    );
            poseStack.popPose();
        }
    }
}
