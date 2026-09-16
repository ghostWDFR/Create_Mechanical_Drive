package dev.createmechanicaldrive.content.overrunning_clutch;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

public enum OverrunningClutchDirection implements INamedIconOptions {
    CLOCKWISE(
            AllIcons.I_REFRESH,
            "mechanical_drive.overrunning_clutch.direction.clockwise"
    ),
    COUNTER_CLOCKWISE(
            AllIcons.I_ROTATE_CCW,
            "mechanical_drive.overrunning_clutch.direction.counter_clockwise"
    );

    private final AllIcons icon;
    private final String translationKey;

    OverrunningClutchDirection(
            AllIcons icon,
            String translationKey
    ) {
        this.icon =
                icon;

        this.translationKey =
                translationKey;
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }
}
