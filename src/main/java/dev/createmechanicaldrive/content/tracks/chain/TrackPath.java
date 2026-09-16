package dev.createmechanicaldrive.content.tracks.chain;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TrackPath {
    private static final double EPSILON = 1.0E-6D;
    private static final double TAU = Math.PI * 2.0D;
    private static final double SAG_RESPONSE_LENGTH = 2.0D;

    private final List<Segment> segments;
    private final double length;

    private TrackPath(List<Segment> segments, double length) {
        this.segments = List.copyOf(segments);
        this.length = length;
    }

    public double length() {
        return length;
    }

    public Sample sample(double distance) {
        double wrapped = distance % length;
        if (wrapped < 0.0D) {
            wrapped += length;
        }

        for (Segment segment : segments) {
            if (wrapped <= segment.length()) {
                return segment.sample(wrapped);
            }
            wrapped -= segment.length();
        }
        Segment last = segments.get(segments.size() - 1);
        return last.sample(last.length());
    }

    /** Returns the upper branch height at a longitudinal coordinate. */
    public double upperYAt(double u) {
        int sampleCount = Math.max(
                512,
                Math.min(8192, (int) Math.ceil(length * 128.0D))
        );
        Sample previous = sample(0.0D);
        double upperY = Double.NEGATIVE_INFINITY;

        for (int index = 1; index <= sampleCount; index++) {
            Sample current = sample(length * index / sampleCount);
            double minU = Math.min(previous.u(), current.u());
            double maxU = Math.max(previous.u(), current.u());
            if (u >= minU - EPSILON && u <= maxU + EPSILON) {
                double deltaU = current.u() - previous.u();
                double y;
                if (Math.abs(deltaU) <= EPSILON) {
                    y = Math.max(previous.y(), current.y());
                } else {
                    double progress = Math.max(0.0D, Math.min(
                            1.0D,
                            (u - previous.u()) / deltaU
                    ));
                    y = previous.y()
                            + (current.y() - previous.y()) * progress;
                }
                upperY = Math.max(upperY, y);
            }
            previous = current;
        }
        return upperY;
    }

    @Nullable
    public static TrackPath build(List<Wheel> sourceWheels) {
        return build(sourceWheels, 0.0D);
    }

    @Nullable
    public static TrackPath build(
            List<Wheel> sourceWheels,
            double upperSag
    ) {
        if (sourceWheels.size() < 2) {
            return null;
        }

        List<Wheel> wheels = new ArrayList<>(sourceWheels);
        wheels.sort(Comparator.comparingDouble(Wheel::u)
                .thenComparingDouble(Wheel::y));
        wheels = removeDuplicateCenters(wheels);
        if (wheels.size() < 2) {
            return null;
        }

        List<Wheel> hull = convexHull(wheels);
        List<Segment> segments = new ArrayList<>();
        if (hull.size() == 2) {
            addTwoWheelLoop(
                    segments,
                    wheels.get(0),
                    wheels.get(wheels.size() - 1),
                    Math.max(0.0D, upperSag)
            );
        } else if (!addSuspendedHullLoop(
                segments,
                hull,
                wheels,
                Math.max(0.0D, upperSag)
        )) {
            addHullLoop(
                    segments,
                    hull,
                    Math.max(0.0D, upperSag)
            );
        }

        double totalLength = segments.stream()
                .mapToDouble(Segment::length)
                .sum();

        return segments.isEmpty() || totalLength <= EPSILON
                ? null
                : new TrackPath(segments, totalLength);
    }

    private static List<Wheel> removeDuplicateCenters(
            List<Wheel> sorted
    ) {
        List<Wheel> unique = new ArrayList<>(sorted.size());
        for (Wheel wheel : sorted) {
            if (unique.isEmpty()) {
                unique.add(wheel);
                continue;
            }
            Wheel previous = unique.get(unique.size() - 1);
            if (Math.abs(wheel.u() - previous.u()) <= EPSILON
                    && Math.abs(wheel.y() - previous.y()) <= EPSILON) {
                unique.set(unique.size() - 1, new Wheel(
                        previous.u(),
                        previous.y(),
                        Math.max(previous.radius(), wheel.radius()),
                        previous.followsLowerRun()
                                || wheel.followsLowerRun(),
                        previous.supportsUpperRun()
                                || wheel.supportsUpperRun(),
                        previous.constrainsUpperRun()
                                || wheel.constrainsUpperRun()
                ));
                continue;
            }
            unique.add(wheel);
        }
        return unique;
    }

    private static List<Wheel> convexHull(List<Wheel> sorted) {
        List<Wheel> lower = new ArrayList<>();
        for (Wheel wheel : sorted) {
            while (lower.size() >= 2 && cross(
                    lower.get(lower.size() - 2),
                    lower.get(lower.size() - 1),
                    wheel
            ) <= EPSILON) {
                lower.remove(lower.size() - 1);
            }
            lower.add(wheel);
        }

        List<Wheel> upper = new ArrayList<>();
        for (int index = sorted.size() - 1; index >= 0; index--) {
            Wheel wheel = sorted.get(index);
            while (upper.size() >= 2 && cross(
                    upper.get(upper.size() - 2),
                    upper.get(upper.size() - 1),
                    wheel
            ) <= EPSILON) {
                upper.remove(upper.size() - 1);
            }
            upper.add(wheel);
        }

        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);
        lower.addAll(upper);

        // Monotonic chain produces a counter-clockwise polygon. Track
        // segments have always travelled clockwise, so reverse the hull to
        // preserve animation direction and link handedness.
        List<Wheel> clockwise = new ArrayList<>(lower.size());
        for (int index = lower.size() - 1; index >= 0; index--) {
            clockwise.add(lower.get(index));
        }
        return clockwise;
    }

    private static double cross(Wheel first, Wheel second, Wheel third) {
        return (second.u() - first.u()) * (third.y() - first.y())
                - (second.y() - first.y()) * (third.u() - first.u());
    }

    private static void addTwoWheelLoop(
            List<Segment> segments,
            Wheel first,
            Wheel second,
            double upperSag
    ) {
        Point firstNormal = upperNormal(first, second);
        Point oppositeNormal = new Point(
                -firstNormal.u(),
                -firstNormal.y()
        );
        addBoundaryRun(
                segments,
                offset(first, firstNormal),
                offset(second, firstNormal),
                firstNormal,
                upperSag
        );
        segments.add(new ArcSegment(
                center(second),
                second.radius(),
                angle(firstNormal),
                clockwiseSweep(angle(firstNormal), angle(oppositeNormal))
        ));
        addLine(
                segments,
                offset(second, oppositeNormal),
                offset(first, oppositeNormal)
        );
        segments.add(new ArcSegment(
                center(first),
                first.radius(),
                angle(oppositeNormal),
                clockwiseSweep(angle(oppositeNormal), angle(firstNormal))
        ));
    }

    private static void addHullLoop(
            List<Segment> segments,
            List<Wheel> clockwiseHull,
            double upperSag
    ) {
        int size = clockwiseHull.size();
        List<Point> normals = new ArrayList<>(size);
        double longestUpperRun = 0.0D;
        for (int index = 0; index < size; index++) {
            Wheel current = clockwiseHull.get(index);
            Wheel next = clockwiseHull.get((index + 1) % size);
            Point normal = upperNormal(current, next);
            normals.add(normal);
            if (normal.y() > EPSILON) {
                longestUpperRun = Math.max(
                        longestUpperRun,
                        centerDistance(current, next)
                );
            }
        }

        for (int index = 0; index < size; index++) {
            Wheel current = clockwiseHull.get(index);
            Wheel next = clockwiseHull.get((index + 1) % size);
            Point normal = normals.get(index);
            double runSag = normal.y() <= EPSILON
                    || longestUpperRun <= EPSILON
                    ? 0.0D
                    : upperSag * centerDistance(current, next)
                    / longestUpperRun;
            addBoundaryRun(
                    segments,
                    offset(current, normal),
                    offset(next, normal),
                    normal,
                    runSag
            );

            Point nextNormal = normals.get((index + 1) % size);
            double startAngle = angle(normal);
            double sweep = clockwiseSweep(
                    startAngle,
                    angle(nextNormal)
            );
            if (Math.abs(sweep) > EPSILON) {
                segments.add(new ArcSegment(
                        center(next),
                        next.radius(),
                        startAngle,
                        sweep
                ));
            }
        }
    }

    private static boolean addSuspendedHullLoop(
            List<Segment> segments,
            List<Wheel> clockwiseHull,
            List<Wheel> wheels,
            double upperSag
    ) {
        if (wheels.stream().noneMatch(Wheel::followsLowerRun)) {
            return false;
        }

        Wheel left = wheels.get(0);
        double rightU = wheels.get(wheels.size() - 1).u();
        Wheel right = wheels.stream()
                .filter(wheel -> Math.abs(wheel.u() - rightU) <= EPSILON)
                .min(Comparator.comparingDouble(Wheel::y))
                .orElse(null);
        if (right == null || right.u() - left.u() <= EPSILON) {
            return false;
        }

        int leftIndex = clockwiseHull.indexOf(left);
        int rightIndex = clockwiseHull.indexOf(right);
        if (leftIndex < 0 || rightIndex < 0 || leftIndex == rightIndex) {
            return false;
        }

        List<Wheel> hullUpperRoute = removeUpperConstraints(
                route(
                        clockwiseHull,
                        leftIndex,
                        rightIndex
                )
        );
        List<Wheel> upperRoute = addUpperSupports(
                hullUpperRoute,
                wheels
        );
        List<Wheel> lowerContacts = lowerContacts(
                wheels,
                clockwiseHull,
                rightIndex,
                leftIndex
        );
        if (upperRoute.size() < 2 || lowerContacts.size() < 3) {
            return false;
        }

        List<Point> upperNormals = new ArrayList<>(
                upperRoute.size() - 1
        );
        double longestUpperRun = 0.0D;
        for (int index = 0; index < upperRoute.size() - 1; index++) {
            Wheel current = upperRoute.get(index);
            Wheel next = upperRoute.get(index + 1);
            Point normal = upperNormal(current, next);
            upperNormals.add(normal);
            if (normal.y() > EPSILON) {
                longestUpperRun = Math.max(
                        longestUpperRun,
                        centerDistance(current, next)
                );
            }
        }

        for (int index = 0; index < upperRoute.size() - 1; index++) {
            Wheel current = upperRoute.get(index);
            Wheel next = upperRoute.get(index + 1);
            Point normal = upperNormals.get(index);
            double runSag = normal.y() <= EPSILON
                    || longestUpperRun <= EPSILON
                    ? 0.0D
                    : upperSag * centerDistance(current, next)
                    / longestUpperRun;
            addBoundaryRun(
                    segments,
                    offset(current, normal),
                    offset(next, normal),
                    normal,
                    runSag,
                    upperConstraintsBetween(wheels, current, next)
            );
            if (index + 1 < upperRoute.size() - 1) {
                addClockwiseArc(
                        segments,
                        next,
                        normal,
                        upperNormals.get(index + 1)
                );
            }
        }

        int lastContact = lowerContacts.size() - 1;
        Wheel leftRoadWheel = lowerContacts.get(1);
        Wheel rightRoadWheel = lowerContacts.get(lastContact - 1);
        Point bottomNormal = new Point(0.0D, -1.0D);
        Point rightNormal = lowerNormal(rightRoadWheel, right);
        addClockwiseArc(
                segments,
                right,
                upperNormals.get(upperNormals.size() - 1),
                rightNormal
        );
        addLine(
                segments,
                offset(right, rightNormal),
                offset(rightRoadWheel, rightNormal)
        );
        addClockwiseArc(
                segments,
                rightRoadWheel,
                rightNormal,
                bottomNormal
        );

        for (int index = lastContact - 1; index > 1; index--) {
            segments.add(new SmoothLowerSegment(
                    bottom(lowerContacts.get(index)),
                    bottom(lowerContacts.get(index - 1))
            ));
        }

        Point leftNormal = lowerNormal(left, leftRoadWheel);
        addClockwiseArc(
                segments,
                leftRoadWheel,
                bottomNormal,
                leftNormal
        );
        addLine(
                segments,
                offset(leftRoadWheel, leftNormal),
                offset(left, leftNormal)
        );
        addClockwiseArc(
                segments,
                left,
                leftNormal,
                upperNormals.get(0)
        );
        return true;
    }

    /**
     * Convex-hull construction intentionally removes collinear and concave
     * points. Explicit support rollers remain physical route points. Large
     * road wheels are handled separately as smooth constraints and therefore
     * never split the sagging branch into many short, sticky runs.
     */
    private static List<Wheel> addUpperSupports(
            List<Wheel> hullRoute,
            List<Wheel> wheels
    ) {
        if (hullRoute.size() < 2
                || wheels.stream().noneMatch(Wheel::supportsUpperRun)) {
            return hullRoute;
        }

        List<Wheel> supports = wheels.stream()
                .filter(Wheel::supportsUpperRun)
                .sorted(Comparator.comparingDouble(Wheel::u)
                        .thenComparingDouble(Wheel::y))
                .toList();
        List<Wheel> result = new ArrayList<>(
                hullRoute.size() + supports.size()
        );

        for (int index = 0; index < hullRoute.size() - 1; index++) {
            Wheel current = hullRoute.get(index);
            Wheel next = hullRoute.get(index + 1);
            result.add(current);

            double minU = Math.min(current.u(), next.u());
            double maxU = Math.max(current.u(), next.u());
            if (maxU - minU <= EPSILON) {
                continue;
            }

            if (next.u() > current.u()) {
                for (Wheel support : supports) {
                    if (support.u() > minU + EPSILON
                            && support.u() < maxU - EPSILON) {
                        result.add(support);
                    }
                }
            } else {
                for (int supportIndex = supports.size() - 1;
                     supportIndex >= 0;
                     supportIndex--) {
                    Wheel support = supports.get(supportIndex);
                    if (support.u() > minU + EPSILON
                            && support.u() < maxU - EPSILON) {
                        result.add(support);
                    }
                }
            }
        }
        result.add(hullRoute.get(hullRoute.size() - 1));
        return result;
    }

    private static List<Wheel> removeUpperConstraints(List<Wheel> route) {
        if (route.size() <= 2) {
            return route;
        }
        List<Wheel> result = new ArrayList<>(route.size());
        result.add(route.get(0));
        for (int index = 1; index < route.size() - 1; index++) {
            Wheel wheel = route.get(index);
            if (!wheel.constrainsUpperRun()) {
                result.add(wheel);
            }
        }
        result.add(route.get(route.size() - 1));
        return result;
    }

    private static List<Wheel> upperConstraintsBetween(
            List<Wheel> wheels,
            Wheel first,
            Wheel second
    ) {
        double minU = Math.min(first.u(), second.u());
        double maxU = Math.max(first.u(), second.u());
        return wheels.stream()
                .filter(Wheel::constrainsUpperRun)
                .filter(wheel -> wheel.u() > minU + EPSILON
                        && wheel.u() < maxU - EPSILON)
                .toList();
    }

    private static List<Wheel> route(
            List<Wheel> loop,
            int startIndex,
            int endIndex
    ) {
        List<Wheel> result = new ArrayList<>();
        int index = startIndex;
        result.add(loop.get(index));
        while (index != endIndex) {
            index = (index + 1) % loop.size();
            result.add(loop.get(index));
        }
        return result;
    }

    private static List<Wheel> lowerContacts(
            List<Wheel> wheels,
            List<Wheel> clockwiseHull,
            int rightIndex,
            int leftIndex
    ) {
        List<Wheel> candidates = new ArrayList<>();
        for (Wheel wheel : wheels) {
            if (wheel.followsLowerRun()) {
                candidates.add(wheel);
            }
        }
        candidates.addAll(route(clockwiseHull, rightIndex, leftIndex));
        candidates.sort(Comparator.comparingDouble(Wheel::u)
                .thenComparingDouble(Wheel::y));

        List<Wheel> contacts = new ArrayList<>();
        for (Wheel wheel : candidates) {
            if (contacts.isEmpty()
                    || Math.abs(wheel.u()
                    - contacts.get(contacts.size() - 1).u()) > EPSILON) {
                contacts.add(wheel);
            }
        }
        return contacts;
    }

    private static void addClockwiseArc(
            List<Segment> segments,
            Wheel wheel,
            Point startNormal,
            Point endNormal
    ) {
        double startAngle = angle(startNormal);
        double sweep = clockwiseSweep(startAngle, angle(endNormal));
        if (Math.abs(sweep) > EPSILON) {
            segments.add(new ArcSegment(
                    center(wheel),
                    wheel.radius(),
                    startAngle,
                    sweep
            ));
        }
    }

    private static double centerDistance(Wheel first, Wheel second) {
        double deltaU = second.u() - first.u();
        double deltaY = second.y() - first.y();
        return Math.sqrt(deltaU * deltaU + deltaY * deltaY);
    }

    private static void addBoundaryRun(
            List<Segment> segments,
            Point start,
            Point end,
            Point outwardNormal,
            double sag
    ) {
        addBoundaryRun(
                segments,
                start,
                end,
                outwardNormal,
                sag,
                List.of()
        );
    }

    private static void addBoundaryRun(
            List<Segment> segments,
            Point start,
            Point end,
            Point outwardNormal,
            double sag,
            List<Wheel> upperConstraints
    ) {
        if (outwardNormal.y() > EPSILON
                && (sag > EPSILON || !upperConstraints.isEmpty())) {
            addUpperRun(segments, start, end, sag, upperConstraints);
        } else {
            addLine(segments, start, end);
        }
    }

    private static Point center(Wheel wheel) {
        return new Point(wheel.u(), wheel.y());
    }

    private static Point bottom(Wheel wheel) {
        return new Point(wheel.u(), wheel.y() - wheel.radius());
    }

    private static Point offset(Wheel wheel, Point normal) {
        return new Point(
                wheel.u() + normal.u() * wheel.radius(),
                wheel.y() + normal.y() * wheel.radius()
        );
    }

    private static double angle(Point vector) {
        return Math.atan2(vector.y(), vector.u());
    }

    private static double clockwiseSweep(double start, double end) {
        double sweep = end - start;
        while (sweep > 0.0D) {
            sweep -= TAU;
        }
        while (sweep <= -TAU) {
            sweep += TAU;
        }
        return sweep;
    }

    private static void addLine(
            List<Segment> segments,
            Point start,
            Point end
    ) {
        LineSegment line = new LineSegment(start, end);
        if (line.length() > EPSILON) {
            segments.add(line);
        }
    }

    private static void addUpperRun(
            List<Segment> segments,
            Point start,
            Point end,
            double sag
    ) {
        addUpperRun(segments, start, end, sag, List.of());
    }

    private static void addUpperRun(
            List<Segment> segments,
            Point start,
            Point end,
            double sag,
            List<Wheel> upperConstraints
    ) {
        double lengthScaledSag = scaledUpperSag(start, end, sag);
        if (lengthScaledSag <= EPSILON && upperConstraints.isEmpty()) {
            addLine(segments, start, end);
            return;
        }
        SaggingUpperSegment upper = new SaggingUpperSegment(
                start,
                end,
                lengthScaledSag,
                upperConstraints
        );
        if (upper.length() > EPSILON) {
            segments.add(upper);
        }
    }

    private static double scaledUpperSag(
            Point start,
            Point end,
            double sag
    ) {
        double deltaU = end.u() - start.u();
        double deltaY = end.y() - start.y();
        double chordLength = Math.sqrt(
                deltaU * deltaU + deltaY * deltaY
        );
        // Keep the whole impulse range responsive instead of clamping it.
        // Short runs attenuate sag smoothly, while long runs approach the
        // full vehicle-wide value without a visible plateau.
        double normalizedLength = chordLength / SAG_RESPONSE_LENGTH;
        double lengthResponse = 1.0D - Math.exp(
                -normalizedLength * normalizedLength
        );
        return sag * lengthResponse;
    }

    public record Wheel(
            double u,
            double y,
            double radius,
            boolean followsLowerRun,
            boolean supportsUpperRun,
            boolean constrainsUpperRun
    ) {
        public Wheel(
                double u,
                double y,
                double radius,
                boolean followsLowerRun,
                boolean supportsUpperRun
        ) {
            this(
                    u,
                    y,
                    radius,
                    followsLowerRun,
                    supportsUpperRun,
                    false
            );
        }

        public Wheel(
                double u,
                double y,
                double radius,
                boolean followsLowerRun
        ) {
            this(u, y, radius, followsLowerRun, false, false);
        }

        public Wheel(double u, double y, double radius) {
            this(u, y, radius, false, false, false);
        }
    }

    public record Sample(
            double u,
            double y,
            double tangentU,
            double tangentY
    ) {
    }

    private record Point(double u, double y) {
    }

    private interface Segment {
        double length();

        Sample sample(double distance);
    }

    private static final class SmoothLowerSegment implements Segment {
        private static final int LENGTH_SAMPLES = 24;

        private final Point start;
        private final double deltaU;
        private final double deltaY;
        private final double[] cumulativeLengths =
                new double[LENGTH_SAMPLES + 1];
        private final double length;

        private SmoothLowerSegment(Point start, Point end) {
            this.start = start;
            deltaU = end.u() - start.u();
            deltaY = end.y() - start.y();

            Point previous = start;
            for (int index = 1; index <= LENGTH_SAMPLES; index++) {
                Point current = point(index / (double) LENGTH_SAMPLES);
                cumulativeLengths[index] = cumulativeLengths[index - 1]
                        + distance(previous, current);
                previous = current;
            }
            length = cumulativeLengths[LENGTH_SAMPLES];
        }

        @Override
        public double length() {
            return length;
        }

        @Override
        public Sample sample(double distance) {
            double clamped = Math.max(0.0D, Math.min(distance, length));
            int upper = 1;
            while (upper < cumulativeLengths.length - 1
                    && cumulativeLengths[upper] < clamped) {
                upper++;
            }

            double lowerLength = cumulativeLengths[upper - 1];
            double sectionLength = cumulativeLengths[upper] - lowerLength;
            double sectionProgress = sectionLength <= EPSILON
                    ? 0.0D
                    : (clamped - lowerLength) / sectionLength;
            double t = (upper - 1 + sectionProgress) / LENGTH_SAMPLES;
            Point point = point(t);
            double tangentY = deltaY * 6.0D * t * (1.0D - t);
            double tangentLength = Math.sqrt(
                    deltaU * deltaU + tangentY * tangentY
            );
            return new Sample(
                    point.u(),
                    point.y(),
                    deltaU / tangentLength,
                    tangentY / tangentLength
            );
        }

        private Point point(double t) {
            double smooth = t * t * (3.0D - 2.0D * t);
            return new Point(
                    start.u() + deltaU * t,
                    start.y() + deltaY * smooth
            );
        }

        private static double distance(Point first, Point second) {
            double deltaU = second.u() - first.u();
            double deltaY = second.y() - first.y();
            return Math.sqrt(deltaU * deltaU + deltaY * deltaY);
        }
    }

    private static final class SaggingUpperSegment implements Segment {
        private static final int LENGTH_SAMPLES = 64;
        private static final double CONTACT_BLEND = 2.0D / 16.0D;
        private static final double SUPPORT_JOIN_BLEND = 1.0D / 16.0D;
        private static final double EDGE_BLEND_PORTION = 0.125D;

        private final Point start;
        private final double deltaU;
        private final double deltaY;
        private final double sag;
        private final List<Wheel> upperConstraints;
        private final double[] cumulativeLengths =
                new double[LENGTH_SAMPLES + 1];
        private final double length;

        private SaggingUpperSegment(
                Point start,
                Point end,
                double sag,
                List<Wheel> upperConstraints
        ) {
            this.start = start;
            deltaU = end.u() - start.u();
            deltaY = end.y() - start.y();
            this.sag = sag;
            this.upperConstraints = List.copyOf(upperConstraints);

            Point previous = start;
            for (int index = 1; index <= LENGTH_SAMPLES; index++) {
                Point current = point(index / (double) LENGTH_SAMPLES);
                cumulativeLengths[index] = cumulativeLengths[index - 1]
                        + distance(previous, current);
                previous = current;
            }
            length = cumulativeLengths[LENGTH_SAMPLES];
        }

        @Override
        public double length() {
            return length;
        }

        @Override
        public Sample sample(double distance) {
            double clamped = Math.max(0.0D, Math.min(distance, length));
            int upper = 1;
            while (upper < cumulativeLengths.length - 1
                    && cumulativeLengths[upper] < clamped) {
                upper++;
            }

            double lowerLength = cumulativeLengths[upper - 1];
            double sectionLength = cumulativeLengths[upper] - lowerLength;
            double sectionProgress = sectionLength <= EPSILON
                    ? 0.0D
                    : (clamped - lowerLength) / sectionLength;
            double t = (upper - 1 + sectionProgress) / LENGTH_SAMPLES;
            Point point = point(t);
            double tangentStep = 1.0D / (LENGTH_SAMPLES * 4.0D);
            Point before = point(Math.max(0.0D, t - tangentStep));
            Point after = point(Math.min(1.0D, t + tangentStep));
            double tangentU = after.u() - before.u();
            double tangentY = after.y() - before.y();
            double tangentLength = Math.sqrt(
                    tangentU * tangentU + tangentY * tangentY
            );
            return new Sample(
                    point.u(),
                    point.y(),
                    tangentU / tangentLength,
                    tangentY / tangentLength
            );
        }

        private Point point(double t) {
            double u = start.u() + deltaU * t;
            double baseY = start.y() + deltaY * t
                    - sag * sagProfile(t);
            double supportY = Double.NEGATIVE_INFINITY;

            for (Wheel wheel : upperConstraints) {
                double distanceU = u - wheel.u();
                double radius = wheel.radius();
                // A broad parabola stays above the wheel without copying its
                // circumference. Neighbouring wheels merge into one smooth
                // supported run instead of producing individual wraps.
                double candidateY = wheel.y() + radius
                        - distanceU * distanceU / (4.0D * radius);
                supportY = Double.isFinite(supportY)
                        ? smoothMaximum(supportY, candidateY)
                        : candidateY;
            }

            double penetration = supportY - baseY;
            if (penetration <= 0.0D) {
                return new Point(u, baseY);
            }

            double contactProgress = Math.min(
                    penetration / CONTACT_BLEND,
                    1.0D
            );
            double contactWeight = smoothStep(contactProgress);
            double edgeProgress = Math.min(
                    Math.min(t, 1.0D - t) / EDGE_BLEND_PORTION,
                    1.0D
            );
            double edgeWeight = smoothStep(Math.max(0.0D, edgeProgress));
            return new Point(
                    u,
                    baseY + penetration * contactWeight * edgeWeight
            );
        }

        private static double sagProfile(double t) {
            double edgeDistance = t * (1.0D - t);
            return 16.0D * edgeDistance * edgeDistance;
        }

        private static double smoothStep(double value) {
            return value * value * (3.0D - 2.0D * value);
        }

        private static double smoothMaximum(double first, double second) {
            double difference = Math.abs(first - second);
            double blend = Math.max(
                    SUPPORT_JOIN_BLEND - difference,
                    0.0D
            ) / SUPPORT_JOIN_BLEND;
            return Math.max(first, second)
                    + blend * blend * SUPPORT_JOIN_BLEND * 0.25D;
        }

        private static double distance(Point first, Point second) {
            double deltaU = second.u() - first.u();
            double deltaY = second.y() - first.y();
            return Math.sqrt(deltaU * deltaU + deltaY * deltaY);
        }
    }

    private static final class LineSegment implements Segment {
        private final Point start;
        private final double deltaU;
        private final double deltaY;
        private final double length;

        private LineSegment(Point start, Point end) {
            this.start = start;
            deltaU = end.u() - start.u();
            deltaY = end.y() - start.y();
            length = Math.sqrt(deltaU * deltaU + deltaY * deltaY);
        }

        @Override
        public double length() {
            return length;
        }

        @Override
        public Sample sample(double distance) {
            double progress = length <= EPSILON
                    ? 0.0D
                    : Math.min(distance / length, 1.0D);
            return new Sample(
                    start.u() + deltaU * progress,
                    start.y() + deltaY * progress,
                    deltaU / length,
                    deltaY / length
            );
        }
    }

    private static final class ArcSegment implements Segment {
        private final Point center;
        private final double radius;
        private final double startAngle;
        private final double sweep;
        private final double length;

        private ArcSegment(
                Point center,
                double radius,
                double startAngle,
                double sweep
        ) {
            this.center = center;
            this.radius = radius;
            this.startAngle = startAngle;
            this.sweep = sweep;
            length = Math.abs(sweep) * radius;
        }

        @Override
        public double length() {
            return length;
        }

        @Override
        public Sample sample(double distance) {
            double progress = length <= EPSILON
                    ? 0.0D
                    : Math.min(distance / length, 1.0D);
            double angle = startAngle + sweep * progress;
            double tangentSign = Math.signum(sweep);
            return new Sample(
                    center.u() + Math.cos(angle) * radius,
                    center.y() + Math.sin(angle) * radius,
                    -Math.sin(angle) * tangentSign,
                    Math.cos(angle) * tangentSign
            );
        }
    }
    private static Point upperNormal(Wheel left, Wheel right) {
        return tangentNormal(left, right, true);
    }

    private static Point lowerNormal(Wheel left, Wheel right) {
        return tangentNormal(left, right, false);
    }

    private static Point tangentNormal(
            Wheel left,
            Wheel right,
            boolean upper
    ) {
        double deltaU = right.u() - left.u();
        double deltaY = right.y() - left.y();
        double length = Math.sqrt(deltaU * deltaU + deltaY * deltaY);
        double tangentU = deltaU / length;
        double tangentY = deltaY / length;
        double along = Math.max(-1.0D, Math.min(
                1.0D,
                (left.radius() - right.radius()) / length
        ));
        double outward = Math.sqrt(Math.max(0.0D, 1.0D - along * along));
        if (!upper) {
            outward = -outward;
        }
        return new Point(
                tangentU * along - tangentY * outward,
                tangentY * along + tangentU * outward
        );
    }

}
