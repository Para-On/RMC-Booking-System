package RMC_Booking_Engine.rmc.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public final class JsonStringListConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonStringListConverter() {
    }

    public static String toJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to serialize amenities", ex);
        }
    }

    public static List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    public static List<String> sanitize(List<String> values) {
        if (values == null) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isBlank()) {
                cleaned.add(trimmed);
            }
        }
        return cleaned;
    }
}
