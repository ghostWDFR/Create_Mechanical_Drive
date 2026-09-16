package dev.createmechanicaldrive.content.tracks.mounts.torsion;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.createmechanicaldrive.content.suspension.SuspensionSpringTuning;
import dev.createmechanicaldrive.content.suspension.SpringTuningWrenchTarget;
import dev.createmechanicaldrive.content.tracks.chain.TrackAssemblyManager;
import dev.createmechanicaldrive.content.tracks.chain.TrackLinkedWheel;
import dev.createmechanicaldrive.content.tracks.chain.TrackPathResolver;
import dev.createmechanicaldrive.content.tracks.mounts.long_torsion.LongTorsionMountBlock;
import dev.createmechanicaldrive.content.tracks.wheels.support.SupportWheelItem;
import dev.createmechanicaldrive.content.tracks.mounts.sprocket.SprocketMountBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Collection;
import java.util.List;

public class TorsionMountBlockEntity extends SmartBlockEntity
        implements BlockEntitySubLevelActor, Clearable,
        SuspensionSpringTuning, SpringTuningWrenchTarget, TrackLinkedWheel {
    private static final String ATTACHMENT_TAG = "Attachment";
    private static final String SUPPORT_WHEEL_TAG = "SupportWheel";
    private static final String TRACK_SPROCKET_TAG = "TrackSprocket";
    private static final String BIG_WHEEL_ORDER_TAG = "BigWheelOrderFlipped";
    public static final int ATTACHMENT_SLOT = 0;
    public static final int SUPPORT_WHEEL_SLOT = 1;
    public static final double SUPPORT_AXLE_HEIGHT = 4.0D / 16.0D;

    public static final double ARM_LENGTH = 8.0D / 16.0D;
    public static final double MIN_ARM_ANGLE = Math.toRadians(-20.0D);
    public static final double MAX_ARM_ANGLE = Math.toRadians(50.0D);
    private static final double DEFAULT_REST_ARM_ANGLE =
            Math.toRadians(DEFAULT_REST_ANGLE_DEGREES);

    private static final double TRACE_ORIGIN_TO_PIVOT = 0.5D;
    private static final double MAX_GROUND_SCAN = 5.0D;
    private static final double SPRING_PRELOAD = 0.10833333333333334D;
    private static final double SUSPENSION_STRENGTH = 10.0D;
    private static final double DEFAULT_SPRING_MULTIPLIER = 0.65D;
    private static final double DEFAULT_DAMPING_MULTIPLIER = 0.75D;
    private static final double MIN_BUMP_ACCELERATION = 18.0D;
    private static final double MAX_BUMP_ACCELERATION = 55.0D;

    private static final Collection<TorsionMountBlockEntity>
            PENDING_FORCE_APPLICATIONS = new ObjectOpenHashSet<>();
    private static boolean physicsCallbackRegistered;

    private final TorsionMountInventory inventory;
    private final Vector3d pendingForcePosition = new Vector3d();
    private final Vector3d pendingImpulse = new Vector3d();
    private final ForceTotal accumulatedForces = new ForceTotal();

    private double physicsExtension;
    private double extension;
    private double previousExtension;
    private double wheelAngle;
    private double previousWheelAngle;
    private double angularVelocity;
    private double contactFriction = 1.0D;
    private boolean wheelOffGround = true;
    private double suspensionSpringMultiplier =
            DEFAULT_SPRING_MULTIPLIER;
    private double suspensionDampingMultiplier =
            DEFAULT_DAMPING_MULTIPLIER;
    private double suspensionBumpClearanceMultiplier = DEFAULT_MULTIPLIER;
    private double suspensionBumpForceMultiplier = DEFAULT_MULTIPLIER;
    private double suspensionMaxImpulseMultiplier = DEFAULT_MULTIPLIER;
    private double suspensionDriveMultiplier = DEFAULT_MULTIPLIER;
    private double suspensionGripMultiplier = DEFAULT_MULTIPLIER;
    private double suspensionRestAngleDegrees =
            DEFAULT_REST_ANGLE_DEGREES;
    private BlockPos trackSprocket;
    private boolean bigWheelOrderFlipped;

    public TorsionMountBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
        inventory = new TorsionMountInventory(this);
        double initialExtension = defaultRestExtension();
        physicsExtension = initialExtension;
        extension = initialExtension;
        previousExtension = initialExtension;
    }

    public static synchronized void registerPhysicsCallback() {
        if (physicsCallbackRegistered) {
            return;
        }

        SableEventPlatform.INSTANCE.onPhysicsTick(
                TorsionMountBlockEntity::flushPendingForces
        );
        physicsCallbackRegistered = true;
    }

    private static void flushPendingForces(
            SubLevelPhysicsSystem physicsSystem,
            double timeStep
    ) {
        for (TorsionMountBlockEntity mount : PENDING_FORCE_APPLICATIONS) {
            if (!mount.isRemoved()) {
                mount.applyAccumulatedForces();
            }
        }
        PENDING_FORCE_APPLICATIONS.clear();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void sable$physicsTick(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            double timeStep
    ) {
        ItemStack attachment = getAttachment();
        if (!TorsionMountAttachments.hasSuspensionWheel(attachment)
                || timeStep <= 0.0D) {
            physicsExtension = restExtension();
            return;
        }

        float wheelRadius = TorsionMountAttachments.wheelRadius(attachment);
        float contactRadius = trackContactRadius(wheelRadius);
        Direction facing = getBlockState().getValue(
                TorsionMountBlock.HORIZONTAL_FACING
        );
        Pose3d vehiclePose = subLevel.logicalPose();
        Vector3dc rollingAxis = rollingAxis(facing);
        TerrainContact contact = scanTerrain(
                rollingAxis,
                vehiclePose,
                contactRadius
        );
        double groundDistance = contact.distance();
        physicsExtension = Mth.clamp(
                groundDistance - contactRadius,
                minExtension(),
                restExtension()
        );

        if (groundDistance > restExtension() + contactRadius
                + 0.25D * suspensionBumpClearanceMultiplier) {
            physicsExtension = restExtension();
            return;
        }

        Vec3 forcePoint = getBlockPos().getCenter();
        pendingForcePosition.set(
                forcePoint.x,
                forcePoint.y,
                forcePoint.z
        );

        double inverseNormalMass = subLevel.getMassTracker()
                .getInverseNormalMass(
                        pendingForcePosition,
                        OrientedBoundingBox3d.UP
                );
        if (!Double.isFinite(inverseNormalMass)
                || inverseNormalMass <= 1.0E-8D) {
            return;
        }

        double effectiveMass = 1.0D / inverseNormalMass;
        double massFactor = Math.min(
                effectiveMass / SUSPENSION_STRENGTH,
                1.0D
        ) * 10.0D;
        double tractionScale = SUSPENSION_STRENGTH
                * massFactor
                * 2.0D;
        double springScale = SUSPENSION_STRENGTH
                * massFactor
                * 40.0D
                * suspensionSpringMultiplier;
        double dampingScale = SUSPENSION_STRENGTH
                * massFactor
                * suspensionDampingMultiplier;

        Vector3d worldVelocity = Sable.HELPER.getVelocity(
                level,
                JOMLConversion.toJOML(forcePoint)
        );
        Vector3d localVelocity = vehiclePose.transformNormalInverse(
                worldVelocity
        );

        // Preload must keep producing force as the arm approaches its rest
        // angle. Adding it here created an equally-sized dead zone instead:
        // no spring force was generated near full extension, so stronger
        // settings could never reach their configured rest angles.
        double measuredLength = groundDistance
                - SPRING_PRELOAD
                - (restExtension() - defaultRestExtension());
        double suspensionTravel = suspensionTravel();
        double compressedLength = Mth.clamp(
                measuredLength
                        - contactRadius
                        - minExtension(),
                0.0D,
                suspensionTravel
        );
        double damping = -localVelocity.y * dampingScale;
        double springImpulse = (
                (suspensionTravel - compressedLength)
                        * springScale
                        * suspensionBumpForceMultiplier
                        + damping
        ) * timeStep * suspensionMaxImpulseMultiplier;

        double compressionRatio = Mth.clamp(
                (suspensionTravel - compressedLength)
                        / suspensionTravel,
                0.0D,
                1.0D
        );
        double maxBumpAcceleration = Mth.lerp(
                compressionRatio,
                MIN_BUMP_ACCELERATION,
                MAX_BUMP_ACCELERATION
        );
        springImpulse = Math.min(
                springImpulse,
                effectiveMass * maxBumpAcceleration * timeStep
        );

        Direction hitDirection = contact.normal();
        Vec3 suspensionImpulse = new Vec3(
                springImpulse * hitDirection.getStepX(),
                springImpulse * hitDirection.getStepY(),
                springImpulse * hitDirection.getStepZ()
        );
        if (contact.hitSubLevel() != null) {
            suspensionImpulse = contact.hitSubLevel()
                    .logicalPose()
                    .transformNormal(suspensionImpulse);
        }
        suspensionImpulse = vehiclePose.transformNormalInverse(
                suspensionImpulse
        );
        pendingImpulse.set(
                suspensionImpulse.x,
                suspensionImpulse.y,
                suspensionImpulse.z
        );

        contactFriction = contact.hitBlock() == null
                ? 1.0D
                : adjustedFriction(PhysicsBlockPropertyHelper.getFriction(
                        level.getBlockState(contact.hitBlock())
                ));

        double brake = level.getSignal(
                getBlockPos().above(),
                Direction.UP
        ) / 15.0D;
        double usableSurfaceFriction = Math.min(contactFriction, 1.0D);
        double rollingResistance = (
                0.075D + brake * 0.3D
        ) * usableSurfaceFriction;
        Vector3dc lateralAxis = lateralAxis(facing);
        SprocketMountBlockEntity trackOwner =
                TrackAssemblyManager.owner(level, this);
        double driveImpulse = 0.0D;
        if (trackOwner != null) {
            float kineticSpeed = facing.getAxisDirection().getStep()
                    * trackOwner.getSpeed();
            driveImpulse = kineticSpeed
                    * (1.0D - brake)
                    * usableSurfaceFriction
                    * 0.875D
                    * suspensionDriveMultiplier
                    * timeStep;
        }

        pendingImpulse.fma(
                localVelocity.dot(rollingAxis)
                        * -rollingResistance
                        * tractionScale
                        * timeStep
                        + driveImpulse,
                rollingAxis
        );
        pendingImpulse.fma(
                localVelocity.dot(lateralAxis)
                        * -0.6D
                        * contactFriction
                        * tractionScale
                        * suspensionGripMultiplier
                        * timeStep,
                lateralAxis
        );

        accumulatedForces.applyImpulseAtPoint(
                subLevel,
                pendingForcePosition,
                pendingImpulse
        );
        PENDING_FORCE_APPLICATIONS.add(this);
    }

    private void applyAccumulatedForces() {
        SubLevel containing = Sable.HELPER.getContaining(this);
        if (containing instanceof ServerSubLevel serverSubLevel) {
            RigidBodyHandle.of(serverSubLevel)
                    .applyForcesAndReset(accumulatedForces);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || !level.isClientSide) {
            return;
        }

        previousExtension = extension;
        previousWheelAngle = wheelAngle;

        ItemStack attachment = getAttachment();
        if (!TorsionMountAttachments.hasSuspensionWheel(attachment)) {
            extension = Mth.lerp(
                    0.6D,
                    extension,
                    defaultRestExtension()
            );
            wheelAngle = 0.0D;
            angularVelocity = 0.0D;
            wheelOffGround = true;
            return;
        }

        float wheelRadius = TorsionMountAttachments.wheelRadius(attachment);
        extension = Mth.lerp(
                0.7D,
                extension,
                clientGroundExtension(trackContactRadius(wheelRadius))
        );

        double attemptedAngularVelocity = Mth.lerp(
                0.2D,
                angularVelocity,
                0.0D
        );
        SubLevel containing = Sable.HELPER.getContaining(this);
        if (containing == null || wheelOffGround) {
            angularVelocity = attemptedAngularVelocity;
            wheelAngle += angularVelocity;
            return;
        }

        Direction facing = getBlockState().getValue(
                TorsionMountBlock.HORIZONTAL_FACING
        );
        Vec3 velocityPoint = getBlockPos().getCenter();
        Vector3d velocity = Sable.HELPER.getVelocity(
                level,
                JOMLConversion.toJOML(velocityPoint)
        );
        Vector3d localVelocity = containing.logicalPose()
                .transformNormalInverse(velocity)
                .div(20.0D);
        Vector3dc rollingAxis = rollingAxis(facing);
        double travelled = localVelocity.dot(rollingAxis);
        double rollingAngularDelta = travelled / wheelRadius;
        if (contactFriction < 1.0D) {
            rollingAngularDelta = Mth.lerp(
                    contactFriction,
                    attemptedAngularVelocity,
                    rollingAngularDelta
            );
        }

        wheelAngle += rollingAngularDelta;
        angularVelocity = rollingAngularDelta;
    }

    private double clientGroundExtension(float wheelRadius) {
        SubLevel containing = Sable.HELPER.getContaining(this);
        if (containing == null) {
            wheelOffGround = true;
            return restExtension();
        }

        Direction facing = getBlockState().getValue(
                TorsionMountBlock.HORIZONTAL_FACING
        );
        TerrainContact contact = scanTerrain(
                rollingAxis(facing),
                containing.logicalPose(),
                wheelRadius
        );
        double rawExtension = contact.distance() - wheelRadius;
        double restingExtension = restExtension();
        wheelOffGround = rawExtension > restingExtension;
        contactFriction = contact.hitBlock() == null
                ? 1.0D
                : adjustedFriction(PhysicsBlockPropertyHelper.getFriction(
                        level.getBlockState(contact.hitBlock())
                ));
        if (wheelOffGround) {
            return restExtension();
        }
        return Mth.clamp(
                rawExtension,
                minExtension(),
                restingExtension
        );
    }

    private float trackContactRadius(float wheelRadius) {
        if (level == null) {
            return wheelRadius;
        }
        SprocketMountBlockEntity owner = TrackAssemblyManager.owner(
                level,
                this
        );
        if (owner == null || owner.getTrackAssembly() == null) {
            return wheelRadius;
        }
        return wheelRadius + (float) owner.getTrackAssembly()
                .type()
                .groundContactOffset();
    }

    private TerrainContact scanTerrain(
            Vector3dc rollingAxis,
            Pose3dc vehiclePose,
            float wheelRadius
    ) {
        Vec3 traceOrigin = getBlockPos().getCenter();
        ClipContext context = new ClipContext(
                traceOrigin,
                traceOrigin.subtract(0.0D, MAX_GROUND_SCAN, 0.0D),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        );
        ((ClipContextExtension) context).sable$setIgnoredSubLevel(
                Sable.HELPER.getContaining(this)
        );
        BlockHitResult hit = level.clip(context);
        if (hit.getType() == BlockHitResult.Type.MISS) {
            return noTerrainContact();
        }

        SubLevel hitSubLevel = Sable.HELPER.getContaining(
                level,
                hit.getLocation()
        );
        Vec3 worldHit = hitSubLevel == null
                ? hit.getLocation()
                : hitSubLevel.logicalPose().transformPosition(
                        hit.getLocation()
                );
        Vec3 localHit = vehiclePose.transformPositionInverse(worldHit);
        double rayDistance = traceOrigin.y - localHit.y;
        if (localHit.y > traceOrigin.y
                || traceOrigin.distanceTo(localHit) < 0.05D
                || rayDistance <= 1.0E-5D) {
            return noTerrainContact();
        }

        Direction hitDirection = hit.getDirection();
        Vector3d normal = new Vector3d(
                hitDirection.getStepX(),
                hitDirection.getStepY(),
                hitDirection.getStepZ()
        );
        if (hitSubLevel != null) {
            hitSubLevel.logicalPose().transformNormal(normal);
        }
        vehiclePose.transformNormalInverse(normal);
        if (normal.lengthSquared() < 1.0E-8D) {
            return noTerrainContact();
        }
        normal.normalize();

        double suspensionProjection = normal.y;
        if (suspensionProjection < 0.5D) {
            return noTerrainContact();
        }

        double rollingProjection = normal.dot(rollingAxis);
        double wheelPlaneProjection = Math.sqrt(
                suspensionProjection * suspensionProjection
                        + rollingProjection * rollingProjection
        );
        double distance = rayDistance + wheelRadius * (
                1.0D - wheelPlaneProjection / suspensionProjection
        );

        return new TerrainContact(
                distance,
                hitDirection,
                hitSubLevel,
                hit.getBlockPos()
        );
    }

    private static TerrainContact noTerrainContact() {
        return new TerrainContact(
                MAX_GROUND_SCAN,
                Direction.UP,
                null,
                null
        );
    }

    public Vec3 pivotPosition(Direction facing) {
        Direction right = facing.getClockWise();
        double side = isReversed() ? -1.0D : 1.0D;
        return getBlockPos().getCenter().add(
                -0.5D * side * right.getStepX(),
                -0.5D,
                -0.5D * side * right.getStepZ()
        );
    }

    public Vec3 wheelCenter(Direction facing, double angle) {
        Direction right = facing.getClockWise();
        double side = isReversed() ? -1.0D : 1.0D;
        double armLength = armLength();
        double horizontal = side * armLength * Math.cos(angle);
        double vertical = -armLength * Math.sin(angle);
        return pivotPosition(facing).add(
                right.getStepX() * horizontal,
                vertical,
                right.getStepZ() * horizontal
        );
    }

    public Vec3 getTrackWheelCenter(float partialTick) {
        Direction facing = getBlockState().getValue(
                TorsionMountBlock.HORIZONTAL_FACING
        );
        double angle = level != null && level.isClientSide
                ? getLerpedArmAngle(partialTick)
                : armAngleForExtension(physicsExtension);
        return wheelCenter(facing, angle);
    }

    private double armAngleForExtension(double currentExtension) {
        double armLength = armLength();
        double suspensionDrop = Mth.clamp(
                currentExtension - TRACE_ORIGIN_TO_PIVOT,
                armLength * Math.sin(MIN_ARM_ANGLE),
                armLength * Math.sin(MAX_ARM_ANGLE)
        );
        return Math.asin(Mth.clamp(
                suspensionDrop / armLength,
                -1.0D,
                1.0D
        ));
    }

    private double restExtension() {
        return TRACE_ORIGIN_TO_PIVOT + armLength() * Math.sin(
                Math.toRadians(suspensionRestAngleDegrees)
        );
    }

    private double defaultRestExtension() {
        return TRACE_ORIGIN_TO_PIVOT
                + armLength() * Math.sin(DEFAULT_REST_ARM_ANGLE);
    }

    private double minExtension() {
        return TRACE_ORIGIN_TO_PIVOT
                + armLength() * Math.sin(MIN_ARM_ANGLE);
    }

    private double suspensionTravel() {
        return defaultRestExtension() - minExtension();
    }

    public double armLength() {
        if (getBlockState().getBlock() instanceof TorsionMountBlock block) {
            return block.armLength();
        }
        return ARM_LENGTH;
    }

    private Vector3dc rollingAxis(Direction facing) {
        Direction right = facing.getClockWise();
        return new Vector3d(
                right.getStepX(),
                0.0D,
                right.getStepZ()
        );
    }

    private Vector3dc lateralAxis(Direction facing) {
        return new Vector3d(
                facing.getStepX(),
                0.0D,
                facing.getStepZ()
        );
    }

    private boolean isReversed() {
        BlockState state = getBlockState();
        return state.hasProperty(TorsionMountBlock.REVERSED)
                && state.getValue(TorsionMountBlock.REVERSED);
    }

    private static double adjustedFriction(double friction) {
        return friction < 1.0D
                ? 0.1D + 0.9D * friction
                : friction;
    }

    @Override
    public double mechanicalDrive$adjustSuspensionTuning(
            String tuning,
            int steps
    ) {
        double multiplier = SuspensionSpringTuning.steppedMultiplier(
                tuning,
                mechanicalDrive$getSuspensionTuning(tuning),
                steps
        );
        mechanicalDrive$setSuspensionTuning(tuning, multiplier);
        mechanicalDrive$syncSuspensionTuning();
        return multiplier;
    }

    @Override
    public double mechanicalDrive$getSuspensionTuning(String tuning) {
        return switch (tuning) {
            case DAMPING -> suspensionDampingMultiplier;
            case BUMP_CLEARANCE -> suspensionBumpClearanceMultiplier;
            case BUMP_FORCE -> suspensionBumpForceMultiplier;
            case MAX_IMPULSE -> suspensionMaxImpulseMultiplier;
            case DRIVE -> suspensionDriveMultiplier;
            case GRIP -> suspensionGripMultiplier;
            default -> suspensionSpringMultiplier;
        };
    }

    @Override
    public boolean mechanicalDrive$supportsSuspensionTuning(String tuning) {
        return DAMPING.equals(tuning)
                || BUMP_CLEARANCE.equals(tuning)
                || BUMP_FORCE.equals(tuning)
                || MAX_IMPULSE.equals(tuning)
                || DRIVE.equals(tuning)
                || GRIP.equals(tuning);
    }

    @Override
    public void mechanicalDrive$resetSuspensionTuning() {
        suspensionDampingMultiplier = DEFAULT_DAMPING_MULTIPLIER;
        suspensionBumpClearanceMultiplier = DEFAULT_MULTIPLIER;
        suspensionBumpForceMultiplier = DEFAULT_MULTIPLIER;
        suspensionMaxImpulseMultiplier = DEFAULT_MULTIPLIER;
        suspensionDriveMultiplier = DEFAULT_MULTIPLIER;
        suspensionGripMultiplier = DEFAULT_MULTIPLIER;
        mechanicalDrive$syncSuspensionTuning();
    }

    @Override
    public double mechanicalDrive$adjustSpringWrenchStrength(int steps) {
        double multiplier = SuspensionSpringTuning.steppedMultiplier(
                suspensionSpringMultiplier,
                steps
        );
        suspensionSpringMultiplier = multiplier;
        mechanicalDrive$syncSuspensionTuning();
        return multiplier;
    }

    @Override
    public double mechanicalDrive$getSpringWrenchStrength() {
        return suspensionSpringMultiplier;
    }

    @Override
    public void mechanicalDrive$setSpringWrenchStrength(double multiplier) {
        suspensionSpringMultiplier = SuspensionSpringTuning.clampMultiplier(
                multiplier
        );
        mechanicalDrive$syncSuspensionTuning();
    }

    @Override
    public double mechanicalDrive$adjustSpringWrenchRestAngle(int steps) {
        suspensionRestAngleDegrees =
                SpringTuningWrenchTarget.steppedRestAngle(
                        suspensionRestAngleDegrees,
                        steps
                );
        mechanicalDrive$syncSuspensionTuning();
        return suspensionRestAngleDegrees;
    }

    @Override
    public double mechanicalDrive$getSpringWrenchRestAngle() {
        return suspensionRestAngleDegrees;
    }

    @Override
    public void mechanicalDrive$setSpringWrenchRestAngle(double degrees) {
        suspensionRestAngleDegrees =
                SpringTuningWrenchTarget.clampRestAngle(degrees);
        mechanicalDrive$syncSuspensionTuning();
    }

    @Override
    public void mechanicalDrive$resetSpringWrenchTuning() {
        suspensionSpringMultiplier = DEFAULT_SPRING_MULTIPLIER;
        suspensionRestAngleDegrees = DEFAULT_REST_ANGLE_DEGREES;
        mechanicalDrive$syncSuspensionTuning();
    }

    private void mechanicalDrive$setSuspensionTuning(
            String tuning,
            double multiplier
    ) {
        switch (tuning) {
            case DAMPING -> suspensionDampingMultiplier = multiplier;
            case BUMP_CLEARANCE ->
                    suspensionBumpClearanceMultiplier = multiplier;
            case BUMP_FORCE -> suspensionBumpForceMultiplier = multiplier;
            case MAX_IMPULSE -> suspensionMaxImpulseMultiplier = multiplier;
            case DRIVE -> suspensionDriveMultiplier = multiplier;
            case GRIP -> suspensionGripMultiplier = multiplier;
            default -> suspensionSpringMultiplier = multiplier;
        }
    }

    private void mechanicalDrive$syncSuspensionTuning() {
        setChanged();
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    public TorsionMountInventory getInventory() {
        return inventory;
    }

    public ItemStack getAttachment() {
        return inventory.getStackInSlot(ATTACHMENT_SLOT);
    }

    public ItemStack getSupportWheel() {
        return inventory.getStackInSlot(SUPPORT_WHEEL_SLOT);
    }

    public boolean isAttachmentSupported(ItemStack stack) {
        if (getBlockState().getBlock() instanceof TorsionMountBlock block) {
            return block.supportsAttachment(stack);
        }
        return TorsionMountAttachments.supports(stack);
    }

    public boolean isSupportWheelSupported(ItemStack stack) {
        if (getBlockState().getBlock() instanceof TorsionMountBlock block) {
            return block.supportsSupportWheel(stack);
        }
        return TorsionMountAttachments.supportsSupportWheel(stack);
    }

    private boolean allowsSupportWheel() {
        return getBlockState().getBlock() instanceof TorsionMountBlock block
                && block.allowsSupportWheel();
    }

    public Vec3 getSupportWheelCenter() {
        // Model-space 8,8,12 becomes +4 px vertically after the mount's
        // standard -90 degree X rotation.
        return getBlockPos().getCenter().add(
                0.0D,
                SUPPORT_AXLE_HEIGHT,
                0.0D
        );
    }

    public boolean canInstallSupportWheel() {
        if (!allowsSupportWheel()
                || level == null
                || !getSupportWheel().isEmpty()) {
            return false;
        }
        SprocketMountBlockEntity owner =
                TrackAssemblyManager.owner(level, this);
        if (owner == null) {
            return true;
        }
        TrackPathResolver.Resolved resolved = TrackPathResolver.resolve(
                owner,
                1.0F,
                owner.getLerpedTrackSag(1.0F),
                true
        );
        if (resolved == null) {
            return false;
        }

        Vec3 relative = getSupportWheelCenter().subtract(
                resolved.anchor()
        );
        double u = relative.x * resolved.rolling().getStepX()
                + relative.z * resolved.rolling().getStepZ();
        double requiredY = relative.y
                + SupportWheelItem.RADIUS
                + TrackPathResolver.TRACK_CLEARANCE;
        double currentUpperY = resolved.path().upperYAt(u);
        return Double.isFinite(currentUpperY)
                && currentUpperY + 1.0E-4D >= requiredY;
    }

    public void onAttachmentChanged(int slot) {
        if (slot == ATTACHMENT_SLOT
                && trackSprocket != null
                && !TorsionMountAttachments.hasSuspensionWheel(
                getAttachment()
        )) {
            TrackAssemblyManager.disassembleForWheel(this);
        }
        setChanged();
        invalidateRenderBoundingBox();
        sendData();
        if (slot == ATTACHMENT_SLOT
                && level != null
                && !level.isClientSide
                && getBlockState().getBlock()
                instanceof LongTorsionMountBlock) {
            LongTorsionMountBlock.synchronizeBigWheelGroup(this);
        }
    }

    public boolean isBigWheelOrderFlipped() {
        return bigWheelOrderFlipped;
    }

    public void setBigWheelOrderFlipped(boolean flipped) {
        if (bigWheelOrderFlipped == flipped) {
            return;
        }
        bigWheelOrderFlipped = flipped;
        setChanged();
        invalidateRenderBoundingBox();
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    public float getLerpedArmAngle(float partialTick) {
        return (float) armAngleForExtension(Mth.lerp(
                partialTick,
                previousExtension,
                extension
        ));
    }

    public float getLerpedWheelAngle(float partialTick) {
        return (float) Mth.lerp(
                partialTick,
                previousWheelAngle,
                wheelAngle
        );
    }

    public boolean hasClientGroundContact() {
        return level != null && level.isClientSide && !wheelOffGround;
    }

    public double getClientRollingAngularVelocity() {
        return angularVelocity;
    }

    @Override
    public BlockPos mechanicalDrive$getTrackSprocket() {
        return trackSprocket;
    }

    @Override
    public void mechanicalDrive$setTrackSprocket(BlockPos sprocketPos) {
        if (java.util.Objects.equals(trackSprocket, sprocketPos)) {
            return;
        }
        trackSprocket = sprocketPos == null ? null : sprocketPos.immutable();
        setChanged();
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.write(tag, registries, clientPacket);
        tag.put(ATTACHMENT_TAG, getAttachment().saveOptional(registries));
        tag.put(
                SUPPORT_WHEEL_TAG,
                getSupportWheel().saveOptional(registries)
        );
        tag.putDouble(SPRING_NBT_KEY, suspensionSpringMultiplier);
        tag.putDouble(DAMPING_NBT_KEY, suspensionDampingMultiplier);
        tag.putDouble(
                BUMP_CLEARANCE_NBT_KEY,
                suspensionBumpClearanceMultiplier
        );
        tag.putDouble(BUMP_FORCE_NBT_KEY, suspensionBumpForceMultiplier);
        tag.putDouble(
                MAX_IMPULSE_NBT_KEY,
                suspensionMaxImpulseMultiplier
        );
        tag.putDouble(DRIVE_NBT_KEY, suspensionDriveMultiplier);
        tag.putDouble(GRIP_NBT_KEY, suspensionGripMultiplier);
        tag.putDouble(REST_ANGLE_NBT_KEY, suspensionRestAngleDegrees);
        tag.putBoolean(BIG_WHEEL_ORDER_TAG, bigWheelOrderFlipped);
        if (trackSprocket != null) {
            tag.putLong(TRACK_SPROCKET_TAG, trackSprocket.asLong());
        }
    }

    @Override
    protected void read(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.read(tag, registries, clientPacket);
        inventory.setWithoutNotification(ItemStack.parseOptional(
                registries,
                tag.getCompound(ATTACHMENT_TAG)
        ));
        inventory.setWithoutNotification(
                SUPPORT_WHEEL_SLOT,
                ItemStack.parseOptional(
                        registries,
                        tag.getCompound(SUPPORT_WHEEL_TAG)
                )
        );
        suspensionSpringMultiplier = readMultiplier(
                tag,
                SPRING_NBT_KEY,
                DEFAULT_SPRING_MULTIPLIER
        );
        suspensionDampingMultiplier = readMultiplier(
                tag,
                DAMPING_NBT_KEY,
                DEFAULT_DAMPING_MULTIPLIER
        );
        suspensionBumpClearanceMultiplier = readMultiplier(
                tag,
                BUMP_CLEARANCE_NBT_KEY,
                DEFAULT_MULTIPLIER
        );
        suspensionBumpForceMultiplier = readMultiplier(
                tag,
                BUMP_FORCE_NBT_KEY,
                DEFAULT_MULTIPLIER
        );
        suspensionMaxImpulseMultiplier = readMultiplier(
                tag,
                MAX_IMPULSE_NBT_KEY,
                DEFAULT_MULTIPLIER
        );
        suspensionDriveMultiplier = readMultiplier(
                tag,
                DRIVE_NBT_KEY,
                DEFAULT_MULTIPLIER
        );
        suspensionGripMultiplier = readMultiplier(
                tag,
                GRIP_NBT_KEY,
                DEFAULT_MULTIPLIER
        );
        suspensionRestAngleDegrees = tag.contains(REST_ANGLE_NBT_KEY)
                ? SpringTuningWrenchTarget.clampRestAngle(
                        tag.getDouble(REST_ANGLE_NBT_KEY)
                )
                : DEFAULT_REST_ANGLE_DEGREES;
        bigWheelOrderFlipped = tag.getBoolean(BIG_WHEEL_ORDER_TAG);
        trackSprocket = tag.contains(TRACK_SPROCKET_TAG)
                ? BlockPos.of(tag.getLong(TRACK_SPROCKET_TAG))
                : null;
        if (clientPacket) {
            invalidateRenderBoundingBox();
        }
    }

    private static double readMultiplier(
            CompoundTag tag,
            String key,
            double defaultValue
    ) {
        return tag.contains(key)
                ? SuspensionSpringTuning.clampMultiplier(tag.getDouble(key))
                : defaultValue;
    }

    @Override
    public void clearContent() {
        inventory.setStackInSlot(ATTACHMENT_SLOT, ItemStack.EMPTY);
        inventory.setStackInSlot(SUPPORT_WHEEL_SLOT, ItemStack.EMPTY);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(2.0D);
    }

    private record TerrainContact(
            double distance,
            Direction normal,
            SubLevel hitSubLevel,
            BlockPos hitBlock
    ) {
    }
}
