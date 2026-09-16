package dev.createmechanicaldrive.network;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.createmechanicaldrive.content.rigid_link.RigidLinkJointBlockEntity;
import dev.createmechanicaldrive.content.rigid_link.RigidLinkItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record LinkRigidJointsPayload(boolean cancelled, BlockPos pos,
                                     RigidLinkJointBlockEntity.LinkType linkType)
        implements CustomPacketPayload {
    private static final Map<UUID, Selection> PENDING = new ConcurrentHashMap<>();

    public static final Type<LinkRigidJointsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateMechanicalDrive.MOD_ID,
                    "link_rigid_joints"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LinkRigidJointsPayload>
            STREAM_CODEC = StreamCodec.ofMember(
                    LinkRigidJointsPayload::write, LinkRigidJointsPayload::read);

    public static LinkRigidJointsPayload select(
            BlockPos pos, RigidLinkJointBlockEntity.LinkType linkType) {
        return new LinkRigidJointsPayload(false, pos, linkType);
    }

    public static LinkRigidJointsPayload cancel() {
        return new LinkRigidJointsPayload(true, BlockPos.ZERO,
                RigidLinkJointBlockEntity.LinkType.FREE);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(cancelled);
        buffer.writeBlockPos(pos);
        buffer.writeByte(linkType.ordinal());
    }

    private static LinkRigidJointsPayload read(RegistryFriendlyByteBuf buffer) {
        boolean cancelled = buffer.readBoolean();
        BlockPos pos = buffer.readBlockPos();
        RigidLinkJointBlockEntity.LinkType linkType = buffer.readUnsignedByte()
                == RigidLinkJointBlockEntity.LinkType.LIMITED.ordinal()
                ? RigidLinkJointBlockEntity.LinkType.LIMITED
                : RigidLinkJointBlockEntity.LinkType.FREE;
        return new LinkRigidJointsPayload(cancelled, pos, linkType);
    }

    public static void handle(LinkRigidJointsPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (payload.cancelled() || !isHoldingLink(player, payload.linkType())) {
            PENDING.remove(player.getUUID());
            return;
        }
        Level level = player.level();
        if (player.position().distanceToSqr(
                SableSubLevelHelper.getWorldCenter(level, payload.pos())) > 1024.0D) {
            fail(player, "message.mechanical_drive.rigid_link.failed.invalid");
            PENDING.remove(player.getUUID());
            return;
        }
        if (!(level.getBlockEntity(payload.pos())
                instanceof RigidLinkJointBlockEntity target)) {
            fail(player, "message.mechanical_drive.rigid_link.failed.not_joint");
            PENDING.remove(player.getUUID());
            return;
        }
        if (!target.canAccept(payload.linkType())) {
            fail(player, "message.mechanical_drive.rigid_link.failed.full");
            PENDING.remove(player.getUUID());
            return;
        }

        Selection selected = PENDING.get(player.getUUID());
        UUID targetSubLevel = SableSubLevelHelper.getSubLevelId(level, payload.pos());
        if (selected == null) {
            PENDING.put(player.getUUID(), new Selection(
                    level.dimension(), payload.pos(), targetSubLevel,
                    payload.linkType()));
            player.displayClientMessage(Component.translatable(
                    "message.mechanical_drive.rigid_link.started"), true);
            return;
        }

        PENDING.remove(player.getUUID());
        if (selected.linkType() != payload.linkType()
                || !selected.dimension().equals(level.dimension())
                || !level.isLoaded(selected.pos())
                || !Objects.equals(selected.subLevelId(),
                SableSubLevelHelper.getSubLevelId(level, selected.pos()))
                || selected.pos().equals(payload.pos())
                && Objects.equals(selected.subLevelId(), targetSubLevel)
                || !(level.getBlockEntity(selected.pos())
                instanceof RigidLinkJointBlockEntity first)) {
            fail(player, "message.mechanical_drive.rigid_link.failed.invalid");
            return;
        }

        RigidLinkJointBlockEntity.LinkResult result = first.createMutualLink(
                target, payload.linkType());
        if (result != RigidLinkJointBlockEntity.LinkResult.SUCCESS) {
            fail(player, switch (result) {
                case FULL -> "message.mechanical_drive.rigid_link.failed.full";
                case DUPLICATE -> "message.mechanical_drive.rigid_link.failed.duplicate";
                case INVALID_LENGTH -> "message.mechanical_drive.rigid_link.failed.length";
                case INVALID_ANGLE -> "message.mechanical_drive.rigid_link.failed.angle";
                case INVALID_PLANE -> "message.mechanical_drive.rigid_link.failed.plane";
                default -> "message.mechanical_drive.rigid_link.failed.invalid";
            });
            return;
        }

        if (!player.hasInfiniteMaterials()
                && !consumeLink(player, payload.linkType())) {
            RigidLinkJointBlockEntity.Connection connection = first.getLinks().stream()
                    .filter(link -> link.pos().equals(target.getBlockPos())
                            && Objects.equals(link.subLevelId(), targetSubLevel)
                            && link.linkType() == payload.linkType())
                    .findFirst().orElse(null);
            if (connection != null) {
                first.destroyConnection(connection, false);
            }
            fail(player, "message.mechanical_drive.rigid_link.failed.no_item");
            return;
        }
        player.displayClientMessage(Component.translatable(
                "message.mechanical_drive.rigid_link.linked"), true);
    }

    private static boolean isHoldingLink(
            Player player, RigidLinkJointBlockEntity.LinkType linkType) {
        return stackLinkType(player.getMainHandItem()) == linkType
                || stackLinkType(player.getOffhandItem()) == linkType;
    }

    private static boolean consumeLink(
            ServerPlayer player, RigidLinkJointBlockEntity.LinkType linkType) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stackLinkType(stack) == linkType) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static RigidLinkJointBlockEntity.LinkType stackLinkType(ItemStack stack) {
        return stack.getItem() instanceof RigidLinkItem item
                ? item.getLinkType() : null;
    }

    private static void fail(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private record Selection(ResourceKey<Level> dimension, BlockPos pos,
                             UUID subLevelId,
                             RigidLinkJointBlockEntity.LinkType linkType) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
