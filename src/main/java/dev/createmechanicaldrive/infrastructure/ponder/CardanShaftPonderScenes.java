package dev.createmechanicaldrive.infrastructure.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.cardan_shaft.CardanJointBlock;
import dev.createmechanicaldrive.content.cardan_shaft.CardanJointBlockEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.ASSEMBLY_SCHEMATIC;
import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.clearWorkspace;
import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.loc;

final class CardanShaftPonderScenes {
    private CardanShaftPonderScenes() {
    }

    static void register(
            PonderSceneRegistrationHelper<ResourceLocation> helper
    ) {
        helper.forComponents(
                        loc("cardan_shaft")
                )
                .addStoryBoard(
                        ASSEMBLY_SCHEMATIC,
                        CardanShaftPonderScenes::placement
                )
                .addStoryBoard(
                        ASSEMBLY_SCHEMATIC,
                        CardanShaftPonderScenes::sableConnections
                )
                .addStoryBoard(
                        ASSEMBLY_SCHEMATIC,
                        CardanShaftPonderScenes::breaking
                )
                .addStoryBoard(
                        ASSEMBLY_SCHEMATIC,
                        CardanShaftPonderScenes::bendBreaking
                );
    }

    private static void placement(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "cardan_shaft_placement",
                "Installing a Cardan Shaft"
        );
        scene.configureBasePlate(
                0,
                0,
                5
        );
        scene.scaleSceneView(
                0.78F
        );
        clearWorkspace(
                scene,
                util
        );
        scene.showBasePlate();

        BlockPos first =
                util.grid().at(
                        1,
                        2,
                        2
                );
        BlockPos second =
                util.grid().at(
                        3,
                        2,
                        2
                );
        BlockPos firstSupport =
                first.west();
        BlockPos secondSupport =
                second.east();

        placeCardanEnd(
                scene,
                firstSupport,
                first,
                Direction.EAST,
                Blocks.GRAY_CONCRETE.defaultBlockState()
        );
        placeCardanEnd(
                scene,
                secondSupport,
                second,
                Direction.WEST,
                Blocks.GRAY_CONCRETE.defaultBlockState()
        );

        scene.world().showSection(
                util.select().position(firstSupport),
                Direction.DOWN
        );
        scene.world().showSection(
                util.select().position(secondSupport),
                Direction.DOWN
        );
        scene.idle(
                15
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "A Cardan Shaft connects two separate kinetic networks, even when their shafts do not line up perfectly."
                )
                .pointAt(
                        util.vector().centerOf(firstSupport)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                85
        );

        scene.overlay().showControls(
                        util.vector().centerOf(firstSupport)
                                .add(0.5D, 0.0D, 0.0D),
                        Pointing.RIGHT,
                        45
                )
                .rightClick()
                .withItem(
                        cardanStack()
                );
        scene.world().showSection(
                util.select().position(first),
                Direction.DOWN
        );
        scene.world().modifyBlockEntity(
                first,
                CardanJointBlockEntity.class,
                joint -> joint.setPending(true)
        );
        scene.idle(
                50
        );
        scene.overlay().showText(
                        75
                )
                .text(
                        "Hold the Cardan Shaft and use it on the first mounting face. This places the first joint and starts the preview."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(first)
                )
                .placeNearTarget();
        scene.idle(
                85
        );

        scene.overlay().showControls(
                        util.vector().centerOf(secondSupport)
                                .add(-0.5D, 0.0D, 0.0D),
                        Pointing.LEFT,
                        45
                )
                .rightClick()
                .withItem(
                        cardanStack()
                );
        scene.world().showSection(
                util.select().position(second),
                Direction.DOWN
        );
        linkCardan(
                scene,
                first,
                second
        );
        scene.effects().indicateSuccess(first);
        scene.effects().indicateSuccess(second);
        scene.idle(
                50
        );
        scene.overlay().showText(
                        80
                )
                .text(
                        "Use it on the second mounting face to finish the link. One item creates both joints and is consumed only when the connection is completed."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(second)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                90
        );

        scene.world().setKineticSpeed(
                util.select().position(first),
                32.0F
        );
        scene.world().setKineticSpeed(
                util.select().position(second),
                32.0F
        );
        scene.effects().rotationSpeedIndicator(first);
        scene.effects().rotationSpeedIndicator(second);
        scene.overlay().showText(
                        85
                )
                .text(
                        "Rotation is transferred between the two joints at the same speed. The articulated ends allow the shaft to work at an angle."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(first)
                                .lerp(
                                        util.vector().centerOf(second),
                                        0.5D
                                )
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                95
        );

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(first, second),
                        85
                )
                .text(
                        "During installation, the complete shaft including both joints must be 2 to 6 blocks long, and the bend at either end must not exceed 80 degrees."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(second)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                95
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "While only the first point is selected, perform the Attack action to cancel placement. This respects the player's configured key binding."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(first)
                )
                .placeNearTarget();
        scene.idle(
                85
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "Sneak-use a Wrench on either joint to remove the complete Cardan Shaft and return its item."
                )
                .pointAt(
                        util.vector().centerOf(second)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                85
        );
    }

    private static void sableConnections(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "cardan_shaft_sable",
                "Connecting Moving Sable Sublevels"
        );
        scene.configureBasePlate(
                0,
                0,
                5
        );
        scene.scaleSceneView(
                0.68F
        );
        clearWorkspace(
                scene,
                util
        );
        scene.showBasePlate();

        BlockPos firstSublevel =
                util.grid().at(1, 2, 1);
        BlockPos secondSublevel =
                util.grid().at(3, 2, 1);
        BlockPos worldJoint =
                util.grid().at(1, 2, 3);
        BlockPos thirdSublevel =
                util.grid().at(3, 2, 3);

        placeCardanEnd(
                scene,
                firstSublevel.west(),
                firstSublevel,
                Direction.EAST,
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState()
        );
        placeCardanEnd(
                scene,
                secondSublevel.east(),
                secondSublevel,
                Direction.WEST,
                Blocks.ORANGE_CONCRETE.defaultBlockState()
        );
        placeCardanEnd(
                scene,
                worldJoint.west(),
                worldJoint,
                Direction.EAST,
                Blocks.GRAY_CONCRETE.defaultBlockState()
        );
        placeCardanEnd(
                scene,
                thirdSublevel.east(),
                thirdSublevel,
                Direction.WEST,
                Blocks.LIME_CONCRETE.defaultBlockState()
        );

        linkCardan(
                scene,
                firstSublevel,
                secondSublevel
        );
        linkCardan(
                scene,
                worldJoint,
                thirdSublevel
        );

        var firstAssembly =
                scene.world().showIndependentSection(
                        util.select().fromTo(
                                firstSublevel.west(),
                                firstSublevel
                        ),
                        Direction.DOWN
                );
        var secondAssembly =
                scene.world().showIndependentSection(
                        util.select().fromTo(
                                secondSublevel,
                                secondSublevel.east()
                        ),
                        Direction.DOWN
                );
        scene.world().showSection(
                util.select().fromTo(
                        worldJoint.west(),
                        worldJoint
                ),
                Direction.DOWN
        );
        var thirdAssembly =
                scene.world().showIndependentSection(
                        util.select().fromTo(
                                thirdSublevel,
                                thirdSublevel.east()
                        ),
                        Direction.DOWN
                );
        scene.idle(
                20
        );

        scene.overlay().showText(
                        80
                )
                .text(
                        "A Cardan Shaft may connect two different Sable sublevels. Each joint belongs to the sublevel carrying its mounting block."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(firstSublevel)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                90
        );

        Vec3 firstOffset =
                util.vector().of(0.0D, 0.0D, 0.35D);
        Vec3 secondOffset =
                util.vector().of(0.0D, 0.0D, -0.35D);
        movePonderEnd(
                scene,
                firstSublevel,
                firstOffset,
                30
        );
        scene.world().moveSection(
                firstAssembly,
                firstOffset,
                30
        );
        movePonderEnd(
                scene,
                secondSublevel,
                secondOffset,
                30
        );
        scene.world().moveSection(
                secondAssembly,
                secondOffset,
                30
        );
        scene.world().setKineticSpeed(
                util.select().position(firstSublevel),
                32.0F
        );
        scene.world().setKineticSpeed(
                util.select().position(secondSublevel),
                32.0F
        );
        scene.idle(
                35
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        "As the sublevels move, the joints follow them and the complete shaft is rebuilt between their live positions while continuing to transmit rotation."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(firstSublevel)
                                .lerp(
                                        util.vector().centerOf(secondSublevel),
                                        0.5D
                                )
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                95
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "The same placement process can connect a moving Sable sublevel to a joint anchored in the normal world."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(worldJoint)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                85
        );

        Vec3 thirdOffset =
                util.vector().of(0.0D, 0.45D, -0.25D);
        movePonderEnd(
                scene,
                thirdSublevel,
                thirdOffset,
                30
        );
        scene.world().moveSection(
                thirdAssembly,
                thirdOffset,
                30
        );
        scene.world().setKineticSpeed(
                util.select().position(worldJoint),
                32.0F
        );
        scene.world().setKineticSpeed(
                util.select().position(thirdSublevel),
                32.0F
        );
        scene.idle(
                35
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        "Between different physical bodies, the Cardan Shaft is also an elastic constraint: it starts pulling them together when the joint centers separate beyond 4 blocks."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(thirdSublevel)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                95
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        "This physical pull resists separation between two sublevels, or between a sublevel and the world, instead of breaking immediately at the placement limit."
                )
                .pointAt(
                        util.vector().centerOf(worldJoint)
                                .lerp(
                                        util.vector().centerOf(thirdSublevel),
                                        0.5D
                                )
                )
                .placeNearTarget();
        scene.idle(
                95
        );

        scene.overlay().showText(
                        80
                )
                .text(
                        "Only enough external force to overpower the constraint and reach the breaking geometry can tear the connection apart."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(secondSublevel)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                90
        );
    }

    private static void breaking(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "cardan_shaft_breaking",
                "Cardan Shaft Limits and Breakage"
        );
        scene.configureBasePlate(
                0,
                0,
                5
        );
        scene.scaleSceneView(
                0.58F
        );
        clearWorkspace(
                scene,
                util
        );
        scene.showBasePlate();

        BlockPos fixedJoint =
                util.grid().at(1, 2, 2);
        BlockPos movingJoint =
                util.grid().at(3, 2, 2);

        placeCardanEnd(
                scene,
                fixedJoint.west(),
                fixedJoint,
                Direction.EAST,
                Blocks.GRAY_CONCRETE.defaultBlockState()
        );
        placeCardanEnd(
                scene,
                movingJoint.east(),
                movingJoint,
                Direction.WEST,
                Blocks.ORANGE_CONCRETE.defaultBlockState()
        );
        linkCardan(
                scene,
                fixedJoint,
                movingJoint
        );

        scene.world().showSection(
                util.select().fromTo(
                        fixedJoint.west(),
                        fixedJoint
                ),
                Direction.DOWN
        );
        var movingAssembly =
                scene.world().showIndependentSection(
                        util.select().fromTo(
                                movingJoint,
                                movingJoint.east()
                        ),
                        Direction.DOWN
                );
        scene.idle(
                20
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        "The 5-block placement limit leaves a small physical safety margin. A completed cross-sublevel link can stretch as far as 5.5 blocks before it breaks."
                )
                .pointAt(
                        util.vector().centerOf(fixedJoint)
                                .lerp(
                                        util.vector().centerOf(movingJoint),
                                        0.5D
                                )
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                95
        );

        double horizontalDistance =
                movingJoint.getX()
                        - fixedJoint.getX();
        double warningDistance =
                5.4D;
        double warningHeight =
                Math.sqrt(
                        warningDistance * warningDistance
                                - horizontalDistance * horizontalDistance
                );
        Vec3 warningOffset =
                util.vector().of(
                        0.0D,
                        warningHeight,
                        0.0D
                );
        movePonderEnd(
                scene,
                movingJoint,
                warningOffset,
                40
        );
        scene.world().moveSection(
                movingAssembly,
                warningOffset,
                40
        );
        animateBreakWarning(
                scene,
                fixedJoint,
                movingJoint,
                0.0F,
                0.88F,
                40
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        "As separation approaches the breaking distance, the entire Cardan Shaft gradually turns red. This is the warning to reduce the load."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(movingJoint)
                                .add(warningOffset)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                95
        );

        double breakingDistance =
                5.6D;
        double breakingHeight =
                Math.sqrt(
                        breakingDistance * breakingDistance
                                - horizontalDistance * horizontalDistance
                );
        Vec3 breakingDelta =
                util.vector().of(
                        0.0D,
                        breakingHeight - warningHeight,
                        0.0D
                );
        Vec3 breakingOffset =
                warningOffset.add(
                        breakingDelta
                );
        movePonderEnd(
                scene,
                movingJoint,
                breakingOffset,
                15
        );
        scene.world().moveSection(
                movingAssembly,
                breakingDelta,
                15
        );
        animateBreakWarning(
                scene,
                fixedJoint,
                movingJoint,
                0.88F,
                1.0F,
                15
        );
        scene.idle(
                5
        );

        scene.world().destroyBlock(
                fixedJoint
        );
        scene.world().hideIndependentSection(
                movingAssembly,
                Direction.UP
        );
        scene.world().createItemEntity(
                util.vector().centerOf(fixedJoint)
                        .add(1.5D, 0.25D, 0.0D),
                util.vector().of(0.0D, 0.12D, 0.0D),
                cardanStack()
        );
        scene.overlay().showText(
                        80
                )
                .text(
                        "If sufficient force stretches the centers beyond 5.5 blocks, both joints break and the Cardan Shaft item is returned."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(fixedJoint)
                                .add(1.5D, 0.0D, 0.0D)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                90
        );

        scene.overlay().showText(
                        90
                )
                .text(
                        "The link also breaks if either end bends past 80 degrees, the centers are forced closer than 1 block, or either joint is removed."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(fixedJoint)
                )
                .placeNearTarget();
        scene.idle(
                100
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        "In short: red means the elastic constraint is close to losing the link; excessive distance or bend causes the actual break."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(fixedJoint)
                                .add(1.5D, 0.0D, 0.0D)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                95
        );
    }

    private static void bendBreaking(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "cardan_shaft_bend_breaking",
                "Breaking from Excessive Bend"
        );
        scene.configureBasePlate(
                0,
                0,
                5
        );
        scene.scaleSceneView(
                0.72F
        );
        clearWorkspace(
                scene,
                util
        );
        scene.showBasePlate();

        BlockPos fixedJoint =
                util.grid().at(1, 2, 2);
        BlockPos rotatingJoint =
                util.grid().at(3, 2, 2);

        placeCardanEnd(
                scene,
                fixedJoint.west(),
                fixedJoint,
                Direction.EAST,
                Blocks.GRAY_CONCRETE.defaultBlockState()
        );
        placeCardanEnd(
                scene,
                rotatingJoint.east(),
                rotatingJoint,
                Direction.WEST,
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState()
        );
        linkCardan(
                scene,
                fixedJoint,
                rotatingJoint
        );

        scene.world().showSection(
                util.select().fromTo(
                        fixedJoint.west(),
                        fixedJoint
                ),
                Direction.DOWN
        );
        var rotatingAssembly =
                scene.world().showIndependentSection(
                        util.select().fromTo(
                                rotatingJoint,
                                rotatingJoint.east()
                        ),
                        Direction.DOWN
                );
        scene.world().configureCenterOfRotation(
                rotatingAssembly,
                util.vector().centerOf(rotatingJoint)
        );
        scene.idle(
                20
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        "Distance is not the only limit. At each joint, the mounting direction may bend no more than 80 degrees away from the line toward the other end."
                )
                .pointAt(
                        util.vector().centerOf(rotatingJoint)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                95
        );

        rotatePonderEnd(
                scene,
                rotatingJoint,
                75.0F,
                40
        );
        scene.world().rotateSection(
                rotatingAssembly,
                0.0D,
                75.0D,
                0.0D,
                40
        );
        scene.idle(
                45
        );

        scene.overlay().showText(
                        80
                )
                .text(
                        "The moving sublevel has rotated this joint by 75 degrees. The shaft follows the live orientation, and the connection remains valid."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(rotatingJoint)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                90
        );

        scene.overlay().showText(
                        30
                )
                .text(
                        "If the sublevel keeps rotating and either joint crosses 80 degrees, the Cardan geometry becomes invalid."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(rotatingJoint)
                )
                .placeNearTarget();
        rotatePonderEnd(
                scene,
                rotatingJoint,
                85.0F,
                25
        );
        scene.world().rotateSection(
                rotatingAssembly,
                0.0D,
                10.0D,
                0.0D,
                25
        );
        scene.idle(
                30
        );

        scene.world().destroyBlock(
                fixedJoint
        );
        scene.world().hideIndependentSection(
                rotatingAssembly,
                Direction.UP
        );
        scene.world().createItemEntity(
                util.vector().centerOf(fixedJoint)
                        .add(1.0D, 0.25D, 0.0D),
                util.vector().of(0.0D, 0.12D, 0.0D),
                cardanStack()
        );
        scene.overlay().showText(
                        85
                )
                .text(
                        "Past the angular limit, both joints break and the Cardan Shaft item is returned, just as with excessive stretching."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(fixedJoint)
                                .add(1.0D, 0.0D, 0.0D)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                95
        );
    }

    private static void placeCardanEnd(
            CreateSceneBuilder scene,
            BlockPos support,
            BlockPos joint,
            Direction facing,
            BlockState supportState
    ) {
        scene.world().setBlock(
                support,
                supportState,
                false
        );
        scene.world().setBlock(
                joint,
                CreateMechanicalDrive.CARDAN_JOINT
                        .get()
                        .defaultBlockState()
                        .setValue(
                                CardanJointBlock.FACING,
                                facing
                        ),
                false
        );
    }

    private static void linkCardan(
            CreateSceneBuilder scene,
            BlockPos first,
            BlockPos second
    ) {
        scene.world().modifyBlockEntity(
                first,
                CardanJointBlockEntity.class,
                firstJoint -> {
                    if (firstJoint.getLevel() != null
                            && firstJoint.getLevel()
                            .getBlockEntity(second)
                            instanceof CardanJointBlockEntity secondJoint) {
                        firstJoint.createMutualLink(
                                secondJoint
                        );
                    }
                }
        );
    }

    private static void movePonderEnd(
            CreateSceneBuilder scene,
            BlockPos joint,
            Vec3 targetOffset,
            int duration
    ) {
        scene.world().modifyBlockEntity(
                joint,
                CardanJointBlockEntity.class,
                blockEntity -> blockEntity.setPonderRenderOffset(
                        targetOffset,
                        duration
                )
        );
    }

    private static void rotatePonderEnd(
            CreateSceneBuilder scene,
            BlockPos joint,
            float targetYaw,
            int duration
    ) {
        scene.world().modifyBlockEntity(
                joint,
                CardanJointBlockEntity.class,
                blockEntity -> blockEntity.setPonderRenderYaw(
                        targetYaw,
                        duration
                )
        );
    }

    private static void animateBreakWarning(
            CreateSceneBuilder scene,
            BlockPos first,
            BlockPos second,
            float start,
            float end,
            int duration
    ) {
        for (int tick = 1; tick <= duration; tick++) {
            float progress =
                    (float) tick
                            / duration;
            float warning =
                    start
                            + (end - start)
                            * progress;

            setPonderBreakWarning(
                    scene,
                    first,
                    warning
            );
            setPonderBreakWarning(
                    scene,
                    second,
                    warning
            );
            scene.idle(
                    1
            );
        }
    }

    private static void setPonderBreakWarning(
            CreateSceneBuilder scene,
            BlockPos joint,
            float progress
    ) {
        scene.world().modifyBlockEntity(
                joint,
                CardanJointBlockEntity.class,
                blockEntity -> blockEntity.setPonderBreakWarningProgress(
                        progress
                )
        );
    }

    private static ItemStack cardanStack() {
        return new ItemStack(
                CreateMechanicalDrive.CARDAN_SHAFT_ITEM.get()
        );
    }
}
