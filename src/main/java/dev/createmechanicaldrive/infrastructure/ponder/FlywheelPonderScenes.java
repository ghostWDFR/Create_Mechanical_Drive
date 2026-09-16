package dev.createmechanicaldrive.infrastructure.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineFlywheelBlock;
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

import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.loc;

final class FlywheelPonderScenes {
    private static final ResourceLocation SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath(
                    "create",
                    "chain_drive/relay"
            );

    private FlywheelPonderScenes() {
    }

    static void register(
            PonderSceneRegistrationHelper<ResourceLocation> helper
    ) {
        helper.forComponents(loc("stirling_engine_flywheel"))
                .addStoryBoard(
                        SCHEMATIC,
                        FlywheelPonderScenes::operation
                );
    }

    private static void operation(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "flywheel_operation",
                "Using the Flywheel"
        );
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.9F);
        scene.world().setBlocks(
                util.select().fromTo(0, 1, 0, 5, 3, 4),
                Blocks.AIR.defaultBlockState(),
                false
        );
        scene.showBasePlate();

        BlockPos flywheel =
                util.grid().at(2, 1, 2);
        Selection shaftLine =
                util.select().fromTo(0, 1, 2, 4, 1, 2);

        scene.world().setBlocks(
                shaftLine,
                shaftState(),
                false
        );
        scene.world().showSection(
                shaftLine,
                Direction.DOWN
        );
        scene.idle(15);

        scene.overlay().showControls(
                        util.vector().topOf(flywheel),
                        Pointing.DOWN,
                        35
                )
                .withItem(
                        new ItemStack(
                                CreateMechanicalDrive
                                        .STIRLING_ENGINE_FLYWHEEL_ITEM
                                        .get()
                        )
                );
        scene.idle(40);

        scene.world().setBlock(
                flywheel,
                flywheelState(),
                false
        );
        scene.effects().indicateSuccess(flywheel);
        scene.overlay().showText(70)
                .text(
                        "Flywheels can be installed directly into any rotating shaft line."
                )
                .pointAt(util.vector().centerOf(flywheel))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.world().setKineticSpeed(
                shaftLine,
                32.0F
        );
        scene.effects().rotationSpeedIndicator(flywheel);
        scene.overlay().showText(75)
                .text(
                        "Below 64 RPM, the Flywheel is still building momentum and provides no bonus."
                )
                .colored(PonderPalette.SLOW)
                .pointAt(util.vector().centerOf(flywheel))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);

        scene.world().setKineticSpeed(
                shaftLine,
                128.0F
        );
        scene.effects().rotationSpeedIndicator(flywheel);
        scene.effects().indicateSuccess(flywheel);
        scene.overlay().showText(80)
                .text(
                        "At 64 RPM or faster, each effective Flywheel adds 512 SU of stress capacity."
                )
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(flywheel))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        for (int x : new int[]{1, 3, 4}) {
            BlockPos pos =
                    util.grid().at(x, 1, 2);
            scene.world().setBlock(
                    pos,
                    flywheelState(),
                    false
            );
            scene.world().setKineticSpeed(
                    util.select().position(pos),
                    128.0F
            );
            scene.effects().indicateSuccess(pos);
            scene.idle(10);
        }

        Selection fourFlywheels =
                util.select().fromTo(1, 1, 2, 4, 1, 2);
        scene.overlay().showOutlineWithText(
                        fourFlywheels,
                        80
                )
                .text(
                        "Up to four Flywheels on the same shaft provide their full benefit."
                )
                .colored(PonderPalette.GREEN)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        BlockPos excessFlywheel =
                util.grid().at(0, 1, 2);
        scene.world().setBlock(
                excessFlywheel,
                flywheelState(),
                false
        );
        scene.world().setKineticSpeed(
                shaftLine,
                128.0F
        );
        scene.effects().indicateRedstone(excessFlywheel);
        scene.overlay().showText(85)
                .text(
                        "Each additional Flywheel on that shaft adds no capacity and consumes 256 SU instead."
                )
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(excessFlywheel))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);
    }

    private static BlockState shaftState() {
        return AllBlocks.SHAFT
                .getDefaultState()
                .setValue(
                        RotatedPillarBlock.AXIS,
                        Direction.Axis.X
                );
    }

    private static BlockState flywheelState() {
        return CreateMechanicalDrive
                .STIRLING_ENGINE_FLYWHEEL
                .get()
                .defaultBlockState()
                .setValue(
                        StirlingEngineFlywheelBlock.AXIS,
                        Direction.Axis.X
                );
    }
}
