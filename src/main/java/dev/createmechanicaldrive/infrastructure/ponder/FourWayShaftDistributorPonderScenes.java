package dev.createmechanicaldrive.infrastructure.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.shaft_distributor.ShaftDistributorBlock;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.clearWorkspace;
import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.loc;

final class FourWayShaftDistributorPonderScenes {
    private static final ResourceLocation SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath(
                    "create",
                    "chain_drive/relay"
            );

    private FourWayShaftDistributorPonderScenes() {
    }

    static void register(
            PonderSceneRegistrationHelper<ResourceLocation> helper
    ) {
        helper.forComponents(loc("four_way_shaft_distributor"))
                .addStoryBoard(
                        SCHEMATIC,
                        FourWayShaftDistributorPonderScenes::operation
                );
    }

    private static void operation(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title(
                "four_way_shaft_distributor_operation",
                "Distributing Rotation in Four Directions"
        );
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.9F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        BlockPos distributor = util.grid().at(2, 1, 2);
        BlockPos north = distributor.north();
        BlockPos south = distributor.south();
        BlockPos east = distributor.east();
        BlockPos west = distributor.west();

        BlockState distributorState =
                CreateMechanicalDrive.FOUR_WAY_SHAFT_DISTRIBUTOR
                        .get()
                        .defaultBlockState()
                        .setValue(ShaftDistributorBlock.FACING, Direction.EAST)
                        .setValue(ShaftDistributorBlock.AXIS, Direction.Axis.X)
                        .setValue(ShaftDistributorBlock.OUTPUT_FACING, Direction.NORTH);

        scene.world().setBlock(distributor, distributorState, false);
        scene.world().setBlock(north, shaft(Direction.Axis.Z), false);
        scene.world().setBlock(south, shaft(Direction.Axis.Z), false);
        scene.world().setBlock(east, shaft(Direction.Axis.X), false);
        scene.world().setBlock(west, shaft(Direction.Axis.X), false);

        Selection center = util.select().position(distributor);
        Selection through = util.select().position(north)
                .add(util.select().position(south));
        Selection shortShafts = util.select().position(east)
                .add(util.select().position(west));
        Selection assembly = center.copy()
                .add(through)
                .add(shortShafts);

        scene.world().showSection(assembly, Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(70)
                .text("Rotation can be supplied through any of the four shaft connections.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().centerOf(distributor))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.world().setKineticSpeed(center, 32.0F);
        scene.world().setKineticSpeed(through, 32.0F);
        scene.world().setKineticSpeed(util.select().position(east), -32.0F);
        scene.world().setKineticSpeed(util.select().position(west), 32.0F);
        scene.effects().rotationDirectionIndicator(north);
        scene.effects().rotationDirectionIndicator(south);
        scene.effects().rotationDirectionIndicator(east);
        scene.effects().rotationDirectionIndicator(west);

        scene.overlay().showText(70)
                .text("The longitudinal shaft passes straight through the block without changing speed.")
                .colored(PonderPalette.GREEN)
                .pointAt(through.getCenter())
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.overlay().showText(80)
                .text("The two short shafts are geared from opposite sides and therefore rotate in opposite directions.")
                .pointAt(shortShafts.getCenter())
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(75)
                .text("Placement and wrench rotation work exactly like on the regular Shaft Distributor.")
                .pointAt(util.vector().centerOf(distributor))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);
    }

    private static BlockState shaft(
            Direction.Axis axis
    ) {
        return AllBlocks.SHAFT
                .getDefaultState()
                .setValue(RotatedPillarBlock.AXIS, axis);
    }
}