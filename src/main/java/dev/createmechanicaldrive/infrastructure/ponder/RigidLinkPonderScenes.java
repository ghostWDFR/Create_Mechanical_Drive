package dev.createmechanicaldrive.infrastructure.ponder;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.rigid_link.RigidLinkJointBlock;
import dev.createmechanicaldrive.content.rigid_link.RigidLinkJointBlockEntity;
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

final class RigidLinkPonderScenes {
    private RigidLinkPonderScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(loc("rigid_link"))
                .addStoryBoard(
                        ASSEMBLY_SCHEMATIC,
                        RigidLinkPonderScenes::freeLink
                )
                .addStoryBoard(
                        ASSEMBLY_SCHEMATIC,
                        RigidLinkPonderScenes::freeSableConnections
                )
                .addStoryBoard(
                        ASSEMBLY_SCHEMATIC,
                        RigidLinkPonderScenes::freeBreaking
                )
                .addStoryBoard(
                        ASSEMBLY_SCHEMATIC,
                        RigidLinkPonderScenes::freeAngleBreaking
                );
        helper.forComponents(loc("rigid_link_limited"))
                .addStoryBoard(
                        ASSEMBLY_SCHEMATIC,
                        RigidLinkPonderScenes::limitedLink
                )
                .addStoryBoard(
                        ASSEMBLY_SCHEMATIC,
                        RigidLinkPonderScenes::limitedSableConnections
                )
                .addStoryBoard(
                        ASSEMBLY_SCHEMATIC,
                        RigidLinkPonderScenes::limitedBreaking
                )
                .addStoryBoard(
                        ASSEMBLY_SCHEMATIC,
                        RigidLinkPonderScenes::limitedAxisBreaking
                );
    }

    private static void freeLink(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title(
                "rigid_link_free",
                "Using a Free Rigid Link"
        );
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.78F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        BlockPos first = util.grid().at(1, 2, 2);
        BlockPos second = util.grid().at(4, 2, 2);
        BlockPos firstSupport = first.west();
        BlockPos secondSupport = second.east();

        placeJoint(
                scene,
                firstSupport,
                first,
                Direction.EAST,
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState()
        );
        placeJoint(
                scene,
                secondSupport,
                second,
                Direction.WEST,
                Blocks.ORANGE_CONCRETE.defaultBlockState()
        );

        scene.world().showSection(
                util.select().position(firstSupport),
                Direction.DOWN
        );
        scene.world().showSection(
                util.select().position(secondSupport),
                Direction.DOWN
        );
        scene.idle(15);

        scene.overlay().showText(75)
                .text(
                        "Free Rigid Links connect mounting points on separate structures, including different Sable sublevels."
                )
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().centerOf(firstSupport))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);

        scene.overlay().showControls(
                        util.vector().blockSurface(firstSupport, Direction.EAST),
                        Pointing.RIGHT,
                        40
                )
                .rightClick()
                .withItem(jointStack());
        scene.world().showSection(
                util.select().position(first),
                Direction.DOWN
        );
        scene.idle(50);

        scene.overlay().showControls(
                        util.vector().blockSurface(secondSupport, Direction.WEST),
                        Pointing.LEFT,
                        40
                )
                .rightClick()
                .withItem(jointStack());
        scene.world().showSection(
                util.select().position(second),
                Direction.DOWN
        );
        scene.idle(50);
        scene.overlay().showText(80)
                .text(
                        "Place each Joint on a solid mounting block. A Joint cannot use another Joint as its support."
                )
                .pointAt(util.vector().centerOf(second))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showControls(
                        util.vector().centerOf(first),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(freeLinkStack());
        scene.idle(50);
        scene.overlay().showText(70)
                .text(
                        "Use the Free Rigid Link on the first Joint to select the starting point."
                )
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().centerOf(first))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(
                        util.vector().centerOf(second),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(freeLinkStack());
        scene.idle(50);
        linkFree(scene, first, second);
        scene.effects().indicateSuccess(first);
        scene.effects().indicateSuccess(second);
        scene.overlay().showText(80)
                .text(
                        "Use it on the second Joint to complete the connection. The item is consumed only after a valid link is created."
                )
                .colored(PonderPalette.GREEN)
                .pointAt(
                        util.vector().centerOf(first)
                                .lerp(util.vector().centerOf(second), 0.5D)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(first, second),
                        90
                )
                .text(
                        "The Free Link holds the distance between its Joint centers, but it does not align their axes or lock them into one plane."
                )
                .colored(PonderPalette.GREEN)
                .pointAt(
                        util.vector().centerOf(first)
                                .lerp(util.vector().centerOf(second), 0.5D)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showText(85)
                .text(
                        "The connected structures may rotate freely relative to the link. Their orientation is not constrained."
                )
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(secondSupport))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(first, second),
                        85
                )
                .text(
                        "Joint centers may be adjacent or up to 10 blocks apart. One Joint can carry up to two Free Links."
                )
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(first))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);

        scene.overlay().showControls(
                        util.vector().centerOf(second),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .whileSneaking()
                .withItem(new ItemStack(AllItems.WRENCH.get()));
        scene.idle(50);
        unlink(scene, first);
        scene.world().destroyBlock(second);
        scene.effects().indicateSuccess(first);
        scene.overlay().showText(75)
                .text(
                        "Sneak-use a Wrench on either Joint to remove it. Any attached links come loose and can be collected."
                )
                .pointAt(util.vector().centerOf(secondSupport))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);
    }

    private static void limitedLink(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title(
                "rigid_link_limited",
                "Using a Limited Rigid Link"
        );
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.78F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        BlockPos first = util.grid().at(1, 2, 2);
        BlockPos second = util.grid().at(4, 2, 2);
        BlockPos firstSupport = first.below();
        BlockPos secondSupport = second.below();

        placeJoint(
                scene,
                firstSupport,
                first,
                Direction.UP,
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState()
        );
        placeJoint(
                scene,
                secondSupport,
                second,
                Direction.UP,
                Blocks.ORANGE_CONCRETE.defaultBlockState()
        );

        scene.world().showSection(
                util.select().position(firstSupport),
                Direction.DOWN
        );
        scene.world().showSection(
                util.select().position(secondSupport),
                Direction.DOWN
        );
        scene.idle(15);

        scene.overlay().showText(80)
                .text(
                        "Limited Rigid Links connect structures with a planar hinge constraint that resists sideways twisting."
                )
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().centerOf(firstSupport))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showControls(
                        util.vector().blockSurface(firstSupport, Direction.UP),
                        Pointing.DOWN,
                        40
                )
                .rightClick()
                .withItem(jointStack());
        scene.world().showSection(
                util.select().position(first),
                Direction.DOWN
        );
        scene.idle(50);

        scene.overlay().showControls(
                        util.vector().blockSurface(secondSupport, Direction.UP),
                        Pointing.DOWN,
                        40
                )
                .rightClick()
                .withItem(jointStack());
        scene.world().showSection(
                util.select().position(second),
                Direction.DOWN
        );
        scene.idle(50);

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(first, second),
                        85
                )
                .text(
                        "Both Joint axes must be parallel, and the line between their centers must be perpendicular to those axes."
                )
                .colored(PonderPalette.RED)
                .pointAt(
                        util.vector().centerOf(first)
                                .lerp(util.vector().centerOf(second), 0.5D)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);

        scene.overlay().showControls(
                        util.vector().centerOf(first),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(limitedLinkStack());
        scene.idle(50);
        scene.overlay().showText(70)
                .text(
                        "Use the Limited Rigid Link on the first correctly oriented Joint to select it."
                )
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().centerOf(first))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(
                        util.vector().centerOf(second),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(limitedLinkStack());
        scene.idle(50);
        linkLimited(scene, first, second);
        scene.effects().indicateSuccess(first);
        scene.effects().indicateSuccess(second);
        scene.overlay().showText(80)
                .text(
                        "Use it on the second Joint to complete the Limited Link. Invalid axis or plane geometry is rejected."
                )
                .colored(PonderPalette.GREEN)
                .pointAt(
                        util.vector().centerOf(first)
                                .lerp(util.vector().centerOf(second), 0.5D)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(first, second),
                        90
                )
                .text(
                        "The link preserves its length, keeps both hinge axes parallel, and prevents the rod from leaving their shared plane."
                )
                .colored(PonderPalette.GREEN)
                .pointAt(
                        util.vector().centerOf(first)
                                .lerp(util.vector().centerOf(second), 0.5D)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showText(90)
                .text(
                        "The Limited Link is not a welded connection. Rotation around the common Joint axis remains free."
                )
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(second))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(first, second),
                        90
                )
                .text(
                        "A Joint accepts only one Limited Link and cannot mix it with Free Links. Centers may be adjacent or up to 10 blocks apart."
                )
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(first))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showControls(
                        util.vector().centerOf(second),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .whileSneaking()
                .withItem(new ItemStack(AllItems.WRENCH.get()));
        scene.idle(50);
        unlink(scene, first);
        scene.world().destroyBlock(second);
        scene.effects().indicateSuccess(first);
        scene.overlay().showText(75)
                .text(
                        "Sneak-use a Wrench on either Joint to remove it. Its Limited Link comes loose and can be collected."
                )
                .pointAt(util.vector().centerOf(secondSupport))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);
    }

    private static void freeSableConnections(SceneBuilder builder,
                                             SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("rigid_link_free_sable", "Free Links on Moving Sublevels");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.68F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        BlockPos first = util.grid().at(1, 2, 2);
        BlockPos second = util.grid().at(4, 2, 2);
        placeJoint(scene, first.west(), first, Direction.EAST,
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState());
        placeJoint(scene, second.east(), second, Direction.WEST,
                Blocks.ORANGE_CONCRETE.defaultBlockState());
        linkFree(scene, first, second);

        var firstAssembly = scene.world().showIndependentSection(
                util.select().fromTo(first.west(), first), Direction.DOWN);
        var secondAssembly = scene.world().showIndependentSection(
                util.select().fromTo(second, second.east()), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("A Free Link can connect Joints carried by two different moving Sable sublevels.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().centerOf(first))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        Vec3 firstOffset = util.vector().of(0.0D, 0.0D, 0.35D);
        Vec3 secondOffset = util.vector().of(0.0D, 0.0D, -0.35D);
        movePonderJoint(scene, first, firstOffset, 30);
        scene.world().moveSection(firstAssembly, firstOffset, 30);
        movePonderJoint(scene, second, secondOffset, 30);
        scene.world().moveSection(secondAssembly, secondOffset, 30);
        scene.idle(35);

        scene.overlay().showText(85)
                .text("As the sublevels move, the link follows the live positions of both Joints and keeps them connected.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(first)
                        .lerp(util.vector().centerOf(second), 0.5D))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);

        scene.overlay().showText(90)
                .text("The Free Link only preserves the installed distance. The two structures remain free to change their relative orientation.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(second).add(secondOffset))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showText(80)
                .text("The same link may connect a moving sublevel to a Joint anchored in the normal world.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().centerOf(first).add(firstOffset))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);
    }

    private static void freeBreaking(SceneBuilder builder,
                                     SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("rigid_link_free_breaking", "Free Link Length Limit");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.72F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        BlockPos first = util.grid().at(1, 2, 2);
        BlockPos second = util.grid().at(3, 2, 2);
        placeJoint(scene, first.west(), first, Direction.EAST,
                Blocks.GRAY_CONCRETE.defaultBlockState());
        placeJoint(scene, second.east(), second, Direction.WEST,
                Blocks.ORANGE_CONCRETE.defaultBlockState());
        linkFree(scene, first, second);
        scene.world().showSection(util.select().fromTo(first.west(), first), Direction.DOWN);
        var movingAssembly = scene.world().showIndependentSection(
                util.select().fromTo(second, second.east()), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(85)
                .text("A Free Link stores the distance between the Joint centers when it is installed.")
                .pointAt(util.vector().centerOf(first)
                        .lerp(util.vector().centerOf(second), 0.5D))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);

        Vec3 breakingOffset = util.vector().of(1.05D, 0.0D, 0.0D);
        scene.overlay().showText(65)
                .text("If that distance changes by 1 block or more, the Free Link breaks.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(second))
                .placeNearTarget();
        movePonderJoint(scene, second, breakingOffset, 35);
        scene.world().moveSection(movingAssembly, breakingOffset, 35);
        scene.idle(40);
        unlink(scene, first);
        scene.world().createItemEntity(
                util.vector().centerOf(first).add(1.5D, 0.3D, 0.0D),
                util.vector().of(0.0D, 0.12D, 0.0D),
                freeLinkStack());
        scene.idle(30);

        scene.overlay().showText(85)
                .text("When this happens, both Joints stay in place. The Free Link can be picked up and used again.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(first).add(1.5D, 0.0D, 0.0D))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);

        scene.overlay().showText(80)
                .text("If either Joint is removed, its connected links come loose and can be collected.")
                .pointAt(util.vector().centerOf(first))
                .placeNearTarget();
        scene.idle(90);
    }

    private static void freeAngleBreaking(SceneBuilder builder,
                                          SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("rigid_link_free_angle_breaking", "Free Link Mounting Limit");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.72F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        BlockPos first = util.grid().at(1, 2, 2);
        BlockPos second = util.grid().at(3, 2, 2);
        placeJoint(scene, first.west(), first, Direction.EAST,
                Blocks.GRAY_CONCRETE.defaultBlockState());
        placeJoint(scene, second.east(), second, Direction.WEST,
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState());
        linkFree(scene, first, second);
        scene.world().showSection(util.select().fromTo(first.west(), first), Direction.DOWN);
        var rotatingAssembly = scene.world().showIndependentSection(
                util.select().fromTo(second, second.east()), Direction.DOWN);
        scene.world().configureCenterOfRotation(rotatingAssembly,
                util.vector().centerOf(second));
        scene.idle(20);

        scene.overlay().showText(90)
                .text("Free rotation is allowed while the rod remains within the wide working range of both mounting faces.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(second))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showText(55)
                .text("The link breaks if the rod is forced more than 45 degrees behind either Joint's mounting face.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(second))
                .placeNearTarget();
        scene.world().rotateSection(rotatingAssembly, 0.0D, 145.0D, 0.0D, 40);
        scene.idle(45);
        unlink(scene, first);
        scene.world().createItemEntity(
                util.vector().centerOf(first).add(1.0D, 0.3D, 0.0D),
                util.vector().of(0.0D, 0.12D, 0.0D),
                freeLinkStack());
        scene.idle(25);

        scene.overlay().showText(80)
                .text("Exceeding this limit causes the connection to give way. The Free Link can be picked up and used again.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(first).add(1.0D, 0.0D, 0.0D))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);
    }

    private static void limitedSableConnections(SceneBuilder builder,
                                                SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("rigid_link_limited_sable", "Limited Links on Moving Sublevels");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.68F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        BlockPos first = util.grid().at(1, 2, 2);
        BlockPos second = util.grid().at(4, 2, 2);
        placeJoint(scene, first.below(), first, Direction.UP,
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState());
        placeJoint(scene, second.below(), second, Direction.UP,
                Blocks.ORANGE_CONCRETE.defaultBlockState());
        linkLimited(scene, first, second);
        var firstAssembly = scene.world().showIndependentSection(
                util.select().fromTo(first.below(), first), Direction.DOWN);
        var secondAssembly = scene.world().showIndependentSection(
                util.select().fromTo(second.below(), second), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(85)
                .text("A Limited Link can connect Joints carried by two different moving Sable sublevels.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().centerOf(first))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);

        Vec3 firstOffset = util.vector().of(0.0D, 0.0D, 0.35D);
        Vec3 secondOffset = util.vector().of(0.0D, 0.0D, -0.35D);
        movePonderJoint(scene, first, firstOffset, 30);
        scene.world().moveSection(firstAssembly, firstOffset, 30);
        movePonderJoint(scene, second, secondOffset, 30);
        scene.world().moveSection(secondAssembly, secondOffset, 30);
        scene.idle(35);

        scene.overlay().showText(90)
                .text("The link follows both moving Joints while they remain in their shared hinge plane.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(first)
                        .lerp(util.vector().centerOf(second), 0.5D))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showText(90)
                .text("The hinge axes must remain parallel, but rotation around their common axis stays free.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(second).add(secondOffset))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showText(80)
                .text("A Limited Link may also connect a moving sublevel to a Joint anchored in the normal world.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().centerOf(first).add(firstOffset))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);
    }

    private static void limitedBreaking(SceneBuilder builder,
                                        SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("rigid_link_limited_breaking", "Limited Link Plane Limit");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.72F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        BlockPos first = util.grid().at(1, 2, 2);
        BlockPos second = util.grid().at(3, 2, 2);
        placeJoint(scene, first.below(), first, Direction.UP,
                Blocks.GRAY_CONCRETE.defaultBlockState());
        placeJoint(scene, second.below(), second, Direction.UP,
                Blocks.ORANGE_CONCRETE.defaultBlockState());
        linkLimited(scene, first, second);
        scene.world().showSection(util.select().fromTo(first.below(), first), Direction.DOWN);
        var movingAssembly = scene.world().showIndependentSection(
                util.select().fromTo(second.below(), second), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(90)
                .text("A Limited Link works while both Joint centers stay in the plane perpendicular to their common hinge axis.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(first)
                        .lerp(util.vector().centerOf(second), 0.5D))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        Vec3 breakingOffset = util.vector().of(0.0D, 1.05D, 0.0D);
        scene.overlay().showText(65)
                .text("Moving either end 1 block out of that shared plane breaks the Limited Link.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(second))
                .placeNearTarget();
        movePonderJoint(scene, second, breakingOffset, 35);
        scene.world().moveSection(movingAssembly, breakingOffset, 35);
        scene.idle(40);
        unlink(scene, first);
        scene.world().createItemEntity(
                util.vector().centerOf(first).add(1.0D, 0.3D, 0.0D),
                util.vector().of(0.0D, 0.12D, 0.0D),
                limitedLinkStack());
        scene.idle(30);

        scene.overlay().showText(85)
                .text("Like the Free Link, it also breaks if its installed length changes by 1 block or more.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(first).add(1.0D, 0.0D, 0.0D))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);

        scene.overlay().showText(80)
                .text("After a break, both Joints stay in place. The Limited Link can be picked up and used again.")
                .pointAt(util.vector().centerOf(first))
                .placeNearTarget();
        scene.idle(90);
    }

    private static void limitedAxisBreaking(SceneBuilder builder,
                                            SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("rigid_link_limited_axis_breaking", "Limited Link Axis Limit");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.72F);
        clearWorkspace(scene, util);
        scene.showBasePlate();

        BlockPos first = util.grid().at(1, 2, 2);
        BlockPos second = util.grid().at(3, 2, 2);
        placeJoint(scene, first.below(), first, Direction.UP,
                Blocks.GRAY_CONCRETE.defaultBlockState());
        placeJoint(scene, second.below(), second, Direction.UP,
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState());
        linkLimited(scene, first, second);
        scene.world().showSection(util.select().fromTo(first.below(), first), Direction.DOWN);
        var rotatingAssembly = scene.world().showIndependentSection(
                util.select().fromTo(second.below(), second), Direction.DOWN);
        scene.world().configureCenterOfRotation(rotatingAssembly,
                util.vector().centerOf(second));
        scene.idle(20);

        scene.overlay().showText(90)
                .text("The two hinge axes may tilt slightly relative to each other while the Limited Link is working.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(second))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showText(55)
                .text("If the axes diverge by more than 45 degrees, the Limited Link breaks.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(second))
                .placeNearTarget();
        scene.world().rotateSection(rotatingAssembly, 50.0D, 0.0D, 0.0D, 35);
        scene.idle(40);
        unlink(scene, first);
        scene.world().createItemEntity(
                util.vector().centerOf(first).add(1.0D, 0.3D, 0.0D),
                util.vector().of(0.0D, 0.12D, 0.0D),
                limitedLinkStack());
        scene.idle(25);

        scene.overlay().showText(85)
                .text("If either Joint is removed, the Limited Link comes loose and can be collected.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(first).add(1.0D, 0.0D, 0.0D))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(95);
    }

    private static void placeJoint(
            CreateSceneBuilder scene,
            BlockPos support,
            BlockPos joint,
            Direction facing,
            BlockState supportState
    ) {
        scene.world().setBlock(support, supportState, false);
        scene.world().setBlock(
                joint,
                CreateMechanicalDrive.RIGID_LINK_JOINT.get()
                        .defaultBlockState()
                        .setValue(RigidLinkJointBlock.FACING, facing),
                false
        );
    }

    private static void linkFree(
            CreateSceneBuilder scene,
            BlockPos first,
            BlockPos second
    ) {
        scene.world().modifyBlockEntity(
                first,
                RigidLinkJointBlockEntity.class,
                firstJoint -> {
                    if (firstJoint.getLevel() != null
                            && firstJoint.getLevel().getBlockEntity(second)
                            instanceof RigidLinkJointBlockEntity secondJoint) {
                        firstJoint.createMutualLink(
                                secondJoint,
                                RigidLinkJointBlockEntity.LinkType.FREE
                        );
                    }
                }
        );
    }

    private static void linkLimited(
            CreateSceneBuilder scene,
            BlockPos first,
            BlockPos second
    ) {
        scene.world().modifyBlockEntity(
                first,
                RigidLinkJointBlockEntity.class,
                firstJoint -> {
                    if (firstJoint.getLevel() != null
                            && firstJoint.getLevel().getBlockEntity(second)
                            instanceof RigidLinkJointBlockEntity secondJoint) {
                        firstJoint.createMutualLink(
                                secondJoint,
                                RigidLinkJointBlockEntity.LinkType.LIMITED
                        );
                    }
                }
        );
    }

    private static void unlink(CreateSceneBuilder scene, BlockPos joint) {
        scene.world().modifyBlockEntity(
                joint,
                RigidLinkJointBlockEntity.class,
                blockEntity -> blockEntity.destroyAllLinks(false)
        );
    }

    private static void movePonderJoint(CreateSceneBuilder scene,
                                        BlockPos joint,
                                        Vec3 targetOffset,
                                        int duration) {
        scene.world().modifyBlockEntity(
                joint,
                RigidLinkJointBlockEntity.class,
                blockEntity -> blockEntity.setPonderRenderOffset(
                        targetOffset,
                        duration
                )
        );
    }

    private static ItemStack jointStack() {
        return new ItemStack(CreateMechanicalDrive.RIGID_LINK_JOINT_ITEM.get());
    }

    private static ItemStack freeLinkStack() {
        return new ItemStack(CreateMechanicalDrive.RIGID_LINK_ITEM.get());
    }

    private static ItemStack limitedLinkStack() {
        return new ItemStack(CreateMechanicalDrive.RIGID_LINK_LIMITED_ITEM.get());
    }
}
