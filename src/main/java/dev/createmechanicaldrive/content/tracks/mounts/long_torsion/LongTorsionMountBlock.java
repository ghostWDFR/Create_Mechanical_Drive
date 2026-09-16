package dev.createmechanicaldrive.content.tracks.mounts.long_torsion;

import dev.createmechanicaldrive.content.tracks.mounts.torsion.TorsionMountBlock;
import dev.createmechanicaldrive.content.tracks.mounts.torsion.TorsionMountBlockEntity;
import dev.createmechanicaldrive.content.tracks.wheels.drive.BigDriveWheelItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class LongTorsionMountBlock extends TorsionMountBlock {
    public static final double ARM_LENGTH = 16.0D / 16.0D;

    public LongTorsionMountBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean supportsAttachment(ItemStack stack) {
        return BigDriveWheelItem.isBigDriveWheel(stack);
    }

    @Override
    public boolean supportsSupportWheel(ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowsSupportWheel() {
        return false;
    }

    @Override
    public double armLength() {
        return ARM_LENGTH;
    }

    @Override
    public InteractionResult onWrenched(
            BlockState state,
            UseOnContext context
    ) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        TorsionMountBlockEntity mount = bigWheelMount(
                level,
                pos,
                state.getValue(HORIZONTAL_FACING)
        );
        if (mount != null) {
            List<TorsionMountBlockEntity> group = connectedGroup(mount);
            if (group.size() > 1) {
                if (!level.isClientSide) {
                    boolean flipped = !mount.isBigWheelOrderFlipped();
                    for (TorsionMountBlockEntity member : group) {
                        member.setBigWheelOrderFlipped(flipped);
                    }
                    level.playSound(
                            null,
                            pos,
                            SoundEvents.ITEM_FRAME_ROTATE_ITEM,
                            SoundSource.BLOCKS,
                            0.75F,
                            1.0F
                    );
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.onWrenched(state, context);
    }

    public static void synchronizeBigWheelGroup(
            TorsionMountBlockEntity changed
    ) {
        if (changed.getLevel() == null
                || changed.getLevel().isClientSide
                || !BigDriveWheelItem.isBigDriveWheel(
                changed.getAttachment()
        )) {
            return;
        }
        List<TorsionMountBlockEntity> group = connectedGroup(changed);
        boolean flipped = changed.isBigWheelOrderFlipped();
        for (TorsionMountBlockEntity member : group) {
            if (member != changed) {
                flipped = member.isBigWheelOrderFlipped();
                break;
            }
        }
        for (TorsionMountBlockEntity member : group) {
            member.setBigWheelOrderFlipped(flipped);
        }
    }

    public static boolean hasAdjacentBigWheel(
            TorsionMountBlockEntity mount
    ) {
        Level level = mount.getLevel();
        BlockState state = mount.getBlockState();
        if (level == null
                || !(state.getBlock() instanceof LongTorsionMountBlock)
                || !BigDriveWheelItem.isBigDriveWheel(
                mount.getAttachment()
        )) {
            return false;
        }
        Direction facing = state.getValue(HORIZONTAL_FACING);
        Direction rolling = facing.getClockWise();
        BlockPos pos = mount.getBlockPos();
        return bigWheelMount(level, pos.relative(rolling), facing) != null
                || bigWheelMount(
                level,
                pos.relative(rolling.getOpposite()),
                facing
        ) != null;
    }

    public static boolean rendersOuterWheel(
            TorsionMountBlockEntity mount
    ) {
        Direction facing = mount.getBlockState().getValue(
                HORIZONTAL_FACING
        );
        Direction rolling = facing.getClockWise();
        BlockPos pos = mount.getBlockPos();
        int coordinate = pos.getX() * rolling.getStepX()
                + pos.getZ() * rolling.getStepZ();
        boolean odd = Math.floorMod(coordinate, 2) == 1;
        return odd ^ mount.isBigWheelOrderFlipped();
    }

    private static List<TorsionMountBlockEntity> connectedGroup(
            TorsionMountBlockEntity origin
    ) {
        Level level = origin.getLevel();
        if (level == null) {
            return List.of();
        }
        Direction facing = origin.getBlockState().getValue(
                HORIZONTAL_FACING
        );
        Direction rolling = facing.getClockWise();
        BlockPos first = origin.getBlockPos();
        while (bigWheelMount(
                level,
                first.relative(rolling.getOpposite()),
                facing
        ) != null) {
            first = first.relative(rolling.getOpposite());
        }

        List<TorsionMountBlockEntity> group = new ArrayList<>();
        BlockPos cursor = first;
        TorsionMountBlockEntity member;
        while ((member = bigWheelMount(level, cursor, facing)) != null) {
            group.add(member);
            cursor = cursor.relative(rolling);
        }
        return group;
    }

    private static TorsionMountBlockEntity bigWheelMount(
            Level level,
            BlockPos pos,
            Direction facing
    ) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LongTorsionMountBlock)
                || state.getValue(HORIZONTAL_FACING) != facing) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof TorsionMountBlockEntity mount)
                || !BigDriveWheelItem.isBigDriveWheel(
                mount.getAttachment()
        )) {
            return null;
        }
        return mount;
    }
}
