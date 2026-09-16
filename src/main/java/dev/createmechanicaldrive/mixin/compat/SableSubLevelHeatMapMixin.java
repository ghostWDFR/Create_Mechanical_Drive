package dev.createmechanicaldrive.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.createmechanicaldrive.content.rigid_link.RigidLinkJointBlock;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutBlock;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SubLevelHeatMapManager.class, remap = false)
public abstract class SableSubLevelHeatMapMixin {
    @Shadow
    @Final
    private ServerSubLevel subLevel;

    @ModifyExpressionValue(
            method = "step",
            at = {
                    @At(
                            value = "INVOKE",
                            target = "Ldev/ryanhcode/sable/sublevel/plot/heat/"
                                    + "SubLevelHeatMapManager;isSolidAt("
                                    + "Lnet/minecraft/core/BlockPos;)Z",
                            ordinal = 0
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Ldev/ryanhcode/sable/sublevel/plot/heat/"
                                    + "SubLevelHeatMapManager;isSolidAt("
                                    + "Lnet/minecraft/core/BlockPos;)Z",
                            ordinal = 1
                    )
            }
    )
    private boolean mechanicalDrive$filterJointConnection(
            boolean solid,
            @Local(name = "p") BlockPos source,
            @Local(name = "p2") BlockPos target
    ) {
        return solid && mechanicalDrive$canConnect(source, target);
    }

    @ModifyExpressionValue(
            method = "step",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/sublevel/plot/heat/"
                            + "SubLevelHeatMapManager;isSolidAt("
                            + "Lnet/minecraft/core/BlockPos;)Z",
                    ordinal = 3
            )
    )
    private boolean mechanicalDrive$filterNestedJointConnection(
            boolean solid,
            @Local(name = "p2") BlockPos source,
            @Local(name = "p3") BlockPos target
    ) {
        return solid && mechanicalDrive$canConnect(source, target);
    }

    @ModifyExpressionValue(
            method = "onSolidAdded",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/sublevel/plot/heat/"
                            + "SubLevelHeatMapManager;heatMapContains("
                            + "Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean mechanicalDrive$filterNewJointConnection(
            boolean contains,
            @Local(argsOnly = true) BlockPos source,
            @Local(name = "neighbor") BlockPos target
    ) {
        return contains && mechanicalDrive$canConnect(source, target);
    }

    @Unique
    private boolean mechanicalDrive$canConnect(
            BlockPos firstPos,
            BlockPos secondPos
    ) {
        Level level = subLevel.getLevel();
        BlockState firstState = level.getBlockState(firstPos);
        BlockState secondState = level.getBlockState(secondPos);

        return mechanicalDrive$jointFacesSupport(
                firstState,
                firstPos,
                secondPos
        ) && mechanicalDrive$jointFacesSupport(
                secondState,
                secondPos,
                firstPos
        );
    }

    @Unique
    private static boolean mechanicalDrive$jointFacesSupport(
            BlockState state,
            BlockPos jointPos,
            BlockPos neighborPos
    ) {
        if (!(state.getBlock() instanceof RigidLinkJointBlock)
                && !(state.getBlock() instanceof SuspensionStrutBlock)) {
            return true;
        }

        return jointPos.relative(
                state.getValue(SuspensionStrutBlock.FACING)
                        .getOpposite()
        ).equals(neighborPos);
    }
}
