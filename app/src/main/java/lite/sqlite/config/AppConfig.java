package lite.sqlite.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application-wide configuration bootstrap.
 * Loads configured property resources once and serves typed access methods.
 */
public final class AppConfig {
    private static final String[] RESOURCE_FILES = {
        "application.properties",
        "kafka-mutation.properties"
    };

    private static final Properties PROPERTIES = new Properties();
    private static volatile boolean initialized = false;

    private AppConfig() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        for (String resource : RESOURCE_FILES) {
            loadOptionalResource(resource);
        }
        initialized = true;
    }

    public static String getRequired(String key) {
        ensureInitialized();
        String override = System.getProperty(key);
        if (override != null && !override.isBlank()) {
            return override.trim();
        }

        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration key: " + key);
        }
        return value.trim();
    }

    public static int getRequiredInt(String key) {
        String value = getRequired(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Configuration key must be integer: " + key + "=" + value, ex);
        }
    }

    public static String getOrDefault(String key, String defaultValue) {
        ensureInitialized();
        String override = System.getProperty(key);
        if (override != null && !override.isBlank()) {
            return override.trim();
        }

        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    private static void loadOptionalResource(String resourceName) {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                return;
            }
            PROPERTIES.load(input);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load configuration resource: " + resourceName, ex);
        }
    }
}
