package dev.createmechanicaldrive.content.stirling_engine;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractShaftBlock;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class StirlingEnginePoweredShaftBlock extends AbstractShaftBlock {

    private static final VoxelShape SHAPE_X =
            box(
                    0.0D,
                    5.0D,
                    5.0D,
                    16.0D,
                    11.0D,
                    11.0D
            );

    private static final VoxelShape SHAPE_Y =
            box(
                    5.0D,
                    0.0D,
                    5.0D,
                    11.0D,
                    16.0D,
                    11.0D
            );

    private static final VoxelShape SHAPE_Z =
            box(
                    5.0D,
                    5.0D,
                    0.0D,
                    11.0D,
                    11.0D,
                    16.0D
            );

    public StirlingEnginePoweredShaftBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            case Z -> SHAPE_Z;
        };
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (StirlingEnginePoweredShaftBlockEntity.hasValidOutput(level, pos, state)) {
            return;
        }

        level.setBlock(pos, getShaftEquivalent(state), 3);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(AllBlocks.SHAFT.asStack());
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return AllBlocks.SHAFT.asStack();
    }

    @Override
    public Class<KineticBlockEntity> getBlockEntityClass() {
        return KineticBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return CreateMechanicalDrive.STIRLING_ENGINE_POWERED_SHAFT_BLOCK_ENTITY.get();
    }

    static BlockState getPoweredEquivalent(BlockState shaftState) {
        BlockState state = CreateMechanicalDrive.STIRLING_ENGINE_POWERED_SHAFT.get()
                .defaultBlockState()
                .setValue(AXIS, shaftState.getValue(AXIS));

        if (shaftState.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(
                    BlockStateProperties.WATERLOGGED,
                    shaftState.getValue(BlockStateProperties.WATERLOGGED)
            );
        }

        return state;
    }

    static BlockState getShaftEquivalent(BlockState poweredState) {
        BlockState state = AllBlocks.SHAFT.get()
                .defaultBlockState()
                .setValue(AXIS, poweredState.getValue(AXIS));

        if (poweredState.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(
                    BlockStateProperties.WATERLOGGED,
                    poweredState.getValue(BlockStateProperties.WATERLOGGED)
            );
        }

        return state;
    }
}

