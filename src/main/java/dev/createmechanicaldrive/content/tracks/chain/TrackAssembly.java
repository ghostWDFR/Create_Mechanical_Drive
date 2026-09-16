package dev.createmechanicaldrive.content.tracks.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record TrackAssembly(
        TrackType type,
        List<BlockPos> nodes,
        int linkCount
) {
    private static final String TYPE_TAG = "Type";
    private static final String NODES_TAG = "Nodes";
    private static final String LINK_COUNT_TAG = "LinkCount";

    public TrackAssembly {
        nodes = List.copyOf(nodes);
    }

    public boolean contains(BlockPos pos) {
        return nodes.contains(pos);
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TYPE_TAG, type.serializedName());
        tag.putLongArray(
                NODES_TAG,
                nodes.stream().mapToLong(BlockPos::asLong).toArray()
        );
        tag.putInt(LINK_COUNT_TAG, linkCount);
        return tag;
    }

    @Nullable
    public static TrackAssembly read(CompoundTag tag) {
        if (!tag.contains(TYPE_TAG, Tag.TAG_STRING)
                || !tag.contains(NODES_TAG, Tag.TAG_LONG_ARRAY)
                || !tag.contains(LINK_COUNT_TAG, Tag.TAG_ANY_NUMERIC)) {
            return null;
        }

        TrackType type = TrackType.fromSerializedName(tag.getString(TYPE_TAG));
        long[] packedNodes = tag.getLongArray(NODES_TAG);
        int linkCount = tag.getInt(LINK_COUNT_TAG);
        if (type == null || packedNodes.length < 2 || linkCount < 1) {
            return null;
        }

        List<BlockPos> nodes = new ArrayList<>(packedNodes.length);
        for (long packedNode : packedNodes) {
            nodes.add(BlockPos.of(packedNode));
        }
        return new TrackAssembly(type, nodes, linkCount);
    }
}
