package dev.createmechanicaldrive.content.angle_gear;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AngleGearItem
        extends BlockItem {

    public AngleGearItem(
            AngleGearBlock block,
            Item.Properties properties
    ) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {
        Level level =
                context.getLevel();

        Player player =
                context.getPlayer();

        Direction clickedFace =
                context.getClickedFace();

        ItemStack stack =
                context.getItemInHand();

        InteractionResult clickedGearResult =
                tryAddToExistingGear(
                        level,
                        context.getClickedPos(),
                        clickedFace,
                        stack,
                        player
                );

        if (clickedGearResult.consumesAction()) {
            return clickedGearResult;
        }

        BlockPos placementPos =
                context.getClickedPos()
                        .relative(clickedFace);

        InteractionResult placedGearResult =
                tryAddToExistingGear(
                        level,
                        placementPos,
                        clickedFace.getOpposite(),
                        stack,
                        player
                );

        if (placedGearResult.consumesAction()) {
            return placedGearResult;
        }

        return super.useOn(context);
    }

    private static InteractionResult tryAddToExistingGear(
            Level level,
            BlockPos pos,
            Direction side,
            ItemStack stack,
            Player player
    ) {
        BlockState state =
                level.getBlockState(pos);

        if (!state.is(CreateMechanicalDrive.ANGLE_GEAR.get())) {
            return InteractionResult.PASS;
        }

        AngleGearBlock.AddGearResult result =
                AngleGearBlock.tryAddGear(
                        level,
                        pos,
                        state,
                        side,
                        stack,
                        player
                );

        return switch (result) {
            case PLACED -> InteractionResult.SUCCESS;
            case BLOCKED -> InteractionResult.CONSUME_PARTIAL;
            case PASS -> InteractionResult.PASS;
        };
    }
}
