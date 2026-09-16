package dev.createmechanicaldrive.content.tracks.mounts.sprocket;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface SprocketMountAttachmentShapeProvider {
    boolean supports(ItemStack attachment);

    VoxelShape getOutlineShape(
            ItemStack attachment,
            Direction outputDirection,
            CollisionContext context
    );

    default VoxelShape getCollisionShape(
            ItemStack attachment,
            Direction outputDirection,
            CollisionContext context
    ) {
        return getOutlineShape(attachment, outputDirection, context);
    }
}
