package lite.sqlite.events;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import lite.sqlite.events.api.EventEmitter;
import lite.sqlite.events.model.MutationEvent;

/**
 * Appends mutation events as NDJSON lines.
 */
public class LoggingEventEmitter implements EventEmitter {
    private final Path outputPath;

    public LoggingEventEmitter(Path outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public synchronized void emit(MutationEvent event) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String jsonLine = JsonEventSerializer.toJson(event.toMap()) + System.lineSeparator();
        Files.writeString(
            outputPath,
            jsonLine,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND
        );
    }
}
