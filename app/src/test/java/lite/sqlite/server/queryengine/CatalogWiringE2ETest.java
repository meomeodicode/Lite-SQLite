package lite.sqlite.server.queryengine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lite.sqlite.cli.TableDto;
import lite.sqlite.events.NoOpEventEmitter;
import lite.sqlite.server.catalog.Catalog;
import lite.sqlite.server.catalog.CatalogCorruptException;
import lite.sqlite.server.catalog.CatalogStore;
import lite.sqlite.server.storage.Page;


@DisplayName("Catalog wiring E2E")
class CatalogWiringE2ETest {

    @TempDir
    Path tempDir;

    private File dbDirectory;
    private Path catalogFile;
    private QueryEngineImpl engine;

    @BeforeEach
    void setUp() {
        dbDirectory = tempDir.resolve("database").toFile();
        catalogFile = dbDirectory.toPath().resolve("catalog.dat");
    }

    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.close();
        }
    }

    private Path tbl(String table) {
        return dbDirectory.toPath().resolve(table + ".tbl");
    }

    /** A store whose save always fails, standing in for a full disk or a failed rename. */
    private CatalogStore failingStore() {
        return new CatalogStore(dbDirectory.toPath()) {
            @Override
            public void save(Catalog catalog) throws IOException {
                throw new IOException("injected save failure");
            }
        };
    }

    @Test
    @DisplayName("C3: catalog entry without its .tbl fails closed and names the file")
    void missingTableFileFailsClosed() throws IOException {
        engine = new QueryEngineImpl(dbDirectory);
        assertNull(engine.doUpdate("CREATE TABLE customers (id INTEGER, name VARCHAR(50))").getErrorMessage());
        engine.close();
        engine = null;

        Files.delete(tbl("customers"));
        byte[] catalogBefore = Files.readAllBytes(catalogFile);

        RuntimeException e = assertThrows(RuntimeException.class, () -> new QueryEngineImpl(dbDirectory));

        assertTrue(e.getMessage().contains("customers.tbl"), "error should name the missing file: " + e.getMessage());
        assertFalse(Files.exists(tbl("customers")), "the missing .tbl must not be recreated");
        assertArrayEquals(catalogBefore, Files.readAllBytes(catalogFile), "catalog.dat must be unchanged");
    }

    @Test
    @DisplayName("C4: a flipped catalog byte fails closed; catalog.dat and .tbl bytes unchanged")
    void flippedCatalogByteFailsClosed() throws IOException {
        engine = new QueryEngineImpl(dbDirectory);
        assertNull(engine.doUpdate("CREATE TABLE customers (id INTEGER, name VARCHAR(50))").getErrorMessage());
        assertNull(engine.doUpdate("INSERT INTO customers (id, name) VALUES (1, 'alice')").getErrorMessage());
        engine.close();
        engine = null;

        byte[] tblBefore = Files.readAllBytes(tbl("customers"));

        // Flip the case of the 'c' in "customers" inside the payload. Header, length and
        // payload shape stay valid, so only the CRC can catch it. Without the CRC the engine
        // would load a table "Customers": on Windows (case-insensitive) it finds customers.tbl
        // and starts with no error; elsewhere it fails on a missing file, which the cause
        // check below tells apart from a CRC rejection.
        byte[] damaged = Files.readAllBytes(catalogFile);
        int at = indexOf(damaged, "customers".getBytes(StandardCharsets.UTF_8));
        assertTrue(at >= 0, "precondition: table name is stored in catalog.dat");
        damaged[at] ^= 0x20;
        Files.write(catalogFile, damaged);

        RuntimeException e = assertThrows(RuntimeException.class, () -> new QueryEngineImpl(dbDirectory));

        CatalogCorruptException cause = assertInstanceOf(CatalogCorruptException.class, e.getCause(),
                "the CRC should reject the catalog: " + e);
        assertTrue(cause.getMessage().contains("checksum"), cause.getMessage());
        assertArrayEquals(damaged, Files.readAllBytes(catalogFile), "damaged catalog.dat must be left as found");
        assertArrayEquals(tblBefore, Files.readAllBytes(tbl("customers")), ".tbl must be unchanged");
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    @Test
    @DisplayName("C5: a failed catalog save makes CREATE fail and the table stays absent, also after reopen")
    void failedSaveLeavesTableAbsent() {
        engine = new QueryEngineImpl(dbDirectory, new NoOpEventEmitter(), failingStore());

        TableDto create = engine.doUpdate("CREATE TABLE t1 (id INTEGER)");
        assertNotNull(create.getErrorMessage(), "CREATE must report the failed save");
        assertTrue(create.getErrorMessage().contains("Error creating table"), create.getErrorMessage());
        assertTableAbsent("t1");

        engine.close();
        engine = new QueryEngineImpl(dbDirectory, new NoOpEventEmitter(), failingStore());
        assertTableAbsent("t1");
    }

    @Test
    @DisplayName("C7: after a failed CREATE, the empty orphan is reused and the table has one page")
    void retryAfterFailedSaveReusesEmptyOrphan() throws IOException {
        engine = new QueryEngineImpl(dbDirectory, new NoOpEventEmitter(), failingStore());
        assertNotNull(engine.doUpdate("CREATE TABLE t1 (id INTEGER)").getErrorMessage());
        engine.close();
        assertTrue(Files.exists(tbl("t1")), "precondition: the failed CREATE left an orphan .tbl");

        engine = new QueryEngineImpl(dbDirectory);
        assertNull(engine.doUpdate("CREATE TABLE t1 (id INTEGER)").getErrorMessage(), "retry CREATE should succeed");

        TableDto select = engine.doQuery("SELECT * FROM t1");
        assertNull(select.getErrorMessage());
        assertEquals(List.of(), select.getRows());
        assertEquals(Page.PAGE_SIZE, Files.size(tbl("t1")), "file should be one page, not orphan + new page");
    }

    @Test
    @DisplayName("C8: an orphan .tbl holding rows is never reused by CREATE")
    void orphanWithRowsIsRefused() throws IOException {
        // Build a .tbl with real rows in a separate directory, then plant it as an orphan.
        File other = tempDir.resolve("other").toFile();
        QueryEngineImpl builder = new QueryEngineImpl(other);
        builder.doUpdate("CREATE TABLE customers (id INTEGER, name VARCHAR(50))");
        builder.doUpdate("INSERT INTO customers (id, name) VALUES (1, 'alice')");
        builder.close();
        Files.createDirectories(dbDirectory.toPath());
        Files.copy(other.toPath().resolve("customers.tbl"), tbl("customers"));
        byte[] before = Files.readAllBytes(tbl("customers"));

        engine = new QueryEngineImpl(dbDirectory);
        TableDto create = engine.doUpdate("CREATE TABLE customers (code INTEGER)");

        assertNotNull(create.getErrorMessage(), "CREATE over an orphan with rows must fail");
        assertTrue(create.getErrorMessage().contains("customers.tbl"), create.getErrorMessage());
        assertArrayEquals(before, Files.readAllBytes(tbl("customers")), "orphan bytes must be unchanged");
        assertTableAbsent("customers");
    }

    private void assertTableAbsent(String table) {
        TableDto select = engine.doQuery("SELECT * FROM " + table);
        assertNotNull(select.getErrorMessage(), "SELECT should say the table does not exist");
        assertTrue(select.getErrorMessage().contains("exist") && !select.getErrorMessage().contains("already"),
                select.getErrorMessage());
    }
}
