package dev.createmechanicaldrive.content.tracks.mounts.idler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class IdlerMountInventory extends ItemStackHandler {
    private final IdlerMountBlockEntity owner;
    private boolean suppressNotifications;

    public IdlerMountInventory(IdlerMountBlockEntity owner) {
        super(1);
        this.owner = owner;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && IdlerMountAttachmentShapes.supports(stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (!suppressNotifications) {
            owner.onAttachmentChanged();
        }
    }

    public void setWithoutNotification(ItemStack stack) {
        suppressNotifications = true;
        try {
            setStackInSlot(0, stack);
        } finally {
            suppressNotifications = false;
        }
    }
}
