package dev.createmechanicaldrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3dc;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

public final class SableContraptionDragTransform {
    private static boolean initialized;
    private static boolean available;
    private static Method getContainer;
    private static Method inBounds;
    private static Method getPlot;
    private static Method getSubLevel;
    private static Method renderPose;
    private static Method renderPoseCurrent;
    private static Method rotationPoint;
    private static Method getRenderData;
    private static Method getTransformation;
    private static Method transformPosition;
    private static Method transformPositionInverse;
    private static Method transformNormal;
    private static Method transformNormalInverse;

    private SableContraptionDragTransform() {
    }

    @Nullable
    public static Vec3 renderWorldCenter(Level level, BlockPos pos) {
        Vec3 center =
                Vec3.atCenterOf(pos);

        Object pose =
                renderPose(
                        level,
                        pos
                );

        if (pose == null
                || transformPosition == null) {
            return center;
        }

        try {
            Object transformed =
                    transformPosition.invoke(
                            pose,
                            center
                    );

            return transformed instanceof Vec3 vec3
                    ? vec3
                    : center;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return center;
        }
    }

    @Nullable
    public static Vec3 worldPositionToLocal(Level level, BlockPos subLevelPos, Vec3 worldPosition) {
        Object pose =
                renderPose(
                        level,
                        subLevelPos
                );

        if (pose == null
                || transformPositionInverse == null) {
            return null;
        }

        try {
            Object transformed =
                    transformPositionInverse.invoke(
                            pose,
                            worldPosition
                    );

            return transformed instanceof Vec3 vec3
                    ? vec3
                    : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    @Nullable
    public static Vec3 worldNormalToLocal(Level level, BlockPos pos, Vec3 normal) {
        Object pose =
                renderPose(
                        level,
                        pos
                );

        if (pose == null
                || transformNormalInverse == null) {
            return null;
        }

        try {
            Object transformed = transformNormalInverse.invoke(pose, normal);
            return transformed instanceof Vec3 vec3 ? vec3 : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    @Nullable
    public static Vec3 localNormalToWorld(Level level, BlockPos pos, Vec3 normal) {
        Object pose =
                renderPose(
                        level,
                        pos
                );

        if (pose == null
                || transformNormal == null) {
            return null;
        }

        try {
            Object transformed = transformNormal.invoke(pose, normal);
            return transformed instanceof Vec3 vec3 ? vec3 : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    @Nullable
    public static Vec3 applyRenderTransform(
            Level level,
            BlockPos pos,
            Vec3 camera,
            PoseStack poseStack
    ) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return null;
        }

        ensureInitialized();
        if (!available
                || renderPoseCurrent == null
                || rotationPoint == null
                || getRenderData == null
                || getTransformation == null) {
            return null;
        }

        try {
            Object container =
                    getContainer.invoke(
                            null,
                            clientLevel
                    );
            if (container == null
                    || !(Boolean) inBounds.invoke(
                    container,
                    pos
            )) {
                return null;
            }

            Object plot =
                    getPlot.invoke(
                            container,
                            new ChunkPos(
                                    pos
                            )
                    );
            if (plot == null) {
                return null;
            }

            Object subLevel =
                    getSubLevel.invoke(
                            plot
                    );
            if (subLevel == null) {
                return null;
            }

            Object pose =
                    renderPoseCurrent.invoke(
                            subLevel
                    );
            if (pose == null) {
                return null;
            }

            Object origin =
                    rotationPoint.invoke(
                            pose
                    );

            Object renderData =
                    getRenderData.invoke(
                            subLevel
                    );

            Object transformation =
                    getTransformation.invoke(
                            renderData,
                            camera.x,
                            camera.y,
                            camera.z
                    );

            if (origin instanceof Vector3dc vector
                    && transformation instanceof Matrix4f matrix) {
                poseStack.mulPose(
                        matrix
                );

                return new Vec3(
                        vector.x(),
                        vector.y(),
                        vector.z()
                );
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }

        return null;
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }

        initialized = true;
        try {
            Class<?> subLevelContainerClass = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            Class<?> levelPlotClass = Class.forName("dev.ryanhcode.sable.sublevel.plot.LevelPlot");
            Class<?> clientSubLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.ClientSubLevel");
            Class<?> poseClass = Class.forName("dev.ryanhcode.sable.companion.math.Pose3dc");
            Class<?> renderDataClass = Class.forName("dev.ryanhcode.sable.sublevel.render.SubLevelRenderData");

            getContainer = subLevelContainerClass.getMethod("getContainer", ClientLevel.class);
            inBounds = subLevelContainerClass.getMethod("inBounds", BlockPos.class);
            getPlot = subLevelContainerClass.getMethod("getPlot", ChunkPos.class);
            getSubLevel = levelPlotClass.getMethod("getSubLevel");
            renderPose = clientSubLevelClass.getMethod("renderPose", float.class);
            renderPoseCurrent = clientSubLevelClass.getMethod("renderPose");
            rotationPoint = poseClass.getMethod("rotationPoint");
            getRenderData = clientSubLevelClass.getMethod("getRenderData");
            getTransformation = renderDataClass.getMethod("getTransformation", double.class, double.class, double.class);
            try {
                transformPosition = poseClass.getMethod("transformPosition", Vec3.class);
            } catch (ReflectiveOperationException ignored) {
                transformPosition = null;
            }
            try {
                transformPositionInverse = poseClass.getMethod("transformPositionInverse", Vec3.class);
            } catch (ReflectiveOperationException ignored) {
                transformPositionInverse = null;
            }
            transformNormalInverse = poseClass.getMethod("transformNormalInverse", Vec3.class);
            try {
                transformNormal = poseClass.getMethod("transformNormal", Vec3.class);
            } catch (ReflectiveOperationException ignored) {
                transformNormal = null;
            }
            available = true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            available = false;
        }
    }

    @Nullable
    private static Object renderPose(Level level, BlockPos pos) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return null;
        }

        ensureInitialized();
        if (!available
                || renderPose == null) {
            return null;
        }

        try {
            Object container = getContainer.invoke(null, clientLevel);
            if (container == null || !(Boolean) inBounds.invoke(container, pos)) {
                return null;
            }

            Object plot = getPlot.invoke(container, new ChunkPos(pos));
            if (plot == null) {
                return null;
            }

            Object subLevel = getSubLevel.invoke(plot);
            if (subLevel == null) {
                return null;
            }

            return renderPose.invoke(
                    subLevel,
                    Minecraft.getInstance()
                            .getTimer()
                            .getGameTimeDeltaPartialTick(true)
            );
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}
