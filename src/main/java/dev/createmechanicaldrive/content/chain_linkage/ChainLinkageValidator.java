package dev.createmechanicaldrive.content.chain_linkage;

import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ChainLinkageValidator {
    public static final int MIN_GEARS = 2;
    public static final int MAX_GEARS = 64;
    public static final int MAX_BOUNDS = 10;
    private static final double GEAR_RADIUS =
            7.0D / 16.0D;

    private static final double CHAINS_PER_BLOCK =
            0.4D;

    public static final double FLEXIBLE_CHAIN_STRETCH =
            1.0D;

    private static final double MIN_FLEXIBLE_CENTER_DISTANCE =
            GEAR_RADIUS * 2.0D;

    private static final double MAX_FLEXIBLE_ANGLE_DEGREES =
            20.0D;

    private static final double MIN_FLEXIBLE_ORIENTATION_DOT =
            Math.cos(Math.toRadians(MAX_FLEXIBLE_ANGLE_DEGREES));

    private ChainLinkageValidator() {
    }

    public static boolean isValidLoop(
            List<BlockPos> positions,
            Direction.Axis axis
    ) {
        return getFailureKey(positions, axis) == null;
    }

    public static String getFailureKey(
            List<BlockPos> positions,
            Direction.Axis axis
    ) {
        if (positions.size() < MIN_GEARS) {
            return "too_short";
        }

        if (positions.size() > MAX_GEARS) {
            return "too_many_gears";
        }

        Set<BlockPos> unique =
                new HashSet<>(positions);

        if (unique.size() != positions.size()) {
            return "duplicate_gear";
        }

        int planeCoordinate =
                positions.getFirst()
                        .get(axis);

        int minX = positions.getFirst().getX();
        int minY = positions.getFirst().getY();
        int minZ = positions.getFirst().getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;

        for (BlockPos position : positions) {
            if (position.get(axis) != planeCoordinate) {
                return "not_planar";
            }

            minX = Math.min(minX, position.getX());
            minY = Math.min(minY, position.getY());
            minZ = Math.min(minZ, position.getZ());
            maxX = Math.max(maxX, position.getX());
            maxY = Math.max(maxY, position.getY());
            maxZ = Math.max(maxZ, position.getZ());
        }

        if (maxX - minX > MAX_BOUNDS
                || maxY - minY > MAX_BOUNDS
                || maxZ - minZ > MAX_BOUNDS) {
            return "too_large";
        }

        if (hasSelfIntersection(positions, axis)) {
            return "self_intersection";
        }

        if (!isOuterLoop(positions, axis)) {
            return "not_outer_loop";
        }

        return null;
    }

    public static int getChainsRequired(
            List<BlockPos> positions
    ) {
        double length =
                0.0D;

        for (int i = 0; i < positions.size(); i++) {
            BlockPos from =
                    positions.get(i);

            BlockPos to =
                    positions.get((i + 1) % positions.size());

            length += Math.sqrt(
                    from.distSqr(to)
            );
        }

        length += Math.PI * 2.0D * GEAR_RADIUS;

        return Math.max(
                1,
                (int) Math.ceil(length * CHAINS_PER_BLOCK)
        );
    }

    public static int getChainsRequired(
            double loopLength
    ) {
        return Math.max(
                1,
                (int) Math.ceil(loopLength * CHAINS_PER_BLOCK)
        );
    }

    public static double getTwoGearLoopLength(
            double centerDistance
    ) {
        return Math.max(
                0.0D,
                centerDistance
        ) * 2.0D + Math.PI * 2.0D * GEAR_RADIUS;
    }

    public static double getWorldCenterDistance(
            Level level,
            List<BlockPos> positions
    ) {
        if (positions.size() != 2) {
            return 0.0D;
        }

        return SableSubLevelHelper
                .getWorldCenter(
                        level,
                        positions.getFirst()
                )
                .distanceTo(
                        SableSubLevelHelper.getWorldCenter(
                                level,
                                positions.getLast()
                        )
                );
    }

    public static double getFlexibleCenterDistance(
            Level level,
            List<BlockPos> positions,
            Direction.Axis axis
    ) {
        if (positions.size() != 2) {
            return 0.0D;
        }

        BlockPos firstPos =
                positions.getFirst();

        BlockPos secondPos =
                positions.getLast();

        Vec3 first =
                SableSubLevelHelper.getWorldCenter(
                        level,
                        firstPos
                );

        Vec3 second =
                SableSubLevelHelper.getWorldCenter(
                        level,
                        secondPos
                );

        Vec3 normal =
                SableSubLevelHelper.getWorldAxis(
                        level,
                        firstPos,
                        axis
                );

        return planarLength(
                second.subtract(first),
                normal
        );
    }

    public static boolean isFlexibleTwoGearLinkValid(
            Level level,
            List<BlockPos> positions,
            Direction.Axis axis,
            double initialCenterDistance,
            double initialLoopLength
    ) {
        return getFlexibleTwoGearFailureKey(
                level,
                positions,
                axis,
                initialCenterDistance,
                initialLoopLength
        ) == null;
    }

    public static String getFlexibleTwoGearFailureKey(
            Level level,
            List<BlockPos> positions,
            Direction.Axis axis,
            double initialCenterDistance,
            double initialLoopLength
    ) {
        if (positions.size() != 2) {
            return "too_short";
        }

        BlockPos firstPos =
                positions.getFirst();

        BlockPos secondPos =
                positions.getLast();

        Vec3 first =
                SableSubLevelHelper.getWorldCenter(
                        level,
                        firstPos
                );

        Vec3 second =
                SableSubLevelHelper.getWorldCenter(
                        level,
                        secondPos
                );

        Vec3 delta =
                second.subtract(first);

        Vec3 firstAxis =
                SableSubLevelHelper.getWorldAxis(
                        level,
                        firstPos,
                        axis
                );

        Vec3 secondAxis =
                SableSubLevelHelper.getWorldAxis(
                        level,
                        secondPos,
                        axis
                );

        if (!hasMatchingWorldOrientation(
                level,
                firstPos,
                secondPos
        )) {
            return "orientation_mismatch";
        }

        double planeOffset =
                Math.max(
                        Math.abs(delta.dot(firstAxis)),
                        Math.abs(delta.dot(secondAxis))
                );

        double planeOffsetAngleDegrees =
                Math.toDegrees(
                        Math.atan2(
                                planeOffset,
                                Math.max(
                                        MIN_FLEXIBLE_CENTER_DISTANCE,
                                        initialCenterDistance
                                )
                        )
                );

        if (planeOffsetAngleDegrees > allowedFlexibleAngleDegrees(
                initialCenterDistance
        )) {
            return "too_deformed";
        }

        double centerDistance =
                planarLength(
                        delta,
                        firstAxis
                );

        if (centerDistance < MIN_FLEXIBLE_CENTER_DISTANCE) {
            return "too_deformed";
        }

        if (initialCenterDistance - centerDistance
                > FLEXIBLE_CHAIN_STRETCH) {
            return "too_deformed";
        }

        double dot =
                Math.abs(
                        firstAxis.dot(secondAxis)
                );

        dot =
                Math.max(
                        -1.0D,
                        Math.min(
                                1.0D,
                                dot
                        )
                );

        double angleDegrees =
                Math.toDegrees(
                        Math.acos(dot)
                );

        if (angleDegrees > allowedFlexibleAngleDegrees(
                initialCenterDistance
        )) {
            return "too_deformed";
        }

        return null;
    }

    private static boolean hasMatchingWorldOrientation(
            Level level,
            BlockPos firstPos,
            BlockPos secondPos
    ) {
        for (Direction.Axis axis : Direction.Axis.values()) {
            Vec3 firstAxis =
                    SableSubLevelHelper.getWorldAxis(
                            level,
                            firstPos,
                            axis
                    );

            Vec3 secondAxis =
                    SableSubLevelHelper.getWorldAxis(
                            level,
                            secondPos,
                            axis
                    );

            if (firstAxis.lengthSqr() < 1.0E-8D
                    || secondAxis.lengthSqr() < 1.0E-8D) {
                return false;
            }

            if (firstAxis.normalize()
                    .dot(
                            secondAxis.normalize()
                    ) < MIN_FLEXIBLE_ORIENTATION_DOT) {
                return false;
            }
        }

        return true;
    }

    private static double planarLength(
            Vec3 delta,
            Vec3 normal
    ) {
        if (normal.lengthSqr() < 1.0E-8D) {
            return delta.length();
        }

        Vec3 unitNormal =
                normal.normalize();

        return delta.subtract(
                unitNormal.scale(
                        delta.dot(unitNormal)
                )
        ).length();
    }

    public static double allowedFlexibleAngleDegrees(
            double initialCenterDistance
    ) {
        return MAX_FLEXIBLE_ANGLE_DEGREES;
    }
    private static boolean hasSelfIntersection(
            List<BlockPos> positions,
            Direction.Axis axis
    ) {
        int size =
                positions.size();

        if (size < 4) {
            return false;
        }

        for (int first = 0; first < size; first++) {
            int firstNext =
                    (first + 1) % size;

            for (int second = first + 1; second < size; second++) {
                int secondNext =
                        (second + 1) % size;

                if (first == second
                        || first == secondNext
                        || firstNext == second
                        || firstNext == secondNext) {
                    continue;
                }

                if (segmentsIntersect(
                        positions.get(first),
                        positions.get(firstNext),
                        positions.get(second),
                        positions.get(secondNext),
                        axis
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isOuterLoop(
            List<BlockPos> positions,
            Direction.Axis axis
    ) {
        int size =
                positions.size();

        if (size < 3) {
            return true;
        }

        long area =
                signedArea(
                        positions,
                        axis
                );

        if (area == 0L) {
            return false;
        }

        int winding =
                Long.signum(area);

        for (int i = 0; i < size; i++) {
            BlockPos previous =
                    positions.get(
                            Math.floorMod(
                                    i - 1,
                                    size
                            )
                    );

            BlockPos current =
                    positions.get(i);

            BlockPos next =
                    positions.get(
                            (i + 1) % size
                    );

            long turn =
                    turn(
                            previous,
                            current,
                            next,
                            axis
                    );

            if (turn != 0L
                    && Long.signum(turn) != winding) {
                return false;
            }
        }

        return true;
    }

    private static long signedArea(
            List<BlockPos> positions,
            Direction.Axis axis
    ) {
        long area =
                0L;

        for (int i = 0; i < positions.size(); i++) {
            BlockPos current =
                    positions.get(i);

            BlockPos next =
                    positions.get(
                            (i + 1) % positions.size()
                    );

            area += (long) planeX(current, axis) * planeY(next, axis)
                    - (long) planeX(next, axis) * planeY(current, axis);
        }

        return area;
    }

    private static long turn(
            BlockPos previous,
            BlockPos current,
            BlockPos next,
            Direction.Axis axis
    ) {
        long incomingX =
                planeX(current, axis) - planeX(previous, axis);

        long incomingY =
                planeY(current, axis) - planeY(previous, axis);

        long outgoingX =
                planeX(next, axis) - planeX(current, axis);

        long outgoingY =
                planeY(next, axis) - planeY(current, axis);

        return incomingX * outgoingY
                - incomingY * outgoingX;
    }

    private static boolean segmentsIntersect(
            BlockPos a,
            BlockPos b,
            BlockPos c,
            BlockPos d,
            Direction.Axis normal
    ) {
        int ax =
                planeX(a, normal);
        int ay =
                planeY(a, normal);
        int bx =
                planeX(b, normal);
        int by =
                planeY(b, normal);
        int cx =
                planeX(c, normal);
        int cy =
                planeY(c, normal);
        int dx =
                planeX(d, normal);
        int dy =
                planeY(d, normal);

        int o1 =
                orientation(ax, ay, bx, by, cx, cy);
        int o2 =
                orientation(ax, ay, bx, by, dx, dy);
        int o3 =
                orientation(cx, cy, dx, dy, ax, ay);
        int o4 =
                orientation(cx, cy, dx, dy, bx, by);

        if (o1 != o2 && o3 != o4) {
            return true;
        }

        return o1 == 0 && onSegment(ax, ay, cx, cy, bx, by)
                || o2 == 0 && onSegment(ax, ay, dx, dy, bx, by)
                || o3 == 0 && onSegment(cx, cy, ax, ay, dx, dy)
                || o4 == 0 && onSegment(cx, cy, bx, by, dx, dy);
    }

    private static int orientation(
            int ax,
            int ay,
            int bx,
            int by,
            int cx,
            int cy
    ) {
        long value =
                (long) (by - ay) * (cx - bx)
                        - (long) (bx - ax) * (cy - by);

        return Long.compare(value, 0L);
    }

    private static boolean onSegment(
            int ax,
            int ay,
            int px,
            int py,
            int bx,
            int by
    ) {
        return px >= Math.min(ax, bx)
                && px <= Math.max(ax, bx)
                && py >= Math.min(ay, by)
                && py <= Math.max(ay, by);
    }

    private static int planeX(
            BlockPos pos,
            Direction.Axis normal
    ) {
        return switch (normal) {
            case X -> pos.getZ();
            case Y, Z -> pos.getX();
        };
    }

    private static int planeY(
            BlockPos pos,
            Direction.Axis normal
    ) {
        return switch (normal) {
            case X, Z -> pos.getY();
            case Y -> pos.getZ();
        };
    }
}
