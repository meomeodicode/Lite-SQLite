package lite.sqlite.analytics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight analytics consumer for append-only event logs.
 *
 * Expected format is JSON-lines with keys like:
 * - event_type or type
 * - table
 * - operation
 */
public class EventLogAnalyticsRunner {
    private static final String DEFAULT_LOG_PATH = "event-log/events.ndjson";

    public static void main(String[] args) {
        String logPath = args.length > 0 ? args[0] : DEFAULT_LOG_PATH;
        String command = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "summary";
        String commandArg = args.length > 2 ? args[2] : "";

        Path path = Paths.get(logPath);
        if (!Files.exists(path)) {
            System.out.println("Event log not found: " + path.toAbsolutePath());
            System.out.println("Usage: runAnalytics -PlogPath=<path> -Pcmd=summary|table|tail [-PcmdArg=<arg>]");
            return;
        }

        try {
            switch (command) {
                case "summary":
                    printSummary(path);
                    break;
                case "table":
                    if (commandArg.isBlank()) {
                        System.out.println("Missing table name. Example: -Pcmd=table -PcmdArg=orders");
                        return;
                    }
                    printTableSummary(path, commandArg);
                    break;
                case "tail":
                    int n = parseIntOrDefault(commandArg, 10);
                    printTail(path, n);
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    System.out.println("Supported commands: summary, table, tail");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to process event log: " + path, e);
        }
    }

    private static void printSummary(Path path) throws IOException {
        long total = 0;
        Map<String, Integer> byType = new HashMap<>();
        Map<String, Integer> byTable = new HashMap<>();

        for (String line : Files.readAllLines(path)) {
            if (line == null || line.isBlank()) {
                continue;
            }
            total++;

            String type = firstNonBlank(
                extractJsonString(line, "event_type"),
                extractJsonString(line, "type"),
                extractJsonString(line, "operation"),
                "UNKNOWN"
            );
            String table = firstNonBlank(
                extractJsonString(line, "table"),
                extractJsonString(line, "aggregate_type"),
                "UNKNOWN"
            );

            byType.merge(type, 1, Integer::sum);
            byTable.merge(table, 1, Integer::sum);
        }

        System.out.println("=== Event Analytics Summary ===");
        System.out.println("Log file: " + path.toAbsolutePath());
        System.out.println("Total events: " + total);
        printMap("By event type", byType);
        printMap("By table", byTable);
    }

    private static void printTableSummary(Path path, String tableName) throws IOException {
        long total = 0;
        Map<String, Integer> byType = new HashMap<>();

        for (String line : Files.readAllLines(path)) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String table = firstNonBlank(
                extractJsonString(line, "table"),
                extractJsonString(line, "aggregate_type"),
                "UNKNOWN"
            );
            if (!tableName.equalsIgnoreCase(table)) {
                continue;
            }

            total++;
            String type = firstNonBlank(
                extractJsonString(line, "event_type"),
                extractJsonString(line, "type"),
                extractJsonString(line, "operation"),
                "UNKNOWN"
            );
            byType.merge(type, 1, Integer::sum);
        }

        System.out.println("=== Table Event Summary ===");
        System.out.println("Log file: " + path.toAbsolutePath());
        System.out.println("Table: " + tableName);
        System.out.println("Total events: " + total);
        printMap("By event type", byType);
    }

    private static void printTail(Path path, int n) throws IOException {
        Deque<String> tail = new ArrayDeque<>(Math.max(n, 1));

        for (String line : Files.readAllLines(path)) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (tail.size() == n) {
                tail.removeFirst();
            }
            tail.addLast(line);
        }

        System.out.println("=== Last " + n + " Events ===");
        System.out.println("Log file: " + path.toAbsolutePath());

        int index = 1;
        for (String line : tail) {
            String ts = firstNonBlank(extractJsonString(line, "ts"), extractJsonString(line, "timestamp"), "-");
            String table = firstNonBlank(extractJsonString(line, "table"), extractJsonString(line, "aggregate_type"), "UNKNOWN");
            String type = firstNonBlank(
                extractJsonString(line, "event_type"),
                extractJsonString(line, "type"),
                extractJsonString(line, "operation"),
                "UNKNOWN"
            );
            System.out.printf("%d. %s | %s | %s%n", index++, ts, table, type);
        }
    }

    private static void printMap(String title, Map<String, Integer> values) {
        System.out.println(title + ":");
        if (values.isEmpty()) {
            System.out.println("  (none)");
            return;
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(values.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
            .reversed()
            .thenComparing(Map.Entry::getKey));

        for (Map.Entry<String, Integer> entry : entries) {
            System.out.printf("  %-24s %d%n", entry.getKey(), entry.getValue());
        }
    }

    private static String extractJsonString(String line, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static int parseIntOrDefault(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
