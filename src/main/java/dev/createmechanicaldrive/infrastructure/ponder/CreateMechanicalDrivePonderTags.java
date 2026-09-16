package dev.createmechanicaldrive.infrastructure.ponder;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class CreateMechanicalDrivePonderTags {
    public static final ResourceLocation GEARBOXES = loc("gearboxes");
    public static final ResourceLocation RIGID_LINKS = loc("rigid_links");
    public static final ResourceLocation TANK_TRANSMISSION = loc("tank_transmission");

    private CreateMechanicalDrivePonderTags() {
    }

    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(GEARBOXES)
                .addToIndex()
                .item(CreateMechanicalDrive.GEARBOX_INPUT.get())
                .title("Gearboxes")
                .description("Assembly, controls, and speed conversion for car gearboxes")
                .register();

        helper.addToTag(GEARBOXES)
                .add(loc("car_gearbox_input"))
                .add(loc("car_gearbox_speed"))
                .add(loc("gearbox_lever_axial"))
                .add(loc("gearbox_lever_linear"));

        helper.registerTag(RIGID_LINKS)
                .item(CreateMechanicalDrive.RIGID_LINK_JOINT_ITEM.get(), true, false)
                .title("Rigid Links")
                .description("Choose the type of Rigid Link to learn about")
                .register();

        helper.addToTag(RIGID_LINKS)
                .add(loc("rigid_link"))
                .add(loc("rigid_link_limited"));

        helper.registerTag(TANK_TRANSMISSION)
                .item(CreateMechanicalDrive.TANK_TRANSMISSION_FRAME_ITEM.get(), true, false)
                .title("Tank Transmission")
                .description("Choose the Tank Transmission module to learn about")
                .register();

        helper.addToTag(TANK_TRANSMISSION)
                .add(loc("tank_transmission_housing"))
                .add(loc("tank_transmission_distributor"))
                .add(loc("tank_transmission_steering"));
    }

    private static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateMechanicalDrive.MOD_ID, path);
    }
}
