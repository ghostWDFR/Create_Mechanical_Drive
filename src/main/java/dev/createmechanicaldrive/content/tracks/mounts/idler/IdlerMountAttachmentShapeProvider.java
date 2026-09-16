package dev.createmechanicaldrive.content.tracks.mounts.idler;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface IdlerMountAttachmentShapeProvider {
    boolean supports(ItemStack attachment);

    VoxelShape getOutlineShape(
            ItemStack attachment,
            Direction outputDirection,
            CollisionContext context,
            double axleOffset
    );

    default VoxelShape getCollisionShape(
            ItemStack attachment,
            Direction outputDirection,
            CollisionContext context,
            double axleOffset
    ) {
        return getOutlineShape(
                attachment,
                outputDirection,
                context,
                axleOffset
        );
    }
}
