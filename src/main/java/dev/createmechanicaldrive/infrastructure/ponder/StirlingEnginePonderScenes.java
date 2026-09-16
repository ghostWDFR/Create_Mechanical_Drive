package dev.createmechanicaldrive.infrastructure.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineCoreBlock;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineFlywheelBlock;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineHeaterBlock;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineHeaterBlockEntity;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineHeaterItem;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineOutputBlock;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEnginePoweredShaftBlock;
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
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.STIRLING_ENGINE_SCHEMATIC;
import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.loc;

final class StirlingEnginePonderScenes {
    private static final Direction ENGINE_FACING = Direction.EAST;

    private StirlingEnginePonderScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                        loc("stirling_engine_heater"),
                        loc("stirling_engine_core"),
                        loc("stirling_engine_output"),
                        loc("stirling_engine_heater_cover")
                )
                .addStoryBoard(
                        STIRLING_ENGINE_SCHEMATIC,
                        StirlingEnginePonderScenes::assembly
                )
                .addStoryBoard(
                        STIRLING_ENGINE_SCHEMATIC,
                        StirlingEnginePonderScenes::startup
                )
                .addStoryBoard(
                        STIRLING_ENGINE_SCHEMATIC,
                        StirlingEnginePonderScenes::shaftOrientation
                )
                .addStoryBoard(
                        STIRLING_ENGINE_SCHEMATIC,
                        StirlingEnginePonderScenes::heatManagement
                )
                .addStoryBoard(
                        STIRLING_ENGINE_SCHEMATIC,
                        StirlingEnginePonderScenes::flywheel
                )
                .addStoryBoard(
                        STIRLING_ENGINE_SCHEMATIC,
                        StirlingEnginePonderScenes::heaterCover
                );

        helper.forComponents(loc("stirling_engine_heater"))
                .addStoryBoard(
                        STIRLING_ENGINE_SCHEMATIC,
                        StirlingEnginePonderScenes::heaterHandling
                );
    }

    private static void assembly(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = begin(
                builder,
                util,
                "stirling_engine_assembly",
                "Assembling a Stirling Engine"
        );
        EnginePositions positions = positions(util);

        scene.world().setBlock(positions.heater(), heaterState(false), false);
        scene.world().setBlock(positions.core(), coreState(), false);
        scene.world().setBlock(positions.output(), outputState(), false);
        scene.world().setBlock(positions.shaft(), shaftState(Direction.Axis.Z), false);
        scene.world().setBlock(positions.flywheel(), flywheelState(Direction.Axis.Z), false);

        showText(
                scene,
                util,
                positions.core(),
                75,
                "A Stirling Engine is built from three blocks in a straight line."
        );

        showPlacement(
                scene,
                util,
                positions.heater(),
                new ItemStack(CreateMechanicalDrive.STIRLING_ENGINE_HEATER_ITEM.get())
        );
        showText(
                scene,
                util,
                positions.heater(),
                70,
                "The Heater sits at the hot end and stores incoming heat."
        );

        showPlacement(
                scene,
                util,
                positions.core(),
                new ItemStack(CreateMechanicalDrive.STIRLING_ENGINE_CORE_ITEM.get())
        );
        showText(
                scene,
                util,
                positions.core(),
                75,
                "Place the Core directly against the Heater, facing the same way."
        );

        showPlacement(
                scene,
                util,
                positions.output(),
                new ItemStack(CreateMechanicalDrive.STIRLING_ENGINE_OUTPUT_ITEM.get())
        );
        showText(
                scene,
                util,
                positions.output(),
                75,
                "Finish with the Output. Its front must point away from the Core."
        );

        Selection engine = util.select().fromTo(positions.heater(), positions.output());
        scene.overlay().showOutlineWithText(engine, 75)
                .text("All three blocks must share one direction and touch each other.")
                .colored(PonderPalette.GREEN)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);

        showPlacement(
                scene,
                util,
                positions.shaft(),
                AllBlocks.SHAFT.asStack()
        );
        showText(
                scene,
                util,
                positions.shaft(),
                85,
                "Leave one empty block after the Output, then place the crankshaft across the engine."
        );

        scene.world().setBlock(
                positions.shaft(),
                poweredShaftState(Direction.Axis.Z),
                false
        );
        scene.effects().indicateSuccess(positions.output());
        scene.idle(20);

        showPlacement(
                scene,
                util,
                positions.flywheel(),
                new ItemStack(CreateMechanicalDrive.STIRLING_ENGINE_FLYWHEEL_ITEM.get())
        );
        showText(
                scene,
                util,
                positions.flywheel(),
                75,
                "Mount the Flywheel on the same shaft to complete the running assembly."
        );
    }

    private static void startup(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = begin(
                builder,
                util,
                "stirling_engine_startup",
                "Starting a Stirling Engine"
        );
        EnginePositions positions = positions(util);
        Selection drive = driveSelection(util, positions);
        BlockPos starter = positions.shaft().relative(Direction.NORTH);
        Selection startingDrive = util.select().fromTo(starter, positions.flywheel());
        BlockState coldBurner = AllBlocks.BLAZE_BURNER.getDefaultState();
        BlockState hotBurner = coldBurner.setValue(
                BlazeBurnerBlock.HEAT_LEVEL,
                HeatLevel.KINDLED
        );
        BlockState starterState = AllBlocks.HAND_CRANK
                .getDefaultState()
                .setValue(DirectionalBlock.FACING, Direction.NORTH);

        placeCompleteEngine(scene, positions, Direction.Axis.Z, false, true);
        scene.world().setBlock(positions.burner(), coldBurner, false);
        scene.world().showSection(
                util.select().fromTo(positions.burner(), positions.shaft()),
                Direction.DOWN
        );
        scene.world().showSection(util.select().position(positions.flywheel()), Direction.DOWN);
        scene.idle(20);

        showText(
                scene,
                util,
                positions.burner(),
                85,
                "A Blaze Burner must touch the outside of the Heater. It needs to be Kindled or hotter."
        );

        scene.world().setBlock(positions.burner(), hotBurner, false);
        scene.world().modifyBlock(
                positions.heater(),
                state -> state.setValue(StirlingEngineHeaterBlock.LIT, true),
                false
        );
        scene.effects().indicateSuccess(positions.heater());
        showText(
                scene,
                util,
                positions.heater(),
                75,
                "Heat builds up gradually. Engineer's Goggles show how much is currently stored."
        );

        scene.world().setBlock(starter, starterState, false);
        scene.overlay().showControls(
                        util.vector().topOf(starter),
                        Pointing.DOWN,
                        25
                )
                .rightClick()
                .withItem(AllBlocks.HAND_CRANK.asStack());
        scene.world().showSection(util.select().position(starter), Direction.DOWN);
        scene.idle(35);
        scene.overlay().showControls(
                        util.vector().centerOf(starter),
                        Pointing.DOWN,
                        25
                )
                .rightClick();
        scene.idle(10);
        scene.world().setKineticSpeed(
                startingDrive,
                32.0F
        );
        scene.effects().rotationSpeedIndicator(starter);
        showText(
                scene,
                util,
                starter,
                80,
                "Use a Hand Crank to give the warm engine a short external turn."
        );

        scene.world().setKineticSpeed(
                startingDrive,
                256.0F
        );
        scene.effects().indicateSuccess(positions.output());
        scene.effects().rotationSpeedIndicator(positions.shaft());
        showText(
                scene,
                util,
                positions.output(),
                80,
                "The direction of that first turn becomes the engine's running direction."
        );

        scene.world().hideSection(util.select().position(starter), Direction.UP);
        scene.idle(10);
        scene.world().setBlock(starter, Blocks.AIR.defaultBlockState(), false);
        scene.world().setKineticSpeed(drive, 256.0F);

        showText(
                scene,
                util,
                positions.shaft(),
                75,
                "While heated, the Output can drive the connected shaft at up to 256 RPM."
        );

        BlockPos redstone = positions.core().above();
        scene.world().setBlock(redstone, Blocks.REDSTONE_BLOCK.defaultBlockState(), false);
        scene.world().showSection(util.select().position(redstone), Direction.DOWN);
        scene.world().setKineticSpeed(
                drive,
                0.0F
        );
        scene.effects().indicateRedstone(redstone);
        showText(
                scene,
                util,
                positions.core(),
                85,
                "A redstone signal on the Core stops the engine without removing its stored heat."
        );
    }

    private static void shaftOrientation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = begin(
                builder,
                util,
                "stirling_engine_shaft_orientation",
                "Positioning the Output Shaft"
        );
        EnginePositions positions = positions(util);

        placeCompleteEngine(scene, positions, Direction.Axis.Z, true, false);
        scene.world().showSection(
                util.select().fromTo(positions.heater(), positions.shaft()),
                Direction.DOWN
        );
        scene.world().setKineticSpeed(
                util.select().position(positions.shaft()),
                128.0F
        );
        scene.idle(20);

        showText(
                scene,
                util,
                positions.shaft(),
                75,
                "The crankshaft can lie horizontally across the engine."
        );

        scene.world().setKineticSpeed(
                util.select().position(positions.shaft()),
                0.0F
        );
        scene.world().setBlock(
                positions.shaft(),
                shaftState(Direction.Axis.X),
                false
        );
        scene.overlay().showOutlineWithText(
                        util.select().position(positions.shaft()),
                        75
                )
                .text("A shaft pointing along the engine cannot connect to the piston.")
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);

        scene.world().setBlock(
                positions.shaft(),
                poweredShaftState(Direction.Axis.Y),
                false
        );
        scene.world().setKineticSpeed(
                util.select().position(positions.shaft()),
                128.0F
        );
        scene.effects().indicateSuccess(positions.shaft());
        showText(
                scene,
                util,
                positions.shaft(),
                80,
                "The crankshaft can also stand vertically. Keep it perpendicular to the three engine blocks."
        );

        showText(
                scene,
                util,
                positions.output(),
                80,
                "The piston and linkage automatically rotate into the selected shaft plane."
        );
    }

    private static void heatManagement(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = begin(
                builder,
                util,
                "stirling_engine_heat_management",
                "Managing Stored Heat"
        );
        EnginePositions positions = positions(util);
        Selection drive = driveSelection(util, positions);
        BlockState hotBurner = AllBlocks.BLAZE_BURNER
                .getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED);

        placeCompleteEngine(scene, positions, Direction.Axis.Z, true, true);
        scene.world().setBlock(positions.burner(), hotBurner, false);
        scene.world().showSection(
                util.select().fromTo(positions.burner(), positions.shaft()),
                Direction.DOWN
        );
        scene.world().showSection(util.select().position(positions.flywheel()), Direction.DOWN);
        scene.world().setKineticSpeed(
                drive,
                256.0F
        );
        scene.idle(20);

        showText(
                scene,
                util,
                positions.heater(),
                80,
                "The Heater keeps storing heat for as long as the heat source remains active."
        );

        scene.world().setKineticSpeed(
                drive,
                128.0F
        );
        scene.effects().rotationSpeedIndicator(positions.shaft());
        scene.overlay().showText(80)
                .text("Above 70% stored heat, the engine starts slowing down in visible steps.")
                .colored(PonderPalette.SLOW)
                .pointAt(util.vector().centerOf(positions.heater()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().setKineticSpeed(
                drive,
                0.0F
        );
        scene.world().modifyBlock(
                positions.heater(),
                state -> state.setValue(StirlingEngineHeaterBlock.LIT, false),
                false
        );
        scene.overlay().showText(75)
                .text("At full heat, the engine overheats and stops.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(positions.heater()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);

        scene.world().hideSection(
                util.select().position(positions.burner()),
                Direction.DOWN
        );
        showText(
                scene,
                util,
                positions.heater(),
                85,
                "Remove the heat source and wait for the stored heat to fall before starting again."
        );

        showText(
                scene,
                util,
                positions.heater(),
                75,
                "Cooling happens automatically whenever the Heater is no longer receiving heat."
        );
    }

    private static void flywheel(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = begin(
                builder,
                util,
                "stirling_engine_flywheel",
                "Using the Flywheel"
        );
        EnginePositions positions = positions(util);
        Selection shaftLine = util.select().fromTo(5, 2, 1, 5, 2, 5);

        placeCompleteEngine(scene, positions, Direction.Axis.Z, true, true);
        scene.world().showSection(
                util.select().fromTo(positions.heater(), positions.shaft()),
                Direction.DOWN
        );
        scene.world().showSection(util.select().position(positions.flywheel()), Direction.DOWN);
        scene.idle(20);

        showText(
                scene,
                util,
                positions.flywheel(),
                75,
                "Mount Flywheels directly on the engine's output shaft."
        );

        scene.world().setKineticSpeed(shaftLine, 32.0F);
        scene.effects().rotationSpeedIndicator(positions.flywheel());
        showText(
                scene,
                util,
                positions.flywheel(),
                80,
                "Below 64 RPM, a Flywheel is still building momentum and provides no bonus."
        );

        scene.world().setKineticSpeed(shaftLine, 128.0F);
        scene.effects().indicateSuccess(positions.flywheel());
        scene.effects().rotationSpeedIndicator(positions.flywheel());
        showText(
                scene,
                util,
                positions.flywheel(),
                85,
                "At 64 RPM or faster, each effective Flywheel adds 512 SU of stress capacity."
        );

        for (int z : new int[]{1, 2, 5}) {
            BlockPos pos = util.grid().at(5, 2, z);
            scene.world().setBlock(pos, flywheelState(Direction.Axis.Z), false);
            scene.world().showSection(util.select().position(pos), Direction.DOWN);
            scene.idle(8);
        }
        scene.world().setKineticSpeed(shaftLine, 128.0F);
        scene.overlay().showOutlineWithText(shaftLine, 80)
                .text("Up to four Flywheels on the same shaft provide their full benefit.")
                .colored(PonderPalette.GREEN)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);
    }

    private static void heaterCover(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = begin(
                builder,
                util,
                "stirling_engine_cover",
                "Cooling with the Heater Cover"
        );
        EnginePositions positions = positions(util);
        BlockState hotBurner = AllBlocks.BLAZE_BURNER
                .getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED);
        Selection drive = util.select().fromTo(positions.shaft(), positions.flywheel());

        placeCompleteEngine(scene, positions, Direction.Axis.Z, true, true);
        scene.world().setBlock(positions.burner(), hotBurner, false);
        scene.world().showSection(
                util.select().fromTo(positions.burner(), positions.shaft()),
                Direction.DOWN
        );
        scene.world().showSection(util.select().position(positions.flywheel()), Direction.DOWN);
        scene.world().setKineticSpeed(drive, 256.0F);
        scene.idle(20);

        showText(
                scene,
                util,
                positions.heater(),
                70,
                "Apply the Heater Cover directly to the Heater."
        );
        scene.overlay().showControls(
                        util.vector().topOf(positions.heater()),
                        Pointing.DOWN,
                        25
                )
                .rightClick()
                .withItem(new ItemStack(CreateMechanicalDrive.STIRLING_ENGINE_HEATER_COVER_ITEM.get()));
        scene.idle(35);
        scene.world().modifyBlockEntity(
                positions.heater(),
                StirlingEngineHeaterBlockEntity.class,
                blockEntity -> blockEntity.setCover(true)
        );
        scene.idle(30);

        showText(
                scene,
                util,
                positions.heater(),
                85,
                "While covered, the Heater stops accepting heat and begins cooling."
        );

        scene.overlay().showControls(
                        util.vector().topOf(positions.heater()),
                        Pointing.DOWN,
                        25
                )
                .rightClick();
        scene.idle(35);
        showText(
                scene,
                util,
                positions.heater(),
                80,
                "Right-click the covered Heater with an empty hand to remove the Cover."
        );
        scene.world().modifyBlockEntity(
                positions.heater(),
                StirlingEngineHeaterBlockEntity.class,
                blockEntity -> blockEntity.setCover(false)
        );
        scene.idle(30);

        showText(
                scene,
                util,
                positions.heater(),
                70,
                "Once removed, the Heater can receive heat again."
        );
    }

    private static void heaterHandling(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = begin(
                builder,
                util,
                "stirling_engine_heater_handling",
                "Handling a Hot Heater"
        );
        EnginePositions positions = positions(util);
        BlockPos water = util.grid().at(5, 1, 3);
        ItemStack hotHeater = heatedHeaterStack();
        ItemStack cooledHeater = new ItemStack(
                CreateMechanicalDrive
                        .STIRLING_ENGINE_HEATER_ITEM
                        .get()
        );
        BlockState hotBurner = AllBlocks.BLAZE_BURNER
                .getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED);

        scene.world().setBlock(positions.burner(), hotBurner, false);
        scene.world().setBlock(positions.heater(), heaterState(true), false);
        scene.world().showSection(
                util.select().fromTo(positions.burner(), positions.heater()),
                Direction.DOWN
        );
        scene.idle(20);

        showText(
                scene,
                util,
                positions.heater(),
                80,
                "The Heater keeps its stored heat when broken and carried as an item."
        );

        scene.overlay().showControls(
                        util.vector().topOf(positions.heater()),
                        Pointing.DOWN,
                        35
                )
                .withItem(hotHeater);
        scene.overlay().showText(90)
                .text("At 70% stored heat or above, carrying it in a survival inventory hurts the player once per second.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(positions.heater()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.world().hideSection(
                util.select().fromTo(positions.burner(), positions.heater()),
                Direction.UP
        );
        scene.idle(15);

        scene.world().setBlocks(
                util.select().fromTo(1, 1, 1, 5, 1, 5),
                Blocks.GRASS_BLOCK.defaultBlockState(),
                false
        );
        scene.world().setBlocks(
                util.select().fromTo(4, 1, 2, 5, 1, 4),
                Blocks.WATER.defaultBlockState(),
                false
        );
        scene.world().setBlocks(
                util.select().fromTo(4, 0, 2, 5, 0, 4),
                Blocks.DIRT.defaultBlockState(),
                false
        );
        scene.world().setBlock(util.grid().at(3, 1, 2), Blocks.DIRT.defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(3, 1, 4), Blocks.DIRT.defaultBlockState(), false);
        scene.world().showSection(
                util.select().fromTo(1, 0, 1, 5, 1, 5),
                Direction.DOWN
        );
        scene.idle(25);

        scene.overlay().showControls(
                        util.vector().topOf(util.grid().at(2, 1, 3)),
                        Pointing.DOWN,
                        35
                )
                .withItem(hotHeater);
        scene.overlay().showLine(
                PonderPalette.INPUT,
                util.vector().topOf(util.grid().at(2, 1, 3)),
                util.vector().topOf(water),
                65
        );
        showText(
                scene,
                util,
                water,
                75,
                "Drop an overheated Heater into water to cool it quickly."
        );

        scene.world().createItemOnBeltLike(water, Direction.SOUTH, hotHeater);
        scene.idle(25);
        scene.effects().indicateSuccess(water);
        scene.world().removeItemsFromBelt(water);
        scene.world().createItemOnBeltLike(water, Direction.SOUTH, cooledHeater);
        scene.overlay().showText(80)
                .text("Water removes heat much faster than waiting in air.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(water))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        showText(
                scene,
                util,
                water,
                65,
                "Once cooled, the Heater can be picked up safely."
        );
    }

    private static CreateSceneBuilder begin(
            SceneBuilder builder,
            SceneBuildingUtil util,
            String id,
            String title
    ) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(id, title);
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.72F);
        scene.world().setBlocks(
                util.select().fromTo(0, 1, 0, 6, 6, 6),
                Blocks.AIR.defaultBlockState(),
                false
        );
        scene.showBasePlate();
        return scene;
    }

    private static EnginePositions positions(SceneBuildingUtil util) {
        BlockPos heater = util.grid().at(1, 2, 3);
        return new EnginePositions(
                heater.below(),
                heater,
                util.grid().at(2, 2, 3),
                util.grid().at(3, 2, 3),
                util.grid().at(5, 2, 3),
                util.grid().at(5, 2, 4)
        );
    }

    private static void placeCompleteEngine(
            CreateSceneBuilder scene,
            EnginePositions positions,
            Direction.Axis shaftAxis,
            boolean lit,
            boolean includeFlywheel
    ) {
        scene.world().setBlock(positions.heater(), heaterState(lit), false);
        scene.world().setBlock(positions.core(), coreState(), false);
        scene.world().setBlock(positions.output(), outputState(), false);
        scene.world().setBlock(positions.shaft(), poweredShaftState(shaftAxis), false);
        if (includeFlywheel) {
            scene.world().setBlock(positions.flywheel(), flywheelState(shaftAxis), false);
        }
    }

    private static void showPlacement(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            BlockPos pos,
            ItemStack stack
    ) {
        scene.overlay().showControls(
                        util.vector().topOf(pos),
                        Pointing.DOWN,
                        25
                )
                .rightClick()
                .withItem(stack);
        scene.world().showSection(util.select().position(pos), Direction.DOWN);
        scene.idle(35);
    }

    private static Selection driveSelection(
            SceneBuildingUtil util,
            EnginePositions positions
    ) {
        return util.select().fromTo(positions.shaft(), positions.flywheel());
    }

    private static void showText(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            BlockPos pos,
            int duration,
            String text
    ) {
        scene.overlay().showText(duration)
                .text(text)
                .pointAt(util.vector().centerOf(pos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(duration + 10);
    }

    private static ItemStack heatedHeaterStack() {
        ItemStack stack = new ItemStack(
                CreateMechanicalDrive
                        .STIRLING_ENGINE_HEATER_ITEM
                        .get()
        );
        StirlingEngineHeaterItem.setHeatTicks(stack, 20 * 60 * 9);
        return stack;
    }

    private static BlockState heaterState(boolean lit) {
        return CreateMechanicalDrive.STIRLING_ENGINE_HEATER
                .get()
                .defaultBlockState()
                .setValue(StirlingEngineHeaterBlock.FACING, ENGINE_FACING)
                .setValue(StirlingEngineHeaterBlock.AXIS, ENGINE_FACING.getAxis())
                .setValue(StirlingEngineHeaterBlock.LIT, lit);
    }

    private static BlockState coreState() {
        return CreateMechanicalDrive.STIRLING_ENGINE_CORE
                .get()
                .defaultBlockState()
                .setValue(StirlingEngineCoreBlock.FACING, ENGINE_FACING)
                .setValue(StirlingEngineCoreBlock.AXIS, ENGINE_FACING.getAxis());
    }

    private static BlockState outputState() {
        return CreateMechanicalDrive.STIRLING_ENGINE_OUTPUT
                .get()
                .defaultBlockState()
                .setValue(StirlingEngineOutputBlock.FACING, ENGINE_FACING)
                .setValue(StirlingEngineOutputBlock.AXIS, ENGINE_FACING.getAxis());
    }

    private static BlockState shaftState(Direction.Axis axis) {
        return AllBlocks.SHAFT
                .getDefaultState()
                .setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState poweredShaftState(Direction.Axis axis) {
        return CreateMechanicalDrive.STIRLING_ENGINE_POWERED_SHAFT
                .get()
                .defaultBlockState()
                .setValue(StirlingEnginePoweredShaftBlock.AXIS, axis);
    }

    private static BlockState flywheelState(Direction.Axis axis) {
        return CreateMechanicalDrive.STIRLING_ENGINE_FLYWHEEL
                .get()
                .defaultBlockState()
                .setValue(StirlingEngineFlywheelBlock.AXIS, axis);
    }

    private record EnginePositions(
            BlockPos burner,
            BlockPos heater,
            BlockPos core,
            BlockPos output,
            BlockPos shaft,
            BlockPos flywheel
    ) {
    }
}
