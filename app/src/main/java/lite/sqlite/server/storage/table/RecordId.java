package lite.sqlite.server.storage.table;

import lite.sqlite.server.storage.Block;

public class RecordId {
    private final Block blockId;
    private final int slotNumber;
    
    public RecordId(Block blockId, int slotNumber) {
        this.blockId = blockId;
        this.slotNumber = slotNumber;
    }
    
    public Block getBlockId() {
        return blockId;
    }
    
    public int getSlotNumber() {
        return slotNumber;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof RecordId)) return false;
        RecordId other = (RecordId) obj;
        return blockId.equals(other.blockId) && slotNumber == other.slotNumber;
    }
    
    @Override
    public int hashCode() {
        return 31 * blockId.hashCode() + slotNumber;
    }
    
    @Override
    public String toString() {
        return "RecordId(" + blockId + "," + slotNumber + ")";
    }
}