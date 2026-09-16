package dev.createmechanicaldrive.content.tracks.mounts.idler;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.createmechanicaldrive.content.tracks.chain.TrackAssemblyManager;
import dev.createmechanicaldrive.content.tracks.chain.TrackEndpointContactPhysics;
import dev.createmechanicaldrive.content.tracks.chain.TrackLinkedWheel;
import dev.createmechanicaldrive.content.tracks.wheels.idler.IdlerWheelItem;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class IdlerMountBlockEntity extends SmartBlockEntity
        implements Clearable, TrackLinkedWheel, BlockEntitySubLevelActor {
    private static final String ATTACHMENT_TAG = "Attachment";
    private static final String AXLE_OFFSET_TAG = "AxleOffset";
    private static final String TRACK_SPROCKET_TAG = "TrackSprocket";
    private static final double AXLE_OFFSET_STEP = 1.0D / 16.0D;
    private static final double MAX_AXLE_OFFSET = 4.0D / 16.0D;

    private final IdlerMountInventory inventory;
    private final TrackEndpointContactPhysics.State endpointContact;
    private double axleOffset;
    private BlockPos trackSprocket;

    private VoxelShape cachedOutlineShape;
    private VoxelShape cachedCollisionShape;
    private boolean shapeCacheDirty = true;

    public IdlerMountBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
        inventory = new IdlerMountInventory(this);
        endpointContact = new TrackEndpointContactPhysics.State(this);
    }

    @Override
    public void sable$physicsTick(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            double timeStep
    ) {
        if (level == null || !IdlerWheelItem.isIdlerWheel(getAttachment())) {
            return;
        }
        var owner = TrackAssemblyManager.owner(level, this);
        if (owner == null || owner.getTrackAssembly() == null) {
            return;
        }
        Direction facing = getBlockState().getValue(
                IdlerMountBlock.HORIZONTAL_FACING
        );
        double radius = IdlerWheelItem.RADIUS
                + owner.getTrackAssembly().type().groundContactOffset();
        TrackEndpointContactPhysics.submit(
                endpointContact,
                subLevel,
                timeStep,
                getTrackWheelCenter(),
                facing,
                radius
        );
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public IdlerMountInventory getInventory() {
        return inventory;
    }

    public ItemStack getAttachment() {
        return inventory.getStackInSlot(0);
    }

    public double getAxleOffset() {
        return axleOffset;
    }

    public double getNormalizedTrackTension(double outwardDirection) {
        if (Math.abs(outwardDirection) < 1.0E-6D) {
            return 1.0D;
        }
        double outwardOffset = Math.copySign(1.0D, outwardDirection)
                * axleOffset;
        return Mth.clamp(
                0.5D + outwardOffset / (2.0D * MAX_AXLE_OFFSET),
                0.0D,
                1.0D
        );
    }

    public Vec3 getTrackWheelCenter() {
        Direction output = getBlockState().getValue(
                IdlerMountBlock.HORIZONTAL_FACING
        );
        Direction lateral = output.getClockWise();
        return getBlockPos().getCenter().add(
                lateral.getStepX() * axleOffset,
                0.0D,
                lateral.getStepZ() * axleOffset
        );
    }

    @Override
    public BlockPos mechanicalDrive$getTrackSprocket() {
        return trackSprocket;
    }

    @Override
    public void mechanicalDrive$setTrackSprocket(BlockPos sprocketPos) {
        if (java.util.Objects.equals(trackSprocket, sprocketPos)) {
            return;
        }
        trackSprocket = sprocketPos == null ? null : sprocketPos.immutable();
        setChanged();
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    public double adjustAxleOffset(int steps) {
        double adjusted = Math.round(
                (axleOffset + steps * AXLE_OFFSET_STEP)
                        / AXLE_OFFSET_STEP
        ) * AXLE_OFFSET_STEP;
        setAxleOffset(adjusted);
        return axleOffset;
    }

    public void setAxleOffset(double axleOffset) {
        double clamped = Mth.clamp(
                axleOffset,
                -MAX_AXLE_OFFSET,
                MAX_AXLE_OFFSET
        );
        if (Double.compare(this.axleOffset, clamped) == 0) {
            return;
        }

        this.axleOffset = clamped;

        invalidateShapeCache();

        setChanged();
        invalidateRenderBoundingBox();
        sendData();
    }

    public void onAttachmentChanged() {
        if (trackSprocket != null
                && !dev.createmechanicaldrive.content.tracks.wheels.idler.IdlerWheelItem
                .isIdlerWheel(getAttachment())) {
            TrackAssemblyManager.disassembleForWheel(this);
        }
        invalidateShapeCache();

        setChanged();
        invalidateRenderBoundingBox();
        sendData();
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.write(tag, registries, clientPacket);
        tag.put(
                ATTACHMENT_TAG,
                getAttachment().saveOptional(registries)
        );
        tag.putDouble(AXLE_OFFSET_TAG, axleOffset);
        if (trackSprocket != null) {
            tag.putLong(TRACK_SPROCKET_TAG, trackSprocket.asLong());
        }
    }

    @Override
    protected void read(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.read(tag, registries, clientPacket);
        inventory.setWithoutNotification(ItemStack.parseOptional(
                registries,
                tag.getCompound(ATTACHMENT_TAG)
        ));
        axleOffset = tag.contains(AXLE_OFFSET_TAG)
                ? Mth.clamp(
                        tag.getDouble(AXLE_OFFSET_TAG),
                        -MAX_AXLE_OFFSET,
                        MAX_AXLE_OFFSET
                )
                : 0.0D;
        trackSprocket = tag.contains(TRACK_SPROCKET_TAG)
                ? BlockPos.of(tag.getLong(TRACK_SPROCKET_TAG))
                : null;

        invalidateShapeCache();

        if (clientPacket) {
            invalidateRenderBoundingBox();
        }
    }

    public VoxelShape getCachedOutlineShape() {
        ensureShapeCache();
        return cachedOutlineShape;
    }

    public VoxelShape getCachedCollisionShape() {
        ensureShapeCache();
        return cachedCollisionShape;
    }

    private void ensureShapeCache() {
        if (!shapeCacheDirty) {
            return;
        }

        BlockState state = getBlockState();
        Direction outputDirection = state.getValue(
                IdlerMountBlock.HORIZONTAL_FACING
        );

        ItemStack attachment = getAttachment();

        cachedOutlineShape = IdlerMountBlock.createCombinedShape(
                outputDirection,
                attachment,
                axleOffset,
                false
        );

        cachedCollisionShape = IdlerMountBlock.createCombinedShape(
                outputDirection,
                attachment,
                axleOffset,
                true
        );

        shapeCacheDirty = false;
    }

    private void invalidateShapeCache() {
        shapeCacheDirty = true;
        cachedOutlineShape = null;
        cachedCollisionShape = null;
    }

    @Override
    public void clearContent() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(
                1.0D + Math.abs(axleOffset)
        );
    }
}
