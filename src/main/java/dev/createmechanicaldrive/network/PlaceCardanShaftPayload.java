package dev.createmechanicaldrive.network;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.createmechanicaldrive.content.cardan_shaft.CardanJointBlock;
import dev.createmechanicaldrive.content.cardan_shaft.CardanJointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record PlaceCardanShaftPayload(
        boolean cancelled,
        BlockPos pos,
        Direction facing
) implements CustomPacketPayload {
    private static final Map<UUID, PendingSelection> PENDING =
            new ConcurrentHashMap<>();

    public static final CustomPacketPayload.Type<PlaceCardanShaftPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "place_cardan_shaft"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            PlaceCardanShaftPayload
            > STREAM_CODEC =
            StreamCodec.ofMember(
                    PlaceCardanShaftPayload::write,
                    PlaceCardanShaftPayload::read
            );

    public static PlaceCardanShaftPayload place(
            UseOnContext context
    ) {
        BlockPos clickedPos =
                context.getClickedPos();
        BlockState clickedState =
                context.getLevel()
                        .getBlockState(clickedPos);
        BlockPos targetPos =
                clickedState.canBeReplaced()
                        ? clickedPos
                        : clickedPos.relative(
                        context.getClickedFace()
                );

        return place(
                targetPos,
                context.getClickedFace()
        );
    }

    public static PlaceCardanShaftPayload place(
            BlockPos pos,
            Direction facing
    ) {
        return new PlaceCardanShaftPayload(
                false,
                pos,
                facing
        );
    }

    public static PlaceCardanShaftPayload cancel() {
        return new PlaceCardanShaftPayload(
                true,
                BlockPos.ZERO,
                Direction.UP
        );
    }

    private void write(
            RegistryFriendlyByteBuf buffer
    ) {
        buffer.writeBoolean(
                cancelled
        );
        buffer.writeBlockPos(
                pos
        );
        buffer.writeEnum(
                facing
        );
    }

    private static PlaceCardanShaftPayload read(
            RegistryFriendlyByteBuf buffer
    ) {
        return new PlaceCardanShaftPayload(
                buffer.readBoolean(),
                buffer.readBlockPos(),
                buffer.readEnum(
                        Direction.class
                )
        );
    }

    public static void handle(
            PlaceCardanShaftPayload payload,
            IPayloadContext context
    ) {
        Player player =
                context.player();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (payload.cancelled()) {
            clearPending(
                    serverPlayer,
                    true
            );
            return;
        }

        if (!isHoldingCardan(
                serverPlayer
        )) {
            clearPending(
                    serverPlayer,
                    true
            );
            return;
        }

        PendingSelection pending =
                PENDING.get(
                        serverPlayer.getUUID()
                );

        if (pending == null) {
            placeFirstJoint(
                    serverPlayer,
                    payload
            );
            return;
        }

        placeSecondJoint(
                serverPlayer,
                pending,
                payload
        );
    }

    private static void placeFirstJoint(
            ServerPlayer player,
            PlaceCardanShaftPayload payload
    ) {
        Level level =
                player.level();

        if (!canPlaceAt(
                level,
                payload.pos()
        )) {
            return;
        }

        if (isPlayerTooFar(
                player,
                level,
                payload.pos()
        )) {
            return;
        }

        BlockState state =
                CreateMechanicalDrive.CARDAN_JOINT
                        .get()
                        .defaultBlockState()
                        .setValue(
                                CardanJointBlock.FACING,
                                payload.facing()
                        );

        level.setBlock(
                payload.pos(),
                state,
                3
        );
        playPlaceSound(
                level,
                payload.pos()
        );

        if (level.getBlockEntity(payload.pos())
                instanceof CardanJointBlockEntity joint) {
            joint.setPending(true);
            PENDING.put(
                    player.getUUID(),
                    new PendingSelection(
                            level.dimension(),
                            payload.pos(),
                            SableSubLevelHelper.getSubLevelId(
                                    level,
                                    payload.pos()
                            ),
                            payload.facing()
                    )
            );
        }
    }

    private static void placeSecondJoint(
            ServerPlayer player,
            PendingSelection pending,
            PlaceCardanShaftPayload payload
    ) {
        Level level =
                player.level();

        UUID targetSubLevelId =
                SableSubLevelHelper.getSubLevelId(
                        level,
                        payload.pos()
                );

        if (!level.dimension().equals(pending.dimension())
                || !level.isLoaded(pending.pos())
                || !Objects.equals(
                pending.subLevelId(),
                SableSubLevelHelper.getSubLevelId(
                        level,
                        pending.pos()
                )
        )
                || !canPlaceAt(level, payload.pos())
                || isSameSelection(
                pending,
                payload.pos(),
                targetSubLevelId
        )
                || isPlayerTooFar(
                player,
                level,
                payload.pos()
        )
                || !CardanJointBlockEntity.isGeometryValid(
                level,
                pending.pos(),
                pending.facing(),
                payload.pos(),
                payload.facing()
        )) {
            clearPending(
                    player,
                    true
            );
            return;
        }

        BlockEntity firstBlockEntity =
                level.getBlockEntity(
                        pending.pos()
                );

        if (!(firstBlockEntity instanceof CardanJointBlockEntity first)
                || !first.isPending()
                || first.hasLink()) {
            clearPending(
                    player,
                    true
            );
            return;
        }

        BlockState secondState =
                CreateMechanicalDrive.CARDAN_JOINT
                        .get()
                        .defaultBlockState()
                        .setValue(
                                CardanJointBlock.FACING,
                                payload.facing()
                        );

        level.setBlock(
                payload.pos(),
                secondState,
                3
        );
        playPlaceSound(
                level,
                payload.pos()
        );

        if (!(level.getBlockEntity(payload.pos())
                instanceof CardanJointBlockEntity second)) {
            clearPending(
                    player,
                    true
            );
            return;
        }

        if (!player.hasInfiniteMaterials()
                && !consumeCardan(player)) {
            level.destroyBlock(
                    payload.pos(),
                    false
            );
            clearPending(
                    player,
                    true
            );
            return;
        }

        first.createMutualLink(
                second
        );
        PENDING.remove(
                player.getUUID()
        );
    }

    private static void clearPending(
            ServerPlayer player,
            boolean removeBlock
    ) {
        PendingSelection pending =
                PENDING.remove(
                        player.getUUID()
                );

        if (pending == null) {
            return;
        }

        Level level =
                player.server.getLevel(
                        pending.dimension()
                );

        if (level == null) {
            return;
        }

        if (removeBlock
                && Objects.equals(
                pending.subLevelId(),
                SableSubLevelHelper.getSubLevelId(
                        level,
                        pending.pos()
                )
        )
                && level.getBlockEntity(pending.pos())
                instanceof CardanJointBlockEntity joint
                && joint.isPending()
                && !joint.hasLink()) {
            level.destroyBlock(
                    pending.pos(),
                    false
            );
        }
    }

    private static boolean isPlayerTooFar(
            ServerPlayer player,
            Level level,
            BlockPos pos
    ) {
        return player.position()
                .distanceToSqr(
                        SableSubLevelHelper.getWorldCenter(
                                level,
                                pos
                        )
                ) > 1024.0D;
    }

    private static boolean isSameSelection(
            PendingSelection pending,
            BlockPos pos,
            UUID subLevelId
    ) {
        return pending.pos().equals(pos)
                && Objects.equals(
                pending.subLevelId(),
                subLevelId
        );
    }

    private static boolean canPlaceAt(
            Level level,
            BlockPos pos
    ) {
        return level.isLoaded(pos)
                && level.getBlockState(pos)
                .canBeReplaced();
    }

    private static boolean isHoldingCardan(
            Player player
    ) {
        return player.getMainHandItem()
                .is(CreateMechanicalDrive.CARDAN_SHAFT_ITEM.get())
                || player.getOffhandItem()
                .is(CreateMechanicalDrive.CARDAN_SHAFT_ITEM.get());
    }

    private static boolean consumeCardan(
            ServerPlayer player
    ) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack =
                    player.getItemInHand(
                            hand
                    );

            if (stack.is(CreateMechanicalDrive.CARDAN_SHAFT_ITEM.get())) {
                stack.shrink(1);
                return true;
            }
        }

        return false;
    }

    private static void playPlaceSound(
            Level level,
            BlockPos pos
    ) {
        level.playSound(
                null,
                pos,
                SoundType.METAL.getPlaceSound(),
                SoundSource.BLOCKS,
                (SoundType.METAL.getVolume() + 1.0F) / 2.0F,
                SoundType.METAL.getPitch() * 0.8F
        );
    }

    private record PendingSelection(
            ResourceKey<Level> dimension,
            BlockPos pos,
            UUID subLevelId,
            Direction facing
    ) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
