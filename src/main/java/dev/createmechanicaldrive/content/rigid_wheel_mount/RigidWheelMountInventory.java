package dev.createmechanicaldrive.content.rigid_wheel_mount;

import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class RigidWheelMountInventory extends ItemStackHandler {

    private final RigidWheelMountBlockEntity owner;
    private boolean suppressNotifications;

    public RigidWheelMountInventory(
            RigidWheelMountBlockEntity owner
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
