package lite.sqlite.server.queryengine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lite.sqlite.cli.TableDto;
import lite.sqlite.events.NoOpEventEmitter;
import lite.sqlite.server.catalog.Catalog;
import lite.sqlite.server.catalog.Catalog.IndexDef;
import lite.sqlite.server.catalog.Catalog.TableDef;
import lite.sqlite.server.catalog.CatalogStore;
import lite.sqlite.server.storage.index.TableIndex;

/**
 * Slice 3 (T0.1): index definitions persist in the catalog, B+trees are rebuilt from the
 * heap on open, and a unique violation during any index build fails closed (D-010).
 */
@DisplayName("Index restart E2E")
class IndexRestartE2ETest {

    private static final String CREATE_USERS = "CREATE TABLE users (id INTEGER, email VARCHAR(50))";

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

    private void restart() {
        engine.close();
        engine = new QueryEngineImpl(dbDirectory);
    }

    private void ok(TableDto result) {
        assertNull(result.getErrorMessage(), "expected success, got: " + result.getErrorMessage());
    }

    private void insertUser(int id, String email) {
        ok(engine.doUpdate("INSERT INTO users (id, email) VALUES (" + id + ", '" + email + "')"));
    }

    private Path tbl(String table) {
        return dbDirectory.toPath().resolve(table + ".tbl");
    }

    /** Rewrites catalog.dat with one more index on {@code table}, as if CREATE INDEX had saved it. */
    private void addIndexToCatalog(String table, IndexDef index) throws IOException {
        CatalogStore store = new CatalogStore(dbDirectory.toPath());
        List<TableDef> defs = new ArrayList<>();
        for (TableDef def : store.load().tables()) {
            if (def.name().equals(table)) {
                List<IndexDef> indexes = new ArrayList<>(def.indexes());
                indexes.add(index);
                def = new TableDef(def.name(), def.columns(), List.copyOf(indexes));
            }
            defs.add(def);
        }
        store.save(new Catalog(defs));
    }

    /** A store whose saves can be switched to fail, standing in for a full disk mid-session. */
    private static final class SwitchableStore extends CatalogStore {
        boolean failSaves;

        SwitchableStore(Path dir) {
            super(dir);
        }

        @Override
        public void save(Catalog catalog) throws IOException {
            if (failSaves) {
                throw new IOException("injected save failure");
            }
            super.save(catalog);
        }
    }

    @Test
    @DisplayName("C9: a UNIQUE index still rejects duplicates after a restart")
    void uniqueIndexSurvivesRestart() {
        engine = new QueryEngineImpl(dbDirectory);
        ok(engine.doUpdate(CREATE_USERS));
        insertUser(1, "a@x.com");
        ok(engine.doCreateIndex("CREATE UNIQUE INDEX idx_email ON users (email)"));

        restart();

        TableDto duplicate = engine.doUpdate("INSERT INTO users (id, email) VALUES (2, 'a@x.com')");
        assertNotNull(duplicate.getErrorMessage(), "duplicate INSERT after restart must be rejected");
        assertTrue(duplicate.getErrorMessage().contains("Duplicate key"), duplicate.getErrorMessage());
        assertEquals(List.of(List.of("1", "a@x.com")), engine.doQuery("SELECT * FROM users").getRows());
    }

    @Test
    @DisplayName("C9: a later CREATE TABLE does not drop index definitions from the catalog")
    void laterCreateTableKeepsIndexDefinitions() {
        engine = new QueryEngineImpl(dbDirectory);
        ok(engine.doUpdate(CREATE_USERS));
        insertUser(1, "a@x.com");
        ok(engine.doCreateIndex("CREATE UNIQUE INDEX idx_email ON users (email)"));
        ok(engine.doUpdate("CREATE TABLE orders (id INTEGER)"));

        restart();

        TableDto duplicate = engine.doUpdate("INSERT INTO users (id, email) VALUES (2, 'a@x.com')");
        assertNotNull(duplicate.getErrorMessage(), "CREATE TABLE orders must not have erased idx_email");
        assertTrue(duplicate.getErrorMessage().contains("Duplicate key"), duplicate.getErrorMessage());
    }

    @Test
    @DisplayName("C10: the rebuilt index holds every pre-restart row and keeps up with new ones")
    void indexRebuiltFromHeapOnOpen() {
        engine = new QueryEngineImpl(dbDirectory);
        ok(engine.doUpdate(CREATE_USERS));
        insertUser(1, "alice@x.com");
        insertUser(2, "bob@x.com");
        insertUser(3, "bob@x.com");
        ok(engine.doCreateIndex("CREATE INDEX idx_email ON users (email)"));

        restart();

        // Without the index a full scan also gives the right rows, so check the index exists.
        TableIndex<?> index = engine.tableForTest("users").findIndexForColumn("email");
        assertNotNull(index, "idx_email should be back after restart");
        assertEquals("idx_email", index.getIndexName());

        // An index that exists but was not populated returns nothing here (non-unique path).
        // No ORDER BY, so row order is not part of the contract: compare as a set.
        assertEquals(java.util.Set.of(List.of("2", "bob@x.com"), List.of("3", "bob@x.com")),
                new java.util.HashSet<>(engine.doQuery("SELECT * FROM users WHERE email = 'bob@x.com'").getRows()));

        insertUser(4, "bob@x.com");
        assertEquals(3, engine.doQuery("SELECT * FROM users WHERE email = 'bob@x.com'").getRows().size(),
                "rows inserted after the rebuild must reach the index too");
    }

    @Test
    @DisplayName("C11: the rebuilt index uses the maxDegree stored in the catalog")
    void maxDegreeComesFromCatalog() throws IOException {
        engine = new QueryEngineImpl(dbDirectory);
        ok(engine.doUpdate(CREATE_USERS));
        for (int i = 1; i <= 20; i++) {
            insertUser(i, "u" + i + "@x.com");
        }
        engine.close();
        engine = null;

        addIndexToCatalog("users", new IndexDef("idx_email", "email", false, 7));
        engine = new QueryEngineImpl(dbDirectory);

        TableIndex<?> index = engine.tableForTest("users").findIndexForColumn("email");
        assertNotNull(index, "idx_email from the catalog should be loaded");
        assertEquals(7, index.getMaxDegree(), "maxDegree must come from the catalog, not the hard-coded 100");
        // Degree 7 with 20 keys forces splits, so the lookup goes through a multi-level tree.
        assertEquals(List.of(List.of("13", "u13@x.com")),
                engine.doQuery("SELECT * FROM users WHERE email = 'u13@x.com'").getRows());
    }

    @Test
    @DisplayName("C12: CREATE UNIQUE INDEX over existing duplicates fails and saves nothing")
    void uniqueIndexOverDuplicatesFails() throws IOException {
        engine = new QueryEngineImpl(dbDirectory);
        ok(engine.doUpdate(CREATE_USERS));
        insertUser(1, "a@x.com");
        insertUser(2, "a@x.com");
        byte[] catalogBefore = Files.readAllBytes(catalogFile);

        TableDto create = engine.doCreateIndex("CREATE UNIQUE INDEX idx_email ON users (email)");

        assertNotNull(create.getErrorMessage(), "a unique index cannot be built over duplicates");
        assertTrue(create.getErrorMessage().contains("a@x.com"), "error should name the key: " + create.getErrorMessage());
        assertArrayEquals(catalogBefore, Files.readAllBytes(catalogFile), "catalog.dat must be unchanged");
        assertNull(engine.tableForTest("users").findIndexForColumn("email"), "no half-built index in memory");
        insertUser(3, "a@x.com");

        restart();
        assertEquals(3, engine.doQuery("SELECT * FROM users").getRows().size());
    }

    @Test
    @DisplayName("C13: open fails closed when a unique index meets duplicate rows; files unchanged")
    void openFailsClosedOnUniqueViolation() throws IOException {
        engine = new QueryEngineImpl(dbDirectory);
        ok(engine.doUpdate(CREATE_USERS));
        insertUser(1, "a@x.com");
        insertUser(2, "a@x.com");
        engine.close();
        engine = null;

        // The catalog says unique while the heap holds duplicates: the state the UPDATE gap
        // (deferred to P1, D-010) can leave behind.
        addIndexToCatalog("users", new IndexDef("idx_email", "email", true, 100));
        byte[] catalogBefore = Files.readAllBytes(catalogFile);
        byte[] tblBefore = Files.readAllBytes(tbl("users"));

        RuntimeException e = assertThrows(RuntimeException.class, () -> new QueryEngineImpl(dbDirectory));

        String message = fullMessage(e);
        assertTrue(message.contains("idx_email"), "error should name the index: " + message);
        assertTrue(message.contains("a@x.com"), "error should name the key: " + message);
        assertArrayEquals(catalogBefore, Files.readAllBytes(catalogFile), "catalog.dat must be unchanged");
        assertArrayEquals(tblBefore, Files.readAllBytes(tbl("users")), "users.tbl must be unchanged");
        // The failed open must release its file handles, or the user cannot repair the file
        // (and on Windows @TempDir cleanup fails). On Windows this delete fails if users.tbl is still open.
        Files.delete(tbl("users"));
    }

    @Test
    @DisplayName("C13 (D-010): a clean open with indexes writes nothing to catalog.dat")
    void cleanOpenWritesNothing() throws IOException {
        engine = new QueryEngineImpl(dbDirectory);
        ok(engine.doUpdate(CREATE_USERS));
        insertUser(1, "a@x.com");
        ok(engine.doCreateIndex("CREATE UNIQUE INDEX idx_email ON users (email)"));
        engine.close();
        byte[] catalogBefore = Files.readAllBytes(catalogFile);

        engine = new QueryEngineImpl(dbDirectory);
        engine.close();
        engine = null;

        assertArrayEquals(catalogBefore, Files.readAllBytes(catalogFile), "opening must only read the catalog");
    }

    @Test
    @DisplayName("C9/C10: every index on every table comes back, unique and non-unique")
    void allIndexesOnAllTablesSurviveRestart() {
        engine = new QueryEngineImpl(dbDirectory);
        ok(engine.doUpdate(CREATE_USERS));
        ok(engine.doUpdate("CREATE TABLE orders (id INTEGER, status VARCHAR(20))"));
        insertUser(1, "a@x.com");
        insertUser(2, "b@x.com");
        ok(engine.doUpdate("INSERT INTO orders (id, status) VALUES (10, 'open')"));
        ok(engine.doUpdate("INSERT INTO orders (id, status) VALUES (11, 'open')"));
        ok(engine.doCreateIndex("CREATE UNIQUE INDEX idx_email ON users (email)"));
        ok(engine.doCreateIndex("CREATE INDEX idx_user_id ON users (id)"));
        ok(engine.doCreateIndex("CREATE INDEX idx_status ON orders (status)"));

        restart();

        assertEquals(2, engine.tableForTest("users").getIndexes().size(), "both users indexes, once each");
        assertEquals(1, engine.tableForTest("orders").getIndexes().size());
        assertNotNull(engine.doUpdate("INSERT INTO users (id, email) VALUES (3, 'b@x.com')").getErrorMessage(),
                "idx_email (second-listed table index or not) must still be unique");
        assertEquals(List.of(List.of("2", "b@x.com")), engine.doQuery("SELECT * FROM users WHERE id = 2").getRows());
        assertEquals(2, engine.doQuery("SELECT * FROM orders WHERE status = 'open'").getRows().size());
    }

    @Test
    @DisplayName("C12: Table.buildIndex itself rejects duplicates for a unique index")
    void buildIndexRejectsDuplicates() {
        engine = new QueryEngineImpl(dbDirectory);
        ok(engine.doUpdate(CREATE_USERS));
        insertUser(1, "a@x.com");
        insertUser(2, "a@x.com");

        // Pins the fix inside populateIndex: a duplicate pre-check in the engine alone would
        // leave rebuildIndexes() (after UPDATE/DELETE) still swallowing violations.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> engine.tableForTest("users").buildIndex("idx_email", "email", true, 100));
        assertTrue(e.getMessage().contains("a@x.com"), e.getMessage());
    }

    @Test
    @DisplayName("Guard: UPDATE and DELETE on an indexed table keep lookups and uniqueness right")
    void updateAndDeleteKeepIndexesUsable() {
        engine = new QueryEngineImpl(dbDirectory);
        ok(engine.doUpdate(CREATE_USERS));
        insertUser(1, "a@x.com");
        insertUser(2, "b@x.com");
        insertUser(3, "c@x.com");
        ok(engine.doCreateIndex("CREATE UNIQUE INDEX idx_email ON users (email)"));

        ok(engine.doUpdate("DELETE FROM users WHERE id = 2"));
        ok(engine.doUpdate("UPDATE users SET email = 'd@x.com' WHERE id = 3"));

        assertEquals(List.of(), engine.doQuery("SELECT * FROM users WHERE email = 'b@x.com'").getRows());
        assertEquals(List.of(), engine.doQuery("SELECT * FROM users WHERE email = 'c@x.com'").getRows());
        assertEquals(List.of(List.of("3", "d@x.com")), engine.doQuery("SELECT * FROM users WHERE email = 'd@x.com'").getRows());
        assertNotNull(engine.doUpdate("INSERT INTO users (id, email) VALUES (4, 'a@x.com')").getErrorMessage(),
                "uniqueness must hold after the rebuild");
        insertUser(5, "b@x.com");
        insertUser(6, "c@x.com");

        restart();
        assertNotNull(engine.doUpdate("INSERT INTO users (id, email) VALUES (7, 'd@x.com')").getErrorMessage(),
                "and after a restart");
    }

    @Test
    @DisplayName("C14: a failed catalog save makes CREATE INDEX fail with no index in memory or on disk")
    void failedSaveLeavesNoIndex() {
        SwitchableStore store = new SwitchableStore(dbDirectory.toPath());
        engine = new QueryEngineImpl(dbDirectory, new NoOpEventEmitter(), store);
        ok(engine.doUpdate(CREATE_USERS));
        insertUser(1, "a@x.com");

        store.failSaves = true;
        TableDto create = engine.doCreateIndex("CREATE UNIQUE INDEX idx_email ON users (email)");

        assertNotNull(create.getErrorMessage(), "CREATE INDEX must report the failed save");
        assertTrue(engine.tableForTest("users").getIndexes().isEmpty(), "index must not be published before the save");
        insertUser(2, "a@x.com");

        store.failSaves = false;
        restart();
        assertNull(engine.tableForTest("users").findIndexForColumn("email"), "nothing about idx_email reached disk");
    }

    private static String fullMessage(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause()) {
            sb.append(t.getMessage()).append(" | ");
        }
        return sb.toString();
    }
}
