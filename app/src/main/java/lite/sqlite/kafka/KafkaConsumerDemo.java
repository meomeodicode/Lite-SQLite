package lite.sqlite.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import lite.sqlite.events.config.KafkaMutationConfig;

/**
 * Lightweight Kafka analytics runner for mutation events.
 */
public class KafkaConsumerDemo {
    public static void main(String[] args) {
        String bootstrapServers = args.length > 0 ? args[0] : KafkaMutationConfig.defaultBootstrapServers();
        String topic = args.length > 1 ? args[1] : KafkaMutationConfig.defaultTopic();
        String groupId = args.length > 2 ? args[2] : KafkaMutationConfig.defaultConsumerGroup();
        String offsetMode = args.length > 3 ? args[3] : KafkaMutationConfig.defaultConsumerOffsetReset();
        int pollCount = args.length > 4 ? parseIntOrDefault(args[4], KafkaMutationConfig.defaultConsumerPollCount())
            : KafkaMutationConfig.defaultConsumerPollCount();
        Duration pollTimeout = Duration.ofMillis(KafkaMutationConfig.defaultConsumerPollTimeoutMs());

        Properties props = KafkaMutationConfig.consumerProperties(bootstrapServers, groupId, offsetMode);

        Map<String, Integer> byType = new HashMap<>();
        Map<String, Integer> byTable = new HashMap<>();
        long total = 0;

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            for (int i = 0; i < pollCount; i++) {
                ConsumerRecords<String, String> records = consumer.poll(pollTimeout);
                for (ConsumerRecord<String, String> record : records) {
                    total++;
                    String eventType = extractJsonString(record.value(), "event_type", "UNKNOWN");
                    String table = extractJsonString(record.value(), "table", "UNKNOWN");
                    byType.merge(eventType, 1, Integer::sum);
                    byTable.merge(table, 1, Integer::sum);
                }
                consumer.commitSync();
            }
        }

        System.out.println("=== Kafka Consumer Demo Summary ===");
        System.out.println("Bootstrap servers: " + bootstrapServers);
        System.out.println("Topic: " + topic);
        System.out.println("Group: " + groupId);
        System.out.println("Offset mode: " + offsetMode);
        System.out.println("Total consumed: " + total);
        printMap("By event type", byType);
        printMap("By table", byTable);
    }

    private static void printMap(String title, Map<String, Integer> values) {
        System.out.println(title + ":");
        if (values.isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(values.entrySet());
        entries.sort(
            Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey)
        );
        for (Map.Entry<String, Integer> entry : entries) {
            System.out.printf("  %-24s %d%n", entry.getKey(), entry.getValue());
        }
    }

    private static String extractJsonString(String json, String key, String fallback) {
        String needle = "\"" + key + "\"";
        int keyIndex = json.indexOf(needle);
        if (keyIndex < 0) {
            return fallback;
        }
        int colon = json.indexOf(':', keyIndex + needle.length());
        if (colon < 0) {
            return fallback;
        }
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) {
            return fallback;
        }
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return fallback;
        }
        return json.substring(firstQuote + 1, secondQuote);
    }

    private static int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
