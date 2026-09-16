package dev.createmechanicaldrive.network;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutBlock;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record PlaceSuspensionStrutPayload(
        boolean cancelled,
        BlockPos pos,
        Direction facing
) implements CustomPacketPayload {
    public static final double MAX_ENDPOINT_DISTANCE =
            SuspensionStrutBlockEntity.MAX_LENGTH;
    private static final double MIN_ENDPOINT_DISTANCE = 1.0E-4D;
    private static final double DISTANCE_EPSILON = 1.0E-5D;

    private static final Map<UUID, PendingSelection> PENDING =
            new ConcurrentHashMap<>();

    public static final Type<PlaceSuspensionStrutPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "place_suspension_strut"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceSuspensionStrutPayload>
            STREAM_CODEC = StreamCodec.ofMember(
                    PlaceSuspensionStrutPayload::write,
                    PlaceSuspensionStrutPayload::read
            );

    public static PlaceSuspensionStrutPayload place(UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = context.getLevel().getBlockState(clickedPos);
        BlockPos targetPos = clickedState.canBeReplaced()
                ? clickedPos
                : clickedPos.relative(context.getClickedFace());
        return place(targetPos, context.getClickedFace());
    }

    public static PlaceSuspensionStrutPayload place(
            BlockPos pos,
            Direction facing
    ) {
        return new PlaceSuspensionStrutPayload(false, pos, facing);
    }

    public static PlaceSuspensionStrutPayload cancel() {
        return new PlaceSuspensionStrutPayload(true, BlockPos.ZERO, Direction.UP);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(cancelled);
        buffer.writeBlockPos(pos);
        buffer.writeEnum(facing);
    }

    private static PlaceSuspensionStrutPayload read(
            RegistryFriendlyByteBuf buffer
    ) {
        return new PlaceSuspensionStrutPayload(
                buffer.readBoolean(),
                buffer.readBlockPos(),
                buffer.readEnum(Direction.class)
        );
    }

    public static void handle(
            PlaceSuspensionStrutPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (payload.cancelled()) {
            clearPending(player, true);
            return;
        }

        if (!isHoldingStrut(player)) {
            clearPending(player, true);
            return;
        }

        PendingSelection pending = PENDING.get(player.getUUID());
        if (pending == null) {
            placeFirstEnd(player, payload);
        } else {
            placeSecondEnd(player, pending, payload);
        }
    }

    private static void placeFirstEnd(
            ServerPlayer player,
            PlaceSuspensionStrutPayload payload
    ) {
        Level level = player.level();
        BlockState state = strutState(payload.facing());

        if (!canPlaceAt(level, payload.pos())
                || !state.canSurvive(level, payload.pos())
                || isPlayerTooFar(player, level, payload.pos())) {
            return;
        }

        level.setBlock(payload.pos(), state, 3);

        if (!(level.getBlockEntity(payload.pos())
                instanceof SuspensionStrutBlockEntity strut)) {
            level.destroyBlock(payload.pos(), false);
            return;
        }

        strut.setPending(true);
        PENDING.put(
                player.getUUID(),
                new PendingSelection(
                        level.dimension(),
                        payload.pos(),
                        SableSubLevelHelper.getSubLevelId(level, payload.pos()),
                        payload.facing()
                )
        );
        playPlaceSound(level, payload.pos());
    }

    private static void placeSecondEnd(
            ServerPlayer player,
            PendingSelection pending,
            PlaceSuspensionStrutPayload payload
    ) {
        Level level = player.level();
        UUID targetSubLevel = SableSubLevelHelper.getSubLevelId(level, payload.pos());
        BlockState state = strutState(payload.facing());
        boolean compactPlacement = isCompactSelection(
                pending,
                payload.pos(),
                payload.facing(),
                targetSubLevel
        );

        if (!level.dimension().equals(pending.dimension())
                || !level.isLoaded(pending.pos())
                || !Objects.equals(
                pending.subLevelId(),
                SableSubLevelHelper.getSubLevelId(level, pending.pos())
        )
                || !compactPlacement && !canPlaceAt(level, payload.pos())
                || !state.canSurvive(level, payload.pos())
                || !compactPlacement
                && isSameSelection(pending, payload.pos(), targetSubLevel)
                || isPlayerTooFar(player, level, payload.pos())
                || !isDistanceValid(
                level,
                pending.pos(),
                pending.facing(),
                payload.pos(),
                payload.facing()
        )
                || !SuspensionStrutBlockEntity.isAngleValid(
                level,
                pending.pos(),
                pending.facing(),
                payload.pos(),
                payload.facing()
        )) {
            clearPending(player, true);
            return;
        }

        if (!(level.getBlockEntity(pending.pos())
                instanceof SuspensionStrutBlockEntity first)
                || !first.isPending()
                || first.hasLink()) {
            clearPending(player, true);
            return;
        }

        if (compactPlacement) {
            if (!player.hasInfiniteMaterials() && !consumeStrut(player)) {
                clearPending(player, true);
                return;
            }
            first.createCompactLink(payload.facing());
            PENDING.remove(player.getUUID());
            playPlaceSound(level, payload.pos());
            return;
        }

        level.setBlock(payload.pos(), state, 3);
        if (!(level.getBlockEntity(payload.pos())
                instanceof SuspensionStrutBlockEntity second)) {
            level.destroyBlock(payload.pos(), false);
            clearPending(player, true);
            return;
        }

        if (!player.hasInfiniteMaterials() && !consumeStrut(player)) {
            level.destroyBlock(payload.pos(), false);
            clearPending(player, true);
            return;
        }

        double endpointDistance = endpointDistance(
                level,
                pending.pos(),
                pending.facing(),
                payload.pos(),
                payload.facing()
        );
        double restLength = Mth.clamp(
                endpointDistance,
                SuspensionStrutBlockEntity.MIN_LENGTH,
                SuspensionStrutBlockEntity.MAX_LENGTH
        );
        first.createMutualLink(second, restLength);
        PENDING.remove(player.getUUID());
        playPlaceSound(level, payload.pos());
    }

    private static BlockState strutState(Direction facing) {
        return CreateMechanicalDrive.SUSPENSION_STRUT_JOINT
                .get()
                .defaultBlockState()
                .setValue(SuspensionStrutBlock.FACING, facing);
    }

    private static void clearPending(
            ServerPlayer player,
            boolean removeBlock
    ) {
        PendingSelection pending = PENDING.remove(player.getUUID());
        if (pending == null) {
            return;
        }

        Level level = player.server.getLevel(pending.dimension());
        if (level == null) {
            return;
        }

        if (removeBlock
                && Objects.equals(
                pending.subLevelId(),
                SableSubLevelHelper.getSubLevelId(level, pending.pos())
        )
                && level.getBlockEntity(pending.pos())
                instanceof SuspensionStrutBlockEntity strut
                && strut.isPending()
                && !strut.hasLink()) {
            level.destroyBlock(pending.pos(), false);
        }
    }

    public static boolean isDistanceValid(
            Level level,
            BlockPos first,
            Direction firstFacing,
            BlockPos second,
            Direction secondFacing
    ) {
        double distance = endpointDistance(
                level,
                first,
                firstFacing,
                second,
                secondFacing
        );
        return distance > MIN_ENDPOINT_DISTANCE
                && distance - DISTANCE_EPSILON <= MAX_ENDPOINT_DISTANCE;
    }

    public static double endpointDistance(
            Level level,
            BlockPos first,
            Direction firstFacing,
            BlockPos second,
            Direction secondFacing
    ) {
        return SuspensionStrutBlockEntity.attachmentPoint(
                level,
                first,
                firstFacing
        ).distanceTo(SuspensionStrutBlockEntity.attachmentPoint(
                level,
                second,
                secondFacing
        ));
    }

    private static boolean canPlaceAt(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockState(pos).canBeReplaced();
    }

    private static boolean isSameSelection(
            PendingSelection pending,
            BlockPos pos,
            UUID subLevelId
    ) {
        return pending.pos().equals(pos)
                && Objects.equals(pending.subLevelId(), subLevelId);
    }

    private static boolean isCompactSelection(
            PendingSelection pending,
            BlockPos pos,
            Direction facing,
            UUID subLevelId
    ) {
        return pending.pos().equals(pos)
                && Objects.equals(pending.subLevelId(), subLevelId)
                && pending.facing().getOpposite() == facing;
    }

    private static boolean isPlayerTooFar(
            ServerPlayer player,
            Level level,
            BlockPos pos
    ) {
        return player.position().distanceToSqr(
                SableSubLevelHelper.getWorldCenter(level, pos)
        ) > 1024.0D;
    }

    private static boolean isHoldingStrut(Player player) {
        return player.getMainHandItem().is(CreateMechanicalDrive.SUSPENSION_STRUT_ITEM.get())
                || player.getOffhandItem().is(CreateMechanicalDrive.SUSPENSION_STRUT_ITEM.get());
    }

    private static boolean consumeStrut(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(CreateMechanicalDrive.SUSPENSION_STRUT_ITEM.get())) {
                stack.shrink(1);
                return true;
            }
        }

        return false;
    }

    private static void playPlaceSound(Level level, BlockPos pos) {
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
