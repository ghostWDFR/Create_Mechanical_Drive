package dev.createmechanicaldrive.client;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.gearbox.GearboxAxialLeverBlock;
import dev.createmechanicaldrive.content.gearbox.GearboxAxialLeverBlockEntity;
import dev.createmechanicaldrive.content.gearbox.GearboxPosition;
import dev.createmechanicaldrive.network.SetGearboxAxialLeverPositionPayload;
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

public final class GearboxAxialLeverClientInput {
    private static final float DRAG_SCALE = 48.0F;

    private static boolean dragging;
    private static BlockPos draggingPos;
    private static float rawGateX;
    private static float rawGateY;
    private static float gateX;
    private static float gateY;
    private static GearboxPosition lastSentPosition = GearboxPosition.NEUTRAL;
    private static GearboxPosition selectedPosition = GearboxPosition.NEUTRAL;

    private GearboxAxialLeverClientInput() {
    }

    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        if (event.getAction() == GLFW.GLFW_PRESS && tryBeginDrag()) {
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

    public static void onClientTick(ClientTickEvent.Post event) {
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

    public static void onCalculatePlayerTurn(CalculatePlayerTurnEvent event) {
        if (!dragging) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

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

        double dx = minecraft.mouseHandler.getXVelocity();
        double dy = minecraft.mouseHandler.getYVelocity();

        event.setMouseSensitivity(-1.0D / 3.0D);
        event.setCinematicCameraEnabled(false);


        if (dx == 0.0 && dy == 0.0) {
            return;
        }

        applyViewRelativeDrag(minecraft, dx, dy);

        updateLocalLeverTarget();
        GearboxPosition position = GearboxPosition.nearest(rawGateX, rawGateY);
        selectedPosition = position;
        sendSelectedPositionIfChanged();
    }

    private static boolean tryBeginDrag() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.hitResult == null) {
            return false;
        }

        if (minecraft.player.isShiftKeyDown()) {
            return false;
        }

        if (!(minecraft.hitResult instanceof BlockHitResult hitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = minecraft.level.getBlockState(pos);
        if (!state.is(CreateMechanicalDrive.GEARBOX_AXIAL_LEVER.get())) {
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
        lastSentPosition = state.getValue(GearboxAxialLeverBlock.POSITION);
        selectedPosition = lastSentPosition;
        rawGateX = lastSentPosition.gateX();
        rawGateY = lastSentPosition.gateY();
        gateX = lastSentPosition.gateX();
        gateY = lastSentPosition.gateY();

        if (minecraft.level.getBlockEntity(pos) instanceof GearboxAxialLeverBlockEntity lever) {
            lever.setLeverPosition(lastSentPosition);
            lever.setGateTarget(gateX, gateY);
        }

        return true;
    }

    private static void finishDrag() {
        if (draggingPos != null) {
            snapLocalLeverToPosition(selectedPosition);
            PacketDistributor.sendToServer(new SetGearboxAxialLeverPositionPayload(draggingPos, selectedPosition));
            lastSentPosition = selectedPosition;
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
        if (draggingPos == null || selectedPosition == lastSentPosition) {
            return;
        }

        PacketDistributor.sendToServer(new SetGearboxAxialLeverPositionPayload(draggingPos, selectedPosition));
        lastSentPosition = selectedPosition;
    }

    private static void updateLocalLeverTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || draggingPos == null) {
            return;
        }

        if (level.getBlockEntity(draggingPos) instanceof GearboxAxialLeverBlockEntity lever) {
            lever.setGateTarget(gateX, gateY);
        }
    }

    private static void snapLocalLeverToPosition(GearboxPosition position) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || draggingPos == null) {
            return;
        }

        BlockState state = level.getBlockState(draggingPos);
        if (state.is(CreateMechanicalDrive.GEARBOX_AXIAL_LEVER.get())) {
            if (state.getValue(GearboxAxialLeverBlock.POSITION) != position) {
                level.setBlock(draggingPos, state.setValue(GearboxAxialLeverBlock.POSITION, position), 3);
            }

            if (level.getBlockEntity(draggingPos) instanceof GearboxAxialLeverBlockEntity lever) {
                lever.setLeverPosition(position);
                lever.setGateTarget(position.gateX(), position.gateY());
            }
        }
    }

    private static void applyViewRelativeDrag(Minecraft minecraft, double dx, double dy) {
        Level level = minecraft.level;
        if (level == null || minecraft.player == null || draggingPos == null) {
            return;
        }

        BlockState state = level.getBlockState(draggingPos);
        if (!state.is(CreateMechanicalDrive.GEARBOX_AXIAL_LEVER.get())) {
            return;
        }

        Direction facing = horizontalOrNorth(state.getValue(GearboxAxialLeverBlock.FACING));
        Direction localForward = staticGateForward(facing);
        Direction localRight = localForward.getClockWise();
        Vec3 screenRight = horizontalViewVector(minecraft, 90.0F);
        Vec3 screenForward = horizontalViewVector(minecraft, 0.0F);

        double screenDeltaX = -dx / DRAG_SCALE;
        double screenDeltaY = dy / DRAG_SCALE;
        double worldDeltaX = screenRight.x * screenDeltaX + screenForward.x * screenDeltaY;
        double worldDeltaZ = screenRight.z * screenDeltaX + screenForward.z * screenDeltaY;

        Vec3 worldDelta = new Vec3(
                worldDeltaX,
                0.0D,
                worldDeltaZ
        );

        Vec3 localDelta =
                SableContraptionDragTransform.worldNormalToLocal(
                        level,
                        draggingPos,
                        worldDelta
                );
        float gateDeltaX;
        float gateDeltaY;

        if (localDelta != null) {
            gateDeltaX = (float) dot(localDelta, localRight);

            gateDeltaY = (float) dot(localDelta, localForward);
        } else {
            gateDeltaX = (float) dot(worldDelta, localRight);

            gateDeltaY = (float) dot(worldDelta, localForward);
        }

        if (facing.getAxis() == Direction.Axis.Z) {
            gateDeltaX = -gateDeltaX;
            gateDeltaY = -gateDeltaY;
        }

        rawGateX += gateDeltaX;
        rawGateY += gateDeltaY;

        rawGateX = Mth.clamp(rawGateX, -1.0F, 1.0F);
        rawGateY = Mth.clamp(rawGateY, -1.0F, 1.0F);

        GearboxPosition.GatePoint projected = GearboxPosition.projectToGate(rawGateX, rawGateY);
        gateX = projected.x();
        gateY = projected.y();
    }

    private static Vec3 horizontalViewVector(Minecraft minecraft, float yawOffset) {
        Vec3 vector = minecraft.player.calculateViewVector(0.0F, minecraft.player.getYRot() + yawOffset);
        double length = Math.sqrt(vector.x * vector.x + vector.z * vector.z);
        if (length < 1.0E-5D) {
            return Vec3.ZERO;
        }

        return new Vec3(vector.x / length, 0.0D, vector.z / length);
    }

    private static double dot(Vec3 vector, Direction direction) {
        return vector.x * direction.getStepX() + vector.y * direction.getStepY() + vector.z * direction.getStepZ();
    }

    private static Direction staticGateForward(Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? facing.getOpposite() : facing;
    }

    private static Direction horizontalOrNorth(Direction direction) {
        return direction.getAxis().isHorizontal() ? direction : Direction.NORTH;
    }
}
