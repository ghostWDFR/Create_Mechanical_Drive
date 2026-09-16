package dev.createmechanicaldrive.content.shaft_marker;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ShaftMarkerBlockEntity extends KineticBlockEntity {
    public ShaftMarkerBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }
}
