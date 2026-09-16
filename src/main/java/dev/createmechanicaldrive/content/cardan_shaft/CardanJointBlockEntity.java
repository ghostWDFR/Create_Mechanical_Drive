package dev.createmechanicaldrive.content.cardan_shaft;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
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
import org.joml.Vector3d;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CardanJointBlockEntity
        extends KineticBlockEntity
        implements BlockEntitySubLevelActor {
    public static final int MIN_LENGTH = 1;
    public static final int MAX_LENGTH = 5;
    private static final double PHYSICS_SOFT_DISTANCE = 4.0D;
    private static final double PHYSICS_DISCONNECT_DISTANCE = 5.5D;
    private static final double PHYSICS_PULL_STIFFNESS = 12.0D;
    private static final double PHYSICS_PULL_CURVE = 4.0D;
    private static final double PHYSICS_PULL_DAMPING = 7.0D;
    private static final double PHYSICS_PULL_DAMPING_GAIN = 3.0D;
    private static final double PHYSICS_ENDPOINT_PULL_MULTIPLIER = 4.0D;
    private static final double MIN_PHYSICS_DISTANCE = 1.0E-4D;
    private static final double BREAK_WARNING_DISTANCE =
            PHYSICS_DISCONNECT_DISTANCE * 0.85D;
    private static final double MAX_BEND_ANGLE_COS =
            Math.cos(
                    Math.toRadians(80.0D)
            );
    private static final double COMPACT_SHAFT_MAX_DISTANCE = 2.0D;
    private static final double COMPACT_BEND_ANGLE_COS = 0.0D;
    private static final double INSTALLATION_POSE_TOLERANCE = 0.25D;
    private static final double MIN_INSTALLATION_DISTANCE =
            MIN_LENGTH - INSTALLATION_POSE_TOLERANCE;
    private static final double GEOMETRY_EPSILON = 1.0E-5D;

    private static final String TAG_LINKED_POS =
            "LinkedPos";
    private static final String TAG_LINKED_SUB_LEVEL =
            "LinkedSubLevel";
    private static final String TAG_CONTROLLER =
            "Controller";
    private static final String TAG_PENDING =
            "Pending";
    private static final String TAG_COMPACT_GEOMETRY =
            "CompactGeometry";
    private BlockPos linkedPos;
    private UUID linkedSubLevelId;
    private boolean controller;
    private boolean pending;
    private boolean compactGeometry;
    private boolean destroyingLink;
    private boolean ponderRenderOffsetEnabled;
    private Vec3 previousPonderRenderOffset = Vec3.ZERO;
    private Vec3 targetPonderRenderOffset = Vec3.ZERO;
    private int ponderRenderOffsetTicks;
    private int ponderRenderOffsetDuration;
    private boolean ponderRenderYawEnabled;
    private float previousPonderRenderYaw;
    private float targetPonderRenderYaw;
    private int ponderRenderYawTicks;
    private int ponderRenderYawDuration;
    private float ponderBreakWarningProgress = -1.0F;
    private final ForceTotal elasticForceTotal =
            new ForceTotal();
    private final ForceTotal partnerElasticForceTotal =
            new ForceTotal();

    public CardanJointBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.write(
                tag,
                registries,
                clientPacket
        );
        writeCardan(
                tag
        );
    }

    @Override
    public void writeSafe(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.writeSafe(
                tag,
                registries
        );
        writeCardan(
                tag
        );
    }

    private void writeCardan(
            CompoundTag tag
    ) {
        if (linkedPos != null) {
            tag.putLong(
                    TAG_LINKED_POS,
                    linkedPos.asLong()
            );
        }

        if (linkedSubLevelId != null) {
            tag.putUUID(
                    TAG_LINKED_SUB_LEVEL,
                    linkedSubLevelId
            );
        }

        tag.putBoolean(
                TAG_CONTROLLER,
                controller
        );
        tag.putBoolean(
                TAG_PENDING,
                pending
        );
        tag.putBoolean(
                TAG_COMPACT_GEOMETRY,
                compactGeometry
        );
    }

    @Override
    protected void read(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.read(
                tag,
                registries,
                clientPacket
        );

        linkedPos =
                tag.contains(TAG_LINKED_POS)
                        ? BlockPos.of(
                        tag.getLong(TAG_LINKED_POS)
                )
                        : null;

        linkedSubLevelId =
                tag.hasUUID(TAG_LINKED_SUB_LEVEL)
                        ? tag.getUUID(TAG_LINKED_SUB_LEVEL)
                        : null;

        controller =
                tag.getBoolean(TAG_CONTROLLER);
        pending =
                tag.getBoolean(TAG_PENDING);
        compactGeometry =
                tag.getBoolean(TAG_COMPACT_GEOMETRY);
    }

    @Override
    public void tick() {
        super.tick();
        tickPonderRenderOffset();
        tickPonderRenderYaw();

        if (level == null
                || level.isClientSide
                || !hasLink()) {
            return;
        }

        if (!isLinkCurrentlyValid()
                && isLinkedPositionLoaded()) {
            destroyFullLink(true);
        }
    }

    @Override
    public void sable$physicsTick(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            double timeStep
    ) {
        if (level == null
                || level.isClientSide
                || !hasLink()
                || timeStep <= 0.0D) {
            return;
        }

        UUID ownSubLevelId =
                SableSubLevelHelper.getSubLevelId(
                        level,
                        worldPosition
                );

        if (!Objects.equals(
                ownSubLevelId,
                subLevel.getUniqueId()
        )) {
            return;
        }

        CardanJointBlockEntity other =
                getLinkedJoint();

        if (other == null
                || !references(other)
                || !shouldControlPhysicsLink(
                ownSubLevelId,
                linkedSubLevelId
        )) {
            return;
        }

        ServerSubLevel otherSubLevel =
                resolveServerSubLevel(
                        linkedSubLevelId
                );

        if (linkedSubLevelId != null
                && otherSubLevel == null) {
            return;
        }

        if (otherSubLevel == subLevel) {
            return;
        }

        Vector3d ownLocal =
                localCenterOf(
                        worldPosition
                );
        Vector3d otherLocal =
                localCenterOf(
                        other.worldPosition
                );
        Vector3d ownWorld =
                toWorldPosition(
                        subLevel,
                        ownLocal
                );
        Vector3d otherWorld =
                toWorldPosition(
                        otherSubLevel,
                        otherLocal
                );
        Vector3d delta =
                otherWorld.sub(
                        ownWorld,
                        new Vector3d()
                );
        double distance =
                delta.length();

        if (distance < MIN_PHYSICS_DISTANCE) {
            return;
        }

        if (distance > PHYSICS_DISCONNECT_DISTANCE) {
            destroyFullLink(true);
            return;
        }

        double overshoot =
                distance
                        - PHYSICS_SOFT_DISTANCE;

        if (overshoot <= 0.0D) {
            return;
        }

        Vector3d direction =
                delta.div(
                        distance,
                        new Vector3d()
                );
        Vector3d ownVelocity =
                Sable.HELPER.getVelocity(
                        level,
                        ownLocal,
                        new Vector3d()
                );
        Vector3d otherVelocity =
                Sable.HELPER.getVelocity(
                        level,
                        otherLocal,
                        new Vector3d()
                );
        double separatingSpeed =
                Math.max(
                        0.0D,
                        otherVelocity.sub(
                                ownVelocity,
                                new Vector3d()
                        ).dot(direction)
                );
        double endpointProgress =
                Math.min(
                        1.0D,
                        overshoot
                                / Math.max(
                                MIN_PHYSICS_DISTANCE,
                                PHYSICS_DISCONNECT_DISTANCE
                                        - PHYSICS_SOFT_DISTANCE
                        )
                );
        double endpointMultiplier =
                1.0D
                        + (PHYSICS_ENDPOINT_PULL_MULTIPLIER - 1.0D)
                        * endpointProgress
                        * endpointProgress;
        double springMagnitude =
                PHYSICS_PULL_STIFFNESS
                        * Math.expm1(
                        overshoot
                                * PHYSICS_PULL_CURVE
                )
                        * endpointMultiplier;
        double dampingMagnitude =
                separatingSpeed
                        * (PHYSICS_PULL_DAMPING
                        + overshoot
                        * PHYSICS_PULL_DAMPING_GAIN);
        double impulseMagnitude =
                (springMagnitude + dampingMagnitude)
                        * timeStep;

        if (impulseMagnitude <= 0.0D) {
            return;
        }

        applyElasticImpulse(
                subLevel,
                handle,
                ownLocal,
                otherSubLevel,
                otherLocal,
                direction.mul(
                        impulseMagnitude,
                        new Vector3d()
                )
        );
    }

    private boolean shouldControlPhysicsLink(
            @Nullable UUID ownSubLevelId,
            @Nullable UUID otherSubLevelId
    ) {
        if (ownSubLevelId == null) {
            return false;
        }

        if (otherSubLevelId == null) {
            return true;
        }

        return !ownSubLevelId.equals(otherSubLevelId)
                && ownSubLevelId.compareTo(otherSubLevelId) < 0;
    }

    @Nullable
    private ServerSubLevel resolveServerSubLevel(
            @Nullable UUID subLevelId
    ) {
        if (subLevelId == null
                || level == null) {
            return null;
        }

        SubLevelContainer container =
                SubLevelContainer.getContainer(
                        level
                );

        if (container == null) {
            return null;
        }

        SubLevel resolved =
                container.getSubLevel(
                        subLevelId
                );

        return resolved instanceof ServerSubLevel serverSubLevel
                ? serverSubLevel
                : null;
    }

    private void applyElasticImpulse(
            ServerSubLevel ownSubLevel,
            RigidBodyHandle ownHandle,
            Vector3d ownLocal,
            @Nullable ServerSubLevel otherSubLevel,
            Vector3d otherLocal,
            Vector3d worldImpulse
    ) {
        Vector3d ownImpulse =
                ownSubLevel.logicalPose()
                        .transformNormalInverse(
                                worldImpulse,
                                new Vector3d()
                        );

        elasticForceTotal.applyImpulseAtPoint(
                ownSubLevel,
                ownLocal,
                ownImpulse
        );
        ownHandle.applyForcesAndReset(
                elasticForceTotal
        );

        if (otherSubLevel == null) {
            return;
        }

        RigidBodyHandle otherHandle =
                RigidBodyHandle.of(
                        otherSubLevel
                );
        Vector3d partnerImpulse =
                otherSubLevel.logicalPose()
                        .transformNormalInverse(
                                worldImpulse.negate(
                                        new Vector3d()
                                ),
                                new Vector3d()
                        );

        partnerElasticForceTotal.applyImpulseAtPoint(
                otherSubLevel,
                otherLocal,
                partnerImpulse
        );
        otherHandle.applyForcesAndReset(
                partnerElasticForceTotal
        );
    }

    private static Vector3d localCenterOf(
            BlockPos pos
    ) {
        return new Vector3d(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );
    }

    private static Vector3d toWorldPosition(
            @Nullable ServerSubLevel subLevel,
            Vector3d localPosition
    ) {
        return subLevel == null
                ? localPosition
                : subLevel.logicalPose()
                .transformPosition(
                        localPosition,
                        new Vector3d()
                );
    }

    @Override
    public List<BlockPos> addPropagationLocations(
            IRotate rotate,
            BlockState state,
            List<BlockPos> neighbours
    ) {
        super.addPropagationLocations(
                rotate,
                state,
                neighbours
        );

        if (linkedPos != null
                && !neighbours.contains(linkedPos)) {
            neighbours.add(linkedPos);
        }

        return neighbours;
    }

    @Override
    public float propagateRotationTo(
            KineticBlockEntity target,
            BlockState stateFrom,
            BlockState stateTo,
            BlockPos diff,
            boolean connectedByAxis,
            boolean connectedByGears
    ) {
        if (!(target instanceof CardanJointBlockEntity other)
                || !references(other)
                || !isLinkCurrentlyValid()) {
            return 0.0F;
        }

        return getCardanRotationModifier(
                stateFrom,
                stateTo
        );
    }

    private static float getCardanRotationModifier(
            BlockState stateFrom,
            BlockState stateTo
    ) {
        Direction fromFacing =
                stateFrom.getValue(
                        CardanJointBlock.FACING
                );

        Direction toFacing =
                stateTo.getValue(
                        CardanJointBlock.FACING
                );

        int fromSign =
                fromFacing.getAxisDirection()
                        == Direction.AxisDirection.POSITIVE
                        ? 1
                        : -1;

        int toSign =
                toFacing.getAxisDirection()
                        == Direction.AxisDirection.POSITIVE
                        ? 1
                        : -1;

        return -fromSign * toSign;
    }

    @Override
    public boolean isCustomConnection(
            KineticBlockEntity other,
            BlockState state,
            BlockState otherState
    ) {
        return other instanceof CardanJointBlockEntity joint
                && references(joint)
                && isLinkCurrentlyValid();
    }

    public void setPending(
            boolean pending
    ) {
        this.pending =
                pending;
        setChanged();
        sendData();
    }

    public boolean isPending() {
        return pending;
    }

    public boolean hasLink() {
        return linkedPos != null;
    }

    @Nullable
    public BlockPos getLinkedPos() {
        return linkedPos;
    }

    public boolean isController() {
        return controller;
    }

    public float getBreakWarningProgress() {
        if (ponderBreakWarningProgress >= 0.0F) {
            return ponderBreakWarningProgress;
        }

        if (level == null
                || linkedPos == null) {
            return 0.0F;
        }

        double distance =
                SableSubLevelHelper.getWorldCenter(
                        level,
                        linkedPos
                )
                        .distanceTo(
                                SableSubLevelHelper.getWorldCenter(
                                        level,
                                        worldPosition
                                )
                        );
        double warningRange =
                PHYSICS_DISCONNECT_DISTANCE
                        - BREAK_WARNING_DISTANCE;
        double progress =
                (distance - BREAK_WARNING_DISTANCE)
                        / warningRange;

        return (float) Math.max(
                0.0D,
                Math.min(
                        1.0D,
                        progress
                )
        );
    }

    public void setPonderRenderOffset(
            Vec3 offset,
            int duration
    ) {
        ponderRenderOffsetEnabled =
                true;
        previousPonderRenderOffset =
                getPonderRenderOffset(1.0F);
        targetPonderRenderOffset =
                offset;
        ponderRenderOffsetTicks =
                0;
        ponderRenderOffsetDuration =
                Math.max(
                        0,
                        duration
                );

        if (ponderRenderOffsetDuration == 0) {
            previousPonderRenderOffset =
                    targetPonderRenderOffset;
        }
    }

    public boolean hasPonderRenderTransform() {
        return ponderRenderOffsetEnabled
                || ponderRenderYawEnabled;
    }

    public Vec3 getPonderRenderOffset(
            float partialTicks
    ) {
        if (ponderRenderOffsetDuration <= 0) {
            return targetPonderRenderOffset;
        }

        double progress =
                Mth.clamp(
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

    public void setPonderBreakWarningProgress(
            float progress
    ) {
        ponderBreakWarningProgress =
                Mth.clamp(
                        progress,
                        0.0F,
                        1.0F
                );
    }

    public void setPonderRenderYaw(
            float yaw,
            int duration
    ) {
        ponderRenderYawEnabled =
                true;
        previousPonderRenderYaw =
                getPonderRenderYaw(1.0F);
        targetPonderRenderYaw =
                yaw;
        ponderRenderYawTicks =
                0;
        ponderRenderYawDuration =
                Math.max(
                        0,
                        duration
                );

        if (ponderRenderYawDuration == 0) {
            previousPonderRenderYaw =
                    targetPonderRenderYaw;
        }
    }

    public float getPonderRenderYaw(
            float partialTicks
    ) {
        if (ponderRenderYawDuration <= 0) {
            return targetPonderRenderYaw;
        }

        float progress =
                Mth.clamp(
                        (ponderRenderYawTicks + partialTicks)
                                / ponderRenderYawDuration,
                        0.0F,
                        1.0F
                );

        return Mth.lerp(
                progress,
                previousPonderRenderYaw,
                targetPonderRenderYaw
        );
    }

    private void tickPonderRenderOffset() {
        if (ponderRenderOffsetDuration <= 0) {
            return;
        }

        ponderRenderOffsetTicks++;

        if (ponderRenderOffsetTicks >= ponderRenderOffsetDuration) {
            previousPonderRenderOffset =
                    targetPonderRenderOffset;
            ponderRenderOffsetDuration =
                    0;
        }
    }

    private void tickPonderRenderYaw() {
        if (ponderRenderYawDuration <= 0) {
            return;
        }

        ponderRenderYawTicks++;

        if (ponderRenderYawTicks >= ponderRenderYawDuration) {
            previousPonderRenderYaw =
                    targetPonderRenderYaw;
            ponderRenderYawDuration =
                    0;
        }
    }

    public void createMutualLink(
            CardanJointBlockEntity other
    ) {
        boolean compact =
                level != null
                        && worldDistance(
                        level,
                        getBlockPos(),
                        other.getBlockPos()
                ) <= COMPACT_SHAFT_MAX_DISTANCE
                        + INSTALLATION_POSE_TOLERANCE;

        applyLink(
                other.getBlockPos(),
                SableSubLevelHelper.getSubLevelId(
                        level,
                        other.getBlockPos()
                ),
                true,
                compact
        );

        other.applyLink(
                getBlockPos(),
                SableSubLevelHelper.getSubLevelId(
                        level,
                        getBlockPos()
                ),
                false,
                compact
        );
    }

    /**
     * Rebinds the reciprocal endpoint after Sable moves this joint between the
     * world and a sub-level, or between sub-levels. Sable already copied the
     * complete block entity NBT and transformed FACING, so this must not create
     * a new cardan or recalculate any of its installation state.
     */
    public void afterAssemblyMove(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockPos oldPos
    ) {
        if (!hasLink()) {
            return;
        }

        UUID newSubLevelId =
                SableSubLevelHelper.getSubLevelId(
                        resultingLevel,
                        worldPosition
                );

        CardanJointBlockEntity other =
                findJointDuringAssembly(
                        originLevel,
                        resultingLevel,
                        oldPos
                );

        if (other != null) {
            other.replaceMovedEndpoint(
                    oldPos,
                    worldPosition,
                    newSubLevelId
            );
        }

        refreshKinetics();
        setChanged();
        sendData();
    }

    @Nullable
    private CardanJointBlockEntity findJointDuringAssembly(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockPos oldPos
    ) {
        if (linkedPos == null) {
            return null;
        }

        BlockEntity resultEntity =
                resultingLevel.getBlockEntity(linkedPos);
        if (resultEntity instanceof CardanJointBlockEntity resultJoint
                && resultJoint.pointsToMovedEndpoint(oldPos, worldPosition)) {
            return resultJoint;
        }

        if (originLevel != resultingLevel) {
            BlockEntity originEntity =
                    originLevel.getBlockEntity(linkedPos);
            if (originEntity instanceof CardanJointBlockEntity originJoint
                    && originJoint.pointsToMovedEndpoint(oldPos, worldPosition)) {
                return originJoint;
            }
        }

        return null;
    }

    private boolean pointsToMovedEndpoint(
            BlockPos oldPos,
            BlockPos newPos
    ) {
        return linkedPos != null
                && (linkedPos.equals(oldPos) || linkedPos.equals(newPos));
    }

    private void replaceMovedEndpoint(
            BlockPos oldPos,
            BlockPos newPos,
            @Nullable UUID newSubLevelId
    ) {
        if (linkedPos == null || !linkedPos.equals(oldPos)) {
            return;
        }

        linkedPos = newPos.immutable();
        linkedSubLevelId = newSubLevelId;

        refreshKinetics();
        setChanged();
        sendData();
    }

    public void destroyLink(
            boolean dropItem
    ) {
        if (destroyingLink) {
            return;
        }

        if (!hasLink()) {
            clearLinkInternal();
            return;
        }

        destroyingLink =
                true;

        CardanJointBlockEntity other =
                getLinkedJoint();

        boolean shouldDrop =
                dropItem
                        && !pending;

        clearLinkInternal();

        if (other != null) {
            other.clearLinkInternal();

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.destroyBlock(
                        other.getBlockPos(),
                        false
                );
            }
        }

        if (shouldDrop
                && level instanceof ServerLevel serverLevel) {
            Block.popResource(
                    serverLevel,
                    worldPosition,
                    new ItemStack(
                            CreateMechanicalDrive
                                    .CARDAN_SHAFT_ITEM
                                    .get()
                    )
            );
        }

        destroyingLink =
                false;
    }

    private void destroyFullLink(
            boolean dropItem
    ) {
        if (destroyingLink) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            clearLinkInternal();
            return;
        }

        destroyingLink =
                true;

        CardanJointBlockEntity other =
                getLinkedJoint();

        if (other != null) {
            other.destroyingLink =
                    true;
        }

        boolean shouldDrop =
                dropItem
                        && !pending;

        BlockPos ownPos =
                getBlockPos();
        BlockPos otherPos =
                other == null
                        ? linkedPos
                        : other.getBlockPos();

        clearLinkInternal();

        if (other != null) {
            other.clearLinkInternal();
        }

        if (shouldDrop) {
            Block.popResource(
                    serverLevel,
                    ownPos,
                    new ItemStack(
                            CreateMechanicalDrive
                                    .CARDAN_SHAFT_ITEM
                                    .get()
                    )
            );
        }

        serverLevel.destroyBlock(
                ownPos,
                false
        );

        if (otherPos != null
                && !otherPos.equals(
                ownPos
        )) {
            serverLevel.destroyBlock(
                    otherPos,
                    false
            );
        }

        destroyingLink =
                false;

        if (other != null) {
            other.destroyingLink =
                    false;
        }
    }

    public boolean references(
            CardanJointBlockEntity other
    ) {
        if (level == null
                || other == null
                || linkedPos == null
                || other.linkedPos == null) {
            return false;
        }

        UUID ownSubLevelId =
                SableSubLevelHelper.getSubLevelId(
                        level,
                        getBlockPos()
                );

        UUID otherSubLevelId =
                SableSubLevelHelper.getSubLevelId(
                        level,
                        other.getBlockPos()
                );

        return linkedPos.equals(
                other.getBlockPos()
        )
                && Objects.equals(
                linkedSubLevelId,
                otherSubLevelId
        )
                && other.linkedPos.equals(
                getBlockPos()
        )
                && Objects.equals(
                other.linkedSubLevelId,
                ownSubLevelId
        );
    }

    public boolean isLinkCurrentlyValid() {
        if (level == null
                || linkedPos == null) {
            return false;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(
                        linkedPos
                );

        if (!(blockEntity instanceof CardanJointBlockEntity other)
                || !references(other)) {
            return false;
        }

        if (!Objects.equals(
                linkedSubLevelId,
                SableSubLevelHelper.getSubLevelId(
                        level,
                        linkedPos
                )
        )) {
            return false;
        }

        return isLinkedGeometryValid(
                level,
                getBlockPos(),
                getBlockState()
                        .getValue(CardanJointBlock.FACING),
                other.getBlockPos(),
                other.getBlockState()
                        .getValue(CardanJointBlock.FACING),
                compactGeometry
        );
    }

    private boolean isLinkedPositionLoaded() {
        return level != null
                && linkedPos != null
                && level.isLoaded(linkedPos);
    }

    @Nullable
    private CardanJointBlockEntity getLinkedJoint() {
        if (level == null
                || linkedPos == null) {
            return null;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(
                        linkedPos
                );

        return blockEntity instanceof CardanJointBlockEntity joint
                ? joint
                : null;
    }

    private void applyLink(
            BlockPos linkedPos,
            UUID linkedSubLevelId,
            boolean controller,
            boolean compactGeometry
    ) {
        this.linkedPos =
                linkedPos;
        this.linkedSubLevelId =
                linkedSubLevelId;
        this.controller =
                controller;
        this.compactGeometry =
                compactGeometry;
        pending =
                false;

        refreshKinetics();
        setChanged();
        sendData();
    }

    private void clearLinkInternal() {
        linkedPos =
                null;
        linkedSubLevelId =
                null;
        controller =
                false;
        compactGeometry =
                false;
        pending =
                false;

        refreshKinetics();
        setChanged();
        sendData();
    }

    private void refreshKinetics() {
        if (level == null
                || level.isClientSide) {
            return;
        }

        detachKinetics();
        attachKinetics();
    }


    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getLoadingDependencies() {
        return getLinkedDifferentSubLevel();
    }

    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getConnectionDependencies() {
        return getLinkedDifferentSubLevel();
    }

    @Nullable
    private Iterable<@NotNull SubLevel> getLinkedDifferentSubLevel() {
        if (linkedSubLevelId == null
                || level == null
                || Objects.equals(
                linkedSubLevelId,
                SableSubLevelHelper.getSubLevelId(
                        level,
                        getBlockPos()
                )
        )) {
            return null;
        }

        SubLevelContainer container =
                SubLevelContainer.getContainer(
                        level
                );

        if (container == null) {
            return null;
        }

        SubLevel connectedSubLevel =
                container.getSubLevel(
                        linkedSubLevelId
                );

        if (connectedSubLevel == null
                || connectedSubLevel.isRemoved()) {
            return null;
        }

        return List.of(
                connectedSubLevel
        );
    }


    public static boolean isGeometryValid(
            Level level,
            BlockPos first,
            Direction firstFacing,
            BlockPos second,
            Direction secondFacing
    ) {
        return isGeometryValid(
                level,
                first,
                firstFacing,
                second,
                secondFacing,
                MAX_LENGTH
        );
    }

    private static boolean isLinkedGeometryValid(
            Level level,
            BlockPos first,
            Direction firstFacing,
            BlockPos second,
            Direction secondFacing,
            boolean compactGeometry
    ) {
        return isGeometryValid(
                level,
                first,
                firstFacing,
                second,
                secondFacing,
                PHYSICS_DISCONNECT_DISTANCE,
                false,
                compactGeometry
        );
    }

    private static boolean isGeometryValid(
            Level level,
            BlockPos first,
            Direction firstFacing,
            BlockPos second,
            Direction secondFacing,
            double maximumDistance
    ) {
        double distance =
                worldDistance(
                        level,
                        first,
                        second
                );

        return isGeometryValid(
                level,
                first,
                firstFacing,
                second,
                secondFacing,
                maximumDistance,
                true,
                distance <= COMPACT_SHAFT_MAX_DISTANCE
                        + INSTALLATION_POSE_TOLERANCE
        );
    }

    private static boolean isGeometryValid(
            Level level,
            BlockPos first,
            Direction firstFacing,
            BlockPos second,
            Direction secondFacing,
            double maximumDistance,
            boolean enforceInstallationMinimum,
            boolean compactGeometry
    ) {
        Vec3 firstCenter =
                SableSubLevelHelper.getWorldCenter(
                        level,
                        first
                );
        Vec3 secondCenter =
                SableSubLevelHelper.getWorldCenter(
                        level,
                        second
                );
        Vec3 delta =
                secondCenter.subtract(
                        firstCenter
                );
        double distance =
                delta.length();

        if ((enforceInstallationMinimum
                && distance + GEOMETRY_EPSILON < MIN_INSTALLATION_DISTANCE)
                || distance - GEOMETRY_EPSILON > maximumDistance) {
            return false;
        }

        if (distance < MIN_PHYSICS_DISTANCE) {
            return false;
        }

        Vec3 direction =
                delta.normalize();
        double minimumBendDot =
                compactGeometry
                        ? COMPACT_BEND_ANGLE_COS
                        : MAX_BEND_ANGLE_COS;

        return isBendValid(
                level,
                first,
                firstFacing,
                direction,
                minimumBendDot
        )
                && isBendValid(
                level,
                second,
                secondFacing,
                direction.scale(-1.0D),
                minimumBendDot
        );
    }

    public static boolean isBendValid(
            Level level,
            BlockPos pos,
            Direction facing,
            Vec3 linkDirection
    ) {
        return isBendValid(
                level,
                pos,
                facing,
                linkDirection,
                MAX_BEND_ANGLE_COS
        );
    }

    private static boolean isBendValid(
            Level level,
            BlockPos pos,
            Direction facing,
            Vec3 linkDirection,
            double minimumBendDot
    ) {
        if (linkDirection.lengthSqr() < 1.0E-8D) {
            return false;
        }

        Vec3 facingNormal =
                SableSubLevelHelper.getWorldNormal(
                        level,
                        pos,
                        Vec3.atLowerCornerOf(
                                facing.getNormal()
                        )
                );

        if (facingNormal.lengthSqr() < 1.0E-8D) {
            return false;
        }

        return facingNormal.normalize()
                .dot(
                        linkDirection.normalize()
                ) + GEOMETRY_EPSILON >= minimumBendDot;
    }

    public static boolean isDistanceValid(
            double distance
    ) {
        return distance + GEOMETRY_EPSILON >= MIN_INSTALLATION_DISTANCE
                && distance - GEOMETRY_EPSILON <= MAX_LENGTH;
    }

    private static double worldDistance(
            Level level,
            BlockPos first,
            BlockPos second
    ) {
        return SableSubLevelHelper.getWorldCenter(level, first)
                .distanceTo(
                        SableSubLevelHelper.getWorldCenter(level, second)
                );
    }
}
