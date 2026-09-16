package dev.createmechanicaldrive.content.double_wheel_mount;

import dev.createmechanicaldrive.content.double_steering_wheel_mount.DoubleSteeringWheelMountBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Reuses the complete two-wheel suspension implementation while explicitly
 * disabling every steering input and steering setting.
 */
public class DoubleWheelMountBlockEntity
        extends DoubleSteeringWheelMountBlockEntity {

    private static final List<String> STEERING_TAGS = List.of(
            "SteeringInputAngleStep",
            "SteeringInputSelectedRow",
            "SteeringTargetAngle",
            "SteeringAngle",
            "SteeringOutputSpeed",
            "NegativeSteeringInputAngleStep",
            "PositiveSteeringInputAngleStep"
    );

    public DoubleWheelMountBlockEntity(
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
    public boolean isSteeringShaftDirection(Direction direction) {
        return false;
    }

    @Override
    protected boolean isSteeringInputAngleSideActive(Direction direction) {
        return false;
    }

    @Override
    @Nullable
    protected Direction getSteeringInputSide() {
        return null;
    }

    @Override
    public boolean isSteeringPassThroughEnabled() {
        return false;
    }

    @Override
    public double getLerpedYaw(float partialTick) {
        return 0.0D;
    }

    @Override
    public String getClipboardKey() {
        return "Double Wheel Mount";
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
