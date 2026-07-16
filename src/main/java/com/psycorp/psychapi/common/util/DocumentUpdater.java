package com.psycorp.psychapi.common.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Updates;

/**
 * Utility class untuk membangun MongoDB update operations.
 *
 * Rules:
 * - Field dengan empty values (null, empty string, empty array, empty object) akan di-unset
 * - Field dengan non-empty values akan di-set
 */
public class DocumentUpdater {
    private final Map<String, Object> setFields = new LinkedHashMap<>();
    private final Set<String> unsetFields = new LinkedHashSet<>();

    private DocumentUpdater() {}
    
    /**
     * Create new DocumentUpdater instance.
     * @return New DocumentUpdater instance
     */
    public static DocumentUpdater update() {
        return new DocumentUpdater();
    }

    /**
     * Set field value. Jika value adalah empty (null, empty string, empty array, empty object),
     * field akan di-unset dari database.
     *
     * @param fieldName Nama field yang akan di-update
     * @param value Nilai untuk field tersebut. Empty values akan trigger unset operation.
     * @return This DocumentUpdater instance for method chaining
     */
    public DocumentUpdater set(String fieldName, Object value) {
        if (isEmpty(value)) {
            return unset(fieldName);
        }
        setFields.put(fieldName, value);
        return this;
    }

    /**
     * Cek apakah value termasuk "empty" yang harus di-unset.
     *
     * Empty values didefinisikan sebagai:
     * - null
     * - Empty string atau string yang hanya berisi whitespace ("", "   ")
     * - Empty array ([])
     * - Empty object ({})
     *
     * @param value Value untuk dicek
     * @return true jika value adalah empty, false jika tidak
     */
    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        
        if (value instanceof String) {
            String str = (String) value;
            return str.isBlank(); // Handle "", "   ", "\t", "\n"
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
