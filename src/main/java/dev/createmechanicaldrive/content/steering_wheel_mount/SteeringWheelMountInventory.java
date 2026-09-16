package dev.createmechanicaldrive.content.steering_wheel_mount;

import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class SteeringWheelMountInventory extends ItemStackHandler {

    protected final SteeringWheelMountBlockEntity owner;
    private boolean suppressNotifications;

    public SteeringWheelMountInventory(
            SteeringWheelMountBlockEntity owner
    ) {
        this(
                owner,
                1
        );
    }

    protected SteeringWheelMountInventory(
            SteeringWheelMountBlockEntity owner,
            int slots
    ) {
        super(slots);
        this.owner = owner;
    }

    @Override
    public boolean isItemValid(
            int slot,
            ItemStack stack
    ) {
        return slot >= 0
                && slot < getSlots()
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
        setWithoutNotification(
                0,
                stack
        );
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
