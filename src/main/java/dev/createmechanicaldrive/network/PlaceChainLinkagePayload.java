package dev.createmechanicaldrive.network;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.createmechanicaldrive.content.chain_linkage.ChainGearBlock;
import dev.createmechanicaldrive.content.chain_linkage.ChainGearBlockEntity;
import dev.createmechanicaldrive.content.chain_linkage.ChainLinkageValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record PlaceChainLinkagePayload(
        List<Endpoint> endpoints
) implements CustomPacketPayload {
    public record Endpoint(
            BlockPos position,
            @Nullable UUID subLevelId
    ) {
    }

    public static final CustomPacketPayload.Type<PlaceChainLinkagePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateMechanicalDrive.MOD_ID,
                            "place_chain_linkage"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            PlaceChainLinkagePayload
            > STREAM_CODEC =
            StreamCodec.ofMember(
                    PlaceChainLinkagePayload::write,
                    PlaceChainLinkagePayload::read
            );

    private void write(
        RegistryFriendlyByteBuf buffer
    ) {
        buffer.writeVarInt(
                endpoints.size()
        );

        for (Endpoint endpoint : endpoints) {
            buffer.writeBlockPos(
                    endpoint.position()
            );

            UUID subLevelId =
                    endpoint.subLevelId();

            boolean hasSubLevel =
                    subLevelId != null;

            buffer.writeBoolean(
                    hasSubLevel
            );

            if (hasSubLevel) {
                buffer.writeUUID(
                        subLevelId
                );
            }
        }
    }

    private static PlaceChainLinkagePayload read(
            RegistryFriendlyByteBuf buffer
    ) {
        int size =
                buffer.readVarInt();

        List<Endpoint> endpoints =
                new ArrayList<>(
                        Math.min(
                                size,
                                ChainLinkageValidator.MAX_GEARS
                        )
                );

        for (int i = 0; i < size; i++) {
            BlockPos position =
                    buffer.readBlockPos();

            boolean hasSubLevel =
                    buffer.readBoolean();

            UUID subLevelId =
                    hasSubLevel
                            ? buffer.readUUID()
                            : null;

            endpoints.add(
                    new Endpoint(
                            position,
                            subLevelId
                    )
            );
        }

        return new PlaceChainLinkagePayload(
                List.copyOf(endpoints)
        );
    }

    public static void handle(
            PlaceChainLinkagePayload payload,
            IPayloadContext context
    ) {
        Player player =
                context.player();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Level level =
                serverPlayer.level();

        List<BlockPos> positions =
                resolvePositions(
                        level,
                        payload.endpoints()
                );

        if (positions == null) {
            return;
        }

        if (!validateEndpointIdentities(
                level,
                payload.endpoints(),
                positions
        )) {
            return;
        }

        Direction.Axis axis =
                validateTargets(
                        level,
                        serverPlayer,
                        positions
                );

        if (axis == null) {
            CreateMechanicalDrive.LOGGER.warn(
                    "Rejected chain linkage: resolved endpoints failed validation. "
                            + "player={}, positions={}",
                    serverPlayer.getGameProfile().getName(),
                    positions
            );

            return;
        }

        int requiredChains =
                getRequiredChains(
                        level,
                        positions
                );

        if (!serverPlayer.hasInfiniteMaterials()
                && !consumeChains(
                serverPlayer,
                requiredChains,
                true
        )) {
            return;
        }

        if (!serverPlayer.hasInfiniteMaterials()) {
            consumeChains(
                    serverPlayer,
                    requiredChains,
                    false
            );
        }

        Set<ChainGearBlockEntity> touched =
                new HashSet<>();

        for (BlockPos pos : positions) {
            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            if (blockEntity instanceof ChainGearBlockEntity gear
                    && touched.add(gear)) {
                gear.destroyChain(
                        false
                );
            }
        }

        List<ChainGearBlockEntity> gears =
                new ArrayList<>(
                        positions.size()
                );

        double initialCenterDistance =
                getInitialCenterDistance(
                        level,
                        positions
                );

        double initialLoopLength =
                getInitialLoopLength(
                        level,
                        positions
                );

        for (int i = 0; i < positions.size(); i++) {
            BlockPos pos =
                    positions.get(i);

            BlockEntity blockEntity =
                    level.getBlockEntity(
                            pos
                    );

            if (!(blockEntity instanceof ChainGearBlockEntity gear)) {
                return;
            }

            UUID ownSubLevelId =
                    SableSubLevelHelper.getSubLevelId(
                            level,
                            pos
                    );

            UUID connectedSubLevelId =
                    null;

            if (positions.size() == 2) {
                BlockPos otherPos =
                        positions.get(
                                i == 0
                                        ? 1
                                        : 0
                        );

                UUID otherSubLevelId =
                        SableSubLevelHelper.getSubLevelId(
                                level,
                                otherPos
                        );

                if (!Objects.equals(
                        ownSubLevelId,
                        otherSubLevelId
                )) {
                    connectedSubLevelId =
                            otherSubLevelId;
                }
            }

            gear.setChainLoop(
                    positions,
                    i,
                    i == 0
                            ? requiredChains
                            : 0,
                    initialCenterDistance,
                    initialLoopLength,
                    connectedSubLevelId
            );

            gears.add(
                    gear
            );
        }

        for (ChainGearBlockEntity gear : gears) {
            gear.detachKinetics();
        }

        for (ChainGearBlockEntity gear : gears) {
            gear.attachKinetics();
        }
    }

    @Nullable
    private static List<BlockPos> resolvePositions(
            Level level,
            List<Endpoint> endpoints
    ) {
        if (endpoints.size() < ChainLinkageValidator.MIN_GEARS
                || endpoints.size() > ChainLinkageValidator.MAX_GEARS) {
            return null;
        }

        List<BlockPos> resolved =
                new ArrayList<>(
                        endpoints.size()
                );

        for (Endpoint endpoint : endpoints) {
            BlockPos position =
                    SableSubLevelHelper.resolveTransferPosition(
                            level,
                            endpoint.subLevelId(),
                            endpoint.position()
                    );

            if (position == null) {
                CreateMechanicalDrive.LOGGER.warn(
                        "Rejected chain linkage endpoint: unable to resolve position. "
                                + "subLevel={}, transferPos={}",
                        endpoint.subLevelId(),
                        endpoint.position()
                );

                return null;
            }

            resolved.add(
                    position
            );
        }

        return List.copyOf(
                resolved
        );
    }

    private static boolean validateEndpointIdentities(
            Level level,
            List<Endpoint> endpoints,
            List<BlockPos> positions
    ) {
        if (endpoints.size() != positions.size()) {
            return false;
        }

        for (int i = 0; i < endpoints.size(); i++) {
            Endpoint endpoint =
                    endpoints.get(i);

            BlockPos position =
                    positions.get(i);

            UUID actualSubLevelId =
                    SableSubLevelHelper.getSubLevelId(
                            level,
                            position
                    );

            if (!Objects.equals(
                    endpoint.subLevelId(),
                    actualSubLevelId
            )) {
                CreateMechanicalDrive.LOGGER.warn(
                        "Rejected chain linkage endpoint identity mismatch: "
                                + "expectedSubLevel={}, actualSubLevel={}, pos={}",
                        endpoint.subLevelId(),
                        actualSubLevelId,
                        position
                );

                return false;
            }
        }

        return true;
    }

    private static Direction.Axis validateTargets(
            Level level,
            ServerPlayer player,
            List<BlockPos> positions
    ) {
        if (positions.size() < ChainLinkageValidator.MIN_GEARS
                || positions.size() > ChainLinkageValidator.MAX_GEARS) {
            return null;
        }

        Direction.Axis axis =
                null;
        UUID subLevelId =
                null;
        boolean hasSubLevelId =
                false;
        boolean sameSubLevel =
                true;

        for (BlockPos pos : positions) {
            if (!level.isLoaded(pos)) {
                CreateMechanicalDrive.LOGGER.warn(
                        "Chain validation failed: position is not loaded: {}",
                        pos
                );

                return null;
            }

            BlockState state =
                    level.getBlockState(pos);

            if (!state.is(CreateMechanicalDrive.CHAIN_GEAR.get())) {
                CreateMechanicalDrive.LOGGER.warn(
                        "Chain validation failed: expected chain gear at {}, found {}",
                        pos,
                        state
                );

                return null;
            }

            BlockEntity blockEntity =
                    level.getBlockEntity(
                            pos
                    );

            if (!(blockEntity instanceof ChainGearBlockEntity gear)) {
                CreateMechanicalDrive.LOGGER.warn(
                        "Chain validation failed: missing ChainGearBlockEntity at {}. BE={}",
                        pos,
                        blockEntity
                );

                return null;
            }

            if (gear.hasChainLoop()) {
                CreateMechanicalDrive.LOGGER.warn(
                        "Chain validation failed: gear at {} already has a chain loop",
                        pos
                );

                return null;
            }

            Direction.Axis gearAxis =
                    state.getValue(
                            ChainGearBlock.AXIS
                    );

            UUID gearSubLevelId =
                    SableSubLevelHelper.getSubLevelId(
                            level,
                            pos
                    );

            if (player.position()
                    .distanceToSqr(
                            SableSubLevelHelper.getWorldCenter(
                                    level,
                                    pos
                            )
                    ) > 1024.0D) {
                return null;
            }

            if (axis == null) {
                axis =
                        gearAxis;
            } else if (axis != gearAxis) {
                return null;
            }

            if (!hasSubLevelId) {
                subLevelId =
                        gearSubLevelId;
                hasSubLevelId =
                        true;
            } else if (!Objects.equals(
                    subLevelId,
                    gearSubLevelId
            )) {
                sameSubLevel =
                        false;
            }
        }

        if (axis == null) {
            return null;
        }

        if (!sameSubLevel) {
            if (positions.size() != 2) {
                return null;
            }

            double initialCenterDistance =
                    ChainLinkageValidator.getFlexibleCenterDistance(
                            level,
                            positions,
                            axis
                    );

            double initialLoopLength =
                    ChainLinkageValidator.getTwoGearLoopLength(
                            initialCenterDistance
                    );

            if (!ChainLinkageValidator.isFlexibleTwoGearLinkValid(
                    level,
                    positions,
                    axis,
                    initialCenterDistance,
                    initialLoopLength
            )) {
                return null;
            }

            return axis;
        }

        if (!ChainLinkageValidator.isValidLoop(
                positions,
                axis
        )) {
            return null;
        }

        return axis;
    }

    private static int getRequiredChains(
            Level level,
            List<BlockPos> positions
    ) {
        if (positions.size() == 2
                && !sameSubLevel(
                level,
                positions
        )) {
            return ChainLinkageValidator.getChainsRequired(
                    getInitialLoopLength(
                            level,
                            positions
                    )
            );
        }

        return ChainLinkageValidator.getChainsRequired(
                positions
        );
    }

    private static double getInitialCenterDistance(
            Level level,
            List<BlockPos> positions
    ) {
        if (positions.size() != 2
                || sameSubLevel(
                level,
                positions
        )) {
            return 0.0D;
        }

        Direction.Axis axis =
                level.getBlockState(
                        positions.getFirst()
                ).getValue(
                        ChainGearBlock.AXIS
                );

        return ChainLinkageValidator.getFlexibleCenterDistance(
                level,
                positions,
                axis
        );
    }

    private static double getInitialLoopLength(
            Level level,
            List<BlockPos> positions
    ) {
        double centerDistance =
                getInitialCenterDistance(
                        level,
                        positions
                );

        if (centerDistance <= 0.0D) {
            return 0.0D;
        }

        return ChainLinkageValidator.getTwoGearLoopLength(
                centerDistance
        );
    }

    private static boolean sameSubLevel(
            Level level,
            List<BlockPos> positions
    ) {
        if (positions.size() != 2) {
            return true;
        }

        return Objects.equals(
                SableSubLevelHelper.getSubLevelId(
                        level,
                        positions.getFirst()
                ),
                SableSubLevelHelper.getSubLevelId(
                        level,
                        positions.getLast()
                )
        );
    }

    private static boolean consumeChains(
            Player player,
            int amount,
            boolean simulate
    ) {
        Inventory inventory =
                player.getInventory();

        int remaining =
                amount;

        for (ItemStack stack : inventory.items) {
            if (!stack.is(CreateMechanicalDrive.CHAIN_LINKAGE_ITEM.get())) {
                continue;
            }

            remaining -= stack.getCount();

            if (remaining <= 0) {
                break;
            }
        }

        if (remaining > 0) {
            for (ItemStack stack : inventory.offhand) {
                if (!stack.is(CreateMechanicalDrive.CHAIN_LINKAGE_ITEM.get())) {
                    continue;
                }

                remaining -= stack.getCount();

                if (remaining <= 0) {
                    break;
                }
            }
        }

        if (remaining > 0) {
            return false;
        }

        if (simulate) {
            return true;
        }

        remaining =
                amount;

        for (ItemStack stack : inventory.items) {
            if (!stack.is(CreateMechanicalDrive.CHAIN_LINKAGE_ITEM.get())) {
                continue;
            }

            int taken =
                    Math.min(
                            remaining,
                            stack.getCount()
                    );

            stack.shrink(
                    taken
            );

            remaining -= taken;

            if (remaining <= 0) {
                return true;
            }
        }

        for (ItemStack stack : inventory.offhand) {
            if (!stack.is(CreateMechanicalDrive.CHAIN_LINKAGE_ITEM.get())) {
                continue;
            }

            int taken =
                    Math.min(
                            remaining,
                            stack.getCount()
                    );

            stack.shrink(
                    taken
            );

            remaining -= taken;

            if (remaining <= 0) {
                return true;
            }
        }

        return true;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
