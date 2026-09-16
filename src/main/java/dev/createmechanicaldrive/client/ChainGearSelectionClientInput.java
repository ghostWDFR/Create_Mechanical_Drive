package dev.createmechanicaldrive.client;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.CreateMechanicalDriveClient;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.createmechanicaldrive.content.chain_linkage.ChainGearBlock;
import dev.createmechanicaldrive.content.chain_linkage.ChainGearBlockEntity;
import dev.createmechanicaldrive.content.chain_linkage.ChainLinkageValidator;
import dev.createmechanicaldrive.network.PlaceChainLinkagePayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ChainGearSelectionClientInput {
    private static final List<BlockPos> SELECTED =
            new ArrayList<>();

    private static ResourceKey<Level> selectedLevel;
    private static UUID selectedSubLevel;
    private static Direction.Axis selectedAxis;
    private static int selectedPlane;

    private ChainGearSelectionClientInput() {
    }

    public static void onClickInput(
            InputEvent.InteractionKeyMappingTriggered event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.screen != null
                || event.getKeyMapping() != minecraft.options.keyUse) {
            return;
        }

        if (handleRightClick(event)) {
            event.setCanceled(true);
        }
    }

    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        if (SELECTED.isEmpty()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null
                || findChainHand() == null) {
            clearSelection();
        }
    }

    public static void onRenderLevelStage(
            RenderLevelStageEvent event
    ) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES
                || SELECTED.isEmpty()
                || selectedAxis == null
                || findChainHand() == null) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null
                || selectedLevel == null
                || !selectedLevel.equals(minecraft.level.dimension())) {
            return;
        }

        List<BlockPos> preview =
                buildPreviewPositions(
                        minecraft
                );

        if (preview.size() < ChainLinkageValidator.MIN_GEARS) {
            return;
        }

        BakedModel linkModel =
                minecraft
                        .getModelManager()
                        .getModel(
                                CreateMechanicalDriveClient
                                        .CHAIN_LINKAGE_MODEL
                        );

        BlockRenderDispatcher blockRenderer =
                minecraft.getBlockRenderer();

        MultiBufferSource.BufferSource buffer =
                minecraft.renderBuffers()
                        .bufferSource();

        Vec3 camera =
                event.getCamera()
                        .getPosition();

        PoseStack poseStack =
                event.getPoseStack();

        poseStack.pushPose();

        boolean flexiblePreview =
                isFlexiblePreview(
                        minecraft.level,
                        preview
                );

        Vec3 origin;

        if (flexiblePreview) {
            poseStack.translate(
                    -camera.x,
                    -camera.y,
                    -camera.z
            );

            origin =
                    Vec3.ZERO;
        } else {
            origin =
                    SableContraptionDragTransform.applyRenderTransform(
                            minecraft.level,
                            preview.getFirst(),
                            camera,
                            poseStack
                    );

            if (origin == null) {
                poseStack.translate(
                        -camera.x,
                        -camera.y,
                        -camera.z
                );

                origin =
                        Vec3.ZERO;
            }
        }


        boolean validPreview =
                hasEnoughChains(
                        minecraft,
                        preview
                );

        if (flexiblePreview) {
            ChainLinkageRenderHelper.renderFlexibleGhostLoop(
                    blockRenderer,
                    linkModel,
                    minecraft.level,
                    preview,
                    selectedAxis,
                    origin,
                    poseStack,
                    buffer,
                    LightTexture.FULL_BRIGHT,
                    0,
                    validPreview
            );
        } else {
            ChainLinkageRenderHelper.renderGhostLoop(
                    blockRenderer,
                    linkModel,
                    preview,
                    selectedAxis,
                    origin,
                    poseStack,
                    buffer,
                    LightTexture.FULL_BRIGHT,
                    0,
                    validPreview
            );
        }

        poseStack.popPose();

        buffer.endBatch(
                RenderType.debugQuads()
        );
    }

    private static boolean handleRightClick(
            InputEvent.InteractionKeyMappingTriggered event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {
            return false;
        }

        InteractionHand hand =
                findChainHand();

        if (hand == null) {
            return !SELECTED.isEmpty();
        }

        if (minecraft.player.isShiftKeyDown()) {
            clearSelection();
            return true;
        }

        if (!(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return !SELECTED.isEmpty();
        }

        BlockPos pos =
                hit.getBlockPos();

        BlockState state =
                minecraft.level.getBlockState(pos);

        if (!state.is(CreateMechanicalDrive.CHAIN_GEAR.get())) {
            return !SELECTED.isEmpty();
        }

        event.setSwingHand(true);

        Direction.Axis axis =
                state.getValue(
                        ChainGearBlock.AXIS
                );

        String existingChainFailure =
                getExistingChainFailure(
                        minecraft.level,
                        pos
                );

        if (existingChainFailure != null) {
            fail(
                    existingChainFailure
            );

            return true;
        }

        if (SELECTED.isEmpty()) {
            startSelection(
                    minecraft.level,
                    pos,
                    axis
            );

            display(
                    ChatFormatting.GOLD,
                    "message.mechanical_drive.chain_linkage.started"
            );

            return true;
        }

        if (selectedLevel == null
                || !selectedLevel.equals(minecraft.level.dimension())) {
            clearSelection();
            startSelection(
                    minecraft.level,
                    pos,
                    axis
            );

            display(
                    ChatFormatting.GOLD,
                    "message.mechanical_drive.chain_linkage.started"
            );

            return true;
        }

        String selectionFailure =
                getSelectionFailure(
                        minecraft.level,
                        pos,
                        axis
                );

        if (selectionFailure != null) {
            fail(selectionFailure);
            return true;
        }

        if (SELECTED.size() > 1
                && pos.equals(SELECTED.getLast())) {
            SELECTED.removeLast();
            return true;
        }

        if (pos.equals(SELECTED.getFirst())) {
            closeLoop();
            return true;
        }

        if (SELECTED.contains(pos)) {
            fail(
                    "duplicate_gear"
            );

            return true;
        }

        List<BlockPos> next =
                new ArrayList<>(SELECTED);

        next.add(
                pos
        );

        String failure =
                getLoopFailure(
                        minecraft.level,
                        next
                );

        if (failure != null
                && !"too_short".equals(failure)) {
            fail(
                    failure
            );

            return true;
        }

        SELECTED.add(
                pos
        );

        display(
                ChatFormatting.AQUA,
                "message.mechanical_drive.chain_linkage.added",
                SELECTED.size()
        );

        return true;
    }

    private static List<BlockPos> buildPreviewPositions(
            Minecraft minecraft
    ) {
        List<BlockPos> preview =
                new ArrayList<>(SELECTED);

        if (!(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || minecraft.level == null) {
            return preview;
        }

        BlockPos pos =
                hit.getBlockPos();

        if (preview.contains(pos)) {
            return preview;
        }

        BlockState state =
                minecraft.level.getBlockState(pos);

        if (!state.is(CreateMechanicalDrive.CHAIN_GEAR.get())) {
            return preview;
        }

        Direction.Axis axis =
                state.getValue(
                        ChainGearBlock.AXIS
                );

        if (getSelectionFailure(
                minecraft.level,
                pos,
                axis
        ) != null) {
            return preview;
        }

        preview.add(pos);

        String failure =
                getLoopFailure(
                        minecraft.level,
                        preview
                );

        if (failure != null
                && !"too_short".equals(failure)) {
            preview.removeLast();
        }

        return preview;
    }

    private static void closeLoop() {
        Minecraft minecraft =
                Minecraft.getInstance();

        String failure =
                getLoopFailure(
                        minecraft.level,
                        SELECTED
                );

        if (failure != null) {
            fail(
                    failure
            );

            return;
        }

        if (!isValidSubLevelSelection(
                Minecraft.getInstance()
        )) {
            fail(
                    "not_same_sublevel"
            );

            return;
        }

        if (!hasEnoughChains(
                minecraft,
                SELECTED
        )) {
            return;
        }

        List<PlaceChainLinkagePayload.Endpoint> endpoints =
                new ArrayList<>(
                        SELECTED.size()
                );

        for (BlockPos pos : SELECTED) {
            UUID subLevelId =
                    SableSubLevelHelper.getSubLevelId(
                            minecraft.level,
                            pos
                    );

            BlockPos transferPosition =
                    SableSubLevelHelper.toTransferPosition(
                            minecraft.level,
                            pos
                    );

            if (transferPosition == null) {
                CreateMechanicalDrive.LOGGER.warn(
                        "Unable to encode chain linkage endpoint for Sable: "
                                + "pos={}, subLevel={}",
                        pos,
                        subLevelId
                );

                fail(
                        "invalid_target"
                );

                return;
            }

            endpoints.add(
                    new PlaceChainLinkagePayload.Endpoint(
                            transferPosition,
                            subLevelId
                    )
            );
        }

        PacketDistributor.sendToServer(
                new PlaceChainLinkagePayload(
                        List.copyOf(endpoints)
                )
        );

        clearSelection();
    }

    private static void startSelection(
            Level level,
            BlockPos pos,
            Direction.Axis axis
    ) {
        SELECTED.clear();
        SELECTED.add(pos);
        selectedLevel =
                level.dimension();
        selectedSubLevel =
                SableSubLevelHelper.getSubLevelId(
                        level,
                        pos
                );
        selectedAxis =
                axis;
        selectedPlane =
                pos.get(axis);
    }

    private static boolean isSelectedSubLevel(
            Level level,
            BlockPos pos
    ) {
        return Objects.equals(
                selectedSubLevel,
                SableSubLevelHelper.getSubLevelId(
                        level,
                        pos
                )
        );
    }

    private static boolean isValidSubLevelSelection(
            Minecraft minecraft
    ) {
        if (minecraft.level == null
                || SELECTED.isEmpty()) {
            return true;
        }

        if (SELECTED.size() == 2
                && !allSelectedOnSameSubLevel(
                minecraft.level
        )) {
            return getFlexibleFailure(
                    minecraft.level,
                    SELECTED
            ) == null;
        }

        return allSelectedOnSameSubLevel(
                minecraft.level
        );
    }

    private static boolean allSelectedOnSameSubLevel(
            Level level
    ) {
        for (BlockPos pos : SELECTED) {
            if (!isSelectedSubLevel(
                    level,
                    pos
            )) {
                return false;
            }
        }

        return true;
    }

    private static String getSelectionFailure(
            Level level,
            BlockPos pos,
            Direction.Axis axis
    ) {
        String existingChainFailure =
                getExistingChainFailure(
                        level,
                        pos
                );

        if (existingChainFailure != null) {
            return existingChainFailure;
        }

        if (axis != selectedAxis) {
            return "not_planar";
        }

        if (isSelectedSubLevel(
                level,
                pos
        )) {
            return pos.get(axis) == selectedPlane
                    ? null
                    : "not_planar";
        }

        if (SELECTED.size() != 1) {
            return "not_same_sublevel";
        }

        List<BlockPos> next =
                new ArrayList<>(SELECTED);

        next.add(pos);

        String failure =
                getFlexibleFailure(
                        level,
                        next
                );

        return failure == null
                ? null
                : failure;
    }

    private static String getLoopFailure(
            Level level,
            List<BlockPos> positions
    ) {
        for (BlockPos pos : positions) {
            String existingChainFailure =
                    getExistingChainFailure(
                            level,
                            pos
                    );

            if (existingChainFailure != null) {
                return existingChainFailure;
            }
        }

        if (positions.size() == 2
                && !sameSubLevel(
                level,
                positions
        )) {
            return getFlexibleFailure(
                    level,
                    positions
            );
        }

        if (!allSameSubLevel(
                level,
                positions
        )) {
            return "not_same_sublevel";
        }

        return ChainLinkageValidator.getFailureKey(
                positions,
                selectedAxis
        );
    }

    private static String getFlexibleFailure(
            Level level,
            List<BlockPos> positions
    ) {
        double initialCenterDistance =
                ChainLinkageValidator.getFlexibleCenterDistance(
                        level,
                        positions,
                        selectedAxis
                );

        return ChainLinkageValidator.getFlexibleTwoGearFailureKey(
                level,
                positions,
                selectedAxis,
                initialCenterDistance,
                ChainLinkageValidator.getTwoGearLoopLength(
                        initialCenterDistance
                )
        );
    }

    private static String getExistingChainFailure(
            Level level,
            BlockPos pos
    ) {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        return blockEntity instanceof ChainGearBlockEntity gear
                && gear.hasChainLoop()
                ? "already_linked"
                : null;
    }

    private static boolean hasEnoughChains(
            Minecraft minecraft,
            List<BlockPos> positions
    ) {
        if (minecraft.player == null
                || minecraft.player.hasInfiniteMaterials()) {
            return true;
        }

        int required =
                getRequiredChains(
                        minecraft.level,
                        positions
                );

        int available =
                0;

        for (ItemStack stack : minecraft.player.getInventory().items) {
            if (stack.is(CreateMechanicalDrive.CHAIN_LINKAGE_ITEM.get())) {
                available += stack.getCount();
            }
        }

        for (ItemStack stack : minecraft.player.getInventory().offhand) {
            if (stack.is(CreateMechanicalDrive.CHAIN_LINKAGE_ITEM.get())) {
                available += stack.getCount();
            }
        }

        return available >= required;
    }

    private static int getRequiredChains(
            Level level,
            List<BlockPos> positions
    ) {
        if (level != null
                && positions.size() == 2
                && !sameSubLevel(
                level,
                positions
        )) {
            double centerDistance =
                    ChainLinkageValidator.getFlexibleCenterDistance(
                            level,
                            positions,
                            selectedAxis
                    );

            return ChainLinkageValidator.getChainsRequired(
                    ChainLinkageValidator.getTwoGearLoopLength(
                            centerDistance
                    )
            );
        }

        return ChainLinkageValidator.getChainsRequired(
                positions
        );
    }

    private static boolean sameSubLevel(
            Level level,
            List<BlockPos> positions
    ) {
        if (positions.size() != 2) {
            return true;
        }

        return Objects.equals(
                SableSubLevelHelper.getSubLevelId(
                        level,
                        positions.getFirst()
                ),
                SableSubLevelHelper.getSubLevelId(
                        level,
                        positions.getLast()
                )
        );
    }

    private static boolean isFlexiblePreview(
            Level level,
            List<BlockPos> positions
    ) {
        return positions.size() == 2
                && !sameSubLevel(
                level,
                positions
        );
    }

    private static boolean allSameSubLevel(
            Level level,
            List<BlockPos> positions
    ) {
        if (positions.isEmpty()) {
            return true;
        }

        UUID first =
                SableSubLevelHelper.getSubLevelId(
                        level,
                        positions.getFirst()
                );

        for (BlockPos pos : positions) {
            if (!Objects.equals(
                    first,
                    SableSubLevelHelper.getSubLevelId(
                            level,
                            pos
                    )
            )) {
                return false;
            }
        }

        return true;
    }

    private static InteractionHand findChainHand() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return null;
        }

        ItemStack mainHand =
                minecraft.player.getMainHandItem();

        if (mainHand.is(CreateMechanicalDrive.CHAIN_LINKAGE_ITEM.get())) {
            return InteractionHand.MAIN_HAND;
        }

        ItemStack offHand =
                minecraft.player.getOffhandItem();

        if (offHand.is(CreateMechanicalDrive.CHAIN_LINKAGE_ITEM.get())) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    private static void fail(
            String failure
    ) {
        display(
                ChatFormatting.RED,
                "message.mechanical_drive.chain_linkage.failed." + failure
        );
    }

    private static void display(
            ChatFormatting formatting,
            String key,
            Object... args
    ) {
        ClientMessages.actionBar(
                formatting,
                key,
                args
        );
    }

    private static void clearSelection() {
        SELECTED.clear();
        selectedLevel =
                null;
        selectedSubLevel =
                null;
        selectedAxis =
                null;
        selectedPlane =
                0;
    }
}
