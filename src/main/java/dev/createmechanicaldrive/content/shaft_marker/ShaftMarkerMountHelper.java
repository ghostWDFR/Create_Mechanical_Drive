package dev.createmechanicaldrive.content.shaft_marker;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class ShaftMarkerMountHelper {
    private ShaftMarkerMountHelper() {
    }

    public static double approachDrivenAngularVelocity(
            KineticBlockEntity mount,
            Direction wheelSide,
            double currentVelocity
    ) {
        Level level = mount.getLevel();
        if (level == null) {
            return 0.0D;
        }

        float speed = wheelSide.getAxis() == Direction.Axis.X
                ? -mount.getSpeed()
                : mount.getSpeed();
        double targetVelocity = speed
                * Math.PI
                * 2.0D
                / 60.0D
                / 20.0D
                * (15 - level.getSignal(
                mount.getBlockPos().above(),
                Direction.UP
        ))
                / 15.0D;

        return Mth.lerp(
                0.2D,
                currentVelocity,
                targetVelocity
        );
    }
}
