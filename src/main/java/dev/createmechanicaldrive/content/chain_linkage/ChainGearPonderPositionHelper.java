package dev.createmechanicaldrive.content.chain_linkage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ChainGearPonderPositionHelper {
    private ChainGearPonderPositionHelper() {
    }

    public static boolean shouldRenderFlexiblePonderChain(
            Level level,
            List<BlockPos> loop,
            float partialTicks
    ) {
        if (level == null
                || loop.size() != 2) {
            return false;
        }

        for (BlockPos pos : loop) {
            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            if (blockEntity instanceof ChainGearBlockEntity gear
                    && gear.hasPonderFlexibleChainRender()) {
                return true;
            }
        }

        return hasRenderOffset(
                level,
                loop.getFirst(),
                partialTicks
        ) || hasRenderOffset(
                level,
                loop.getLast(),
                partialTicks
        );
    }

    public static Vec3 renderCenter(
            Level level,
            BlockPos pos,
            float partialTicks
    ) {
        return Vec3.atCenterOf(pos)
                .add(
                        renderOffset(
                                level,
                                pos,
                                partialTicks
                        )
                );
    }

    public static Vec3 renderOffset(
            Level level,
            BlockPos pos,
            float partialTicks
    ) {
        if (level == null) {
            return Vec3.ZERO;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        return blockEntity instanceof ChainGearBlockEntity gear
                ? gear.getPonderRenderOffset(partialTicks)
                : Vec3.ZERO;
    }

    public static Vec3 axisVector(
            Direction.Axis axis
    ) {
        return switch (axis) {
            case X -> new Vec3(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3(0.0D, 0.0D, 1.0D);
        };
    }

    private static boolean hasRenderOffset(
            Level level,
            BlockPos pos,
            float partialTicks
    ) {
        return renderOffset(
                level,
                pos,
                partialTicks
        ).lengthSqr() > 1.0E-6D;
    }
}