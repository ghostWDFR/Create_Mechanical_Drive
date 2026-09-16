package dev.createmechanicaldrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutBlock;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutBlockEntity;
import dev.createmechanicaldrive.network.PlaceSuspensionStrutPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
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
import net.neoforged.neoforge.network.PacketDistributor;

public final class SuspensionStrutPlacementClientInput {
    private static final double CANCEL_DISTANCE = 12.0D;

    private static BlockPos selectedPos;
    private static Direction selectedFacing;
    private static ResourceKey<Level> selectedLevel;

    private SuspensionStrutPlacementClientInput() {
    }

    public static void onClickInput(
            InputEvent.InteractionKeyMappingTriggered event
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }

        if (event.isAttack() && selectedPos != null) {
            cancel(true);
            event.setCanceled(true);
            event.setSwingHand(true);
            return;
        }

        if (event.isUseItem() && handleRightClick()) {
            event.setCanceled(true);
            event.setSwingHand(true);
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (selectedPos == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || findStrutHand() == null
                || selectedLevel == null
                || !selectedLevel.equals(minecraft.level.dimension())) {
            if (minecraft.player != null && minecraft.level != null) {
                PacketDistributor.sendToServer(PlaceSuspensionStrutPayload.cancel());
            }
            clearSelection();
            return;
        }

        Vec3 selectedCenter = SableSubLevelHelper.getWorldCenter(
                minecraft.level,
                selectedPos
        );
        if (minecraft.player.position().distanceToSqr(selectedCenter)
                > CANCEL_DISTANCE * CANCEL_DISTANCE) {
            cancel(false);
            ClientMessages.actionBar(
                    ChatFormatting.RED,
                    "message.mechanical_drive.suspension_strut.failed.invalid_length"
            );
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES
                || selectedPos == null
                || selectedFacing == null
                || findStrutHand() == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || minecraft.player == null
                || selectedLevel == null
                || !selectedLevel.equals(minecraft.level.dimension())) {
            return;
        }

        PlacementTarget target = getPlacementTarget(minecraft);
        if (target == null) {
            return;
        }

        boolean distanceValid = PlaceSuspensionStrutPayload.isDistanceValid(
                minecraft.level,
                selectedPos,
                selectedFacing,
                target.pos(),
                target.facing()
        );
        boolean angleValid = SuspensionStrutBlockEntity.isAngleValid(
                minecraft.level,
                selectedPos,
                selectedFacing,
                target.pos(),
                target.facing()
        );
        boolean valid = distanceValid && angleValid;
        float green = valid ? 1.0F : 0.2F;
        float blue = valid ? 1.0F : 0.2F;

        Vec3 firstFacing = SableSubLevelHelper.getWorldNormal(
                minecraft.level,
                selectedPos,
                directionVector(selectedFacing)
        );
        Vec3 secondFacing = SableSubLevelHelper.getWorldNormal(
                minecraft.level,
                target.pos(),
                directionVector(target.facing())
        );
        Vec3 firstOrientation = SableSubLevelHelper.getWorldNormal(
                minecraft.level,
                selectedPos,
                SuspensionStrutRenderHelper.orientationReference(selectedFacing)
        );
        Vec3 secondOrientation = SableSubLevelHelper.getWorldNormal(
                minecraft.level,
                target.pos(),
                SuspensionStrutRenderHelper.orientationReference(target.facing())
        );
        Vec3 first = SuspensionStrutRenderHelper.attachmentPoint(
                SableSubLevelHelper.getWorldCenter(minecraft.level, selectedPos),
                firstFacing
        );
        Vec3 second = SuspensionStrutRenderHelper.attachmentPoint(
                SableSubLevelHelper.getWorldCenter(minecraft.level, target.pos()),
                secondFacing
        );

        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        SuspensionStrutRenderHelper.renderStrutBetween(
                blockRenderer,
                first,
                second,
                poseStack,
                buffer,
                LightTexture.FULL_BRIGHT,
                0,
                1.0F,
                green,
                blue,
                first.distanceTo(second),
                firstOrientation,
                secondOrientation
        );
        SuspensionStrutRenderHelper.renderJoint(
                blockRenderer,
                second,
                secondFacing,
                secondOrientation,
                poseStack,
                buffer,
                LightTexture.FULL_BRIGHT,
                0,
                1.0F,
                green,
                blue
        );
        poseStack.popPose();
        buffer.endBatch();

        if (!valid) {
            ClientMessages.actionBar(
                    ChatFormatting.RED,
                    distanceValid
                            ? "message.mechanical_drive.suspension_strut.failed.invalid_angle"
                            : "message.mechanical_drive.suspension_strut.failed.invalid_length"
            );
        }
    }

    private static boolean handleRightClick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }

        if (findStrutHand() == null) {
            return selectedPos != null;
        }

        if (minecraft.player.isShiftKeyDown() && selectedPos != null) {
            cancel(true);
            return true;
        }

        PlacementTarget target = getPlacementTarget(minecraft);
        if (target == null) {
            return selectedPos != null;
        }

        if (selectedPos == null) {
            selectedPos = target.pos();
            selectedFacing = target.facing();
            selectedLevel = minecraft.level.dimension();
            PacketDistributor.sendToServer(
                    PlaceSuspensionStrutPayload.place(target.pos(), target.facing())
            );
            ClientMessages.actionBar(
                    ChatFormatting.GOLD,
                    "message.mechanical_drive.suspension_strut.started"
            );
            return true;
        }

        if (!PlaceSuspensionStrutPayload.isDistanceValid(
                minecraft.level,
                selectedPos,
                selectedFacing,
                target.pos(),
                target.facing()
        )) {
            ClientMessages.actionBar(
                    ChatFormatting.RED,
                    "message.mechanical_drive.suspension_strut.failed.invalid_length"
            );
            return true;
        }

        if (!SuspensionStrutBlockEntity.isAngleValid(
                minecraft.level,
                selectedPos,
                selectedFacing,
                target.pos(),
                target.facing()
        )) {
            ClientMessages.actionBar(
                    ChatFormatting.RED,
                    "message.mechanical_drive.suspension_strut.failed.invalid_angle"
            );
            return true;
        }

        PacketDistributor.sendToServer(
                PlaceSuspensionStrutPayload.place(target.pos(), target.facing())
        );
        clearSelection();
        ClientMessages.actionBar(
                ChatFormatting.AQUA,
                "message.mechanical_drive.suspension_strut.linked"
        );
        return true;
    }

    private static PlacementTarget getPlacementTarget(Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || minecraft.level == null) {
            return null;
        }

        BlockPos clickedPos = hit.getBlockPos();
        Direction facing = hit.getDirection();
        BlockPos targetPos = canBeReplaced(minecraft, clickedPos, facing)
                ? clickedPos
                : clickedPos.relative(facing);

        boolean compactPlacement = selectedPos != null
                && selectedFacing != null
                && selectedPos.equals(targetPos)
                && selectedFacing.getOpposite() == facing;
        if (!compactPlacement
                && !canBeReplaced(minecraft, targetPos, facing)) {
            return null;
        }

        BlockState state = CreateMechanicalDrive.SUSPENSION_STRUT_JOINT
                .get()
                .defaultBlockState()
                .setValue(SuspensionStrutBlock.FACING, facing);
        if (!state.canSurvive(minecraft.level, targetPos)) {
            return null;
        }

        return new PlacementTarget(targetPos, facing);
    }

    private static boolean canBeReplaced(
            Minecraft minecraft,
            BlockPos pos,
            Direction direction
    ) {
        InteractionHand hand = findStrutHand();
        if (minecraft.level == null || minecraft.player == null || hand == null) {
            return false;
        }

        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(pos),
                direction,
                pos,
                false
        );
        BlockPlaceContext context = new BlockPlaceContext(
                minecraft.level,
                minecraft.player,
                hand,
                minecraft.player.getItemInHand(hand),
                hit
        );
        return minecraft.level.getBlockState(pos).canBeReplaced(context);
    }

    private static InteractionHand findStrutHand() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }

        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (mainHand.is(CreateMechanicalDrive.SUSPENSION_STRUT_ITEM.get())) {
            return InteractionHand.MAIN_HAND;
        }

        ItemStack offHand = minecraft.player.getOffhandItem();
        return offHand.is(CreateMechanicalDrive.SUSPENSION_STRUT_ITEM.get())
                ? InteractionHand.OFF_HAND
                : null;
    }

    private static void cancel(boolean showMessage) {
        PacketDistributor.sendToServer(PlaceSuspensionStrutPayload.cancel());
        clearSelection();
        if (showMessage) {
            ClientMessages.actionBar(
                    ChatFormatting.GOLD,
                    "message.mechanical_drive.suspension_strut.cancelled"
            );
        }
    }

    private static void clearSelection() {
        selectedPos = null;
        selectedFacing = null;
        selectedLevel = null;
    }

    private static Vec3 directionVector(Direction direction) {
        return Vec3.atLowerCornerOf(direction.getNormal());
    }

    private record PlacementTarget(BlockPos pos, Direction facing) {
    }
}
