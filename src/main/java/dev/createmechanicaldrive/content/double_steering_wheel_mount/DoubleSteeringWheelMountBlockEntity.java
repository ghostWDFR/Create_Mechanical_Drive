package dev.createmechanicaldrive.content.double_steering_wheel_mount;

import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StackRequirement;
import dev.createmechanicaldrive.content.steering_wheel_mount.SteeringWheelMountBlockEntity;
import dev.createmechanicaldrive.content.steering_wheel_mount.SteeringWheelMountInventory;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerMountHelper;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffsets;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.world.level.LevelAccessor;

public class DoubleSteeringWheelMountBlockEntity
        extends SteeringWheelMountBlockEntity {

    private static final double REST_TRAVEL = 0.65D;
    private static final double EMPTY_VISUAL_EXTENSION = 0.5D;

    private static final String SECOND_WHEEL_TAG =
            "SecondWheel";

    private double secondExtension =
            EMPTY_VISUAL_EXTENSION;

    private double previousSecondExtension =
            secondExtension;

    private double secondWheelAngle;
    private double previousSecondWheelAngle;
    private double secondAngularVelocity;
    private double secondContactFriction =
            1.0D;

    private boolean secondWheelOffGround;

    public DoubleSteeringWheelMountBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(
                type,
                pos,
                state
        );
    }

    @Override
    protected SteeringWheelMountInventory createInventory() {
        return new DoubleSteeringWheelMountInventory(
                this
        );
    }

    @Override
    public DoubleSteeringWheelMountInventory getInventory() {
        return (DoubleSteeringWheelMountInventory)
                super.getInventory();
    }

    public ItemStack getWheel(
            int slot
    ) {
        if (slot < 0
                || slot >= 2) {
            return ItemStack.EMPTY;
        }

        return getInventory()
                .getStackInSlot(
                        slot
                );
    }

    public ItemStack getWheel(
            Direction side
    ) {
        int slot =
                getSlotForSide(
                        side
                );

        return slot < 0
                ? ItemStack.EMPTY
                : getWheel(
                slot
        );
    }

    public int getSlotForSide(
            Direction side
    ) {
        Direction facing =
                getBlockState()
                        .getValue(
                                DoubleSteeringWheelMountBlock.HORIZONTAL_FACING
                        );

        if (side == facing) {
            return 0;
        }

        if (side == facing.getOpposite()) {
            return 1;
        }

        return -1;
    }

    @Nullable
    private TireLike getSecondWheelData() {
        return getWheel(
                1
        ).get(
                OffroadDataComponents.TIRE
        );
    }

    @Override
    protected List<Direction> getSteeringInputAngleSides() {
        return List.of(
                DoubleSteeringWheelMountBlock
                        .getSteeringShaftDirection(
                                getBlockState()
                        )
        );
    }

    @Override
    public Direction getSteeringShaftDirection() {
        return DoubleSteeringWheelMountBlock
                .getSteeringShaftDirection(
                        getBlockState()
                );
    }

    @Override
    public boolean isSteeringShaftDirection(
            Direction direction
    ) {
        return direction ==
                getSteeringShaftDirection();
    }

    @Override
    protected boolean isSteeringInputAngleSideActive(
            Direction direction
    ) {
        return direction ==
                DoubleSteeringWheelMountBlock
                        .getSteeringShaftDirection(
                                getBlockState()
                        );
    }

    @Override
    protected Direction getSteeringInputSide() {
        Direction steeringSide =
                getSteeringShaftDirection();

        return Math.abs(
                getSteeringSpeedFromSide(
                        steeringSide
                )
        ) > 0.001F
                ? steeringSide
                : null;
    }

    @Override
    public boolean isSteeringPassThroughEnabled() {
        return false;
    }

    @Override
    public void sable$physicsTick(
            ServerSubLevel subLevel,
            dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle handle,
            double timeStep
    ) {
        super.sable$physicsTick(
                subLevel,
                handle,
                timeStep
        );

        TireLike wheel =
                getSecondWheelData();

        if (wheel == null) {
            return;
        }

        Direction facing =
                getBlockState()
                        .getValue(
                                DoubleSteeringWheelMountBlock.HORIZONTAL_FACING
                        )
                        .getOpposite();

        BlockPos mountPos =
                getBlockPos();

        Vec3 forcePoint =
                mechanicalDrive$getWheelCenter(facing);

        Vector3d forcePosition =
                new Vector3d(
                        forcePoint.x,
                        forcePoint.y,
                        forcePoint.z
                );

        MassData massData =
                subLevel.getMassTracker();

        double effectiveMass =
                1.0D
                        / massData.getInverseNormalMass(
                        forcePosition,
                        OrientedBoundingBox3d.UP
                );

        double setting =
                getSuspensionStrengthValue();

        double massFactor =
                Math.min(
                        effectiveMass / setting,
                        1.0D
                ) * 10.0D;

        double tractionScale =
                setting
                        * massFactor
                        * 2.0D;

        double springScale =
                setting
                        * massFactor
                        * 40.0D
                        * mechanicalDrive$getSuspensionSpringMultiplier();

        double dampingScale =
                setting
                        * massFactor
                        * mechanicalDrive$getSuspensionTuning(DAMPING);

        Pose3d vehiclePose =
                subLevel.logicalPose();

        Vector3dc lateralAxis =
                rotatedWheelAxis(
                        axisUnit(
                                facing.getAxis()
                        )
                );

        Vector3dc rollingAxis =
                rotatedWheelAxis(
                        perpendicularHorizontal(
                                axisUnit(
                                        facing.getAxis()
                                )
                        )
                );

        TerrainContact contact =
                scanTerrain(
                        rollingAxis,
                        vehiclePose,
                        facing,
                        wheel.radius()
                );

        double groundDistance =
                contact.distance();

        secondExtension =
                groundDistance;

        if (groundDistance
                > REST_TRAVEL
                + wheel.radius()
                + 0.25D
                * mechanicalDrive$getSuspensionTuning(BUMP_CLEARANCE)) {

            secondExtension =
                    REST_TRAVEL;

            return;
        }

        double measuredLength =
                0.10833333333333334D
                        + secondExtension;

        double compressedLength =
                Mth.clamp(
                        measuredLength
                                - wheel.radius(),
                        0.0D,
                        REST_TRAVEL
                );

        Vector3d worldVelocity =
                Sable.HELPER.getVelocity(
                        level,
                        JOMLConversion.toJOML(
                                forcePoint
                        )
                );

        Vector3d localVelocity =
                vehiclePose
                        .transformNormalInverse(
                                worldVelocity
                        );

        double damping =
                -localVelocity.y
                        * dampingScale;

        double springImpulse =
                (
                        (
                                REST_TRAVEL
                                        - compressedLength
                        )
                                * springScale
                                * mechanicalDrive$getSuspensionTuning(BUMP_FORCE)
                                + damping
                )
                        * timeStep
                        * mechanicalDrive$getSuspensionTuning(MAX_IMPULSE);

        double compressionRatio =
                Mth.clamp(
                        (REST_TRAVEL - compressedLength)
                                / REST_TRAVEL,
                        0.0D,
                        1.0D
                );

        double targetBumpSpeed =
                Mth.lerp(
                        compressionRatio,
                        MIN_BUMP_NORMAL_SPEED,
                        MAX_BUMP_NORMAL_SPEED
                );

        double availableBumpSpeed =
                Math.max(
                        targetBumpSpeed
                                - localVelocity.y,
                        0.0D
                );

        springImpulse =
                Math.min(
                        springImpulse,
                        effectiveMass
                                * availableBumpSpeed
                );

        Vec3i hitNormal =
                contact.normal()
                        .getNormal();

        Vec3 suspensionImpulse =
                new Vec3(
                        springImpulse
                                * hitNormal.getX(),
                        springImpulse
                                * hitNormal.getY(),
                        springImpulse
                                * hitNormal.getZ()
                );

        if (contact.hitSubLevel()
                != null) {

            suspensionImpulse =
                    contact.hitSubLevel()
                            .logicalPose()
                            .transformNormal(
                                    suspensionImpulse
                            );
        }

        suspensionImpulse =
                vehiclePose
                        .transformNormalInverse(
                                suspensionImpulse
                        );

        Vector3d impulse =
                new Vector3d(
                        suspensionImpulse.x,
                        suspensionImpulse.y,
                        suspensionImpulse.z
                );

        secondContactFriction =
                contact.hitBlock() == null
                        ? 1.0D
                        : adjustedFriction(
                        PhysicsBlockPropertyHelper
                                .getFriction(
                                        level.getBlockState(
                                                contact.hitBlock()
                                        )
                                )
                );

        double brake =
                level.getSignal(
                        mountPos.above(),
                        Direction.UP
                )
                        / 15.0D;

        double usableSurfaceFriction =
                Math.min(
                        secondContactFriction,
                        1.0D
                );

        double rollingResistance =
                (
                        0.075D
                                + brake
                                * 0.3D
                )
                        * usableSurfaceFriction;

        float kineticSpeed =
                facing.getAxis()
                        == Axis.X
                        ? getSpeed()
                        : -getSpeed();

        impulse.fma(
                localVelocity.dot(
                        rollingAxis
                )
                        * -rollingResistance
                        * tractionScale
                        * timeStep
                        + kineticSpeed
                        * (
                        1.0D
                                - brake
                )
                        * usableSurfaceFriction
                        * 1.75D
                        * mechanicalDrive$getSuspensionTuning(DRIVE)
                        * timeStep,
                rollingAxis
        );

        impulse.fma(
                localVelocity.dot(
                        lateralAxis
                )
                        * -0.6D
                        * secondContactFriction
                        * tractionScale
                        * mechanicalDrive$getSuspensionTuning(GRIP)
                        * timeStep,
                lateralAxis
        );

        queueWheelImpulse(
                subLevel,
                forcePoint,
                impulse
        );
    }

    @Override
    protected ValueBoxTransform createSuspensionStrengthValueBox() {
        return new DoubleSuspensionStrengthValueBox();
    }

    private static final class DoubleSuspensionStrengthValueBox
            extends ValueBoxTransform {

        @Override
        public void rotate(
                LevelAccessor level,
                BlockPos pos,
                BlockState state,
                PoseStack poseStack
        ) {
            Direction facing =
                    state.getValue(
                            DoubleSteeringWheelMountBlock.HORIZONTAL_FACING
                    );

            float yaw =
                    AngleHelper.horizontalAngle(
                            facing
                    ) + 180.0F;

            ((PoseTransformStack)
                    TransformStack.of(
                                    poseStack
                            )
                            .rotateYDegrees(
                                    yaw
                            ))
                    .rotateXDegrees(
                            90.0F
                    );
        }

        @Override
        public boolean testHit(
                LevelAccessor level,
                BlockPos pos,
                BlockState state,
                Vec3 localHit
        ) {
            Vec3 target =
                    getLocalOffset(
                            level,
                            pos,
                            state
                    );

            return target != null
                    && localHit.distanceTo(
                    target
            ) < scale / 3.0F;
        }

        @Override
        public Vec3 getLocalOffset(
                LevelAccessor level,
                BlockPos pos,
                BlockState state
        ) {
            return VecHelper.voxelSpace(
                    8.0D,
                    15.5D,
                    8.0D
            );
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null
                || !level.isClientSide) {
            return;
        }

        TireLike wheel =
                getSecondWheelData();

        previousSecondExtension =
                secondExtension;

        if (wheel == null) {
            if (ShaftMarkerItem.isMarker(getWheel(1))) {
                Direction facing = getBlockState()
                        .getValue(
                                DoubleSteeringWheelMountBlock.HORIZONTAL_FACING
                        )
                        .getOpposite();
                previousSecondWheelAngle = secondWheelAngle;
                secondAngularVelocity = ShaftMarkerMountHelper
                        .approachDrivenAngularVelocity(
                                this,
                                facing,
                                secondAngularVelocity
                        );
                secondWheelAngle += secondAngularVelocity;
                secondWheelOffGround = true;
                secondExtension = Mth.lerp(
                        0.6D,
                        secondExtension,
                        EMPTY_VISUAL_EXTENSION
                );
                return;
            }

            previousSecondWheelAngle =
                    0.0D;

            secondWheelAngle =
                    0.0D;

            secondAngularVelocity =
                    0.0D;

            secondExtension =
                    Mth.lerp(
                            0.6D,
                            secondExtension,
                            EMPTY_VISUAL_EXTENSION
                    );

            return;
        }

        Direction facing =
                getBlockState()
                        .getValue(
                                DoubleSteeringWheelMountBlock.HORIZONTAL_FACING
                        )
                        .getOpposite();

        secondExtension =
                Mth.lerp(
                        0.7D,
                        secondExtension,
                        clientGroundExtension(
                                wheel.radius(),
                                facing
                        )
                );

        float speed =
                facing.getAxis()
                        == Axis.X
                        ? -getSpeed()
                        : getSpeed();

        double poweredAngularVelocity =
                speed
                        * Math.PI
                        * 2.0D
                        / 60.0D
                        / 20.0D
                        * (
                        15
                                - level.getSignal(
                                getBlockPos().above(),
                                Direction.UP
                        )
                )
                        / 15.0D;

        double attemptedAngularVelocity =
                Mth.lerp(
                        0.2D,
                        secondAngularVelocity,
                        poweredAngularVelocity
                );

        SubLevel subLevel =
                Sable.HELPER.getContaining(
                        this
                );

        previousSecondWheelAngle =
                secondWheelAngle;

        if (subLevel == null
                || secondWheelOffGround) {

            secondAngularVelocity =
                    attemptedAngularVelocity;

            secondWheelAngle +=
                    secondAngularVelocity;

            return;
        }

        Vector3d velocity =
                Sable.HELPER.getVelocity(
                        level,
                        JOMLConversion.toJOML(mechanicalDrive$getWheelCenter(facing))
                );

        Vector3d localVelocity =
                subLevel.logicalPose()
                        .transformNormalInverse(
                                velocity
                        )
                        .div(
                                20.0D
                        );

        Vector3dc rollingAxis =
                rotatedWheelAxis(
                        perpendicularHorizontal(
                                axisUnit(
                                        facing.getAxis()
                                )
                        )
                );

        double travelled =
                localVelocity.dot(
                        rollingAxis
                );

        double rollingAngularDelta =
                -travelled
                        / (
                        Math.PI
                                * wheel.radius()
                                * 2.0D
                )
                        * Math.PI
                        * 2.0D;

        if (secondContactFriction
                < 1.0D) {

            rollingAngularDelta =
                    Mth.lerp(
                            secondContactFriction,
                            attemptedAngularVelocity,
                            rollingAngularDelta
                    );
        }

        secondWheelAngle +=
                rollingAngularDelta;

        secondAngularVelocity =
                rollingAngularDelta;
    }

    private double clientGroundExtension(
            float wheelRadius,
            Direction facing
    ) {
        SubLevel containing =
                Sable.HELPER.getContaining(
                        this
                );

        if (containing == null) {
            return REST_TRAVEL;
        }

        Vector3dc rollingAxis =
                rotatedWheelAxis(
                        perpendicularHorizontal(
                                axisUnit(
                                        facing.getAxis()
                                )
                        )
                );

        TerrainContact contact =
                scanTerrain(
                        rollingAxis,
                        containing.logicalPose(),
                        facing,
                        wheelRadius
                );

        double rawExtension =
                contact.distance()
                        - wheelRadius;

        secondWheelOffGround =
                rawExtension
                        > REST_TRAVEL;

        secondContactFriction =
                contact.hitBlock() == null
                        ? 1.0D
                        : adjustedFriction(
                        PhysicsBlockPropertyHelper
                                .getFriction(
                                        level.getBlockState(
                                                contact.hitBlock()
                                        )
                                )
                );

        return Mth.clamp(
                rawExtension,
                -0.45D,
                REST_TRAVEL
        );
    }

    public double getLerpedSecondExtension(
            float partialTick
    ) {
        return Mth.lerp(
                partialTick,
                previousSecondExtension,
                secondExtension
        );
    }

    public float getLerpedSecondWheelAngle(
            float partialTick
    ) {
        return (float) Mth.lerp(
                partialTick,
                previousSecondWheelAngle,
                secondWheelAngle
        );
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied =
                32.0F;

        return lastStressApplied;
    }

    @Override
    public ItemRequirement getRequiredItems(
            BlockState state
    ) {
        List<StackRequirement> requirements =
                new ArrayList<>();

        ItemStack first =
                getWheel(
                        0
                );

        ItemStack second =
                getWheel(
                        1
                );

        if (!first.isEmpty()) {
            requirements.add(
                    new StackRequirement(
                            first,
                            ItemUseType.CONSUME
                    )
            );
        }

        if (!second.isEmpty()) {
            requirements.add(
                    new StackRequirement(
                            second,
                            ItemUseType.CONSUME
                    )
            );
        }

        if (requirements.isEmpty()) {
            return super.getRequiredItems(
                    state
            );
        }

        return new ItemRequirement(
                requirements
        );
    }

    @Override
    public String getClipboardKey() {
        return "Double Steering Wheel Mount";
    }

    @Override
    protected void write(
            CompoundTag tag,
            Provider registries,
            boolean clientPacket
    ) {
        super.write(
                tag,
                registries,
                clientPacket
        );

        tag.put(
                SECOND_WHEEL_TAG,
                getWheel(
                        1
                ).saveOptional(
                        registries
                )
        );
    }

    @Override
    protected void read(
            CompoundTag tag,
            Provider registries,
            boolean clientPacket
    ) {
        super.read(
                tag,
                registries,
                clientPacket
        );

        getInventory()
                .setWithoutNotification(
                        1,
                        ItemStack.parseOptional(
                                registries,
                                tag.getCompound(
                                        SECOND_WHEEL_TAG
                                )
                        )
                );

        if (clientPacket) {
            invalidateRenderBoundingBox();
        }
    }

    @Override
    public void clearContent() {
        getInventory()
                .setStackInSlot(
                        0,
                        ItemStack.EMPTY
                );

        getInventory()
                .setStackInSlot(
                        1,
                        ItemStack.EMPTY
                );
    }

    @Override
    protected AABB createRenderBoundingBox() {
        AABB bounds =
                new AABB(
                        getBlockPos()
                ).inflate(
                        WheelMountOffsets.maximumAbsolute(this)
                );

        float radius =
                0.0F;

        TireLike first =
                getWheel(
                        0
                ).get(
                        OffroadDataComponents.TIRE
                );

        TireLike second =
                getWheel(
                        1
                ).get(
                        OffroadDataComponents.TIRE
                );

        if (first != null) {
            radius =
                    Math.max(
                            radius,
                            first.radius()
                    );
        }

        if (second != null) {
            radius =
                    Math.max(
                            radius,
                            second.radius()
                    );
        }

        return radius <= 0.0F
                ? bounds
                : bounds.inflate(
                radius + 1.0F
        );
    }
}
