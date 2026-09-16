package dev.createmechanicaldrive.client;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.gearbox.GearboxLinearLeverBlock;
import dev.createmechanicaldrive.content.gearbox.GearboxLinearLeverBlockEntity;
import dev.createmechanicaldrive.content.gearbox.GearboxPosition;
import dev.createmechanicaldrive.network.SetGearboxLinearLeverPositionPayload;
import dev.createmechanicaldrive.ControlDistance;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
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

public final class GearboxLinearLeverClientInput {

    private static final float DRAG_SCALE = 48.0F;

    private static boolean dragging;
    private static BlockPos draggingPos;

    private static float rawLinear;

    private static GearboxPosition lastSentPosition =
            GearboxPosition.NEUTRAL;

    private static GearboxPosition selectedPosition =
            GearboxPosition.NEUTRAL;

    private GearboxLinearLeverClientInput() {
    }

    public static void onMouseButton(
            InputEvent.MouseButton.Pre event
    ) {
        if (event.getButton()
                != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        if (event.getAction() == GLFW.GLFW_PRESS
                && tryBeginDrag()) {
            event.setCanceled(true);
            return;
        }

        if (dragging) {
            if (event.getAction() == GLFW.GLFW_RELEASE) {
                finishDrag();
            }

            event.setCanceled(true);
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

        if (minecraft.level == null
                || minecraft.player == null
                || minecraft.screen != null
                || draggingPos == null) {
            finishDrag();
            return;
        }

        if (ControlDistance.isTooFar(
                minecraft.player,
                draggingPos
        )) {
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

        if (minecraft.player == null
                || draggingPos == null) {
            cancelDrag();
            return;
        }

        if (ControlDistance.isTooFar(
                minecraft.player,
                draggingPos
        )) {
            cancelDrag();

            ClientMessages.tooFar();

            return;
        }

        double dx =
                minecraft.mouseHandler.getXVelocity();

        double dy =
                minecraft.mouseHandler.getYVelocity();

        event.setMouseSensitivity(-1.0D / 3.0D);
        event.setCinematicCameraEnabled(false);

        if (dx == 0.0D && dy == 0.0D) {
            return;
        }

        applyViewRelativeDrag(
                minecraft,
                dx,
                dy
        );

        updateLocalLeverTarget();

        selectedPosition =
                GearboxPosition.nearestLinear(rawLinear);

        sendSelectedPositionIfChanged();
    }

    private static boolean tryBeginDrag() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null
                || minecraft.hitResult == null) {
            return false;
        }

        if (minecraft.player.isShiftKeyDown()) {
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
                minecraft.level.getBlockState(pos);

        if (!state.is(
                CreateMechanicalDrive
                        .GEARBOX_LINEAR_LEVER
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

        dragging = true;
        draggingPos = pos;

        lastSentPosition =
                state.getValue(
                        GearboxLinearLeverBlock.POSITION
                );

        selectedPosition =
                lastSentPosition;

        rawLinear =
                lastSentPosition.linearValue();

        if (minecraft.level.getBlockEntity(pos)
                instanceof GearboxLinearLeverBlockEntity lever) {
            lever.setLeverPosition(lastSentPosition);
            lever.setLinearTarget(rawLinear);
        }

        return true;
    }

    private static void finishDrag() {
        if (draggingPos != null) {
            snapLocalLeverToPosition(
                    selectedPosition
            );

            PacketDistributor.sendToServer(
                    new SetGearboxLinearLeverPositionPayload(
                            draggingPos,
                            selectedPosition
                    )
            );

            lastSentPosition =
                    selectedPosition;
        }

        dragging = false;
        draggingPos = null;
    }

    private static void cancelDrag() {
        if (draggingPos != null) {
            snapLocalLeverToPosition(
                    lastSentPosition
            );
        }

        dragging = false;
        draggingPos = null;
    }

    private static void sendSelectedPositionIfChanged() {
        if (draggingPos == null
                || selectedPosition == lastSentPosition) {
            return;
        }

        PacketDistributor.sendToServer(
                new SetGearboxLinearLeverPositionPayload(
                        draggingPos,
                        selectedPosition
                )
        );

        lastSentPosition =
                selectedPosition;
    }

    private static void updateLocalLeverTarget() {
        Minecraft minecraft =
                Minecraft.getInstance();

        Level level =
                minecraft.level;

        if (level == null || draggingPos == null) {
            return;
        }

        if (level.getBlockEntity(draggingPos)
                instanceof GearboxLinearLeverBlockEntity lever) {
            lever.setLinearTarget(rawLinear);
        }
    }

    private static void snapLocalLeverToPosition(
            GearboxPosition position
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        Level level =
                minecraft.level;

        if (level == null || draggingPos == null) {
            return;
        }

        BlockState state =
                level.getBlockState(draggingPos);

        if (!state.is(
                CreateMechanicalDrive
                        .GEARBOX_LINEAR_LEVER
                        .get()
        )) {
            return;
        }

        if (state.getValue(
                GearboxLinearLeverBlock.POSITION
        ) != position) {
            level.setBlock(
                    draggingPos,
                    state.setValue(
                            GearboxLinearLeverBlock.POSITION,
                            position
                    ),
                    3
            );
        }

        if (level.getBlockEntity(draggingPos)
                instanceof GearboxLinearLeverBlockEntity lever) {
            lever.setLeverPosition(position);
            lever.setLinearTarget(
                    position.linearValue()
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

        if (level == null
                || minecraft.player == null
                || draggingPos == null) {
            return;
        }

        BlockState state =
                level.getBlockState(draggingPos);

        if (!state.is(
                CreateMechanicalDrive
                        .GEARBOX_LINEAR_LEVER
                        .get()
        )) {
            return;
        }

        Direction facing =
                horizontalOrNorth(
                        state.getValue(
                                GearboxLinearLeverBlock.FACING
                        )
                );

        Direction localForward =
                staticLeverForward(facing);

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

        double worldDeltaX =
                screenRight.x * screenDeltaX
                        + screenForward.x * screenDeltaY;

        double worldDeltaZ =
                screenRight.z * screenDeltaX
                        + screenForward.z * screenDeltaY;

        Vec3 worldDelta =
                new Vec3(
                        worldDeltaX,
                        0.0D,
                        worldDeltaZ
                );

        Vec3 localDelta =
                SableContraptionDragTransform
                        .worldNormalToLocal(
                                level,
                                draggingPos,
                                worldDelta
                        );

        float linearDelta;

        if (localDelta != null) {
            linearDelta =
                    (float) dot(
                            localDelta,
                            localForward
                    );
        } else {
            linearDelta =
                    (float) dot(
                            worldDelta,
                            localForward
                    );
        }
        if (facing.getAxis() == Direction.Axis.Z) {
            linearDelta = -linearDelta;
        }

        rawLinear += linearDelta;

        rawLinear =
                Mth.clamp(
                        rawLinear,
                        -1.0F,
                        1.0F
                );
    }

    private static Vec3 horizontalViewVector(
            Minecraft minecraft,
            float yawOffset
    ) {
        Vec3 vector =
                minecraft.player.calculateViewVector(
                        0.0F,
                        minecraft.player.getYRot()
                                + yawOffset
                );

        double length =
                Math.sqrt(
                        vector.x * vector.x
                                + vector.z * vector.z
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

    private static double dot(
            Vec3 vector,
            Direction direction
    ) {
        return vector.x * direction.getStepX()
                + vector.y * direction.getStepY()
                + vector.z * direction.getStepZ();
    }

    private static Direction staticLeverForward(
            Direction facing
    ) {
        return facing.getAxis() == Direction.Axis.Z
                ? facing.getOpposite()
                : facing;
    }

    private static Direction horizontalOrNorth(
            Direction direction
    ) {
        return direction.getAxis().isHorizontal()
                ? direction
                : Direction.NORTH;
    }
}
