package dev.createmechanicaldrive.infrastructure.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.gearbox.CarGearboxInputBlock;
import dev.createmechanicaldrive.content.gearbox.CarGearboxSpeedBlock;
import dev.createmechanicaldrive.content.gearbox.GearboxAxialLeverBlock;
import dev.createmechanicaldrive.content.gearbox.GearboxLinearLeverBlock;
import dev.createmechanicaldrive.content.gearbox.GearboxPosition;
import dev.createmechanicaldrive.content.dog_clutch.DogClutchBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import dev.createmechanicaldrive.content.gear_reducer.GearReducerBlock;
import dev.createmechanicaldrive.content.gear_reducer.GearReducerBlockEntity;
import dev.createmechanicaldrive.content.overrunning_clutch.OverrunningClutchBlock;
import dev.createmechanicaldrive.content.overrunning_clutch.OverrunningClutchBlockEntity;
import dev.createmechanicaldrive.content.overrunning_clutch.OverrunningClutchDirection;
import dev.createmechanicaldrive.content.engine.EngineBlock;
import dev.createmechanicaldrive.content.engine.EngineBlockEntity;
import dev.createmechanicaldrive.content.hand_crank.HandCrankBlock;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionDistributorBlock;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionDistributorBlockEntity;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionFrame;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionHousingBlock;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionSteeringBlock;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionSteeringBlockEntity;
import dev.createmechanicaldrive.content.steering_wheel.SteeringWheelBlock;
import dev.createmechanicaldrive.content.steering_wheel.SteeringWheelBlockEntity;
import dev.createmechanicaldrive.content.steering_wheel.SteeringWheelColor;
import dev.createmechanicaldrive.content.worm_gears.WormGearRegularBlock;
import dev.createmechanicaldrive.content.worm_gears.WormGearSmallBlock;
import net.minecraft.world.item.Items;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

final class PonderSceneCommon {
    static final Direction GEARBOX_FACING = Direction.EAST;
    static final ResourceLocation ASSEMBLY_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "chain_drive/relay");
    static final ResourceLocation CONTROLS_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "chain_drive/relay");
    static final ResourceLocation SPEED_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "mechanical_press/pressing");
    static final ResourceLocation HAND_CRANK_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "chain_drive/relay");
    static final ResourceLocation STEERING_WHEEL_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "chain_drive/relay");
    static final ResourceLocation SEAT_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "chain_drive/relay");
    static final ResourceLocation DOG_CLUTCH_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "chain_drive/relay");
    static final ResourceLocation GEAR_REDUCER_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "chain_drive/relay");
    static final ResourceLocation OVERRUNNING_CLUTCH_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "chain_drive/relay");
    static final ResourceLocation ENGINE_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "chain_drive/relay");
    static final ResourceLocation STIRLING_ENGINE_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "high_logistics/stock_link");
    static final ResourceLocation TANK_TRANSMISSION_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "chain_drive/relay");
    static final ResourceLocation WORM_GEAR_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "chain_drive/relay");
    private PonderSceneCommon() {
    }

    static GearboxPositions placeCompleteGearbox(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            GearboxPosition position
    ) {
        BlockPos outputShaft = util.grid().at(1, 1, 2);
        BlockPos speed = util.grid().at(2, 1, 2);
        BlockPos input = util.grid().at(3, 1, 2);
        BlockPos sourceShaft = util.grid().at(4, 1, 2);
        BlockPos lever = input.above();

        BlockState cogwheel = AllBlocks.COGWHEEL.getDefaultState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);

        scene.world().setBlock(outputShaft, cogwheel, false);
        scene.world().setBlock(speed, speedState(GEARBOX_FACING, position), false);
        scene.world().setBlock(input, inputState(GEARBOX_FACING, position), false);
        scene.world().setBlock(sourceShaft, cogwheel, false);

        return new GearboxPositions(outputShaft, speed, input, sourceShaft, lever);
    }

    static void showAxialLever(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            GearboxPositions positions,
            GearboxPosition position
    ) {
        scene.world().setBlock(positions.lever(), ponderLeverState(GEARBOX_FACING, position), false);
        scene.overlay().showControls(util.vector().topOf(positions.input()), Pointing.DOWN, 40)
                .rightClick()
                .withItem(new ItemStack(CreateMechanicalDrive.GEARBOX_AXIAL_LEVER_ITEM.get()));
        scene.world().showSection(util.select().position(positions.lever()), Direction.DOWN);
        scene.effects().indicateSuccess(positions.lever());
        scene.idle(15);
    }

    static void shiftTo(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            GearboxPositions positions,
            GearboxPosition position,
            float outputSpeed
    ) {
        scene.world().modifyBlock(positions.input(),
                state -> state.setValue(CarGearboxInputBlock.POSITION, position), false);

        scene.world().modifyBlock(positions.speed(),
                state -> state.setValue(CarGearboxSpeedBlock.POSITION, position), false);

        scene.world().modifyBlock(positions.lever(), state -> {
            if (state.is(CreateMechanicalDrive.GEARBOX_AXIAL_LEVER.get())) {
                return state.setValue(GearboxAxialLeverBlock.POSITION, position);
            }
            if (state.is(CreateMechanicalDrive.PONDER_GEARBOX_LEVER.get())) {
                return state.setValue(PonderGearboxLeverBlock.POSITION, position);
            }
            return state.setValue(GearboxLinearLeverBlock.POSITION, position);
        }, false);
        scene.world().setKineticSpeed(outputSelection(util, positions), outputSpeed);
        scene.effects().indicateSuccess(positions.lever());
        scene.effects().rotationSpeedIndicator(positions.sourceShaft());
        scene.effects().rotationSpeedIndicator(positions.outputShaft());
        scene.idle(10);
    }

    static void animateSteeringWheel(
            CreateSceneBuilder scene,
            BlockPos wheel,
            float startAngle,
            float endAngle,
            int steps,
            int ticksPerStep
    ) {
        for (int step = 1; step <= steps; step++) {
            float progress =
                    (float) step
                            / steps;

            float angle =
                    startAngle
                            + (endAngle - startAngle)
                            * progress;

            scene.world().modifyBlockEntity(
                    wheel,
                    SteeringWheelBlockEntity.class,
                    blockEntity ->
                            blockEntity.setTargetAngle(
                                    angle
                            )
            );

            scene.idle(
                    ticksPerStep
            );
        }
    }

    static Selection outputSelection(SceneBuildingUtil util, GearboxPositions positions) {
        return util.select().fromTo(positions.outputShaft(), positions.speed());
    }

    static BlockState regularWormState(
            Direction.Axis axis,
            boolean connectedNegative,
            boolean connectedPositive
    ) {
        return CreateMechanicalDrive
                .WORM_GEAR_REGULAR
                .get()
                .defaultBlockState()
                .setValue(
                        RotatedPillarBlock.AXIS,
                        axis
                )
                .setValue(
                        WormGearRegularBlock.CONNECTED_NEGATIVE,
                        connectedNegative
                )
                .setValue(
                        WormGearRegularBlock.CONNECTED_POSITIVE,
                        connectedPositive
                );
    }

    static BlockState smallWormState(
            Direction.Axis axis,
            boolean connectedNegative,
            boolean connectedPositive
    ) {
        return CreateMechanicalDrive
                .WORM_GEAR_SMALL
                .get()
                .defaultBlockState()
                .setValue(
                        RotatedPillarBlock.AXIS,
                        axis
                )
                .setValue(
                        WormGearSmallBlock.CONNECTED_NEGATIVE,
                        connectedNegative
                )
                .setValue(
                        WormGearSmallBlock.CONNECTED_POSITIVE,
                        connectedPositive
                );
    }

    static BlockState overrunningClutchState(
            Direction facing
    ) {
        return CreateMechanicalDrive
                .OVERRUNNING_CLUTCH
                .get()
                .defaultBlockState()
                .setValue(
                        OverrunningClutchBlock.FACING,
                        facing
                )
                .setValue(
                        OverrunningClutchBlock.AXIS,
                        facing.getAxis()
                )
                .setValue(
                        OverrunningClutchBlock.POWERED,
                        false
                );
    }

    static void clearWorkspace(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.world().setBlocks(
                util.select().fromTo(0, 1, 0, 5, 3, 4),
                Blocks.AIR.defaultBlockState(),
                false
        );
    }

    static BlockState tankHousingState(
            Direction facing,
            boolean frameOnly
    ) {
        return CreateMechanicalDrive
                .TANK_TRANSMISSION_HOUSING
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
    }

    static BlockState tankDistributorState(
            Direction facing,
            boolean frameOnly
    ) {
        return CreateMechanicalDrive
                .TANK_TRANSMISSION_DISTRIBUTOR
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
    }

    static BlockState tankSteeringState(
            Direction facing,
            boolean frameOnly,
            boolean vertical
    ) {
        return CreateMechanicalDrive
                .TANK_TRANSMISSION_STEERING
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
    }

    static BlockState inputState(Direction facing, GearboxPosition position) {
        return CreateMechanicalDrive.GEARBOX_INPUT.get()
                .defaultBlockState()
                .setValue(CarGearboxInputBlock.FACING, facing)
                .setValue(CarGearboxInputBlock.AXIS, facing.getAxis())
                .setValue(CarGearboxInputBlock.POSITION, position);
    }

    static BlockState speedState(Direction facing, GearboxPosition position) {
        return CreateMechanicalDrive.GEARBOX_SPEED.get()
                .defaultBlockState()
                .setValue(CarGearboxSpeedBlock.FACING, facing)
                .setValue(CarGearboxSpeedBlock.AXIS, facing.getAxis())
                .setValue(CarGearboxSpeedBlock.POSITION, position);
    }

    static BlockState ponderLeverState(Direction facing, GearboxPosition position) {
        return CreateMechanicalDrive.PONDER_GEARBOX_LEVER.get()
                .defaultBlockState()
                .setValue(PonderGearboxLeverBlock.FACING, facing)
                .setValue(PonderGearboxLeverBlock.POSITION, position);
    }

    static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateMechanicalDrive.MOD_ID, path);
    }

    public record GearboxPositions(
            BlockPos outputShaft,
            BlockPos speed,
            BlockPos input,
            BlockPos sourceShaft,
            BlockPos lever
    ) {
    }
}
