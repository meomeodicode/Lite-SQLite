package lite.sqlite.server.storage;

public class BlockId {
    private String filename;
    private int blockNum;

    BlockId (String filename, int blockNum) {
        this.filename = filename;
        this.blockNum = blockNum;
    }

    public String getFileName() {
        return filename;
    }

    public int getBlockNum() {
        return blockNum;
    } 

    @Override
    public boolean equals(Object anotherBlock) {
        BlockId blk = (BlockId) anotherBlock;
        return blk.getFileName().equals(this.filename) && blk.getBlockNum() == this.blockNum; 
    }

    public String toString() {
        return "[file " + filename + ", block " + blockNum + "]";
    }

    public int hashCode() {
        return toString().hashCode();
    }
}
