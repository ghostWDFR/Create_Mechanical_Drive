package dev.createmechanicaldrive;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

public final class ControlDistance {

    public static final double MAX_DISTANCE_SQR =
            9.0D;

    private ControlDistance() {
    }

    public static boolean isTooFar(
            Player player,
            BlockPos pos
    ) {
        return player.distanceToSqr(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        ) >= MAX_DISTANCE_SQR;
    }
}