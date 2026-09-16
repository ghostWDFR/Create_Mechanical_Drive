package dev.createmechanicaldrive.content.tracks.chain;

import dev.createmechanicaldrive.content.suspension.RigidContactSolver;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Collection;

/** Passive rigid contacts for the sprocket and idler of an assembled track. */
public final class TrackEndpointContactPhysics {
    private static final double MAX_GROUND_SCAN = 5.0D;
    private static final double CONTACT_TOLERANCE = 0.05D;
    private static final double CONTACT_RELEASE_TOLERANCE = 0.10D;
    private static final double POSITION_CORRECTION = 0.85D;
    private static final double MAX_CORRECTION_SPEED = 2.5D;
    private static final double SUPPORT_SLOP = 0.01D;
    private static final double LATERAL_RESPONSE = 0.75D;
    private static final double ROLLING_RESISTANCE = 0.025D;
    private static final double MAX_FRICTION_MULTIPLIER = 1.25D;

    private static final Collection<State> PENDING_FORCE_APPLICATIONS =
            new ObjectOpenHashSet<>();
    private static boolean physicsCallbackRegistered;

    private TrackEndpointContactPhysics() {
    }

    public static synchronized void registerPhysicsCallback() {
        if (physicsCallbackRegistered) {
            return;
        }
        RigidContactSolver.registerPhysicsCallback();
        SableEventPlatform.INSTANCE.onPhysicsTick(
                TrackEndpointContactPhysics::flushPendingForces
        );
        physicsCallbackRegistered = true;
    }

    public static void submit(
            State state,
            ServerSubLevel subLevel,
            double timeStep,
            Vec3 wheelCenter,
            Direction facing,
            double contactRadius
    ) {
        BlockEntity mount = state.mount;
        Level level = mount.getLevel();
        if (level == null || timeStep <= 0.0D) {
            return;
        }

        Pose3d vehiclePose = subLevel.logicalPose();
        Direction rollingDirection = facing.getClockWise();
        Vector3d rollingAxis = new Vector3d(
                rollingDirection.getStepX(),
                0.0D,
                rollingDirection.getStepZ()
        );
        Vector3d lateralAxis = new Vector3d(
                facing.getStepX(),
                0.0D,
                facing.getStepZ()
        );
        TerrainContact contact = scanTerrain(
                mount,
                level,
                vehiclePose,
                wheelCenter,
                rollingAxis,
                contactRadius
        );
        double tolerance = state.offGround
                ? CONTACT_TOLERANCE
                : CONTACT_RELEASE_TOLERANCE;
        if (contact.distance > contactRadius + tolerance) {
            state.offGround = true;
            return;
        }
        state.offGround = false;

        state.forcePosition.set(
                wheelCenter.x,
                wheelCenter.y,
                wheelCenter.z
        );
        Vector3d worldVelocity = Sable.HELPER.getVelocity(
                level,
                state.forcePosition,
                new Vector3d()
        );
        Vector3d localVelocity = vehiclePose.transformNormalInverse(
                worldVelocity,
                new Vector3d()
        );
        Vector3d localNormal = contactNormal(contact, vehiclePose);
        if (localNormal == null) {
            return;
        }

        double inverseNormalMass = subLevel.getMassTracker()
                .getInverseNormalMass(state.forcePosition, localNormal);
        if (!Double.isFinite(inverseNormalMass)
                || inverseNormalMass <= 1.0E-8D) {
            return;
        }

        double positionError = contactRadius - contact.distance;
        double correctionSpeed = Mth.clamp(
                positionError * POSITION_CORRECTION / timeStep,
                0.0D,
                MAX_CORRECTION_SPEED
        );
        Vector3d localGravity = DimensionPhysicsData.getGravity(level);
        vehiclePose.transformNormalInverse(localGravity);
        double contactRange = Math.max(CONTACT_TOLERANCE, SUPPORT_SLOP);
        double contactWeight = Mth.clamp(
                (positionError + contactRange) / contactRange,
                0.0D,
                1.0D
        );
        double supportVelocity = correctionSpeed + Math.max(
                -localGravity.dot(localNormal) * timeStep,
                0.0D
        ) * contactWeight;

        projectOntoContactPlane(lateralAxis, localNormal);
        projectOntoContactPlane(rollingAxis, localNormal);
        if (lateralAxis.lengthSquared() < 1.0E-8D) {
            lateralAxis.set(rollingAxis).cross(localNormal);
        }
        lateralAxis.normalize();
        rollingAxis.fma(-rollingAxis.dot(lateralAxis), lateralAxis);
        if (rollingAxis.lengthSquared() < 1.0E-8D) {
            rollingAxis.set(localNormal).cross(lateralAxis);
        }
        rollingAxis.normalize();

        double inverseRollingMass = subLevel.getMassTracker()
                .getInverseNormalMass(state.forcePosition, rollingAxis);
        double friction = contact.hitBlock == null
                ? 1.0D
                : adjustedFriction(PhysicsBlockPropertyHelper.getFriction(
                        level.getBlockState(contact.hitBlock)
                ));
        double usableFriction = Math.min(friction, 1.0D);
        Vector3d position = new Vector3d(state.forcePosition);
        Vector3d normal = new Vector3d(localNormal);
        Vector3d lateral = new Vector3d(lateralAxis);
        Vector3d rolling = new Vector3d(rollingAxis);

        RigidContactSolver.submit(
                level,
                subLevel,
                position,
                normal,
                localVelocity.dot(normal),
                supportVelocity,
                0.0D,
                lateral,
                LATERAL_RESPONSE,
                usableFriction * MAX_FRICTION_MULTIPLIER,
                (normalImpulse, lateralImpulse, postVelocity) -> {
                    if (!Double.isFinite(normalImpulse)
                            || normalImpulse <= 0.0D) {
                        return;
                    }
                    double maxFrictionImpulse = normalImpulse
                            * usableFriction
                            * MAX_FRICTION_MULTIPLIER;
                    Vector3d impulse = new Vector3d(normal)
                            .mul(normalImpulse)
                            .fma(lateralImpulse, lateral);
                    if (Double.isFinite(inverseRollingMass)
                            && inverseRollingMass > 1.0E-8D) {
                        double rollingImpulse = -postVelocity.dot(rolling)
                                / inverseRollingMass
                                * ROLLING_RESISTANCE
                                * usableFriction;
                        impulse.fma(
                                Mth.clamp(
                                        rollingImpulse,
                                        -maxFrictionImpulse,
                                        maxFrictionImpulse
                                ),
                                rolling
                        );
                    }
                    state.accumulatedForces.applyImpulseAtPoint(
                            subLevel,
                            position,
                            impulse
                    );
                }
        );
        PENDING_FORCE_APPLICATIONS.add(state);
    }

    private static TerrainContact scanTerrain(
            BlockEntity mount,
            Level level,
            Pose3dc vehiclePose,
            Vec3 wheelCenter,
            Vector3dc rollingAxis,
            double radius
    ) {
        double closestDistance = MAX_GROUND_SCAN;
        Direction closestNormal = Direction.UP;
        SubLevel closestSubLevel = null;
        BlockPos closestBlock = null;
        Vec3 rolling = JOMLConversion.toMojang(rollingAxis);

        for (int offset = -1; offset <= 1; offset++) {
            Vec3 rayStart = wheelCenter
                    .add(rolling.scale(offset))
                    .add(0.0D, radius + CONTACT_TOLERANCE, 0.0D);
            ClipContext context = new ClipContext(
                    rayStart,
                    rayStart.subtract(0.0D, MAX_GROUND_SCAN, 0.0D),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
            );
            ((ClipContextExtension) context).sable$setIgnoredSubLevel(
                    Sable.HELPER.getContaining(mount)
            );
            BlockHitResult hit = level.clip(context);
            if (hit.getType() == BlockHitResult.Type.MISS) {
                continue;
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
            double rayDistance = wheelCenter.y - localHit.y;
            if (localHit.y > wheelCenter.y + radius + CONTACT_TOLERANCE
                    || rayStart.distanceTo(localHit) < 0.05D) {
                continue;
            }

            Vector3d normal = new Vector3d(
                    hit.getDirection().getStepX(),
                    hit.getDirection().getStepY(),
                    hit.getDirection().getStepZ()
            );
            if (hitSubLevel != null) {
                hitSubLevel.logicalPose().transformNormal(normal);
            }
            vehiclePose.transformNormalInverse(normal);
            if (normal.lengthSquared() < 1.0E-8D) {
                continue;
            }
            normal.normalize();
            double suspensionProjection = normal.y;
            if (suspensionProjection < 0.5D) {
                continue;
            }

            double rollingProjection = normal.dot(rollingAxis);
            double wheelPlaneProjection = Math.sqrt(
                    suspensionProjection * suspensionProjection
                            + rollingProjection * rollingProjection
            );
            double distance = rayDistance
                    - rollingProjection * offset / suspensionProjection
                    + radius * (1.0D
                    - wheelPlaneProjection / suspensionProjection);
            if (distance >= closestDistance) {
                continue;
            }
            closestDistance = distance;
            closestNormal = hit.getDirection();
            closestSubLevel = hitSubLevel;
            closestBlock = hit.getBlockPos();
        }
        return new TerrainContact(
                closestDistance,
                closestNormal,
                closestSubLevel,
                closestBlock
        );
    }

    private static Vector3d contactNormal(
            TerrainContact contact,
            Pose3dc vehiclePose
    ) {
        Vector3d normal = new Vector3d(
                contact.normal.getStepX(),
                contact.normal.getStepY(),
                contact.normal.getStepZ()
        );
        if (contact.hitSubLevel != null) {
            contact.hitSubLevel.logicalPose().transformNormal(normal);
        }
        vehiclePose.transformNormalInverse(normal);
        if (normal.lengthSquared() < 1.0E-8D) {
            return null;
        }
        return normal.normalize();
    }

    private static void projectOntoContactPlane(
            Vector3d direction,
            Vector3dc normal
    ) {
        direction.fma(-direction.dot(normal), normal);
    }

    private static double adjustedFriction(double friction) {
        return friction < 1.0D ? 0.1D + 0.9D * friction : friction;
    }

    private static void flushPendingForces(
            SubLevelPhysicsSystem physicsSystem,
            double timeStep
    ) {
        for (State state : PENDING_FORCE_APPLICATIONS) {
            if (!state.mount.isRemoved()) {
                state.applyAccumulatedForces();
            }
        }
        PENDING_FORCE_APPLICATIONS.clear();
    }

    public static final class State {
        private final BlockEntity mount;
        private final Vector3d forcePosition = new Vector3d();
        private final ForceTotal accumulatedForces = new ForceTotal();
        private boolean offGround = true;

        public State(BlockEntity mount) {
            this.mount = mount;
        }

        private void applyAccumulatedForces() {
            SubLevel containing = Sable.HELPER.getContaining(mount);
            if (containing instanceof ServerSubLevel serverSubLevel) {
                RigidBodyHandle.of(serverSubLevel).applyForcesAndReset(
                        accumulatedForces
                );
            }
        }
    }

    private record TerrainContact(
            double distance,
            Direction normal,
            SubLevel hitSubLevel,
            BlockPos hitBlock
    ) {
    }
}
