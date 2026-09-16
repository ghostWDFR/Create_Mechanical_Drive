package dev.createmechanicaldrive.content.suspension_strut;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

public class SuspensionStrutBlock extends SpringBlock {
    public SuspensionStrutBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return jointShape(state.getValue(FACING));
    }

    static VoxelShape jointShape(Direction facing) {
        return switch (facing) {
            case DOWN -> Block.box(4.0D, 12.0D, 4.0D, 12.0D, 16.0D, 12.0D);
            case NORTH -> Block.box(4.0D, 4.0D, 12.0D, 12.0D, 12.0D, 16.0D);
            case SOUTH -> Block.box(4.0D, 4.0D, 0.0D, 12.0D, 12.0D, 4.0D);
            case WEST -> Block.box(12.0D, 4.0D, 4.0D, 16.0D, 12.0D, 12.0D);
            case EAST -> Block.box(0.0D, 4.0D, 4.0D, 4.0D, 12.0D, 12.0D);
            case UP -> Block.box(4.0D, 0.0D, 4.0D, 12.0D, 4.0D, 12.0D);
        };
    }

    @Override
    public InteractionResult onWrenched(
            BlockState state,
            UseOnContext context
    ) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(
            BlockState state,
            UseOnContext context
    ) {
        return context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof SuspensionStrutBlockEntity strut
                ? pickupWithWrench(strut, context.getPlayer())
                : InteractionResult.SUCCESS;
    }

    public static InteractionResult pickupWithWrench(
            SuspensionStrutBlockEntity strut,
            Player player
    ) {
        if (!(strut.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = strut.getBlockPos();
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(
                level,
                pos,
                strut.getBlockState(),
                player
        );
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return InteractionResult.SUCCESS;
        }

        boolean collect = strut.hasLink() && !strut.isPending();
        level.destroyBlock(pos, false);
        if (collect && player != null && !player.isCreative()) {
            player.getInventory().placeItemBackInInventory(
                    new ItemStack(CreateMechanicalDrive.SUSPENSION_STRUT_ITEM.get())
            );
        }

        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighbourState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos
    ) {
        Direction primarySupport = state.getValue(FACING).getOpposite();
        boolean lostPrimarySupport = direction == primarySupport
                && !SpringBlock.canAttach(level, pos, primarySupport);

        SuspensionStrutBlockEntity strut = level.getBlockEntity(pos)
                instanceof SuspensionStrutBlockEntity found
                ? found
                : null;
        Direction linkedFacing = strut == null ? null : strut.getLinkedFacing();
        Direction compactSupport = linkedFacing == null
                ? null
                : linkedFacing.getOpposite();
        boolean lostCompactSupport = strut != null
                && strut.isCompactLink()
                && direction == compactSupport
                && !SpringBlock.canAttach(level, pos, compactSupport);

        if (lostPrimarySupport || lostCompactSupport) {
            if (level instanceof ServerLevel serverLevel
                    && strut != null
                    && strut.claimSupportLossDrop()) {
                Block.popResource(
                        serverLevel,
                        pos,
                        new ItemStack(
                                CreateMechanicalDrive.SUSPENSION_STRUT_ITEM.get()
                        )
                );
            }
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(
                state,
                direction,
                neighbourState,
                level,
                pos,
                neighbourPos
        );
    }

    @Override
    public void afterMove(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockState newState,
            BlockPos oldPos,
            BlockPos newPos
    ) {
        if (resultingLevel.getBlockEntity(newPos)
                instanceof SuspensionStrutBlockEntity movedStrut) {
            movedStrut.afterAssemblyMove(
                    originLevel,
                    resultingLevel,
                    oldPos
            );
        }
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of();
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level,
            BlockPos pos,
            BlockState state
    ) {
        return new ItemStack(CreateMechanicalDrive.SUSPENSION_STRUT_ITEM.get());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntityType<? extends dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive.SUSPENSION_STRUT_BLOCK_ENTITY.get();
    }
}
