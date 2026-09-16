package dev.createmechanicaldrive.content.engine;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class EngineBlockEntity
        extends GeneratingKineticBlockEntity {

    private static final float GENERATED_RPM =
            64.0F;

    private static final float STRESS_CAPACITY =
            64.0F;

    private static final float STARTER_STRESS_IMPACT =
            8.0F;

    private static final int LAVA_CAPACITY_PER_ENGINE_MB =
            1000;

    private static final int LAVA_FUEL_TICKS_PER_MB =
            2;

    private static final String FUEL_TICKS_TAG =
            "FuelTicks";

    private static final String LAVA_AMOUNT_TAG =
            "LavaAmount";

    private static Consumer<EngineBlockEntity>
            clientEffectsHandler =
            engine -> {
            };

    private int fuelTicksRemaining;

    private int lavaAmountMb;

    private boolean running;

    private int rotationSign = 1;

    private static final int FUEL_BURN_RATE =
            2;

    private static final int MULTI_ENGINE_FUEL_MULTIPLIER =
            2;

    private static final int MAX_ENGINE_COUNT =
            3;

    private static final String RUNNING_TAG =
            "Running";

    private static final String ROTATION_SIGN_TAG =
            "RotationSign";

    private final IFluidHandler lavaFuelHandler =
            new IFluidHandler() {

                @Override
                public int getTanks() {
                    return 1;
                }

                @Override
                public FluidStack getFluidInTank(
                        int tank
                ) {
                    return FluidStack.EMPTY;
                }

                @Override
                public int getTankCapacity(
                        int tank
                ) {
                    List<EngineBlockEntity> group =
                            getEngineGroup();

                    int engineCount =
                            Math.min(
                                    group.size(),
                                    MAX_ENGINE_COUNT
                            );

                    return LAVA_CAPACITY_PER_ENGINE_MB
                            * Math.max(
                            1,
                            engineCount
                    );
                }

                @Override
                public boolean isFluidValid(
                        int tank,
                        FluidStack stack
                ) {
                    return stack.getFluid()
                            == Fluids.LAVA;
                }

                @Override
                public int fill(
                        FluidStack resource,
                        FluidAction action
                ) {
                    if (resource.isEmpty()
                            || resource.getFluid()
                            != Fluids.LAVA) {
                        return 0;
                    }

                    List<EngineBlockEntity> group =
                            getEngineGroup();

                    if (group.isEmpty()
                            || group.size()
                            > MAX_ENGINE_COUNT) {
                        return 0;
                    }

                    long totalFuel =
                            getGroupFuelTicks(
                                    group
                            );

                    long totalLavaMb =
                            totalFuel
                                    / LAVA_FUEL_TICKS_PER_MB;

                    long maximumLavaMb =
                            (long) LAVA_CAPACITY_PER_ENGINE_MB
                                    * group.size();

                    long freeLavaMb =
                            Math.max(
                                    0L,
                                    maximumLavaMb
                                            - totalLavaMb
                            );

                    int requestedMb =
                            resource.getAmount();

                    if (freeLavaMb < requestedMb) {
                        return 0;
                    }

                    int acceptedMb =
                            requestedMb;

                    if (acceptedMb <= 0) {
                        return 0;
                    }

                    if (action
                            == FluidAction.EXECUTE) {
                        long addedFuel =
                                (long) acceptedMb
                                        * LAVA_FUEL_TICKS_PER_MB;

                        distributeGroupFuel(
                                group,
                                totalFuel
                                        + addedFuel
                        );

                        distributeGroupLava(
                                group,
                                totalLavaMb
                                        + acceptedMb
                        );

                        syncGroup(
                                group
                        );
                    }

                    return acceptedMb;
                }

                @Override
                public FluidStack drain(
                        FluidStack resource,
                        FluidAction action
                ) {
                    return FluidStack.EMPTY;
                }

                @Override
                public FluidStack drain(
                        int maxDrain,
                        FluidAction action
                ) {
                    return FluidStack.EMPTY;
                }
            };

    public EngineBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    public static void registerClientEffectsHandler(
            Consumer<EngineBlockEntity> handler
    ) {
        clientEffectsHandler =
                handler;
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            if (running) {
                clientEffectsHandler.accept(this);
                spawnEngineParticles();
            }

            return;
        }

        List<EngineBlockEntity> group =
                getEngineGroup();

        if (group.isEmpty()
                || group.get(0) != this) {
            return;
        }

        boolean groupRunning =
                isGroupRunning(group);

        if (isGroupRedstonePowered(group)) {
            if (groupRunning) {
                for (EngineBlockEntity engine : group) {
                    engine.updateRunningState(
                            false
                    );
                }

                syncGroup(group);
            }

            return;
        }

        long totalFuel =
                getGroupFuelTicks(group);

        if (!groupRunning) {
            float startSpeed =
                    findGroupStartSpeed(group);

            if (totalFuel <= 0
                    || Math.abs(startSpeed) <= 0.001F) {
                return;
            }

            if (group.size() > MAX_ENGINE_COUNT) {
                List<EngineBlockEntity> validGroup =
                        new ArrayList<>(
                                group.subList(
                                        0,
                                        MAX_ENGINE_COUNT
                                )
                        );

                for (int i = MAX_ENGINE_COUNT;
                     i < group.size();
                     i++) {
                    EngineBlockEntity extraEngine =
                            group.get(i);

                    BlockPos extraPos =
                            extraEngine.getBlockPos();

                    Block.popResource(
                            level,
                            extraPos,
                            new ItemStack(
                                    CreateMechanicalDrive.ENGINE.get()
                            )
                    );

                    level.destroyBlock(
                            extraPos,
                            false
                    );
                }

                group =
                        validGroup;

                distributeGroupFuel(
                        group,
                        totalFuel
                );
            }

            int startRotationSign =
                    startSpeed < 0.0F
                            ? -1
                            : 1;

            for (EngineBlockEntity engine : group) {
                engine.rotationSign =
                        startRotationSign;

                engine.updateRunningState(
                        true
                );
            }

            groupRunning =
                    true;
        }

        if (!groupRunning) {
            return;
        }

        int activeEngines =
                group.size();

        long fuelConsumption =
                getFuelBurnRate(
                        activeEngines
                );

        totalFuel =
                getGroupFuelTicks(group);

        long remainingFuel =
                Math.max(
                        0L,
                        totalFuel
                                - fuelConsumption
                );

        distributeGroupFuel(
                group,
                remainingFuel
        );

        long totalLavaMb =
                getGroupLavaAmountMb(
                        group
                );

        if (totalLavaMb > 0L) {
            long consumedLavaMb =
                    (fuelConsumption
                            + LAVA_FUEL_TICKS_PER_MB
                            - 1L)
                            / LAVA_FUEL_TICKS_PER_MB;

            long remainingLavaMb =
                    Math.max(
                            0L,
                            totalLavaMb
                                    - consumedLavaMb
                    );

            distributeGroupLava(
                    group,
                    remainingLavaMb
            );
        }

        if (remainingFuel == 0L) {
            for (EngineBlockEntity engine : group) {
                engine.updateRunningState(
                        false
                );
            }

            return;
        }

        if (level.getGameTime() % 20L == 0L) {
            syncGroup(group);
        }
    }

    public List<EngineBlockEntity> getEngineGroup() {
        if (level == null) {
            return List.of(this);
        }

        Direction facing =
                getBlockState().getValue(
                        EngineBlock.FACING
                );

        EngineBlockEntity first =
                this;

        while (true) {
            EngineBlockEntity previous =
                    getConnectedEngine(
                            first,
                            facing.getOpposite()
                    );

            if (previous == null) {
                break;
            }

            first =
                    previous;
        }

        List<EngineBlockEntity> group =
                new ArrayList<>();

        EngineBlockEntity current =
                first;

        while (current != null) {
            group.add(
                    current
            );

            current =
                    getConnectedEngine(
                            current,
                            facing
                    );
        }

        return group;
    }

    private EngineBlockEntity getConnectedEngine(
            EngineBlockEntity origin,
            Direction direction
    ) {
        if (level == null) {
            return null;
        }

        BlockState originState =
                origin.getBlockState();

        if (!(originState.getBlock()
                instanceof EngineBlock originBlock)) {
            return null;
        }

        Direction originFacing =
                originState.getValue(
                        EngineBlock.FACING
                );

        BlockPos neighbourPos =
                origin.getBlockPos()
                        .relative(
                                direction
                        );

        BlockState neighbourState =
                level.getBlockState(
                        neighbourPos
                );

        if (!(neighbourState.getBlock()
                instanceof EngineBlock neighbourBlock)) {
            return null;
        }

        Direction neighbourFacing =
                neighbourState.getValue(
                        EngineBlock.FACING
                );

        if (neighbourFacing != originFacing) {
            return null;
        }

        if (!originBlock.hasShaftTowards(
                level,
                origin.getBlockPos(),
                originState,
                direction
        )) {
            return null;
        }

        if (!neighbourBlock.hasShaftTowards(
                level,
                neighbourPos,
                neighbourState,
                direction.getOpposite()
        )) {
            return null;
        }

        if (!(level.getBlockEntity(
                neighbourPos
        ) instanceof EngineBlockEntity engine)) {
            return null;
        }

        return engine;
    }

    private long getGroupFuelTicks(
            List<EngineBlockEntity> group
    ) {
        long totalFuel =
                0L;

        for (EngineBlockEntity engine : group) {
            totalFuel +=
                    Math.max(
                            0,
                            engine.fuelTicksRemaining
                    );
        }

        return totalFuel;
    }

    private long getGroupLavaAmountMb(
            List<EngineBlockEntity> group
    ) {
        long totalLava =
                0L;

        for (EngineBlockEntity engine : group) {
            totalLava +=
                    Math.max(
                            0,
                            engine.lavaAmountMb
                    );
        }

        return totalLava;
    }

    private void distributeGroupFuel(
            List<EngineBlockEntity> group,
            long totalFuel
    ) {
        if (group.isEmpty()) {
            return;
        }

        long maximumFuel =
                (long) Integer.MAX_VALUE
                        * group.size();

        totalFuel =
                Math.max(
                        0L,
                        Math.min(
                                totalFuel,
                                maximumFuel
                        )
                );

        long baseFuel =
                totalFuel
                        / group.size();

        int remainder =
                (int) (
                        totalFuel
                                % group.size()
                );

        for (int i = 0;
             i < group.size();
             i++) {
            EngineBlockEntity engine =
                    group.get(i);

            engine.fuelTicksRemaining =
                    (int) baseFuel
                            + (i < remainder
                            ? 1
                            : 0);

            engine.setChanged();
        }
    }

    private void distributeGroupLava(
            List<EngineBlockEntity> group,
            long totalLavaMb
    ) {
        if (group.isEmpty()) {
            return;
        }

        long maximumLava =
                (long) LAVA_CAPACITY_PER_ENGINE_MB
                        * group.size();

        totalLavaMb =
                Math.max(
                        0L,
                        Math.min(
                                totalLavaMb,
                                maximumLava
                        )
                );

        long baseLava =
                totalLavaMb
                        / group.size();

        int remainder =
                (int) (
                        totalLavaMb
                                % group.size()
                );

        for (int i = 0;
             i < group.size();
             i++) {
            EngineBlockEntity engine =
                    group.get(i);

            engine.lavaAmountMb =
                    (int) baseLava
                            + (i < remainder
                            ? 1
                            : 0);

            engine.setChanged();
        }
    }

    private long getFuelBurnRate(
            int engineCount
    ) {
        int count =
                Math.max(
                        1,
                        Math.min(
                                engineCount,
                                MAX_ENGINE_COUNT
                        )
                );

        long multiplier =
                1L;

        for (int i = 1;
             i < count;
             i++) {
            multiplier *=
                    MULTI_ENGINE_FUEL_MULTIPLIER;
        }

        return (long) FUEL_BURN_RATE
                * multiplier;
    }

    private boolean isGroupRunning(
            List<EngineBlockEntity> group
    ) {
        for (EngineBlockEntity engine : group) {
            if (engine.running) {
                return true;
            }
        }

        return false;
    }

    private boolean isGroupRedstonePowered(
            List<EngineBlockEntity> group
    ) {
        if (level == null) {
            return false;
        }

        for (EngineBlockEntity engine : group) {
            if (level.hasNeighborSignal(
                    engine.getBlockPos()
            )) {
                return true;
            }
        }

        return false;
    }

    private float findGroupStartSpeed(
            List<EngineBlockEntity> group
    ) {
        for (EngineBlockEntity engine : group) {
            float speed =
                    engine.getSpeed();

            if (Math.abs(speed) > 0.001F) {
                return speed;
            }
        }

        return 0.0F;
    }

    private void syncGroup(
            List<EngineBlockEntity> group
    ) {
        for (EngineBlockEntity engine : group) {
            engine.setChanged();
            engine.sendData();
        }
    }

    private Direction getOutputDirection() {
        Direction facing =
                getBlockState().getValue(
                        EngineBlock.FACING
                );

        return facing.getAxis() == Direction.Axis.X
                ? facing
                : facing.getOpposite();
    }

    public boolean isFirstEngineInGroup() {
        Direction starterDirection =
                getOutputDirection()
                        .getOpposite();

        return getConnectedEngine(
                this,
                starterDirection
        ) == null;
    }

    private void spawnEngineParticles() {
        if (level == null
                || !level.isClientSide) {
            return;
        }

        List<EngineBlockEntity> group =
                getEngineGroup();

        int engineCount =
                Math.min(
                        group.size(),
                        MAX_ENGINE_COUNT
                );

        Direction rear =
                getOutputDirection();

        EngineBlockEntity nextEngine =
                getConnectedEngine(
                        this,
                        rear
                );

        if (nextEngine != null) {
            return;
        }

        Vec3 rearVector =
                new Vec3(
                        rear.getStepX(),
                        0.0D,
                        rear.getStepZ()
                );

        Vec3 sideVector =
                new Vec3(
                        -rearVector.z,
                        0.0D,
                        rearVector.x
                );

        Vec3 center =
                Vec3.atCenterOf(
                        worldPosition
                );

        for (int emission = 0;
             emission < engineCount;
             emission++) {
            for (int side = -1;
                 side <= 1;
                 side += 2) {
                if (level.getRandom()
                        .nextFloat() >= 0.25F) {
                    continue;
                }

                Vec3 position =
                        center
                                .add(
                                        rearVector.scale(
                                                0.53D
                                        )
                                )
                                .add(
                                        sideVector.scale(
                                                0.28D * side
                                        )
                                )
                                .add(
                                        0.0D,
                                        -0.12D,
                                        0.0D
                                );

                double randomX =
                        (level.getRandom().nextDouble()
                                - 0.5D)
                                * 0.02D;

                double randomY =
                        level.getRandom().nextDouble()
                                * 0.02D;

                double randomZ =
                        (level.getRandom().nextDouble()
                                - 0.5D)
                                * 0.02D;

                Vec3 velocity =
                        rearVector.scale(
                                0.015D
                        ).add(
                                randomX,
                                randomY,
                                randomZ
                        );

                level.addParticle(
                        ParticleTypes.SMOKE,
                        position.x,
                        position.y,
                        position.z,
                        velocity.x,
                        velocity.y,
                        velocity.z
                );
            }
        }
    }

    public void addFuel(
            int burnTime
    ) {
        if (burnTime <= 0) {
            return;
        }

        List<EngineBlockEntity> group =
                getEngineGroup();

        long totalFuel =
                getGroupFuelTicks(
                        group
                );

        totalFuel +=
                burnTime;

        distributeGroupFuel(
                group,
                totalFuel
        );

        syncGroup(
                group
        );
    }

    public int getFuelTicksRemaining() {
        return fuelTicksRemaining;
    }

    public long getDisplayFuelTicksRemaining() {
        return getGroupFuelTicks(getEngineGroup());
    }

    public long getDisplayFuelAmountMb() {
        return getGroupLavaAmountMb(getEngineGroup());
    }

    public long getDisplayFuelTimeSeconds() {
        List<EngineBlockEntity> group =
                getEngineGroup();

        long fuelTicks =
                getGroupFuelTicks(group);

        if (fuelTicks <= 0L) {
            return 0L;
        }

        long burnRate =
                getFuelBurnRate(group.size());

        long remainingGameTicks =
                (fuelTicks + burnRate - 1L)
                        / burnRate;

        return (remainingGameTicks + 19L)
                / 20L;
    }

    public IFluidHandler getLavaFuelHandler() {
        return lavaFuelHandler;
    }

    public boolean isRunning() {
        return running;
    }

    public void setPonderFuel(
            int lavaMb
    ) {
        lavaAmountMb =
                Math.max(
                        0,
                        lavaMb
                );

        fuelTicksRemaining =
                lavaAmountMb
                        * LAVA_FUEL_TICKS_PER_MB;

        setChanged();
    }

    public void setPonderRunning(
            boolean running,
            int rotationSign
    ) {
        this.running =
                running;

        this.rotationSign =
                rotationSign < 0
                        ? -1
                        : 1;

        setChanged();
    }

    public void synchronizeWithEngineGroupAfterPlacement() {
        if (level == null
                || level.isClientSide) {
            return;
        }

        List<EngineBlockEntity> group =
                getEngineGroup();

        if (group.size() <= 1
                || group.size() > MAX_ENGINE_COUNT) {
            return;
        }

        EngineBlockEntity runningEngine =
                null;

        for (EngineBlockEntity engine : group) {
            if (engine.running) {
                runningEngine =
                        engine;

                break;
            }
        }

        if (runningEngine == null) {
            return;
        }

        int groupRotationSign =
                runningEngine.rotationSign;

        for (EngineBlockEntity engine : group) {
            engine.rotationSign =
                    groupRotationSign;

            engine.updateRunningState(
                    true
            );
        }

        long totalFuel =
                getGroupFuelTicks(
                        group
                );

        distributeGroupFuel(
                group,
                totalFuel
        );

        syncGroup(
                group
        );
    }

    private void updateRunningState(
            boolean running
    ) {
        if (level == null
                || level.isClientSide) {
            return;
        }

        if (this.running == running) {
            return;
        }

        this.running =
                running;

        BlockState state =
                getBlockState();

        if (state.getValue(EngineBlock.LIT)
                != running) {
            level.setBlock(
                    worldPosition,
                    state.setValue(
                            EngineBlock.LIT,
                            running
                    ),
                    3
            );
        }

        updateGeneratedRotation();
        updateStarterStressInNetwork();

        setChanged();
        sendData();
    }

    private void updateStarterStressInNetwork() {
        if (level == null
                || level.isClientSide
                || !hasNetwork()) {
            return;
        }

        KineticNetwork network =
                getOrCreateNetwork();

        if (network == null) {
            return;
        }

        network.updateStressFor(
                this,
                calculateStressApplied()
        );

        networkDirty =
                true;
    }

    @Override
    public float calculateStressApplied() {
        float impact =
                running
                        ? 0.0F
                        : STARTER_STRESS_IMPACT;

        lastStressApplied =
                impact;

        return impact;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity =
                running
                        ? STRESS_CAPACITY
                        : 0.0F;

        lastCapacityProvided =
                capacity;

        return capacity;
    }

    @Override
    public float getGeneratedSpeed() {
        if (!running) {
            return 0.0F;
        }

        return GENERATED_RPM
                * rotationSign;
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        tag.putInt(
                FUEL_TICKS_TAG,
                fuelTicksRemaining
        );

        tag.putInt(
                LAVA_AMOUNT_TAG,
                lavaAmountMb
        );

        tag.putBoolean(
                RUNNING_TAG,
                running
        );

        tag.putInt(
                ROTATION_SIGN_TAG,
                rotationSign
        );

        super.write(
                tag,
                registries,
                clientPacket
        );
    }

    @Override
    protected void read(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        fuelTicksRemaining =
                tag.getInt(
                        FUEL_TICKS_TAG
                );

        lavaAmountMb =
                tag.getInt(
                        LAVA_AMOUNT_TAG
                );

        running =
                tag.getBoolean(
                        RUNNING_TAG
                );

        rotationSign =
                tag.getInt(
                        ROTATION_SIGN_TAG
                );

        if (rotationSign == 0) {
            rotationSign = 1;
        }

        super.read(
                tag,
                registries,
                clientPacket
        );
    }

    @Override
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        int tooltipStart =
                tooltip.size();

        super.addToGoggleTooltip(
                tooltip,
                isPlayerSneaking
        );

        if (tooltip.size() > tooltipStart) {
            tooltip.remove(
                    tooltipStart
            );
        }

        CreateLang.builder()
                .add(
                        Component.translatable(
                                "tooltip.mechanical_drive.engine.name"
                        ).append(
                                Component.literal(":")
                        )
                )
                .style(
                        ChatFormatting.WHITE
                )
                .forGoggles(
                        tooltip
                );

        Component engineTitle =
                tooltip.remove(
                        tooltip.size() - 1
                );

        tooltip.add(
                tooltipStart,
                engineTitle
        );

        List<EngineBlockEntity> group =
                getEngineGroup();

        long groupFuelTicks =
                getGroupFuelTicks(
                        group
                );

        long groupLavaMb =
                groupFuelTicks
                        / LAVA_FUEL_TICKS_PER_MB;

        boolean groupRunning =
                isGroupRunning(
                        group
                );

        long groupLavaCapacityMb =
                (long) LAVA_CAPACITY_PER_ENGINE_MB
                        * Math.max(
                        1,
                        Math.min(
                                group.size(),
                                MAX_ENGINE_COUNT
                        )
                );

        CreateLang.builder()
                .add(
                        Component.literal(
                                String.format(
                                        Locale.ROOT,
                                        "%,dmB",
                                        groupLavaMb
                                )
                        ).withStyle(
                                ChatFormatting.GOLD
                        ).append(
                                Component.literal(
                                        " / "
                                ).withStyle(
                                        ChatFormatting.DARK_GRAY
                                )
                        ).append(
                                Component.literal(
                                        String.format(
                                                Locale.ROOT,
                                                "%,dmB",
                                                groupLavaCapacityMb
                                        )
                                ).withStyle(
                                        ChatFormatting.DARK_GRAY
                                )
                        )
                )
                .forGoggles(
                        tooltip
                );

        if (groupFuelTicks <= 0L) {
            CreateLang.builder()
                    .add(
                            Component.translatable(
                                    "tooltip.mechanical_drive.engine.fuel",
                                    Component.translatable(
                                            "tooltip.mechanical_drive.engine.empty"
                                    ).withStyle(
                                            ChatFormatting.RED
                                    )
                            ).withStyle(
                                    ChatFormatting.GRAY
                            )
                    )
                    .forGoggles(
                            tooltip
                    );

            return true;
        }

        if (groupRunning) {
            CreateLang.builder()
                    .add(
                            Component.translatable(
                                    "tooltip.mechanical_drive.engine.remaining_time",
                                    Component.literal(
                                            formatFuelTime(
                                                    groupFuelTicks,
                                                    group.size()
                                            )
                                    ).withStyle(
                                            ChatFormatting.AQUA
                                    )
                            ).withStyle(
                                    ChatFormatting.GRAY
                            )
                    )
                    .forGoggles(
                            tooltip
                    );
        } else {
            CreateLang.builder()
                    .add(
                            Component.translatable(
                                    "tooltip.mechanical_drive.engine.fuel",
                                    Component.literal(
                                            formatFuelTime(
                                                    groupFuelTicks,
                                                    group.size()
                                            )
                                    ).withStyle(
                                            ChatFormatting.GREEN
                                    )
                            ).withStyle(
                                    ChatFormatting.GRAY
                            )
                    )
                    .forGoggles(
                            tooltip
                    );
        }

        return true;
    }

    private String formatFuelTime(
            long fuelTicks,
            int engineCount
    ) {
        long burnRate =
                getFuelBurnRate(
                        engineCount
                );

        long remainingGameTicks =
                (fuelTicks
                        + burnRate
                        - 1L)
                        / burnRate;

        long totalSeconds =
                (remainingGameTicks
                        + 19L)
                        / 20L;

        long minutes =
                totalSeconds
                        / 60L;

        long seconds =
                totalSeconds
                        % 60L;

        if (minutes > 0L) {
            return String.format(
                    Locale.ROOT,
                    "%dm %ds",
                    minutes,
                    seconds
            );
        }

        return String.format(
                Locale.ROOT,
                "%ds",
                seconds
        );
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }
}
