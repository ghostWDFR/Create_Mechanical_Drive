package dev.createmechanicaldrive.infrastructure.ponder;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class CreateMechanicalDrivePonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return CreateMechanicalDrive.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        CreateMechanicalDrivePonderScenes.register(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        CreateMechanicalDrivePonderTags.register(helper);
    }
}
