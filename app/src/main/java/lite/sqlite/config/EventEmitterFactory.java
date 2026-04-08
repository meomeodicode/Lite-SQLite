package lite.sqlite.config;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import lite.sqlite.events.KafkaEventEmitter;
import lite.sqlite.events.LoggingEventEmitter;
import lite.sqlite.events.NoOpEventEmitter;
import lite.sqlite.events.api.EventEmitter;
import lite.sqlite.events.config.KafkaMutationConfig;

/**
 * Creates the runtime EventEmitter using application properties.
 */
public final class EventEmitterFactory {
    private static final String KEY_EMITTER_TYPE = "event.emitter.type";
    private static final String KEY_EMITTER_FALLBACK_TYPE = "event.emitter.fallback.type";
    private static final String KEY_LOGGING_PATH = "event.logging.path";

    private EventEmitterFactory() {
    }

    public static EventEmitter create() {
        String emitterType = AppConfig.getOrDefault(KEY_EMITTER_TYPE, "kafka").toLowerCase(Locale.ROOT);
        try {
            return createByType(emitterType);
        } catch (Exception ex) {
            System.err.println("Failed to initialize emitter type '" + emitterType + "': " + ex.getMessage());
            return createFallbackEmitter();
        }
    }

    private static EventEmitter createByType(String type) {
        return switch (type) {
            case "kafka" -> createKafkaEmitter();
            case "logging" -> createLoggingEmitter();
            case "noop" -> new NoOpEventEmitter();
            default -> throw new IllegalStateException("Unknown event.emitter.type: " + type);
        };
    }

    private static EventEmitter createKafkaEmitter() {
        String bootstrapServers = KafkaMutationConfig.defaultBootstrapServers();
        String topic = KafkaMutationConfig.defaultTopic();
        Path fallbackPath = Path.of(KafkaMutationConfig.emitterFallbackPath());
        Properties producerProperties = KafkaMutationConfig.producerProperties(bootstrapServers);
        return new KafkaEventEmitter(producerProperties, topic, fallbackPath);
    }

    private static EventEmitter createLoggingEmitter() {
        String path = AppConfig.getOrDefault(KEY_LOGGING_PATH, KafkaMutationConfig.emitterFallbackPath());
        return new LoggingEventEmitter(Path.of(path));
    }

    private static EventEmitter createFallbackEmitter() {
        String fallbackType = AppConfig.getOrDefault(KEY_EMITTER_FALLBACK_TYPE, "logging")
            .toLowerCase(Locale.ROOT);
        try {
            if ("noop".equals(fallbackType)) {
                System.err.println("Falling back to NoOpEventEmitter.");
                return new NoOpEventEmitter();
            }
            System.err.println("Falling back to LoggingEventEmitter.");
            return createLoggingEmitter();
        } catch (Exception ex) {
            System.err.println("Fallback emitter initialization failed: " + ex.getMessage());
            System.err.println("Falling back to NoOpEventEmitter.");
            return new NoOpEventEmitter();
        }
    }
}
