package lite.sqlite.server.storage.io;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Opens files. A provider, not a single {@link FileIO}, because a simulated power loss must hit
 * every open file at the same instant.
 */
@FunctionalInterface
public interface FileIOProvider {

    /** Opens {@code file} for reading and writing, creating it if absent. */
    FileIO open(Path file) throws IOException;
}
