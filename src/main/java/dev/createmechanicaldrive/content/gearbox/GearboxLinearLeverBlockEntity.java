package dev.createmechanicaldrive.content.gearbox;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

public class GearboxLinearLeverBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    private GearboxPosition leverPosition =
            GearboxPosition.NEUTRAL;

    private float linearTarget =
            GearboxPosition.NEUTRAL.linearValue();

    private float previousAnimatedLinear =
            linearTarget;

    private float animatedLinear =
            linearTarget;

    public GearboxLinearLeverBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);

        if (state.hasProperty(GearboxLinearLeverBlock.POSITION)) {
            leverPosition =
                    state.getValue(GearboxLinearLeverBlock.POSITION);

            linearTarget =
                    leverPosition.linearValue();

            previousAnimatedLinear =
                    linearTarget;

            animatedLinear =
                    linearTarget;
        }
    }

    public void tickAnimation() {
        BlockState state = getBlockState();

        if (state.hasProperty(GearboxLinearLeverBlock.POSITION)) {
            GearboxPosition statePosition =
                    state.getValue(GearboxLinearLeverBlock.POSITION);

            if (leverPosition != statePosition) {
                setLeverPosition(statePosition);
            }
        }

        previousAnimatedLinear =
                animatedLinear;

        animatedLinear +=
                (linearTarget - animatedLinear) * 0.35F;

        if (Math.abs(linearTarget - animatedLinear) < 0.001F) {
            animatedLinear =
                    linearTarget;
        }
    }

    public GearboxPosition getLeverPosition() {
        return leverPosition;
    }

    public void setLeverPosition(
            GearboxPosition leverPosition
    ) {
        if (this.leverPosition == leverPosition) {
            return;
        }

        this.leverPosition =
                leverPosition;

        this.linearTarget =
                leverPosition.linearValue();

        setChanged();
    }

    public void setLinearTarget(float linearTarget) {
        this.linearTarget =
                Mth.clamp(linearTarget, -1.0F, 1.0F);
    }

    public float getAnimatedLinear(
            float partialTicks
    ) {
        return Mth.lerp(
                partialTicks,
                previousAnimatedLinear,
                animatedLinear
        );
    }

    @Override
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        CreateLang.builder()
                .add(Component.translatable(
                        "tooltip.mechanical_drive.gearbox.linear_lever"
                ))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        tooltip.add(Component.translatable(
                "tooltip.mechanical_drive.gearbox.selected_gear",
                getLeverPosition().labelComponent().withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.DARK_GRAY));

        return true;
    }
}
