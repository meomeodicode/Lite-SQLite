package lite.sqlite.server.catalog;

import java.io.IOException;

/** catalog.dat exists but fails its magic, version, length or checksum check. */
public class CatalogCorruptException extends IOException {
    public CatalogCorruptException(String message) {
        super(message);
    }
}
