package dev.createmechanicaldrive.client;

import dev.createmechanicaldrive.ControlDistance;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.seats.SeatBackPosition;
import dev.createmechanicaldrive.content.seats.SeatBlock;
import dev.createmechanicaldrive.content.seats.SeatBlockEntity;
import dev.createmechanicaldrive.network.SetSeatBackPositionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class SeatClientInput {

    private static final float DRAG_SCALE =
            48.0F;

    private static boolean dragging;

    private static BlockPos draggingPos;

    private static float rawRecline;

    private static SeatBackPosition lastSentPosition =
            SeatBackPosition.UPRIGHT;

    private static SeatBackPosition selectedPosition =
            SeatBackPosition.UPRIGHT;

    private SeatClientInput() {
    }

    public static void onMouseButton(
            InputEvent.MouseButton.Pre event
    ) {
        if (
                event.getButton()
                        != GLFW.GLFW_MOUSE_BUTTON_RIGHT
        ) {
            return;
        }

        if (
                event.getAction() == GLFW.GLFW_PRESS
                        && tryBeginDrag()
        ) {
            event.setCanceled(
                    true
            );

            return;
        }

        if (dragging) {
            if (event.getAction() == GLFW.GLFW_RELEASE) {
                finishDrag();
            }

            event.setCanceled(
                    true
            );
        }
    }

    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        if (!dragging) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (
                minecraft.level == null
                        || minecraft.player == null
                        || minecraft.screen != null
                        || draggingPos == null
        ) {
            finishDrag();
            return;
        }

        if (
                !canControl(
                        minecraft.player
                )
        ) {
            finishDrag();
            return;
        }

        if (
                ControlDistance.isTooFar(
                        minecraft.player,
                        draggingPos
                )
        ) {
            cancelDrag();

            ClientMessages.tooFar();
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

        if (
                minecraft.player == null
                        || draggingPos == null
        ) {
            cancelDrag();
            return;
        }

        if (
                ControlDistance.isTooFar(
                        minecraft.player,
                        draggingPos
                )
        ) {
            cancelDrag();

            ClientMessages.tooFar();

            return;
        }

        double dx =
                minecraft.mouseHandler
                        .getXVelocity();

        double dy =
                minecraft.mouseHandler
                        .getYVelocity();

        event.setMouseSensitivity(
                -1.0D / 3.0D
        );

        event.setCinematicCameraEnabled(
                false
        );

        if (
                dx == 0.0D
                        && dy == 0.0D
        ) {
            return;
        }

        applyViewRelativeDrag(
                minecraft,
                dx,
                dy
        );

        updateLocalSeatTarget();

        selectedPosition =
                SeatBackPosition.nearest(
                        rawRecline
                );

        sendSelectedPositionIfChanged();
    }

    private static boolean tryBeginDrag() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (
                minecraft.level == null
                        || minecraft.player == null
                        || minecraft.hitResult == null
                        || !canControl(
                        minecraft.player
                )
        ) {
            return false;
        }

        if (!(
                minecraft.hitResult
                        instanceof BlockHitResult hitResult
        )
                || hitResult.getType()
                != HitResult.Type.BLOCK
        ) {
            return false;
        }

        BlockPos pos =
                hitResult.getBlockPos();

        BlockState state =
                minecraft.level
                        .getBlockState(
                                pos
                        );

        if (!state.is(
                CreateMechanicalDrive
                        .SEAT
                        .get()
        )) {
            return false;
        }

        if (
                ControlDistance.isTooFar(
                        minecraft.player,
                        pos
                )
        ) {
            ClientMessages.tooFar();

            return false;
        }

        dragging =
                true;

        draggingPos =
                pos;

        lastSentPosition =
                state.getValue(
                        SeatBlock.BACK_POSITION
                );

        selectedPosition =
                lastSentPosition;

        rawRecline =
                lastSentPosition.recline();

        if (
                minecraft.level
                        .getBlockEntity(
                                pos
                        )
                        instanceof SeatBlockEntity seat
        ) {
            seat.setBackPosition(
                    lastSentPosition
            );

            seat.setReclineTarget(
                    rawRecline
            );
        }

        return true;
    }

    private static void finishDrag() {
        if (draggingPos != null) {
            snapLocalSeatToPosition(
                    selectedPosition
            );

            PacketDistributor.sendToServer(
                    new SetSeatBackPositionPayload(
                            draggingPos,
                            selectedPosition
                    )
            );

            lastSentPosition =
                    selectedPosition;
        }

        dragging =
                false;

        draggingPos =
                null;
    }

    private static void cancelDrag() {
        if (draggingPos != null) {
            snapLocalSeatToPosition(
                    lastSentPosition
            );
        }

        dragging =
                false;

        draggingPos =
                null;
    }

    private static void sendSelectedPositionIfChanged() {
        if (
                draggingPos == null
                        || selectedPosition == lastSentPosition
        ) {
            return;
        }

        PacketDistributor.sendToServer(
                new SetSeatBackPositionPayload(
                        draggingPos,
                        selectedPosition
                )
        );

        lastSentPosition =
                selectedPosition;
    }

    private static void updateLocalSeatTarget() {
        Minecraft minecraft =
                Minecraft.getInstance();

        Level level =
                minecraft.level;

        if (
                level == null
                        || draggingPos == null
        ) {
            return;
        }

        if (
                level.getBlockEntity(
                        draggingPos
                )
                        instanceof SeatBlockEntity seat
        ) {
            seat.setReclineTarget(
                    rawRecline
            );
        }
    }

    private static void snapLocalSeatToPosition(
            SeatBackPosition position
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        Level level =
                minecraft.level;

        if (
                level == null
                        || draggingPos == null
        ) {
            return;
        }

        BlockState state =
                level.getBlockState(
                        draggingPos
                );

        if (!state.is(
                CreateMechanicalDrive
                        .SEAT
                        .get()
        )) {
            return;
        }

        if (
                state.getValue(
                        SeatBlock.BACK_POSITION
                )
                        != position
        ) {
            level.setBlock(
                    draggingPos,
                    state.setValue(
                            SeatBlock.BACK_POSITION,
                            position
                    ),
                    3
            );
        }

        if (
                level.getBlockEntity(
                        draggingPos
                )
                        instanceof SeatBlockEntity seat
        ) {
            seat.setBackPosition(
                    position
            );

            seat.setReclineTarget(
                    position.recline()
            );
        }
    }

    private static void applyViewRelativeDrag(
            Minecraft minecraft,
            double dx,
            double dy
    ) {
        Level level =
                minecraft.level;

        if (
                level == null
                        || minecraft.player == null
                        || draggingPos == null
        ) {
            return;
        }

        BlockState state =
                level.getBlockState(
                        draggingPos
                );

        if (!state.is(
                CreateMechanicalDrive
                        .SEAT
                        .get()
        )) {
            return;
        }

        Direction front =
                state.getValue(
                        SeatBlock.FACING
                );

        Direction localDragDirection =
                seatDragAxis(front);

        Vec3 localDragAxis =
                directionVector(
                        localDragDirection
                );

        Vec3 screenRight =
                horizontalViewVector(
                        minecraft,
                        90.0F
                );

        Vec3 screenForward =
                horizontalViewVector(
                        minecraft,
                        0.0F
                );

        double screenDeltaX =
                -dx / DRAG_SCALE;

        double screenDeltaY =
                dy / DRAG_SCALE;

        Vec3 worldDelta =
                screenRight
                        .scale(screenDeltaX)
                        .add(
                                screenForward.scale(
                                        screenDeltaY
                                )
                        );

        Vec3 localDelta =
                SableContraptionDragTransform
                        .worldNormalToLocal(
                                level,
                                draggingPos,
                                worldDelta
                        );

        Vec3 worldDragAxis =
                SableContraptionDragTransform
                        .localNormalToWorld(
                                level,
                                draggingPos,
                                localDragAxis
                        );

        if (worldDragAxis == null) {
            worldDragAxis =
                    localDragAxis;
        }

        float reclineDelta =
                projectedHorizontalDelta(
                        screenDeltaX,
                        screenDeltaY,
                        worldDragAxis,
                        screenRight,
                        screenForward
                );

        if (Float.isNaN(reclineDelta)) {
            Vec3 dragDelta =
                    localDelta != null
                            ? localDelta
                            : worldDelta;

            reclineDelta =
                    (float) dragDelta.dot(
                            localDragAxis
                    );
        }

        rawRecline += reclineDelta;

        rawRecline =
                Mth.clamp(
                        rawRecline,
                        -1.0F,
                        1.0F
                );
    }

    private static Vec3 horizontalViewVector(
            Minecraft minecraft,
            float yawOffset
    ) {
        Vec3 vector =
                minecraft.player
                        .calculateViewVector(
                                0.0F,
                                minecraft.player
                                        .getYRot()
                                        + yawOffset
                        );

        double length =
                Math.sqrt(
                        vector.x
                                * vector.x
                                + vector.z
                                * vector.z
                );

        if (length < 1.0E-5D) {
            return Vec3.ZERO;
        }

        return new Vec3(
                vector.x / length,
                0.0D,
                vector.z / length
        );
    }

    private static float projectedHorizontalDelta(
            double screenDeltaX,
            double screenDeltaY,
            Vec3 worldAxis,
            Vec3 screenRight,
            Vec3 screenForward
    ) {
        Vec3 axis =
                worldAxis.normalize();

        double axisScreenX =
                axis.dot(
                        screenRight
                );

        double axisScreenY =
                axis.dot(
                        screenForward
                );

        double axisLength =
                Math.sqrt(
                        axisScreenX
                                * axisScreenX
                                + axisScreenY
                                * axisScreenY
                );

        if (axisLength < 1.0E-5D) {
            return Float.NaN;
        }

        return (float) (
                (
                        screenDeltaX
                                * axisScreenX
                                + screenDeltaY
                                * axisScreenY
                )
                        / axisLength
        );
    }

    private static Direction seatDragAxis(
            Direction front
    ) {
        return front.getAxis() == Direction.Axis.Z
                ? front.getOpposite()
                : front;
    }

    private static Vec3 directionVector(
            Direction direction
    ) {
        return new Vec3(
                direction.getStepX(),
                direction.getStepY(),
                direction.getStepZ()
        );
    }

    private static double dot(
            Vec3 vector,
            Direction direction
    ) {
        return vector.x
                * direction.getStepX()
                + vector.y
                * direction.getStepY()
                + vector.z
                * direction.getStepZ();
    }

    private static boolean canControl(
            Player player
    ) {
        return (
                player.isShiftKeyDown()
                        || player.isCrouching()
        )
                && player.getMainHandItem()
                .isEmpty()
                && player.getOffhandItem()
                .isEmpty();
    }
}
