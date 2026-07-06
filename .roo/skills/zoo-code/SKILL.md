# Skill: Zoo Code - Quarkus API Best Practices

## Description

Skill ini memandu pengembangan Quarkus API mengikuti standar industri dan best practice. Fokus pada arsitektur berlapis (layered architecture), konfigurasi observability (health, metrics, logging), integrasi MongoDB dengan Panache, dan strategi CI/CD untuk Google Cloud Run.

## Instructions

Sebagai Zoo Code, ikuti prinsip-prinsip ini saat bekerja pada project `psych-api`:

1.  **Arsitektur Berlapis & Struktur Folder**: Implementasikan fitur menggunakan struktur package `api`, `service`, `repository`, `domain`, dan `infrastructure`.
    - `src/main/java/com/example/psychapi/`:
      - `api/resource`: REST endpoint (JAX-RS Resource).
      - `api/dto`: Data Transfer Objects (Request/Response).
      - `api/mapper`: Mapper antara DTO dan Domain Model (MapStruct/manual).
      - `api/exception`: JAX-RS ExceptionMapper untuk penanganan error.
      - `service`: Business logic inti, orkestrasi antar repository, transaksi.
      - `repository`: Interaksi dengan database (MongoDB Panache Repository).
      - `domain/model`: Entitas inti aplikasi, logika bisnis murni (MongoDB Entity).
      - `domain/exception`: Custom exception khusus domain.
      - `infrastructure/config`: Konfigurasi aplikasi (misal: `GreetingConfig`, `MongoConfig`).
      - `infrastructure/health`: Implementasi health check (misal: `MyLivenessCheck`).
      - `infrastructure/security`: Logika autentikasi dan otorisasi (misal: JWT, RBAC).
      - `infrastructure/seeder`: Class untuk mengisi data awal (seed data) saat aplikasi start, **hanya untuk profile `dev` dan `test`**. Jangan aktifkan seeder di `prod`.

    - `src/test/java/com/example/psychapi/`:
      - `api/resource`: Integration tests untuk REST endpoint (menggunakan `@QuarkusTest`, `RestAssured`).
      - `service`: Unit tests untuk business logic (menggunakan JUnit 5, Mockito).
      - `domain/model`: Unit tests untuk entity/domain logic.

2.  **Konfigurasi**: Gunakan `application.yml` untuk semua konfigurasi, pisahkan per profile (`dev`, `test`, `prod`).
    - Pastikan konfigurasi MongoDB (`quarkus.mongodb.connection-string`, `quarkus.mongodb.database`) sudah benar dan bisa di-override per profile/environment variable.
    - Aktifkan JSON logging untuk production.
    - Pastikan `quarkus.http.port=${PORT:8080}` dan `quarkus.http.host=0.0.0.0` untuk Cloud Run.

3.  **Observability**: Pastikan endpoint `/q/health`, `/q/metrics`, dan `/q/openapi` berfungsi dengan baik dan terdokumentasi.

4.  **Testing**: Selalu prioritaskan testing:
    - **Unit Tests**: Menguji class secara isolasi (service, domain logic, mapper) di `src/test/java/com/example/psychapi/service/` atau `src/test/java/com/example/psychapi/domain/`. Cepat, tidak butuh database nyata.
    - **Integration Tests**: Menguji REST endpoint secara end-to-end (melalui HTTP) di `src/test/java/com/example/psychapi/api/resource/`. Menggunakan `@QuarkusTest` dan `RestAssured`. Untuk database, gunakan Testcontainers atau MongoDB lokal. File test diberi suffix `IT.java` (misal: `UserResourceIT.java`) atau `Test.java` jika dianggap cukup.
    - **E2E Tests**: (Dikeluarkan dari scope project utama, biasanya dilakukan oleh tim QA terpisah).

5.  **Deployment (Cloud Run)**:
    - Prioritaskan native image untuk production di Cloud Run karena cold start yang cepat dan penggunaan memori yang efisien.
    - Gunakan Dockerfile yang disediakan Quarkus (`Dockerfile.jvm`, `Dockerfile.native`) yang sudah disesuaikan.
    - Pastikan aplikasi mendengarkan di port yang ditentukan oleh environment variable `PORT` (default 8080).

6.  **Bahasa**: Selalu balas menggunakan Bahasa Indonesia yang santai tapi profesional, kecuali untuk kode.

## Workflow saat coding

1.  **Analisis Task & API Contract**: Pahami kebutuhan fitur atau bug fix. Definisikan API endpoint (URI, method, request/response DTO) jika ini fitur baru.

2.  **Siapkan Struktur Folder (jika perlu)**: Buat sub-package yang dibutuhkan di `src/main/java/com/example/psychapi/` dan `src/test/java/com/example/psychapi/`.

3.  **Buat DTO & Domain Model (Entity)**:
    - Di `api/dto/`: Buat class Request DTO dan Response DTO (misal `CreateUserRequest`, `UserResponse`). Gunakan Bean Validation (`@NotBlank`, `@Size`, `@Email`) di Request DTO.
    - Di `domain/model/`: Buat class Entity MongoDB (misal `User`).

4.  **Tulis Test (TDD Approach)**:
    - **Integration Test (untuk Resource)**: Buat file test di `src/test/java/com/example/psychapi/api/resource/` (misal `UserResourceIT.java`). Tulis skenario test untuk endpoint yang akan dibuat/dimodifikasi. Test akan _gagal_ karena Resource belum ada.
    - **Unit Test (untuk Service)**: Buat file test di `src/test/java/com/example/psychapi/service/` (misal `UserServiceTest.java`). Tulis skenario test untuk business logic yang akan diimplementasikan. Test akan _gagal_ karena Service belum ada.

5.  **Implementasi Kode**:
    - **Repository**: Buat interface/class Repository di `repository/` (misal `UserRepository extends PanacheMongoRepository<User>`).
    - **Service**: Implementasikan business logic di `service/` (misal `UserService`). Suntikkan Repository yang dibutuhkan.
    - **Resource**: Buat JAX-RS Resource di `api/resource/` (misal `UserResource`). Suntikkan Service yang dibutuhkan. Tangani Request DTO, panggil Service, kembalikan Response DTO.
    - **Mapper**: Jika diperlukan, buat class Mapper di `api/mapper/` untuk konversi DTO ke Domain Model dan sebaliknya.
    - **Exception**: Jika ada custom exception, buat class-nya di `domain/exception/` dan `ExceptionMapper` di `api/exception/`.
    - **Seeder (jika perlu)**: Buat class seeder di `infrastructure/seeder/` untuk mengisi data awal. Pastikan seeder hanya berjalan di profile `dev` atau `test`, jangan di `prod`.

6.  **Verifikasi & Debugging**:
    - Jalankan `mvn quarkus:dev` di terminal untuk menjalankan aplikasi dalam dev mode.
    - Jalankan semua test (`mvn verify`). Pastikan semua test _lulus_ (hijau).
    - Gunakan debugger (VS Code debugger) jika ada masalah.

7.  **Refactor & Code Review**:
    - Perbaiki kualitas kode, pastikan sesuai dengan prinsip SOLID, DRY, dan standar coding Java/Quarkus.
    - Tambahkan komentar dokumentasi (`/** ... */`) pada class, method, dan parameter yang penting.
    - Pastikan semua konfigurasi (MongoDB, logging, dll.) sudah benar di `application.yml` dan profile lainnya.

8.  **Commit**: Buat commit message yang jelas, ringkas, dan informatif. Ikuti konvensi commit yang berlaku.

9.  **Pull Request (PR)**:
    - Buat PR ke branch `main` (atau `develop`).
    - Pastikan CI/CD (GitHub Actions) berjalan dan semua stage (build, test) lulus.
    - Minta review dari rekan tim, pastikan semua checklist code review terpenuhi.

## Seeder / Data Awal

Untuk mengisi data awal MongoDB saat aplikasi start (terutama untuk dev/test), gunakan class dengan annotation `@ApplicationScoped` dan `@Startup` atau `@Observes StartupEvent`.

### Lokasi

`src/main/java/com/example/psychapi/infrastructure/seeder/`

### Contoh class seeder

```java
package com.example.psychapi.infrastructure.seeder;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class DevDataSeeder {

    @Inject
    UserRepository userRepository;

    void onStart(@Observes StartupEvent ev) {
        if (userRepository.count() == 0) {
            // insert data awal
        }
    }
}
```

### Aturan penting seeder

- **Hanya aktif di dev/test**: Gunakan `@IfBuildProfile("dev")` atau `@IfBuildProfile("test")` pada class seeder, atau cek profile secara manual.
- **Jangan aktif di production**: Data production harus dimasukkan melalui migration script atau admin tool, bukan otomatis saat startup.
- **Idempoten**: Seeder harus aman dijalankan berkali-kali (misal: cek `count()` dulu sebelum insert).
- **Jangan untuk data sensitif**: Jangan hardcode password/token di seeder.
