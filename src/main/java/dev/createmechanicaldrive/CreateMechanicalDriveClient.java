package dev.createmechanicaldrive;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import dev.createmechanicaldrive.client.ChainGearSelectionClientInput;
import dev.createmechanicaldrive.client.CardanShaftPlacementClientInput;
import dev.createmechanicaldrive.client.CompactStrutInteractionClientInput;
import dev.createmechanicaldrive.client.SuspensionStrutPlacementClientInput;
import dev.createmechanicaldrive.client.RigidLinkPlacementClientInput;
import dev.createmechanicaldrive.client.GearboxAxialLeverClientInput;
import dev.createmechanicaldrive.client.GearboxLinearLeverClientInput;
import dev.createmechanicaldrive.client.SeatClientInput;
import dev.createmechanicaldrive.client.SteeringWheelClientInput;
import dev.createmechanicaldrive.client.EngineClientEffects;
import dev.createmechanicaldrive.content.engine.EngineBlockEntity;
import dev.createmechanicaldrive.content.engine.EngineRenderer;
import dev.createmechanicaldrive.content.chain_linkage.ChainGearRenderer;
import dev.createmechanicaldrive.content.cardan_shaft.CardanJointRenderer;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutRenderer;
import dev.createmechanicaldrive.content.rigid_link.RigidLinkJointRenderer;
import dev.createmechanicaldrive.content.angle_gear.AngleGearRenderer;
import dev.createmechanicaldrive.content.gearbox.GearboxAxialLeverRenderer;
import dev.createmechanicaldrive.content.gearbox.GearboxLinearLeverRenderer;
import dev.createmechanicaldrive.content.gearbox.CarGearboxInputRenderer;
import dev.createmechanicaldrive.content.gearbox.CarGearboxSpeedRenderer;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionHousingRenderer;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionDistributorRenderer;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionSteeringRenderer;
import dev.createmechanicaldrive.content.steering_wheel.SteeringWheelRenderer;
import dev.createmechanicaldrive.content.hand_crank.HandCrankRenderer;
import dev.createmechanicaldrive.content.mechanical_starter.MechanicalStarterRenderer;
import dev.createmechanicaldrive.content.dog_clutch.DogClutchRenderer;
import dev.createmechanicaldrive.content.gear_reducer.GearReducerRenderer;
import dev.createmechanicaldrive.content.shaft_distributor.ShaftDistributorRenderer;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerRenderer;
import dev.createmechanicaldrive.content.tracks.mounts.idler.IdlerMountRenderer;
import dev.createmechanicaldrive.content.tracks.mounts.sprocket.SprocketMountRenderer;
import dev.createmechanicaldrive.content.tracks.mounts.torsion.TorsionMountRenderer;
import dev.createmechanicaldrive.content.tracks.wheels.drive.DriveWheelRenderer;
import dev.createmechanicaldrive.content.tracks.wheels.idler.IdlerWheelRenderer;
import dev.createmechanicaldrive.content.tracks.wheels.sprocket.SprocketWheelRenderer;
import dev.createmechanicaldrive.content.tracks.wheels.support.SupportWheelRenderer;
import dev.createmechanicaldrive.content.tracks.chain.TrackAssemblyRenderer;
import dev.createmechanicaldrive.content.overrunning_clutch.OverrunningClutchRenderer;
import dev.createmechanicaldrive.content.seats.SeatEntityRenderer;
import dev.createmechanicaldrive.content.seats.SeatBlock;
import dev.createmechanicaldrive.content.seats.SeatColor;
import dev.createmechanicaldrive.content.seats.SeatRenderer;
import dev.createmechanicaldrive.content.worm_gears.WormGearRegularRenderer;
import dev.createmechanicaldrive.content.worm_gears.WormGearSmallRenderer;
import dev.createmechanicaldrive.content.mechanical_jack.MechanicalJackRenderer;
import dev.createmechanicaldrive.content.steering_wheel_mount.SteeringWheelMountRenderer;
import dev.createmechanicaldrive.content.rigid_steering_wheel_mount.RigidSteeringWheelMountRenderer;
import dev.createmechanicaldrive.content.rigid_wheel_mount.RigidWheelMountRenderer;
import dev.createmechanicaldrive.content.double_steering_wheel_mount.DoubleSteeringWheelMountRenderer;
import dev.createmechanicaldrive.content.double_rigid_steering_wheel_mount.DoubleRigidSteeringWheelMountRenderer;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineHeaterBlockEntity;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineHeaterItem;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineHeaterRenderer;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineOutputRenderer;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEnginePoweredShaftRenderer;
import dev.createmechanicaldrive.content.rotary_limiter.RotaryLimiterRenderer;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineFlywheelRenderer;
import dev.createmechanicaldrive.infrastructure.ponder.CreateMechanicalDrivePonderPlugin;
import dev.createmechanicaldrive.content.steering_wheel.SteeringWheelBlock;
import dev.createmechanicaldrive.content.steering_wheel.SteeringWheelColor;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeColor;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.Minecraft;

@EventBusSubscriber(
        modid = CreateMechanicalDrive.MOD_ID,
        value = Dist.CLIENT
)
public class CreateMechanicalDriveClient {

    public static final ModelResourceLocation RIGID_STEERING_MOUNT_PART_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/wheel_mounts/rigid_steering_mount_part"
                    )
            );    public static final ModelResourceLocation RIGID_WHEEL_MOUNT_PART_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/wheel_mounts/rigid_wheel_mount_part"
                    )
            );
    public static final ModelResourceLocation STIRLING_ENGINE_HEATER_COVER_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/stirling_engine/stirling_engine_heater_cover"
                    )
            );

    public static final ModelResourceLocation GEARBOX_LEVER_AXIAL_HANDLE_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/gearbox/gearbox_lever_axial_handle"
                    )
            );

    public static final ModelResourceLocation GEARBOX_LEVER_LINEAR_HANDLE_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/gearbox/gearbox_lever_linear_handle"
                    )
            );

    public static final ModelResourceLocation
            HAND_CRANK_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/hand_crank/hand_crank"
                    )
            );

    public static final ModelResourceLocation
            HAND_CRANK_HANDLE_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/hand_crank/hand_crank_handle"
                    )
            );

    public static final ModelResourceLocation ENGINE_PIPES_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/engine/engine_pipes"
                    )
            );

    public static final ModelResourceLocation ENGINE_PIPES_HEATED_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/engine/engine_pipes_heated"
                    )
            );

    public static final ModelResourceLocation ENGINE_PIPES_EXTENDED_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/engine/engine_pipes_extended"
                    )
            );

    public static final ModelResourceLocation ENGINE_PIPES_EXTENDED_HEATED_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/engine/engine_pipes_extended_heated"
                    )
            );

    public static final ModelResourceLocation STEERING_WHEEL_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/steering_wheel/steering_wheel"
                    )
            );

    public static final ModelResourceLocation STEERING_WHEEL_BLUE_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/steering_wheel/steering_wheel_blue"
                    )
            );

    public static final ModelResourceLocation STEERING_WHEEL_GRAY_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/steering_wheel/steering_wheel_gray"
                    )
            );

    public static final ModelResourceLocation STEERING_WHEEL_GREEN_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/steering_wheel/steering_wheel_green"
                    )
            );

    public static final ModelResourceLocation STEERING_WHEEL_RED_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/steering_wheel/steering_wheel_red"
                    )
            );

    public static final ModelResourceLocation STEERING_WHEEL_YELLOW_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/steering_wheel/steering_wheel_yellow"
                    )
            );

    public static final ModelResourceLocation OVERRUNNING_CLUTCH_INNER_MIDDLE_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/overrunning_clutch/overrunning_clutch_inner_middle"
                    )
            );

    public static final ModelResourceLocation OVERRUNNING_CLUTCH_INNER_SIDES_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/overrunning_clutch/overrunning_clutch_inner_sides"
                    )
            );

    public static final ModelResourceLocation SHAFT_DISTRIBUTOR_FRAME_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/distributor_shaft/shaft_distributor_frame"
                    )
            );
    public static final ModelResourceLocation
            FOUR_WAY_SHAFT_DISTRIBUTOR_FRAME_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/distributor_shaft/shaft_distributor_double_frame"
                    )
            );
    public static final ModelResourceLocation SHAFT_DISTRIBUTOR_INPUT_SHAFT_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/distributor_shaft/shaft_distributor_input_shaft"
                    )
            );

    public static final ModelResourceLocation SHAFT_DISTRIBUTOR_HORIZONTAL_SHAFT_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/distributor_shaft/shaft_distributor_horizontal_shaft"
                    )
            );
    public static final ModelResourceLocation SEAT_REAR_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/seats/seat_rear"
                    )
            );

    public static final ModelResourceLocation SEAT_REAR_BLUE_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/seats/seat_rear_blue"
                    )
            );

    public static final ModelResourceLocation SEAT_REAR_GRAY_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/seats/seat_rear_gray"
                    )
            );

    public static final ModelResourceLocation SEAT_REAR_GREEN_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/seats/seat_rear_green"
                    )
            );

    public static final ModelResourceLocation SEAT_REAR_RED_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/seats/seat_rear_red"
                    )
            );

    public static final ModelResourceLocation SEAT_REAR_YELLOW_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/seats/seat_rear_yellow"
                    )
            );

    public static final ModelResourceLocation WORM_GEAR_SMALL_ENCASED_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/worm_gear/worm_gear_small_encased"
                    )
            );

    public static final ModelResourceLocation WORM_GEAR_REGULAR_ENCASED_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/worm_gear/worm_gear_encased"
                    )
            );

    public static final ModelResourceLocation CHAIN_GEAR_M_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/chain_linkage/chain_gear_m"
                    )
            );

    public static final ModelResourceLocation CHAIN_GEAR_T_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/chain_linkage/chain_gear_t"
                    )
            );

    public static final ModelResourceLocation CHAIN_GEAR_B_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/chain_linkage/chain_gear_b"
                    )
            );

    public static final ModelResourceLocation CHAIN_LINKAGE_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/chain_linkage/chain_linkage"
                    )
            );

    public static final ModelResourceLocation CARDAN_SHAFT_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/cardan_shaft/cardan_shaft"
                    )
            );

    public static final ModelResourceLocation CARDAN_SHAFT_CENTER_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/cardan_shaft/cardan_shaft_center"
                    )
            );

    public static final ModelResourceLocation CARDAN_SHAFT_EDGE_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/cardan_shaft/cardan_shaft_edge"
                    )
            );

    public static final ModelResourceLocation CARDAN_JOINT_CONNECTOR_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/cardan_shaft/cardan_joint_connector"
                    )
            );

    public static final ModelResourceLocation SUSPENSION_STRUT_BALL_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/suspension_strut/suspension_strut_ball"
                    )
            );

    public static final ModelResourceLocation SUSPENSION_STRUT_JOINT_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/suspension_strut/suspension_strut_joint"
                    )
            );

    public static final ModelResourceLocation SUSPENSION_STRUT_SPRING_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/suspension_strut/suspension_strut_spring"
                    )
            );

    public static final ModelResourceLocation SUSPENSION_STRUT_SHAFT_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/suspension_strut/suspension_strut_shaft"
                    )
            );

    public static final ModelResourceLocation SUSPENSION_STRUT_UPPER_SHAFT_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/suspension_strut/suspension_strut_upper_shaft"
                    )
            );

    public static final ModelResourceLocation RIGID_LINK_SHAFT_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/rigid_link/rigid_link_shaft"
                    )
            );

    public static final ModelResourceLocation RIGID_LINK_SHAFT_DETAIL_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/rigid_link/rigid_link_shaft_detail"
                    )
            );

    public static final ModelResourceLocation RIGID_LINK_CONNECTOR_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/rigid_link/rigid_link_connector"
                    )
            );

    public static final ModelResourceLocation RIGID_LINK_LIMITED_CONNECTOR_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/rigid_link/rigid_link_connector_limited"
                    )
            );

    public static final ModelResourceLocation MECHANICAL_JACK_BASE_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/mechanical_jack/mechanical_jack_base"
                    )
            );

    public static final ModelResourceLocation MECHANICAL_JACK_INPUT_SHAFT_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/mechanical_jack/mechanical_jack_input_shaft"
                    )
            );

    public static final ModelResourceLocation MECHANICAL_JACK_SHAFT_1_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/mechanical_jack/mechanical_jack_shaft_1"
                    )
            );

    public static final ModelResourceLocation MECHANICAL_JACK_SHAFT_2_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/mechanical_jack/mechanical_jack_shaft_2"
                    )
            );

    public static final ModelResourceLocation MECHANICAL_JACK_TOP_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/mechanical_jack/mechanical_jack_top_part"
                    )
            );

    public static final ModelResourceLocation STIRLING_ENGINE_FLYWHEEL_INNER_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/stirling_engine/stirling_engine_flywheel_inner"
                    )
            );

    public static final ModelResourceLocation STIRLING_ENGINE_FLYWHEEL_OUTER_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "block/stirling_engine/stirling_engine_flywheel_outer"
                    )
            );

    public static final ModelResourceLocation SEPARATED_SMALL_TIRE_BLOCK_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "item/wheels/small_tire/separated/separated_small_tire_block"
                    )
            );

    public static final ModelResourceLocation DOUBLE_SMALL_TIRE_BLOCK_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "item/wheels/small_tire/double/double_small_tire_block"
                    )
            );

    public static final ModelResourceLocation SEPARATED_TIRE_BLOCK_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "item/wheels/tire/separated/separated_tire_block"
                    )
            );

    public static final ModelResourceLocation DOUBLE_TIRE_BLOCK_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "item/wheels/tire/double/double_tire_block"
                    )
            );

    private static void registerCreateTooltips() {
        registerCreateTooltip(CreateMechanicalDrive.STEERING_WHEEL_MOUNT_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.RIGID_STEERING_WHEEL_MOUNT_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.RIGID_WHEEL_MOUNT_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.SPROCKET_MOUNT_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.IDLER_MOUNT_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.TORSION_MOUNT_ITEM.get());
        registerCreateTooltip(
                CreateMechanicalDrive.LONG_TORSION_MOUNT_ITEM.get()
        );
        registerCreateTooltip(CreateMechanicalDrive.DOUBLE_STEERING_WHEEL_MOUNT_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.DOUBLE_WHEEL_MOUNT_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.DOUBLE_RIGID_STEERING_WHEEL_MOUNT_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.DOUBLE_RIGID_WHEEL_MOUNT_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.SEPARATED_SMALL_TIRE_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.DOUBLE_SMALL_TIRE_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.SEPARATED_TIRE_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.DOUBLE_TIRE_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.SHAFT_MARKER_ITEM.get());
        registerCreateTooltip(CreateMechanicalDrive.SUSPENSION_STRUT_ITEM.get());
    }

    private static void registerCreateTooltip(Item item) {
        TooltipModifier.REGISTRY.register(
                item,
                new ItemDescription.Modifier(
                        item,
                        FontHelper.Palette.STANDARD_CREATE
                ).andThen(
                        TooltipModifier.mapNull(KineticStats.create(item))
                )
        );
    }

    @SubscribeEvent
    public static void clientSetup(
            FMLClientSetupEvent event
    ) {
        EngineBlockEntity.registerClientEffectsHandler(
                EngineClientEffects::tick
        );

        registerCreateTooltips();
        PonderIndex.addPlugin(new CreateMechanicalDrivePonderPlugin());

        NeoForge.EVENT_BUS.addListener(
                GearboxAxialLeverClientInput::onMouseButton
        );

        NeoForge.EVENT_BUS.addListener(
                GearboxAxialLeverClientInput::onClientTick
        );

        NeoForge.EVENT_BUS.addListener(
                GearboxAxialLeverClientInput::onCalculatePlayerTurn
        );

        NeoForge.EVENT_BUS.addListener(
                GearboxLinearLeverClientInput::onMouseButton
        );

        NeoForge.EVENT_BUS.addListener(
                GearboxLinearLeverClientInput::onClientTick
        );

        NeoForge.EVENT_BUS.addListener(
                GearboxLinearLeverClientInput::onCalculatePlayerTurn
        );

        NeoForge.EVENT_BUS.addListener(
                SteeringWheelClientInput::onMouseButton
        );

        NeoForge.EVENT_BUS.addListener(
                SteeringWheelClientInput::onClientTick
        );

        NeoForge.EVENT_BUS.addListener(
                SteeringWheelClientInput::onCalculatePlayerTurn
        );

        NeoForge.EVENT_BUS.addListener(
                SeatClientInput::onMouseButton
        );

        NeoForge.EVENT_BUS.addListener(
                SeatClientInput::onClientTick
        );

        NeoForge.EVENT_BUS.addListener(
                SeatClientInput::onCalculatePlayerTurn
        );

        NeoForge.EVENT_BUS.addListener(
                ChainGearSelectionClientInput::onClickInput
        );

        NeoForge.EVENT_BUS.addListener(
                ChainGearSelectionClientInput::onClientTick
        );

        NeoForge.EVENT_BUS.addListener(
                ChainGearSelectionClientInput::onRenderLevelStage
        );

        NeoForge.EVENT_BUS.addListener(
                CardanShaftPlacementClientInput::onClickInput
        );

        NeoForge.EVENT_BUS.addListener(
                CardanShaftPlacementClientInput::onClientTick
        );

        NeoForge.EVENT_BUS.addListener(
                CardanShaftPlacementClientInput::onRenderLevelStage
        );

        NeoForge.EVENT_BUS.addListener(
                SuspensionStrutPlacementClientInput::onClickInput
        );

        NeoForge.EVENT_BUS.addListener(
                SuspensionStrutPlacementClientInput::onClientTick
        );

        NeoForge.EVENT_BUS.addListener(
                SuspensionStrutPlacementClientInput::onRenderLevelStage
        );

        NeoForge.EVENT_BUS.addListener(
                CompactStrutInteractionClientInput::onClickInput
        );

        NeoForge.EVENT_BUS.addListener(
                RigidLinkPlacementClientInput::onClickInput
        );

        NeoForge.EVENT_BUS.addListener(
                RigidLinkPlacementClientInput::onClientTick
        );

        NeoForge.EVENT_BUS.addListener(
                RigidLinkPlacementClientInput::onRenderLevelStage
        );

        event.enqueueWork(() ->
                ItemProperties.register(
                        CreateMechanicalDrive
                                .STEERING_WHEEL_ITEM
                                .get(),
                        ResourceLocation.fromNamespaceAndPath(
                                CreateMechanicalDrive.MOD_ID,
                                "steering_wheel_color"
                        ),
                        (stack, level, entity, seed) -> {
                            BlockItemStateProperties properties =
                                    stack.getOrDefault(
                                            DataComponents.BLOCK_STATE,
                                            BlockItemStateProperties.EMPTY
                                    );

                            SteeringWheelColor color =
                                    properties.get(
                                            SteeringWheelBlock.COLOR
                                    );

                            if (color == null) {
                                return 0.0F;
                            }

                            return switch (color) {
                                case BLACK -> 0.0F;
                                case BLUE -> 1.0F;
                                case GRAY -> 2.0F;
                                case GREEN -> 3.0F;
                                case RED -> 4.0F;
                                case YELLOW -> 5.0F;
                            };
                        }
                )
        );

        event.enqueueWork(() ->
                ItemProperties.register(
                        CreateMechanicalDrive.SHAFT_MARKER_ITEM.get(),
                        ResourceLocation.fromNamespaceAndPath(
                                CreateMechanicalDrive.MOD_ID,
                                "shaft_marker_color"
                        ),
                        (stack, level, entity, seed) ->
                                ShaftMarkerItem.getColor(stack).getId()
                )
        );

        event.enqueueWork(() ->
                ItemProperties.register(
                        CreateMechanicalDrive
                                .SEAT_ITEM
                                .get(),
                        ResourceLocation.fromNamespaceAndPath(
                                CreateMechanicalDrive.MOD_ID,
                                "seat_color"
                        ),
                        (stack, level, entity, seed) -> {
                            BlockItemStateProperties properties =
                                    stack.getOrDefault(
                                            DataComponents.BLOCK_STATE,
                                            BlockItemStateProperties.EMPTY
                                    );

                            SeatColor color =
                                    properties.get(
                                            SeatBlock.COLOR
                                    );

                            if (color == null) {
                                return 0.0F;
                            }

                            return switch (color) {
                                case BLACK -> 0.0F;
                                case BLUE -> 1.0F;
                                case GRAY -> 2.0F;
                                case GREEN -> 3.0F;
                                case RED -> 4.0F;
                                case YELLOW -> 5.0F;
                            };
                        }
                )
        );
        event.enqueueWork(() ->
                ItemProperties.register(
                        CreateMechanicalDrive
                                .FLAT_SEAT_ITEM
                                .get(),
                        ResourceLocation.fromNamespaceAndPath(
                                CreateMechanicalDrive.MOD_ID,
                                "seat_color"
                        ),
                        (stack, level, entity, seed) -> {
                            BlockItemStateProperties properties =
                                    stack.getOrDefault(
                                            DataComponents.BLOCK_STATE,
                                            BlockItemStateProperties.EMPTY
                                    );

                            SeatColor color =
                                    properties.get(
                                            SeatBlock.COLOR
                                    );

                            if (color == null) {
                                return 0.0F;
                            }

                            return switch (color) {
                                case BLACK -> 0.0F;
                                case BLUE -> 1.0F;
                                case GRAY -> 2.0F;
                                case GREEN -> 3.0F;
                                case RED -> 4.0F;
                                case YELLOW -> 5.0F;
                            };
                        }
                )
        );
    }
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) {
                        return 0xFFFFFF;
                    }

                    if (level.getBlockEntity(pos) instanceof StirlingEngineHeaterBlockEntity heater) {
                        return StirlingEngineHeaterItem.getHeatTint(heater.getVisualHeatRatio());
                    }

                    return 0xFFFFFF;
                },
                CreateMechanicalDrive.STIRLING_ENGINE_HEATER.get()
        );
    }


    @SubscribeEvent
    public static void registerItemColors(
            RegisterColorHandlersEvent.Item event
    ) {
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex != 0) {
                        return 0xFFFFFF;
                    }

                    Minecraft minecraft =
                            Minecraft.getInstance();

                    float heatRatio =
                            minecraft.level != null
                                    ? StirlingEngineHeaterItem.getHeatRatio(
                                    stack,
                                    minecraft.level.getGameTime()
                            )
                                    : StirlingEngineHeaterItem.getHeatRatio(
                                    stack
                            );

                    return StirlingEngineHeaterItem.getHeatTint(
                            heatRatio
                    );
                },
                CreateMechanicalDrive.STIRLING_ENGINE_HEATER_ITEM.get()
        );
    }

    @SubscribeEvent
    public static void registerAdditionalModels(
            ModelEvent.RegisterAdditional event
    ) {
        for (DyeColor color : DyeColor.values()) {
            event.register(ShaftMarkerRenderer.modelLocation(color));
        }
        event.register(SprocketWheelRenderer.modelLocation());
        event.register(DriveWheelRenderer.modelLocation());
        event.register(DriveWheelRenderer.bigModelLocation());
        event.register(DriveWheelRenderer.bigInnerModelLocation());
        event.register(DriveWheelRenderer.bigOuterModelLocation());
        event.register(SupportWheelRenderer.modelLocation());
        event.register(IdlerMountRenderer.axleModelLocation());
        event.register(IdlerWheelRenderer.modelLocation());
        event.register(TrackAssemblyRenderer.narrowModelLocation());
        event.register(TrackAssemblyRenderer.wideModelLocation());
        event.register(TorsionMountRenderer.mountModelLocation());
        event.register(TorsionMountRenderer.reversedMountModelLocation());
        event.register(TorsionMountRenderer.armModelLocation());
        event.register(TorsionMountRenderer.emptyArmModelLocation());
        event.register(TorsionMountRenderer.reversedArmModelLocation());
        event.register(TorsionMountRenderer.reversedEmptyArmModelLocation());
        event.register(TorsionMountRenderer.longMountModelLocation());
        event.register(TorsionMountRenderer.longReversedMountModelLocation());
        event.register(TorsionMountRenderer.longArmModelLocation());
        event.register(TorsionMountRenderer.longEmptyArmModelLocation());
        event.register(TorsionMountRenderer.longReversedArmModelLocation());
        event.register(
                TorsionMountRenderer.longReversedEmptyArmModelLocation()
        );
        event.register(RIGID_STEERING_MOUNT_PART_MODEL);
        event.register(RIGID_WHEEL_MOUNT_PART_MODEL);
        event.register(GEARBOX_LEVER_AXIAL_HANDLE_MODEL);
        event.register(GEARBOX_LEVER_LINEAR_HANDLE_MODEL);
        event.register(HAND_CRANK_MODEL);
        event.register(HAND_CRANK_HANDLE_MODEL);
        event.register(
                MechanicalStarterRenderer.buttonModelLocation()
        );
        event.register(ENGINE_PIPES_MODEL);
        event.register(ENGINE_PIPES_HEATED_MODEL);
        event.register(ENGINE_PIPES_EXTENDED_MODEL);
        event.register(ENGINE_PIPES_EXTENDED_HEATED_MODEL);
        event.register(STEERING_WHEEL_MODEL);
        event.register(STEERING_WHEEL_BLUE_MODEL);
        event.register(STEERING_WHEEL_GRAY_MODEL);
        event.register(STEERING_WHEEL_GREEN_MODEL);
        event.register(STEERING_WHEEL_RED_MODEL);
        event.register(STEERING_WHEEL_YELLOW_MODEL);
        event.register(OVERRUNNING_CLUTCH_INNER_MIDDLE_MODEL);
        event.register(OVERRUNNING_CLUTCH_INNER_SIDES_MODEL);
        event.register(SHAFT_DISTRIBUTOR_FRAME_MODEL);
        event.register(FOUR_WAY_SHAFT_DISTRIBUTOR_FRAME_MODEL);
        event.register(SHAFT_DISTRIBUTOR_INPUT_SHAFT_MODEL);
        event.register(SHAFT_DISTRIBUTOR_HORIZONTAL_SHAFT_MODEL);
        event.register(SEAT_REAR_MODEL);
        event.register(SEAT_REAR_BLUE_MODEL);
        event.register(SEAT_REAR_GRAY_MODEL);
        event.register(SEAT_REAR_GREEN_MODEL);
        event.register(SEAT_REAR_RED_MODEL);
        event.register(SEAT_REAR_YELLOW_MODEL);
        event.register(WORM_GEAR_SMALL_ENCASED_MODEL);
        event.register(WORM_GEAR_REGULAR_ENCASED_MODEL);
        event.register(CHAIN_GEAR_M_MODEL);
        event.register(CHAIN_GEAR_T_MODEL);
        event.register(CHAIN_GEAR_B_MODEL);
        event.register(CHAIN_LINKAGE_MODEL);
        event.register(CARDAN_SHAFT_MODEL);
        event.register(CARDAN_SHAFT_CENTER_MODEL);
        event.register(CARDAN_SHAFT_EDGE_MODEL);
        event.register(CARDAN_JOINT_CONNECTOR_MODEL);
        event.register(SUSPENSION_STRUT_BALL_MODEL);
        event.register(SUSPENSION_STRUT_JOINT_MODEL);
        event.register(SUSPENSION_STRUT_SPRING_MODEL);
        event.register(SUSPENSION_STRUT_SHAFT_MODEL);
        event.register(SUSPENSION_STRUT_UPPER_SHAFT_MODEL);
        event.register(RIGID_LINK_SHAFT_MODEL);
        event.register(RIGID_LINK_SHAFT_DETAIL_MODEL);
        event.register(RIGID_LINK_CONNECTOR_MODEL);
        event.register(RIGID_LINK_LIMITED_CONNECTOR_MODEL);
        event.register(MECHANICAL_JACK_BASE_MODEL);
        event.register(MECHANICAL_JACK_INPUT_SHAFT_MODEL);
        event.register(MECHANICAL_JACK_SHAFT_1_MODEL);
        event.register(MECHANICAL_JACK_SHAFT_2_MODEL);
        event.register(MECHANICAL_JACK_TOP_MODEL);
        event.register(STIRLING_ENGINE_FLYWHEEL_INNER_MODEL);
        event.register(STIRLING_ENGINE_FLYWHEEL_OUTER_MODEL);
        event.register(SEPARATED_SMALL_TIRE_BLOCK_MODEL);
        event.register(DOUBLE_SMALL_TIRE_BLOCK_MODEL);
        event.register(SEPARATED_TIRE_BLOCK_MODEL);
        event.register(DOUBLE_TIRE_BLOCK_MODEL);
        event.register(STIRLING_ENGINE_HEATER_COVER_MODEL);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.GEARBOX_INPUT_BLOCK_ENTITY.get(),
                CarGearboxInputRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.DOG_CLUTCH_BLOCK_ENTITY.get(),
                DogClutchRenderer::new
        );

        event.registerBlockEntityRenderer(CreateMechanicalDrive.ENGINE_BLOCK_ENTITY.get(),
                EngineRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.TANK_TRANSMISSION_HOUSING_BLOCK_ENTITY.get(),
                TankTransmissionHousingRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.TANK_TRANSMISSION_DISTRIBUTOR_BLOCK_ENTITY.get(),
                TankTransmissionDistributorRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.TANK_TRANSMISSION_STEERING_BLOCK_ENTITY.get(),
                TankTransmissionSteeringRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.GEARBOX_SPEED_BLOCK_ENTITY.get(),
                CarGearboxSpeedRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.GEARBOX_AXIAL_LEVER_BLOCK_ENTITY.get(),
                GearboxAxialLeverRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.GEARBOX_LINEAR_LEVER_BLOCK_ENTITY.get(),
                GearboxLinearLeverRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.STEERING_WHEEL_BLOCK_ENTITY.get(),
                SteeringWheelRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.SHAFT_MARKER_BLOCK_ENTITY.get(),
                ShaftMarkerRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.SEAT_BLOCK_ENTITY.get(),
                SeatRenderer::new
        );

        event.registerEntityRenderer(
                CreateMechanicalDrive.SEAT_ENTITY.get(),
                SeatEntityRenderer::new
        );

        event.registerBlockEntityRenderer(CreateMechanicalDrive.HAND_CRANK_BLOCK_ENTITY.get(),
                HandCrankRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.MECHANICAL_STARTER_BLOCK_ENTITY.get(),
                MechanicalStarterRenderer::new
        );

        event.registerBlockEntityRenderer(CreateMechanicalDrive.GEAR_REDUCER_BLOCK_ENTITY.get(),
                GearReducerRenderer::new
        );

        event.registerBlockEntityRenderer(CreateMechanicalDrive.ROTARY_LIMITER_BLOCK_ENTITY.get(),
                RotaryLimiterRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.SHAFT_DISTRIBUTOR_BLOCK_ENTITY.get(),
                ShaftDistributorRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.FOUR_WAY_SHAFT_DISTRIBUTOR_BLOCK_ENTITY.get(),
                ShaftDistributorRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.ANGLE_GEAR_BLOCK_ENTITY.get(),
                AngleGearRenderer::new
        );

        event.registerBlockEntityRenderer(CreateMechanicalDrive.OVERRUNNING_CLUTCH_BLOCK_ENTITY.get(),
                OverrunningClutchRenderer::new
        );

        event.registerBlockEntityRenderer(CreateMechanicalDrive.WORM_GEAR_REGULAR_BLOCK_ENTITY.get(),
                WormGearRegularRenderer::new
        );

        event.registerBlockEntityRenderer(CreateMechanicalDrive.WORM_GEAR_SMALL_BLOCK_ENTITY.get(),
                WormGearSmallRenderer::new
        );

        event.registerBlockEntityRenderer(CreateMechanicalDrive.CHAIN_GEAR_BLOCK_ENTITY.get(),
                ChainGearRenderer::new
        );

        event.registerBlockEntityRenderer(CreateMechanicalDrive.CARDAN_JOINT_BLOCK_ENTITY.get(),
                CardanJointRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.SUSPENSION_STRUT_BLOCK_ENTITY.get(),
                SuspensionStrutRenderer::new
        );

        event.registerBlockEntityRenderer(CreateMechanicalDrive.RIGID_LINK_JOINT_BLOCK_ENTITY.get(),
                RigidLinkJointRenderer::new
        );

        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.MECHANICAL_JACK_BLOCK_ENTITY.get(),
                MechanicalJackRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.STEERING_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                SteeringWheelMountRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.DOUBLE_STEERING_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                DoubleSteeringWheelMountRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.DOUBLE_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                DoubleSteeringWheelMountRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.RIGID_STEERING_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                RigidSteeringWheelMountRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.DOUBLE_RIGID_STEERING_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                DoubleRigidSteeringWheelMountRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.DOUBLE_RIGID_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                DoubleRigidSteeringWheelMountRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.RIGID_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                RigidWheelMountRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.SPROCKET_MOUNT_BLOCK_ENTITY.get(),
                SprocketMountRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.IDLER_MOUNT_BLOCK_ENTITY.get(),
                IdlerMountRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.TORSION_MOUNT_BLOCK_ENTITY.get(),
                TorsionMountRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.STIRLING_ENGINE_OUTPUT_BLOCK_ENTITY.get(),
                StirlingEngineOutputRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.STIRLING_ENGINE_HEATER_BLOCK_ENTITY.get(),
                StirlingEngineHeaterRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.STIRLING_ENGINE_POWERED_SHAFT_BLOCK_ENTITY.get(),
                StirlingEnginePoweredShaftRenderer::new
        );
        event.registerBlockEntityRenderer(
                CreateMechanicalDrive.STIRLING_ENGINE_FLYWHEEL_BLOCK_ENTITY.get(),
                StirlingEngineFlywheelRenderer::new
        );
    }
}
