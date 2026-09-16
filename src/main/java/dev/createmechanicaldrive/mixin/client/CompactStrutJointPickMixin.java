package dev.createmechanicaldrive.mixin.client;

import dev.createmechanicaldrive.content.suspension_strut.CompactStrutSupportShapes;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class CompactStrutJointPickMixin {
    @Inject(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void createMechanicalDrive$pickCompactStrutJoint(
            Entity cameraEntity,
            double blockInteractionRange,
            double entityInteractionRange,
            float partialTick,
            CallbackInfoReturnable<HitResult> cir
    ) {
        Vec3 start = cameraEntity.getEyePosition(partialTick);
        Vec3 end = start.add(
                cameraEntity.getViewVector(partialTick)
                        .scale(blockInteractionRange)
        );
        BlockHitResult jointHit =
                CompactStrutSupportShapes.clipSupportJoints(
                        cameraEntity.level(),
                        start,
                        end
                );
        if (jointHit == null) {
            return;
        }

        HitResult vanillaHit = cir.getReturnValue();
        if (vanillaHit.getType() == HitResult.Type.MISS
                || jointHit.getLocation().distanceToSqr(start)
                < vanillaHit.getLocation().distanceToSqr(start)) {
            cir.setReturnValue(jointHit);
        }
    }
}
