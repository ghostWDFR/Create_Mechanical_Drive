package dev.createmechanicaldrive.mixin;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.createmechanicaldrive.content.gear_reducer.GearReducerBlockEntity;
import dev.createmechanicaldrive.content.gear_reducer.GearReducerKinetics;
import dev.createmechanicaldrive.content.worm_gears.WormGearRegularBlockEntity;
import dev.createmechanicaldrive.content.worm_gears.WormGearRegularKinetics;
import dev.createmechanicaldrive.content.worm_gears.WormGearSmallBlockEntity;
import dev.createmechanicaldrive.content.worm_gears.WormGearSmallKinetics;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineFlywheelBlockEntity;
import dev.createmechanicaldrive.content.stirling_engine.StirlingEngineFlywheelKinetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(KineticNetwork.class)
public class KineticNetworkMixin {

    @Shadow
    public Map<KineticBlockEntity, Float> sources;

    @Shadow
    public Map<KineticBlockEntity, Float> members;

    @Shadow
    private float unloadedCapacity;

    @Shadow
    private float unloadedStress;

    @Shadow
    private int unloadedMembers;

    @Unique
    private boolean createMechanicalDrive$syncingWormOutputs;

    @Unique
    private float createMechanicalDrive$reconciledFlywheelCapacity;

    @Unique
    private float createMechanicalDrive$reconciledFlywheelStress;

    @Unique
    private boolean createMechanicalDrive$hasLoadedFlywheel;

    @Inject(
            method = "getActualCapacityOf",
            at = @At("HEAD"),
            cancellable = true
    )
    private void createMechanicalDrive$getWormOutputCapacity(
            KineticBlockEntity be,
            CallbackInfoReturnable<Float> cir
    ) {
        if (WormGearRegularKinetics
                .isVirtualOutputCog(be)) {
            cir.setReturnValue(
                    WormGearRegularKinetics
                            .getOutputActualCapacity(be)
            );

            return;
        }

        if (WormGearSmallKinetics
                .isVirtualOutputCog(be)) {
            cir.setReturnValue(
                    WormGearSmallKinetics
                            .getOutputActualCapacity(be)
            );

            return;
        }

        if (GearReducerKinetics
                .isVirtualReductionOutput(be)) {
            cir.setReturnValue(
                    GearReducerKinetics
                            .getOutputActualCapacity(be)
            );
        }
    }

    @Inject(
            method = "calculateCapacity",
            at = @At("RETURN"),
            cancellable = true
    )
    private void createMechanicalDrive$addFlywheelCapacity(
            CallbackInfoReturnable<Float> cir
    ) {
        cir.setReturnValue(
                cir.getReturnValue()
                        + createMechanicalDrive$calculateFlywheelCapacity()
        );
    }

    @Inject(
            method = "calculateStress",
            at = @At("RETURN"),
            cancellable = true
    )
    private void createMechanicalDrive$addFlywheelStress(
            CallbackInfoReturnable<Float> cir
    ) {
        cir.setReturnValue(
                cir.getReturnValue()
                        + createMechanicalDrive$calculateFlywheelStress()
        );
    }

    @Inject(
            method = "addSilently",
            at = @At("RETURN")
    )
    private void createMechanicalDrive$reconcileLoadedFlywheelCapacity(
            KineticBlockEntity blockEntity,
            float lastCapacity,
            float lastStress,
            CallbackInfo ci
    ) {
        if (
                blockEntity
                        instanceof StirlingEngineFlywheelBlockEntity
        ) {
            createMechanicalDrive$hasLoadedFlywheel =
                    true;
        }

        float flywheelCapacity =
                createMechanicalDrive$calculateFlywheelCapacity();

        float flywheelStress =
                createMechanicalDrive$calculateFlywheelStress();

        unloadedCapacity =
                Math.max(
                        0.0F,
                        unloadedCapacity
                                - (flywheelCapacity
                                - createMechanicalDrive$reconciledFlywheelCapacity)
                );

        unloadedStress =
                Math.max(
                        0.0F,
                        unloadedStress
                                - (flywheelStress
                                - createMechanicalDrive$reconciledFlywheelStress)
                );

        createMechanicalDrive$reconciledFlywheelCapacity =
                flywheelCapacity;

        createMechanicalDrive$reconciledFlywheelStress =
                flywheelStress;

        if (
                unloadedMembers == 0
                        && createMechanicalDrive$hasLoadedFlywheel
        ) {
            unloadedCapacity =
                    0.0F;

            unloadedStress =
                    0.0F;
        }
    }

    @Inject(
            method = "remove",
            at = @At("HEAD")
    )
    private void createMechanicalDrive$removeVirtualSource(
            KineticBlockEntity be,
            CallbackInfo ci
    ) {
        sources.remove(be);
    }

    @Inject(
            method = "updateCapacity",
            at = @At("RETURN")
    )
    private void createMechanicalDrive$syncWormOutputCapacity(
            CallbackInfo ci
    ) {
        createMechanicalDrive$syncWormOutputs();
    }

    @Inject(
            method = "updateNetwork",
            at = @At("RETURN")
    )
    private void createMechanicalDrive$syncNetworkChanges(
            CallbackInfo ci
    ) {
        createMechanicalDrive$syncWormOutputs();
        createMechanicalDrive$syncFlywheelNetworkToClients();
    }

    @Unique
    private float createMechanicalDrive$calculateFlywheelCapacity() {
        float bonus =
                0.0F;

        java.util.HashSet<BlockPos> counted =
                new java.util.HashSet<>();

        for (
                KineticBlockEntity member
                : members.keySet()
        ) {
            if (!createMechanicalDrive$isPresentMember(member)) {
                continue;
            }

            if (!(
                    member
                            instanceof StirlingEngineFlywheelBlockEntity flywheel
            )) {
                continue;
            }

            if (
                    counted.contains(
                            flywheel.getBlockPos()
                    )
            ) {
                continue;
            }

            java.util.List<StirlingEngineFlywheelBlockEntity> group =
                    StirlingEngineFlywheelKinetics
                            .getFlywheelsOnShaft(
                                    flywheel
                            );

            float totalEfficiency =
                    0.0F;

            for (
                    StirlingEngineFlywheelBlockEntity groupedFlywheel
                    : group
            ) {
                counted.add(
                        groupedFlywheel.getBlockPos()
                );

                if (
                        members.containsKey(
                                groupedFlywheel
                        )
                ) {
                    totalEfficiency +=
                            groupedFlywheel
                                    .getEfficiency();
                }
            }

            bonus +=
                    Math.min(
                            totalEfficiency,
                            StirlingEngineFlywheelKinetics
                                    .MAX_EFFECTIVE_FLYWHEELS
                    )
                            * StirlingEngineFlywheelKinetics
                            .BONUS_SU_PER_FLYWHEEL;
        }

        return bonus;
    }

    @Unique
    private float createMechanicalDrive$calculateFlywheelStress() {
        float extraStress =
                0.0F;

        java.util.HashSet<BlockPos> counted =
                new java.util.HashSet<>();

        for (
                KineticBlockEntity member
                : members.keySet()
        ) {
            if (!createMechanicalDrive$isPresentMember(member)) {
                continue;
            }

            if (!(
                    member
                            instanceof StirlingEngineFlywheelBlockEntity flywheel
            )) {
                continue;
            }

            if (
                    counted.contains(
                            flywheel.getBlockPos()
                    )
            ) {
                continue;
            }

            java.util.List<StirlingEngineFlywheelBlockEntity> group =
                    StirlingEngineFlywheelKinetics
                            .getFlywheelsOnShaft(
                                    flywheel
                            );

            float totalEfficiency =
                    0.0F;

            for (
                    StirlingEngineFlywheelBlockEntity groupedFlywheel
                    : group
            ) {
                counted.add(
                        groupedFlywheel.getBlockPos()
                );

                if (
                        members.containsKey(
                                groupedFlywheel
                        )
                ) {
                    totalEfficiency +=
                            groupedFlywheel
                                    .getEfficiency();
                }
            }

            float excessEfficiency =
                    Math.max(
                            0.0F,
                            totalEfficiency
                                    - StirlingEngineFlywheelKinetics
                                    .MAX_EFFECTIVE_FLYWHEELS
                    );

            extraStress +=
                    excessEfficiency
                            * StirlingEngineFlywheelKinetics
                            .EXCESS_COST_SU_PER_FLYWHEEL;
        }

        return extraStress;
    }

    @Unique
    private boolean createMechanicalDrive$isPresentMember(KineticBlockEntity member) {
        return member.getLevel() != null
                && member.getLevel()
                .getBlockEntity(
                        member.getBlockPos()
                ) == member;
    }

    @Unique
    private void createMechanicalDrive$syncFlywheelNetworkToClients() {
        boolean hasFlywheel =
                false;

        for (
                KineticBlockEntity member
                : members.keySet()
        ) {
            if (
                    member
                            instanceof StirlingEngineFlywheelBlockEntity
            ) {
                hasFlywheel =
                        true;

                break;
            }
        }

        if (!hasFlywheel) {
            return;
        }

        for (
                KineticBlockEntity member
                : members.keySet()
        ) {
            member.sendData();
        }
    }

    @Unique
    private void createMechanicalDrive$syncWormOutputs() {
        if (createMechanicalDrive$syncingWormOutputs) {
            return;
        }

        createMechanicalDrive$syncingWormOutputs =
                true;

        try {
            for (
                    KineticBlockEntity member
                    : members.keySet()
            ) {
                if (!(
                        member
                                instanceof GearReducerBlockEntity reducer
                )) {
                    continue;
                }

                if (
                        reducer.getLevel() == null
                                || !reducer.isReductionMode()
                ) {
                    continue;
                }

                BlockPos outputPos =
                        reducer.getBlockPos()
                                .relative(
                                        reducer.getInputDirection()
                                );

                BlockEntity blockEntity =
                        reducer.getLevel()
                                .getBlockEntity(outputPos);

                if (!(
                        blockEntity
                                instanceof KineticBlockEntity output
                )) {
                    continue;
                }

                if (
                        GearReducerKinetics
                                .findDrivingReducer(output)
                                != reducer
                ) {
                    continue;
                }

                if (!output.hasNetwork()) {
                    continue;
                }

                KineticNetwork downstream =
                        output.getOrCreateNetwork();

                if (
                        downstream
                                == (Object) this
                ) {
                    continue;
                }

                downstream.updateCapacity();
            }
            for (
                    KineticBlockEntity member
                    : members.keySet()
            ) {
                if (!(
                        member
                                instanceof WormGearRegularBlockEntity worm
                )) {
                    continue;
                }

                if (worm.getLevel() == null) {
                    continue;
                }

                for (
                        Direction direction
                        : Direction.values()
                ) {
                    BlockPos cogPos =
                            worm.getBlockPos()
                                    .relative(direction);

                    BlockEntity blockEntity =
                            worm.getLevel()
                                    .getBlockEntity(cogPos);

                    if (!(
                            blockEntity
                                    instanceof KineticBlockEntity cog
                    )) {
                        continue;
                    }

                    if (
                            WormGearRegularKinetics
                                    .findDrivingWorm(cog)
                                    != worm
                    ) {
                        continue;
                    }

                    if (!cog.hasNetwork()) {
                        continue;
                    }

                    KineticNetwork downstream =
                            cog.getOrCreateNetwork();

                    if (
                            downstream
                                    == (Object) this
                    ) {
                        continue;
                    }

                    downstream.updateCapacity();
                }
            }
            for (
                    KineticBlockEntity member
                    : members.keySet()
            ) {
                if (!(
                        member
                                instanceof WormGearSmallBlockEntity worm
                )) {
                    continue;
                }

                if (worm.getLevel() == null) {
                    continue;
                }

                for (
                        Direction direction
                        : Direction.values()
                ) {
                    BlockPos cogPos =
                            worm.getBlockPos()
                                    .relative(direction);

                    BlockEntity blockEntity =
                            worm.getLevel()
                                    .getBlockEntity(cogPos);

                    if (!(
                            blockEntity
                                    instanceof KineticBlockEntity cog
                    )) {
                        continue;
                    }

                    if (
                            WormGearSmallKinetics
                                    .findDrivingWorm(cog)
                                    != worm
                    ) {
                        continue;
                    }

                    if (!cog.hasNetwork()) {
                        continue;
                    }

                    KineticNetwork downstream =
                            cog.getOrCreateNetwork();

                    if (
                            downstream
                                    == (Object) this
                    ) {
                        continue;
                    }

                    downstream.updateCapacity();
                }
            }
        } finally {
            createMechanicalDrive$syncingWormOutputs =
                    false;
        }
    }
}
