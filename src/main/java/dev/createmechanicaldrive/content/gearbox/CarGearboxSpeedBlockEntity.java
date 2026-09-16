package dev.createmechanicaldrive.content.gearbox;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public class CarGearboxSpeedBlockEntity extends SplitShaftBlockEntity {
    private static final String ACTIVE_POSITION_TAG = "ActivePosition";
    private static final float BASE_STRESS_IMPACT = 4.0F;

    private GearboxPosition activePosition = GearboxPosition.NEUTRAL;
    private ScrollOptionBehaviour<OutputRotationDirection> outputDirection;

    private enum OutputRotationDirection implements INamedIconOptions {
        CLOCKWISE(
                AllIcons.I_REFRESH,
                "mechanical_drive.car_gearbox.output_direction.clockwise"
        ),
        COUNTER_CLOCKWISE(
                AllIcons.I_ROTATE_CCW,
                "mechanical_drive.car_gearbox.output_direction.counter_clockwise"
        );

        private final AllIcons icon;
        private final String translationKey;

        OutputRotationDirection(
                AllIcons icon,
                String translationKey
        ) {
            this.icon = icon;
            this.translationKey = translationKey;
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

    public CarGearboxSpeedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (state.hasProperty(CarGearboxSpeedBlock.POSITION)) {
            activePosition = state.getValue(CarGearboxSpeedBlock.POSITION);
        }
    }

    @Override
    public void addBehaviours(
            List<BlockEntityBehaviour> behaviours
    ) {
        super.addBehaviours(behaviours);

        outputDirection =
                new ScrollOptionBehaviour<>(
                        OutputRotationDirection.class,
                        Component.translatable(
                                "mechanical_drive.car_gearbox.output_direction"
                        ),
                        this,
                        new OutputDirectionValueBoxTransform()
                                .fromSide(Direction.UP)
                );

        outputDirection.withCallback(
                value -> onOutputDirectionChanged()
        );

        behaviours.add(outputDirection);
    }

    private static class OutputDirectionValueBoxTransform
            extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(
                    8.0F,
                    8.0F,
                    15.51F
            );
        }

        @Override
        protected boolean isSideActive(
                BlockState state,
                Direction direction
        ) {
            return direction == Direction.UP;
        }

        @Override
        public float getScale() {
            return 0.5F;
        }
    }

    private void onOutputDirectionChanged() {
        if (level == null || level.isClientSide) {
            return;
        }
        RotationPropagator.handleRemoved(
                level,
                worldPosition,
                this
        );
        RotationPropagator.handleAdded(
                level,
                worldPosition,
                this
        );
        setChanged();
        sendData();
    }

    private float getOutputDirectionMultiplier() {
        if (outputDirection == null) {
            return 1.0F;
        }

        return outputDirection.get()
                == OutputRotationDirection.CLOCKWISE
                ? 1.0F
                : -1.0F;
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) {
            return;
        }
        GearboxPosition position;

        if (!hasValidInputBlock()) {
            position = GearboxPosition.NEUTRAL;
        } else if (isRedstoneForcedNeutral()) {
            position = GearboxPosition.NEUTRAL;
        } else {
            GearboxPosition inputPosition =
                    readInputPositionOrNull();

            position = inputPosition != null
                    ? inputPosition
                    : GearboxPosition.NEUTRAL;
        }

        if (position != getActivePosition()) {
            setActivePosition(position);
        }
    }

    @Override
    public float getRotationSpeedModifier(Direction direction) {
        if (!hasSource()) {
            return 1.0F;
        }

        Direction sourceFacing = getSourceFacing();
        if (direction == sourceFacing) {
            return 1.0F;
        }

        float modifier = getActivePosition().speedMultiplier();
        if (modifier == 0.0F) {
            return 0.0F;
        }

        Direction inputDirection = getInputDirection();

        if (sourceFacing == inputDirection.getOpposite()
                && direction == inputDirection) {
            modifier = 1.0F / modifier;
        }
        modifier *= getOutputDirectionMultiplier();

        return modifier;
    }

    @Override
    public float calculateStressApplied() {
        float impact = BASE_STRESS_IMPACT * Math.abs(getActivePosition().speedMultiplier());
        lastStressApplied = impact;
        return impact;
    }

    public void setActivePosition(GearboxPosition position) {
        if (position == null) {
            position = GearboxPosition.NEUTRAL;
        }

        if (level != null && !level.isClientSide) {
            if (!hasValidInputBlock()) {
                position = GearboxPosition.NEUTRAL;
            } else if (isRedstoneForcedNeutral()) {
                position = GearboxPosition.NEUTRAL;
            }
        }

        if (position == activePosition
                && position == getActivePosition()) {
            return;
        }

        if (level == null || level.isClientSide) {
            activePosition = position;
            return;
        }

        RotationPropagator.handleRemoved(level, worldPosition, this);
        activePosition = position;
        RotationPropagator.handleAdded(level, worldPosition, this);
        updateStressImpactInNetwork();
        setChanged();
        sendData();
    }

    public GearboxPosition getActivePosition() {
        return activePosition;
    }

    private boolean hasValidInputBlock() {
        if (level == null) {
            return false;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(CarGearboxSpeedBlock.FACING)) {
            return false;
        }

        Direction facing =
                state.getValue(CarGearboxSpeedBlock.FACING);

        BlockPos inputPos =
                worldPosition.relative(facing);

        BlockState inputState =
                level.getBlockState(inputPos);

        return inputState.is(CreateMechanicalDrive.GEARBOX_INPUT.get())
                && inputState.hasProperty(CarGearboxInputBlock.FACING)
                && inputState.getValue(CarGearboxInputBlock.FACING) == facing;
    }

    private boolean isRedstoneForcedNeutral() {
        return level != null
                && level.hasNeighborSignal(worldPosition);
    }

    private void updateStressImpactInNetwork() {
        if (level == null || level.isClientSide || !hasNetwork()) {
            return;
        }

        KineticNetwork kineticNetwork = getOrCreateNetwork();
        if (kineticNetwork == null) {
            return;
        }

        kineticNetwork.updateStressFor(this, calculateStressApplied());
        networkDirty = true;
    }

    public float getVisualSpeedForSide(Direction side) {
        if (!hasSource()) {
            return getSpeed();
        }

        Direction sourceFacing = getSourceFacing();
        if (side == sourceFacing) {
            return getSpeed();
        }

        float modifier = getActivePosition().speedMultiplier();
        if (modifier == 0.0F) {
            return 0.0F;
        }

        Direction inputDirection = getInputDirection();
        Direction outputDirection = inputDirection.getOpposite();
        float directionMultiplier =
                getOutputDirectionMultiplier();

        if (sourceFacing == inputDirection
                && side == outputDirection) {
            return getSpeed()
                    * modifier
                    * directionMultiplier;
        }

        if (sourceFacing == outputDirection
                && side == inputDirection) {
            return getSpeed()
                    / modifier
                    * directionMultiplier;
        }

        return getSpeed();
    }

    public static void refreshFromInput(Level level, BlockPos inputPos) {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState inputState = level.getBlockState(inputPos);
        if (!inputState.is(CreateMechanicalDrive.GEARBOX_INPUT.get())) {
            return;
        }

        Direction facing = inputState.getValue(CarGearboxInputBlock.FACING);
        GearboxPosition position = readInputPosition(level, inputPos, inputState);
        BlockPos speedPos = inputPos.relative(facing.getOpposite());

        if (level.getBlockEntity(speedPos) instanceof CarGearboxSpeedBlockEntity speedBox) {
            speedBox.setActivePosition(position);
        }
    }

    public GearboxPosition readInputPositionOrNull() {
        if (level == null) {
            return null;
        }

        BlockState state = getBlockState();
        if (!state.hasProperty(CarGearboxSpeedBlock.FACING)) {
            return null;
        }

        Direction facing = state.getValue(CarGearboxSpeedBlock.FACING);
        BlockPos inputPos = worldPosition.relative(facing);
        BlockState inputState = level.getBlockState(inputPos);
        if (!inputState.is(CreateMechanicalDrive.GEARBOX_INPUT.get())
                || inputState.getValue(CarGearboxInputBlock.FACING) != facing) {
            return null;
        }

        return readInputPosition(level, inputPos, inputState);
    }

    private Direction getInputDirection() {
        BlockState state = getBlockState();
        if (state.hasProperty(CarGearboxSpeedBlock.FACING)) {
            return state.getValue(CarGearboxSpeedBlock.FACING);
        }

        Direction.Axis axis = state.hasProperty(CarGearboxSpeedBlock.AXIS)
                ? state.getValue(CarGearboxSpeedBlock.AXIS)
                : Direction.Axis.Z;
        return Direction.get(Direction.AxisDirection.POSITIVE, axis);
    }

    private static GearboxPosition readInputPosition(Level level, BlockPos inputPos, BlockState inputState) {
        if (level.getBlockEntity(inputPos) instanceof CarGearboxInputBlockEntity input) {
            return input.getRequestedPosition();
        }

        if (inputState.hasProperty(CarGearboxInputBlock.POSITION)) {
            return inputState.getValue(CarGearboxInputBlock.POSITION);
        }

        return GearboxPosition.NEUTRAL;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putInt(ACTIVE_POSITION_TAG, activePosition.id());
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        activePosition = GearboxPosition.byId(tag.getInt(ACTIVE_POSITION_TAG));
        super.read(tag, registries, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Direction inputDirection = getInputDirection();
        Direction outputDirection = inputDirection.getOpposite();
        float inputSpeed = getVisualSpeedForSide(inputDirection);
        float outputSpeed = getVisualSpeedForSide(outputDirection);

        CreateLang.builder()
                .add(Component.translatable("tooltip.mechanical_drive.gearbox.rear"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);
        tooltip.add(Component.translatable(
                "tooltip.mechanical_drive.gearbox.active_gear",
                activePosition.labelComponent().withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(
                "tooltip.mechanical_drive.gearbox.ratio",
                formatMultiplier(activePosition.speedMultiplier()).withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(
                "tooltip.mechanical_drive.gearbox.input_speed",
                Component.literal(formatSpeed(inputSpeed)).withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(
                "tooltip.mechanical_drive.gearbox.output_speed",
                Component.literal(formatSpeed(outputSpeed)).withStyle(ChatFormatting.AQUA)
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

    private static MutableComponent formatMultiplier(float multiplier) {
        if (multiplier == 0.0F) {
            return Component.translatable(
                    "mechanical_drive.gearbox.ratio.free"
            );
        }

        return Component.literal(
                String.format(Locale.ROOT, "%.2fx", multiplier)
        );
    }

    private static String formatSpeed(float speed) {
        return String.format(Locale.ROOT, "%.1f RPM", speed);
    }

    private static String formatStress(float value) {
        return String.format(Locale.ROOT, "%.1f SU", value);
    }
}
