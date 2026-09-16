package dev.createmechanicaldrive.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int createMechanicalDrive$getLeftPos();

    @Accessor("topPos")
    int createMechanicalDrive$getTopPos();
}