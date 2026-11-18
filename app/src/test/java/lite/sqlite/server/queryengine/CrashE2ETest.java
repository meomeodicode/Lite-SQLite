package lite.sqlite.server.queryengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lite.sqlite.server.catalog.CatalogStore;
import lite.sqlite.events.NoOpEventEmitter;
import lite.sqlite.server.storage.Page;
import lite.sqlite.testing.SimulatedDisk;

/**
 * T0.2 E2E: a statement that returned is still there after a power loss. The crashed engine is
 * never closed (close would flush); the reopen uses the real disk and sees only what was forced.
 */
@DisplayName("Crash E2E (SimulatedDisk)")
class CrashE2ETest {

    @TempDir
    Path tempDir;

    private File dbDirectory;
    private SimulatedDisk disk;
    private QueryEngineImpl reopened;

    @BeforeEach
    void setUp() {
        dbDirectory = tempDir.resolve("database").toFile();
        disk = new SimulatedDisk();
    }

    @AfterEach
    void tearDown() throws Exception {
        disk.crash(); // releases the durable file handles if a test failed before crashing
        if (reopened != null) {
            reopened.close();
        }
    }

    private QueryEngineImpl onSimulatedDisk() {
        return new QueryEngineImpl(dbDirectory, new NoOpEventEmitter(), new CatalogStore(dbDirectory.toPath()), disk);
    }

    private QueryEngineImpl powerLossAndReopen() throws Exception {
        disk.crash();
        reopened = new QueryEngineImpl(dbDirectory);
        return reopened;
    }

    @Test
    @DisplayName("CREATE TABLE: the table file holds its first page after a power loss")
    void createdTableFileSurvives() throws Exception {
        QueryEngineImpl engine = onSimulatedDisk();
        assertNull(engine.doUpdate("CREATE TABLE t (id INTEGER, name VARCHAR(20))").getErrorMessage());

        powerLossAndReopen();

        assertEquals(Page.PAGE_SIZE, Files.size(dbDirectory.toPath().resolve("t.tbl")),
                "catalog was saved but the table's first page was not forced before it");
    }

    @Test
    @DisplayName("INSERT that returned survives a power loss")
    void insertedRowSurvives() throws Exception {
        QueryEngineImpl engine = onSimulatedDisk();
        assertNull(engine.doUpdate("CREATE TABLE t (id INTEGER, name VARCHAR(20))").getErrorMessage());
        assertNull(engine.doUpdate("INSERT INTO t (id, name) VALUES (1, 'alice')").getErrorMessage());
        assertNull(engine.doUpdate("INSERT INTO t (id, name) VALUES (2, 'bob')").getErrorMessage());

        QueryEngineImpl after = powerLossAndReopen();

        assertEquals(List.of(List.of("1", "alice"), List.of("2", "bob")), after.doQuery("SELECT * FROM t").getRows());
    }

    @Test
    @DisplayName("DELETE that returned survives a power loss")
    void deleteSurvives() throws Exception {
        QueryEngineImpl engine = onSimulatedDisk();
        assertNull(engine.doUpdate("CREATE TABLE t (id INTEGER, name VARCHAR(20))").getErrorMessage());
        assertNull(engine.doUpdate("INSERT INTO t (id, name) VALUES (1, 'alice')").getErrorMessage());
        assertNull(engine.doUpdate("INSERT INTO t (id, name) VALUES (2, 'bob')").getErrorMessage());
        assertNull(engine.doUpdate("DELETE FROM t WHERE id = 1").getErrorMessage());

        QueryEngineImpl after = powerLossAndReopen();

        assertEquals(List.of(List.of("2", "bob")), after.doQuery("SELECT * FROM t").getRows());
    }
}
