package dev.createmechanicaldrive.content.tank_transmission;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class TankTransmissionFrame extends Block implements IWrenchable {

    public static final BooleanProperty FRAME_ONLY =
            BooleanProperty.create("frame_only");

    public TankTransmissionFrame(Properties properties) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(FRAME_ONLY, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);
        builder.add(FRAME_ONLY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos posBelow = context.getClickedPos().below();
        BlockState stateBelow = context.getLevel().getBlockState(posBelow);

        boolean frameBelow =
                stateBelow.is(this)
                        || stateBelow.is(
                        CreateMechanicalDrive
                                .TANK_TRANSMISSION_HOUSING
                                .get()
                )
                        || stateBelow.is(
                        CreateMechanicalDrive
                                .TANK_TRANSMISSION_DISTRIBUTOR
                                .get()
                )
                        || stateBelow.is(
                        CreateMechanicalDrive
                                .TANK_TRANSMISSION_STEERING
                                .get()
                );

        return defaultBlockState()
                .setValue(FRAME_ONLY, frameBelow);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos currentPos,
            BlockPos neighborPos
    ) {
        if (direction == Direction.DOWN) {
            boolean frameBelow =
                    neighborState.is(this)
                            || neighborState.is(
                            CreateMechanicalDrive
                                    .TANK_TRANSMISSION_HOUSING
                                    .get()
                    )
                            || neighborState.is(
                            CreateMechanicalDrive
                                    .TANK_TRANSMISSION_DISTRIBUTOR
                                    .get()
                    )
                            || neighborState.is(
                            CreateMechanicalDrive
                                    .TANK_TRANSMISSION_STEERING
                                    .get()
                    );

            return state.setValue(
                    FRAME_ONLY,
                    frameBelow
            );
        }

        return super.updateShape(
                state,
                direction,
                neighborState,
                level,
                currentPos,
                neighborPos
        );
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of(new ItemStack(this));
    }

    @Override
    public ItemStack getCloneItemStack(
            BlockState state,
            HitResult target,
            LevelReader level,
            BlockPos pos,
            Player player
    ) {
        return new ItemStack(this);
    }
}