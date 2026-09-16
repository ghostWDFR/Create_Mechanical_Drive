package dev.createmechanicaldrive.infrastructure.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.seats.SeatBackPosition;
import dev.createmechanicaldrive.content.seats.SeatBlock;
import dev.createmechanicaldrive.content.seats.SeatBlockEntity;
import dev.createmechanicaldrive.content.seats.SeatColor;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.SEAT_SCHEMATIC;
import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.clearWorkspace;
import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.loc;

final class SeatPonderScenes {
    private SeatPonderScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                        loc("seat")
                )
                .addStoryBoard(
                        SEAT_SCHEMATIC,
                        SeatPonderScenes::seatAdjustments
                );
    }

    private static void seatAdjustments(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "seat_adjustments",
                "Adjusting Seats"
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

        BlockPos seat =
                util.grid().at(
                        2,
                        1,
                        2
                );

        BlockState seatState =
                CreateMechanicalDrive
                        .SEAT
                        .get()
                        .defaultBlockState()
                        .setValue(
                                SeatBlock.FACING,
                                Direction.SOUTH
                        )
                        .setValue(
                                SeatBlock.BACK_POSITION,
                                SeatBackPosition.UPRIGHT
                        )
                        .setValue(
                                SeatBlock.COLOR,
                                SeatColor.BLACK
                        );

        scene.world().setBlock(
                seat,
                seatState,
                false
        );

        scene.world().showSection(
                util.select().position(
                        seat
                ),
                Direction.DOWN
        );

        scene.idle(
                20
        );

        scene.overlay().showText(
                        70
                )
                .text(
                        "Seats can be used as compact passenger seats for your contraptions."
                )
                .pointAt(
                        util.vector().centerOf(
                                seat
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                80
        );

        scene.overlay().showControls(
                        util.vector().blockSurface(
                                seat,
                                Direction.NORTH
                        ),
                        Pointing.DOWN,
                        45
                )
                .rightClick();

        scene.overlay().showText(
                        80
                )
                .text(
                        "Sneak with empty hands, then hold right-click and drag to adjust the backrest."
                )
                .pointAt(
                        util.vector().centerOf(
                                seat
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                85
        );

        setBackPosition(
                scene,
                seat,
                SeatBackPosition.BACKWARD
        );

        scene.overlay().showText(
                        65
                )
                .text(
                        "Pull it back for a more relaxed seating position."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(
                                seat
                        )
                )
                .placeNearTarget();

        scene.idle(
                75
        );

        setBackPosition(
                scene,
                seat,
                SeatBackPosition.UPRIGHT
        );

        scene.overlay().showText(
                        65
                )
                .text(
                        "Move it to the middle to return the backrest upright."
                )
                .pointAt(
                        util.vector().centerOf(
                                seat
                        )
                )
                .placeNearTarget();

        scene.idle(
                75
        );

        setBackPosition(
                scene,
                seat,
                SeatBackPosition.FORWARD
        );

        scene.overlay().showText(
                        80
                )
                .text(
                        "Fold the backrest forward when you want a low profile. You cannot sit while it is folded down."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(
                                seat
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                90
        );

        setBackPosition(
                scene,
                seat,
                SeatBackPosition.UPRIGHT
        );

        scene.overlay().showControls(
                        util.vector().blockSurface(
                                seat,
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

        SeatColor[] colors =
                new SeatColor[]{
                        SeatColor.BLACK,
                        SeatColor.BLUE,
                        SeatColor.GRAY,
                        SeatColor.GREEN,
                        SeatColor.RED,
                        SeatColor.YELLOW
                };

        for (SeatColor color : colors) {
            scene.world().modifyBlock(
                    seat,
                    state ->
                            state.setValue(
                                    SeatBlock.COLOR,
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
                        "Apply dyes directly to recolor the seat: black, blue, gray, green, red, or yellow."
                )
                .pointAt(
                        util.vector().centerOf(
                                seat
                        )
                )
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(
                85
        );
    }

    private static void setBackPosition(
            CreateSceneBuilder scene,
            BlockPos seat,
            SeatBackPosition position
    ) {
        scene.world().modifyBlock(
                seat,
                state ->
                        state.setValue(
                                SeatBlock.BACK_POSITION,
                                position
                        ),
                false
        );

        scene.world().modifyBlockEntity(
                seat,
                SeatBlockEntity.class,
                blockEntity -> {
                    blockEntity.setBackPosition(
                            position
                    );

                    blockEntity.setReclineTarget(
                            position.recline()
                    );
                }
        );
    }
}
