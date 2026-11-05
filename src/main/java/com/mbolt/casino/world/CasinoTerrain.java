package com.mbolt.casino.world;

import java.util.OptionalInt;
import java.util.function.BiFunction;

/** Pure footprint selection: every column must be dry and gentle, and the entrance stays at ground level. */
public final class CasinoTerrain {
    /** Columns apart in the coarse pass, which finds most ponds and slopes for a sixteenth of the reads. */
    private static final int GRID = 4;

    private CasinoTerrain() {
    }

    public static OptionalInt floor(int minX, int minZ, int width, int depth, int entranceX, int entranceZ,
                                    int maxSlope, BiFunction<Integer, Integer, Surface> read) {
        if (width <= 0 || depth <= 0 || maxSlope < 0 || entranceX < minX || entranceX >= minX + width
                || entranceZ < minZ || entranceZ >= minZ + depth) {
            throw new IllegalArgumentException("Invalid casino footprint");
        }
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        int entrance = 0;
        // Reject obviously unsuitable large sites before asking the noise generator for every column: first the
        // entrance, the corners and the middle, then a coarse grid. Successful sites still inspect the entire
        // footprint, including ponds between the grid columns.
        int entranceIndex = (entranceZ - minZ) * width + entranceX - minX;
        int[] probes = {entranceIndex, 0, width - 1, (depth - 1) * width, width * depth - 1,
                (depth / 2) * width + width / 2};
        int[][] passes = {probes, grid(width, depth), null};
        boolean[] visited = new boolean[width * depth];
        for (int[] pass : passes) {
            int count = pass == null ? visited.length : pass.length;
            for (int i = 0; i < count; i++) {
                int index = pass == null ? i : pass[i];
                if (visited[index]) {
                    continue;
                }
                visited[index] = true;
                int x = minX + index % width;
                int z = minZ + index / width;
                Surface surface = read.apply(x, z);
                if (!surface.dry()) {
                    return OptionalInt.empty();
                }
                lowest = Math.min(lowest, surface.y());
                highest = Math.max(highest, surface.y());
                if ((long) highest - lowest > maxSlope) {
                    return OptionalInt.empty();
                }
                if (x == entranceX && z == entranceZ) {
                    entrance = surface.y();
                }
            }
        }
        return OptionalInt.of(entrance);
    }

    /** Every GRID-th column of every GRID-th row, and the last row and column. */
    private static int[] grid(int width, int depth) {
        int[] xs = lines(width);
        int[] zs = lines(depth);
        int[] indices = new int[xs.length * zs.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = zs[i / xs.length] * width + xs[i % xs.length];
        }
        return indices;
    }

    private static int[] lines(int length) {
        int count = (length - 1) / GRID + 1;
        boolean last = (length - 1) % GRID != 0;
        int[] lines = new int[count + (last ? 1 : 0)];
        for (int i = 0; i < count; i++) {
            lines[i] = i * GRID;
        }
        if (last) {
            lines[count] = length - 1;
        }
        return lines;
    }

    public record Surface(int y, boolean dry) {
    }
}
