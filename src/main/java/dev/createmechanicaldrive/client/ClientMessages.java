package dev.createmechanicaldrive.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientMessages {
    private ClientMessages() {
    }

    public static void actionBar(
            ChatFormatting formatting,
            String key,
            Object... args
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        minecraft.player.displayClientMessage(
                Component.translatable(
                                key,
                                args
                        )
                        .withStyle(formatting),
                true
        );
    }

    public static void tooFar() {
        actionBar(
                ChatFormatting.RED,
                "message.mechanical_drive.too_far"
        );
    }
}
