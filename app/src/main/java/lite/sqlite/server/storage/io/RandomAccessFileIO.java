package lite.sqlite.server.storage.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Production {@link FileIO}: a {@code RandomAccessFile} in {@code "rw"} mode. Writes go to the OS
 * page cache; durability comes only from {@link #force}. This replaces the old {@code "rws"} mode,
 * which synced every single write.
 */
public final class RandomAccessFileIO implements FileIO {

    /** Opens real files on the local file system. */
    public static final FileIOProvider PROVIDER = RandomAccessFileIO::new;

    private final RandomAccessFile file;
    private final FileChannel channel;

    public RandomAccessFileIO(Path path) throws IOException {
        this.file = new RandomAccessFile(path.toFile(), "rw");
        this.channel = file.getChannel();
    }

    @Override
    public int read(long position, ByteBuffer dst) throws IOException {
        int total = 0;
        while (dst.hasRemaining()) {
            int n = channel.read(dst, position + total);
            if (n < 0) {
                return total == 0 ? -1 : total;
            }
            total += n;
        }
        return total;
    }

    @Override
    public void write(long position, ByteBuffer src) throws IOException {
        long at = position;
        while (src.hasRemaining()) {
            at += channel.write(src, at);
        }
    }

    @Override
    public long size() throws IOException {
        return channel.size();
    }

    @Override
    public void force() throws IOException {
        channel.force(true);
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}
