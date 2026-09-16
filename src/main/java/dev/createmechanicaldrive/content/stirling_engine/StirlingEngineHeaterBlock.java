package dev.createmechanicaldrive.content.stirling_engine;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class StirlingEngineHeaterBlock extends Block implements EntityBlock, IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final float SMOKE_START_HEAT = 0.5F;
    private static final int MAX_SMOKE_PARTICLES = 4;
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(3.0, 3.0, 2.0, 13.0, 13.0, 16.0),
            Block.box(2.0, 2.0, 0.0, 14.0, 14.0, 2.0)
    );
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(3.0, 3.0, 0.0, 13.0, 13.0, 14.0),
            Block.box(2.0, 2.0, 14.0, 14.0, 14.0, 16.0)
    );
    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(0.0, 3.0, 3.0, 14.0, 13.0, 13.0),
            Block.box(14.0, 2.0, 2.0, 16.0, 14.0, 14.0)
    );
    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(2.0, 3.0, 3.0, 16.0, 13.0, 13.0),
            Block.box(0.0, 2.0, 2.0, 2.0, 14.0, 14.0)
    );
    private static final VoxelShape SHAPE_UP = Shapes.or(
            Block.box(3.0, 0.0, 3.0, 13.0, 14.0, 13.0),
            Block.box(2.0, 14.0, 2.0, 14.0, 16.0, 14.0)
    );
    private static final VoxelShape SHAPE_DOWN = Shapes.or(
            Block.box(3.0, 2.0, 3.0, 13.0, 16.0, 13.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 2.0, 14.0)
    );

    public StirlingEngineHeaterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(AXIS, Direction.Axis.Z)
                .setValue(LIT, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        for (Direction direction : Direction.values()) {
            BlockState coreState = level.getBlockState(pos.relative(direction));
            if (coreState.is(CreateMechanicalDrive.STIRLING_ENGINE_CORE.get())
                    && coreState.getValue(StirlingEngineCoreBlock.FACING) == direction) {
                return stateFacing(direction);
            }
        }

        return stateFacing(context.getNearestLookingDirection());
    }

    private BlockState stateFacing(Direction facing) {
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(AXIS, facing.getAxis())
                .setValue(LIT, false);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(this);
        List<ItemStack> drops = new ArrayList<>();
        drops.add(stack);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof StirlingEngineHeaterBlockEntity heater) {

            StirlingEngineHeaterItem.setHeatTicks(
                    stack,
                    heater.getHeatTicks(),
                    heater.getLevel() != null
                            ? heater.getLevel().getGameTime()
                            : 0L
            );

            if (heater.hasCover()) {
                drops.add(new ItemStack(CreateMechanicalDrive.STIRLING_ENGINE_HEATER_COVER_ITEM.get()));
            }
        }
        return drops;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!stack.is(CreateMechanicalDrive.STIRLING_ENGINE_HEATER_COVER_ITEM.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!(level.getBlockEntity(pos) instanceof StirlingEngineHeaterBlockEntity heater)
                || heater.hasCover()
                || heater.isCoverRemoving()) {
            return ItemInteractionResult.SUCCESS;
        }

        if (!level.isClientSide) {
            heater.setCover(true);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof StirlingEngineHeaterBlockEntity heater)
                || !heater.hasCover()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            heater.setCover(false);
            if (!player.isCreative()) {
                ItemStack cover = new ItemStack(CreateMechanicalDrive.STIRLING_ENGINE_HEATER_COVER_ITEM.get());
                if (!player.addItem(cover)) {
                    player.drop(cover, false);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof StirlingEngineHeaterBlockEntity heater) {

            StirlingEngineHeaterItem.setHeatTicks(
                    stack,
                    heater.getHeatTicks(),
                    heater.getLevel() != null
                            ? heater.getLevel().getGameTime()
                            : 0L
            );
        }
        return stack;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getShapeForFacing(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getShapeForFacing(state.getValue(FACING));
    }

    private static VoxelShape getShapeForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            default -> SHAPE_NORTH;
        };
    }
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof StirlingEngineHeaterBlockEntity heater)) {
            return;
        }

        float heatRatio = heater.getHeatRatio();
        if (heatRatio <= SMOKE_START_HEAT) {
            return;
        }

        float smokeIntensity = (heatRatio - SMOKE_START_HEAT) / (1.0F - SMOKE_START_HEAT);
        if (random.nextFloat() > 0.2F + 0.8F * smokeIntensity) {
            return;
        }

        int particles = 1 + Math.min(
                MAX_SMOKE_PARTICLES - 1,
                (int) (smokeIntensity * MAX_SMOKE_PARTICLES)
        );
        for (int i = 0; i < particles; i++) {
            spawnHeatSmoke(level, pos, random, smokeIntensity);
        }
    }

    private static void spawnHeatSmoke(
            Level level,
            BlockPos pos,
            RandomSource random,
            float smokeIntensity
    ) {
        double x = pos.getX() + 0.25D + random.nextDouble() * 0.5D;
        double y = pos.getY() + 0.92D + random.nextDouble() * 0.08D;
        double z = pos.getZ() + 0.25D + random.nextDouble() * 0.5D;
        double xSpeed = (random.nextDouble() - 0.5D) * 0.02D;
        double ySpeed = 0.02D + random.nextDouble() * 0.025D + smokeIntensity * 0.035D;
        double zSpeed = (random.nextDouble() - 0.5D) * 0.02D;

        level.addParticle(
                ParticleTypes.SMOKE,
                x,
                y,
                z,
                xSpeed,
                ySpeed,
                zSpeed
        );
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, AXIS, LIT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StirlingEngineHeaterBlockEntity(
                CreateMechanicalDrive.STIRLING_ENGINE_HEATER_BLOCK_ENTITY.get(),
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
        if (blockEntityType != CreateMechanicalDrive.STIRLING_ENGINE_HEATER_BLOCK_ENTITY.get()) {
            return null;
        }

        return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                ((StirlingEngineHeaterBlockEntity) blockEntity).tick();
    }
}
