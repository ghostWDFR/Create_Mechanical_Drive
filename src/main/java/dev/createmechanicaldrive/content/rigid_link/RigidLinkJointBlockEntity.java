package dev.createmechanicaldrive.content.rigid_link;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class RigidLinkJointBlockEntity extends SmartBlockEntity
        implements BlockEntitySubLevelActor {
    public static final int MAX_LINKS = 2;
    public static final double MIN_LENGTH = 1.0D;
    public static final double MAX_LENGTH = 10.0D;
    private static final double FREE_INSTALLATION_POSE_TOLERANCE = 0.25D;
    private static final double LENGTH_EPSILON = 1.0E-5D;

    private static final double MIN_DIRECTION_DOT = Math.cos(Math.toRadians(135.0D));
    private static final double POSITION_CORRECTION = 0.85D;
    private static final double LIMITED_POSITION_CORRECTION = 0.3D;
    private static final double LIMITED_MAX_POSITION_CORRECTION_SPEED = 2.5D;
    private static final double BREAK_FORCE = 250000.0D;
    private static final double FORCED_BREAK_LENGTH_CHANGE = 1.0D;
    private static final double LIMITED_FORCED_SIDE_CHANGE = 1.0D;
    private static final double LIMITED_PLANE_ALIGNMENT_DOT =
            Math.cos(Math.toRadians(1.0D));
    private static final double LIMITED_PLANE_DIRECTION_DOT =
            Math.sin(Math.toRadians(1.0D));
    private static final double LIMITED_RUNTIME_AXIS_BREAK_DOT =
            Math.cos(Math.toRadians(45.0D));
    private static final double LIMITED_ANGULAR_POSITION_CORRECTION = 0.2D;
    private static final double LIMITED_BREAK_ANGULAR_CORRECTION_SPEED = 12.0D;
    private static final double MIN_DISTANCE = 1.0E-5D;
    private static final double MIN_INERTIA_DETERMINANT = 1.0E-12D;
    private static final String TAG_LINKS = "RigidLinks";
    private static final String TAG_LINK_ID = "LinkId";
    private static final String TAG_POS = "Pos";
    private static final String TAG_SUB_LEVEL = "SubLevel";
    private static final String TAG_REST_LENGTH = "RestLength";
    private static final String TAG_CONTROLLER = "Controller";
    private static final String TAG_LINK_TYPE = "LinkType";

    private final List<Connection> links = new ArrayList<>(MAX_LINKS);
    private final ForceTotal constraintForces = new ForceTotal();
    private final ForceTotal partnerConstraintForces = new ForceTotal();
    private boolean destroyingLinks;
    private int validationCountdown;
    private boolean ponderRenderOffsetEnabled;
    private Vec3 previousPonderRenderOffset = Vec3.ZERO;
    private Vec3 targetPonderRenderOffset = Vec3.ZERO;
    private int ponderRenderOffsetTicks;
    private int ponderRenderOffsetDuration;

    public RigidLinkJointBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries,
                         boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        writeLinks(tag);
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        writeLinks(tag);
    }

    private void writeLinks(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Connection link : links) {
            CompoundTag entry = new CompoundTag();
            if (link.id() != null) {
                entry.putUUID(TAG_LINK_ID, link.id());
            }
            entry.putLong(TAG_POS, link.pos().asLong());
            if (link.subLevelId() != null) {
                entry.putUUID(TAG_SUB_LEVEL, link.subLevelId());
            }
            entry.putDouble(TAG_REST_LENGTH, link.restLength());
            entry.putBoolean(TAG_CONTROLLER, link.controller());
            entry.putString(TAG_LINK_TYPE, link.linkType().serializedName());
            list.add(entry);
        }
        tag.put(TAG_LINKS, list);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries,
                        boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        links.clear();
        ListTag list = tag.getList(TAG_LINKS, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size() && links.size() < MAX_LINKS; index++) {
            CompoundTag entry = list.getCompound(index);
            if (!entry.contains(TAG_POS)) {
                continue;
            }
            links.add(new Connection(
                    entry.hasUUID(TAG_LINK_ID) ? entry.getUUID(TAG_LINK_ID) : null,
                    BlockPos.of(entry.getLong(TAG_POS)),
                    entry.hasUUID(TAG_SUB_LEVEL) ? entry.getUUID(TAG_SUB_LEVEL) : null,
                    sanitizeRestLength(entry.getDouble(TAG_REST_LENGTH)),
                    entry.getBoolean(TAG_CONTROLLER),
                    LinkType.fromSerializedName(entry.getString(TAG_LINK_TYPE))
            ));
        }
    }

    @Override
    public void tick() {
        super.tick();
        tickPonderRenderOffset();
        if (level == null || level.isClientSide || links.isEmpty()) {
            return;
        }
        if (validationCountdown-- > 0) {
            return;
        }
        validationCountdown = 4;

        for (Connection link : List.copyOf(links)) {
            RigidLinkJointBlockEntity other = resolve(link);
            if (other == null) {
                UUID ownSubLevel =
                        SableSubLevelHelper
                                .getSubLevelId(
                                        level,
                                        worldPosition
                                );

                boolean sameContainer =
                        Objects.equals(
                                ownSubLevel,
                                link.subLevelId()
                        );

                if (sameContainer
                        || level.isLoaded(
                        link.pos()
                )) {
                    destroyConnection(
                            link,
                            true
                    );
                }

                continue;
            }
            if (!references(other, link)
                    || isLengthDeformed(other, link)
                    || !isRuntimeGeometryValid(other, link)) {
                destroyConnection(link, true);
            }
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle,
                                  double timeStep) {
        if (level == null || level.isClientSide || links.isEmpty() || timeStep <= 0.0D) {
            return;
        }
        UUID ownSubLevelId = SableSubLevelHelper.getSubLevelId(level, worldPosition);
        if (!Objects.equals(ownSubLevelId, subLevel.getUniqueId())) {
            return;
        }

        for (Connection link : List.copyOf(links)) {
            if (!shouldControlPhysics(ownSubLevelId, link.subLevelId())) {
                continue;
            }
            RigidLinkJointBlockEntity other = resolve(link);
            if (other == null || !references(other, link)) {
                continue;
            }
            ServerSubLevel otherSubLevel = resolveServerSubLevel(link.subLevelId());
            if (link.subLevelId() != null && otherSubLevel == null) {
                continue;
            }
            if (otherSubLevel == subLevel) {
                continue;
            }
            applyRigidLengthConstraint(subLevel, handle, otherSubLevel, other, link, timeStep);
            if (link.linkType() == LinkType.LIMITED && links.contains(link)) {
                applyLimitedPlaneConstraint(subLevel, handle, otherSubLevel,
                        other, link, timeStep);
                if (links.contains(link)) {
                    applyLimitedAngularConstraint(subLevel, handle, otherSubLevel,
                            other, link, timeStep);
                }
            }
        }
    }

    private void applyLimitedAngularConstraint(ServerSubLevel ownSubLevel,
                                               RigidBodyHandle ownHandle,
                                               @Nullable ServerSubLevel otherSubLevel,
                                               RigidLinkJointBlockEntity other,
                                               Connection link,
                                               double timeStep) {
        Vector3d ownAxis = limitedWorldAxis(this);
        Vector3d otherAxis = limitedWorldAxis(other);
        double axisDot = ownAxis.dot(otherAxis);
        if (Math.abs(axisDot) < LIMITED_RUNTIME_AXIS_BREAK_DOT) {
            destroyConnection(link, true);
            return;
        }
        if (axisDot < 0.0D) {
            otherAxis.negate();
        }

        Vector3d hingeAxis = ownAxis.add(otherAxis, new Vector3d());
        if (hingeAxis.lengthSquared() < MIN_DISTANCE * MIN_DISTANCE) {
            destroyConnection(link, true);
            return;
        }
        hingeAxis.normalize();

        RigidBodyHandle otherHandle = otherSubLevel == null
                ? null : RigidBodyHandle.of(otherSubLevel);
        Vector3d ownAngularVelocity = ownHandle.getAngularVelocity(new Vector3d());
        Vector3d otherAngularVelocity = otherHandle == null
                ? new Vector3d()
                : otherHandle.getAngularVelocity(new Vector3d());
        Vector3d relativeAngularVelocity = otherAngularVelocity
                .sub(ownAngularVelocity, new Vector3d());

        Vector3d forbiddenAngularVelocity = relativeAngularVelocity.sub(
                hingeAxis.mul(relativeAngularVelocity.dot(hingeAxis), new Vector3d()),
                new Vector3d());
        Vector3d orientationError = ownAxis.cross(otherAxis, new Vector3d());
        Vector3d correction = forbiddenAngularVelocity.add(
                orientationError.mul(LIMITED_ANGULAR_POSITION_CORRECTION / timeStep,
                        new Vector3d()),
                new Vector3d());
        if (!Double.isFinite(correction.lengthSquared())
                || correction.length() >= LIMITED_BREAK_ANGULAR_CORRECTION_SPEED) {
            destroyConnection(link, true);
            return;
        }

        applyConservativeAngularCorrection(ownSubLevel, ownHandle,
                otherSubLevel, correction);
    }

    private void applyLimitedPlaneConstraint(ServerSubLevel ownSubLevel,
                                             RigidBodyHandle ownHandle,
                                             @Nullable ServerSubLevel otherSubLevel,
                                             RigidLinkJointBlockEntity other,
                                             Connection link,
                                             double timeStep) {
        Vector3d ownAxis = limitedWorldAxis(this);
        Vector3d otherAxis = limitedWorldAxis(other);
        double axisDot = ownAxis.dot(otherAxis);
        if (Math.abs(axisDot) < LIMITED_RUNTIME_AXIS_BREAK_DOT) {
            destroyConnection(link, true);
            return;
        }
        if (axisDot < 0.0D) {
            otherAxis.negate();
        }

        Vector3d planeNormal = ownAxis.add(otherAxis, new Vector3d());
        if (planeNormal.lengthSquared() < MIN_DISTANCE * MIN_DISTANCE) {
            destroyConnection(link, true);
            return;
        }
        planeNormal.normalize();

        Vector3d ownLocal = localCenterOf(worldPosition);
        Vector3d otherLocal = localCenterOf(other.worldPosition);
        Vector3d ownWorld = toWorldPosition(ownSubLevel, ownLocal);
        Vector3d otherWorld = toWorldPosition(otherSubLevel, otherLocal);
        Vector3d delta = otherWorld.sub(ownWorld, new Vector3d());
        double sideError = delta.dot(planeNormal);
        if (Math.abs(sideError) >= LIMITED_FORCED_SIDE_CHANGE) {
            destroyConnection(link, true);
            return;
        }

        Vector3d ownVelocity = Sable.HELPER.getVelocity(level, ownLocal, new Vector3d());
        Vector3d otherVelocity = Sable.HELPER.getVelocity(level, otherLocal, new Vector3d());
        double relativeSpeed = otherVelocity.sub(ownVelocity, new Vector3d())
                .dot(planeNormal);

        Vector3d ownNormal = ownSubLevel.logicalPose()
                .transformNormalInverse(planeNormal, new Vector3d());
        double inverseNormalMass = ownSubLevel.getMassTracker()
                .getInverseNormalMass(ownLocal, ownNormal);
        if (otherSubLevel != null) {
            Vector3d otherNormal = otherSubLevel.logicalPose()
                    .transformNormalInverse(planeNormal, new Vector3d());
            inverseNormalMass += otherSubLevel.getMassTracker()
                    .getInverseNormalMass(otherLocal, otherNormal);
        }
        if (!Double.isFinite(inverseNormalMass) || inverseNormalMass <= MIN_DISTANCE) {
            return;
        }

        double correctionSpeed = limitedCorrectionSpeed(sideError, timeStep);
        double impulseMagnitude = (relativeSpeed + correctionSpeed) / inverseNormalMass;
        double requiredForce = Math.abs(impulseMagnitude / timeStep);
        if (!Double.isFinite(requiredForce) || requiredForce >= BREAK_FORCE) {
            destroyConnection(link, true);
            return;
        }

        Vector3d worldImpulse = planeNormal.mul(impulseMagnitude, new Vector3d());
        applyConstraintImpulse(ownSubLevel, ownHandle, ownLocal,
                otherSubLevel, otherLocal, worldImpulse);
    }

    private void applyRigidLengthConstraint(ServerSubLevel ownSubLevel,
                                            RigidBodyHandle ownHandle,
                                            @Nullable ServerSubLevel otherSubLevel,
                                            RigidLinkJointBlockEntity other,
                                            Connection link,
                                            double timeStep) {
        Vector3d ownLocal = localCenterOf(worldPosition);
        Vector3d otherLocal = localCenterOf(other.worldPosition);
        Vector3d ownWorld = toWorldPosition(ownSubLevel, ownLocal);
        Vector3d otherWorld = toWorldPosition(otherSubLevel, otherLocal);
        Vector3d delta = otherWorld.sub(ownWorld, new Vector3d());
        double distance = delta.length();
        if (distance < MIN_DISTANCE) {
            destroyConnection(link, true);
            return;
        }
        if (Math.abs(distance - link.restLength())
                >= FORCED_BREAK_LENGTH_CHANGE) {
            destroyConnection(link, true);
            return;
        }

        Vector3d direction = delta.div(distance, new Vector3d());
        Vector3d ownVelocity = Sable.HELPER.getVelocity(level, ownLocal, new Vector3d());
        Vector3d otherVelocity = Sable.HELPER.getVelocity(level, otherLocal, new Vector3d());
        double relativeSpeed = otherVelocity.sub(ownVelocity, new Vector3d()).dot(direction);
        Vector3d ownNormal = ownSubLevel.logicalPose()
                .transformNormalInverse(direction, new Vector3d());
        double inverseNormalMass = ownSubLevel.getMassTracker()
                .getInverseNormalMass(ownLocal, ownNormal);
        if (otherSubLevel != null) {
            Vector3d otherNormal = otherSubLevel.logicalPose()
                    .transformNormalInverse(direction, new Vector3d());
            inverseNormalMass += otherSubLevel.getMassTracker()
                    .getInverseNormalMass(otherLocal, otherNormal);
        }
        if (!Double.isFinite(inverseNormalMass) || inverseNormalMass <= MIN_DISTANCE) {
            return;
        }

        double positionError = distance - link.restLength();
        double correctionSpeed = link.linkType() == LinkType.LIMITED
                ? limitedCorrectionSpeed(positionError, timeStep)
                : positionError * POSITION_CORRECTION / timeStep;
        double impulseMagnitude = (relativeSpeed + correctionSpeed) / inverseNormalMass;
        double requiredForce = Math.abs(impulseMagnitude / timeStep);

        if (!Double.isFinite(requiredForce) || requiredForce >= BREAK_FORCE) {
            destroyConnection(link, true);
            return;
        }

        Vector3d worldImpulse = direction.mul(impulseMagnitude, new Vector3d());
        applyConstraintImpulse(ownSubLevel, ownHandle, ownLocal,
                otherSubLevel, otherLocal, worldImpulse);
    }

    private void applyConstraintImpulse(ServerSubLevel ownSubLevel,
                                        RigidBodyHandle ownHandle,
                                        Vector3d ownLocal,
                                        @Nullable ServerSubLevel otherSubLevel,
                                        Vector3d otherLocal,
                                        Vector3d worldImpulse) {
        Vector3d ownAngularBalance = null;
        Vector3d otherAngularBalance = null;
        if (otherSubLevel != null) {
            Vector3d ownWorld = toWorldPosition(ownSubLevel, ownLocal);
            Vector3d otherWorld = toWorldPosition(otherSubLevel, otherLocal);
            Vector3d missingAngularImpulse = otherWorld
                    .sub(ownWorld, new Vector3d())
                    .cross(worldImpulse, new Vector3d());
            if (missingAngularImpulse.lengthSquared()
                    >= MIN_DISTANCE * MIN_DISTANCE) {
                Matrix3d ownInertia = worldInertiaMatrix(ownSubLevel, false);
                Matrix3d otherInertia = worldInertiaMatrix(otherSubLevel, false);
                Vector3d commonAngularVelocity = solveAngularSystem(
                        new Matrix3d(ownInertia).add(otherInertia),
                        missingAngularImpulse);
                if (commonAngularVelocity != null) {
                    ownAngularBalance = ownInertia.transform(
                            commonAngularVelocity, new Vector3d());
                    otherAngularBalance = otherInertia.transform(
                            commonAngularVelocity, new Vector3d());
                }
            }
        }

        Vector3d ownImpulse = ownSubLevel.logicalPose()
                .transformNormalInverse(worldImpulse, new Vector3d());
        constraintForces.applyImpulseAtPoint(ownSubLevel, ownLocal, ownImpulse);
        if (ownAngularBalance != null) {
            constraintForces.applyAngularImpulse(ownSubLevel.logicalPose()
                    .transformNormalInverse(ownAngularBalance, new Vector3d()));
        }
        ownHandle.applyForcesAndReset(constraintForces);

        if (otherSubLevel == null) {
            return;
        }
        Vector3d otherImpulse = otherSubLevel.logicalPose().transformNormalInverse(
                worldImpulse.negate(new Vector3d()), new Vector3d());
        partnerConstraintForces.applyImpulseAtPoint(otherSubLevel, otherLocal, otherImpulse);
        if (otherAngularBalance != null) {
            partnerConstraintForces.applyAngularImpulse(otherSubLevel.logicalPose()
                    .transformNormalInverse(otherAngularBalance, new Vector3d()));
        }
        RigidBodyHandle.of(otherSubLevel).applyForcesAndReset(partnerConstraintForces);
    }

    private static void applyConservativeAngularCorrection(
            ServerSubLevel ownSubLevel,
            RigidBodyHandle ownHandle,
            @Nullable ServerSubLevel otherSubLevel,
            Vector3d relativeVelocityCorrection) {
        if (relativeVelocityCorrection.lengthSquared()
                < MIN_DISTANCE * MIN_DISTANCE) {
            return;
        }

        Matrix3d inverseAngularMass = worldInertiaMatrix(ownSubLevel, true);
        if (otherSubLevel != null) {
            inverseAngularMass.add(worldInertiaMatrix(otherSubLevel, true));
        }
        Vector3d worldAngularImpulse = solveAngularSystem(
                inverseAngularMass, relativeVelocityCorrection);
        if (worldAngularImpulse == null) {
            return;
        }

        Vector3d ownLocalImpulse = ownSubLevel.logicalPose()
                .transformNormalInverse(worldAngularImpulse, new Vector3d());
        ownHandle.applyAngularImpulse(ownLocalImpulse);
        if (otherSubLevel == null) {
            return;
        }

        Vector3d otherLocalImpulse = otherSubLevel.logicalPose()
                .transformNormalInverse(
                        worldAngularImpulse.negate(new Vector3d()),
                        new Vector3d());
        RigidBodyHandle.of(otherSubLevel).applyAngularImpulse(otherLocalImpulse);
    }

    private static Matrix3d worldInertiaMatrix(ServerSubLevel subLevel,
                                                boolean inverse) {
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

    public void setPonderRenderOffset(Vec3 offset, int duration) {
        ponderRenderOffsetEnabled = true;
        previousPonderRenderOffset = getPonderRenderOffset(1.0F);
        targetPonderRenderOffset = offset;
        ponderRenderOffsetTicks = 0;
        ponderRenderOffsetDuration = Math.max(0, duration);

        if (ponderRenderOffsetDuration == 0) {
            previousPonderRenderOffset = targetPonderRenderOffset;
        }
    }

    public boolean hasPonderRenderOffset() {
        return ponderRenderOffsetEnabled;
    }

    public Vec3 getPonderRenderOffset(float partialTicks) {
        if (ponderRenderOffsetDuration <= 0) {
            return targetPonderRenderOffset;
        }

        double progress = Mth.clamp(
                (ponderRenderOffsetTicks + partialTicks)
                        / ponderRenderOffsetDuration,
                0.0F,
                1.0F
        );
        return previousPonderRenderOffset.lerp(
                targetPonderRenderOffset,
                progress
        );
    }

    private void tickPonderRenderOffset() {
        if (ponderRenderOffsetDuration <= 0) {
            return;
        }

        ponderRenderOffsetTicks++;
        if (ponderRenderOffsetTicks >= ponderRenderOffsetDuration) {
            previousPonderRenderOffset = targetPonderRenderOffset;
            ponderRenderOffsetDuration = 0;
        }
    }

    @Nullable
    private static Vector3d solveAngularSystem(Matrix3d matrix,
                                               Vector3d target) {
        double determinant = matrix.determinant();
        if (!matrix.isFinite() || !Double.isFinite(determinant)
                || Math.abs(determinant) <= MIN_INERTIA_DETERMINANT) {
            return null;
        }
        Vector3d solution = matrix.invert(new Matrix3d())
                .transform(target, new Vector3d());
        return solution.isFinite() ? solution : null;
    }

    public LinkResult createMutualLink(RigidLinkJointBlockEntity other) {
        return createMutualLink(other, LinkType.FREE);
    }

    public LinkResult createMutualLink(RigidLinkJointBlockEntity other,
                                       LinkType linkType) {
        if (level == null || other == null || other.level != level) {
            return LinkResult.INVALID;
        }
        if (!canAccept(linkType) || !other.canAccept(linkType)) {
            return LinkResult.FULL;
        }
        if (getConnectionTo(other) != null || other.getConnectionTo(this) != null) {
            return LinkResult.DUPLICATE;
        }
        double length = worldCenter().distanceTo(other.worldCenter());
        if (!isLengthValid(length, linkType)) {
            return LinkResult.INVALID_LENGTH;
        }
        if (linkType == LinkType.FREE && !isRuntimeAngleValid(other)) {
            return LinkResult.INVALID_ANGLE;
        }
        if (linkType == LinkType.LIMITED && !isLimitedPlacementValid(other)) {
            return LinkResult.INVALID_PLANE;
        }

        UUID ownSubLevel = SableSubLevelHelper.getSubLevelId(level, worldPosition);
        UUID otherSubLevel = SableSubLevelHelper.getSubLevelId(level, other.worldPosition);
        UUID linkId = UUID.randomUUID();
        addConnection(new Connection(linkId, other.worldPosition, otherSubLevel,
                length, true, linkType));
        other.addConnection(new Connection(linkId, worldPosition, ownSubLevel,
                length, false, linkType));
        return LinkResult.SUCCESS;
    }

    private void addConnection(Connection connection) {
        links.add(connection);
        syncLinkState();
    }

    /**
     * Rebinds the copied link records after Sable moves this joint between the
     * world and a sub-level, or between two sub-levels. Only the address of the
     * moved endpoint changes; the link id, rest length and ownership stay intact.
     */
    public void afterAssemblyMove(ServerLevel originLevel,
                                  ServerLevel resultingLevel,
                                  BlockPos oldPos) {
        if (links.isEmpty()) {
            return;
        }

        UUID oldSubLevelId = SableSubLevelHelper.getSubLevelId(originLevel, oldPos);
        UUID newSubLevelId = SableSubLevelHelper.getSubLevelId(
                resultingLevel, worldPosition);

        for (Connection connection : List.copyOf(links)) {
            RigidLinkJointBlockEntity other = findJointDuringAssembly(
                    originLevel, resultingLevel, connection);
            if (other != null) {
                other.replaceMovedEndpoint(connection, oldPos, oldSubLevelId,
                        worldPosition, newSubLevelId);
            }
        }

        syncLinkState();
    }

    @Nullable
    private static RigidLinkJointBlockEntity findJointDuringAssembly(
            ServerLevel originLevel, ServerLevel resultingLevel,
            Connection connection) {
        BlockEntity resultEntity = resultingLevel.getBlockEntity(connection.pos());
        if (resultEntity instanceof RigidLinkJointBlockEntity resultJoint
                && resultJoint.containsLink(connection)) {
            return resultJoint;
        }

        if (originLevel != resultingLevel) {
            BlockEntity originEntity = originLevel.getBlockEntity(connection.pos());
            if (originEntity instanceof RigidLinkJointBlockEntity originJoint
                    && originJoint.containsLink(connection)) {
                return originJoint;
            }
        }
        return null;
    }

    private boolean containsLink(Connection connection) {
        return connection.id() == null
                || links.stream().anyMatch(link -> sameLinkId(link, connection));
    }

    private void replaceMovedEndpoint(Connection sourceConnection,
                                      BlockPos oldPos,
                                      @Nullable UUID oldSubLevelId,
                                      BlockPos newPos,
                                      @Nullable UUID newSubLevelId) {
        boolean changed = false;
        for (int index = 0; index < links.size(); index++) {
            Connection current = links.get(index);
            boolean matches = sameLinkId(current, sourceConnection)
                    || current.id() == null && sourceConnection.id() == null
                    && current.pos().equals(oldPos)
                    && Objects.equals(current.subLevelId(), oldSubLevelId);
            if (!matches) {
                continue;
            }

            links.set(index, current.withEndpoint(newPos, newSubLevelId));
            changed = true;
        }
        if (changed) {
            syncLinkState();
        }
    }

    public void destroyAllLinks(
            boolean dropItems
    ) {
        if (destroyingLinks) {
            return;
        }

        destroyingLinks = true;

        try {
            for (Connection connection :
                    List.copyOf(links)) {

                destroyConnection(
                        connection,
                        dropItems
                );
            }
        } finally {
            destroyingLinks = false;
        }
    }

    public void destroyConnection(
            Connection connection,
            boolean dropItem
    ) {
        if (!links.contains(connection)) {
            return;
        }

        RigidLinkJointBlockEntity other =
                resolve(
                        connection
                );

        if (!connection.controller()
                && other != null) {

            Connection controllerConnection =
                    other.findReciprocalConnection(
                            this,
                            connection
                    );

            if (controllerConnection != null
                    && controllerConnection.controller()) {

                other.destroyConnection(
                        controllerConnection,
                        dropItem
                );

                return;
            }
        }

        if (!removeLocalConnection(
                connection
        )) {
            return;
        }

        if (other != null) {
            Connection reciprocal =
                    other.findReciprocalConnection(
                            this,
                            connection
                    );

            if (reciprocal != null) {
                other.removeLocalConnection(
                        reciprocal
                );
            }
        }

        if (dropItem
                && connection.controller()
                && level instanceof ServerLevel serverLevel) {

            Block.popResource(
                    serverLevel,
                    worldPosition,
                    new ItemStack(
                            connection.linkType()
                                    == LinkType.LIMITED
                                    ? CreateMechanicalDrive
                                    .RIGID_LINK_LIMITED_ITEM
                                    .get()
                                    : CreateMechanicalDrive
                                    .RIGID_LINK_ITEM
                                    .get()
                    )
            );
        }
    }

    private void removeReferenceTo(RigidLinkJointBlockEntity other,
                                   Connection sourceConnection) {
        UUID otherSubLevel = level == null ? null
                : SableSubLevelHelper.getSubLevelId(level, other.worldPosition);
        if (links.removeIf(link -> sameLinkId(link, sourceConnection)
                || link.pos().equals(other.worldPosition)
                && Objects.equals(link.subLevelId(), otherSubLevel))) {
            syncLinkState();
        }
    }

    @Nullable
    private Connection findReciprocalConnection(RigidLinkJointBlockEntity other,
                                                Connection sourceConnection) {
        UUID otherSubLevel = level == null ? null
                : SableSubLevelHelper.getSubLevelId(level, other.worldPosition);
        return links.stream().filter(link -> sameLinkId(link, sourceConnection)
                || link.pos().equals(other.worldPosition)
                && Objects.equals(link.subLevelId(), otherSubLevel))
                .findFirst().orElse(null);
    }

    private static boolean sameLinkId(Connection first, Connection second) {
        return first.id() != null && second.id() != null
                && first.id().equals(second.id());
    }

    @Nullable
    private Connection findConnectionById(
            UUID linkId
    ) {
        if (linkId == null) {
            return null;
        }

        return links.stream()
                .filter(
                        link -> link.id() != null
                                && link.id().equals(
                                linkId
                        )
                )
                .findFirst()
                .orElse(null);
    }

    private boolean removeLocalConnection(
            Connection connection
    ) {
        if (!links.remove(connection)) {
            return false;
        }

        syncLinkState();

        return true;
    }

    private void syncLinkState() {
        setChanged();
        sendData();
        invalidateRenderBoundingBox();
    }

    public boolean hasCapacity() {
        return links.isEmpty()
                || links.size() < MAX_LINKS
                && links.stream().allMatch(link -> link.linkType() == LinkType.FREE);
    }

    public boolean canAccept(LinkType requestedType) {
        if (links.isEmpty()) {
            return true;
        }
        return requestedType == LinkType.FREE
                && links.size() < MAX_LINKS
                && links.stream().allMatch(link -> link.linkType() == LinkType.FREE);
    }

    public boolean hasLinks() {
        return !links.isEmpty();
    }

    public boolean hasConnectionTo(RigidLinkJointBlockEntity other) {
        return getConnectionTo(other) != null;
    }

    public List<Connection> getLinks() {
        return List.copyOf(links);
    }

    public List<Connection> getValidRenderLinks() {
        if (level == null || links.isEmpty()) {
            return List.of();
        }

        List<Connection> valid = new ArrayList<>();

        for (Connection link : links) {
            RigidLinkJointBlockEntity other = resolve(link);

            if (other == null) {
                continue;
            }

            if (!references(other, link)) {
                continue;
            }

            valid.add(link);
        }

        return List.copyOf(valid);
    }

    @Nullable
    public LinkType getConnectorType() {
        return links.isEmpty() ? null : links.getFirst().linkType();
    }

    public boolean references(RigidLinkJointBlockEntity other, Connection connection) {
        if (level == null || other == null) {
            return false;
        }
        UUID ownSubLevel = SableSubLevelHelper.getSubLevelId(level, worldPosition);
        UUID actualOtherSubLevel = SableSubLevelHelper.getSubLevelId(level, other.worldPosition);
        if (!connection.pos().equals(other.worldPosition)
                || !Objects.equals(connection.subLevelId(), actualOtherSubLevel)) {
            return false;
        }
        return other.links.stream().anyMatch(link -> sameLinkId(link, connection)
                || link.pos().equals(worldPosition)
                && Objects.equals(link.subLevelId(), ownSubLevel)
                && link.linkType() == connection.linkType());
    }

    @Nullable
    public RigidLinkJointBlockEntity resolve(Connection connection) {
        if (level == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(connection.pos());
        if (!(blockEntity instanceof RigidLinkJointBlockEntity joint)) {
            return null;
        }
        UUID actualSubLevel = SableSubLevelHelper.getSubLevelId(level, connection.pos());
        return Objects.equals(connection.subLevelId(), actualSubLevel) ? joint : null;
    }

    @Nullable
    private Connection getConnectionTo(RigidLinkJointBlockEntity other) {
        UUID otherSubLevel = SableSubLevelHelper.getSubLevelId(level, other.worldPosition);
        return links.stream().filter(link -> link.pos().equals(other.worldPosition)
                && Objects.equals(link.subLevelId(), otherSubLevel)).findFirst().orElse(null);
    }

    private boolean isRuntimeAngleValid(RigidLinkJointBlockEntity other) {
        Vec3 ownCenter = worldCenter();
        Vec3 otherCenter = other.worldCenter();
        Vec3 direction = otherCenter.subtract(ownCenter);
        if (direction.lengthSqr() < MIN_DISTANCE * MIN_DISTANCE) {
            return false;
        }
        Direction ownFacing = getBlockState().getValue(RigidLinkJointBlock.FACING);
        Direction otherFacing = other.getBlockState().getValue(RigidLinkJointBlock.FACING);
        return isDirectionValid(level, worldPosition, ownFacing, direction)
                && isDirectionValid(level, other.worldPosition, otherFacing, direction.scale(-1.0D));
    }

    private boolean isRuntimeGeometryValid(RigidLinkJointBlockEntity other,
                                           Connection connection) {
        return connection.linkType() == LinkType.FREE
                ? isRuntimeAngleValid(other)
                : isLimitedRuntimeGeometryValid(other);
    }

    private boolean isLimitedPlacementValid(RigidLinkJointBlockEntity other) {
        Direction ownFacing = getBlockState().getValue(RigidLinkJointBlock.FACING);
        Direction otherFacing = other.getBlockState().getValue(RigidLinkJointBlock.FACING);
        return isLimitedPlacementValid(level, worldPosition, ownFacing,
                other.worldPosition, otherFacing);
    }

    public static boolean isLimitedPlacementValid(Level level,
                                                  BlockPos firstPos,
                                                  Direction firstFacing,
                                                  BlockPos secondPos,
                                                  Direction secondFacing) {
        if (level == null) {
            return false;
        }
        Vec3 delta = SableSubLevelHelper.getWorldCenter(level, secondPos)
                .subtract(SableSubLevelHelper.getWorldCenter(level, firstPos));
        if (delta.lengthSqr() < MIN_DISTANCE * MIN_DISTANCE) {
            return false;
        }
        Vec3 firstAxis = worldJointAxis(level, firstPos, firstFacing);
        Vec3 secondAxis = worldJointAxis(level, secondPos, secondFacing);
        Vec3 direction = delta.normalize();
        return Math.abs(firstAxis.dot(secondAxis)) >= LIMITED_PLANE_ALIGNMENT_DOT
                && Math.abs(firstAxis.dot(direction)) <= LIMITED_PLANE_DIRECTION_DOT
                && Math.abs(secondAxis.dot(direction)) <= LIMITED_PLANE_DIRECTION_DOT;
    }

    private boolean isLimitedRuntimeGeometryValid(RigidLinkJointBlockEntity other) {
        Direction ownFacing = getBlockState().getValue(RigidLinkJointBlock.FACING);
        Direction otherFacing = other.getBlockState().getValue(RigidLinkJointBlock.FACING);
        Vec3 ownAxis = worldJointAxis(level, worldPosition, ownFacing);
        Vec3 otherAxis = worldJointAxis(level, other.worldPosition, otherFacing);
        if (Math.abs(ownAxis.dot(otherAxis)) < LIMITED_RUNTIME_AXIS_BREAK_DOT) {
            return false;
        }
        Vec3 delta = other.worldCenter().subtract(worldCenter());
        return Math.abs(delta.dot(ownAxis)) < LIMITED_FORCED_SIDE_CHANGE
                && Math.abs(delta.dot(otherAxis)) < LIMITED_FORCED_SIDE_CHANGE;
    }

    private static Vec3 worldJointAxis(Level level, BlockPos pos, Direction facing) {
        return SableSubLevelHelper.getWorldNormal(level, pos,
                Vec3.atLowerCornerOf(facing.getNormal())).normalize();
    }

    private static Vector3d limitedWorldAxis(RigidLinkJointBlockEntity joint) {
        Direction facing = joint.getBlockState().getValue(RigidLinkJointBlock.FACING);
        Vec3 axis = worldJointAxis(joint.level, joint.worldPosition, facing);
        return new Vector3d(axis.x, axis.y, axis.z);
    }

    private boolean isLengthDeformed(RigidLinkJointBlockEntity other,
                                     Connection connection) {
        return Math.abs(worldCenter().distanceTo(other.worldCenter())
                - connection.restLength()) >= FORCED_BREAK_LENGTH_CHANGE;
    }

    public static boolean isDirectionValid(Level level, BlockPos pos,
                                           Direction facing, Vec3 linkDirection) {
        if (level == null || linkDirection.lengthSqr() < MIN_DISTANCE * MIN_DISTANCE) {
            return false;
        }
        Vec3 worldNormal = SableSubLevelHelper.getWorldNormal(level, pos,
                Vec3.atLowerCornerOf(facing.getNormal()));
        return worldNormal.lengthSqr() >= MIN_DISTANCE * MIN_DISTANCE
                && worldNormal.normalize().dot(linkDirection.normalize()) >= MIN_DIRECTION_DOT;
    }

    public static boolean isLengthValid(double length, LinkType linkType) {
        double minimumLength = linkType == LinkType.FREE
                ? MIN_LENGTH - FREE_INSTALLATION_POSE_TOLERANCE
                : MIN_LENGTH;
        return Double.isFinite(length)
                && length + LENGTH_EPSILON >= minimumLength
                && length - LENGTH_EPSILON <= MAX_LENGTH;
    }

    private Vec3 worldCenter() {
        return SableSubLevelHelper.getWorldCenter(level, worldPosition);
    }

    private boolean shouldControlPhysics(@Nullable UUID ownSubLevel,
                                         @Nullable UUID otherSubLevel) {
        if (ownSubLevel == null) {
            return false;
        }
        return otherSubLevel == null
                || (!ownSubLevel.equals(otherSubLevel)
                && ownSubLevel.compareTo(otherSubLevel) < 0);
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
        SubLevel resolved = container.getSubLevel(id);
        return resolved instanceof ServerSubLevel serverSubLevel ? serverSubLevel : null;
    }

    private static Vector3d localCenterOf(BlockPos pos) {
        return new Vector3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private static Vector3d toWorldPosition(@Nullable ServerSubLevel subLevel,
                                            Vector3d localPosition) {
        return subLevel == null ? localPosition
                : subLevel.logicalPose().transformPosition(localPosition, new Vector3d());
    }

    private static double sanitizeRestLength(double length) {
        return Double.isFinite(length) && length > MIN_DISTANCE ? length : MIN_LENGTH;
    }

    private static double limitedCorrectionSpeed(double error, double timeStep) {
        double speed = error * LIMITED_POSITION_CORRECTION / timeStep;
        return Math.max(-LIMITED_MAX_POSITION_CORRECTION_SPEED,
                Math.min(LIMITED_MAX_POSITION_CORRECTION_SPEED, speed));
    }

    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getLoadingDependencies() {
        return linkedSubLevels();
    }

    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getConnectionDependencies() {
        return linkedSubLevels();
    }

    @Nullable
    private Iterable<@NotNull SubLevel> linkedSubLevels() {
        if (level == null || links.isEmpty()) {
            return null;
        }
        UUID ownId = SableSubLevelHelper.getSubLevelId(level, worldPosition);
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }
        List<SubLevel> dependencies = new ArrayList<>();
        for (Connection link : links) {
            if (link.subLevelId() == null || Objects.equals(link.subLevelId(), ownId)) {
                continue;
            }
            SubLevel subLevel = container.getSubLevel(link.subLevelId());
            if (subLevel != null && !subLevel.isRemoved() && !dependencies.contains(subLevel)) {
                dependencies.add(subLevel);
            }
        }
        return dependencies.isEmpty() ? null : dependencies;
    }

    public record Connection(@Nullable UUID id, BlockPos pos, @Nullable UUID subLevelId,
                             double restLength, boolean controller, LinkType linkType) {
        public Connection withEndpoint(BlockPos newPos, @Nullable UUID newSubLevelId) {
            return new Connection(id, newPos.immutable(), newSubLevelId,
                    restLength, controller, linkType);
        }
    }

    public enum LinkType {
        FREE("free"),
        LIMITED("limited");

        private final String serializedName;

        LinkType(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static LinkType fromSerializedName(String name) {
            return LIMITED.serializedName.equals(name) ? LIMITED : FREE;
        }
    }

    public enum LinkResult {
        SUCCESS,
        FULL,
        DUPLICATE,
        INVALID_LENGTH,
        INVALID_ANGLE,
        INVALID_PLANE,
        INVALID
    }
}
