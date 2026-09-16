package dev.createmechanicaldrive.content.engine.display;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.createmechanicaldrive.content.engine.EngineBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;

public class EngineStatusDisplaySource
        extends DisplaySource {

    @Override
    public List<MutableComponent> provideText(
            DisplayLinkContext context,
            DisplayTargetStats stats
    ) {
        if (!(context.getSourceBlockEntity()
                instanceof EngineBlockEntity engine)) {
            return EMPTY;
        }

        long remainingSeconds =
                engine.getDisplayFuelTimeSeconds();

        long fuelAmountMb =
                engine.getDisplayFuelAmountMb();

        int columns =
                stats.maxColumns();

        if (columns <= 4) {
            return List.of(
                    Component.literal(
                            formatTime(remainingSeconds)
                    ),
                    Component.literal(
                            Long.toString(fuelAmountMb)
                    )
            );
        }

        return List.of(
                Component.literal(
                        "T "
                                + formatTime(remainingSeconds)
                ),
                Component.literal(
                        "F "
                                + fuelAmountMb
                )
        );
    }

    private static String formatTime(
            long totalSeconds
    ) {
        long hours =
                totalSeconds / 3600L;

        long minutes =
                (totalSeconds % 3600L) / 60L;

        long seconds =
                totalSeconds % 60L;

        if (hours > 0L) {
            return String.format(
                    Locale.ROOT,
                    "%d:%02d:%02d",
                    hours,
                    minutes,
                    seconds
            );
        }

        return String.format(
                Locale.ROOT,
                "%d:%02d",
                minutes,
                seconds
        );
    }
}