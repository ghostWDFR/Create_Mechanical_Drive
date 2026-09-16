package dev.createmechanicaldrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.createmechanicaldrive.content.cardan_shaft.CardanJointBlock;
import dev.createmechanicaldrive.content.cardan_shaft.CardanJointBlockEntity;
import dev.createmechanicaldrive.network.PlaceCardanShaftPayload;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CardanShaftPlacementClientInput {
    private static final double CANCEL_DISTANCE = 7.0D;

    private static BlockPos selectedPos;
    private static Direction selectedFacing;
    private static ResourceKey<Level> selectedLevel;

    private CardanShaftPlacementClientInput() {
    }

    public static void onClickInput(
            InputEvent.InteractionKeyMappingTriggered event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.screen != null) {
            return;
        }

        if (event.isAttack()
                && selectedPos != null) {
            PacketDistributor.sendToServer(
                    PlaceCardanShaftPayload.cancel()
            );
            clearSelection();
            ClientMessages.actionBar(
                    ChatFormatting.GOLD,
                    "message.mechanical_drive.cardan_shaft.cancelled"
            );
            event.setCanceled(true);
            event.setSwingHand(true);
            return;
        }

        if (!event.isUseItem()) {
            return;
        }

        if (handleRightClick()) {
            event.setCanceled(true);
            event.setSwingHand(true);
        }
    }

    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        if (selectedPos == null) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null
                || findCardanHand() == null
                || selectedLevel == null
                || !selectedLevel.equals(minecraft.level.dimension())) {
            if (minecraft.player != null
                    && minecraft.level != null) {
                PacketDistributor.sendToServer(
                        PlaceCardanShaftPayload.cancel()
                );
            }

            clearSelection();
            return;
        }

        if (isSelectionTooFar(minecraft)) {
            PacketDistributor.sendToServer(
                    PlaceCardanShaftPayload.cancel()
            );
            clearSelection();
            ClientMessages.actionBar(
                    ChatFormatting.RED,
                    "message.mechanical_drive.cardan_shaft.failed.invalid_length"
            );
        }
    }

    public static void onRenderLevelStage(
            RenderLevelStageEvent event
    ) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES
                || selectedPos == null
                || findCardanHand() == null) {
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

        PlacementTarget target =
                getPlacementTarget(minecraft);

        if (target == null) {
            return;
        }

        boolean tooLong =
                isTooLongDistance(
                        selectedPos,
                        target.pos()
                );
        boolean invalidBend =
                isInvalidBend(
                        selectedPos,
                        selectedFacing,
                        target.pos(),
                        target.facing()
                );
        boolean invalidPlacement =
                tooLong
                        || invalidBend;

        float red = 1.0F;
        float green =
                invalidPlacement
                        ? 0.2F
                        : 1.0F;
        float blue =
                invalidPlacement
                        ? 0.2F
                        : 1.0F;

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
        poseStack.translate(
                -camera.x,
                -camera.y,
                -camera.z
        );

        Vec3 first =
                SableSubLevelHelper.getWorldCenter(
                        minecraft.level,
                        selectedPos
                );
        Vec3 second =
                SableSubLevelHelper.getWorldCenter(
                        minecraft.level,
                        target.pos()
                );
        Vec3 firstJointDirection =
                SableSubLevelHelper.getWorldNormal(
                        minecraft.level,
                        selectedPos,
                        Vec3.atLowerCornerOf(
                                selectedFacing.getNormal()
                        )
                );
        Vec3 secondJointDirection =
                SableSubLevelHelper.getWorldNormal(
                        minecraft.level,
                        target.pos(),
                        Vec3.atLowerCornerOf(
                                target.facing()
                                        .getNormal()
                        )
                );

        CardanShaftRenderHelper.renderRodBetween(
                blockRenderer,
                first,
                second,
                firstJointDirection,
                secondJointDirection,
                poseStack,
                buffer,
                LightTexture.FULL_BRIGHT,
                0,
                0.0F,
                red,
                green,
                blue
        );

        BlockState previewState =
                CreateMechanicalDrive.CARDAN_JOINT
                        .get()
                        .defaultBlockState()
                        .setValue(
                                CardanJointBlock.FACING,
                                target.facing()
                        );

        poseStack.popPose();
        poseStack.pushPose();

        Vec3 previewOrigin =
                SableContraptionDragTransform.applyRenderTransform(
                        minecraft.level,
                        target.pos(),
                        camera,
                        poseStack
                );

        if (previewOrigin == null) {
            poseStack.translate(
                    target.pos().getX() - camera.x,
                    target.pos().getY() - camera.y,
                    target.pos().getZ() - camera.z
            );
        } else {
            poseStack.translate(
                    target.pos().getX() - previewOrigin.x,
                    target.pos().getY() - previewOrigin.y,
                    target.pos().getZ() - previewOrigin.z
            );
        }

        BakedModel previewModel =
                blockRenderer.getBlockModel(
                        previewState
                );
        RenderType previewRenderType =
                RenderType.cutoutMipped();

        blockRenderer
                .getModelRenderer()
                .renderModel(
                        poseStack.last(),
                        buffer.getBuffer(
                                previewRenderType
                        ),
                        previewState,
                        previewModel,
                        red,
                        green,
                        blue,
                        LightTexture.FULL_BRIGHT,
                        0,
                        ModelData.EMPTY,
                        previewRenderType
                );
        poseStack.popPose();

        buffer.endBatch();

        if (tooLong) {
            ClientMessages.actionBar(
                    ChatFormatting.RED,
                    "message.mechanical_drive.cardan_shaft.failed.invalid_length"
            );
        } else if (invalidBend) {
            ClientMessages.actionBar(
                    ChatFormatting.RED,
                    "message.mechanical_drive.cardan_shaft.failed.invalid_angle"
            );
        }
    }

    private static boolean handleRightClick() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {
            return false;
        }

        if (findCardanHand() == null) {
            return selectedPos != null;
        }

        if (minecraft.player.isShiftKeyDown()
                && selectedPos != null) {
            PacketDistributor.sendToServer(
                    PlaceCardanShaftPayload.cancel()
            );
            clearSelection();
            ClientMessages.actionBar(
                    ChatFormatting.GOLD,
                    "message.mechanical_drive.cardan_shaft.cancelled"
            );
            return true;
        }

        PlacementTarget target =
                getPlacementTarget(minecraft);

        if (target == null) {
            return selectedPos != null;
        }

        if (selectedPos == null) {
            selectedPos =
                    target.pos();
            selectedFacing =
                    target.facing();
            selectedLevel =
                    minecraft.level.dimension();
            PacketDistributor.sendToServer(
                    PlaceCardanShaftPayload.place(
                            target.pos(),
                            target.facing()
                    )
            );
            ClientMessages.actionBar(
                    ChatFormatting.GOLD,
                    "message.mechanical_drive.cardan_shaft.started"
            );
            return true;
        }

        if (!isValidDistance(
                selectedPos,
                target.pos()
        )) {
            ClientMessages.actionBar(
                    ChatFormatting.RED,
                    "message.mechanical_drive.cardan_shaft.failed.invalid_length"
            );
            return true;
        }

        if (selectedFacing == null
                || !isValidGeometry(
                selectedPos,
                selectedFacing,
                target.pos(),
                target.facing()
        )) {
            ClientMessages.actionBar(
                    ChatFormatting.RED,
                    "message.mechanical_drive.cardan_shaft.failed.invalid_angle"
            );
            return true;
        }

        PacketDistributor.sendToServer(
                PlaceCardanShaftPayload.place(
                        target.pos(),
                        target.facing()
                )
        );
        clearSelection();
        ClientMessages.actionBar(
                ChatFormatting.AQUA,
                "message.mechanical_drive.cardan_shaft.linked"
        );
        return true;
    }

    private static PlacementTarget getPlacementTarget(
            Minecraft minecraft
    ) {
        if (!(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || minecraft.level == null) {
            return null;
        }

        BlockPos clickedPos =
                hit.getBlockPos();
        Direction direction =
                hit.getDirection();
        BlockPos targetPos =
                canBeReplaced(
                        minecraft,
                        clickedPos,
                        direction
                )
                        ? clickedPos
                        : clickedPos.relative(
                        direction
                );

        if (!canBeReplaced(
                minecraft,
                targetPos,
                direction
        )) {
            return null;
        }

        return new PlacementTarget(
                targetPos,
                direction
        );
    }

    private static boolean canBeReplaced(
            Minecraft minecraft,
            BlockPos pos,
            Direction direction
    ) {
        InteractionHand hand =
                findCardanHand();

        if (minecraft.level == null
                || minecraft.player == null
                || hand == null) {
            return false;
        }

        BlockHitResult hitResult =
                new BlockHitResult(
                        Vec3.atCenterOf(pos),
                        direction,
                        pos,
                        false
                );

        BlockPlaceContext context =
                new BlockPlaceContext(
                        minecraft.level,
                        minecraft.player,
                        hand,
                        minecraft.player.getItemInHand(hand),
                        hitResult
                );

        return minecraft.level
                .getBlockState(pos)
                .canBeReplaced(context);
    }

    private static boolean isValidDistance(
            BlockPos first,
            BlockPos second
    ) {
        return CardanJointBlockEntity.isDistanceValid(
                distanceBetween(
                        first,
                        second
                )
        );
    }

    private static boolean isTooLongDistance(
            BlockPos first,
            BlockPos second
    ) {
        return distanceBetween(
                first,
                second
        ) > CardanJointBlockEntity.MAX_LENGTH;
    }

    private static boolean isInvalidBend(
            BlockPos first,
            Direction firstFacing,
            BlockPos second,
            Direction secondFacing
    ) {
        return isValidDistance(
                first,
                second
        )
                && !isValidGeometry(
                first,
                firstFacing,
                second,
                secondFacing
        );
    }

    private static boolean isValidGeometry(
            BlockPos first,
            Direction firstFacing,
            BlockPos second,
            Direction secondFacing
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        return minecraft.level != null
                && firstFacing != null
                && CardanJointBlockEntity.isGeometryValid(
                minecraft.level,
                first,
                firstFacing,
                second,
                secondFacing
        );
    }

    private static double distanceBetween(
            BlockPos first,
            BlockPos second
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return Vec3.atCenterOf(first)
                    .distanceTo(
                            Vec3.atCenterOf(second)
                    );
        }

        return SableSubLevelHelper.getWorldCenter(
                        minecraft.level,
                        first
                )
                .distanceTo(
                        SableSubLevelHelper.getWorldCenter(
                                minecraft.level,
                                second
                        )
                );
    }

    private static boolean isSelectionTooFar(
            Minecraft minecraft
    ) {
        if (minecraft.player == null
                || selectedPos == null) {
            return false;
        }

        Vec3 selectedCenter =
                minecraft.level == null
                        ? Vec3.atCenterOf(
                        selectedPos
                )
                        : SableSubLevelHelper.getWorldCenter(
                        minecraft.level,
                        selectedPos
                );

        return minecraft.player.position()
                .distanceToSqr(
                        selectedCenter
                ) > CANCEL_DISTANCE
                * CANCEL_DISTANCE;
    }

    private static InteractionHand findCardanHand() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return null;
        }

        ItemStack mainHand =
                minecraft.player.getMainHandItem();

        if (mainHand.is(CreateMechanicalDrive.CARDAN_SHAFT_ITEM.get())) {
            return InteractionHand.MAIN_HAND;
        }

        ItemStack offHand =
                minecraft.player.getOffhandItem();

        if (offHand.is(CreateMechanicalDrive.CARDAN_SHAFT_ITEM.get())) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    private static void clearSelection() {
        selectedPos =
                null;
        selectedFacing =
                null;
        selectedLevel =
                null;
    }

    private record PlacementTarget(
            BlockPos pos,
            Direction facing
    ) {
    }
}
