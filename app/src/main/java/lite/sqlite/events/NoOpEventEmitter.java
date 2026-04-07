package lite.sqlite.events;

import java.io.IOException;

import lite.sqlite.events.api.EventEmitter;
import lite.sqlite.events.model.MutationEvent;

/**
 * Drops events. Useful for tests or local runs without event persistence.
 */
public class NoOpEventEmitter implements EventEmitter {
    @Override
    public void emit(MutationEvent event) throws IOException {
        // Intentionally empty.
    }
}
