package dev.createmechanicaldrive.content.stirling_engine;

import com.simibubi.create.content.kinetics.base.IRotate;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class StirlingEngineFlywheelKinetics {

    public static final float BONUS_SU_PER_FLYWHEEL =
            512.0F;

    public static final float EXCESS_COST_SU_PER_FLYWHEEL =
            256.0F;

    public static final int MAX_EFFECTIVE_FLYWHEELS =
            4;

    private static final int MAX_SCAN_DISTANCE =
            128;

    private StirlingEngineFlywheelKinetics() {
    }

    public static List<StirlingEngineFlywheelBlockEntity>
    getFlywheelsOnShaft(
            StirlingEngineFlywheelBlockEntity origin
    ) {
        List<StirlingEngineFlywheelBlockEntity> result =
                new ArrayList<>();

        Level level =
                origin.getLevel();

        if (level == null) {
            result.add(origin);
            return result;
        }

        result.add(origin);

        Direction.Axis axis =
                origin.getAxis();

        collect(
                level,
                origin.getBlockPos(),
                Direction.get(
                        Direction.AxisDirection.POSITIVE,
                        axis
                ),
                result
        );

        collect(
                level,
                origin.getBlockPos(),
                Direction.get(
                        Direction.AxisDirection.NEGATIVE,
                        axis
                ),
                result
        );

        return result;
    }

    private static void collect(
            Level level,
            BlockPos origin,
            Direction direction,
            List<StirlingEngineFlywheelBlockEntity> result
    ) {
        BlockPos pos =
                origin.relative(
                        direction
                );

        for (
                int distance = 0;
                distance < MAX_SCAN_DISTANCE;
                distance++
        ) {
            BlockState state =
                    level.getBlockState(
                            pos
                    );

            if (!(
                    state.getBlock()
                            instanceof IRotate rotate
            )) {
                break;
            }

            if (!rotate.hasShaftTowards(
                    level,
                    pos,
                    state,
                    direction.getOpposite()
            )) {
                break;
            }

            if (
                    state.getBlock()
                            == CreateMechanicalDrive
                            .STIRLING_ENGINE_FLYWHEEL
                            .get()
            ) {
                BlockEntity blockEntity =
                        level.getBlockEntity(
                                pos
                        );

                if (
                        blockEntity
                                instanceof StirlingEngineFlywheelBlockEntity flywheel
                ) {
                    result.add(
                            flywheel
                    );
                }
            }

            if (!rotate.hasShaftTowards(
                    level,
                    pos,
                    state,
                    direction
            )) {
                break;
            }

            pos =
                    pos.relative(
                            direction
                    );
        }
    }
}