package dev.createmechanicaldrive.content.gearbox;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

public class CarGearboxInputBlockEntity extends KineticBlockEntity {
    private static final String REQUESTED_POSITION_TAG = "RequestedPosition";

    private GearboxPosition requestedPosition = GearboxPosition.NEUTRAL;

    public CarGearboxInputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        BlockState leverState = level.getBlockState(worldPosition.above());
        if (leverState.is(CreateMechanicalDrive.GEARBOX_AXIAL_LEVER.get())
                && canReadLever(leverState, state)) {
            GearboxPosition leverPosition = leverState.getValue(GearboxAxialLeverBlock.POSITION);
            if (leverPosition != getRequestedPosition()) {
                setRequestedPosition(leverPosition);
            } else {
                CarGearboxSpeedBlockEntity.refreshFromInput(level, worldPosition);
            }
        }
    }

    public GearboxPosition getRequestedPosition() {
        return requestedPosition;
    }

    private static boolean canReadLever(BlockState leverState, BlockState inputState) {
        Direction inputFacing = inputState.getValue(CarGearboxInputBlock.FACING);
        Direction leverFacing = leverState.getValue(GearboxAxialLeverBlock.FACING);
        return inputFacing.getAxis().isHorizontal()
                && (inputFacing == leverFacing.getOpposite() || inputFacing == leverFacing);
    }

    public void setRequestedPosition(GearboxPosition position) {
        if (position == null) {
            position = GearboxPosition.NEUTRAL;
        }

        requestedPosition = position;

        if (level != null && !level.isClientSide) {
            CarGearboxSpeedBlockEntity.refreshFromInput(level, worldPosition);
            setChanged();
            sendData();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putInt(REQUESTED_POSITION_TAG, requestedPosition.id());
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        requestedPosition = GearboxPosition.byId(tag.getInt(REQUESTED_POSITION_TAG));
        super.read(tag, registries, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder()
                .add(Component.translatable("tooltip.mechanical_drive.gearbox.front"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);
        tooltip.add(Component.translatable(
                "tooltip.mechanical_drive.gearbox.requested_gear",
                requestedPosition.labelComponent().withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(
                "tooltip.mechanical_drive.gearbox.input_speed",
                Component.literal(formatSpeed(getSpeed())).withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(
                "tooltip.mechanical_drive.gearbox.output_speed",
                Component.literal(formatSpeed(getSpeed())).withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(
                "tooltip.mechanical_drive.gearbox.network_stress",
                Component.literal(formatStress(stress)).withStyle(ChatFormatting.AQUA),
                Component.literal(formatStress(capacity)).withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.DARK_GRAY));
        return true;
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }

    private static String formatSpeed(float speed) {
        return String.format(Locale.ROOT, "%.1f RPM", speed);
    }

    private static String formatStress(float value) {
        return String.format(Locale.ROOT, "%.1f SU", value);
    }
}
