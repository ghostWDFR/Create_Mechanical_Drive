package dev.createmechanicaldrive.infrastructure.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.rotary_limiter.RotaryLimiterBlock;
import dev.createmechanicaldrive.content.rotary_limiter.RotaryLimiterBlockEntity;
import net.createmod.catnip.math.Pointing;
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

final class RotaryLimiterPonderScenes {
    private static final ResourceLocation SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath(
                    "create",
                    "chain_drive/relay"
            );

    private RotaryLimiterPonderScenes() {
    }

    static void register(
            PonderSceneRegistrationHelper<ResourceLocation> helper
    ) {
        helper.forComponents(loc("rotary_limiter"))
                .addStoryBoard(
                        SCHEMATIC,
                        RotaryLimiterPonderScenes::operation
                );
    }

    private static void operation(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "rotary_limiter_operation",
                "Limiting Rotary Travel"
        );
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.9F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        BlockPos limiter =
                util.grid().at(2, 1, 2);
        BlockPos input =
                util.grid().at(0, 1, 2);
        BlockPos output =
                util.grid().at(4, 1, 2);

        Selection limiterSelection =
                util.select().position(limiter);
        Selection inputSide =
                util.select().fromTo(0, 1, 2, 1, 1, 2);
        Selection outputSide =
                util.select().fromTo(3, 1, 2, 4, 1, 2);

        scene.world().setBlocks(
                inputSide,
                shaftState(),
                false
        );
        scene.world().setBlock(
                limiter,
                limiterState(),
                false
        );
        scene.world().setBlocks(
                outputSide,
                shaftState(),
                false
        );

        scene.world().showSection(
                limiterSelection,
                Direction.DOWN
        );
        scene.idle(10);
        scene.world().showSection(
                inputSide,
                Direction.EAST
        );
        scene.world().showSection(
                outputSide,
                Direction.WEST
        );
        scene.idle(15);

        scene.overlay().showLine(
                PonderPalette.INPUT,
                util.vector().centerOf(input),
                util.vector().blockSurface(
                        limiter,
                        Direction.WEST
                ),
                80
        );
        scene.overlay().showLine(
                PonderPalette.GREEN,
                util.vector().blockSurface(
                        limiter,
                        Direction.EAST
                ),
                util.vector().centerOf(output),
                80
        );
        scene.overlay().showText(80)
                .text(
                        "The rear shaft is the input. The facing side is an independent output."
                )
                .pointAt(util.vector().centerOf(limiter))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showControls(
                        util.vector().blockSurface(
                                limiter,
                                Direction.NORTH
                        ),
                        Pointing.DOWN,
                        30
                )
                .scroll();
        scene.idle(35);
        scene.overlay().showControls(
                        util.vector().topOf(limiter),
                        Pointing.DOWN,
                        30
                )
                .scroll();
        scene.idle(35);

        scene.overlay().showText(85)
                .text(
                        "Configure the maximum angle by scrolling on any side of the housing around the shaft."
                )
                .pointAt(
                        util.vector().blockSurface(
                                limiter,
                                Direction.NORTH
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);

        scene.overlay().showText(70)
                .text(
                        "This example uses 45 degrees. The available range is 5 to 360 degrees in 5 degree steps."
                )
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(limiter))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.world().setKineticSpeed(
                inputSide,
                64.0F
        );
        scene.effects().rotationDirectionIndicator(input);
        setOutputSpeed(
                scene,
                limiter,
                outputSide,
                output,
                16.0F
        );
        scene.overlay().showText(85)
                .text(
                        "The Limiter stores input rotation as a target angle. The output follows it at no more than 16 RPM."
                )
                .pointAt(util.vector().centerOf(limiter))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(9);
        setOutputSpeed(
                scene,
                limiter,
                outputSide,
                output,
                6.0F
        );
        scene.idle(1);
        setOutputSpeed(
                scene,
                limiter,
                outputSide,
                output,
                0.0F
        );
        scene.idle(85);

        scene.effects().indicateSuccess(limiter);
        scene.overlay().showText(80)
                .text(
                        "At the configured angle, the output stops even while the input keeps rotating."
                )
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(output))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().setKineticSpeed(
                inputSide,
                -64.0F
        );
        scene.effects().rotationDirectionIndicator(input);
        setOutputSpeed(
                scene,
                limiter,
                outputSide,
                output,
                -16.0F
        );
        scene.overlay().showText(85)
                .text(
                        "Reverse the input to move the output back toward the same limit in the opposite direction."
                )
                .pointAt(util.vector().centerOf(limiter))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(18);
        setOutputSpeed(
                scene,
                limiter,
                outputSide,
                output,
                -12.0F
        );
        scene.idle(1);
        setOutputSpeed(
                scene,
                limiter,
                outputSide,
                output,
                0.0F
        );
        scene.idle(76);
    }

    private static void setOutputSpeed(
            CreateSceneBuilder scene,
            BlockPos limiter,
            Selection outputSide,
            BlockPos output,
            float speed
    ) {
        scene.world().setKineticSpeed(
                outputSide,
                speed
        );
        scene.world().modifyBlockEntity(
                limiter,
                RotaryLimiterBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderOutputSpeed(speed)
        );

        if (Math.abs(speed) > 0.001F) {
            scene.effects().rotationDirectionIndicator(output);
        }
    }

    private static BlockState shaftState() {
        return AllBlocks.SHAFT
                .getDefaultState()
                .setValue(
                        RotatedPillarBlock.AXIS,
                        Direction.Axis.X
                );
    }

    private static BlockState limiterState() {
        return CreateMechanicalDrive
                .ROTARY_LIMITER
                .get()
                .defaultBlockState()
                .setValue(
                        RotaryLimiterBlock.FACING,
                        Direction.EAST
                )
                .setValue(
                        RotaryLimiterBlock.AXIS,
                        Direction.Axis.X
                );
    }
}
