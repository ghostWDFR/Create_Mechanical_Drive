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

final class HandCrankPonderScenes {
    private HandCrankPonderScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                        loc("hand_crank")
                )
                .addStoryBoard(
                        HAND_CRANK_SCHEMATIC,
                        HandCrankPonderScenes::handCrankInstallation
                )
                .addStoryBoard(
                        HAND_CRANK_SCHEMATIC,
                        HandCrankPonderScenes::handCrankOperation
                );
    }

    private static void handCrankInstallation(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "hand_crank_installation",
                "Inline Hand Crank"
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

        BlockPos createShaft =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos createCrank =
                util.grid().at(
                        3,
                        1,
                        2
                );

        BlockState shaftState =
                AllBlocks.SHAFT
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        BlockState createCrankState =
                AllBlocks.HAND_CRANK
                        .getDefaultState()
                        .setValue(
                                DirectionalBlock.FACING,
                                Direction.EAST
                        );

        scene.world().setBlock(
                createShaft,
                shaftState,
                false
        );

        scene.world().setBlock(
                createCrank,
                createCrankState,
                false
        );

        scene.world().showSection(
                util.select().position(
                        createShaft
                ),
                Direction.DOWN
        );

        scene.idle(
                10
        );

        scene.world().showSection(
                util.select().position(
                        createCrank
                ),
                Direction.WEST
        );

        scene.idle(
                15
        );

        scene.overlay().showOutlineWithText(
                        util.select().position(
                                createCrank
                        ),
                        80
                )
                .text(
                        "Create's Hand Crank is mounted at the end of a kinetic network."
                )
                .pointAt(
                        util.vector().centerOf(
                                createCrank
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                90
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "It connects to a shaft from only one side."
                )
                .pointAt(
                        util.vector().blockSurface(
                                createCrank,
                                Direction.WEST
                        )
                )
                .placeNearTarget();

        scene.idle(
                80
        );

        Selection createSelection =
                util.select().fromTo(
                        createShaft,
                        createCrank
                );

        scene.world().hideSection(
                createSelection,
                Direction.UP
        );

        scene.idle(
                20
        );

        scene.world().setBlocks(
                createSelection,
                Blocks.AIR.defaultBlockState(),
                false
        );

        BlockPos leftShaft =
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

        BlockPos rightShaft =
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
                                dev.createmechanicaldrive
                                        .content
                                        .hand_crank
                                        .HandCrankBlock
                                        .AXIS,
                                Direction.Axis.X
                        );

        scene.world().setBlock(
                leftShaft,
                shaftState,
                false
        );

        scene.world().setBlock(
                crank,
                crankState,
                false
        );

        scene.world().showSection(
                util.select().fromTo(
                        leftShaft,
                        crank
                ),
                Direction.DOWN
        );

        scene.idle(
                15
        );

        scene.overlay().showOutlineWithText(
                        util.select().position(
                                crank
                        ),
                        80
                )
                .text(
                        "The Mechanical Drive Hand Crank is an inline component."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(
                                crank
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                90
        );

        scene.overlay().showText(
                        80
                )
                .text(
                        "It has a shaft connection on both sides of its rotation axis."
                )
                .pointAt(
                        util.vector().centerOf(
                                crank
                        )
                )
                .placeNearTarget();

        scene.idle(
                90
        );

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                leftShaft,
                                crank
                        ),
                        70
                )
                .text(
                        "One connected side is not enough."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(
                                crank
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                80
        );

        scene.overlay().showControls(
                        util.vector().centerOf(
                                crank
                        ),
                        Pointing.DOWN,
                        35
                )
                .rightClick();

        scene.idle(
                20
        );

        scene.world().hideSection(
                util.select().position(
                        crank
                ),
                Direction.UP
        );

        scene.idle(
                15
        );

        scene.world().setBlock(
                crank,
                Blocks.AIR.defaultBlockState(),
                false
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "Trying to turn it without valid kinetic connections on both sides breaks the crank."
                )
                .colored(
                        PonderPalette.RED
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

        scene.world().setBlock(
                crank,
                crankState,
                false
        );

        scene.world().setBlock(
                rightShaft,
                shaftState,
                false
        );

        scene.world().showSection(
                util.select().fromTo(
                        crank,
                        rightShaft
                ),
                Direction.DOWN
        );

        scene.effects().indicateSuccess(
                crank
        );

        scene.idle(
                15
        );

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                leftShaft,
                                rightShaft
                        ),
                        90
                )
                .text(
                        "Place the crank directly between two compatible kinetic components."
                )
                .colored(
                        PonderPalette.GREEN
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

        scene.overlay().showText(
                        85
                )
                .text(
                        "A Create Hand Crank cannot be used as either of these two neighbouring connections."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(
                                rightShaft
                        )
                )
                .placeNearTarget();

        scene.idle(
                95
        );
    }

    private static void handCrankOperation(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "hand_crank_operation",
                "Operating the Hand Crank"
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

        BlockPos leftShaft =
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

        BlockPos rightShaft =
                util.grid().at(
                        3,
                        1,
                        2
                );

        BlockPos cogwheel =
                util.grid().at(
                        4,
                        1,
                        2
                );

        BlockState shaftState =
                AllBlocks.SHAFT
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        BlockState crankState =
                CreateMechanicalDrive
                        .HAND_CRANK
                        .get()
                        .defaultBlockState()
                        .setValue(
                                dev.createmechanicaldrive
                                        .content
                                        .hand_crank
                                        .HandCrankBlock
                                        .AXIS,
                                Direction.Axis.X
                        );

        BlockState cogwheelState =
                AllBlocks.COGWHEEL
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        scene.world().setBlock(
                leftShaft,
                shaftState,
                false
        );

        scene.world().setBlock(
                crank,
                crankState,
                false
        );

        scene.world().setBlock(
                rightShaft,
                shaftState,
                false
        );

        scene.world().setBlock(
                cogwheel,
                cogwheelState,
                false
        );

        Selection network =
                util.select().fromTo(
                        leftShaft,
                        cogwheel
                );

        scene.world().showSection(
                network,
                Direction.DOWN
        );

        scene.idle(
                20
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "Use the Hand Crank with an empty hand to power the connected kinetic network."
                )
                .pointAt(
                        util.vector().centerOf(
                                crank
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                85
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

        scene.effects().rotationSpeedIndicator(
                cogwheel
        );

        scene.effects().indicateSuccess(
                crank
        );

        scene.idle(
                15
        );

        scene.overlay().showText(
                        80
                )
                .text(
                        "A normal use generates 32 RPM through the entire line."
                )
                .colored(
                        PonderPalette.FAST
                )
                .pointAt(
                        util.vector().centerOf(
                                rightShaft
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                90
        );

        scene.world().setKineticSpeed(
                network,
                0.0F
        );

        scene.idle(
                25
        );

        scene.overlay().showText(
                        90
                )
                .text(
                        "Hold Shift while using the crank to reverse its direction."
                )
                .pointAt(
                        util.vector().centerOf(
                                crank
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.overlay().showControls(
                        util.vector().centerOf(
                                crank
                        ),
                        Pointing.DOWN,
                        45
                )
                .rightClick();

        scene.idle(
                15
        );

        scene.world().setKineticSpeed(
                network,
                -32.0F
        );

        scene.effects().rotationDirectionIndicator(
                crank
        );

        scene.effects().rotationDirectionIndicator(
                cogwheel
        );

        scene.idle(
                15
        );

        scene.overlay().showText(
                        80
                )
                .text(
                        "Reverse still runs at 32 RPM, but in the opposite direction."
                )
                .colored(
                        PonderPalette.MEDIUM
                )
                .pointAt(
                        util.vector().centerOf(
                                cogwheel
                        )
                )
                .placeNearTarget();

        scene.idle(
                90
        );

        scene.world().setKineticSpeed(
                network,
                32.0F
        );

        scene.effects().rotationSpeedIndicator(
                crank
        );

        scene.idle(
                15
        );

        scene.overlay().showOutlineWithText(
                        network,
                        100
                )
                .text(
                        "The crank provides 16 SU per RPM: 512 SU of stress capacity at 32 RPM."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(
                                crank
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                110
        );

        scene.overlay().showText(
                        95
                )
                .text(
                        "That is twice the default stress capacity of Create's standard Hand Crank."
                )
                .pointAt(
                        util.vector().centerOf(
                                crank
                        )
                )
                .placeNearTarget();

        scene.idle(
                105
        );

        scene.world().setKineticSpeed(
                network,
                0.0F
        );

        scene.idle(
                20
        );

        scene.overlay().showText(
                        90
                )
                .text(
                        "The crank only generates rotation briefly after each use, so keep operating it to maintain power."
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
    }
}
