package lite.sqlite.events.config;

import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import lite.sqlite.config.AppConfig;

/**
 * Central Kafka config facade backed by the app-wide configuration bootstrap.
 */
public final class KafkaMutationConfig {
    public static final String KEY_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";
    public static final String KEY_TOPIC = "kafka.topic";
    public static final String KEY_CONSUMER_GROUP = "kafka.consumer.group.id";
    public static final String KEY_CONSUMER_OFFSET_RESET = "kafka.consumer.offset.reset";
    public static final String KEY_CONSUMER_POLL_COUNT = "kafka.consumer.poll.count";
    public static final String KEY_CONSUMER_POLL_TIMEOUT_MS = "kafka.consumer.poll.timeout.ms";
    public static final String KEY_PRODUCER_DEMO_COUNT = "kafka.producer.demo.count";
    public static final String KEY_EMITTER_FALLBACK_PATH = "kafka.emitter.fallback.path";
    public static final String KEY_PRODUCER_ACKS = "kafka.producer.acks";
    public static final String KEY_PRODUCER_IDEMPOTENCE = "kafka.producer.enable.idempotence";
    public static final String KEY_PRODUCER_RETRIES = "kafka.producer.retries";
    public static final String KEY_PRODUCER_MAX_IN_FLIGHT = "kafka.producer.max.in.flight.requests.per.connection";
    public static final String KEY_DEMO_TABLE = "kafka.demo.table";
    public static final String KEY_DEMO_SOURCE = "kafka.demo.source";
    public static final String KEY_DEMO_STATUS_BUCKETS = "kafka.demo.status.buckets";

    private KafkaMutationConfig() {
    }

    public static String defaultBootstrapServers() {
        return AppConfig.getRequired(KEY_BOOTSTRAP_SERVERS);
    }

    public static String defaultTopic() {
        return AppConfig.getRequired(KEY_TOPIC);
    }

    public static String defaultConsumerGroup() {
        return AppConfig.getRequired(KEY_CONSUMER_GROUP);
    }

    public static String defaultConsumerOffsetReset() {
        return AppConfig.getRequired(KEY_CONSUMER_OFFSET_RESET);
    }

    public static int defaultConsumerPollCount() {
        return AppConfig.getRequiredInt(KEY_CONSUMER_POLL_COUNT);
    }

    public static int defaultConsumerPollTimeoutMs() {
        return AppConfig.getRequiredInt(KEY_CONSUMER_POLL_TIMEOUT_MS);
    }

    public static int defaultProducerDemoCount() {
        return AppConfig.getRequiredInt(KEY_PRODUCER_DEMO_COUNT);
    }

    public static String emitterFallbackPath() {
        return AppConfig.getRequired(KEY_EMITTER_FALLBACK_PATH);
    }

    public static String demoTable() {
        return AppConfig.getRequired(KEY_DEMO_TABLE);
    }

    public static String demoSource() {
        return AppConfig.getRequired(KEY_DEMO_SOURCE);
    }

    public static int demoStatusBuckets() {
        return AppConfig.getRequiredInt(KEY_DEMO_STATUS_BUCKETS);
    }

    public static Properties producerProperties(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, AppConfig.getRequired(KEY_PRODUCER_ACKS));
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, AppConfig.getRequired(KEY_PRODUCER_IDEMPOTENCE));
        props.put(ProducerConfig.RETRIES_CONFIG, AppConfig.getRequired(KEY_PRODUCER_RETRIES));
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, AppConfig.getRequired(KEY_PRODUCER_MAX_IN_FLIGHT));
        return props;
    }

    public static Properties consumerProperties(
        String bootstrapServers,
        String groupId,
        String autoOffsetReset
    ) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        return props;
    }
}
