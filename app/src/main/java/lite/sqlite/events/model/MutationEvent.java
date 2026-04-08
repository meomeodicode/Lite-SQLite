package lite.sqlite.events.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lite.sqlite.server.storage.record.Schema;
import lite.sqlite.server.storage.record.SlottedRecordPage.RecordWithSlot;
import lite.sqlite.server.storage.table.RecordId;
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
    private final Integer rowsAffected;
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
        Integer rowsAffected,
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
        this.rowsAffected = rowsAffected;
        this.schemaVersion = schemaVersion;
        this.traceId = traceId;
    }

    public static MutationEvent forInsert(String table, MutationRecordId recordId, Map<String, Object> payload) {
        return baseBuilder(table, MutationOperation.INSERT, "ROW_INSERTED", recordId)
            .payload(payload)
            .afterImage(payload)
            .rowsAffected(1)
            .build();
    }

    public static MutationEvent forInsert(String table, RecordId recordId, Map<String, Object> payload) {
        return forInsert(table, MutationRecordId.from(recordId), payload);
    }

    public static MutationEvent forInsert(String table, RecordWithSlot recordWithSlot, Map<String, Object> payload) {
        return forInsert(table, MutationRecordId.from(recordWithSlot), payload);
    }

    public static MutationEvent forInsert(String table, RecordId recordId, Schema schema, Object[] afterValues) {
        return forInsert(table, MutationRecordId.from(recordId), payloadFromValues(schema, afterValues));
    }

    public static MutationEvent forInsert(
        String table,
        RecordWithSlot recordWithSlot,
        Schema schema,
        Object[] afterValues
    ) {
        return forInsert(table, MutationRecordId.from(recordWithSlot), payloadFromValues(schema, afterValues));
    }

    public static MutationEvent forInsert(String table, int block, int slot, Map<String, Object> payload) {
        return forInsert(table, MutationRecordId.fromLocation(block, slot), payload);
    }

    public static MutationEvent forInsert(String table, int block, int slot, Schema schema, Object[] afterValues) {
        return forInsert(table, MutationRecordId.fromLocation(block, slot), payloadFromValues(schema, afterValues));
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
            .rowsAffected(1)
            .build();
    }

    public static MutationEvent forUpdate(
        String table,
        RecordWithSlot recordWithSlot,
        Map<String, Object> beforeImage,
        Map<String, Object> afterImage
    ) {
        return forUpdate(table, MutationRecordId.from(recordWithSlot), beforeImage, afterImage);
    }

    public static MutationEvent forUpdate(
        String table,
        RecordWithSlot recordWithSlot,
        Schema schema,
        Object[] beforeValues,
        Object[] afterValues
    ) {
        return forUpdate(
            table,
            MutationRecordId.from(recordWithSlot),
            payloadFromValues(schema, beforeValues),
            payloadFromValues(schema, afterValues)
        );
    }

    public static MutationEvent forUpdate(
        String table,
        int block,
        int slot,
        Map<String, Object> beforeImage,
        Map<String, Object> afterImage
    ) {
        return forUpdate(table, MutationRecordId.fromLocation(block, slot), beforeImage, afterImage);
    }

    public static MutationEvent forUpdate(
        String table,
        int block,
        int slot,
        Schema schema,
        Object[] beforeValues,
        Object[] afterValues
    ) {
        return forUpdate(
            table,
            MutationRecordId.fromLocation(block, slot),
            payloadFromValues(schema, beforeValues),
            payloadFromValues(schema, afterValues)
        );
    }

    public static MutationEvent forDelete(String table, MutationRecordId recordId, Map<String, Object> beforeImage) {
        return baseBuilder(table, MutationOperation.DELETE, "ROW_DELETED", recordId)
            .payload(beforeImage)
            .beforeImage(beforeImage)
            .rowsAffected(1)
            .build();
    }

    public static MutationEvent forMutationResult(String table, MutationOperation operation, int rowsAffected) {
        String eventType = switch (operation) {
            case INSERT -> "ROWS_INSERTED";
            case UPDATE -> "ROWS_UPDATED";
            case DELETE -> "ROWS_DELETED";
        };

        return baseBuilder(table, operation, eventType, null)
            .payload(summaryPayload(rowsAffected))
            .rowsAffected(rowsAffected)
            .build();
    }

    public static MutationEvent forDelete(String table, RecordWithSlot recordWithSlot, Map<String, Object> beforeImage) {
        return forDelete(table, MutationRecordId.from(recordWithSlot), beforeImage);
    }

    public static MutationEvent forDelete(
        String table,
        RecordWithSlot recordWithSlot,
        Schema schema,
        Object[] beforeValues
    ) {
        return forDelete(table, MutationRecordId.from(recordWithSlot), payloadFromValues(schema, beforeValues));
    }

    public static MutationEvent forDelete(String table, int block, int slot, Map<String, Object> beforeImage) {
        return forDelete(table, MutationRecordId.fromLocation(block, slot), beforeImage);
    }

    public static MutationEvent forDelete(String table, int block, int slot, Schema schema, Object[] beforeValues) {
        return forDelete(table, MutationRecordId.fromLocation(block, slot), payloadFromValues(schema, beforeValues));
    }

    public String eventKey() {
        if (recordId == null) {
            return table + ":unknown:" + eventId;
        }
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

    public static Map<String, Object> payloadFromValues(Schema schema, Object[] values) {
        if (schema == null) {
            throw new IllegalArgumentException("schema must not be null");
        }
        if (values == null) {
            return null;
        }

        List<String> columnNames = schema.getColumnNames();
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            Object value = i < values.length ? values[i] : null;
            payload.put(columnNames.get(i), value);
        }
        return payload;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("event_id", eventId);
        map.put("event_type", eventType);
        map.put("table", table);
        map.put("operation", operation.name());
        map.put("record_id", recordId == null ? null : recordId.toJsonMap());
        map.put("occurred_at", occurredAt.toString());
        map.put("payload", payload);
        map.put("before_image", beforeImage);
        map.put("after_image", afterImage);
        map.put("rows_affected", rowsAffected);
        map.put("schema_version", schemaVersion);
        map.put("trace_id", traceId);
        return map;
    }

    private static Map<String, Object> summaryPayload(int rowsAffected) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rows_affected", rowsAffected);
        return payload;
    }
}
