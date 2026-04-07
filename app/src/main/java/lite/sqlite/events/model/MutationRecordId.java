package lite.sqlite.events.model;

import java.util.LinkedHashMap;
import java.util.Map;

import lite.sqlite.server.storage.table.RecordId;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Serializable view of a physical record location.
 */
@Getter
@AllArgsConstructor
public class MutationRecordId {
    private final int block;
    private final int slot;

    public static MutationRecordId from(RecordId recordId) {
        return new MutationRecordId(
            recordId.getBlockId().getBlockNum(),
            recordId.getSlotNumber()
        );
    }

    public String toEventKey(String table) {
        return table + ":" + block + ":" + slot;
    }

    public Map<String, Object> toJsonMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("block", block);
        map.put("slot", slot);
        return map;
    }
}
