package dev.createmechanicaldrive.content.tracks.mounts.idler;

import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import dev.createmechanicaldrive.content.tracks.wheels.idler.IdlerWheelItem;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class IdlerMountAttachmentShapes {
    private static final double MOUNT_THICKNESS = 3.0D / 16.0D;
    private static final VoxelShape MARKER_SHAPE_UP = Block.box(
            4.0D, 0.0D, 4.0D,
            12.0D, 10.0D, 12.0D
    );
    private static final VoxelShape IDLER_WHEEL_SHAPE_UP = Shapes.or(
            Block.box(
                    3.0D, 3.0D, 0.0D,
                    13.0D, 13.0D, 16.0D
            ),
            Block.box(
                    0.0D, 3.0D, 3.0D,
                    16.0D, 13.0D, 13.0D
            )
    );
    private static final Map<Direction, VoxelShape> IDLER_WHEEL_SHAPES =
            createRotatedShapes(IDLER_WHEEL_SHAPE_UP);

    private static final Map<Direction, VoxelShape> MARKER_SHAPES =
            createPlacedShapes(MARKER_SHAPE_UP);

    private static final List<IdlerMountAttachmentShapeProvider> PROVIDERS =
            new ArrayList<>();

    static {
        register(new IdlerMountAttachmentShapeProvider() {
            @Override
            public boolean supports(ItemStack attachment) {
                return ShaftMarkerItem.isMarker(attachment);
            }

            @Override
            public VoxelShape getOutlineShape(
                    ItemStack attachment,
                    Direction outputDirection,
                    CollisionContext context,
                    double axleOffset
            ) {
                return moveLaterally(
                        MARKER_SHAPES.get(outputDirection),
                        outputDirection,
                        axleOffset
                );
            }
        });
        register(new IdlerMountAttachmentShapeProvider() {
            @Override
            public boolean supports(ItemStack attachment) {
                return IdlerWheelItem.isIdlerWheel(attachment);
            }

            @Override
            public VoxelShape getOutlineShape(
                    ItemStack attachment,
                    Direction outputDirection,
                    CollisionContext context,
                    double axleOffset
            ) {
                return moveLaterally(
                        IDLER_WHEEL_SHAPES.get(outputDirection),
                        outputDirection,
                        axleOffset
                );
            }
        });
    }

    private IdlerMountAttachmentShapes() {
    }

    public static void register(
            IdlerMountAttachmentShapeProvider provider
    ) {
        PROVIDERS.add(Objects.requireNonNull(provider));
    }

    public static boolean supports(ItemStack attachment) {
        return find(attachment) != null;
    }

    public static VoxelShape getOutlineShape(
            ItemStack attachment,
            Direction outputDirection,
            CollisionContext context,
            double axleOffset
    ) {
        IdlerMountAttachmentShapeProvider provider = find(attachment);
        return provider == null
                ? Shapes.empty()
                : provider.getOutlineShape(
                        attachment,
                        outputDirection,
                        context,
                        axleOffset
                );
    }

    public static VoxelShape getCollisionShape(
            ItemStack attachment,
            Direction outputDirection,
            CollisionContext context,
            double axleOffset
    ) {
        IdlerMountAttachmentShapeProvider provider = find(attachment);
        return provider == null
                ? Shapes.empty()
                : provider.getCollisionShape(
                        attachment,
                        outputDirection,
                        context,
                        axleOffset
                );
    }

    private static IdlerMountAttachmentShapeProvider find(
            ItemStack attachment
    ) {
        if (attachment.isEmpty()) {
            return null;
        }
        for (IdlerMountAttachmentShapeProvider provider : PROVIDERS) {
            if (provider.supports(attachment)) {
                return provider;
            }
        }
        return null;
    }

    private static Map<Direction, VoxelShape> createRotatedShapes(
            VoxelShape shape
    ) {
        Map<Direction, VoxelShape> result =
                new EnumMap<>(Direction.class);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            result.put(
                    direction,
                    rotateFromUp(shape, direction)
            );
        }

        return result;
    }

    private static Map<Direction, VoxelShape> createPlacedShapes(
            VoxelShape shape
    ) {
        Map<Direction, VoxelShape> result =
                new EnumMap<>(Direction.class);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            result.put(
                    direction,
                    placeOnMount(shape, direction)
            );
        }

        return result;
    }

    private static VoxelShape placeOnMount(
            VoxelShape shapeUp,
            Direction outputDirection
    ) {
        return rotateFromUp(shapeUp, outputDirection).move(
                outputDirection.getStepX() * MOUNT_THICKNESS,
                outputDirection.getStepY() * MOUNT_THICKNESS,
                outputDirection.getStepZ() * MOUNT_THICKNESS
        );
    }

    static VoxelShape rotateFromUp(
            VoxelShape shape,
            Direction facing
    ) {
        VoxelShape result = Shapes.empty();
        for (var box : shape.toAabbs()) {
            double minX = box.minX * 16.0D;
            double minY = box.minY * 16.0D;
            double minZ = box.minZ * 16.0D;
            double maxX = box.maxX * 16.0D;
            double maxY = box.maxY * 16.0D;
            double maxZ = box.maxZ * 16.0D;

            VoxelShape transformed = switch (facing) {
                case NORTH -> Block.box(
                        minX, minZ, 16.0D - maxY,
                        maxX, maxZ, 16.0D - minY
                );
                case SOUTH -> Block.box(
                        minX, 16.0D - maxZ, minY,
                        maxX, 16.0D - minZ, maxY
                );
                case WEST -> Block.box(
                        16.0D - maxY, minZ, minX,
                        16.0D - minY, maxZ, maxX
                );
                case EAST -> Block.box(
                        minY, minZ, 16.0D - maxX,
                        maxY, maxZ, 16.0D - minX
                );
                default -> throw new IllegalArgumentException(
                        "Idler mount attachment cannot face " + facing
                );
            };
            result = Shapes.or(result, transformed);
        }
        return result;
    }

    static VoxelShape moveLaterally(
            VoxelShape shape,
            Direction outputDirection,
            double offset
    ) {
        if (offset == 0.0D) {
            return shape;
        }

        Direction lateral = outputDirection.getClockWise();
        return shape.move(
                lateral.getStepX() * offset,
                0.0D,
                lateral.getStepZ() * offset
        );
    }
}
