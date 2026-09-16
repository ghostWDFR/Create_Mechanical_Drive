package dev.createmechanicaldrive.content.gearbox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.simibubi.create.foundation.utility.CreateLang;

import java.util.List;

public class GearboxAxialLeverBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private static final String LEVER_POSITION_TAG = "LeverPosition";
    private static final String GATE_X_TAG = "GateX";
    private static final String GATE_Y_TAG = "GateY";

    private GearboxPosition leverPosition = GearboxPosition.NEUTRAL;
    private float gateX = GearboxPosition.NEUTRAL.gateX();
    private float gateY = GearboxPosition.NEUTRAL.gateY();
    private float previousAnimatedGateX = gateX;
    private float previousAnimatedGateY = gateY;
    private float animatedGateX = gateX;
    private float animatedGateY = gateY;

    public GearboxAxialLeverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tickAnimation() {
        if (getBlockState().hasProperty(GearboxAxialLeverBlock.POSITION)) {
            GearboxPosition statePosition = getBlockState().getValue(GearboxAxialLeverBlock.POSITION);
            if (leverPosition != statePosition) {
                setLeverPosition(statePosition);
            }
        }

        previousAnimatedGateX = animatedGateX;
        previousAnimatedGateY = animatedGateY;
        animatedGateX += (gateX - animatedGateX) * 0.35F;
        animatedGateY += (gateY - animatedGateY) * 0.35F;
    }

    public GearboxPosition getLeverPosition() {
        return leverPosition;
    }

    public void setLeverPosition(GearboxPosition leverPosition) {
        if (this.leverPosition == leverPosition) {
            return;
        }

        this.leverPosition = leverPosition;
        setGateTarget(leverPosition.gateX(), leverPosition.gateY());
        setChanged();
    }

    public void setGateTarget(float gateX, float gateY) {
        GearboxPosition.GatePoint point = GearboxPosition.projectToGate(gateX, gateY);
        this.gateX = point.x();
        this.gateY = point.y();
        setChanged();
    }

    public float getAnimatedGateX(float partialTicks) {
        return Mth.lerp(partialTicks, previousAnimatedGateX, animatedGateX);
    }

    public float getAnimatedGateY(float partialTicks) {
        return Mth.lerp(partialTicks, previousAnimatedGateY, animatedGateY);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        leverPosition = GearboxPosition.byId(tag.getInt(LEVER_POSITION_TAG));
        gateX = tag.contains(GATE_X_TAG) ? tag.getFloat(GATE_X_TAG) : leverPosition.gateX();
        gateY = tag.contains(GATE_Y_TAG) ? tag.getFloat(GATE_Y_TAG) : leverPosition.gateY();
        previousAnimatedGateX = gateX;
        previousAnimatedGateY = gateY;
        animatedGateX = gateX;
        animatedGateY = gateY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(LEVER_POSITION_TAG, leverPosition.id());
        tag.putFloat(GATE_X_TAG, gateX);
        tag.putFloat(GATE_Y_TAG, gateY);
    }

    @Override
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        CreateLang.builder()
                .add(Component.translatable(
                        "tooltip.mechanical_drive.gearbox.lever"
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
