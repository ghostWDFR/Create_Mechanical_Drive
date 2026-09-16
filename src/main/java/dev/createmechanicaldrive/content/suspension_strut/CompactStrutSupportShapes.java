package dev.createmechanicaldrive.content.suspension_strut;

import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the virtual endpoint shape of a one-block strut on the endpoint's
 * actual support block. This is deliberately keyed by the support's local
 * block coordinates: Sable can then bake and move the shape with the same
 * sublevel as that support instead of borrowing the other endpoint's frame.
 */
public final class CompactStrutSupportShapes {
    private static final Map<Level, ConcurrentHashMap<BlockPos, ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>>>
            ATTACHMENTS = Collections.synchronizedMap(new WeakHashMap<>());

    private CompactStrutSupportShapes() {
    }

    public static void register(
            Level level,
            BlockPos supportPos,
            Direction jointFacing,
            SuspensionStrutBlockEntity owner
    ) {
        attachments(level)
                .computeIfAbsent(
                        supportPos.immutable(),
                        ignored -> new ConcurrentHashMap<>()
                )
                .put(jointFacing, new WeakReference<>(owner));
    }

    public static void unregister(
            Level level,
            BlockPos supportPos,
            Direction jointFacing,
            SuspensionStrutBlockEntity owner
    ) {
        ConcurrentHashMap<BlockPos, ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>>
                attachments = attachmentsIfPresent(level);
        if (attachments == null) {
            return;
        }

        attachments.computeIfPresent(supportPos, (ignored, byFacing) -> {
            byFacing.computeIfPresent(jointFacing, (unused, reference) -> {
                SuspensionStrutBlockEntity registered = reference.get();
                return registered == null || registered == owner
                        ? null
                        : reference;
            });
            return byFacing.isEmpty() ? null : byFacing;
        });
    }

    public static VoxelShape addSupportJoint(
            BlockGetter getter,
            BlockPos supportPos,
            BlockState supportState,
            VoxelShape original
    ) {
        if (!(getter instanceof Level level) || supportState.isAir()) {
            return original;
        }

        ConcurrentHashMap<BlockPos, ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>>
                attachments = attachmentsIfPresent(level);
        if (attachments == null || attachments.isEmpty()) {
            return original;
        }
        ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>
                byFacing = attachments.get(supportPos);
        if (byFacing == null || byFacing.isEmpty()) {
            return original;
        }

        VoxelShape result = original;
        for (Map.Entry<Direction, WeakReference<SuspensionStrutBlockEntity>>
                entry : byFacing.entrySet()) {
            Direction facing = entry.getKey();
            WeakReference<SuspensionStrutBlockEntity> reference =
                    entry.getValue();
            SuspensionStrutBlockEntity owner = reference.get();
            if (!isCurrent(owner, level, supportPos, facing)) {
                byFacing.remove(facing, reference);
                continue;
            }

            result = Shapes.or(result, supportLocalJointShape(facing));
        }
        if (byFacing.isEmpty()) {
            attachments.remove(supportPos, byFacing);
        }
        return result;
    }

    public static SuspensionStrutBlockEntity findAttachedStrut(
            Level level,
            BlockPos supportPos,
            Vec3 hitLocation
    ) {
        ConcurrentHashMap<BlockPos, ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>>
                attachments = attachmentsIfPresent(level);
        if (attachments == null || attachments.isEmpty()) {
            return findAdjacentAttachedStrut(
                    level,
                    supportPos,
                    hitLocation
            );
        }
        ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>
                byFacing = attachments.get(supportPos);
        if (byFacing == null || byFacing.isEmpty()) {
            return findAdjacentAttachedStrut(
                    level,
                    supportPos,
                    hitLocation
            );
        }

        Vec3 localHitLocation = SableSubLevelHelper.getLocalPosition(
                level,
                supportPos,
                hitLocation
        );
        for (Map.Entry<Direction, WeakReference<SuspensionStrutBlockEntity>>
                entry : byFacing.entrySet()) {
            Direction facing = entry.getKey();
            WeakReference<SuspensionStrutBlockEntity> reference =
                    entry.getValue();
            SuspensionStrutBlockEntity owner = reference.get();
            if (!isCurrent(owner, level, supportPos, facing)) {
                byFacing.remove(facing, reference);
                continue;
            }

            AABB jointBounds = supportLocalInteractionShape(facing)
                    .bounds()
                    .move(supportPos)
                    .inflate(1.0E-4D);
            if (jointBounds.contains(localHitLocation)
                    && isOutsideSupport(
                            localHitLocation,
                            supportPos,
                            facing
                    )) {
                return owner;
            }
        }
        if (byFacing.isEmpty()) {
            attachments.remove(supportPos, byFacing);
        }
        return findAdjacentAttachedStrut(
                level,
                supportPos,
                hitLocation
        );
    }

    public static SuspensionStrutBlockEntity getAttachedStrut(
            Level level,
            BlockPos supportPos,
            Direction facing
    ) {
        ConcurrentHashMap<BlockPos, ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>>
                attachments = attachmentsIfPresent(level);
        if (attachments != null) {
            ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>
                    byFacing = attachments.get(supportPos);
            if (byFacing != null) {
                WeakReference<SuspensionStrutBlockEntity> reference =
                        byFacing.get(facing);
                SuspensionStrutBlockEntity owner = reference == null
                        ? null
                        : reference.get();
                if (isCurrent(owner, level, supportPos, facing)) {
                    return owner;
                }
                if (reference != null) {
                    byFacing.remove(facing, reference);
                }
            }
        }

        return level.getBlockEntity(supportPos.relative(facing))
                instanceof SuspensionStrutBlockEntity owner
                && isCurrent(owner, level, supportPos, facing)
                ? owner
                : null;
    }

    public static BlockHitResult clipSupportJoint(
            Level level,
            BlockPos supportPos,
            Direction facing,
            Vec3 worldStart,
            Vec3 worldEnd
    ) {
        if (getAttachedStrut(level, supportPos, facing) == null
                || level.getBlockState(supportPos).isAir()) {
            return null;
        }

        Vec3 localStart = SableSubLevelHelper.getLocalPosition(
                level,
                supportPos,
                worldStart
        );
        Vec3 localEnd = SableSubLevelHelper.getLocalPosition(
                level,
                supportPos,
                worldEnd
        );
        BlockHitResult localHit = supportLocalInteractionShape(facing).clip(
                localStart,
                localEnd,
                supportPos
        );
        if (localHit == null) {
            return null;
        }

        Vec3 worldHit = SableSubLevelHelper.getWorldPosition(
                level,
                supportPos,
                localHit.getLocation()
        );
        Vec3 worldNormal = SableSubLevelHelper.getWorldNormal(
                level,
                supportPos,
                Vec3.atLowerCornerOf(localHit.getDirection().getNormal())
        );
        return new BlockHitResult(
                worldHit,
                Direction.getNearest(
                        worldNormal.x,
                        worldNormal.y,
                        worldNormal.z
                ),
                supportPos,
                localHit.isInside()
        );
    }

    private static SuspensionStrutBlockEntity findAdjacentAttachedStrut(
            Level level,
            BlockPos supportPos,
            Vec3 hitLocation
    ) {
        Vec3 localHitLocation = SableSubLevelHelper.getLocalPosition(
                level,
                supportPos,
                hitLocation
        );
        for (Direction facing : Direction.values()) {
            if (!(level.getBlockEntity(supportPos.relative(facing))
                    instanceof SuspensionStrutBlockEntity owner)
                    || owner.isRemoved()
                    || !owner.isCompactLink()
                    || owner.getLinkedFacing() != facing) {
                continue;
            }

            AABB jointBounds = supportLocalInteractionShape(facing)
                    .bounds()
                    .move(supportPos)
                    .inflate(1.0E-4D);
            if (jointBounds.contains(localHitLocation)
                    && isOutsideSupport(
                            localHitLocation,
                            supportPos,
                            facing
                    )) {
                return owner;
            }
        }
        return null;
    }

    public static BlockHitResult clipSupportJoints(
            Level level,
            Vec3 worldStart,
            Vec3 worldEnd
    ) {
        ConcurrentHashMap<BlockPos, ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>>
                attachments = attachmentsIfPresent(level);
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }

        BlockHitResult closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (Map.Entry<BlockPos, ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>>
                supportEntry : attachments.entrySet()) {
            BlockPos supportPos = supportEntry.getKey();
            if (level.getBlockState(supportPos).isAir()) {
                continue;
            }

            Vec3 localStart = SableSubLevelHelper.getLocalPosition(
                    level,
                    supportPos,
                    worldStart
            );
            Vec3 localEnd = SableSubLevelHelper.getLocalPosition(
                    level,
                    supportPos,
                    worldEnd
            );
            ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>
                    byFacing = supportEntry.getValue();
            for (Map.Entry<Direction, WeakReference<SuspensionStrutBlockEntity>>
                    facingEntry : byFacing.entrySet()) {
                Direction facing = facingEntry.getKey();
                WeakReference<SuspensionStrutBlockEntity> reference =
                        facingEntry.getValue();
                SuspensionStrutBlockEntity owner = reference.get();
                if (!isCurrent(owner, level, supportPos, facing)) {
                    byFacing.remove(facing, reference);
                    continue;
                }

                BlockHitResult localHit =
                        supportLocalInteractionShape(facing).clip(
                        localStart,
                        localEnd,
                        supportPos
                );
                if (localHit == null) {
                    continue;
                }

                Vec3 worldHit = SableSubLevelHelper.getWorldPosition(
                        level,
                        supportPos,
                        localHit.getLocation()
                );
                double distance = worldHit.distanceToSqr(worldStart);
                if (distance >= closestDistance) {
                    continue;
                }

                Vec3 worldNormal = SableSubLevelHelper.getWorldNormal(
                        level,
                        supportPos,
                        Vec3.atLowerCornerOf(localHit.getDirection().getNormal())
                );
                closestDistance = distance;
                closest = new BlockHitResult(
                        worldHit,
                        Direction.getNearest(
                                worldNormal.x,
                                worldNormal.y,
                                worldNormal.z
                        ),
                        supportPos,
                        localHit.isInside()
                );
            }
            if (byFacing.isEmpty()) {
                attachments.remove(supportPos, byFacing);
            }
        }
        return closest;
    }

    public static VoxelShape getHitSupportJointShape(
            Level level,
            BlockPos supportPos,
            Vec3 worldHitLocation
    ) {
        ConcurrentHashMap<BlockPos, ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>>
                attachments = attachmentsIfPresent(level);
        if (attachments == null) {
            return null;
        }
        ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>
                byFacing = attachments.get(supportPos);
        if (byFacing == null) {
            return null;
        }

        Vec3 localHitLocation = SableSubLevelHelper.getLocalPosition(
                level,
                supportPos,
                worldHitLocation
        );
        for (Map.Entry<Direction, WeakReference<SuspensionStrutBlockEntity>>
                entry : byFacing.entrySet()) {
            Direction facing = entry.getKey();
            SuspensionStrutBlockEntity owner = entry.getValue().get();
            if (!isCurrent(owner, level, supportPos, facing)) {
                continue;
            }
            VoxelShape shape = supportLocalInteractionShape(facing);
            if (shape.bounds()
                    .move(supportPos)
                    .inflate(1.0E-4D)
                    .contains(localHitLocation)
                    && isOutsideSupport(
                            localHitLocation,
                            supportPos,
                            facing
                    )) {
                return shape;
            }
        }
        return null;
    }

    public static void breakAttachedStruts(
            ServerLevel level,
            BlockPos supportPos
    ) {
        ConcurrentHashMap<BlockPos, ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>>
                attachments = attachmentsIfPresent(level);
        if (attachments == null) {
            return;
        }
        ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>
                byFacing = attachments.get(supportPos);
        if (byFacing == null || byFacing.isEmpty()) {
            return;
        }

        Set<SuspensionStrutBlockEntity> owners =
                Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Map.Entry<Direction, WeakReference<SuspensionStrutBlockEntity>>
                entry : byFacing.entrySet()) {
            SuspensionStrutBlockEntity owner = entry.getValue().get();
            if (isCurrent(owner, level, supportPos, entry.getKey())) {
                owners.add(owner);
            }
        }
        for (SuspensionStrutBlockEntity owner : owners) {
            owner.onCompactSupportBroken(level, supportPos);
        }
    }

    private static boolean isCurrent(
            SuspensionStrutBlockEntity owner,
            Level level,
            BlockPos supportPos,
            Direction facing
    ) {
        return owner != null
                && !owner.isRemoved()
                && owner.getLevel() == level
                && owner.isCompactLink()
                && supportPos.equals(owner.getLinkedTransformPos())
                && facing == owner.getLinkedFacing();
    }

    private static VoxelShape supportLocalJointShape(Direction facing) {
        return SuspensionStrutBlock.jointShape(facing).move(
                facing.getStepX(),
                facing.getStepY(),
                facing.getStepZ()
        );
    }

    private static VoxelShape supportLocalInteractionShape(
            Direction facing
    ) {
        return supportLocalJointShape(facing);
    }

    private static boolean isOutsideSupport(
            Vec3 localHit,
            BlockPos supportPos,
            Direction facing
    ) {
        double epsilon = 1.0E-5D;
        return switch (facing) {
            case DOWN -> localHit.y < supportPos.getY() - epsilon;
            case UP -> localHit.y > supportPos.getY() + 1.0D + epsilon;
            case NORTH -> localHit.z < supportPos.getZ() - epsilon;
            case SOUTH -> localHit.z > supportPos.getZ() + 1.0D + epsilon;
            case WEST -> localHit.x < supportPos.getX() - epsilon;
            case EAST -> localHit.x > supportPos.getX() + 1.0D + epsilon;
        };
    }

    private static ConcurrentHashMap<BlockPos, ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>>
    attachments(Level level) {
        synchronized (ATTACHMENTS) {
            return ATTACHMENTS.computeIfAbsent(
                    level,
                    ignored -> new ConcurrentHashMap<>()
            );
        }
    }

    private static ConcurrentHashMap<BlockPos, ConcurrentHashMap<Direction, WeakReference<SuspensionStrutBlockEntity>>>
    attachmentsIfPresent(Level level) {
        synchronized (ATTACHMENTS) {
            return ATTACHMENTS.get(level);
        }
    }
}
