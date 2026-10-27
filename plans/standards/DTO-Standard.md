# Standar Implementasi DTO (Request & Response)

Gunakan standar ini untuk membuat Data Transfer Object (DTO) pada API.

## 1. Aturan Request DTO (List / Pagination)

1. **Gunakan `record`**: Jangan gunakan `class` biasa.
2. **Implement `PageableRequest`**: Wajib implement interface ini.
3. **Anotasi JAX-RS & OpenAPI**: Gunakan `@QueryParam`, `@DefaultValue`, dan `@Parameter`.
4. **Konstanta Wajib**: Harus memiliki `SEARCH_FIELDS_LIST`, `SEARCH_FIELDS`, dan `DESCRIPTION`.

## 2. Aturan Request DTO (Create & Update)

1. **Gunakan `record`**.
2. **Validasi Wajib**: Gunakan anotasi dari `jakarta.validation.constraints` (seperti `@NotBlank`, `@NotNull`, `@Email`, dll) beserta pesan errornya (`message = "..."`).
3. **Pemisahan DTO**: Pisahkan `CreateRequest` dan `UpdateRequest` jika ada field yang tidak boleh diubah saat update (misalnya field `code` yang unik).

### Template Create & Update Request DTO

```java
package com.psycorp.psychapi.feature.[domain].api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EntityCreateRequest(
    @NotBlank(message = "Code is required") String code,
    @NotBlank(message = "Name is required") String name,
    @NotNull(message = "Status is required") EntityStatus status
) {}

public record EntityUpdateRequest(
    // Field 'code' dihilangkan jika tidak boleh di-update
    @NotBlank(message = "Name is required") String name,
    @NotNull(message = "Status is required") EntityStatus status
) {}
```
