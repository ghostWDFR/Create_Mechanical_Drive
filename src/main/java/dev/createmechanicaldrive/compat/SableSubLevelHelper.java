package dev.createmechanicaldrive.compat;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public final class SableSubLevelHelper {
    private SableSubLevelHelper() {
    }

    public static boolean isSameSubLevel(
            Level level,
            BlockPos first,
            BlockPos second
    ) {
        return Objects.equals(
                getSubLevelId(
                        level,
                        first
                ),
                getSubLevelId(
                        level,
                        second
                )
        );
    }

    @Nullable
    public static UUID getSubLevelId(
            Level level,
            BlockPos pos
    ) {
        SubLevel subLevel =
                getTypedSubLevel(
                        level,
                        pos
                );

        if (subLevel == null) {
            return null;
        }

        try {
            return subLevel.getUniqueId();
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    public static Object getSubLevel(
            Level level,
            BlockPos pos
    ) {
        return getTypedSubLevel(
                level,
                pos
        );
    }

    @Nullable
    private static SubLevel getTypedSubLevel(
            Level level,
            BlockPos pos
    ) {
        try {
            SubLevelContainer container =
                    SubLevelContainer.getContainer(
                            level
                    );

            if (container == null
                    || !container.inBounds(
                    pos
            )) {
                return null;
            }

            LevelPlot plot =
                    container.getPlot(
                            new ChunkPos(
                                    pos
                            )
                    );

            if (plot == null) {
                return null;
            }

            SubLevel subLevel =
                    plot.getSubLevel();

            if (subLevel == null
                    || subLevel.isRemoved()) {
                return null;
            }

            return subLevel;
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    public static Vec3 getWorldCenter(
            Level level,
            BlockPos pos
    ) {
        Vec3 center =
                Vec3.atCenterOf(
                        pos
                );

        SubLevel subLevel =
                getTypedSubLevel(
                        level,
                        pos
                );

        if (subLevel == null) {
            return center;
        }

        try {
            Pose3dc pose =
                    subLevel.logicalPose();

            return pose.transformPosition(
                    center
            );
        } catch (LinkageError | RuntimeException ignored) {
            return center;
        }
    }

    public static Vec3 getWorldPosition(
            Level level,
            BlockPos subLevelPos,
            Vec3 localPosition
    ) {
        SubLevel subLevel =
                getTypedSubLevel(
                        level,
                        subLevelPos
                );

        if (subLevel == null) {
            return localPosition;
        }

        try {
            return subLevel.logicalPose()
                    .transformPosition(
                            localPosition
                    );
        } catch (LinkageError | RuntimeException ignored) {
            return localPosition;
        }
    }

    public static Vec3 getLocalPosition(
            Level level,
            BlockPos subLevelPos,
            Vec3 worldPosition
    ) {
        SubLevel subLevel =
                getTypedSubLevel(
                        level,
                        subLevelPos
                );

        if (subLevel == null) {
            return worldPosition;
        }

        try {
            return subLevel.logicalPose()
                    .transformPositionInverse(
                            worldPosition
                    );
        } catch (LinkageError | RuntimeException ignored) {
            return worldPosition;
        }
    }

    public static Vec3 getWorldNormal(
            Level level,
            BlockPos pos,
            Vec3 normal
    ) {
        SubLevel subLevel =
                getTypedSubLevel(
                        level,
                        pos
                );

        if (subLevel == null) {
            return normal;
        }

        try {
            Vec3 transformed =
                    subLevel.logicalPose()
                            .transformNormal(
                                    normal
                            );

            if (transformed.lengthSqr() > 1.0E-8D) {
                return transformed.normalize();
            }
        } catch (LinkageError | RuntimeException ignored) {
            return normal;
        }

        return normal;
    }

    public static Vec3 getWorldAxis(
            Level level,
            BlockPos pos,
            net.minecraft.core.Direction.Axis axis
    ) {
        Vec3 normal =
                axisVector(axis);

        SubLevel subLevel =
                getTypedSubLevel(
                        level,
                        pos
                );

        if (subLevel == null) {
            return normal;
        }

        try {
            Vec3 transformed =
                    subLevel.logicalPose()
                            .transformNormal(
                                    normal
                            );

            if (transformed.lengthSqr() > 1.0E-8D) {
                return transformed.normalize();
            }
        } catch (LinkageError | RuntimeException ignored) {
            return normal;
        }

        return normal;
    }

    public static Vec3 getLocalNormal(
            Level level,
            BlockPos pos,
            Vec3 normal
    ) {
        SubLevel subLevel =
                getTypedSubLevel(
                        level,
                        pos
                );

        if (subLevel == null) {
            return normal;
        }

        try {
            Vec3 transformed =
                    subLevel.logicalPose()
                            .transformNormalInverse(
                                    normal
                            );

            if (transformed.lengthSqr() > 1.0E-8D) {
                return transformed.normalize();
            }
        } catch (LinkageError | RuntimeException ignored) {
            return normal;
        }

        return normal;
    }

    @Nullable
    public static BlockPos toTransferPosition(
            Level level,
            BlockPos pos
    ) {
        SubLevel subLevel =
                getTypedSubLevel(
                        level,
                        pos
                );

        if (subLevel == null) {
            return pos.immutable();
        }

        try {
            LevelPlot plot =
                    subLevel.getPlot();

            if (plot == null) {
                return null;
            }

            BlockPos center =
                    plot.getCenterBlock();

            return pos.subtract(
                    center
            );
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    public static BlockPos resolveTransferPosition(
            Level level,
            @Nullable UUID subLevelId,
            BlockPos transferPosition
    ) {
        if (subLevelId == null) {
            if (getSubLevelId(
                    level,
                    transferPosition
            ) != null) {
                return null;
            }

            return transferPosition.immutable();
        }

        try {
            SubLevelContainer container =
                    SubLevelContainer.getContainer(
                            level
                    );

            if (container == null) {
                return null;
            }

            SubLevel subLevel =
                    container.getSubLevel(
                            subLevelId
                    );

            if (subLevel == null
                    || subLevel.isRemoved()) {
                return null;
            }

            LevelPlot plot =
                    subLevel.getPlot();

            if (plot == null) {
                return null;
            }

            BlockPos center =
                    plot.getCenterBlock();

            BlockPos resolved =
                    center.offset(
                            transferPosition
                    );

            UUID resolvedSubLevelId =
                    getSubLevelId(
                            level,
                            resolved
                    );

            if (!Objects.equals(
                    subLevelId,
                    resolvedSubLevelId
            )) {
                return null;
            }

            return resolved;
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static Vec3 axisVector(
            net.minecraft.core.Direction.Axis axis
    ) {
        return switch (axis) {
            case X -> new Vec3(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3(0.0D, 0.0D, 1.0D);
        };
    }
}
