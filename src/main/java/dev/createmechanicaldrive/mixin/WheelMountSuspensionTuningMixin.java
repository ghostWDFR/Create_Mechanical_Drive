package dev.createmechanicaldrive.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.createmechanicaldrive.compat.tracks.TracksWheelMountOffsetBridge;
import dev.createmechanicaldrive.compat.tracks.TracksWheelMountTuningBridge;
import dev.createmechanicaldrive.content.suspension.SuspensionSpringTuning;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffset;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffsetState;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlock;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WheelMountBlockEntity.class)
public abstract class WheelMountSuspensionTuningMixin
        implements SuspensionSpringTuning, WheelMountOffset {

    @Unique
    private final WheelMountOffsetState mechanicalDrive$wheelOffset =
            new WheelMountOffsetState();

    @Unique
    private double mechanicalDrive$suspensionSpringMultiplier =
            DEFAULT_MULTIPLIER;
    @Unique
    private double mechanicalDrive$suspensionDampingMultiplier =
            DEFAULT_MULTIPLIER;
    @Unique
    private double mechanicalDrive$suspensionBumpClearanceMultiplier =
            DEFAULT_MULTIPLIER;
    @Unique
    private double mechanicalDrive$suspensionBumpForceMultiplier =
            DEFAULT_MULTIPLIER;
    @Unique
    private double mechanicalDrive$suspensionMaxImpulseMultiplier =
            DEFAULT_MULTIPLIER;
    @Unique
    private double mechanicalDrive$suspensionDriveMultiplier =
            DEFAULT_MULTIPLIER;
    @Unique
    private double mechanicalDrive$suspensionGripMultiplier =
            DEFAULT_MULTIPLIER;

    @Override
    public WheelMountOffsetState mechanicalDrive$getWheelOffsetState() {
        return mechanicalDrive$wheelOffset;
    }

    @Override
    public void mechanicalDrive$syncWheelOffset() {
        mechanicalDrive$syncSuspensionTuning();
    }

    @Override
    public double mechanicalDrive$adjustLateralOffset(int steps) {
        Double tracksValue = TracksWheelMountOffsetBridge.adjustLateral(this, steps);
        if (tracksValue != null) {
            return tracksValue;
        }
        double value = mechanicalDrive$wheelOffset.adjustLateral(steps);
        mechanicalDrive$syncSuspensionTuning();
        return value;
    }

    @Override
    public double mechanicalDrive$adjustLongitudinalOffset(int steps) {
        Double tracksValue = TracksWheelMountOffsetBridge.adjustLongitudinal(this, steps);
        if (tracksValue != null) {
            return tracksValue;
        }
        double value = mechanicalDrive$wheelOffset.adjustLongitudinal(steps);
        mechanicalDrive$syncSuspensionTuning();
        return value;
    }

    @Override
    public double mechanicalDrive$adjustHeightOffset(int steps) {
        Double tracksValue = TracksWheelMountOffsetBridge.adjustHeight(this, steps);
        if (tracksValue != null) {
            return tracksValue;
        }
        double value = mechanicalDrive$wheelOffset.adjustHeight(steps);
        mechanicalDrive$syncSuspensionTuning();
        return value;
    }

    @Override
    public double mechanicalDrive$getLerpedLateralOffset(float partialTicks) {
        Double value = TracksWheelMountOffsetBridge.getLerpedLateral(this, partialTicks);
        return value != null
                ? value
                : mechanicalDrive$wheelOffset.getLerpedLateral(partialTicks);
    }

    @Override
    public double mechanicalDrive$getLerpedLongitudinalOffset(float partialTicks) {
        Double value = TracksWheelMountOffsetBridge.getLerpedLongitudinal(this, partialTicks);
        return value != null
                ? value
                : mechanicalDrive$wheelOffset.getLerpedLongitudinal(partialTicks);
    }

    @Override
    public double mechanicalDrive$getLerpedHeightOffset(float partialTicks) {
        Double value = TracksWheelMountOffsetBridge.getLerpedHeight(this, partialTicks);
        return value != null
                ? value
                : mechanicalDrive$wheelOffset.getLerpedHeight(partialTicks);
    }

    @Override
    public Direction mechanicalDrive$getOffsetFacing() {
        WheelMountBlockEntity blockEntity =
                (WheelMountBlockEntity) (Object) this;
        return blockEntity.getBlockState().getValue(
                WheelMountBlock.HORIZONTAL_FACING
        );
    }
    @Override
    public double mechanicalDrive$adjustSuspensionTuning(
            String tuning,
            int steps
    ) {
        if (mechanicalDrive$isTracksNativeTuning(tuning)) {
            Double tracksValue =
                    TracksWheelMountTuningBridge.adjustTuning(
                            this,
                            tuning,
                            steps
                    );
            if (tracksValue != null) {
                return tracksValue;
            }
        }

        double multiplier = SuspensionSpringTuning.steppedMultiplier(
                tuning,
                mechanicalDrive$getLocalMultiplier(tuning),
                steps
        );
        mechanicalDrive$setLocalMultiplier(tuning, multiplier);
        mechanicalDrive$syncSuspensionTuning();
        return multiplier;
    }

    @Override
    public double mechanicalDrive$getSuspensionTuning(String tuning) {
        if (mechanicalDrive$isTracksNativeTuning(tuning)) {
            Double tracksValue =
                    TracksWheelMountTuningBridge.getTuning(this, tuning);
            if (tracksValue != null) {
                return tracksValue;
            }
        }

        return mechanicalDrive$getLocalMultiplier(tuning);
    }

    @Override
    public void mechanicalDrive$resetSuspensionTuning() {
        TracksWheelMountTuningBridge.reset(this);
        mechanicalDrive$suspensionSpringMultiplier = DEFAULT_MULTIPLIER;
        mechanicalDrive$suspensionDampingMultiplier = DEFAULT_MULTIPLIER;
        mechanicalDrive$suspensionBumpClearanceMultiplier =
                DEFAULT_MULTIPLIER;
        mechanicalDrive$suspensionBumpForceMultiplier = DEFAULT_MULTIPLIER;
        mechanicalDrive$suspensionMaxImpulseMultiplier = DEFAULT_MULTIPLIER;
        mechanicalDrive$suspensionDriveMultiplier = DEFAULT_MULTIPLIER;
        mechanicalDrive$suspensionGripMultiplier = DEFAULT_MULTIPLIER;
        mechanicalDrive$syncSuspensionTuning();
    }

    @Unique
    private boolean mechanicalDrive$isTracksNativeTuning(String tuning) {
        return SPRING.equals(tuning)
                || DRIVE.equals(tuning)
                || GRIP.equals(tuning);
    }

    @Unique
    private double mechanicalDrive$getLocalMultiplier(String tuning) {
        return switch (tuning) {
            case DAMPING -> mechanicalDrive$suspensionDampingMultiplier;
            case BUMP_CLEARANCE ->
                    mechanicalDrive$suspensionBumpClearanceMultiplier;
            case BUMP_FORCE -> mechanicalDrive$suspensionBumpForceMultiplier;
            case MAX_IMPULSE ->
                    mechanicalDrive$suspensionMaxImpulseMultiplier;
            case DRIVE -> mechanicalDrive$suspensionDriveMultiplier;
            case GRIP -> mechanicalDrive$suspensionGripMultiplier;
            default -> mechanicalDrive$suspensionSpringMultiplier;
        };
    }

    @Unique
    private void mechanicalDrive$setLocalMultiplier(
            String tuning,
            double multiplier
    ) {
        switch (tuning) {
            case DAMPING -> mechanicalDrive$suspensionDampingMultiplier =
                    multiplier;
            case BUMP_CLEARANCE ->
                    mechanicalDrive$suspensionBumpClearanceMultiplier =
                            multiplier;
            case BUMP_FORCE -> mechanicalDrive$suspensionBumpForceMultiplier =
                    multiplier;
            case MAX_IMPULSE ->
                    mechanicalDrive$suspensionMaxImpulseMultiplier =
                            multiplier;
            case DRIVE -> mechanicalDrive$suspensionDriveMultiplier =
                    multiplier;
            case GRIP -> mechanicalDrive$suspensionGripMultiplier =
                    multiplier;
            default -> mechanicalDrive$suspensionSpringMultiplier =
                    multiplier;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void mechanicalDrive$tickWheelOffset(CallbackInfo ci) {
        if (!TracksWheelMountOffsetBridge.isAvailable(this)) {
            mechanicalDrive$wheelOffset.tick();
        }
    }
    @Inject(method = "write", at = @At("TAIL"))
    private void mechanicalDrive$writeSuspensionTuning(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket,
            CallbackInfo ci
    ) {
        if (!TracksWheelMountOffsetBridge.isAvailable(this)) {
            mechanicalDrive$wheelOffset.write(tag);
        }

        if (!TracksWheelMountTuningBridge.isAvailable(this)) {
            tag.putDouble(
                    SPRING_NBT_KEY,
                    mechanicalDrive$suspensionSpringMultiplier
            );
            tag.putDouble(
                    DRIVE_NBT_KEY,
                    mechanicalDrive$suspensionDriveMultiplier
            );
            tag.putDouble(
                    GRIP_NBT_KEY,
                    mechanicalDrive$suspensionGripMultiplier
            );
        }

        tag.putDouble(
                DAMPING_NBT_KEY,
                mechanicalDrive$suspensionDampingMultiplier
        );
        tag.putDouble(
                BUMP_CLEARANCE_NBT_KEY,
                mechanicalDrive$suspensionBumpClearanceMultiplier
        );
        tag.putDouble(
                BUMP_FORCE_NBT_KEY,
                mechanicalDrive$suspensionBumpForceMultiplier
        );
        tag.putDouble(
                MAX_IMPULSE_NBT_KEY,
                mechanicalDrive$suspensionMaxImpulseMultiplier
        );
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void mechanicalDrive$readSuspensionTuning(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket,
            CallbackInfo ci
    ) {
        if (!TracksWheelMountOffsetBridge.isAvailable(this)) {
            mechanicalDrive$wheelOffset.read(tag, clientPacket);
        }

        if (!TracksWheelMountTuningBridge.isAvailable(this)) {
            mechanicalDrive$suspensionSpringMultiplier =
                    mechanicalDrive$readMultiplier(tag, SPRING_NBT_KEY);
            mechanicalDrive$suspensionDriveMultiplier =
                    mechanicalDrive$readMultiplier(tag, DRIVE_NBT_KEY);
            mechanicalDrive$suspensionGripMultiplier =
                    mechanicalDrive$readMultiplier(tag, GRIP_NBT_KEY);
        }

        mechanicalDrive$suspensionDampingMultiplier =
                mechanicalDrive$readMultiplier(tag, DAMPING_NBT_KEY);
        mechanicalDrive$suspensionBumpClearanceMultiplier =
                mechanicalDrive$readMultiplier(tag, BUMP_CLEARANCE_NBT_KEY);
        mechanicalDrive$suspensionBumpForceMultiplier =
                mechanicalDrive$readMultiplier(tag, BUMP_FORCE_NBT_KEY);
        mechanicalDrive$suspensionMaxImpulseMultiplier =
                mechanicalDrive$readMultiplier(tag, MAX_IMPULSE_NBT_KEY);
    }

    @Unique
    private static double mechanicalDrive$readMultiplier(
            CompoundTag tag,
            String key
    ) {
        return tag.contains(key)
                ? SuspensionSpringTuning.clampMultiplier(tag.getDouble(key))
                : DEFAULT_MULTIPLIER;
    }

    @ModifyVariable(
            method = "sable$physicsTick",
            at = @At(value = "STORE"),
            index = 22
    )
    private double mechanicalDrive$applySuspensionSpringAndBump(
            double original
    ) {
        double multiplier = mechanicalDrive$suspensionBumpForceMultiplier;
        if (!TracksWheelMountTuningBridge.isAvailable(this)) {
            multiplier *= mechanicalDrive$suspensionSpringMultiplier;
        }
        return original * multiplier;
    }

    @ModifyVariable(
            method = "sable$physicsTick",
            at = @At(value = "STORE"),
            index = 24
    )
    private double mechanicalDrive$applySuspensionDamping(
            double original
    ) {
        return original * mechanicalDrive$suspensionDampingMultiplier;
    }

    @ModifyConstant(
            method = "sable$physicsTick",
            constant = @Constant(doubleValue = 0.25D)
    )
    private double mechanicalDrive$applyBumpClearance(double original) {
        return original * mechanicalDrive$suspensionBumpClearanceMultiplier;
    }

    @ModifyVariable(
            method = "sable$physicsTick",
            at = @At(value = "STORE"),
            index = 42
    )
    private double mechanicalDrive$applyMaximumImpulse(double original) {
        return original * mechanicalDrive$suspensionMaxImpulseMultiplier;
    }

    @ModifyVariable(
            method = "sable$physicsTick",
            at = @At(value = "LOAD"),
            index = 52
    )
    private float mechanicalDrive$applySuspensionDrive(float original) {
        if (TracksWheelMountTuningBridge.isAvailable(this)) {
            return original;
        }

        return (float) (
                original * mechanicalDrive$suspensionDriveMultiplier
        );
    }

    @ModifyArg(
            method = "sable$physicsTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/joml/Vector3d;fma(DLorg/joml/Vector3dc;)Lorg/joml/Vector3d;",
                    ordinal = 1
            ),
            index = 0
    )
    private double mechanicalDrive$applySuspensionGrip(
            double original
    ) {
        if (TracksWheelMountTuningBridge.isAvailable(this)) {
            return original;
        }

        return original * mechanicalDrive$suspensionGripMultiplier;
    }

    @ModifyExpressionValue(
            method = {"sable$physicsTick", "computeMaxExtensionToTerrain"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;getCenter()Lnet/minecraft/world/phys/Vec3;"
            ),
            require = 0
    )
    private Vec3 mechanicalDrive$applyLocalWheelOffset(Vec3 original) {
        if (TracksWheelMountOffsetBridge.isAvailable(this)) {
            return original;
        }
        return mechanicalDrive$applyWheelOffset(
                original,
                mechanicalDrive$getOffsetFacing()
        );
    }

    @Inject(
            method = "createRenderBoundingBox",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private void mechanicalDrive$inflateOffsetRenderBounds(
            CallbackInfoReturnable<AABB> cir
    ) {
        if (!TracksWheelMountOffsetBridge.isAvailable(this)) {
            cir.setReturnValue(cir.getReturnValue().inflate(
                    mechanicalDrive$wheelOffset.getMaximumAbsoluteOffset()
            ));
        }
    }
    @Unique
    private void mechanicalDrive$syncSuspensionTuning() {
        WheelMountBlockEntity blockEntity =
                (WheelMountBlockEntity) (Object) this;

        blockEntity.setChanged();

        Level level = blockEntity.getLevel();
        if (level != null && !level.isClientSide) {
            blockEntity.sendData();
        }
    }
}
