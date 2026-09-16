package dev.createmechanicaldrive.network;

import com.simibubi.create.content.equipment.wrench.WrenchItem;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.adjustment_wrench.AdjustmentWrenchItem;
import dev.createmechanicaldrive.content.suspension_strut.CompactStrutSupportShapes;
import dev.createmechanicaldrive.content.suspension_strut.SpringTuningWrenchItem;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutBlock;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record InteractCompactStrutPayload(
        BlockPos supportPos,
        Direction facing,
        InteractionHand hand
) implements CustomPacketPayload {
    public static final Type<InteractCompactStrutPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateMechanicalDrive.MOD_ID,
                    "interact_compact_strut"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, InteractCompactStrutPayload>
            STREAM_CODEC = StreamCodec.ofMember(
                    InteractCompactStrutPayload::write,
                    InteractCompactStrutPayload::read
            );

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(supportPos);
        buffer.writeEnum(facing);
        buffer.writeEnum(hand);
    }

    private static InteractCompactStrutPayload read(
            RegistryFriendlyByteBuf buffer
    ) {
        return new InteractCompactStrutPayload(
                buffer.readBlockPos(),
                buffer.readEnum(Direction.class),
                buffer.readEnum(InteractionHand.class)
        );
    }

    public static void handle(
            InteractCompactStrutPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        Level level = player.level();
        SuspensionStrutBlockEntity strut =
                CompactStrutSupportShapes.getAttachedStrut(
                        level,
                        payload.supportPos(),
                        payload.facing()
                );
        if (strut == null
                || strut.isPending()
                || !strut.hasLink()
                || !strut.isCompactLink()) {
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 rayEnd = eye.add(player.getLookAngle().scale(
                player.blockInteractionRange() + 0.5D
        ));
        BlockHitResult verifiedHit =
                CompactStrutSupportShapes.clipSupportJoint(
                        level,
                        payload.supportPos(),
                        payload.facing(),
                        eye,
                        rayEnd
                );
        if (verifiedHit == null) {
            return;
        }

        ItemStack stack = player.getItemInHand(payload.hand());
        Item item = stack.getItem();
        UseOnContext useContext = new UseOnContext(
                player,
                payload.hand(),
                verifiedHit
        );
        InteractionResult result;
        if (item instanceof SpringTuningWrenchItem) {
            result = SpringTuningWrenchItem.interactWithStrut(
                    useContext,
                    strut
            );
        } else if (item instanceof AdjustmentWrenchItem) {
            result = AdjustmentWrenchItem.interactWithStrut(
                    useContext,
                    strut
            );
        } else if (item instanceof WrenchItem && player.isShiftKeyDown()) {
            result = SuspensionStrutBlock.pickupWithWrench(strut, player);
        } else {
            return;
        }

        if (result.consumesAction()) {
            player.swing(payload.hand(), true);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
