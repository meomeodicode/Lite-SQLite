package lite.sqlite.events;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import lite.sqlite.events.api.EventEmitter;
import lite.sqlite.events.model.MutationEvent;

/**
 * Kafka publisher for mutation events with optional local fallback log.
 */
public class KafkaEventEmitter implements EventEmitter, AutoCloseable {
    private final KafkaProducer<String, String> producer;
    private final String topic;
    private final Path fallbackPath;
    private final AtomicLong sentCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);
    private final AtomicLong fallbackCount = new AtomicLong(0);

    public KafkaEventEmitter(Properties producerProperties, String topic, Path fallbackPath) {
        this(new KafkaProducer<>(producerProperties), topic, fallbackPath);
    }

    public KafkaEventEmitter(
        KafkaProducer<String, String> producer,
        String topic,
        Path fallbackPath
    ) {
        this.producer = Objects.requireNonNull(producer, "producer must not be null");
        this.topic = Objects.requireNonNull(topic, "topic must not be null");
        this.fallbackPath = fallbackPath;
    }

    @Override
    public void emit(MutationEvent event) throws IOException {
        String key = event.eventKey();
        String value = JsonEventSerializer.toJson(event.toMap());

        try {
            producer.send(new ProducerRecord<>(topic, key, value)).get();
            sentCount.incrementAndGet();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            failedCount.incrementAndGet();
            writeFallback(value, ex);
            throw new IOException("Interrupted while publishing event to Kafka", ex);
        } catch (ExecutionException ex) {
            failedCount.incrementAndGet();
            writeFallback(value, ex);
            throw new IOException("Failed to publish event to Kafka", ex);
        }
    }

    private void writeFallback(String jsonPayload, Exception cause) {
        if (fallbackPath == null) {
            return;
        }
        try {
            Path parent = fallbackPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String line = jsonPayload + System.lineSeparator();
            Files.writeString(
                fallbackPath,
                line,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
            );
            fallbackCount.incrementAndGet();
        } catch (IOException ioEx) {
            System.err.println("Failed to write Kafka fallback event: " + ioEx.getMessage());
        }
        System.err.println("Kafka publish failed, event written to fallback. Cause: " + cause.getMessage());
    }

    public long getSentCount() {
        return sentCount.get();
    }

    public long getFailedCount() {
        return failedCount.get();
    }

    public long getFallbackCount() {
        return fallbackCount.get();
    }

    @Override
    public void close() {
        producer.flush();
        producer.close();
    }
}
