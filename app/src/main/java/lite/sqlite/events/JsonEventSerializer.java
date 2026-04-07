package lite.sqlite.events;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

final class JsonEventSerializer {
    private JsonEventSerializer() {
    }

    static String toJson(Map<String, Object> payload) {
        return writeValue(payload);
    }

    @SuppressWarnings("unchecked")
    private static String writeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String str) {
            return "\"" + escape(str) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder json = new StringBuilder("{");
            Iterator<? extends Map.Entry<?, ?>> it = ((Map<?, ?>) map).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> entry = it.next();
                json.append("\"")
                    .append(escape(String.valueOf(entry.getKey())))
                    .append("\":")
                    .append(writeValue(entry.getValue()));
                if (it.hasNext()) {
                    json.append(",");
                }
            }
            json.append("}");
            return json.toString();
        }
        if (value instanceof Collection<?> collection) {
            StringBuilder json = new StringBuilder("[");
            Iterator<?> it = collection.iterator();
            while (it.hasNext()) {
                json.append(writeValue(it.next()));
                if (it.hasNext()) {
                    json.append(",");
                }
            }
            json.append("]");
            return json.toString();
        }
        if (value instanceof Enum<?> enumValue) {
            return "\"" + escape(enumValue.name()) + "\"";
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(c);
                    break;
            }
        }
        return escaped.toString();
    }
}
