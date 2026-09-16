package dev.createmechanicaldrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public final class ChainLinkageRenderHelper {
    public static final double LINK_LENGTH =
            4.0D / 16.0D;

    public static final double GEAR_RADIUS =
            7.0D / 16.0D;

    private static final double TAU =
            Math.PI * 2.0D;

    private static final float WIDE_LINK_SCALE =
            1.035F;

    private static final float NARROW_LINK_SCALE =
            0.965F;

    private static final double LINK_WIDTH =
            3.0D / 16.0D;

    private static final double LINK_HEIGHT =
            2.0D / 16.0D;

    private static final int GHOST_PREVIEW_VALID_COLOR =
            FastColor.ARGB32.color(
                    77,
                    0,
                    255,
                    0
            );

    private static final int GHOST_PREVIEW_INVALID_COLOR =
            FastColor.ARGB32.color(
                    77,
                    255,
                    0,
                    0
            );

    private ChainLinkageRenderHelper() {
    }

    public static void renderLoop(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            List<BlockPos> loop,
            Direction.Axis axis,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        renderLoop(
                blockRenderer,
                linkModel,
                loop,
                axis,
                origin,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                0.0F
        );
    }

    public static void renderLoop(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            List<BlockPos> loop,
            Direction.Axis axis,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float textureVOffset
    ) {
        RenderState renderState =
                new RenderState(
                        0,
                        normalizeTextureOffset(textureVOffset)
                );

        for (int i = 0; i < loop.size(); i++) {
            renderLoopPart(
                    blockRenderer,
                    linkModel,
                    loop,
                    i,
                    axis,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );
        }
    }

    public static void renderGhostLoop(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            List<BlockPos> loop,
            Direction.Axis axis,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        renderGhostLoop(
                blockRenderer,
                linkModel,
                loop,
                axis,
                origin,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                true
        );
    }

    public static void renderGhostLoop(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            List<BlockPos> loop,
            Direction.Axis axis,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            boolean valid
    ) {
        RenderState renderState =
                new RenderState(
                        0,
                        0.0F,
                        true,
                        valid
                                ? GHOST_PREVIEW_VALID_COLOR
                                : GHOST_PREVIEW_INVALID_COLOR
                );

        for (int i = 0; i < loop.size(); i++) {
            renderLoopPart(
                    blockRenderer,
                    linkModel,
                    loop,
                    i,
                    axis,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );
        }
    }

    public static void renderFlexibleGhostLoop(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            Level level,
            List<BlockPos> loop,
            Direction.Axis axis,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            boolean valid
    ) {
        if (loop.size() != 2) {
            return;
        }

        RenderState renderState =
                new RenderState(
                        0,
                        0.0F,
                        true,
                        valid
                                ? GHOST_PREVIEW_VALID_COLOR
                                : GHOST_PREVIEW_INVALID_COLOR
                );

        renderFlexibleTwoGearWorldPart(
                blockRenderer,
                linkModel,
                level,
                loop,
                0,
                axis,
                origin,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                renderState
        );

        renderFlexibleTwoGearWorldPart(
                blockRenderer,
                linkModel,
                level,
                loop,
                1,
                axis,
                origin,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                renderState
        );
    }

    public static void renderLoopPart(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            List<BlockPos> loop,
            int index,
            Direction.Axis axis,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        renderLoopPart(
                blockRenderer,
                linkModel,
                loop,
                index,
                axis,
                origin,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                0.0F
        );
    }

    public static void renderLoopPart(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            List<BlockPos> loop,
            int index,
            Direction.Axis axis,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float textureVOffset
    ) {
        RenderState renderState =
                new RenderState(
                        estimatePartSeed(
                                loop,
                                index,
                                axis
                        ),
                        normalizeTextureOffset(textureVOffset)
                );

        renderLoopPart(
                blockRenderer,
                linkModel,
                loop,
                index,
                axis,
                origin,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                renderState
        );
    }

    private static void renderLoopPart(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            List<BlockPos> loop,
            int index,
            Direction.Axis axis,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            RenderState renderState
    ) {
        int size =
                loop.size();

        if (size < 2
                || index < 0
                || index >= size) {
            return;
        }

        if (size == 2) {
            renderTwoGearPart(
                    blockRenderer,
                    linkModel,
                    loop,
                    index,
                    axis,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );

            return;
        }

        renderPolygonPart(
                blockRenderer,
                linkModel,
                loop,
                index,
                axis,
                origin,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                renderState
        );
    }

    public static double getLoopLength(
            List<BlockPos> loop,
            Direction.Axis axis
    ) {
        int size =
                loop.size();

        if (size < 2) {
            return 0.0D;
        }

        if (size == 2) {
            Point2 first =
                    Point2.of(loop.getFirst(), axis);

            Point2 second =
                    Point2.of(loop.getLast(), axis);

            double straightLength =
                    Math.max(
                            0.0D,
                            first.distanceTo(second)
                    );

            return straightLength * 2.0D
                    + GEAR_RADIUS * TAU;
        }

        List<Point2> points =
                loop.stream()
                        .map(pos -> Point2.of(pos, axis))
                        .toList();

        double area =
                signedArea(points);

        int side =
                outsideSide(area);

        double length =
                0.0D;

        for (int i = 0; i < size; i++) {
            Point2 previous =
                    points.get(
                            Math.floorMod(
                                    i - 1,
                                    size
                            )
                    );

            Point2 current =
                    points.get(i);

            Point2 next =
                    points.get(
                            (i + 1) % size
                    );

            Point2 incoming =
                    tangentEnd(
                            previous,
                            current,
                            side
                    );

            Point2 outgoing =
                    tangentStart(
                            current,
                            next,
                            side
                    );

            Point2 outgoingEnd =
                    tangentEnd(
                            current,
                            next,
                            side
                    );

            length += arcLength(
                    current,
                    incoming,
                    outgoing,
                    isConcave(
                            previous,
                            current,
                            next,
                            area
                    )
            );

            length += outgoing.distanceTo(
                    outgoingEnd
            );
        }

        return length;
    }

    public static float textureMotionSign(
            List<BlockPos> loop,
            Direction.Axis axis
    ) {
        if (loop.size() < 3) {
            return -planeHandedness(
                    axis
            );
        }

        return -outsideSide(
                loop,
                axis
        ) * planeHandedness(
                axis
        );
    }

    public static void renderFlexibleTwoGearPart(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            Level level,
            List<BlockPos> loop,
            int index,
            Direction.Axis axis,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float textureVOffset
    ) {
        if (loop.size() != 2
                || index < 0
                || index > 1) {
            return;
        }

        Vec3 first =
                SableContraptionDragTransform.renderWorldCenter(
                        level,
                        loop.getFirst()
                );

        Vec3 second =
                SableContraptionDragTransform.renderWorldCenter(
                        level,
                        loop.getLast()
                );

        RenderState renderState =
                new RenderState(
                        estimateFlexiblePartSeed(
                                first,
                                second,
                                index
                        ),
                        normalizeTextureOffset(textureVOffset)
                );

        renderFlexibleTwoGearPart(
                blockRenderer,
                linkModel,
                level,
                loop,
                index,
                axis,
                origin,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                renderState
        );
    }

    public static void renderFlexibleTwoGearPart(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            Vec3 first,
            Vec3 second,
            Direction.Axis axis,
            Vec3 planeNormal,
            int index,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float textureVOffset
    ) {
        if (index < 0
                || index > 1) {
            return;
        }

        RenderState renderState =
                new RenderState(
                        estimateFlexiblePartSeed(
                                first,
                                second,
                                index
                        ),
                        normalizeTextureOffset(textureVOffset)
                );
        renderState.invertWorldOutward =
                true;

        renderFlexibleTwoGearPart(
                blockRenderer,
                linkModel,
                first,
                second,
                axis,
                planeNormal,
                planeNormal,
                index,
                origin,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                renderState
        );
    }
    private static void renderFlexibleTwoGearWorldPart(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            Level level,
            List<BlockPos> loop,
            int index,
            Direction.Axis axis,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            RenderState renderState
    ) {
        if (loop.size() != 2
                || index < 0
                || index > 1) {
            return;
        }

        Vec3 first =
                SableContraptionDragTransform.renderWorldCenter(
                        level,
                        loop.getFirst()
                );
        Vec3 second =
                SableContraptionDragTransform.renderWorldCenter(
                        level,
                        loop.getLast()
                );

        renderFlexibleTwoGearPart(
                blockRenderer,
                linkModel,
                first,
                second,
                axis,
                flexibleWorldPlaneNormal(
                        level,
                        loop.getFirst(),
                        axis
                ),
                flexibleWorldPlaneNormal(
                        level,
                        loop.getLast(),
                        axis
                ),
                index,
                origin,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                renderState
        );
    }
    private static Vec3 flexibleWorldPlaneNormal(
            Level level,
            BlockPos pos,
            Direction.Axis axis
    ) {
        Vec3 normal =
                SableContraptionDragTransform.localNormalToWorld(
                        level,
                        pos,
                        axisVector(axis)
                );

        if (normal != null
                && normal.lengthSqr() > 1.0E-8D) {
            return normal.normalize();
        }

        return axisVector(axis);
    }

    private static void renderFlexibleTwoGearPart(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            Level level,
            List<BlockPos> loop,
            int index,
            Direction.Axis axis,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            RenderState renderState
    ) {
        if (loop.size() != 2
                || index < 0
                || index > 1) {
            return;
        }

        Vec3 first =
                Vec3.atCenterOf(loop.getFirst());
        Vec3 secondWorld =
                SableContraptionDragTransform.renderWorldCenter(
                        level,
                        loop.getLast()
                );
        Vec3 second =
                SableContraptionDragTransform.worldPositionToLocal(
                        level,
                        loop.getFirst(),
                        secondWorld
                );

        if (second == null) {
            second =
                    secondWorld;
        }

        Vec3 secondPlaneNormalWorld =
                flexibleWorldPlaneNormal(
                        level,
                        loop.getLast(),
                        axis
                );
        Vec3 secondPlaneNormal =
                SableContraptionDragTransform.worldNormalToLocal(
                        level,
                        loop.getFirst(),
                        secondPlaneNormalWorld
                );

        if (secondPlaneNormal == null) {
            secondPlaneNormal =
                    secondPlaneNormalWorld;
        }

        renderFlexibleTwoGearPart(
                blockRenderer,
                linkModel,
                first,
                second,
                axis,
                axisVector(axis),
                secondPlaneNormal,
                index,
                origin,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                renderState
        );
    }
    private static void renderTwoGearPart(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            List<BlockPos> loop,
            int index,
            Direction.Axis axis,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            RenderState renderState
    ) {
        double plane =
                loop.getFirst()
                        .get(axis)
                        + 0.5D;

        Point2 first =
                Point2.of(loop.getFirst(), axis);

        Point2 second =
                Point2.of(loop.getLast(), axis);

        Point2 direction =
                second.subtract(first)
                        .normalize();

        if (direction.isZero()) {
            return;
        }

        Point2 normal =
                direction.leftNormal()
                        .scale(GEAR_RADIUS);

        if (index == 0) {
            renderArc(
                    blockRenderer,
                    linkModel,
                    first,
                    first.subtract(normal),
                    first.add(normal),
                    true,
                    axis,
                    plane,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );

            renderLine(
                    blockRenderer,
                    linkModel,
                    first.add(normal),
                    second.add(normal),
                    1,
                    axis,
                    plane,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );
        } else {
            renderArc(
                    blockRenderer,
                    linkModel,
                    second,
                    second.add(normal),
                    second.subtract(normal),
                    true,
                    axis,
                    plane,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );

            renderLine(
                    blockRenderer,
                    linkModel,
                    second.subtract(normal),
                    first.subtract(normal),
                    1,
                    axis,
                    plane,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );
        }
    }

    private static void renderFlexibleTwoGearPart(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            Vec3 first,
            Vec3 second,
            Direction.Axis axis,
            Vec3 firstPlaneNormal,
            Vec3 secondPlaneNormal,
            int index,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            RenderState renderState
    ) {
        Vec3 delta =
                second.subtract(first);

        if (delta.lengthSqr() < 1.0E-6D) {
            return;
        }

        firstPlaneNormal =
                normalizedOrFallback(
                        firstPlaneNormal,
                        axisVector(axis)
                );
        secondPlaneNormal =
                normalizedOrFallback(
                        secondPlaneNormal,
                        firstPlaneNormal
                );

        if (firstPlaneNormal.dot(secondPlaneNormal) < 0.0D) {
            secondPlaneNormal =
                    secondPlaneNormal.scale(-1.0D);
        }

        Vec3 firstDirection =
                planarDirection(
                        delta,
                        firstPlaneNormal
                );
        Vec3 secondDirection =
                planarDirection(
                        delta,
                        secondPlaneNormal
                );

        if (firstDirection.lengthSqr() < 1.0E-6D
                || secondDirection.lengthSqr() < 1.0E-6D) {
            return;
        }

        Vec3 firstSide =
                firstPlaneNormal.cross(firstDirection)
                        .normalize()
                        .scale(GEAR_RADIUS);
        Vec3 secondSide =
                secondPlaneNormal.cross(secondDirection)
                        .normalize()
                        .scale(GEAR_RADIUS);

        if (index == 0) {
            renderArcWorld(
                    blockRenderer,
                    linkModel,
                    first,
                    first.subtract(firstSide),
                    first.add(firstSide),
                    axis,
                    firstPlaneNormal,
                    true,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );

            renderLineWorld(
                    blockRenderer,
                    linkModel,
                    first.add(firstSide),
                    second.add(secondSide),
                    axis,
                    firstPlaneNormal,
                    secondPlaneNormal,
                    firstSide,
                    secondSide,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );
        } else {
            renderArcWorld(
                    blockRenderer,
                    linkModel,
                    second,
                    second.add(secondSide),
                    second.subtract(secondSide),
                    axis,
                    secondPlaneNormal,
                    true,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );

            renderLineWorld(
                    blockRenderer,
                    linkModel,
                    second.subtract(secondSide),
                    first.subtract(firstSide),
                    axis,
                    secondPlaneNormal,
                    firstPlaneNormal,
                    secondSide.scale(-1.0D),
                    firstSide.scale(-1.0D),
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );
        }
    }

    private static Vec3 planarDirection(
            Vec3 direction,
            Vec3 planeNormal
    ) {
        Vec3 projected =
                direction.subtract(
                        planeNormal.scale(
                                direction.dot(planeNormal)
                        )
                );

        return projected.lengthSqr() < 1.0E-8D
                ? Vec3.ZERO
                : projected.normalize();
    }

    private static Vec3 normalizedOrFallback(
            Vec3 vector,
            Vec3 fallback
    ) {
        return vector.lengthSqr() < 1.0E-8D
                ? fallback.normalize()
                : vector.normalize();
    }
    private static void renderPolygonPart(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            List<BlockPos> loop,
            int index,
            Direction.Axis axis,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            RenderState renderState
    ) {
        int size =
                loop.size();

        double plane =
                loop.getFirst()
                        .get(axis)
                        + 0.5D;

        List<Point2> points =
                loop.stream()
                        .map(pos -> Point2.of(pos, axis))
                        .toList();

        double area =
                signedArea(points);

        int side =
                outsideSide(area);

        Point2 previous =
                points.get(
                        Math.floorMod(
                                index - 1,
                                size
                        )
                );

        Point2 current =
                points.get(index);

        Point2 next =
                points.get(
                        (index + 1) % size
                );

        Point2 incoming =
                tangentEnd(
                        previous,
                        current,
                        side
                );

        Point2 outgoing =
                tangentStart(
                        current,
                        next,
                        side
                );

        Point2 outgoingEnd =
                tangentEnd(
                        current,
                        next,
                        side
                );

        renderArc(
                blockRenderer,
                linkModel,
                current,
                incoming,
                outgoing,
                isConcave(
                        previous,
                        current,
                        next,
                        area
                ),
                axis,
                plane,
                origin,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                renderState
        );

        renderLine(
                blockRenderer,
                linkModel,
                outgoing,
                outgoingEnd,
                side,
                axis,
                plane,
                origin,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                renderState
        );
    }

    private static Point2 tangentStart(
            Point2 from,
            Point2 to,
            int side
    ) {
        Point2 direction =
                to.subtract(from)
                        .normalize();

        if (direction.isZero()) {
            return to;
        }

        return from.add(
                direction
                        .leftNormal()
                        .scale(GEAR_RADIUS * side)
        );
    }

    private static Point2 tangentEnd(
            Point2 from,
            Point2 to,
            int side
    ) {
        Point2 direction =
                to.subtract(from)
                        .normalize();

        if (direction.isZero()) {
            return to;
        }

        return to.add(
                direction
                        .leftNormal()
                        .scale(GEAR_RADIUS * side)
        );
    }

    private static void renderLine(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            Point2 from,
            Point2 to,
            int outwardSide,
            Direction.Axis axis,
            double plane,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            RenderState renderState
    ) {
        Point2 delta =
                to.subtract(from);

        double length =
                delta.length();

        if (length < 0.001D) {
            return;
        }

        Point2 direction =
                delta.scale(
                        1.0D / length
                );

        int linkCount =
                linkCountForLength(
                        length
                );

        for (int i = 0; i < linkCount; i++) {
            double offset =
                    (i + 0.5D) * length / linkCount;

            renderLink(
                    blockRenderer,
                    linkModel,
                    from.add(
                            direction.scale(offset)
                    ),
                    direction,
                    direction
                            .leftNormal()
                            .scale(outwardSide),
                    false,
                    axis,
                    plane,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );
        }
    }

    private static void renderArc(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            Point2 center,
            Point2 from,
            Point2 to,
            boolean concave,
            Direction.Axis axis,
            double plane,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            RenderState renderState
    ) {
        double startAngle =
                Math.atan2(
                        from.y - center.y,
                        from.x - center.x
                );

        double endAngle =
                Math.atan2(
                        to.y - center.y,
                        to.x - center.x
                );

        double sweep =
                outsideSweep(
                        startAngle,
                        endAngle,
                        concave
                );

        double arcLength =
                Math.abs(sweep) * GEAR_RADIUS;

        if (arcLength < 0.001D) {
            return;
        }

        int linkCount =
                linkCountForLength(
                        arcLength
                );

        double sign =
                Math.signum(sweep);

        for (int i = 0; i < linkCount; i++) {
            double step =
                    (i + 0.5D) / linkCount;

            double angle =
                    startAngle + sweep * step;

            Point2 position =
                    new Point2(
                            center.x + Math.cos(angle) * GEAR_RADIUS,
                            center.y + Math.sin(angle) * GEAR_RADIUS
                    );

            Point2 direction =
                    new Point2(
                            -Math.sin(angle) * sign,
                            Math.cos(angle) * sign
                    );

            renderLink(
                    blockRenderer,
                    linkModel,
                    position,
                    direction,
                    position.subtract(center),
                    true,
                    axis,
                    plane,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );
        }
    }

    private static void renderLineWorld(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            Vec3 from,
            Vec3 to,
            Direction.Axis axis,
            Vec3 fromPlaneNormal,
            Vec3 toPlaneNormal,
            Vec3 fromOutward,
            Vec3 toOutward,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            RenderState renderState
    ) {
        Vec3 delta =
                to.subtract(from);
        double length =
                delta.length();

        if (length < 0.001D) {
            return;
        }

        Vec3 direction =
                delta.scale(1.0D / length);
        int linkCount =
                linkCountForLength(length);

        for (int i = 0; i < linkCount; i++) {
            double progress =
                    (i + 0.5D) / linkCount;
            Vec3 planeNormal =
                    interpolateUnitVector(
                            fromPlaneNormal,
                            toPlaneNormal,
                            progress
                    );
            Vec3 outward =
                    interpolateUnitVector(
                            fromOutward,
                            toOutward,
                            progress
                    );

            renderLinkWorld(
                    blockRenderer,
                    linkModel,
                    from.add(
                            direction.scale(progress * length)
                    ),
                    direction,
                    axis,
                    planeNormal,
                    outward,
                    false,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );
        }
    }

    private static Vec3 interpolateUnitVector(
            Vec3 from,
            Vec3 to,
            double progress
    ) {
        if (from.lengthSqr() < 1.0E-8D) {
            return normalizedOrFallback(to, new Vec3(0.0D, 1.0D, 0.0D));
        }

        Vec3 first =
                from.normalize();
        Vec3 second =
                normalizedOrFallback(to, first);

        if (first.dot(second) < 0.0D) {
            second =
                    second.scale(-1.0D);
        }

        Vec3 interpolated =
                first.scale(1.0D - progress)
                        .add(second.scale(progress));

        return normalizedOrFallback(
                interpolated,
                first
        );
    }
    private static void renderArcWorld(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            Vec3 center,
            Vec3 from,
            Vec3 to,
            Direction.Axis axis,
            Vec3 planeNormal,
            boolean concave,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            RenderState renderState
    ) {
        Vec3 radiusStart =
                from.subtract(center);

        Vec3 radiusEnd =
                to.subtract(center);

        if (radiusStart.lengthSqr() < 1.0E-6D
                || radiusEnd.lengthSqr() < 1.0E-6D) {
            return;
        }

        radiusStart =
                radiusStart.normalize();

        radiusEnd =
                radiusEnd.normalize();

        Vec3 normal =
                planeNormal.lengthSqr() > 1.0E-8D
                        ? planeNormal.normalize()
                        : axisVector(axis);

        double sweep =
                outsideSweep(
                        0.0D,
                        signedAngle(
                                radiusStart,
                                radiusEnd,
                                normal
                        ),
                        concave
                );

        double arcLength =
                Math.abs(sweep) * GEAR_RADIUS;

        if (arcLength < 0.001D) {
            return;
        }

        int linkCount =
                linkCountForLength(
                        arcLength
                );

        double sign =
                Math.signum(sweep);

        for (int i = 0; i < linkCount; i++) {
            double angle =
                    sweep * (i + 0.5D) / linkCount;

            Vec3 radial =
                    rotateAroundAxis(
                            radiusStart,
                            normal,
                            angle
                    );

            Vec3 tangent =
                    normal.cross(radial)
                            .scale(sign);

            if (tangent.lengthSqr() < 1.0E-6D) {
                continue;
            }

            renderLinkWorld(
                    blockRenderer,
                    linkModel,
                    center.add(
                            radial.scale(GEAR_RADIUS)
                    ),
                    tangent.normalize(),
                    axis,
                    planeNormal,
                    radial,
                    true,
                    origin,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    renderState
            );
        }
    }

    private static double arcLength(
            Point2 center,
            Point2 from,
            Point2 to,
            boolean concave
    ) {
        double startAngle =
                Math.atan2(
                        from.y - center.y,
                        from.x - center.x
                );

        double endAngle =
                Math.atan2(
                        to.y - center.y,
                        to.x - center.x
                );

        return Math.abs(
                outsideSweep(
                        startAngle,
                        endAngle,
                        concave
                )
        ) * GEAR_RADIUS;
    }

    private static double outsideSweep(
            double startAngle,
            double endAngle,
            boolean concave
    ) {
        double sweep =
                shortestSweep(
                        startAngle,
                        endAngle
                );

        if (!concave) {
            return sweep;
        }

        if (sweep > 0.0D) {
            return sweep - TAU;
        }

        return sweep + TAU;
    }

    private static double shortestSweep(
            double startAngle,
            double endAngle
    ) {
        double sweep =
                endAngle - startAngle;

        while (sweep <= -Math.PI) {
            sweep += TAU;
        }

        while (sweep > Math.PI) {
            sweep -= TAU;
        }

        return sweep;
    }

    private static void renderLink(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            Point2 position,
            Point2 direction,
            Point2 outward,
            boolean alternateWidth,
            Direction.Axis axis,
            double plane,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            RenderState renderState
    ) {
        Vec3 worldPosition =
                toWorld(
                        position,
                        axis,
                        plane
                );

        Vec3 worldDirection =
                directionToWorld(
                        direction,
                        axis
                );

        poseStack.pushPose();
        poseStack.translate(
                worldPosition.x - origin.x,
                worldPosition.y - origin.y,
                worldPosition.z - origin.z
        );

        alignModelAxis(
                poseStack,
                axis
        );

        poseStack.mulPose(
                Axis.YP.rotation(
                        segmentAngle(
                                worldDirection,
                                axis
                        )
                )
        );

        boolean modelOutwardFlipped =
                shouldFlipOutward(
                        direction,
                        outward
                );

        if (axis == Direction.Axis.Y
                || axis == Direction.Axis.X) {
            modelOutwardFlipped =
                    !modelOutwardFlipped;
        }

        if (!modelOutwardFlipped) {
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            180.0F
                    )
            );
        }

        poseStack.scale(
                1.0F,
                renderState.nextWidthScale(
                        alternateWidth
                ),
                1.0F
        );

        poseStack.translate(
                -0.5D,
                -0.5D,
                -0.5D
        );

        renderModel(
                blockRenderer,
                linkModel,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                renderState.textureVOffset,
                renderState.ghost,
                renderState.ghostColor
        );

        poseStack.popPose();
    }

    private static void renderLinkWorld(
            BlockRenderDispatcher blockRenderer,
            BakedModel linkModel,
            Vec3 position,
            Vec3 direction,
            Direction.Axis axis,
            Vec3 planeNormal,
            Vec3 outward,
            boolean alternateWidth,
            Vec3 origin,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            RenderState renderState
    ) {
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }

        Vec3 unitDirection =
                direction.normalize();

        poseStack.pushPose();
        poseStack.translate(
                position.x - origin.x,
                position.y - origin.y,
                position.z - origin.z
        );

        boolean modelOutwardFlipped =
                shouldFlipOutwardWorld(
                        unitDirection,
                        outward,
                        planeNormal
                );

        if (shouldInvertWorldOutwardForPlane(planeNormal)) {
            modelOutwardFlipped =
                    !modelOutwardFlipped;
        }

        if (renderState.invertWorldOutward) {
            modelOutwardFlipped =
                    !modelOutwardFlipped;
        }

        alignModelToWorldPlane(
                poseStack,
                planeNormal,
                unitDirection,
                !modelOutwardFlipped
        );

        poseStack.scale(
                1.0F,
                renderState.nextWidthScale(
                        alternateWidth
                ),
                1.0F
        );

        poseStack.translate(
                -0.5D,
                -0.5D,
                -0.5D
        );

        renderModel(
                blockRenderer,
                linkModel,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                renderState.textureVOffset,
                renderState.ghost,
                renderState.ghostColor
        );

        poseStack.popPose();
    }

    private static boolean shouldInvertWorldOutwardForPlane(
            Vec3 planeNormal
    ) {
        return false;
    }

    private static boolean shouldFlipOutwardWorld(
            Vec3 direction,
            Vec3 outward,
            Vec3 planeNormal
    ) {
        if (direction.lengthSqr() < 1.0E-8D
                || outward.lengthSqr() < 1.0E-8D
                || planeNormal.lengthSqr() < 1.0E-8D) {
            return false;
        }

        Vec3 currentOutward =
                planeNormal.normalize()
                        .cross(
                                direction.normalize()
                        );

        if (currentOutward.lengthSqr() < 1.0E-8D) {
            return false;
        }

        return currentOutward.normalize()
                .dot(
                        outward.normalize()
                ) < 0.0D;
    }

    private static void alignModelToWorldPlane(
            PoseStack poseStack,
            Vec3 planeNormal,
            Vec3 direction,
            boolean flipOutward
    ) {
        if (planeNormal.lengthSqr() < 1.0E-8D
                || direction.lengthSqr() < 1.0E-8D) {
            return;
        }

        Vec3 tangent =
                direction.normalize();

        Vec3 normal =
                planeNormal.subtract(
                        tangent.scale(
                                planeNormal.dot(tangent)
                        )
                );

        if (normal.lengthSqr() < 1.0E-8D) {
            return;
        }

        normal =
                normal.normalize();

        Quaternionf orientation =
                new Quaternionf()
                        .rotationTo(
                                0.0F,
                                1.0F,
                                0.0F,
                                (float) normal.x,
                                (float) normal.y,
                                (float) normal.z
                        );

        Vector3f currentX =
                orientation.transform(
                        new Vector3f(
                                1.0F,
                                0.0F,
                                0.0F
                        )
                );

        Vec3 baseDirection =
                new Vec3(
                        currentX.x(),
                        currentX.y(),
                        currentX.z()
                );

        baseDirection =
                baseDirection.subtract(
                        normal.scale(
                                baseDirection.dot(normal)
                        )
                );

        if (baseDirection.lengthSqr() > 1.0E-8D) {
            orientation.rotateY(
                    (float) signedAngle(
                            baseDirection.normalize(),
                            tangent,
                            normal
                    )
            );
        }

        poseStack.mulPose(
                orientation
        );

        if (flipOutward) {
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            180.0F
                    )
            );
        }
    }

    private static void renderModel(
            BlockRenderDispatcher blockRenderer,
            BakedModel model,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float textureVOffset,
            boolean ghost,
            int ghostColor
    ) {
        if (ghost) {
            renderGhostBox(
                    poseStack,
                    buffer,
                    ghostColor
            );

            return;
        }

        RenderType renderType =
                RenderType.cutout();

        VertexConsumer vertexConsumer =
                buffer.getBuffer(renderType);

        if (textureVOffset != 0.0F) {
            vertexConsumer =
                    new TextureVOffsetVertexConsumer(
                            vertexConsumer,
                            textureVOffset
                    );
        }

        BlockShadedModelRenderer.render(
                poseStack.last(),
                vertexConsumer,
                null,
                model,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay,
                renderType
        );
    }

    private static void renderGhostBox(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int color
    ) {
        double minX =
                0.5D - LINK_LENGTH * 0.5D;

        double maxX =
                0.5D + LINK_LENGTH * 0.5D;

        double minY =
                0.5D - LINK_WIDTH * 0.5D;

        double maxY =
                0.5D + LINK_WIDTH * 0.5D;

        double minZ =
                0.5D - LINK_HEIGHT * 0.5D;

        double maxZ =
                0.5D + LINK_HEIGHT * 0.5D;

        VertexConsumer vertexConsumer =
                buffer.getBuffer(
                        RenderType.debugQuads()
                );

        PoseStack.Pose pose =
                poseStack.last();

        addQuad(
                vertexConsumer,
                pose,
                minX,
                minY,
                maxZ,
                maxX,
                minY,
                maxZ,
                maxX,
                maxY,
                maxZ,
                minX,
                maxY,
                maxZ,
                color
        );

        addQuad(
                vertexConsumer,
                pose,
                maxX,
                minY,
                minZ,
                minX,
                minY,
                minZ,
                minX,
                maxY,
                minZ,
                maxX,
                maxY,
                minZ,
                color
        );

        addQuad(
                vertexConsumer,
                pose,
                minX,
                minY,
                minZ,
                minX,
                minY,
                maxZ,
                minX,
                maxY,
                maxZ,
                minX,
                maxY,
                minZ,
                color
        );

        addQuad(
                vertexConsumer,
                pose,
                maxX,
                minY,
                maxZ,
                maxX,
                minY,
                minZ,
                maxX,
                maxY,
                minZ,
                maxX,
                maxY,
                maxZ,
                color
        );

        addQuad(
                vertexConsumer,
                pose,
                minX,
                maxY,
                maxZ,
                maxX,
                maxY,
                maxZ,
                maxX,
                maxY,
                minZ,
                minX,
                maxY,
                minZ,
                color
        );

        addQuad(
                vertexConsumer,
                pose,
                minX,
                minY,
                minZ,
                maxX,
                minY,
                minZ,
                maxX,
                minY,
                maxZ,
                minX,
                minY,
                maxZ,
                color
        );
    }

    private static void addQuad(
            VertexConsumer vertexConsumer,
            PoseStack.Pose pose,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            double x3,
            double y3,
            double z3,
            double x4,
            double y4,
            double z4,
            int color
    ) {
        addGhostVertex(
                vertexConsumer,
                pose,
                x1,
                y1,
                z1,
                color
        );

        addGhostVertex(
                vertexConsumer,
                pose,
                x2,
                y2,
                z2,
                color
        );

        addGhostVertex(
                vertexConsumer,
                pose,
                x3,
                y3,
                z3,
                color
        );

        addGhostVertex(
                vertexConsumer,
                pose,
                x4,
                y4,
                z4,
                color
        );
    }

    private static void addGhostVertex(
            VertexConsumer vertexConsumer,
            PoseStack.Pose pose,
            double x,
            double y,
            double z,
            int color
    ) {
        vertexConsumer
                .addVertex(
                        pose,
                        (float) x,
                        (float) y,
                        (float) z
                )
                .setColor(
                        color
                );
    }

    private static int estimatePartSeed(
            List<BlockPos> loop,
            int index,
            Direction.Axis axis
    ) {
        int seed =
                0;

        for (int i = 0; i < index; i++) {
            seed += estimatePartLinkCount(
                    loop,
                    i,
                    axis
            );
        }

        return seed;
    }

    private static int estimatePartLinkCount(
            List<BlockPos> loop,
            int index,
            Direction.Axis axis
    ) {
        int size =
                loop.size();

        if (size < 2) {
            return 0;
        }

        if (size == 2) {
            Point2 first =
                    Point2.of(loop.getFirst(), axis);

            Point2 second =
                    Point2.of(loop.getLast(), axis);

            return linkCountForLength(
                    Math.PI * GEAR_RADIUS
            ) + linkCountForLength(
                    first.distanceTo(second)
            );
        }

        List<Point2> points =
                loop.stream()
                        .map(pos -> Point2.of(pos, axis))
                        .toList();

        double area =
                signedArea(points);

        int side =
                outsideSide(area);

        Point2 previous =
                points.get(
                        Math.floorMod(
                                index - 1,
                                size
                        )
                );

        Point2 current =
                points.get(index);

        Point2 next =
                points.get(
                        (index + 1) % size
                );

        Point2 incoming =
                tangentEnd(
                        previous,
                        current,
                        side
                );

        Point2 outgoing =
                tangentStart(
                        current,
                        next,
                        side
                );

        Point2 outgoingEnd =
                tangentEnd(
                        current,
                        next,
                        side
                );

        return linkCountForLength(
                arcLength(
                        current,
                        incoming,
                        outgoing,
                        isConcave(
                                previous,
                                current,
                                next,
                                area
                        )
                )
        ) + linkCountForLength(
                outgoing.distanceTo(
                        outgoingEnd
                )
        );
    }

    private static int estimateFlexiblePartSeed(
            Vec3 first,
            Vec3 second,
            int index
    ) {
        if (index <= 0) {
            return 0;
        }

        return linkCountForLength(
                Math.PI * GEAR_RADIUS
        ) + linkCountForLength(
                first.distanceTo(second)
        );
    }

    private static int linkCountForLength(
            double length
    ) {
        if (!Double.isFinite(length)
                || length < 0.001D) {
            return 0;
        }

        return Math.min(
                1024,
                Math.max(
                        1,
                        (int) Math.ceil(
                                length / LINK_LENGTH
                        )
                )
        );
    }

    private static Vec3 toWorld(
            Point2 point,
            Direction.Axis axis,
            double plane
    ) {
        return switch (axis) {
            case X -> new Vec3(
                    plane,
                    point.y,
                    point.x
            );
            case Y -> new Vec3(
                    point.x,
                    plane,
                    point.y
            );
            case Z -> new Vec3(
                    point.x,
                    point.y,
                    plane
            );
        };
    }

    private static Vec3 directionToWorld(
            Point2 direction,
            Direction.Axis axis
    ) {
        return switch (axis) {
            case X -> new Vec3(
                    0.0D,
                    direction.y,
                    direction.x
            );
            case Y -> new Vec3(
                    direction.x,
                    0.0D,
                    direction.y
            );
            case Z -> new Vec3(
                    direction.x,
                    direction.y,
                    0.0D
            );
        };
    }

    private static Vec3 axisVector(
            Direction.Axis axis
    ) {
        return switch (axis) {
            case X -> new Vec3(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3(0.0D, 0.0D, 1.0D);
        };
    }

    private static double signedAngle(
            Vec3 from,
            Vec3 to,
            Vec3 normal
    ) {
        return Math.atan2(
                normal.dot(
                        from.cross(to)
                ),
                from.dot(to)
        );
    }

    private static Vec3 rotateAroundAxis(
            Vec3 vector,
            Vec3 axis,
            double angle
    ) {
        double cos =
                Math.cos(angle);

        double sin =
                Math.sin(angle);

        return vector.scale(cos)
                .add(
                        axis.cross(vector)
                                .scale(sin)
                )
                .add(
                        axis.scale(
                                axis.dot(vector)
                                        * (1.0D - cos)
                        )
                );
    }

    public static float segmentAngle(
            Vec3 direction,
            Direction.Axis axis
    ) {
        double localX;
        double localZ;

        switch (axis) {
            case X -> {
                localX =
                        -direction.y;
                localZ =
                        direction.z;
            }
            case Z -> {
                localX =
                        direction.x;
                localZ =
                        -direction.y;
            }
            case Y -> {
                localX =
                        direction.x;
                localZ =
                        direction.z;
            }
            default -> {
                localX =
                        direction.x;
                localZ =
                        direction.z;
            }
        }

        return (float) -Math.atan2(
                localZ,
                localX
        );
    }

    public static void alignModelAxis(
            PoseStack poseStack,
            Direction.Axis axis
    ) {
        switch (axis) {
            case X ->
                    poseStack.mulPose(
                            Axis.ZP.rotationDegrees(
                                    -90.0F
                            )
                    );
            case Z ->
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(
                                    90.0F
                            )
                    );
            case Y -> {
            }
        }
    }

    private static int planeHandedness(
            Direction.Axis axis
    ) {
        return axis == Direction.Axis.Z
                ? 1
                : -1;
    }
    private static double signedArea(
            List<Point2> points
    ) {
        double area =
                0.0D;

        for (int i = 0; i < points.size(); i++) {
            Point2 current =
                    points.get(i);

            Point2 next =
                    points.get(
                            (i + 1) % points.size()
                    );

            area += current.x * next.y
                    - next.x * current.y;
        }

        return area;
    }

    private static int outsideSide(
            List<BlockPos> loop,
            Direction.Axis axis
    ) {
        List<Point2> points =
                loop.stream()
                        .map(pos -> Point2.of(pos, axis))
                        .toList();

        return outsideSide(
                signedArea(points)
        );
    }

    private static int outsideSide(
            double signedArea
    ) {
        return signedArea >= 0.0D
                ? -1
                : 1;
    }

    private static boolean isConcave(
            Point2 previous,
            Point2 current,
            Point2 next,
            double signedArea
    ) {
        Point2 incoming =
                current.subtract(previous);

        Point2 outgoing =
                next.subtract(current);

        double turn =
                cross(
                        incoming,
                        outgoing
                );

        if (Math.abs(turn) < 0.0001D
                || Math.abs(signedArea) < 0.0001D) {
            return false;
        }

        return Math.signum(turn)
                != Math.signum(signedArea);
    }

    private static double cross(
            Point2 first,
            Point2 second
    ) {
        return first.x * second.y
                - first.y * second.x;
    }

    private static boolean shouldFlipOutward(
            Point2 direction,
            Point2 outward
    ) {
        Point2 currentOutward =
                direction
                        .leftNormal()
                        .normalize();

        Point2 targetOutward =
                outward.normalize();

        if (currentOutward.isZero()
                || targetOutward.isZero()) {
            return false;
        }

        return currentOutward.dot(targetOutward) < 0.0D;
    }

    private static float wrapUnit(
            float value
    ) {
        return value - (float) Math.floor(value);
    }

    private static float normalizeTextureOffset(
            float value
    ) {
        if (value < 0.0F) {
            return -wrapUnit(
                    -value
            );
        }

        return wrapUnit(value);
    }

    private static final class RenderState {
        private int linkIndex;
        private final float textureVOffset;
        private final boolean ghost;
        private final int ghostColor;
        private boolean invertWorldOutward;

        private RenderState(
                int linkIndex,
                float textureVOffset
        ) {
            this(
                    linkIndex,
                    textureVOffset,
                    false,
                    GHOST_PREVIEW_VALID_COLOR
            );
        }

        private RenderState(
                int linkIndex,
                float textureVOffset,
                boolean ghost
        ) {
            this(
                    linkIndex,
                    textureVOffset,
                    ghost,
                    GHOST_PREVIEW_VALID_COLOR
            );
        }

        private RenderState(
                int linkIndex,
                float textureVOffset,
                boolean ghost,
                int ghostColor
        ) {
            this.linkIndex =
                    linkIndex;

            this.textureVOffset =
                    textureVOffset;

            this.ghost =
                    ghost;

            this.ghostColor =
                    ghostColor;
        }

        private float nextWidthScale(
                boolean alternate
        ) {
            if (!alternate) {
                return 1.0F;
            }

            float scale =
                    (linkIndex & 1) == 0
                            ? WIDE_LINK_SCALE
                            : NARROW_LINK_SCALE;

            linkIndex++;

            return scale;
        }
    }

    private static final class TextureVOffsetVertexConsumer
            extends VertexConsumerWrapper {
        private final VertexData[] vertices =
                new VertexData[4];
        private int vertexCount;
        private final float vOffsetFraction;

        private TextureVOffsetVertexConsumer(
                VertexConsumer parent,
                float textureVOffset
        ) {
            super(parent);

            vOffsetFraction =
                    textureVOffset;
        }

        @Override
        public void addVertex(
                float x,
                float y,
                float z,
                int color,
                float u,
                float v,
                int packedOverlay,
                int packedLight,
                float normalX,
                float normalY,
                float normalZ
        ) {
            vertices[vertexCount] =
                    new VertexData(
                            x,
                            y,
                            z,
                            color,
                            u,
                            v,
                            packedOverlay,
                            packedLight,
                            normalX,
                            normalY,
                            normalZ
                    );

            vertexCount++;

            if (vertexCount == vertices.length) {
                flushQuad();
            }
        }

        private void flushQuad() {
            float minV =
                    Float.POSITIVE_INFINITY;

            float maxV =
                    Float.NEGATIVE_INFINITY;

            for (VertexData vertex : vertices) {
                minV =
                        Math.min(
                                minV,
                                vertex.v
                        );

                maxV =
                        Math.max(
                                maxV,
                                vertex.v
                        );
            }

            float vSpan =
                    maxV - minV;

            float vShift =
                    shiftedV(
                            vSpan
                    );

            for (VertexData vertex : vertices) {
                parent.addVertex(
                        vertex.x,
                        vertex.y,
                        vertex.z,
                        vertex.color,
                        vertex.u,
                        vSpan <= 0.0F
                                ? vertex.v
                                : vertex.v + vShift,
                        vertex.packedOverlay,
                        vertex.packedLight,
                        vertex.normalX,
                        vertex.normalY,
                        vertex.normalZ
                );
            }

            vertexCount =
                    0;
        }

        private float shiftedV(
                float vSpan
        ) {
            if (vOffsetFraction < 0.0F) {
                return -(1.0F + vOffsetFraction)
                        * vSpan;
            }

            return -vOffsetFraction
                    * vSpan;
        }
    }

    private record VertexData(
            float x,
            float y,
            float z,
            int color,
            float u,
            float v,
            int packedOverlay,
            int packedLight,
            float normalX,
            float normalY,
            float normalZ
    ) {
    }

    private record Point2(
            double x,
            double y
    ) {
        private static Point2 of(
                BlockPos pos,
                Direction.Axis axis
        ) {
            return switch (axis) {
                case X -> new Point2(
                        pos.getZ() + 0.5D,
                        pos.getY() + 0.5D
                );
                case Y -> new Point2(
                        pos.getX() + 0.5D,
                        pos.getZ() + 0.5D
                );
                case Z -> new Point2(
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D
                );
            };
        }

        private Point2 add(
                Point2 other
        ) {
            return new Point2(
                    x + other.x,
                    y + other.y
            );
        }

        private Point2 subtract(
                Point2 other
        ) {
            return new Point2(
                    x - other.x,
                    y - other.y
            );
        }

        private Point2 scale(
                double scale
        ) {
            return new Point2(
                    x * scale,
                    y * scale
            );
        }

        private Point2 leftNormal() {
            return new Point2(
                    -y,
                    x
            );
        }

        private double length() {
            return Math.sqrt(
                    x * x + y * y
            );
        }

        private double distanceTo(
                Point2 other
        ) {
            return subtract(other)
                    .length();
        }

        private Point2 normalize() {
            double length =
                    length();

            if (length < 0.0001D) {
                return new Point2(
                        0.0D,
                        0.0D
                );
            }

            return scale(
                    1.0D / length
            );
        }

        private boolean isZero() {
            return Math.abs(x) < 0.0001D
                    && Math.abs(y) < 0.0001D;
        }

        private double dot(
                Point2 other
        ) {
            return x * other.x
                    + y * other.y;
        }
    }
}
