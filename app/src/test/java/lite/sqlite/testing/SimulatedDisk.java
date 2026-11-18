package lite.sqlite.testing;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import lite.sqlite.server.storage.io.FileIO;
import lite.sqlite.server.storage.io.FileIOProvider;

/**
 * A {@link FileIOProvider} that can lose power (spec T0.2).
 *
 * <p>Each file has two layers. The durable layer is the real file in the test's directory, so
 * {@code Files.exists} and a reopen with the production provider see exactly what survived. The
 * volatile layer holds every write since the last {@link FileIO#force}, like an OS page cache:
 * reads see it, {@code force} moves it to the durable layer, and a crash drops it.
 *
 * <p>Simplification: on a crash, unforced writes are lost entirely (or one is torn). A real disk
 * may also persist some of them; the WAL tests in P1 will need that mode, and it is a small change.
 */
public final class SimulatedDisk implements FileIOProvider {

    /** Writes tear at this boundary, the smallest unit a disk writes atomically. */
    public static final int SECTOR = 512;

    /** What {@link #crashTearing} kept: the first {@code keptBytes} of a {@code length}-byte write at {@code position}. */
    public record TornWrite(Path file, long position, int length, int keptBytes) {}

    private record Pending(long position, byte[] bytes) {
        long end() {
            return position + bytes.length;
        }
    }

    private final Map<Path, FileState> files = new TreeMap<>();
    private boolean crashed;
    private int forceCount;

    @Override
    public synchronized FileIO open(Path file) throws IOException {
        checkPower();
        Path key = file.toAbsolutePath().normalize();
        FileState state = files.get(key);
        if (state == null) {
            state = new FileState(key);
            files.put(key, state);
        }
        return new Handle(state);
    }

    /** Power loss: every unforced write is gone, and every later operation through this disk fails. */
    public synchronized void crash() throws IOException {
        for (FileState state : files.values()) {
            state.pending.clear();
        }
        powerOff();
    }

    /**
     * Power loss in the middle of one unforced write: a sector-aligned prefix of it (possibly empty,
     * never all of it) reaches the disk, and every other unforced write is lost. The victim is
     * chosen by {@code random}, so a seed reproduces the crash. Returns null when nothing was pending.
     */
    public synchronized TornWrite crashTearing(Random random) throws IOException {
        List<FileState> withPending = new ArrayList<>();
        for (FileState state : files.values()) {
            if (!state.pending.isEmpty()) {
                withPending.add(state);
            }
        }
        TornWrite torn = null;
        if (!withPending.isEmpty()) {
            FileState state = withPending.get(random.nextInt(withPending.size()));
            Pending victim = state.pending.get(random.nextInt(state.pending.size()));
            int sectors = (victim.bytes.length - 1) / SECTOR; // full sectors strictly before the last byte
            int kept = sectors == 0 ? 0 : random.nextInt(sectors + 1) * SECTOR;
            state.writeDurable(victim.position, victim.bytes, kept);
            torn = new TornWrite(state.path, victim.position, victim.bytes.length, kept);
        }
        crash();
        return torn;
    }

    /** Number of {@code force} calls so far, across all files. */
    public synchronized int forceCount() {
        return forceCount;
    }

    private void powerOff() throws IOException {
        crashed = true;
        for (FileState state : files.values()) {
            state.closeDurable();
        }
    }

    private void checkPower() throws IOException {
        if (crashed) {
            throw new IOException("SimulatedDisk: power is off");
        }
    }

    /** One file's two layers; outlives handles, the way the page cache outlives a close. */
    private final class FileState {
        final Path path;
        final List<Pending> pending = new ArrayList<>();
        RandomAccessFile durable;

        FileState(Path path) throws IOException {
            this.path = path;
            this.durable = new RandomAccessFile(path.toFile(), "rw");
        }

        long size() throws IOException {
            long size = durable.length();
            for (Pending p : pending) {
                size = Math.max(size, p.end());
            }
            return size;
        }

        int read(long position, ByteBuffer dst) throws IOException {
            long size = size();
            if (position >= size) {
                return -1;
            }
            int n = (int) Math.min(dst.remaining(), size - position);
            byte[] buf = new byte[n];
            long durableLen = durable.length();
            if (position < durableLen) {
                durable.seek(position);
                durable.readFully(buf, 0, (int) Math.min(n, durableLen - position));
            }
            for (Pending p : pending) { // in write order, so later writes win
                long from = Math.max(position, p.position);
                long to = Math.min(position + n, p.end());
                if (from < to) {
                    System.arraycopy(p.bytes, (int) (from - p.position), buf, (int) (from - position), (int) (to - from));
                }
            }
            dst.put(buf);
            return n;
        }

        void write(long position, ByteBuffer src) {
            byte[] bytes = new byte[src.remaining()];
            src.get(bytes);
            pending.add(new Pending(position, bytes));
        }

        void force() throws IOException {
            for (Pending p : pending) {
                writeDurable(p.position, p.bytes, p.bytes.length);
            }
            pending.clear();
            durable.getChannel().force(true);
            forceCount++;
        }

        void writeDurable(long position, byte[] bytes, int length) throws IOException {
            if (length > 0) {
                durable.seek(position);
                durable.write(bytes, 0, length);
            }
        }

        void closeDurable() throws IOException {
            durable.close();
        }
    }

    private final class Handle implements FileIO {
        private final FileState state;

        Handle(FileState state) {
            this.state = state;
        }

        @Override
        public int read(long position, ByteBuffer dst) throws IOException {
            synchronized (SimulatedDisk.this) {
                checkPower();
                return state.read(position, dst);
            }
        }

        @Override
        public void write(long position, ByteBuffer src) throws IOException {
            synchronized (SimulatedDisk.this) {
                checkPower();
                state.write(position, src);
            }
        }

        @Override
        public long size() throws IOException {
            synchronized (SimulatedDisk.this) {
                checkPower();
                return state.size();
            }
        }

        @Override
        public void force() throws IOException {
            synchronized (SimulatedDisk.this) {
                checkPower();
                state.force();
            }
        }

        @Override
        public void close() {
            // Unforced writes stay in the volatile layer: closing a file does not sync it.
        }
    }
}
