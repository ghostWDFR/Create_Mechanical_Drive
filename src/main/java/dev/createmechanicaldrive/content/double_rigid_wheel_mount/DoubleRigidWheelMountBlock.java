package dev.createmechanicaldrive.content.double_rigid_wheel_mount;

import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StackRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StrictNbtStackRequirement;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.double_rigid_steering_wheel_mount.DoubleRigidSteeringWheelMountBlock;
import dev.createmechanicaldrive.content.double_rigid_steering_wheel_mount.DoubleRigidSteeringWheelMountBlockEntity;
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
 * A rigid two-sided wheel mount with one continuous wheel-drive shaft and no
 * steering shaft or configurable properties.
 */
public class DoubleRigidWheelMountBlock
        extends DoubleRigidSteeringWheelMountBlock {

    public DoubleRigidWheelMountBlock(Properties properties) {
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
    public BlockEntityType<? extends DoubleRigidSteeringWheelMountBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive
                .DOUBLE_RIGID_WHEEL_MOUNT_BLOCK_ENTITY
                .get();
    }

    @Override
    public ItemRequirement getRequiredItems(
            BlockState state,
            @Nullable BlockEntity blockEntity
    ) {
        ItemStack mount = CreateMechanicalDrive
                .DOUBLE_RIGID_WHEEL_MOUNT_ITEM
                .get()
                .getDefaultInstance();

        List<StackRequirement> requirements = new ArrayList<>();
        requirements.add(new StackRequirement(mount, ItemUseType.CONSUME));

        if (blockEntity instanceof DoubleRigidSteeringWheelMountBlockEntity wheelMount) {
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
