package lite.sqlite.benchmark;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lite.sqlite.cli.TableDto;
import lite.sqlite.server.queryengine.QueryEngineImpl;

public class IndexBenchmarkRunner {
    private static final String TABLE_NAME = "bench_orders";
    private static final String INDEX_NAME = "idx_bench_orders_status";
    private static final int STATUS_BUCKETS = 10;
    private static final int TARGET_STATUS = 7;

    public static void main(String[] args) {
        int rowCount = parseArg(args, 0, 20000);
        int warmup = parseArg(args, 1, 50);
        int iterations = parseArg(args, 2, 200);

        File dbDirectory = new File("app/benchmark-db");

        System.out.println("=== Lite SQLite Index Benchmark ===");
        System.out.println("Rows: " + rowCount);
        System.out.println("Warmup iterations: " + warmup);
        System.out.println("Measured iterations: " + iterations);

        deleteDirectory(dbDirectory.toPath());
        dbDirectory.mkdirs();

        QueryEngineImpl engine = new QueryEngineImpl(dbDirectory);
        try {
            setupData(engine, rowCount);

            String query = "SELECT id FROM " + TABLE_NAME + " WHERE status = " + TARGET_STATUS;
            int expectedMatches = rowCount / STATUS_BUCKETS;
            System.out.println("Query: " + query);
            System.out.println("Expected matching rows: ~" + expectedMatches);

            System.out.println("\nRunning full-scan baseline...");
            BenchmarkStats withoutIndex = measure(engine, query, warmup, iterations);

            TableDto createIndexResult = engine.doCreateIndex(
                "CREATE INDEX " + INDEX_NAME + " ON " + TABLE_NAME + "(status)"
            );
            if (isError(createIndexResult)) {
                throw new RuntimeException("Failed to create benchmark index: " + createIndexResult.getErrorMessage());
            }

            System.out.println("Running indexed lookup...");
            BenchmarkStats withIndex = measure(engine, query, warmup, iterations);

            printSummary(withoutIndex, withIndex);
        } finally {
            engine.close();
        }
    }

    private static void setupData(QueryEngineImpl engine, int rowCount) {
        TableDto createResult = engine.doUpdate(
            "CREATE TABLE " + TABLE_NAME + " (id INTEGER, status INTEGER, payload VARCHAR(100))"
        );
        if (isError(createResult)) {
            throw new RuntimeException("Failed to create benchmark table: " + createResult.getErrorMessage());
        }

        for (int i = 1; i <= rowCount; i++) {
            int status = i % STATUS_BUCKETS;
            String sql = String.format(
                "INSERT INTO %s (id, status, payload) VALUES (%d, %d, 'payload_%d')",
                TABLE_NAME,
                i,
                status,
                i
            );

            TableDto insertResult = engine.doUpdate(sql);
            if (isError(insertResult)) {
                throw new RuntimeException("Failed to insert benchmark row " + i + ": " + insertResult.getErrorMessage());
            }

            if (i % 5000 == 0) {
                System.out.println("Inserted rows: " + i);
            }
        }
    }

    private static BenchmarkStats measure(QueryEngineImpl engine, String query, int warmup, int iterations) {
        for (int i = 0; i < warmup; i++) {
            TableDto warmupResult = engine.doQuery(query);
            if (isError(warmupResult)) {
                throw new RuntimeException("Warmup query failed: " + warmupResult.getErrorMessage());
            }
        }

        List<Long> samplesNanos = new ArrayList<>(iterations);
        int resultCount = -1;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            TableDto result = engine.doQuery(query);
            long elapsed = System.nanoTime() - start;

            if (isError(result)) {
                throw new RuntimeException("Measured query failed: " + result.getErrorMessage());
            }

            if (resultCount < 0) {
                resultCount = result.getRows() == null ? 0 : result.getRows().size();
            }
            samplesNanos.add(elapsed);
        }

        return new BenchmarkStats(samplesNanos, resultCount);
    }

    private static void printSummary(BenchmarkStats withoutIndex, BenchmarkStats withIndex) {
        double withoutMedianMs = nanosToMillis(withoutIndex.medianNanos());
        double withoutP95Ms = nanosToMillis(withoutIndex.p95Nanos());

        double withMedianMs = nanosToMillis(withIndex.medianNanos());
        double withP95Ms = nanosToMillis(withIndex.p95Nanos());

        double medianSpeedup = withoutMedianMs / withMedianMs;
        double p95Speedup = withoutP95Ms / withP95Ms;

        System.out.println("\n=== Benchmark Results ===");
        System.out.println("Rows returned: " + withIndex.rowsReturned);
        System.out.printf("No index  - median: %.3f ms, p95: %.3f ms%n", withoutMedianMs, withoutP95Ms);
        System.out.printf("With index- median: %.3f ms, p95: %.3f ms%n", withMedianMs, withP95Ms);
        System.out.printf("Speedup   - median: %.2fx, p95: %.2fx%n", medianSpeedup, p95Speedup);
    }

    private static boolean isError(TableDto dto) {
        return dto != null && dto.getErrorMessage() != null;
    }

    private static int parseArg(String[] args, int index, int defaultValue) {
        if (args.length <= index) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static void deleteDirectory(Path path) {
        if (!Files.exists(path)) {
            return;
        }

        try {
            Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed deleting benchmark path: " + p, e);
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException("Failed cleaning benchmark directory: " + path, e);
        }
    }

    private static final class BenchmarkStats {
        private final List<Long> samples;
        private final int rowsReturned;

        private BenchmarkStats(List<Long> samples, int rowsReturned) {
            this.samples = new ArrayList<>(samples);
            Collections.sort(this.samples);
            this.rowsReturned = rowsReturned;
        }

        private long medianNanos() {
            int size = samples.size();
            int mid = size / 2;
            if (size % 2 == 1) {
                return samples.get(mid);
            }
            return (samples.get(mid - 1) + samples.get(mid)) / 2;
        }

        private long p95Nanos() {
            int size = samples.size();
            int idx = (int) Math.ceil(size * 0.95) - 1;
            idx = Math.max(0, Math.min(idx, size - 1));
            return samples.get(idx);
        }
    }
}
