package dev.createmechanicaldrive.content.suspension_strut;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity;
import dev.simulated_team.simulated.util.SimLevelUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.Objects;
import java.util.UUID;

public class SuspensionStrutBlockEntity extends SpringBlockEntity {
    public static final double MIN_LENGTH = 1.0D;
    public static final double MAX_LENGTH = 5.0D;

    private static final String TAG_PENDING = "Pending";
    private static final String TAG_COMPACT_LINK = "CompactLink";
    private static final String TAG_COMPACT_FACING = "CompactFacing";
    private static final String TAG_COMPACT_SUPPORT_POS = "CompactSupportPos";
    private static final String TAG_COMPACT_SUPPORT_SUB_LEVEL =
            "CompactSupportSubLevel";
    private static final String TAG_COMPACT_SUPPORT_WORLD_X =
            "CompactSupportWorldX";
    private static final String TAG_COMPACT_SUPPORT_WORLD_Y =
            "CompactSupportWorldY";
    private static final String TAG_COMPACT_SUPPORT_WORLD_Z =
            "CompactSupportWorldZ";
    private static final String TAG_SETTINGS = "MechanicalDriveStrutSettings";
    private static final String TAG_INSTALLATION_POSE = "InstallationPose";
    private static final String TAG_AXIS_X = "AxisX";
    private static final String TAG_AXIS_Y = "AxisY";
    private static final String TAG_AXIS_Z = "AxisZ";
    private static final String TAG_ROTATION_X = "RotationX";
    private static final String TAG_ROTATION_Y = "RotationY";
    private static final String TAG_ROTATION_Z = "RotationZ";
    private static final String TAG_ROTATION_W = "RotationW";
    private static final String TAG_INSTALLATION_LENGTH = "RestLength";
    private static final int PENDING_LIFETIME_TICKS = 20 * 30;
    private static final double MAX_BEND_ANGLE_COS =
            Math.cos(Math.toRadians(80.0D));
    private static final double GEOMETRY_EPSILON = 1.0E-5D;
    private static final double PHYSICS_EPSILON = 1.0E-7D;
    private static final double MIN_INERTIA_DETERMINANT = 1.0E-12D;
    private static final double BASE_SPRING_STIFFNESS = 145.0D;
    private static final double BASE_LINEAR_DAMPING = 4.5D;
    private static final double BASE_ANGULAR_STIFFNESS = 20.0D;
    private static final double BASE_ANGULAR_DAMPING = 2.0D;
    private static final double MAX_RUNTIME_LENGTH = MAX_LENGTH;
    private static final double MIN_RUPTURE_STRETCH_SPEED = 4.0D;
    private static final double RUPTURE_EPSILON = 1.0E-4D;
    private static final int COMPACT_SUPPORT_REBIND_GRACE_TICKS = 40;
    private static final double COMPACT_SUPPORT_REBIND_DISTANCE_SQUARED =
            0.25D * 0.25D;

    private boolean pending;
    private int pendingTicks;
    private boolean destroyingStrut;
    private boolean supportDropClaimed;
    private boolean compactLink;
    private Direction compactPartnerFacing = Direction.DOWN;
    @Nullable
    private BlockPos compactPartnerSupportPos;
    @Nullable
    private UUID compactPartnerSubLevel;
    private final Vector3d compactPartnerLastWorldCenter = new Vector3d();
    private boolean hasCompactPartnerLastWorldCenter;
    private int compactMissingSupportTicks;
    @Nullable
    private Level registeredCompactSupportLevel;
    @Nullable
    private BlockPos registeredCompactSupportPos;
    @Nullable
    private Direction registeredCompactSupportFacing;
    private final SuspensionStrutSettings settings =
            new SuspensionStrutSettings(MIN_LENGTH);
    private final Vector3d installationAxisLocal =
            new Vector3d(0.0D, 1.0D, 0.0D);
    private final Quaterniond installationRelativeRotation =
            new Quaterniond();
    private double installationLength = MIN_LENGTH;
    private boolean hasInstallationPose;
    private final ForceTotal ownForces = new ForceTotal();
    private final ForceTotal partnerForces = new ForceTotal();

    public SuspensionStrutBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        refreshAdjacentCompactSupportBinding();
        refreshCompactSupportShapeRegistration();

        if (pending) {
            if (level != null
                    && !level.isClientSide
                    && ++pendingTicks > PENDING_LIFETIME_TICKS) {
                level.destroyBlock(getBlockPos(), false);
            }
            return;
        }

        // Aeronautics treats a null partnerPos as an orphan and destroys it on
        // the server. A compact strut deliberately stores its second endpoint
        // in this block entity instead of a second block, so only keep the
        // parent client animation tick for that representation.
        if (!compactLink || level == null || level.isClientSide) {
            super.tick();
        }

        if (level instanceof ServerLevel serverLevel && compactLink) {
            CompactSupportStatus supportStatus =
                    ensureCompactPartnerSupport(serverLevel);
            if (supportStatus != CompactSupportStatus.READY) {
                if (supportStatus == CompactSupportStatus.UNLOADED) {
                    compactMissingSupportTicks = 0;
                } else if (++compactMissingSupportTicks
                        > COMPACT_SUPPORT_REBIND_GRACE_TICKS) {
                    destroyFullStrut(serverLevel);
                }
                return;
            }
        }
        compactMissingSupportTicks = 0;

        if (level instanceof ServerLevel
                && compactLink
                && SableSubLevelHelper.getSubLevelId(level, worldPosition) == null) {
            applyStrutPhysics(null, null, 1.0D / 20.0D);
        }

        if (level instanceof ServerLevel serverLevel
                && !isRemoved()
                && !assembling
                && isController()
                && hasLink()) {
            LinkGeometry geometry = resolveLinkGeometry(null);
            if (geometry != null
                    && (geometry.distance() > MAX_RUNTIME_LENGTH + RUPTURE_EPSILON
                    || !isAngleValid(geometry))) {
                destroyFullStrut(serverLevel);
            }
        }
    }

    @Override
    public void sable$physicsTick(
            ServerSubLevel ownSubLevel,
            RigidBodyHandle ownHandle,
            double timeStep
    ) {
        applyStrutPhysics(ownSubLevel, ownHandle, timeStep);
    }

    private void applyStrutPhysics(
            @Nullable ServerSubLevel ownSubLevel,
            @Nullable RigidBodyHandle ownHandle,
            double timeStep
    ) {
        if (level == null
                || level.isClientSide
                || pending
                || !hasLink()
                || timeStep <= 0.0D
                || !Double.isFinite(timeStep)) {
            return;
        }
        if (compactLink
                && level instanceof ServerLevel serverLevel
                && !isCompactSupportAt(
                serverLevel,
                compactPartnerSubLevel,
                compactPartnerSupportPos()
        )) {
            // A moved endpoint is rebound by the regular server tick. Until
            // then, and especially after its support was broken, applying a
            // force to the stale body is what launches the opposite sublevel.
            return;
        }

        UUID ownSubLevelId = SableSubLevelHelper.getSubLevelId(level, worldPosition);
        if (ownSubLevel != null
                && !Objects.equals(ownSubLevelId, ownSubLevel.getUniqueId())) {
            return;
        }

        SuspensionStrutBlockEntity other = getPairedSpring();
        if (!compactLink && other == null) {
            return;
        }

        BlockPos loadPos = compactLink
                ? compactPartnerSupportPos()
                : partnerPos;
        if (loadPos == null
                || !SimLevelUtil.isAreaActuallyLoaded(level, loadPos, 1)) {
            return;
        }

        UUID otherSubLevelId = compactLink
                ? SableSubLevelHelper.getSubLevelId(level, loadPos)
                : getPartnerSubLevelID();
        if (compactLink && !Objects.equals(
                compactPartnerSubLevel,
                otherSubLevelId
        )) {
            compactPartnerSubLevel = otherSubLevelId;
            setChanged();
            sendData();
        }
        ServerSubLevel otherSubLevel = resolveServerSubLevel(otherSubLevelId);
        if (otherSubLevelId != null && otherSubLevel == null) {
            return;
        }
        if (ownSubLevel == null && otherSubLevel == null
                || ownSubLevel != null && otherSubLevel == ownSubLevel) {
            return;
        }

        // With two moving bodies only the controller applies the pair. If one
        // end is in the world, the moving endpoint must perform the update.
        if (!compactLink && otherSubLevel != null && !isController()) {
            return;
        }

        Direction ownFacing = getBlockState()
                .getValue(SuspensionStrutBlock.FACING);
        Direction otherFacing = compactLink
                ? compactPartnerFacing
                : other.getBlockState().getValue(SuspensionStrutBlock.FACING);
        Vector3d ownLocal = localAttachment(worldPosition, ownFacing);
        Vector3d otherLocal = compactLink
                ? compactPartnerAttachment()
                : localAttachment(other.worldPosition, otherFacing);
        Vector3d ownWorld = toWorldPosition(ownSubLevel, ownLocal);
        Vector3d otherWorld = toWorldPosition(otherSubLevel, otherLocal);
        Vector3d delta = otherWorld.sub(ownWorld, new Vector3d());
        double distance = delta.length();
        if (!Double.isFinite(distance) || distance < PHYSICS_EPSILON) {
            return;
        }
        if (distance > MAX_RUNTIME_LENGTH + RUPTURE_EPSILON) {
            if (level instanceof ServerLevel serverLevel) {
                destroyFullStrut(serverLevel);
            }
            return;
        }

        Vector3d ownVelocity = pointVelocity(ownSubLevel, ownLocal);
        Vector3d otherVelocity = pointVelocity(otherSubLevel, otherLocal);
        Vector3d relativeVelocity = otherVelocity.sub(
                ownVelocity,
                new Vector3d()
        );
        Vector3d ownAngularVelocity = ownHandle == null
                ? new Vector3d()
                : ownHandle.getAngularVelocity(new Vector3d());
        Vector3d otherAngularVelocity = otherSubLevel == null
                ? new Vector3d()
                : RigidBodyHandle.of(otherSubLevel)
                .getAngularVelocity(new Vector3d());

        Vector3d worldForce;
        Vector3d ownAngularImpulse = new Vector3d();
        switch (settings.behavior()) {
            case AXIAL -> worldForce = axialForce(
                    delta,
                    distance
            );
            case LOCKED_AXIS -> {
                worldForce = lockedAxisForce(
                        ownSubLevel,
                        delta
                );
                ownAngularImpulse = lockedPoseAngularImpulse(
                        ownSubLevel,
                        otherSubLevel,
                        timeStep
                );
            }
            case ALIGN -> {
                worldForce = alignmentForce(
                        ownSubLevel,
                        otherSubLevel,
                        ownFacing,
                        otherFacing,
                        delta
                );
                ownAngularImpulse = alignmentAngularImpulse(
                        ownSubLevel,
                        otherSubLevel,
                        ownFacing,
                        otherFacing,
                        timeStep
                );
            }
            default -> throw new IllegalStateException(
                    "Unknown Suspension Strut behavior: " + settings.behavior()
            );
        }

        Vector3d dampingVelocity = linearDampingVelocity(
                settings.behavior(),
                delta,
                distance,
                relativeVelocity,
                ownAngularVelocity,
                otherAngularVelocity
        );
        double linearStiffness = switch (settings.behavior()) {
            case AXIAL -> BASE_SPRING_STIFFNESS * settings.strength();
            case ALIGN -> BASE_SPRING_STIFFNESS
                    * settings.strength()
                    * 0.5D;
            case LOCKED_AXIS -> BASE_SPRING_STIFFNESS * Math.max(
                    settings.strength(),
                    settings.constraint() * 0.5D
            );
        };
        Vector3d elasticWorldImpulse = stablePointElasticImpulse(
                ownSubLevel,
                ownLocal,
                otherSubLevel,
                otherLocal,
                worldForce,
                linearStiffness,
                timeStep
        );
        Vector3d dampingWorldImpulse = stablePointDampingImpulse(
                ownSubLevel,
                ownLocal,
                otherSubLevel,
                otherLocal,
                dampingVelocity,
                BASE_LINEAR_DAMPING * settings.damping(),
                timeStep
        );
        Vector3d ownDampingImpulse = passivePointDampingImpulse(
                ownSubLevel,
                ownLocal,
                ownVelocity,
                dampingWorldImpulse
        );
        Vector3d otherDampingImpulse = passivePointDampingImpulse(
                otherSubLevel,
                otherLocal,
                otherVelocity,
                dampingWorldImpulse.negate(new Vector3d())
        );
        Vector3d rawOwnWorldImpulse = new Vector3d(elasticWorldImpulse)
                .add(ownDampingImpulse);
        Vector3d rawOtherWorldImpulse = elasticWorldImpulse.negate(
                new Vector3d()
        ).add(otherDampingImpulse);

        Vector3d ownAngularDampingImpulse = new Vector3d();
        Vector3d otherAngularDampingImpulse = new Vector3d();
        if (settings.behavior() != SuspensionStrutSettings.Behavior.AXIAL) {
            ownAngularImpulse = stableAngularElasticImpulse(
                    ownSubLevel,
                    otherSubLevel,
                    ownAngularImpulse,
                    BASE_ANGULAR_STIFFNESS * settings.constraint(),
                    timeStep
            );
            Vector3d relativeAngularVelocity = otherAngularVelocity.sub(
                    ownAngularVelocity,
                    new Vector3d()
            );
            Vector3d angularDampingImpulse = stableAngularDampingImpulse(
                    ownSubLevel,
                    otherSubLevel,
                    relativeAngularVelocity,
                    BASE_ANGULAR_DAMPING * settings.damping(),
                    timeStep
            );
            ownAngularDampingImpulse = passiveAngularDampingImpulse(
                    ownSubLevel,
                    ownAngularVelocity,
                    angularDampingImpulse
            );
            otherAngularDampingImpulse = passiveAngularDampingImpulse(
                    otherSubLevel,
                    otherAngularVelocity,
                    angularDampingImpulse.negate(new Vector3d())
            );
        }
        Vector3d currentAxis = delta.div(distance, new Vector3d());
        double separatingSpeed = relativeVelocity.dot(currentAxis);
        double tensileImpulse = rawOwnWorldImpulse.dot(currentAxis);
        if (separatingSpeed > MIN_RUPTURE_STRETCH_SPEED
                && tensileImpulse
                > settings.maxImpulse() + RUPTURE_EPSILON) {
            if (level instanceof ServerLevel serverLevel) {
                destroyFullStrut(serverLevel);
            }
            return;
        }

        Vector3d ownWorldImpulse = clampMagnitude(
                rawOwnWorldImpulse,
                settings.maxImpulse()
        );
        Vector3d otherWorldImpulse = clampMagnitude(
                rawOtherWorldImpulse,
                settings.maxImpulse()
        );
        Vector3d ownWorldAngularImpulse = clampMagnitude(
                new Vector3d(ownAngularImpulse)
                        .add(ownAngularDampingImpulse),
                settings.maxImpulse()
        );
        Vector3d otherWorldAngularImpulse = clampMagnitude(
                ownAngularImpulse.negate(new Vector3d())
                        .add(otherAngularDampingImpulse),
                settings.maxImpulse()
        );

        applyImpulsePair(
                ownSubLevel,
                ownHandle,
                ownLocal,
                otherSubLevel,
                otherLocal,
                ownWorldImpulse,
                otherWorldImpulse,
                ownWorldAngularImpulse,
                otherWorldAngularImpulse,
                elasticWorldImpulse
        );
    }

    private Vector3d axialForce(
            Vector3d delta,
            double distance
    ) {
        Vector3d axis = delta.div(distance, new Vector3d());
        double extension = distance - settings.length();
        double magnitude = BASE_SPRING_STIFFNESS
                * settings.strength()
                * extension;
        return axis.mul(magnitude);
    }

    private Vector3d alignmentForce(
            ServerSubLevel ownSubLevel,
            @Nullable ServerSubLevel otherSubLevel,
            Direction ownFacing,
            Direction otherFacing,
            Vector3d delta
    ) {
        Vector3d ownNormal = worldFacing(ownSubLevel, ownFacing);
        Vector3d otherNormal = worldFacing(otherSubLevel, otherFacing).negate();
        Vector3d average = ownNormal.add(otherNormal, new Vector3d());
        if (average.lengthSquared() < PHYSICS_EPSILON) {
            average.set(delta);
        }
        average.normalize();

        Vector3d targetDelta = average.mul(
                settings.length(),
                new Vector3d()
        );
        Vector3d positionError = delta.sub(
                targetDelta,
                new Vector3d()
        );
        return positionError.mul(
                BASE_SPRING_STIFFNESS
                        * settings.strength()
                        * 0.5D
        );
    }

    private Vector3d lockedAxisForce(
            @Nullable ServerSubLevel ownSubLevel,
            Vector3d delta
    ) {
        Vector3d worldAxis = ownSubLevel == null
                ? new Vector3d(installationAxisLocal)
                : ownSubLevel.logicalPose().transformNormal(
                        installationAxisLocal,
                        new Vector3d()
                );
        if (worldAxis.lengthSquared() < PHYSICS_EPSILON) {
            worldAxis.set(delta);
        }
        worldAxis.normalize();
        if (worldAxis.dot(delta) < 0.0D) {
            worldAxis.negate();
        }

        double axialDistance = delta.dot(worldAxis);
        Vector3d axial = worldAxis.mul(
                BASE_SPRING_STIFFNESS
                        * settings.strength()
                        * (axialDistance - settings.length()),
                new Vector3d()
        );

        Vector3d lateralError = delta.sub(
                worldAxis.mul(axialDistance, new Vector3d()),
                new Vector3d()
        );
        return axial.fma(
                BASE_SPRING_STIFFNESS
                        * settings.constraint()
                        * 0.5D,
                lateralError
        );
    }

    private static Vector3d linearDampingVelocity(
            SuspensionStrutSettings.Behavior behavior,
            Vector3d delta,
            double distance,
            Vector3d relativeVelocity,
            Vector3d ownAngularVelocity,
            Vector3d otherAngularVelocity
    ) {
        if (behavior == SuspensionStrutSettings.Behavior.AXIAL) {
            Vector3d axis = delta.div(distance, new Vector3d());
            return axis.mul(relativeVelocity.dot(axis));
        }

        Vector3d frameAngularVelocity = behavior
                == SuspensionStrutSettings.Behavior.LOCKED_AXIS
                ? new Vector3d(ownAngularVelocity)
                : ownAngularVelocity.add(
                        otherAngularVelocity,
                        new Vector3d()
                ).mul(0.5D);
        Vector3d frameVelocity = frameAngularVelocity.cross(
                delta,
                new Vector3d()
        );
        return relativeVelocity.sub(frameVelocity, new Vector3d());
    }

    private static Vector3d stablePointElasticImpulse(
            @Nullable ServerSubLevel ownSubLevel,
            Vector3d ownLocal,
            @Nullable ServerSubLevel otherSubLevel,
            Vector3d otherLocal,
            Vector3d elasticForce,
            double stiffness,
            double timeStep
    ) {
        Vector3d impulse = elasticForce.mul(timeStep, new Vector3d());
        double magnitude = impulse.length();
        if (!Double.isFinite(magnitude)
                || magnitude < PHYSICS_EPSILON) {
            return new Vector3d();
        }

        Vector3d worldDirection = impulse.div(
                magnitude,
                new Vector3d()
        );
        double inverseEffectiveMass = inverseNormalMass(
                ownSubLevel,
                ownLocal,
                worldDirection
        ) + inverseNormalMass(
                otherSubLevel,
                otherLocal,
                worldDirection
        );
        if (!Double.isFinite(inverseEffectiveMass)
                || inverseEffectiveMass <= PHYSICS_EPSILON) {
            return new Vector3d();
        }

        double denominator = 1.0D + inverseEffectiveMass
                * stiffness
                * timeStep
                * timeStep;
        if (!Double.isFinite(denominator) || denominator <= 0.0D) {
            return new Vector3d();
        }
        return impulse.div(denominator);
    }

    private static Vector3d stablePointDampingImpulse(
            @Nullable ServerSubLevel ownSubLevel,
            Vector3d ownLocal,
            @Nullable ServerSubLevel otherSubLevel,
            Vector3d otherLocal,
            Vector3d relativeVelocity,
            double dampingCoefficient,
            double timeStep
    ) {
        double speed = relativeVelocity.length();
        if (!Double.isFinite(speed)
                || speed < PHYSICS_EPSILON
                || dampingCoefficient <= 0.0D) {
            return new Vector3d();
        }

        Vector3d worldDirection = relativeVelocity.div(
                speed,
                new Vector3d()
        );
        double inverseEffectiveMass = inverseNormalMass(
                ownSubLevel,
                ownLocal,
                worldDirection
        ) + inverseNormalMass(
                otherSubLevel,
                otherLocal,
                worldDirection
        );
        return stableDampingImpulse(
                relativeVelocity,
                dampingCoefficient,
                inverseEffectiveMass,
                timeStep
        );
    }

    private static Vector3d passivePointDampingImpulse(
            @Nullable ServerSubLevel subLevel,
            Vector3d localPoint,
            Vector3d pointVelocity,
            Vector3d desiredWorldImpulse
    ) {
        if (subLevel == null) {
            return new Vector3d();
        }
        double magnitudeSquared = desiredWorldImpulse.lengthSquared();
        if (!Double.isFinite(magnitudeSquared)
                || magnitudeSquared < PHYSICS_EPSILON) {
            return new Vector3d();
        }

        double magnitude = Math.sqrt(magnitudeSquared);
        Vector3d worldDirection = desiredWorldImpulse.div(
                magnitude,
                new Vector3d()
        );
        double inverseMass = inverseNormalMass(
                subLevel,
                localPoint,
                worldDirection
        );
        double velocityWork = pointVelocity.dot(desiredWorldImpulse);
        if (!Double.isFinite(velocityWork)
                || velocityWork >= -PHYSICS_EPSILON
                || inverseMass <= PHYSICS_EPSILON) {
            return new Vector3d();
        }

        // Never let damping push an endpoint through zero velocity. This is
        // the per-body passivity clamp used by Aeronautics' spring: damping
        // may remove kinetic energy, but cannot transfer it into a light
        // suspension sublevel and start a lateral oscillation.
        double stoppingScale = -velocityWork
                / (inverseMass * magnitudeSquared);
        if (!Double.isFinite(stoppingScale) || stoppingScale <= 0.0D) {
            return new Vector3d();
        }
        return desiredWorldImpulse.mul(
                Math.min(1.0D, stoppingScale),
                new Vector3d()
        );
    }

    private static double inverseNormalMass(
            @Nullable ServerSubLevel subLevel,
            Vector3d localPoint,
            Vector3d worldDirection
    ) {
        if (subLevel == null) {
            return 0.0D;
        }
        Vector3d localDirection = subLevel.logicalPose()
                .transformNormalInverse(
                        worldDirection,
                        new Vector3d()
                );
        double inverseMass = subLevel.getMassTracker()
                .getInverseNormalMass(localPoint, localDirection);
        return Double.isFinite(inverseMass) && inverseMass > 0.0D
                ? inverseMass
                : 0.0D;
    }

    private static Vector3d stableAngularElasticImpulse(
            @Nullable ServerSubLevel ownSubLevel,
            @Nullable ServerSubLevel otherSubLevel,
            Vector3d elasticImpulse,
            double stiffness,
            double timeStep
    ) {
        Vector3d impulse = new Vector3d(elasticImpulse);
        double magnitude = impulse.length();
        if (!Double.isFinite(magnitude)
                || magnitude < PHYSICS_EPSILON) {
            return new Vector3d();
        }

        Vector3d worldDirection = impulse.div(
                magnitude,
                new Vector3d()
        );
        double inverseAngularMass = inverseAngularMass(
                ownSubLevel,
                worldDirection
        ) + inverseAngularMass(
                otherSubLevel,
                worldDirection
        );
        if (!Double.isFinite(inverseAngularMass)
                || inverseAngularMass <= PHYSICS_EPSILON) {
            return new Vector3d();
        }

        double denominator = 1.0D + inverseAngularMass
                * stiffness
                * timeStep
                * timeStep;
        if (!Double.isFinite(denominator) || denominator <= 0.0D) {
            return new Vector3d();
        }
        return impulse.div(denominator);
    }

    private static Vector3d stableAngularDampingImpulse(
            @Nullable ServerSubLevel ownSubLevel,
            @Nullable ServerSubLevel otherSubLevel,
            Vector3d relativeAngularVelocity,
            double dampingCoefficient,
            double timeStep
    ) {
        double speed = relativeAngularVelocity.length();
        if (!Double.isFinite(speed)
                || speed < PHYSICS_EPSILON
                || dampingCoefficient <= 0.0D) {
            return new Vector3d();
        }

        Vector3d worldDirection = relativeAngularVelocity.div(
                speed,
                new Vector3d()
        );
        double inverseEffectiveMass = inverseAngularMass(
                ownSubLevel,
                worldDirection
        ) + inverseAngularMass(
                otherSubLevel,
                worldDirection
        );
        return stableDampingImpulse(
                relativeAngularVelocity,
                dampingCoefficient,
                inverseEffectiveMass,
                timeStep
        );
    }

    private static Vector3d passiveAngularDampingImpulse(
            @Nullable ServerSubLevel subLevel,
            Vector3d angularVelocity,
            Vector3d desiredWorldImpulse
    ) {
        if (subLevel == null) {
            return new Vector3d();
        }
        double magnitudeSquared = desiredWorldImpulse.lengthSquared();
        if (!Double.isFinite(magnitudeSquared)
                || magnitudeSquared < PHYSICS_EPSILON) {
            return new Vector3d();
        }

        double magnitude = Math.sqrt(magnitudeSquared);
        Vector3d worldDirection = desiredWorldImpulse.div(
                magnitude,
                new Vector3d()
        );
        double inverseMass = inverseAngularMass(subLevel, worldDirection);
        double velocityWork = angularVelocity.dot(desiredWorldImpulse);
        if (!Double.isFinite(velocityWork)
                || velocityWork >= -PHYSICS_EPSILON
                || inverseMass <= PHYSICS_EPSILON) {
            return new Vector3d();
        }

        double stoppingScale = -velocityWork
                / (inverseMass * magnitudeSquared);
        if (!Double.isFinite(stoppingScale) || stoppingScale <= 0.0D) {
            return new Vector3d();
        }
        return desiredWorldImpulse.mul(
                Math.min(1.0D, stoppingScale),
                new Vector3d()
        );
    }

    private static Vector3d stableDampingImpulse(
            Vector3d relativeVelocity,
            double dampingCoefficient,
            double inverseEffectiveMass,
            double timeStep
    ) {
        if (!Double.isFinite(inverseEffectiveMass)
                || inverseEffectiveMass <= PHYSICS_EPSILON) {
            return new Vector3d();
        }

        double response = dampingCoefficient
                * inverseEffectiveMass
                * timeStep;
        if (!Double.isFinite(response) || response <= 0.0D) {
            return new Vector3d();
        }

        double impulseScale = -Math.expm1(-response)
                / inverseEffectiveMass;
        return relativeVelocity.mul(impulseScale, new Vector3d());
    }

    private static double inverseAngularMass(
            @Nullable ServerSubLevel subLevel,
            Vector3d worldDirection
    ) {
        if (subLevel == null) {
            return 0.0D;
        }
        Vector3d localDirection = subLevel.logicalPose()
                .transformNormalInverse(
                        worldDirection,
                        new Vector3d()
                );
        Vector3d localResponse = subLevel.getMassTracker()
                .getInverseInertiaTensor()
                .transform(localDirection, new Vector3d());
        double inverseMass = localDirection.dot(localResponse);
        return Double.isFinite(inverseMass) && inverseMass > 0.0D
                ? inverseMass
                : 0.0D;
    }

    private Vector3d alignmentAngularImpulse(
            @Nullable ServerSubLevel ownSubLevel,
            @Nullable ServerSubLevel otherSubLevel,
            Direction ownFacing,
            Direction otherFacing,
            double timeStep
    ) {
        Vector3d ownNormal = worldFacing(ownSubLevel, ownFacing);
        Vector3d otherNormal = worldFacing(otherSubLevel, otherFacing).negate();
        Vector3d angularError = ownNormal.cross(
                otherNormal,
                new Vector3d()
        );
        return angularError.mul(
                BASE_ANGULAR_STIFFNESS
                        * settings.constraint()
                        * timeStep
        );
    }

    private Vector3d lockedPoseAngularImpulse(
            ServerSubLevel ownSubLevel,
            @Nullable ServerSubLevel otherSubLevel,
            double timeStep
    ) {
        if (!hasInstallationPose) {
            return new Vector3d();
        }

        Quaterniond ownRotation = worldOrientation(ownSubLevel);
        Quaterniond otherRotation = worldOrientation(otherSubLevel);
        Quaterniond expectedOtherRotation = ownRotation.mul(
                installationRelativeRotation,
                new Quaterniond()
        );
        Quaterniond error = expectedOtherRotation.mul(
                new Quaterniond(otherRotation).invert(),
                new Quaterniond()
        ).normalize();
        Vector3d rotationError = rotationVector(error);

        // The error is the rotation wanted by the partner, so the actor gets
        // the equal and opposite angular impulse.
        return rotationError.mul(
                -BASE_ANGULAR_STIFFNESS
                        * settings.constraint()
                        * timeStep
        );
    }

    private void applyImpulsePair(
            @Nullable ServerSubLevel ownSubLevel,
            @Nullable RigidBodyHandle ownHandle,
            Vector3d ownLocal,
            @Nullable ServerSubLevel otherSubLevel,
            Vector3d otherLocal,
            Vector3d ownWorldImpulse,
            Vector3d otherWorldImpulse,
            Vector3d ownWorldAngularImpulse,
            Vector3d otherWorldAngularImpulse,
            Vector3d elasticWorldImpulse
    ) {
        boolean ownHasLinear = ownWorldImpulse.lengthSquared()
                > PHYSICS_EPSILON;
        boolean otherHasLinear = otherWorldImpulse.lengthSquared()
                > PHYSICS_EPSILON;
        boolean ownHasAngular = ownWorldAngularImpulse.lengthSquared()
                > PHYSICS_EPSILON;
        boolean otherHasAngular = otherWorldAngularImpulse.lengthSquared()
                > PHYSICS_EPSILON;
        if (!ownHasLinear
                && !otherHasLinear
                && !ownHasAngular
                && !otherHasAngular) {
            return;
        }

        Vector3d ownAngularBalance = null;
        Vector3d otherAngularBalance = null;
        if (elasticWorldImpulse.lengthSquared() > PHYSICS_EPSILON
                && ownSubLevel != null
                && otherSubLevel != null) {
            Vector3d ownWorld = toWorldPosition(ownSubLevel, ownLocal);
            Vector3d otherWorld = toWorldPosition(otherSubLevel, otherLocal);
            Vector3d missingAngularImpulse = otherWorld.sub(
                    ownWorld,
                    new Vector3d()
            ).cross(elasticWorldImpulse, new Vector3d());
            if (missingAngularImpulse.lengthSquared() > PHYSICS_EPSILON) {
                Matrix3d ownInertia = worldInertiaMatrix(
                        ownSubLevel,
                        false
                );
                Matrix3d otherInertia = worldInertiaMatrix(
                        otherSubLevel,
                        false
                );
                Vector3d commonAngularVelocity = solveAngularSystem(
                        new Matrix3d(ownInertia).add(otherInertia),
                        missingAngularImpulse
                );
                if (commonAngularVelocity != null) {
                    ownAngularBalance = ownInertia.transform(
                            commonAngularVelocity,
                            new Vector3d()
                    );
                    otherAngularBalance = otherInertia.transform(
                            commonAngularVelocity,
                            new Vector3d()
                    );
                }
            }
        }

        if (ownSubLevel != null && ownHandle != null && ownHasLinear) {
            ownForces.applyImpulseAtPoint(
                    ownSubLevel,
                    ownLocal,
                    ownSubLevel.logicalPose().transformNormalInverse(
                            ownWorldImpulse,
                            new Vector3d()
                    )
            );
        }
        if (ownSubLevel != null
                && ownHandle != null
                && (ownHasAngular || ownAngularBalance != null)) {
            Vector3d angularImpulse = ownHasAngular
                    ? new Vector3d(ownWorldAngularImpulse)
                    : new Vector3d();
            if (ownAngularBalance != null) {
                angularImpulse.add(ownAngularBalance);
            }
            ownForces.applyLinearAndAngularImpulse(
                    JOMLConversion.ZERO,
                    ownSubLevel.logicalPose().transformNormalInverse(
                            angularImpulse,
                            new Vector3d()
                    )
            );
        }
        if (ownSubLevel != null && ownHandle != null) {
            ownHandle.applyForcesAndReset(ownForces);
        }

        if (otherSubLevel == null) {
            return;
        }

        if (otherHasLinear) {
            partnerForces.applyImpulseAtPoint(
                    otherSubLevel,
                    otherLocal,
                    otherSubLevel.logicalPose().transformNormalInverse(
                            otherWorldImpulse,
                            new Vector3d()
                    )
            );
        }
        if (otherHasAngular || otherAngularBalance != null) {
            Vector3d angularImpulse = otherHasAngular
                    ? new Vector3d(otherWorldAngularImpulse)
                    : new Vector3d();
            if (otherAngularBalance != null) {
                angularImpulse.add(otherAngularBalance);
            }
            partnerForces.applyLinearAndAngularImpulse(
                    JOMLConversion.ZERO,
                    otherSubLevel.logicalPose().transformNormalInverse(
                            angularImpulse,
                            new Vector3d()
                    )
            );
        }
        RigidBodyHandle.of(otherSubLevel).applyForcesAndReset(partnerForces);
    }

    private static Matrix3d worldInertiaMatrix(
            ServerSubLevel subLevel,
            boolean inverse
    ) {
        Matrix3d result = new Matrix3d();
        for (int column = 0; column < 3; column++) {
            Vector3d worldBasis = switch (column) {
                case 0 -> new Vector3d(1.0D, 0.0D, 0.0D);
                case 1 -> new Vector3d(0.0D, 1.0D, 0.0D);
                default -> new Vector3d(0.0D, 0.0D, 1.0D);
            };
            Vector3d localBasis = subLevel.logicalPose()
                    .transformNormalInverse(worldBasis, new Vector3d());
            Vector3d localResponse = inverse
                    ? subLevel.getMassTracker().getInverseInertiaTensor()
                    .transform(localBasis, new Vector3d())
                    : subLevel.getMassTracker().getInertiaTensor()
                    .transform(localBasis, new Vector3d());
            Vector3d worldResponse = subLevel.logicalPose()
                    .transformNormal(localResponse, new Vector3d());
            result.setColumn(column, worldResponse);
        }
        return result;
    }

    @Nullable
    private static Vector3d solveAngularSystem(
            Matrix3d matrix,
            Vector3d target
    ) {
        double determinant = matrix.determinant();
        if (!matrix.isFinite()
                || !Double.isFinite(determinant)
                || Math.abs(determinant) <= MIN_INERTIA_DETERMINANT) {
            return null;
        }
        Vector3d solution = matrix.invert(new Matrix3d())
                .transform(target, new Vector3d());
        return solution.isFinite() ? solution : null;
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.write(tag, registries, clientPacket);
        writeStrutData(tag);
    }

    @Override
    public void writeSafe(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.writeSafe(tag, registries);
        writeStrutData(tag);
    }

    private void writeStrutData(CompoundTag tag) {
        tag.putBoolean(TAG_PENDING, pending);
        tag.putBoolean(TAG_COMPACT_LINK, compactLink);
        if (compactLink) {
            tag.putByte(
                    TAG_COMPACT_FACING,
                    (byte) compactPartnerFacing.get3DDataValue()
            );
            BlockPos supportPos = compactPartnerSupportPos();
            tag.putLong(TAG_COMPACT_SUPPORT_POS, supportPos.asLong());
            if (compactPartnerSubLevel != null) {
                tag.putUUID(
                        TAG_COMPACT_SUPPORT_SUB_LEVEL,
                        compactPartnerSubLevel
                );
            }
            if (hasCompactPartnerLastWorldCenter) {
                tag.putDouble(
                        TAG_COMPACT_SUPPORT_WORLD_X,
                        compactPartnerLastWorldCenter.x
                );
                tag.putDouble(
                        TAG_COMPACT_SUPPORT_WORLD_Y,
                        compactPartnerLastWorldCenter.y
                );
                tag.putDouble(
                        TAG_COMPACT_SUPPORT_WORLD_Z,
                        compactPartnerLastWorldCenter.z
                );
            }
        }
        tag.put(TAG_SETTINGS, settings.write());
        if (hasInstallationPose) {
            CompoundTag pose = new CompoundTag();
            pose.putDouble(TAG_AXIS_X, installationAxisLocal.x);
            pose.putDouble(TAG_AXIS_Y, installationAxisLocal.y);
            pose.putDouble(TAG_AXIS_Z, installationAxisLocal.z);
            pose.putDouble(TAG_ROTATION_X, installationRelativeRotation.x);
            pose.putDouble(TAG_ROTATION_Y, installationRelativeRotation.y);
            pose.putDouble(TAG_ROTATION_Z, installationRelativeRotation.z);
            pose.putDouble(TAG_ROTATION_W, installationRelativeRotation.w);
            pose.putDouble(TAG_INSTALLATION_LENGTH, installationLength);
            tag.put(TAG_INSTALLATION_POSE, pose);
        }
    }

    @Override
    protected void read(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.read(tag, registries, clientPacket);
        pending = tag.getBoolean(TAG_PENDING);
        compactLink = tag.getBoolean(TAG_COMPACT_LINK);
        compactPartnerFacing = tag.contains(
                TAG_COMPACT_FACING,
                Tag.TAG_ANY_NUMERIC
        )
                ? Direction.from3DDataValue(
                        tag.getByte(TAG_COMPACT_FACING)
                )
                : Direction.DOWN;
        compactPartnerSupportPos = tag.contains(
                TAG_COMPACT_SUPPORT_POS,
                Tag.TAG_LONG
        )
                ? BlockPos.of(tag.getLong(TAG_COMPACT_SUPPORT_POS))
                : null;
        compactPartnerSubLevel = tag.hasUUID(TAG_COMPACT_SUPPORT_SUB_LEVEL)
                ? tag.getUUID(TAG_COMPACT_SUPPORT_SUB_LEVEL)
                : null;
        hasCompactPartnerLastWorldCenter = tag.contains(
                TAG_COMPACT_SUPPORT_WORLD_X,
                Tag.TAG_ANY_NUMERIC
        ) && tag.contains(
                TAG_COMPACT_SUPPORT_WORLD_Y,
                Tag.TAG_ANY_NUMERIC
        ) && tag.contains(
                TAG_COMPACT_SUPPORT_WORLD_Z,
                Tag.TAG_ANY_NUMERIC
        );
        if (hasCompactPartnerLastWorldCenter) {
            compactPartnerLastWorldCenter.set(
                    tag.getDouble(TAG_COMPACT_SUPPORT_WORLD_X),
                    tag.getDouble(TAG_COMPACT_SUPPORT_WORLD_Y),
                    tag.getDouble(TAG_COMPACT_SUPPORT_WORLD_Z)
            );
        }
        compactMissingSupportTicks = 0;

        if (tag.contains(TAG_SETTINGS, Tag.TAG_COMPOUND)
                && settings.read(tag.getCompound(TAG_SETTINGS))) {
            desiredLength = settings.length();
        } else {
            settings.reset(desiredLength > 0.0D ? desiredLength : MIN_LENGTH);
        }

        hasInstallationPose = false;
        installationLength = settings.length();
        if (tag.contains(TAG_INSTALLATION_POSE, Tag.TAG_COMPOUND)) {
            CompoundTag pose = tag.getCompound(TAG_INSTALLATION_POSE);
            if (hasPoseNumbers(pose)) {
                installationAxisLocal.set(
                        pose.getDouble(TAG_AXIS_X),
                        pose.getDouble(TAG_AXIS_Y),
                        pose.getDouble(TAG_AXIS_Z)
                );
                installationRelativeRotation.set(
                        pose.getDouble(TAG_ROTATION_X),
                        pose.getDouble(TAG_ROTATION_Y),
                        pose.getDouble(TAG_ROTATION_Z),
                        pose.getDouble(TAG_ROTATION_W)
                );
                if (installationAxisLocal.lengthSquared() > PHYSICS_EPSILON
                        && installationRelativeRotation.lengthSquared()
                        > PHYSICS_EPSILON) {
                    installationAxisLocal.normalize();
                    installationRelativeRotation.normalize();
                    if (pose.contains(
                            TAG_INSTALLATION_LENGTH,
                            Tag.TAG_ANY_NUMERIC
                    ) && Double.isFinite(
                            pose.getDouble(TAG_INSTALLATION_LENGTH)
                    )) {
                        installationLength = Mth.clamp(
                                pose.getDouble(TAG_INSTALLATION_LENGTH),
                                MIN_LENGTH,
                                MAX_LENGTH
                        );
                    }
                    hasInstallationPose = true;
                }
            }
        }
    }

    @Override
    public SuspensionStrutBlockEntity getPairedSpring() {
        if (compactLink) {
            return null;
        }
        SpringBlockEntity paired = super.getPairedSpring();
        return paired instanceof SuspensionStrutBlockEntity strut
                ? strut
                : null;
    }

    public BlockPos getPartnerPos() {
        return compactLink ? worldPosition : partnerPos;
    }

    public boolean isCompactLink() {
        return compactLink;
    }

    public boolean claimSupportLossDrop() {
        if (pending
                || assembling
                || destroyingStrut
                || supportDropClaimed
                || !hasLink()) {
            return false;
        }
        supportDropClaimed = true;
        return true;
    }

    public void onCompactSupportBroken(
            ServerLevel serverLevel,
            BlockPos supportPos
    ) {
        if (level == serverLevel
                && compactLink
                && supportPos.equals(compactPartnerSupportPos())) {
            destroyFullStrut(serverLevel);
        }
    }

    public BlockPos getLinkedTransformPos() {
        return compactLink ? compactPartnerSupportPos() : partnerPos;
    }

    @Nullable
    public Direction getLinkedFacing() {
        if (compactLink) {
            return compactPartnerFacing;
        }

        SuspensionStrutBlockEntity other = getPairedSpring();
        if (other == null
                || !(other.getBlockState().getBlock()
                instanceof SuspensionStrutBlock)
                || !other.getBlockState().hasProperty(
                        SuspensionStrutBlock.FACING
                )) {
            return null;
        }
        return other.getBlockState().getValue(SuspensionStrutBlock.FACING);
    }

    public boolean hasLink() {
        return compactLink || partnerPos != null;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
        pendingTicks = 0;
        setChanged();
        sendData();
    }

    public double getRestLength() {
        return settings.length();
    }

    public void setRestLength(double length) {
        settings.setLength(length);
        desiredLength = settings.length();
        syncSettings();
    }

    public double getStrength() {
        return settings.strength();
    }

    public double getDamping() {
        return settings.damping();
    }

    public double getMaxImpulse() {
        return settings.maxImpulse();
    }

    public double getConstraintStrength() {
        return settings.constraint();
    }

    public SuspensionStrutSettings.Behavior getBehavior() {
        return settings.behavior();
    }

    public CompoundTag writeSettingsProfile() {
        return settings.write();
    }

    public boolean applySettingsProfile(CompoundTag tag) {
        SuspensionStrutSettings replacement =
                new SuspensionStrutSettings(settings.length());
        if (!replacement.read(tag)) {
            return false;
        }
        settings.copyFrom(replacement);
        desiredLength = settings.length();
        syncSettings();
        return true;
    }

    public double adjustStrength(int steps) {
        settings.setStrength(
                settings.strength()
                        + steps * SuspensionStrutSettings.MULTIPLIER_STEP
        );
        syncSettings();
        return settings.strength();
    }

    public double adjustLength(int steps) {
        settings.setLength(
                settings.length()
                        + steps * SuspensionStrutSettings.LENGTH_STEP
        );
        desiredLength = settings.length();
        syncSettings();
        return settings.length();
    }

    public double adjustDamping(int steps) {
        settings.setDamping(
                settings.damping()
                        + steps * SuspensionStrutSettings.MULTIPLIER_STEP
        );
        syncSettings();
        return settings.damping();
    }

    public double adjustMaxImpulse(int steps) {
        settings.setMaxImpulse(
                settings.maxImpulse()
                        + steps * SuspensionStrutSettings.MAX_IMPULSE_STEP
        );
        syncSettings();
        return settings.maxImpulse();
    }

    public double adjustConstraintStrength(int steps) {
        settings.setConstraint(
                settings.constraint()
                        + steps * SuspensionStrutSettings.MULTIPLIER_STEP
        );
        syncSettings();
        return settings.constraint();
    }

    public SuspensionStrutSettings.Behavior cycleBehavior(int direction) {
        settings.setBehavior(settings.behavior().cycle(direction));
        syncSettings();
        return settings.behavior();
    }

    public void resetSettings() {
        settings.reset(installationLength);
        desiredLength = settings.length();
        syncSettings();
    }

    private void syncSettings() {
        setChanged();
        sendData();

        SuspensionStrutBlockEntity paired = getPairedSpring();
        if (paired != null) {
            paired.settings.copyFrom(settings);
            paired.desiredLength = desiredLength;
            paired.setChanged();
            paired.sendData();
        }
    }

    public void createMutualLink(
            SuspensionStrutBlockEntity other,
            double restLength
    ) {
        if (level == null || other.level == null) {
            return;
        }

        pending = false;
        other.pending = false;
        pendingTicks = 0;
        other.pendingTicks = 0;

        settings.reset(Mth.clamp(restLength, MIN_LENGTH, MAX_LENGTH));
        other.settings.copyFrom(settings);
        desiredLength = settings.length();
        other.desiredLength = settings.length();
        setController(true);
        other.setController(false);
        setPartnerPos(other.getBlockPos(), other.getPartnerSubLevelIDForPlacement());
        other.setPartnerPos(getBlockPos(), getPartnerSubLevelIDForPlacement());
        captureInstallationPose(other);

        setChanged();
        other.setChanged();
        sendData();
        other.sendData();
    }

    public void createCompactLink(Direction partnerFacing) {
        if (level == null) {
            return;
        }

        pending = false;
        pendingTicks = 0;
        compactLink = true;
        compactPartnerFacing = partnerFacing;
        compactPartnerSupportPos = worldPosition.relative(
                compactPartnerFacing.getOpposite()
        );
        compactPartnerSubLevel = SableSubLevelHelper.getSubLevelId(
                level,
                compactPartnerSupportPos()
        );
        captureCompactPartnerWorldCenter();
        settings.reset(MIN_LENGTH);
        desiredLength = settings.length();
        setController(true);
        setPartnerPos(null, null);
        captureCompactInstallationPose();
        refreshCompactSupportShapeRegistration();
        setChanged();
        sendData();
    }

    public void afterAssemblyMove(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockPos oldPos
    ) {
        assembling = false;
        if (!hasLink()) {
            return;
        }

        if (compactLink) {
            compactPartnerFacing = getBlockState()
                    .getValue(SuspensionStrutBlock.FACING)
                    .getOpposite();
            BlockPos relativeSupport = worldPosition.relative(
                    compactPartnerFacing.getOpposite()
            );
            UUID relativeSupportSubLevel = SableSubLevelHelper.getSubLevelId(
                    resultingLevel,
                    relativeSupport
            );
            if (isCompactSupportAt(
                    resultingLevel,
                    relativeSupportSubLevel,
                    relativeSupport
            )) {
                compactPartnerSupportPos = relativeSupport.immutable();
                compactPartnerSubLevel = relativeSupportSubLevel;
                captureCompactPartnerWorldCenter();
            }
            ensureCompactPartnerSupport(resultingLevel);
            refreshCompactSupportShapeRegistration();
            setChanged();
            sendData();
            return;
        }

        UUID newSubLevelId = SableSubLevelHelper.getSubLevelId(
                resultingLevel,
                worldPosition
        );
        SuspensionStrutBlockEntity other = findPartnerDuringAssembly(
                originLevel,
                resultingLevel,
                oldPos
        );
        if (other != null) {
            other.replaceMovedEndpoint(oldPos, worldPosition, newSubLevelId);
        }
        setChanged();
        sendData();
    }

    @Nullable
    private SuspensionStrutBlockEntity findPartnerDuringAssembly(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockPos oldPos
    ) {
        if (partnerPos == null) {
            return null;
        }

        var resultEntity = resultingLevel.getBlockEntity(partnerPos);
        if (resultEntity instanceof SuspensionStrutBlockEntity resultStrut
                && resultStrut.pointsToMovedEndpoint(oldPos, worldPosition)) {
            return resultStrut;
        }

        if (originLevel != resultingLevel) {
            var originEntity = originLevel.getBlockEntity(partnerPos);
            if (originEntity instanceof SuspensionStrutBlockEntity originStrut
                    && originStrut.pointsToMovedEndpoint(oldPos, worldPosition)) {
                return originStrut;
            }
        }
        return null;
    }

    private boolean pointsToMovedEndpoint(BlockPos oldPos, BlockPos newPos) {
        return !compactLink
                && partnerPos != null
                && (partnerPos.equals(oldPos) || partnerPos.equals(newPos));
    }

    private void replaceMovedEndpoint(
            BlockPos oldPos,
            BlockPos newPos,
            @Nullable UUID newSubLevelId
    ) {
        if (compactLink || partnerPos == null || !partnerPos.equals(oldPos)) {
            return;
        }
        setPartnerPos(newPos.immutable(), newSubLevelId);
        assembling = false;
        setChanged();
        sendData();
    }

    private void captureInstallationPose(SuspensionStrutBlockEntity other) {
        Quaterniond ownRotation = currentWorldOrientation(this);
        Quaterniond otherRotation = currentWorldOrientation(other);
        Vec3 ownPoint = attachmentPoint(
                level,
                worldPosition,
                getBlockState().getValue(SuspensionStrutBlock.FACING)
        );
        Vec3 otherPoint = attachmentPoint(
                other.level,
                other.worldPosition,
                other.getBlockState().getValue(SuspensionStrutBlock.FACING)
        );
        Vector3d worldAxis = new Vector3d(
                otherPoint.x - ownPoint.x,
                otherPoint.y - ownPoint.y,
                otherPoint.z - ownPoint.z
        );
        if (worldAxis.lengthSquared() < PHYSICS_EPSILON) {
            worldAxis.set(0.0D, 1.0D, 0.0D);
        } else {
            worldAxis.normalize();
        }

        installationAxisLocal.set(worldAxis);
        new Quaterniond(ownRotation).invert().transform(installationAxisLocal);
        installationRelativeRotation.set(ownRotation)
                .invert()
                .mul(otherRotation)
                .normalize();
        installationLength = settings.length();
        hasInstallationPose = true;

        other.installationAxisLocal.set(worldAxis).negate();
        new Quaterniond(otherRotation).invert()
                .transform(other.installationAxisLocal);
        other.installationRelativeRotation.set(otherRotation)
                .invert()
                .mul(ownRotation)
                .normalize();
        other.installationLength = settings.length();
        other.hasInstallationPose = true;
    }

    private void captureCompactInstallationPose() {
        UUID ownId = SableSubLevelHelper.getSubLevelId(level, worldPosition);
        ServerSubLevel ownSubLevel = resolveServerSubLevel(ownId);
        ServerSubLevel otherSubLevel = resolveServerSubLevel(
                compactPartnerSubLevel
        );
        Quaterniond ownRotation = worldOrientation(ownSubLevel);
        Quaterniond otherRotation = worldOrientation(otherSubLevel);
        Vector3d ownPoint = toWorldPosition(
                ownSubLevel,
                localAttachment(
                        worldPosition,
                        getBlockState().getValue(SuspensionStrutBlock.FACING)
                )
        );
        Vector3d otherPoint = toWorldPosition(
                otherSubLevel,
                compactPartnerAttachment()
        );
        Vector3d worldAxis = otherPoint.sub(ownPoint, new Vector3d());
        if (worldAxis.lengthSquared() < PHYSICS_EPSILON) {
            worldAxis.set(0.0D, 1.0D, 0.0D);
        } else {
            worldAxis.normalize();
        }

        installationAxisLocal.set(worldAxis);
        new Quaterniond(ownRotation).invert().transform(installationAxisLocal);
        installationRelativeRotation.set(ownRotation)
                .invert()
                .mul(otherRotation)
                .normalize();
        installationLength = MIN_LENGTH;
        hasInstallationPose = true;
    }

    private void destroyFullStrut(ServerLevel serverLevel) {
        if (destroyingStrut || isRemoved()) {
            return;
        }
        destroyingStrut = true;
        SuspensionStrutBlockEntity other = findPairedSpringForRemoval();
        BlockPos otherPos = partnerPos;
        if (other != null) {
            otherPos = other.getBlockPos();
        }
        clearLinkWithoutDestroyingPartner();
        if (other != null && otherPos != null) {
            other.destroyingStrut = true;
            other.clearLinkWithoutDestroyingPartner();
            if (other.level instanceof ServerLevel otherLevel) {
                otherLevel.destroyBlock(otherPos, false);
            }
        }
        Block.popResource(
                serverLevel,
                getBlockPos(),
                new ItemStack(CreateMechanicalDrive.SUSPENSION_STRUT_ITEM.get())
        );
        serverLevel.destroyBlock(getBlockPos(), false);
    }

    @Override
    public void remove() {
        Level currentLevel = level;
        BlockPos otherPos = partnerPos;
        SuspensionStrutBlockEntity other = findPairedSpringForRemoval();
        if (other != null) {
            otherPos = other.getBlockPos();
        }
        boolean removePartner = currentLevel != null
                && !currentLevel.isClientSide
                && !assembling
                && !destroyingStrut
                && !compactLink
                && otherPos != null;

        clearLinkWithoutDestroyingPartner();
        if (!removePartner) {
            return;
        }

        if (other != null) {
            other.destroyingStrut = true;
            other.clearLinkWithoutDestroyingPartner();
        }
        currentLevel.destroyBlock(otherPos, false);
    }

    @Nullable
    private SuspensionStrutBlockEntity findPairedSpringForRemoval() {
        SuspensionStrutBlockEntity paired = getPairedSpring();
        if (paired != null || level == null || partnerPos == null) {
            return paired;
        }

        if (level.getBlockEntity(partnerPos)
                instanceof SuspensionStrutBlockEntity direct
                && direct != this
                && !direct.compactLink
                && direct.partnerPos != null
                && direct.partnerPos.equals(worldPosition)) {
            return direct;
        }

        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }
        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel.isRemoved()) {
                continue;
            }
            for (var actor : subLevel.getPlot().getBlockEntityActors()) {
                if (actor instanceof SuspensionStrutBlockEntity candidate
                        && candidate != this
                        && !candidate.compactLink
                        && candidate.partnerPos != null
                        && (candidate.partnerPos.equals(worldPosition)
                        || partnerPos.equals(candidate.worldPosition))) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private void clearLinkWithoutDestroyingPartner() {
        clearCompactSupportShapeRegistration();
        partnerPos = null;
        compactLink = false;
        compactPartnerSupportPos = null;
        compactPartnerSubLevel = null;
        hasCompactPartnerLastWorldCenter = false;
        compactMissingSupportTicks = 0;
        pending = false;
        setPartnerPos(null, null);
    }

    @Nullable
    private LinkGeometry resolveLinkGeometry(
            @Nullable ServerSubLevel knownOwnSubLevel
    ) {
        if (level == null
                || !(getBlockState().getBlock() instanceof SuspensionStrutBlock)
                || !getBlockState().hasProperty(SuspensionStrutBlock.FACING)) {
            return null;
        }

        Direction ownFacing = getBlockState()
                .getValue(SuspensionStrutBlock.FACING);
        ServerSubLevel ownSubLevel = knownOwnSubLevel;
        if (ownSubLevel == null) {
            ownSubLevel = resolveServerSubLevel(
                    SableSubLevelHelper.getSubLevelId(level, worldPosition)
            );
        }

        Direction otherFacing;
        Vector3d otherLocal;
        ServerSubLevel otherSubLevel;
        if (compactLink) {
            otherFacing = compactPartnerFacing;
            BlockPos supportPos = compactPartnerSupportPos();
            compactPartnerSubLevel = SableSubLevelHelper.getSubLevelId(
                    level,
                    supportPos
            );
            otherSubLevel = resolveServerSubLevel(compactPartnerSubLevel);
            otherLocal = compactPartnerAttachment();
        } else {
            SuspensionStrutBlockEntity other = getPairedSpring();
            if (other == null
                    || !(other.getBlockState().getBlock()
                    instanceof SuspensionStrutBlock)
                    || !other.getBlockState().hasProperty(
                            SuspensionStrutBlock.FACING
                    )) {
                return null;
            }
            otherFacing = other.getBlockState()
                    .getValue(SuspensionStrutBlock.FACING);
            otherSubLevel = resolveServerSubLevel(getPartnerSubLevelID());
            otherLocal = localAttachment(other.worldPosition, otherFacing);
        }

        Vector3d ownLocal = localAttachment(worldPosition, ownFacing);
        Vector3d ownPoint = toWorldPosition(ownSubLevel, ownLocal);
        Vector3d otherPoint = toWorldPosition(otherSubLevel, otherLocal);
        Vector3d ownNormal = worldFacing(ownSubLevel, ownFacing);
        Vector3d otherNormal = worldFacing(otherSubLevel, otherFacing);
        return new LinkGeometry(
                ownPoint,
                otherPoint,
                ownNormal,
                otherNormal,
                ownPoint.distance(otherPoint)
        );
    }

    private static boolean isAngleValid(LinkGeometry geometry) {
        Vector3d delta = geometry.otherPoint().sub(
                geometry.ownPoint(),
                new Vector3d()
        );
        if (delta.lengthSquared() < PHYSICS_EPSILON) {
            return false;
        }
        delta.normalize();
        return geometry.ownNormal().dot(delta) + GEOMETRY_EPSILON
                >= MAX_BEND_ANGLE_COS
                && geometry.otherNormal().dot(delta.negate(new Vector3d()))
                + GEOMETRY_EPSILON >= MAX_BEND_ANGLE_COS;
    }

    private BlockPos compactPartnerSupportPos() {
        if (compactPartnerSupportPos == null) {
            compactPartnerSupportPos = worldPosition.relative(
                    compactPartnerFacing.getOpposite()
            );
        }
        return compactPartnerSupportPos;
    }

    private CompactSupportStatus ensureCompactPartnerSupport(
            ServerLevel serverLevel
    ) {
        if (!compactLink) {
            return CompactSupportStatus.READY;
        }

        BlockPos supportPos = compactPartnerSupportPos();
        boolean supportAreaLoaded = SimLevelUtil.isAreaActuallyLoaded(
                serverLevel,
                supportPos,
                1
        );
        if (supportAreaLoaded && isCompactSupportAt(
                serverLevel,
                compactPartnerSubLevel,
                supportPos
        )) {
            captureCompactPartnerWorldCenter();
            return CompactSupportStatus.READY;
        }

        if (!hasCompactPartnerLastWorldCenter) {
            captureCompactPartnerWorldCenter();
        }
        CompactSupportBinding rebound = findCompactSupportBinding(serverLevel);
        if (rebound == null) {
            return supportAreaLoaded
                    ? CompactSupportStatus.REBINDING
                    : CompactSupportStatus.UNLOADED;
        }

        boolean changed = !rebound.pos().equals(compactPartnerSupportPos)
                || !Objects.equals(
                rebound.subLevelId(),
                compactPartnerSubLevel
        );
        compactPartnerSupportPos = rebound.pos();
        compactPartnerSubLevel = rebound.subLevelId();
        captureCompactPartnerWorldCenter();
        if (changed) {
            refreshCompactSupportShapeRegistration();
            setChanged();
            sendData();
        }
        return CompactSupportStatus.READY;
    }

    private void refreshCompactSupportShapeRegistration() {
        if (level == null || !compactLink || isRemoved()) {
            clearCompactSupportShapeRegistration();
            return;
        }

        BlockPos supportPos = compactPartnerSupportPos();
        if (registeredCompactSupportLevel == level
                && supportPos.equals(registeredCompactSupportPos)
                && compactPartnerFacing == registeredCompactSupportFacing) {
            return;
        }

        clearCompactSupportShapeRegistration();
        CompactStrutSupportShapes.register(
                level,
                supportPos,
                compactPartnerFacing,
                this
        );
        registeredCompactSupportLevel = level;
        registeredCompactSupportPos = supportPos.immutable();
        registeredCompactSupportFacing = compactPartnerFacing;
    }

    private void refreshAdjacentCompactSupportBinding() {
        if (level == null || !compactLink || isRemoved()) {
            return;
        }

        BlockPos adjacentSupport = worldPosition.relative(
                compactPartnerFacing.getOpposite()
        );
        UUID ownSubLevel = SableSubLevelHelper.getSubLevelId(
                level,
                worldPosition
        );
        UUID adjacentSubLevel = SableSubLevelHelper.getSubLevelId(
                level,
                adjacentSupport
        );
        if (!Objects.equals(ownSubLevel, adjacentSubLevel)
                || !level.getBlockState(adjacentSupport).isFaceSturdy(
                        level,
                        adjacentSupport,
                        compactPartnerFacing
                )
                || adjacentSupport.equals(compactPartnerSupportPos)
                && Objects.equals(
                        adjacentSubLevel,
                        compactPartnerSubLevel
                )) {
            return;
        }

        clearCompactSupportShapeRegistration();
        compactPartnerSupportPos = adjacentSupport.immutable();
        compactPartnerSubLevel = adjacentSubLevel;
        captureCompactPartnerWorldCenter();
        refreshCompactSupportShapeRegistration();
        if (!level.isClientSide) {
            setChanged();
            sendData();
        }
    }

    private void clearCompactSupportShapeRegistration() {
        if (registeredCompactSupportLevel == null
                || registeredCompactSupportPos == null
                || registeredCompactSupportFacing == null) {
            registeredCompactSupportLevel = null;
            registeredCompactSupportPos = null;
            registeredCompactSupportFacing = null;
            return;
        }

        CompactStrutSupportShapes.unregister(
                registeredCompactSupportLevel,
                registeredCompactSupportPos,
                registeredCompactSupportFacing,
                this
        );
        registeredCompactSupportLevel = null;
        registeredCompactSupportPos = null;
        registeredCompactSupportFacing = null;
    }

    private boolean isCompactSupportAt(
            ServerLevel serverLevel,
            @Nullable UUID expectedSubLevel,
            BlockPos supportPos
    ) {
        if (!Objects.equals(
                expectedSubLevel,
                SableSubLevelHelper.getSubLevelId(serverLevel, supportPos)
        )) {
            return false;
        }
        return serverLevel.getBlockState(supportPos).isFaceSturdy(
                serverLevel,
                supportPos,
                compactPartnerFacing
        );
    }

    @Nullable
    private CompactSupportBinding findCompactSupportBinding(
            ServerLevel serverLevel
    ) {
        if (!hasCompactPartnerLastWorldCenter) {
            return null;
        }

        BlockPos worldPos = BlockPos.containing(
                compactPartnerLastWorldCenter.x,
                compactPartnerLastWorldCenter.y,
                compactPartnerLastWorldCenter.z
        );
        if (isCompactSupportAt(serverLevel, null, worldPos)) {
            return new CompactSupportBinding(worldPos.immutable(), null);
        }

        SubLevelContainer container = SubLevelContainer.getContainer(serverLevel);
        if (container == null) {
            return null;
        }

        CompactSupportBinding closest = null;
        double closestDistanceSquared =
                COMPACT_SUPPORT_REBIND_DISTANCE_SQUARED;
        for (SubLevel candidate : container.getAllSubLevels()) {
            if (!(candidate instanceof ServerSubLevel serverSubLevel)
                    || candidate.isRemoved()) {
                continue;
            }

            Vector3d localCenter = serverSubLevel.logicalPose()
                    .transformPositionInverse(
                            compactPartnerLastWorldCenter,
                            new Vector3d()
                    );
            BlockPos localPos = BlockPos.containing(
                    localCenter.x,
                    localCenter.y,
                    localCenter.z
            );
            UUID candidateId = serverSubLevel.getUniqueId();
            if (!isCompactSupportAt(serverLevel, candidateId, localPos)) {
                continue;
            }

            Vector3d actualWorldCenter = toWorldPosition(
                    serverSubLevel,
                    new Vector3d(
                            localPos.getX() + 0.5D,
                            localPos.getY() + 0.5D,
                            localPos.getZ() + 0.5D
                    )
            );
            double distanceSquared = actualWorldCenter.distanceSquared(
                    compactPartnerLastWorldCenter
            );
            if (distanceSquared <= closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closest = new CompactSupportBinding(
                        localPos.immutable(),
                        candidateId
                );
            }
        }
        return closest;
    }

    private void captureCompactPartnerWorldCenter() {
        if (level == null || !compactLink) {
            return;
        }
        ServerSubLevel partnerSubLevel = resolveServerSubLevel(
                compactPartnerSubLevel
        );
        BlockPos supportPos = compactPartnerSupportPos();
        compactPartnerLastWorldCenter.set(toWorldPosition(
                partnerSubLevel,
                new Vector3d(
                        supportPos.getX() + 0.5D,
                        supportPos.getY() + 0.5D,
                        supportPos.getZ() + 0.5D
                )
        ));
        hasCompactPartnerLastWorldCenter = true;
    }

    private Vector3d compactPartnerAttachment() {
        BlockPos supportPos = compactPartnerSupportPos();
        return new Vector3d(
                supportPos.getX() + 0.5D
                        + compactPartnerFacing.getStepX() * 0.5D,
                supportPos.getY() + 0.5D
                        + compactPartnerFacing.getStepY() * 0.5D,
                supportPos.getZ() + 0.5D
                        + compactPartnerFacing.getStepZ() * 0.5D
        );
    }

    public static boolean isAngleValid(
            Level level,
            BlockPos first,
            Direction firstFacing,
            BlockPos second,
            Direction secondFacing
    ) {
        Vec3 firstNormal = worldFacing(level, first, firstFacing);
        Vec3 secondNormal = worldFacing(level, second, secondFacing);
        Vec3 firstPoint = attachmentPoint(level, first, firstNormal);
        Vec3 secondPoint = attachmentPoint(level, second, secondNormal);
        Vec3 delta = secondPoint.subtract(firstPoint);
        if (delta.lengthSqr() < 1.0E-8D) {
            return false;
        }

        Vec3 direction = delta.normalize();
        return isBendValid(firstNormal, direction)
                && isBendValid(secondNormal, direction.scale(-1.0D));
    }

    private static boolean isBendValid(
            Vec3 facingNormal,
            Vec3 linkDirection
    ) {
        if (facingNormal.lengthSqr() < 1.0E-8D) {
            return false;
        }

        return facingNormal.normalize()
                .dot(linkDirection.normalize())
                + GEOMETRY_EPSILON >= MAX_BEND_ANGLE_COS;
    }

    public static Vec3 attachmentPoint(
            Level level,
            BlockPos pos,
            Direction facing
    ) {
        return attachmentPoint(level, pos, worldFacing(level, pos, facing));
    }

    private static Vec3 attachmentPoint(
            Level level,
            BlockPos pos,
            Vec3 facingNormal
    ) {
        return SableSubLevelHelper.getWorldCenter(level, pos)
                .subtract(facingNormal.normalize().scale(0.5D));
    }

    private static Vec3 worldFacing(
            Level level,
            BlockPos pos,
            Direction facing
    ) {
        return SableSubLevelHelper.getWorldNormal(
                level,
                pos,
                Vec3.atLowerCornerOf(facing.getNormal())
        );
    }

    private static Vector3d localAttachment(SuspensionStrutBlockEntity strut) {
        Direction facing = strut.getBlockState()
                .getValue(SuspensionStrutBlock.FACING);
        return localAttachment(strut.worldPosition, facing);
    }

    private static Vector3d localAttachment(
            BlockPos pos,
            Direction facing
    ) {
        return new Vector3d(
                pos.getX() + 0.5D - facing.getStepX() * 0.5D,
                pos.getY() + 0.5D - facing.getStepY() * 0.5D,
                pos.getZ() + 0.5D - facing.getStepZ() * 0.5D
        );
    }

    private static Vector3d worldFacing(
            @Nullable ServerSubLevel subLevel,
            SuspensionStrutBlockEntity strut
    ) {
        Direction facing = strut.getBlockState()
                .getValue(SuspensionStrutBlock.FACING);
        return worldFacing(subLevel, facing);
    }

    private static Vector3d worldFacing(
            @Nullable ServerSubLevel subLevel,
            Direction facing
    ) {
        Vector3d normal = new Vector3d(
                facing.getStepX(),
                facing.getStepY(),
                facing.getStepZ()
        );
        if (subLevel != null) {
            subLevel.logicalPose().transformNormal(normal);
        }
        return normal.normalize();
    }

    private Vector3d pointVelocity(
            @Nullable ServerSubLevel subLevel,
            Vector3d localPoint
    ) {
        if (subLevel == null || level == null) {
            return new Vector3d();
        }
        return Sable.HELPER.getVelocity(
                level,
                subLevel,
                localPoint,
                new Vector3d()
        );
    }

    private static Vector3d toWorldPosition(
            @Nullable ServerSubLevel subLevel,
            Vector3d localPosition
    ) {
        return subLevel == null
                ? new Vector3d(localPosition)
                : subLevel.logicalPose().transformPosition(
                        localPosition,
                        new Vector3d()
                );
    }

    private static Quaterniond currentWorldOrientation(
            SuspensionStrutBlockEntity strut
    ) {
        UUID id = SableSubLevelHelper.getSubLevelId(
                strut.level,
                strut.worldPosition
        );
        if (id == null) {
            return new Quaterniond();
        }
        SubLevelContainer container = SubLevelContainer.getContainer(strut.level);
        SubLevel subLevel = container == null ? null : container.getSubLevel(id);
        return subLevel == null
                ? new Quaterniond()
                : new Quaterniond(subLevel.logicalPose().orientation());
    }

    private static Quaterniond worldOrientation(
            @Nullable ServerSubLevel subLevel
    ) {
        return subLevel == null
                ? new Quaterniond()
                : new Quaterniond(subLevel.logicalPose().orientation());
    }

    @Nullable
    private ServerSubLevel resolveServerSubLevel(@Nullable UUID id) {
        if (id == null || level == null) {
            return null;
        }
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }
        SubLevel subLevel = container.getSubLevel(id);
        return subLevel instanceof ServerSubLevel serverSubLevel
                ? serverSubLevel
                : null;
    }

    private static Vector3d clampMagnitude(Vector3d vector, double maximum) {
        double lengthSquared = vector.lengthSquared();
        if (!Double.isFinite(lengthSquared)) {
            return new Vector3d();
        }
        double maximumSquared = maximum * maximum;
        if (lengthSquared > maximumSquared && lengthSquared > PHYSICS_EPSILON) {
            vector.mul(maximum / Math.sqrt(lengthSquared));
        }
        return vector;
    }

    private static Vector3d rotationVector(Quaterniond rotation) {
        if (rotation.w < 0.0D) {
            rotation.mul(-1.0D);
        }
        double w = Math.max(-1.0D, Math.min(1.0D, rotation.w));
        double angle = 2.0D * Math.acos(w);
        double sine = Math.sqrt(Math.max(0.0D, 1.0D - w * w));
        if (sine < PHYSICS_EPSILON || angle < PHYSICS_EPSILON) {
            return new Vector3d();
        }
        return new Vector3d(rotation.x, rotation.y, rotation.z)
                .div(sine)
                .mul(angle);
    }

    private static boolean hasPoseNumbers(CompoundTag tag) {
        return tag.contains(TAG_AXIS_X, Tag.TAG_ANY_NUMERIC)
                && tag.contains(TAG_AXIS_Y, Tag.TAG_ANY_NUMERIC)
                && tag.contains(TAG_AXIS_Z, Tag.TAG_ANY_NUMERIC)
                && tag.contains(TAG_ROTATION_X, Tag.TAG_ANY_NUMERIC)
                && tag.contains(TAG_ROTATION_Y, Tag.TAG_ANY_NUMERIC)
                && tag.contains(TAG_ROTATION_Z, Tag.TAG_ANY_NUMERIC)
                && tag.contains(TAG_ROTATION_W, Tag.TAG_ANY_NUMERIC);
    }

    private UUID getPartnerSubLevelIDForPlacement() {
        return SableSubLevelHelper.getSubLevelId(level, getBlockPos());
    }

    private record LinkGeometry(
            Vector3d ownPoint,
            Vector3d otherPoint,
            Vector3d ownNormal,
            Vector3d otherNormal,
            double distance
    ) {
    }

    private record CompactSupportBinding(
            BlockPos pos,
            @Nullable UUID subLevelId
    ) {
    }

    private enum CompactSupportStatus {
        READY,
        REBINDING,
        UNLOADED
    }
}
