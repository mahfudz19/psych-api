Pertanyaan bagus — sebelum bikin, mari bedah dulu apa yang **benar-benar berbeda** antara `referralCode` dan `inviteCode`.

Read , lines 100 to 200

Read , lines 1 to 120

Read , lines 120 to 180

Sekarang saya sudah punya gambaran lengkap. Berikut analisis perbedaan kunci:

---

## Referral Code vs Invite Code — Apa yang Harus Dibedakan?

### 1. **Tujuan & Konteks**

| Aspek                   | `referralCode`                                | `inviteCode`                                          |
| ----------------------- | --------------------------------------------- | ----------------------------------------------------- |
| **Tujuan**              | User mengajak orang **mendaftar** ke platform | User mengajak orang **masuk ke organization**         |
| **Scope**               | Platform-wide (siapa saja bisa pakai)         | Organization-scoped (terikat ke org tertentu)         |
| **Siapa yang generate** | Semua user otomatis punya                     | Hanya user dengan role tertentu di org (owner/admin?) |

### 2. **Data Model — Sudah Ada di `User.java`**

Model kamu sudah punya field `inviteCode` tapi juga punya `invitedOrganizationId`, `invitationRole`, `invitationStatus`, dll. Ini berarti invite code **harus membawa konteks tambahan**:

- **`organizationId`** — org mana yang diundang
- **`invitationRole`** — role apa yang ditawarkan (`member`, `admin`)
- **`invitationStatus`** — lifecycle: `pending` → `accepted` / `declined` / `expired`
- **Expiry** — referral code tidak expire, tapi invite code **seharusnya expire** (ada `invitationSentAt` tapi belum ada `expiresAt`)

### 3. **Perbedaan Logic yang Perlu Dibangun**

| Concern              | Referral Code                        | Invite Code                                                                    |
| -------------------- | ------------------------------------ | ------------------------------------------------------------------------------ |
| **Authorization**    | `@RolesAllowed("USER")` — semua user | Perlu cek: user harus punya `organizationId` + role `owner`/`admin`            |
| **Generate**         | Random 8 char, 1 per user            | Random code, tapi **terikat ke org + role target**                             |
| **Validate**         | Return referrer user                 | Return org info + role yang ditawarkan                                         |
| **Expiry**           | Tidak expire                         | **Harus expire** (e.g., 7 hari)                                                |
| **Status lifecycle** | Tidak ada (code aktif/archived)      | `pending` → `accepted` / `declined` / `expired`                                |
| **One-time use?**    | Bisa dipakai berkali-kali            | Kemungkinan **single-use** (1 code = 1 undangan)                               |
| **Self-use check**   | Tidak bisa referral diri sendiri     | Tidak bisa invite diri sendiri + **tidak bisa invite orang yang sudah di org** |
| **History**          | Archive di `referralCodeHistory`     | Perlu tracking siapa yang diundang, status apa                                 |

### 4. **Pertanyaan Desain Sebelum Coding**

Ini yang perlu kamu putuskan:

1. **Apakah invite code reusable atau single-use?**
   - Referral code = 1 code bisa dipakai banyak orang
   - Invite code biasanya = 1 code untuk 1 orang spesifik (atau 1 code bisa dipakai N orang?)

2. **Apakah invite code perlu target email?**
   - Kalau single-use: generate code → kirim ke email tertentu → hanya email itu yang bisa pakai
   - Kalau reusable: generate link → siapa saja yang punya link bisa join

3. **Apakah invite code disimpan di `User` atau perlu collection sendiri?**
   - Sekarang `inviteCode` ada di `User`, tapi ini awkward karena 1 user bisa invite banyak orang
   - Lebih masuk akal: **collection `invitations` terpisah** dengan `{code, orgId, invitedBy, targetEmail?, role, status, expiresAt}`

4. **Siapa yang boleh generate?**
   - Hanya org `owner`? Atau `admin` juga?

### Rekomendasi

Jangan copy-paste `ReferralService`. Perbedaan terlalu banyak. Yang bisa di-reuse hanya `generateRandomCode()` — extract itu ke shared utility, sisanya buat fresh sebagai `InvitationService` dengan model `Invitation` terpisah dari `User`.
