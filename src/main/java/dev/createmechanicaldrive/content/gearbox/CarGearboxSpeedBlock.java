package dev.createmechanicaldrive.content.gearbox;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

public class CarGearboxSpeedBlock extends AbstractEncasedShaftBlock implements IBE<CarGearboxSpeedBlockEntity>, IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<GearboxPosition> POSITION = EnumProperty.create("position", GearboxPosition.class);

    public CarGearboxSpeedBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(AXIS, Direction.Axis.Z)
                .setValue(FACING, Direction.NORTH)
                .setValue(POSITION, GearboxPosition.NEUTRAL));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState inputState = context.getLevel().getBlockState(pos.relative(direction));

            if (inputState.is(CreateMechanicalDrive.GEARBOX_INPUT.get())
                    && inputState.getValue(CarGearboxInputBlock.FACING) == direction) {
                return defaultBlockState()
                        .setValue(FACING, direction)
                        .setValue(AXIS, direction.getAxis())
                        .setValue(POSITION, GearboxPosition.NEUTRAL);
            }
        }

        Direction connectedDirection = context.getClickedFace().getOpposite();

        if (connectedDirection.getAxis().isHorizontal()) {
            BlockPos neighbourPos = pos.relative(connectedDirection);
            BlockState neighbourState = context.getLevel().getBlockState(neighbourPos);

            if (neighbourState.getBlock() instanceof IRotate rotate
                    && rotate.hasShaftTowards(
                    context.getLevel(),
                    neighbourPos,
                    neighbourState,
                    connectedDirection.getOpposite()
            )) {
                Direction facing = connectedDirection.getOpposite();

                return defaultBlockState()
                        .setValue(FACING, facing)
                        .setValue(AXIS, facing.getAxis())
                        .setValue(POSITION, GearboxPosition.NEUTRAL);
            }
        }

        Direction facing = context.getHorizontalDirection();

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(AXIS, facing.getAxis())
                .setValue(POSITION, GearboxPosition.NEUTRAL);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic) {
            RotationPropagator.handleAdded(level, pos, kinetic);
        }
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction direction) {
        return direction.getAxis() == state.getValue(AXIS);
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

        Direction rotatedFacing = state.getValue(FACING).getClockWise();

        return state
                .setValue(FACING, rotatedFacing)
                .setValue(AXIS, rotatedFacing.getAxis());
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return super.areStatesKineticallyEquivalent(oldState, newState);
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
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POSITION);
    }

    @Override
    public Class<CarGearboxSpeedBlockEntity> getBlockEntityClass() {
        return CarGearboxSpeedBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CarGearboxSpeedBlockEntity> getBlockEntityType() {
        return CreateMechanicalDrive.GEARBOX_SPEED_BLOCK_ENTITY.get();
    }
}
