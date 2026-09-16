package dev.createmechanicaldrive.network;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.steering_wheel.SteeringWheelBlockEntity;
import dev.createmechanicaldrive.ControlDistance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetSteeringWheelAnglePayload(
        BlockPos pos,
        float angle,
        boolean held
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<
            SetSteeringWheelAnglePayload
            > TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "set_steering_wheel_angle"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SetSteeringWheelAnglePayload
            > STREAM_CODEC =
            StreamCodec.ofMember(
                    SetSteeringWheelAnglePayload::write,
                    SetSteeringWheelAnglePayload::read
            );

    private void write(
            RegistryFriendlyByteBuf buffer
    ) {
        buffer.writeBlockPos(pos);
        buffer.writeFloat(angle);
        buffer.writeBoolean(held);
    }

    private static SetSteeringWheelAnglePayload read(
            RegistryFriendlyByteBuf buffer
    ) {
        return new SetSteeringWheelAnglePayload(
                buffer.readBlockPos(),
                buffer.readFloat(),
                buffer.readBoolean()
        );
    }

    public static void handle(
            SetSteeringWheelAnglePayload payload,
            IPayloadContext context
    ) {
        Player player =
                context.player();

        if (!(player
                instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (ControlDistance.isTooFar(
                serverPlayer,
                payload.pos()
        )) {
            return;
        }

        if (!Float.isFinite(
                payload.angle()
        )) {
            return;
        }

        Level level =
                serverPlayer.level();

        if (!(level.getBlockEntity(
                payload.pos()
        ) instanceof SteeringWheelBlockEntity steeringWheel)) {
            return;
        }

        if (payload.held()
                && !serverPlayer
                .getMainHandItem()
                .isEmpty()) {
            if (steeringWheel.isControlledBy(
                    serverPlayer.getUUID()
            )) {
                steeringWheel.setHeld(
                        false,
                        null
                );
            }

            return;
        }

        if (ControlDistance.isTooFar(
                serverPlayer,
                payload.pos()
        )) {
            if (steeringWheel.isControlledBy(
                    serverPlayer.getUUID()
            )) {
                steeringWheel.setHeld(
                        false,
                        null
                );
            }

            return;
        }

        if (payload.held()) {
            if (steeringWheel.isControlledByAnother(
                    serverPlayer.getUUID()
            )) {
                return;
            }
        } else if (!steeringWheel.isControlledBy(
                serverPlayer.getUUID()
        )) {
            return;
        }

        steeringWheel.setTargetAngle(
                payload.angle()
        );

        steeringWheel.setHeld(
                payload.held(),
                payload.held()
                        ? serverPlayer.getUUID()
                        : null
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
