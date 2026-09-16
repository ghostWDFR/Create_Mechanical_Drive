package dev.createmechanicaldrive.content.chain_linkage;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ChainGearBlockEntity
        extends KineticBlockEntity
        implements BlockEntitySubLevelActor {

    private static final String LOOP_TAG = "ChainLoop";

    private static final String INDEX_TAG = "ChainIndex";

    private static final String CHAINS_TO_REFUND_TAG = "ChainsToRefund";

    private static final String INITIAL_CENTER_DISTANCE_TAG =
            "ChainInitialCenterDistance";

    private static final String INITIAL_LOOP_LENGTH_TAG =
            "ChainInitialLoopLength";

    private static final String INITIAL_PHYSICS_DISTANCE_TAG =
            "ChainInitialPhysicsDistance";

    private static final String CONNECTED_SUB_LEVEL_TAG =
            "ChainConnectedSubLevel";

    private static final float SPEED_EPSILON =
            0.001F;

    private static final double PHYSICS_FREE_STRETCH = 1.0D;
    private static final double PHYSICS_PULL_STIFFNESS = 12.0D;
    private static final double PHYSICS_PULL_CURVE = 4.0D;
    private static final double PHYSICS_PULL_DAMPING = 7.0D;
    private static final double PHYSICS_PULL_DAMPING_GAIN = 3.0D;
    private static final double PHYSICS_ENDPOINT_PULL_MULTIPLIER = 4.0D;
    private static final double PHYSICS_ENDPOINT_RAMP_DISTANCE = 1.5D;
    private static final double MIN_PHYSICS_DISTANCE = 1.0E-4D;

    private List<BlockPos> chainLoop = List.of();
    private int chainIndex = -1;
    private int chainsToRefund;
    private boolean assemblyMoving;
    private double initialCenterDistance;
    private double initialLoopLength;
    private double initialPhysicsDistance;

    @Nullable
    private UUID connectedSubLevelId;

    private boolean ponderFlexibleChainRender;
    private Vec3 previousPonderRenderOffset = Vec3.ZERO;
    private Vec3 targetPonderRenderOffset = Vec3.ZERO;
    private int ponderRenderOffsetTicks;
    private int ponderRenderOffsetDuration;
    private final ForceTotal elasticForceTotal =
            new ForceTotal();
    private final ForceTotal partnerElasticForceTotal =
            new ForceTotal();

    public ChainGearBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(
                type,
                pos,
                state
        );
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

        writeChain(
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

        writeChain(
                tag
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

        chainIndex =
                tag.getInt(INDEX_TAG);

        chainsToRefund =
                tag.getInt(CHAINS_TO_REFUND_TAG);

        initialCenterDistance =
                tag.getDouble(INITIAL_CENTER_DISTANCE_TAG);

        initialLoopLength =
                tag.getDouble(INITIAL_LOOP_LENGTH_TAG);

        initialPhysicsDistance =
                tag.contains(INITIAL_PHYSICS_DISTANCE_TAG)
                        ? tag.getDouble(INITIAL_PHYSICS_DISTANCE_TAG)
                        : initialCenterDistance;

        connectedSubLevelId =
                tag.hasUUID(CONNECTED_SUB_LEVEL_TAG)
                        ? tag.getUUID(CONNECTED_SUB_LEVEL_TAG)
                        : null;

        ListTag list =
                tag.getList(
                        LOOP_TAG,
                        Tag.TAG_COMPOUND
                );

        List<BlockPos> positions =
                new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            CompoundTag posTag =
                    list.getCompound(i);

            positions.add(
                    new BlockPos(
                            posTag.getInt("X"),
                            posTag.getInt("Y"),
                            posTag.getInt("Z")
                    )
            );
        }

        chainLoop =
                List.copyOf(positions);
    }

    private void writeChain(
            CompoundTag tag
    ) {
        tag.putInt(
                INDEX_TAG,
                chainIndex
        );

        tag.putInt(
                CHAINS_TO_REFUND_TAG,
                chainsToRefund
        );

        tag.putDouble(
                INITIAL_CENTER_DISTANCE_TAG,
                initialCenterDistance
        );

        tag.putDouble(
                INITIAL_LOOP_LENGTH_TAG,
                initialLoopLength
        );

        tag.putDouble(
                INITIAL_PHYSICS_DISTANCE_TAG,
                initialPhysicsDistance
        );

        ListTag list =
                new ListTag();

        for (BlockPos pos : chainLoop) {
            CompoundTag posTag =
                    new CompoundTag();

            posTag.putInt(
                    "X",
                    pos.getX()
            );

            posTag.putInt(
                    "Y",
                    pos.getY()
            );

            posTag.putInt(
                    "Z",
                    pos.getZ()
            );

            list.add(
                    posTag
            );
        }

        tag.put(
                LOOP_TAG,
                list
        );

        if (connectedSubLevelId != null) {
            tag.putUUID(
                    CONNECTED_SUB_LEVEL_TAG,
                    connectedSubLevelId
            );
        } else {
            tag.remove(
                    CONNECTED_SUB_LEVEL_TAG
            );
        }
    }

    public boolean hasChainLoop() {
        return chainIndex >= 0
                && chainIndex < chainLoop.size()
                && chainLoop.size() >= ChainLinkageValidator.MIN_GEARS;
    }

    public boolean isChainController() {
        return hasChainLoop()
                && chainIndex == 0;
    }

    public List<BlockPos> getChainLoop() {
        return chainLoop;
    }

    public int getChainIndex() {
        return chainIndex;
    }

    public int getChainRefundAmount() {
        if (!hasChainLoop()) {
            return 0;
        }

        return findRefundAmount(
                List.copyOf(chainLoop)
        );
    }

    public int consumeChainRefundAmount() {
        if (!hasChainLoop()) {
            return 0;
        }

        List<BlockPos> loop =
                List.copyOf(chainLoop);

        int refund =
                findRefundAmount(loop);

        if (refund <= 0) {
            return 0;
        }

        for (BlockPos pos : loop) {
            BlockEntity blockEntity =
                    level != null
                            ? level.getBlockEntity(pos)
                            : null;

            if (blockEntity instanceof ChainGearBlockEntity gear) {
                gear.chainsToRefund =
                        0;

                gear.setChanged();
            }
        }

        chainsToRefund =
                0;

        setChanged();

        return refund;
    }

    public void setChainLoop(
            List<BlockPos> loop,
            int index,
            int chainsToRefund
    ) {
        setChainLoop(
                loop,
                index,
                chainsToRefund,
                0.0D,
                0.0D,
                null
        );
    }

    public void setChainLoop(
            List<BlockPos> loop,
            int index,
            int chainsToRefund,
            double initialCenterDistance,
            double initialLoopLength,
            @Nullable UUID connectedSubLevelId
    ) {
        chainLoop =
                List.copyOf(loop);

        chainIndex =
                index;

        this.chainsToRefund =
                chainsToRefund;

        this.initialCenterDistance =
                initialCenterDistance;

        this.initialLoopLength =
                initialLoopLength;

        this.initialPhysicsDistance =
                initialCenterDistance > 0.0D
                        && level != null
                        && loop.size() == 2
                        ? Math.max(
                        initialCenterDistance,
                        ChainLinkageValidator.getWorldCenterDistance(
                                level,
                                loop
                        )
                )
                        : 0.0D;

        this.connectedSubLevelId =
                connectedSubLevelId;

        setChanged();
        sendData();
    }

    public void clearChainLoop() {
        chainLoop =
                List.of();

        chainIndex =
                -1;

        chainsToRefund =
                0;

        initialCenterDistance =
                0.0D;

        initialLoopLength =
                0.0D;

        initialPhysicsDistance =
                0.0D;

        connectedSubLevelId =
                null;

        setChanged();
        sendData();
    }

    void prepareAssemblyMove() {
        assemblyMoving =
                true;
    }

    boolean isAssemblyMoving() {
        return assemblyMoving;
    }

    /**
     * Rebinds every copied loop record after Sable moves this gear between the
     * world and a sub-level, or between two sub-levels. The refund owner and
     * chain geometry are preserved; only endpoint addresses and derived
     * cross-level state are updated.
     */
    public void afterAssemblyMove(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockPos oldPos
    ) {
        assemblyMoving =
                false;

        if (!hasChainLoop()) {
            return;
        }

        int movedIndex =
                chainIndex;

        List<BlockPos> originalLoop =
                List.copyOf(chainLoop);

        if (movedIndex < 0
                || movedIndex >= originalLoop.size()) {
            return;
        }

        BlockPos newPos =
                worldPosition.immutable();

        List<BlockPos> reboundLoop =
                new ArrayList<>(originalLoop);

        reboundLoop.set(
                movedIndex,
                newPos
        );

        chainLoop =
                List.copyOf(reboundLoop);

        List<ChainGearBlockEntity> touched =
                new ArrayList<>();

        touched.add(this);

        for (int peerIndex = 0;
             peerIndex < reboundLoop.size();
             peerIndex++) {

            if (peerIndex == movedIndex) {
                continue;
            }

            ChainGearBlockEntity peer =
                    findGearDuringAssembly(
                            originLevel,
                            resultingLevel,
                            reboundLoop.get(peerIndex),
                            peerIndex,
                            movedIndex,
                            originalLoop.size(),
                            oldPos,
                            newPos
                    );

            if (peer == null) {
                continue;
            }

            peer.replaceMovedEndpoint(
                    movedIndex,
                    oldPos,
                    newPos
            );

            if (!touched.contains(peer)) {
                touched.add(peer);
            }
        }

        for (ChainGearBlockEntity gear : touched) {
            gear.finishAssemblyMove();
        }
    }

    @Nullable
    private static ChainGearBlockEntity findGearDuringAssembly(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockPos peerPos,
            int peerIndex,
            int movedIndex,
            int expectedLoopSize,
            BlockPos oldPos,
            BlockPos newPos
    ) {
        ChainGearBlockEntity resultingGear =
                matchingAssemblyGear(
                        resultingLevel.getBlockEntity(peerPos),
                        peerIndex,
                        movedIndex,
                        expectedLoopSize,
                        oldPos,
                        newPos
                );

        if (resultingGear != null) {
            return resultingGear;
        }

        if (originLevel != resultingLevel) {
            return matchingAssemblyGear(
                    originLevel.getBlockEntity(peerPos),
                    peerIndex,
                    movedIndex,
                    expectedLoopSize,
                    oldPos,
                    newPos
            );
        }

        return null;
    }

    @Nullable
    private static ChainGearBlockEntity matchingAssemblyGear(
            BlockEntity blockEntity,
            int peerIndex,
            int movedIndex,
            int expectedLoopSize,
            BlockPos oldPos,
            BlockPos newPos
    ) {
        if (!(blockEntity instanceof ChainGearBlockEntity gear)
                || !gear.hasChainLoop()
                || gear.chainIndex != peerIndex
                || gear.chainLoop.size() != expectedLoopSize
                || movedIndex < 0
                || movedIndex >= gear.chainLoop.size()) {
            return null;
        }

        BlockPos recordedMovedPos =
                gear.chainLoop.get(movedIndex);

        return recordedMovedPos.equals(oldPos)
                || recordedMovedPos.equals(newPos)
                ? gear
                : null;
    }

    private void replaceMovedEndpoint(
            int movedIndex,
            BlockPos oldPos,
            BlockPos newPos
    ) {
        if (!hasChainLoop()
                || movedIndex < 0
                || movedIndex >= chainLoop.size()) {
            return;
        }

        BlockPos recordedMovedPos =
                chainLoop.get(movedIndex);

        if (!recordedMovedPos.equals(oldPos)
                && !recordedMovedPos.equals(newPos)) {
            return;
        }

        if (!recordedMovedPos.equals(newPos)) {
            List<BlockPos> reboundLoop =
                    new ArrayList<>(chainLoop);

            reboundLoop.set(
                    movedIndex,
                    newPos.immutable()
            );

            chainLoop =
                    List.copyOf(reboundLoop);
        }
    }

    private void finishAssemblyMove() {
        refreshAssemblyDerivedState();
        refreshKineticsAfterAssemblyMove();
        setChanged();
        sendData();
    }

    private void refreshAssemblyDerivedState() {
        connectedSubLevelId =
                null;

        if (level == null
                || !hasChainLoop()
                || chainLoop.size() != 2) {
            return;
        }

        int otherIndex =
                chainIndex == 0
                        ? 1
                        : 0;

        if (otherIndex < 0
                || otherIndex >= chainLoop.size()) {
            return;
        }

        UUID ownSubLevelId =
                SableSubLevelHelper.getSubLevelId(
                        level,
                        worldPosition
                );

        UUID otherSubLevelId =
                SableSubLevelHelper.getSubLevelId(
                        level,
                        chainLoop.get(otherIndex)
                );

        if (Objects.equals(
                ownSubLevelId,
                otherSubLevelId
        )) {
            initialCenterDistance =
                    0.0D;

            initialLoopLength =
                    0.0D;

            initialPhysicsDistance =
                    0.0D;

            return;
        }

        connectedSubLevelId =
                otherSubLevelId;

        if (initialCenterDistance > 0.0D
                && initialLoopLength > 0.0D) {
            if (initialPhysicsDistance <= 0.0D) {
                initialPhysicsDistance =
                        initialCenterDistance;
            }

            return;
        }

        double centerDistance =
                ChainLinkageValidator.getFlexibleCenterDistance(
                        level,
                        chainLoop,
                        getBlockState().getValue(ChainGearBlock.AXIS)
                );

        if (!Double.isFinite(centerDistance)
                || centerDistance <= 0.0D) {
            return;
        }

        initialCenterDistance =
                centerDistance;

        initialLoopLength =
                ChainLinkageValidator.getTwoGearLoopLength(
                        centerDistance
                );

        initialPhysicsDistance =
                Math.max(
                        centerDistance,
                        ChainLinkageValidator.getWorldCenterDistance(
                                level,
                                chainLoop
                        )
                );
    }

    private void refreshKineticsAfterAssemblyMove() {
        if (level == null
                || level.isClientSide
                || isRemoved()) {
            return;
        }

        detachKinetics();
        attachKinetics();
    }
    public void destroyChain(
            boolean drop
    ) {
        if (!hasChainLoop()
                || level == null) {
            return;
        }

        List<BlockPos> loop =
                List.copyOf(chainLoop);

        int refund =
                findRefundAmount(loop);

        List<ChainGearBlockEntity> gears =
                new ArrayList<>();

        gears.add(this);

        for (BlockPos pos : loop) {
            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            if (blockEntity instanceof ChainGearBlockEntity gear
                    && !gears.contains(gear)) {
                gears.add(gear);
            }
        }

        for (ChainGearBlockEntity gear : gears) {
            gear.detachKinetics();
        }

        for (ChainGearBlockEntity gear : gears) {
            gear.clearChainLoop();
        }

        for (ChainGearBlockEntity gear : gears) {
            Level gearLevel =
                    gear.getLevel();

            if (gearLevel == null
                    || gear.isRemoved()) {
                continue;
            }

            if (gearLevel.getBlockState(
                    gear.getBlockPos()
            ).is(
                    gear.getBlockState()
                            .getBlock()
            )) {
                gear.attachKinetics();
            }
        }

        if (drop && refund > 0) {
            Block.popResource(
                    level,
                    worldPosition,
                    new ItemStack(
                            CreateMechanicalDrive.CHAIN_LINKAGE_ITEM.get(),
                            refund
                    )
            );
        }
    }

    private int findRefundAmount(
            List<BlockPos> loop
    ) {
        if (loop.isEmpty()
                || level == null) {
            return chainsToRefund;
        }

        BlockEntity controller =
                level.getBlockEntity(
                        loop.getFirst()
                );

        if (controller instanceof ChainGearBlockEntity gear) {
            if (gear.hasFlexibleTwoGearChain()) {
                return gear.chainsToRefund;
            }

            return Math.min(
                    gear.chainsToRefund,
                    ChainLinkageValidator.getChainsRequired(
                            loop
                    )
            );
        }

        if (hasFlexibleTwoGearChain()) {
            return chainsToRefund;
        }

        return Math.min(
                chainsToRefund,
                ChainLinkageValidator.getChainsRequired(
                        loop
                )
        );
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
        if (!(target instanceof ChainGearBlockEntity chainGear)) {
            return 0.0F;
        }

        int straightModifier =
                getStraightChainModifierTo(
                        chainGear
                );

        if (straightModifier != 0) {
            return straightModifier;
        }

        if (!isLinkedNeighbour(chainGear)) {
            return 0.0F;
        }

        return 1.0F;
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

        if (!hasChainLoop()) {
            addStraightChainPropagationLocationsForAuxiliary(
                    neighbours
            );

            return neighbours;
        }

        addIfMissing(
                neighbours,
                previousChainPos()
        );

        addIfMissing(
                neighbours,
                nextChainPos()
        );

        addStraightAuxiliaryPropagationLocations(
                neighbours
        );

        return neighbours;
    }

    @Override
    public boolean isCustomConnection(
            KineticBlockEntity other,
            BlockState state,
            BlockState otherState
    ) {
        return other instanceof ChainGearBlockEntity chainGear
                && (isLinkedNeighbour(chainGear)
                || getStraightChainModifierTo(chainGear) != 0);
    }

    @Override
    public void tick() {
        super.tick();
        tickPonderRenderOffset();

        if (level == null
                || level.isClientSide) {
            return;
        }

        repairLegacyAssemblyAddress();

        if (!hasChainLoop()) {
            return;
        }

        if (hasFlexibleTwoGearChain()
                && !isFlexibleTwoGearChainCurrentlyValid()) {

            destroyChain(true);
            return;
        }

        if (isChainController()
                && hasImpossibleStraightAuxiliaryRotation()) {

            destroyChain(true);
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
                || !hasFlexibleTwoGearChain()
                || initialPhysicsDistance <= 0.0D
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

        ChainGearBlockEntity other =
                getOtherEndpointGear();

        if (other == null
                || !referencesFlexibleChain(other)
                || !shouldControlPhysicsLink(
                ownSubLevelId,
                connectedSubLevelId
        )) {
            return;
        }

        ServerSubLevel otherSubLevel =
                resolveServerSubLevel(
                        connectedSubLevelId
                );

        if (connectedSubLevelId != null
                && otherSubLevel == null) {
            return;
        }

        if (otherSubLevel == subLevel) {
            return;
        }

        Vector3d ownLocal = localCenterOf(worldPosition);
        Vector3d otherLocal = localCenterOf(other.worldPosition);
        Vector3d ownWorld = toWorldPosition(subLevel, ownLocal);
        Vector3d otherWorld = toWorldPosition(otherSubLevel, otherLocal);
        Vector3d delta = otherWorld.sub(ownWorld, new Vector3d());
        double distance = delta.length();

        if (distance < MIN_PHYSICS_DISTANCE) {
            return;
        }

        double overshoot =
                distance
                        - initialPhysicsDistance
                        - PHYSICS_FREE_STRETCH;

        if (overshoot <= 0.0D) {
            return;
        }

        Vector3d direction = delta.div(distance, new Vector3d());
        Vector3d ownVelocity = Sable.HELPER.getVelocity(
                level,
                ownLocal,
                new Vector3d()
        );
        Vector3d otherVelocity = Sable.HELPER.getVelocity(
                level,
                otherLocal,
                new Vector3d()
        );
        double separatingSpeed = Math.max(
                0.0D,
                otherVelocity.sub(ownVelocity, new Vector3d()).dot(direction)
        );
        double endpointProgress = Math.min(
                1.0D,
                overshoot / PHYSICS_ENDPOINT_RAMP_DISTANCE
        );
        double endpointMultiplier =
                1.0D
                        + (PHYSICS_ENDPOINT_PULL_MULTIPLIER - 1.0D)
                        * endpointProgress
                        * endpointProgress;
        double springMagnitude =
                PHYSICS_PULL_STIFFNESS
                        * Math.expm1(overshoot * PHYSICS_PULL_CURVE)
                        * endpointMultiplier;
        double dampingMagnitude =
                separatingSpeed
                        * (PHYSICS_PULL_DAMPING
                        + overshoot * PHYSICS_PULL_DAMPING_GAIN);
        double impulseMagnitude =
                (springMagnitude + dampingMagnitude) * timeStep;

        if (impulseMagnitude <= 0.0D) {
            return;
        }

        applyElasticImpulse(
                subLevel,
                handle,
                ownLocal,
                otherSubLevel,
                otherLocal,
                direction.mul(impulseMagnitude, new Vector3d())
        );
    }

    private boolean referencesFlexibleChain(
            ChainGearBlockEntity other
    ) {
        return other.hasFlexibleTwoGearChain()
                && chainLoop.equals(other.chainLoop)
                && chainIndex != other.chainIndex
                && Objects.equals(getFlexibleOtherPos(), other.worldPosition)
                && Objects.equals(other.getFlexibleOtherPos(), worldPosition);
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

        SubLevelContainer container = SubLevelContainer.getContainer(level);

        if (container == null) {
            return null;
        }

        SubLevel resolved = container.getSubLevel(subLevelId);

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
        Vector3d ownImpulse = ownSubLevel.logicalPose()
                .transformNormalInverse(worldImpulse, new Vector3d());

        elasticForceTotal.applyImpulseAtPoint(
                ownSubLevel,
                ownLocal,
                ownImpulse
        );
        ownHandle.applyForcesAndReset(elasticForceTotal);

        if (otherSubLevel == null) {
            return;
        }

        RigidBodyHandle otherHandle = RigidBodyHandle.of(otherSubLevel);
        Vector3d partnerImpulse = otherSubLevel.logicalPose()
                .transformNormalInverse(
                        worldImpulse.negate(new Vector3d()),
                        new Vector3d()
                );

        partnerElasticForceTotal.applyImpulseAtPoint(
                otherSubLevel,
                otherLocal,
                partnerImpulse
        );
        otherHandle.applyForcesAndReset(partnerElasticForceTotal);
    }

    private static Vector3d localCenterOf(BlockPos pos) {
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
                .transformPosition(localPosition, new Vector3d());
    }
    private void repairLegacyAssemblyAddress() {
        if (!(level instanceof ServerLevel serverLevel)
                || !hasChainLoop()
                || chainIndex < 0
                || chainIndex >= chainLoop.size()) {
            return;
        }

        BlockPos recordedOwnPos =
                chainLoop.get(chainIndex);

        if (recordedOwnPos.equals(worldPosition)) {
            return;
        }

        chainsToRefund =
                0;

        afterAssemblyMove(
                serverLevel,
                serverLevel,
                recordedOwnPos
        );
    }

    @Nullable
    private BlockPos getFlexibleOtherPos() {
        if (!hasFlexibleTwoGearChain()) {
            return null;
        }

        int otherIndex =
                chainIndex == 0
                        ? 1
                        : 0;

        if (otherIndex < 0
                || otherIndex >= chainLoop.size()) {
            return null;
        }

        return chainLoop.get(otherIndex);
    }

    public boolean hasFlexibleTwoGearChain() {
        return hasChainLoop()
                && chainLoop.size() == 2
                && initialCenterDistance > 0.0D
                && initialLoopLength > 0.0D;
    }

    public boolean needsWorldSpaceChainRender() {
        if (!hasFlexibleTwoGearChain()
                || level == null) {
            return false;
        }

        BlockPos otherPos =
                getFlexibleOtherPos();

        if (otherPos == null) {
            return false;
        }

        return !java.util.Objects.equals(
                dev.createmechanicaldrive.compat.SableSubLevelHelper
                        .getSubLevelId(
                                level,
                                getBlockPos()
                        ),
                dev.createmechanicaldrive.compat.SableSubLevelHelper
                        .getSubLevelId(
                                level,
                                otherPos
                        )
        );
    }

    public boolean isFlexibleTwoGearChainCurrentlyValid() {
        if (level == null
                || !hasFlexibleTwoGearChain()) {
            return false;
        }

        return ChainLinkageValidator.isFlexibleTwoGearLinkValid(
                level,
                chainLoop,
                getBlockState().getValue(ChainGearBlock.AXIS),
                initialCenterDistance,
                initialLoopLength
        );
    }

    public double getInitialCenterDistance() {
        return initialCenterDistance;
    }

    public double getInitialLoopLength() {
        return initialLoopLength;
    }

    public void setPonderFlexibleChainRender(
            boolean ponderFlexibleChainRender
    ) {
        this.ponderFlexibleChainRender =
                ponderFlexibleChainRender;
    }

    public boolean hasPonderFlexibleChainRender() {
        return ponderFlexibleChainRender;
    }

    public void setPonderRenderOffset(
            Vec3 offset,
            int duration
    ) {
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

    private int getStraightChainModifierTo(
            ChainGearBlockEntity other
    ) {
        int modifier =
                getStraightAuxiliaryModifierTo(
                        other
                );

        if (modifier != 0) {
            return modifier;
        }

        return other.getStraightAuxiliaryModifierTo(
                this
        );
    }

    private int getStraightAuxiliaryModifierTo(
            ChainGearBlockEntity other
    ) {
        if (other == this
                || other.hasChainLoop()) {
            return 0;
        }

        StraightChainGeometry geometry =
                getStraightChainGeometry();

        if (geometry == null
                || !isSameChainPlaneSubLevel(
                other.getBlockPos()
        )) {
            return 0;
        }

        return getStraightAuxiliaryModifier(
                other.getBlockPos(),
                other.getBlockState(),
                geometry
        );
    }

    private void addStraightAuxiliaryPropagationLocations(
            List<BlockPos> neighbours
    ) {
        StraightChainGeometry geometry =
                getStraightChainGeometry();

        if (geometry == null
                || level == null) {
            return;
        }

        for (int step = 1;
             step < geometry.steps();
             step++) {

            BlockPos pos =
                    geometry.start()
                            .offset(
                                    geometry.step()
                                            .multiply(
                                                    step
                                            )
                            );

            BlockEntity blockEntity =
                    level.getBlockEntity(
                            pos
                    );

            if (!(blockEntity instanceof ChainGearBlockEntity gear)
                    || gear.hasChainLoop()) {
                continue;
            }

            if (getStraightAuxiliaryModifier(
                    pos,
                    gear.getBlockState(),
                    geometry
            ) != 0) {
                addIfMissing(
                        neighbours,
                        pos
                );
            }
        }
    }

    private void addStraightChainPropagationLocationsForAuxiliary(
            List<BlockPos> neighbours
    ) {
        if (level == null) {
            return;
        }

        Direction.Axis axis =
                getBlockState().getValue(
                        ChainGearBlock.AXIS
                );

        BlockPos pos =
                getBlockPos();

        for (int first = -ChainLinkageValidator.MAX_BOUNDS;
             first <= ChainLinkageValidator.MAX_BOUNDS;
             first++) {

            for (int second = -ChainLinkageValidator.MAX_BOUNDS;
                 second <= ChainLinkageValidator.MAX_BOUNDS;
                 second++) {

                BlockPos candidate =
                        offsetInPlane(
                                pos,
                                axis,
                                first,
                                second
                        );

                BlockEntity blockEntity =
                        level.getBlockEntity(
                                candidate
                        );

                if (!(blockEntity instanceof ChainGearBlockEntity gear)
                        || !gear.hasStraightTwoGearChain()) {
                    continue;
                }

                if (gear.getStraightAuxiliaryModifierTo(
                        this
                ) != 0) {
                    addIfMissing(
                            neighbours,
                            candidate
                    );
                }
            }
        }
    }

    private boolean hasImpossibleStraightAuxiliaryRotation() {
        StraightChainGeometry geometry =
                getStraightChainGeometry();

        if (geometry == null
                || level == null) {
            return false;
        }

        ChainGearBlockEntity otherEndpoint =
                getOtherEndpointGear();

        float chainSpeed =
                getTheoreticalSpeed();

        if (otherEndpoint != null) {
            float otherSpeed =
                    otherEndpoint.getTheoreticalSpeed();

            if (hasSpeed(chainSpeed)
                    && hasSpeed(otherSpeed)
                    && !speedsMatch(
                    otherSpeed,
                    chainSpeed
            )) {
                return true;
            }

            if (!hasSpeed(chainSpeed)) {
                chainSpeed =
                        otherSpeed;
            }
        }

        if (!hasSpeed(chainSpeed)) {
            return false;
        }

        for (ChainAuxiliaryGear auxiliary : findStraightAuxiliaryGears(
                geometry
        )) {
            float auxiliarySpeed =
                    auxiliary.gear()
                            .getTheoreticalSpeed();

            if (hasSpeed(auxiliarySpeed)
                    && !speedsMatch(
                    auxiliarySpeed,
                    chainSpeed * auxiliary.modifier()
            )) {
                return true;
            }
        }

        return false;
    }

    private List<ChainAuxiliaryGear> findStraightAuxiliaryGears(
            StraightChainGeometry geometry
    ) {
        if (level == null) {
            return List.of();
        }

        List<ChainAuxiliaryGear> gears =
                new ArrayList<>();

        for (int step = 1;
             step < geometry.steps();
             step++) {

            BlockPos pos =
                    geometry.start()
                            .offset(
                                    geometry.step()
                                            .multiply(
                                                    step
                                            )
                            );

            BlockEntity blockEntity =
                    level.getBlockEntity(
                            pos
                    );

            if (!(blockEntity instanceof ChainGearBlockEntity gear)
                    || gear.hasChainLoop()) {
                continue;
            }

            int modifier =
                    getStraightAuxiliaryModifier(
                            pos,
                            gear.getBlockState(),
                            geometry
                    );

            if (modifier != 0) {
                gears.add(
                        new ChainAuxiliaryGear(
                                gear,
                                modifier
                        )
                );
            }
        }

        return gears;
    }

    private int getStraightAuxiliaryModifier(
            BlockPos pos,
            BlockState state,
            StraightChainGeometry geometry
    ) {
        if (!state.is(
                CreateMechanicalDrive.CHAIN_GEAR.get()
        ) || state.getValue(
                ChainGearBlock.AXIS
        ) != geometry.axis()
                || !isSameChainPlaneSubLevel(pos)) {

            return 0;
        }

        Integer step =
                getStepOnStraightChain(
                        pos,
                        geometry
                );

        if (step == null
                || step <= 0
                || step >= geometry.steps()) {
            return 0;
        }

        return 1;
    }

    @Nullable
    private Integer getStepOnStraightChain(
            BlockPos pos,
            StraightChainGeometry geometry
    ) {
        BlockPos delta =
                pos.subtract(
                        geometry.start()
                );

        if (delta.get(
                geometry.axis()
        ) != 0) {
            return null;
        }

        Integer step =
                null;

        int[] deltaValues = {
                delta.getX(),
                delta.getY(),
                delta.getZ()
        };

        int[] stepValues = {
                geometry.step().getX(),
                geometry.step().getY(),
                geometry.step().getZ()
        };

        for (int i = 0; i < deltaValues.length; i++) {
            int stepValue =
                    stepValues[i];

            int deltaValue =
                    deltaValues[i];

            if (stepValue == 0) {
                if (deltaValue != 0) {
                    return null;
                }

                continue;
            }

            if (deltaValue % stepValue != 0) {
                return null;
            }

            int coordinateStep =
                    deltaValue / stepValue;

            if (step == null) {
                step =
                        coordinateStep;
            } else if (step != coordinateStep) {
                return null;
            }
        }

        return step;
    }

    private boolean hasStraightTwoGearChain() {
        return getStraightChainGeometry() != null;
    }

    @Nullable
    private StraightChainGeometry getStraightChainGeometry() {
        if (!hasChainLoop()
                || chainLoop.size() != 2
                || level == null
                || !sameSubLevel(
                chainLoop.getFirst(),
                chainLoop.getLast()
        )) {
            return null;
        }

        Direction.Axis axis =
                getBlockState().getValue(
                        ChainGearBlock.AXIS
                );

        BlockPos start =
                chainLoop.getFirst();

        BlockPos end =
                chainLoop.getLast();

        BlockPos delta =
                end.subtract(
                        start
                );

        if (delta.get(axis) != 0) {
            return null;
        }

        int steps =
                gcd(
                        Math.abs(delta.getX()),
                        gcd(
                                Math.abs(delta.getY()),
                                Math.abs(delta.getZ())
                        )
                );

        if (steps <= 0
                || steps > ChainLinkageValidator.MAX_BOUNDS) {
            return null;
        }

        return new StraightChainGeometry(
                start,
                new BlockPos(
                        delta.getX() / steps,
                        delta.getY() / steps,
                        delta.getZ() / steps
                ),
                steps,
                axis
        );
    }

    private boolean isSameChainPlaneSubLevel(
            BlockPos pos
    ) {
        if (level == null
                || chainLoop.isEmpty()) {
            return false;
        }

        Direction.Axis axis =
                getBlockState().getValue(
                        ChainGearBlock.AXIS
                );

        return pos.get(axis) == chainLoop.getFirst().get(axis)
                && sameSubLevel(
                chainLoop.getFirst(),
                pos
        );
    }

    private boolean sameSubLevel(
            BlockPos first,
            BlockPos second
    ) {
        return level != null
                && Objects.equals(
                dev.createmechanicaldrive.compat.SableSubLevelHelper
                        .getSubLevelId(
                                level,
                                first
                        ),
                dev.createmechanicaldrive.compat.SableSubLevelHelper
                        .getSubLevelId(
                                level,
                                second
                        )
        );
    }

    @Nullable
    private ChainGearBlockEntity getOtherEndpointGear() {
        if (level == null
                || !hasChainLoop()
                || chainLoop.size() != 2) {
            return null;
        }

        int otherIndex =
                chainIndex == 0
                        ? 1
                        : 0;

        if (otherIndex < 0
                || otherIndex >= chainLoop.size()) {
            return null;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(
                        chainLoop.get(otherIndex)
                );

        return blockEntity instanceof ChainGearBlockEntity gear
                ? gear
                : null;
    }

    private static BlockPos offsetInPlane(
            BlockPos pos,
            Direction.Axis axis,
            int first,
            int second
    ) {
        return switch (axis) {
            case X -> pos.offset(
                    0,
                    first,
                    second
            );
            case Y -> pos.offset(
                    first,
                    0,
                    second
            );
            case Z -> pos.offset(
                    first,
                    second,
                    0
            );
        };
    }

    private static boolean hasSpeed(
            float speed
    ) {
        return Math.abs(speed) >= SPEED_EPSILON;
    }

    private static boolean speedsMatch(
            float actual,
            float expected
    ) {
        return Math.abs(actual - expected) < SPEED_EPSILON;
    }

    private static int gcd(
            int first,
            int second
    ) {
        first =
                Math.abs(first);

        second =
                Math.abs(second);

        while (second != 0) {
            int remainder =
                    first % second;

            first =
                    second;

            second =
                    remainder;
        }

        return first;
    }

    private boolean isLinkedNeighbour(
            ChainGearBlockEntity other
    ) {
        if (!hasChainLoop()
                || !other.hasChainLoop()) {
            return false;
        }

        if (!chainLoop.equals(other.chainLoop)) {
            return false;
        }

        if (hasFlexibleTwoGearChain()
                && !isFlexibleTwoGearChainCurrentlyValid()) {
            return false;
        }

        if (hasFlexibleTwoGearChain()) {
            BlockPos otherPos =
                    getFlexibleOtherPos();

            if (otherPos == null
                    || !otherPos.equals(
                    other.getBlockPos()
            )) {
                return otherPos.equals(
                        other.getBlockPos()
                );
            }
        }

        return previousChainPos().equals(
                other.getBlockPos()
        ) || nextChainPos().equals(
                other.getBlockPos()
        );
    }

    private BlockPos previousChainPos() {
        return chainLoop.get(
                Math.floorMod(
                        chainIndex - 1,
                        chainLoop.size()
                )
        );
    }

    private BlockPos nextChainPos() {
        return chainLoop.get(
                (chainIndex + 1) % chainLoop.size()
        );
    }

    private static void addIfMissing(
            List<BlockPos> positions,
            BlockPos pos
    ) {
        if (!positions.contains(pos)) {
            positions.add(pos);
        }
    }

    private record StraightChainGeometry(
            BlockPos start,
            BlockPos step,
            int steps,
            Direction.Axis axis
    ) {
    }

    private record ChainAuxiliaryGear(
            ChainGearBlockEntity gear,
            int modifier
    ) {
    }

    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getLoadingDependencies() {
        return getConnectedSubLevelDependency();
    }

    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getConnectionDependencies() {
        return getConnectedSubLevelDependency();
    }

    @Nullable
    private Iterable<@NotNull SubLevel> getConnectedSubLevelDependency() {
        if (connectedSubLevelId == null
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

        SubLevel connectedSubLevel =
                container.getSubLevel(
                        connectedSubLevelId
                );

        if (connectedSubLevel == null
                || connectedSubLevel.isRemoved()) {
            return null;
        }

        return List.of(
                connectedSubLevel
        );
    }
}
