package dev.createmechanicaldrive.mixin;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import dev.createmechanicaldrive.content.gear_reducer.GearReducerKinetics;
import dev.createmechanicaldrive.content.steering_wheel.SteeringWheelBlockEntity;
import dev.createmechanicaldrive.content.steering_wheel_mount.SteeringWheelMountBlockEntity;
import dev.createmechanicaldrive.content.rigid_steering_wheel_mount.RigidSteeringWheelMountBlockEntity;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionSteeringBlockEntity;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionSteeringControlPassThrough;
import dev.createmechanicaldrive.content.worm_gears.WormGearRegularKinetics;
import dev.createmechanicaldrive.content.worm_gears.WormGearSmallKinetics;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KineticBlockEntity.class)
public abstract class KineticBlockEntityMixin
        implements TankTransmissionSteeringControlPassThrough {

    @Unique
    private boolean createMechanicalDrive$gearReducerVirtualOutput;

    @Unique
    private boolean createMechanicalDrive$tankSteeringControlPassThrough;

    @Unique
    private BlockPos createMechanicalDrive$tankSteeringControlPassThroughSource;

    @Override
    public boolean mechanicalDrive$isTankSteeringControlPassThrough() {
        return createMechanicalDrive$tankSteeringControlPassThrough;
    }

    @Override
    public BlockPos mechanicalDrive$getTankSteeringControlPassThroughSource() {
        return createMechanicalDrive$tankSteeringControlPassThroughSource;
    }

    @Inject(
            method = "getGeneratedSpeed",
            at = @At("HEAD"),
            cancellable = true
    )
    private void createMechanicalDrive$wormGeneratedSpeed(
            CallbackInfoReturnable<Float> cir
    ) {
        KineticBlockEntity self =
                (KineticBlockEntity) (Object) this;

        float speed = 0.0F;

        if (ICogWheel.isSmallCog(
                self.getBlockState()
        )) {
            speed =
                    WormGearRegularKinetics
                            .getOutputSpeed(self);
        } else if (ICogWheel.isLargeCog(
                self.getBlockState()
        )) {
            speed =
                    WormGearSmallKinetics
                            .getOutputSpeed(self);
        }

        if (speed == 0.0F) {
            speed =
                    GearReducerKinetics
                            .getOutputSpeed(self);
        }

        if (speed == 0.0F) {
            return;
        }

        cir.setReturnValue(speed);
    }

    @Inject(
            method = "calculateAddedStressCapacity",
            at = @At("HEAD"),
            cancellable = true
    )
    private void createMechanicalDrive$wormCapacity(
            CallbackInfoReturnable<Float> cir
    ) {
        KineticBlockEntity self =
                (KineticBlockEntity) (Object) this;

        if (WormGearRegularKinetics
                .isVirtualOutputCog(self)) {
            cir.setReturnValue(
                    WormGearRegularKinetics
                            .getOutputBaseCapacity(self)
            );

            return;
        }

        if (WormGearSmallKinetics
                .isVirtualOutputCog(self)) {
            cir.setReturnValue(
                    WormGearSmallKinetics
                            .getOutputBaseCapacity(self)
            );

            return;
        }

        if (GearReducerKinetics
                .isVirtualReductionOutput(self)) {
            cir.setReturnValue(
                    GearReducerKinetics
                            .getOutputBaseCapacity(self)
            );
        }
    }

    @Inject(
            method = "calculateStressApplied",
            at = @At("HEAD"),
            cancellable = true
    )
    private void createMechanicalDrive$wormBackdriveStress(
            CallbackInfoReturnable<Float> cir
    ) {
        KineticBlockEntity self =
                (KineticBlockEntity) (Object) this;

        if (
                !WormGearRegularKinetics
                        .isBackDrivenCog(self)
                        && !WormGearSmallKinetics
                        .isBackDrivenCog(self)
        ) {
            return;
        }

        cir.setReturnValue(
                Float.MAX_VALUE
        );
    }

    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private void createMechanicalDrive$updateWormOutput(
            CallbackInfo ci
    ) {
        KineticBlockEntity self =
                (KineticBlockEntity) (Object) this;

        if (self.getLevel() == null) {
            return;
        }

        if (self.getLevel().isClientSide) {
            return;
        }

        if (
                createMechanicalDrive$updateGearReducerOutput(
                        self
                )
        ) {
            return;
        }

        if (
                createMechanicalDrive$updateTankSteeringControlPassThrough(
                        self
                )
        ) {
            return;
        }

        if (
                !ICogWheel.isSmallCog(
                        self.getBlockState()
                )
                        && !ICogWheel.isLargeCog(
                        self.getBlockState()
                )
        ) {
            return;
        }

        if (
                WormGearRegularKinetics
                        .shouldBreakBackDrivenCog(self)
                        || WormGearSmallKinetics
                        .shouldBreakBackDrivenCog(self)
        ) {
            self.getLevel()
                    .destroyBlock(
                            self.getBlockPos(),
                            true
                    );

            return;
        }

        long ownNetworkId =
                self.getBlockPos()
                        .asLong();

        boolean ownsOutputNetwork =
                self.network != null
                        && self.network == ownNetworkId;

        float generatedSpeed;

        if (ICogWheel.isSmallCog(
                self.getBlockState()
        )) {
            generatedSpeed =
                    WormGearRegularKinetics
                            .getOutputSpeed(self);
        } else {
            generatedSpeed =
                    WormGearSmallKinetics
                            .getOutputSpeed(self);
        }

        if (
                generatedSpeed != 0.0F
                        && !self.hasSource()
        ) {
            if (!ownsOutputNetwork) {
                float previousSpeed =
                        self.getTheoreticalSpeed();

                if (self.hasNetwork()) {
                    self.setNetwork(null);
                }

                self.setSpeed(
                        generatedSpeed
                );

                self.setNetwork(
                        ownNetworkId
                );

                self.onSpeedChanged(
                        previousSpeed
                );

                self.attachKinetics();
                self.sendData();

                return;
            }

            if (
                    Math.abs(
                            self.getTheoreticalSpeed()
                                    - generatedSpeed
                    ) > 0.0001F
            ) {
                float previousSpeed =
                        self.getTheoreticalSpeed();

                self.detachKinetics();

                self.setSpeed(
                        generatedSpeed
                );

                if (self.hasNetwork()) {
                    self.getOrCreateNetwork()
                            .updateCapacityFor(
                                    self,
                                    self.calculateAddedStressCapacity()
                            );
                }

                self.onSpeedChanged(
                        previousSpeed
                );

                self.attachKinetics();
                self.sendData();

                return;
            }

            return;
        }

        if (
                ownsOutputNetwork
                        && !self.hasSource()
        ) {
            float previousSpeed =
                    self.getTheoreticalSpeed();

            self.detachKinetics();

            self.setSpeed(
                    0.0F
            );

            self.setNetwork(
                    null
            );

            self.onSpeedChanged(
                    previousSpeed
            );

            self.sendData();
        }
    }

    @Unique
    private boolean createMechanicalDrive$updateGearReducerOutput(
            KineticBlockEntity self
    ) {
        long ownNetworkId =
                self.getBlockPos()
                        .asLong();

        boolean ownsOutputNetwork =
                self.network != null
                        && self.network == ownNetworkId;

        float generatedSpeed =
                GearReducerKinetics
                        .getOutputSpeed(self);

        if (
                generatedSpeed != 0.0F
                        && !self.hasSource()
        ) {
            createMechanicalDrive$gearReducerVirtualOutput =
                    true;

            if (!ownsOutputNetwork) {
                float previousSpeed =
                        self.getTheoreticalSpeed();

                if (self.hasNetwork()) {
                    self.setNetwork(null);
                }

                self.setSpeed(
                        generatedSpeed
                );

                self.setNetwork(
                        ownNetworkId
                );

                self.onSpeedChanged(
                        previousSpeed
                );

                self.attachKinetics();

                if (self.hasNetwork()) {
                    KineticNetwork network =
                            self.getOrCreateNetwork();

                    network.updateCapacityFor(
                            self,
                            self.calculateAddedStressCapacity()
                    );

                    network.updateStressFor(
                            self,
                            self.calculateStressApplied()
                    );

                    network.updateCapacity();
                }

                self.sendData();

                return true;
            }

            if (
                    Math.abs(
                            self.getTheoreticalSpeed()
                                    - generatedSpeed
                    ) > 0.0001F
            ) {
                float previousSpeed =
                        self.getTheoreticalSpeed();

                self.detachKinetics();

                self.setSpeed(
                        generatedSpeed
                );

                self.onSpeedChanged(
                        previousSpeed
                );

                self.attachKinetics();

                if (self.hasNetwork()) {
                    KineticNetwork network =
                            self.getOrCreateNetwork();

                    network.updateCapacityFor(
                            self,
                            self.calculateAddedStressCapacity()
                    );

                    network.updateStressFor(
                            self,
                            self.calculateStressApplied()
                    );

                    network.updateCapacity();
                }

                self.sendData();

                return true;
            }

            return true;
        }

        if (
                createMechanicalDrive$gearReducerVirtualOutput
                        && ownsOutputNetwork
        ) {
            createMechanicalDrive$gearReducerVirtualOutput =
                    false;

            KineticNetwork oldNetwork =
                    self.hasNetwork()
                            ? self.getOrCreateNetwork()
                            : null;

            float previousSpeed =
                    self.getTheoreticalSpeed();

            boolean hasRealSource =
                    self.hasSource();

            self.detachKinetics();

            self.setNetwork(
                    null
            );

            if (!hasRealSource) {
                self.setSpeed(
                        0.0F
                );
            }

            createMechanicalDrive$refreshNetwork(
                    oldNetwork
            );

            self.onSpeedChanged(
                    previousSpeed
            );

            if (hasRealSource) {
                self.attachKinetics();

                if (self.hasNetwork()) {
                    createMechanicalDrive$refreshNetwork(
                            self.getOrCreateNetwork()
                    );
                }
            }

            self.sendData();

            return true;
        }

        return false;
    }

    @Unique
    private void createMechanicalDrive$refreshNetwork(
            KineticNetwork network
    ) {
        if (network == null) {
            return;
        }

        network.updateCapacity();
    }

    @Unique
    private boolean createMechanicalDrive$updateTankSteeringControlPassThrough(
            KineticBlockEntity self
    ) {
        long ownNetworkId =
                self.getBlockPos()
                        .asLong();

        boolean ownsOutputNetwork =
                self.network != null
                        && self.network == ownNetworkId;

        TankTransmissionSteeringBlockEntity.ControlPassThrough passThrough =
                TankTransmissionSteeringBlockEntity
                        .getControlPassThrough(self);

        SteeringWheelMountBlockEntity.SteeringPassThrough steeringMountPassThrough =
                SteeringWheelMountBlockEntity.getSteeringPassThrough(
                        self
                );

        RigidSteeringWheelMountBlockEntity.SteeringPassThrough rigidSteeringMountPassThrough =
                RigidSteeringWheelMountBlockEntity.getSteeringPassThrough(
                        self
                );

        boolean fromTankSteering =
                Math.abs(
                        passThrough.speed()
                ) > 0.001F;

        boolean fromSteeringWheelMount =
                Math.abs(
                        steeringMountPassThrough.speed()
                ) > 0.001F;

        boolean fromRigidSteeringWheelMount =
                Math.abs(
                        rigidSteeringMountPassThrough.speed()
                ) > 0.001F;

        float generatedSpeed;

        BlockPos passThroughSource;

        if (fromTankSteering) {
            generatedSpeed =
                    passThrough.speed();

            passThroughSource =
                    passThrough.sourceSteeringPos();

        } else if (fromSteeringWheelMount) {
            generatedSpeed =
                    steeringMountPassThrough.speed();

            passThroughSource =
                    steeringMountPassThrough.sourceMountPos();

        } else if (fromRigidSteeringWheelMount) {
            generatedSpeed =
                    rigidSteeringMountPassThrough.speed();

            passThroughSource =
                    rigidSteeringMountPassThrough.sourceMountPos();

        } else {
            generatedSpeed = 0.0F;
            passThroughSource = null;
        }

        if (
                generatedSpeed != 0.0F
                        && (
                        !self.hasSource()
                                || createMechanicalDrive$tankSteeringControlPassThrough
                )
                        && !self.isSource()
        ) {
            createMechanicalDrive$tankSteeringControlPassThrough =
                    true;

            createMechanicalDrive$tankSteeringControlPassThroughSource =
                    passThroughSource;

            if (!ownsOutputNetwork) {
                float previousSpeed =
                        self.getTheoreticalSpeed();

                if (self.hasNetwork()) {
                    self.setNetwork(null);
                }

                self.setSpeed(
                        generatedSpeed
                );

                self.setNetwork(
                        ownNetworkId
                );

                if (fromTankSteering) {
                    createMechanicalDrive$setPassThroughSourceIfDirectWheel(self, passThrough);
                }

                self.onSpeedChanged(
                        previousSpeed
                );

                self.attachKinetics();
                self.sendData();

                return true;
            }

            if (
                    Math.abs(
                            self.getTheoreticalSpeed()
                                    - generatedSpeed
                    ) > 0.0001F
            ) {
                float previousSpeed =
                        self.getTheoreticalSpeed();

                self.detachKinetics();

                self.setSpeed(
                        generatedSpeed
                );

                if (fromTankSteering) {
                    createMechanicalDrive$setPassThroughSourceIfDirectWheel(self, passThrough);
                }

                self.onSpeedChanged(
                        previousSpeed
                );

                self.attachKinetics();
                self.sendData();

                return true;
            }

            return true;
        }

        if (
                createMechanicalDrive$tankSteeringControlPassThrough
                        && ownsOutputNetwork
                        && !self.isSource()
        ) {
            createMechanicalDrive$tankSteeringControlPassThrough =
                    false;

            createMechanicalDrive$tankSteeringControlPassThroughSource =
                    null;

            float previousSpeed =
                    self.getTheoreticalSpeed();

            self.detachKinetics();

            self.setSpeed(
                    0.0F
            );

            self.setNetwork(
                    null
            );

            if (self.hasSource()) {
                self.removeSource();
            }

            self.onSpeedChanged(
                    previousSpeed
            );

            self.sendData();

            return true;
        }

        if (self.isSource()) {
            createMechanicalDrive$tankSteeringControlPassThrough =
                    false;

            createMechanicalDrive$tankSteeringControlPassThroughSource =
                    null;
        }

        return false;
    }

    @Unique
    private void createMechanicalDrive$setPassThroughSourceIfDirectWheel(
            KineticBlockEntity self,
            TankTransmissionSteeringBlockEntity.ControlPassThrough passThrough
    ) {
        if (!(self instanceof SteeringWheelBlockEntity)) {
            return;
        }

        if (passThrough.sourceSteeringPos() == null) {
            return;
        }

        if (!self.hasSource()
                || !passThrough.sourceSteeringPos().equals(
                self.source
        )) {
            self.setSource(
                    passThrough.sourceSteeringPos()
            );
        }
    }
}
