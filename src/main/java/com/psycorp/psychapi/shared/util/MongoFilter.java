package com.psycorp.psychapi.shared.util;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.psycorp.psychapi.shared.request.PageableRequest;

/**
 * Utility class untuk membangun dan menggabungkan MongoDB Bson filters dan sorts.
 * Mengkonsolidasikan fungsi dari FilterParser, SearchBuilder, FilterCombiner, dan SortBuilder.
 *
 * <p>Usage Examples:</p>
 * <pre>
 * // Parse filter string "field:operator:value"
 * Bson filter = MongoFilter.parse("status:in:draft,published");
 *
 * // Search across multiple fields
 * Bson search = MongoFilter.search("keyword", "title", "description");
 *
 * // Combine filters with AND
 * Bson combined = MongoFilter.and(filter1, filter2, filter3);
 *
 * // Combine filters with OR
 * Bson orCombined = MongoFilter.or(filter1, filter2);
 *
 * // Build sort
 * Bson sort = MongoFilter.sort("createdAt", "desc");
 *
 * // Combine multiple sorts
 * Bson orderBy = MongoFilter.orderBy(sort1, sort2);
 * </pre>
 */
public final class MongoFilter {
    
    private MongoFilter() {
        // Prevent instantiation
    }

    private static Instant toInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            if (!value.contains("T")) {
                return Instant.parse(value + "T00:00:00Z");
            }
            if (!value.endsWith("Z")) {
                return Instant.parse(value + "Z");
            }
            throw e;
        }
    }

    private static boolean isObjectIdField(String field) {
        if (field == null) return false;
        String lower = field.toLowerCase();
        return lower.equals("id") || lower.equals("_id") || lower.endsWith("id") || lower.endsWith("_id");
    }

    private static Object toTypedValue(String field, String value) {
        if (value == null) {
            return null;
        }
        if (isObjectIdField(field) && ObjectId.isValid(value)) {
            return new ObjectId(value);
        }
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        try {
            return toInstant(value);
        } catch (DateTimeParseException e) {
            return value;
        }
    }
    
    // =========================================================================
    // PARSE METHODS (dari FilterParser)
    // =========================================================================
    
    /**
     * Parse filter string menjadi Bson filter.
     * Format: "field:operator:value"
     * Contoh: "status:in:draft,published" atau "age:gt:18"
     * 
     * @param filter Filter string dengan format "field:operator:value"
     * @return Bson filter atau null jika format tidak valid
     */
    public static Bson parse(String filter) {
        if (filter == null || filter.isBlank()) {
            return null;
        }
        
        // Split max 3 parts: field, operator, value
        String[] parts = filter.split(":", 3);
        if (parts.length < 3) {
            return null;
        }
        
        String field = parts[0];
        String operator = parts[1].toLowerCase();
        String value = parts[2];

        if ("deviceInfo".equals(field)) {
            return Filters.or(
                Filters.regex("deviceInfo.os", value, "i"),
                Filters.regex("deviceInfo.browser", value, "i"),
                Filters.regex("deviceInfo.userAgent", value, "i"),
                Filters.regex("deviceInfo.ip", value, "i"),
                Filters.regex("deviceInfo.location", value, "i")
            );
        }
        
        return switch (operator) {
            case "in" -> {
                String[] values = value.split(",");
                List<Object> typedValues = Arrays.stream(values)
                    .map(v -> toTypedValue(field, v.trim()))
                    .toList();
                yield Filters.in(field, typedValues);
            }
            case "nin" -> {
                String[] values = value.split(",");
                List<Object> typedValues = Arrays.stream(values)
                    .map(v -> toTypedValue(field, v.trim()))
                    .toList();
                yield Filters.nin(field, typedValues);
            }
            case "eq" -> Filters.eq(field, toTypedValue(field, value));
            case "ne" -> Filters.ne(field, toTypedValue(field, value));
            case "gt" -> Filters.gt(field, toTypedValue(field, value));
            case "gte" -> Filters.gte(field, toTypedValue(field, value));
            case "lt" -> Filters.lt(field, toTypedValue(field, value));
            case "lte" -> Filters.lte(field, toTypedValue(field, value));
            case "between" -> {
                String[] range = value.split(",", 2);
                if (range.length < 2) yield null;
                yield Filters.and(
                    Filters.gte(field, toTypedValue(field, range[0].trim())),
                    Filters.lte(field, toTypedValue(field, range[1].trim()))
                );
            }
            case "contains" -> {
                if (isObjectIdField(field)) {
                    if (ObjectId.isValid(value)) {
                        yield Filters.eq(field, new ObjectId(value));
                    }
                    yield Filters.expr(new Document("$regexMatch",
                        new Document("input", new Document("$toString", "$" + field))
                            .append("regex", value)
                            .append("options", "i")));
                }
                yield Filters.regex(field, value, "i");
            }
            default -> Filters.eq(field, toTypedValue(field, value));
        };
    }
    
    /**
     * Parse multiple filter strings dan gabungkan dengan $and.
     * 
     * @param filters Array of filter strings
     * @return Bson filter gabungan atau null jika tidak ada filter valid
     */
    public static Bson parseAll(String... filters) {
        if (filters == null || filters.length == 0) {
            return null;
        }
        
        List<Bson> bsonFilters = new ArrayList<>();
        for (String filter : filters) {
            Bson bson = parse(filter);
            if (bson != null) {
                bsonFilters.add(bson);
            }
        }
        
        if (bsonFilters.isEmpty()) {
            return null;
        }
        
        return bsonFilters.size() == 1 
            ? bsonFilters.get(0)
            : Filters.and(bsonFilters);
    }
    
    // =========================================================================
    // SEARCH METHODS (dari SearchBuilder)
    // =========================================================================
    
    /**
     * Build search filter dengan regex across multiple fields.
     * 
     * @param searchTerm Kata kunci pencarian
     * @param fields Field-field yang akan dicari
     * @return Bson filter dengan $or untuk multiple fields, atau null jika tidak valid
     */
    public static Bson search(String searchTerm, String... fields) {
        if (searchTerm == null || searchTerm.isBlank() || fields == null || fields.length == 0) {
            return null;
        }
        
        List<Bson> searches = new ArrayList<>();
        for (String field : fields) {
            if (field != null && !field.isBlank()) {
                searches.add(Filters.regex(field, searchTerm, "i"));
            }
        }
        
        if (searches.isEmpty()) {
            return null;
        }
        
        return searches.size() == 1 
            ? searches.get(0)
            : Filters.or(searches);
    }
    
    /**
     * Build search filter untuk single field.
     * 
     * @param searchTerm Kata kunci pencarian
     * @param field Field yang akan dicari
     * @return Bson filter regex atau null jika tidak valid
     */
    public static Bson searchSingle(String searchTerm, String field) {
        if (searchTerm == null || searchTerm.isBlank() || field == null || field.isBlank()) {
            return null;
        }
        
        return Filters.regex(field, searchTerm, "i");
    }
    
    /**
     * Build exact match filter (case-insensitive).
     * 
     * @param value Nilai yang dicocokkan
     * @param field Field yang dicocokkan
     * @return Bson filter eq atau null jika tidak valid
     */
    public static Bson exactMatch(String value, String field) {
        if (value == null || value.isBlank() || field == null || field.isBlank()) {
            return null;
        }
        
        return Filters.eq(field, value.toLowerCase());
    }
    
    /**
     * Build contains filter (alias untuk searchSingle).
     * 
     * @param searchTerm Kata kunci pencarian
     * @param field Field yang akan dicari
     * @return Bson filter regex atau null jika tidak valid
     */
    public static Bson contains(String searchTerm, String field) {
        return searchSingle(searchTerm, field);
    }
    
    /**
     * Build startsWith filter dengan regex.
     * 
     * @param prefix Prefix yang dicari
     * @param field Field yang dicocokkan
     * @return Bson filter regex dengan ^ atau null jika tidak valid
     */
    public static Bson startsWith(String prefix, String field) {
        if (prefix == null || prefix.isBlank() || field == null || field.isBlank()) {
            return null;
        }
        
        // Escape special regex characters
        String escapedPrefix = prefix.replaceAll("([\\\\^$|?*+()\\[\\]{}])", "\\\\$1");
        return Filters.regex(field, "^" + escapedPrefix, "i");
    }
    
    /**
     * Build endsWith filter dengan regex.
     * 
     * @param suffix Suffix yang dicari
     * @param field Field yang dicocokkan
     * @return Bson filter regex dengan $ atau null jika tidak valid
     */
    public static Bson endsWith(String suffix, String field) {
        if (suffix == null || suffix.isBlank() || field == null || field.isBlank()) {
            return null;
        }
        
        // Escape special regex characters
        String escapedSuffix = suffix.replaceAll("([\\\\^$|?*+()\\[\\]{}])", "\\\\$1");
        return Filters.regex(field, escapedSuffix + "$", "i");
    }
    
    // =========================================================================
    // COMBINE METHODS (dari FilterCombiner)
    // =========================================================================
    
    /**
     * Combine multiple Bson filters menjadi satu dengan $and operator.
     * Filter yang null akan diabaikan.
     * 
     * @param filters Varargs Bson filters (bisa null)
     * @return Combined Bson filter, atau Document kosong jika semua null
     */
    public static Bson and(Bson... filters) {
        List<Bson> validFilters = new ArrayList<>();
        
        for (Bson filter : filters) {
            if (filter != null) {
                validFilters.add(filter);
            }
        }
        
        if (validFilters.isEmpty()) {
            return new Document(); // Empty = select all
        }
        
        return validFilters.size() == 1
            ? validFilters.get(0)
            : Filters.and(validFilters);
    }
    
    /**
     * Combine multiple Bson filters menjadi satu dengan $or operator.
     * Filter yang null akan diabaikan.
     * 
     * @param filters Varargs Bson filters (bisa null)
     * @return Combined Bson filter dengan $or, atau Document kosong jika semua null
     */
    public static Bson or(Bson... filters) {
        List<Bson> validFilters = new ArrayList<>();
        
        for (Bson filter : filters) {
            if (filter != null) {
                validFilters.add(filter);
            }
        }
        
        if (validFilters.isEmpty()) {
            return new Document();
        }
        
        return validFilters.size() == 1
            ? validFilters.get(0)
            : Filters.or(validFilters);
    }
    
    // =========================================================================
    // SORT METHODS (dari SortBuilder)
    // =========================================================================
    
    /**
     * Build sort Bson berdasarkan field dan order.
     * Default: field = "createdAt", order = "desc"
     *
     * @param sortBy Field untuk sorting
     * @param sortOrder Order ("asc" atau "desc")
     * @return Bson sort
     */
    public static Bson sort(String sortBy, String sortOrder) {
        String field = sortBy != null && !sortBy.isBlank() ? sortBy : "createdAt";
        String order = sortOrder != null && !sortOrder.isBlank() ? sortOrder : "desc";
        
        return "asc".equalsIgnoreCase(order)
            ? Sorts.ascending(field)
            : Sorts.descending(field);
    }
    
    /**
     * Build default sort (createdAt descending).
     *
     * @return Bson sort descending createdAt
     */
    public static Bson sortDefault() {
        return Sorts.descending("createdAt");
    }
    
    /**
     * Build ascending sort.
     *
     * @param field Field untuk sorting
     * @return Bson sort ascending
     */
    public static Bson ascending(String field) {
        return Sorts.ascending(field);
    }
    
    /**
     * Build descending sort.
     *
     * @param field Field untuk sorting
     * @return Bson sort descending
     */
    public static Bson descending(String field) {
        return Sorts.descending(field);
    }
    
    /**
     * Combine multiple sort Bson menjadi satu.
     *
     * @param sorts Varargs Bson sorts (bisa null)
     * @return Combined Bson sort, atau default sort jika semua null
     */
    public static Bson orderBy(Bson... sorts) {
        if (sorts == null || sorts.length == 0) {
            return sortDefault();
        }
        
        if (sorts.length == 1) {
            return sorts[0];
        }
        
        return Sorts.orderBy(Arrays.asList(sorts));
    }

    public static Bson fromRequest(PageableRequest req, String... searchFields) {
        return and(
            search(req.search(), searchFields),
            parseAll(req.filter().toArray(String[]::new))
        );
    }

    public static Bson sort(PageableRequest req) {
        return sort(req.sortBy(), req.sortOrder());
    }
}
