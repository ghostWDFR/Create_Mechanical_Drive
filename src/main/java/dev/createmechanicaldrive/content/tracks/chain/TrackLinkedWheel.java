package dev.createmechanicaldrive.content.tracks.chain;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public interface TrackLinkedWheel {
    @Nullable
    BlockPos mechanicalDrive$getTrackSprocket();

    void mechanicalDrive$setTrackSprocket(@Nullable BlockPos sprocketPos);
}
