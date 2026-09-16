package dev.createmechanicaldrive.mixin.client;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.infrastructure.ponder.CreateMechanicalDrivePonderTags;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.api.registration.SceneRegistryAccess;
import net.createmod.ponder.foundation.PonderTooltipHandler;
import net.createmod.ponder.foundation.ui.PonderTagScreen;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PonderTooltipHandler.class)
public abstract class PonderTooltipHandlerMixin {

    @Shadow
    static LerpedFloat holdKeyProgress;

    @Shadow
    static ItemStack trackingStack;

    @Redirect(
            method = "updateHovered",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/createmod/ponder/api/registration/SceneRegistryAccess;doScenesExistForId(Lnet/minecraft/resources/ResourceLocation;)Z"
            )
    )
    private static boolean createMechanicalDrive$makeSelectorPonderable(
            SceneRegistryAccess sceneRegistry,
            ResourceLocation component
    ) {
        return createMechanicalDrive$isSelector(component)
                || sceneRegistry.doScenesExistForId(component);
    }

    @Inject(
            method = "deferredTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/createmod/ponder/foundation/ui/PonderUI;of(Lnet/minecraft/world/item/ItemStack;)Lnet/createmod/ponder/foundation/ui/PonderUI;"
            ),
            cancellable = true
    )
    private static void createMechanicalDrive$openSelector(
            CallbackInfo ci
    ) {
        ResourceLocation selector = createMechanicalDrive$getSelector(trackingStack);
        if (selector == null) {
            return;
        }

        ScreenOpener.transitionTo(
                new PonderTagScreen(selector)
        );
        holdKeyProgress.startWithValue(0);
        ci.cancel();
    }

    private static boolean createMechanicalDrive$isSelector(
            ResourceLocation component
    ) {
        return component.equals(CreateMechanicalDrive.RIGID_LINK_JOINT_ITEM.getId())
                || component.equals(CreateMechanicalDrive.TANK_TRANSMISSION_FRAME_ITEM.getId());
    }

    private static ResourceLocation createMechanicalDrive$getSelector(
            ItemStack stack
    ) {
        if (stack.is(CreateMechanicalDrive.RIGID_LINK_JOINT_ITEM.get())) {
            return CreateMechanicalDrivePonderTags.RIGID_LINKS;
        }
        if (stack.is(CreateMechanicalDrive.TANK_TRANSMISSION_FRAME_ITEM.get())) {
            return CreateMechanicalDrivePonderTags.TANK_TRANSMISSION;
        }
        return null;
    }
}
