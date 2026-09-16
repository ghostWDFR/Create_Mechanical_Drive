package dev.createmechanicaldrive.client;

import dev.createmechanicaldrive.content.engine.EngineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

public final class EngineClientEffects {

    private static final ResourceLocation
            PORTABLE_ENGINE_PUFF_SOUND =
            ResourceLocation.fromNamespaceAndPath(
                    "simulated",
                    "block.portable_engine.puff"
            );

    private static final ResourceLocation
            PORTABLE_ENGINE_AMBIENT_SOUND =
            ResourceLocation.fromNamespaceAndPath(
                    "simulated",
                    "block.portable_engine.ambient"
            );

    private EngineClientEffects() {
    }

    public static void tick(
            EngineBlockEntity engine
    ) {
        Level level =
                engine.getLevel();

        if (level == null
                || !level.isClientSide
                || level.getGameTime() % 5L != 0L) {
            return;
        }

        playSimulatedSound(
                level,
                engine,
                PORTABLE_ENGINE_PUFF_SOUND,
                0.8F,
                1.0F
        );

        if (level.getRandom()
                .nextFloat() < 0.05F) {
            playSimulatedSound(
                    level,
                    engine,
                    PORTABLE_ENGINE_AMBIENT_SOUND,
                    0.8F,
                    1.0F
            );
        }
    }

    private static void playSimulatedSound(
            Level level,
            EngineBlockEntity engine,
            ResourceLocation soundId,
            float volume,
            float pitch
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        BlockPos position =
                engine.getBlockPos();

        double soundX =
                position.getX() + 0.5D;

        double soundY =
                position.getY() + 0.5D;

        double soundZ =
                position.getZ() + 0.5D;

        double distance =
                Math.sqrt(
                        minecraft.player.distanceToSqr(
                                soundX,
                                soundY,
                                soundZ
                        )
                );

        double fullVolumeDistance = 4.0D;
        double maxDistance = 22.0D;

        if (distance >= maxDistance) {
            return;
        }

        float distanceVolume = 1.0F;

        if (distance > fullVolumeDistance) {
            distanceVolume =
                    (float) (
                            1.0D
                                    - (
                                    distance
                                            - fullVolumeDistance
                            )
                                    / (
                                    maxDistance
                                            - fullVolumeDistance
                            )
                    );
        }

        float finalVolume =
                volume * distanceVolume;

        BuiltInRegistries.SOUND_EVENT
                .getOptional(soundId)
                .ifPresent(soundEvent ->
                        level.playLocalSound(
                                        soundX,
                                        soundY,
                                        soundZ,
                                        soundEvent,
                                        SoundSource.BLOCKS,
                                        finalVolume,
                                        pitch,
                                        false
                                )
                );
    }
}
