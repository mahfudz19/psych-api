package com.psycorp.psychapi.common.util;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

/**
 * Utility class untuk combine multiple Bson filters menjadi satu.
 * 
 * Usage:
 * <pre>
 * Bson finalFilter = FilterCombiner.combine(searchFilter, customFilter, statusFilter);
 * </pre>
 */
public final class FilterCombiner {

    private FilterCombiner() {} // Prevent instantiation

    /**
     * Combine multiple Bson filters menjadi satu dengan $and operator.
     * Filter yang null akan diabaikan.
     * 
     * @param filters Varargs Bson filters (bisa null)
     * @return Combined Bson filter, atau Document kosong jika semua null
     */
    public static Bson combine(Bson... filters) {
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
    public static Bson combineOr(Bson... filters) {
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
}
