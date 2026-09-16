package dev.createmechanicaldrive.content.steering_wheel;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticEffectHandler;

final class NoParticleKineticEffectHandler
        extends KineticEffectHandler {

    NoParticleKineticEffectHandler(
            KineticBlockEntity blockEntity
    ) {
        super(blockEntity);
    }

    @Override
    public void spawnRotationIndicators() {
    }
}
