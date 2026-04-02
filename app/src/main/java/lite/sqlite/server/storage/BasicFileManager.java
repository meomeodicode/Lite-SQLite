package lite.sqlite.server.storage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;

import lite.sqlite.server.storage.buffer.BufferPool;
import lite.sqlite.server.storage.filemanager.FileManager;
import lite.sqlite.server.storage.table.Table;

public class BasicFileManager implements FileManager {

    private final File dbDir;
    private ConcurrentHashMap<String, RandomAccessFile> openFiles;
    private final BufferPool bufferPool;

    public BasicFileManager(File dbDirectory) {
        this(dbDirectory, null);
    }

    public BasicFileManager(File dbDirectory, BufferPool bufferPool) {
        this.dbDir = dbDirectory;
        this.openFiles = new ConcurrentHashMap<>();
        this.bufferPool = bufferPool;

        if (!dbDirectory.exists()) {
            dbDirectory.mkdirs();
        }
        
        // Clean up temp files
        String[] files = dbDirectory.list();
        if (files != null) {
            for (String filename : files) {
                if (filename.startsWith("temp")) {
                    new File(dbDirectory, filename).delete();
                }
            }
        }
    }

    @Override 
    public synchronized void read(Block blockId, Page page) throws IOException {
        try {
            RandomAccessFile file = getFile(blockId.getFileName());
            long position = (long) blockId.getBlockNum() * Page.PAGE_SIZE; // Use Page.PAGE_SIZE!
            file.seek(position);
            
            page.contents().clear(); // Reset buffer position
            file.getChannel().read(page.contents());
            page.contents().rewind(); // Reset for reading
            
        } catch (IOException e) {
            throw new RuntimeException("Cannot read block " + blockId, e);
        }
    }

    @Override
    public synchronized void write(Block blockId, Page page) throws IOException {
        try {
            RandomAccessFile file = getFile(blockId.getFileName());
            long position = (long) blockId.getBlockNum() * Page.PAGE_SIZE; // Use Page.PAGE_SIZE!
            file.seek(position);
            
            page.contents().rewind(); // Reset buffer for writing
            file.getChannel().write(page.contents());            
        } catch (IOException e) {
            throw new RuntimeException("Cannot write block " + blockId, e);
        }
    }

    @Override
    public synchronized Block append(String fileName) throws IOException {
        try {
            int newBlockNum = getBlockCount(fileName); // Get current block count
            Block newBlock = new Block(fileName, newBlockNum);
            
            RandomAccessFile file = getFile(fileName);
            long position = (long) newBlockNum * Page.PAGE_SIZE; // Use PAGE_SIZE!
            file.seek(position);
            
            // Write full page of zeros
            byte[] emptyPage = new byte[Page.PAGE_SIZE]; // Fixed size!
            file.write(emptyPage);
            
            return newBlock;
            
        } catch (IOException e) {
            throw new RuntimeException("Cannot append to " + fileName, e);
        }
    }

    @Override
    public synchronized int getBlockCount(String fileName) throws IOException {
        try {
            RandomAccessFile file = getFile(fileName);
            long fileLength = file.length();
            return (int) Math.ceil((double) fileLength / Page.PAGE_SIZE);
            
        } catch (IOException e) {
            throw new RuntimeException("Cannot get file size for " + fileName, e);
        }
    }

    public File initializePhysicalTable(Table table) throws IOException {
        
        String fileName = table.getTableName() + ".tbl";
        File tableFile = new File(dbDir, fileName);
        if (!tableFile.exists()) {
            Files.createFile(tableFile.toPath());
        }

        Block block0 = append(fileName);
        if (bufferPool != null) {
            Page page = bufferPool.pinBlock(block0);
            bufferPool.unpinBlock(block0);
        }
        return tableFile;
    }

    @Override
    public void close() throws IOException {
        for (RandomAccessFile file : openFiles.values()) {
            file.close();
        }
        openFiles.clear();
    }

    private RandomAccessFile getFile(String filename) throws IOException {
        return openFiles.computeIfAbsent(filename, fn -> {
            try {
                File dbTable = new File(dbDir, fn);
                return new RandomAccessFile(dbTable, "rws");
            } catch (IOException e) {
                throw new RuntimeException("Cannot open file: " + fn, e);
            }
        });
    }
}