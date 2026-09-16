package dev.createmechanicaldrive.content.worm_gears;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class WormGearRegularKinetics {

    public static final float FIRST_STAGE_MULTIPLIER = 4.0F;
    public static final float SECOND_STAGE_MULTIPLIER = 2.0F;
    public static final float THIRD_STAGE_MULTIPLIER = 1.5F;
    public static final float LATER_STAGE_MULTIPLIER = 1.0F;

    public static final float BACKDRIVE_BREAK_THRESHOLD = 1024.0F;

    private WormGearRegularKinetics() {
    }

    public static WormGearRegularBlockEntity
    findAdjacentWorm(
            KineticBlockEntity cog
    ) {
        Level level =
                cog.getLevel();

        if (level == null) {
            return null;
        }

        if (!ICogWheel.isSmallCog(
                cog.getBlockState()
        )) {
            return null;
        }

        BlockPos cogPos =
                cog.getBlockPos();

        for (Direction direction : Direction.values()) {
            BlockPos wormPos =
                    cogPos.relative(direction);

            BlockEntity blockEntity =
                    level.getBlockEntity(wormPos);

            if (!(
                    blockEntity
                            instanceof WormGearRegularBlockEntity worm
            )) {
                continue;
            }

            BlockPos diff =
                    cogPos.subtract(wormPos);

            if (!WormGearRegularBlockEntity
                    .isValidWormCogConnection(
                            worm.getBlockState(),
                            cog.getBlockState(),
                            diff
                    )) {
                continue;
            }

            return worm;
        }

        return null;
    }

    public static WormGearRegularBlockEntity findDrivingWorm(KineticBlockEntity cog)
    {
        if (cog.hasSource()) {
            return null;
        }

        Level level =
                cog.getLevel();

        if (level == null) {
            return null;
        }

        if (!ICogWheel.isSmallCog(
                cog.getBlockState()
        )) {
            return null;
        }

        BlockPos cogPos =
                cog.getBlockPos();

        for (Direction direction : Direction.values()) {
            BlockPos wormPos =
                    cogPos.relative(direction);

            BlockEntity blockEntity =
                    level.getBlockEntity(wormPos);

            if (!(
                    blockEntity
                            instanceof WormGearRegularBlockEntity worm
            )) {
                continue;
            }

            BlockPos diff =
                    cogPos.subtract(wormPos);

            if (!WormGearRegularBlockEntity
                    .isValidWormCogConnection(
                            worm.getBlockState(),
                            cog.getBlockState(),
                            diff
                    )) {
                continue;
            }

            if (!worm.hasSource()) {
                continue;
            }

            if (worm.getSpeed() == 0.0F) {
                continue;
            }

            return worm;
        }

        return null;
    }

    public static boolean isVirtualOutputCog(
            KineticBlockEntity cog
    ) {
        return findDrivingWorm(cog) != null;
    }

    public static boolean isBackDrivenCog(
            KineticBlockEntity cog
    ) {
        if (!cog.hasSource()) {
            return false;
        }

        return findAdjacentWorm(cog) != null;
    }

    public static float getBackdriveCapacity(
            KineticBlockEntity cog
    ) {
        if (!isBackDrivenCog(cog)) {
            return 0.0F;
        }

        if (!cog.hasNetwork()) {
            return 0.0F;
        }

        return cog.getOrCreateNetwork()
                .calculateCapacity();
    }

    public static boolean shouldBreakBackDrivenCog(
            KineticBlockEntity cog
    ) {
        return getBackdriveCapacity(cog)
                > BACKDRIVE_BREAK_THRESHOLD;
    }

    public static float getOutputSpeed(
            KineticBlockEntity cog
    ) {
        WormGearRegularBlockEntity worm =
                findDrivingWorm(cog);

        if (worm == null) {
            return 0.0F;
        }

        float outputSpeed =
                worm.getOutputSpeedFor(cog);

        float maxSpeed =
                AllConfigs.server()
                        .kinetics
                        .maxRotationSpeed
                        .get();

        return Math.copySign(
                Math.min(
                        Math.abs(outputSpeed),
                        maxSpeed
                ),
                outputSpeed
        );
    }

    public static boolean isWormOutputNetwork(
            KineticBlockEntity kinetic
    ) {
        if (kinetic.getLevel() == null) {
            return false;
        }

        if (kinetic.network == null) {
            return false;
        }

        BlockPos rootPos =
                BlockPos.of(
                        kinetic.network
                );

        if (!(
                kinetic.getLevel()
                        .getBlockEntity(rootPos)
                        instanceof KineticBlockEntity root
        )) {
            return false;
        }

        return isVirtualOutputCog(root);
    }

    public static int getWormStageDepth(
            KineticBlockEntity kinetic
    ) {
        if (kinetic.getLevel() == null) {
            return 0;
        }

        KineticBlockEntity current =
                kinetic;

        int depth = 0;

        for (int i = 0; i < 32; i++) {
            if (current.network == null) {
                break;
            }

            BlockPos rootPos =
                    BlockPos.of(
                            current.network
                    );

            if (!(
                    current.getLevel()
                            .getBlockEntity(rootPos)
                            instanceof KineticBlockEntity root
            )) {
                break;
            }

            WormGearRegularBlockEntity drivingWorm =
                    findDrivingWorm(root);

            if (drivingWorm == null) {
                break;
            }

            depth++;

            if (!drivingWorm.hasNetwork()) {
                break;
            }

            BlockPos upstreamRootPos =
                    BlockPos.of(
                            drivingWorm.network
                    );

            if (!(
                    current.getLevel()
                            .getBlockEntity(upstreamRootPos)
                            instanceof KineticBlockEntity upstreamRoot
            )) {
                break;
            }

            if (upstreamRoot == root) {
                break;
            }

            current =
                    upstreamRoot;
        }

        return depth;
    }

    public static float getCapacityMultiplierForStage(
            int stage
    ) {
        return switch (stage) {
            case 1 -> FIRST_STAGE_MULTIPLIER;
            case 2 -> SECOND_STAGE_MULTIPLIER;
            case 3 -> THIRD_STAGE_MULTIPLIER;
            default -> LATER_STAGE_MULTIPLIER;
        };
    }

    public static float getOutputActualCapacity(
            KineticBlockEntity cog
    ) {
        WormGearRegularBlockEntity worm =
                findDrivingWorm(cog);

        if (worm == null) {
            return 0.0F;
        }

        if (!worm.hasNetwork()) {
            return 0.0F;
        }

        float inputCapacity =
                worm.getOrCreateNetwork()
                        .calculateCapacity();

        int upstreamDepth =
                getWormStageDepth(worm);

        int outputStage =
                upstreamDepth + 1;

        float multiplier =
                getCapacityMultiplierForStage(
                        outputStage
                );

        return inputCapacity
                * multiplier;
    }

    public static float getOutputBaseCapacity(
            KineticBlockEntity cog
    ) {
        float speed =
                Math.abs(
                        getOutputSpeed(cog)
                );

        if (speed == 0.0F) {
            return 0.0F;
        }

        return getOutputActualCapacity(cog)
                / speed;
    }
}