package dev.createmechanicaldrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.createmechanicaldrive.content.rigid_link.RigidLinkJointBlock;
import dev.createmechanicaldrive.content.rigid_link.RigidLinkJointBlockEntity;
import dev.createmechanicaldrive.content.rigid_link.RigidLinkItem;
import dev.createmechanicaldrive.network.LinkRigidJointsPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RigidLinkPlacementClientInput {
    private static final double CANCEL_DISTANCE = 7.0D;
    private static BlockPos selectedPos;
    private static ResourceKey<Level> selectedLevel;
    private static RigidLinkJointBlockEntity.LinkType selectedType;

    private RigidLinkPlacementClientInput() {
    }

    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }
        if (event.isAttack() && selectedPos != null) {
            PacketDistributor.sendToServer(LinkRigidJointsPayload.cancel());
            clear();
            ClientMessages.actionBar(ChatFormatting.GOLD,
                    "message.mechanical_drive.rigid_link.cancelled");
            event.setCanceled(true);
            event.setSwingHand(true);
            return;
        }
        RigidLinkJointBlockEntity.LinkType heldType = findLinkType();
        if (!event.isUseItem() || heldType == null
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || minecraft.level == null
                || !(minecraft.level.getBlockEntity(hit.getBlockPos())
                instanceof RigidLinkJointBlockEntity target)) {
            return;
        }

        if (selectedPos == null) {
            if (!target.canAccept(heldType)) {
                ClientMessages.actionBar(ChatFormatting.RED,
                        "message.mechanical_drive.rigid_link.failed.full");
                event.setCanceled(true);
                return;
            }
            selectedPos = hit.getBlockPos();
            selectedLevel = minecraft.level.dimension();
            selectedType = heldType;
            PacketDistributor.sendToServer(LinkRigidJointsPayload.select(
                    selectedPos, selectedType));
            event.setCanceled(true);
            event.setSwingHand(true);
            return;
        }

        if (selectedType != heldType
                || !isLocallyValidTarget(minecraft.level, hit.getBlockPos(),
                target, selectedType, true)) {
            event.setCanceled(true);
            return;
        }
        PacketDistributor.sendToServer(LinkRigidJointsPayload.select(
                hit.getBlockPos(), selectedType));
        clear();
        event.setCanceled(true);
        event.setSwingHand(true);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (selectedPos == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || selectedLevel == null
                || !selectedLevel.equals(minecraft.level.dimension())
                || findLinkType() != selectedType
                || minecraft.player.position().distanceToSqr(
                SableSubLevelHelper.getWorldCenter(minecraft.level, selectedPos))
                > CANCEL_DISTANCE * CANCEL_DISTANCE) {
            if (minecraft.player != null && minecraft.level != null) {
                PacketDistributor.sendToServer(LinkRigidJointsPayload.cancel());
            }
            clear();
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES
                || selectedPos == null || selectedType == null
                || findLinkType() != selectedType) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || !(minecraft.level.getBlockEntity(hit.getBlockPos())
                instanceof RigidLinkJointBlockEntity target)) {
            return;
        }

        boolean valid = isLocallyValidTarget(
                minecraft.level, hit.getBlockPos(), target, selectedType, false);
        Vec3 start = SableSubLevelHelper.getWorldCenter(minecraft.level, selectedPos);
        Vec3 end = SableSubLevelHelper.getWorldCenter(minecraft.level, hit.getBlockPos());
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        RigidLinkRenderHelper.renderBetween(minecraft.getBlockRenderer(), start, end,
                poseStack, buffer, LightTexture.FULL_BRIGHT, 0,
                1.0F, valid ? 1.0F : 0.2F, valid ? 1.0F : 0.2F);
        poseStack.popPose();

        RigidLinkJointBlockEntity first = minecraft.level.getBlockEntity(selectedPos)
                instanceof RigidLinkJointBlockEntity joint ? joint : null;
        if (first != null && !first.hasLinks()) {
            renderPreviewConnector(minecraft, selectedPos, first,
                    end.subtract(start), camera, poseStack, buffer,
                    selectedType, valid);
        }
        if (!hit.getBlockPos().equals(selectedPos) && !target.hasLinks()) {
            renderPreviewConnector(minecraft, hit.getBlockPos(), target,
                    start.subtract(end), camera, poseStack, buffer,
                    selectedType, valid);
        }
        buffer.endBatch();
    }

    private static void renderPreviewConnector(Minecraft minecraft,
                                               BlockPos pos,
                                               RigidLinkJointBlockEntity joint,
                                               Vec3 worldLinkDirection,
                                               Vec3 camera,
                                               PoseStack poseStack,
                                               MultiBufferSource buffer,
                                               RigidLinkJointBlockEntity.LinkType linkType,
                                               boolean valid) {
        if (minecraft.level == null) {
            return;
        }
        poseStack.pushPose();
        Vec3 renderOrigin = SableContraptionDragTransform.applyRenderTransform(
                minecraft.level, pos, camera, poseStack);
        if (renderOrigin == null) {
            poseStack.translate(pos.getX() - camera.x,
                    pos.getY() - camera.y,
                    pos.getZ() - camera.z);
        } else {
            poseStack.translate(pos.getX() - renderOrigin.x,
                    pos.getY() - renderOrigin.y,
                    pos.getZ() - renderOrigin.z);
        }
        Direction facing = joint.getBlockState().getValue(RigidLinkJointBlock.FACING);
        Vec3 localLinkDirection = SableContraptionDragTransform.worldNormalToLocal(
                minecraft.level, pos, worldLinkDirection);
        if (localLinkDirection == null) {
            localLinkDirection = worldLinkDirection;
        }
        RigidLinkRenderHelper.renderConnector(minecraft.getBlockRenderer(), facing,
                linkType, localLinkDirection,
                poseStack, buffer, LightTexture.FULL_BRIGHT, 0,
                1.0F, valid ? 1.0F : 0.2F, valid ? 1.0F : 0.2F);
        poseStack.popPose();
    }

    private static boolean isLocallyValidTarget(Level level, BlockPos targetPos,
                                                RigidLinkJointBlockEntity target,
                                                RigidLinkJointBlockEntity.LinkType linkType,
                                                boolean showMessage) {
        if (selectedPos == null
                || !(level.getBlockEntity(selectedPos)
                instanceof RigidLinkJointBlockEntity first)) {
            return false;
        }
        if (selectedPos.equals(targetPos)) {
            message(showMessage, "message.mechanical_drive.rigid_link.failed.invalid");
            return false;
        }
        if (!first.canAccept(linkType) || !target.canAccept(linkType)) {
            message(showMessage, "message.mechanical_drive.rigid_link.failed.full");
            return false;
        }
        if (first.hasConnectionTo(target) || target.hasConnectionTo(first)) {
            message(showMessage, "message.mechanical_drive.rigid_link.failed.duplicate");
            return false;
        }
        Vec3 firstCenter = SableSubLevelHelper.getWorldCenter(level, selectedPos);
        Vec3 targetCenter = SableSubLevelHelper.getWorldCenter(level, targetPos);
        Vec3 delta = targetCenter.subtract(firstCenter);
        if (!RigidLinkJointBlockEntity.isLengthValid(delta.length(), linkType)) {
            message(showMessage, "message.mechanical_drive.rigid_link.failed.length");
            return false;
        }
        Direction firstFacing = first.getBlockState().getValue(RigidLinkJointBlock.FACING);
        Direction targetFacing = target.getBlockState().getValue(RigidLinkJointBlock.FACING);
        boolean angleValid;
        if (linkType == RigidLinkJointBlockEntity.LinkType.LIMITED) {
            angleValid = RigidLinkJointBlockEntity.isLimitedPlacementValid(
                    level, selectedPos, firstFacing, targetPos, targetFacing);
            if (!angleValid) {
                message(showMessage,
                        "message.mechanical_drive.rigid_link.failed.plane");
            }
            return angleValid;
        }
        angleValid = RigidLinkJointBlockEntity.isDirectionValid(
                level, selectedPos, firstFacing, delta)
                && RigidLinkJointBlockEntity.isDirectionValid(
                level, targetPos, targetFacing, delta.scale(-1.0D));
        if (!angleValid) {
            message(showMessage, "message.mechanical_drive.rigid_link.failed.angle");
        }
        return angleValid;
    }

    private static void message(boolean enabled, String key) {
        if (enabled) {
            ClientMessages.actionBar(ChatFormatting.RED, key);
        }
    }

    private static RigidLinkJointBlockEntity.LinkType findLinkType() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        ItemStack main = minecraft.player.getMainHandItem();
        if (main.getItem() instanceof RigidLinkItem item) {
            return item.getLinkType();
        }
        ItemStack off = minecraft.player.getOffhandItem();
        return off.getItem() instanceof RigidLinkItem item
                ? item.getLinkType() : null;
    }

    private static void clear() {
        selectedPos = null;
        selectedLevel = null;
        selectedType = null;
    }
}
