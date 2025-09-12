package lite.sqlite.server.storage.filemanager;

import java.io.IOException;

import lite.sqlite.server.storage.BlockId;
import lite.sqlite.server.storage.Page;

public interface FileManager {
    void read(BlockId blockId, Page page) throws IOException;
    void write(BlockId blockId, Page page) throws IOException;
    BlockId append(String fileName) throws IOException;
    int getBlockCount(String fileName) throws IOException;
    void close() throws IOException;
}
