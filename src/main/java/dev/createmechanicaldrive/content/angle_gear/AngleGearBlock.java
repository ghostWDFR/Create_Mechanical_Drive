package dev.createmechanicaldrive.content.angle_gear;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AngleGearBlock
        extends RotatedPillarKineticBlock
        implements IBE<AngleGearBlockEntity>, IWrenchable {

    private static final int MAX_GEARS = 4;

    public enum AddGearResult {
        PASS,
        PLACED,
        BLOCKED
    }

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty ENCASED =
            BooleanProperty.create("encased");

    private static final VoxelShape FULL_BLOCK =
            box(
                    0.0D,
                    0.0D,
                    0.0D,
                    16.0D,
                    16.0D,
                    16.0D
            );

    private static final Map<Direction, BooleanProperty> SIDE_PROPERTIES =
            new EnumMap<>(Direction.class);

    private static final Map<Direction, VoxelShape> SIDE_SHAPES =
            new EnumMap<>(Direction.class);

    static {
        SIDE_PROPERTIES.put(Direction.NORTH, NORTH);
        SIDE_PROPERTIES.put(Direction.SOUTH, SOUTH);
        SIDE_PROPERTIES.put(Direction.WEST, WEST);
        SIDE_PROPERTIES.put(Direction.EAST, EAST);
        SIDE_PROPERTIES.put(Direction.DOWN, DOWN);
        SIDE_PROPERTIES.put(Direction.UP, UP);

        SIDE_SHAPES.put(Direction.NORTH, box(3.0D, 3.0D, 0.0D, 13.0D, 13.0D, 6.0D));
        SIDE_SHAPES.put(Direction.SOUTH, box(3.0D, 3.0D, 10.0D, 13.0D, 13.0D, 16.0D));
        SIDE_SHAPES.put(Direction.WEST, box(0.0D, 3.0D, 3.0D, 6.0D, 13.0D, 13.0D));
        SIDE_SHAPES.put(Direction.EAST, box(10.0D, 3.0D, 3.0D, 16.0D, 13.0D, 13.0D));
        SIDE_SHAPES.put(Direction.DOWN, box(3.0D, 0.0D, 3.0D, 13.0D, 6.0D, 13.0D));
        SIDE_SHAPES.put(Direction.UP, box(3.0D, 10.0D, 3.0D, 13.0D, 16.0D, 13.0D));
    }

    public AngleGearBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(AXIS, Direction.Axis.Z)
                        .setValue(NORTH, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false)
                        .setValue(EAST, false)
                        .setValue(DOWN, false)
                        .setValue(UP, false)
                        .setValue(ENCASED, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Direction side =
                getTargetedGearSide(context);

        return withGear(
                defaultBlockState()
                        .setValue(AXIS, side.getAxis())
                        .setValue(ENCASED, false),
                side
        );
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
        if (AllBlocks.ANDESITE_CASING.isIn(stack)) {
            if (state.getValue(ENCASED)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!level.isClientSide) {
                level.setBlock(
                        pos,
                        state.setValue(ENCASED, true),
                        3
                );

                playCasingPlaceSound(
                        level,
                        pos,
                        player
                );

                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }

            return ItemInteractionResult.SUCCESS;
        }

        if (!stack.is(CreateMechanicalDrive.ANGLE_GEAR_ITEM.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Direction side =
                hitResult.getDirection();

        AddGearResult result =
                tryAddGear(level, pos, state, side, stack, player);

        if (result == AddGearResult.PLACED) {
            return ItemInteractionResult.SUCCESS;
        }

        if (result == AddGearResult.BLOCKED) {
            return ItemInteractionResult.CONSUME_PARTIAL;
        }

        if (hasGearTowards(state, side)
                && tryPlaceAdjacentGear(
                        level,
                        pos,
                        side,
                        side.getOpposite(),
                        stack,
                        player,
                        hand
                )) {
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static boolean tryPlaceAdjacentGear(
            Level level,
            BlockPos pos,
            Direction clickedFace,
            Direction side,
            ItemStack stack,
            Player player,
            InteractionHand hand
    ) {
        BlockPos targetPos =
                pos.relative(clickedFace);

        BlockState targetState =
                level.getBlockState(targetPos);

        BlockHitResult targetHit =
                new BlockHitResult(
                        Vec3.atCenterOf(targetPos),
                        clickedFace,
                        targetPos,
                        false
                );
        BlockPlaceContext context =
                new BlockPlaceContext(
                        level,
                        player,
                        hand,
                        stack,
                        targetHit
                );

        if (!targetState.canBeReplaced(context)) {
            return false;
        }

        BlockState placedState =
                withGear(
                        CreateMechanicalDrive.ANGLE_GEAR.get()
                                .defaultBlockState()
                                .setValue(AXIS, side.getAxis())
                                .setValue(ENCASED, false),
                        side
                );

        if (!level.isClientSide) {
            level.setBlock(targetPos, placedState, 3);
            rebuildGearKinetics(level, targetPos);
            playGearPlaceSound(level, targetPos, placedState, player);

            if (player == null || !player.isCreative()) {
                stack.shrink(1);
            }
        }

        return true;
    }

    public static AddGearResult tryAddGear(
            Level level,
            BlockPos pos,
            BlockState state,
            Direction side,
            ItemStack stack,
            Player player
    ) {
        if (!state.is(CreateMechanicalDrive.ANGLE_GEAR.get())) {
            return AddGearResult.PASS;
        }

        if (state.getValue(ENCASED)) {
            return AddGearResult.PASS;
        }

        if (hasGearTowards(state, side)) {
            return AddGearResult.PASS;
        }

        BlockState addedState =
                withGear(state, side);

        if (!isValidGearSet(addedState)) {
            return AddGearResult.BLOCKED;
        }

        if (!level.isClientSide) {
            level.setBlock(pos, addedState, 3);
            rebuildGearKinetics(level, pos);
            playGearPlaceSound(level, pos, addedState, player);

            if (player == null || !player.isCreative()) {
                stack.shrink(1);
            }
        }

        return AddGearResult.PLACED;
    }

    private static void rebuildGearKinetics(
            Level level,
            BlockPos pos
    ) {
        if (level.getBlockEntity(pos) instanceof AngleGearBlockEntity gear) {
            gear.rebuildKinetics();
        }
    }

    private static void playGearPlaceSound(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        SoundType soundType =
                state.getSoundType(
                        level,
                        pos,
                        player
                );

        level.playSound(
                null,
                pos,
                soundType.getPlaceSound(),
                SoundSource.BLOCKS,
                (soundType.getVolume()
                        + 1.0F)
                        / 2.0F,
                soundType.getPitch()
                        * 0.8F
        );
    }

    private static void playCasingPlaceSound(
            Level level,
            BlockPos pos,
            Player player
    ) {
        SoundType soundType =
                AllBlocks.ANDESITE_CASING
                        .get()
                        .defaultBlockState()
                        .getSoundType(
                                level,
                                pos,
                                player
                        );

        level.playSound(
                null,
                pos,
                soundType.getPlaceSound(),
                SoundSource.BLOCKS,
                (soundType.getVolume()
                        + 1.0F)
                        / 2.0F,
                soundType.getPitch()
                        * 0.8F
        );
    }

    public static Direction getTargetedGearSide(
            BlockPlaceContext context
    ) {
        return context.getClickedFace()
                .getOpposite();
    }

    public static boolean hasGearTowards(
            BlockState state,
            Direction direction
    ) {
        BooleanProperty property =
                SIDE_PROPERTIES.get(direction);

        return property != null
                && state.hasProperty(property)
                && state.getValue(property);
    }

    private static BlockState withGear(
            BlockState state,
            Direction direction
    ) {
        return state.setValue(
                SIDE_PROPERTIES.get(direction),
                true
        );
    }

    private static BlockState withoutGear(
            BlockState state,
            Direction direction
    ) {
        return state.setValue(
                SIDE_PROPERTIES.get(direction),
                false
        );
    }

    public static int countGears(
            BlockState state
    ) {
        int count = 0;

        for (Direction direction : Direction.values()) {
            if (hasGearTowards(state, direction)) {
                count++;
            }
        }

        return count;
    }

    public static boolean isValidGearSet(
            BlockState state
    ) {
        int count =
                countGears(state);

        if (count <= 0 || count > MAX_GEARS) {
            return false;
        }

        for (Direction x : List.of(Direction.WEST, Direction.EAST)) {
            for (Direction y : List.of(Direction.DOWN, Direction.UP)) {
                for (Direction z : List.of(Direction.NORTH, Direction.SOUTH)) {
                    if (hasGearTowards(state, x)
                            && hasGearTowards(state, y)
                            && hasGearTowards(state, z)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        return hasGearTowards(state, direction);
    }

    @Override
    public Direction.Axis getRotationAxis(
            BlockState state
    ) {
        return state.getValue(AXIS);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        if (state.getValue(ENCASED)) {
            return FULL_BLOCK;
        }

        VoxelShape shape =
                Shapes.empty();

        for (Direction direction : Direction.values()) {
            if (hasGearTowards(state, direction)) {
                shape = Shapes.or(
                        shape,
                        SIDE_SHAPES.get(direction)
                );
            }
        }

        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getShape(
                state,
                level,
                pos,
                context
        );
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public InteractionResult onWrenched(
            BlockState state,
            UseOnContext context
    ) {
        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResult onSneakWrenched(
            BlockState state,
            UseOnContext context
    ) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos =
                context.getClickedPos();

        if (state.getValue(ENCASED)) {
            level.setBlock(
                    pos,
                    state.setValue(ENCASED, false),
                    3
            );

            giveRemovedCasing(
                    level,
                    pos,
                    context.getPlayer()
            );

            IWrenchable.playRemoveSound(
                    level,
                    pos
            );

            return InteractionResult.SUCCESS;
        }

        Direction side =
                getWrenchedGearSide(
                        state,
                        context
                );

        if (side == null) {
            return InteractionResult.SUCCESS;
        }

        BlockState removedState =
                withoutGear(
                        state,
                        side
                );

        if (countGears(removedState) <= 0) {
            level.destroyBlock(
                    pos,
                    false
            );
        } else {
            level.setBlock(
                    pos,
                    removedState.setValue(
                            AXIS,
                            getFirstGearAxis(removedState)
                    ),
                    3
            );
            rebuildGearKinetics(level, pos);
        }

        giveRemovedGear(
                level,
                pos,
                context.getPlayer()
        );

        IWrenchable.playRemoveSound(
                level,
                pos
        );

        return InteractionResult.SUCCESS;
    }

    private static Direction getWrenchedGearSide(
            BlockState state,
            UseOnContext context
    ) {
        Vec3 localHit =
                context.getClickLocation()
                        .subtract(
                                Vec3.atLowerCornerOf(
                                        context.getClickedPos()
                                )
                        );

        Direction bestSide =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        for (Direction direction : Direction.values()) {
            if (!hasGearTowards(state, direction)) {
                continue;
            }

            double distance =
                    localHit.distanceToSqr(
                            getGearSideCenter(direction)
                    );

            if (distance < bestDistance) {
                bestDistance = distance;
                bestSide = direction;
            }
        }

        return bestSide;
    }

    private static Vec3 getGearSideCenter(
            Direction direction
    ) {
        double x =
                0.5D;
        double y =
                0.5D;
        double z =
                0.5D;

        double sideOffset =
                direction.getAxisDirection() == Direction.AxisDirection.POSITIVE
                        ? 0.8125D
                        : 0.1875D;

        switch (direction.getAxis()) {
            case X -> x = sideOffset;
            case Y -> y = sideOffset;
            case Z -> z = sideOffset;
        }

        return new Vec3(
                x,
                y,
                z
        );
    }

    private static Direction.Axis getFirstGearAxis(
            BlockState state
    ) {
        for (Direction direction : Direction.values()) {
            if (hasGearTowards(state, direction)) {
                return direction.getAxis();
            }
        }

        return Direction.Axis.Z;
    }

    private static void giveRemovedGear(
            Level level,
            BlockPos pos,
            Player player
    ) {
        if (player != null && player.isCreative()) {
            return;
        }

        ItemStack stack =
                new ItemStack(
                        CreateMechanicalDrive.ANGLE_GEAR_ITEM.get()
                );

        if (player != null && player.addItem(stack)) {
            return;
        }

        Block.popResource(
                level,
                pos,
                stack
        );
    }

    private static void giveRemovedCasing(
            Level level,
            BlockPos pos,
            Player player
    ) {
        if (player != null && player.isCreative()) {
            return;
        }

        ItemStack stack =
                new ItemStack(
                        AllBlocks.ANDESITE_CASING.get()
                );

        if (player != null && player.addItem(stack)) {
            return;
        }

        Block.popResource(
                level,
                pos,
                stack
        );
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        ItemStack gears =
                new ItemStack(
                        CreateMechanicalDrive.ANGLE_GEAR_ITEM.get(),
                        countGears(state)
                );

        if (!state.getValue(ENCASED)) {
            return List.of(gears);
        }

        return List.of(
                gears,
                new ItemStack(
                        AllBlocks.ANDESITE_CASING.get()
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
        return new ItemStack(
                CreateMechanicalDrive.ANGLE_GEAR_ITEM.get()
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, SOUTH, WEST, EAST, DOWN, UP, ENCASED);
    }

    @Override
    public Class<AngleGearBlockEntity> getBlockEntityClass() {
        return AngleGearBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AngleGearBlockEntity> getBlockEntityType() {
        return CreateMechanicalDrive
                .ANGLE_GEAR_BLOCK_ENTITY
                .get();
    }
}
