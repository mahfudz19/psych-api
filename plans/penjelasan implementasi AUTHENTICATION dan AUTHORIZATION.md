Tunggu sebentar, ada satu kesalahpahaman fatal di sini! Anda **SANGAT MEMBUTUHKAN** kedua filter tersebut (`AuthenticationFilter.java` dan `AuthorizationFilter.java`). Mereka adalah pasangan yang tidak bisa dipisahkan, seperti resepsionis dan satpam.

Mari kita luruskan pembagian tugasnya agar fondasi arsitektur Anda benar-benar kokoh:

1. **`AuthenticationFilter.java` (Resepsionis):** Tugasnya hanya mengenali _siapa_ yang datang. Ia mengambil token, mengecek ke _database_, membungkus data `User` ke dalam `requestContext.setProperty`, dan mendefinisikan **buku aturan (kamus role)** di dalam fungsi `isUserInRole()`. Ia _tidak pernah_ menolak akses karena masalah _role_.
2. **`AuthorizationFilter.java` (Satpam):** Tugasnya menegakkan aturan. Ia membaca pintu (_endpoint_) yang ingin dimasuki (misal ada tulisan `@RolesAllowed("USER")`), lalu ia bertanya kepada Resepsionis (memanggil fungsi `isUserInRole()`). Jika Resepsionis bilang "Tidak", Satpam akan menendang _user_ tersebut (403 Forbidden).

Jika Anda menghapus `AuthenticationFilter`, maka `AuthorizationFilter` akan buta (tidak punya objek `User` dan tidak punya fungsi `isUserInRole`), sehingga semua _request_ akan ditolak!

---

### Jawaban: Implementasi AND dan OR pada `@RolesAllowed`

Ini adalah inti kebingungan Anda. Secara bawaan (standar Java EE/JAX-RS), menuliskan dua _role_ di dalam array seperti `@RolesAllowed({"USER", "ORGANIZATION_HAS_MEMBER"})` **selalu dibaca sebagai logika OR (ATAU)**.

Jika Anda menuliskannya seperti itu: Quarkus akan mengecek "USER" dulu. Jika _user_ tersebut adalah "USER", Quarkus langsung mengizinkan masuk tanpa mempedulikan apakah dia _member_ organisasi atau bukan (pengecekan kedua dilewati sepenuhnya / _short-circuit_).

**Lalu, bagaimana cara membuat logika AND (USER "DAN" MEMBER ORGANISASI)?**

Cara paling elegan dan bersih ala _Quarkus Way_ adalah dengan menyembunyikan logika "AND" tersebut di dalam _kamus role_ Anda (di dalam `AuthenticationFilter.java`). Anda cukup menggunakan `@RolesAllowed("ORGANIZATION_HAS_MEMBER")` di _controller_, lalu biarkan filter yang bekerja keras mengevaluasi keduanya.

Berikut adalah implementasi konkret untuk kasus kompleks 2 _collection_ Anda di masa depan:

#### 1. Update `AuthenticationFilter.java` (Fungsi `isUserInRole`)

```java
@Override
public boolean isUserInRole(String role) {
    if (role == null) return false;

    // --- LOGIKA KOMPLEKS (ABAC) ---
    if (role.equals("ORGANIZATION_HAS_MEMBER")) {

        // SYARAT 1 (Logika AND): User HARUS memiliki role "USER" di array database-nya
        if (user.getRoles() == null || !user.getRoles().contains("USER")) {
            return false; // Langsung tolak jika dia bukan USER
        }

        // SYARAT 2: Ambil ID Organisasi dari URL
        // (Asumsi URL endpoint Anda adalah /api/v1/organizations/{orgId}/members)
        String orgId = requestContext.getUriInfo().getPathParameters().getFirst("orgId");
        if (orgId == null) return false;

        // SYARAT 3: Query Kompleks 2 Collection (User & OrganizationMember)
        // Contoh pemanggilan service/repository Anda:
        // OrganizationMember memberData = db.findMember(user.getId(), new ObjectId(orgId));
        Object memberData = null; // Ganti dengan hasil query asli Anda

        if (memberData != null) {
            // KEAJAIBAN: Teruskan data hasil query ke Controller!
            requestContext.setProperty("orgMemberData", memberData);
            return true; // Lolos semua syarat (AND)
        }

        return false; // Gagal di syarat kepemilikan
    }

    // --- LOGIKA DEFAULT (RBAC) ---
    List<String> userRoles = user.getRoles();
    return userRoles != null && userRoles.contains(role);
}

```

#### 2. Implementasi di Controller (Sangat Bersih)

Sekarang di _controller_, Anda hanya perlu menggunakan satu _role_ saja, karena di baliknya sudah mengandung validasi ganda (AND) sekaligus _injection_ data.

```java
@GET
@Path("/{orgId}/members")
@RolesAllowed("ORGANIZATION_HAS_MEMBER") // <-- Hanya butuh 1 role ini!
public Response getOrganizationMembers(
    @PathParam("orgId") String orgId,
    @Context ContainerRequestContext requestContext
) {
    // 1. Ambil data User seperti biasa
    User user = (User) requestContext.getProperty("validatedUser");

    // 2. Ambil data kompleks dari 2 collection yang diteruskan oleh Filter
    // (Tanpa perlu query database lagi di sini!)
    OrganizationMember memberData = (OrganizationMember) requestContext.getProperty("orgMemberData");

    // Lakukan bisnis logika...
    return ResponseHelper.ok(memberData, "Success");
}

```

### Kesimpulan Arsitektur:

- Gunakan **`@RolesAllowed({"A", "B"})`** HANYA jika Anda ingin logika **OR** (Boleh A, Boleh B, salah satu saja cukup).
- Jika Anda butuh logika **AND**, buat sebuah _String Role_ baru yang mewakili kondisi tersebut (misal: `"ORGANIZATION_HAS_MEMBER"`), lalu jahit semua pengecekan kondisinya di dalam `isUserInRole()` pada `AuthenticationFilter`.

Pendekatan ini menjamin _endpoint_ Anda tetap bersih, _database_ sangat efisien (tidak ada _query_ berulang), dan keamanan terjaga ketat tanpa kebingungan _AND/OR_. Apakah desain logika kustom ini cukup mudah untuk diterapkan pada struktur _database_ MongoDB Anda nanti?
