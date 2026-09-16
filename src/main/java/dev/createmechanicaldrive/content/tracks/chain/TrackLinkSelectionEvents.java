package dev.createmechanicaldrive.content.tracks.chain;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CreateMechanicalDrive.MOD_ID)
public final class TrackLinkSelectionEvents {
    private TrackLinkSelectionEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            TrackLinkItem.cancelSelectionIfItemChanged(player);
        }
    }
}
