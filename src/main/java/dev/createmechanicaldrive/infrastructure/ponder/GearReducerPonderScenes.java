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

final class GearReducerPonderScenes {
    private GearReducerPonderScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                        loc("gear_reducer")
                )
                .addStoryBoard(
                        GEAR_REDUCER_SCHEMATIC,
                        GearReducerPonderScenes::gearReducerRatio
                )
                .addStoryBoard(
                        GEAR_REDUCER_SCHEMATIC,
                        GearReducerPonderScenes::gearReducerRedstone
                );
    }

    private static void gearReducerRatio(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "gear_reducer_ratio",
                "Changing the Gear Ratio"
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

        BlockPos input =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos reducer =
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

        BlockState shaft =
                AllBlocks.SHAFT
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        BlockState reducerState =
                CreateMechanicalDrive
                        .GEAR_REDUCER
                        .get()
                        .defaultBlockState()
                        .setValue(
                                GearReducerBlock.FACING,
                                Direction.EAST
                        )
                        .setValue(
                                GearReducerBlock.AXIS,
                                Direction.Axis.X
                        )
                        .setValue(
                                GearReducerBlock.POWERED,
                                false
                        );

        scene.world().setBlock(
                input,
                shaft,
                false
        );

        scene.world().setBlock(
                reducer,
                reducerState,
                false
        );

        scene.world().setBlock(
                output,
                shaft,
                false
        );

        Selection inputSide =
                util.select().position(
                        input
                );

        Selection outputSide =
                util.select().position(
                        output
                );

        Selection drivetrain =
                util.select().fromTo(
                        input,
                        output
                );

        scene.world().showSection(
                drivetrain,
                Direction.DOWN
        );

        scene.world().setKineticSpeed(
                inputSide,
                32.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        reducer
                ),
                32.0F
        );

        scene.world().setKineticSpeed(
                outputSide,
                32.0F
        );

        scene.idle(
                20
        );

        scene.overlay().showControls(
                        util.vector().blockSurface(
                                reducer,
                                Direction.NORTH
                        ),
                        Pointing.DOWN,
                        50
                )
                .scroll();

        scene.overlay().showText(
                        70
                )
                .text(
                        "Scroll on the reducer to select a ratio from 1x to 4x."
                )
                .pointAt(
                        util.vector().centerOf(
                                reducer
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                80
        );

        scene.world().modifyBlockEntity(
                reducer,
                GearReducerBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderMultiplier(
                                2
                        )
        );

        scene.world().setKineticSpeed(
                outputSide,
                64.0F
        );

        scene.effects().rotationSpeedIndicator(
                input
        );

        scene.effects().rotationSpeedIndicator(
                output
        );

        scene.overlay().showText(
                        60
                )
                .text(
                        "2x"
                )
                .colored(
                        PonderPalette.FAST
                )
                .pointAt(
                        util.vector().centerOf(
                                output
                        )
                )
                .placeNearTarget();

        scene.idle(
                70
        );

        scene.world().modifyBlockEntity(
                reducer,
                GearReducerBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderMultiplier(
                                4
                        )
        );

        scene.world().setKineticSpeed(
                outputSide,
                128.0F
        );

        scene.effects().rotationSpeedIndicator(
                output
        );

        scene.overlay().showText(
                        60
                )
                .text(
                        "4x"
                )
                .colored(
                        PonderPalette.FAST
                )
                .pointAt(
                        util.vector().centerOf(
                                output
                        )
                )
                .placeNearTarget();

        scene.idle(
                70
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "Higher ratios also create more stress."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(
                                reducer
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                85
        );

        scene.world().setKineticSpeed(
                inputSide,
                32.0F
        );

        scene.world().setKineticSpeed(
                outputSide,
                128.0F
        );

        scene.effects().rotationDirectionIndicator(
                output
        );

        scene.idle(
                20
        );

        scene.world().setKineticSpeed(
                outputSide,
                32.0F
        );

        scene.world().setKineticSpeed(
                inputSide,
                8.0F
        );

        scene.effects().rotationSpeedIndicator(
                input
        );

        scene.effects().rotationSpeedIndicator(
                output
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        "Drive it from the output side and the same ratio works in reverse: 32 RPM becomes 8 RPM at 4x."
                )
                .pointAt(
                        util.vector().centerOf(
                                input
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                95
        );
    }

    private static void gearReducerRedstone(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "gear_reducer_redstone",
                "Redstone Override"
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

        BlockPos input =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos reducer =
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
                reducer.above();

        BlockState shaft =
                AllBlocks.SHAFT
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        BlockState reducerOff =
                CreateMechanicalDrive
                        .GEAR_REDUCER
                        .get()
                        .defaultBlockState()
                        .setValue(
                                GearReducerBlock.FACING,
                                Direction.EAST
                        )
                        .setValue(
                                GearReducerBlock.AXIS,
                                Direction.Axis.X
                        )
                        .setValue(
                                GearReducerBlock.POWERED,
                                false
                        );

        BlockState reducerOn =
                reducerOff.setValue(
                        GearReducerBlock.POWERED,
                        true
                );

        scene.world().setBlock(
                input,
                shaft,
                false
        );

        scene.world().setBlock(
                reducer,
                reducerOff,
                false
        );

        scene.world().setBlock(
                output,
                shaft,
                false
        );

        Selection inputSide =
                util.select().position(
                        input
                );

        Selection outputSide =
                util.select().position(
                        output
                );

        scene.world().showSection(
                util.select().fromTo(
                        input,
                        output
                ),
                Direction.DOWN
        );

        scene.world().modifyBlockEntity(
                reducer,
                GearReducerBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderMultiplier(
                                4
                        )
        );

        scene.world().setKineticSpeed(
                inputSide,
                32.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        reducer
                ),
                32.0F
        );

        scene.world().setKineticSpeed(
                outputSide,
                128.0F
        );

        scene.effects().rotationSpeedIndicator(
                input
        );

        scene.effects().rotationSpeedIndicator(
                output
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "The selected ratio is kept until you change it."
                )
                .pointAt(
                        util.vector().centerOf(
                                reducer
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                80
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

        scene.world().setBlock(
                reducer,
                reducerOn,
                false
        );

        scene.world().setKineticSpeed(
                outputSide,
                32.0F
        );

        scene.effects().indicateRedstone(
                reducer
        );

        scene.effects().rotationSpeedIndicator(
                output
        );

        scene.idle(
                15
        );

        scene.overlay().showText(
                        80
                )
                .text(
                        "Redstone forces the reducer to 1x."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(
                                reducer
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                90
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

        scene.world().setBlock(
                reducer,
                reducerOff,
                false
        );

        scene.world().setKineticSpeed(
                outputSide,
                128.0F
        );

        scene.effects().indicateSuccess(
                reducer
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
                        "Remove the signal and the selected 4x ratio returns."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(
                                output
                        )
                )
                .placeNearTarget();

        scene.idle(
                85
        );
    }
}
