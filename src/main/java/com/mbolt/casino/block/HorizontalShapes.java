package com.mbolt.casino.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Outline shapes of a block model that faces north, turned for every horizontal facing like its blockstate. */
public final class HorizontalShapes {
    private static final double SIZE = 16;

    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    /** Each box is {x1, y1, z1, x2, y2, z2} in model pixels for the north-facing model. */
    public HorizontalShapes(double[]... boxes) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] box : boxes) {
                shape = Shapes.or(shape, turn(box, facing));
            }
            shapes.put(facing, shape.optimize());
        }
    }

    public VoxelShape get(Direction facing) {
        return shapes.get(facing);
    }

    /** Matches blockstate rotation y = 90, 180, 270 for east, south and west (clockwise seen from above). */
    private static VoxelShape turn(double[] box, Direction facing) {
        double x1 = box[0];
        double y1 = box[1];
        double z1 = box[2];
        double x2 = box[3];
        double y2 = box[4];
        double z2 = box[5];
        return switch (facing) {
            case EAST -> Block.box(SIZE - z2, y1, x1, SIZE - z1, y2, x2);
            case SOUTH -> Block.box(SIZE - x2, y1, SIZE - z2, SIZE - x1, y2, SIZE - z1);
            case WEST -> Block.box(z1, y1, SIZE - x2, z2, y2, SIZE - x1);
            default -> Block.box(x1, y1, z1, x2, y2, z2);
        };
    }
}
