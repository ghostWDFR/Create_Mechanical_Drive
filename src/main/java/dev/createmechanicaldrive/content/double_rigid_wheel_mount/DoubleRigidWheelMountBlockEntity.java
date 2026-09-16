package dev.createmechanicaldrive.content.double_rigid_wheel_mount;

import dev.createmechanicaldrive.content.double_rigid_steering_wheel_mount.DoubleRigidSteeringWheelMountBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Reuses the stabilized two-wheel rigid contact solver while disabling every
 * steering input, setting and persisted steering value.
 */
public class DoubleRigidWheelMountBlockEntity
        extends DoubleRigidSteeringWheelMountBlockEntity {

    private static final List<String> STEERING_TAGS = List.of(
            "SteeringInputAngleStep",
            "SteeringInputSelectedRow",
            "SteeringTargetAngle",
            "SteeringAngle",
            "SteeringOutputSpeed",
            "NegativeSteeringInputAngleStep",
            "PositiveSteeringInputAngleStep"
    );

    public DoubleRigidWheelMountBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    @Override
    protected List<Direction> getSteeringInputAngleSides() {
        return List.of();
    }

    @Override
    public float getSteeringInputAngleDegrees() {
        return 0.0F;
    }

    @Override
    public boolean isSteeringShaftDirection(Direction direction) {
        return false;
    }

    @Override
    @Nullable
    protected Direction getSteeringInputSide() {
        return null;
    }

    @Override
    public double getLerpedYaw(float partialTick) {
        return 0.0D;
    }

    @Override
    public String getClipboardKey() {
        return "Double Rigid Wheel Mount";
    }

    @Override
    protected void write(
            CompoundTag tag,
            Provider registries,
            boolean clientPacket
    ) {
        super.write(tag, registries, clientPacket);
        removeSteeringTags(tag);
    }

    @Override
    protected void read(
            CompoundTag tag,
            Provider registries,
            boolean clientPacket
    ) {
        CompoundTag sanitizedTag = tag.copy();
        removeSteeringTags(sanitizedTag);
        super.read(sanitizedTag, registries, clientPacket);
    }

    private static void removeSteeringTags(CompoundTag tag) {
        for (String key : STEERING_TAGS) {
            tag.remove(key);
        }
    }
}
