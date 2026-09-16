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

final class OverrunningClutchPonderScenes {
    private OverrunningClutchPonderScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                        loc("overrunning_clutch")
                )
                .addStoryBoard(
                        OVERRUNNING_CLUTCH_SCHEMATIC,
                        OverrunningClutchPonderScenes::overrunningClutchOperation
                );
    }

    private static void overrunningClutchOperation(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "overrunning_clutch_operation",
                "Using the Overrunning Clutch"
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

        BlockPos clutch =
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

        BlockPos machine =
                util.grid().at(
                        4,
                        1,
                        2
                );

        BlockPos redstone =
                clutch.above();

        BlockState shaftX =
                AllBlocks.SHAFT
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        BlockState cogX =
                AllBlocks.COGWHEEL
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.X
                        );

        BlockState clutchState =
                overrunningClutchState(
                        Direction.EAST
                );

        scene.world().setBlock(
                input,
                shaftX,
                false
        );

        scene.world().setBlock(
                output,
                shaftX,
                false
        );

        scene.world().setBlock(
                machine,
                cogX,
                false
        );

        scene.world().showSection(
                util.select().position(
                        input
                ),
                Direction.DOWN
        );

        scene.world().showSection(
                util.select().fromTo(
                        output,
                        machine
                ),
                Direction.DOWN
        );

        scene.idle(
                15
        );

        scene.overlay().showControls(
                        util.vector().centerOf(
                                clutch
                        ),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(
                        new ItemStack(
                                CreateMechanicalDrive
                                        .OVERRUNNING_CLUTCH_ITEM
                                        .get()
                        )
                );

        scene.idle(
                20
        );

        scene.world().setBlock(
                clutch,
                clutchState,
                false
        );

        scene.world().showSection(
                util.select().position(
                        clutch
                ),
                Direction.DOWN
        );

        scene.effects().indicateSuccess(
                clutch
        );

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                input,
                                output
                        ),
                        90
                )
                .text(
                        "The Overrunning Clutch is placed inline on a shaft. When possible, it aligns to neighbouring shaft connections."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(
                                clutch
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
                        "Place it so the shaft line passes through both ends of the clutch."
                )
                .pointAt(
                        util.vector().centerOf(
                                clutch
                        )
                )
                .placeNearTarget();

        scene.idle(
                95
        );

        Selection fullLine =
                util.select().fromTo(
                        input,
                        machine
                );

        scene.world().setKineticSpeed(
                fullLine,
                64.0F
        );

        scene.effects().rotationDirectionIndicator(
                input
        );

        scene.effects().rotationSpeedIndicator(
                machine
        );

        scene.overlay().showText(
                        90
                )
                .text(
                        "When the input turns in the allowed direction, the clutch engages and drives the output."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(
                                machine
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                100
        );

        Selection inputHalf =
                util.select().fromTo(
                        input,
                        clutch
                );

        Selection outputHalf =
                util.select().fromTo(
                        output,
                        machine
                );

        scene.world().setKineticSpeed(
                inputHalf,
                -64.0F
        );

        scene.world().setKineticSpeed(
                outputHalf,
                0.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        clutch
                ),
                0.0F
        );

        scene.effects().rotationDirectionIndicator(
                input
        );

        scene.idle(
                15
        );

        scene.overlay().showOutlineWithText(
                        util.select().position(
                                clutch
                        ),
                        95
                )
                .text(
                        "Reverse rotation is not transmitted. The input side can spin while the output side stays still."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(
                                clutch
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                105
        );

        scene.overlay().showText(
                        95
                )
                .text(
                        "Use it anywhere a mechanism should be driven forward, but allowed to coast or overrun backward."
                )
                .pointAt(
                        util.vector().centerOf(
                                clutch
                        )
                )
                .placeNearTarget();

        scene.idle(
                105
        );

        scene.overlay().showControls(
                        util.vector().topOf(
                                clutch
                        ),
                        Pointing.DOWN,
                        55
                )
                .scroll();

        scene.overlay().showText(
                        85
                )
                .text(
                        "Scroll the side setting to choose which rotation direction is allowed to pass through."
                )
                .pointAt(
                        util.vector().topOf(
                                clutch
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                95
        );

        scene.world().modifyBlockEntity(
                clutch,
                OverrunningClutchBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderDirection(
                                OverrunningClutchDirection.COUNTER_CLOCKWISE
                        )
        );

        scene.effects().indicateSuccess(
                clutch
        );

        scene.world().setKineticSpeed(
                fullLine,
                -64.0F
        );

        scene.effects().rotationSpeedIndicator(
                machine
        );

        scene.overlay().showText(
                        90
                )
                .text(
                        "After switching the setting, that rotation direction engages the clutch and drives the output again."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(
                                machine
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                100
        );

        scene.world().setKineticSpeed(
                inputHalf,
                64.0F
        );

        scene.world().setKineticSpeed(
                outputHalf,
                0.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        clutch
                ),
                0.0F
        );

        scene.effects().rotationDirectionIndicator(
                input
        );

        scene.idle(
                15
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
                clutch,
                clutchState.setValue(
                        OverrunningClutchBlock.POWERED,
                        true
                ),
                false
        );

        scene.world().setKineticSpeed(
                fullLine,
                64.0F
        );

        scene.effects().indicateRedstone(
                clutch
        );

        scene.effects().rotationSpeedIndicator(
                machine
        );

        scene.overlay().showText(
                        90
                )
                .text(
                        "A redstone signal locks the clutch, making it transmit rotation in both directions."
                )
                .colored(PonderPalette.RED)
                .pointAt(
                        util.vector().centerOf(
                                clutch
                        )
                )
                .placeNearTarget();

        scene.idle(
                100
        );
    }
}
