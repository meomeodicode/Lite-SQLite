package lite.sqlite.server.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lite.sqlite.server.catalog.Catalog.ColumnDef;
import lite.sqlite.server.catalog.Catalog.IndexDef;
import lite.sqlite.server.catalog.Catalog.TableDef;
import lite.sqlite.server.storage.record.DataType;

class CatalogStoreTest {

    @TempDir
    Path dir;

    private static Catalog customersOnly() {
        return new Catalog(List.of(
                new TableDef("customers",
                        List.of(new ColumnDef("id", DataType.INTEGER, 0),
                                new ColumnDef("name", DataType.VARCHAR, 50)),
                        List.of(new IndexDef("idx_customers_id", "id", true, 4)))));
    }

    private static Catalog customersAndOrders() {
        return new Catalog(List.of(
                customersOnly().tables().get(0),
                new TableDef("orders",
                        List.of(new ColumnDef("order_id", DataType.INTEGER, 0),
                                new ColumnDef("customer_id", DataType.INTEGER, 0)),
                        List.of())));
    }

    @Test
    void firstRunWithNoFileLoadsEmptyCatalog() throws IOException {
        assertEquals(new Catalog(List.of()), new CatalogStore(dir).load());
    }

    @Test
    void saveThenLoadReturnsSameCatalog() throws IOException {
        CatalogStore store = new CatalogStore(dir);
        store.save(customersAndOrders());

        assertEquals(customersAndOrders(), new CatalogStore(dir).load());
    }

    @Test
    void halfWrittenTempFileIsIgnoredAndOldCatalogSurvives() throws IOException {
        new CatalogStore(dir).save(customersOnly());
        Files.write(dir.resolve("catalog.tmp"), new byte[] {1, 2, 3, 4, 5});

        assertEquals(customersOnly(), new CatalogStore(dir).load());

        new CatalogStore(dir).save(customersAndOrders());
        assertEquals(customersAndOrders(), new CatalogStore(dir).load());
    }

    @Test
    void crashBeforeRenameLeavesCompleteOldCatalog() throws IOException {
        new CatalogStore(dir).save(customersOnly());

        CatalogStore crashing = new CatalogStore(dir, () -> {
            throw new IllegalStateException("simulated power loss before rename");
        });
        assertThrows(IllegalStateException.class, () -> crashing.save(customersAndOrders()));

        assertEquals(customersOnly(), new CatalogStore(dir).load());
    }

    @Test
    void flippedByteIsDetectedNotSilentlyLoaded() throws IOException {
        new CatalogStore(dir).save(customersAndOrders());
        Path file = dir.resolve("catalog.dat");
        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length / 2] ^= 0x01;
        Files.write(file, bytes);

        assertThrows(CatalogCorruptException.class, () -> new CatalogStore(dir).load());
    }

    @Test
    void truncatedFileIsDetected() throws IOException {
        new CatalogStore(dir).save(customersAndOrders());
        Path file = dir.resolve("catalog.dat");
        byte[] bytes = Files.readAllBytes(file);
        Files.write(file, java.util.Arrays.copyOf(bytes, bytes.length - 3));

        assertThrows(CatalogCorruptException.class, () -> new CatalogStore(dir).load());
    }

    @Test
    void successfulSaveLeavesNoTempFileBehind() throws IOException {
        new CatalogStore(dir).save(customersOnly());

        assertTrue(Files.exists(dir.resolve("catalog.dat")));
        assertTrue(Files.notExists(dir.resolve("catalog.tmp")));
    }
}
