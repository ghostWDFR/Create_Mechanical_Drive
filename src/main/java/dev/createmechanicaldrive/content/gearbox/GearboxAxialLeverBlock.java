package dev.createmechanicaldrive.content.gearbox;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class GearboxAxialLeverBlock extends Block implements EntityBlock, IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<GearboxPosition> POSITION = EnumProperty.create("position", GearboxPosition.class);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 15.0, 13.0);
    private static boolean sableLookupInitialized;
    private static boolean sableLookupAvailable;
    private static Object sableHelper;
    private static Method sableGetContaining;

    public GearboxAxialLeverBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POSITION, GearboxPosition.NEUTRAL));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return null;
        }

        BlockState gearboxState = context.getLevel().getBlockState(context.getClickedPos().below());
        if (!gearboxState.is(CreateMechanicalDrive.GEARBOX_INPUT.get())) {
            return null;
        }

        Direction gearboxFacing = gearboxState.getValue(CarGearboxInputBlock.FACING);
        if (!gearboxFacing.getAxis().isHorizontal()) {
            return null;
        }

        boolean inSableSubLevel = isInSableSubLevel(
                context.getLevel(),
                context.getClickedPos().below()
        );

        Direction leverFacing = leverFacingForPlacement(
                gearboxFacing,
                inSableSubLevel
        );

        Vec3 localLookDirection = getLocalLookDirectionForPlacement(
                context,
                inSableSubLevel
        );

        if (localLookDirection != null) {
            leverFacing = facingFromLookDirection(
                    gearboxFacing,
                    localLookDirection
            );
        }

        return defaultBlockState()
                .setValue(FACING, leverFacing)
                .setValue(POSITION, GearboxPosition.NEUTRAL);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState gearboxState = level.getBlockState(pos.below());
        return gearboxState.is(CreateMechanicalDrive.GEARBOX_INPUT.get())
                && gearboxState.getValue(CarGearboxInputBlock.FACING).getAxis().isHorizontal()
                && isMountedFacing(state.getValue(FACING), gearboxState.getValue(CarGearboxInputBlock.FACING));
    }

    private static boolean isMountedFacing(Direction leverFacing, Direction gearboxFacing) {
        return leverFacing == gearboxFacing.getOpposite() || leverFacing == gearboxFacing;
    }

    private static Direction leverFacingForPlacement(Direction gearboxFacing, boolean inSableSubLevel) {
        if (inSableSubLevel) {
            return gearboxFacing;
        }

        return gearboxFacing.getAxis() == Direction.Axis.X
                ? gearboxFacing
                : gearboxFacing.getOpposite();
    }

    private static Direction facingFromLookDirection(
            Direction gearboxFacing,
            Vec3 localLookDirection
    ) {
        if (gearboxFacing.getAxis() == Direction.Axis.X) {
            return localLookDirection.x >= 0.0D
                    ? Direction.EAST
                    : Direction.WEST;
        }

        return localLookDirection.z >= 0.0D
                ? Direction.SOUTH
                : Direction.NORTH;
    }

    @Nullable
    private static Vec3 getLocalLookDirectionForPlacement(
            BlockPlaceContext context,
            boolean inSableSubLevel
    ) {
        Player player = context.getPlayer();

        if (player == null) {
            return null;
        }
        Vec3 worldLookDirection = player.getLookAngle();

        if (!inSableSubLevel) {
            return worldLookDirection;
        }
        ensureSableLookupInitialized();

        if (!sableLookupAvailable) {
            return null;
        }

        try {
            BlockPos gearboxPos =
                    context.getClickedPos().below();

            Object subLevel = sableGetContaining.invoke(
                    sableHelper,
                    context.getLevel(),
                    gearboxPos
            );

            if (subLevel == null) {
                return null;
            }

            Method logicalPoseMethod =
                    subLevel.getClass().getMethod("logicalPose");

            Object logicalPose =
                    logicalPoseMethod.invoke(subLevel);

            if (logicalPose == null) {
                return null;
            }

            Method transformNormalInverseMethod =
                    logicalPose.getClass().getMethod(
                            "transformNormalInverse",
                            Vec3.class
                    );

            Object transformed =
                    transformNormalInverseMethod.invoke(
                            logicalPose,
                            worldLookDirection
                    );

            return transformed instanceof Vec3 localLookDirection
                    ? localLookDirection
                    : null;

        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static boolean isInSableSubLevel(Level level, BlockPos pos) {
        ensureSableLookupInitialized();
        if (!sableLookupAvailable) {
            return false;
        }

        try {
            return sableGetContaining.invoke(sableHelper, level, pos) != null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static void ensureSableLookupInitialized() {
        if (sableLookupInitialized) {
            return;
        }

        sableLookupInitialized = true;
        try {
            Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable");
            Class<?> helperClass = Class.forName("dev.ryanhcode.sable.ActiveSableCompanion");
            Field helperField = sableClass.getField("HELPER");
            sableHelper = helperField.get(null);
            sableGetContaining = helperClass.getMethod("getContaining", Level.class, net.minecraft.core.Vec3i.class);
            sableLookupAvailable = sableHelper != null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            sableLookupAvailable = false;
        }
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
        if (direction == Direction.DOWN && !state.canSurvive(level, currentPos)) {
            level.scheduleTick(currentPos, this, 1);
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
    protected void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.SUCCESS;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(
                new ItemStack(CreateMechanicalDrive.GEARBOX_AXIAL_LEVER_ITEM.get())
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POSITION);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GearboxAxialLeverBlockEntity(CreateMechanicalDrive.GEARBOX_AXIAL_LEVER_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != CreateMechanicalDrive.GEARBOX_AXIAL_LEVER_BLOCK_ENTITY.get()) {
            return null;
        }

        return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                ((GearboxAxialLeverBlockEntity) blockEntity).tickAnimation();
    }
}
