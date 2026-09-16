package dev.createmechanicaldrive.mixin.client;

import dev.createmechanicaldrive.creative.CreativeTabSections;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public abstract class ItemPickerMenuMixin {

    @Shadow
    protected abstract int getRowIndexForScroll(float scroll);

    @Inject(
            method = "scrollTo",
            at = @At("HEAD")
    )
    private void createMechanicalDrive$scrollTo(
            float scroll,
            CallbackInfo ci
    ) {
        CreativeTabSections.setCurrentScrollRow(
                getRowIndexForScroll(scroll)
        );
    }
}