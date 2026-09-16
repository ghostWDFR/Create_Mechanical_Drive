package dev.createmechanicaldrive.content.mechanical_jack;

import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.*;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MechanicalJackPhysics {

    private static final double MOTOR_STIFFNESS =
            20_000.0D;

    private static final double MOTOR_DAMPING =
            1_500.0D;

    private static final double MOTOR_MAX_FORCE =
            100_000.0D;

    private static final Map<
            HeadKey,
            RuntimeJack
            > RUNTIME_JACKS =
            new ConcurrentHashMap<>();

    private static final java.util.Set<UUID> REMOVING_JACKS =
            ConcurrentHashMap.newKeySet();

    private static final java.util.Set<HeadKey> REBINDING_JACKS =
            ConcurrentHashMap.newKeySet();

    private static final Map<HeadKey, PendingHeadPayloadMerge> PENDING_HEAD_PAYLOAD_MERGES =
            new ConcurrentHashMap<>();

    private static final ThreadLocal<Boolean> DETACHING_HEAD_PAYLOAD =
            ThreadLocal.withInitial(
                    () -> false
            );

    private static final ThreadLocal<Boolean> MERGING_HEAD_PAYLOAD =
            ThreadLocal.withInitial(
                    () -> false
            );

    private static final ThreadLocal<java.util.Set<BlockPos>> MOVING_BASES =
            ThreadLocal.withInitial(
                    java.util.HashSet::new
            );

    private static final ThreadLocal<java.util.Map<BlockPos, MovingHead>> MOVING_HEADS =
            ThreadLocal.withInitial(
                    java.util.HashMap::new
            );

    private static final Map<BasePayloadKey, PendingBasePayload> PENDING_BASE_PAYLOADS =
            new ConcurrentHashMap<>();

    private static final ThreadLocal<java.util.Map<BlockPos, PendingBasePayload>> MOVING_BASE_PAYLOADS =
            ThreadLocal.withInitial(
                    java.util.HashMap::new
            );

    public static boolean isDetachingHeadPayload() {
        return DETACHING_HEAD_PAYLOAD.get();
    }

    public static void beforeBaseMoved(
            ServerLevel originLevel,
            BlockState state,
            BlockPos oldPos,
            BlockPos newPos
    ) {
        MOVING_BASES.get()
                .add(
                        oldPos.immutable()
                );

        captureGluedHeadPayload(
                originLevel,
                state,
                oldPos,
                newPos
        );
    }

    public static void afterBaseMoved(
            BlockPos oldPos,
            BlockPos newPos
    ) {
        java.util.Set<BlockPos> movingBases =
                MOVING_BASES.get();

        movingBases.remove(
                oldPos
        );

        if (movingBases.isEmpty()) {
            MOVING_BASES.remove();
        }

        java.util.Map<BlockPos, PendingBasePayload> pendingPayloads =
                MOVING_BASE_PAYLOADS.get();

        pendingPayloads.remove(
                newPos
        );

        if (pendingPayloads.isEmpty()) {
            MOVING_BASE_PAYLOADS.remove();
        }
    }

    public static boolean isMovingBase(
            BlockPos pos
    ) {
        return MOVING_BASES.get()
                .contains(
                        pos
                );
    }

    private static void captureGluedHeadPayload(
            ServerLevel level,
            BlockState state,
            BlockPos oldPos,
            BlockPos newPos
    ) {
        if (!state.hasProperty(
                MechanicalJackBlock.MOUNT
        )) {
            return;
        }

        Direction mount =
                state.getValue(
                        MechanicalJackBlock.MOUNT
                );

        BlockPos payloadOrigin =
                oldPos.relative(
                        mount
                );

        if (level.getBlockState(
                payloadOrigin
        ).isAir()) {
            return;
        }

        java.util.Set<SuperGlueEntity> glueEntities =
                new java.util.HashSet<>();

        if (!SuperGlueEntity.isGlued(
                level,
                oldPos,
                mount,
                glueEntities
        )) {
            return;
        }

        SubLevelAssemblyHelper.GatherResult gatherResult =
                SubLevelAssemblyHelper.gatherConnectedBlocks(
                        payloadOrigin,
                        level,
                        4096,
                        (currentPos, currentState, nextPos, nextState, direction) ->
                                !nextPos.equals(
                                        oldPos
                                )
                                        && !nextState.isAir()
                                        && SuperGlueEntity.isGlued(
                                        level,
                                        currentPos,
                                        direction,
                                        glueEntities
                                )
                );

        if (gatherResult.assemblyState()
                != SubLevelAssemblyHelper.GatherResult.State.SUCCESS
                || gatherResult.blocks() == null
                || gatherResult.blocks().isEmpty()) {
            return;
        }

        java.util.Set<BlockPos> relativePayloadPositions =
                new java.util.HashSet<>();

        for (BlockPos payloadPos : gatherResult.blocks()) {
            if (payloadPos.equals(
                    oldPos
            ) || level.getBlockState(
                    payloadPos
            ).isAir()) {
                continue;
            }

            relativePayloadPositions.add(
                    new BlockPos(
                            payloadPos.getX() - oldPos.getX(),
                            payloadPos.getY() - oldPos.getY(),
                            payloadPos.getZ() - oldPos.getZ()
                    )
            );
        }

        if (relativePayloadPositions.isEmpty()) {
            return;
        }

        MOVING_BASE_PAYLOADS.get()
                .put(
                        newPos.immutable(),
                        new PendingBasePayload(
                                mount,
                                relativePayloadPositions
                        )
                );
    }

    public static void beforeHeadMoved(
            ServerLevel originLevel,
            BlockPos oldPos
    ) {
        if (MERGING_HEAD_PAYLOAD.get()) {
            return;
        }

        Object containing =
                dev.createmechanicaldrive.compat.SableSubLevelHelper
                        .getSubLevel(
                                originLevel,
                                oldPos
                        );

        if (!(containing instanceof ServerSubLevel headSubLevel)) {
            return;
        }

        HeadKey key =
                new HeadKey(
                        headSubLevel.getUniqueId(),
                        oldPos
                );

        MechanicalJackBlockEntity owningJack =
                findJackForHead(
                        headSubLevel.getLevel(),
                        key.subLevelId(),
                        key.headPos()
                );

        if (owningJack == null) {
            owningJack =
                    findJackForRuntime(
                            RUNTIME_JACKS.get(
                                    key
                            )
                    );
        }

        MOVING_HEADS.get()
                .put(
                        oldPos.immutable(),
                        new MovingHead(
                                key,
                                owningJack
                        )
                );
    }

    public static void afterHeadMoved(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockPos oldPos,
            BlockPos newPos
    ) {
        if (MERGING_HEAD_PAYLOAD.get()) {
            return;
        }

        java.util.Map<BlockPos, MovingHead> movingHeads =
                MOVING_HEADS.get();

        MovingHead movingHead =
                movingHeads.remove(
                        oldPos
                );

        if (movingHeads.isEmpty()) {
            MOVING_HEADS.remove();
        }

        if (movingHead == null) {
            return;
        }

        Object containing =
                dev.createmechanicaldrive.compat.SableSubLevelHelper
                        .getSubLevel(
                                resultingLevel,
                                newPos
                        );

        if (!(containing instanceof ServerSubLevel newHeadSubLevel)) {
            return;
        }

        MechanicalJackBlockEntity owningJack =
                movingHead.jack();

        if (owningJack == null) {
            owningJack =
                    findJackForHead(
                            newHeadSubLevel.getLevel(),
                            movingHead.key().subLevelId(),
                            movingHead.key().headPos()
                    );
        }

        if (owningJack == null) {
            return;
        }

        RuntimeJack oldRuntime =
                RUNTIME_JACKS.remove(
                        movingHead.key()
                );

        if (oldRuntime != null
                && oldRuntime.constraint() != null
                && oldRuntime.constraint().isValid()) {

            oldRuntime.constraint().remove();
        }

        HeadKey newKey =
                new HeadKey(
                        newHeadSubLevel.getUniqueId(),
                        newPos
                );

        PENDING_HEAD_PAYLOAD_MERGES.put(
                newKey,
                new PendingHeadPayloadMerge(
                        movingHead.key(),
                        newKey
                )
        );

        REBINDING_JACKS.add(
                newKey
        );

        owningJack.setHeadSubLevelId(
                newKey.subLevelId()
        );

        owningJack.setHeadBlockPos(
                newKey.headPos()
        );

        owningJack.requestKineticReattach();
        owningJack.setChanged();
        owningJack.sendData();
    }

    public static boolean isMovingHead(
            BlockPos pos
    ) {
        return MOVING_HEADS.get()
                .containsKey(
                        pos
                );
    }

    private MechanicalJackPhysics() {
    }

    public static boolean isRemovingJack(
            UUID runtimeId
    ) {
        return runtimeId != null
                && REMOVING_JACKS.contains(
                runtimeId
        );
    }

    public static void assembleBase(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!level
                .getBlockState(pos)
                .is(
                        CreateMechanicalDrive
                                .MECHANICAL_JACK
                                .get()
                )) {
            return;
        }

        BlockPos immutablePos =
                pos.immutable();

        BoundingBox3i bounds =
                new BoundingBox3i(
                        pos.getX() - 1,
                        pos.getY() - 1,
                        pos.getZ() - 1,
                        pos.getX() + 1,
                        pos.getY() + 1,
                        pos.getZ() + 1
                );

        ServerSubLevel baseSubLevel =
                SubLevelAssemblyHelper
                        .assembleBlocks(
                                level,
                                immutablePos,
                                java.util.List.of(
                                        immutablePos
                                ),
                                bounds
                        );

        if (baseSubLevel == null) {
            return;
        }
    }

    private static void invalidateRuntimeForRebind(
            MechanicalJackBlockEntity jack
    ) {
        UUID headId =
                jack.getHeadSubLevelId();

        if (headId == null) {
            return;
        }

        HeadKey key =
                getRuntimeKey(
                        jack
                );

        RuntimeJack runtime =
                removeRuntime(
                        key
                );

        if (runtime == null) {
            java.util.Map.Entry<HeadKey, RuntimeJack> entry =
                    findRuntimeEntry(
                            headId,
                            jack.getHeadBlockPos()
                    );

            if (entry != null) {
                key =
                        entry.getKey();

                runtime =
                        RUNTIME_JACKS.remove(
                                key
                        );
            }
        }

        if (key != null) {
            REBINDING_JACKS.add(
                    key
            );
        }

        if (runtime == null) {
            return;
        }

        if (runtime.constraint() != null
                && runtime.constraint().isValid()) {

            runtime.constraint().remove();
        }
    }
    public static void onBaseAssembled(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockPos newPos
    ) {
        if (!(resultingLevel.getBlockEntity(newPos)
                instanceof MechanicalJackBlockEntity jack)) {
            return;
        }

        ServerSubLevel baseSubLevel =
                resolveBaseSubLevel(
                        resultingLevel,
                        newPos
                );

        invalidateRuntimeForRebind(
                jack
        );

        if (baseSubLevel == null) {
            releaseHeadPayloadToWorldBlocks(
                    resultingLevel,
                    jack
            );

            jack.setAssembled(false);
            jack.setFullBlockCollision(true);
            jack.requestKineticReattach();

            updateBaseCollisionShape(
                    resultingLevel,
                    newPos
            );

            jack.setChanged();
            jack.sendData();
            return;
        }

        jack.setAssembled(true);
        jack.setFullBlockCollision(false);
        jack.requestKineticReattach();

        promotePendingBasePayload(
                baseSubLevel,
                newPos
        );

        jack.requestHeadInitialization();

        updateBaseCollisionShape(
                resultingLevel,
                newPos
        );

        jack.setChanged();
        jack.sendData();
    }

    private static void releaseHeadPayloadToWorldBlocks(
            ServerLevel rootLevel,
            MechanicalJackBlockEntity jack
    ) {
        UUID headId =
                jack.getHeadSubLevelId();

        if (headId == null) {
            return;
        }

        HeadKey runtimeKey =
                getRuntimeKey(
                        jack
                );

        RuntimeJack runtime =
                removeRuntime(
                        runtimeKey
                );

        if (runtime == null) {
            java.util.Map.Entry<HeadKey, RuntimeJack> entry =
                    findRuntimeEntry(
                            headId,
                            jack.getHeadBlockPos()
                    );

            if (entry != null) {
                runtime =
                        RUNTIME_JACKS.remove(
                                entry.getKey()
                        );
            }
        }

        if (runtime != null
                && runtime.constraint() != null
                && runtime.constraint().isValid()) {

            runtime.constraint().remove();
        }

        ServerSubLevel headSubLevel =
                runtime != null
                        ? runtime.head()
                        : findSubLevel(
                        rootLevel,
                        headId
                );

        if (headSubLevel == null
                || headSubLevel.isRemoved()) {
            clearHeadReference(
                    jack
            );
            return;
        }

        BlockPos headPos =
                getKnownHeadBlockPos(
                        jack,
                        headSubLevel
                );

        if (hasOtherHeadBlock(
                headSubLevel,
                headPos
        )) {
            if (headPos != null
                    && headSubLevel.getLevel()
                    instanceof ServerLevel headLevel) {

                headLevel.removeBlock(
                        headPos,
                        false
                );
            }

            clearHeadReference(
                    jack
            );
            return;
        }

        java.util.List<BlockPos> payloadBlocks =
                collectHeadPayloadBlockPositions(
                        rootLevel,
                        headSubLevel,
                        headPos
                );

        moveHeadPayloadToWorld(
                rootLevel,
                headSubLevel,
                headPos,
                payloadBlocks
        );

        if (headPos != null
                && headSubLevel.getLevel()
                instanceof ServerLevel headLevel) {

            headLevel.removeBlock(
                    headPos,
                    false
            );
        }

        removeHeadSubLevel(
                rootLevel,
                headSubLevel
        );

        clearHeadReference(
                jack
        );
    }

    private static java.util.List<BlockPos> collectHeadPayloadBlockPositions(
            ServerLevel rootLevel,
            ServerSubLevel headSubLevel,
            BlockPos headPos
    ) {
        java.util.List<BlockPos> payloadBlocks =
                new java.util.ArrayList<>();

        var bounds =
                headSubLevel
                        .getPlot()
                        .getBoundingBox();

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        bounds.minX(),
                        bounds.minY(),
                        bounds.minZ(),
                        bounds.maxX(),
                        bounds.maxY(),
                        bounds.maxZ()
                )) {

            BlockPos localPos =
                    pos.immutable();

            if (headPos != null
                    && localPos.equals(
                    headPos
            )) {
                continue;
            }

            if (rootLevel.getBlockState(
                    localPos
            ).isAir()) {
                continue;
            }

            payloadBlocks.add(
                    localPos
            );
        }

        return payloadBlocks;
    }

    private static void moveHeadPayloadToWorld(
            ServerLevel rootLevel,
            ServerSubLevel headSubLevel,
            BlockPos headPos,
            java.util.List<BlockPos> payloadBlocks
    ) {
        if (headPos == null
                || payloadBlocks.isEmpty()) {
            return;
        }

        BlockPos headWorldPos =
                BlockPos.containing(
                        headSubLevel
                                .logicalPose()
                                .transformPosition(
                                        Vec3.atCenterOf(
                                                headPos
                                        )
                                )
                );

        SubLevelAssemblyHelper.AssemblyTransform transform =
                new SubLevelAssemblyHelper.AssemblyTransform(
                        headPos,
                        headWorldPos,
                        0,
                        Rotation.NONE,
                        rootLevel
                );

        SubLevelAssemblyHelper.moveBlocks(
                rootLevel,
                transform,
                payloadBlocks
        );
    }

    private static void clearHeadReference(
            MechanicalJackBlockEntity jack
    ) {
        jack.setHeadSubLevelId(
                null
        );

        jack.setHeadBlockPos(
                null
        );

        jack.setActualExtension(
                MechanicalJackBlockEntity.MIN_EXTENSION
        );

        jack.setTargetExtension(
                MechanicalJackBlockEntity.MIN_EXTENSION
        );
    }
    private static void updateBaseCollisionShape(
            ServerLevel level,
            BlockPos pos
    ) {
        BlockState state =
                level.getBlockState(
                        pos
                );

        level.sendBlockUpdated(
                pos,
                state,
                state,
                Block.UPDATE_CLIENTS
                        | Block.UPDATE_KNOWN_SHAPE
        );
    }

    private static void promotePendingBasePayload(
            ServerSubLevel baseSubLevel,
            BlockPos jackPos
    ) {
        PendingBasePayload pendingPayload =
                MOVING_BASE_PAYLOADS.get()
                        .get(
                                jackPos
                        );

        if (pendingPayload == null) {
            return;
        }

        PENDING_BASE_PAYLOADS.put(
                new BasePayloadKey(
                        baseSubLevel.getUniqueId(),
                        jackPos
                ),
                pendingPayload
        );
    }

    private static ServerSubLevel resolveBaseSubLevel(
            ServerLevel level,
            BlockPos pos
    ) {
        Object containing =
                dev.createmechanicaldrive.compat.SableSubLevelHelper
                        .getSubLevel(
                                level,
                                pos
                        );

        if (containing instanceof ServerSubLevel serverSubLevel) {
            return serverSubLevel;
        }

        return null;
    }

    public static void initializeHead(
            MechanicalJackBlockEntity jack,
            ServerLevel currentLevel
    ) {
        ServerSubLevel baseSubLevel =
                resolveBaseSubLevel(
                        currentLevel,
                        jack.getBlockPos()
                );

        if (baseSubLevel == null) {
            jack.requestHeadInitialization();
            return;
        }

        if (shouldRebuildHeadFromBasePayload(
                baseSubLevel,
                jack
        )) {
            discardExistingHeadForPayloadRebuild(
                    baseSubLevel.getLevel(),
                    jack
            );

            createHead(
                    baseSubLevel.getLevel(),
                    baseSubLevel,
                    jack
            );

            return;
        }

        if (jack.getHeadSubLevelId() != null) {
            RuntimeJack runtime =
                    restoreRuntime(
                            jack,
                            baseSubLevel
                    );

            if (runtime == null) {
                jack.requestHeadInitialization();
            }

            return;
        }

        ServerLevel rootLevel =
                baseSubLevel.getLevel();

        createHead(
                rootLevel,
                baseSubLevel,
                jack
        );
    }

    private static void createHead(
            ServerLevel rootLevel,
            ServerSubLevel baseSubLevel,
            MechanicalJackBlockEntity jack
    ) {
        BlockPos jackLocalPos =
                jack.getBlockPos();

        Direction mount =
                jack
                        .getBlockState()
                        .getValue(
                                MechanicalJackBlock.MOUNT
                        );

        Vec3 baseWorldCenter =
                baseSubLevel
                        .logicalPose()
                        .transformPosition(
                                Vec3.atCenterOf(
                                        jackLocalPos
                                )
                        );

        BlockPos headWorldPos =
                BlockPos.containing(
                        baseWorldCenter.x,
                        baseWorldCenter.y,
                        baseWorldCenter.z
                );

        HeadAssembly headAssembly =
                createHeadFromBasePayload(
                        rootLevel,
                        baseSubLevel,
                        jackLocalPos,
                        mount
                );

        ServerSubLevel headSubLevel;
        BlockPos headBlockPos;

        if (headAssembly != null) {
            headSubLevel =
                    headAssembly.headSubLevel();

            headBlockPos =
                    headAssembly.headPos();
        } else {
            if (!rootLevel
                    .getBlockState(headWorldPos)
                    .canBeReplaced()) {

                jack.requestHeadInitialization();
                return;
            }

            rootLevel.setBlock(
                    headWorldPos,
                    CreateMechanicalDrive
                            .MECHANICAL_JACK_HEAD
                            .get()
                            .defaultBlockState()
                            .setValue(
                                    MechanicalJackHeadBlock.MOUNT,
                                    mount
                            ),
                    Block.UPDATE_CLIENTS
                            | Block.UPDATE_KNOWN_SHAPE
            );

            BoundingBox3i headBounds =
                    new BoundingBox3i(
                            headWorldPos.getX() - 1,
                            headWorldPos.getY() - 1,
                            headWorldPos.getZ() - 1,
                            headWorldPos.getX() + 1,
                            headWorldPos.getY() + 1,
                            headWorldPos.getZ() + 1
                    );

            headSubLevel =
                    SubLevelAssemblyHelper
                            .assembleBlocks(
                                    rootLevel,
                                    headWorldPos,
                                    java.util.List.of(
                                            headWorldPos.immutable()
                                    ),
                                    headBounds
                            );

            if (headSubLevel == null) {
                return;
            }

            headBlockPos =
                    findHeadBlockPos(
                            headSubLevel
                    );

            if (headBlockPos == null) {
                return;
            }
        }

        SubLevelPhysicsSystem physicsSystem =
                SubLevelPhysicsSystem.get(
                        rootLevel
                );

        if (physicsSystem == null) {
            return;
        }

        PhysicsPipeline pipeline =
                physicsSystem.getPipeline();

        Vector3d baseAnchor =
                new Vector3d(
                        jackLocalPos.getX() + 0.5D,
                        jackLocalPos.getY() + 0.5D,
                        jackLocalPos.getZ() + 0.5D
                );
        Vector3d headAnchor =
                new Vector3d(
                        headBlockPos.getX() + 0.5D,
                        headBlockPos.getY() + 0.5D,
                        headBlockPos.getZ() + 0.5D
                );

        Vec3 desiredHeadAnchorWorld =
                baseSubLevel
                        .logicalPose()
                        .transformPosition(
                                new Vec3(
                                        baseAnchor.x,
                                        baseAnchor.y,
                                        baseAnchor.z
                                )
                        );

        var currentHeadPose =
                headSubLevel.logicalPose();

        Vec3 currentHeadAnchorWorld =
                currentHeadPose
                        .transformPosition(
                                new Vec3(
                                        headAnchor.x,
                                        headAnchor.y,
                                        headAnchor.z
                                )
                        );

        var currentHeadBodyPosition =
                currentHeadPose.position();

        Quaterniond currentOrientation =
                new Quaterniond(
                        currentHeadPose.orientation()
                );

        Quaterniond targetOrientation =
                new Quaterniond(
                        baseSubLevel
                                .logicalPose()
                                .orientation()
                );

        Vector3d currentAnchorOffsetWorld =
                new Vector3d(
                        currentHeadAnchorWorld.x
                                - currentHeadBodyPosition.x(),
                        currentHeadAnchorWorld.y
                                - currentHeadBodyPosition.y(),
                        currentHeadAnchorWorld.z
                                - currentHeadBodyPosition.z()
                );

        Quaterniond rotationDelta =
                new Quaterniond(
                        targetOrientation
                ).mul(
                        new Quaterniond(
                                currentOrientation
                        ).invert()
                );

        Vector3d targetAnchorOffsetWorld =
                rotationDelta.transform(
                        new Vector3d(
                                currentAnchorOffsetWorld
                        )
                );

        Vector3d correctedHeadBodyPosition =
                new Vector3d(
                        desiredHeadAnchorWorld.x
                                - targetAnchorOffsetWorld.x,
                        desiredHeadAnchorWorld.y
                                - targetAnchorOffsetWorld.y,
                        desiredHeadAnchorWorld.z
                                - targetAnchorOffsetWorld.z
                );

        pipeline.resetVelocity(
                headSubLevel
        );

        pipeline.teleport(
                headSubLevel,
                correctedHeadBodyPosition,
                targetOrientation
        );

        UUID headId =
                headSubLevel.getUniqueId();

        jack.setHeadSubLevelId(
                headId
        );

        jack.setHeadBlockPos(
                headBlockPos
        );

        jack.setActualExtension(
                MechanicalJackBlockEntity.MIN_EXTENSION
        );

        jack.setTargetExtension(
                MechanicalJackBlockEntity.MIN_EXTENSION
        );

        jack.setChanged();
        jack.sendData();
    }

    private static boolean shouldRebuildHeadFromBasePayload(
            ServerSubLevel baseSubLevel,
            MechanicalJackBlockEntity jack
    ) {
        ServerLevel rootLevel =
                baseSubLevel.getLevel();

        Direction mount =
                jack
                        .getBlockState()
                        .getValue(
                                MechanicalJackBlock.MOUNT
                        );

        return isBasePayloadBlock(
                rootLevel,
                baseSubLevel,
                jack.getBlockPos()
                        .relative(
                                mount
                        ),
                jack.getBlockPos()
        );
    }

    private static void discardExistingHeadForPayloadRebuild(
            ServerLevel rootLevel,
            MechanicalJackBlockEntity jack
    ) {
        UUID headId =
                jack.getHeadSubLevelId();

        if (headId == null) {
            return;
        }

        HeadKey runtimeKey =
                getRuntimeKey(
                        jack
                );

        RuntimeJack runtime =
                removeRuntime(
                        runtimeKey
                );

        if (runtime == null) {
            java.util.Map.Entry<HeadKey, RuntimeJack> entry =
                    findRuntimeEntry(
                            headId,
                            jack.getHeadBlockPos()
                    );

            if (entry != null) {
                runtime =
                        RUNTIME_JACKS.remove(
                                entry.getKey()
                        );
            }
        }

        if (runtime != null
                && runtime.constraint() != null
                && runtime.constraint().isValid()) {

            runtime.constraint().remove();
        }

        ServerSubLevel headSubLevel =
                runtime != null
                        ? runtime.head()
                        : findSubLevel(
                        rootLevel,
                        headId
                );

        BlockPos headPos =
                headSubLevel == null
                        ? null
                        : getKnownHeadBlockPos(
                        jack,
                        headSubLevel
                );

        if (headPos != null
                && headSubLevel.getLevel()
                instanceof ServerLevel headLevel) {

            headLevel.removeBlock(
                    headPos,
                    false
            );
        }

        removeHeadSubLevel(
                rootLevel,
                headSubLevel
        );

        jack.setHeadSubLevelId(
                null
        );

        jack.setHeadBlockPos(
                null
        );
    }

    private static HeadAssembly createHeadFromBasePayload(
            ServerLevel level,
            ServerSubLevel baseSubLevel,
            BlockPos jackPos,
            Direction mount
    ) {
        java.util.List<BlockPos> payloadBlocks =
                getBaseHeadPayloadBlocks(
                        level,
                        baseSubLevel,
                        jackPos,
                        mount
                );

        if (payloadBlocks.isEmpty()) {
            return null;
        }

        BlockPos payloadOrigin =
                jackPos.relative(
                        mount
                );

        if (!payloadBlocks.contains(
                payloadOrigin
        )) {
            payloadBlocks =
                    new java.util.ArrayList<>(
                            payloadBlocks
                    );

            payloadBlocks.add(
                    payloadOrigin.immutable()
            );
        }

        BoundingBox3i bounds =
                createBounds(
                        payloadBlocks
                );

        ServerSubLevel headSubLevel;

        MERGING_HEAD_PAYLOAD.set(
                true
        );

        try {
            headSubLevel =
                    SubLevelAssemblyHelper
                            .assembleBlocks(
                                    level,
                                    payloadOrigin,
                                    payloadBlocks,
                                    bounds
                            );
        } finally {
            MERGING_HEAD_PAYLOAD.set(
                    false
            );
        }

        if (headSubLevel == null) {
            return null;
        }

        BlockPos movedPayloadOrigin =
                headSubLevel
                        .getPlot()
                        .getCenterBlock();

        BlockPos headPos =
                movedPayloadOrigin.relative(
                        mount.getOpposite()
                );

        BlockState existingHeadState =
                level.getBlockState(
                        headPos
                );

        if (!existingHeadState.isAir()
                && !existingHeadState.canBeReplaced()) {
            return null;
        }

        headSubLevel
                .getPlot()
                .expandIfNecessary(
                        headPos
                );

        level.setBlock(
                headPos,
                CreateMechanicalDrive
                        .MECHANICAL_JACK_HEAD
                        .get()
                        .defaultBlockState()
                        .setValue(
                                MechanicalJackHeadBlock.MOUNT,
                                mount
                        ),
                Block.UPDATE_CLIENTS
                        | Block.UPDATE_KNOWN_SHAPE
        );

        return new HeadAssembly(
                headSubLevel,
                headPos.immutable()
        );
    }

    private static java.util.List<BlockPos> getBaseHeadPayloadBlocks(
            ServerLevel level,
            ServerSubLevel baseSubLevel,
            BlockPos jackPos,
            Direction mount
    ) {
        java.util.List<BlockPos> pendingPayloadBlocks =
                getPendingBasePayloadBlocks(
                        level,
                        baseSubLevel,
                        jackPos,
                        mount
                );

        if (!pendingPayloadBlocks.isEmpty()) {
            return pendingPayloadBlocks;
        }

        BlockPos payloadOrigin =
                jackPos.relative(
                        mount
                );

        if (!isBasePayloadBlock(
                level,
                baseSubLevel,
                payloadOrigin,
                jackPos
        )) {
            return java.util.List.of();
        }

        return gatherHeadSidePayloadBlocks(
                level,
                baseSubLevel,
                payloadOrigin,
                jackPos,
                mount
        );
    }
    private static ServerSubLevel attachGluedPayloadToHead(
            ServerLevel level,
            ServerSubLevel baseSubLevel,
            ServerSubLevel headSubLevel,
            BlockPos headPos,
            BlockPos jackPos,
            Direction mount
    ) {
        java.util.List<BlockPos> pendingPayloadBlocks =
                getPendingBasePayloadBlocks(
                        level,
                        baseSubLevel,
                        jackPos,
                        mount
                );

        if (!pendingPayloadBlocks.isEmpty()) {
            return assembleHeadWithPayload(
                    level,
                    headSubLevel,
                    headPos,
                    pendingPayloadBlocks
            );
        }

        BlockPos payloadOrigin =
                jackPos.relative(
                        mount
                );

        if (!isBasePayloadBlock(
                level,
                baseSubLevel,
                payloadOrigin,
                jackPos
        )) {
            return headSubLevel;
        }

        java.util.Set<SuperGlueEntity> glueEntities =
                new java.util.HashSet<>();

        if (!SuperGlueEntity.isGlued(
                level,
                jackPos,
                mount,
                glueEntities
        )) {
            java.util.List<BlockPos> headSidePayloadBlocks =
                    gatherHeadSidePayloadBlocks(
                            level,
                            baseSubLevel,
                            payloadOrigin,
                            jackPos,
                            mount
                    );

            return assembleHeadWithPayload(
                    level,
                    headSubLevel,
                    headPos,
                    headSidePayloadBlocks
            );
        }

        SubLevelAssemblyHelper.GatherResult gatherResult =
                SubLevelAssemblyHelper.gatherConnectedBlocks(
                        payloadOrigin,
                        level,
                        4096,
                        (currentPos, currentState, nextPos, nextState, direction) ->
                                isValidHeadPayloadConnection(
                                        level,
                                        baseSubLevel,
                                        jackPos,
                                        nextPos,
                                        nextState
                                )
                                        && SuperGlueEntity.isGlued(
                                        level,
                                        currentPos,
                                        direction,
                                        glueEntities
                                )
                );

        if (gatherResult.assemblyState()
                != SubLevelAssemblyHelper.GatherResult.State.SUCCESS
                || gatherResult.blocks() == null
                || gatherResult.blocks().isEmpty()) {
            return headSubLevel;
        }

        java.util.List<BlockPos> payloadBlocks =
                new java.util.ArrayList<>();

        for (BlockPos payloadPos : gatherResult.blocks()) {
            if (!isBasePayloadBlock(
                    level,
                    baseSubLevel,
                    payloadPos,
                    jackPos
            )) {
                continue;
            }

            payloadBlocks.add(
                    payloadPos.immutable()
            );
        }

        return assembleHeadWithPayload(
                level,
                headSubLevel,
                headPos,
                payloadBlocks
        );
    }

    private static java.util.List<BlockPos> gatherHeadSidePayloadBlocks(
            ServerLevel level,
            ServerSubLevel baseSubLevel,
            BlockPos payloadOrigin,
            BlockPos jackPos,
            Direction mount
    ) {
        SubLevelAssemblyHelper.GatherResult gatherResult =
                SubLevelAssemblyHelper.gatherConnectedBlocks(
                        payloadOrigin,
                        level,
                        4096,
                        (currentPos, currentState, nextPos, nextState, direction) ->
                                isHeadSideBasePayloadBlock(
                                        level,
                                        baseSubLevel,
                                        jackPos,
                                        mount,
                                        nextPos,
                                        nextState
                                )
                );

        if (gatherResult.assemblyState()
                != SubLevelAssemblyHelper.GatherResult.State.SUCCESS
                || gatherResult.blocks() == null
                || gatherResult.blocks().isEmpty()) {
            return java.util.List.of();
        }

        java.util.List<BlockPos> payloadBlocks =
                new java.util.ArrayList<>();

        for (BlockPos payloadPos : gatherResult.blocks()) {
            if (!isHeadSideBasePayloadBlock(
                    level,
                    baseSubLevel,
                    jackPos,
                    mount,
                    payloadPos,
                    level.getBlockState(
                            payloadPos
                    )
            )) {
                continue;
            }

            payloadBlocks.add(
                    payloadPos.immutable()
            );
        }

        return payloadBlocks;
    }

    private static boolean isHeadSideBasePayloadBlock(
            ServerLevel level,
            ServerSubLevel baseSubLevel,
            BlockPos jackPos,
            Direction mount,
            BlockPos payloadPos,
            BlockState payloadState
    ) {
        return isInMountHalfSpace(
                jackPos,
                mount,
                payloadPos
        )
                && isValidHeadPayloadConnection(
                level,
                baseSubLevel,
                jackPos,
                payloadPos,
                payloadState
        );
    }

    private static boolean isInMountHalfSpace(
            BlockPos jackPos,
            Direction mount,
            BlockPos pos
    ) {
        return (pos.getX() - jackPos.getX()) * mount.getStepX()
                + (pos.getY() - jackPos.getY()) * mount.getStepY()
                + (pos.getZ() - jackPos.getZ()) * mount.getStepZ()
                > 0;
    }

    private static java.util.List<BlockPos> getPendingBasePayloadBlocks(
            ServerLevel level,
            ServerSubLevel baseSubLevel,
            BlockPos jackPos,
            Direction mount
    ) {
        BasePayloadKey key =
                new BasePayloadKey(
                        baseSubLevel.getUniqueId(),
                        jackPos
                );

        PendingBasePayload pendingPayload =
                PENDING_BASE_PAYLOADS.get(
                        key
                );

        if (pendingPayload == null
                || pendingPayload.mount() != mount) {
            return java.util.List.of();
        }

        java.util.List<BlockPos> payloadBlocks =
                new java.util.ArrayList<>();

        for (BlockPos relativePayloadPos : pendingPayload.relativePayloadPositions()) {
            BlockPos payloadPos =
                    jackPos.offset(
                            relativePayloadPos
                    );

            if (!isBasePayloadBlock(
                    level,
                    baseSubLevel,
                    payloadPos,
                    jackPos
            )) {
                continue;
            }

            payloadBlocks.add(
                    payloadPos.immutable()
            );
        }

        if (!payloadBlocks.isEmpty()) {
            PENDING_BASE_PAYLOADS.remove(
                    key
            );
        }

        return payloadBlocks;
    }

    private static ServerSubLevel assembleHeadWithPayload(
            ServerLevel level,
            ServerSubLevel headSubLevel,
            BlockPos headPos,
            java.util.List<BlockPos> payloadBlocks
    ) {
        if (payloadBlocks.isEmpty()) {
            return headSubLevel;
        }

        java.util.List<BlockPos> blocksToAttach =
                new java.util.ArrayList<>();

        blocksToAttach.add(
                headPos.immutable()
        );

        for (BlockPos payloadPos : payloadBlocks) {
            if (payloadPos.equals(
                    headPos
            )) {
                continue;
            }

            blocksToAttach.add(
                    payloadPos.immutable()
            );
        }

        if (blocksToAttach.size() <= 1) {
            return headSubLevel;
        }

        BoundingBox3i bounds =
                createBounds(
                        blocksToAttach
                );

        MERGING_HEAD_PAYLOAD.set(
                true
        );

        try {
            ServerSubLevel mergedHeadSubLevel =
                    SubLevelAssemblyHelper
                            .assembleBlocks(
                                    level,
                                    headPos,
                                    blocksToAttach,
                                    bounds
                            );

            return mergedHeadSubLevel == null
                    ? headSubLevel
                    : mergedHeadSubLevel;
        } finally {
            MERGING_HEAD_PAYLOAD.set(
                    false
            );
        }
    }
    private static boolean isValidHeadPayloadConnection(
            ServerLevel level,
            ServerSubLevel baseSubLevel,
            BlockPos jackPos,
            BlockPos nextPos,
            BlockState nextState
    ) {
        return !nextPos.equals(
                jackPos
        )
                && !nextState.isAir()
                && baseSubLevel.getUniqueId()
                .equals(
                        dev.createmechanicaldrive.compat.SableSubLevelHelper
                                .getSubLevelId(
                                        level,
                                        nextPos
                                )
                );
    }

    private static boolean isBasePayloadBlock(
            ServerLevel level,
            ServerSubLevel baseSubLevel,
            BlockPos pos,
            BlockPos jackPos
    ) {
        if (pos.equals(
                jackPos
        )) {
            return false;
        }

        BlockState state =
                level.getBlockState(
                        pos
                );

        return isValidHeadPayloadConnection(
                level,
                baseSubLevel,
                jackPos,
                pos,
                state
        );
    }

    private static BoundingBox3i createBounds(
            java.util.List<BlockPos> positions
    ) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : positions) {
            minX = Math.min(
                    minX,
                    pos.getX()
            );
            minY = Math.min(
                    minY,
                    pos.getY()
            );
            minZ = Math.min(
                    minZ,
                    pos.getZ()
            );
            maxX = Math.max(
                    maxX,
                    pos.getX()
            );
            maxY = Math.max(
                    maxY,
                    pos.getY()
            );
            maxZ = Math.max(
                    maxZ,
                    pos.getZ()
            );
        }

        return new BoundingBox3i(
                minX - 1,
                minY - 1,
                minZ - 1,
                maxX + 1,
                maxY + 1,
                maxZ + 1
        );
    }
    private static boolean recreateMissingHead(
            ServerLevel rootLevel,
            ServerSubLevel baseSubLevel,
            MechanicalJackBlockEntity jack
    ) {
        HeadKey oldKey =
                getRuntimeKey(
                        jack
                );

        RuntimeJack oldRuntime =
                removeRuntime(
                        oldKey
                );

        if (oldRuntime == null) {
            java.util.Map.Entry<HeadKey, RuntimeJack> entry =
                    findRuntimeEntry(
                            jack.getHeadSubLevelId(),
                            jack.getHeadBlockPos()
                    );

            if (entry != null) {
                oldRuntime =
                        RUNTIME_JACKS.remove(
                                entry.getKey()
                        );
            }
        }

        if (oldRuntime != null
                && oldRuntime.constraint() != null
                && oldRuntime.constraint().isValid()) {

            oldRuntime.constraint().remove();
        }

        BlockPos jackLocalPos =
                jack.getBlockPos();

        Direction mount =
                jack
                        .getBlockState()
                        .getValue(
                                MechanicalJackBlock.MOUNT
                        );

        Vector3d baseAnchor =
                new Vector3d(
                        jackLocalPos.getX() + 0.5D,
                        jackLocalPos.getY() + 0.5D,
                        jackLocalPos.getZ() + 0.5D
                );

        Vec3 baseAnchorWorld =
                baseSubLevel
                        .logicalPose()
                        .transformPosition(
                                new Vec3(
                                        baseAnchor.x,
                                        baseAnchor.y,
                                        baseAnchor.z
                                )
                        );

        Vec3 extensionAxisWorld =
                getLocalAxisWorld(
                        baseSubLevel,
                        baseAnchor,
                        mount
                );

        double extension =
                Mth.clamp(
                        jack.getActualExtension(),
                        MechanicalJackBlockEntity.MIN_EXTENSION,
                        jack.getMaxExtension()
                );

        Vec3 desiredHeadAnchorWorld =
                baseAnchorWorld.add(
                        extensionAxisWorld.scale(
                                extension
                        )
                );

        BlockPos headWorldPos =
                BlockPos.containing(
                        desiredHeadAnchorWorld
                );

        HeadAssembly headAssembly =
                createHeadFromBasePayload(
                        rootLevel,
                        baseSubLevel,
                        jackLocalPos,
                        mount
                );

        ServerSubLevel headSubLevel;
        BlockPos headBlockPos;

        if (headAssembly != null) {
            headSubLevel =
                    headAssembly.headSubLevel();

            headBlockPos =
                    headAssembly.headPos();
        } else {
            BlockState existingState =
                    rootLevel.getBlockState(
                            headWorldPos
                    );

            boolean alreadyHead =
                    existingState.is(
                            CreateMechanicalDrive
                                    .MECHANICAL_JACK_HEAD
                                    .get()
                    );

            if (!alreadyHead
                    && !existingState.canBeReplaced()) {
                jack.requestHeadInitialization();
                return false;
            }

            rootLevel.setBlock(
                    headWorldPos,
                    CreateMechanicalDrive
                            .MECHANICAL_JACK_HEAD
                            .get()
                            .defaultBlockState()
                            .setValue(
                                    MechanicalJackHeadBlock.MOUNT,
                                    mount
                            ),
                    Block.UPDATE_CLIENTS
                            | Block.UPDATE_KNOWN_SHAPE
            );

            BoundingBox3i headBounds =
                    new BoundingBox3i(
                            headWorldPos.getX() - 1,
                            headWorldPos.getY() - 1,
                            headWorldPos.getZ() - 1,
                            headWorldPos.getX() + 1,
                            headWorldPos.getY() + 1,
                            headWorldPos.getZ() + 1
                    );

            headSubLevel =
                    SubLevelAssemblyHelper
                            .assembleBlocks(
                                    rootLevel,
                                    headWorldPos,
                                    java.util.List.of(
                                            headWorldPos.immutable()
                                    ),
                                    headBounds
                            );

            if (headSubLevel == null) {
                jack.requestHeadInitialization();
                return false;
            }

            headBlockPos =
                    isHeadBlockInSubLevel(
                            headSubLevel,
                            headWorldPos
                    )
                            ? headWorldPos.immutable()
                            : findSingleHeadBlockPos(
                            headSubLevel
                    );

            if (headBlockPos == null) {
                jack.requestHeadInitialization();
                return false;
            }
        }

        SubLevelPhysicsSystem physicsSystem =
                SubLevelPhysicsSystem.get(
                        rootLevel
                );

        if (physicsSystem == null) {
            return false;
        }

        PhysicsPipeline pipeline =
                physicsSystem.getPipeline();
        Vector3d headAnchor =
                new Vector3d(
                        headBlockPos.getX() + 0.5D,
                        headBlockPos.getY() + 0.5D,
                        headBlockPos.getZ() + 0.5D
                );

        var currentHeadPose =
                headSubLevel.logicalPose();

        Vec3 currentHeadAnchorWorld =
                currentHeadPose
                        .transformPosition(
                                new Vec3(
                                        headAnchor.x,
                                        headAnchor.y,
                                        headAnchor.z
                                )
                        );

        var currentHeadBodyPosition =
                currentHeadPose.position();

        Quaterniond currentOrientation =
                new Quaterniond(
                        currentHeadPose.orientation()
                );

        Quaterniond targetOrientation =
                new Quaterniond(
                        baseSubLevel
                                .logicalPose()
                                .orientation()
                );

        Vector3d currentAnchorOffsetWorld =
                new Vector3d(
                        currentHeadAnchorWorld.x
                                - currentHeadBodyPosition.x(),
                        currentHeadAnchorWorld.y
                                - currentHeadBodyPosition.y(),
                        currentHeadAnchorWorld.z
                                - currentHeadBodyPosition.z()
                );

        Quaterniond rotationDelta =
                new Quaterniond(
                        targetOrientation
                ).mul(
                        new Quaterniond(
                                currentOrientation
                        ).invert()
                );

        Vector3d targetAnchorOffsetWorld =
                rotationDelta.transform(
                        new Vector3d(
                                currentAnchorOffsetWorld
                        )
                );

        Vector3d correctedHeadBodyPosition =
                new Vector3d(
                        desiredHeadAnchorWorld.x
                                - targetAnchorOffsetWorld.x,
                        desiredHeadAnchorWorld.y
                                - targetAnchorOffsetWorld.y,
                        desiredHeadAnchorWorld.z
                                - targetAnchorOffsetWorld.z
                );

        pipeline.resetVelocity(
                headSubLevel
        );

        pipeline.teleport(
                headSubLevel,
                correctedHeadBodyPosition,
                targetOrientation
        );

        jack.setHeadSubLevelId(
                headSubLevel.getUniqueId()
        );

        jack.setHeadBlockPos(
                headBlockPos
        );

        jack.setChanged();
        jack.sendData();

        return true;
    }
    private static GenericConstraintHandle
    createExtensionConstraint(
            PhysicsPipeline pipeline,
            ServerSubLevel baseSubLevel,
            ServerSubLevel headSubLevel,
            Vector3d baseAnchor,
            Vector3d headAnchor,
            Direction mount
    ) {
        EnumSet<ConstraintJointAxis> lockedAxes =
                EnumSet.of(
                        ConstraintJointAxis.LINEAR_X,
                        ConstraintJointAxis.LINEAR_Z,
                        ConstraintJointAxis.ANGULAR_X,
                        ConstraintJointAxis.ANGULAR_Y,
                        ConstraintJointAxis.ANGULAR_Z
                );

        Quaterniond jointOrientation =
                getJointOrientation(
                        mount
                );

        GenericConstraintConfiguration configuration =
                new GenericConstraintConfiguration(
                        baseAnchor,
                        headAnchor,
                        new Quaterniond(
                                jointOrientation
                        ),
                        new Quaterniond(
                                jointOrientation
                        ),
                        lockedAxes
                );

        return pipeline.addConstraint(
                baseSubLevel,
                headSubLevel,
                configuration
        );
    }

    private static Quaterniond getJointOrientation(
            Direction mount
    ) {
        Quaterniond orientation =
                new Quaterniond();

        switch (mount) {
            case UP -> {
            }

            case DOWN ->
                    orientation.rotateX(
                            Math.PI
                    );

            case NORTH ->
                    orientation.rotateX(
                            -Math.PI / 2.0D
                    );

            case SOUTH ->
                    orientation.rotateX(
                            Math.PI / 2.0D
                    );

            case EAST ->
                    orientation.rotateZ(
                            -Math.PI / 2.0D
                    );

            case WEST ->
                    orientation.rotateZ(
                            Math.PI / 2.0D
                    );
        }

        return orientation;
    }

    public static void tickJack(
            MechanicalJackBlockEntity jack,
            ServerLevel currentLevel
    ) {
        ServerSubLevel baseSubLevel =
                resolveBaseSubLevel(
                        currentLevel,
                        jack.getBlockPos()
                );

        if (baseSubLevel == null) {
            return;
        }

        UUID runtimeId =
                jack.getHeadSubLevelId();

        if (runtimeId == null) {
            return;
        }

        HeadKey runtimeKey =
                getRuntimeKey(
                        jack
                );

        processPendingHeadPayloadMerge(
                baseSubLevel.getLevel(),
                jack,
                runtimeKey
        );

        runtimeId =
                jack.getHeadSubLevelId();

        if (runtimeId == null) {
            return;
        }

        runtimeKey =
                getRuntimeKey(
                        jack
                );

        RuntimeJack runtime =
                runtimeKey == null
                        ? null
                        : RUNTIME_JACKS.get(
                        runtimeKey
                );

        if (runtime == null) {
            java.util.Map.Entry<HeadKey, RuntimeJack> entry =
                    findRuntimeEntry(
                            runtimeId,
                            jack.getHeadBlockPos()
                    );

            if (entry != null) {
                runtimeKey =
                        entry.getKey();

                runtime =
                        entry.getValue();
            }
        }

        if (runtime != null
                && runtime.isValid()
                && !runtime.base()
                .getUniqueId()
                .equals(
                        baseSubLevel.getUniqueId()
                )) {

            removeRuntime(
                    runtimeKey
            );

            if (runtime.constraint() != null
                    && runtime.constraint().isValid()) {

                runtime.constraint().remove();
            }

            runtime = null;
        }

        if (runtime == null
                || !runtime.isValid()) {

            runtime =
                    restoreRuntime(
                            jack,
                            baseSubLevel
                    );

            if (runtime == null) {
                return;
            }
        }

        double actual =
                calculateActualExtension(
                        runtime,
                        jack.getMaxExtension()
                );

        double target =
                jack.getTargetExtension();

        runtime.constraint()
                .setLimit(
                        ConstraintJointAxis.LINEAR_Y,
                        MechanicalJackBlockEntity.MIN_EXTENSION,
                        jack.getMaxExtension()
                );

        runtime.constraint()
                .setMotor(
                        ConstraintJointAxis.LINEAR_Y,
                        target,
                        MOTOR_STIFFNESS,
                        MOTOR_DAMPING,
                        true,
                        MOTOR_MAX_FORCE
                );

        if (Math.abs(target - actual) > 0.0001D) {
            SubLevelPhysicsSystem physicsSystem =
                    SubLevelPhysicsSystem.get(
                            currentLevel
                    );

            if (physicsSystem != null) {
                PhysicsPipeline pipeline =
                        physicsSystem.getPipeline();

                pipeline.wakeUp(
                        runtime.base()
                );

                pipeline.wakeUp(
                        runtime.head()
                );
            }
        }

        jack.setActualExtension(
                actual
        );
    }

    private static RuntimeJack restoreRuntime(
            MechanicalJackBlockEntity jack,
            ServerSubLevel baseSubLevel
    ) {
        UUID headId =
                jack.getHeadSubLevelId();

        if (headId == null) {
            return null;
        }

        if (!(baseSubLevel.getLevel()
                instanceof ServerLevel rootLevel)) {
            return null;
        }

        ServerSubLevel headSubLevel =
                findSubLevel(
                        rootLevel,
                        headId
                );

        if (headSubLevel != null
                && jack.getHeadBlockPos() == null) {
            BlockPos singleHeadPos =
                    findSingleHeadBlockPos(
                            headSubLevel
                    );

            if (singleHeadPos != null) {
                jack.setHeadBlockPos(
                        singleHeadPos
                );
            }
        }

        if (headSubLevel == null
                || !isHeadBlockInSubLevel(
                headSubLevel,
                jack.getHeadBlockPos()
        )) {
            if (!recreateMissingHead(
                    rootLevel,
                    baseSubLevel,
                    jack
            )) {
                return null;
            }

            headId =
                    jack.getHeadSubLevelId();

            headSubLevel =
                    findSubLevel(
                            rootLevel,
                            headId
                    );
        }

        if (headSubLevel == null) {
            return null;
        }

        SubLevelPhysicsSystem physicsSystem =
                SubLevelPhysicsSystem.get(
                        rootLevel
                );

        if (physicsSystem == null) {
            return null;
        }

        PhysicsPipeline pipeline =
                physicsSystem.getPipeline();

        BlockPos jackLocalPos =
                jack.getBlockPos();

        Vector3d baseAnchor =
                new Vector3d(
                        jackLocalPos.getX() + 0.5D,
                        jackLocalPos.getY() + 0.5D,
                        jackLocalPos.getZ() + 0.5D
                );

        BlockPos headBlockPos =
                getKnownHeadBlockPos(
                        jack,
                        headSubLevel
                );

        if (headBlockPos == null) {
            return null;
        }

        Direction mount =
                jack
                        .getBlockState()
                        .getValue(
                                MechanicalJackBlock.MOUNT
                        );

        HeadKey runtimeKey =
                new HeadKey(
                        headId,
                        headBlockPos
                );

        Vector3d headAnchor =
                new Vector3d(
                        headBlockPos.getX() + 0.5D,
                        headBlockPos.getY() + 0.5D,
                        headBlockPos.getZ() + 0.5D
                );

        if (REBINDING_JACKS.remove(runtimeKey)) {

            Vec3 baseAnchorWorld =
                    getAnchorWorldPosition(
                            baseSubLevel,
                            baseAnchor
                    );

            Vec3 headAnchorWorld =
                    getAnchorWorldPosition(
                            headSubLevel,
                            headAnchor
                    );

            Vec3 extensionAxisWorld =
                    getLocalAxisWorld(
                            baseSubLevel,
                            baseAnchor,
                            mount
                    );

            double currentExtension =
                    headAnchorWorld
                            .subtract(
                                    baseAnchorWorld
                            )
                            .dot(
                                    extensionAxisWorld
                            );

            currentExtension =
                    Mth.clamp(
                            currentExtension,
                            MechanicalJackBlockEntity.MIN_EXTENSION,
                            jack.getMaxExtension()
                    );

            jack.setActualExtension(
                    currentExtension
            );

            jack.setTargetExtension(
                    currentExtension
            );

            pipeline.resetVelocity(
                    headSubLevel
            );
        }

        GenericConstraintHandle constraint =
                createExtensionConstraint(
                        pipeline,
                        baseSubLevel,
                        headSubLevel,
                        baseAnchor,
                        headAnchor,
                        mount
                );

        if (constraint == null) {
            return null;
        }

        constraint.setContactsEnabled(false);

        constraint.setLimit(
                ConstraintJointAxis.LINEAR_Y,
                MechanicalJackBlockEntity.MIN_EXTENSION,
                jack.getMaxExtension()
        );

        constraint.setMotor(
                ConstraintJointAxis.LINEAR_Y,
                jack.getTargetExtension(),
                MOTOR_STIFFNESS,
                MOTOR_DAMPING,
                true,
                MOTOR_MAX_FORCE
        );

        RuntimeJack runtime =
                new RuntimeJack(
                        baseSubLevel,
                        headSubLevel,
                        constraint,
                        baseAnchor,
                        headAnchor,
                        mount
                );

        RUNTIME_JACKS.put(
                runtimeKey,
                runtime
        );

        return runtime;
    }
    private static double calculateActualExtension(
            RuntimeJack runtime,
            double maxExtension
    ) {
        Vec3 baseAnchorWorld =
                getAnchorWorldPosition(
                        runtime.base(),
                        runtime.baseAnchor()
                );

        Vec3 headAnchorWorld =
                getAnchorWorldPosition(
                        runtime.head(),
                        runtime.headAnchor()
                );

        Vec3 extensionAxisWorld =
                getLocalAxisWorld(
                        runtime.base(),
                        runtime.baseAnchor(),
                        runtime.mount()
                );

        Vec3 baseToHead =
                headAnchorWorld.subtract(
                        baseAnchorWorld
                );

        double difference =
                baseToHead.dot(
                        extensionAxisWorld
                );

        if (difference
                < MechanicalJackBlockEntity.MIN_EXTENSION) {

            return MechanicalJackBlockEntity.MIN_EXTENSION;
        }

        if (difference
                > maxExtension) {

            return maxExtension;
        }

        return difference;
    }

    private static Vec3 getAnchorWorldPosition(
            ServerSubLevel subLevel,
            Vector3d localAnchor
    ) {
        Vec3 localPosition =
                new Vec3(
                        localAnchor.x,
                        localAnchor.y,
                        localAnchor.z
                );

        return subLevel
                .logicalPose()
                .transformPosition(
                        localPosition
                );
    }

    private static Vec3 getLocalAxisWorld(
            ServerSubLevel subLevel,
            Vector3d localAnchor,
            Direction direction
    ) {
        Vec3 localOrigin =
                new Vec3(
                        localAnchor.x,
                        localAnchor.y,
                        localAnchor.z
                );

        Vec3 localAxisPoint =
                new Vec3(
                        localAnchor.x
                                + direction.getStepX(),
                        localAnchor.y
                                + direction.getStepY(),
                        localAnchor.z
                                + direction.getStepZ()
                );

        Vec3 worldOrigin =
                subLevel
                        .logicalPose()
                        .transformPosition(
                                localOrigin
                        );

        Vec3 worldAxisPoint =
                subLevel
                        .logicalPose()
                        .transformPosition(
                                localAxisPoint
                        );

        return worldAxisPoint
                .subtract(
                        worldOrigin
                )
                .normalize();
    }

    private static HeadKey getRuntimeKey(
            MechanicalJackBlockEntity jack
    ) {
        UUID headId =
                jack.getHeadSubLevelId();

        BlockPos headPos =
                jack.getHeadBlockPos();

        if (headId == null
                || headPos == null) {
            return null;
        }

        return new HeadKey(
                headId,
                headPos
        );
    }

    private static java.util.Map.Entry<HeadKey, RuntimeJack> findRuntimeEntry(
            UUID headId,
            BlockPos headPos
    ) {
        if (headId == null) {
            return null;
        }

        for (java.util.Map.Entry<HeadKey, RuntimeJack> entry :
                RUNTIME_JACKS.entrySet()) {

            HeadKey key =
                    entry.getKey();

            if (!headId.equals(
                    key.subLevelId()
            )) {
                continue;
            }

            if (headPos == null
                    || headPos.equals(
                    key.headPos()
            )) {
                return entry;
            }
        }

        return null;
    }

    private static RuntimeJack removeRuntime(
            HeadKey key
    ) {
        if (key == null) {
            return null;
        }

        return RUNTIME_JACKS.remove(
                key
        );
    }

    private static BlockPos getKnownHeadBlockPos(
            MechanicalJackBlockEntity jack,
            ServerSubLevel headSubLevel
    ) {
        BlockPos storedHeadPos =
                jack.getHeadBlockPos();

        if (isHeadBlockInSubLevel(
                headSubLevel,
                storedHeadPos
        )) {
            return storedHeadPos.immutable();
        }

        BlockPos foundHeadPos =
                findSingleHeadBlockPos(
                        headSubLevel
                );

        if (foundHeadPos != null) {
            jack.setHeadBlockPos(
                    foundHeadPos
            );
        }

        return foundHeadPos;
    }

    private static BlockPos findSingleHeadBlockPos(
            ServerSubLevel headSubLevel
    ) {
        if (!(headSubLevel.getLevel()
                instanceof ServerLevel level)) {
            return null;
        }

        var bounds =
                headSubLevel
                        .getPlot()
                        .getBoundingBox();

        BlockPos found =
                null;

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        bounds.minX(),
                        bounds.minY(),
                        bounds.minZ(),
                        bounds.maxX(),
                        bounds.maxY(),
                        bounds.maxZ()
                )) {

            BlockPos immutable =
                    pos.immutable();

            if (!level.getBlockState(
                    immutable
            ).is(
                    CreateMechanicalDrive
                            .MECHANICAL_JACK_HEAD
                            .get()
            )) {
                continue;
            }

            if (found != null) {
                return null;
            }

            found =
                    immutable;
        }

        return found;
    }

    private static boolean isHeadBlockInSubLevel(
            ServerSubLevel headSubLevel,
            BlockPos pos
    ) {
        if (pos == null
                || !(headSubLevel.getLevel()
                instanceof ServerLevel level)) {
            return false;
        }

        if (!level.getBlockState(
                pos
        ).is(
                CreateMechanicalDrive
                        .MECHANICAL_JACK_HEAD
                        .get()
        )) {
            return false;
        }

        UUID containingId =
                dev.createmechanicaldrive.compat.SableSubLevelHelper
                        .getSubLevelId(
                                level,
                                pos
                        );

        return headSubLevel.getUniqueId()
                .equals(
                        containingId
                );
    }

    private static boolean hasOtherHeadBlock(
            ServerSubLevel headSubLevel,
            BlockPos skippedHeadPos
    ) {
        if (!(headSubLevel.getLevel()
                instanceof ServerLevel level)) {
            return false;
        }

        var bounds =
                headSubLevel
                        .getPlot()
                        .getBoundingBox();

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        bounds.minX(),
                        bounds.minY(),
                        bounds.minZ(),
                        bounds.maxX(),
                        bounds.maxY(),
                        bounds.maxZ()
                )) {

            BlockPos immutable =
                    pos.immutable();

            if (skippedHeadPos != null
                    && immutable.equals(
                    skippedHeadPos
            )) {
                continue;
            }

            if (level.getBlockState(
                    immutable
            ).is(
                    CreateMechanicalDrive
                            .MECHANICAL_JACK_HEAD
                            .get()
            )) {
                return true;
            }
        }

        return false;
    }
    private static BlockPos findHeadBlockPos(
            ServerSubLevel headSubLevel
    ) {
        if (!(headSubLevel.getLevel()
                instanceof ServerLevel level)) {
            return null;
        }

        var bounds =
                headSubLevel
                        .getPlot()
                        .getBoundingBox();

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        bounds.minX(),
                        bounds.minY(),
                        bounds.minZ(),
                        bounds.maxX(),
                        bounds.maxY(),
                        bounds.maxZ()
                )) {

            BlockPos immutable =
                    pos.immutable();

            if (level
                    .getBlockState(immutable)
                    .is(
                            CreateMechanicalDrive
                                    .MECHANICAL_JACK_HEAD
                                    .get()
                    )) {

                return immutable;
            }
        }

        return null;
    }

    private static void detachBlocksFromHead(
            ServerSubLevel headSubLevel,
            BlockPos headPos
    ) {
        if (!(headSubLevel.getLevel()
                instanceof ServerLevel level)) {
            return;
        }

        var bounds =
                headSubLevel
                        .getPlot()
                        .getBoundingBox();

        java.util.List<BlockPos> blocksToDetach =
                new java.util.ArrayList<>();

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        bounds.minX(),
                        bounds.minY(),
                        bounds.minZ(),
                        bounds.maxX(),
                        bounds.maxY(),
                        bounds.maxZ()
                )) {

            BlockPos immutable =
                    pos.immutable();

            if (headPos != null
                    && immutable.equals(headPos)) {
                continue;
            }

            BlockState state =
                    level.getBlockState(
                            immutable
                    );

            if (state.isAir()) {
                continue;
            }

            blocksToDetach.add(
                    immutable
            );
        }

        if (blocksToDetach.isEmpty()) {
            return;
        }

        int minX =
                Integer.MAX_VALUE;
        int minY =
                Integer.MAX_VALUE;
        int minZ =
                Integer.MAX_VALUE;

        int maxX =
                Integer.MIN_VALUE;
        int maxY =
                Integer.MIN_VALUE;
        int maxZ =
                Integer.MIN_VALUE;

        for (BlockPos pos : blocksToDetach) {
            minX =
                    Math.min(
                            minX,
                            pos.getX()
                    );

            minY =
                    Math.min(
                            minY,
                            pos.getY()
                    );

            minZ =
                    Math.min(
                            minZ,
                            pos.getZ()
                    );

            maxX =
                    Math.max(
                            maxX,
                            pos.getX()
                    );

            maxY =
                    Math.max(
                            maxY,
                            pos.getY()
                    );

            maxZ =
                    Math.max(
                            maxZ,
                            pos.getZ()
                    );
        }

        BoundingBox3i detachedBounds =
                new BoundingBox3i(
                        minX - 1,
                        minY - 1,
                        minZ - 1,
                        maxX + 1,
                        maxY + 1,
                        maxZ + 1
                );

        BlockPos assemblyOrigin =
                blocksToDetach.getFirst();

        DETACHING_HEAD_PAYLOAD.set(
                true
        );

        try {
            SubLevelAssemblyHelper
                    .assembleBlocks(
                            level,
                            assemblyOrigin,
                            blocksToDetach,
                            detachedBounds
                    );
        } finally {
            DETACHING_HEAD_PAYLOAD.set(
                    false
            );
        }
    }


    private static void processPendingHeadPayloadMerge(
            ServerLevel level,
            MechanicalJackBlockEntity jack,
            HeadKey currentKey
    ) {
        if (currentKey == null) {
            return;
        }

        PendingHeadPayloadMerge pendingMerge =
                PENDING_HEAD_PAYLOAD_MERGES.remove(
                        currentKey
                );

        if (pendingMerge == null) {
            return;
        }

        ServerSubLevel mergedHeadSubLevel =
                mergeSplitHeadPayload(
                        level,
                        pendingMerge.oldKey(),
                        pendingMerge.newKey()
                );

        if (mergedHeadSubLevel == null) {
            return;
        }

        BlockPos mergedHeadPos =
                isHeadBlockInSubLevel(
                        mergedHeadSubLevel,
                        pendingMerge.newKey().headPos()
                )
                        ? pendingMerge.newKey().headPos()
                        .immutable()
                        : findHeadBlockPos(
                        mergedHeadSubLevel
                );

        if (mergedHeadPos == null) {
            return;
        }

        HeadKey mergedKey =
                new HeadKey(
                        mergedHeadSubLevel.getUniqueId(),
                        mergedHeadPos
                );

        removeRuntime(
                pendingMerge.newKey()
        );

        REBINDING_JACKS.add(
                mergedKey
        );

        jack.setHeadSubLevelId(
                mergedKey.subLevelId()
        );

        jack.setHeadBlockPos(
                mergedKey.headPos()
        );

        jack.requestKineticReattach();
        jack.setChanged();
        jack.sendData();
    }

    private static ServerSubLevel mergeSplitHeadPayload(
            ServerLevel level,
            HeadKey oldKey,
            HeadKey newKey
    ) {
        if (level == null
                || oldKey == null
                || newKey == null
                || newKey.headPos() == null) {
            return null;
        }

        dev.ryanhcode.sable.api.sublevel.SubLevelContainer container =
                dev.ryanhcode.sable.api.sublevel.SubLevelContainer
                        .getContainer(
                                level
                        );

        if (container == null) {
            return null;
        }

        java.util.List<ServerSubLevel> splitSubLevels =
                new java.util.ArrayList<>();

        for (dev.ryanhcode.sable.sublevel.SubLevel subLevel :
                container.getAllSubLevels()) {

            if (!(subLevel instanceof ServerSubLevel splitSubLevel)
                    || splitSubLevel.isRemoved()
                    || splitSubLevel.getUniqueId()
                    .equals(
                            newKey.subLevelId()
                    )) {
                continue;
            }

            if (!oldKey.subLevelId()
                    .equals(
                            splitSubLevel.getSplitFromSubLevel()
                    )) {
                continue;
            }

            splitSubLevels.add(
                    splitSubLevel
            );
        }

        if (splitSubLevels.isEmpty()) {
            return null;
        }

        java.util.List<BlockPos> blocksToMerge =
                new java.util.ArrayList<>();

        blocksToMerge.add(
                newKey.headPos()
                        .immutable()
        );

        int minX =
                newKey.headPos()
                        .getX();
        int minY =
                newKey.headPos()
                        .getY();
        int minZ =
                newKey.headPos()
                        .getZ();
        int maxX =
                minX;
        int maxY =
                minY;
        int maxZ =
                minZ;

        for (ServerSubLevel splitSubLevel : splitSubLevels) {
            var bounds =
                    splitSubLevel
                            .getPlot()
                            .getBoundingBox();

            for (BlockPos pos :
                    BlockPos.betweenClosed(
                            bounds.minX(),
                            bounds.minY(),
                            bounds.minZ(),
                            bounds.maxX(),
                            bounds.maxY(),
                            bounds.maxZ()
                    )) {

                BlockPos immutable =
                        pos.immutable();

                BlockState state =
                        level.getBlockState(
                                immutable
                        );

                if (state.isAir()) {
                    continue;
                }

                UUID containingId =
                        dev.createmechanicaldrive.compat.SableSubLevelHelper
                                .getSubLevelId(
                                        level,
                                        immutable
                                );

                if (!splitSubLevel.getUniqueId()
                        .equals(
                                containingId
                        )) {
                    continue;
                }

                blocksToMerge.add(
                        immutable
                );

                minX =
                        Math.min(
                                minX,
                                immutable.getX()
                        );
                minY =
                        Math.min(
                                minY,
                                immutable.getY()
                        );
                minZ =
                        Math.min(
                                minZ,
                                immutable.getZ()
                        );
                maxX =
                        Math.max(
                                maxX,
                                immutable.getX()
                        );
                maxY =
                        Math.max(
                                maxY,
                                immutable.getY()
                        );
                maxZ =
                        Math.max(
                                maxZ,
                                immutable.getZ()
                        );
            }
        }

        if (blocksToMerge.size() <= 1) {
            return null;
        }

        BoundingBox3i mergedBounds =
                new BoundingBox3i(
                        minX - 1,
                        minY - 1,
                        minZ - 1,
                        maxX + 1,
                        maxY + 1,
                        maxZ + 1
                );

        MERGING_HEAD_PAYLOAD.set(
                true
        );

        try {
            return SubLevelAssemblyHelper
                    .assembleBlocks(
                            level,
                            newKey.headPos(),
                            blocksToMerge,
                            mergedBounds
                    );
        } finally {
            MERGING_HEAD_PAYLOAD.set(
                    false
            );
        }
    }

    private static void removeHeadSubLevel(
            ServerLevel rootLevel,
            ServerSubLevel headSubLevel
    ) {
        if (headSubLevel == null
                || headSubLevel.isRemoved()) {
            return;
        }

        dev.ryanhcode.sable.api.sublevel.SubLevelContainer container =
                dev.ryanhcode.sable.api.sublevel.SubLevelContainer
                        .getContainer(
                                rootLevel
                        );

        if (container == null) {
            return;
        }

        container.removeSubLevel(
                headSubLevel,
                SubLevelRemovalReason.REMOVED
        );
    }

    public static void removeFromBase(
            MechanicalJackBlockEntity jack
    ) {
        if (!(jack.getLevel() instanceof ServerLevel rootLevel)) {
            return;
        }

        UUID runtimeId =
                jack.getHeadSubLevelId();

        if (runtimeId == null) {
            return;
        }

        if (!REMOVING_JACKS.add(runtimeId)) {
            return;
        }

        try {
            HeadKey runtimeKey =
                    getRuntimeKey(
                            jack
                    );

            RuntimeJack runtime =
                    removeRuntime(
                            runtimeKey
                    );

            if (runtime == null) {
                java.util.Map.Entry<HeadKey, RuntimeJack> entry =
                        findRuntimeEntry(
                                runtimeId,
                                jack.getHeadBlockPos()
                        );

                if (entry != null) {
                    runtimeKey =
                            entry.getKey();

                    runtime =
                            RUNTIME_JACKS.remove(
                                    runtimeKey
                            );
                }
            }

            ServerSubLevel headSubLevel =
                    runtime != null
                            ? runtime.head()
                            : findSubLevel(
                            rootLevel,
                            runtimeId
                    );

            if (runtime != null
                    && runtime.constraint() != null
                    && runtime.constraint().isValid()) {

                runtime.constraint().remove();
            }

            if (headSubLevel != null
                    && !headSubLevel.isRemoved()) {

                BlockPos headPos =
                        getKnownHeadBlockPos(
                                jack,
                                headSubLevel
                        );

                if (headPos == null) {
                    return;
                }

                boolean sharedHeadSubLevel =
                        hasOtherHeadBlock(
                                headSubLevel,
                                headPos
                        );

                if (!sharedHeadSubLevel) {
                    detachBlocksFromHead(
                            headSubLevel,
                            headPos
                    );
                }

                if (headSubLevel.getLevel()
                        instanceof ServerLevel headLevel) {

                    headLevel.removeBlock(
                            headPos,
                            false
                    );
                }

                if (!sharedHeadSubLevel) {
                    removeHeadSubLevel(
                            rootLevel,
                            headSubLevel
                    );
                }
            }
        } finally {
            REMOVING_JACKS.remove(runtimeId);
        }
    }

    public static void removeFromHead(
            ServerLevel rootLevel,
            BlockPos headPos
    ) {
        removeFromHead(
                rootLevel,
                headPos,
                true
        );
    }

    public static void removeFromHead(
            ServerLevel rootLevel,
            BlockPos headPos,
            boolean dropBaseItem
    ) {
        Object containing =
                dev.createmechanicaldrive.compat.SableSubLevelHelper
                        .getSubLevel(
                                rootLevel,
                                headPos
                        );

        if (!(containing instanceof ServerSubLevel headSubLevel)) {
            return;
        }

        BlockPos actualHeadPos =
                rootLevel.getBlockState(
                        headPos
                ).is(
                        CreateMechanicalDrive
                                .MECHANICAL_JACK_HEAD
                                .get()
                )
                        ? headPos.immutable()
                        : findHeadBlockPos(
                        headSubLevel
                );

        if (actualHeadPos == null) {
            actualHeadPos =
                    headPos.immutable();
        }

        UUID runtimeId =
                headSubLevel.getUniqueId();

        HeadKey runtimeKey =
                new HeadKey(
                        runtimeId,
                        actualHeadPos
                );

        RuntimeJack foundRuntime =
                RUNTIME_JACKS.get(
                        runtimeKey
                );

        if (foundRuntime == null) {
            java.util.Map.Entry<HeadKey, RuntimeJack> entry =
                    findRuntimeEntry(
                            runtimeId,
                            actualHeadPos
                    );

            if (entry != null) {
                runtimeKey =
                        entry.getKey();

                foundRuntime =
                        entry.getValue();
            }
        }

        MechanicalJackBlockEntity owningJack =
                findJackForHead(
                        rootLevel,
                        runtimeId,
                        actualHeadPos
                );

        if (foundRuntime == null
                && owningJack == null) {
            return;
        }

        if (!REMOVING_JACKS.add(runtimeId)) {
            return;
        }

        try {
            RUNTIME_JACKS.remove(
                    runtimeKey
            );

            if (foundRuntime != null
                    && foundRuntime.constraint() != null
                    && foundRuntime.constraint().isValid()) {

                foundRuntime.constraint().remove();
            }

            boolean sharedHeadSubLevel =
                    hasOtherHeadBlock(
                            headSubLevel,
                            actualHeadPos
                    );

            if (!sharedHeadSubLevel) {
                detachBlocksFromHead(
                        headSubLevel,
                        actualHeadPos
                );
            }

            if (headSubLevel.getLevel()
                    instanceof ServerLevel headLevel) {

                headLevel.removeBlock(
                        actualHeadPos,
                        false
                );
            }

            ServerSubLevel baseSubLevel;
            BlockPos basePos;

            if (foundRuntime != null) {
                baseSubLevel =
                        foundRuntime.base();

                Vector3d baseAnchor =
                        foundRuntime.baseAnchor();

                basePos =
                        BlockPos.containing(
                                baseAnchor.x,
                                baseAnchor.y,
                                baseAnchor.z
                        );
            } else {
                baseSubLevel =
                        resolveBaseSubLevel(
                                rootLevel,
                                owningJack.getBlockPos()
                        );

                basePos =
                        owningJack.getBlockPos();
            }

            if (baseSubLevel != null
                    && baseSubLevel.getLevel()
                    instanceof ServerLevel baseLevel) {

                Vec3 baseWorldPosition =
                        baseSubLevel
                                .logicalPose()
                                .transformPosition(
                                        Vec3.atCenterOf(
                                                basePos
                                        )
                                );

                baseLevel.removeBlock(
                        basePos,
                        false
                );

                if (dropBaseItem) {
                    Block.popResource(
                            rootLevel,
                            BlockPos.containing(
                                    baseWorldPosition
                            ),
                            new net.minecraft.world.item.ItemStack(
                                    CreateMechanicalDrive
                                            .MECHANICAL_JACK_ITEM
                                            .get()
                            )
                    );
                }
            }

            if (!sharedHeadSubLevel) {
                removeHeadSubLevel(
                        rootLevel,
                        headSubLevel
                );
            }
        } finally {
            REMOVING_JACKS.remove(
                    runtimeId
            );
        }
    }

    private static MechanicalJackBlockEntity findJackForRuntime(
            RuntimeJack runtime
    ) {
        if (runtime == null
                || runtime.base() == null
                || runtime.base().isRemoved()
                || !(runtime.base().getLevel()
                instanceof ServerLevel level)) {
            return null;
        }

        Vector3d baseAnchor =
                runtime.baseAnchor();

        BlockPos basePos =
                BlockPos.containing(
                        baseAnchor.x,
                        baseAnchor.y,
                        baseAnchor.z
                );

        if (level.getBlockEntity(
                basePos
        ) instanceof MechanicalJackBlockEntity jack) {
            return jack;
        }

        return null;
    }
    private static MechanicalJackBlockEntity findJackForHead(
            ServerLevel level,
            UUID headId,
            BlockPos headPos
    ) {
        dev.ryanhcode.sable.api.sublevel.SubLevelContainer container =
                dev.ryanhcode.sable.api.sublevel.SubLevelContainer
                        .getContainer(
                                level
                        );

        if (container == null) {
            return null;
        }

        for (ServerSubLevel subLevel :
                RUNTIME_JACKS.values()
                        .stream()
                        .map(RuntimeJack::base)
                        .distinct()
                        .toList()) {

            if (subLevel == null
                    || subLevel.isRemoved()) {
                continue;
            }

            var bounds =
                    subLevel
                            .getPlot()
                            .getBoundingBox();

            for (BlockPos pos :
                    BlockPos.betweenClosed(
                            bounds.minX(),
                            bounds.minY(),
                            bounds.minZ(),
                            bounds.maxX(),
                            bounds.maxY(),
                            bounds.maxZ()
                    )) {

                if (!(level.getBlockEntity(pos)
                        instanceof MechanicalJackBlockEntity jack)) {
                    continue;
                }

                if (!headId.equals(
                        jack.getHeadSubLevelId()
                )) {
                    continue;
                }

                if (headPos == null
                        || headPos.equals(
                        jack.getHeadBlockPos()
                )) {
                    return jack;
                }
            }
        }

        if (headPos != null) {
            return findJackForHead(
                    level,
                    headId,
                    null
            );
        }

        return null;
    }
    private static ServerSubLevel findSubLevel(
            ServerLevel level,
            UUID id
    ) {
        dev.ryanhcode.sable.api.sublevel.SubLevelContainer container =
                dev.ryanhcode.sable.api.sublevel.SubLevelContainer
                        .getContainer(
                                level
                        );

        if (container == null) {
            return null;
        }

        dev.ryanhcode.sable.sublevel.SubLevel subLevel =
                container.getSubLevel(
                        id
                );

        if (subLevel
                instanceof ServerSubLevel serverSubLevel) {

            return serverSubLevel;
        }

        return null;
    }

    private record HeadKey(
            UUID subLevelId,
            BlockPos headPos
    ) {
        private HeadKey {
            headPos =
                    headPos == null
                            ? null
                            : headPos.immutable();
        }
    }

    private record MovingHead(
            HeadKey key,
            MechanicalJackBlockEntity jack
    ) {
    }

    private record PendingHeadPayloadMerge(
            HeadKey oldKey,
            HeadKey newKey
    ) {
    }

    private record BasePayloadKey(
            UUID baseSubLevelId,
            BlockPos jackPos
    ) {
        private BasePayloadKey {
            jackPos =
                    jackPos.immutable();
        }
    }

    private record HeadAssembly(
            ServerSubLevel headSubLevel,
            BlockPos headPos
    ) {
        private HeadAssembly {
            headPos =
                    headPos.immutable();
        }
    }

    private record PendingBasePayload(
            Direction mount,
            java.util.Set<BlockPos> relativePayloadPositions
    ) {
        private PendingBasePayload {
            relativePayloadPositions =
                    java.util.Set.copyOf(
                            relativePayloadPositions
                    );
        }
    }

    private record RuntimeJack(
            ServerSubLevel base,
            ServerSubLevel head,
            GenericConstraintHandle constraint,
            Vector3d baseAnchor,
            Vector3d headAnchor,
            Direction mount
    ) {

        private boolean isValid() {
            return !base.isRemoved()
                    && !head.isRemoved()
                    && constraint != null
                    && constraint.isValid();
        }
    }
}