package dev.createmechanicaldrive.content.tracks.chain;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.tracks.mounts.sprocket.SprocketMountBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class TrackAssemblyManager {
    private TrackAssemblyManager() {
    }

    @Nullable
    public static SprocketMountBlockEntity owner(
            Level level,
            TrackLinkedWheel wheel
    ) {
        if (!(wheel instanceof BlockEntity wheelBlockEntity)) {
            return null;
        }
        BlockPos ownerPos = wheel.mechanicalDrive$getTrackSprocket();
        if (ownerPos == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(ownerPos);
        if (!(blockEntity instanceof SprocketMountBlockEntity sprocket)) {
            return null;
        }
        TrackAssembly assembly = sprocket.getTrackAssembly();
        return assembly != null && assembly.contains(
                wheelBlockEntity.getBlockPos()
        ) ? sprocket : null;
    }

    public static void disassembleForWheel(TrackLinkedWheel wheel) {
        if (!(wheel instanceof BlockEntity blockEntity)) {
            return;
        }
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        SprocketMountBlockEntity owner = owner(level, wheel);
        if (owner != null) {
            disassemble(owner, true);
        } else {
            wheel.mechanicalDrive$setTrackSprocket(null);
        }
    }

    public static void disassemble(
            SprocketMountBlockEntity sprocket,
            boolean refundLinks
    ) {
        Level level = sprocket.getLevel();
        TrackAssembly assembly = sprocket.getTrackAssembly();
        if (level == null || level.isClientSide || assembly == null) {
            return;
        }

        for (BlockPos nodePos : assembly.nodes()) {
            if (nodePos.equals(sprocket.getBlockPos())) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(nodePos);
            if (blockEntity instanceof TrackLinkedWheel linked
                    && sprocket.getBlockPos().equals(
                    linked.mechanicalDrive$getTrackSprocket()
            )) {
                linked.mechanicalDrive$setTrackSprocket(null);
            }
        }

        sprocket.setTrackAssembly(null);
        if (!refundLinks) {
            return;
        }

        ItemStack links = new ItemStack(
                assembly.type() == TrackType.NARROW
                        ? CreateMechanicalDrive.NARROW_TRACK_LINK_ITEM.get()
                        : CreateMechanicalDrive.WIDE_TRACK_LINK_ITEM.get(),
                assembly.linkCount()
        );
        Containers.dropItemStack(
                level,
                sprocket.getBlockPos().getX() + 0.5D,
                sprocket.getBlockPos().getY() + 0.5D,
                sprocket.getBlockPos().getZ() + 0.5D,
                links
        );
    }
}
