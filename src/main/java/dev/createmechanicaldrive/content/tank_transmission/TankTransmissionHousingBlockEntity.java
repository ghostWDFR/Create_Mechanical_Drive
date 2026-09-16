package dev.createmechanicaldrive.content.tank_transmission;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public class TankTransmissionHousingBlockEntity
        extends KineticBlockEntity {

    public TankTransmissionHousingBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    private static final float STRESS_IMPACT =
            2.0F;

    @Override
    public float calculateStressApplied() {
        lastStressApplied =
                STRESS_IMPACT;

        return STRESS_IMPACT;
    }

    @Override
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        float stressImpact =
                calculateStressApplied();

        float stressUsage =
                Math.abs(getSpeed())
                        * stressImpact;

        CreateLang.builder()
                .add(
                        Component.translatable(
                                "block.mechanical_drive.tank_transmission_housing"
                        ).append(
                                ":"
                        )
                )
                .style(
                        ChatFormatting.WHITE
                )
                .forGoggles(
                        tooltip
                );

        CreateLang.builder()
                .add(
                        Component.translatable(
                                "tooltip.mechanical_drive.tank_transmission.stress_impact",
                                Component.literal(
                                        formatStress(
                                                stressUsage
                                        )
                                ).withStyle(
                                        ChatFormatting.AQUA
                                )
                        ).withStyle(
                                ChatFormatting.GRAY
                        )
                )
                .forGoggles(
                        tooltip
                );

        return true;
    }

    private static String formatStress(
            float value
    ) {
        return String.format(
                Locale.ROOT,
                "%.0f su",
                value
        );
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }
}