package dev.createmechanicaldrive.content.mechanical_jack;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.UUID;

final class MechanicalJackRenderOffsetHelper {
    private static boolean initialized;
    private static boolean available;
    private static Method getContainer;
    private static Method inBounds;
    private static Method containerGetPlot;
    private static Method containerGetSubLevel;
    private static Method plotGetSubLevel;
    private static Method subLevelGetLevel;
    private static Method subLevelGetPlot;
    private static Method plotGetBoundingBox;
    private static Method renderPose;
    private static Method transformPosition;
    private static Method transformPositionInverse;

    private MechanicalJackRenderOffsetHelper() {
    }

    @Nullable
    static Vec3 getRenderedHeadOffset(
            MechanicalJackBlockEntity blockEntity,
            Direction mount,
            float partialTick
    ) {
        if (!(blockEntity.getLevel() instanceof ClientLevel level)) {
            return null;
        }

        UUID headId =
                blockEntity.getHeadSubLevelId();

        if (headId == null) {
            return null;
        }

        ensureInitialized();

        if (!available) {
            return null;
        }

        try {
            Object container =
                    getContainer.invoke(
                            null,
                            level
                    );

            if (container == null) {
                return null;
            }

            Object baseSubLevel =
                    getContainingSubLevel(
                            container,
                            blockEntity.getBlockPos()
                    );

            Object headSubLevel =
                    containerGetSubLevel.invoke(
                            container,
                            headId
                    );

            if (baseSubLevel == null
                    || headSubLevel == null) {
                return null;
            }

            BlockPos headPos =
                    blockEntity.getHeadBlockPos();

            if (headPos == null
                    || !level.getBlockState(
                    headPos
            ).is(
                    CreateMechanicalDrive
                            .MECHANICAL_JACK_HEAD
                            .get()
            )) {
                headPos =
                        findHeadBlockPos(
                                headSubLevel
                        );
            }

            if (headPos == null) {
                return null;
            }

            Object basePose =
                    renderPose.invoke(
                            baseSubLevel,
                            partialTick
                    );

            Object headPose =
                    renderPose.invoke(
                            headSubLevel,
                            partialTick
                    );

            if (basePose == null
                    || headPose == null) {
                return null;
            }

            Vec3 baseAnchor =
                    Vec3.atCenterOf(
                            blockEntity.getBlockPos()
                    );

            Vec3 headAnchor =
                    Vec3.atCenterOf(
                            headPos
                    );

            Object headAnchorWorldObject =
                    transformPosition.invoke(
                            headPose,
                            headAnchor
                    );

            if (!(headAnchorWorldObject instanceof Vec3 headAnchorWorld)) {
                return null;
            }

            Object headAnchorInBaseObject =
                    transformPositionInverse.invoke(
                            basePose,
                            headAnchorWorld
                    );

            if (!(headAnchorInBaseObject instanceof Vec3 headAnchorInBase)) {
                return null;
            }

            Vec3 delta =
                    headAnchorInBase.subtract(
                            baseAnchor
                    );

            Vec3 axis =
                    directionVector(
                            mount
                    );

            double extension =
                    Mth.clamp(
                            dot(
                                    delta,
                                    axis
                            ),
                            MechanicalJackBlockEntity.MIN_EXTENSION,
                            MechanicalJackBlockEntity.MAX_EXTENSION
                    );

            return axis.scale(
                    extension
            );
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    @Nullable
    private static Object getContainingSubLevel(
            Object container,
            BlockPos pos
    ) throws ReflectiveOperationException {
        if (!(Boolean) inBounds.invoke(
                container,
                pos
        )) {
            return null;
        }

        Object plot =
                containerGetPlot.invoke(
                        container,
                        new net.minecraft.world.level.ChunkPos(
                                pos
                        )
                );

        if (plot == null) {
            return null;
        }

        return plotGetSubLevel.invoke(
                plot
        );
    }

    @Nullable
    private static BlockPos findHeadBlockPos(
            Object headSubLevel
    ) throws ReflectiveOperationException {
        Object levelObject =
                subLevelGetLevel.invoke(
                        headSubLevel
                );

        if (!(levelObject instanceof Level level)) {
            return null;
        }

        Object plot =
                subLevelGetPlot.invoke(
                        headSubLevel
                );

        if (plot == null) {
            return null;
        }

        Object bounds =
                plotGetBoundingBox.invoke(
                        plot
                );

        if (bounds == null) {
            return null;
        }

        int boundsMinX =
                invokeInt(
                        bounds,
                        "minX"
                );
        int boundsMinY =
                invokeInt(
                        bounds,
                        "minY"
                );
        int boundsMinZ =
                invokeInt(
                        bounds,
                        "minZ"
                );
        int boundsMaxX =
                invokeInt(
                        bounds,
                        "maxX"
                );
        int boundsMaxY =
                invokeInt(
                        bounds,
                        "maxY"
                );
        int boundsMaxZ =
                invokeInt(
                        bounds,
                        "maxZ"
                );

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        boundsMinX,
                        boundsMinY,
                        boundsMinZ,
                        boundsMaxX,
                        boundsMaxY,
                        boundsMaxZ
                )) {

            BlockPos immutable =
                    pos.immutable();

            if (level.getBlockState(
                    immutable
            ).is(
                    CreateMechanicalDrive
                            .MECHANICAL_JACK_HEAD
                            .get()
            )) {
                return immutable;
            }
        }

        return null;
    }

    private static int invokeInt(
            Object target,
            String methodName
    ) throws ReflectiveOperationException {
        return ((Number) target.getClass()
                .getMethod(
                        methodName
                )
                .invoke(
                        target
                )).intValue();
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }

        initialized =
                true;

        try {
            Class<?> subLevelContainerClass =
                    Class.forName(
                            "dev.ryanhcode.sable.api.sublevel.SubLevelContainer"
                    );
            Class<?> levelPlotClass =
                    Class.forName(
                            "dev.ryanhcode.sable.sublevel.plot.LevelPlot"
                    );
            Class<?> subLevelClass =
                    Class.forName(
                            "dev.ryanhcode.sable.sublevel.SubLevel"
                    );
            Class<?> clientSubLevelClass =
                    Class.forName(
                            "dev.ryanhcode.sable.sublevel.ClientSubLevel"
                    );
            Class<?> poseClass =
                    Class.forName(
                            "dev.ryanhcode.sable.companion.math.Pose3dc"
                    );

            getContainer =
                    subLevelContainerClass.getMethod(
                            "getContainer",
                            ClientLevel.class
                    );
            inBounds =
                    subLevelContainerClass.getMethod(
                            "inBounds",
                            BlockPos.class
                    );
            containerGetPlot =
                    subLevelContainerClass.getMethod(
                            "getPlot",
                            net.minecraft.world.level.ChunkPos.class
                    );
            containerGetSubLevel =
                    subLevelContainerClass.getMethod(
                            "getSubLevel",
                            UUID.class
                    );
            plotGetSubLevel =
                    levelPlotClass.getMethod(
                            "getSubLevel"
                    );
            subLevelGetLevel =
                    subLevelClass.getMethod(
                            "getLevel"
                    );
            subLevelGetPlot =
                    subLevelClass.getMethod(
                            "getPlot"
                    );
            plotGetBoundingBox =
                    levelPlotClass.getMethod(
                            "getBoundingBox"
                    );
            renderPose =
                    clientSubLevelClass.getMethod(
                            "renderPose",
                            float.class
                    );
            transformPosition =
                    poseClass.getMethod(
                            "transformPosition",
                            Vec3.class
                    );
            transformPositionInverse =
                    poseClass.getMethod(
                            "transformPositionInverse",
                            Vec3.class
                    );

            available =
                    true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            available =
                    false;
        }
    }

    private static Vec3 directionVector(
            Direction direction
    ) {
        return new Vec3(
                direction.getStepX(),
                direction.getStepY(),
                direction.getStepZ()
        );
    }

    private static double dot(
            Vec3 first,
            Vec3 second
    ) {
        return first.x * second.x
                + first.y * second.y
                + first.z * second.z;
    }
}