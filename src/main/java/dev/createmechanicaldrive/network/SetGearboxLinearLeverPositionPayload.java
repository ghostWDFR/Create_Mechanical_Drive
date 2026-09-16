package dev.createmechanicaldrive.network;

import dev.createmechanicaldrive.ControlDistance;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.gearbox.CarGearboxInputBlockEntity;
import dev.createmechanicaldrive.content.gearbox.GearboxLinearLeverBlock;
import dev.createmechanicaldrive.content.gearbox.GearboxLinearLeverBlockEntity;
import dev.createmechanicaldrive.content.gearbox.GearboxPosition;
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

public record SetGearboxLinearLeverPositionPayload(
        BlockPos pos,
        GearboxPosition position
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<
            SetGearboxLinearLeverPositionPayload
            > TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "set_gearbox_linear_lever_position"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SetGearboxLinearLeverPositionPayload
            > STREAM_CODEC =
            StreamCodec.ofMember(
                    SetGearboxLinearLeverPositionPayload::write,
                    SetGearboxLinearLeverPositionPayload::read
            );

    private void write(
            RegistryFriendlyByteBuf buffer
    ) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(position.id());
    }

    private static SetGearboxLinearLeverPositionPayload read(
            RegistryFriendlyByteBuf buffer
    ) {
        return new SetGearboxLinearLeverPositionPayload(
                buffer.readBlockPos(),
                GearboxPosition.byId(
                        buffer.readVarInt()
                )
        );
    }

    public static void handle(
            SetGearboxLinearLeverPositionPayload payload,
            IPayloadContext context
    ) {
        Player player =
                context.player();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (ControlDistance.isTooFar(
                serverPlayer,
                payload.pos()
        )) {
            return;
        }

        Level level =
                serverPlayer.level();

        BlockState state =
                level.getBlockState(payload.pos());

        if (!state.is(
                CreateMechanicalDrive
                        .GEARBOX_LINEAR_LEVER
                        .get()
        )) {
            return;
        }

        GearboxPosition previousPosition =
                state.getValue(
                        GearboxLinearLeverBlock.POSITION
                );

        if (previousPosition != payload.position()) {
            BlockState updatedState =
                    state.setValue(
                            GearboxLinearLeverBlock.POSITION,
                            payload.position()
                    );

            level.setBlock(
                    payload.pos(),
                    updatedState,
                    3
            );

            level.playSound(
                    null,
                    payload.pos(),
                    SoundEvents.LEVER_CLICK,
                    SoundSource.BLOCKS,
                    0.1F,
                    getShiftSoundPitch(
                            payload.position()
                    )
            );
        }

        if (level.getBlockEntity(payload.pos())
                instanceof GearboxLinearLeverBlockEntity lever) {
            lever.setLeverPosition(
                    payload.position()
            );

            lever.setLinearTarget(
                    payload.position().linearValue()
            );
        }

        BlockPos inputPos =
                payload.pos().below();

        if (level.getBlockEntity(inputPos)
                instanceof CarGearboxInputBlockEntity input) {
            input.setRequestedPosition(
                    payload.position()
            );
        }
    }

    private static float getShiftSoundPitch(
            GearboxPosition position
    ) {
        return switch (position) {
            case REVERSE -> 0.75F;
            case FIRST -> 0.90F;
            case NEUTRAL -> 1.00F;
            case SECOND -> 1.10F;
            case THIRD -> 1.25F;
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
