package com.psycorp.psychapi.common.util;

import java.util.Arrays;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

/**
 * Utility class untuk parse filter string menjadi Bson filter MongoDB.
 * Format filter: "field:operator:value"
 * Contoh: "status:in:draft,published" atau "age:gt:18"
 */
public final class FilterParser {
    
    private FilterParser() {
        // Prevent instantiation
    }
    
    /**
     * Parse filter string menjadi Bson filter.
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
        
        return switch (operator) {
            case "in" -> {
                String[] values = value.split(",");
                yield Filters.in(field, Arrays.asList(values));
            }
            case "nin" -> {
                String[] values = value.split(",");
                yield Filters.nin(field, Arrays.asList(values));
            }
            case "eq" -> Filters.eq(field, value);
            case "ne" -> Filters.ne(field, value);
            case "gt" -> Filters.gt(field, value);
            case "gte" -> Filters.gte(field, value);
            case "lt" -> Filters.lt(field, value);
            case "lte" -> Filters.lte(field, value);
            case "contains" -> Filters.regex(field, value, "i");
            default -> Filters.eq(field, value);
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
        
        java.util.List<Bson> bsonFilters = new java.util.ArrayList<>();
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
}
