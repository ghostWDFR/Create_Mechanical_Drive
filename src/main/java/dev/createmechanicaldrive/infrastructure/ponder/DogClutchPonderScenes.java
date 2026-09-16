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

final class DogClutchPonderScenes {
    private DogClutchPonderScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                        loc("dog_clutch")
                )
                .addStoryBoard(
                        DOG_CLUTCH_SCHEMATIC,
                        DogClutchPonderScenes::dogClutchApplications
                );
    }

    private static void dogClutchApplications(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "dog_clutch_applications",
                "Using the Dog Clutch"
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

        BlockState engagedClutch =
                CreateMechanicalDrive
                        .DOG_CLUTCH_ENGAGED
                        .get()
                        .defaultBlockState()
                        .setValue(
                                DogClutchBlock.FACING,
                                Direction.EAST
                        );

        BlockState disengagedClutch =
                CreateMechanicalDrive
                        .DOG_CLUTCH
                        .get()
                        .defaultBlockState()
                        .setValue(
                                DogClutchBlock.FACING,
                                Direction.EAST
                        );

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

        BlockPos branchCog =
                util.grid().at(
                        2,
                        1,
                        1
                );

        BlockPos branchShaft =
                util.grid().at(
                        3,
                        1,
                        1
                );

        scene.world().setBlock(
                input,
                shaftX,
                false
        );

        scene.world().setBlock(
                clutch,
                engagedClutch,
                false
        );

        scene.world().setBlock(
                output,
                shaftX,
                false
        );

        scene.world().setBlock(
                branchCog,
                cogX,
                false
        );

        scene.world().setBlock(
                branchShaft,
                shaftX,
                false
        );

        Selection mainLine =
                util.select().fromTo(
                        input,
                        output
                );

        Selection branch =
                util.select().fromTo(
                        branchShaft,
                        branchCog
                );

        scene.world().showSection(
                mainLine,
                Direction.DOWN
        );

        scene.idle(
                15
        );

        scene.world().showSection(
                branch,
                Direction.SOUTH
        );

        scene.idle(
                20
        );

        scene.world().setKineticSpeed(
                mainLine,
                64.0F
        );

        scene.world().setKineticSpeed(
                branch,
                -64.0F
        );

        scene.effects().rotationSpeedIndicator(
                input
        );

        scene.effects().rotationSpeedIndicator(
                branchCog
        );

        scene.overlay().showOutlineWithText(
                        util.select().position(
                                clutch
                        ),
                        90
                )
                .text(
                        "Without a redstone signal, the Dog Clutch is engaged."
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
                        90
                )
                .text(
                        "Its sliding gear meshes with a neighbouring cogwheel and transfers rotation into the side branch."
                )
                .pointAt(
                        util.vector().centerOf(
                                branchCog
                        )
                )
                .placeNearTarget();

        scene.idle(
                100
        );

        BlockPos redstone =
                util.grid().at(
                        2,
                        2,
                        2
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
                clutch
        );

        scene.idle(
                15
        );

        scene.world().setBlock(
                clutch,
                disengagedClutch,
                false
        );

        scene.world().setKineticSpeed(
                branch,
                0.0F
        );

        scene.idle(
                15
        );

        scene.overlay().showOutlineWithText(
                        util.select().position(
                                clutch
                        ),
                        90
                )
                .text(
                        "A redstone signal disengages the Dog Clutch and moves its gear away from the neighbouring cogwheel."
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
                100
        );

        scene.effects().rotationSpeedIndicator(
                output
        );

        scene.overlay().showText(
                        90
                )
                .text(
                        "The shaft through the Dog Clutch keeps rotating. Only the side connection is disconnected."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(
                                output
                        )
                )
                .placeNearTarget();

        scene.idle(
                100
        );

        scene.world().hideSection(
                util.select().fromTo(
                        branchShaft,
                        redstone
                ),
                Direction.UP
        );

        scene.world().hideSection(
                mainLine,
                Direction.UP
        );

        scene.idle(
                20
        );

        clearWorkspace(
                scene,
                util
        );

        BlockPos driveInput =
                util.grid().at(
                        0,
                        1,
                        2
                );

        BlockPos driveShaft =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos machineClutch =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos driveOutput =
                util.grid().at(
                        3,
                        1,
                        2
                );

        BlockPos machineCog =
                util.grid().at(
                        2,
                        1,
                        1
                );

        BlockPos machineShaft =
                util.grid().at(
                        3,
                        1,
                        1
                );

        scene.world().setBlock(
                driveInput,
                cogX,
                false
        );

        scene.world().setBlock(
                driveShaft,
                shaftX,
                false
        );

        scene.world().setBlock(
                machineClutch,
                engagedClutch,
                false
        );

        scene.world().setBlock(
                driveOutput,
                shaftX,
                false
        );

        scene.world().setBlock(
                machineCog,
                cogX,
                false
        );

        scene.world().setBlock(
                machineShaft,
                shaftX,
                false
        );

        Selection drive =
                util.select().fromTo(
                        driveInput,
                        driveOutput
                );

        Selection machine =
                util.select().fromTo(
                        machineShaft,
                        machineCog
                );

        scene.world().showSection(
                drive,
                Direction.DOWN
        );

        scene.world().showSection(
                machine,
                Direction.SOUTH
        );

        scene.world().setKineticSpeed(
                drive,
                64.0F
        );

        scene.world().setKineticSpeed(
                machine,
                -64.0F
        );

        scene.idle(
                20
        );

        scene.overlay().showOutlineWithText(
                        machine,
                        100
                )
                .text(
                        "This makes the Dog Clutch useful for machines that should only run when needed."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(
                                machineCog
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                110
        );

        BlockPos machineRedstone =
                machineClutch.above();

        scene.world().setBlock(
                machineRedstone,
                Blocks.REDSTONE_BLOCK
                        .defaultBlockState(),
                false
        );

        scene.world().showSection(
                util.select().position(
                        machineRedstone
                ),
                Direction.DOWN
        );

        scene.effects().indicateRedstone(
                machineClutch
        );

        scene.world().setBlock(
                machineClutch,
                disengagedClutch,
                false
        );

        scene.world().setKineticSpeed(
                machine,
                0.0F
        );

        scene.idle(
                20
        );

        scene.overlay().showText(
                        90
                )
                .text(
                        "Redstone can stop the secondary mechanism without stopping the main drivetrain."
                )
                .pointAt(
                        util.vector().centerOf(
                                machineCog
                        )
                )
                .placeNearTarget();

        scene.idle(
                100
        );

        scene.world().hideSection(
                util.select().fromTo(
                        0,
                        1,
                        0,
                        4,
                        2,
                        2
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

        BlockPos selectorInput =
                util.grid().at(
                        0,
                        1,
                        2
                );

        BlockPos selectorLeft =
                util.grid().at(
                        1,
                        1,
                        2
                );

        BlockPos selectorCenter =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos selectorRight =
                util.grid().at(
                        3,
                        1,
                        2
                );

        BlockPos selectorOutput =
                util.grid().at(
                        4,
                        1,
                        2
                );

        BlockPos branchA =
                util.grid().at(
                        1,
                        1,
                        1
                );

        BlockPos branchB =
                util.grid().at(
                        3,
                        1,
                        1
                );

        BlockPos branchAEnd =
                util.grid().at(
                        0,
                        1,
                        1
                );

        BlockPos branchBEnd =
                util.grid().at(
                        4,
                        1,
                        1
                );

        scene.world().setBlock(
                selectorInput,
                shaftX,
                false
        );

        scene.world().setBlock(
                selectorLeft,
                engagedClutch,
                false
        );

        scene.world().setBlock(
                selectorCenter,
                shaftX,
                false
        );

        scene.world().setBlock(
                selectorRight,
                disengagedClutch,
                false
        );

        scene.world().setBlock(
                selectorOutput,
                shaftX,
                false
        );

        scene.world().setBlock(
                branchA,
                cogX,
                false
        );

        scene.world().setBlock(
                branchAEnd,
                shaftX,
                false
        );

        scene.world().setBlock(
                branchB,
                cogX,
                false
        );

        scene.world().setBlock(
                branchBEnd,
                shaftX,
                false
        );

        Selection selectorMain =
                util.select().fromTo(
                        selectorInput,
                        selectorOutput
                );

        Selection selectorBranchA =
                util.select().fromTo(
                        branchAEnd,
                        branchA
                );

        Selection selectorBranchB =
                util.select().fromTo(
                        branchBEnd,
                        branchB
                );

        scene.world().showSection(
                selectorMain,
                Direction.DOWN
        );

        scene.world().showSection(
                selectorBranchA,
                Direction.SOUTH
        );

        scene.world().showSection(
                selectorBranchB,
                Direction.SOUTH
        );

        scene.world().setKineticSpeed(
                selectorMain,
                64.0F
        );

        scene.world().setKineticSpeed(
                selectorBranchA,
                -64.0F
        );

        scene.world().setKineticSpeed(
                selectorBranchB,
                0.0F
        );

        scene.idle(
                25
        );

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                selectorLeft,
                                selectorRight
                        ),
                        100
                )
                .text(
                        "Multiple Dog Clutches can select which branch receives power from a common drivetrain."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(
                                selectorCenter
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                110
        );

        scene.effects().rotationSpeedIndicator(
                branchA
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "Here, the first clutch is engaged."
                )
                .pointAt(
                        util.vector().centerOf(
                                selectorLeft
                        )
                )
                .placeNearTarget();

        scene.idle(
                80
        );

        scene.world().setBlock(
                selectorLeft,
                disengagedClutch,
                false
        );

        scene.world().setBlock(
                selectorRight,
                engagedClutch,
                false
        );

        scene.world().setKineticSpeed(
                selectorBranchA,
                0.0F
        );

        scene.world().setKineticSpeed(
                selectorBranchB,
                -64.0F
        );

        scene.effects().indicateSuccess(
                selectorRight
        );

        scene.idle(
                20
        );

        scene.effects().rotationSpeedIndicator(
                branchB
        );

        scene.overlay().showText(
                        90
                )
                .text(
                        "Switching the clutch states transfers power to the other branch while the main shaft keeps running."
                )
                .pointAt(
                        util.vector().centerOf(
                                selectorRight
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                100
        );
    }
}
