package lite.sqlite.testing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lite.sqlite.server.storage.io.FileIO;

/** T0.2 check: an unforced write is lost on a crash, a forced write survives, a torn write keeps a sector prefix. */
@DisplayName("SimulatedDisk")
class SimulatedDiskTest {

    private static final int PAGE = 4096;

    @TempDir
    Path dir;

    private static ByteBuffer page(int fill) {
        byte[] bytes = new byte[PAGE];
        Arrays.fill(bytes, (byte) fill);
        return ByteBuffer.wrap(bytes);
    }

    private static byte[] filled(int length, int fill) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) fill);
        return bytes;
    }

    @Test
    @DisplayName("Unforced write is lost on crash")
    void unforcedWriteLost() throws IOException {
        SimulatedDisk disk = new SimulatedDisk();
        FileIO file = disk.open(dir.resolve("t.tbl"));
        file.write(0, page(0x11));

        disk.crash();

        assertEquals(0, Files.size(dir.resolve("t.tbl")));
    }

    @Test
    @DisplayName("Forced write survives crash")
    void forcedWriteSurvives() throws IOException {
        SimulatedDisk disk = new SimulatedDisk();
        FileIO file = disk.open(dir.resolve("t.tbl"));
        file.write(0, page(0x11));
        file.force();

        disk.crash();

        assertArrayEquals(filled(PAGE, 0x11), Files.readAllBytes(dir.resolve("t.tbl")));
        assertEquals(1, disk.forceCount());
    }

    @Test
    @DisplayName("Overwrite after force rolls back to the forced bytes on crash")
    void overwriteAfterForceRollsBack() throws IOException {
        SimulatedDisk disk = new SimulatedDisk();
        FileIO file = disk.open(dir.resolve("t.tbl"));
        file.write(0, page(0x11));
        file.force();
        file.write(0, page(0x22));

        disk.crash();

        assertArrayEquals(filled(PAGE, 0x11), Files.readAllBytes(dir.resolve("t.tbl")));
    }

    @Test
    @DisplayName("Before a crash, reads and size see unforced writes (page cache)")
    void readsSeeUnforcedWrites() throws IOException {
        SimulatedDisk disk = new SimulatedDisk();
        FileIO file = disk.open(dir.resolve("t.tbl"));
        file.write(0, page(0x11));
        file.force();
        file.write(PAGE, page(0x33));
        file.write(100, ByteBuffer.wrap(filled(10, 0x22)));

        assertEquals(2L * PAGE, file.size());
        ByteBuffer first = ByteBuffer.allocate(PAGE);
        assertEquals(PAGE, file.read(0, first));
        byte[] expected = filled(PAGE, 0x11);
        Arrays.fill(expected, 100, 110, (byte) 0x22);
        assertArrayEquals(expected, first.array());
        ByteBuffer second = ByteBuffer.allocate(PAGE);
        file.read(PAGE, second);
        assertArrayEquals(filled(PAGE, 0x33), second.array());
        assertEquals(-1, file.read(2L * PAGE, ByteBuffer.allocate(1)));
        disk.crash(); // releases the durable handle so @TempDir can be deleted
    }

    @Test
    @DisplayName("Unforced writes survive closing and reopening the file, but not a crash")
    void closeDoesNotSync() throws IOException {
        SimulatedDisk disk = new SimulatedDisk();
        FileIO file = disk.open(dir.resolve("t.tbl"));
        file.write(0, page(0x11));
        file.close();

        FileIO again = disk.open(dir.resolve("t.tbl"));
        assertEquals(PAGE, again.size());

        disk.crash();
        assertEquals(0, Files.size(dir.resolve("t.tbl")));
    }

    @Test
    @DisplayName("Every operation fails after the power is off")
    void operationsFailAfterCrash() throws IOException {
        SimulatedDisk disk = new SimulatedDisk();
        FileIO file = disk.open(dir.resolve("t.tbl"));
        disk.crash();

        assertThrows(IOException.class, () -> file.write(0, page(0x11)));
        assertThrows(IOException.class, () -> file.read(0, ByteBuffer.allocate(1)));
        assertThrows(IOException.class, file::force);
        assertThrows(IOException.class, () -> disk.open(dir.resolve("other.tbl")));
    }

    @Test
    @DisplayName("Torn write keeps a sector-aligned strict prefix of new bytes, old bytes after it (seeds 0..99)")
    void tornWriteKeepsSectorPrefix() throws IOException {
        boolean sawPartial = false;
        for (int seed = 0; seed < 100; seed++) {
            Path f = dir.resolve("t" + seed + ".tbl");
            SimulatedDisk disk = new SimulatedDisk();
            FileIO file = disk.open(f);
            file.write(0, page(0x11));
            file.force();
            file.write(0, page(0x22));

            SimulatedDisk.TornWrite torn = disk.crashTearing(new Random(seed));

            assertNotNull(torn, "seed " + seed);
            int kept = torn.keptBytes();
            assertEquals(0, kept % SimulatedDisk.SECTOR, "seed " + seed + ": tear not on a sector boundary");
            assertTrue(kept < PAGE, "seed " + seed + ": a torn write must not land whole");
            byte[] expected = filled(PAGE, 0x11);
            Arrays.fill(expected, 0, kept, (byte) 0x22);
            assertArrayEquals(expected, Files.readAllBytes(f), "seed " + seed);
            sawPartial |= kept > 0;
        }
        assertTrue(sawPartial, "100 seeds never kept part of the write; the tear is not exercised");
    }
}
