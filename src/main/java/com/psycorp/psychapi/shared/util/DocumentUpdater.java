package com.psycorp.psychapi.shared.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Updates;

public class DocumentUpdater {
    private final Map<String, Object> setFields = new LinkedHashMap<>();
    private final Set<String> unsetFields = new LinkedHashSet<>();

    private DocumentUpdater() {}

    public static DocumentUpdater update() {
        return new DocumentUpdater();
    }

    public DocumentUpdater set(String fieldName, Object value) {
        if (isEmpty(value)) {
            return unset(fieldName);
        }
        setFields.put(fieldName, value);
        return this;
    }

    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        
        if (value instanceof String str) {
            return str.isBlank();
        }
        
        if (value instanceof List) {
            return ((List<?>) value).isEmpty();
        }
        
        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        }
        
        // Untuk array, cek length
        if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }
        
        // Untuk tipe lain (Number, Boolean, ObjectId, Instant, dll) selalu dianggap tidak empty
        return false;
    }

    public DocumentUpdater setRaw(String fieldName, Object value) {
        setFields.put(fieldName, value);
        return this;
    }

    public DocumentUpdater unset(String fieldName) {
        unsetFields.add(fieldName);
        return this;
    }

    public Bson build() {
        List<Bson> updates = new ArrayList<>();

        // Add $set operations
        setFields.forEach((field, value) -> 
            updates.add(Updates.set(field, value))
        );

        // Add $unset operations
        unsetFields.forEach(field -> 
            updates.add(Updates.unset(field))
        );

        // Return null jika tidak ada perubahan
        return updates.isEmpty() ? null : Updates.combine(updates);
    }

    public boolean hasChanges() {
        return !setFields.isEmpty() || !unsetFields.isEmpty();
    }
}
