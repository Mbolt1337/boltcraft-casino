package com.mbolt.casino.world;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoTerrainTest {
    @Test
    void flatGroundUsesTheActualSurfaceWithoutBuryingTheFloor() {
        var calls = new AtomicInteger();
        var floor = CasinoTerrain.floor(-12, 30, 17, 17, -4, 46, 3, (x, z) -> {
            calls.incrementAndGet();
            return new CasinoTerrain.Surface(63, true);
        });
        assertEquals(63, floor.orElseThrow());
        assertEquals(17 * 17, calls.get());
    }

    @Test
    void aPondAwayFromTheOldCornerSamplesRejectsTheSite() {
        var floor = CasinoTerrain.floor(0, 0, 27, 24, 13, 23, 4,
                (x, z) -> new CasinoTerrain.Surface(63, x != 5 || z != 9));
        assertTrue(floor.isEmpty());
    }

    @Test
    void aCliffInsideTheFootprintCannotBeHiddenByLevelCorners() {
        var floor = CasinoTerrain.floor(0, 0, 27, 24, 13, 23, 4,
                (x, z) -> new CasinoTerrain.Surface(x == 5 && z == 9 ? 75 : 63, true));
        assertTrue(floor.isEmpty());
    }

    @Test
    void aGentleSlopeKeepsTheEntranceAtItsOwnGroundHeight() {
        var floor = CasinoTerrain.floor(0, 0, 17, 17, 8, 16, 3,
                (x, z) -> new CasinoTerrain.Surface(64 + z / 8, true));
        assertEquals(66, floor.orElseThrow());
    }

    @Test
    void invalidEntrancesCannotSilentlyChooseZero() {
        assertThrows(IllegalArgumentException.class, () -> CasinoTerrain.floor(0, 0, 17, 17, 18, 0, 3,
                (x, z) -> new CasinoTerrain.Surface(63, true)));
    }

    @Test
    void aLargeHillsideIsRejectedBeforeScanningItsInterior() {
        var calls = new AtomicInteger();
        var floor = CasinoTerrain.floor(0, 0, 43, 43, 21, 42, 4, (x, z) -> {
            calls.incrementAndGet();
            return new CasinoTerrain.Surface(x < 21 ? 63 : 75, true);
        });
        assertTrue(floor.isEmpty());
        assertTrue(calls.get() <= 6, "An obvious hillside should need only a few noise columns");
    }

    @Test
    void aPondFarFromTheProbesIsFoundByTheCoarsePass() {
        var calls = new AtomicInteger();
        var floor = CasinoTerrain.floor(0, 0, 41, 46, 20, 45, 4, (x, z) -> {
            calls.incrementAndGet();
            return new CasinoTerrain.Surface(63, x < 36 || x > 38 || z < 2 || z > 6);
        });
        assertTrue(floor.isEmpty());
        assertTrue(calls.get() < 41 * 46 / 8, "A pond the size of a house should not need a full scan");
    }
}
