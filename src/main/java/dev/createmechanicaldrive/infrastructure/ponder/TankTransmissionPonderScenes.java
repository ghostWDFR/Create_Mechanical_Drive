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
import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.*;
final class TankTransmissionPonderScenes {
    private TankTransmissionPonderScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                        loc("tank_transmission_housing"),
                        loc("tank_transmission_housing_assembled"),
                        loc("tank_transmission_distributor"),
                        loc("tank_transmission_distributor_assembled"),
                        loc("tank_transmission_steering"),
                        loc("tank_transmission_steering_assembled")
                )
                .addStoryBoard(
                        TANK_TRANSMISSION_SCHEMATIC,
                        TankTransmissionPonderScenes::tankTransmissionFrame
                );

        helper.forComponents(
                        loc("tank_transmission_housing"),
                        loc("tank_transmission_housing_assembled")
                )
                .addStoryBoard(
                        TANK_TRANSMISSION_SCHEMATIC,
                        TankTransmissionPonderScenes::tankTransmissionHousing
                );

        helper.forComponents(
                        loc("tank_transmission_distributor"),
                        loc("tank_transmission_distributor_assembled")
                )
                .addStoryBoard(
                        TANK_TRANSMISSION_SCHEMATIC,
                        TankTransmissionPonderScenes::tankTransmissionDistributor
                );

        helper.forComponents(
                        loc("tank_transmission_steering"),
                        loc("tank_transmission_steering_assembled")
                )
                .addStoryBoard(
                        TANK_TRANSMISSION_SCHEMATIC,
                        TankTransmissionPonderScenes::tankTransmissionSteering
                );
    }

    private static void tankTransmissionFrame(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "tank_transmission_frame",
                "Assembling Tank Transmission Components"
        );

        scene.configureBasePlate(
                0,
                0,
                5
        );

        scene.scaleSceneView(
                0.85F
        );

        clearWorkspace(
                scene,
                util
        );

        scene.showBasePlate();

        BlockPos frame =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockState frameState =
                CreateMechanicalDrive
                        .TANK_TRANSMISSION_FRAME
                        .get()
                        .defaultBlockState()
                        .setValue(
                                TankTransmissionFrame.FRAME_ONLY,
                                false
                        );

        scene.world().setBlock(
                frame,
                frameState,
                false
        );

        scene.world().showSection(
                util.select().position(
                        frame
                ),
                Direction.DOWN
        );

        scene.idle(
                20
        );

        scene.overlay().showText(
                        65
                )
                .text(
                        "Tank transmission components are assembled inside a common frame."
                )
                .pointAt(
                        util.vector().centerOf(
                                frame
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                75
        );

        BlockState housing =
                tankHousingState(
                        Direction.EAST,
                        false
                );

        scene.overlay().showControls(
                        util.vector().centerOf(
                                frame
                        ),
                        Pointing.DOWN,
                        40
                )
                .rightClick()
                .withItem(
                        new ItemStack(
                                CreateMechanicalDrive
                                        .TANK_TRANSMISSION_HOUSING_ITEM
                                        .get()
                        )
                );

        scene.idle(
                10
        );

        scene.world().setBlock(
                frame,
                housing,
                false
        );

        scene.effects().indicateSuccess(
                frame
        );

        scene.overlay().showText(
                        65
                )
                .text(
                        "Use an inner component on the frame to install it."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(
                                frame
                        )
                )
                .placeNearTarget();

        scene.idle(
                75
        );

        scene.world().setBlock(
                frame,
                frameState,
                false
        );

        scene.idle(
                15
        );

        scene.world().setBlock(
                frame,
                tankDistributorState(
                        Direction.NORTH,
                        false
                ),
                false
        );

        scene.idle(
                30
        );

        scene.world().setBlock(
                frame,
                frameState,
                false
        );

        scene.idle(
                15
        );

        scene.world().setBlock(
                frame,
                tankSteeringState(
                        Direction.NORTH,
                        false,
                        false
                ),
                false
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "Housing, Distributor and Steering use the same modular frame."
                )
                .pointAt(
                        util.vector().centerOf(
                                frame
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                85
        );

        scene.world().setBlock(
                frame,
                frameState,
                false
        );

        scene.overlay().showControls(
                        util.vector().centerOf(
                                frame
                        ),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(
                        new ItemStack(
                                CreateMechanicalDrive
                                        .TANK_TRANSMISSION_DISTRIBUTOR_ITEM
                                        .get()
                        )
                );

        scene.idle(
                15
        );

        scene.world().setBlock(
                frame,
                tankDistributorState(
                        Direction.NORTH,
                        false
                ),
                false
        );

        scene.effects().indicateSuccess(
                frame
        );

        scene.idle(
                70
        );
    }

    private static void tankTransmissionHousing(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "tank_transmission_housing",
                "Tank Transmission Housing"
        );

        scene.configureBasePlate(
                0,
                0,
                5
        );

        scene.scaleSceneView(
                0.85F
        );

        clearWorkspace(
                scene,
                util
        );

        scene.showBasePlate();

        BlockPos leftCog =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos housing =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos rightCog =
                util.grid().at(
                        3,
                        1,
                        2
                );

        BlockState cogX =
                AllBlocks.COGWHEEL
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        scene.world().setBlock(
                leftCog,
                cogX,
                false
        );

        scene.world().setBlock(
                housing,
                tankHousingState(
                        Direction.EAST,
                        false
                ),
                false
        );

        scene.world().setBlock(
                rightCog,
                cogX,
                false
        );

        Selection line =
                util.select().fromTo(
                        leftCog,
                        rightCog
                );

        scene.world().showSection(
                line,
                Direction.DOWN
        );

        scene.world().setKineticSpeed(
                line,
                64.0F
        );

        scene.idle(
                20
        );

        scene.effects().rotationSpeedIndicator(
                leftCog
        );

        scene.effects().rotationSpeedIndicator(
                rightCog
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "Housing carries rotation straight through the transmission."
                )
                .pointAt(
                        util.vector().centerOf(
                                housing
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                80
        );

        BlockPos secondHousing =
                util.grid().at(
                        2,
                        2,
                        2
                );

        scene.world().setBlock(
                secondHousing,
                tankHousingState(
                        Direction.EAST,
                        true
                ),
                false
        );

        scene.world().showSection(
                util.select().position(
                        secondHousing
                ),
                Direction.DOWN
        );

        scene.idle(
                15
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "Transmission modules can also be stacked vertically."
                )
                .pointAt(
                        util.vector().centerOf(
                                secondHousing
                        )
                )
                .placeNearTarget();

        scene.idle(
                80
        );
    }

    private static void tankTransmissionDistributor(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "tank_transmission_distributor",
                "Distributing Rotation"
        );

        scene.configureBasePlate(
                0,
                0,
                5
        );

        scene.scaleSceneView(
                0.82F
        );

        clearWorkspace(
                scene,
                util
        );

        scene.showBasePlate();

        BlockPos distributor =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos inputCog =
                distributor.north();

        BlockPos leftCog =
                distributor.east();

        BlockPos rightCog =
                distributor.west();

        BlockState inputCogState =
                AllBlocks.COGWHEEL
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Z
                        );

        BlockState outputCogState =
                AllBlocks.COGWHEEL
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        scene.world().setBlock(
                distributor,
                tankDistributorState(
                        Direction.NORTH,
                        false
                ),
                false
        );

        scene.world().setBlock(
                inputCog,
                inputCogState,
                false
        );

        scene.world().setBlock(
                leftCog,
                outputCogState,
                false
        );

        scene.world().setBlock(
                rightCog,
                outputCogState,
                false
        );

        Selection assembly =
                util.select().fromTo(
                        1,
                        1,
                        1,
                        3,
                        1,
                        2
                );

        scene.world().showSection(
                assembly,
                Direction.DOWN
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        inputCog
                ),
                64.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        distributor
                ),
                64.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        leftCog
                ),
                64.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        rightCog
                ),
                64.0F
        );

        scene.idle(
                20
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "Distributor takes one input and creates separate left and right outputs."
                )
                .pointAt(
                        util.vector().centerOf(
                                distributor
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.effects().rotationSpeedIndicator(
                inputCog
        );

        scene.effects().rotationSpeedIndicator(
                leftCog
        );

        scene.effects().rotationSpeedIndicator(
                rightCog
        );

        scene.idle(
                85
        );

        scene.overlay().showLine(
                PonderPalette.INPUT,
                util.vector().centerOf(
                        inputCog
                ),
                util.vector().centerOf(
                        distributor
                ),
                60
        );

        scene.overlay().showLine(
                PonderPalette.GREEN,
                util.vector().centerOf(
                        distributor
                ),
                util.vector().centerOf(
                        leftCog
                ),
                60
        );

        scene.overlay().showLine(
                PonderPalette.GREEN,
                util.vector().centerOf(
                        distributor
                ),
                util.vector().centerOf(
                        rightCog
                ),
                60
        );

        scene.idle(
                70
        );

        scene.overlay().showText(
                        65
                )
                .text(
                        "Without steering, both tracks receive the same rotation."
                )
                .pointAt(
                        util.vector().centerOf(
                                distributor
                        )
                )
                .placeNearTarget();

        scene.idle(
                75
        );
    }

    private static void tankTransmissionSteering(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "tank_transmission_steering",
                "Steering a Tracked Vehicle"
        );

        scene.configureBasePlate(
                0,
                0,
                5
        );

        scene.scaleSceneView(
                0.78F
        );

        clearWorkspace(
                scene,
                util
        );

        scene.showBasePlate();

        BlockPos distributor =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos inputCog =
                distributor.north();

        BlockPos rightCog =
                distributor.west();

        BlockPos housing =
                distributor.east();

        BlockPos steering =
                housing.east();

        BlockPos leftCog =
                steering.east();

        BlockPos wheel =
                steering.north();

        BlockState inputCogState =
                AllBlocks.COGWHEEL
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Z
                        );

        BlockState outputCogState =
                AllBlocks.COGWHEEL
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        BlockState wheelState =
                CreateMechanicalDrive
                        .STEERING_WHEEL
                        .get()
                        .defaultBlockState()
                        .setValue(
                                SteeringWheelBlock.FACING,
                                Direction.NORTH
                        )
                        .setValue(
                                SteeringWheelBlock.COLOR,
                                SteeringWheelColor.BLACK
                        );

        scene.world().setBlock(
                distributor,
                tankDistributorState(
                        Direction.NORTH,
                        false
                ),
                false
        );

        scene.world().setBlock(
                inputCog,
                inputCogState,
                false
        );

        scene.world().setBlock(
                rightCog,
                outputCogState,
                false
        );

        scene.world().setBlock(
                housing,
                tankHousingState(
                        Direction.EAST,
                        false
                ),
                false
        );

        scene.world().setBlock(
                steering,
                tankSteeringState(
                        Direction.NORTH,
                        false,
                        false
                ),
                false
        );

        scene.world().setBlock(
                leftCog,
                outputCogState,
                false
        );

        scene.world().setBlock(
                wheel,
                wheelState,
                false
        );

        Selection complete =
                util.select().fromTo(
                        0,
                        1,
                        1,
                        4,
                        1,
                        2
                );

        scene.world().showSection(
                complete,
                Direction.DOWN
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        inputCog
                ),
                64.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        distributor
                ),
                64.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        housing
                ),
                64.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        leftCog
                ),
                64.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        rightCog
                ),
                64.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        wheel
                ),
                0.0F
        );

        scene.idle(
                20
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "Connect a Steering module to control the Distributor."
                )
                .pointAt(
                        util.vector().centerOf(
                                steering
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                80
        );

        scene.overlay().showControls(
                        util.vector().centerOf(
                                wheel
                        ),
                        Pointing.DOWN,
                        45
                )
                .rightClick();

        scene.overlay().showText(
                        65
                )
                .text(
                        "Use a Steering Wheel to turn the steering input."
                )
                .pointAt(
                        util.vector().centerOf(
                                wheel
                        )
                )
                .placeNearTarget();

        scene.idle(
                75
        );

        scene.effects().rotationSpeedIndicator(
                leftCog
        );

        scene.effects().rotationSpeedIndicator(
                rightCog
        );

        scene.overlay().showText(
                        55
                )
                .text(
                        "Centered"
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(
                                distributor
                        )
                )
                .placeNearTarget();

        scene.idle(
                65
        );

        animateSteeringWheel(
                scene,
                wheel,
                0.0F,
                90.0F,
                20,
                1
        );

        scene.world().modifyBlockEntity(
                steering,
                TankTransmissionSteeringBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderSteeringAngle(
                                90.0F
                        )
        );

        scene.world().modifyBlockEntity(
                distributor,
                TankTransmissionDistributorBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderOutputMultipliers(
                                1.25F,
                                0.5F
                        )
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        leftCog
                ),
                80.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        rightCog
                ),
                32.0F
        );

        scene.effects().rotationSpeedIndicator(
                leftCog
        );

        scene.effects().rotationSpeedIndicator(
                rightCog
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "Turning changes the speed of the two track outputs."
                )
                .pointAt(
                        util.vector().centerOf(
                                distributor
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                85
        );

        animateSteeringWheel(
                scene,
                wheel,
                90.0F,
                180.0F,
                20,
                1
        );

        scene.world().modifyBlockEntity(
                steering,
                TankTransmissionSteeringBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderSteeringAngle(
                                180.0F
                        )
        );

        scene.world().modifyBlockEntity(
                distributor,
                TankTransmissionDistributorBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderOutputMultipliers(
                                1.0F,
                                -1.0F
                        )
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        leftCog
                ),
                64.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        rightCog
                ),
                -64.0F
        );

        scene.effects().rotationDirectionIndicator(
                leftCog
        );

        scene.effects().rotationDirectionIndicator(
                rightCog
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "At full steering, one track reverses for a pivot turn."
                )
                .colored(
                        PonderPalette.FAST
                )
                .pointAt(
                        util.vector().centerOf(
                                rightCog
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                90
        );

        animateSteeringWheel(
                scene,
                wheel,
                180.0F,
                -180.0F,
                30,
                1
        );

        scene.world().modifyBlockEntity(
                steering,
                TankTransmissionSteeringBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderSteeringAngle(
                                -180.0F
                        )
        );

        scene.world().modifyBlockEntity(
                distributor,
                TankTransmissionDistributorBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderOutputMultipliers(
                                -1.0F,
                                1.0F
                        )
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        leftCog
                ),
                -64.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        rightCog
                ),
                64.0F
        );

        scene.effects().rotationDirectionIndicator(
                leftCog
        );

        scene.effects().rotationDirectionIndicator(
                rightCog
        );

        scene.idle(
                65
        );

        scene.overlay().showText(
                        60
                )
                .text(
                        "Turn the wheel the other way to pivot in the opposite direction."
                )
                .pointAt(
                        util.vector().centerOf(
                                leftCog
                        )
                )
                .placeNearTarget();

        scene.idle(
                75
        );

        scene.world().hideSection(
                complete,
                Direction.UP
        );

        scene.idle(
                25
        );

        clearWorkspace(
                scene,
                util
        );

        BlockPos verticalDistributor =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos verticalHousing =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos verticalSteering =
                verticalHousing.above();

        BlockPos verticalWheel =
                verticalSteering.north();

        BlockState verticalWheelState =
                CreateMechanicalDrive
                        .STEERING_WHEEL
                        .get()
                        .defaultBlockState()
                        .setValue(
                                SteeringWheelBlock.FACING,
                                Direction.NORTH
                        )
                        .setValue(
                                SteeringWheelBlock.COLOR,
                                SteeringWheelColor.BLACK
                        );

        scene.world().setBlock(
                verticalDistributor,
                tankDistributorState(
                        Direction.NORTH,
                        false
                ),
                false
        );

        scene.world().setBlock(
                verticalHousing,
                tankHousingState(
                        Direction.EAST,
                        false
                ),
                false
        );

        scene.world().setBlock(
                verticalSteering,
                tankSteeringState(
                        Direction.NORTH,
                        false,
                        true
                ),
                false
        );

        scene.world().setBlock(
                verticalWheel,
                verticalWheelState,
                false
        );

        Selection verticalAssembly =
                util.select().fromTo(
                        1,
                        1,
                        1,
                        2,
                        2,
                        2
                );

        scene.world().showSection(
                verticalAssembly,
                Direction.DOWN
        );

        scene.idle(
                20
        );

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                verticalDistributor,
                                verticalSteering
                        ),
                        80
                )
                .text(
                        "Steering can also be mounted vertically above a Distributor or Housing."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(
                                verticalSteering
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                90
        );
    }
}
