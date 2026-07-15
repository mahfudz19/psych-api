package com.psycorp.psychapi.common.util;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Sorts;

/**
 * Utility class untuk build sort Bson untuk MongoDB.
 */
public final class SortBuilder {
    
    private SortBuilder() {
        // Prevent instantiation
    }
    
    /**
     * Build sort Bson dengan field dan order.
     * 
     * @param sortBy Field untuk sort (default: "createdAt")
     * @param sortOrder Order: "asc" atau "desc" (default: "desc")
     * @return Bson sort
     */
    public static Bson build(String sortBy, String sortOrder) {
        String field = sortBy != null && !sortBy.isBlank() ? sortBy : "createdAt";
        String order = sortOrder != null && !sortOrder.isBlank() ? sortOrder : "desc";
        
        return "asc".equalsIgnoreCase(order) 
            ? Sorts.ascending(field)
            : Sorts.descending(field);
    }
    
    /**
     * Build sort Bson dengan default (createdAt desc).
     * 
     * @return Bson sort descending berdasarkan createdAt
     */
    public static Bson buildDefault() {
        return Sorts.descending("createdAt");
    }
    
    /**
     * Build sort Bson ascending untuk field tertentu.
     * 
     * @param field Field untuk sort
     * @return Bson sort ascending
     */
    public static Bson ascending(String field) {
        return Sorts.ascending(field);
    }
    
    /**
     * Build sort Bson descending untuk field tertentu.
     * 
     * @param field Field untuk sort
     * @return Bson sort descending
     */
    public static Bson descending(String field) {
        return Sorts.descending(field);
    }
    
    /**
     * Build multiple sort Bson (multi-field sorting).
     * 
     * @param sorts Array of sort Bson
     * @return Bson sort gabungan
     */
    public static Bson orderBy(Bson... sorts) {
        if (sorts == null || sorts.length == 0) {
            return buildDefault();
        }
        
        if (sorts.length == 1) {
            return sorts[0];
        }
        
        return Sorts.orderBy(java.util.Arrays.asList(sorts));
    }
}
