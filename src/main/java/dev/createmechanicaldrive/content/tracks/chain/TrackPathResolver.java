package dev.createmechanicaldrive.content.tracks.chain;

import dev.createmechanicaldrive.content.tracks.mounts.sprocket.SprocketMountBlock;
import dev.createmechanicaldrive.content.tracks.mounts.sprocket.SprocketMountBlockEntity;
import dev.createmechanicaldrive.content.tracks.wheels.drive.BigDriveWheelItem;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class TrackPathResolver {
    public static final double TRACK_CLEARANCE = 0.5D / 16.0D;

    private TrackPathResolver() {
    }

    @Nullable
    public static Resolved resolve(
            SprocketMountBlockEntity mount,
            float partialTick,
            double upperSag,
            boolean includeSupportWheels
    ) {
        TrackAssembly assembly = mount.getTrackAssembly();
        Level level = mount.getLevel();
        if (assembly == null || level == null || assembly.linkCount() < 1) {
            return null;
        }

        Direction facing = mount.getBlockState().getValue(
                SprocketMountBlock.HORIZONTAL_FACING
        );
        Direction rolling = facing.getClockWise();
        Vec3 anchor = mount.getBlockPos().getCenter();
        List<TrackPath.Wheel> wheels = new ArrayList<>(
                assembly.nodes().size() * 2
        );

        for (var nodePos : assembly.nodes()) {
            TrackWheelLookup.WheelNode node = TrackWheelLookup.find(
                    level,
                    nodePos,
                    partialTick
            );
            if (node == null || node.facing() != facing) {
                return null;
            }
            addWheel(wheels, node, anchor, rolling);

            if (includeSupportWheels) {
                TrackWheelLookup.WheelNode support =
                        TrackWheelLookup.findSupportWheel(level, nodePos);
                if (support != null && support.facing() == facing) {
                    addWheel(wheels, support, anchor, rolling);
                }
            }
        }

        TrackPath path = TrackPath.build(wheels, upperSag);
        return path == null
                ? null
                : new Resolved(path, facing, rolling, anchor);
    }

    private static void addWheel(
            List<TrackPath.Wheel> wheels,
            TrackWheelLookup.WheelNode node,
            Vec3 anchor,
            Direction rolling
    ) {
        Vec3 relative = node.center().subtract(anchor);
        double u = relative.x * rolling.getStepX()
                + relative.z * rolling.getStepZ();
        wheels.add(new TrackPath.Wheel(
                u,
                relative.y,
                node.radius() + TRACK_CLEARANCE,
                node.kind() == TrackWheelLookup.Kind.DRIVE,
                node.kind() == TrackWheelLookup.Kind.SUPPORT,
                node.kind() == TrackWheelLookup.Kind.DRIVE
                        && node.radius() >= BigDriveWheelItem.RADIUS
                        - 1.0E-6D
        ));
    }

    public record Resolved(
            TrackPath path,
            Direction facing,
            Direction rolling,
            Vec3 anchor
    ) {
    }
}
