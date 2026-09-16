package dev.createmechanicaldrive.content.double_rigid_steering_wheel_mount;

import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class DoubleRigidSteeringWheelMountInventory extends ItemStackHandler {

    private final DoubleRigidSteeringWheelMountBlockEntity owner;
    private boolean suppressNotifications;

    public DoubleRigidSteeringWheelMountInventory(
            DoubleRigidSteeringWheelMountBlockEntity owner
    ) {
        super(2);
        this.owner = owner;
    }

    @Override
    public boolean isItemValid(
            int slot,
            ItemStack stack
    ) {
        return slot >= 0
                && slot < 2
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
        setWithoutNotification(0, stack);
    }

    public void setWithoutNotification(
            int slot,
            ItemStack stack
    ) {
        suppressNotifications = true;

        try {
            setStackInSlot(
                    slot,
                    stack
            );
        } finally {
            suppressNotifications = false;
        }
    }
}
