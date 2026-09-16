package dev.createmechanicaldrive.infrastructure.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.mechanical_starter.MechanicalStarterBlock;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.loc;

final class MechanicalStarterPonderScenes {

    private MechanicalStarterPonderScenes() {
    }

    static void register(
            PonderSceneRegistrationHelper<ResourceLocation> helper
    ) {
        helper.forComponents(
                        loc("mechanical_starter")
                )
                .addStoryBoard(
                        PonderSceneCommon.HAND_CRANK_SCHEMATIC,
                        MechanicalStarterPonderScenes::operation
                );
    }

    private static void operation(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "mechanical_starter_operation",
                "Starting a Kinetic Network"
        );

        scene.configureBasePlate(
                0,
                0,
                5
        );

        scene.scaleSceneView(
                0.9F
        );

        // Очищаем содержимое исходной schematic.
        scene.world().setBlocks(
                util.select().fromTo(
                        0,
                        1,
                        0,
                        4,
                        3,
                        4
                ),
                Blocks.AIR.defaultBlockState(),
                false
        );

        scene.showBasePlate();

        /*
         * Камера в этой Ponder-сцене смотрит примерно
         * со стороны NORTH.
         *
         * Поэтому кнопку разворачиваем на NORTH,
         * а shaft ставим с противоположной стороны,
         * то есть SOUTH.
         */

        BlockPos starterPos =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockPos shaftPos =
                util.grid().at(
                        2,
                        1,
                        3
                );

        BlockState starterState =
                CreateMechanicalDrive
                        .MECHANICAL_STARTER
                        .get()
                        .defaultBlockState()
                        .setValue(
                                MechanicalStarterBlock.FACING,
                                Direction.NORTH
                        );

        BlockState shaftState =
                AllBlocks.SHAFT
                        .getDefaultState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Z
                        );

        scene.world().setBlock(
                starterPos,
                starterState,
                false
        );

        scene.world().setBlock(
                shaftPos,
                shaftState,
                false
        );

        Selection setup =
                util.select().fromTo(
                        starterPos,
                        shaftPos
                );

        scene.world().showSection(
                setup,
                Direction.DOWN
        );

        scene.idle(
                20
        );

        scene.overlay().showText(
                        80
                )
                .text(
                        "The Mechanical Starter provides a short burst of rotational power to a stopped kinetic network."
                )
                .pointAt(
                        util.vector().blockSurface(
                                starterPos,
                                Direction.NORTH
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                90
        );

        /*
         * Показываем RMB прямо на красной кнопке,
         * а не на центре блока или стороне shaft.
         */
        scene.overlay().showControls(
                        util.vector().blockSurface(
                                starterPos,
                                Direction.NORTH
                        ),
                        Pointing.DOWN,
                        40
                )
                .rightClick();

        scene.idle(
                20
        );

        scene.effects().indicateSuccess(
                starterPos
        );

        /*
         * Визуально показываем импульс вращения.
         */
        scene.world().setKineticSpeed(
                setup,
                32.0F
        );

        scene.effects().rotationSpeedIndicator(
                shaftPos
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "Press the button with an empty hand to briefly output 32 RPM and up to 512 SU."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().blockSurface(
                                starterPos,
                                Direction.NORTH
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                80
        );

        /*
         * Импульс закончился.
         */
        scene.world().setKineticSpeed(
                setup,
                0.0F
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "The Starter only provides an impulse. Another power source must keep the mechanism running."
                )
                .pointAt(
                        util.vector().centerOf(
                                shaftPos
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                80
        );

        scene.markAsFinished();
    }
}