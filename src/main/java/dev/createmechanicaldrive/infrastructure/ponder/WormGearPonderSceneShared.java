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

final class WormGearPonderSceneShared {
    private WormGearPonderSceneShared() {
    }

    static void operation(
            SceneBuilder builder,
            SceneBuildingUtil util,
            boolean small
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        String sceneId =
                small
                        ? "worm_gear_small_operation"
                        : "worm_gear_regular_operation";

        String title =
                small
                        ? "Using the Small Worm Gear"
                        : "Using the Worm Gear";

        scene.title(
                sceneId,
                title
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

        BlockPos source =
                util.grid().at(
                        0,
                        1,
                        2
                );

        BlockPos shaft =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos worm =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos cog =
                util.grid().at(
                        2,
                        1,
                        1
                );

        BlockPos output =
                util.grid().at(
                        2,
                        2,
                        1
                );

        BlockState shaftX =
                AllBlocks.SHAFT
                        .get()
                        .defaultBlockState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        BlockState shaftY =
                AllBlocks.SHAFT
                        .get()
                        .defaultBlockState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Y
                        );

        BlockState cogState =
                (
                        small
                                ? AllBlocks.LARGE_COGWHEEL.get()
                                : AllBlocks.COGWHEEL.get()
                )
                        .defaultBlockState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Y
                        );

        BlockState wormState =
                small
                        ? smallWormState(
                        Direction.Axis.X,
                        false,
                        false
                )
                        : regularWormState(
                        Direction.Axis.X,
                        false,
                        false
                );

        scene.world().setBlock(
                cog,
                cogState,
                false
        );

        scene.world().setBlock(
                output,
                shaftY,
                false
        );

        scene.world().showSection(
                util.select().fromTo(
                        cog,
                        output
                ),
                Direction.DOWN
        );

        scene.idle(
                15
        );

        scene.overlay().showOutlineWithText(
                        util.select().position(cog),
                        80
                )
                .text(
                        small
                                ? "The Small Worm Gear meshes only with Create's Large Cogwheel."
                                : "The Worm Gear meshes only with Create's regular Cogwheel."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(cog)
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                90
        );

        scene.overlay().showControls(
                        util.vector().centerOf(worm),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(
                        new ItemStack(
                                small
                                        ? CreateMechanicalDrive.WORM_GEAR_SMALL_ITEM.get()
                                        : CreateMechanicalDrive.WORM_GEAR_REGULAR_ITEM.get()
                        )
                );

        scene.idle(
                20
        );

        scene.world().setBlock(
                worm,
                wormState,
                false
        );

        scene.world().showSection(
                util.select().position(worm),
                Direction.DOWN
        );

        scene.effects().indicateSuccess(
                worm
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        "When placed beside a supported cogwheel or a compatible shaft, the Worm Gear automatically aligns itself."
                )
                .pointAt(
                        util.vector().centerOf(worm)
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                95
        );

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                worm,
                                cog
                        ),
                        90
                )
                .text(
                        "For a valid mesh, the Worm Gear axis, Cogwheel axis, and the direction between them must all use different axes."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(worm)
                )
                .placeNearTarget();

        scene.idle(
                100
        );

        scene.world().setBlock(
                source,
                shaftX,
                false
        );

        scene.world().setBlock(
                shaft,
                shaftX,
                false
        );

        scene.world().showSection(
                util.select().fromTo(
                        source,
                        shaft
                ),
                Direction.DOWN
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "Feed rotation into either end of the Worm Gear's shaft axis."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(shaft)
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                85
        );

        Selection inputNetwork =
                util.select().fromTo(
                        source,
                        worm
                );

        Selection outputNetwork =
                util.select().fromTo(
                        cog,
                        output
                );

        float outputSpeed =
                small
                        ? -8.0F
                        : -16.0F;

        scene.world().setKineticSpeed(
                inputNetwork,
                64.0F
        );

        scene.world().setKineticSpeed(
                outputNetwork,
                outputSpeed
        );

        scene.effects().rotationSpeedIndicator(
                worm
        );

        scene.effects().rotationSpeedIndicator(
                cog
        );

        scene.idle(
                15
        );

        scene.overlay().showText(
                        90
                )
                .text(
                        small
                                ? "The Small Worm Gear has a fixed 1:8 ratio: 64 RPM on the worm becomes 8 RPM on the Large Cogwheel."
                                : "The Worm Gear has a fixed 1:4 ratio: 64 RPM on the worm becomes 16 RPM on the Cogwheel."
                )
                .colored(
                        PonderPalette.SLOW
                )
                .pointAt(
                        util.vector().centerOf(cog)
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                100
        );

        scene.effects().rotationDirectionIndicator(
                worm
        );

        scene.effects().rotationDirectionIndicator(
                cog
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        "The transmission redirects rotation by 90 degrees, and the output can continue into another kinetic network."
                )
                .pointAt(
                        util.vector().centerOf(output)
                )
                .placeNearTarget();

        scene.idle(
                95
        );

        scene.overlay().showControls(
                        util.vector().blockSurface(
                                worm,
                                Direction.UP
                        ),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(
                        new ItemStack(
                                AllBlocks
                                        .ANDESITE_CASING
                                        .get()
                        )
                );

        scene.idle(
                20
        );

        scene.world().modifyBlock(
                worm,
                state -> {
                    if (small) {
                        return state
                                .setValue(
                                        WormGearSmallBlock.ENCASED,
                                        true
                                )
                                .setValue(
                                        WormGearSmallBlock.CONNECTED_NEGATIVE,
                                        false
                                )
                                .setValue(
                                        WormGearSmallBlock.CONNECTED_POSITIVE,
                                        false
                                );
                    }

                    return state
                            .setValue(
                                    WormGearRegularBlock.ENCASED,
                                    true
                            )
                            .setValue(
                                    WormGearRegularBlock.CONNECTED_NEGATIVE,
                                    false
                            )
                            .setValue(
                                    WormGearRegularBlock.CONNECTED_POSITIVE,
                                    false
                            );
                },
                false
        );

        scene.effects().indicateSuccess(
                worm
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        small
                                ? "Right-click the Small Worm Gear with Andesite Casing to turn it into an encased variant."
                                : "Right-click the Worm Gear with Andesite Casing to turn it into an encased variant."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(worm)
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                95
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "The encased gear keeps transmitting rotation, but it no longer forms seamless worm-shaft extensions."
                )
                .pointAt(
                        util.vector().centerOf(worm)
                )
                .placeNearTarget();

        scene.idle(
                85
        );
    }

    static void cascade(
            SceneBuilder builder,
            SceneBuildingUtil util,
            boolean small
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                small
                        ? "worm_gear_small_cascade"
                        : "worm_gear_regular_cascade",
                small
                        ? "Connecting Small Worm Gears"
                        : "Connecting Worm Gears"
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

        BlockPos left =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos middle =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos right =
                util.grid().at(
                        3,
                        1,
                        2
                );

        BlockState singleState =
                small
                        ? smallWormState(
                        Direction.Axis.X,
                        false,
                        false
                )
                        : regularWormState(
                        Direction.Axis.X,
                        false,
                        false
                );

        BlockState leftState =
                small
                        ? smallWormState(
                        Direction.Axis.X,
                        false,
                        true
                )
                        : regularWormState(
                        Direction.Axis.X,
                        false,
                        true
                );

        BlockState rightState =
                small
                        ? smallWormState(
                        Direction.Axis.X,
                        true,
                        false
                )
                        : regularWormState(
                        Direction.Axis.X,
                        true,
                        false
                );

        BlockState middleState =
                small
                        ? smallWormState(
                        Direction.Axis.X,
                        true,
                        true
                )
                        : regularWormState(
                        Direction.Axis.X,
                        true,
                        true
                );

        scene.world().setBlock(
                left,
                singleState,
                false
        );

        scene.world().showSection(
                util.select().position(left),
                Direction.DOWN
        );

        scene.idle(
                15
        );

        scene.world().setBlock(
                middle,
                rightState,
                false
        );

        scene.world().setBlock(
                left,
                leftState,
                false
        );

        scene.world().showSection(
                util.select().position(middle),
                Direction.WEST
        );

        scene.idle(
                15
        );

        scene.world().setBlock(
                right,
                rightState,
                false
        );

        scene.world().setBlock(
                middle,
                middleState,
                false
        );

        scene.world().showSection(
                util.select().position(right),
                Direction.WEST
        );

        scene.effects().indicateSuccess(
                middle
        );

        scene.idle(
                15
        );

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                left,
                                right
                        ),
                        90
                )
                .text(
                        "Worm Gears of the same type connect when placed directly beside each other along the same rotation axis."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(middle)
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
                        "Adjacent Worm Gears join seamlessly, allowing several blocks to form one continuous worm shaft."
                )
                .pointAt(
                        util.vector().centerOf(middle)
                )
                .placeNearTarget();

        scene.idle(
                95
        );

        scene.world().setKineticSpeed(
                util.select().fromTo(
                        left,
                        right
                ),
                64.0F
        );

        scene.effects().rotationSpeedIndicator(
                middle
        );

        scene.overlay().showText(
                        80
                )
                .text(
                        "Connected Worm Gears also share rotation through their common shaft axis."
                )
                .pointAt(
                        util.vector().centerOf(middle)
                )
                .placeNearTarget();

        scene.idle(
                90
        );

        scene.world().hideSection(
                util.select().fromTo(
                        left,
                        right
                ),
                Direction.UP
        );

        scene.idle(
                20
        );

        clearWorkspace(
                scene,
                util
        );

        BlockPos source =
                util.grid().at(
                        0,
                        1,
                        3
                );

        BlockPos firstWorm =
                util.grid().at(
                        1,
                        1,
                        3
                );

        BlockPos firstCog =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos verticalShaft =
                util.grid().at(
                        1,
                        2,
                        2
                );

        BlockPos secondWorm =
                util.grid().at(
                        1,
                        3,
                        2
                );

        BlockPos secondCog =
                util.grid().at(
                        2,
                        3,
                        2
                );

        BlockPos output =
                util.grid().at(
                        2,
                        3,
                        1
                );

        BlockState shaftX =
                AllBlocks.SHAFT
                        .get()
                        .defaultBlockState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        BlockState shaftY =
                AllBlocks.SHAFT
                        .get()
                        .defaultBlockState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Y
                        );

        BlockState shaftZ =
                AllBlocks.SHAFT
                        .get()
                        .defaultBlockState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Z
                        );

        BlockState firstCogState =
                (
                        small
                                ? AllBlocks.LARGE_COGWHEEL.get()
                                : AllBlocks.COGWHEEL.get()
                )
                        .defaultBlockState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Y
                        );

        BlockState secondCogState =
                (
                        small
                                ? AllBlocks.LARGE_COGWHEEL.get()
                                : AllBlocks.COGWHEEL.get()
                )
                        .defaultBlockState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Z
                        );

        BlockState firstWormState =
                small
                        ? smallWormState(
                        Direction.Axis.X,
                        false,
                        false
                )
                        : regularWormState(
                        Direction.Axis.X,
                        false,
                        false
                );

        BlockState secondWormState =
                small
                        ? smallWormState(
                        Direction.Axis.Y,
                        false,
                        false
                )
                        : regularWormState(
                        Direction.Axis.Y,
                        false,
                        false
                );

        scene.world().setBlock(
                source,
                shaftX,
                false
        );

        scene.world().setBlock(
                firstWorm,
                firstWormState,
                false
        );

        scene.world().setBlock(
                firstCog,
                firstCogState,
                false
        );

        scene.world().setBlock(
                verticalShaft,
                shaftY,
                false
        );

        scene.world().setBlock(
                secondWorm,
                secondWormState,
                false
        );

        scene.world().setBlock(
                secondCog,
                secondCogState,
                false
        );

        scene.world().setBlock(
                output,
                shaftZ,
                false
        );

        scene.world().showSection(
                util.select().fromTo(
                        source,
                        firstWorm
                ),
                Direction.DOWN
        );

        scene.world().showSection(
                util.select().fromTo(
                        firstCog,
                        secondWorm
                ),
                Direction.DOWN
        );

        scene.world().showSection(
                util.select().fromTo(
                        output,
                        secondCog
                ),
                Direction.DOWN
        );

        float firstOutput =
                small
                        ? -8.0F
                        : -16.0F;

        float secondOutput =
                small
                        ? -1.0F
                        : -4.0F;

        scene.world().setKineticSpeed(
                util.select().fromTo(
                        source,
                        firstWorm
                ),
                64.0F
        );

        scene.world().setKineticSpeed(
                util.select().fromTo(
                        firstCog,
                        secondWorm
                ),
                firstOutput
        );

        scene.world().setKineticSpeed(
                util.select().fromTo(
                        output,
                        secondCog
                ),
                secondOutput
        );

        scene.effects().rotationSpeedIndicator(
                firstWorm
        );

        scene.effects().rotationSpeedIndicator(
                firstCog
        );

        scene.effects().rotationSpeedIndicator(
                secondCog
        );

        scene.idle(
                20
        );

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                firstWorm,
                                firstCog
                        ),
                        90
                )
                .text(
                        small
                                ? "The first Small Worm Gear stage multiplies the transferred stress capacity by 8."
                                : "The first Worm Gear stage multiplies the transferred stress capacity by 4."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(firstCog)
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                100
        );

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                secondWorm,
                                secondCog
                        ),
                        90
                )
                .text(
                        small
                                ? "On the second Small Worm Gear stage, the capacity multiplier is reduced to 4."
                                : "On the second Worm Gear stage, the capacity multiplier is reduced to 2."
                )
                .pointAt(
                        util.vector().centerOf(secondCog)
                )
                .placeNearTarget();

        scene.idle(
                100
        );

        scene.overlay().showText(
                        95
                )
                .text(
                        small
                                ? "Further Small Worm Gear stages use 2x on the third stage and 1x from the fourth stage onward."
                                : "Further Worm Gear stages use 1.5x on the third stage and 1x from the fourth stage onward."
                )
                .pointAt(
                        util.vector().centerOf(output)
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                105
        );
    }

    static void backdrive(
            SceneBuilder builder,
            SceneBuildingUtil util,
            boolean small
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                small
                        ? "worm_gear_small_backdrive"
                        : "worm_gear_regular_backdrive",
                small
                        ? "One-Way Small Worm Gear"
                        : "One-Way Worm Gear"
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

        BlockPos worm =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos cog =
                util.grid().at(
                        2,
                        1,
                        1
                );

        BlockPos driveShaft =
                util.grid().at(
                        2,
                        2,
                        1
                );

        BlockState wormState =
                small
                        ? smallWormState(
                        Direction.Axis.X,
                        false,
                        false
                )
                        : regularWormState(
                        Direction.Axis.X,
                        false,
                        false
                );

        BlockState cogState =
                (
                        small
                                ? AllBlocks.LARGE_COGWHEEL.get()
                                : AllBlocks.COGWHEEL.get()
                )
                        .defaultBlockState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Y
                        );

        BlockState shaftY =
                AllBlocks.SHAFT
                        .get()
                        .defaultBlockState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Y
                        );

        scene.world().setBlock(
                worm,
                wormState,
                false
        );

        scene.world().setBlock(
                cog,
                cogState,
                false
        );

        scene.world().setBlock(
                driveShaft,
                shaftY,
                false
        );

        scene.world().showSection(
                util.select().position(worm),
                Direction.DOWN
        );

        scene.world().showSection(
                util.select().fromTo(
                        cog,
                        driveShaft
                ),
                Direction.DOWN
        );

        scene.idle(
                20
        );

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                worm,
                                cog
                        ),
                        90
                )
                .text(
                        small
                                ? "The Small Worm Gear is one-way: the worm can drive the Large Cogwheel, but the Large Cogwheel cannot drive the worm."
                                : "The Worm Gear is one-way: the worm can drive the Cogwheel, but the Cogwheel cannot drive the worm."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(cog)
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                100
        );

        scene.world().setKineticSpeed(
                util.select().fromTo(
                        cog,
                        driveShaft
                ),
                32.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(worm),
                0.0F
        );

        scene.effects().rotationDirectionIndicator(
                cog
        );

        scene.effects().rotationSpeedIndicator(
                cog
        );

        scene.overlay().showText(
                        95
                )
                .text(
                        "Powering the Cogwheel from another kinetic source creates a back-driven connection. This rotation is not transferred into the worm."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(cog)
                )
                .placeNearTarget();

        scene.idle(
                105
        );

        scene.overlay().showText(
                        95
                )
                .text(
                        "A back-driven Cogwheel applies effectively unlimited stress to its kinetic network."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(cog)
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                105
        );

        scene.overlay().showText(
                        100
                )
                .text(
                        "If the back-driving network has more than 1024 SU of stress capacity, the Cogwheel is destroyed."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(cog)
                )
                .placeNearTarget();

        scene.idle(
                70
        );

        scene.world().hideSection(
                util.select().position(cog),
                Direction.UP
        );

        scene.idle(
                15
        );

        scene.world().setBlock(
                cog,
                Blocks.AIR.defaultBlockState(),
                false
        );

        scene.idle(
                45
        );
    }
}
