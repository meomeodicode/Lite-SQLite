package lite.sqlite.server.storage.io;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * One open file. Every byte the engine persists goes through this seam, so tests can swap the
 * disk for {@code SimulatedDisk} and cut the power (spec T0.2).
 *
 * <p>Contract: {@link #write} returns once the bytes are visible to later reads of this process,
 * not once they are durable. Only {@link #force} makes earlier writes survive a power loss; until
 * then each written page may land on disk, not land, or land torn at a 512-byte boundary.
 */
public interface FileIO extends Closeable {

    /** Reads from {@code position} into {@code dst} until it is full or the file ends. Returns bytes read, or -1 at end of file. */
    int read(long position, ByteBuffer dst) throws IOException;

    /** Writes all remaining bytes of {@code src} at {@code position}, growing the file if needed. */
    void write(long position, ByteBuffer src) throws IOException;

    /** Current length in bytes, including writes not yet forced. */
    long size() throws IOException;

    /** Makes every earlier write to this file durable (fsync). */
    void force() throws IOException;
}
