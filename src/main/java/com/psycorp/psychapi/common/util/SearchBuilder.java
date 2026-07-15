package com.psycorp.psychapi.common.util;

import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

/**
 * Utility class untuk build search Bson filter dengan regex di multiple fields.
 */
public final class SearchBuilder {
    
    private SearchBuilder() {
        // Prevent instantiation
    }
    
    /**
     * Build search Bson dengan regex di multiple fields (menggunakan $or).
     * Search akan case-insensitive.
     * 
     * @param searchTerm Kata kunci pencarian
     * @param fields Field-field yang akan di-search
     * @return Bson search filter atau null jika searchTerm kosong
     */
    public static Bson build(String searchTerm, String... fields) {
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
     * Build search Bson dengan regex di single field.
     * Search akan case-insensitive.
     * 
     * @param searchTerm Kata kunci pencarian
     * @param field Field yang akan di-search
     * @return Bson search filter atau null jika searchTerm kosong
     */
    public static Bson buildSingle(String searchTerm, String field) {
        if (searchTerm == null || searchTerm.isBlank() || field == null || field.isBlank()) {
            return null;
        }
        
        return Filters.regex(field, searchTerm, "i");
    }
    
    /**
     * Build search Bson dengan exact match (case-insensitive) di single field.
     * 
     * @param searchTerm Kata kunci pencarian
     * @param field Field yang akan di-search
     * @return Bson exact match filter atau null jika searchTerm kosong
     */
    public static Bson exactMatch(String searchTerm, String field) {
        if (searchTerm == null || searchTerm.isBlank() || field == null || field.isBlank()) {
            return null;
        }
        
        return Filters.eq(field, searchTerm.toLowerCase());
    }
    
    /**
     * Build search Bson dengan contains (case-insensitive) di single field.
     * Sama seperti buildSingle, tapi dengan nama method yang lebih deskriptif.
     * 
     * @param searchTerm Kata kunci pencarian
     * @param field Field yang akan di-search
     * @return Bson contains filter atau null jika searchTerm kosong
     */
    public static Bson contains(String searchTerm, String field) {
        return buildSingle(searchTerm, field);
    }
    
    /**
     * Build search Bson dengan startsWith (case-insensitive) di single field.
     * 
     * @param prefix Prefix/kata awalan
     * @param field Field yang akan di-search
     * @return Bson startsWith filter atau null jika prefix kosong
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
     * Build search Bson dengan endsWith (case-insensitive) di single field.
     * 
     * @param suffix Suffix/kata akhiran
     * @param field Field yang akan di-search
     * @return Bson endsWith filter atau null jika suffix kosong
     */
    public static Bson endsWith(String suffix, String field) {
        if (suffix == null || suffix.isBlank() || field == null || field.isBlank()) {
            return null;
        }
        
        // Escape special regex characters
        String escapedSuffix = suffix.replaceAll("([\\\\^$|?*+()\\[\\]{}])", "\\\\$1");
        return Filters.regex(field, escapedSuffix + "$", "i");
    }
}
