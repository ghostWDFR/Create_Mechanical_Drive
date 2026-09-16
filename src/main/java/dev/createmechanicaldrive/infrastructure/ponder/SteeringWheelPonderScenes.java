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

final class SteeringWheelPonderScenes {
    private SteeringWheelPonderScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                        loc("steering_wheel")
                )
                .addStoryBoard(
                        STEERING_WHEEL_SCHEMATIC,
                        SteeringWheelPonderScenes::steeringWheelOperation
                );
    }

    private static void steeringWheelOperation(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "steering_wheel_operation",
                "Using the Steering Wheel"
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

        BlockPos wheel =
                util.grid().at(
                        2,
                        1,
                        1
                );

        BlockPos shaft =
                wheel.south();

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

        BlockState shaftState =
                AllBlocks.SHAFT
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Z
                        );

        scene.world().setBlock(
                shaft,
                shaftState,
                false
        );

        scene.world().setBlock(
                wheel,
                wheelState,
                false
        );

        Selection assembly =
                util.select().fromTo(
                        wheel,
                        shaft
                );

        scene.world().showSection(
                assembly,
                Direction.DOWN
        );

        scene.idle(
                20
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "The Steering Wheel is a small kinetic source: turn it by hand to generate rotation."
                )
                .pointAt(
                        util.vector().centerOf(
                                wheel
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

        scene.world().setKineticSpeed(
                util.select().position(
                        shaft
                ),
                8.0F
        );

        scene.effects().rotationSpeedIndicator(
                shaft
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "Slow movement creates low RPM. The output scales with how quickly the wheel is turned."
                )
                .colored(
                        PonderPalette.SLOW
                )
                .pointAt(
                        util.vector().centerOf(
                                shaft
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        animateSteeringWheel(
                scene,
                wheel,
                0.0F,
                180.0F,
                84,
                1
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        shaft
                ),
                32.0F
        );

        scene.effects().rotationSpeedIndicator(
                shaft
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "The generated speed is dynamic and clamps between 2 RPM and 32 RPM."
                )
                .colored(
                        PonderPalette.FAST
                )
                .pointAt(
                        util.vector().centerOf(
                                wheel
                        )
                )
                .placeNearTarget();

        animateSteeringWheel(
                scene,
                wheel,
                180.0F,
                900.0F,
                84,
                1
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        shaft
                ),
                -16.0F
        );

        scene.effects().rotationDirectionIndicator(
                shaft
        );

        scene.overlay().showText(
                        65
                )
                .text(
                        "Turning it the other way reverses the generated rotation."
                )
                .pointAt(
                        util.vector().centerOf(
                                shaft
                        )
                )
                .placeNearTarget();

        animateSteeringWheel(
                scene,
                wheel,
                900.0F,
                540.0F,
                70,
                1
        );

        scene.overlay().showText(
                        80
                )
                .text(
                        "It provides 4 SU of stress capacity to the connected kinetic network."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(
                                wheel
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        animateSteeringWheel(
                scene,
                wheel,
                540.0F,
                156.0F,
                80,
                1
        );

        scene.world().setKineticSpeed(
                util.select().position(
                        shaft
                ),
                0.0F
        );

        scene.idle(
                10
        );

        scene.overlay().showControls(
                        util.vector().blockSurface(
                                wheel,
                                Direction.NORTH
                        ),
                        Pointing.DOWN,
                        35
                )
                .rightClick()
                .withItem(
                        new ItemStack(
                                Items.RED_DYE
                        )
                );

        SteeringWheelColor[] colors =
                new SteeringWheelColor[]{
                        SteeringWheelColor.BLACK,
                        SteeringWheelColor.BLUE,
                        SteeringWheelColor.GRAY,
                        SteeringWheelColor.GREEN,
                        SteeringWheelColor.RED,
                        SteeringWheelColor.YELLOW
                };

        for (SteeringWheelColor color : colors) {
            scene.world().modifyBlock(
                    wheel,
                    state ->
                            state.setValue(
                                    SteeringWheelBlock.COLOR,
                                    color
                            ),
                    false
            );

            scene.idle(
                    18
            );
        }

        scene.overlay().showText(
                        75
                )
                .text(
                        "Apply dyes directly to the wheel to recolor it."
                )
                .pointAt(
                        util.vector().centerOf(
                                wheel
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                85
        );
    }
}
