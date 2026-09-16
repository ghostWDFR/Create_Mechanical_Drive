package dev.createmechanicaldrive.content.tracks.chain;

import dev.createmechanicaldrive.content.tracks.mounts.idler.IdlerMountBlock;
import dev.createmechanicaldrive.content.tracks.mounts.idler.IdlerMountBlockEntity;
import dev.createmechanicaldrive.content.tracks.mounts.sprocket.SprocketMountBlock;
import dev.createmechanicaldrive.content.tracks.mounts.sprocket.SprocketMountBlockEntity;
import dev.createmechanicaldrive.content.tracks.mounts.torsion.TorsionMountBlock;
import dev.createmechanicaldrive.content.tracks.mounts.torsion.TorsionMountBlockEntity;
import dev.createmechanicaldrive.content.tracks.mounts.torsion.TorsionMountAttachments;
import dev.createmechanicaldrive.content.tracks.wheels.idler.IdlerWheelItem;
import dev.createmechanicaldrive.content.tracks.wheels.sprocket.SprocketWheelItem;
import dev.createmechanicaldrive.content.tracks.wheels.support.SupportWheelItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class TrackWheelLookup {
    private TrackWheelLookup() {
    }

    @Nullable
    public static WheelNode find(
            Level level,
            BlockPos pos,
            float partialTick
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SprocketMountBlockEntity sprocket
                && SprocketWheelItem.isSprocketWheel(
                sprocket.getAttachment()
        )) {
            Direction facing = sprocket.getBlockState().getValue(
                    SprocketMountBlock.HORIZONTAL_FACING
            );
            return new WheelNode(
                    pos.getCenter(),
                    SprocketWheelItem.RADIUS,
                    facing,
                    Kind.SPROCKET
            );
        }
        if (blockEntity instanceof TorsionMountBlockEntity torsion
                && TorsionMountAttachments.hasSuspensionWheel(
                torsion.getAttachment()
        )) {
            Direction facing = torsion.getBlockState().getValue(
                    TorsionMountBlock.HORIZONTAL_FACING
            );
            return new WheelNode(
                    torsion.getTrackWheelCenter(partialTick),
                    TorsionMountAttachments.wheelRadius(
                            torsion.getAttachment()
                    ),
                    facing,
                    Kind.DRIVE
            );
        }
        if (blockEntity instanceof IdlerMountBlockEntity idler
                && IdlerWheelItem.isIdlerWheel(idler.getAttachment())) {
            Direction facing = idler.getBlockState().getValue(
                    IdlerMountBlock.HORIZONTAL_FACING
            );
            return new WheelNode(
                    idler.getTrackWheelCenter(),
                    IdlerWheelItem.RADIUS,
                    facing,
                    Kind.IDLER
            );
        }
        return null;
    }

    @Nullable
    public static WheelNode findSupportWheel(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TorsionMountBlockEntity torsion
                && SupportWheelItem.isSupportWheel(
                torsion.getSupportWheel()
        )) {
            Direction facing = torsion.getBlockState().getValue(
                    TorsionMountBlock.HORIZONTAL_FACING
            );
            return new WheelNode(
                    torsion.getSupportWheelCenter(),
                    SupportWheelItem.RADIUS,
                    facing,
                    Kind.SUPPORT
            );
        }
        return null;
    }

    public enum Kind {
        SPROCKET,
        DRIVE,
        IDLER,
        SUPPORT
    }

    public record WheelNode(
            Vec3 center,
            double radius,
            Direction facing,
            Kind kind
    ) {
    }
}
