package dev.createmechanicaldrive.infrastructure.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.chain_linkage.ChainGearBlock;
import dev.createmechanicaldrive.content.chain_linkage.ChainGearBlockEntity;
import dev.createmechanicaldrive.content.chain_linkage.ChainLinkageValidator;
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
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static dev.createmechanicaldrive.infrastructure.ponder.PonderSceneCommon.*;

final class ChainGearPonderScenes {
    private static final ResourceLocation CHAIN_GEAR_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "chain_drive/relay");

    private ChainGearPonderScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                        loc("chain_gear"),
                        loc("chain_linkage")
                )
                .addStoryBoard(
                        CHAIN_GEAR_SCHEMATIC,
                        ChainGearPonderScenes::chainGearLinkage
                )
                .addStoryBoard(
                        CHAIN_GEAR_SCHEMATIC,
                        ChainGearPonderScenes::chainGearStraightLink
                )
                .addStoryBoard(
                        CHAIN_GEAR_SCHEMATIC,
                        ChainGearPonderScenes::chainGearSable
                );
    }

    private static void chainGearLinkage(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "chain_gear_linkage",
                "Linking Chain Gears"
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

        BlockPos first =
                util.grid().at(
                        1,
                        1,
                        1
                );
        BlockPos second =
                util.grid().at(
                        4,
                        1,
                        1
                );
        BlockPos third =
                util.grid().at(
                        4,
                        1,
                        3
                );
        BlockPos fourth =
                util.grid().at(
                        1,
                        1,
                        3
                );

        List<BlockPos> loop =
                List.of(
                        first,
                        second,
                        third,
                        fourth
                );

        BlockState shaft =
                shaftState(Direction.Axis.Y);

        for (BlockPos gear : loop) {
            scene.world().setBlock(
                    gear,
                    chainGearState(Direction.Axis.Y),
                    false
            );
            scene.world().setBlock(
                    gear.above(),
                    shaft,
                    false
            );
        }

        scene.world().showSection(
                util.select().fromTo(
                        first,
                        third.above()
                ),
                Direction.DOWN
        );
        scene.idle(
                15
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "Chain Gears are kinetic shafts. Place them with the same rotation axis before linking them."
                )
                .pointAt(
                        util.vector().centerOf(first)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                85
        );

        scene.overlay().showControls(
                        util.vector().topOf(first),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(
                        new ItemStack(
                                CreateMechanicalDrive.CHAIN_LINKAGE_ITEM.get()
                        )
                );
        scene.overlay().showText(
                        80
                )
                .text(
                        "Hold Drive Chain and right-click each Chain Gear in order."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(first)
                )
                .placeNearTarget();
        scene.idle(
                90
        );

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                first,
                                third
                        ),
                        85
                )
                .text(
                        "Click the first selected gear again to close the loop and consume the required Drive Chains."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(fourth)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                45
        );

        linkChain(
                scene,
                loop,
                ChainLinkageValidator.getChainsRequired(loop)
        );
        scene.effects().indicateSuccess(first);
        scene.effects().indicateSuccess(second);
        scene.effects().indicateSuccess(third);
        scene.effects().indicateSuccess(fourth);
        scene.idle(
                50
        );

        scene.world().setKineticSpeed(
                util.select().fromTo(
                        first,
                        third.above()
                ),
                32.0F
        );
        scene.effects().rotationSpeedIndicator(first);
        scene.effects().rotationSpeedIndicator(third);
        scene.overlay().showText(
                        80
                )
                .text(
                        "A completed chain transmits rotation between all linked Chain Gears at the same speed."
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

        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                first,
                                third
                        ),
                        90
                )
                .text(
                        "Loops must stay in one plane, use matching axes, fit within 10 blocks, and follow the outside edge without crossing themselves."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(third)
                )
                .placeNearTarget();
        scene.idle(
                100
        );

        scene.world().hideSection(
                util.select().position(third),
                Direction.UP
        );
        scene.idle(
                20
        );
        scene.overlay().showText(
                        75
                )
                .text(
                        "Breaking a linked Chain Gear removes the whole chain and returns the stored Drive Chains."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(first)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                85
        );
    }

    private static void chainGearStraightLink(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "chain_gear_straight_link",
                "Straight Chain Links"
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

        BlockPos left =
                util.grid().at(
                        1,
                        1,
                        2
                );
        BlockPos middle =
                util.grid().at(
                        2,
                        1,
                        2
                );
        BlockPos right =
                util.grid().at(
                        4,
                        1,
                        2
                );

        scene.world().setBlock(
                left,
                chainGearState(Direction.Axis.Y),
                false
        );
        scene.world().setBlock(
                right,
                chainGearState(Direction.Axis.Y),
                false
        );
        scene.world().setBlock(
                left.above(),
                shaftState(Direction.Axis.Y),
                false
        );
        scene.world().setBlock(
                right.above(),
                shaftState(Direction.Axis.Y),
                false
        );
        scene.world().showSection(
                util.select().fromTo(
                        left,
                        right.above()
                ),
                Direction.DOWN
        );
        scene.idle(
                20
        );

        List<BlockPos> straightLink =
                List.of(
                        left,
                        right
                );
        linkChain(
                scene,
                straightLink,
                ChainLinkageValidator.getChainsRequired(straightLink)
        );
        scene.overlay().showOutlineWithText(
                        util.select().fromTo(
                                left,
                                right
                        ),
                        85
                )
                .text(
                        "Two Chain Gears can form a straight chain link."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(left)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                90
        );

        scene.world().setBlock(
                middle,
                chainGearState(Direction.Axis.Y),
                false
        );
        scene.world().setBlock(
                middle.above(),
                shaftState(Direction.Axis.Y),
                false
        );
        scene.world().showSection(
                util.select().fromTo(
                        middle,
                        middle.above()
                ),
                Direction.DOWN
        );
        scene.effects().indicateSuccess(middle);
        scene.overlay().showControls(
                        util.vector().topOf(middle),
                        Pointing.DOWN,
                        45
                )
                .rightClick()
                .withItem(
                        new ItemStack(
                                CreateMechanicalDrive.CHAIN_GEAR_ITEM.get()
                        )
                );
        scene.overlay().showText(
                        80
                )
                .text(
                        "A Chain Gear placed on the same straight span joins the driven line without creating a new loop."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(middle)
                )
                .placeNearTarget();
        scene.idle(
                90
        );

        scene.world().setKineticSpeed(
                util.select().fromTo(
                        left,
                        right.above()
                ),
                32.0F
        );
        scene.effects().rotationSpeedIndicator(left);
        scene.effects().rotationSpeedIndicator(middle);
        scene.effects().rotationSpeedIndicator(right);
        scene.overlay().showText(
                        80
                )
                .text(
                        "Only the two end gears store the straight chain. Any matching Chain Gear between them can still receive rotation."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(middle)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                90
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        "Intermediate gears must sit on the line between the endpoints, in the same plane and with the same rotation axis."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(middle)
                )
                .placeNearTarget();
        scene.idle(
                95
        );
    }

    private static void chainGearSable(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "chain_gear_sable",
                "Chains Between Sable Sublevels"
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

        BlockPos firstSublevelGear =
                util.grid().at(
                        1,
                        2,
                        1
                );
        BlockPos secondSublevelGear =
                util.grid().at(
                        4,
                        2,
                        1
                );
        BlockPos worldGear =
                util.grid().at(
                        4,
                        2,
                        3
                );
        BlockPos thirdGear =
                util.grid().at(
                        1,
                        2,
                        3
                );

        buildSablePlatform(
                scene,
                firstSublevelGear,
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState()
        );
        buildSablePlatform(
                scene,
                secondSublevelGear,
                Blocks.ORANGE_CONCRETE.defaultBlockState()
        );
        buildSablePlatform(
                scene,
                thirdGear,
                Blocks.LIME_CONCRETE.defaultBlockState()
        );
        buildWorldAnchor(
                scene,
                worldGear
        );

        List<BlockPos> sublevelLink =
                List.of(
                        firstSublevelGear,
                        secondSublevelGear
                );
        List<BlockPos> worldLink =
                List.of(
                        thirdGear,
                        worldGear
                );

        linkChain(
                scene,
                sublevelLink,
                ChainLinkageValidator.getChainsRequired(sublevelLink)
        );
        linkChain(
                scene,
                worldLink,
                ChainLinkageValidator.getChainsRequired(worldLink)
        );
        enablePonderFlexibleChain(
                scene,
                sublevelLink
        );
        enablePonderFlexibleChain(
                scene,
                worldLink
        );

        var firstPlatform = scene.world().showIndependentSection(
                util.select().position(firstSublevelGear.below()),
                Direction.DOWN
        );
        scene.world().showSection(
                util.select().position(firstSublevelGear),
                Direction.DOWN
        );
        var secondPlatform = scene.world().showIndependentSection(
                util.select().position(secondSublevelGear.below()),
                Direction.DOWN
        );
        scene.world().showSection(
                util.select().position(secondSublevelGear),
                Direction.DOWN
        );
        var thirdPlatform = scene.world().showIndependentSection(
                util.select().position(thirdGear.below()),
                Direction.DOWN
        );
        scene.world().showSection(
                util.select().position(thirdGear),
                Direction.DOWN
        );
        scene.world().showSection(
                util.select().fromTo(
                        worldGear.below(),
                        worldGear
                ),
                Direction.DOWN
        );
        scene.idle(
                20
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "With Sable, a two-gear chain may connect one moving sublevel to another moving sublevel."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(firstSublevelGear)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                85
        );

        scene.overlay().showText(
                        75
                )
                .text(
                        "The gears may ride on different sublevels, as long as those sublevels keep the same orientation."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(secondSublevelGear)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                85
        );

        Vec3 firstSublevelOffset =
                util.vector().of(
                        0.0D,
                        0.0D,
                        0.35D
                );
        scene.world().moveSection(
                firstPlatform,
                firstSublevelOffset,
                25
        );
        movePonderGear(
                scene,
                firstSublevelGear,
                firstSublevelOffset,
                25
        );
        Vec3 secondSublevelOffset =
                util.vector().of(
                        0.0D,
                        0.0D,
                        -0.35D
                );
        scene.world().moveSection(
                secondPlatform,
                secondSublevelOffset,
                25
        );
        movePonderGear(
                scene,
                secondSublevelGear,
                secondSublevelOffset,
                25
        );
        scene.world().setKineticSpeed(
                util.select().fromTo(
                        firstSublevelGear,
                        secondSublevelGear.above()
                ),
                32.0F
        );
        scene.effects().rotationSpeedIndicator(firstSublevelGear);
        scene.effects().rotationSpeedIndicator(secondSublevelGear);
        scene.idle(
                35
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        "This flexible link follows small movement between the two sublevels instead of requiring a rigid loop."
                )
                .colored(
                        PonderPalette.GREEN
                )
                .pointAt(
                        util.vector().centerOf(secondSublevelGear)
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
                        "The same two-gear link can also connect a Sable sublevel to the normal world."
                )
                .colored(
                        PonderPalette.INPUT
                )
                .pointAt(
                        util.vector().centerOf(worldGear)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                85
        );

        Vec3 thirdSublevelOffset =
                util.vector().of(
                        0.35D,
                        0.0D,
                        0.0D
                );
        scene.world().moveSection(
                thirdPlatform,
                thirdSublevelOffset,
                25
        );
        movePonderGear(
                scene,
                thirdGear,
                thirdSublevelOffset,
                25
        );
        scene.world().setKineticSpeed(
                util.select().fromTo(
                        thirdGear,
                        worldGear.above()
                ),
                32.0F
        );
        scene.effects().rotationSpeedIndicator(thirdGear);
        scene.effects().rotationSpeedIndicator(worldGear);
        scene.idle(
                35
        );

        scene.overlay().showText(
                        85
                )
                .text(
                        "Cross-sublevel chains are only supported for two Chain Gears. Larger loops must stay on one Sable sublevel."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(firstSublevelGear)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                95
        );

        scene.overlay().showText(
                        80
                )
                .text(
                        "If either side rotates away, shifts too far out of plane, or stretches the chain too much, the link breaks and refunds its Drive Chains."
                )
                .colored(
                        PonderPalette.RED
                )
                .pointAt(
                        util.vector().centerOf(secondSublevelGear)
                )
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(
                90
        );
    }

    private static void enablePonderFlexibleChain(
            CreateSceneBuilder scene,
            List<BlockPos> loop
    ) {
        for (BlockPos gear : loop) {
            scene.world().modifyBlockEntity(
                    gear,
                    ChainGearBlockEntity.class,
                    blockEntity -> blockEntity.setPonderFlexibleChainRender(true)
            );
        }
    }

    private static void movePonderGear(
            CreateSceneBuilder scene,
            BlockPos gear,
            Vec3 offset,
            int duration
    ) {
        scene.world().modifyBlockEntity(
                gear,
                ChainGearBlockEntity.class,
                blockEntity -> blockEntity.setPonderRenderOffset(
                        offset,
                        duration
                )
        );
    }

    private static void buildSablePlatform(
            CreateSceneBuilder scene,
            BlockPos gear,
            BlockState platform
    ) {
        scene.world().setBlock(
                gear.below(),
                platform,
                false
        );
        scene.world().setBlock(
                gear,
                chainGearState(Direction.Axis.Y),
                false
        );
    }

    private static void buildWorldAnchor(
            CreateSceneBuilder scene,
            BlockPos gear
    ) {
        scene.world().setBlock(
                gear.below(),
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                false
        );
        scene.world().setBlock(
                gear,
                chainGearState(Direction.Axis.Y),
                false
        );
    }

    private static BlockState chainGearState(
            Direction.Axis axis
    ) {
        return CreateMechanicalDrive
                .CHAIN_GEAR
                .get()
                .defaultBlockState()
                .setValue(
                        ChainGearBlock.AXIS,
                        axis
                )
                .setValue(
                        ChainGearBlock.CONNECTED_NEGATIVE,
                        true
                )
                .setValue(
                        ChainGearBlock.CONNECTED_POSITIVE,
                        true
                );
    }

    private static BlockState shaftState(
            Direction.Axis axis
    ) {
        return AllBlocks.SHAFT
                .get()
                .defaultBlockState()
                .setValue(
                        RotatedPillarBlock.AXIS,
                        axis
                );
    }

    private static void linkChain(
            CreateSceneBuilder scene,
            List<BlockPos> loop,
            int chainsToRefund
    ) {
        for (int i = 0; i < loop.size(); i++) {
            final int index =
                    i;
            final int refund =
                    index == 0
                            ? chainsToRefund
                            : 0;

            scene.world().modifyBlockEntity(
                    loop.get(index),
                    ChainGearBlockEntity.class,
                    gear -> gear.setChainLoop(
                            loop,
                            index,
                            refund
                    )
            );
        }
    }
}