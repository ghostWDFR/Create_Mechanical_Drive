package dev.createmechanicaldrive.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.createmechanicaldrive.content.double_rigid_steering_wheel_mount.DoubleRigidSteeringWheelMountBlockEntity;
import dev.createmechanicaldrive.content.double_steering_wheel_mount.DoubleSteeringWheelMountBlockEntity;
import dev.createmechanicaldrive.content.rigid_steering_wheel_mount.RigidSteeringWheelMountBlockEntity;
import dev.createmechanicaldrive.content.rigid_wheel_mount.RigidWheelMountBlockEntity;
import dev.createmechanicaldrive.content.steering_wheel_mount.SteeringWheelMountBlockEntity;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffset;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffsetState;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({
        SteeringWheelMountBlockEntity.class,
        RigidSteeringWheelMountBlockEntity.class,
        DoubleRigidSteeringWheelMountBlockEntity.class,
        RigidWheelMountBlockEntity.class
})
public abstract class MechanicalDriveWheelMountOffsetMixin
        implements WheelMountOffset {

    @Unique
    private final WheelMountOffsetState mechanicalDrive$wheelOffset =
            new WheelMountOffsetState();

    @Override
    public WheelMountOffsetState mechanicalDrive$getWheelOffsetState() {
        return mechanicalDrive$wheelOffset;
    }

    @Override
    public void mechanicalDrive$syncWheelOffset() {
        KineticBlockEntity blockEntity =
                (KineticBlockEntity) (Object) this;
        blockEntity.setChanged();
        Level level = blockEntity.getLevel();
        if (level != null && !level.isClientSide) {
            blockEntity.sendData();
        }
    }

    @Override
    public Direction mechanicalDrive$getOffsetFacing() {
        return ((KineticBlockEntity) (Object) this)
                .getBlockState()
                .getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public boolean mechanicalDrive$isDoubleSidedOffset() {
        Object target = this;
        return target instanceof DoubleSteeringWheelMountBlockEntity
                || target instanceof DoubleRigidSteeringWheelMountBlockEntity;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void mechanicalDrive$tickWheelOffset(CallbackInfo ci) {
        mechanicalDrive$wheelOffset.tick();
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void mechanicalDrive$writeWheelOffset(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket,
            CallbackInfo ci
    ) {
        mechanicalDrive$wheelOffset.write(tag);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void mechanicalDrive$readWheelOffset(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket,
            CallbackInfo ci
    ) {
        mechanicalDrive$wheelOffset.read(tag, clientPacket);
    }
}