package dev.createmechanicaldrive.content.seats;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

public class SeatEntity
        extends Entity
        implements IEntityWithComplexSpawn {

    public SeatEntity(
            EntityType<?> type,
            Level level
    ) {
        super(
                type,
                level
        );

        noPhysics =
                true;
    }

    public SeatEntity(
            Level level
    ) {
        this(
                CreateMechanicalDrive
                        .SEAT_ENTITY
                        .get(),
                level
        );

    }

    public static boolean isSeatOccupied(
            Level level,
            BlockPos pos
    ) {
        return !level
                .getEntitiesOfClass(
                        SeatEntity.class,
                        new AABB(
                                pos
                        )
                )
                .isEmpty();
    }

    public static void sitDown(
            Level level,
            BlockPos pos,
            Entity passenger
    ) {
        if (level.isClientSide) {
            return;
        }

        SeatEntity seat =
                new SeatEntity(
                        level
                );

        seat.setPos(
                pos.getX()
                        + 0.5D,
                pos.getY(),
                pos.getZ()
                        + 0.5D
        );

        level.addFreshEntity(
                seat
        );

        passenger.startRiding(
                seat,
                true
        );
    }

    @Override
    public void setPos(
            double x,
            double y,
            double z
    ) {
        super.setPos(
                x,
                y,
                z
        );

        AABB box =
                getBoundingBox();

        setBoundingBox(
                box.move(
                        new Vec3(
                                x,
                                y,
                                z
                        )
                                .subtract(
                                        box.getCenter()
                                )
                )
        );
    }

    @Override
    protected void positionRider(
            Entity passenger,
            MoveFunction moveFunction
    ) {
        if (!hasPassenger(
                passenger
        )) {
            return;
        }

        double y =
                getPassengerRidingPosition(
                        passenger
                ).y
                        - passenger
                        .getVehicleAttachmentPoint(
                                this
                        ).y;

        moveFunction.accept(
                passenger,
                getX(),
                -0.1875D
                        + y,
                getZ()
        );
    }

    @Override
    public void onPassengerTurned(
            Entity passenger
    ) {
        passenger.setYHeadRot(
                passenger.getYRot()
        );
    }

    @Override
    public void setDeltaMovement(
            Vec3 deltaMovement
    ) {
    }

    @Override
    public void tick() {
        if (level().isClientSide) {
            return;
        }

        boolean seatStillExists =
                level()
                        .getBlockState(
                                blockPosition()
                        )
                        .getBlock()
                        instanceof SeatBlock
                        || level()
                        .getBlockState(
                                blockPosition()
                        )
                        .getBlock()
                        instanceof FlatSeatBlock;

        if (
                isVehicle()
                        && seatStillExists
        ) {
            return;
        }

        discard();
    }

    @Override
    protected boolean canRide(
            Entity vehicle
    ) {
        return !(vehicle instanceof FakePlayer);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(
            LivingEntity passenger
    ) {
        return super
                .getDismountLocationForPassenger(
                        passenger
                )
                .add(
                        0.0D,
                        0.5D,
                        0.0D
                );
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
    }

    @Override
    public void writeSpawnData(
            RegistryFriendlyByteBuf buffer
    ) {
    }

    @Override
    public void readSpawnData(
            RegistryFriendlyByteBuf buffer
    ) {
    }
}
