package dev.createmechanicaldrive.content.stirling_engine;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class StirlingEngineHeaterBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    static final int MAX_HEAT_TICKS = 20 * 60 * 10;
    static final int COOL_TICKS_PER_TICK = 2;
    static final float SLOWDOWN_START = 0.7F;
    private static final float SPEED_MULTIPLIER_STEP = 0.125F;
    private static final float DAMAGE_HEAT = 0.1F;
    static final String HEATED_TAG = "Heated";
    static final String RECEIVING_HEAT_TAG = "ReceivingHeat";
    static final String HEAT_TICKS_TAG = "HeatTicks";
    static final String COVER_TAG = "Cover";
    private static final String COVER_REMOVING_TAG = "CoverRemoving";
    private static final String COVER_ANIMATION_TICKS_TAG = "CoverAnimationTicks";
    private static final int COVER_ANIMATION_TICKS = 8;
    private static final int HEAT_SYNC_INTERVAL = 20;
    private static final int CLIENT_HEAT_RENDER_UPDATE_INTERVAL = 4;
    private static final float CLIENT_HEAT_LERP = 0.35F;

    private boolean heated;
    private boolean receivingHeat;
    private int heatTicks;
    private float visualHeatTicks;
    private boolean visualHeatInitialized;
    private boolean cover;
    private boolean coverRemoving;
    private int coverAnimationTicks = COVER_ANIMATION_TICKS;
    private float coverProgress = 1.0F;
    private float previousCoverProgress = 1.0F;

    public StirlingEngineHeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void tick() {
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            previousCoverProgress = coverProgress;
            if (cover && coverProgress < 1.0F) {
                coverProgress = Math.min(1.0F, coverProgress + 1.0F / COVER_ANIMATION_TICKS);
            } else if (coverRemoving && coverProgress > 0.0F) {
                coverProgress = Math.max(0.0F, coverProgress - 1.0F / COVER_ANIMATION_TICKS);
                if (coverProgress <= 0.0F) {
                    coverRemoving = false;
                }
            }
            tickClientHeat();
            return;
        }

        tickCoverAnimation();

        boolean nowReceivingHeat = hasStableHeat();
        int previousHeatTicks = heatTicks;
        boolean previousHeated = heated;
        boolean previousReceivingHeat = receivingHeat;

        receivingHeat = nowReceivingHeat;
        if (receivingHeat && !isOverheated()) {
            heatTicks = Math.min(
                    MAX_HEAT_TICKS,
                    heatTicks + 1
            );
        } else if (!receivingHeat && heatTicks > 0) {
            heatTicks = Math.max(
                    0,
                    heatTicks - COOL_TICKS_PER_TICK
            );
        }

        heated = receivingHeat && !isOverheated();
        updateLitState();

        if (causesHeatDamage(getHeatRatio())) {
            damageTouchingPlayers();
        }

        boolean heatChanged = previousHeatTicks != heatTicks;
        boolean heatStateChanged = previousHeated != heated
                || previousReceivingHeat != receivingHeat;

        if (heatChanged || heatStateChanged) {
            notifyOutput();
            setChanged();
            if (heatStateChanged || shouldSyncHeat()) {
                syncState();
            }
        }
    }

    public boolean isHeated() {
        return heated;
    }

    public boolean isReceivingHeat() {
        return receivingHeat;
    }

    public boolean isOverheated() {
        return heatTicks >= MAX_HEAT_TICKS;
    }

    public int getHeatTicks() {
        return heatTicks;
    }

    public boolean hasCover() {
        return cover;
    }

    public boolean isCoverRemoving() {
        return coverRemoving;
    }

    public float getCoverProgress(float partialTick) {
        return previousCoverProgress + (coverProgress - previousCoverProgress) * partialTick;
    }

    public void setCover(boolean cover) {
        if (this.cover == cover) {
            return;
        }

        this.cover = cover;
        coverRemoving = !cover;
        coverAnimationTicks = 0;

        if (level != null && level.isClientSide) {
            previousCoverProgress = cover ? 0.0F : 1.0F;
            coverProgress = previousCoverProgress;
        }

        if (level != null) {
            level.playSound(
                    null,
                    worldPosition,
                    cover ? SoundEvents.ITEM_FRAME_ADD_ITEM : SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                    SoundSource.BLOCKS,
                    0.75F,
                    cover ? 0.9F : 1.05F
            );
        }

        if (cover) {
            receivingHeat = false;
            heated = false;
            updateLitState();
            notifyOutput();
        }

        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    private void tickCoverAnimation() {
        if (level == null || coverAnimationTicks >= COVER_ANIMATION_TICKS) {
            return;
        }

        coverAnimationTicks++;
        if (coverAnimationTicks < COVER_ANIMATION_TICKS) {
            return;
        }

        if (!cover) {
            coverRemoving = false;
            syncState();
        }
    }

    private void syncState() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public float getHeatRatio() {
        return heatTicks / (float) MAX_HEAT_TICKS;
    }

    public static boolean causesHeatDamage(float heatRatio) {
        return heatRatio >= DAMAGE_HEAT;
    }

    public float getVisualHeatRatio() {
        if (level != null && level.isClientSide) {
            return visualHeatInitialized
                    ? visualHeatTicks / (float) MAX_HEAT_TICKS
                    : getHeatRatio();
        }

        return getHeatRatio();
    }

    public float getSpeedMultiplier() {
        float rawMultiplier = getRawSpeedMultiplier();
        if (rawMultiplier <= 0.0F || rawMultiplier >= 1.0F) {
            return rawMultiplier;
        }

        return Math.min(
                1.0F,
                (float) Math.ceil(rawMultiplier / SPEED_MULTIPLIER_STEP) * SPEED_MULTIPLIER_STEP
        );
    }

    private float getRawSpeedMultiplier() {
        float heatRatio = getHeatRatio();
        if (heatRatio <= SLOWDOWN_START) {
            return 1.0F;
        }

        if (heatRatio >= 1.0F) {
            return 0.0F;
        }

        return 1.0F - (heatRatio - SLOWDOWN_START) / (1.0F - SLOWDOWN_START);
    }

    private boolean hasStableHeat() {
        if (level == null || cover) {
            return false;
        }

        Direction facing = getBlockState().getValue(StirlingEngineHeaterBlock.FACING);
        BlockPos defaultHeatPos = facing.getAxis() == Direction.Axis.Y
                ? worldPosition.relative(facing.getOpposite())
                : worldPosition.below();

        BlockState defaultHeatState = level.getBlockState(defaultHeatPos);
        boolean blazeBurnerAbove = defaultHeatPos.equals(worldPosition.above())
                && defaultHeatState.getBlock() instanceof BlazeBurnerBlock;
        if (!blazeBurnerAbove && hasKindledHeat(defaultHeatState)) {
            return true;
        }

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) {
                continue;
            }

            BlockPos heatPos = worldPosition.relative(direction);
            if (heatPos.equals(defaultHeatPos)) {
                continue;
            }

            BlockState heatState = level.getBlockState(heatPos);
            if (heatState.getBlock() instanceof BlazeBurnerBlock && hasKindledHeat(heatState)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasKindledHeat(BlockState state) {
        return BlazeBurnerBlock.getHeatLevelOf(state).isAtLeast(HeatLevel.KINDLED);
    }

    private boolean isEngineRunning() {
        StirlingEnginePoweredShaftBlockEntity shaft = getPoweredShaft();
        return shaft != null && shaft.isRunning();
    }

    @Nullable
    private StirlingEnginePoweredShaftBlockEntity getPoweredShaft() {
        if (level == null) {
            return null;
        }

        BlockState state = getBlockState();
        if (!state.hasProperty(StirlingEngineHeaterBlock.FACING)) {
            return null;
        }

        BlockPos corePos = worldPosition.relative(state.getValue(StirlingEngineHeaterBlock.FACING));
        BlockState coreState = level.getBlockState(corePos);
        if (!coreState.is(CreateMechanicalDrive.STIRLING_ENGINE_CORE.get())) {
            return null;
        }

        Direction facing = coreState.getValue(StirlingEngineCoreBlock.FACING);
        BlockPos outputPos = corePos.relative(facing);
        BlockState outputState = level.getBlockState(outputPos);
        if (!outputState.is(CreateMechanicalDrive.STIRLING_ENGINE_OUTPUT.get())) {
            return null;
        }

        BlockPos shaftPos = StirlingEngineOutputBlock.getShaftPos(outputState, outputPos);
        if (level.getBlockEntity(shaftPos) instanceof StirlingEnginePoweredShaftBlockEntity shaft) {
            return shaft;
        }

        return null;
    }

    private void updateLitState() {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();
        if (state.hasProperty(StirlingEngineHeaterBlock.LIT)
                && state.getValue(StirlingEngineHeaterBlock.LIT) != heated) {
            level.setBlock(
                    worldPosition,
                    state.setValue(StirlingEngineHeaterBlock.LIT, heated),
                    3
            );
        }
    }

    private void damageTouchingPlayers() {
        if (level == null || level.getGameTime() % 20 != 0) {
            return;
        }

        AABB damageArea = new AABB(worldPosition).inflate(0.12D);
        List<Player> players = level.getEntitiesOfClass(Player.class, damageArea);
        for (Player player : players) {
            player.hurt(level.damageSources().hotFloor(), 1.0F);
        }
    }

    private void tickClientHeat() {
        int previousHeatTicks = heatTicks;
        if (receivingHeat && !isOverheated()) {
            heatTicks = Math.min(MAX_HEAT_TICKS, heatTicks + 1);
        } else if (!receivingHeat && heatTicks > 0) {
            heatTicks = Math.max(0, heatTicks - COOL_TICKS_PER_TICK);
        }
        heated = receivingHeat && !isOverheated();

        if (!visualHeatInitialized) {
            visualHeatTicks = heatTicks;
            visualHeatInitialized = true;
        }

        float previousVisualHeatTicks = visualHeatTicks;
        visualHeatTicks += (heatTicks - visualHeatTicks) * CLIENT_HEAT_LERP;
        if (Math.abs(heatTicks - visualHeatTicks) < 0.5F) {
            visualHeatTicks = heatTicks;
        }

        if (previousHeatTicks == heatTicks && Math.abs(previousVisualHeatTicks - visualHeatTicks) < 0.5F) {
            return;
        }

        if (level.getGameTime() % CLIENT_HEAT_RENDER_UPDATE_INTERVAL == 0) {
            refreshClientRender();
        }
    }

    private boolean shouldSyncHeat() {
        if (level == null) {
            return false;
        }

        if (heatTicks == 0 || heatTicks == MAX_HEAT_TICKS) {
            return true;
        }

        return level.getGameTime() % HEAT_SYNC_INTERVAL == 0;
    }

    private void notifyOutput() {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();
        if (!state.hasProperty(StirlingEngineHeaterBlock.FACING)) {
            return;
        }

        BlockPos corePos = worldPosition.relative(state.getValue(StirlingEngineHeaterBlock.FACING));
        BlockState coreState = level.getBlockState(corePos);
        if (!coreState.is(CreateMechanicalDrive.STIRLING_ENGINE_CORE.get())) {
            return;
        }

        BlockPos outputPos = corePos.relative(coreState.getValue(StirlingEngineCoreBlock.FACING));
        if (level.getBlockEntity(outputPos) instanceof StirlingEngineOutputBlockEntity output) {
            output.refreshPoweredShaft();
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder()
                .add(Component.translatable("tooltip.mechanical_drive.stirling_engine.heater.name").append(Component.literal(":")))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        ChatFormatting heatStateColor = receivingHeat ? ChatFormatting.GREEN : ChatFormatting.RED;
        Component heatState = Component.translatable(cover
                        ? "tooltip.mechanical_drive.stirling_engine.heater.covered"
                        : receivingHeat
                                ? "tooltip.mechanical_drive.stirling_engine.heater.receiving_heat"
                                : "tooltip.mechanical_drive.stirling_engine.heater.no_heat")
                .withStyle(heatStateColor);
        CreateLang.builder()
                .add(Component.translatable("tooltip.mechanical_drive.stirling_engine.heater.heat_source", heatState)
                        .withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.translatable(
                        "tooltip.mechanical_drive.stirling_engine.heater.heat",
                        Component.literal(formatHeatPercent()).withStyle(heatColor()),
                        heatBar()
                ).withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);

        if (isOverheated()) {
            CreateLang.builder()
                    .add(Component.translatable("tooltip.mechanical_drive.stirling_engine.heater.overheated")
                            .withStyle(ChatFormatting.RED))
                    .forGoggles(tooltip);
        }

        return true;
    }

    private String formatHeatPercent() {
        return String.format(Locale.ROOT, "%d%%", Math.round(getHeatRatio() * 100.0F));
    }

    private Component heatBar() {
        int segments = 10;
        int filled = Math.round(getHeatRatio() * segments);
        Component result = Component.literal("[").withStyle(ChatFormatting.DARK_GRAY);
        for (int i = 0; i < segments; i++) {
            result = result.copy().append(Component.literal(i < filled ? "|" : ".")
                    .withStyle(i < filled ? heatColor() : ChatFormatting.DARK_GRAY));
        }
        return result.copy().append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
    }

    private ChatFormatting heatColor() {
        float ratio = getHeatRatio();
        if (ratio >= 0.9F) {
            return ChatFormatting.RED;
        }
        if (ratio >= SLOWDOWN_START) {
            return ChatFormatting.GOLD;
        }
        return ChatFormatting.GREEN;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeHeat(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readHeat(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeHeat(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        int previousHeatTicks = heatTicks;
        boolean previousHeated = heated;
        boolean previousReceivingHeat = receivingHeat;
        boolean previousCover = cover;
        boolean previousCoverRemoving = coverRemoving;

        if (!tag.isEmpty()) {
            loadWithComponents(tag, registries);
        }

        refreshClientRenderIfChanged(
                previousHeatTicks,
                previousHeated,
                previousReceivingHeat,
                previousCover,
                previousCoverRemoving
        );
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            Connection net,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries
    ) {
        int previousHeatTicks = heatTicks;
        boolean previousHeated = heated;
        boolean previousReceivingHeat = receivingHeat;
        boolean previousCover = cover;
        boolean previousCoverRemoving = coverRemoving;

        CompoundTag tag = packet.getTag();
        if (!tag.isEmpty()) {
            loadWithComponents(tag, registries);
        }

        refreshClientRenderIfChanged(
                previousHeatTicks,
                previousHeated,
                previousReceivingHeat,
                previousCover,
                previousCoverRemoving
        );
    }

    private void refreshClientRenderIfChanged(
            int previousHeatTicks,
            boolean previousHeated,
            boolean previousReceivingHeat,
            boolean previousCover,
            boolean previousCoverRemoving
    ) {
        if (level == null || !level.isClientSide) {
            return;
        }

        if (previousHeatTicks == heatTicks
                && previousHeated == heated
                && previousReceivingHeat == receivingHeat
                && previousCover == cover
                && previousCoverRemoving == coverRemoving) {
            return;
        }

        if (!previousCover && cover) {
            previousCoverProgress = 0.0F;
            coverProgress = 0.0F;
        }
        if (!previousCoverRemoving && coverRemoving) {
            previousCoverProgress = 1.0F;
            coverProgress = 1.0F;
        } else if (previousCoverRemoving && !coverRemoving && !cover) {
            previousCoverProgress = 0.0F;
            coverProgress = 0.0F;
        }

        if (!visualHeatInitialized) {
            visualHeatTicks = heatTicks;
            visualHeatInitialized = true;
        }

        refreshClientRender();
    }

    private void refreshClientRender() {
        if (level == null || !level.isClientSide) {
            return;
        }

        requestModelDataUpdate();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 8);
    }

    private void writeHeat(CompoundTag tag) {
        tag.putBoolean(HEATED_TAG, heated);
        tag.putBoolean(RECEIVING_HEAT_TAG, receivingHeat);
        tag.putInt(HEAT_TICKS_TAG, heatTicks);
        tag.putBoolean(COVER_TAG, cover);
        tag.putBoolean(COVER_REMOVING_TAG, coverRemoving);
        tag.putInt(COVER_ANIMATION_TICKS_TAG, coverAnimationTicks);
    }

    private void readHeat(CompoundTag tag) {
        heated = tag.getBoolean(HEATED_TAG);
        receivingHeat = tag.getBoolean(RECEIVING_HEAT_TAG);
        heatTicks = tag.getInt(HEAT_TICKS_TAG);
        cover = tag.getBoolean(COVER_TAG);
        coverRemoving = tag.getBoolean(COVER_REMOVING_TAG);
        coverAnimationTicks = tag.contains(COVER_ANIMATION_TICKS_TAG)
                ? tag.getInt(COVER_ANIMATION_TICKS_TAG)
                : COVER_ANIMATION_TICKS;
    }
}
