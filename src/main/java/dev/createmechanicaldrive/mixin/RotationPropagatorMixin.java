package dev.createmechanicaldrive.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.createmechanicaldrive.content.gear_reducer.GearReducerKinetics;
import dev.createmechanicaldrive.content.worm_gears.WormGearSmallKinetics;
import dev.createmechanicaldrive.content.overrunning_clutch.OverrunningClutchBlock;
import dev.createmechanicaldrive.content.overrunning_clutch.OverrunningClutchBlockEntity;
import dev.createmechanicaldrive.content.rotary_limiter.RotaryLimiterBlockEntity;
import dev.createmechanicaldrive.content.steering_wheel_mount.SteeringWheelMountBlockEntity;
import dev.createmechanicaldrive.content.rigid_steering_wheel_mount.RigidSteeringWheelMountBlockEntity;
import dev.createmechanicaldrive.content.double_rigid_steering_wheel_mount.DoubleRigidSteeringWheelMountBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RotationPropagator.class)
public class RotationPropagatorMixin {

    @Unique
    private static final float createMechanicalDrive$EPSILON =
            0.001F;

    @Inject(
            method = "getRotationSpeedModifier",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void createMechanicalDrive$filterOverrunningClutch(
            KineticBlockEntity from,
            KineticBlockEntity to,
            CallbackInfoReturnable<Float> cir
    ) {
        float modifier =
                cir.getReturnValue();

        if (
                Math.abs(
                        modifier
                ) < createMechanicalDrive$EPSILON
        ) {
            return;
        }

        if (
                !createMechanicalDrive$isAllowedByOverrunningClutch(
                        from,
                        to,
                        modifier
                )
                        || GearReducerKinetics
                        .shouldSeparateReductionConnection(
                                from,
                                to
                        )
                        || WormGearSmallKinetics
                        .shareAdjacentWorm(
                                from,
                                to
                        )
                        || !createMechanicalDrive$isAllowedByRotaryLimiter(
                        from,
                        to
                )
                        || !createMechanicalDrive$isAllowedBySteeringWheelMount(
                        from,
                        to
                )
        ) {
            cir.setReturnValue(
                    0.0F
            );
        }
    }

    @Unique
    private static boolean createMechanicalDrive$isAllowedByRotaryLimiter(
            KineticBlockEntity from,
            KineticBlockEntity to
    ) {
        if (from instanceof RotaryLimiterBlockEntity limiter
                && limiter.isInputNeighbour(
                to.getBlockPos()
        )) {
            return false;
        }

        if (to instanceof RotaryLimiterBlockEntity limiter
                && limiter.isInputNeighbour(
                from.getBlockPos()
        )) {
            return false;
        }

        return true;
    }

    @Unique
    private static boolean createMechanicalDrive$isAllowedBySteeringWheelMount(
            KineticBlockEntity from,
            KineticBlockEntity to
    ) {
        if (from instanceof SteeringWheelMountBlockEntity mount
                && mount.isSteeringInputNeighbour(
                to.getBlockPos()
        )) {
            return false;
        }

        if (to instanceof SteeringWheelMountBlockEntity mount
                && mount.isSteeringInputNeighbour(
                from.getBlockPos()
        )) {
            return false;
        }

        if (from instanceof RigidSteeringWheelMountBlockEntity mount
                && mount.isSteeringInputNeighbour(
                to.getBlockPos()
        )) {
            return false;
        }

        if (to instanceof RigidSteeringWheelMountBlockEntity mount
                && mount.isSteeringInputNeighbour(
                from.getBlockPos()
        )) {
            return false;
        }

        if (from instanceof DoubleRigidSteeringWheelMountBlockEntity mount
                && mount.isSteeringInputNeighbour(
                to.getBlockPos()
        )) {
            return false;
        }

        if (to instanceof DoubleRigidSteeringWheelMountBlockEntity mount
                && mount.isSteeringInputNeighbour(
                from.getBlockPos()
        )) {
            return false;
        }

        return true;
    }

    @Unique
    private static boolean createMechanicalDrive$isAllowedByOverrunningClutch(
            KineticBlockEntity from,
            KineticBlockEntity to,
            float modifier
    ) {
        if (!(to instanceof OverrunningClutchBlockEntity clutch)) {
            return true;
        }

        return createMechanicalDrive$isClutchConnectionAllowed(
                clutch,
                from,
                from.getTheoreticalSpeed()
                        * modifier
        );
    }

    @Unique
    private static boolean createMechanicalDrive$isClutchConnectionAllowed(
            OverrunningClutchBlockEntity clutch,
            KineticBlockEntity other,
            float conveyedSpeed
    ) {
        if (
                !createMechanicalDrive$isDirectShaftConnection(
                        clutch,
                        other
                )
        ) {
            return true;
        }

        if (
                Math.abs(
                        conveyedSpeed
                ) < createMechanicalDrive$EPSILON
        ) {
            conveyedSpeed =
                    other.getTheoreticalSpeed();
        }

        return clutch.isSpeedAllowed(
                conveyedSpeed
        );
    }

    @Unique
    private static boolean createMechanicalDrive$isDirectShaftConnection(
            OverrunningClutchBlockEntity clutch,
            KineticBlockEntity other
    ) {
        BlockPos offset =
                other.getBlockPos()
                        .subtract(
                                clutch.getBlockPos()
                        );

        if (
                offset.distManhattan(
                        BlockPos.ZERO
                ) != 1
        ) {
            return false;
        }

        Direction direction =
                Direction.getNearest(
                        offset.getX(),
                        offset.getY(),
                        offset.getZ()
                );

        return direction.getAxis()
                == clutch.getBlockState()
                .getValue(
                        OverrunningClutchBlock.AXIS
                );
    }
}
