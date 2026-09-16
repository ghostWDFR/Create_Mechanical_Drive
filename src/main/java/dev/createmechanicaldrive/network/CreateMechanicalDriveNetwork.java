package dev.createmechanicaldrive.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CreateMechanicalDriveNetwork {
    private CreateMechanicalDriveNetwork() {
    }

    public static void registerPayloads(
            RegisterPayloadHandlersEvent event
    ) {
        PayloadRegistrar registrar =
                event.registrar("1");

        registrar.playToServer(
                SetGearboxAxialLeverPositionPayload.TYPE,
                SetGearboxAxialLeverPositionPayload.STREAM_CODEC,
                SetGearboxAxialLeverPositionPayload::handle
        );

        registrar.playToServer(
                SetGearboxLinearLeverPositionPayload.TYPE,
                SetGearboxLinearLeverPositionPayload.STREAM_CODEC,
                SetGearboxLinearLeverPositionPayload::handle
        );

        registrar.playToServer(
                SetSteeringWheelAnglePayload.TYPE,
                SetSteeringWheelAnglePayload.STREAM_CODEC,
                SetSteeringWheelAnglePayload::handle
        );

        registrar.playToServer(
                SetSeatBackPositionPayload.TYPE,
                SetSeatBackPositionPayload.STREAM_CODEC,
                SetSeatBackPositionPayload::handle
        );

        registrar.playToServer(
                PlaceChainLinkagePayload.TYPE,
                PlaceChainLinkagePayload.STREAM_CODEC,
                PlaceChainLinkagePayload::handle
        );

        registrar.playToServer(
                PlaceCardanShaftPayload.TYPE,
                PlaceCardanShaftPayload.STREAM_CODEC,
                PlaceCardanShaftPayload::handle
        );

        registrar.playToServer(
                PlaceSuspensionStrutPayload.TYPE,
                PlaceSuspensionStrutPayload.STREAM_CODEC,
                PlaceSuspensionStrutPayload::handle
        );

        registrar.playToServer(
                InteractCompactStrutPayload.TYPE,
                InteractCompactStrutPayload.STREAM_CODEC,
                InteractCompactStrutPayload::handle
        );

        registrar.playToServer(
                LinkRigidJointsPayload.TYPE,
                LinkRigidJointsPayload.STREAM_CODEC,
                LinkRigidJointsPayload::handle
        );
    }
}
