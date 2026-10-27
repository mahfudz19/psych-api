# Standar Error Handling (Quarkus)

Gunakan standar ini untuk menangani error dan validasi di seluruh layer aplikasi. Project ini menggunakan `GlobalExceptionHandler` untuk memetakan exception menjadi response JSON standar (`ApiErrorResponse`).

## 1. Aturan Umum

- **JANGAN** menggunakan `try-catch` di Resource/Controller untuk menangkap error bisnis. Biarkan `GlobalExceptionHandler` yang memprosesnya.
- Gunakan exception bawaan project: `ValidationException` dan `NotFoundException`.

## 2. Layer Service

- **Business Logic Error**: Lempar `ValidationException(String code, String message)`.
  Contoh: `throw new ValidationException("DUPLICATE_CODE", "Kode sudah digunakan");`
- **Data Not Found**: Lempar `NotFoundException(String code, String message)`.
  Contoh: `throw new NotFoundException("TEST_NOT_FOUND", "Test definition tidak ditemukan");`
- **Validasi ObjectId**: Gunakan `ValidationUtils.validateObjectId(id)` untuk mengonversi String ID menjadi `ObjectId`. Method ini otomatis melempar `ValidationException("INVALID_ID", ...)` jika format salah.

## 3. Layer Resource (API Endpoint)

- Cukup panggil method dari Service.
- Kembalikan response sukses menggunakan `ResponseHelper.ok(...)`.
- Tidak perlu blok `try-catch` kecuali untuk error spesifik library eksternal yang belum di-handle secara global.
