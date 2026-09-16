package dev.createmechanicaldrive.infrastructure.ponder;

import com.mojang.serialization.MapCodec;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.gearbox.GearboxAxialLeverBlock;
import dev.createmechanicaldrive.content.gearbox.GearboxAxialLeverBlockEntity;
import dev.createmechanicaldrive.content.gearbox.GearboxPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nullable;

public class PonderGearboxLeverBlock extends Block implements EntityBlock {
    public static final MapCodec<PonderGearboxLeverBlock> CODEC =
            simpleCodec(PonderGearboxLeverBlock::new);

    public static final DirectionProperty FACING =
            GearboxAxialLeverBlock.FACING;
    public static final EnumProperty<GearboxPosition> POSITION =
            GearboxAxialLeverBlock.POSITION;

    public PonderGearboxLeverBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POSITION, GearboxPosition.NEUTRAL));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(POSITION, GearboxPosition.NEUTRAL);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POSITION);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GearboxAxialLeverBlockEntity(
                CreateMechanicalDrive.GEARBOX_AXIAL_LEVER_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (blockEntityType != CreateMechanicalDrive.GEARBOX_AXIAL_LEVER_BLOCK_ENTITY.get()) {
            return null;
        }

        return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                ((GearboxAxialLeverBlockEntity) blockEntity).tickAnimation();
    }
}
