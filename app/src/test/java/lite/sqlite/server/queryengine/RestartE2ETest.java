package lite.sqlite.server.queryengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lite.sqlite.cli.TableDto;

/**
 * End-to-end check for slice 2: tables created in one engine lifetime are usable
 * after a clean close and reopen on the same directory.
 */
@DisplayName("Restart E2E")
class RestartE2ETest {

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

    private void restart() {
        engine.close();
        engine = new QueryEngineImpl(dbDirectory);
    }

    @Test
    @DisplayName("Rows inserted before a restart are returned by SELECT after it")
    void rowsSurviveRestart() {
        assertNull(engine.doUpdate("CREATE TABLE customers (id INTEGER, name VARCHAR(50))").getErrorMessage());
        assertNull(engine.doUpdate("INSERT INTO customers (id, name) VALUES (1, 'alice')").getErrorMessage());
        assertNull(engine.doUpdate("INSERT INTO customers (id, name) VALUES (2, 'bob')").getErrorMessage());

        restart();

        TableDto result = engine.doQuery("SELECT * FROM customers");
        assertNull(result.getErrorMessage(), "SELECT after restart should succeed");
        assertEquals(List.of(List.of("1", "alice"), List.of("2", "bob")), result.getRows());
    }

    @Test
    @DisplayName("A table reopened after a restart accepts new rows next to the old ones")
    void insertAfterRestartAppendsToOldRows() {
        engine.doUpdate("CREATE TABLE customers (id INTEGER, name VARCHAR(50))");
        engine.doUpdate("INSERT INTO customers (id, name) VALUES (1, 'alice')");

        restart();

        assertNull(engine.doUpdate("INSERT INTO customers (id, name) VALUES (2, 'bob')").getErrorMessage(),
                "INSERT after restart should succeed");
        assertEquals(List.of(List.of("1", "alice"), List.of("2", "bob")),
                engine.doQuery("SELECT * FROM customers").getRows());
    }

    @Test
    @DisplayName("Two restarts in a row keep every table")
    void everyTableSurvivesTwoRestarts() {
        engine.doUpdate("CREATE TABLE customers (id INTEGER, name VARCHAR(50))");
        restart();
        assertNull(engine.doUpdate("CREATE TABLE orders (order_id INTEGER, customer_id INTEGER)").getErrorMessage());
        engine.doUpdate("INSERT INTO orders (order_id, customer_id) VALUES (10, 1)");

        restart();

        assertNull(engine.doQuery("SELECT * FROM customers").getErrorMessage(), "customers should still exist");
        assertEquals(List.of(List.of("10", "1")), engine.doQuery("SELECT * FROM orders").getRows());
    }
}
