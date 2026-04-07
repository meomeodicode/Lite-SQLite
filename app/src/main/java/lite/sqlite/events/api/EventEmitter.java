package lite.sqlite.events.api;

import java.io.IOException;

import lite.sqlite.events.model.MutationEvent;

/**
 * Contract for mutation event sinks (Kafka, NDJSON log, test doubles).
 */
public interface EventEmitter {

    void emit(MutationEvent event) throws IOException;
}
