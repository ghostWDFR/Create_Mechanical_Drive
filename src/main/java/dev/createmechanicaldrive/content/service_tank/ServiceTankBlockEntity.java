package dev.createmechanicaldrive.content.service_tank;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ServiceTankBlockEntity extends FluidTankBlockEntity {
    public static final int BASE_CAPACITY = 6000;
    public static final int MAX_LINKED_TANKS = 2;
    private static final String LINKED_DIRECTIONS_KEY = "LinkedDirections";

    private final Set<Direction> linkedDirections = EnumSet.noneOf(Direction.class);

    public ServiceTankBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
        tankInventory.setCapacity(BASE_CAPACITY);
        window = false;
        luminosity = 0;
    }

    @Override
    public void toggleWindows() {
        window = false;
    }

    @Override
    public void setWindows(boolean window) {
        this.window = false;
    }

    @Override
    public void setExtraData(Object extraData) {
        window = false;
    }

    @Override
    protected void setLuminosity(int luminosity) {
        super.setLuminosity(0);
    }

    public void requestNetworkUpdate() {
        updateConnectivity = true;
    }

    @Override
    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level == null || level.isClientSide) {
            return;
        }
        reconcileNetwork();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level != null && !level.isClientSide) {
            reconcileNetwork();
        }
    }

    public boolean hasLinkedTank(BlockPos pos) {
        BlockPos offset = pos.subtract(worldPosition);
        Direction direction = Direction.fromDelta(
                offset.getX(),
                offset.getY(),
                offset.getZ()
        );
        return direction != null && linkedDirections.contains(direction);
    }

    public static boolean isServiceLinked(BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof ServiceTankBlockEntity service
                    && service.hasLinkedTank(pos)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isServiceConnection(
            BlockGetter level,
            BlockPos first,
            BlockPos second
    ) {
        return isServiceLinked(level, first) || isServiceLinked(level, second);
    }

    public static boolean claimForAdjacentService(Level level, BlockPos tankPos) {
        if (level.isClientSide) {
            return isServiceLinked(level, tankPos);
        }
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(tankPos.relative(direction))
                    instanceof ServiceTankBlockEntity service) {
                service.reconcileNetwork();
                if (service.hasLinkedTank(tankPos)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void reconcileNetwork() {
        Set<Direction> target = EnumSet.noneOf(Direction.class);

        for (Direction direction : linkedDirections) {
            if (target.size() >= MAX_LINKED_TANKS) {
                break;
            }
            FluidTankBlockEntity candidate = getRegularTank(direction);
            if (candidate != null && isControlledByThis(candidate)) {
                target.add(direction);
            }
        }

        for (Direction direction : Direction.values()) {
            if (target.size() >= MAX_LINKED_TANKS) {
                break;
            }
            if (target.contains(direction)) {
                continue;
            }
            FluidTankBlockEntity candidate = getRegularTank(direction);
            if (candidate != null && canAttach(candidate)) {
                target.add(direction);
            }
        }

        boolean changed = !target.equals(linkedDirections);
        List<FluidTankBlockEntity> detached = new ArrayList<>();
        for (Direction direction : linkedDirections) {
            if (target.contains(direction)) {
                continue;
            }
            FluidTankBlockEntity candidate = getRegularTank(direction);
            if (candidate != null && isControlledByThis(candidate)) {
                candidate.removeController(false);
                detached.add(candidate);
            }
        }

        int finalCapacity = capacityFor(target.size());
        tankInventory.setCapacity(Math.max(tankInventory.getCapacity(), finalCapacity));

        Set<Direction> attached = EnumSet.noneOf(Direction.class);
        for (Direction direction : target) {
            FluidTankBlockEntity candidate = getRegularTank(direction);
            if (candidate == null) {
                continue;
            }
            if (!isControlledByThis(candidate) && !attach(candidate)) {
                changed = true;
                continue;
            }
            forceSingleModel(candidate);
            attached.add(direction);
        }

        finalCapacity = capacityFor(attached.size());
        moveOverflowToDetachedTanks(finalCapacity, detached);
        tankInventory.setCapacity(finalCapacity);

        if (!attached.equals(linkedDirections)) {
            linkedDirections.clear();
            linkedDirections.addAll(attached);
            changed = true;
        }

        if (changed) {
            setChanged();
            sendData();
        }
    }

    private FluidTankBlockEntity getRegularTank(Direction direction) {
        if (level == null) {
            return null;
        }
        BlockPos pos = worldPosition.relative(direction);
        if (!level.getBlockState(pos).is(AllBlocks.FLUID_TANK.get())) {
            return null;
        }
        return level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank
                ? tank
                : null;
    }

    private boolean canAttach(FluidTankBlockEntity candidate) {
        if (isControlledByThis(candidate)) {
            return true;
        }
        if (!candidate.isController()
                || candidate.getWidth() != 1
                || candidate.getHeight() != 1) {
            return false;
        }
        FluidStack ownFluid = tankInventory.getFluid();
        FluidStack candidateFluid = candidate.getTankInventory().getFluid();
        return ownFluid.isEmpty()
                || candidateFluid.isEmpty()
                || FluidStack.isSameFluidSameComponents(ownFluid, candidateFluid);
    }

    private boolean attach(FluidTankBlockEntity candidate) {
        FluidTank candidateTank = candidate.getTankInventory();
        FluidStack fluid = candidateTank.getFluid().copy();
        if (!fluid.isEmpty()) {
            int accepted = tankInventory.fill(fluid, IFluidHandler.FluidAction.SIMULATE);
            if (accepted != fluid.getAmount()) {
                return false;
            }
            tankInventory.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
            candidateTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        }
        candidate.setController(worldPosition);
        candidate.preventConnectivityUpdate();
        return true;
    }

    private boolean isControlledByThis(FluidTankBlockEntity candidate) {
        return !candidate.isController()
                && worldPosition.equals(candidate.getController());
    }

    private void forceSingleModel(FluidTankBlockEntity tank) {
        if (level == null) {
            return;
        }
        BlockState state = tank.getBlockState()
                .setValue(FluidTankBlock.TOP, true)
                .setValue(FluidTankBlock.BOTTOM, true)
                .setValue(FluidTankBlock.SHAPE, FluidTankBlock.Shape.PLAIN);
        if (state != tank.getBlockState()) {
            level.setBlock(tank.getBlockPos(), state, 22);
        }
    }

    private void moveOverflowToDetachedTanks(
            int finalCapacity,
            List<FluidTankBlockEntity> detached
    ) {
        int overflow = tankInventory.getFluidAmount() - finalCapacity;
        for (FluidTankBlockEntity tank : detached) {
            if (overflow <= 0) {
                break;
            }
            FluidStack drained = tankInventory.drain(
                    Math.min(overflow, FluidTankBlockEntity.getCapacityMultiplier()),
                    IFluidHandler.FluidAction.EXECUTE
            );
            int accepted = tank.getTankInventory().fill(
                    drained,
                    IFluidHandler.FluidAction.EXECUTE
            );
            overflow -= accepted;
            if (accepted < drained.getAmount()) {
                tankInventory.fill(
                        drained.copyWithAmount(drained.getAmount() - accepted),
                        IFluidHandler.FluidAction.EXECUTE
                );
            }
        }
        if (tankInventory.getFluidAmount() > finalCapacity) {
            tankInventory.drain(
                    tankInventory.getFluidAmount() - finalCapacity,
                    IFluidHandler.FluidAction.EXECUTE
            );
        }
    }

    public void detachAll() {
        if (level == null || level.isClientSide) {
            return;
        }
        List<FluidTankBlockEntity> detached = new ArrayList<>();
        for (Direction direction : linkedDirections) {
            FluidTankBlockEntity candidate = getRegularTank(direction);
            if (candidate != null && isControlledByThis(candidate)) {
                candidate.removeController(false);
                detached.add(candidate);
            }
        }
        moveOverflowToDetachedTanks(0, detached);
        linkedDirections.clear();
        setChanged();
    }

    private static int capacityFor(int linkedTankCount) {
        return BASE_CAPACITY
                + linkedTankCount * FluidTankBlockEntity.getCapacityMultiplier();
    }

    @Override
    protected void read(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.read(tag, registries, clientPacket);
        window = false;
        luminosity = 0;
        linkedDirections.clear();
        for (int ordinal : tag.getIntArray(LINKED_DIRECTIONS_KEY)) {
            if (ordinal >= 0
                    && ordinal < Direction.values().length
                    && linkedDirections.size() < MAX_LINKED_TANKS) {
                linkedDirections.add(Direction.values()[ordinal]);
            }
        }
        tankInventory.setCapacity(capacityFor(linkedDirections.size()));
    }

    @Override
    public void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.write(tag, registries, clientPacket);
        tag.putIntArray(
                LINKED_DIRECTIONS_KEY,
                linkedDirections.stream().mapToInt(Direction::ordinal).toArray()
        );
    }
}
