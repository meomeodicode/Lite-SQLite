package lite.sqlite.server.storage.filemanager;

import java.io.IOException;

import lite.sqlite.server.storage.Block;
import lite.sqlite.server.storage.Page;

public interface FileManager {
    void read(Block blockId, Page page) throws IOException;
    void write(Block blockId, Page page) throws IOException;
    Block append(String fileName) throws IOException;
    int getBlockCount(String fileName) throws IOException;
    void close() throws IOException; 
}
