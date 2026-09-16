package dev.createmechanicaldrive.network;

import dev.createmechanicaldrive.ControlDistance;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.seats.SeatBackPosition;
import dev.createmechanicaldrive.content.seats.SeatBlock;
import dev.createmechanicaldrive.content.seats.SeatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetSeatBackPositionPayload(
        BlockPos pos,
        SeatBackPosition position
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<
            SetSeatBackPositionPayload
            > TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "set_seat_back_position"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SetSeatBackPositionPayload
            > STREAM_CODEC =
            StreamCodec.ofMember(
                    SetSeatBackPositionPayload::write,
                    SetSeatBackPositionPayload::read
            );

    private void write(
            RegistryFriendlyByteBuf buffer
    ) {
        buffer.writeBlockPos(
                pos
        );

        buffer.writeVarInt(
                position.id()
        );
    }

    private static SetSeatBackPositionPayload read(
            RegistryFriendlyByteBuf buffer
    ) {
        return new SetSeatBackPositionPayload(
                buffer.readBlockPos(),
                SeatBackPosition.byId(
                        buffer.readVarInt()
                )
        );
    }

    public static void handle(
            SetSeatBackPositionPayload payload,
            IPayloadContext context
    ) {
        Player player =
                context.player();

        if (!(
                player instanceof ServerPlayer serverPlayer
        )) {
            return;
        }

        if (
                !(
                        serverPlayer.isShiftKeyDown()
                                || serverPlayer.isCrouching()
                )
                        || !serverPlayer
                        .getMainHandItem()
                        .isEmpty()
                        || !serverPlayer
                        .getOffhandItem()
                        .isEmpty()
                        || ControlDistance.isTooFar(
                        serverPlayer,
                        payload.pos()
                )
        ) {
            return;
        }

        Level level =
                serverPlayer.level();

        BlockState state =
                level.getBlockState(
                        payload.pos()
                );

        if (!state.is(
                CreateMechanicalDrive
                        .SEAT
                        .get()
        )) {
            return;
        }

        SeatBackPosition previousPosition =
                state.getValue(
                        SeatBlock.BACK_POSITION
                );

        if (previousPosition != payload.position()) {
            level.setBlock(
                    payload.pos(),
                    state.setValue(
                            SeatBlock.BACK_POSITION,
                            payload.position()
                    ),
                    3
            );

            level.playSound(
                    null,
                    payload.pos(),
                    SoundEvents.LEVER_CLICK,
                    SoundSource.BLOCKS,
                    0.1F,
                    getSeatSoundPitch(
                            payload.position()
                    )
            );
        }

        if (
                level.getBlockEntity(
                        payload.pos()
                )
                        instanceof SeatBlockEntity seat
        ) {
            seat.setBackPosition(
                    payload.position()
            );

            seat.setReclineTarget(
                    payload.position()
                            .recline()
            );
        }
    }

    private static float getSeatSoundPitch(
            SeatBackPosition position
    ) {
        return switch (position) {
            case FORWARD -> 1.20F;
            case UPRIGHT -> 1.10F;
            case BACKWARD -> 1.00F;
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
