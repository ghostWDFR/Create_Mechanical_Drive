package dev.createmechanicaldrive;

import com.mojang.logging.LogUtils;
import dev.createmechanicaldrive.content.chain_linkage.ChainGearBlock;
import dev.createmechanicaldrive.content.chain_linkage.ChainGearBlockEntity;
import dev.createmechanicaldrive.content.cardan_shaft.CardanJointBlock;
import dev.createmechanicaldrive.content.cardan_shaft.CardanJointBlockEntity;
import dev.createmechanicaldrive.content.cardan_shaft.CardanShaftItem;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutBlock;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutBlockEntity;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutItem;
import dev.createmechanicaldrive.content.suspension_strut.SpringTuningWrenchItem;
import dev.createmechanicaldrive.content.tracks.mounts.idler.IdlerMountBlock;
import dev.createmechanicaldrive.content.tracks.mounts.idler.IdlerMountBlockEntity;
import dev.createmechanicaldrive.content.tracks.wheels.idler.IdlerWheelItem;
import dev.createmechanicaldrive.content.tracks.mounts.torsion.TorsionMountBlock;
import dev.createmechanicaldrive.content.tracks.mounts.torsion.TorsionMountBlockEntity;
import dev.createmechanicaldrive.content.tracks.mounts.long_torsion.LongTorsionMountBlock;
import dev.createmechanicaldrive.content.tracks.wheels.drive.BigDriveWheelItem;
import dev.createmechanicaldrive.content.tracks.wheels.drive.DriveWheelItem;
import dev.createmechanicaldrive.content.tracks.wheels.support.SupportWheelItem;
import dev.createmechanicaldrive.content.tracks.wheels.sprocket.SprocketWheelItem;
import dev.createmechanicaldrive.content.tracks.chain.TrackLinkItem;
import dev.createmechanicaldrive.content.tracks.chain.TrackEndpointContactPhysics;
import dev.createmechanicaldrive.content.tracks.chain.TrackType;
import dev.createmechanicaldrive.content.rigid_link.RigidLinkItem;
import dev.createmechanicaldrive.content.rigid_link.RigidLinkJointBlock;
import dev.createmechanicaldrive.content.rigid_link.RigidLinkJointBlockEntity;
import dev.createmechanicaldrive.content.angle_gear.AngleGearBlock;
import dev.createmechanicaldrive.content.angle_gear.AngleGearBlockEntity;
import dev.createmechanicaldrive.content.angle_gear.AngleGearItem;
import dev.createmechanicaldrive.content.engine.EngineBlock;
import dev.createmechanicaldrive.content.engine.EngineBlockEntity;
import dev.createmechanicaldrive.content.gearbox.CarGearboxInputBlock;
import dev.createmechanicaldrive.content.gearbox.CarGearboxInputBlockEntity;
import dev.createmechanicaldrive.content.gearbox.CarGearboxSpeedBlock;
import dev.createmechanicaldrive.content.gearbox.CarGearboxSpeedBlockEntity;
import dev.createmechanicaldrive.content.gearbox.GearboxAxialLeverBlock;
import dev.createmechanicaldrive.content.gearbox.GearboxAxialLeverBlockEntity;
import dev.createmechanicaldrive.content.gearbox.GearboxLinearLeverBlock;
import dev.createmechanicaldrive.content.gearbox.GearboxLinearLeverBlockEntity;
import dev.createmechanicaldrive.infrastructure.ponder.PonderGearboxLeverBlock;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionFrame;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionHousingBlock;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionHousingBlockEntity;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionDistributorBlock;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionDistributorBlockEntity;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionSteeringBlock;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionSteeringBlockEntity;
import dev.createmechanicaldrive.content.hand_crank.HandCrankBlock;
import dev.createmechanicaldrive.content.hand_crank.HandCrankBlockEntity;
import dev.createmechanicaldrive.content.hand_crank.HandCrankItem;
import dev.createmechanicaldrive.content.mechanical_starter.MechanicalStarterBlock;
import dev.createmechanicaldrive.content.mechanical_starter.MechanicalStarterBlockEntity;
import dev.createmechanicaldrive.content.steering_wheel.SteeringWheelBlock;
import dev.createmechanicaldrive.content.steering_wheel.SteeringWheelBlockEntity;
import dev.createmechanicaldrive.content.seats.FlatSeatBlock;
import dev.createmechanicaldrive.content.seats.SeatBlock;
import dev.createmechanicaldrive.content.seats.SeatBlockEntity;
import dev.createmechanicaldrive.content.seats.SeatEntity;
import dev.createmechanicaldrive.content.screwdriver.ScrewdriverItem;
import dev.createmechanicaldrive.content.adjustment_wrench.AdjustmentWrenchItem;
import dev.createmechanicaldrive.content.dog_clutch.DogClutchBlock;
import dev.createmechanicaldrive.content.dog_clutch.DogClutchBlockEntity;
import dev.createmechanicaldrive.content.gear_reducer.GearReducerBlock;
import dev.createmechanicaldrive.content.gear_reducer.GearReducerBlockEntity;
import dev.createmechanicaldrive.content.overrunning_clutch.OverrunningClutchBlock;
import dev.createmechanicaldrive.content.overrunning_clutch.OverrunningClutchBlockEntity;
import dev.createmechanicaldrive.content.shaft_distributor.FourWayShaftDistributorBlock;
import dev.createmechanicaldrive.content.shaft_distributor.ShaftDistributorBlock;
import dev.createmechanicaldrive.content.shaft_distributor.ShaftDistributorBlockEntity;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerBlock;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerBlockEntity;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import dev.createmechanicaldrive.content.tracks.mounts.sprocket.SprocketMountBlock;
import dev.createmechanicaldrive.content.tracks.mounts.sprocket.SprocketMountBlockEntity;
import dev.createmechanicaldrive.content.worm_gears.WormGearRegularBlock;
import dev.createmechanicaldrive.content.worm_gears.WormGearRegularBlockEntity;
import dev.createmechanicaldrive.content.worm_gears.WormGearSmallBlock;
import dev.createmechanicaldrive.content.worm_gears.WormGearSmallBlockEntity;
import dev.createmechanicaldrive.content.dog_clutch.DogClutchEngagedBlock;
import dev.createmechanicaldrive.content.mechanical_jack.MechanicalJackBlock;
import dev.createmechanicaldrive.content.mechanical_jack.MechanicalJackBlockEntity;
import dev.createmechanicaldrive.content.mechanical_jack.MechanicalJackHeadBlock;
import dev.createmechanicaldrive.content.steering_wheel_mount.SteeringWheelMountBlock;
import dev.createmechanicaldrive.content.steering_wheel_mount.SteeringWheelMountBlockEntity;
import dev.createmechanicaldrive.content.rigid_steering_wheel_mount.RigidSteeringWheelMountBlock;
import dev.createmechanicaldrive.content.rigid_steering_wheel_mount.RigidSteeringWheelMountBlockEntity;
import dev.createmechanicaldrive.content.rigid_wheel_mount.RigidWheelMountBlock;
import dev.createmechanicaldrive.content.rigid_wheel_mount.RigidWheelMountBlockEntity;
import dev.createmechanicaldrive.content.double_steering_wheel_mount.DoubleSteeringWheelMountBlock;
import dev.createmechanicaldrive.content.double_steering_wheel_mount.DoubleSteeringWheelMountBlockEntity;
import dev.createmechanicaldrive.content.double_rigid_steering_wheel_mount.DoubleRigidSteeringWheelMountBlock;
import dev.createmechanicaldrive.content.double_rigid_steering_wheel_mount.DoubleRigidSteeringWheelMountBlockEntity;
import dev.createmechanicaldrive.content.double_rigid_wheel_mount.DoubleRigidWheelMountBlock;
import dev.createmechanicaldrive.content.double_rigid_wheel_mount.DoubleRigidWheelMountBlockEntity;
import dev.createmechanicaldrive.content.double_wheel_mount.DoubleWheelMountBlock;
import dev.createmechanicaldrive.content.double_wheel_mount.DoubleWheelMountBlockEntity;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineCoreBlock;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineHeaterBlock;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineHeaterBlockEntity;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineHeaterItem;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineOutputBlock;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineOutputBlockEntity;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEnginePoweredShaftBlock;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEnginePoweredShaftBlockEntity;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineFlywheelBlock;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineFlywheelBlockEntity;
import dev.createmechanicaldrive.content.rotary_limiter.RotaryLimiterBlock;
import dev.createmechanicaldrive.content.rotary_limiter.RotaryLimiterBlockEntity;
import dev.createmechanicaldrive.content.service_tank.ServiceTankBlock;
import dev.createmechanicaldrive.content.service_tank.ServiceTankBlockEntity;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.content.items.tire.TireItem;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import dev.createmechanicaldrive.network.CreateMechanicalDriveNetwork;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.api.registry.CreateRegistries;
import dev.createmechanicaldrive.content.engine.display.EngineStatusDisplaySource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(CreateMechanicalDrive.MOD_ID)
public class CreateMechanicalDrive {
    public static final String MOD_ID = "mechanical_drive";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    MOD_ID
            );
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    private static final ResourceKey<CreativeModeTab> CREATE_PALETTES_TAB =
            creativeTabKey("create", "palettes");
    private static final ResourceKey<CreativeModeTab> SIMULATED_MAIN_TAB =
            creativeTabKey("simulated", "main_tab");

    public static final DeferredRegister<DisplaySource>
            DISPLAY_SOURCES =
            DeferredRegister.create(
                    CreateRegistries.DISPLAY_SOURCE,
                    MOD_ID
            );

    public static final DeferredBlock<ServiceTankBlock> SERVICE_TANK =
            BLOCKS.registerBlock(
                    "service_tank",
                    ServiceTankBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.COPPER)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<ServiceTankBlockEntity>
            > SERVICE_TANK_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "service_tank",
                    () -> BlockEntityType.Builder.of(
                                    CreateMechanicalDrive::createServiceTankBlockEntity,
                                    SERVICE_TANK.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<CarGearboxInputBlock> GEARBOX_INPUT = BLOCKS.registerBlock("car_gearbox_input",
            CarGearboxInputBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion());

    public static final DeferredBlock<EngineBlock> ENGINE =
            BLOCKS.registerBlock(
                    "engine",
                    EngineBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .lightLevel(state -> state.getValue(EngineBlock.LIT) ? 6 : 0)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<EngineBlockEntity>
            > ENGINE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "engine",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createEngineBlockEntity,
                                    ENGINE.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            DisplaySource,
            EngineStatusDisplaySource
            > ENGINE_STATUS_DISPLAY_SOURCE =
            DISPLAY_SOURCES.register(
                    "engine_status",
                    EngineStatusDisplaySource::new
            );

    public static final DeferredBlock<DogClutchBlock>
            DOG_CLUTCH =
            BLOCKS.registerBlock(
                    "dog_clutch",
                    DogClutchBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredBlock<
            DogClutchEngagedBlock
            > DOG_CLUTCH_ENGAGED =
            BLOCKS.registerBlock(
                    "dog_clutch_engaged",
                    DogClutchEngagedBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<DogClutchBlockEntity>
            > DOG_CLUTCH_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "dog_clutch",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createDogClutchBlockEntity,
                                    DOG_CLUTCH.get(),
                                    DOG_CLUTCH_ENGAGED.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<GearReducerBlock>
            GEAR_REDUCER =
            BLOCKS.registerBlock(
                    "gear_reducer",
                    GearReducerBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<GearReducerBlockEntity>
            > GEAR_REDUCER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "gear_reducer",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createGearReducerBlockEntity,
                                    GEAR_REDUCER.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<ShaftDistributorBlock>
            SHAFT_DISTRIBUTOR =
            BLOCKS.registerBlock(
                    "shaft_distributor",
                    ShaftDistributorBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<ShaftDistributorBlockEntity>
            > SHAFT_DISTRIBUTOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "shaft_distributor",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createShaftDistributorBlockEntity,
                                    SHAFT_DISTRIBUTOR.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<FourWayShaftDistributorBlock>
            FOUR_WAY_SHAFT_DISTRIBUTOR =
            BLOCKS.registerBlock(
                    "four_way_shaft_distributor",
                    FourWayShaftDistributorBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<ShaftDistributorBlockEntity>
            > FOUR_WAY_SHAFT_DISTRIBUTOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "four_way_shaft_distributor",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createFourWayShaftDistributorBlockEntity,
                                    FOUR_WAY_SHAFT_DISTRIBUTOR.get()
                            )
                            .build(null)
            );
    public static final DeferredBlock<AngleGearBlock>
            ANGLE_GEAR =
            BLOCKS.registerBlock(
                    "angle_gear",
                    AngleGearBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 3.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<AngleGearBlockEntity>
            > ANGLE_GEAR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "angle_gear",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createAngleGearBlockEntity,
                                    ANGLE_GEAR.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<RotaryLimiterBlock>
            ROTARY_LIMITER =
            BLOCKS.registerBlock(
                    "rotary_limiter",
                    RotaryLimiterBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<RotaryLimiterBlockEntity>
            > ROTARY_LIMITER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "rotary_limiter",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createRotaryLimiterBlockEntity,
                                    ROTARY_LIMITER.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<OverrunningClutchBlock>
            OVERRUNNING_CLUTCH =
            BLOCKS.registerBlock(
                    "overrunning_clutch",
                    OverrunningClutchBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<OverrunningClutchBlockEntity>
            > OVERRUNNING_CLUTCH_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "overrunning_clutch",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createOverrunningClutchBlockEntity,
                                    OVERRUNNING_CLUTCH.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<WormGearRegularBlock>
            WORM_GEAR_REGULAR =
            BLOCKS.registerBlock(
                    "worm_gear_regular",
                    WormGearRegularBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(1.5F, 3.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<WormGearRegularBlockEntity>
            > WORM_GEAR_REGULAR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "worm_gear_regular",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createWormGearRegularBlockEntity,
                                    WORM_GEAR_REGULAR.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<WormGearSmallBlock>
            WORM_GEAR_SMALL =
            BLOCKS.registerBlock(
                    "worm_gear_small",
                    WormGearSmallBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(1.5F, 3.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<WormGearSmallBlockEntity>
            > WORM_GEAR_SMALL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "worm_gear_small",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createWormGearSmallBlockEntity,
                                    WORM_GEAR_SMALL.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<ChainGearBlock>
            CHAIN_GEAR =
            BLOCKS.registerBlock(
                    "chain_gear",
                    ChainGearBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 3.0F)
                            .sound(SoundType.CHAIN)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<ChainGearBlockEntity>
            > CHAIN_GEAR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "chain_gear",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createChainGearBlockEntity,
                                    CHAIN_GEAR.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<CardanJointBlock>
            CARDAN_JOINT =
            BLOCKS.registerBlock(
                    "cardan_joint",
                    CardanJointBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 3.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .dynamicShape()
                            .noLootTable()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<CardanJointBlockEntity>
            > CARDAN_JOINT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "cardan_joint",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createCardanJointBlockEntity,
                                    CARDAN_JOINT.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<SuspensionStrutBlock>
            SUSPENSION_STRUT_JOINT =
            BLOCKS.registerBlock(
                    "suspension_strut_joint",
                    SuspensionStrutBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 3.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .noLootTable()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<SuspensionStrutBlockEntity>
            > SUSPENSION_STRUT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "suspension_strut_joint",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createSuspensionStrutBlockEntity,
                                    SUSPENSION_STRUT_JOINT.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<RigidLinkJointBlock>
            RIGID_LINK_JOINT =
            BLOCKS.registerBlock(
                    "rigid_link_joint",
                    RigidLinkJointBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<RigidLinkJointBlockEntity>
            > RIGID_LINK_JOINT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "rigid_link_joint",
                    () -> BlockEntityType.Builder.of(
                                    CreateMechanicalDrive::createRigidLinkJointBlockEntity,
                                    RIGID_LINK_JOINT.get())
                            .build(null)
            );

    public static final DeferredBlock<MechanicalJackBlock>
            MECHANICAL_JACK =
            BLOCKS.registerBlock(
                    "mechanical_jack",
                    MechanicalJackBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
                            .dynamicShape()
            );

    public static final DeferredBlock<MechanicalJackHeadBlock>
            MECHANICAL_JACK_HEAD =
            BLOCKS.registerBlock(
                    "mechanical_jack_head",
                    MechanicalJackHeadBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .noOcclusion()
                            .noLootTable()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<MechanicalJackBlockEntity>
            > MECHANICAL_JACK_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "mechanical_jack",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createMechanicalJackBlockEntity,
                                    MECHANICAL_JACK.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<SteeringWheelMountBlock> STEERING_WHEEL_MOUNT =
            BLOCKS.registerBlock(
                    "steering_wheel_mount",
                    SteeringWheelMountBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
                            .isRedstoneConductor((state, level, pos) -> false)
            );

    public static final DeferredBlock<DoubleSteeringWheelMountBlock>
            DOUBLE_STEERING_WHEEL_MOUNT =
            BLOCKS.registerBlock(
                    "double_steering_wheel_mount",
                    DoubleSteeringWheelMountBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(
                                    2.0F,
                                    6.0F
                            )
                            .sound(
                                    SoundType.NETHERITE_BLOCK
                            )
                            .noOcclusion()
                            .isRedstoneConductor(
                                    (
                                            state,
                                            level,
                                            pos
                                    ) -> false
                            )
            );

    public static final DeferredBlock<DoubleWheelMountBlock>
            DOUBLE_WHEEL_MOUNT =
            BLOCKS.registerBlock(
                    "double_wheel_mount",
                    DoubleWheelMountBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
                            .isRedstoneConductor(
                                    (state, level, pos) -> false
                            )
            );

    public static final DeferredBlock<DoubleRigidSteeringWheelMountBlock>
            DOUBLE_RIGID_STEERING_WHEEL_MOUNT =
            BLOCKS.registerBlock(
                    "double_rigid_steering_wheel_mount",
                    DoubleRigidSteeringWheelMountBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
                            .isRedstoneConductor(
                                    (state, level, pos) -> false
                            )
            );

    public static final DeferredBlock<DoubleRigidWheelMountBlock>
            DOUBLE_RIGID_WHEEL_MOUNT =
            BLOCKS.registerBlock(
                    "double_rigid_wheel_mount",
                    DoubleRigidWheelMountBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
                            .isRedstoneConductor(
                                    (state, level, pos) -> false
                            )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<SteeringWheelMountBlockEntity>
            > STEERING_WHEEL_MOUNT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "steering_wheel_mount",
                    () -> BlockEntityType.Builder.of(
                                    CreateMechanicalDrive::createSteeringWheelMountBlockEntity,
                                    STEERING_WHEEL_MOUNT.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<DoubleSteeringWheelMountBlockEntity>
            > DOUBLE_STEERING_WHEEL_MOUNT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "double_steering_wheel_mount",
                    () -> BlockEntityType.Builder.of(
                                    CreateMechanicalDrive
                                            ::createDoubleSteeringWheelMountBlockEntity,
                                    DOUBLE_STEERING_WHEEL_MOUNT.get()
                            )
                            .build(
                                    null
                            )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<DoubleWheelMountBlockEntity>
            > DOUBLE_WHEEL_MOUNT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "double_wheel_mount",
                    () -> BlockEntityType.Builder.of(
                                    CreateMechanicalDrive
                                            ::createDoubleWheelMountBlockEntity,
                                    DOUBLE_WHEEL_MOUNT.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<DoubleRigidSteeringWheelMountBlockEntity>
            > DOUBLE_RIGID_STEERING_WHEEL_MOUNT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "double_rigid_steering_wheel_mount",
                    () -> BlockEntityType.Builder.of(
                                    CreateMechanicalDrive
                                            ::createDoubleRigidSteeringWheelMountBlockEntity,
                                    DOUBLE_RIGID_STEERING_WHEEL_MOUNT.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<DoubleRigidWheelMountBlockEntity>
            > DOUBLE_RIGID_WHEEL_MOUNT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "double_rigid_wheel_mount",
                    () -> BlockEntityType.Builder.of(
                                    CreateMechanicalDrive
                                            ::createDoubleRigidWheelMountBlockEntity,
                                    DOUBLE_RIGID_WHEEL_MOUNT.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<RigidSteeringWheelMountBlock>
            RIGID_STEERING_WHEEL_MOUNT =
            BLOCKS.registerBlock(
                    "rigid_steering_wheel_mount",
                    RigidSteeringWheelMountBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
                            .isRedstoneConductor(
                                    (
                                            state,
                                            level,
                                            pos
                                    ) -> false
                            )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<RigidSteeringWheelMountBlockEntity>
            > RIGID_STEERING_WHEEL_MOUNT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "rigid_steering_wheel_mount",
                    () -> BlockEntityType.Builder.of(
                                    CreateMechanicalDrive
                                            ::createRigidSteeringWheelMountBlockEntity,
                                    RIGID_STEERING_WHEEL_MOUNT.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<RigidWheelMountBlock>
            RIGID_WHEEL_MOUNT =
            BLOCKS.registerBlock(
                    "rigid_wheel_mount",
                    RigidWheelMountBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(
                                    2.0F,
                                    6.0F
                            )
                            .sound(
                                    SoundType.NETHERITE_BLOCK
                            )
                            .noOcclusion()
                            .isRedstoneConductor(
                                    (
                                            state,
                                            level,
                                            pos
                                    ) -> false
                            )
            );

    public static final DeferredBlock<SprocketMountBlock>
            SPROCKET_MOUNT =
            BLOCKS.registerBlock(
                    "sprocket_mount",
                    SprocketMountBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
                            .dynamicShape()
                            .isRedstoneConductor(
                                    (state, level, pos) -> false
                            )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<SprocketMountBlockEntity>
            > SPROCKET_MOUNT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "sprocket_mount",
                    () -> BlockEntityType.Builder.of(
                                    CreateMechanicalDrive
                                            ::createSprocketMountBlockEntity,
                                    SPROCKET_MOUNT.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<IdlerMountBlock>
            IDLER_MOUNT =
            BLOCKS.registerBlock(
                    "idler_mount",
                    IdlerMountBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
                            .dynamicShape()
                            .isRedstoneConductor(
                                    (state, level, pos) -> false
                            )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<IdlerMountBlockEntity>
            > IDLER_MOUNT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "idler_mount",
                    () -> BlockEntityType.Builder.of(
                                    CreateMechanicalDrive
                                            ::createIdlerMountBlockEntity,
                                    IDLER_MOUNT.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<TorsionMountBlock>
            TORSION_MOUNT =
            BLOCKS.registerBlock(
                    "torsion_mount",
                    TorsionMountBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
                            .dynamicShape()
                            .isRedstoneConductor(
                                    (state, level, pos) -> false
                            )
            );

    public static final DeferredBlock<LongTorsionMountBlock>
            LONG_TORSION_MOUNT =
            BLOCKS.registerBlock(
                    "long_torsion_mount",
                    LongTorsionMountBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<TorsionMountBlockEntity>
            > TORSION_MOUNT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "torsion_mount",
                    () -> BlockEntityType.Builder.of(
                                    CreateMechanicalDrive
                                            ::createTorsionMountBlockEntity,
                                    TORSION_MOUNT.get(),
                                    LONG_TORSION_MOUNT.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CarGearboxInputBlockEntity>> GEARBOX_INPUT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("car_gearbox_input", () -> BlockEntityType.Builder
                    .of(CreateMechanicalDrive::createGearboxInputBlockEntity, GEARBOX_INPUT.get())
                    .build(null));

    public static final DeferredBlock<CarGearboxSpeedBlock> GEARBOX_SPEED = BLOCKS.registerBlock("car_gearbox_speed",
            CarGearboxSpeedBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CarGearboxSpeedBlockEntity>> GEARBOX_SPEED_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("car_gearbox_speed", () -> BlockEntityType.Builder
                    .of(CreateMechanicalDrive::createGearboxSpeedBlockEntity, GEARBOX_SPEED.get())
                    .build(null));

    public static final DeferredBlock<StirlingEngineHeaterBlock> STIRLING_ENGINE_HEATER =
            BLOCKS.registerBlock(
                    "stirling_engine_heater",
                    StirlingEngineHeaterBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .lightLevel(state -> state.getValue(StirlingEngineHeaterBlock.LIT) ? 8 : 0)
                            .noOcclusion()
            );

    public static final DeferredBlock<StirlingEngineCoreBlock> STIRLING_ENGINE_CORE =
            BLOCKS.registerBlock(
                    "stirling_engine_core",
                    StirlingEngineCoreBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
            );

    public static final DeferredBlock<StirlingEngineOutputBlock> STIRLING_ENGINE_OUTPUT =
            BLOCKS.registerBlock(
                    "stirling_engine_output",
                    StirlingEngineOutputBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
            );

    public static final DeferredBlock<StirlingEnginePoweredShaftBlock> STIRLING_ENGINE_POWERED_SHAFT =
            BLOCKS.registerBlock(
                    "stirling_engine_powered_shaft",
                    StirlingEnginePoweredShaftBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.8F, 3.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredBlock<StirlingEngineFlywheelBlock>
            STIRLING_ENGINE_FLYWHEEL =
            BLOCKS.registerBlock(
                    "stirling_engine_flywheel",
                    StirlingEngineFlywheelBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<StirlingEngineHeaterBlockEntity>
            > STIRLING_ENGINE_HEATER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "stirling_engine_heater",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive::createStirlingEngineHeaterBlockEntity,
                                    STIRLING_ENGINE_HEATER.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<StirlingEngineOutputBlockEntity>
            > STIRLING_ENGINE_OUTPUT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "stirling_engine_output",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive::createStirlingEngineOutputBlockEntity,
                                    STIRLING_ENGINE_OUTPUT.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<StirlingEngineFlywheelBlockEntity>
            > STIRLING_ENGINE_FLYWHEEL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "stirling_engine_flywheel",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive::createStirlingEngineFlywheelBlockEntity,
                                    STIRLING_ENGINE_FLYWHEEL.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<StirlingEnginePoweredShaftBlockEntity>
            > STIRLING_ENGINE_POWERED_SHAFT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "stirling_engine_powered_shaft",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive::createStirlingEnginePoweredShaftBlockEntity,
                                    STIRLING_ENGINE_POWERED_SHAFT.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<TankTransmissionFrame>
            TANK_TRANSMISSION_FRAME =
            BLOCKS.registerBlock(
                    "tank_transmission_frame",
                    TankTransmissionFrame::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            );

    public static final DeferredBlock<TankTransmissionHousingBlock>
            TANK_TRANSMISSION_HOUSING =
            BLOCKS.registerBlock(
                    "tank_transmission_housing",
                    TankTransmissionHousingBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
            );

    public static final DeferredBlock<TankTransmissionDistributorBlock>
            TANK_TRANSMISSION_DISTRIBUTOR =
            BLOCKS.registerBlock(
                    "tank_transmission_distributor",
                    TankTransmissionDistributorBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
            );

    public static final DeferredBlock<TankTransmissionSteeringBlock>
            TANK_TRANSMISSION_STEERING =
            BLOCKS.registerBlock(
                    "tank_transmission_steering",
                    TankTransmissionSteeringBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
            );

    public static final DeferredBlock<SteeringWheelBlock>
            STEERING_WHEEL =
            BLOCKS.registerBlock(
                    "steering_wheel",
                    SteeringWheelBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(0.5F, 0.5F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            );

    public static final DeferredBlock<ShaftMarkerBlock>
            SHAFT_MARKER =
            BLOCKS.registerBlock(
                    "shaft_marker",
                    ShaftMarkerBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(state -> state
                                    .getValue(ShaftMarkerBlock.COLOR)
                                    .getMapColor())
                            .strength(0.5F, 0.5F)
                            .sound(SoundType.WOOL)
                            .noOcclusion()
            );

    public static final DeferredBlock<SeatBlock>
            SEAT =
            BLOCKS.registerBlock(
                    "seat",
                    SeatBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(0.5F, 0.5F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredBlock<FlatSeatBlock>
            FLAT_SEAT =
            BLOCKS.registerBlock(
                    "flat_seat",
                    FlatSeatBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(0.5F, 0.5F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            EntityType<?>,
            EntityType<SeatEntity>
            > SEAT_ENTITY =
            ENTITY_TYPES.register(
                    "seat",
                    () -> EntityType.Builder
                            .<SeatEntity>of(
                                    SeatEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(
                                    0.25F,
                                    0.35F
                            )
                            .clientTrackingRange(
                                    10
                            )
                            .updateInterval(
                                    Integer.MAX_VALUE
                            )
                            .build(
                                    "seat"
                            )
            );

    public static final DeferredBlock<HandCrankBlock>
            HAND_CRANK =
            BLOCKS.registerBlock(
                    "hand_crank",
                    HandCrankBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.0F, 2.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<HandCrankBlockEntity>
            > HAND_CRANK_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "hand_crank",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createHandCrankBlockEntity,
                                    HAND_CRANK.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<MechanicalStarterBlock>
            MECHANICAL_STARTER =
            BLOCKS.registerBlock(
                    "mechanical_starter",
                    MechanicalStarterBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<MechanicalStarterBlockEntity>
            > MECHANICAL_STARTER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "mechanical_starter",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createMechanicalStarterBlockEntity,
                                    MECHANICAL_STARTER.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<SteeringWheelBlockEntity>
            > STEERING_WHEEL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "steering_wheel",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createSteeringWheelBlockEntity,
                                    STEERING_WHEEL.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<ShaftMarkerBlockEntity>
            > SHAFT_MARKER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "shaft_marker",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createShaftMarkerBlockEntity,
                                    SHAFT_MARKER.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<SeatBlockEntity>
            > SEAT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "seat",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createSeatBlockEntity,
                                    SEAT.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<TankTransmissionSteeringBlockEntity>
            > TANK_TRANSMISSION_STEERING_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "tank_transmission_steering",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createTankTransmissionSteeringBlockEntity,
                                    TANK_TRANSMISSION_STEERING.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<TankTransmissionDistributorBlockEntity>
            > TANK_TRANSMISSION_DISTRIBUTOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "tank_transmission_distributor",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive
                                            ::createTankTransmissionDistributorBlockEntity,
                                    TANK_TRANSMISSION_DISTRIBUTOR.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<TankTransmissionHousingBlockEntity>
            > TANK_TRANSMISSION_HOUSING_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "tank_transmission_housing",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive::createTankTransmissionHousingBlockEntity,
                                    TANK_TRANSMISSION_HOUSING.get()
                            )
                            .build(null)
            );

    public static final DeferredBlock<GearboxAxialLeverBlock> GEARBOX_AXIAL_LEVER =
            BLOCKS.registerBlock("gearbox_lever_axial",
                    GearboxAxialLeverBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(0.5F, 0.5F)
                            .sound(SoundType.WOOD)
                            .noOcclusion());

    public static final DeferredBlock<GearboxLinearLeverBlock> GEARBOX_LINEAR_LEVER =
            BLOCKS.registerBlock(
                    "gearbox_lever_linear",
                    GearboxLinearLeverBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(0.5F, 0.5F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredBlock<PonderGearboxLeverBlock> PONDER_GEARBOX_LEVER =
            BLOCKS.registerBlock(
                    "ponder_gearbox_lever",
                    PonderGearboxLeverBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(0.5F, 0.5F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GearboxAxialLeverBlockEntity>> GEARBOX_AXIAL_LEVER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("gearbox_lever_axial", () -> BlockEntityType.Builder
                    .of(CreateMechanicalDrive::createGearShiftLeverBlockEntity,
                            GEARBOX_AXIAL_LEVER.get(),
                            PONDER_GEARBOX_LEVER.get())
                    .build(null));

    public static final DeferredItem<BlockItem> GEARBOX_INPUT_ITEM =
            ITEMS.registerSimpleBlockItem("car_gearbox_input", GEARBOX_INPUT);

    public static final DeferredItem<BlockItem> SERVICE_TANK_ITEM =
            ITEMS.registerSimpleBlockItem("service_tank", SERVICE_TANK);

    public static final DeferredItem<BlockItem> ENGINE_ITEM =
            ITEMS.registerSimpleBlockItem("engine", ENGINE);

    public static final DeferredItem<BlockItem> GEARBOX_SPEED_ITEM =
            ITEMS.registerSimpleBlockItem("car_gearbox_speed", GEARBOX_SPEED);

    public static final DeferredItem<StirlingEngineHeaterItem> STIRLING_ENGINE_HEATER_ITEM =
            ITEMS.register(
                    "stirling_engine_heater",
                    () -> new StirlingEngineHeaterItem(
                            STIRLING_ENGINE_HEATER.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<Item> STIRLING_ENGINE_HEATER_COVER_ITEM =
            ITEMS.register(
                    "stirling_engine_heater_cover",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<BlockItem> STIRLING_ENGINE_CORE_ITEM =
            ITEMS.registerSimpleBlockItem("stirling_engine_core", STIRLING_ENGINE_CORE);

    public static final DeferredItem<BlockItem> STIRLING_ENGINE_OUTPUT_ITEM =
            ITEMS.registerSimpleBlockItem("stirling_engine_output", STIRLING_ENGINE_OUTPUT);

    public static final DeferredItem<BlockItem>
            STIRLING_ENGINE_FLYWHEEL_ITEM =
            ITEMS.registerSimpleBlockItem("stirling_engine_flywheel", STIRLING_ENGINE_FLYWHEEL);

    public static final DeferredItem<BlockItem> DOG_CLUTCH_ITEM =
            ITEMS.registerSimpleBlockItem("dog_clutch", DOG_CLUTCH_ENGAGED);

    public static final DeferredItem<BlockItem> GEAR_REDUCER_ITEM =
            ITEMS.registerSimpleBlockItem("gear_reducer", GEAR_REDUCER);

    public static final DeferredItem<BlockItem> ROTARY_LIMITER_ITEM =
            ITEMS.registerSimpleBlockItem("rotary_limiter", ROTARY_LIMITER);

    public static final DeferredItem<BlockItem> SHAFT_DISTRIBUTOR_ITEM =
            ITEMS.registerSimpleBlockItem("shaft_distributor", SHAFT_DISTRIBUTOR);
    public static final DeferredItem<BlockItem> FOUR_WAY_SHAFT_DISTRIBUTOR_ITEM =
            ITEMS.registerSimpleBlockItem(
                    "four_way_shaft_distributor",
                    FOUR_WAY_SHAFT_DISTRIBUTOR
            );

    public static final DeferredItem<AngleGearItem> ANGLE_GEAR_ITEM =
            ITEMS.register("angle_gear", () -> new AngleGearItem(ANGLE_GEAR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> OVERRUNNING_CLUTCH_ITEM =
            ITEMS.registerSimpleBlockItem("overrunning_clutch", OVERRUNNING_CLUTCH);

    public static final DeferredItem<BlockItem> WORM_GEAR_REGULAR_ITEM =
            ITEMS.registerSimpleBlockItem("worm_gear_regular", WORM_GEAR_REGULAR);

    public static final DeferredItem<BlockItem> WORM_GEAR_SMALL_ITEM =
            ITEMS.registerSimpleBlockItem("worm_gear_small", WORM_GEAR_SMALL);

    public static final DeferredItem<BlockItem> CHAIN_GEAR_ITEM =
            ITEMS.registerSimpleBlockItem("chain_gear", CHAIN_GEAR);

    public static final DeferredItem<BlockItem> MECHANICAL_JACK_ITEM =
            ITEMS.registerSimpleBlockItem("mechanical_jack", MECHANICAL_JACK);

    public static final DeferredItem<BlockItem> STEERING_WHEEL_MOUNT_ITEM =
            ITEMS.register(
                    "steering_wheel_mount",
                    () -> new BlockItem(
                            STEERING_WHEEL_MOUNT.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<BlockItem>
            RIGID_STEERING_WHEEL_MOUNT_ITEM =
            ITEMS.register(
                    "rigid_steering_wheel_mount",
                    () -> new BlockItem(
                            RIGID_STEERING_WHEEL_MOUNT.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<BlockItem>
            RIGID_WHEEL_MOUNT_ITEM =
            ITEMS.register(
                    "rigid_wheel_mount",
                    () -> new BlockItem(
                            RIGID_WHEEL_MOUNT.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<BlockItem>
            SPROCKET_MOUNT_ITEM =
            ITEMS.registerSimpleBlockItem(
                    "sprocket_mount",
                    SPROCKET_MOUNT
            );

    public static final DeferredItem<BlockItem>
            IDLER_MOUNT_ITEM =
            ITEMS.registerSimpleBlockItem(
                    "idler_mount",
                    IDLER_MOUNT
            );

    public static final DeferredItem<BlockItem>
            TORSION_MOUNT_ITEM =
            ITEMS.registerSimpleBlockItem(
                    "torsion_mount",
                    TORSION_MOUNT
            );

    public static final DeferredItem<BlockItem>
            LONG_TORSION_MOUNT_ITEM =
            ITEMS.registerSimpleBlockItem(
                    "long_torsion_mount",
                    LONG_TORSION_MOUNT
            );

    public static final DeferredItem<SprocketWheelItem>
            SPROCKET_WHEEL_ITEM =
            ITEMS.register(
                    "sprocket_wheel",
                    () -> new SprocketWheelItem(new Item.Properties())
            );

    public static final DeferredItem<DriveWheelItem>
            DRIVE_WHEEL_ITEM =
            ITEMS.register(
                    "drive_wheel",
                    () -> new DriveWheelItem(new Item.Properties())
            );

    public static final DeferredItem<BigDriveWheelItem> DRIVE_WHEEL_BIG_ITEM =
            ITEMS.register(
                    "drive_wheel_big",
                    () -> new BigDriveWheelItem(new Item.Properties())
            );

    public static final DeferredItem<SupportWheelItem>
            SUPPORT_WHEEL_ITEM =
            ITEMS.register(
                    "support_wheel",
                    () -> new SupportWheelItem(new Item.Properties())
            );

    public static final DeferredItem<IdlerWheelItem>
            IDLER_WHEEL_ITEM =
            ITEMS.register(
                    "idler_wheel",
                    () -> new IdlerWheelItem(new Item.Properties())
            );

    public static final DeferredItem<TrackLinkItem>
            NARROW_TRACK_LINK_ITEM =
            ITEMS.register(
                    "track_single_narrow",
                    () -> new TrackLinkItem(
                            TrackType.NARROW,
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<TrackLinkItem>
            WIDE_TRACK_LINK_ITEM =
            ITEMS.register(
                    "track_single_wide",
                    () -> new TrackLinkItem(
                            TrackType.WIDE,
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<BlockItem>
            DOUBLE_STEERING_WHEEL_MOUNT_ITEM =
            ITEMS.register(
                    "double_steering_wheel_mount",
                    () -> new BlockItem(
                            DOUBLE_STEERING_WHEEL_MOUNT.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<BlockItem>
            DOUBLE_WHEEL_MOUNT_ITEM =
            ITEMS.register(
                    "double_wheel_mount",
                    () -> new BlockItem(
                            DOUBLE_WHEEL_MOUNT.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<BlockItem>
            DOUBLE_RIGID_STEERING_WHEEL_MOUNT_ITEM =
            ITEMS.register(
                    "double_rigid_steering_wheel_mount",
                    () -> new BlockItem(
                            DOUBLE_RIGID_STEERING_WHEEL_MOUNT.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<BlockItem>
            DOUBLE_RIGID_WHEEL_MOUNT_ITEM =
            ITEMS.register(
                    "double_rigid_wheel_mount",
                    () -> new BlockItem(
                            DOUBLE_RIGID_WHEEL_MOUNT.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<TireItem> SEPARATED_SMALL_TIRE_ITEM =
            ITEMS.register(
                    "separated_small_tire",
                    () -> new TireItem(
                            new Item.Properties().component(
                                    OffroadDataComponents.TIRE,
                                    smallTireWithModel(
                                            "item/wheels/small_tire/separated/separated_small_tire_block"
                                    )
                            )
                    )
            );

    public static final DeferredItem<TireItem> DOUBLE_SMALL_TIRE_ITEM =
            ITEMS.register(
                    "double_small_tire",
                    () -> new TireItem(
                            new Item.Properties().component(
                                    OffroadDataComponents.TIRE,
                                    smallTireWithModel(
                                            "item/wheels/small_tire/double/double_small_tire_block"
                                    )
                            )
                    )
            );

    public static final DeferredItem<TireItem> SEPARATED_TIRE_ITEM =
            ITEMS.register(
                    "separated_tire",
                    () -> new TireItem(
                            new Item.Properties().component(
                                    OffroadDataComponents.TIRE,
                                    tireWithModel(
                                            TireLike.TIRE,
                                            "item/wheels/tire/separated/separated_tire_block"
                                    )
                            )
                    )
            );

    public static final DeferredItem<TireItem> DOUBLE_TIRE_ITEM =
            ITEMS.register(
                    "double_tire",
                    () -> new TireItem(
                            new Item.Properties().component(
                                    OffroadDataComponents.TIRE,
                                    tireWithModel(
                                            TireLike.TIRE,
                                            "item/wheels/tire/double/double_tire_block"
                                    )
                            )
                    )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<RigidWheelMountBlockEntity>
            > RIGID_WHEEL_MOUNT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "rigid_wheel_mount",
                    () -> BlockEntityType.Builder.of(
                                    CreateMechanicalDrive
                                            ::createRigidWheelMountBlockEntity,
                                    RIGID_WHEEL_MOUNT.get()
                            )
                            .build(null)
            );

    public static final DeferredItem<CardanShaftItem> CARDAN_SHAFT_ITEM =
            ITEMS.register(
                    "cardan_shaft",
                    () -> new CardanShaftItem(
                            CARDAN_JOINT.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<SuspensionStrutItem> SUSPENSION_STRUT_ITEM =
            ITEMS.register(
                    "suspension_strut",
                    () -> new SuspensionStrutItem(
                            SUSPENSION_STRUT_JOINT.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<Item> CARDAN_YOKE_ELEMENT_ITEM =
            ITEMS.register(
                    "cardan_yoke_element",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> IRON_ROD_ITEM =
            ITEMS.register(
                    "iron_rod",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> SHORT_TORSION_BAR_ITEM =
            ITEMS.register(
                    "short_torsion_bar",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> TORSION_MOUNT_COMPONENT_ITEM =
            ITEMS.register(
                    "torsion_mount_component",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> LONG_TORSION_BAR_ITEM =
            ITEMS.register(
                    "long_torsion_bar",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> LONG_TORSION_MOUNT_COMPONENT_ITEM =
            ITEMS.register(
                    "long_torsion_mount_component",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> TANK_WHEEL_BASE_ITEM =
            ITEMS.register(
                    "tank_wheel_base",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> TANK_DRIVE_WHEEL_ITEM =
            ITEMS.register(
                    "tank_drive_wheel",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> TANK_BIG_DRIVE_WHEEL_ITEM =
            ITEMS.register(
                    "tank_big_drive_wheel",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> TANK_IDLER_WHEEL_ITEM =
            ITEMS.register(
                    "tank_idler_wheel",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> TANK_SPROCKET_WHEEL_ITEM =
            ITEMS.register(
                    "tank_sprocket_wheel",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> SCREWDRIVER_ITEM =
            ITEMS.register(
                    "screwdriver",
                    () -> new ScrewdriverItem(new Item.Properties())
            );

    public static final DeferredItem<Item> ADJUSTMENT_WRENCH_ITEM =
            ITEMS.register(
                    "adjustment_wrench",
                    () -> new AdjustmentWrenchItem(new Item.Properties())
            );

    public static final DeferredItem<Item> SPRING_TUNING_WRENCH_ITEM =
            ITEMS.register(
                    "spring_tuning_wrench",
                    () -> new SpringTuningWrenchItem(new Item.Properties())
            );

    public static final DeferredItem<Item> CARDAN_ELEMENT_ITEM =
            ITEMS.register(
                    "cardan_element",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<BlockItem> RIGID_LINK_JOINT_ITEM =
            ITEMS.registerSimpleBlockItem("rigid_link_joint", RIGID_LINK_JOINT);

    public static final DeferredItem<RigidLinkItem> RIGID_LINK_ITEM =
            ITEMS.register(
                    "rigid_link",
                    () -> new RigidLinkItem(new Item.Properties(),
                            RigidLinkJointBlockEntity.LinkType.FREE)
            );

    public static final DeferredItem<RigidLinkItem> RIGID_LINK_LIMITED_ITEM =
            ITEMS.register(
                    "rigid_link_limited",
                    () -> new RigidLinkItem(new Item.Properties(),
                            RigidLinkJointBlockEntity.LinkType.LIMITED)
            );

    public static final DeferredItem<Item> BEARING_BALLS_ITEM =
            ITEMS.register(
                    "bearing_balls",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> BEARING_ELEMENT_ITEM =
            ITEMS.register(
                    "bearing_element",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> LINK_ELEMENT_FREE_ITEM =
            ITEMS.register(
                    "link_element_free",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> LINK_ELEMENT_LIMITED_ITEM =
            ITEMS.register(
                    "link_element_limited",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<Item> CHAIN_LINKAGE_ITEM =
            ITEMS.register(
                    "chain_linkage",
                    () -> new Item(
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<BlockItem> TANK_TRANSMISSION_FRAME_ITEM =
            ITEMS.registerSimpleBlockItem("tank_transmission_frame", TANK_TRANSMISSION_FRAME
            );

    public static final DeferredItem<BlockItem> TANK_TRANSMISSION_HOUSING_ITEM =
            ITEMS.register(
                    "tank_transmission_housing",
                    () -> new BlockItem(
                            TANK_TRANSMISSION_HOUSING.get(),
                            new Item.Properties()
                    ) {
                        @Override
                        public InteractionResult useOn(UseOnContext context) {
                            BlockPos clickedPos =
                                    context.getClickedPos();

                            BlockState clickedState =
                                    context.getLevel()
                                            .getBlockState(clickedPos);

                            boolean insertingIntoFrame =
                                    clickedState.is(
                                            TANK_TRANSMISSION_FRAME.get()
                                    );

                            boolean placingOnTransmissionBlock =
                                    context.getClickedFace() == Direction.UP
                                            && (
                                            clickedState.is(
                                                    TANK_TRANSMISSION_HOUSING.get()
                                            )
                                                    || clickedState.is(
                                                    TANK_TRANSMISSION_DISTRIBUTOR.get()
                                            )
                                    );

                            if (!insertingIntoFrame
                                    && !placingOnTransmissionBlock) {
                                return InteractionResult.FAIL;
                            }

                            BlockPos pos =
                                    insertingIntoFrame
                                            ? clickedPos
                                            : clickedPos.above();

                            if (placingOnTransmissionBlock
                                    && !context.getLevel()
                                    .getBlockState(pos)
                                    .canBeReplaced()) {
                                return InteractionResult.FAIL;
                            }

                            Direction facing =
                                    context.getHorizontalDirection().getOpposite();

                            boolean frameOnly;

                            if (insertingIntoFrame) {
                                frameOnly =
                                        clickedState.getValue(
                                                TankTransmissionFrame.FRAME_ONLY
                                        );
                            } else {
                                frameOnly = true;
                            }

                            BlockState stateBelow =
                                    context.getLevel()
                                            .getBlockState(pos.below());

                            boolean vertical =
                                    stateBelow.is(
                                            TANK_TRANSMISSION_HOUSING.get()
                                    )
                                            || stateBelow.is(
                                            TANK_TRANSMISSION_DISTRIBUTOR.get()
                                    );

                            BlockState housingState =
                                    TANK_TRANSMISSION_HOUSING
                                            .get()
                                            .defaultBlockState()
                                            .setValue(
                                                    TankTransmissionHousingBlock.FACING,
                                                    facing
                                            )
                                            .setValue(
                                                    TankTransmissionHousingBlock.AXIS,
                                                    facing.getAxis()
                                            )
                                            .setValue(
                                                    TankTransmissionHousingBlock.FRAME_ONLY,
                                                    frameOnly
                                            );

                            if (!context.getLevel().isClientSide) {
                                context.getLevel().setBlock(
                                        pos,
                                        housingState,
                                        3
                                );

                                context.getLevel().playSound(
                                        null,
                                        pos,
                                        SoundType.NETHERITE_BLOCK.getPlaceSound(),
                                        SoundSource.BLOCKS,
                                        (SoundType.NETHERITE_BLOCK.getVolume() + 1.0F) / 2.0F,
                                        SoundType.NETHERITE_BLOCK.getPitch() * 0.8F
                                );

                                if (context.getPlayer() == null
                                        || !context.getPlayer()
                                        .getAbilities()
                                        .instabuild) {
                                    context.getItemInHand().shrink(1);
                                }
                            }

                            return InteractionResult.SUCCESS;
                        }
                    }
            );

    public static final DeferredItem<BlockItem>
            TANK_TRANSMISSION_DISTRIBUTOR_ITEM =
            ITEMS.register(
                    "tank_transmission_distributor",
                    () -> new BlockItem(
                            TANK_TRANSMISSION_DISTRIBUTOR.get(),
                            new Item.Properties()
                    ) {
                        @Override
                        public InteractionResult useOn(
                                UseOnContext context
                        ) {
                            BlockPos pos =
                                    context.getClickedPos();

                            BlockPos clickedPos =
                                    context.getClickedPos();

                            BlockState clickedState =
                                    context.getLevel()
                                            .getBlockState(clickedPos);

                            boolean insertingIntoFrame =
                                    clickedState.is(
                                            TANK_TRANSMISSION_FRAME.get()
                                    );

                            boolean placingOnTransmissionBlock =
                                    context.getClickedFace() == Direction.UP
                                            && (
                                            clickedState.is(
                                                    TANK_TRANSMISSION_HOUSING.get()
                                            )
                                                    || clickedState.is(
                                                    TANK_TRANSMISSION_DISTRIBUTOR.get()
                                            )
                                    );

                            if (!insertingIntoFrame
                                    && !placingOnTransmissionBlock) {
                                return InteractionResult.FAIL;
                            }

                            if (placingOnTransmissionBlock
                                    && !context.getLevel()
                                    .getBlockState(pos)
                                    .canBeReplaced()) {
                                return InteractionResult.FAIL;
                            }

                            Direction facing =
                                    context.getHorizontalDirection();

                            boolean frameOnly;

                            if (insertingIntoFrame) {
                                frameOnly =
                                        clickedState.getValue(
                                                TankTransmissionFrame.FRAME_ONLY
                                        );
                            } else {
                                frameOnly = true;
                            }

                            BlockState distributorState =
                                    TANK_TRANSMISSION_DISTRIBUTOR
                                            .get()
                                            .defaultBlockState()
                                            .setValue(
                                                    TankTransmissionDistributorBlock.FACING,
                                                    facing
                                            )
                                            .setValue(
                                                    TankTransmissionDistributorBlock.AXIS,
                                                    facing.getAxis()
                                            )
                                            .setValue(
                                                    TankTransmissionDistributorBlock.FRAME_ONLY,
                                                    frameOnly
                                            );

                            if (!context.getLevel().isClientSide) {
                                context.getLevel().setBlock(
                                        pos,
                                        distributorState,
                                        3
                                );

                                context.getLevel().playSound(
                                        null,
                                        pos,
                                        SoundType.NETHERITE_BLOCK
                                                .getPlaceSound(),
                                        SoundSource.BLOCKS,
                                        (SoundType.NETHERITE_BLOCK.getVolume() + 1.0F) / 2.0F,
                                        SoundType.NETHERITE_BLOCK.getPitch() * 0.8F
                                );

                                if (
                                        context.getPlayer() == null
                                                || !context.getPlayer()
                                                .getAbilities()
                                                .instabuild
                                ) {
                                    context.getItemInHand()
                                            .shrink(1);
                                }
                            }

                            return InteractionResult.SUCCESS;
                        }
                    }
            );

    public static final DeferredItem<BlockItem>
            TANK_TRANSMISSION_STEERING_ITEM =
            ITEMS.register(
                    "tank_transmission_steering",
                    () -> new BlockItem(
                            TANK_TRANSMISSION_STEERING.get(),
                            new Item.Properties()
                    ) {
                        @Override
                        public InteractionResult useOn(
                                UseOnContext context
                        ) {
                            BlockPos clickedPos =
                                    context.getClickedPos();

                            BlockState clickedState =
                                    context.getLevel()
                                            .getBlockState(clickedPos);

                            boolean insertingIntoFrame =
                                    clickedState.is(
                                            TANK_TRANSMISSION_FRAME.get()
                                    );

                            boolean placingOnTransmissionBlock =
                                    context.getClickedFace() == Direction.UP
                                            && (
                                            clickedState.is(
                                                    TANK_TRANSMISSION_HOUSING.get()
                                            )
                                                    || clickedState.is(
                                                    TANK_TRANSMISSION_DISTRIBUTOR.get()
                                            )
                                    );

                            if (!insertingIntoFrame
                                    && !placingOnTransmissionBlock) {
                                return InteractionResult.FAIL;
                            }

                            BlockPos targetPos =
                                    insertingIntoFrame
                                            ? clickedPos
                                            : clickedPos.above();

                            if (placingOnTransmissionBlock
                                    && !context.getLevel()
                                    .getBlockState(targetPos)
                                    .canBeReplaced()) {
                                return InteractionResult.FAIL;
                            }

                            boolean frameOnly;

                            if (insertingIntoFrame) {
                                frameOnly =
                                        clickedState.getValue(
                                                TankTransmissionFrame.FRAME_ONLY
                                        );
                            } else {
                                frameOnly = true;
                            }

                            BlockState stateBelow =
                                    context.getLevel()
                                            .getBlockState(targetPos.below());

                            boolean vertical =
                                    stateBelow.is(
                                            TANK_TRANSMISSION_HOUSING.get()
                                    )
                                            || stateBelow.is(
                                            TANK_TRANSMISSION_DISTRIBUTOR.get()
                                    );

                            Direction facing =
                                    context.getHorizontalDirection();

                            if (vertical
                                    && stateBelow.is(
                                    TANK_TRANSMISSION_DISTRIBUTOR.get()
                            )) {
                                facing =
                                        stateBelow.getValue(
                                                TankTransmissionDistributorBlock.FACING
                                        );
                            }

                            BlockState steeringState =
                                    TANK_TRANSMISSION_STEERING
                                            .get()
                                            .defaultBlockState()
                                            .setValue(
                                                    TankTransmissionSteeringBlock.FACING,
                                                    facing
                                            )
                                            .setValue(
                                                    TankTransmissionSteeringBlock.AXIS,
                                                    facing.getAxis()
                                            )
                                            .setValue(
                                                    TankTransmissionSteeringBlock.FRAME_ONLY,
                                                    frameOnly
                                            )
                                            .setValue(
                                                    TankTransmissionSteeringBlock.VERTICAL,
                                                    vertical
                                            );

                            if (!context.getLevel().isClientSide) {
                                context.getLevel().setBlock(
                                        targetPos,
                                        steeringState,
                                        3
                                );

                                context.getLevel().playSound(
                                        null,
                                        targetPos,
                                        SoundType.NETHERITE_BLOCK.getPlaceSound(),
                                        SoundSource.BLOCKS,
                                        (SoundType.NETHERITE_BLOCK.getVolume() + 1.0F) / 2.0F,
                                        SoundType.NETHERITE_BLOCK.getPitch() * 0.8F
                                );

                                if (
                                        context.getPlayer() == null
                                                || !context.getPlayer()
                                                .getAbilities()
                                                .instabuild
                                ) {
                                    context.getItemInHand()
                                            .shrink(1);
                                }
                            }

                            return InteractionResult.SUCCESS;
                        }
                    }
            );



    public static final DeferredItem<BlockItem>
            TANK_TRANSMISSION_DISTRIBUTOR_ASSEMBLED_ITEM =
            ITEMS.register(
                    "tank_transmission_distributor_assembled",
                    () -> new BlockItem(
                            TANK_TRANSMISSION_DISTRIBUTOR.get(),
                            new Item.Properties()
                    ) {
                        @Override
                        public String getDescriptionId() {
                            return "item.mechanical_drive.tank_transmission_distributor_assembled";
                        }
                    }
            );

    public static final DeferredItem<BlockItem> TANK_TRANSMISSION_HOUSING_ASSEMBLED_ITEM =
            ITEMS.register(
                    "tank_transmission_housing_assembled",
                    () -> new BlockItem(
                            TANK_TRANSMISSION_HOUSING.get(),
                            new Item.Properties()
                    ) {
                        @Override
                        public String getDescriptionId() {
                            return "item.mechanical_drive.tank_transmission_housing_assembled";
                        }
                    }
            );

    public static final DeferredItem<BlockItem>
            TANK_TRANSMISSION_STEERING_ASSEMBLED_ITEM =
            ITEMS.register(
                    "tank_transmission_steering_assembled",
                    () -> new BlockItem(
                            TANK_TRANSMISSION_STEERING.get(),
                            new Item.Properties()
                    ) {
                        @Override
                        public String getDescriptionId() {
                            return "item.mechanical_drive.tank_transmission_steering_assembled";
                        }
                    }
            );

    public static final DeferredItem<BlockItem> GEARBOX_AXIAL_LEVER_ITEM =
            ITEMS.registerSimpleBlockItem("gearbox_lever_axial", GEARBOX_AXIAL_LEVER);

    public static final DeferredItem<BlockItem> GEARBOX_LINEAR_LEVER_ITEM =
            ITEMS.registerSimpleBlockItem("gearbox_lever_linear", GEARBOX_LINEAR_LEVER);

    public static final DeferredItem<BlockItem> STEERING_WHEEL_ITEM =
            ITEMS.register(
                    "steering_wheel",
                    () -> new BlockItem(
                            STEERING_WHEEL.get(),
                            new Item.Properties()
                                    .stacksTo(16)
                    )
            );

    public static final DeferredItem<ShaftMarkerItem> SHAFT_MARKER_ITEM =
            ITEMS.register(
                    "shaft_marker",
                    () -> new ShaftMarkerItem(
                            SHAFT_MARKER.get(),
                            new Item.Properties().component(
                                    DataComponents.BLOCK_STATE,
                                    BlockItemStateProperties.EMPTY.with(
                                            ShaftMarkerBlock.COLOR,
                                            DyeColor.WHITE
                                    )
                            )
                    )
            );

    public static final DeferredItem<BlockItem> SEAT_ITEM =
            ITEMS.registerSimpleBlockItem(
                    "seat",
                    SEAT
            );

    public static final DeferredItem<BlockItem> FLAT_SEAT_ITEM =
            ITEMS.registerSimpleBlockItem(
                    "flat_seat",
                    FLAT_SEAT
            );

    public static final DeferredItem<HandCrankItem>
            HAND_CRANK_ITEM =
            ITEMS.register(
                    "hand_crank",
                    () -> new HandCrankItem(
                            HAND_CRANK.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<BlockItem>
            MECHANICAL_STARTER_ITEM =
            ITEMS.registerSimpleBlockItem(
                    "mechanical_starter",
                    MECHANICAL_STARTER
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mechanical_drive"))
                    .withTabsAfter(CREATE_PALETTES_TAB, SIMULATED_MAIN_TAB)
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> STEERING_WHEEL_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(SERVICE_TANK_ITEM.get());
                        output.accept(GEARBOX_INPUT_ITEM.get());
                        output.accept(GEARBOX_SPEED_ITEM.get());
                        output.accept(STIRLING_ENGINE_HEATER_ITEM.get());
                        output.accept(STIRLING_ENGINE_HEATER_COVER_ITEM.get());
                        output.accept(STIRLING_ENGINE_CORE_ITEM.get());
                        output.accept(STIRLING_ENGINE_OUTPUT_ITEM.get());
                        output.accept(STIRLING_ENGINE_FLYWHEEL_ITEM.get());
                        output.accept(DOG_CLUTCH_ITEM.get());
                        output.accept(GEAR_REDUCER_ITEM.get());
                        output.accept(SHAFT_DISTRIBUTOR_ITEM.get());
                        output.accept(FOUR_WAY_SHAFT_DISTRIBUTOR_ITEM.get());
                        output.accept(ANGLE_GEAR_ITEM.get());
                        output.accept(ROTARY_LIMITER_ITEM.get());
                        output.accept(OVERRUNNING_CLUTCH_ITEM.get());
                        output.accept(WORM_GEAR_REGULAR_ITEM.get());
                        output.accept(WORM_GEAR_SMALL_ITEM.get());
                        output.accept(MECHANICAL_JACK_ITEM.get());
                        output.accept(STEERING_WHEEL_MOUNT_ITEM.get());
                        output.accept(DOUBLE_STEERING_WHEEL_MOUNT_ITEM.get());
                        output.accept(DOUBLE_RIGID_STEERING_WHEEL_MOUNT_ITEM.get());
                        output.accept(DOUBLE_RIGID_WHEEL_MOUNT_ITEM.get());
                        output.accept(DOUBLE_WHEEL_MOUNT_ITEM.get());
                        output.accept(RIGID_STEERING_WHEEL_MOUNT_ITEM.get());
                        output.accept(RIGID_WHEEL_MOUNT_ITEM.get());
                        output.accept(SPROCKET_MOUNT_ITEM.get());
                        output.accept(SPROCKET_WHEEL_ITEM.get());
                        output.accept(DRIVE_WHEEL_ITEM.get());
                        output.accept(DRIVE_WHEEL_BIG_ITEM.get());
                        output.accept(SUPPORT_WHEEL_ITEM.get());
                        output.accept(TORSION_MOUNT_ITEM.get());
                        output.accept(LONG_TORSION_MOUNT_ITEM.get());
                        output.accept(IDLER_MOUNT_ITEM.get());
                        output.accept(IDLER_WHEEL_ITEM.get());
                        output.accept(NARROW_TRACK_LINK_ITEM.get());
                        output.accept(WIDE_TRACK_LINK_ITEM.get());
                        output.accept(SEPARATED_SMALL_TIRE_ITEM.get());
                        output.accept(DOUBLE_SMALL_TIRE_ITEM.get());
                        output.accept(SEPARATED_TIRE_ITEM.get());
                        output.accept(DOUBLE_TIRE_ITEM.get());
                        output.accept(CHAIN_GEAR_ITEM.get());
                        output.accept(CHAIN_LINKAGE_ITEM.get());
                        output.accept(IRON_ROD_ITEM.get());
                        output.accept(SHORT_TORSION_BAR_ITEM.get());
                        output.accept(TORSION_MOUNT_COMPONENT_ITEM.get());
                        output.accept(LONG_TORSION_BAR_ITEM.get());
                        output.accept(LONG_TORSION_MOUNT_COMPONENT_ITEM.get());
                        output.accept(TANK_WHEEL_BASE_ITEM.get());
                        output.accept(TANK_DRIVE_WHEEL_ITEM.get());
                        output.accept(TANK_BIG_DRIVE_WHEEL_ITEM.get());
                        output.accept(TANK_IDLER_WHEEL_ITEM.get());
                        output.accept(TANK_SPROCKET_WHEEL_ITEM.get());
                        output.accept(CARDAN_YOKE_ELEMENT_ITEM.get());
                        output.accept(CARDAN_ELEMENT_ITEM.get());
                        output.accept(CARDAN_SHAFT_ITEM.get());
                        output.accept(SUSPENSION_STRUT_ITEM.get());
                        output.accept(RIGID_LINK_JOINT_ITEM.get());
                        output.accept(BEARING_BALLS_ITEM.get());
                        output.accept(BEARING_ELEMENT_ITEM.get());
                        output.accept(LINK_ELEMENT_FREE_ITEM.get());
                        output.accept(RIGID_LINK_ITEM.get());
                        output.accept(LINK_ELEMENT_LIMITED_ITEM.get());
                        output.accept(RIGID_LINK_LIMITED_ITEM.get());
                        output.accept(SCREWDRIVER_ITEM.get());
                        output.accept(ADJUSTMENT_WRENCH_ITEM.get());
                        output.accept(SPRING_TUNING_WRENCH_ITEM.get());
                        output.accept(ENGINE_ITEM.get());
                        output.accept(TANK_TRANSMISSION_FRAME_ITEM.get());
                        output.accept(TANK_TRANSMISSION_HOUSING_ITEM.get());
                        output.accept(GEARBOX_AXIAL_LEVER_ITEM.get());
                        output.accept(GEARBOX_LINEAR_LEVER_ITEM.get());
                        output.accept(STEERING_WHEEL_ITEM.get());
                        for (DyeColor color : DyeColor.values()) {
                            output.accept(ShaftMarkerItem.colored(
                                    new ItemStack(SHAFT_MARKER_ITEM.get()),
                                    color
                            ));
                        }
                        output.accept(SEAT_ITEM.get());
                        output.accept(FLAT_SEAT_ITEM.get());
                        output.accept(HAND_CRANK_ITEM.get());
                        output.accept(MECHANICAL_STARTER_ITEM.get());
                    })
                    .build());

    private static ResourceKey<CreativeModeTab> creativeTabKey(String namespace, String path) {
        return ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(namespace, path)
        );
    }

    private static TireLike smallTireWithModel(String modelPath) {
        return tireWithModel(TireLike.SMALL_TIRE, modelPath);
    }

    private static TireLike tireWithModel(TireLike tire, String modelPath) {
        return new TireLike(
                tire.radius(),
                tire.rotation(),
                tire.offset().add(0.0D, -0.5D, 0.0D),
                ResourceLocation.fromNamespaceAndPath(MOD_ID, modelPath)
        );
    }

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<GearboxLinearLeverBlockEntity>
            > GEARBOX_LINEAR_LEVER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "gearbox_lever_linear",
                    () -> BlockEntityType.Builder
                            .of(
                                    CreateMechanicalDrive::createGearboxLinearLeverBlockEntity,
                                    GEARBOX_LINEAR_LEVER.get()
                            )
                            .build(null)
            );

    private static GearboxLinearLeverBlockEntity createGearboxLinearLeverBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new GearboxLinearLeverBlockEntity(
                GEARBOX_LINEAR_LEVER_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static SteeringWheelMountBlockEntity createSteeringWheelMountBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new SteeringWheelMountBlockEntity(
                STEERING_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static DoubleSteeringWheelMountBlockEntity
    createDoubleSteeringWheelMountBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new DoubleSteeringWheelMountBlockEntity(
                DOUBLE_STEERING_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static DoubleWheelMountBlockEntity
    createDoubleWheelMountBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new DoubleWheelMountBlockEntity(
                DOUBLE_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static DoubleRigidSteeringWheelMountBlockEntity
    createDoubleRigidSteeringWheelMountBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new DoubleRigidSteeringWheelMountBlockEntity(
                DOUBLE_RIGID_STEERING_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static DoubleRigidWheelMountBlockEntity
    createDoubleRigidWheelMountBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new DoubleRigidWheelMountBlockEntity(
                DOUBLE_RIGID_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static RigidSteeringWheelMountBlockEntity
    createRigidSteeringWheelMountBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new RigidSteeringWheelMountBlockEntity(
                RIGID_STEERING_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static RigidWheelMountBlockEntity
    createRigidWheelMountBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new RigidWheelMountBlockEntity(
                RIGID_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static ServiceTankBlockEntity createServiceTankBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new ServiceTankBlockEntity(
                SERVICE_TANK_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    public CreateMechanicalDrive(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(CreateMechanicalDriveNetwork::registerPayloads);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        DISPLAY_SOURCES.register(modEventBus);
        SteeringWheelMountBlockEntity.registerPhysicsCallback();
        RigidSteeringWheelMountBlockEntity.registerPhysicsCallback();
        DoubleRigidSteeringWheelMountBlockEntity.registerPhysicsCallback();
        RigidWheelMountBlockEntity.registerPhysicsCallback();
        TorsionMountBlockEntity.registerPhysicsCallback();
        TrackEndpointContactPhysics.registerPhysicsCallback();
    }

    private void registerCapabilities(
            RegisterCapabilitiesEvent event
    ) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ENGINE_BLOCK_ENTITY.get(),
                (engine, side) ->
                        engine.getLavaFuelHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                SERVICE_TANK_BLOCK_ENTITY.get(),
                (tank, side) -> tank.getTankInventory()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                STEERING_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                (mount, side) -> mount.getInventory()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                RIGID_STEERING_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                (mount, side) -> mount.getInventory()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                RIGID_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                (mount, side) -> mount.getInventory()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                DOUBLE_STEERING_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                (mount, side) -> mount.getInventory()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                DOUBLE_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                (mount, side) -> mount.getInventory()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                DOUBLE_RIGID_STEERING_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                (mount, side) -> mount.getInventory()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                DOUBLE_RIGID_WHEEL_MOUNT_BLOCK_ENTITY.get(),
                (mount, side) -> mount.getInventory()
        );
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DisplaySource.BY_BLOCK_ENTITY.add(
                    ENGINE_BLOCK_ENTITY.get(),
                    ENGINE_STATUS_DISPLAY_SOURCE.get()
            );

            registerBlockMovementChecks();
        });

        LOGGER.info(
                "Create Mechanical Drive common setup complete"
        );
    }

    private static void registerBlockMovementChecks() {
        BlockMovementChecks.registerMovementNecessaryCheck(
                (state, level, pos) -> isMechanicalJackAssemblyBlock(
                        state
                )
                        ? CheckResult.SUCCESS
                        : CheckResult.PASS
        );

        BlockMovementChecks.registerNotSupportiveCheck(
                (state, direction) -> isMechanicalJackAssemblyBlock(
                        state
                )
                        ? CheckResult.FAIL
                        : CheckResult.PASS
        );

        BlockMovementChecks.registerNotSupportiveCheck(
                (state, direction) -> (state.is(RIGID_LINK_JOINT.get())
                        || state.is(SUSPENSION_STRUT_JOINT.get()))
                        && direction != state.getValue(
                                RigidLinkJointBlock.FACING
                        ).getOpposite()
                        ? CheckResult.SUCCESS
                        : CheckResult.PASS
        );
    }

    private static boolean isMechanicalJackAssemblyBlock(
            BlockState state
    ) {
        return state.is(
                MECHANICAL_JACK.get()
        ) || state.is(
                MECHANICAL_JACK_HEAD.get()
        );
    }

    private static EngineBlockEntity createEngineBlockEntity(
            BlockPos pos, BlockState state
    ) {
        return new EngineBlockEntity(ENGINE_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static CarGearboxInputBlockEntity createGearboxInputBlockEntity(
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state
    ) {
        return new CarGearboxInputBlockEntity(GEARBOX_INPUT_BLOCK_ENTITY.get(), pos, state);
    }

    private static CarGearboxSpeedBlockEntity createGearboxSpeedBlockEntity(
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state
    ) {
        return new CarGearboxSpeedBlockEntity(GEARBOX_SPEED_BLOCK_ENTITY.get(), pos, state);
    }

    private static StirlingEngineHeaterBlockEntity createStirlingEngineHeaterBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new StirlingEngineHeaterBlockEntity(
                STIRLING_ENGINE_HEATER_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static StirlingEngineOutputBlockEntity createStirlingEngineOutputBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new StirlingEngineOutputBlockEntity(
                STIRLING_ENGINE_OUTPUT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static StirlingEnginePoweredShaftBlockEntity createStirlingEnginePoweredShaftBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new StirlingEnginePoweredShaftBlockEntity(
                STIRLING_ENGINE_POWERED_SHAFT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static StirlingEngineFlywheelBlockEntity
    createStirlingEngineFlywheelBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new StirlingEngineFlywheelBlockEntity(
                STIRLING_ENGINE_FLYWHEEL_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static DogClutchBlockEntity createDogClutchBlockEntity(
            BlockPos pos, BlockState state
    ) {
        return new DogClutchBlockEntity(DOG_CLUTCH_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static TankTransmissionHousingBlockEntity createTankTransmissionHousingBlockEntity(
            BlockPos pos, BlockState state
    ) {
        return new TankTransmissionHousingBlockEntity(TANK_TRANSMISSION_HOUSING_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static TankTransmissionDistributorBlockEntity
    createTankTransmissionDistributorBlockEntity(
            BlockPos pos, BlockState state
    ) {
        return new TankTransmissionDistributorBlockEntity(TANK_TRANSMISSION_DISTRIBUTOR_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static TankTransmissionSteeringBlockEntity
    createTankTransmissionSteeringBlockEntity(
            BlockPos pos, BlockState state
    ) {
        return new TankTransmissionSteeringBlockEntity(TANK_TRANSMISSION_STEERING_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static SteeringWheelBlockEntity
    createSteeringWheelBlockEntity(
            BlockPos pos, BlockState state
    ) {
        return new SteeringWheelBlockEntity(STEERING_WHEEL_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static ShaftMarkerBlockEntity createShaftMarkerBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new ShaftMarkerBlockEntity(
                SHAFT_MARKER_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static SprocketMountBlockEntity createSprocketMountBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new SprocketMountBlockEntity(
                SPROCKET_MOUNT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static IdlerMountBlockEntity createIdlerMountBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new IdlerMountBlockEntity(
                IDLER_MOUNT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static TorsionMountBlockEntity createTorsionMountBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new TorsionMountBlockEntity(
                TORSION_MOUNT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static SeatBlockEntity createSeatBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new SeatBlockEntity(
                SEAT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static HandCrankBlockEntity
    createHandCrankBlockEntity(BlockPos pos, BlockState state
    ) {
        return new HandCrankBlockEntity(HAND_CRANK_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static MechanicalStarterBlockEntity
    createMechanicalStarterBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new MechanicalStarterBlockEntity(
                MECHANICAL_STARTER_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static GearReducerBlockEntity
    createGearReducerBlockEntity(BlockPos pos, BlockState state
    ) {
        return new GearReducerBlockEntity(GEAR_REDUCER_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static RotaryLimiterBlockEntity
    createRotaryLimiterBlockEntity(BlockPos pos, BlockState state
    ) {
        return new RotaryLimiterBlockEntity(ROTARY_LIMITER_BLOCK_ENTITY.get(), pos, state);
    }

    private static ShaftDistributorBlockEntity
    createShaftDistributorBlockEntity(BlockPos pos, BlockState state
    ) {
        return new ShaftDistributorBlockEntity(SHAFT_DISTRIBUTOR_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static ShaftDistributorBlockEntity
    createFourWayShaftDistributorBlockEntity(BlockPos pos, BlockState state
    ) {
        return new ShaftDistributorBlockEntity(
                FOUR_WAY_SHAFT_DISTRIBUTOR_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }
    private static AngleGearBlockEntity
    createAngleGearBlockEntity(BlockPos pos, BlockState state
    ) {
        return new AngleGearBlockEntity(ANGLE_GEAR_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static OverrunningClutchBlockEntity
    createOverrunningClutchBlockEntity(BlockPos pos, BlockState state
    ) {
        return new OverrunningClutchBlockEntity(OVERRUNNING_CLUTCH_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static WormGearRegularBlockEntity
    createWormGearRegularBlockEntity(BlockPos pos, BlockState state
    ) {
        return new WormGearRegularBlockEntity(WORM_GEAR_REGULAR_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static WormGearSmallBlockEntity
    createWormGearSmallBlockEntity(BlockPos pos, BlockState state
    ) {
        return new WormGearSmallBlockEntity(WORM_GEAR_SMALL_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static ChainGearBlockEntity
    createChainGearBlockEntity(BlockPos pos, BlockState state
    ) {
        return new ChainGearBlockEntity(CHAIN_GEAR_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static CardanJointBlockEntity
    createCardanJointBlockEntity(BlockPos pos, BlockState state
    ) {
        return new CardanJointBlockEntity(CARDAN_JOINT_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static SuspensionStrutBlockEntity
    createSuspensionStrutBlockEntity(BlockPos pos, BlockState state
    ) {
        return new SuspensionStrutBlockEntity(
                SUSPENSION_STRUT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    private static RigidLinkJointBlockEntity
    createRigidLinkJointBlockEntity(BlockPos pos, BlockState state
    ) {
        return new RigidLinkJointBlockEntity(RIGID_LINK_JOINT_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static MechanicalJackBlockEntity
    createMechanicalJackBlockEntity(BlockPos pos, BlockState state
    ) {
        return new MechanicalJackBlockEntity(MECHANICAL_JACK_BLOCK_ENTITY.get(), pos, state
        );
    }

    private static GearboxAxialLeverBlockEntity createGearShiftLeverBlockEntity(
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state
    ) {
        return new GearboxAxialLeverBlockEntity(GEARBOX_AXIAL_LEVER_BLOCK_ENTITY.get(), pos, state);
    }
}
