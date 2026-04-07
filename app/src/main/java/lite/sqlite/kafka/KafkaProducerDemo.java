package lite.sqlite.kafka;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import lite.sqlite.events.KafkaEventEmitter;
import lite.sqlite.events.model.MutationEvent;
import lite.sqlite.events.model.MutationRecordId;
import lite.sqlite.events.config.KafkaMutationConfig;

/**
 * Sends synthetic mutation events to Kafka to validate producer setup.
 */
public class KafkaProducerDemo {
    public static void main(String[] args) throws Exception {
        String bootstrapServers = args.length > 0 ? args[0] : KafkaMutationConfig.DEFAULT_BOOTSTRAP_SERVERS;
        String topic = args.length > 1 ? args[1] : KafkaMutationConfig.DEFAULT_TOPIC;
        int count = args.length > 2 ? parseIntOrDefault(args[2], 100) : 100;

        Properties props = KafkaMutationConfig.producerProperties(bootstrapServers);
        Path fallback = Path.of("event-log", "kafka-fallback.ndjson");

        try (KafkaEventEmitter emitter = new KafkaEventEmitter(props, topic, fallback)) {
            long started = System.nanoTime();
            for (int i = 0; i < count; i++) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("id", i + 1);
                payload.put("status", i % 3);
                payload.put("source", "KafkaProducerDemo");

                MutationEvent event = MutationEvent.forInsert(
                    "demo_orders",
                    new MutationRecordId(i / 50, i % 50),
                    payload
                );
                emitter.emit(event);
            }
            long elapsedNanos = System.nanoTime() - started;
            double elapsedMs = elapsedNanos / 1_000_000.0;
            double throughput = count / (elapsedNanos / 1_000_000_000.0);

            System.out.println("=== Kafka Producer Demo ===");
            System.out.println("Bootstrap servers: " + bootstrapServers);
            System.out.println("Topic: " + topic);
            System.out.println("Events attempted: " + count);
            System.out.println("Sent: " + emitter.getSentCount());
            System.out.println("Failed: " + emitter.getFailedCount());
            System.out.println("Fallback written: " + emitter.getFallbackCount());
            System.out.printf("Elapsed: %.2f ms | Throughput: %.2f events/sec%n", elapsedMs, throughput);
        }
    }

    private static int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
