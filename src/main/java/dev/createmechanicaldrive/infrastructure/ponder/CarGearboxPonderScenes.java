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
import dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.GearboxPositions;
import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.*;

final class CarGearboxPonderScenes {
    private CarGearboxPonderScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                        loc("car_gearbox_input"),
                        loc("car_gearbox_speed"),
                        loc("gearbox_lever_axial"),
                        loc("gearbox_lever_linear")
                )
                .addStoryBoard(ASSEMBLY_SCHEMATIC, CarGearboxPonderScenes::assembly,
                        CreateMechanicalDrivePonderTags.GEARBOXES)
                .addStoryBoard(CONTROLS_SCHEMATIC, CarGearboxPonderScenes::inputAndLevers,
                        CreateMechanicalDrivePonderTags.GEARBOXES)
                .addStoryBoard(SPEED_SCHEMATIC, CarGearboxPonderScenes::speedConversion,
                        CreateMechanicalDrivePonderTags.GEARBOXES);
    }

    private static void assembly(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("car_gearbox_assembly", "Assembling the Gearbox");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.85F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        BlockPos speed = util.grid().at(2, 1, 3);
        BlockPos input = util.grid().at(3, 1, 3);
        BlockPos lever = input.above();
        BlockPos wrongSpeed = util.grid().at(2, 1, 1);
        BlockPos wrongInput = util.grid().at(3, 1, 1);

        scene.world().setBlock(speed, speedState(GEARBOX_FACING, GearboxPosition.NEUTRAL), false);
        scene.world().showSection(util.select().position(speed), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(70)
                .text("Place the rear speed stage first. Its FACING direction must point toward the front input half.")
                .pointAt(util.vector().blockSurface(speed, GEARBOX_FACING))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.world().setBlock(input, inputState(GEARBOX_FACING, GearboxPosition.NEUTRAL), false);
        scene.world().showSection(util.select().position(input), Direction.DOWN);
        scene.effects().indicateSuccess(input);
        scene.idle(10);

        scene.overlay().showOutlineWithText(util.select().fromTo(speed, input), 80)
                .text("A valid pair has the rear speed stage behind the input, with both halves using the same FACING.")
                .colored(PonderPalette.GREEN)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().setBlock(wrongSpeed, speedState(GEARBOX_FACING, GearboxPosition.NEUTRAL), false);
        scene.world().setBlock(wrongInput, inputState(GEARBOX_FACING.getOpposite(), GearboxPosition.NEUTRAL), false);
        scene.world().showSection(util.select().fromTo(wrongSpeed, wrongInput), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showOutlineWithText(util.select().fromTo(wrongSpeed, wrongInput), 80)
                .text("If the halves are turned incorrectly, the speed stage cannot find the input and stays in NEUTRAL.")
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().hideSection(util.select().fromTo(wrongSpeed, wrongInput), Direction.UP);
        scene.idle(15);

        scene.world().setBlock(lever, ponderLeverState(GEARBOX_FACING, GearboxPosition.NEUTRAL), false);
        scene.world().showSection(util.select().position(lever), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Levers can only be mounted on top of the front input half. They must face along the same axis.")
                .pointAt(util.vector().centerOf(lever).add(0, -0.25, 0))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().setBlock(lever, ponderLeverState(GEARBOX_FACING, GearboxPosition.NEUTRAL), false);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Use either the axial or linear lever. Place one of them; both send the selected gear to the input block.")
                .pointAt(util.vector().centerOf(lever).add(0, -0.25, 0))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);
    }

    private static void inputAndLevers(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("car_gearbox_controls", "Rotation Input and Levers");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.85F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        GearboxPositions positions = placeCompleteGearbox(scene, util, GearboxPosition.NEUTRAL);
        Selection drivetrain = util.select().fromTo(positions.outputShaft(), positions.sourceShaft());
        scene.world().showSection(drivetrain, Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(positions.input(), positions.sourceShaft()), 64.0F);
        scene.world().setKineticSpeed(outputSelection(util, positions), 0.0F);
        scene.idle(15);

        scene.overlay().showLine(PonderPalette.INPUT,
                util.vector().centerOf(positions.sourceShaft()),
                util.vector().blockSurface(positions.input(), GEARBOX_FACING), 70);
        scene.overlay().showText(75)
                .text("Feed rotation from the front input side, along FACING. In this scene the source is on the right.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(positions.input(), GEARBOX_FACING))
                .placeNearTarget()
                .attachKeyFrame();
        scene.effects().rotationDirectionIndicator(positions.sourceShaft());
        scene.effects().rotationSpeedIndicator(positions.sourceShaft());
        scene.idle(85);

        showAxialLever(scene, util, positions, GearboxPosition.NEUTRAL);
        scene.overlay().showText(65)
                .text("Mount the lever on top of the input half before selecting gears.")
                .pointAt(util.vector().topOf(positions.lever()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(75);

        shiftTo(scene, util, positions, GearboxPosition.FIRST, 32.0F);
        scene.overlay().showControls(util.vector().topOf(positions.lever()), Pointing.DOWN, 45)
                .rightClick()
                .withItem(new ItemStack(CreateMechanicalDrive.GEARBOX_AXIAL_LEVER_ITEM.get()));
        scene.overlay().showText(75)
                .text("Drag the axial lever into first gear: 0.5x turns 64 RPM into 32 RPM.")
                .pointAt(util.vector().topOf(positions.lever()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.effects().rotationSpeedIndicator(positions.outputShaft());
        scene.idle(85);

        scene.world().setBlock(positions.lever(), ponderLeverState(GEARBOX_FACING, GearboxPosition.FIRST), false);
        scene.idle(10);
        scene.overlay().showText(75)
                .text("The linear lever does the same job on a straight gear scale: reverse, neutral, first, second, third.")
                .pointAt(util.vector().topOf(positions.lever()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);

        shiftTo(scene, util, positions, GearboxPosition.THIRD, 96.0F);
        scene.overlay().showText(75)
                .text("Third gear is 1.5x: the same 64 RPM input leaves as 96 RPM.")
                .colored(PonderPalette.FAST)
                .pointAt(util.vector().centerOf(positions.outputShaft()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.effects().rotationSpeedIndicator(positions.outputShaft());
        scene.idle(85);
    }

    private static void speedConversion(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("car_gearbox_speed_conversion", "Speed Conversion");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.85F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        GearboxPositions positions = placeCompleteGearbox(scene, util, GearboxPosition.NEUTRAL);
        scene.world().showSection(util.select().fromTo(positions.outputShaft(), positions.sourceShaft()), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(positions.input(), positions.sourceShaft()), 64.0F);
        showAxialLever(scene, util, positions, GearboxPosition.NEUTRAL);
        scene.idle(15);

        shiftTo(scene, util, positions, GearboxPosition.NEUTRAL, 0.0F);
        scene.overlay().showText(65)
                .text("NEUTRAL has a 0.0x multiplier: the gearbox receives rotation, but the output does not spin.")
                .colored(PonderPalette.MEDIUM)
                .pointAt(util.vector().centerOf(positions.speed()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(75);

        shiftTo(scene, util, positions, GearboxPosition.SECOND, 64.0F);
        scene.overlay().showText(65)
                .text("SECOND is 1.0x: 64 RPM on the input gives 64 RPM on the output.")
                .pointAt(util.vector().centerOf(positions.outputShaft()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.effects().rotationSpeedIndicator(positions.outputShaft());
        scene.idle(75);

        shiftTo(scene, util, positions, GearboxPosition.THIRD, 96.0F);
        scene.overlay().showText(65)
                .text("THIRD is 1.5x: speed increases, and stress impact scales with the absolute multiplier.")
                .colored(PonderPalette.FAST)
                .pointAt(util.vector().centerOf(positions.speed()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.effects().rotationSpeedIndicator(positions.outputShaft());
        scene.idle(75);

        shiftTo(scene, util, positions, GearboxPosition.REVERSE, -32.0F);
        scene.overlay().showText(70)
                .text("REVERSE is -0.5x: the output slows to 32 RPM and reverses direction.")
                .colored(PonderPalette.SLOW)
                .pointAt(util.vector().centerOf(positions.outputShaft()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.effects().rotationDirectionIndicator(positions.outputShaft());
        scene.idle(80);

        BlockPos redstone = positions.speed().north();
        scene.world().setBlock(redstone, Blocks.REDSTONE_BLOCK.defaultBlockState(), false);
        scene.world().showSection(util.select().position(redstone), Direction.DOWN);
        shiftTo(scene, util, positions, GearboxPosition.NEUTRAL, 0.0F);
        scene.effects().indicateRedstone(positions.speed());
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Any redstone signal on the rear speed stage forces NEUTRAL, even when the lever selected a gear.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(redstone))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showControls(util.vector().topOf(positions.speed()), Pointing.DOWN, 55)
                .scroll()
                .withItem(new ItemStack(AllBlocks.SPEEDOMETER.get()));
        scene.overlay().showText(75)
                .text("The rear speed stage has an output direction setting. Scrolling can invert the final rotation.")
                .pointAt(util.vector().topOf(positions.speed()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);
    }
}
