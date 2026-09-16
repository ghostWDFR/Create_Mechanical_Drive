package dev.createmechanicaldrive.mixin.client;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.client.creative.CreativeTabSectionRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {

    @Shadow
    private static CreativeModeTab selectedTab;

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void createMechanicalDrive$renderSections(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if (selectedTab != CreateMechanicalDrive.MAIN_TAB.get()) {
            return;
        }

        CreativeTabSectionRenderer.render(
                (CreativeModeInventoryScreen) (Object) this,
                graphics,
                mouseX,
                mouseY
        );
    }
}