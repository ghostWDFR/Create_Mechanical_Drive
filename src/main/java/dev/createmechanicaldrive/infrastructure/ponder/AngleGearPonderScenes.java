package dev.createmechanicaldrive.infrastructure.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.angle_gear.AngleGearBlock;
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
import dev.createmechanicaldrive.content.angle_gear.AngleGearBlockEntity;

import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.loc;

final class AngleGearPonderScenes {
    private static final ResourceLocation SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath(
                    "create",
                    "chain_drive/relay"
            );

    private AngleGearPonderScenes() {
    }

    static void register(
            PonderSceneRegistrationHelper<ResourceLocation> helper
    ) {
        helper.forComponents(loc("angle_gear"))
                .addStoryBoard(
                        SCHEMATIC,
                        AngleGearPonderScenes::operation
                );
    }

    private static void operation(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "angle_gear_operation",
                "Using Angle Gears"
        );
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.82F);
        scene.world().setBlocks(
                util.select().fromTo(0, 1, 0, 5, 4, 4),
                Blocks.AIR.defaultBlockState(),
                false
        );
        scene.showBasePlate();

        BlockPos gear =
                util.grid().at(2, 1, 2);
        BlockPos westShaft =
                gear.west();
        BlockPos eastShaft =
                gear.east();
        BlockPos northShaft =
                gear.north();
        BlockPos upShaft =
                gear.above();
        BlockPos invalidPreview =
                util.grid().at(4, 1, 2);

        scene.world().setBlock(
                westShaft,
                shaft(Direction.Axis.X),
                false
        );
        scene.world().showSection(
                util.select().position(westShaft),
                Direction.DOWN
        );
        scene.idle(15);

        scene.overlay().showControls(
                        util.vector().blockSurface(
                                westShaft,
                                Direction.EAST
                        ),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(angleGearStack());
        scene.idle(20);

        scene.world().setBlock(
                gear,
                angleGear(Direction.WEST),
                false
        );

        scene.world().modifyBlockEntity(
                gear,
                AngleGearBlockEntity.class,
                blockEntity ->
                        blockEntity.setPonderSourceFacing(
                                Direction.WEST
                        )
        );

        scene.world().showSection(
                util.select().position(gear),
                Direction.DOWN
        );
        scene.effects().indicateSuccess(gear);
        scene.overlay().showText(75)
                .text(
                        "Angle Gears are placed on the side you click, making them line up with nearby shafts."
                )
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().centerOf(gear))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);

        scene.overlay().showControls(
                        util.vector().blockSurface(
                                gear,
                                Direction.EAST
                        ),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(angleGearStack());
        scene.idle(20);

        scene.world().modifyBlock(
                gear,
                state -> withGear(state, Direction.EAST),
                false
        );
        scene.world().setBlock(
                eastShaft,
                shaft(Direction.Axis.X),
                false
        );
        scene.world().showSection(
                util.select().position(eastShaft),
                Direction.DOWN
        );
        scene.effects().indicateSuccess(gear);
        scene.overlay().showOutlineWithText(
                        util.select().fromTo(westShaft, eastShaft),
                        75
                )
                .text(
                        "Click another side of the same block to add another gear inside it."
                )
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(gear))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);

        Selection westSide =
                util.select().position(westShaft);

        Selection eastSide =
                util.select().position(eastShaft);

        Selection gearSelection =
                util.select().position(gear);

        scene.world().setKineticSpeed(
                westSide,
                48.0F
        );

        scene.world().setKineticSpeed(
                gearSelection,
                48.0F
        );

        scene.world().setKineticSpeed(
                eastSide,
                0.0F
        );

        scene.effects().rotationSpeedIndicator(
                westShaft
        );

        scene.effects().rotationSpeedIndicator(
                eastShaft
        );

        scene.overlay().showText(85)
                .text(
                        "Opposite Angle Gears on the same axis remain independent. Rotation does not pass straight through between them."
                )
                .colored(PonderPalette.INPUT)
                .pointAt(
                        util.vector().centerOf(gear)
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(95);

        scene.overlay().showControls(
                        util.vector().blockSurface(
                                gear,
                                Direction.NORTH
                        ),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(angleGearStack());
        scene.idle(20);

        scene.world().modifyBlock(
                gear,
                state -> withGear(state, Direction.NORTH),
                false
        );
        scene.world().setBlock(
                northShaft,
                shaft(Direction.Axis.Z),
                false
        );
        scene.world().showSection(
                util.select().position(northShaft),
                Direction.SOUTH
        );
        scene.world().setKineticSpeed(
                util.select().position(westShaft),
                48.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(gear),
                48.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(eastShaft),
                -48.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(northShaft),
                -48.0F
        );
        scene.effects().indicateSuccess(gear);
        scene.effects().rotationDirectionIndicator(northShaft);
        scene.overlay().showText(85)
                .text(
                        "A perpendicular Angle Gear connects the transmission across the corner. With this bridge present, both opposite shafts become connected through it."
                )
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(northShaft))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);

        scene.overlay().showControls(
                        util.vector().topOf(gear),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(angleGearStack());
        scene.idle(15);
        scene.effects().indicateRedstone(gear);
        scene.overlay().showText(85)
                .text(
                        "A single Angle Gear block can hold up to four sides, but it cannot connect all three axes at once."
                )
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(gear))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);

        scene.world().modifyBlock(
                gear,
                state -> withGear(
                        state.setValue(AngleGearBlock.NORTH, false),
                        Direction.UP
                ),
                false
        );
        scene.world().setBlock(
                northShaft,
                Blocks.AIR.defaultBlockState(),
                false
        );
        scene.world().setBlock(
                upShaft,
                shaft(Direction.Axis.Y),
                false
        );
        scene.world().showSection(
                util.select().position(upShaft),
                Direction.DOWN
        );
        scene.world().setKineticSpeed(
                util.select().position(westShaft),
                48.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(gear),
                48.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(eastShaft),
                -48.0F
        );

        scene.world().setKineticSpeed(
                util.select().position(upShaft),
                48.0F
        );
        scene.effects().rotationDirectionIndicator(upShaft);
        scene.overlay().showText(80)
                .text(
                        "For vertical builds, click the top or bottom face to add the upward or downward side."
                )
                .pointAt(util.vector().centerOf(upShaft))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showControls(
                        util.vector().blockSurface(
                                gear,
                                Direction.SOUTH
                        ),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(
                        new ItemStack(
                                AllBlocks.ANDESITE_CASING.get()
                        )
                );
        scene.idle(20);

        scene.world().modifyBlock(
                gear,
                state -> state.setValue(
                        AngleGearBlock.ENCASED,
                        true
                ),
                false
        );
        scene.effects().indicateSuccess(gear);
        scene.overlay().showText(85)
                .text(
                        "Right-click with Andesite Casing to cover the assembly while keeping every installed side usable."
                )
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(gear))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);

        scene.overlay().showControls(
                        util.vector().centerOf(gear),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .whileSneaking()
                .withItem(
                        new ItemStack(
                                AllItems.WRENCH.get()
                        )
                );
        scene.idle(20);
        scene.world().modifyBlock(
                gear,
                state -> state.setValue(
                        AngleGearBlock.ENCASED,
                        false
                ),
                false
        );
        scene.effects().indicateSuccess(gear);
        scene.overlay().showText(75)
                .text(
                        "Sneak-right-click with a Wrench removes the casing first."
                )
                .pointAt(util.vector().centerOf(gear))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);

        scene.overlay().showControls(
                        util.vector().blockSurface(
                                gear,
                                Direction.EAST
                        ),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .whileSneaking()
                .withItem(
                        new ItemStack(
                                AllItems.WRENCH.get()
                        )
                );
        scene.idle(20);
        scene.world().modifyBlock(
                gear,
                state -> state.setValue(
                        AngleGearBlock.EAST,
                        false
                ),
                false
        );

        scene.world().setKineticSpeed(
                util.select().position(eastShaft),
                0.0F
        );
        scene.effects().indicateSuccess(gear);
        scene.overlay().showText(85)
                .text(
                        "Without casing, the Wrench removes only the side you are pointing at and returns one Angle Gear."
                )
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().centerOf(gear))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);

        scene.world().setBlock(
                invalidPreview,
                angleGear(Direction.SOUTH, Direction.EAST, Direction.UP),
                false
        );
        scene.world().showSection(
                util.select().position(invalidPreview),
                Direction.DOWN
        );
        scene.effects().indicateRedstone(invalidPreview);
        scene.overlay().showText(80)
                .text(
                        "If a new side would make an impossible shape, the click is ignored and nothing is consumed."
                )
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(invalidPreview))
                .placeNearTarget();
        scene.idle(90);
    }

    private static ItemStack angleGearStack() {
        return new ItemStack(
                CreateMechanicalDrive.ANGLE_GEAR_ITEM.get()
        );
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

    private static BlockState angleGear(
            Direction... sides
    ) {
        BlockState state =
                CreateMechanicalDrive.ANGLE_GEAR
                        .get()
                        .defaultBlockState();

        if (sides.length > 0) {
            state = state.setValue(
                    AngleGearBlock.AXIS,
                    sides[0].getAxis()
            );
        }

        for (Direction side : sides) {
            state = withGear(state, side);
        }

        return state;
    }

    private static BlockState withGear(
            BlockState state,
            Direction side
    ) {
        return switch (side) {
            case NORTH -> state.setValue(AngleGearBlock.NORTH, true);
            case SOUTH -> state.setValue(AngleGearBlock.SOUTH, true);
            case WEST -> state.setValue(AngleGearBlock.WEST, true);
            case EAST -> state.setValue(AngleGearBlock.EAST, true);
            case DOWN -> state.setValue(AngleGearBlock.DOWN, true);
            case UP -> state.setValue(AngleGearBlock.UP, true);
        };
    }
}
