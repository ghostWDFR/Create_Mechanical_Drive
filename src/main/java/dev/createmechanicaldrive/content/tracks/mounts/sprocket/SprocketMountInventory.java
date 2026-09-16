package dev.createmechanicaldrive.content.tracks.mounts.sprocket;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class SprocketMountInventory extends ItemStackHandler {
    private final SprocketMountBlockEntity owner;
    private boolean suppressNotifications;

    public SprocketMountInventory(SprocketMountBlockEntity owner) {
        super(1);
        this.owner = owner;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && SprocketMountAttachmentShapes.supports(stack);
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
