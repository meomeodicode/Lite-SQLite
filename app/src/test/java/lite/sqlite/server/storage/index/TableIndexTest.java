package lite.sqlite.server.storage.index;

import lite.sqlite.server.storage.Block;
import lite.sqlite.server.storage.table.RecordId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TableIndex Tests")
class TableIndexTest {

    @Test
    @DisplayName("Non-unique index should return all matching RecordIds")
    void testSearchAllForDuplicateKey() {
        TableIndex<Integer> index = new TableIndex<>("idx_orders_status", "orders", "status", false, 3);
        List<RecordId> expected = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            RecordId rid = rid(i, 0);
            if (i % 2 == 0) {
                index.insert(7, rid);
                expected.add(rid);
            } else {
                // Keep odd keys away from 7 so only the intended duplicates are tested.
                index.insert(100 + i, rid);
            }
        }

        List<RecordId> actual = index.searchAll(7);
        assertNotNull(actual, "searchAll should not return null");
        assertEquals(expected.size(), actual.size(), "searchAll should return all duplicates");
        assertEquals(new HashSet<>(expected), new HashSet<>(actual), "searchAll should return exactly the expected RecordIds");
    }

    @Test
    @DisplayName("Unique index should reject duplicate keys")
    void testUniqueIndexRejectsDuplicateKey() {
        TableIndex<Integer> uniqueIndex = new TableIndex<>("idx_users_id", "users", "id", true, 3);
        uniqueIndex.insert(10, rid(1, 1));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> uniqueIndex.insert(10, rid(1, 2)),
            "Unique index must reject duplicate keys"
        );
        assertTrue(ex.getMessage().contains("Duplicate key"), "Error should explain duplicate key");
    }

    private RecordId rid(int blockNum, int slotNum) {
        return new RecordId(new Block("test.tbl", blockNum), slotNum);
    }
}
