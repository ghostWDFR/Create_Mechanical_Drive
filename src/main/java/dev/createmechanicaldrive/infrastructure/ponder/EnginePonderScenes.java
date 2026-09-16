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

final class EnginePonderScenes {
    private EnginePonderScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                        loc("engine")
                )
                .addStoryBoard(
                        ENGINE_SCHEMATIC,
                        EnginePonderScenes::engineStartup
                )
                .addStoryBoard(
                        ENGINE_SCHEMATIC,
                        EnginePonderScenes::engineMultiple
                )
                .addStoryBoard(
                        ENGINE_SCHEMATIC,
                        EnginePonderScenes::engineRedstone
                );
    }

    private static void engineStartup(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "engine_startup",
                "Starting the Engine"
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

        BlockPos starterShaft =
                util.grid().at(
                        0,
                        1,
                        2
                );

        BlockPos crank =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos engine =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos output =
                util.grid().at(
                        3,
                        1,
                        2
                );

        BlockState crankState =
                CreateMechanicalDrive
                        .HAND_CRANK
                        .get()
                        .defaultBlockState()
                        .setValue(
                                HandCrankBlock.AXIS,
                                Direction.Axis.X
                        );

        BlockState engineOff =
                CreateMechanicalDrive
                        .ENGINE
                        .get()
                        .defaultBlockState()
                        .setValue(
                                EngineBlock.FACING,
                                Direction.EAST
                        )
                        .setValue(
                                EngineBlock.LIT,
                                false
                        );

        BlockState engineOn =
                engineOff.setValue(
                        EngineBlock.LIT,
                        true
                );

        BlockState shaft =
                AllBlocks.SHAFT
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        scene.world().setBlock(
                starterShaft,
                shaft,
                false
        );

        scene.world().setBlock(
                crank,
                crankState,
                false
        );

        scene.world().setBlock(
                engine,
                engineOff,
                false
        );

        scene.world().setBlock(
                output,
                shaft,
                false
        );

        Selection network =
                util.select().fromTo(
                        starterShaft,
                        output
                );

        scene.world().showSection(
                network,
                Direction.DOWN
        );

        scene.idle(
                20
        );

        scene.overlay().showControls(
                        util.vector().topOf(
                                engine
                        ),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(
                        new ItemStack(
                                Items.LAVA_BUCKET
                        )
                );

        scene.overlay().showText(
                        65
                )
                .text(
                        "Fill the engine with lava."
                )
                .pointAt(
                        util.vector().centerOf(
                                engine
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.world().modifyBlockEntity(
                engine,
                EngineBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderFuel(
                                1000
                        )
        );

        scene.idle(
                75
        );

        scene.overlay().showText(
                        60
                )
                .text(
                        "Fuel alone does not start it."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(
                                engine
                        )
                )
                .placeNearTarget();

        scene.idle(
                70
        );

        scene.overlay().showControls(
                        util.vector().centerOf(
                                crank
                        ),
                        Pointing.DOWN,
                        40
                )
                .rightClick();

        scene.idle(
                10
        );

        scene.world().setKineticSpeed(
                network,
                32.0F
        );

        scene.effects().rotationSpeedIndicator(
                crank
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "Give the engine a brief external rotation to start it."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(
                                crank
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                75
        );

        scene.world().setBlock(
                engine,
                engineOn,
                false
        );

        scene.world().modifyBlockEntity(
                engine,
                EngineBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderRunning(
                                true,
                                1
                        )
        );

        scene.world().setKineticSpeed(
                network,
                64.0F
        );

        scene.effects().indicateSuccess(
                engine
        );

        scene.effects().rotationSpeedIndicator(
                output
        );

        scene.idle(
                15
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "Once started, the engine maintains 64 RPM."
                )
                .colored(
                        PonderPalette.FAST
                )
                .pointAt(
                        util.vector().centerOf(
                                output
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                85
        );

        scene.world().modifyBlockEntity(
                engine,
                EngineBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderRunning(
                                false,
                                1
                        )
        );

        scene.world().setBlock(
                engine,
                engineOff,
                false
        );

        scene.world().setKineticSpeed(
                network,
                0.0F
        );

        scene.idle(
                20
        );

        scene.overlay().showControls(
                        util.vector().centerOf(
                                crank
                        ),
                        Pointing.DOWN,
                        40
                )
                .rightClick();

        scene.world().setKineticSpeed(
                network,
                -32.0F
        );

        scene.idle(
                15
        );

        scene.world().setBlock(
                engine,
                engineOn,
                false
        );

        scene.world().modifyBlockEntity(
                engine,
                EngineBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderRunning(
                                true,
                                -1
                        )
        );

        scene.world().setKineticSpeed(
                network,
                -64.0F
        );

        scene.effects().rotationDirectionIndicator(
                output
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "The starter direction also sets the engine's rotation direction."
                )
                .pointAt(
                        util.vector().centerOf(
                                output
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                85
        );
    }

    private static void engineMultiple(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "engine_multiple",
                "Using Multiple Engines"
        );

        scene.configureBasePlate(
                0,
                0,
                5
        );

        scene.scaleSceneView(
                0.8F
        );

        clearWorkspace(
                scene,
                util
        );

        scene.showBasePlate();

        BlockState engineOff =
                CreateMechanicalDrive
                        .ENGINE
                        .get()
                        .defaultBlockState()
                        .setValue(
                                EngineBlock.FACING,
                                Direction.EAST
                        )
                        .setValue(
                                EngineBlock.LIT,
                                false
                        );

        BlockState engineOn =
                engineOff.setValue(
                        EngineBlock.LIT,
                        true
                );

        BlockPos first =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos second =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos third =
                util.grid().at(
                        3,
                        1,
                        2
                );

        scene.world().setBlock(
                first,
                engineOff,
                false
        );

        scene.world().setBlock(
                second,
                engineOff,
                false
        );

        scene.world().setBlock(
                third,
                engineOff,
                false
        );

        Selection engineGroup =
                util.select().fromTo(
                        first,
                        third
                );

        scene.world().showSection(
                engineGroup,
                Direction.DOWN
        );

        scene.idle(
                20
        );

        scene.overlay().showOutlineWithText(
                        engineGroup,
                        80
                )
                .text(
                        "Up to three directly connected engines form one group."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                90
        );

        scene.overlay().showControls(
                        util.vector().topOf(
                                second
                        ),
                        Pointing.DOWN,
                        40
                )
                .rightClick()
                .withItem(
                        new ItemStack(
                                Items.LAVA_BUCKET
                        )
                );

        scene.world().modifyBlockEntity(
                first,
                EngineBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderFuel(
                                1000
                        )
        );

        scene.world().modifyBlockEntity(
                second,
                EngineBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderFuel(
                                1000
                        )
        );

        scene.world().modifyBlockEntity(
                third,
                EngineBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderFuel(
                                1000
                        )
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "Connected engines share their fuel reserve."
                )
                .pointAt(
                        util.vector().centerOf(
                                second
                        )
                )
                .placeNearTarget();

        scene.idle(
                80
        );

        scene.world().hideSection(
                engineGroup,
                Direction.UP
        );

        scene.idle(
                20
        );

        scene.world().setBlocks(
                engineGroup,
                Blocks.AIR.defaultBlockState(),
                false
        );

        scene.idle(
                10
        );

        BlockPos leftEngine =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos crank =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos rightEngine =
                util.grid().at(
                        3,
                        1,
                        2
                );

        BlockState crankState =
                CreateMechanicalDrive
                        .HAND_CRANK
                        .get()
                        .defaultBlockState()
                        .setValue(
                                HandCrankBlock.AXIS,
                                Direction.Axis.X
                        );

        scene.world().setBlock(
                leftEngine,
                engineOff,
                false
        );

        scene.world().setBlock(
                crank,
                crankState,
                false
        );

        scene.world().setBlock(
                rightEngine,
                engineOff,
                false
        );

        Selection parallel =
                util.select().fromTo(
                        leftEngine,
                        rightEngine
                );

        scene.idle(
                10
        );

        scene.world().showSection(
                parallel,
                Direction.DOWN
        );

        scene.world().modifyBlockEntity(
                leftEngine,
                EngineBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderFuel(
                                1000
                        )
        );

        scene.world().modifyBlockEntity(
                rightEngine,
                EngineBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderFuel(
                                1000
                        )
        );

        scene.idle(
                20
        );

        scene.overlay().showOutlineWithText(
                        parallel,
                        90
                )
                .text(
                        "A Hand Crank can also start two engines connected on opposite sides."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(
                                crank
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                100
        );

        scene.overlay().showControls(
                        util.vector().centerOf(
                                crank
                        ),
                        Pointing.DOWN,
                        40
                )
                .rightClick();

        scene.world().setKineticSpeed(
                parallel,
                32.0F
        );

        scene.effects().rotationSpeedIndicator(
                crank
        );

        scene.idle(
                20
        );

        scene.world().setBlock(
                leftEngine,
                engineOn,
                false
        );

        scene.world().setBlock(
                rightEngine,
                engineOn,
                false
        );

        scene.world().modifyBlockEntity(
                leftEngine,
                EngineBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderRunning(
                                true,
                                1
                        )
        );

        scene.world().modifyBlockEntity(
                rightEngine,
                EngineBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderRunning(
                                true,
                                1
                        )
        );

        scene.world().setKineticSpeed(
                parallel,
                64.0F
        );

        scene.effects().indicateSuccess(
                leftEngine
        );

        scene.effects().indicateSuccess(
                rightEngine
        );

        scene.idle(
                15
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "One turn starts both engines from the same kinetic line."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(
                                crank
                        )
                )
                .placeNearTarget();

        scene.idle(
                85
        );

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                leftEngine,
                                rightEngine
                        ),
                        80
                )
                .text(
                        "Each running engine adds more stress capacity to the network."
                )
                .colored(
                        PonderPalette.FAST
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                90
        );
    }

    private static void engineRedstone(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "engine_redstone",
                "Stopping Engines with Redstone"
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

        BlockPos first =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos second =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos output =
                util.grid().at(
                        3,
                        1,
                        2
                );

        BlockPos redstone =
                first.above();

        BlockState engineOn =
                CreateMechanicalDrive
                        .ENGINE
                        .get()
                        .defaultBlockState()
                        .setValue(
                                EngineBlock.FACING,
                                Direction.EAST
                        )
                        .setValue(
                                EngineBlock.LIT,
                                true
                        );

        BlockState engineOff =
                engineOn.setValue(
                        EngineBlock.LIT,
                        false
                );

        BlockState shaft =
                AllBlocks.SHAFT
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        scene.world().setBlock(
                first,
                engineOn,
                false
        );

        scene.world().setBlock(
                second,
                engineOn,
                false
        );

        scene.world().setBlock(
                output,
                shaft,
                false
        );

        scene.world().modifyBlockEntity(
                first,
                EngineBlockEntity.class,
                blockEntity -> {
                    blockEntity.setPonderFuel(
                            1000
                    );

                    blockEntity.setPonderRunning(
                            true,
                            1
                    );
                }
        );

        scene.world().modifyBlockEntity(
                second,
                EngineBlockEntity.class,
                blockEntity -> {
                    blockEntity.setPonderFuel(
                            1000
                    );

                    blockEntity.setPonderRunning(
                            true,
                            1
                    );
                }
        );

        Selection network =
                util.select().fromTo(
                        first,
                        output
                );

        scene.world().showSection(
                network,
                Direction.DOWN
        );

        scene.world().setKineticSpeed(
                network,
                64.0F
        );

        scene.idle(
                20
        );

        scene.world().setBlock(
                redstone,
                Blocks.REDSTONE_BLOCK
                        .defaultBlockState(),
                false
        );

        scene.world().showSection(
                util.select().position(
                        redstone
                ),
                Direction.DOWN
        );

        scene.effects().indicateRedstone(
                first
        );

        scene.idle(
                15
        );

        scene.world().setBlock(
                first,
                engineOff,
                false
        );

        scene.world().setBlock(
                second,
                engineOff,
                false
        );

        scene.world().modifyBlockEntity(
                first,
                EngineBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderRunning(
                                false,
                                1
                        )
        );

        scene.world().modifyBlockEntity(
                second,
                EngineBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderRunning(
                                false,
                                1
                        )
        );

        scene.world().setKineticSpeed(
                network,
                0.0F
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "Redstone on any engine stops the entire connected engine group."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(
                                first
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                85
        );

        scene.world().hideSection(
                util.select().position(
                        redstone
                ),
                Direction.UP
        );

        scene.world().setBlock(
                redstone,
                Blocks.AIR
                        .defaultBlockState(),
                false
        );

        scene.effects().indicateSuccess(
                first
        );

        scene.idle(
                20
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "Removing the signal does not restart it automatically."
                )
                .pointAt(
                        util.vector().centerOf(
                                second
                        )
                )
                .placeNearTarget();

        scene.idle(
                80
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "Fuel remains, but another starter rotation is required."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(
                                output
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                80
        );
    }
}
