package dev.createmechanicaldrive.content.stirling_engine;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractShaftBlock;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class StirlingEngineFlywheelBlock
        extends AbstractShaftBlock
        implements IWrenchable {

    private static final VoxelShape SHAPE_X =
            box(
                    0.0D,
                    0.0D,
                    0.0D,
                    16.0D,
                    16.0D,
                    16.0D
            );

    private static final VoxelShape SHAPE_Y =
            box(
                    0.0D,
                    0.0D,
                    0.0D,
                    16.0D,
                    16.0D,
                    16.0D
            );

    private static final VoxelShape SHAPE_Z =
            box(
                    0.0D,
                    0.0D,
                    0.0D,
                    16.0D,
                    16.0D,
                    16.0D
            );

    public StirlingEngineFlywheelBlock(
            Properties properties
    ) {
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of(
                new ItemStack(
                        CreateMechanicalDrive
                                .STIRLING_ENGINE_FLYWHEEL_ITEM
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
        return new ItemStack(
                CreateMechanicalDrive
                        .STIRLING_ENGINE_FLYWHEEL_ITEM
                        .get()
        );
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Class<KineticBlockEntity> getBlockEntityClass() {
        return (Class) StirlingEngineFlywheelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return CreateMechanicalDrive.STIRLING_ENGINE_FLYWHEEL_BLOCK_ENTITY.get();
    }
}