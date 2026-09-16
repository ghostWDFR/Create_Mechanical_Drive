package dev.createmechanicaldrive.content.mechanical_jack;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MechanicalJackBlockEntity
        extends KineticBlockEntity {

    public static final double MIN_EXTENSION =
            0.0D;

    public static final double MAX_EXTENSION =
            2.0D;

    public static final int MIN_MAX_EXTENSION_BLOCKS =
            1;

    public static final int MAX_MAX_EXTENSION_BLOCKS =
            2;

    public static final int DEFAULT_MAX_EXTENSION_BLOCKS =
            MAX_MAX_EXTENSION_BLOCKS;

    private static final double MAX_TARGET_LEAD =
            0.10D;

    private static final double EXTENSION_PER_RPM_PER_TICK =
            1.0D / 4096.0D;

    private static final String TARGET_EXTENSION_TAG =
            "TargetExtension";

    private static final String ACTUAL_EXTENSION_TAG =
            "ActualExtension";

    private static final String MAX_EXTENSION_TAG =
            "MaxExtension";

    private static final String ASSEMBLED_TAG =
            "JackAssembled";

    private static final String HEAD_UUID_TAG =
            "JackHeadSubLevel";

    private static final String HEAD_POS_TAG =
            "JackHeadBlockPos";

    private static final String FULL_BLOCK_COLLISION_TAG =
            "JackFullBlockCollision";

    private double targetExtension =
            0.0D;

    private double actualExtension =
            0.0D;

    private double previousActualExtension =
            0.0D;

    private double renderExtension =
            0.0D;

    private double previousRenderExtension =
            0.0D;

    private boolean assembled =
            false;

    private boolean needsKineticReattach =
            false;

    private boolean needsHeadInitialization =
            false;

    private boolean fullBlockCollision =
            true;

    private int maxExtensionBlocks =
            DEFAULT_MAX_EXTENSION_BLOCKS;

    private List<ScrollValueBehaviour> maxExtensionBehaviours =
            List.of();

    private boolean syncingMaxExtension =
            false;

    private UUID headSubLevelId;

    private BlockPos headBlockPos;

    public MechanicalJackBlockEntity(
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
    public void addBehaviours(
            List<BlockEntityBehaviour> behaviours
    ) {
        super.addBehaviours(
                behaviours
        );

        maxExtensionBehaviours =
                new ArrayList<>();

        for (
                Direction side
                : Direction.values()
        ) {
            ScrollValueBehaviour behaviour =
                    new MaxExtensionScrollValueBehaviour(
                            Component.translatable(
                                    "mechanical_drive.mechanical_jack.max_extension"
                            ),
                            this,
                            new MaxExtensionValueBoxTransform()
                                    .fromSide(
                                            side
                                    )
                    );

            behaviour
                    .between(
                            MIN_MAX_EXTENSION_BLOCKS,
                            MAX_MAX_EXTENSION_BLOCKS
                    )
                    .withFormatter(
                            ignored -> String.valueOf(
                                    maxExtensionBlocks
                            )
                    )
                    .withCallback(
                            this::setMaxExtensionBlocks
                    );

            behaviour.setValue(
                    maxExtensionBlocks
            );

            maxExtensionBehaviours.add(
                    behaviour
            );

            behaviours.add(
                    behaviour
            );
        }
    }

    public void requestKineticReattach() {
        needsKineticReattach = true;
    }

    public void requestHeadInitialization() {
        needsHeadInitialization = true;
    }

    private void tickRenderExtension() {
        previousRenderExtension =
                renderExtension;

        renderExtension =
                actualExtension;
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            tickRenderExtension();
            return;
        }

        previousActualExtension =
                actualExtension;

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (needsHeadInitialization) {
            needsHeadInitialization = false;

            MechanicalJackPhysics.initializeHead(
                    this,
                    serverLevel
            );

            return;
        }

        if (needsKineticReattach) {
            needsKineticReattach = false;

            detachKinetics();
            attachKinetics();
            sendData();
        }

        Object containingSubLevel =
                dev.createmechanicaldrive.compat.SableSubLevelHelper
                        .getSubLevel(
                                level,
                                worldPosition
                        );

        if (!assembled) {

            if (containingSubLevel
                    instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel) {

                tryAssembleIntoSable();
            } else if (!fullBlockCollision) {

                serverLevel.scheduleTick(
                        worldPosition,
                        getBlockState().getBlock(),
                        1
                );
            }

            return;
        }

        tickTargetExtension();

        MechanicalJackPhysics.tickJack(
                this,
                serverLevel
        );
    }

    private void tickTargetExtension() {
        float speed =
                getSpeed();

        if (Math.abs(speed) < 0.001F) {
            return;
        }

        Direction inputSide =
                MechanicalJackBlock.getInputSide(
                        getBlockState()
                );

        float localSpeed =
                KineticBlockEntity.convertToDirection(
                        speed,
                        inputSide
                );

        double step =
                localSpeed
                        * EXTENSION_PER_RPM_PER_TICK;

        double currentError =
                targetExtension
                        - actualExtension;

        if (currentError * step < 0.0D) {
            targetExtension =
                    actualExtension;
        }

        targetExtension +=
                step;

        targetExtension =
                Mth.clamp(
                        targetExtension,
                        actualExtension - MAX_TARGET_LEAD,
                        actualExtension + MAX_TARGET_LEAD
                );

        targetExtension =
                Mth.clamp(
                        targetExtension,
                        MIN_EXTENSION,
                        getMaxExtension()
                );

        setChanged();
    }

    public void tryAssembleIntoSable() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (assembled) {
            return;
        }

        Object existingSubLevel =
                dev.createmechanicaldrive.compat.SableSubLevelHelper
                        .getSubLevel(
                                level,
                                worldPosition
                        );

        if (existingSubLevel
                instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel) {

            assembled = true;
            fullBlockCollision = false;

            requestKineticReattach();
            requestHeadInitialization();

            setChanged();
            sendData();

            return;
        }

        if (fullBlockCollision) {
            return;
        }

        MechanicalJackPhysics.assembleBase(
                serverLevel,
                worldPosition
        );
    }

    public double getTargetExtension() {
        return targetExtension;
    }

    public double getActualExtension() {
        return actualExtension;
    }

    public int getMaxExtensionBlocks() {
        return maxExtensionBlocks;
    }

    public double getMaxExtension() {
        return maxExtensionBlocks;
    }

    public void setMaxExtensionBlocks(
            int maxExtensionBlocks
    ) {
        int newMaxExtensionBlocks =
                Mth.clamp(
                        maxExtensionBlocks,
                        MIN_MAX_EXTENSION_BLOCKS,
                        MAX_MAX_EXTENSION_BLOCKS
                );

        if (this.maxExtensionBlocks
                == newMaxExtensionBlocks) {

            syncMaxExtensionBehaviours();
            return;
        }

        this.maxExtensionBlocks =
                newMaxExtensionBlocks;

        clampExtensionsToSelectedMax();
        syncMaxExtensionBehaviours();

        setChanged();
        sendData();
    }

    private void clampExtensionsToSelectedMax() {
        double maxExtension =
                getMaxExtension();

        targetExtension =
                Mth.clamp(
                        targetExtension,
                        MIN_EXTENSION,
                        maxExtension
                );

        actualExtension =
                Mth.clamp(
                        actualExtension,
                        MIN_EXTENSION,
                        maxExtension
                );

        previousActualExtension =
                Mth.clamp(
                        previousActualExtension,
                        MIN_EXTENSION,
                        maxExtension
                );

        renderExtension =
                Mth.clamp(
                        renderExtension,
                        MIN_EXTENSION,
                        maxExtension
                );

        previousRenderExtension =
                Mth.clamp(
                        previousRenderExtension,
                        MIN_EXTENSION,
                        maxExtension
                );
    }

    private void syncMaxExtensionBehaviours() {
        if (syncingMaxExtension) {
            return;
        }

        syncingMaxExtension =
                true;

        for (
                ScrollValueBehaviour behaviour
                : maxExtensionBehaviours
        ) {
            if (behaviour.getValue()
                    != maxExtensionBlocks) {

                behaviour.setValue(
                        maxExtensionBlocks
                );
            }
        }

        syncingMaxExtension =
                false;
    }

    public void setTargetExtension(
            double targetExtension
    ) {
        this.targetExtension =
                Mth.clamp(
                        targetExtension,
                        MIN_EXTENSION,
                        getMaxExtension()
                );

        setChanged();
    }

    public void setActualExtension(
            double actualExtension
    ) {
        this.actualExtension =
                Mth.clamp(
                        actualExtension,
                        MIN_EXTENSION,
                        getMaxExtension()
                );

        setChanged();
        sendData();
    }

    public float getInterpolatedExtension(
            float partialTick
    ) {
        return (float) Mth.lerp(
                partialTick,
                previousRenderExtension,
                renderExtension
        );
    }

    public boolean isAssembled() {
        return assembled;
    }

    public void setAssembled(
            boolean assembled
    ) {
        this.assembled =
                assembled;

        setChanged();
    }

    public boolean hasFullBlockCollision() {
        return fullBlockCollision;
    }

    public void setFullBlockCollision(
            boolean fullBlockCollision
    ) {
        if (this.fullBlockCollision == fullBlockCollision) {
            return;
        }

        this.fullBlockCollision =
                fullBlockCollision;

        setChanged();
    }

    public UUID getHeadSubLevelId() {
        return headSubLevelId;
    }

    public void setHeadSubLevelId(
            UUID headSubLevelId
    ) {
        this.headSubLevelId =
                headSubLevelId;

        setChanged();
    }

    public BlockPos getHeadBlockPos() {
        return headBlockPos;
    }

    public void setHeadBlockPos(
            BlockPos headBlockPos
    ) {
        this.headBlockPos =
                headBlockPos == null
                        ? null
                        : headBlockPos.immutable();

        setChanged();
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

        tag.putDouble(
                TARGET_EXTENSION_TAG,
                targetExtension
        );

        tag.putDouble(
                ACTUAL_EXTENSION_TAG,
                actualExtension
        );

        tag.putInt(
                MAX_EXTENSION_TAG,
                maxExtensionBlocks
        );

        tag.putBoolean(
                ASSEMBLED_TAG,
                assembled
        );

        tag.putBoolean(
                FULL_BLOCK_COLLISION_TAG,
                fullBlockCollision
        );

        if (headSubLevelId != null) {
            tag.putUUID(
                    HEAD_UUID_TAG,
                    headSubLevelId
            );
        }

        if (headBlockPos != null) {
            tag.putLong(
                    HEAD_POS_TAG,
                    headBlockPos.asLong()
            );
        }
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

        maxExtensionBlocks =
                Mth.clamp(
                        tag.contains(
                                MAX_EXTENSION_TAG
                        )
                                ? tag.getInt(
                                MAX_EXTENSION_TAG
                        )
                                : DEFAULT_MAX_EXTENSION_BLOCKS,
                        MIN_MAX_EXTENSION_BLOCKS,
                        MAX_MAX_EXTENSION_BLOCKS
                );

        targetExtension =
                Mth.clamp(
                        tag.getDouble(
                                TARGET_EXTENSION_TAG
                        ),
                        MIN_EXTENSION,
                        getMaxExtension()
                );

        double newActualExtension =
                Mth.clamp(
                        tag.getDouble(
                                ACTUAL_EXTENSION_TAG
                        ),
                        MIN_EXTENSION,
                        getMaxExtension()
                );

        if (clientPacket) {
            previousActualExtension =
                    actualExtension;

            actualExtension =
                    newActualExtension;

            if (renderExtension == 0.0D
                    && previousRenderExtension == 0.0D) {

                renderExtension =
                        newActualExtension;

                previousRenderExtension =
                        newActualExtension;
            }
        } else {
            actualExtension =
                    newActualExtension;

            previousActualExtension =
                    newActualExtension;

            renderExtension =
                    newActualExtension;

            previousRenderExtension =
                    newActualExtension;
        }

        assembled =
                tag.getBoolean(
                        ASSEMBLED_TAG
                );

        fullBlockCollision =
                tag.contains(
                        FULL_BLOCK_COLLISION_TAG
                )
                        ? tag.getBoolean(
                        FULL_BLOCK_COLLISION_TAG
                )
                        : !assembled;

        if (tag.hasUUID(
                HEAD_UUID_TAG
        )) {
            headSubLevelId =
                    tag.getUUID(
                            HEAD_UUID_TAG
                    );
        } else {
            headSubLevelId =
                    null;
        }

        if (tag.contains(
                HEAD_POS_TAG
        )) {
            headBlockPos =
                    BlockPos.of(
                            tag.getLong(
                                    HEAD_POS_TAG
                            )
                    );
        } else {
            headBlockPos =
                    null;
        }

        syncMaxExtensionBehaviours();
    }

    private static class MaxExtensionScrollValueBehaviour
            extends ScrollValueBehaviour {

        private final MechanicalJackBlockEntity blockEntity;

        public MaxExtensionScrollValueBehaviour(
                Component label,
                MechanicalJackBlockEntity blockEntity,
                ValueBoxTransform slot
        ) {
            super(
                    label,
                    blockEntity,
                    slot
            );

            this.blockEntity =
                    blockEntity;
        }

        @Override
        public ValueSettingsBoard createBoard(
                Player player,
                BlockHitResult hitResult
        ) {
            return new ValueSettingsBoard(
                    label,
                    1,
                    1,
                    List.of(
                            Component.translatable(
                                    "mechanical_drive.mechanical_jack.max_extension.board"
                            )
                    ),
                    new ValueSettingsFormatter(
                            settings ->
                                    formatMaxExtensionValue(
                                            settings.value()
                                                    + MIN_MAX_EXTENSION_BLOCKS
                                    )
                    )
            );
        }

        @Override
        public ValueSettings getValueSettings() {
            return new ValueSettings(
                    0,
                    blockEntity.getMaxExtensionBlocks()
                            - MIN_MAX_EXTENSION_BLOCKS
            );
        }

        @Override
        public void setValueSettings(
                Player player,
                ValueSettings valueSetting,
                boolean ctrlDown
        ) {
            int selectedMaxExtension =
                    valueSetting.value()
                            + MIN_MAX_EXTENSION_BLOCKS;

            blockEntity.setMaxExtensionBlocks(
                    selectedMaxExtension
            );

            setValue(
                    blockEntity.getMaxExtensionBlocks()
            );

            playFeedbackSound(
                    this
            );
        }
    }

    private static MutableComponent formatMaxExtensionValue(
            int value
    ) {
        return Component.translatable(
                value <= 1
                        ? "mechanical_drive.mechanical_jack.max_extension.one"
                        : "mechanical_drive.mechanical_jack.max_extension.two"
        );
    }

    private static class MaxExtensionValueBoxTransform
            extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(
                    8.0F,
                    8.0F,
                    15.51F
            );
        }

        @Override
        protected boolean isSideActive(
                BlockState state,
                Direction direction
        ) {
            if (!state.hasProperty(
                    MechanicalJackBlock.MOUNT
            )) {
                return false;
            }

            Direction mount =
                    state.getValue(
                            MechanicalJackBlock.MOUNT
                    );

            if (direction == mount
                    || direction == mount.getOpposite()) {

                return false;
            }

            Direction inputSide =
                    MechanicalJackBlock.getInputSide(
                            state
                    );

            return direction.getAxis()
                    != inputSide.getAxis();
        }

        @Override
        public float getScale() {
            return 0.5F;
        }
    }
}
