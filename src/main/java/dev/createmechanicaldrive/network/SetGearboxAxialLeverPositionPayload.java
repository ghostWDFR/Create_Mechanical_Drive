package dev.createmechanicaldrive.network;

import dev.createmechanicaldrive.ControlDistance;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.gearbox.GearboxAxialLeverBlock;
import dev.createmechanicaldrive.content.gearbox.GearboxAxialLeverBlockEntity;
import dev.createmechanicaldrive.content.gearbox.GearboxPosition;
import dev.createmechanicaldrive.content.gearbox.CarGearboxInputBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public record SetGearboxAxialLeverPositionPayload(BlockPos pos, GearboxPosition position) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetGearboxAxialLeverPositionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "set_gear_shift_lever_position"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetGearboxAxialLeverPositionPayload> STREAM_CODEC =
            StreamCodec.ofMember(SetGearboxAxialLeverPositionPayload::write, SetGearboxAxialLeverPositionPayload::read);

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(position.id());
    }

    private static SetGearboxAxialLeverPositionPayload read(RegistryFriendlyByteBuf buffer) {
        return new SetGearboxAxialLeverPositionPayload(
                buffer.readBlockPos(),
                GearboxPosition.byId(buffer.readVarInt())
        );
    }

    public static void handle(SetGearboxAxialLeverPositionPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (ControlDistance.isTooFar(
                serverPlayer,
                payload.pos()
        )) {
            return;
        }

        Level level = serverPlayer.level();

        BlockState state = level.getBlockState(payload.pos);
        if (!state.is(CreateMechanicalDrive.GEARBOX_AXIAL_LEVER.get())) {
            return;
        }

        GearboxPosition previousPosition =
                state.getValue(GearboxAxialLeverBlock.POSITION);

        if (previousPosition != payload.position) {
            BlockState updatedState =
                    state.setValue(GearboxAxialLeverBlock.POSITION, payload.position);

            level.setBlock(payload.pos, updatedState, 3);

            level.playSound(
                    null,
                    payload.pos,
                    SoundEvents.LEVER_CLICK,
                    SoundSource.BLOCKS,
                    0.1F,
                    getShiftSoundPitch(payload.position)
            );
        }

        if (level.getBlockEntity(payload.pos) instanceof GearboxAxialLeverBlockEntity lever) {
            lever.setLeverPosition(payload.position);
            lever.setGateTarget(payload.position.gateX(), payload.position.gateY());
        }

        BlockPos inputPos = payload.pos.below();
        if (level.getBlockEntity(inputPos) instanceof CarGearboxInputBlockEntity input) {
            input.setRequestedPosition(payload.position);
        }
    }

    private static float getShiftSoundPitch(GearboxPosition position) {
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
