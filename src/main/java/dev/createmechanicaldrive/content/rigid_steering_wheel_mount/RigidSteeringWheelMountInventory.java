package dev.createmechanicaldrive.content.rigid_steering_wheel_mount;

import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class RigidSteeringWheelMountInventory extends ItemStackHandler {

    private final RigidSteeringWheelMountBlockEntity owner;
    private boolean suppressNotifications;

    public RigidSteeringWheelMountInventory(
            RigidSteeringWheelMountBlockEntity owner
    ) {
        super(1);
        this.owner = owner;
    }

    @Override
    public boolean isItemValid(
            int slot,
            ItemStack stack
    ) {
        return slot == 0
                && ShaftMarkerItem.isWheelSlotItem(stack);
    }

    @Override
    protected void onContentsChanged(
            int slot
    ) {
        if (!suppressNotifications) {
            owner.onWheelChanged();
        }
    }

    public void setWithoutNotification(
            ItemStack stack
    ) {
        suppressNotifications = true;

        try {
            setStackInSlot(
                    0,
                    stack
            );
        } finally {
            suppressNotifications = false;
        }
    }
}
