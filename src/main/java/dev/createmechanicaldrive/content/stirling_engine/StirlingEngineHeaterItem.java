package dev.createmechanicaldrive.content.stirling_engine;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Locale;

public class StirlingEngineHeaterItem extends BlockItem {

    private static final int PASSIVE_COOLING_PER_TICK = 1;
    private static final int WATER_COOL_TICKS_PER_TICK = 20;
    private static final int DAMAGE_INTERVAL = 20;
    private static final String COOLING_STARTED_AT_TAG =
            "HeaterCoolingStartedAt";


    public StirlingEngineHeaterItem(
            Block block,
            Item.Properties properties
    ) {
        super(
                block,
                properties
        );
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slotId,
            boolean isSelected
    ) {
        if (level.isClientSide) {
            return;
        }

        int heatTicks =
                getHeatTicks(
                        stack,
                        level.getGameTime()
                );

        if (heatTicks <= 0) {
            return;
        }

        float heatRatio =
                getHeatRatio(
                        heatTicks
                );

        if (entity instanceof Player player
                && !player.getAbilities().instabuild
                && StirlingEngineHeaterBlockEntity.causesHeatDamage(heatRatio)
                && level.getGameTime() % DAMAGE_INTERVAL == 0) {

            player.hurt(
                    level.damageSources().hotFloor(),
                    heatRatio >= 0.9F
                            ? 2.0F
                            : 1.0F
            );
        }
    }

    @Override
    public boolean onEntityItemUpdate(
            ItemStack stack,
            ItemEntity entity
    ) {
        Level level =
                entity.level();

        int heatTicks =
                getHeatTicks(
                        stack,
                        level.getGameTime()
                );

        if (heatTicks <= 0) {
            return false;
        }

        if (entity.isInWater()) {

            if (!level.isClientSide) {

                int cooledHeatTicks =
                        Math.max(
                                0,
                                heatTicks
                                        - WATER_COOL_TICKS_PER_TICK
                        );

                setHeatTicks(
                        stack,
                        cooledHeatTicks,
                        level.getGameTime()
                );

                entity.setItem(
                        stack.copy()
                );

            } else {

                float heatRatio =
                        getHeatRatio(
                                heatTicks
                        );

                if (heatRatio > 0.0F) {
                    spawnWaterCoolingSmoke(
                            level,
                            entity,
                            heatRatio
                    );
                }
            }
        }

        return false;
    }

    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {
        Level level =
                context.getLevel();

        ItemStack stack =
                context.getItemInHand();

        if (!level.isClientSide) {

            int effectiveHeat =
                    getHeatTicks(
                            stack,
                            level.getGameTime()
                    );

            setHeatTicks(
                    stack,
                    effectiveHeat,
                    level.getGameTime()
            );
        }

        return super.useOn(
                context
        );
    }

    private static void spawnWaterCoolingSmoke(
            Level level,
            ItemEntity entity,
            float heatRatio
    ) {
        float clampedHeat =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                heatRatio
                        )
                );

        float expectedParticles =
                clampedHeat * 4.0F;

        int particleCount =
                (int) expectedParticles;

        if (level.random.nextFloat()
                < expectedParticles - particleCount) {
            particleCount++;
        }

        if (particleCount <= 0) {
            return;
        }

        for (int i = 0;
             i < particleCount;
             i++) {

            double spread =
                    0.2D
                            + 0.3D
                            * clampedHeat;

            double x =
                    entity.getX()
                            + (level.random.nextDouble() - 0.5D)
                            * spread;

            double y =
                    entity.getY()
                            + 0.15D
                            + level.random.nextDouble()
                            * (0.2D
                            + 0.15D
                            * clampedHeat);

            double z =
                    entity.getZ()
                            + (level.random.nextDouble() - 0.5D)
                            * spread;

            double xSpeed =
                    (level.random.nextDouble() - 0.5D)
                            * (0.02D
                            + 0.03D
                            * clampedHeat);

            double ySpeed =
                    0.025D
                            + level.random.nextDouble()
                            * 0.035D
                            + clampedHeat
                            * 0.055D;

            double zSpeed =
                    (level.random.nextDouble() - 0.5D)
                            * (0.02D
                            + 0.03D
                            * clampedHeat);

            level.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    x,
                    y,
                    z,
                    xSpeed,
                    ySpeed,
                    zSpeed
            );
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(
            ItemStack oldStack,
            ItemStack newStack,
            boolean slotChanged
    ) {
        if (slotChanged) {
            return true;
        }

        return oldStack.getItem()
                != newStack.getItem();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(
                stack,
                context,
                tooltipComponents,
                tooltipFlag
        );

        Level level =
                context.level();

        int heatTicks =
                level != null
                        ? getHeatTicks(
                        stack,
                        level.getGameTime()
                )
                        : getStoredHeatTicks(
                        stack
                );

        if (heatTicks <= 0) {
            return;
        }

        tooltipComponents.add(
                Component.translatable(
                        "tooltip.mechanical_drive.stirling_engine.heater.item_heat",
                        String.format(
                                Locale.ROOT,
                                "%d%%",
                                Math.round(
                                        getHeatRatio(
                                                heatTicks
                                        ) * 100.0F
                                )
                        )
                ).withStyle(
                        ChatFormatting.RED
                )
        );
    }

    public static int getHeatTicks(
            ItemStack stack
    ) {
        return getStoredHeatTicks(
                stack
        );
    }

    public static int getHeatTicks(
            ItemStack stack,
            long currentGameTime
    ) {
        CustomData data =
                stack.getOrDefault(
                        DataComponents.BLOCK_ENTITY_DATA,
                        CustomData.EMPTY
                );

        if (data.isEmpty()) {
            return 0;
        }

        CompoundTag tag =
                data.copyTag();

        int storedHeatTicks =
                clampHeatTicks(
                        tag.getInt(
                                StirlingEngineHeaterBlockEntity.HEAT_TICKS_TAG
                        )
                );

        if (storedHeatTicks <= 0) {
            return 0;
        }

        if (!tag.contains(
                COOLING_STARTED_AT_TAG
        )) {
            return storedHeatTicks;
        }

        long coolingStartedAt =
                tag.getLong(
                        COOLING_STARTED_AT_TAG
                );

        long elapsedTicks =
                Math.max(
                        0L,
                        currentGameTime
                                - coolingStartedAt
                );

        long cooledAmount =
                elapsedTicks
                        * PASSIVE_COOLING_PER_TICK;

        if (cooledAmount
                >= storedHeatTicks) {
            return 0;
        }

        return clampHeatTicks(
                storedHeatTicks
                        - (int) cooledAmount
        );
    }


    private static int getStoredHeatTicks(
            ItemStack stack
    ) {
        CustomData data =
                stack.getOrDefault(
                        DataComponents.BLOCK_ENTITY_DATA,
                        CustomData.EMPTY
                );

        if (data.isEmpty()) {
            return 0;
        }

        return clampHeatTicks(
                data.copyTag().getInt(
                        StirlingEngineHeaterBlockEntity.HEAT_TICKS_TAG
                )
        );
    }

    public static void setHeatTicks(
            ItemStack stack,
            int heatTicks
    ) {
        int clampedHeatTicks =
                clampHeatTicks(
                        heatTicks
                );

        if (clampedHeatTicks <= 0) {
            stack.remove(
                    DataComponents.BLOCK_ENTITY_DATA
            );

            return;
        }

        CompoundTag tag =
                stack.getOrDefault(
                        DataComponents.BLOCK_ENTITY_DATA,
                        CustomData.EMPTY
                ).copyTag();

        tag.putBoolean(
                StirlingEngineHeaterBlockEntity.HEATED_TAG,
                false
        );

        tag.putBoolean(
                StirlingEngineHeaterBlockEntity.RECEIVING_HEAT_TAG,
                false
        );

        tag.putInt(
                StirlingEngineHeaterBlockEntity.HEAT_TICKS_TAG,
                clampedHeatTicks
        );

        tag.remove(
                COOLING_STARTED_AT_TAG
        );

        BlockItem.setBlockEntityData(
                stack,
                CreateMechanicalDrive
                        .STIRLING_ENGINE_HEATER_BLOCK_ENTITY
                        .get(),
                tag
        );
    }

    public static void setHeatTicks(
            ItemStack stack,
            int heatTicks,
            long currentGameTime
    ) {
        int clampedHeatTicks =
                clampHeatTicks(
                        heatTicks
                );

        if (clampedHeatTicks <= 0) {
            stack.remove(
                    DataComponents.BLOCK_ENTITY_DATA
            );

            return;
        }

        CompoundTag tag =
                stack.getOrDefault(
                        DataComponents.BLOCK_ENTITY_DATA,
                        CustomData.EMPTY
                ).copyTag();

        tag.putBoolean(
                StirlingEngineHeaterBlockEntity.HEATED_TAG,
                false
        );

        tag.putBoolean(
                StirlingEngineHeaterBlockEntity.RECEIVING_HEAT_TAG,
                false
        );

        tag.putInt(
                StirlingEngineHeaterBlockEntity.HEAT_TICKS_TAG,
                clampedHeatTicks
        );

        tag.putLong(
                COOLING_STARTED_AT_TAG,
                currentGameTime
        );

        BlockItem.setBlockEntityData(
                stack,
                CreateMechanicalDrive
                        .STIRLING_ENGINE_HEATER_BLOCK_ENTITY
                        .get(),
                tag
        );
    }

    public static float getHeatRatio(
            ItemStack stack
    ) {
        return getHeatRatio(
                getStoredHeatTicks(
                        stack
                )
        );
    }


    public static float getHeatRatio(
            ItemStack stack,
            long currentGameTime
    ) {
        return getHeatRatio(
                getHeatTicks(
                        stack,
                        currentGameTime
                )
        );
    }

    public static int getHeatTint(
            float heatRatio
    ) {
        float ratio =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                heatRatio
                        )
                );

        float aggressiveHeat =
                (float) Math.sqrt(
                        ratio
                );

        int red =
                255;

        int green =
                Math.round(
                        255.0F
                                - 220.0F
                                * aggressiveHeat
                );

        int blue =
                Math.round(
                        255.0F
                                - 245.0F
                                * aggressiveHeat
                );

        return red << 16
                | green << 8
                | blue;
    }


    private static float getHeatRatio(
            int heatTicks
    ) {
        return clampHeatTicks(
                heatTicks
        ) / (float) StirlingEngineHeaterBlockEntity.MAX_HEAT_TICKS;
    }


    private static int clampHeatTicks(
            int heatTicks
    ) {
        return Math.max(
                0,
                Math.min(
                        StirlingEngineHeaterBlockEntity.MAX_HEAT_TICKS,
                        heatTicks
                )
        );
    }
}