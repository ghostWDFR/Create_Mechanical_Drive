package dev.createmechanicaldrive.infrastructure.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class CreateMechanicalDrivePonderScenes {
    private CreateMechanicalDrivePonderScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        CarGearboxPonderScenes.register(helper);
        HandCrankPonderScenes.register(helper);
        SteeringWheelPonderScenes.register(helper);
        MechanicalStarterPonderScenes.register(helper);
        SeatPonderScenes.register(helper);
        DogClutchPonderScenes.register(helper);
        GearReducerPonderScenes.register(helper);
        RotaryLimiterPonderScenes.register(helper);
        ShaftDistributorPonderScenes.register(helper);
        FourWayShaftDistributorPonderScenes.register(helper);
        OverrunningClutchPonderScenes.register(helper);
        EnginePonderScenes.register(helper);
        StirlingEnginePonderScenes.register(helper);
        FlywheelPonderScenes.register(helper);
        ChainGearPonderScenes.register(helper);
        CardanShaftPonderScenes.register(helper);
        RigidLinkPonderScenes.register(helper);
        AngleGearPonderScenes.register(helper);
        RegularWormGearPonderScenes.register(helper);
        SmallWormGearPonderScenes.register(helper);
        TankTransmissionPonderScenes.register(helper);
    }
}
