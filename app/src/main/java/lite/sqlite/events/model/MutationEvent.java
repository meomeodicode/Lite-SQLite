package lite.sqlite.events.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * Canonical mutation event payload that can be emitted to Kafka or NDJSON.
 */
@Getter
public class MutationEvent {
    private static final String SCHEMA_VERSION = "v1";

    private final String eventId;
    private final String eventType;
    private final String table;
    private final MutationOperation operation;
    private final MutationRecordId recordId;
    private final Instant occurredAt;
    private final Map<String, Object> payload;
    private final Map<String, Object> beforeImage;
    private final Map<String, Object> afterImage;
    private final String schemaVersion;
    private final String traceId;

    @Builder(access = AccessLevel.PRIVATE)
    private MutationEvent(
        String eventId,
        String eventType,
        String table,
        MutationOperation operation,
        MutationRecordId recordId,
        Instant occurredAt,
        Map<String, Object> payload,
        Map<String, Object> beforeImage,
        Map<String, Object> afterImage,
        String schemaVersion,
        String traceId
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.table = table;
        this.operation = operation;
        this.recordId = recordId;
        this.occurredAt = occurredAt;
        this.payload = payload == null ? null : Map.copyOf(payload);
        this.beforeImage = beforeImage == null ? null : Map.copyOf(beforeImage);
        this.afterImage = afterImage == null ? null : Map.copyOf(afterImage);
        this.schemaVersion = schemaVersion;
        this.traceId = traceId;
    }

    public static MutationEvent forInsert(String table, MutationRecordId recordId, Map<String, Object> payload) {
        return baseBuilder(table, MutationOperation.INSERT, "ROW_INSERTED", recordId)
            .payload(payload)
            .afterImage(payload)
            .build();
    }

    public static MutationEvent forUpdate(
        String table,
        MutationRecordId recordId,
        Map<String, Object> beforeImage,
        Map<String, Object> afterImage
    ) {
        return baseBuilder(table, MutationOperation.UPDATE, "ROW_UPDATED", recordId)
            .payload(afterImage)
            .beforeImage(beforeImage)
            .afterImage(afterImage)
            .build();
    }

    public static MutationEvent forDelete(String table, MutationRecordId recordId, Map<String, Object> beforeImage) {
        return baseBuilder(table, MutationOperation.DELETE, "ROW_DELETED", recordId)
            .payload(beforeImage)
            .beforeImage(beforeImage)
            .build();
    }

    public String eventKey() {
        return recordId.toEventKey(table);
    }

    private static MutationEventBuilder baseBuilder(
        String table,
        MutationOperation operation,
        String eventType,
        MutationRecordId recordId
    ) {
        return MutationEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(eventType)
            .table(table)
            .operation(operation)
            .recordId(recordId)
            .occurredAt(Instant.now())
            .schemaVersion(SCHEMA_VERSION)
            .traceId(UUID.randomUUID().toString());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("event_id", eventId);
        map.put("event_type", eventType);
        map.put("table", table);
        map.put("operation", operation.name());
        map.put("record_id", recordId.toJsonMap());
        map.put("occurred_at", occurredAt.toString());
        map.put("payload", payload);
        map.put("before_image", beforeImage);
        map.put("after_image", afterImage);
        map.put("schema_version", schemaVersion);
        map.put("trace_id", traceId);
        return map;
    }
}
