package dev.createmechanicaldrive.content.tank_transmission;

import net.minecraft.core.BlockPos;

public interface TankTransmissionSteeringControlPassThrough {

    boolean mechanicalDrive$isTankSteeringControlPassThrough();

    BlockPos mechanicalDrive$getTankSteeringControlPassThroughSource();
}
