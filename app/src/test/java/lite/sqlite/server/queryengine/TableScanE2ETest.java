package lite.sqlite.server.queryengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lite.sqlite.cli.TableDto;
import lite.sqlite.server.storage.Page;

/**
 * Slice 2.5: the full-table scan must visit every block, not stop at the first empty one
 * (docs/learning/comprehension-slice2.md gotcha 6, Table.java TableIterator).
 */
@DisplayName("Table scan E2E")
class TableScanE2ETest {

    private static final String PAD = "x".repeat(200);

    @TempDir
    Path tempDir;

    private File dbDirectory;
    private QueryEngineImpl engine;

    @BeforeEach
    void setUp() {
        dbDirectory = tempDir.resolve("database").toFile();
        engine = new QueryEngineImpl(dbDirectory);
    }

    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    @DisplayName("rows in block 1 stay visible after DELETE empties block 0")
    void scanSkipsEmptiedBlock() throws IOException {
        assertNull(engine.doUpdate("CREATE TABLE t (id INTEGER, pad VARCHAR(255))").getErrorMessage());
        Path tbl = dbDirectory.toPath().resolve("t.tbl");

        // Insert until the table spills into block 1; remember how many rows block 0 holds.
        int rowsInBlock0 = -1;
        int total = 0;
        while (total < 1000) {
            total++;
            assertNull(engine.doUpdate("INSERT INTO t (id, pad) VALUES (" + total + ", '" + PAD + "')").getErrorMessage());
            if (rowsInBlock0 < 0 && Files.size(tbl) > Page.PAGE_SIZE) {
                rowsInBlock0 = total - 1;
            }
            if (rowsInBlock0 > 0 && total == 2 * rowsInBlock0) {
                break;
            }
        }
        assertTrue(rowsInBlock0 > 0, "precondition: table must span two blocks");
        assertEquals(total, engine.doQuery("SELECT * FROM t").getRows().size(), "precondition: full scan before DELETE");

        // Empty block 0 only.
        for (int id = 1; id <= rowsInBlock0; id++) {
            TableDto delete = engine.doUpdate("DELETE FROM t WHERE id = " + id);
            assertNull(delete.getErrorMessage(), "DELETE id=" + id);
        }

        TableDto select = engine.doQuery("SELECT * FROM t");
        assertNull(select.getErrorMessage());
        assertEquals(total - rowsInBlock0, select.getRows().size(),
                "rows in block 1 must still be returned after block 0 is emptied");
    }

    @Test
    @DisplayName("a damaged block makes SELECT return an error, not a partial or empty result")
    void damagedBlockFailsLoudly() throws IOException {
        assertNull(engine.doUpdate("CREATE TABLE t (id INTEGER, pad VARCHAR(255))").getErrorMessage());
        Path tbl = dbDirectory.toPath().resolve("t.tbl");
        int total = 0;
        while (Files.size(tbl) <= Page.PAGE_SIZE) {
            total++;
            assertNull(engine.doUpdate("INSERT INTO t (id, pad) VALUES (" + total + ", '" + PAD + "')").getErrorMessage());
        }
        engine.close();

        // Damage block 1: point its first slot past the end of the page.
        // Slot directory starts after the 32-byte page header; each slot is (offset:int, length:int).
        byte[] bytes = Files.readAllBytes(tbl);
        ByteBuffer.wrap(bytes).putInt(Page.PAGE_SIZE + 32, Page.PAGE_SIZE + 1000);
        Files.write(tbl, bytes);

        engine = new QueryEngineImpl(dbDirectory);
        TableDto select = engine.doQuery("SELECT * FROM t");

        assertNotNull(select.getErrorMessage(), "a damaged block must surface as an error, got "
                + (select.getRows() == null ? "no rows" : select.getRows().size() + " rows"));
        assertTrue(select.getErrorMessage().contains("t.tbl"), "error should name the file: " + select.getErrorMessage());
    }
}
