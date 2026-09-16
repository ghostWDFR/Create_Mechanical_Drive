package dev.createmechanicaldrive.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffset;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerRenderer;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(WheelMountRenderer.class)
public abstract class OffroadWheelMountRendererOffsetMixin {

    private static final String RENDER_METHOD =
            "renderSafe(Ldev/ryanhcode/offroad/content/blocks/wheel_mount/"
                    + "WheelMountBlockEntity;FLcom/mojang/blaze3d/vertex/"
                    + "PoseStack;Lnet/minecraft/client/renderer/"
                    + "MultiBufferSource;II)V";

    @Inject(
            method = RENDER_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;"
                            + "get(Lnet/minecraft/core/component/"
                            + "DataComponentType;)Ljava/lang/Object;",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            ),
            require = 1
    )
    private void mechanicalDrive$renderShaftMarker(
            WheelMountBlockEntity mount,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay,
            CallbackInfo ci
    ) {
        ShaftMarkerRenderer.renderInWheelMount(
                mount.getHeldItem(),
                mount.getBlockState(),
                poseStack,
                buffers,
                light
        );
    }

    @ModifyExpressionValue(
            method = RENDER_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/offroad/content/blocks/"
                            + "wheel_mount/WheelMountBlockEntity;"
                            + "getLerpedExtension(F)D"
            ),
            require = 0
    )
    private double mechanicalDrive$applyHeightOffset(
            double extension,
            WheelMountBlockEntity mount,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        return extension - offset(mount)
                .mechanicalDrive$getLerpedHeightOffset(partialTicks);
    }

    @ModifyArgs(
            method = RENDER_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "translate(DDD)V",
                    ordinal = 0
            ),
            require = 0
    )
    private void mechanicalDrive$offsetTelescopingLinkage(
            Args args,
            WheelMountBlockEntity mount,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        addHorizontalOffset(args, mount, partialTicks, false);
    }

    @ModifyArgs(
            method = RENDER_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "translate(DDD)V",
                    ordinal = 4
            ),
            require = 0
    )
    private void mechanicalDrive$offsetWheel(
            Args args,
            WheelMountBlockEntity mount,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        addHorizontalOffset(args, mount, partialTicks, false);
    }

    @ModifyArgs(
            method = RENDER_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "translate(DDD)V",
                    ordinal = 11
            ),
            require = 0
    )
    private void mechanicalDrive$offsetSpringPivot(
            Args args,
            WheelMountBlockEntity mount,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        addHorizontalOffset(args, mount, partialTicks, false);
    }

    @ModifyArgs(
            method = RENDER_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "translate(DDD)V",
                    ordinal = 12
            ),
            require = 0
    )
    private void mechanicalDrive$offsetSpringPivotBack(
            Args args,
            WheelMountBlockEntity mount,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        addHorizontalOffset(args, mount, partialTicks, true);
    }

    private static void addHorizontalOffset(
            Args args,
            WheelMountBlockEntity mount,
            float partialTicks,
            boolean inverse
    ) {
        WheelMountOffset offset = offset(mount);
        Direction side = offset.mechanicalDrive$getOffsetFacing();
        double sign = inverse ? -1.0D : 1.0D;
        args.set(0, args.<Double>get(0) + sign
                * offset.mechanicalDrive$getRenderedLateralOffset(
                        partialTicks,
                        side
                ));
        args.set(2, args.<Double>get(2) + sign
                * offset.mechanicalDrive$getRenderedLongitudinalOffset(
                        partialTicks
                ));
    }

    private static WheelMountOffset offset(WheelMountBlockEntity mount) {
        return (WheelMountOffset) mount;
    }
}
