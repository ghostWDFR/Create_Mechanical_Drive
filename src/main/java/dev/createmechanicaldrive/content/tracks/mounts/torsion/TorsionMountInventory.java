package dev.createmechanicaldrive.content.tracks.mounts.torsion;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class TorsionMountInventory extends ItemStackHandler {
    private final TorsionMountBlockEntity owner;
    private boolean suppressNotifications;

    public TorsionMountInventory(TorsionMountBlockEntity owner) {
        super(2);
        this.owner = owner;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return (slot == 0 && owner.isAttachmentSupported(stack))
                || (slot == 1 && owner.isSupportWheelSupported(stack));
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (!suppressNotifications) {
            owner.onAttachmentChanged(slot);
        }
    }

    public void setWithoutNotification(ItemStack stack) {
        setWithoutNotification(0, stack);
    }

    public void setWithoutNotification(int slot, ItemStack stack) {
        suppressNotifications = true;
        try {
            setStackInSlot(slot, stack);
        } finally {
            suppressNotifications = false;
        }
    }
}
