package dev.createmechanicaldrive.infrastructure.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.shaft_distributor.ShaftDistributorBlock;
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

import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.clearWorkspace;
import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.loc;

final class ShaftDistributorPonderScenes {
    private static final ResourceLocation SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath(
                    "create",
                    "chain_drive/relay"
            );

    private ShaftDistributorPonderScenes() {
    }

    static void register(
            PonderSceneRegistrationHelper<ResourceLocation> helper
    ) {
        helper.forComponents(
                        loc("shaft_distributor")
                )
                .addStoryBoard(
                        SCHEMATIC,
                        ShaftDistributorPonderScenes::operation
                );
    }

    private static void operation(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "shaft_distributor_operation",
                "Distributing Shaft Rotation"
        );

        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.9F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        BlockPos distributor =
                util.grid().at(2, 1, 2);
        BlockPos input =
                util.grid().at(3, 1, 2);
        BlockPos firstOutput =
                util.grid().at(2, 1, 1);
        BlockPos secondOutput =
                util.grid().at(2, 1, 3);

        BlockState inputShaft =
                shaft(Direction.Axis.X);
        BlockState outputShaft =
                shaft(Direction.Axis.Z);
        BlockState distributorState =
                distributorState(
                        Direction.EAST,
                        Direction.NORTH
                );

        scene.world().setBlock(
                distributor,
                distributorState,
                false
        );
        scene.world().setBlock(
                input,
                inputShaft,
                false
        );
        scene.world().setBlock(
                firstOutput,
                outputShaft,
                false
        );
        scene.world().setBlock(
                secondOutput,
                outputShaft,
                false
        );

        Selection distributorSelection =
                util.select().position(distributor);
        Selection inputSelection =
                util.select().position(input);
        Selection outputSelection =
                util.select().position(firstOutput)
                        .add(
                                util.select().position(secondOutput)
                        );
        Selection horizontalAssembly =
                distributorSelection.copy()
                        .add(inputSelection)
                        .add(outputSelection);

        scene.world().showSection(
                distributorSelection,
                Direction.DOWN
        );
        scene.idle(15);

        scene.overlay().showLine(
                PonderPalette.INPUT,
                util.vector().blockSurface(
                        distributor,
                        Direction.EAST
                ),
                util.vector().centerOf(input),
                65
        );
        scene.overlay().showText(65)
                .text(
                        "Rotation can be supplied through any of the three shaft connections."
                )
                .colored(PonderPalette.INPUT)
                .pointAt(
                        util.vector().blockSurface(
                                distributor,
                                Direction.EAST
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(75);

        scene.world().showSection(
                inputSelection,
                Direction.WEST
        );
        scene.world().setKineticSpeed(
                inputSelection,
                32.0F
        );
        scene.world().setKineticSpeed(
                distributorSelection,
                32.0F
        );
        scene.effects().rotationDirectionIndicator(input);

        scene.overlay().showText(65)
                .text(
                        "Rotation is transferred at 90 degrees between the input shaft and the longitudinal shaft."
                )
                .pointAt(
                        util.vector().centerOf(distributor)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(75);

        scene.world().showSection(
                outputSelection,
                Direction.DOWN
        );
        scene.world().setKineticSpeed(
                outputSelection,
                32.0F
        );
        scene.effects().rotationDirectionIndicator(firstOutput);
        scene.effects().rotationDirectionIndicator(secondOutput);

        scene.overlay().showText(75)
                .text(
                        "The longitudinal shaft crosses the block and drives both sides at the same speed and in the same direction."
                )
                .colored(PonderPalette.GREEN)
                .pointAt(
                        outputSelection.getCenter()
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);

        scene.world().setKineticSpeed(
                inputSelection,
                -32.0F
        );
        scene.world().setKineticSpeed(
                distributorSelection,
                -32.0F
        );
        scene.world().setKineticSpeed(
                outputSelection,
                -32.0F
        );
        scene.effects().rotationDirectionIndicator(input);
        scene.effects().rotationDirectionIndicator(firstOutput);

        scene.overlay().showText(70)
                .text(
                        "Either end of the longitudinal shaft can drive the input shaft and the opposite end."
                )
                .pointAt(
                        util.vector().centerOf(distributor)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.world().hideSection(
                horizontalAssembly,
                Direction.UP
        );
        scene.idle(20);
        scene.world().setBlocks(
                horizontalAssembly,
                Blocks.AIR.defaultBlockState(),
                false
        );

        BlockPos verticalInput =
                distributor.above();
        BlockPos leftOutput =
                distributor.west();
        BlockPos rightOutput =
                distributor.east();

        BlockState verticalDistributor =
                distributorState(
                        Direction.UP,
                        Direction.EAST
                );
        BlockState verticalInputShaft =
                shaft(Direction.Axis.Y);
        BlockState horizontalOutputShaft =
                shaft(Direction.Axis.X);

        scene.world().setBlock(
                distributor,
                verticalDistributor,
                false
        );
        scene.world().setBlock(
                verticalInput,
                verticalInputShaft,
                false
        );
        scene.world().setBlock(
                leftOutput,
                horizontalOutputShaft,
                false
        );
        scene.world().setBlock(
                rightOutput,
                horizontalOutputShaft,
                false
        );

        Selection verticalAssembly =
                util.select().position(distributor)
                        .add(
                                util.select().position(verticalInput)
                        )
                        .add(
                                util.select().position(leftOutput)
                        )
                        .add(
                                util.select().position(rightOutput)
                        );

        scene.world().showSection(
                verticalAssembly,
                Direction.DOWN
        );
        scene.world().setKineticSpeed(
                verticalAssembly,
                32.0F
        );
        scene.effects().rotationDirectionIndicator(verticalInput);
        scene.idle(15);

        scene.overlay().showText(70)
                .text(
                        "The input can face any of the six directions, including up and down. The longitudinal shaft remains perpendicular."
                )
                .pointAt(
                        util.vector().centerOf(distributor)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.overlay().showControls(
                        util.vector().topOf(distributor),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .whileSneaking()
                .withItem(
                        new ItemStack(
                                CreateMechanicalDrive
                                        .SHAFT_DISTRIBUTOR_ITEM
                                        .get()
                        )
                );
        scene.idle(55);

        scene.overlay().showText(75)
                .text(
                        "Normally the input points away from the player. Place while crouching to reverse it toward the player."
                )
                .colored(PonderPalette.INPUT)
                .pointAt(
                        util.vector().centerOf(distributor)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);
    }

    private static BlockState shaft(
            Direction.Axis axis
    ) {
        return AllBlocks.SHAFT
                .getDefaultState()
                .setValue(
                        RotatedPillarBlock.AXIS,
                        axis
                );
    }

    private static BlockState distributorState(
            Direction facing,
            Direction outputFacing
    ) {
        return CreateMechanicalDrive
                .SHAFT_DISTRIBUTOR
                .get()
                .defaultBlockState()
                .setValue(
                        ShaftDistributorBlock.FACING,
                        facing
                )
                .setValue(
                        ShaftDistributorBlock.AXIS,
                        facing.getAxis()
                )
                .setValue(
                        ShaftDistributorBlock.OUTPUT_FACING,
                        outputFacing
                );
    }
}