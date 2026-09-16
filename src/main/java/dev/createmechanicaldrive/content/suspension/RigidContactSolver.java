package dev.createmechanicaldrive.content.suspension;

import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import org.joml.Vector3d;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Solves every rigid wheel contact on one body as a single constraint system. */
public final class RigidContactSolver {

    private static final int SOLVER_ITERATIONS = 48;
    private static final double SOLVER_EPSILON = 1.0E-7D;
    private static final double LATERAL_HOLD_SPEED = 0.08D;
    private static final double LATERAL_HOLD_RELAXATION = 0.25D;
    private static final List<Contact> PENDING_CONTACTS = new ArrayList<>();
    private static boolean physicsCallbackRegistered;

    private RigidContactSolver() {
    }

    @FunctionalInterface
    public interface ContactResultConsumer {
        void accept(
                double normalImpulse,
                double lateralImpulse,
                Vector3d postConstraintLocalVelocity
        );
    }

    public static synchronized void registerPhysicsCallback() {
        if (physicsCallbackRegistered) {
            return;
        }
        SableEventPlatform.INSTANCE.onPhysicsTick(RigidContactSolver::flush);
        physicsCallbackRegistered = true;
    }

    public static void submit(
            Level level,
            ServerSubLevel subLevel,
            Vector3d position,
            Vector3d normal,
            double normalSpeed,
            double targetNormalSpeed,
            double initialImpulse,
            Vector3d lateralDirection,
            double lateralResponse,
            double lateralImpulseRatio,
            ContactResultConsumer result
    ) {
        PENDING_CONTACTS.add(new Contact(
                level,
                subLevel,
                position,
                normal,
                normalSpeed,
                targetNormalSpeed,
                Math.max(initialImpulse, 0.0D),
                lateralDirection,
                lateralResponse,
                lateralImpulseRatio,
                result
        ));
    }

    private static void flush(
            SubLevelPhysicsSystem physicsSystem,
            double timeStep
    ) {
        Map<ServerSubLevel, List<Contact>>
                contactsByBody =
                new IdentityHashMap<>();

        for (Contact contact :
                PENDING_CONTACTS) {

            contactsByBody.computeIfAbsent(
                    contact.subLevel,
                    ignored -> new ArrayList<>()
            ).add(contact);
        }

        for (List<Contact> contacts :
                contactsByBody.values()) {

            MassData massData =
                    contacts.getFirst()
                            .subLevel
                            .getMassTracker();

            refreshNormalSpeeds(contacts);
            solveCoupled(contacts, massData);

            for (Contact measuredContact : contacts) {
                Vector3d postConstraintLocalVelocity =
                        new Vector3d(
                                measuredContact.localVelocity
                        );

                for (Contact impulseContact : contacts) {
                    if (impulseContact.normalImpulse
                            > SOLVER_EPSILON) {
                        postConstraintLocalVelocity.add(
                                pointVelocityResponse(
                                        massData,
                                        measuredContact.position,
                                        impulseContact.position,
                                        new Vector3d(
                                                impulseContact.normal
                                        ).mul(
                                                impulseContact.normalImpulse
                                        ),
                                        new Vector3d()
                                )
                        );
                    }

                    if (Math.abs(impulseContact.lateralImpulse)
                            > SOLVER_EPSILON) {
                        postConstraintLocalVelocity.add(
                                pointVelocityResponse(
                                        massData,
                                        measuredContact.position,
                                        impulseContact.position,
                                        new Vector3d(
                                                impulseContact.lateralDirection
                                        ).mul(
                                                impulseContact.lateralImpulse
                                        ),
                                        new Vector3d()
                                )
                        );
                    }
                }

                measuredContact.result.accept(
                        measuredContact.normalImpulse,
                        measuredContact.lateralImpulse,
                        postConstraintLocalVelocity
                );
            }
        }

        PENDING_CONTACTS.clear();
    }

    private static void solveCoupled(
            List<Contact> contacts,
            MassData massData
    ) {
        int count = contacts.size();
        if (count == 0) {
            return;
        }

        double[][] normalFromNormal = new double[count][count];
        double[][] normalFromLateral = new double[count][count];
        double[][] lateralFromNormal = new double[count][count];
        double[][] lateralFromLateral = new double[count][count];
        double[] targetLateralSpeeds = new double[count];

        for (int i = 0; i < count; i++) {
            Contact measured = contacts.get(i);
            double lateralSpeed = measured.localVelocity.dot(
                    measured.lateralDirection
            );
            double responseScale = Math.abs(lateralSpeed)
                    <= LATERAL_HOLD_SPEED
                    ? LATERAL_HOLD_RELAXATION
                    : measured.lateralResponse;
            targetLateralSpeeds[i] =
                    lateralSpeed * (1.0D - responseScale);

            for (int j = 0; j < count; j++) {
                Contact impulse = contacts.get(j);
                normalFromNormal[i][j] = directionalResponse(
                        massData,
                        measured.position,
                        measured.normal,
                        impulse.position,
                        impulse.normal
                );
                normalFromLateral[i][j] = directionalResponse(
                        massData,
                        measured.position,
                        measured.normal,
                        impulse.position,
                        impulse.lateralDirection
                );
                lateralFromNormal[i][j] = directionalResponse(
                        massData,
                        measured.position,
                        measured.lateralDirection,
                        impulse.position,
                        impulse.normal
                );
                lateralFromLateral[i][j] = directionalResponse(
                        massData,
                        measured.position,
                        measured.lateralDirection,
                        impulse.position,
                        impulse.lateralDirection
                );
            }
        }

        for (int iteration = 0; iteration < SOLVER_ITERATIONS; iteration++) {
            solveCoupledSweep(
                    contacts,
                    normalFromNormal,
                    normalFromLateral,
                    lateralFromNormal,
                    lateralFromLateral,
                    targetLateralSpeeds,
                    0,
                    count,
                    1
            );
            double largestResidual = solveCoupledSweep(
                    contacts,
                    normalFromNormal,
                    normalFromLateral,
                    lateralFromNormal,
                    lateralFromLateral,
                    targetLateralSpeeds,
                    count - 1,
                    -1,
                    -1
            );
            if (largestResidual <= SOLVER_EPSILON) {
                break;
            }
        }
    }

    private static double solveCoupledSweep(
            List<Contact> contacts,
            double[][] normalFromNormal,
            double[][] normalFromLateral,
            double[][] lateralFromNormal,
            double[][] lateralFromLateral,
            double[] targetLateralSpeeds,
            int start,
            int end,
            int step
    ) {
        double largestResidual = 0.0D;
        for (int i = start; i != end; i += step) {
            Contact contact = contacts.get(i);
            double normalDiagonal = normalFromNormal[i][i];
            if (Double.isFinite(normalDiagonal)
                    && normalDiagonal > 1.0E-8D) {
                double appliedNormalSpeed = 0.0D;
                for (int j = 0; j < contacts.size(); j++) {
                    Contact impulse = contacts.get(j);
                    appliedNormalSpeed += normalFromNormal[i][j]
                            * impulse.normalImpulse;
                    appliedNormalSpeed += normalFromLateral[i][j]
                            * impulse.lateralImpulse;
                }

                double normalResidual = contact.targetNormalSpeed
                        - contact.normalSpeed
                        - appliedNormalSpeed;
                largestResidual = Math.max(
                        largestResidual,
                        Math.abs(normalResidual)
                );
                contact.normalImpulse = Math.max(
                        0.0D,
                        contact.normalImpulse
                                + normalResidual / normalDiagonal
                );
            }

            double impulseLimit = Math.max(
                    0.0D,
                    contact.normalImpulse * contact.lateralImpulseRatio
            );
            contact.lateralImpulse = Math.max(
                    -impulseLimit,
                    Math.min(impulseLimit, contact.lateralImpulse)
            );

            double lateralDiagonal = lateralFromLateral[i][i];
            if (!Double.isFinite(lateralDiagonal)
                    || lateralDiagonal <= 1.0E-8D) {
                continue;
            }

            double appliedLateralSpeed = 0.0D;
            for (int j = 0; j < contacts.size(); j++) {
                Contact impulse = contacts.get(j);
                appliedLateralSpeed += lateralFromNormal[i][j]
                        * impulse.normalImpulse;
                appliedLateralSpeed += lateralFromLateral[i][j]
                        * impulse.lateralImpulse;
            }

            double residual = targetLateralSpeeds[i]
                    - contact.localVelocity.dot(
                    contact.lateralDirection
            )
                    - appliedLateralSpeed;
            largestResidual = Math.max(
                    largestResidual,
                    Math.abs(residual)
            );
            contact.lateralImpulse = Math.max(
                    -impulseLimit,
                    Math.min(
                            impulseLimit,
                            contact.lateralImpulse
                                    + residual / lateralDiagonal
                    )
            );
        }
        return largestResidual;
    }

    private static void refreshNormalSpeeds(
            List<Contact> contacts
    ) {
        if (contacts.isEmpty()) {
            return;
        }

        ServerSubLevel subLevel =
                contacts.getFirst().subLevel;

        Pose3d vehiclePose =
                subLevel.logicalPose();

        for (Contact contact : contacts) {
            Vector3d worldVelocity =
                    Sable.HELPER.getVelocity(
                            contact.level,
                            contact.position,
                            new Vector3d()
                    );

            Vector3d localVelocity =
                    vehiclePose.transformNormalInverse(
                            worldVelocity,
                            new Vector3d()
                    );

            if (!Double.isFinite(localVelocity.x)
                    || !Double.isFinite(localVelocity.y)
                    || !Double.isFinite(localVelocity.z)) {
                continue;
            }

            contact.localVelocity.set(
                    localVelocity
            );

            contact.normalSpeed =
                    localVelocity.dot(
                            contact.normal
                    );
        }
    }

    private static double directionalResponse(
            MassData massData,
            Vector3d measuredPosition,
            Vector3d measuredDirection,
            Vector3d impulsePosition,
            Vector3d impulseDirection
    ) {
        Vector3d impulseOffset = new Vector3d(impulsePosition)
                .sub(massData.getCenterOfMass());
        Vector3d measuredOffset = new Vector3d(measuredPosition)
                .sub(massData.getCenterOfMass());
        Vector3d angularVelocity = impulseOffset.cross(
                impulseDirection,
                new Vector3d()
        );
        massData.getInverseInertiaTensor().transform(
                angularVelocity,
                angularVelocity
        );
        Vector3d pointVelocity = angularVelocity.cross(
                measuredOffset,
                new Vector3d()
        ).fma(
                massData.getInverseMass(),
                impulseDirection
        );
        return measuredDirection.dot(pointVelocity);
    }

    private static Vector3d pointVelocityResponse(
            MassData massData,
            Vector3d measuredPosition,
            Vector3d impulsePosition,
            Vector3d impulse,
            Vector3d destination
    ) {

        Vector3d impulseOffset =
                new Vector3d(
                        impulsePosition
                ).sub(
                        massData.getCenterOfMass()
                );

        Vector3d measuredOffset =
                new Vector3d(
                        measuredPosition
                ).sub(
                        massData.getCenterOfMass()
                );

        Vector3d angularVelocityDelta =
                impulseOffset.cross(
                        impulse,
                        new Vector3d()
                );

        massData.getInverseInertiaTensor()
                .transform(
                        angularVelocityDelta,
                        angularVelocityDelta
                );

        destination.set(
                angularVelocityDelta
                        .cross(
                                measuredOffset,
                                new Vector3d()
                        )
        );

        destination.fma(
                massData.getInverseMass(),
                impulse
        );

        return destination;
    }

    private static final class Contact {
        private final Level level;
        private final ServerSubLevel subLevel;
        private final Vector3d position;
        private final Vector3d normal;
        private double normalSpeed;

        private final Vector3d localVelocity =
                new Vector3d();

        private final double targetNormalSpeed;
        private final Vector3d lateralDirection;
        private final double lateralResponse;
        private final double lateralImpulseRatio;
        private final ContactResultConsumer result;
        private double normalImpulse;
        private double lateralImpulse;
        private Contact(
                Level level,
                ServerSubLevel subLevel,
                Vector3d position,
                Vector3d normal,
                double normalSpeed,
                double targetNormalSpeed,
                double normalImpulse,
                Vector3d lateralDirection,
                double lateralResponse,
                double lateralImpulseRatio,
                ContactResultConsumer result
        ) {
            this.level = level;
            this.subLevel = subLevel;
            this.position = position;
            this.normal = normal;
            this.normalSpeed = normalSpeed;
            this.targetNormalSpeed = targetNormalSpeed;
            this.normalImpulse = normalImpulse;
            this.lateralDirection = new Vector3d(lateralDirection).normalize();
            this.lateralResponse = Math.max(
                    0.0D,
                    Math.min(1.0D, lateralResponse)
            );
            this.lateralImpulseRatio = Math.max(
                    0.0D,
                    lateralImpulseRatio
            );
            this.result = result;
        }
    }
}
