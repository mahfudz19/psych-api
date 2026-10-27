# Standar Implementasi Pagination (Quarkus + MongoDB Panache)

Gunakan standar ini setiap kali membuat fitur list/get-all yang membutuhkan pagination, pencarian (search), dan filter dinamis.

## 1. Komponen Utama

- `MongoFilter`: Utility untuk memproses search, filter, dan sort dari request.
- `PanacheQuery`: Digunakan untuk eksekusi query ke MongoDB.
- `PaginationMeta`: Wrapper metadata pagination untuk response.

## 2. Aturan Wajib (Hard Rules)

1. **Soft Delete**: Selalu tambahkan `Filters.eq("deletedAt", null)` sebagai `baseFilter` jika entity mendukung soft delete.
2. **Zero-based Page**: Panache menggunakan index 0 untuk page, sehingga selalu gunakan `request.page() - 1`.
3. **Total Count**: Hitung total dokumen menggunakan `count(finalFilter)` sebelum mapping data.

## 3. Template Implementasi di Resource/Service

```java
// 1. Buat Base Filter (Soft Delete)
Bson baseFilter = Filters.eq("deletedAt", null);

// 2. Parse Search & Filter dari Request
Bson searchFilter = MongoFilter.fromRequest(request, EntityListRequest.SEARCH_FIELDS);

// 3. Gabungkan Filter
Bson finalFilter = MongoFilter.and(baseFilter, searchFilter);

// 4. Parse Sort
Bson sort = MongoFilter.sort(request);

// 5. Eksekusi Query dengan Pagination (page - 1)
PanacheQuery<Entity> query = entityService.find(finalFilter, sort)
    .page(request.page() - 1, request.limit());

// 6. Hitung Total Data
long total = entityService.count(finalFilter);

// 7. Map ke DTO Response
List<EntityResponse> data = query.stream().map(EntityResponse::fromEntity).toList();

// 8. Buat Pagination Meta
PaginationMeta meta = PaginationMeta.of(request, total);
```
