package dev.createmechanicaldrive.content.steering_wheel_mount;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.schematic.requirement.SpecialBlockEntityItemRequirement;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.createmechanicaldrive.content.suspension.SuspensionSpringTuning;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffsets;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerMountHelper;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionSteeringControlPassThrough;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SteeringWheelMountBlockEntity extends KineticBlockEntity
        implements BlockEntitySubLevelActor, Clearable, ClipboardCloneable,
        SpecialBlockEntityItemRequirement, SuspensionSpringTuning {

    private static final double REST_TRAVEL = 0.65D;
    private static final double EMPTY_VISUAL_EXTENSION = 0.5D;
    private static final double MAX_GROUND_SCAN = 5.0D;
    protected static final double MIN_BUMP_NORMAL_SPEED = 0.75D;
    protected static final double MAX_BUMP_NORMAL_SPEED = 2.5D;
    private static final float WHEEL_STEERING_LIMIT_DEGREES = 45.0F;
    private static final float STEERING_MIN_OUTPUT_RPM = 8.0F;
    private static final float STEERING_MAX_OUTPUT_RPM = 64.0F;
    private static final float STEERING_ERROR_DEGREES_PER_RPM = 0.6F;
    private static final float STEERING_START_DEADBAND = 5.0F;
    private static final float STEERING_STOP_EPSILON = 0.1F;
    private static final float STEERING_EPSILON = 0.001F;
    private static final String STEERING_TARGET_TAG = "SteeringTargetAngle";
    private static final String STEERING_ANGLE_TAG = "SteeringAngle";
    private static final String STEERING_OUTPUT_SPEED_TAG = "SteeringOutputSpeed";

    private static final String STEERING_INPUT_ANGLE_STEP_TAG =
            "SteeringInputAngleStep";

    private static final String LEGACY_NEGATIVE_STEERING_INPUT_ANGLE_STEP_TAG =
            "NegativeSteeringInputAngleStep";

    private static final String LEGACY_POSITIVE_STEERING_INPUT_ANGLE_STEP_TAG =
            "PositiveSteeringInputAngleStep";

    private static final String SUSPENSION_STRENGTH_TAG =
            "SuspensionStrength";

    private static final int DEFAULT_SUSPENSION_STRENGTH = 10;
    private static final int MIN_SUSPENSION_STRENGTH = 5;
    private static final int MAX_SUSPENSION_STRENGTH = 180;

    private static final Collection<SteeringWheelMountBlockEntity> PENDING_FORCE_APPLICATIONS =
            new ObjectOpenHashSet<>();

    private static final int STEERING_INPUT_ANGLE_STEP = 5;

    private static final int MIN_STEERING_INPUT_ANGLE_DEGREES = 5;
    private static final int MAX_STEERING_INPUT_ANGLE_DEGREES = 360;
    private static final int DEFAULT_STEERING_INPUT_ANGLE_DEGREES = 90;

    private static final int MIN_STEERING_INPUT_ANGLE_STEP =
            MIN_STEERING_INPUT_ANGLE_DEGREES / STEERING_INPUT_ANGLE_STEP;

    private static final int MAX_STEERING_INPUT_ANGLE_STEP =
            MAX_STEERING_INPUT_ANGLE_DEGREES / STEERING_INPUT_ANGLE_STEP;

    private static final int DEFAULT_STEERING_INPUT_ANGLE_STEP =
            DEFAULT_STEERING_INPUT_ANGLE_DEGREES / STEERING_INPUT_ANGLE_STEP;

    private static boolean physicsCallbackRegistered;

    private final SteeringWheelMountInventory inventory;
    private SuspensionStrengthBehaviour suspensionStrength;
    private double suspensionSpringMultiplier = DEFAULT_MULTIPLIER;
    private double suspensionDampingMultiplier = DEFAULT_MULTIPLIER;
    private double suspensionBumpClearanceMultiplier = DEFAULT_MULTIPLIER;
    private double suspensionBumpForceMultiplier = DEFAULT_MULTIPLIER;
    private double suspensionMaxImpulseMultiplier = DEFAULT_MULTIPLIER;
    private double suspensionDriveMultiplier = DEFAULT_MULTIPLIER;
    private double suspensionGripMultiplier = DEFAULT_MULTIPLIER;

    private double extension = EMPTY_VISUAL_EXTENSION;
    private double previousExtension = extension;
    private double steeringYaw;
    private double previousSteeringYaw;
    private float steeringTargetAngle;
    private float steeringAngle;
    private float steeringOutputSpeed;

    private int steeringInputAngleStep =
            DEFAULT_STEERING_INPUT_ANGLE_STEP;

    private int selectedSteeringInputAngleRow = 0;

    private static final String STEERING_INPUT_SELECTED_ROW_TAG =
            "SteeringInputSelectedRow";

    private double wheelAngle;
    private double previousWheelAngle;
    private double angularVelocity;
    private double contactFriction = 1.0D;
    private boolean wheelOffGround;

    private List<SteeringInputAngleBehaviour> steeringInputAngleBehaviours;
    private boolean syncingSteeringInputAngles;

    private final Vector3d pendingForcePosition = new Vector3d();
    private final Vector3d pendingImpulse = new Vector3d();
    private final ForceTotal accumulatedForces = new ForceTotal();

    public SteeringWheelMountBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(
                type,
                pos,
                state
        );

        inventory =
                createInventory();
    }

    protected SteeringWheelMountInventory createInventory() {
        return new SteeringWheelMountInventory(
                this
        );
    }

    public static synchronized void registerPhysicsCallback() {
        if (physicsCallbackRegistered) {
            return;
        }
        SableEventPlatform.INSTANCE.onPhysicsTick(SteeringWheelMountBlockEntity::flushPendingForces);
        physicsCallbackRegistered = true;
    }

    private static void flushPendingForces(SubLevelPhysicsSystem physicsSystem, double timeStep) {
        for (SteeringWheelMountBlockEntity mount : PENDING_FORCE_APPLICATIONS) {
            if (!mount.isRemoved()) {
                mount.applyAccumulatedForces();
            }
        }
        PENDING_FORCE_APPLICATIONS.clear();
    }

    public float getSteeringInputAngleDegrees() {
        return steeringInputAngleStep
                * STEERING_INPUT_ANGLE_STEP;
    }

    public float getNegativeSteeringInputAngleDegrees() {
        return getSteeringInputAngleDegrees();
    }

    public float getPositiveSteeringInputAngleDegrees() {
        return getSteeringInputAngleDegrees();
    }

    private static final BehaviourType<SteeringInputAngleBehaviour>
            STEERING_INPUT_ANGLE_FIRST_TYPE =
            new BehaviourType<>();

    private static final BehaviourType<SteeringInputAngleBehaviour>
            STEERING_INPUT_ANGLE_SECOND_TYPE =
            new BehaviourType<>();

    @Override
    public void addBehaviours(
            List<BlockEntityBehaviour> behaviours
    ) {
        super.addBehaviours(behaviours);

        suspensionStrength =
                new SuspensionStrengthBehaviour(
                        Component.translatable(
                                "mechanical_drive.scroll_option.suspension_strength"
                        ),
                        this,
                        createSuspensionStrengthValueBox()
                );

        suspensionStrength.value = DEFAULT_SUSPENSION_STRENGTH;

        behaviours.add(suspensionStrength);

        steeringInputAngleBehaviours =
                new ArrayList<>();

        List<Direction> steeringInputSides =
                getSteeringInputAngleSides();

        for (int i = 0; i < steeringInputSides.size(); i++) {
            addSteeringInputAngleBehaviour(
                    behaviours,
                    steeringInputSides.get(i),
                    i == 0
                            ? STEERING_INPUT_ANGLE_FIRST_TYPE
                            : STEERING_INPUT_ANGLE_SECOND_TYPE
            );
        }
    }

    protected List<Direction> getSteeringInputAngleSides() {
        Direction facing =
                getBlockState().getValue(
                        SteeringWheelMountBlock.HORIZONTAL_FACING
                );

        Direction.Axis sideAxis =
                facing.getAxis()
                        == Direction.Axis.X
                        ? Direction.Axis.Z
                        : Direction.Axis.X;

        Direction firstSide =
                Direction.get(
                        Direction.AxisDirection.POSITIVE,
                        sideAxis
                );

        Direction secondSide =
                Direction.get(
                        Direction.AxisDirection.NEGATIVE,
                        sideAxis
                );

        return List.of(
                firstSide,
                secondSide
        );
    }

    protected ValueBoxTransform createSuspensionStrengthValueBox() {
        return new SuspensionStrengthValueBox();
    }

    private void addSteeringInputAngleBehaviour(
            List<BlockEntityBehaviour> behaviours,
            Direction side,
            BehaviourType<SteeringInputAngleBehaviour> behaviourType
    ) {
        SteeringInputAngleBehaviour behaviour =
                new SteeringInputAngleBehaviour(
                        Component.translatable(
                                "mechanical_drive.steering_wheel_mount.input_angle"
                        ),
                        this,
                        new SteeringInputAngleValueBoxTransform(
                                this,
                                getBlockState().getValue(
                                        SteeringWheelMountBlock.HORIZONTAL_FACING
                                ),
                                side
                        ),
                        behaviourType
                );

        steeringInputAngleBehaviours.add(
                behaviour
        );

        behaviours.add(
                behaviour
        );
    }

    private static final class SteeringInputAngleValueBoxTransform
            extends ValueBoxTransform.Sided {

        private final SteeringWheelMountBlockEntity mount;
        private final Direction initialBlockFacing;
        private final Direction initialSide;

        private SteeringInputAngleValueBoxTransform(
                SteeringWheelMountBlockEntity mount,
                Direction blockFacing,
                Direction side
        ) {
            this.mount = mount;
            this.initialBlockFacing = blockFacing;
            this.initialSide = side;
            this.direction = side;
        }

        @Override
        protected Vec3 getSouthLocation() {
            Direction blockFacing = mount.getBlockState().getValue(
                    SteeringWheelMountBlock.HORIZONTAL_FACING
            );
            boolean oppositeFacing =
                    blockFacing.getAxisDirection()
                            == AxisDirection.NEGATIVE;

            boolean positiveSide =
                    getSide().getAxisDirection()
                            == AxisDirection.POSITIVE;

            boolean mirror =
                    oppositeFacing
                            ^ positiveSide
                            ^ (blockFacing.getAxis() == Axis.Z);

            float x =
                    mirror
                            ? 13.0F
                            : 3.0F;

            return VecHelper.voxelSpace(
                    x,
                    13.0F,
                    15.55F
            );
        }

        @Override
        public Direction getSide() {
            BlockState state = mount.getBlockState();
            if (!state.hasProperty(SteeringWheelMountBlock.HORIZONTAL_FACING)) {
                return initialSide;
            }
            if (!initialBlockFacing.getAxis().isHorizontal()
                    || !initialSide.getAxis().isHorizontal()) {
                return initialSide;
            }

            Direction currentFacing = state.getValue(
                    SteeringWheelMountBlock.HORIZONTAL_FACING
            );
            Direction rotatedFacing = initialBlockFacing;
            Direction rotatedSide = initialSide;

            for (int turn = 0; turn < 4; turn++) {
                if (rotatedFacing == currentFacing) {
                    return rotatedSide;
                }
                rotatedFacing = rotatedFacing.getClockWise();
                rotatedSide = rotatedSide.getClockWise();
            }

            return initialSide;
        }

        @Override
        protected boolean isSideActive(
                BlockState state,
                Direction direction
        ) {
            return mount.isSteeringInputAngleSideActive(
                    direction
            );
        }
    }

    private static final class SteeringInputAngleBehaviour
            extends ScrollValueBehaviour {

        private final SteeringWheelMountBlockEntity mount;
        private final BehaviourType<SteeringInputAngleBehaviour> behaviourType;

        private SteeringInputAngleBehaviour(
                Component label,
                SteeringWheelMountBlockEntity mount,
                ValueBoxTransform transform,
                BehaviourType<SteeringInputAngleBehaviour> behaviourType
        ) {
            super(
                    label,
                    mount,
                    transform
            );

            this.mount = mount;
            this.behaviourType = behaviourType;

            between(
                    MIN_STEERING_INPUT_ANGLE_STEP,
                    MAX_STEERING_INPUT_ANGLE_STEP
            );

            withFormatter(
                    step -> {
                        int degrees =
                                step * STEERING_INPUT_ANGLE_STEP;

                        return mount.selectedSteeringInputAngleRow == 0
                                ? "+" + degrees + "\u00B0"
                                : "-" + degrees + "\u00B0";
                    }
            );

            this.value =
                    mount.steeringInputAngleStep;
        }

        @Override
        public BehaviourType<?> getType() {
            return behaviourType;
        }

        @Override
        public int netId() {
            return 1;
        }

        @Override
        public ValueSettingsBoard createBoard(
                Player player,
                BlockHitResult hitResult
        ) {
            return new ValueSettingsBoard(
                    label,
                    MAX_STEERING_INPUT_ANGLE_STEP
                            - MIN_STEERING_INPUT_ANGLE_STEP,
                    2,
                    List.of(
                            Component.translatable(
                                    "mechanical_drive.steering_wheel_mount.input_angle_positive"
                            ),
                            Component.translatable(
                                    "mechanical_drive.steering_wheel_mount.input_angle_negative"
                            )
                    ),
                    new ValueSettingsFormatter(
                            settings -> {
                                int degrees =
                                        (
                                                settings.value()
                                                        + MIN_STEERING_INPUT_ANGLE_STEP
                                        )
                                                * STEERING_INPUT_ANGLE_STEP;

                                return Component.literal(
                                        settings.row() == 0
                                                ? "+" + degrees + "\u00B0"
                                                : "-" + degrees + "\u00B0"
                                );
                            }
                    )
            );
        }

        @Override
        public ValueSettings getValueSettings() {
            return new ValueSettings(
                    Mth.clamp(
                            mount.selectedSteeringInputAngleRow,
                            0,
                            1
                    ),
                    Mth.clamp(
                            mount.steeringInputAngleStep
                                    - MIN_STEERING_INPUT_ANGLE_STEP,
                            0,
                            MAX_STEERING_INPUT_ANGLE_STEP
                                    - MIN_STEERING_INPUT_ANGLE_STEP
                    )
            );
        }

        @Override
        public void setValueSettings(
                Player player,
                ValueSettings settings,
                boolean ctrlDown
        ) {
            int newRow =
                    Mth.clamp(
                            settings.row(),
                            0,
                            1
                    );

            int newStep =
                    Mth.clamp(
                            settings.value()
                                    + MIN_STEERING_INPUT_ANGLE_STEP,
                            MIN_STEERING_INPUT_ANGLE_STEP,
                            MAX_STEERING_INPUT_ANGLE_STEP
                    );

            boolean invertExistingSteering =
                    mount.selectedSteeringInputAngleRow != newRow;

            mount.selectedSteeringInputAngleRow =
                    newRow;

            mount.setSteeringInputAngleStep(
                    newStep,
                    invertExistingSteering
            );

            playFeedbackSound(
                    this
            );
        }
    }

    private void setSteeringInputAngleStep(
            int value,
            boolean invertExistingSteering
    ) {
        steeringInputAngleStep =
                Mth.clamp(
                        value,
                        MIN_STEERING_INPUT_ANGLE_STEP,
                        MAX_STEERING_INPUT_ANGLE_STEP
                );

        if (invertExistingSteering) {
            steeringTargetAngle =
                    -steeringTargetAngle;

            steeringAngle =
                    -steeringAngle;

            steeringOutputSpeed =
                    -steeringOutputSpeed;
        }

        clampSteeringInputAngles();

        syncSteeringInputAngleBehaviours();

        setChanged();

        if (level != null
                && !level.isClientSide) {
            sendData();
        }
    }

    private void syncSteeringInputAngleBehaviours() {
        if (steeringInputAngleBehaviours == null) {
            return;
        }

        for (SteeringInputAngleBehaviour behaviour :
                steeringInputAngleBehaviours) {

            behaviour.value =
                    steeringInputAngleStep;
        }
    }

    private void clampSteeringInputAngles() {
        float inputLimit =
                getSteeringInputAngleDegrees();

        steeringTargetAngle =
                Mth.clamp(
                        steeringTargetAngle,
                        -inputLimit,
                        inputLimit
                );

        steeringAngle =
                Mth.clamp(
                        steeringAngle,
                        -inputLimit,
                        inputLimit
                );
    }

    protected boolean isSteeringInputAngleSideActive(
            Direction direction
    ) {
        Direction facing =
                getBlockState().getValue(
                        SteeringWheelMountBlock.HORIZONTAL_FACING
                );

        return direction.getAxis().isHorizontal()
                && direction.getAxis()
                != facing.getAxis();
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        TireLike wheel = getWheelData();
        if (wheel == null) {
            return;
        }

        Direction facing = getBlockState().getValue(SteeringWheelMountBlock.HORIZONTAL_FACING);
        BlockPos mountPos = getBlockPos();
        Vec3 forcePoint = mechanicalDrive$getWheelCenter(facing);
        pendingForcePosition.set(forcePoint.x, forcePoint.y, forcePoint.z);

        MassData massData = subLevel.getMassTracker();
        double effectiveMass = 1.0D / massData.getInverseNormalMass(
                pendingForcePosition,
                OrientedBoundingBox3d.UP
        );
        double setting = suspensionStrength.getValue();
        double massFactor = Math.min(effectiveMass / setting, 1.0D) * 10.0D;
        double tractionScale = setting * massFactor * 2.0D;
        double springScale = setting * massFactor * 40.0D
                * suspensionSpringMultiplier;
        double dampingScale = setting * massFactor * suspensionDampingMultiplier;

        Pose3d vehiclePose = subLevel.logicalPose();
        Vector3dc lateralAxis = rotatedWheelAxis(axisUnit(facing.getAxis()));
        Vector3dc rollingAxis = rotatedWheelAxis(perpendicularHorizontal(axisUnit(facing.getAxis())));
        TerrainContact contact =
                scanTerrain(
                        rollingAxis,
                        vehiclePose,
                        facing,
                        wheel.radius()
                );
        double groundDistance = contact.distance();
        extension = groundDistance;

        if (groundDistance > REST_TRAVEL + wheel.radius()
                + 0.25D * suspensionBumpClearanceMultiplier) {
            extension = REST_TRAVEL;
            return;
        }

        double measuredLength = 0.10833333333333334D + extension;
        double compressedLength = Mth.clamp(measuredLength - wheel.radius(), 0.0D, REST_TRAVEL);
        Vector3d worldVelocity = Sable.HELPER.getVelocity(level, JOMLConversion.toJOML(forcePoint));
        Vector3d localVelocity = vehiclePose.transformNormalInverse(worldVelocity);
        double damping = -localVelocity.y * dampingScale;
        double springImpulse = (
                (REST_TRAVEL - compressedLength)
                        * springScale
                        * suspensionBumpForceMultiplier
                        + damping
        ) * timeStep * suspensionMaxImpulseMultiplier;

        double compressionRatio = Mth.clamp(
                (REST_TRAVEL - compressedLength) / REST_TRAVEL,
                0.0D,
                1.0D
        );
        double targetBumpSpeed = Mth.lerp(
                compressionRatio,
                MIN_BUMP_NORMAL_SPEED,
                MAX_BUMP_NORMAL_SPEED
        );
        double availableBumpSpeed = Math.max(
                targetBumpSpeed - localVelocity.y,
                0.0D
        );
        springImpulse = Math.min(
                springImpulse,
                effectiveMass * availableBumpSpeed
        );

        Vec3i hitNormal = contact.normal().getNormal();
        Vec3 suspensionImpulse = new Vec3(
                springImpulse * hitNormal.getX(),
                springImpulse * hitNormal.getY(),
                springImpulse * hitNormal.getZ()
        );
        if (contact.hitSubLevel() != null) {
            suspensionImpulse = contact.hitSubLevel().logicalPose().transformNormal(suspensionImpulse);
        }
        suspensionImpulse = vehiclePose.transformNormalInverse(suspensionImpulse);
        pendingImpulse.set(suspensionImpulse.x, suspensionImpulse.y, suspensionImpulse.z);

        contactFriction = contact.hitBlock() == null
                ? 1.0D
                : adjustedFriction(PhysicsBlockPropertyHelper.getFriction(level.getBlockState(contact.hitBlock())));

        double brake = level.getSignal(mountPos.above(), Direction.UP) / 15.0D;
        double usableSurfaceFriction = Math.min(contactFriction, 1.0D);
        double rollingResistance = (0.075D + brake * 0.3D) * usableSurfaceFriction;
        float kineticSpeed = facing.getAxis() == Axis.X ? getSpeed() : -getSpeed();

        pendingImpulse.fma(
                localVelocity.dot(rollingAxis) * -rollingResistance * tractionScale * timeStep
                        + kineticSpeed * (1.0D - brake) * usableSurfaceFriction * 1.75D
                        * suspensionDriveMultiplier * timeStep,
                rollingAxis
        );
        pendingImpulse.fma(
                localVelocity.dot(lateralAxis) * -0.6D * contactFriction * tractionScale
                        * suspensionGripMultiplier * timeStep,
                lateralAxis
        );

        accumulatedForces.applyImpulseAtPoint(subLevel, pendingForcePosition, pendingImpulse);
        PENDING_FORCE_APPLICATIONS.add(this);
    }

    private void applyAccumulatedForces() {
        SubLevel containing = Sable.HELPER.getContaining(this);
        if (containing instanceof ServerSubLevel serverSubLevel) {
            RigidBodyHandle.of(serverSubLevel).applyForcesAndReset(accumulatedForces);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null) {
            return;
        }

        if (!level.isClientSide) {
            updateSteeringFromShaft();
            steeringYaw = wheelYawFromShaftAngle(steeringAngle);
            return;
        }

        previousSteeringYaw = steeringYaw;
        steeringYaw = Mth.lerp(0.4D, steeringYaw, wheelYawFromShaftAngle(steeringAngle));

        TireLike wheel = getWheelData();
        previousExtension = extension;
        if (wheel == null) {
            if (ShaftMarkerItem.isMarker(getWheel())) {
                Direction facing = getBlockState().getValue(
                        SteeringWheelMountBlock.HORIZONTAL_FACING
                );
                previousWheelAngle = wheelAngle;
                angularVelocity = ShaftMarkerMountHelper
                        .approachDrivenAngularVelocity(
                                this,
                                facing,
                                angularVelocity
                        );
                wheelAngle += angularVelocity;
                wheelOffGround = true;
                extension = Mth.lerp(
                        0.6D,
                        extension,
                        EMPTY_VISUAL_EXTENSION
                );
                return;
            }

            previousWheelAngle = 0.0D;
            wheelAngle = 0.0D;
            angularVelocity = 0.0D;
            extension = Mth.lerp(0.6D, extension, EMPTY_VISUAL_EXTENSION);
            return;
        }

        extension = Mth.lerp(0.7D, extension, clientGroundExtension(wheel.radius()));
        Direction facing = getBlockState().getValue(SteeringWheelMountBlock.HORIZONTAL_FACING);
        float speed = facing.getAxis() == Axis.X ? -getSpeed() : getSpeed();
        double poweredAngularVelocity = speed * Math.PI * 2.0D / 60.0D / 20.0D
                * (15 - level.getSignal(getBlockPos().above(), Direction.UP)) / 15.0D;
        double attemptedAngularVelocity = Mth.lerp(0.2D, angularVelocity, poweredAngularVelocity);
        SubLevel subLevel = Sable.HELPER.getContaining(this);

        previousWheelAngle = wheelAngle;
        if (subLevel == null || wheelOffGround) {
            angularVelocity = attemptedAngularVelocity;
            wheelAngle += angularVelocity;
            return;
        }

        Vector3d velocity = Sable.HELPER.getVelocity(
                level,
                JOMLConversion.toJOML(mechanicalDrive$getWheelCenter(facing))
        );
        Vector3d localVelocity = subLevel.logicalPose().transformNormalInverse(velocity).div(20.0D);
        Vector3dc rollingAxis = rotatedWheelAxis(perpendicularHorizontal(axisUnit(facing.getAxis())));
        double travelled = localVelocity.dot(rollingAxis);
        double rollingAngularDelta = -travelled / (Math.PI * wheel.radius() * 2.0D) * Math.PI * 2.0D;
        if (contactFriction < 1.0D) {
            rollingAngularDelta = Mth.lerp(contactFriction, attemptedAngularVelocity, rollingAngularDelta);
        }
        wheelAngle += rollingAngularDelta;
        angularVelocity = rollingAngularDelta;
    }

    private void updateSteeringFromShaft() {
        boolean changed = updateSteeringTarget();
        changed |= advanceSteeringAngle();
        changed |= updateSteeringOutputSpeed();

        if (changed) {
            setChanged();
            sendData();
        }
    }

    private boolean updateSteeringTarget() {
        float inputSpeed = getLogicalSteeringShaftSpeed();
        if (Math.abs(inputSpeed) < STEERING_EPSILON) {
            return false;
        }

        float inputAngleDelta = KineticBlockEntity.convertToAngular(inputSpeed);
        float inputLimit = getSteeringInputAngleDegrees();
        float newTarget = Mth.clamp(
                steeringTargetAngle + inputAngleDelta,
                -inputLimit,
                inputLimit
        );
        if (Math.abs(newTarget - steeringTargetAngle) < STEERING_EPSILON) {
            return false;
        }

        steeringTargetAngle = newTarget;
        return true;
    }

    private boolean advanceSteeringAngle() {
        if (Math.abs(steeringOutputSpeed) < STEERING_EPSILON) {
            return false;
        }

        float previousError = steeringTargetAngle - steeringAngle;
        float newAngle = steeringAngle
                + KineticBlockEntity.convertToAngular(steeringOutputSpeed);
        float newError = steeringTargetAngle - newAngle;

        steeringAngle = Math.signum(previousError) != Math.signum(newError)
                ? steeringTargetAngle
                : newAngle;
        return true;
    }

    private boolean updateSteeringOutputSpeed() {
        float error = steeringTargetAngle - steeringAngle;
        float absoluteError = Math.abs(error);
        boolean moving = Math.abs(steeringOutputSpeed) > STEERING_EPSILON;
        float newSpeed;

        if ((!moving && absoluteError < STEERING_START_DEADBAND)
                || absoluteError <= STEERING_STOP_EPSILON) {
            if (moving && absoluteError <= STEERING_STOP_EPSILON) {
                steeringAngle = steeringTargetAngle;
            }
            newSpeed = 0.0F;
        } else {
            float errorDrivenRpm = Mth.clamp(
                    absoluteError / STEERING_ERROR_DEGREES_PER_RPM,
                    STEERING_MIN_OUTPUT_RPM,
                    STEERING_MAX_OUTPUT_RPM
            );
            newSpeed = Math.copySign(errorDrivenRpm, error);
        }

        if (Math.abs(newSpeed - steeringOutputSpeed) < STEERING_EPSILON) {
            return false;
        }
        steeringOutputSpeed = newSpeed;
        return true;
    }

    private double wheelYawFromShaftAngle(float shaftAngle) {
        float inputLimit = getSteeringInputAngleDegrees();

        if (inputLimit <= STEERING_EPSILON) {
            return 0.0D;
        }

        float normalized = Mth.clamp(
                shaftAngle / inputLimit,
                -1.0F,
                1.0F
        );

        return Math.toRadians(
                normalized * WHEEL_STEERING_LIMIT_DEGREES
        );
    }

    public Direction getSteeringShaftDirection() {
        return SteeringWheelMountBlock.getSteeringShaftDirection(getBlockState());
    }

    public boolean isSteeringInputNeighbour(BlockPos neighbourPos) {
        BlockPos offset = neighbourPos.subtract(worldPosition);
        if (offset.distManhattan(BlockPos.ZERO) != 1) {
            return false;
        }
        Direction direction = Direction.getNearest(offset.getX(), offset.getY(), offset.getZ());
        return isSteeringShaftDirection(direction);
    }

    public boolean isSteeringShaftDirection(Direction direction) {
        return direction.getAxis() == getSteeringShaftDirection().getAxis();
    }

    public float getSteeringInputVisualSpeed() {
        Direction inputSide = getSteeringInputSide();
        if (inputSide == null) {
            return 0.0F;
        }

        float rawSpeed = getSteeringSpeedFromSide(inputSide);
        Direction positiveAxis = Direction.get(
                AxisDirection.POSITIVE,
                getSteeringShaftDirection().getAxis()
        );
        return KineticBlockEntity.convertToDirection(rawSpeed, positiveAxis);
    }

    private float getLogicalSteeringShaftSpeed() {
        Direction inputSide = getSteeringInputSide();
        if (inputSide == null) {
            return 0.0F;
        }

        Direction positiveAxis = Direction.get(
                AxisDirection.POSITIVE,
                getSteeringShaftDirection().getAxis()
        );

        float speed = KineticBlockEntity.convertToDirection(
                getSteeringSpeedFromSide(inputSide),
                positiveAxis
        );

        return selectedSteeringInputAngleRow == 0
                ? speed
                : -speed;
    }

    @Nullable
    protected Direction getSteeringInputSide() {
        Direction first = getSteeringShaftDirection();
        if (Math.abs(getSteeringSpeedFromSide(first)) > STEERING_EPSILON) {
            return first;
        }

        Direction opposite = first.getOpposite();
        return Math.abs(getSteeringSpeedFromSide(opposite)) > STEERING_EPSILON
                ? opposite
                : null;
    }

    protected float getSteeringSpeedFromSide(Direction side) {
        return getSteeringSpeedFromSide(this, side, null);
    }

    public boolean isSteeringPassThroughEnabled() {
        return true;
    }

    private static float getSteeringSpeedFromSide(
            SteeringWheelMountBlockEntity mount,
            Direction side,
            @Nullable BlockPos excludedNeighbour
    ) {
        LevelAccessor level = mount.level;
        if (level == null) {
            return 0.0F;
        }

        BlockPos neighbourPos = mount.worldPosition.relative(side);
        if (neighbourPos.equals(excludedNeighbour)) {
            return 0.0F;
        }

        BlockState neighbourState = level.getBlockState(neighbourPos);
        if (!(level.getBlockEntity(neighbourPos) instanceof KineticBlockEntity kinetic)
                || !(neighbourState.getBlock() instanceof KineticBlock kineticBlock)
                || kinetic instanceof TankTransmissionSteeringControlPassThrough passThrough
                && passThrough.mechanicalDrive$isTankSteeringControlPassThrough()
                && mount.worldPosition.equals(
                passThrough.mechanicalDrive$getTankSteeringControlPassThroughSource()
        )
                || !kineticBlock.hasShaftTowards(
                level,
                neighbourPos,
                neighbourState,
                side.getOpposite()
        )) {
            return 0.0F;
        }

        return kinetic.getSpeed();
    }

    public static SteeringPassThrough getSteeringPassThrough(KineticBlockEntity output) {
        if (output.getLevel() == null) {
            return SteeringPassThrough.EMPTY;
        }

        for (Direction directionToMount : Direction.values()) {
            BlockPos mountPos = output.getBlockPos().relative(directionToMount);
            if (!(output.getLevel().getBlockEntity(mountPos)
                    instanceof SteeringWheelMountBlockEntity mount)) {
                continue;
            }

            if (!mount.isSteeringPassThroughEnabled()) {
                continue;
            }

            Direction outputSide = directionToMount.getOpposite();
            if (!mount.isSteeringShaftDirection(outputSide)
                    || !hasShaftTowards(output, directionToMount)) {
                continue;
            }

            Direction inputSide = outputSide.getOpposite();
            float inputSpeed = getSteeringSpeedFromSide(
                    mount,
                    inputSide,
                    output.getBlockPos()
            );
            if (Math.abs(inputSpeed) > STEERING_EPSILON) {
                return new SteeringPassThrough(inputSpeed, mount.getBlockPos());
            }
        }

        return SteeringPassThrough.EMPTY;
    }

    private static boolean hasShaftTowards(
            KineticBlockEntity blockEntity,
            Direction direction
    ) {
        BlockState state = blockEntity.getBlockState();
        return state.getBlock() instanceof KineticBlock kineticBlock
                && kineticBlock.hasShaftTowards(
                blockEntity.getLevel(),
                blockEntity.getBlockPos(),
                state,
                direction
        );
    }

    public record SteeringPassThrough(float speed, BlockPos sourceMountPos) {
        public static final SteeringPassThrough EMPTY =
                new SteeringPassThrough(0.0F, null);
    }

    private double clientGroundExtension(float wheelRadius) {
        SubLevel containing = Sable.HELPER.getContaining(this);
        if (containing == null) {
            return REST_TRAVEL;
        }

        Direction facing = getBlockState().getValue(SteeringWheelMountBlock.HORIZONTAL_FACING);
        Vector3dc rollingAxis = rotatedWheelAxis(perpendicularHorizontal(axisUnit(facing.getAxis())));
        TerrainContact contact =
                scanTerrain(
                        rollingAxis,
                        containing.logicalPose(),
                        facing,
                        wheelRadius
                );
        double rawExtension = contact.distance() - wheelRadius;
        wheelOffGround = rawExtension > REST_TRAVEL;
        contactFriction = contact.hitBlock() == null
                ? 1.0D
                : adjustedFriction(PhysicsBlockPropertyHelper.getFriction(level.getBlockState(contact.hitBlock())));
        return Mth.clamp(rawExtension, -0.45D, REST_TRAVEL);
    }

    protected Vec3 mechanicalDrive$getWheelCenter(Direction wheelSide) {
        return WheelMountOffsets.apply(
                this,
                getBlockPos().relative(wheelSide).getCenter(),
                wheelSide
        );
    }
    protected TerrainContact scanTerrain(
            Vector3dc rollingAxis,
            Pose3dc vehiclePose,
            Direction facing,
            float wheelRadius
    ) {
        Vec3 wheelCenter = mechanicalDrive$getWheelCenter(facing);
        double closestDistance = MAX_GROUND_SCAN;
        Direction closestNormal = Direction.UP;
        SubLevel closestSubLevel = null;
        BlockPos closestBlock = null;

        for (int offset = -1; offset <= 1; offset++) {
            Vec3 rayStart = wheelCenter.add(JOMLConversion.toMojang(rollingAxis).scale(offset));
            ClipContext context = new ClipContext(
                    rayStart,
                    rayStart.subtract(0.0D, MAX_GROUND_SCAN, 0.0D),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
            );
            ((ClipContextExtension) context).sable$setIgnoredSubLevel(Sable.HELPER.getContaining(this));
            BlockHitResult hit = level.clip(context);
            if (hit.getType() == BlockHitResult.Type.MISS) {
                continue;
            }

            SubLevel hitSubLevel = Sable.HELPER.getContaining(level, hit.getLocation());
            Vec3 worldHit = hitSubLevel == null
                    ? hit.getLocation()
                    : hitSubLevel.logicalPose().transformPosition(hit.getLocation());
            Vec3 localHit = vehiclePose.transformPositionInverse(worldHit);
            double rayDistance = wheelCenter.y - localHit.y;
            if (localHit.y > wheelCenter.y
                    || rayStart.distanceTo(localHit) < 0.05D
                    || rayDistance <= 1.0E-5D) {
                continue;
            }

            Direction hitDirection = hit.getDirection();
            Vector3d normal = new Vector3d(
                    hitDirection.getStepX(),
                    hitDirection.getStepY(),
                    hitDirection.getStepZ()
            );
            if (hitSubLevel != null) {
                hitSubLevel.logicalPose().transformNormal(normal);
            }
            vehiclePose.transformNormalInverse(normal);
            if (normal.lengthSquared() < 1.0E-8D) {
                continue;
            }
            normal.normalize();

            double suspensionProjection = normal.y;
            if (suspensionProjection < 0.5D) {
                continue;
            }

            double rollingProjection = normal.dot(rollingAxis);
            double wheelPlaneProjection = Math.sqrt(
                    suspensionProjection * suspensionProjection
                            + rollingProjection * rollingProjection
            );
            double distance = rayDistance
                    - rollingProjection * offset / suspensionProjection
                    + wheelRadius * (1.0D - wheelPlaneProjection / suspensionProjection);

            if (distance >= closestDistance) {
                continue;
            }

            closestDistance = distance;
            closestNormal = hitDirection;
            closestSubLevel = hitSubLevel;
            closestBlock = hit.getBlockPos();
        }

        return new TerrainContact(closestDistance, closestNormal, closestSubLevel, closestBlock);
    }

    protected static Vec3i axisUnit(Axis axis) {
        return Direction.get(AxisDirection.POSITIVE, axis).getNormal();
    }

    protected static Vec3i perpendicularHorizontal(Vec3i axis) {
        return new Vec3i(axis.getZ(), 0, axis.getX());
    }

    protected Vector3dc rotatedWheelAxis(Vec3i axis) {
        return new Vector3d(axis.getX(), axis.getY(), axis.getZ()).rotateY(steeringYaw);
    }

    protected static double adjustedFriction(double friction) {
        return friction < 1.0D ? 0.1D + 0.9D * friction : friction;
    }

    public SteeringWheelMountInventory getInventory() {
        return inventory;
    }

    public ItemStack getWheel() {
        return inventory.getStackInSlot(0);
    }

    @Nullable
    private TireLike getWheelData() {
        return getWheel().get(OffroadDataComponents.TIRE);
    }

    public void onWheelChanged() {
        setChanged();
        invalidateRenderBoundingBox();
        sendData();
    }

    public double getLerpedExtension(float partialTick) {
        return Mth.lerp(partialTick, previousExtension, extension);
    }

    public double getLerpedYaw(float partialTick) {
        return Mth.lerp(partialTick, previousSteeringYaw, steeringYaw);
    }

    public float getLerpedWheelAngle(float partialTick) {
        return (float) Mth.lerp(partialTick, previousWheelAngle, wheelAngle);
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = 16.0F;
        return lastStressApplied;
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state) {
        return getWheel().isEmpty()
                ? super.getRequiredItems(state)
                : new ItemRequirement(ItemUseType.CONSUME, getWheel());
    }

    @Override
    public String getClipboardKey() {
        return "Steering Wheel Mount";
    }

    @Override
    public boolean writeToClipboard(@NotNull Provider registries, CompoundTag tag, Direction side) {
        return false;
    }

    @Override
    public boolean readFromClipboard(
            @NotNull Provider registries,
            CompoundTag tag,
            Player player,
            Direction side,
            boolean simulate
    ) {
        return false;
    }

    protected void queueWheelImpulse(
            ServerSubLevel subLevel,
            Vec3 forcePoint,
            Vector3dc impulse
    ) {
        Vector3d position =
                new Vector3d(
                        forcePoint.x,
                        forcePoint.y,
                        forcePoint.z
                );

        Vector3d force =
                new Vector3d(
                        impulse
                );

        accumulatedForces.applyImpulseAtPoint(
                subLevel,
                position,
                force
        );

        PENDING_FORCE_APPLICATIONS.add(
                this
        );
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

        int suspensionValue =
                suspensionStrength != null
                        ? suspensionStrength.getValue()
                        : DEFAULT_SUSPENSION_STRENGTH;

        tag.putInt(
                SUSPENSION_STRENGTH_TAG,
                Mth.clamp(
                        suspensionValue,
                        MIN_SUSPENSION_STRENGTH,
                        MAX_SUSPENSION_STRENGTH
                )
        );

        tag.putDouble(
                SPRING_NBT_KEY,
                suspensionSpringMultiplier
        );
        tag.putDouble(DAMPING_NBT_KEY, suspensionDampingMultiplier);
        tag.putDouble(
                BUMP_CLEARANCE_NBT_KEY,
                suspensionBumpClearanceMultiplier
        );
        tag.putDouble(BUMP_FORCE_NBT_KEY, suspensionBumpForceMultiplier);
        tag.putDouble(
                MAX_IMPULSE_NBT_KEY,
                suspensionMaxImpulseMultiplier
        );
        tag.putDouble(DRIVE_NBT_KEY, suspensionDriveMultiplier);
        tag.putDouble(GRIP_NBT_KEY, suspensionGripMultiplier);

        tag.put(
                "Wheel",
                getWheel().saveOptional(registries)
        );

        tag.putInt(
                STEERING_INPUT_ANGLE_STEP_TAG,
                steeringInputAngleStep
        );

        tag.putInt(
                STEERING_INPUT_SELECTED_ROW_TAG,
                selectedSteeringInputAngleRow
        );

        tag.putFloat(
                STEERING_TARGET_TAG,
                steeringTargetAngle
        );

        tag.putFloat(
                STEERING_ANGLE_TAG,
                steeringAngle
        );

        tag.putFloat(
                STEERING_OUTPUT_SPEED_TAG,
                steeringOutputSpeed
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

        inventory.setWithoutNotification(
                ItemStack.parseOptional(
                        registries,
                        tag.getCompound("Wheel")
                )
        );

        int loadedSuspensionStrength =
                tag.contains(SUSPENSION_STRENGTH_TAG)
                        ? tag.getInt(SUSPENSION_STRENGTH_TAG)
                        : DEFAULT_SUSPENSION_STRENGTH;

        loadedSuspensionStrength =
                Mth.clamp(
                        loadedSuspensionStrength,
                        MIN_SUSPENSION_STRENGTH,
                        MAX_SUSPENSION_STRENGTH
                );

        if (suspensionStrength != null) {
            suspensionStrength.value =
                    loadedSuspensionStrength;
        }

        suspensionSpringMultiplier =
                tag.contains(SPRING_NBT_KEY)
                        ? SuspensionSpringTuning.clampMultiplier(
                                tag.getDouble(SPRING_NBT_KEY)
                        )
                        : DEFAULT_MULTIPLIER;
        suspensionDampingMultiplier =
                tag.contains(DAMPING_NBT_KEY)
                        ? SuspensionSpringTuning.clampMultiplier(
                                tag.getDouble(DAMPING_NBT_KEY)
                        )
                        : DEFAULT_MULTIPLIER;
        suspensionBumpClearanceMultiplier =
                tag.contains(BUMP_CLEARANCE_NBT_KEY)
                        ? SuspensionSpringTuning.clampMultiplier(
                                tag.getDouble(BUMP_CLEARANCE_NBT_KEY)
                        )
                        : DEFAULT_MULTIPLIER;
        suspensionBumpForceMultiplier =
                tag.contains(BUMP_FORCE_NBT_KEY)
                        ? SuspensionSpringTuning.clampMultiplier(
                                tag.getDouble(BUMP_FORCE_NBT_KEY)
                        )
                        : DEFAULT_MULTIPLIER;
        suspensionMaxImpulseMultiplier =
                tag.contains(MAX_IMPULSE_NBT_KEY)
                        ? SuspensionSpringTuning.clampMultiplier(
                                tag.getDouble(MAX_IMPULSE_NBT_KEY)
                        )
                        : DEFAULT_MULTIPLIER;
        suspensionDriveMultiplier =
                tag.contains(DRIVE_NBT_KEY)
                        ? SuspensionSpringTuning.clampMultiplier(
                                tag.getDouble(DRIVE_NBT_KEY)
                        )
                        : DEFAULT_MULTIPLIER;
        suspensionGripMultiplier =
                tag.contains(GRIP_NBT_KEY)
                        ? SuspensionSpringTuning.clampMultiplier(
                                tag.getDouble(GRIP_NBT_KEY)
                        )
                        : DEFAULT_MULTIPLIER;

        int loadedSteeringInputAngleStep;

        if (tag.contains(STEERING_INPUT_ANGLE_STEP_TAG)) {
            loadedSteeringInputAngleStep =
                    tag.getInt(STEERING_INPUT_ANGLE_STEP_TAG);
        } else if (tag.contains(LEGACY_POSITIVE_STEERING_INPUT_ANGLE_STEP_TAG)
                || tag.contains(LEGACY_NEGATIVE_STEERING_INPUT_ANGLE_STEP_TAG)) {
            loadedSteeringInputAngleStep =
                    tag.contains(LEGACY_POSITIVE_STEERING_INPUT_ANGLE_STEP_TAG)
                            ? tag.getInt(LEGACY_POSITIVE_STEERING_INPUT_ANGLE_STEP_TAG)
                            : tag.getInt(LEGACY_NEGATIVE_STEERING_INPUT_ANGLE_STEP_TAG);
        } else {
            loadedSteeringInputAngleStep =
                    DEFAULT_STEERING_INPUT_ANGLE_STEP;
        }

        steeringInputAngleStep =
                Mth.clamp(
                        loadedSteeringInputAngleStep,
                        MIN_STEERING_INPUT_ANGLE_STEP,
                        MAX_STEERING_INPUT_ANGLE_STEP
                );

        selectedSteeringInputAngleRow =
                Mth.clamp(
                        tag.contains(
                                STEERING_INPUT_SELECTED_ROW_TAG
                        )
                                ? tag.getInt(
                                STEERING_INPUT_SELECTED_ROW_TAG
                        )
                                : 0,
                        0,
                        1
                );

        syncSteeringInputAngleBehaviours();

        float steeringInputLimit =
                getSteeringInputAngleDegrees();

        steeringTargetAngle =
                Mth.clamp(
                        tag.getFloat(
                                STEERING_TARGET_TAG
                        ),
                        -steeringInputLimit,
                        steeringInputLimit
                );

        steeringAngle =
                Mth.clamp(
                        tag.getFloat(
                                STEERING_ANGLE_TAG
                        ),
                        -steeringInputLimit,
                        steeringInputLimit
                );

        steeringOutputSpeed =
                tag.getFloat(
                        STEERING_OUTPUT_SPEED_TAG
                );

        if (clientPacket) {
            invalidateRenderBoundingBox();
        } else {
            steeringYaw =
                    wheelYawFromShaftAngle(
                            steeringAngle
                    );

            previousSteeringYaw =
                    steeringYaw;
        }
    }

    @Override
    public void clearContent() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        AABB bounds = new AABB(getBlockPos())
                .inflate(WheelMountOffsets.maximumAbsolute(this));
        TireLike wheel = getWheelData();
        return wheel == null ? bounds : bounds.inflate(wheel.radius() + 1.0F);
    }

    private static final class SuspensionStrengthBehaviour
            extends ScrollValueBehaviour {

        private static final BehaviourType<SuspensionStrengthBehaviour> TYPE =
                new BehaviourType<>();

        private static final int SUSPENSION_MILESTONE_INTERVAL = 10;

        private SuspensionStrengthBehaviour(
                Component label,
                SmartBlockEntity blockEntity,
                ValueBoxTransform transform
        ) {
            super(
                    label,
                    blockEntity,
                    transform
            );

            between(
                    MIN_SUSPENSION_STRENGTH,
                    MAX_SUSPENSION_STRENGTH
            );
        }

        @Override
        public BehaviourType<?> getType() {
            return TYPE;
        }

        @Override
        public int netId() {
            return 0;
        }

        @Override
        public ValueSettingsBoard createBoard(
                Player player,
                BlockHitResult hitResult
        ) {
            return new ValueSettingsBoard(
                    label,
                    MAX_SUSPENSION_STRENGTH,
                    SUSPENSION_MILESTONE_INTERVAL,

                    List.of(
                            Component.translatable(
                                    "mechanical_drive.scroll_option.suspension_strength_label"
                            )
                    ),

                    new ValueSettingsFormatter(
                            settings ->
                                    Component.literal(
                                            Integer.toString(
                                                    settings.value()
                                            )
                                    )
                    )
            );
        }

        @Override
        public ValueSettings getValueSettings() {
            return new ValueSettings(
                    0,
                    Mth.clamp(
                            getValue(),
                            MIN_SUSPENSION_STRENGTH,
                            MAX_SUSPENSION_STRENGTH
                    )
            );
        }

        @Override
        public void setValueSettings(
                Player player,
                ValueSettings settings,
                boolean ctrlDown
        ) {
            int newValue =
                    Mth.clamp(
                            settings.value(),
                            MIN_SUSPENSION_STRENGTH,
                            MAX_SUSPENSION_STRENGTH
                    );

            setValue(
                    newValue
            );

            playFeedbackSound(
                    this
            );
        }
    }

    protected static class SuspensionStrengthValueBox extends ValueBoxTransform {
        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack poseStack) {
            Direction facing = state.getValue(SteeringWheelMountBlock.HORIZONTAL_FACING);
            float yaw = AngleHelper.horizontalAngle(facing) + 180.0F;
            ((PoseTransformStack) TransformStack.of(poseStack).rotateYDegrees(yaw)).rotateXDegrees(90.0F);
        }

        @Override
        public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
            Vec3 target = getLocalOffset(level, pos, state);
            return target != null && localHit.distanceTo(target) < scale / 3.0F;
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction facing = state.getValue(SteeringWheelMountBlock.HORIZONTAL_FACING);
            float yaw = AngleHelper.horizontalAngle(facing) + 180.0F;
            return VecHelper.rotateCentered(VecHelper.voxelSpace(8.0D, 15.5D, 11.0D), yaw, Axis.Y);
        }
    }

    @Override
    public double mechanicalDrive$adjustSuspensionTuning(
            String tuning,
            int steps
    ) {
        double multiplier =
                SuspensionSpringTuning.steppedMultiplier(
                        tuning,
                        mechanicalDrive$getSuspensionTuning(tuning),
                        steps
                );
        mechanicalDrive$setSuspensionTuning(tuning, multiplier);

        mechanicalDrive$syncSuspensionTuning();
        return multiplier;
    }

    @Override
    public double mechanicalDrive$getSuspensionTuning(String tuning) {
        return switch (tuning) {
            case DAMPING -> suspensionDampingMultiplier;
            case BUMP_CLEARANCE -> suspensionBumpClearanceMultiplier;
            case BUMP_FORCE -> suspensionBumpForceMultiplier;
            case MAX_IMPULSE -> suspensionMaxImpulseMultiplier;
            case DRIVE -> suspensionDriveMultiplier;
            case GRIP -> suspensionGripMultiplier;
            default -> suspensionSpringMultiplier;
        };
    }

    @Override
    public void mechanicalDrive$resetSuspensionTuning() {
        suspensionSpringMultiplier = DEFAULT_MULTIPLIER;
        suspensionDampingMultiplier = DEFAULT_MULTIPLIER;
        suspensionBumpClearanceMultiplier = DEFAULT_MULTIPLIER;
        suspensionBumpForceMultiplier = DEFAULT_MULTIPLIER;
        suspensionMaxImpulseMultiplier = DEFAULT_MULTIPLIER;
        suspensionDriveMultiplier = DEFAULT_MULTIPLIER;
        suspensionGripMultiplier = DEFAULT_MULTIPLIER;
        mechanicalDrive$syncSuspensionTuning();
    }

    private void mechanicalDrive$setSuspensionTuning(
            String tuning,
            double multiplier
    ) {
        switch (tuning) {
            case DAMPING -> suspensionDampingMultiplier = multiplier;
            case BUMP_CLEARANCE ->
                    suspensionBumpClearanceMultiplier = multiplier;
            case BUMP_FORCE -> suspensionBumpForceMultiplier = multiplier;
            case MAX_IMPULSE -> suspensionMaxImpulseMultiplier = multiplier;
            case DRIVE -> suspensionDriveMultiplier = multiplier;
            case GRIP -> suspensionGripMultiplier = multiplier;
            default -> suspensionSpringMultiplier = multiplier;
        }
    }


    private void mechanicalDrive$syncSuspensionTuning() {
        setChanged();

        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    protected int getSuspensionStrengthValue() {
        return suspensionStrength != null
                ? suspensionStrength.getValue()
                : DEFAULT_SUSPENSION_STRENGTH;
    }

    protected record TerrainContact(
            double distance,
            @NotNull Direction normal,
            @Nullable SubLevel hitSubLevel,
            @Nullable BlockPos hitBlock
    ) {
    }
}
