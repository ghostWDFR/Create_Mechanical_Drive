package dev.createmechanicaldrive.client;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.steering_wheel.SteeringWheelBlockEntity;
import dev.createmechanicaldrive.network.SetSteeringWheelAnglePayload;
import dev.createmechanicaldrive.ControlDistance;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class SteeringWheelClientInput {

    private static final float DRAG_SCALE =
            10.0F;

    private static boolean dragging =
            false;

    private static BlockPos draggingPos;

    private static float targetAngle =
            0.0F;

    private static float lastSentAngle =
            Float.NaN;

    private SteeringWheelClientInput() {
    }

    public static void onMouseButton(
            InputEvent.MouseButton.Pre event
    ) {
        if (event.getButton()
                != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        if (event.getAction()
                == GLFW.GLFW_PRESS
                && tryBeginDrag()) {
            event.setCanceled(true);
            return;
        }

        if (!dragging) {
            return;
        }

        if (event.getAction()
                == GLFW.GLFW_RELEASE) {
            finishDrag();
        }

        event.setCanceled(true);
    }

    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        if (!dragging) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null
                || minecraft.screen != null
                || draggingPos == null) {
            finishDrag();
            return;
        }

        if (!minecraft.player
                .getMainHandItem()
                .isEmpty()) {
            finishDrag();
            return;
        }

        BlockState state =
                minecraft.level
                        .getBlockState(
                                draggingPos
                        );

        if (!state.is(
                CreateMechanicalDrive
                        .STEERING_WHEEL
                        .get()
        )) {
            finishDrag();
            return;
        }

        if (ControlDistance.isTooFar(
                minecraft.player,
                draggingPos
        )) {
            ClientMessages.tooFar();

            finishDrag();
            return;
        }

        if (minecraft.level
                .getBlockEntity(draggingPos)
                instanceof SteeringWheelBlockEntity steeringWheel
                && steeringWheel.isControlledByAnother(
                minecraft.player.getUUID()
        )) {
            finishDrag();
        }
    }

    public static void onCalculatePlayerTurn(
            CalculatePlayerTurnEvent event
    ) {
        if (!dragging) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        double dx =
                minecraft.mouseHandler
                        .getXVelocity();

        event.setMouseSensitivity(
                -1.0D / 3.0D
        );

        event.setCinematicCameraEnabled(
                false
        );

        if (dx == 0.0D) {
            return;
        }

        targetAngle +=
                (float) (
                        dx
                                / DRAG_SCALE
                );

        updateLocalTarget();

        sendTargetIfChanged();
    }

    private static boolean tryBeginDrag() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null
                || minecraft.hitResult == null
                || minecraft.screen != null) {
            return false;
        }

        if (!minecraft.player
                .getMainHandItem()
                .isEmpty()) {
            return false;
        }

        if (!(minecraft.hitResult
                instanceof BlockHitResult hitResult)
                || hitResult.getType()
                != HitResult.Type.BLOCK) {
            return false;
        }

        BlockPos pos =
                hitResult.getBlockPos();

        BlockState state =
                minecraft.level
                        .getBlockState(pos);

        if (!state.is(
                CreateMechanicalDrive
                        .STEERING_WHEEL
                        .get()
        )) {
            return false;
        }

        if (ControlDistance.isTooFar(
                minecraft.player,
                pos
        )) {
            ClientMessages.tooFar();

            return false;
        }

        if (!(minecraft.level
                .getBlockEntity(pos)
                instanceof SteeringWheelBlockEntity steeringWheel)) {
            return false;
        }

        if (steeringWheel.isControlledByAnother(
                minecraft.player.getUUID()
        )) {
            return false;
        }

        dragging =
                true;

        draggingPos =
                pos;

        targetAngle =
                steeringWheel.getTargetAngle();

        lastSentAngle =
                targetAngle;

        PacketDistributor.sendToServer(
                new SetSteeringWheelAnglePayload(
                        draggingPos,
                        targetAngle,
                        true
                )
        );

        return true;
    }

    private static void updateLocalTarget() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || draggingPos == null) {
            return;
        }

        if (minecraft.level
                .getBlockEntity(draggingPos)
                instanceof SteeringWheelBlockEntity steeringWheel) {
            steeringWheel.setTargetAngle(
                    targetAngle
            );
        }
    }

    private static void sendTargetIfChanged() {
        if (draggingPos == null
                || targetAngle == lastSentAngle) {
            return;
        }

        PacketDistributor.sendToServer(
                new SetSteeringWheelAnglePayload(
                        draggingPos,
                        targetAngle,
                        true
                )
        );

        lastSentAngle =
                targetAngle;
    }

    private static void finishDrag() {
        if (draggingPos != null) {
            PacketDistributor.sendToServer(
                    new SetSteeringWheelAnglePayload(
                            draggingPos,
                            targetAngle,
                            false
                    )
            );
        }

        dragging =
                false;

        draggingPos =
                null;

        lastSentAngle =
                Float.NaN;
    }

    public static boolean isDragging(
            BlockPos pos
    ) {
        return dragging
                && draggingPos != null
                && draggingPos.equals(pos);
    }

    public static float getDraggingAngle() {
        return targetAngle;
    }
}
