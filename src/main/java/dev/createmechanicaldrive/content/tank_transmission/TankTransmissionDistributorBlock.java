package dev.createmechanicaldrive.content.tank_transmission;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

public class TankTransmissionDistributorBlock
        extends RotatedPillarKineticBlock
        implements IBE<TankTransmissionDistributorBlockEntity>, IWrenchable {

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public static final BooleanProperty FRAME_ONLY =
            TankTransmissionFrame.FRAME_ONLY;

    public TankTransmissionDistributorBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(AXIS, Direction.Axis.Z)
                        .setValue(FACING, Direction.NORTH)
                        .setValue(FRAME_ONLY, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing =
                context.getHorizontalDirection();

        BlockState stateBelow =
                context.getLevel()
                        .getBlockState(context.getClickedPos().below());

        boolean frameBelow =
                isTransmissionFrameBlock(stateBelow);

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(AXIS, facing.getAxis())
                .setValue(FRAME_ONLY, frameBelow);
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        Direction inputSide =
                state.getValue(FACING);

        Direction leftSide =
                inputSide.getClockWise();

        Direction rightSide =
                inputSide.getCounterClockWise();

        return direction == inputSide
                || direction == leftSide
                || direction == rightSide;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public BlockState getRotatedBlockState(
            BlockState state,
            Direction targetedFace
    ) {
        if (targetedFace.getAxis() != Direction.Axis.Y) {
            return state;
        }

        Direction rotatedFacing =
                state.getValue(FACING).getClockWise();

        return state
                .setValue(FACING, rotatedFacing)
                .setValue(AXIS, rotatedFacing.getAxis());
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
            return state.setValue(
                    FRAME_ONLY,
                    isTransmissionFrameBlock(neighborState)
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

    private static boolean isTransmissionFrameBlock(
            BlockState state
    ) {
        return state.is(CreateMechanicalDrive.TANK_TRANSMISSION_FRAME.get())
                || state.is(CreateMechanicalDrive.TANK_TRANSMISSION_HOUSING.get())
                || state.is(CreateMechanicalDrive.TANK_TRANSMISSION_DISTRIBUTOR.get())
                || state.is(CreateMechanicalDrive.TANK_TRANSMISSION_STEERING.get());
    }

    @Override
    public float getParticleTargetRadius() {
        return 0.85F;
    }

    @Override
    public float getParticleInitialRadius() {
        return 0.75F;
    }

    @Override
    public InteractionResult onSneakWrenched(
            BlockState state,
            UseOnContext context
    ) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        BlockEvent.BreakEvent event =
                new BlockEvent.BreakEvent(
                        level,
                        pos,
                        state,
                        player
                );

        NeoForge.EVENT_BUS.post(event);

        if (event.isCanceled()) {
            return InteractionResult.SUCCESS;
        }

        boolean frameOnly =
                state.getValue(FRAME_ONLY);

        BlockState frameState =
                CreateMechanicalDrive.TANK_TRANSMISSION_FRAME
                        .get()
                        .defaultBlockState()
                        .setValue(
                                TankTransmissionFrame.FRAME_ONLY,
                                frameOnly
                        );

        level.setBlock(
                pos,
                frameState,
                3
        );

        if (player != null && !player.isCreative()) {
            player.getInventory().placeItemBackInInventory(
                    new ItemStack(
                            CreateMechanicalDrive
                                    .TANK_TRANSMISSION_DISTRIBUTOR_ITEM
                                    .get()
                    )
            );
        }

        IWrenchable.playRemoveSound(
                level,
                pos
        );

        return InteractionResult.SUCCESS;
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of(
                new ItemStack(
                        CreateMechanicalDrive
                                .TANK_TRANSMISSION_FRAME_ITEM
                                .get()
                ),
                new ItemStack(
                        CreateMechanicalDrive
                                .TANK_TRANSMISSION_DISTRIBUTOR_ITEM
                                .get()
                )
        );
    }

    @Override
    public ItemStack getCloneItemStack(
            BlockState state,
            HitResult target,
            LevelReader level,
            BlockPos pos,
            Player player
    ) {
        if (player.isCreative()) {
            return new ItemStack(
                    CreateMechanicalDrive
                            .TANK_TRANSMISSION_DISTRIBUTOR_ASSEMBLED_ITEM
                            .get()
            );
        }

        return new ItemStack(
                CreateMechanicalDrive
                        .TANK_TRANSMISSION_DISTRIBUTOR_ITEM
                        .get()
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, FRAME_ONLY);
    }

    @Override
    public Class<TankTransmissionDistributorBlockEntity>
    getBlockEntityClass() {
        return TankTransmissionDistributorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TankTransmissionDistributorBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive
                .TANK_TRANSMISSION_DISTRIBUTOR_BLOCK_ENTITY
                .get();
    }
}