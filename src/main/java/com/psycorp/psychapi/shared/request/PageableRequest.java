package com.psycorp.psychapi.shared.request;

import java.util.List;

public interface PageableRequest {
    default String search() { return null; }
    default List<String> filter() { return List.of(); }
    int page();
    int limit();
    String sortBy();
    String sortOrder();
}