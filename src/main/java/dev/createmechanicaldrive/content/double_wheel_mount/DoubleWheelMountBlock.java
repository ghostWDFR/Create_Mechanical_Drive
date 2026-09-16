package dev.createmechanicaldrive.content.double_wheel_mount;

import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StackRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StrictNbtStackRequirement;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.double_steering_wheel_mount.DoubleSteeringWheelMountBlock;
import dev.createmechanicaldrive.content.double_steering_wheel_mount.DoubleSteeringWheelMountBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A two-sided wheel mount whose wheel drive shaft passes straight through the
 * block. Unlike {@link DoubleSteeringWheelMountBlock}, it has no steering
 * input shaft.
 */
public class DoubleWheelMountBlock extends DoubleSteeringWheelMountBlock {

    public DoubleWheelMountBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction face
    ) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public BlockEntityType<? extends DoubleSteeringWheelMountBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive
                .DOUBLE_WHEEL_MOUNT_BLOCK_ENTITY
                .get();
    }

    @Override
    public ItemRequirement getRequiredItems(
            BlockState state,
            @Nullable BlockEntity blockEntity
    ) {
        ItemStack mount = CreateMechanicalDrive
                .DOUBLE_WHEEL_MOUNT_ITEM
                .get()
                .getDefaultInstance();

        List<StackRequirement> requirements = new ArrayList<>();
        requirements.add(new StackRequirement(mount, ItemUseType.CONSUME));

        if (blockEntity instanceof DoubleSteeringWheelMountBlockEntity wheelMount) {
            for (int slot = 0; slot < 2; slot++) {
                ItemStack wheel = wheelMount.getWheel(slot);
                if (!wheel.isEmpty()) {
                    requirements.add(new StrictNbtStackRequirement(
                            wheel,
                            ItemUseType.CONSUME
                    ));
                }
            }
        }

        return new ItemRequirement(requirements);
    }
}
