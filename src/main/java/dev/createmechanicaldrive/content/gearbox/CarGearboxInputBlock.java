package dev.createmechanicaldrive.content.gearbox;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class CarGearboxInputBlock extends RotatedPillarKineticBlock implements IBE<CarGearboxInputBlockEntity>, IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<GearboxPosition> POSITION = EnumProperty.create("position", GearboxPosition.class);

    public CarGearboxInputBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(AXIS, Direction.Axis.Z)
                .setValue(FACING, Direction.NORTH)
                .setValue(POSITION, GearboxPosition.NEUTRAL));
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        BlockPos pos =
                context.getClickedPos();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState speedState =
                    context.getLevel()
                            .getBlockState(
                                    pos.relative(direction)
                            );

            if (speedState.is(
                    CreateMechanicalDrive
                            .GEARBOX_SPEED
                            .get()
            )
                    && speedState.getValue(
                    CarGearboxSpeedBlock.FACING
            ) == direction.getOpposite()) {
                Direction facing =
                        direction.getOpposite();

                return defaultBlockState()
                        .setValue(
                                FACING,
                                facing
                        )
                        .setValue(
                                AXIS,
                                facing.getAxis()
                        )
                        .setValue(
                                POSITION,
                                GearboxPosition.NEUTRAL
                        );
            }
        }

        Direction facing =
                context.getHorizontalDirection()
                        .getOpposite();

        return defaultBlockState()
                .setValue(
                        FACING,
                        facing
                )
                .setValue(
                        AXIS,
                        facing.getAxis()
                )
                .setValue(
                        POSITION,
                        GearboxPosition.NEUTRAL
                );
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
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POSITION);
    }

    @Override
    public Class<CarGearboxInputBlockEntity> getBlockEntityClass() {
        return CarGearboxInputBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CarGearboxInputBlockEntity> getBlockEntityType() {
        return CreateMechanicalDrive.GEARBOX_INPUT_BLOCK_ENTITY.get();
    }
}
