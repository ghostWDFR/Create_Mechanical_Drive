package dev.createmechanicaldrive.content.tracks.mounts.sprocket;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.createmechanicaldrive.content.tracks.chain.TrackAssembly;
import dev.createmechanicaldrive.content.tracks.chain.TrackAssemblyManager;
import dev.createmechanicaldrive.content.tracks.chain.TrackEndpointContactPhysics;
import dev.createmechanicaldrive.content.tracks.mounts.idler.IdlerMountBlockEntity;
import dev.createmechanicaldrive.content.tracks.mounts.torsion.TorsionMountBlockEntity;
import dev.createmechanicaldrive.content.tracks.mounts.torsion.TorsionMountAttachments;
import dev.createmechanicaldrive.content.tracks.wheels.sprocket.SprocketWheelItem;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class SprocketMountBlockEntity extends KineticBlockEntity
        implements Clearable, BlockEntitySubLevelActor {
    private static final String ATTACHMENT_TAG = "Attachment";
    private static final String TRACK_ASSEMBLY_TAG = "TrackAssembly";
    private static final String TRACK_SAG_TAG = "TrackSag";
    private static final String TRACK_SAG_VELOCITY_TAG = "TrackSagVelocity";
    private static final double MIN_BASE_SAG = 0.035D;
    private static final double MAX_BASE_SAG = 0.30D;
    private static final double MIN_MAX_SAG = 0.16D;
    private static final double MAX_MAX_SAG = 0.62D;
    private static final int SAG_SYNC_INTERVAL_TICKS = 4;
    private static final double SAG_SYNC_EPSILON = 1.0D / 128.0D;

    private final SprocketMountInventory inventory;
    private final TrackEndpointContactPhysics.State endpointContact;
    private double visualAngle;
    private double previousVisualAngle;
    private double visualAngularVelocity;
    private TrackAssembly trackAssembly;
    private volatile double trackSag;
    private double previousTrackSag;
    private double targetTrackSag;
    private double targetTrackSagVelocity;
    private double trackSagVelocity;
    private double previousLocalVerticalVelocity;
    private double filteredLocalVerticalAcceleration;
    private double lastSyncedTrackSag = Double.NaN;
    private double lastSyncedTrackSagVelocity = Double.NaN;
    private Vec3 trackMotionSamplePoint;
    private int sagSyncTicks;
    private boolean sagVelocityInitialized;
    private boolean clientSagInitialized;

    public SprocketMountBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
        inventory = new SprocketMountInventory(this);
        endpointContact = new TrackEndpointContactPhysics.State(this);
    }

    @Override
    public void sable$physicsTick(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            double timeStep
    ) {
        if (trackAssembly == null || timeStep <= 0.0D) {
            resetSagVelocity();
            return;
        }
        updateTrackSagPhysics(subLevel, timeStep);
        if (!SprocketWheelItem.isSprocketWheel(getAttachment())) {
            return;
        }
        Direction facing = getBlockState().getValue(
                SprocketMountBlock.HORIZONTAL_FACING
        );
        double radius = SprocketWheelItem.RADIUS
                + trackAssembly.type().groundContactOffset();
        TrackEndpointContactPhysics.submit(
                endpointContact,
                subLevel,
                timeStep,
                getBlockPos().getCenter(),
                facing,
                radius
        );
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (!level.isClientSide) {
            if (trackAssembly != null
                    && Sable.HELPER.getContaining(this) == null) {
                trackSag = Mth.lerp(
                        0.2D,
                        trackSag,
                        baseTrackSag()
                );
                trackSagVelocity = 0.0D;
            }
            syncTrackSagWhenNeeded();
            return;
        }

        previousVisualAngle = visualAngle;
        previousTrackSag = trackSag;
        SubLevel containing = trackAssembly == null
                ? null
                : Sable.HELPER.getContaining(this);
        if (containing != null) {
            updateTrackSagFromMotion(containing, 1.0D / 20.0D);
            trackSag = Mth.lerp(0.04D, trackSag, targetTrackSag);
            trackSagVelocity = Mth.lerp(
                    0.04D,
                    trackSagVelocity,
                    targetTrackSagVelocity
            );
        } else {
            trackSag = Mth.lerp(0.3D, trackSag, targetTrackSag);
            trackSagVelocity = Mth.lerp(
                    0.3D,
                    trackSagVelocity,
                    targetTrackSagVelocity
            );
        }
        if (!SprocketMountAttachmentShapes.supports(getAttachment())) {
            visualAngle = 0.0D;
            previousVisualAngle = 0.0D;
            visualAngularVelocity = 0.0D;
            return;
        }

        Direction output = getBlockState().getValue(
                SprocketMountBlock.HORIZONTAL_FACING
        );
        double targetVelocity = -output.getAxisDirection().getStep()
                * getSpeed()
                * Math.PI
                / 600.0D;
        if (trackAssembly != null && Math.abs(getSpeed()) < 0.001F) {
            targetVelocity = passiveTrackAngularVelocity();
        }
        visualAngularVelocity = Mth.lerp(
                0.2D,
                visualAngularVelocity,
                targetVelocity
        );
        visualAngle += visualAngularVelocity;
    }

    private double passiveTrackAngularVelocity() {
        double trackVelocity = 0.0D;
        int groundedWheels = 0;
        for (BlockPos nodePos : trackAssembly.nodes()) {
            BlockEntity blockEntity = level.getBlockEntity(nodePos);
            if (!(blockEntity instanceof TorsionMountBlockEntity torsion)
                    || !TorsionMountAttachments.hasSuspensionWheel(
                    torsion.getAttachment()
            )
                    || !torsion.hasClientGroundContact()) {
                continue;
            }
            trackVelocity += torsion.getClientRollingAngularVelocity()
                    * TorsionMountAttachments.wheelRadius(
                    torsion.getAttachment()
            );
            groundedWheels++;
        }
        if (groundedWheels == 0) {
            return 0.0D;
        }

        // Torsion wheels rotate around the outward axis while the sprocket
        // model rotates around local +Z, which points inward.
        return -trackVelocity
                / groundedWheels
                / SprocketWheelItem.RADIUS;
    }

    public double getLerpedVisualAngle(float partialTick) {
        return Mth.lerp(
                partialTick,
                previousVisualAngle,
                visualAngle
        );
    }

    public double getLerpedTrackSag(float partialTick) {
        return Mth.lerp(partialTick, previousTrackSag, trackSag);
    }

    public SprocketMountInventory getInventory() {
        return inventory;
    }

    public ItemStack getAttachment() {
        return inventory.getStackInSlot(0);
    }

    public void onAttachmentChanged() {
        if (trackAssembly != null
                && !SprocketWheelItem.isSprocketWheel(getAttachment())) {
            TrackAssemblyManager.disassemble(this, true);
        }
        setChanged();
        invalidateRenderBoundingBox();
        sendData();
    }

    public TrackAssembly getTrackAssembly() {
        return trackAssembly;
    }

    public void setTrackAssembly(TrackAssembly trackAssembly) {
        if (this.trackAssembly == trackAssembly
                || this.trackAssembly != null
                && this.trackAssembly.equals(trackAssembly)) {
            return;
        }
        this.trackAssembly = trackAssembly;
        trackMotionSamplePoint = null;
        resetTrackSag(trackAssembly == null ? 0.0D : baseTrackSag());
        setChanged();
        invalidateRenderBoundingBox();
        if (level != null && !level.isClientSide) {
            sendData();
        }
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
        if (trackAssembly != null) {
            tag.put(TRACK_ASSEMBLY_TAG, trackAssembly.write());
        }
        tag.putDouble(TRACK_SAG_TAG, trackSag);
        tag.putDouble(TRACK_SAG_VELOCITY_TAG, trackSagVelocity);
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
        TrackAssembly loadedAssembly = tag.contains(TRACK_ASSEMBLY_TAG)
                ? TrackAssembly.read(tag.getCompound(TRACK_ASSEMBLY_TAG))
                : null;
        boolean assemblyChanged = !java.util.Objects.equals(
                trackAssembly,
                loadedAssembly
        );
        trackAssembly = loadedAssembly;
        if (assemblyChanged) {
            trackMotionSamplePoint = null;
            resetSagVelocity();
        }
        double loadedSag = trackAssembly != null && tag.contains(TRACK_SAG_TAG)
                ? Mth.clamp(tag.getDouble(TRACK_SAG_TAG), 0.0D, MAX_MAX_SAG)
                : 0.0D;
        double loadedSagVelocity = trackAssembly != null
                && tag.contains(TRACK_SAG_VELOCITY_TAG)
                ? Mth.clamp(
                        tag.getDouble(TRACK_SAG_VELOCITY_TAG),
                        -2.0D,
                        2.0D
                )
                : 0.0D;

        if (clientPacket) {
            targetTrackSag = loadedSag;
            targetTrackSagVelocity = loadedSagVelocity;
            if (!clientSagInitialized) {
                trackSag = loadedSag;
                previousTrackSag = loadedSag;
                trackSagVelocity = loadedSagVelocity;
                clientSagInitialized = true;
            }
        } else {
            trackSag = loadedSag;
            targetTrackSag = loadedSag;
            previousTrackSag = loadedSag;
            trackSagVelocity = loadedSagVelocity;
            targetTrackSagVelocity = loadedSagVelocity;
        }

        if (clientPacket) {
            invalidateRenderBoundingBox();
        }
    }

    @Override
    public void clearContent() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        AABB bounds = new AABB(getBlockPos()).inflate(1.0D);
        if (trackAssembly != null) {
            for (BlockPos node : trackAssembly.nodes()) {
                bounds = bounds.minmax(new AABB(node).inflate(2.0D));
            }
        }
        return bounds;
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }

    private void updateTrackSagPhysics(
            ServerSubLevel subLevel,
            double timeStep
    ) {
        double dt = Mth.clamp(timeStep, 1.0D / 240.0D, 0.05D);
        updateTrackSagFromMotion(subLevel, dt);
    }

    private void updateTrackSagFromMotion(
            SubLevel subLevel,
            double dt
    ) {
        Vector3d worldVelocity = Sable.HELPER.getVelocity(
                level,
                JOMLConversion.toJOML(trackMotionSamplePoint())
        );
        double localVerticalVelocity = subLevel.logicalPose()
                .transformNormalInverse(worldVelocity)
                .y;

        integrateTrackSag(localVerticalVelocity, dt);
    }

    private void integrateTrackSag(
            double localVerticalVelocity,
            double dt
    ) {

        if (!sagVelocityInitialized) {
            previousLocalVerticalVelocity = localVerticalVelocity;
            sagVelocityInitialized = true;
        }
        double acceleration = Mth.clamp(
                (localVerticalVelocity - previousLocalVerticalVelocity) / dt,
                -40.0D,
                40.0D
        );
        previousLocalVerticalVelocity = localVerticalVelocity;
        double filter = 1.0D - Math.exp(-30.0D * dt);
        filteredLocalVerticalAcceleration = Mth.lerp(
                filter,
                filteredLocalVerticalAcceleration,
                acceleration
        );

        double tension = trackTension();
        double baseSag = baseTrackSag(tension);
        double angularFrequency = Mth.lerp(tension, 7.0D, 11.0D);
        double dampingRatio = Mth.lerp(tension, 0.18D, 0.55D);
        double accelerationResponse = Mth.lerp(
                tension,
                0.55D,
                0.14D
        );
        double sagAcceleration = (baseSag - trackSag)
                * angularFrequency
                * angularFrequency
                - trackSagVelocity
                * 2.0D
                * dampingRatio
                * angularFrequency
                + filteredLocalVerticalAcceleration
                * accelerationResponse;

        trackSagVelocity += sagAcceleration * dt;
        trackSag += trackSagVelocity * dt;
        double maxSag = Mth.lerp(tension, MAX_MAX_SAG, MIN_MAX_SAG);
        if (trackSag < 0.0D) {
            trackSag = 0.0D;
            trackSagVelocity = Math.max(0.0D, trackSagVelocity) * 0.25D;
        } else if (trackSag > maxSag) {
            trackSag = maxSag;
            trackSagVelocity = Math.min(0.0D, trackSagVelocity) * 0.25D;
        }
    }

    private double baseTrackSag() {
        return baseTrackSag(trackTension());
    }

    private static double baseTrackSag(double tension) {
        return Mth.lerp(tension, MAX_BASE_SAG, MIN_BASE_SAG);
    }

    private double trackTension() {
        if (level == null || trackAssembly == null) {
            return 1.0D;
        }
        Direction facing = getBlockState().getValue(
                SprocketMountBlock.HORIZONTAL_FACING
        );
        Direction rolling = facing.getClockWise();
        double tension = 0.0D;
        int idlers = 0;
        for (BlockPos nodePos : trackAssembly.nodes()) {
            BlockEntity blockEntity = level.getBlockEntity(nodePos);
            if (!(blockEntity instanceof IdlerMountBlockEntity idler)) {
                continue;
            }
            double relativeU = (nodePos.getX() - getBlockPos().getX())
                    * rolling.getStepX()
                    + (nodePos.getZ() - getBlockPos().getZ())
                    * rolling.getStepZ();
            if (Math.abs(relativeU) < 1.0E-6D) {
                continue;
            }
            tension += idler.getNormalizedTrackTension(relativeU);
            idlers++;
        }
        return idlers == 0
                ? 1.0D
                : Mth.clamp(tension / idlers, 0.0D, 1.0D);
    }

    private Vec3 trackMotionSamplePoint() {
        if (trackMotionSamplePoint != null) {
            return trackMotionSamplePoint;
        }
        if (trackAssembly == null || trackAssembly.nodes().isEmpty()) {
            trackMotionSamplePoint = getBlockPos().getCenter();
            return trackMotionSamplePoint;
        }

        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (BlockPos node : trackAssembly.nodes()) {
            x += node.getX() + 0.5D;
            y += node.getY() + 0.5D;
            z += node.getZ() + 0.5D;
        }
        double inverseCount = 1.0D / trackAssembly.nodes().size();
        trackMotionSamplePoint = new Vec3(
                x * inverseCount,
                y * inverseCount,
                z * inverseCount
        );
        return trackMotionSamplePoint;
    }

    private void syncTrackSagWhenNeeded() {
        if (trackAssembly == null) {
            return;
        }
        sagSyncTicks++;
        if (sagSyncTicks < SAG_SYNC_INTERVAL_TICKS) {
            return;
        }
        sagSyncTicks = 0;
        if (Double.isFinite(lastSyncedTrackSag)
                && Math.abs(trackSag - lastSyncedTrackSag)
                < SAG_SYNC_EPSILON
                && Double.isFinite(lastSyncedTrackSagVelocity)
                && Math.abs(
                        trackSagVelocity - lastSyncedTrackSagVelocity
                ) < SAG_SYNC_EPSILON * 4.0D) {
            return;
        }
        lastSyncedTrackSag = trackSag;
        lastSyncedTrackSagVelocity = trackSagVelocity;
        setChanged();
        sendData();
    }

    private void resetTrackSag(double sag) {
        trackSag = sag;
        previousTrackSag = sag;
        targetTrackSag = sag;
        targetTrackSagVelocity = 0.0D;
        trackSagVelocity = 0.0D;
        lastSyncedTrackSag = Double.NaN;
        lastSyncedTrackSagVelocity = Double.NaN;
        sagSyncTicks = 0;
        resetSagVelocity();
    }

    private void resetSagVelocity() {
        sagVelocityInitialized = false;
        previousLocalVerticalVelocity = 0.0D;
        filteredLocalVerticalAcceleration = 0.0D;
        trackSagVelocity = 0.0D;
    }
}
