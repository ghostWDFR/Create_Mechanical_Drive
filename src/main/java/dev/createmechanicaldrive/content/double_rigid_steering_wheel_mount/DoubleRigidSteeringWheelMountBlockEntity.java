package dev.createmechanicaldrive.content.double_rigid_steering_wheel_mount;

import com.simibubi.create.api.schematic.requirement.SpecialBlockEntityItemRequirement;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StackRequirement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.createmechanicaldrive.content.suspension.RigidContactSolver;
import dev.createmechanicaldrive.content.suspension.RigidMountTuningValues;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffsets;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerMountHelper;
import dev.createmechanicaldrive.content.suspension.SuspensionSpringTuning;
import dev.createmechanicaldrive.content.tank_transmission.TankTransmissionSteeringControlPassThrough;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

public class DoubleRigidSteeringWheelMountBlockEntity extends KineticBlockEntity
        implements BlockEntitySubLevelActor, Clearable, ClipboardCloneable,
        SpecialBlockEntityItemRequirement, SuspensionSpringTuning {


    private static final double MAX_GROUND_SCAN = 5.0D;
    private static final double RIGID_CONTACT_TOLERANCE = 0.05D;
    private static final double RIGID_CONTACT_RELEASE_TOLERANCE = 0.10D;

    private static final double RIGID_CONTACT_SKIN = 0.0625D;
    private static final double RIGID_POSITION_SLOP = 0.01D;
    private static final double RIGID_POSITION_CORRECTION = 0.2D;
    private static final double RIGID_MAX_CORRECTION_SPEED = 1.0D;

    private static final double RIGID_LATERAL_FRICTION = 1.0D;
    private static final double RIGID_ROLLING_RESISTANCE = 0.025D;
    private static final double RIGID_POWERED_ROLLING_RESISTANCE_SCALE = 0.25D;
    private static final double RIGID_BRAKE_FRICTION = 0.75D;
    private static final double RIGID_MAX_FRICTION_MULTIPLIER = 1.25D;
    private static final double RIGID_DRIVE_SCALE = 1.75D;
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
    private static final String SECOND_WHEEL_TAG = "SecondWheel";

    private static final String STEERING_INPUT_ANGLE_STEP_TAG =
            "SteeringInputAngleStep";

    private static final String LEGACY_NEGATIVE_STEERING_INPUT_ANGLE_STEP_TAG =
            "NegativeSteeringInputAngleStep";

    private static final String LEGACY_POSITIVE_STEERING_INPUT_ANGLE_STEP_TAG =
            "PositiveSteeringInputAngleStep";

    private static final Collection<DoubleRigidSteeringWheelMountBlockEntity> PENDING_FORCE_APPLICATIONS =
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

    private final DoubleRigidSteeringWheelMountInventory inventory;
    private final RigidMountTuningValues tuning =
            new RigidMountTuningValues();
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

    private double secondWheelAngle;
    private double previousSecondWheelAngle;
    private double secondAngularVelocity;
    private double secondContactFriction = 1.0D;
    private boolean secondWheelOffGround;

    private List<SteeringInputAngleBehaviour> steeringInputAngleBehaviours;
    private final Vector3d pendingForcePosition = new Vector3d();
    private final Vector3d pendingImpulse = new Vector3d();
    private final ForceTotal accumulatedForces = new ForceTotal();

    public DoubleRigidSteeringWheelMountBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inventory = new DoubleRigidSteeringWheelMountInventory(this);
    }

    public static synchronized void registerPhysicsCallback() {
        if (physicsCallbackRegistered) {
            return;
        }
        RigidContactSolver.registerPhysicsCallback();
        SableEventPlatform.INSTANCE.onPhysicsTick(DoubleRigidSteeringWheelMountBlockEntity::flushPendingForces);
        physicsCallbackRegistered = true;
    }

    private static void flushPendingForces(
            SubLevelPhysicsSystem physicsSystem,
            double timeStep
    ) {
        for (DoubleRigidSteeringWheelMountBlockEntity mount :
                PENDING_FORCE_APPLICATIONS) {

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
            STEERING_INPUT_ANGLE_TYPE =
            new BehaviourType<>();

    @Override
    public void addBehaviours(
            List<BlockEntityBehaviour> behaviours
    ) {
        super.addBehaviours(behaviours);

        steeringInputAngleBehaviours =
                new ArrayList<>();

        for (Direction side : getSteeringInputAngleSides()) {
            addSteeringInputAngleBehaviour(
                    behaviours,
                    side,
                    STEERING_INPUT_ANGLE_TYPE
            );
        }
    }

    protected List<Direction> getSteeringInputAngleSides() {
        return List.of(
                DoubleRigidSteeringWheelMountBlock.getSteeringShaftDirection(
                        getBlockState()
                )
        );
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
                                        DoubleRigidSteeringWheelMountBlock.HORIZONTAL_FACING
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

        private final DoubleRigidSteeringWheelMountBlockEntity mount;
        private final Direction initialBlockFacing;
        private final Direction initialSide;

        private SteeringInputAngleValueBoxTransform(
                DoubleRigidSteeringWheelMountBlockEntity mount,
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
                    DoubleRigidSteeringWheelMountBlock.HORIZONTAL_FACING
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
            if (!state.hasProperty(
                    DoubleRigidSteeringWheelMountBlock.HORIZONTAL_FACING
            )) {
                return initialSide;
            }
            if (!initialBlockFacing.getAxis().isHorizontal()
                    || !initialSide.getAxis().isHorizontal()) {
                return initialSide;
            }

            Direction currentFacing = state.getValue(
                    DoubleRigidSteeringWheelMountBlock.HORIZONTAL_FACING
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
            if (!state.hasProperty(
                    DoubleRigidSteeringWheelMountBlock.HORIZONTAL_FACING
            )) {
                return false;
            }

            return direction ==
                    DoubleRigidSteeringWheelMountBlock.getSteeringShaftDirection(
                            state
                    );
        }

        @Override
        public float getScale() {
            return 0.5F;
        }
    }

    private static final class SteeringInputAngleBehaviour
            extends ScrollValueBehaviour {

        private final DoubleRigidSteeringWheelMountBlockEntity mount;
        private final BehaviourType<SteeringInputAngleBehaviour> behaviourType;

        private SteeringInputAngleBehaviour(
                Component label,
                DoubleRigidSteeringWheelMountBlockEntity mount,
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

    @Override
    public void sable$physicsTick(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            double timeStep
    ) {
        Direction facing =
                getBlockState().getValue(
                        DoubleRigidSteeringWheelMountBlock.HORIZONTAL_FACING
                );

        RigidContactState firstState =
                applyRigidWheelPhysics(
                        subLevel,
                        timeStep,
                        getWheelData(0),
                        facing,
                        wheelOffGround,
                        contactFriction
                );

        wheelOffGround = firstState.offGround;
        contactFriction = firstState.contactFriction;

        RigidContactState secondState =
                applyRigidWheelPhysics(
                        subLevel,
                        timeStep,
                        getWheelData(1),
                        facing.getOpposite(),
                        secondWheelOffGround,
                        secondContactFriction
                );

        secondWheelOffGround = secondState.offGround;
        secondContactFriction = secondState.contactFriction;
    }

    private RigidContactState applyRigidWheelPhysics(
            ServerSubLevel subLevel,
            double timeStep,
            @Nullable TireLike wheel,
            Direction facing,
            boolean currentOffGround,
            double currentContactFriction
    ) {
        RigidContactState wheelState =
                new RigidContactState(
                        currentOffGround,
                        currentContactFriction
                );

        if (wheel == null || timeStep <= 0.0D) {
            wheelState.offGround = true;
            return wheelState;
        }
        BlockPos mountPos = getBlockPos();
        Vec3 forcePoint = mechanicalDrive$getWheelCenter(facing);

        pendingForcePosition.set(
                forcePoint.x,
                forcePoint.y,
                forcePoint.z
        );

        Pose3d vehiclePose = subLevel.logicalPose();

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

        double groundDistance = contact.distance();

        double targetGroundDistance =
                wheel.radius()
                        + RIGID_CONTACT_SKIN;

        double contactTolerance =
                (currentOffGround
                        ? RIGID_CONTACT_TOLERANCE
                        : RIGID_CONTACT_RELEASE_TOLERANCE)
                        * tuning.get(BUMP_CLEARANCE);

        if (groundDistance
                > targetGroundDistance
                + contactTolerance) {

            wheelState.offGround = true;
            return wheelState;
        }

        wheelState.offGround = false;

        Vector3d worldVelocity =
                Sable.HELPER.getVelocity(
                        level,
                        JOMLConversion.toJOML(
                                forcePoint
                        )
                );

        Vector3d localVelocity =
                vehiclePose.transformNormalInverse(
                        worldVelocity
                );

        Vec3i hitNormal = contact.normal().getNormal();

        Vector3d localNormal =
                new Vector3d(
                        hitNormal.getX(),
                        hitNormal.getY(),
                        hitNormal.getZ()
                );

        if (contact.hitSubLevel() != null) {
            contact.hitSubLevel()
                    .logicalPose()
                    .transformNormal(
                            localNormal
                    );
        }

        vehiclePose.transformNormalInverse(
                localNormal
        );

        if (localNormal.lengthSquared() < 1.0E-8D) {
            return wheelState;
        }

        localNormal.normalize();

        double inverseNormalMass =
                subLevel.getMassTracker()
                        .getInverseNormalMass(
                                pendingForcePosition,
                                localNormal
                        );

        if (!Double.isFinite(inverseNormalMass)
                || inverseNormalMass <= 1.0E-8D) {
            return wheelState;
        }

        double positionError =
                targetGroundDistance
                        - groundDistance;

        double correctionSpeed =
                Mth.clamp(
                        Math.max(
                                positionError
                                        - RIGID_POSITION_SLOP,
                                0.0D
                        )
                                * RIGID_POSITION_CORRECTION
                                * tuning.get(BUMP_FORCE)
                                / timeStep,
                        0.0D,
                        RIGID_MAX_CORRECTION_SPEED
                );

        double normalSpeed =
                localVelocity.dot(
                        localNormal
                );

        Vector3d localGravity =
                DimensionPhysicsData.getGravity(level);

        vehiclePose.transformNormalInverse(
                localGravity
        );

        double contactWeight =
                Mth.clamp(
                        (
                                positionError
                                        + RIGID_CONTACT_TOLERANCE
                                        * tuning.get(BUMP_CLEARANCE)
                        ) / (
                                RIGID_CONTACT_TOLERANCE
                                        * tuning.get(BUMP_CLEARANCE)
                        ),
                        0.0D,
                        1.0D
                );

        double targetNormalSpeed =
                correctionSpeed
                        * tuning.get(MAX_IMPULSE)
                        + Math.max(
                        -localGravity.dot(localNormal)
                                * timeStep,
                        0.0D
                ) * contactWeight;

        wheelState.contactFriction =
                contact.hitBlock() == null
                        ? 1.0D
                        : adjustedFriction(
                        PhysicsBlockPropertyHelper.getFriction(
                                level.getBlockState(
                                        contact.hitBlock()
                                )
                        )
                );

        double usableSurfaceFriction =
                Math.min(
                        wheelState.contactFriction,
                        1.0D
                );

        double brake =
                level.getSignal(
                        mountPos.above(),
                        Direction.UP
                ) / 15.0D;

        Vector3d lateralDirection =
                new Vector3d(
                        lateralAxis
                );

        Vector3d rollingDirection =
                new Vector3d(
                        rollingAxis
                );

        lateralDirection.fma(
                -lateralDirection.dot(localNormal),
                localNormal
        );
        rollingDirection.fma(
                -rollingDirection.dot(localNormal),
                localNormal
        );

        if (lateralDirection.lengthSquared() < 1.0E-8D) {
            lateralDirection.set(rollingDirection).cross(localNormal);
        }

        lateralDirection.normalize();
        rollingDirection.fma(
                -rollingDirection.dot(lateralDirection),
                lateralDirection
        );
        if (rollingDirection.lengthSquared() < 1.0E-8D) {
            rollingDirection.set(localNormal).cross(lateralDirection);
        }
        rollingDirection.normalize();

        double inverseRollingMass =
                subLevel.getMassTracker()
                        .getInverseNormalMass(
                                pendingForcePosition,
                                rollingDirection
                        );

        float kineticSpeed =
                facing.getAxis() == Axis.X
                        ? getSpeed()
                        : -getSpeed();

        PendingRigidContact pendingContact =
                new PendingRigidContact(
                        this,
                        subLevel,
                        new Vector3d(pendingForcePosition),
                        new Vector3d(localNormal),
                        lateralDirection,
                        rollingDirection,
                        normalSpeed * tuning.get(DAMPING),
                        targetNormalSpeed,
                        inverseRollingMass,
                        usableSurfaceFriction,
                        brake,
                        kineticSpeed,
                        timeStep
                );
        RigidContactSolver.submit(
                level,
                subLevel,
                pendingContact.position,
                pendingContact.normal,
                pendingContact.normalSpeed,
                pendingContact.targetNormalSpeed,
                0.0D,
                pendingContact.lateralDirection,
                RIGID_LATERAL_FRICTION * Math.min(
                        tuning.get(GRIP),
                        1.0D
                ),
                pendingContact.usableSurfaceFriction
                        * RIGID_MAX_FRICTION_MULTIPLIER
                        * tuning.get(GRIP),
                (solvedImpulse, solvedLateralImpulse,
                 postConstraintLocalVelocity) -> {
                    if (pendingContact.mount.isRemoved()) {
                        return;
                    }

                    pendingContact.normalImpulse =
                            solvedImpulse;
                    pendingContact.lateralImpulse =
                            solvedLateralImpulse;

                    pendingContact.postConstraintLocalVelocity.set(
                            postConstraintLocalVelocity
                    );

                    pendingContact.mount.applySolvedContact(
                            pendingContact
                    );
                }
        );

        return wheelState;
    }

    private void applySolvedContact(PendingRigidContact contact) {
        double appliedNormalImpulse =
                contact.normalImpulse;

        if (!Double.isFinite(appliedNormalImpulse)
                || appliedNormalImpulse <= 0.0D) {
            return;
        }

        double currentRollingSpeed =
                contact.postConstraintLocalVelocity.dot(
                        contact.rollingDirection
                );

        double maxFrictionImpulse =
                appliedNormalImpulse
                        * contact.usableSurfaceFriction
                        * RIGID_MAX_FRICTION_MULTIPLIER
                        * tuning.get(GRIP);

        boolean activelyDriven =
                Math.abs(contact.kineticSpeed)
                        > STEERING_EPSILON
                        && contact.brake < 1.0D;

        pendingImpulse
                .set(contact.normal)
                .mul(appliedNormalImpulse);

        pendingImpulse.fma(
                contact.lateralImpulse,
                contact.lateralDirection
        );

        if (Double.isFinite(contact.inverseRollingMass)
                && contact.inverseRollingMass > 1.0E-8D) {

            double rollingResistanceScale =
                    activelyDriven
                            ? RIGID_POWERED_ROLLING_RESISTANCE_SCALE
                            : 1.0D;

            double rollingResistanceImpulse =
                    -currentRollingSpeed
                            / contact.inverseRollingMass
                            * RIGID_ROLLING_RESISTANCE
                            * rollingResistanceScale
                            * contact.usableSurfaceFriction;

            if (contact.brake > 0.0D) {
                rollingResistanceImpulse +=
                        -currentRollingSpeed
                                / contact.inverseRollingMass
                                * contact.brake
                                * RIGID_BRAKE_FRICTION
                                * contact.usableSurfaceFriction;
            }

            rollingResistanceImpulse =
                    Mth.clamp(
                            rollingResistanceImpulse,
                            -maxFrictionImpulse,
                            maxFrictionImpulse
                    );

            pendingImpulse.fma(
                    rollingResistanceImpulse,
                    contact.rollingDirection
            );
        }

        if (activelyDriven) {
            double driveImpulse =
                    contact.kineticSpeed
                            * (1.0D - contact.brake)
                            * contact.usableSurfaceFriction
                            * RIGID_DRIVE_SCALE
                            * tuning.get(DRIVE)
                            * contact.timeStep;

            driveImpulse = Mth.clamp(
                    driveImpulse,
                    -maxFrictionImpulse,
                    maxFrictionImpulse
            );
            pendingImpulse.fma(
                    driveImpulse,
                    contact.rollingDirection
            );
        }

        accumulatedForces.applyImpulseAtPoint(
                contact.subLevel,
                contact.position,
                pendingImpulse
        );
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

            steeringYaw =
                    wheelYawFromShaftAngle(
                            steeringAngle
                    );

            return;
        }

        previousSteeringYaw = steeringYaw;

        steeringYaw =
                Mth.lerp(
                        0.4D,
                        steeringYaw,
                        wheelYawFromShaftAngle(
                                steeringAngle
                        )
                );

        tickFirstWheelClient();
        tickSecondWheelClient();
    }

    private void tickFirstWheelClient() {
        TireLike wheel = getWheelData(0);

        if (wheel == null) {
            if (ShaftMarkerItem.isMarker(getWheel(0))) {
                Direction facing = getBlockState().getValue(
                        DoubleRigidSteeringWheelMountBlock.HORIZONTAL_FACING
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
                return;
            }

            previousWheelAngle = 0.0D;
            wheelAngle = 0.0D;
            angularVelocity = 0.0D;
            wheelOffGround = true;
            return;
        }

        updateClientGroundContact(
                wheel.radius()
        );

        Direction facing =
                getBlockState().getValue(
                        DoubleRigidSteeringWheelMountBlock.HORIZONTAL_FACING
                );

        float speed =
                facing.getAxis() == Axis.X
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
                        angularVelocity,
                        poweredAngularVelocity
                );

        SubLevel subLevel =
                Sable.HELPER.getContaining(
                        this
                );

        previousWheelAngle =
                wheelAngle;

        if (subLevel == null
                || wheelOffGround) {
            angularVelocity =
                    attemptedAngularVelocity;

            wheelAngle +=
                    angularVelocity;

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
                        / wheel.radius();

        if (contactFriction < 1.0D) {
            rollingAngularDelta =
                    Mth.lerp(
                            contactFriction,
                            attemptedAngularVelocity,
                            rollingAngularDelta
                    );
        }

        wheelAngle +=
                rollingAngularDelta;

        angularVelocity =
                rollingAngularDelta;
    }

    private void tickSecondWheelClient() {
        TireLike wheel = getWheelData(1);

        if (wheel == null) {
            if (ShaftMarkerItem.isMarker(getWheel(1))) {
                Direction facing = getBlockState().getValue(
                        DoubleRigidSteeringWheelMountBlock.HORIZONTAL_FACING
                ).getOpposite();
                previousSecondWheelAngle = secondWheelAngle;
                secondAngularVelocity = ShaftMarkerMountHelper
                        .approachDrivenAngularVelocity(
                                this,
                                facing,
                                secondAngularVelocity
                        );
                secondWheelAngle += secondAngularVelocity;
                secondWheelOffGround = true;
                return;
            }

            previousSecondWheelAngle = 0.0D;
            secondWheelAngle = 0.0D;
            secondAngularVelocity = 0.0D;
            secondWheelOffGround = true;
            return;
        }

        updateSecondClientGroundContact(
                wheel.radius()
        );

        Direction facing =
                getBlockState().getValue(
                        DoubleRigidSteeringWheelMountBlock.HORIZONTAL_FACING
                ).getOpposite();

        float speed =
                facing.getAxis() == Axis.X
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
                        / wheel.radius();

        if (secondContactFriction < 1.0D) {
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
        return DoubleRigidSteeringWheelMountBlock.getSteeringShaftDirection(getBlockState());
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
        return direction == getSteeringShaftDirection();
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
        Direction steeringSide = getSteeringShaftDirection();
        return Math.abs(getSteeringSpeedFromSide(steeringSide)) > STEERING_EPSILON
                ? steeringSide
                : null;
    }

    private float getSteeringSpeedFromSide(Direction side) {
        return getSteeringSpeedFromSide(this, side, null);
    }

    public boolean isSteeringPassThroughEnabled() {
        return false;
    }

    private static float getSteeringSpeedFromSide(
            DoubleRigidSteeringWheelMountBlockEntity mount,
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
                    instanceof DoubleRigidSteeringWheelMountBlockEntity mount)) {
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

    private void updateClientGroundContact(
            float wheelRadius
    ) {
        Direction facing =
                getBlockState().getValue(
                        DoubleRigidSteeringWheelMountBlock.HORIZONTAL_FACING
                );

        RigidContactState state =
                getClientGroundContact(
                        wheelRadius,
                        facing
                );

        wheelOffGround = state.offGround;
        contactFriction = state.contactFriction;
    }

    private void updateSecondClientGroundContact(
            float wheelRadius
    ) {
        Direction facing =
                getBlockState().getValue(
                        DoubleRigidSteeringWheelMountBlock.HORIZONTAL_FACING
                ).getOpposite();

        RigidContactState state =
                getClientGroundContact(
                        wheelRadius,
                        facing
                );

        secondWheelOffGround = state.offGround;
        secondContactFriction = state.contactFriction;
    }

    private RigidContactState getClientGroundContact(
            float wheelRadius,
            Direction facing
    ) {
        SubLevel containing =
                Sable.HELPER.getContaining(
                        this
                );

        if (containing == null) {
            return new RigidContactState(
                    true,
                    1.0D
            );
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

        boolean offGround = contact.distance()
                > wheelRadius
                + RIGID_CONTACT_SKIN
                + RIGID_CONTACT_TOLERANCE;

        double friction =
                contact.hitBlock() == null
                        ? 1.0D
                        : adjustedFriction(
                        PhysicsBlockPropertyHelper.getFriction(
                                level.getBlockState(
                                        contact.hitBlock()
                                )
                                )
                        );

        return new RigidContactState(
                offGround,
                friction
        );
    }

    protected Vec3 mechanicalDrive$getWheelCenter(Direction wheelSide) {
        return WheelMountOffsets.apply(
                this,
                getBlockPos().relative(wheelSide).getCenter(),
                wheelSide
        );
    }
    private TerrainContact scanTerrain(
            Vector3dc rollingAxis,
            Pose3dc vehiclePose,
            Direction facing,
            float wheelRadius
    ) {
        Vec3 wheelCenter = mechanicalDrive$getWheelCenter(facing);

        Vector3d localDown =
                new Vector3d(
                        0.0D,
                        -1.0D,
                        0.0D
                );

        Vector3d localUp =
                new Vector3d(
                        0.0D,
                        1.0D,
                        0.0D
                );

        Vec3 rayDirection =
                JOMLConversion.toMojang(
                        localDown
                );

        double closestDistance = MAX_GROUND_SCAN;
        Direction closestNormal = Direction.UP;
        SubLevel closestSubLevel = null;
        BlockPos closestBlock = null;

        for (int offset = -1; offset <= 1; offset++) {
            Vec3 rayStart = wheelCenter.add(JOMLConversion.toMojang(rollingAxis).scale(offset));
            ClipContext context = new ClipContext(
                    rayStart,
                    rayStart.add(
                            rayDirection.scale(
                                    MAX_GROUND_SCAN
                            )
                    ),
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
            Vector3d wheelToHit =
                    new Vector3d(
                            localHit.x - wheelCenter.x,
                            localHit.y - wheelCenter.y,
                            localHit.z - wheelCenter.z
                    );

            double rayDistance =
                    wheelToHit.dot(localDown);

            if (rayStart.distanceTo(localHit) < 0.05D
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

            double suspensionProjection =
                    normal.dot(localUp);
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

    private static Vec3i axisUnit(Axis axis) {
        return Direction.get(AxisDirection.POSITIVE, axis).getNormal();
    }

    private static Vec3i perpendicularHorizontal(Vec3i axis) {
        return new Vec3i(axis.getZ(), 0, axis.getX());
    }

    private Vector3dc rotatedWheelAxis(Vec3i axis) {
        return new Vector3d(axis.getX(), axis.getY(), axis.getZ()).rotateY(steeringYaw);
    }

    private static double adjustedFriction(double friction) {
        return friction < 1.0D ? 0.1D + 0.9D * friction : friction;
    }

    public DoubleRigidSteeringWheelMountInventory getInventory() {
        return inventory;
    }

    public ItemStack getWheel() {
        return getWheel(0);
    }

    public ItemStack getWheel(int slot) {
        return slot >= 0 && slot < inventory.getSlots()
                ? inventory.getStackInSlot(slot)
                : ItemStack.EMPTY;
    }

    public ItemStack getWheel(Direction side) {
        return getWheel(getSlotForSide(side));
    }

    public int getSlotForSide(Direction side) {
        Direction facing = getBlockState().getValue(
                DoubleRigidSteeringWheelMountBlock.HORIZONTAL_FACING
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
    private TireLike getWheelData(int slot) {
        return getWheel(slot).get(OffroadDataComponents.TIRE);
    }

    public void onWheelChanged() {
        setChanged();
        invalidateRenderBoundingBox();
        sendData();
    }

    public double getLerpedYaw(float partialTick) {
        return Mth.lerp(partialTick, previousSteeringYaw, steeringYaw);
    }

    public float getLerpedWheelAngle(float partialTick) {
        return (float) Mth.lerp(partialTick, previousWheelAngle, wheelAngle);
    }

    public float getLerpedSecondWheelAngle(float partialTick) {
        return (float) Mth.lerp(
                partialTick,
                previousSecondWheelAngle,
                secondWheelAngle
        );
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = 32.0F;
        return lastStressApplied;
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state) {
        List<StackRequirement> requirements = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack wheel = getWheel(slot);
            if (!wheel.isEmpty()) {
                requirements.add(new StackRequirement(wheel, ItemUseType.CONSUME));
            }
        }
        return requirements.isEmpty()
                ? super.getRequiredItems(state)
                : new ItemRequirement(requirements);
    }

    @Override
    public String getClipboardKey() {
        return "Double Rigid Steering Wheel Mount";
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
        tuning.write(tag);

        tag.put(
                "Wheel",
                getWheel().saveOptional(registries)
        );

        tag.put(
                SECOND_WHEEL_TAG,
                getWheel(1).saveOptional(registries)
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
        tuning.read(tag);

        inventory.setWithoutNotification(
                ItemStack.parseOptional(
                        registries,
                        tag.getCompound("Wheel")
                )
        );

        inventory.setWithoutNotification(
                1,
                ItemStack.parseOptional(
                        registries,
                        tag.getCompound(SECOND_WHEEL_TAG)
                )
        );

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
    public double mechanicalDrive$adjustSuspensionTuning(
            String tuningKey,
            int steps
    ) {
        double value = tuning.adjust(tuningKey, steps);
        mechanicalDrive$syncTuning();
        return value;
    }

    @Override
    public double mechanicalDrive$getSuspensionTuning(String tuningKey) {
        return tuning.get(tuningKey);
    }

    @Override
    public void mechanicalDrive$resetSuspensionTuning() {
        tuning.reset();
        mechanicalDrive$syncTuning();
    }

    @Override
    public boolean mechanicalDrive$supportsSuspensionTuning(
            String tuningKey
    ) {
        return RigidMountTuningValues.supports(tuningKey);
    }


    private void mechanicalDrive$syncTuning() {
        setChanged();
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    @Override
    public void clearContent() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        inventory.setStackInSlot(1, ItemStack.EMPTY);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        AABB bounds = new AABB(getBlockPos())
                .inflate(WheelMountOffsets.maximumAbsolute(this));
        float maxRadius = 0.0F;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            TireLike wheel = getWheelData(slot);
            if (wheel != null) {
                maxRadius = Math.max(maxRadius, wheel.radius());
            }
        }
        return maxRadius == 0.0F ? bounds : bounds.inflate(maxRadius + 1.0F);
    }

    private static final class PendingRigidContact {
        private final DoubleRigidSteeringWheelMountBlockEntity mount;
        private final ServerSubLevel subLevel;
        private final Vector3d position;
        private final Vector3d normal;
        private final Vector3d lateralDirection;
        private final Vector3d rollingDirection;
        private final double normalSpeed;
        private final double targetNormalSpeed;
        private final double inverseRollingMass;
        private final double usableSurfaceFriction;
        private final double brake;
        private final float kineticSpeed;
        private final double timeStep;

        private final Vector3d postConstraintLocalVelocity =
                new Vector3d();

        private double normalImpulse;
        private double lateralImpulse;

        private PendingRigidContact(
                DoubleRigidSteeringWheelMountBlockEntity mount,
                ServerSubLevel subLevel,
                Vector3d position,
                Vector3d normal,
                Vector3d lateralDirection,
                Vector3d rollingDirection,
                double normalSpeed,
                double targetNormalSpeed,
                double inverseRollingMass,
                double usableSurfaceFriction,
                double brake,
                float kineticSpeed,
                double timeStep
        ) {
            this.mount = mount;
            this.subLevel = subLevel;
            this.position = position;
            this.normal = normal;
            this.lateralDirection = lateralDirection;
            this.rollingDirection = rollingDirection;
            this.normalSpeed = normalSpeed;
            this.targetNormalSpeed = targetNormalSpeed;
            this.inverseRollingMass = inverseRollingMass;
            this.usableSurfaceFriction = usableSurfaceFriction;
            this.brake = brake;
            this.kineticSpeed = kineticSpeed;
            this.timeStep = timeStep;
        }
    }

    private static final class RigidContactState {
        private boolean offGround;
        private double contactFriction;

        private RigidContactState(boolean offGround, double contactFriction) {
            this.offGround = offGround;
            this.contactFriction = contactFriction;
        }
    }

    private record TerrainContact(
            double distance,
            @NotNull Direction normal,
            @Nullable SubLevel hitSubLevel,
            @Nullable BlockPos hitBlock
    ) {
    }
}
