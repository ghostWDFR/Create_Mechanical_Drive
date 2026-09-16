package dev.createmechanicaldrive.content.double_steering_wheel_mount;

import dev.createmechanicaldrive.content.steering_wheel_mount.SteeringWheelMountInventory;

public class DoubleSteeringWheelMountInventory
        extends SteeringWheelMountInventory {

    public DoubleSteeringWheelMountInventory(
            DoubleSteeringWheelMountBlockEntity owner
    ) {
        super(
                owner,
                2
        );
    }
}