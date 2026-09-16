package dev.createmechanicaldrive.content.suspension_strut;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CreateMechanicalDrive.MOD_ID)
public final class CompactStrutSupportEvents {
    private CompactStrutSupportEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSupportBroken(BlockEvent.BreakEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Player player = event.getPlayer();
        Vec3 eye = player.getEyePosition();
        Vec3 rayEnd = eye.add(
                player.getLookAngle().scale(
                        player.blockInteractionRange() + 0.5D
                )
        );
        BlockHitResult jointHit =
                CompactStrutSupportShapes.clipSupportJoints(
                        level,
                        eye,
                        rayEnd
                );
        if (jointHit != null
                && jointHit.getBlockPos().equals(event.getPos())) {
            SuspensionStrutBlockEntity strut =
                    CompactStrutSupportShapes.findAttachedStrut(
                            level,
                            event.getPos(),
                            jointHit.getLocation()
                    );
            if (strut != null) {
                event.setCanceled(true);
                strut.onCompactSupportBroken(level, event.getPos());
                return;
            }
        }

        CompactStrutSupportShapes.breakAttachedStruts(
                level,
                event.getPos()
        );
    }
}
