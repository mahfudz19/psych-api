# Arsitektur Collection untuk 25 Alat Tes

Rancangan ini adalah hasil gabungan dari semua pembahasan kita, termasuk koreksi dan tambahan skema (MSAI, TIU 5, IST ME, EPPS, Kraepelin, Wartegg). Ada **15 collection** dalam tiga kelompok.

| Kelompok      | Collection                                                                                                                 |
| ------------- | -------------------------------------------------------------------------------------------------------------------------- |
| A. Katalog    | `test_definitions`, `test_sections`, `test_scales`, `test_items`, `test_assets`, `norm_tables`, `interpretation_templates` |
| B. Pengerjaan | `test_assignments`, `test_attempts`, `attempt_answers`, `kraepelin_records`, `attempt_files`                               |
| C. Hasil      | `test_results`, `interpretations`, `assessment_rubrics`, `review_tasks`                                                    |

**Konvensi:** semua collection punya `createdAt`, `updatedAt` (Instant, default waktu saat dibuat), dan `deletedAt` (Instant, default `null`) untuk audit dan soft delete. Kolom "Default" berisi `-` jika field wajib diisi tanpa nilai bawaan.

---

# A. KATALOG

## 1. `test_definitions`

Satu dokumen per alat tes (25 dokumen).

| Field                    | Tipe            | Contoh Value                                                   | Default               | Tujuan                                                                                                                 |
| ------------------------ | --------------- | -------------------------------------------------------------- | --------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `_id`                    | ObjectId        | `66f1a1...`                                                    | auto                  | Primary key                                                                                                            |
| `code`                   | String (unik)   | `"BIG_FIVE"`                                                   | -                     | Kode tetap untuk kode aplikasi, URL, dan hak akses                                                                     |
| `name`                   | String          | `"Big Five Personality"`                                       | -                     | Nama lengkap                                                                                                           |
| `shortName`              | String          | `"Big Five"`                                                   | -                     | Nama singkat untuk kartu UI                                                                                            |
| `category`               | Enum            | `PERSONALITY_SCALE`                                            | -                     | `ABILITY`, `PERSONALITY_FORCED_CHOICE`, `PERSONALITY_SCALE`, `INTEREST`, `SPEED_ENDURANCE`, `PROJECTIVE`, `MANAGEMENT` |
| `description`            | String          | `"Mengukur 5 dimensi kepribadian"`                             | `""`                  | Deskripsi di katalog                                                                                                   |
| `instructions`           | String          | `"Pilih jawaban yang paling sesuai"`                           | `""`                  | Instruksi umum sebelum tes                                                                                             |
| `purposes`               | Array\<Enum\>   | `["SELF_DISCOVERY","RECRUITMENT"]`                             | `["SELF_DISCOVERY"]`  | Tujuan penggunaan, untuk filter                                                                                        |
| `version`                | String          | `"1.0.0"`                                                      | `"1.0.0"`             | Versi tes, dicatat di attempt dan hasil                                                                                |
| `status`                 | Enum            | `ACTIVE`                                                       | `DRAFT`               | `DRAFT`, `ACTIVE`, `ARCHIVED`                                                                                          |
| `scoringMode`            | Enum            | `AUTO`                                                         | `AUTO`                | `AUTO`, `MANUAL` (Wartegg), `HYBRID`                                                                                   |
| `scorerKey`              | String          | `"BIG_FIVE_V1"`                                                | -                     | Nama scorer di kode                                                                                                    |
| `scoringConfig`          | Object          | `{kCorrection:true}`                                           | `{}`                  | Parameter scorer (koreksi K MMPI, aturan EPPS, dsb.)                                                                   |
| `resultViewType`         | Enum            | `RADAR`                                                        | `PROFILE`             | Visualisasi hasil: `RADAR`, `BAR`, `PROFILE`, `TYPE_CARD`, `IQ_CARD`, `NARRATIVE_ONLY`                                 |
| `totalItems`             | Integer         | `50`                                                           | `0`                   | Jumlah soal, validasi kelengkapan                                                                                      |
| `totalDurationSeconds`   | Integer         | `null`                                                         | `null`                | Batas waktu total, null jika tanpa batas                                                                               |
| `estimatedMinutes`       | Integer         | `15`                                                           | `0`                   | Estimasi pengerjaan                                                                                                    |
| `pausePolicy`            | Enum            | `FREE`                                                         | `FREE`                | `FREE`, `BETWEEN_SECTIONS_ONLY`, `NONE`                                                                                |
| `disconnectGraceSeconds` | Integer         | `120`                                                          | `120`                 | Toleransi koneksi putus                                                                                                |
| `itemOrderPolicy`        | Enum            | `FIXED`                                                        | `FIXED`               | `FIXED` atau `RANDOMIZED`                                                                                              |
| `targetAge`              | Object          | `{min:17,max:65}`                                              | `{min:null,max:null}` | Rentang usia valid                                                                                                     |
| `minEducation`           | Enum            | `SMA`                                                          | `null`                | Syarat pendidikan minimum                                                                                              |
| `language`               | String          | `"id"`                                                         | `"id"`                | Bahasa soal                                                                                                            |
| `requiredFeature`        | String          | `"test.big_five"`                                              | -                     | Kunci hak akses ke subscription                                                                                        |
| `resultAccessLevel`      | Enum            | `STANDARD`                                                     | `STANDARD`            | `STANDARD` atau `RESTRICTED` (MMPI wajib review)                                                                       |
| `config`                 | Object          | `{columns:50,secondsPerColumn:60}`                             | `{}`                  | Konfigurasi unik per tes (Kraepelin)                                                                                   |
| `license`                | Object          | `{owner:"LPSP3 UI",status:"LICENSED",validUntil:"2027-12-31"}` | `{status:"UNKNOWN"}`  | Status hak penggunaan tes                                                                                              |
| `thumbnailUrl`           | String          | `"https://cdn.../bigfive.png"`                                 | `null`                | Gambar kartu                                                                                                           |
| `tags`                   | Array\<String\> | `["kepribadian"]`                                              | `[]`                  | Pencarian dan filter                                                                                                   |

## 2. `test_sections`

Subtes. Tes tanpa subtes tetap punya satu section.

| Field               | Tipe              | Contoh Value                                                  | Default         | Tujuan                                                 |
| ------------------- | ----------------- | ------------------------------------------------------------- | --------------- | ------------------------------------------------------ |
| `_id`               | ObjectId          | `66f2b1...`                                                   | auto            | Primary key                                            |
| `testId`            | ObjectId          | `66f1a1...`                                                   | -               | Referensi ke `test_definitions`                        |
| `testVersion`       | String            | `"1.0.0"`                                                     | -               | Versi tes pemilik                                      |
| `code`              | String            | `"SE"`                                                        | -               | Kode subtes, unik per tes                              |
| `order`             | Integer           | `1`                                                           | -               | Urutan pengerjaan                                      |
| `name`              | String            | `"Melengkapi Kalimat"`                                        | -               | Nama subtes                                            |
| `instructions`      | String            | `"Pilih kata yang melengkapi kalimat"`                        | `""`            | Instruksi subtes                                       |
| `examples`          | Array\<Object\>   | `[{contentId:..., answer:"c", explanation:"..."}]`            | `[]`            | Contoh dan latihan (IST, CFIT, TIU 5)                  |
| `showExamplesFirst` | Boolean           | `true`                                                        | `true`          | Tampilkan contoh sebelum soal                          |
| `timeLimitSeconds`  | Integer           | `360`                                                         | `null`          | Batas waktu subtes                                     |
| `itemCount`         | Integer           | `20`                                                          | `0`             | Jumlah soal, validasi                                  |
| `navigationPolicy`  | Enum              | `FORWARD_ONLY`                                                | `FREE`          | `FREE` atau `FORWARD_ONLY`                             |
| `defaultItemType`   | Enum              | `SINGLE_CHOICE`                                               | `SINGLE_CHOICE` | Komponen UI bawaan                                     |
| `scaleIds`          | Array\<ObjectId\> | `[66f3c1...]`                                                 | `[]`            | Skala yang dinilai, mempercepat scoring                |
| `stimulusPhase`     | Object            | `{contentType:"WORD_LIST",displaySeconds:180,hideAfter:true}` | `null`          | Fase hafalan (IST ME): tampil dulu, lalu disembunyikan |
| `config`            | Object            | `{autoAdvance:true}`                                          | `{}`            | Konfigurasi fleksibel                                  |
| `status`            | Enum              | `ACTIVE`                                                      | `ACTIVE`        | `ACTIVE` atau `RETIRED`                                |

## 3. `test_scales`

Kamus hasil: dimensi, faktor, subtes, dan skor gabungan.

| Field                    | Tipe     | Contoh Value                          | Default       | Tujuan                                                                                                                                   |
| ------------------------ | -------- | ------------------------------------- | ------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `_id`                    | ObjectId | `66f3c1...`                           | auto          | Primary key                                                                                                                              |
| `testId`                 | ObjectId | `66f1a1...`                           | -             | Referensi ke tes                                                                                                                         |
| `code`                   | String   | `"O"`                                 | -             | Kode skala, unik per tes                                                                                                                 |
| `name`                   | String   | `"Openness"`                          | -             | Nama tampilan                                                                                                                            |
| `type`                   | Enum     | `DIMENSION`                           | `DIMENSION`   | `DIMENSION`, `FACET`, `SUBTEST`, `VALIDITY`, `COMPOSITE`, `TYPE_POLE`                                                                    |
| `role`                   | Enum     | `PERFORMANCE`                         | `PERFORMANCE` | `PERFORMANCE`, `EXPECTATION`, `IMPORTANCE`, `REFLECTION` (MSAI mengukur aktivitas sama 3 kali)                                           |
| `parentScaleId`          | ObjectId | `null`                                | `null`        | Hierarki (MSAI: 4 budaya → 12 aktivitas; Big Five facet)                                                                                 |
| `sectionId`              | ObjectId | `66f2b1...`                           | `null`        | Terisi jika skala = satu subtes (IST)                                                                                                    |
| `order`                  | Integer  | `5`                                   | `0`           | Urutan di laporan                                                                                                                        |
| `description`            | String   | `"Keterbukaan terhadap ide baru"`     | `""`          | Penjelasan untuk laporan                                                                                                                 |
| `lowLabel` / `highLabel` | String   | `"Konvensional"` / `"Imajinatif"`     | `null`        | Label kutub rendah dan tinggi                                                                                                            |
| `rawMin` / `rawMax`      | Integer  | `10` / `50`                           | `null`        | Rentang skor mentah                                                                                                                      |
| `scoreType`              | Enum     | `RAW_TO_T`                            | `RAW_ONLY`    | `RAW_ONLY`, `RAW_AVERAGE`, `RAW_TO_SW`, `RAW_TO_WS`, `RAW_TO_IQ`, `RAW_TO_T`, `RAW_TO_STEN`, `RAW_TO_PERCENTILE`, `IPSATIVE`, `RANK_SUM` |
| `composite`              | Object   | `{sourceScaleIds:[...],method:"SUM"}` | `null`        | Skor gabungan (total IST, IQ, total AQ)                                                                                                  |
| `isReported`             | Boolean  | `true`                                | `true`        | Tampil di laporan peserta                                                                                                                |
| `isValidityScale`        | Boolean  | `false`                               | `false`       | Menandai skala validitas (MMPI: L, F, K)                                                                                                 |
| `color`                  | String   | `"#4F46E5"`                           | `null`        | Warna grafik                                                                                                                             |

## 4. `test_items`

Bank soal (collection terbesar dan paling sensitif).

| Field              | Tipe            | Contoh Value                  | Default  | Tujuan                                                              |
| ------------------ | --------------- | ----------------------------- | -------- | ------------------------------------------------------------------- |
| `_id`              | ObjectId        | `66f4d1...`                   | auto     | Primary key                                                         |
| `testId`           | ObjectId        | `66f1a1...`                   | -        | Referensi ke tes                                                    |
| `sectionId`        | ObjectId        | `66f2b1...`                   | -        | Referensi ke section                                                |
| `testVersion`      | String          | `"1.0.0"`                     | -        | Versi tes saat soal dibuat                                          |
| `number`           | Integer         | `7`                           | -        | Nomor urut dalam section                                            |
| `type`             | Enum            | `LIKERT`                      | -        | Tipe soal (tabel di bawah)                                          |
| `content`          | Object          | `{text:"...",images:[...]}`   | -        | Isi soal (sub-objek di bawah)                                       |
| `options`          | Array\<Object\> | lihat sub-objek               | `[]`     | Opsi jawaban                                                        |
| `groupId`          | String          | `"grp-12"`                    | `null`   | Beberapa soal berbagi satu bacaan atau gambar                       |
| `scaleId`          | ObjectId        | `66f3c1...`                   | `null`   | Skala yang dinilai (soal satu skala)                                |
| `reverseScored`    | Boolean         | `false`                       | `false`  | Skor dibalik (Big Five, 16PF)                                       |
| `weight`           | Double          | `1.0`                         | `1.0`    | Bobot soal                                                          |
| `scoredInTotal`    | Boolean         | `true`                        | `true`   | `false` untuk soal yang disimpan tetapi tidak dinilai (MSAI Q61-87) |
| `consistencyGroup` | String          | `"EPPS_C_03"`                 | `null`   | Menautkan soal yang mengulang soal lain (konsistensi EPPS)          |
| `answerKey`        | Object          | `{correct:["3"],accepted:[]}` | `null`   | Kunci tes kemampuan. **Jangan dikirim ke klien**                    |
| `timeLimitSeconds` | Integer         | `null`                        | `null`   | Batas waktu per soal (Wartegg per kotak)                            |
| `constraints`      | Object          | `{minSelect:1,maxSelect:2}`   | `{}`     | Aturan jawaban (jumlah pilihan, rentang angka, jumlah ranking)      |
| `difficulty`       | Double          | `0.62`                        | `null`   | Hasil kalibrasi, untuk tes adaptif nanti                            |
| `isExample`        | Boolean         | `false`                       | `false`  | Soal contoh atau latihan, tidak dihitung                            |
| `status`           | Enum            | `ACTIVE`                      | `ACTIVE` | `ACTIVE` atau `RETIRED`                                             |
| `tags`             | Array\<String\> | `["verbal"]`                  | `[]`     | Pengelompokan                                                       |

**Sub-objek `content`:**

| Field          | Tipe            | Contoh                                                        | Default | Tujuan                                           |
| -------------- | --------------- | ------------------------------------------------------------- | ------- | ------------------------------------------------ |
| `text`         | String          | `"Saya mudah bergaul"`                                        | `null`  | Teks soal                                        |
| `images`       | Array\<Object\> | `[{label:"A",assetId:"..."},{label:"B",...},{label:"C",...}]` | `[]`    | Gambar stimulus (TIU 5 punya A, B, C; SPM, CFIT) |
| `audioAssetId` | ObjectId        | `null`                                                        | `null`  | Stimulus audio, jika ada                         |

**Sub-objek `options`:**

| Field     | Tipe     | Contoh            | Default      | Tujuan                                                 |
| --------- | -------- | ----------------- | ------------ | ------------------------------------------------------ |
| `key`     | String   | `"a"` atau `"3"`  | -            | Identitas opsi, disimpan sebagai jawaban               |
| `label`   | String   | `"Sangat setuju"` | `null`       | Teks opsi                                              |
| `assetId` | ObjectId | `66f7g1...`       | `null`       | Opsi berupa gambar                                     |
| `order`   | Integer  | `5`               | urutan array | Urutan tampil                                          |
| `scoring` | Object   | lihat di bawah    | `null`       | Aturan skor (kepribadian). **Jangan dikirim ke klien** |

**Isi `scoring`:** `{points:5}` (Likert), `{scaleMappings:[{scaleId,points}]}` (Holland, EPPS, Papikostick), `{most:{scaleMappings:[...]}, least:{scaleMappings:[...]}}` (DISC, Kuder).

**Nilai `type` yang didukung:**

| Type                          | Dipakai oleh                                  |
| ----------------------------- | --------------------------------------------- |
| `SINGLE_CHOICE`               | IST, CFIT, SPM, WPT, TIU 5/6, FRT, Army Alpha |
| `MULTI_CHOICE`                | CFIT (soal dua jawaban), Army Alpha           |
| `TRUE_FALSE`, `YES_NO`        | MMPI, Army Alpha                              |
| `LIKERT`                      | Big Five, AQ, Enneagram, MSAI, Holland        |
| `FORCED_CHOICE`               | EPPS, Papikostick, MBTI, MSDT, 16PF           |
| `MOST_LEAST`                  | DISC, Kuder                                   |
| `RANKING`                     | RMIB                                          |
| `NUMERIC_INPUT`, `TEXT_INPUT` | IST (RA, ZR, ME), WPT                         |
| `DRAWING`                     | Wartegg                                       |
| `MATRIX_TASK`                 | Kraepelin                                     |

## 5. `test_assets`

Metadata gambar dan audio. File fisik disimpan di object storage.

| Field              | Tipe     | Contoh Value                    | Default | Tujuan                            |
| ------------------ | -------- | ------------------------------- | ------- | --------------------------------- |
| `_id`              | ObjectId | `66f7g1...`                     | auto    | Primary key                       |
| `testId`           | ObjectId | `66f1a1...`                     | -       | Tes pemilik aset                  |
| `type`             | Enum     | `IMAGE`                         | `IMAGE` | `IMAGE`, `AUDIO`                  |
| `storageKey`       | String   | `"tests/tiu5/q07-a.png"`        | -       | Lokasi di object storage          |
| `url`              | String   | `"https://cdn.../q07-a.png"`    | -       | URL akses (dapat bertanda tangan) |
| `mimeType`         | String   | `"image/png"`                   | -       | Tipe berkas                       |
| `sizeBytes`        | Long     | `18432`                         | -       | Ukuran                            |
| `checksum`         | String   | `"sha256:ab12..."`              | -       | Validasi integritas saat impor    |
| `width` / `height` | Integer  | `400` / `300`                   | `null`  | Dimensi gambar                    |
| `altText`          | String   | `"Segitiga diputar 90 derajat"` | `null`  | Aksesibilitas                     |

## 6. `norm_tables`

Konversi skor mentah ke skor standar.

| Field                      | Tipe            | Contoh Value                                             | Default        | Tujuan                                                       |
| -------------------------- | --------------- | -------------------------------------------------------- | -------------- | ------------------------------------------------------------ |
| `_id`                      | ObjectId        | `66f5e1...`                                              | auto           | Primary key                                                  |
| `testId`                   | ObjectId        | `66f1a1...`                                              | -              | Referensi ke tes                                             |
| `testVersion`              | String          | `"1.0.0"`                                                | -              | Versi tes yang cocok                                         |
| `normVersion`              | String          | `"2024.1"`                                               | -              | Versi norma, dicatat di hasil                                |
| `name`                     | String          | `"Norma TIU 5 Umum"`                                     | -              | Nama norma                                                   |
| `scaleId`                  | ObjectId        | `66f3c1...`                                              | `null`         | Skala yang dikonversi                                        |
| `sectionId`                | ObjectId        | `null`                                                   | `null`         | Terisi jika norma per subtes (IST)                           |
| `targetScoreType`          | Enum            | `WS`                                                     | -              | `SW`, `WS`, `IQ`, `T_SCORE`, `STEN`, `STANINE`, `PERCENTILE` |
| `method`                   | Enum            | `LOOKUP`                                                 | `LOOKUP`       | `LOOKUP` atau `MEAN_SD`                                      |
| `criteria`                 | Object          | `{ageMin:null,ageMax:null,gender:"ANY",education:"ANY"}` | semua `ANY`    | Kelompok pembanding, dicocokkan dengan snapshot peserta      |
| `entries`                  | Array\<Object\> | lihat sub-objek                                          | `[]`           | Baris konversi (`LOOKUP`)                                    |
| `mean` / `sd`              | Double          | `35.2` / `6.1`                                           | `null`         | Parameter `MEAN_SD`                                          |
| `categoryCutoffs`          | Array\<Object\> | `[{from:0,to:5,label:"KS"}]`                             | `[]`           | Batas kategori                                               |
| `sampleSize`               | Integer         | `1200`                                                   | `null`         | Bukti kualitas norma                                         |
| `source`                   | String          | `"Manual TIU 5 LPSP3 UI"`                                | `null`         | Sumber norma                                                 |
| `validFrom` / `validUntil` | Instant         | `2024-01-01` / `null`                                    | `now` / `null` | Masa berlaku                                                 |
| `status`                   | Enum            | `ACTIVE`                                                 | `DRAFT`        | `DRAFT`, `ACTIVE`, `ARCHIVED`                                |

**Sub-objek `entries`:** `rawMin` (Integer, `29`), `rawMax` (Integer, `30`), `standard` (Double, `16`, null jika rentang), `standardMin`/`standardMax` (Double, `17`/`20`, untuk baris yang memetakan ke rentang), `percentile` (Double, `null`), `category` (String, `"BS"`).

## 7. `interpretation_templates`

Narasi siap pakai untuk mode Template, konteks AI, dan draf psikolog.

| Field                                                                      | Tipe            | Contoh Value                    | Default       | Tujuan                                                        |
| -------------------------------------------------------------------------- | --------------- | ------------------------------- | ------------- | ------------------------------------------------------------- |
| `_id`                                                                      | ObjectId        | `66f6f1...`                     | auto          | Primary key                                                   |
| `testId`                                                                   | ObjectId        | `66f1a1...`                     | -             | Referensi ke tes                                              |
| `testVersion`                                                              | String          | `"1.0.0"`                       | -             | Versi tes                                                     |
| `scope`                                                                    | Enum            | `SCALE_LEVEL`                   | -             | `SCALE_LEVEL`, `TYPE`, `COMBINATION`, `GAP` (MSAI), `OVERALL` |
| `scaleId`                                                                  | ObjectId        | `66f3c1...`                     | `null`        | Skala yang dijelaskan                                         |
| `levelKey`                                                                 | String          | `"HIGH"`                        | `null`        | `LOW`, `MEDIUM`, `HIGH`, atau kategori (BS, B, S, K, KS)      |
| `typeKey`                                                                  | String          | `"ENTJ"`                        | `null`        | Tipe atau kombinasi (`"ENNEAGRAM_3"`, `"RIA"`, `"D-I"`)       |
| `scoreRange`                                                               | Object          | `{min:60,max:100}`              | `null`        | Rentang skor pemicu                                           |
| `language`                                                                 | String          | `"id"`                          | `"id"`        | Bahasa narasi                                                 |
| `audience`                                                                 | Enum            | `PARTICIPANT`                   | `PARTICIPANT` | `PARTICIPANT`, `ORGANIZATION`, `PSYCHOLOGIST`                 |
| `title`                                                                    | String          | `"Keterbukaan Tinggi"`          | -             | Judul bagian laporan                                          |
| `summary`                                                                  | String          | `"Anda cenderung terbuka..."`   | `""`          | Ringkasan singkat                                             |
| `narrative`                                                                | String          | `"Orang dengan skor tinggi..."` | `""`          | Penjelasan utama                                              |
| `strengths` / `developmentAreas` / `recommendations` / `careerSuggestions` | Array\<String\> | `["Kreatif"]`                   | `[]`          | Kekuatan, area pengembangan, saran, dan saran karier          |
| `requiredFeature`                                                          | String          | `"report.detailed"`             | `null`        | Hak akses untuk konten detail                                 |
| `version`                                                                  | String          | `"1.2"`                         | `"1.0"`       | Versi template, dicatat di hasil                              |
| `status`                                                                   | Enum            | `ACTIVE`                        | `DRAFT`       | `DRAFT`, `ACTIVE`, `ARCHIVED`                                 |

---

# B. PENGERJAAN

## 8. `test_assignments`

Pusat alur. Pada self-discovery, dibuat otomatis saat user menekan "mulai tes".

| Field                              | Tipe          | Contoh Value                                                                          | Default          | Tujuan                                                    |
| ---------------------------------- | ------------- | ------------------------------------------------------------------------------------- | ---------------- | --------------------------------------------------------- |
| `_id`                              | ObjectId      | `66f8h1...`                                                                           | auto             | Primary key                                               |
| `workspaceId`                      | ObjectId      | `66a1...`                                                                             | -                | Pemilik (workspace pribadi atau organisasi)               |
| `participantId`                    | ObjectId      | `66a2...`                                                                             | -                | Peserta (bisa tanpa akun)                                 |
| `userId`                           | ObjectId      | `66a3...`                                                                             | `null`           | Akun peserta jika ada                                     |
| `assignedBy`                       | ObjectId      | `66a3...`                                                                             | `= userId`       | Pemberi tugas (peserta sendiri pada self-discovery)       |
| `testId`                           | ObjectId      | `66f1a1...`                                                                           | -                | Tes yang ditugaskan                                       |
| `testVersion`                      | String        | `"1.0.0"`                                                                             | -                | Versi tes yang dibekukan                                  |
| `packageId`                        | ObjectId      | `66p1...`                                                                             | `null`           | Paket baterai (jika dari paket)                           |
| `subscriptionId`                   | ObjectId      | `66s1...`                                                                             | `null`           | Langganan yang dipakai, untuk entitlement                 |
| `policyProfile`                    | Object        | `{deliveryMode:"ASYNC",resultVisibility:"PARTICIPANT",interpretationMode:"TEMPLATE"}` | profil "Mandiri" | Aturan main (snapshot kebijakan)                          |
| `purpose`                          | Enum          | `SELF_DISCOVERY`                                                                      | `SELF_DISCOVERY` | `SELF_DISCOVERY` atau `RECRUITMENT`                       |
| `accessToken`                      | String (unik) | `"tok_8f3a..."`                                                                       | auto             | Link unik untuk peserta tanpa akun                        |
| `availableFrom` / `availableUntil` | Instant       | `2026-10-02` / `2026-10-09`                                                           | `now` / `null`   | Jendela ketersediaan (asinkron)                           |
| `maxAttempts`                      | Integer       | `1`                                                                                   | `1`              | Batas percobaan ulang                                     |
| `participantSnapshot`              | Object        | `{age:27,gender:"F",education:"S1",position:null}`                                    | -                | Data pemilih norma, tidak berubah walau profil berubah    |
| `consentId`                        | ObjectId      | `66c1...`                                                                             | `null`           | Persetujuan yang terekam                                  |
| `status`                           | Enum          | `CREATED`                                                                             | `CREATED`        | `CREATED`, `STARTED`, `COMPLETED`, `EXPIRED`, `CANCELLED` |

## 9. `test_attempts`

Satu dokumen per percobaan mengerjakan satu tes.

| Field                           | Tipe              | Contoh Value                                            | Default        | Tujuan                                                    |
| ------------------------------- | ----------------- | ------------------------------------------------------- | -------------- | --------------------------------------------------------- |
| `_id`                           | ObjectId          | `66f9i1...`                                             | auto           | Primary key                                               |
| `assignmentId`                  | ObjectId          | `66f8h1...`                                             | -              | Referensi ke assignment                                   |
| `workspaceId` / `participantId` | ObjectId          |                                                         | -              | Disalin untuk query cepat dan kontrol akses               |
| `testId` / `testVersion`        | ObjectId / String |                                                         | -              | Tes dan versi yang dikerjakan                             |
| `attemptNumber`                 | Integer           | `1`                                                     | `1`            | Percobaan ke-berapa                                       |
| `status`                        | Enum              | `IN_PROGRESS`                                           | `IN_PROGRESS`  | `IN_PROGRESS`, `COMPLETED`, `ABANDONED`, `EXPIRED`        |
| `currentSectionId`              | ObjectId          | `66f2b1...`                                             | pertama        | Section aktif                                             |
| `sectionStates`                 | Array\<Object\>   | `[{sectionId,status,startedAt,deadlineAt,completedAt}]` | `[]`           | Status dan batas waktu absolut per section (timer server) |
| `startedAt` / `completedAt`     | Instant           |                                                         | `now` / `null` | Waktu mulai dan selesai                                   |
| `lastActivityAt`                | Instant           |                                                         | `now`          | Deteksi sesi menggantung dan scheduler penutup            |
| `pauseLog`                      | Array\<Object\>   | `[{pausedAt,resumedAt}]`                                | `[]`           | Riwayat jeda dan lanjut                                   |
| `clientInfo`                    | Object            | `{ip,userAgent,device}`                                 | `{}`           | Info perangkat, kunci satu sesi aktif                     |
| `anomalyFlags`                  | Array\<Object\>   | `[{type:"TAB_SWITCH",count:3}]`                         | `[]`           | Sinyal anomali                                            |
| `proctored`                     | Boolean           | `false`                                                 | `false`        | Penanda unproctored pada hasil                            |
| `scoringStatus`                 | Enum              | `PENDING`                                               | `PENDING`      | `PENDING`, `RUNNING`, `DONE`, `FAILED`, `NOT_APPLICABLE`  |

## 10. `attempt_answers`

Satu dokumen per section per attempt. Aman dari batas ukuran dokumen untuk tes besar (MMPI 567 soal).

| Field       | Tipe            | Contoh Value    | Default | Tujuan                                                |
| ----------- | --------------- | --------------- | ------- | ----------------------------------------------------- |
| `_id`       | ObjectId        | `66fai1...`     | auto    | Primary key                                           |
| `attemptId` | ObjectId        | `66f9i1...`     | -       | Referensi ke attempt                                  |
| `sectionId` | ObjectId        | `66f2b1...`     | -       | Section yang dijawab                                  |
| `testId`    | ObjectId        |                 | -       | Untuk query analitik                                  |
| `answers`   | Array\<Object\> | lihat sub-objek | `[]`    | Daftar jawaban                                        |
| `status`    | Enum            | `OPEN`          | `OPEN`  | `OPEN`, `SUBMITTED`, `AUTO_CLOSED`                    |
| `savedAt`   | Instant         |                 | `now`   | Autosave terakhir                                     |
| `revision`  | Integer         | `12`            | `0`     | Penguncian optimis agar autosave tidak saling menimpa |

**Sub-objek `answers`:** `itemId` (ObjectId), `number` (Integer), `response` (Object), `answeredAt` (Instant), `responseTimeMs` (Integer), `order` (Integer, urutan pengerjaan).

**Bentuk `response`:** `{keys:["c"]}` (pilihan), `{value:"12"}` (isian), `{most:"a",least:"c"}` (DISC, Kuder), `{ranking:["a","c","b"]}` (RMIB), `{fileId:"..."}` (Wartegg).

## 11. `kraepelin_records`

Data mentah khusus Kraepelin. Soal dibangkitkan dari `seed`, bukan disimpan di bank soal.

| Field            | Tipe            | Contoh Value                                                         | Default                        | Tujuan                                                       |
| ---------------- | --------------- | -------------------------------------------------------------------- | ------------------------------ | ------------------------------------------------------------ |
| `_id`            | ObjectId        | `66fbj1...`                                                          | auto                           | Primary key                                                  |
| `attemptId`      | ObjectId (unik) | `66f9i1...`                                                          | -                              | Satu rekaman per attempt                                     |
| `configSnapshot` | Object          | `{columnCount:40,digitsPerColumn:50,secondsPerColumn:30,seed:48211}` | dari `test_definitions.config` | Konfigurasi dibekukan, `seed` memungkinkan soal direproduksi |
| `columns`        | Array\<Object\> | lihat sub-objek                                                      | `[]`                           | Data per kolom                                               |
| `totals`         | Object          | `{answered:1840,correct:1790,wrong:50,skipped:12}`                   | `null`                         | Ringkasan untuk scoring                                      |

**Sub-objek `columns`:** `index` (Integer), `startedAt`/`endedAt` (Instant), `answered`/`correct`/`wrong`/`skipped` (Integer), `intervals` (Array: `[{second:5,count:6,wrong:0}]`, data kecepatan per interval).

## 12. `attempt_files`

Metadata berkas hasil kerja peserta (Wartegg).

| Field        | Tipe     | Contoh Value                  | Default | Tujuan                                                  |
| ------------ | -------- | ----------------------------- | ------- | ------------------------------------------------------- |
| `_id`        | ObjectId | `66fck1...`                   | auto    | Primary key                                             |
| `attemptId`  | ObjectId | `66f9i1...`                   | -       | Referensi ke attempt                                    |
| `itemId`     | ObjectId | `66f4d1...`                   | -       | Soal terkait (satu kotak Wartegg)                       |
| `boxNumber`  | Integer  | `3`                           | `null`  | Nomor kotak Wartegg                                     |
| `storageKey` | String   | `"attempts/66f9.../box3.png"` | -       | Lokasi di object storage                                |
| `mimeType`   | String   | `"image/png"`                 | -       | Tipe berkas                                             |
| `sizeBytes`  | Long     | `52311`                       | -       | Ukuran                                                  |
| `checksum`   | String   | `"sha256:..."`                | -       | Integritas                                              |
| `strokeData` | Object   | `{strokes:[...]}`             | `null`  | Data goresan (opsional, untuk replay urutan menggambar) |
| `uploadedAt` | Instant  |                               | `now`   | Waktu unggah                                            |

---

# C. HASIL

## 13. `test_results`

Hasil perhitungan. Satu dokumen per attempt.

| Field                           | Tipe              | Contoh Value                                          | Default                  | Tujuan                                                       |
| ------------------------------- | ----------------- | ----------------------------------------------------- | ------------------------ | ------------------------------------------------------------ |
| `_id`                           | ObjectId          | `66fdl1...`                                           | auto                     | Primary key                                                  |
| `attemptId`                     | ObjectId (unik)   | `66f9i1...`                                           | -                        | Menjamin idempoten, scoring dua kali tidak membuat dua hasil |
| `workspaceId` / `participantId` | ObjectId          |                                                       | -                        | Kontrol akses                                                |
| `testId` / `testVersion`        | ObjectId / String |                                                       | -                        | Tes dan versinya                                             |
| `scorerVersion`                 | String            | `"BIG_FIVE_V1.2"`                                     | -                        | Versi scorer, agar bisa dihitung ulang                       |
| `normVersion`                   | String            | `"2024.1"`                                            | `null`                   | Norma yang dipakai                                           |
| `status`                        | Enum              | `CALCULATED`                                          | `PENDING`                | `PENDING`, `CALCULATED`, `AWAITING_MANUAL`, `FINAL`          |
| `scaleScores`                   | Array\<Object\>   | lihat sub-objek                                       | `[]`                     | Skor per skala atau subtes                                   |
| `derived`                       | Object            | `{type:"ENTJ",hollandCode:"RIA",discPattern:"D-I"}`   | `{}`                     | Hasil turunan (tipe, kode, pola)                             |
| `validity`                      | Object            | `{ok:true,flags:[]}`                                  | `{ok:true,flags:[]}`     | Hasil skala validitas (MMPI, 16PF, EPPS konsistensi)         |
| `gapAnalysis`                   | Array\<Object\>   | `[{scaleId,performance:3.2,importance:4.5,gap:-1.3}]` | `null`                   | Selisih kinerja dan kepentingan (MSAI)                       |
| `manualAssessment`              | Object            | `{rubricId,aspects:[{key,score,note}],assessedBy}`    | `null`                   | Penilaian psikolog (Wartegg)                                 |
| `conditions`                    | Object            | `{proctored:false,deliveryMode:"ASYNC"}`              | `{}`                     | Konteks pengerjaan                                           |
| `requiresReview`                | Boolean           | `false`                                               | dari `resultAccessLevel` | Tahan hasil sampai psikolog menyetujui                       |
| `calculatedAt`                  | Instant           |                                                       | `now`                    | Waktu perhitungan                                            |

**Sub-objek `scaleScores`:** `scaleId` (ObjectId), `code` (String, `"SE"`), `raw` (Double, `14`), `standard` (Double, `105`), `standardType` (Enum, `SW`), `standardMin`/`standardMax` (Double, `null`), `percentile` (Double, `null`), `category` (String, `"TINGGI"`), `rank` (Integer, urutan skala, untuk Holland dan RMIB).

## 14. `interpretations`

Interpretasi berversi dari berbagai sumber. Tidak ditimpa, hanya ditambah versi baru.

| Field                       | Tipe               | Contoh Value                                                                  | Default       | Tujuan                                                                             |
| --------------------------- | ------------------ | ----------------------------------------------------------------------------- | ------------- | ---------------------------------------------------------------------------------- |
| `_id`                       | ObjectId           | `66fem1...`                                                                   | auto          | Primary key                                                                        |
| `resultId`                  | ObjectId           | `66fdl1...`                                                                   | -             | Hasil yang diinterpretasi                                                          |
| `attemptId`                 | ObjectId           |                                                                               | -             | Akses cepat                                                                        |
| `source`                    | Enum               | `AI`                                                                          | -             | `TEMPLATE`, `AI`, `PSYCHOLOGIST`                                                   |
| `versionNo`                 | Integer            | `2`                                                                           | `1`           | Nomor versi                                                                        |
| `status`                    | Enum               | `DRAFT`                                                                       | `DRAFT`       | `DRAFT`, `IN_REVIEW`, `FINAL`, `PUBLISHED`                                         |
| `isOfficial`                | Boolean            | `false`                                                                       | `false`       | Penanda versi resmi yang dilihat pembaca                                           |
| `audience`                  | Enum               | `PARTICIPANT`                                                                 | `PARTICIPANT` | `PARTICIPANT`, `ORGANIZATION`, `PSYCHOLOGIST`                                      |
| `content`                   | Object             | `{overall:"...",sections:[{scaleId,title,narrative,...}],recommendations:[]}` | `{}`          | Isi interpretasi                                                                   |
| `authorId`                  | ObjectId           | `66u1...`                                                                     | `null`        | Penulis (psikolog), null untuk sistem atau AI                                      |
| `basedOnId`                 | ObjectId           | `66fem0...`                                                                   | `null`        | Versi sebelumnya (draf AI → revisi psikolog)                                       |
| `templateVersion`           | String             | `"1.2"`                                                                       | `null`        | Versi template yang dipakai                                                        |
| `aiMeta`                    | Object             | `{model:"...",promptVersion:"v3",logId:"..."}`                                | `null`        | Jejak pemanggilan AI                                                               |
| `disclosureLabel`           | Enum               | `AI_GENERATED`                                                                | dari `source` | `TEMPLATE`, `AI_GENERATED`, `AI_REVIEWED`, `PSYCHOLOGIST` (label wajib di laporan) |
| `approvedBy` / `approvedAt` | ObjectId / Instant |                                                                               | `null`        | Persetujuan psikolog                                                               |
| `publishedAt`               | Instant            |                                                                               | `null`        | Waktu terbit ke pembaca                                                            |

## 15. `assessment_rubrics`

Rubrik penilaian manual (Wartegg dan tes yang dinilai psikolog).

| Field     | Tipe            | Contoh Value                                                       | Default | Tujuan                                  |
| --------- | --------------- | ------------------------------------------------------------------ | ------- | --------------------------------------- |
| `_id`     | ObjectId        | `66fgn1...`                                                        | auto    | Primary key                             |
| `testId`  | ObjectId        | `66f1a1...`                                                        | -       | Tes pemilik rubrik                      |
| `code`    | String          | `"WARTEGG_STD"`                                                    | -       | Kode rubrik                             |
| `version` | String          | `"1.0"`                                                            | `"1.0"` | Versi rubrik                            |
| `aspects` | Array\<Object\> | `[{key:"EMOTION",name:"Emosi",scale:{min:1,max:5},anchors:[...]}]` | `[]`    | Aspek penilaian dan skalanya            |
| `perItem` | Boolean         | `true`                                                             | `false` | Dinilai per kotak/soal atau keseluruhan |
| `status`  | Enum            | `ACTIVE`                                                           | `DRAFT` | `DRAFT`, `ACTIVE`, `ARCHIVED`           |

## 16. `review_tasks`

Antrean tugas untuk psikolog (asinkron, ber-SLA).

| Field                       | Tipe            | Contoh Value                        | Default          | Tujuan                                                                      |
| --------------------------- | --------------- | ----------------------------------- | ---------------- | --------------------------------------------------------------------------- |
| `_id`                       | ObjectId        | `66fho1...`                         | auto             | Primary key                                                                 |
| `resultId`                  | ObjectId        | `66fdl1...`                         | -                | Hasil yang direview                                                         |
| `attemptId` / `workspaceId` | ObjectId        |                                     | -                | Akses cepat dan kontrol                                                     |
| `reason`                    | Enum            | `PROJECTIVE_TEST`                   | -                | `PROJECTIVE_TEST`, `RESTRICTED_TEST`, `PURCHASED_REVIEW`, `AI_DRAFT_REVIEW` |
| `status`                    | Enum            | `OPEN`                              | `OPEN`           | `OPEN`, `CLAIMED`, `IN_REVIEW`, `DONE`, `ESCALATED`, `CANCELLED`            |
| `assigneeId`                | ObjectId        | `66u1...`                           | `null`           | Psikolog yang mengklaim                                                     |
| `priority`                  | Integer         | `2`                                 | `0`              | Urutan antrean                                                              |
| `slaHours`                  | Integer         | `48`                                | `48`             | Target waktu penyelesaian                                                   |
| `dueAt`                     | Instant         |                                     | `now + slaHours` | Tenggat                                                                     |
| `claimedAt` / `completedAt` | Instant         |                                     | `null`           | Pelacakan waktu                                                             |
| `escalationHistory`         | Array\<Object\> | `[{at,fromUserId,toUserId,reason}]` | `[]`             | Riwayat pengalihan                                                          |

---

## Cakupan 25 tes terhadap collection

| Kelompok tes                                        | Collection kunci yang dipakai                                                             |
| --------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| IST, CFIT 2A/3A, SPM, WPT, Army Alpha, TIU 5/6, FRT | Definitions, sections, items (`answerKey`), assets, norms; IST ME memakai `stimulusPhase` |
| EPPS, Papikostick, MBTI, MSDT, 16PF                 | Items dengan `scoring.scaleMappings`; EPPS memakai `consistencyGroup`                     |
| Big Five, AQ, Enneagram, MMPI, Holland              | Items `LIKERT`/`TRUE_FALSE`; MMPI memakai skala `VALIDITY` dan `RESTRICTED`               |
| DISC, Kuder                                         | Items `MOST_LEAST` dengan `scoring.most`/`scoring.least`                                  |
| RMIB                                                | Items `RANKING`, skala `RANK_SUM`                                                         |
| MSAI                                                | Skala `role`, hierarki 2 level, `scoredInTotal`, `gapAnalysis`, template `GAP`            |
| Kraepelin                                           | `kraepelin_records` dan `config` di definisi                                              |
| Wartegg                                             | `attempt_files`, `assessment_rubrics`, `review_tasks`, `scoringMode = MANUAL`             |

## Collection pendukung di luar katalog tes

Ini sudah dibahas dan hanya direferensikan oleh tabel di atas, tidak dirinci di sini: `users` (sudah ada), `organizations` (sudah ada), `subscription_plans` (sudah ada, perlu ditambah hak akses), `subscriptions`, `usage_ledger`, `workspaces`, `memberships`, `participants`, `consents`, `test_packages`, `reports`, `ai_generation_logs`, `job_queue`, `notifications`, dan `audit_logs`.

## Catatan yang masih menggantung

1. **FRT, TIU 6, dan ADKUDAG**: model di atas sudah generik untuk tes pilihan ganda bergambar atau teks, tetapi format TIU 6 dan FRT (apakah ada subtes dan norma per usia) belum terkonfirmasi. ADKUDAG tidak ada di daftar 25 tes Anda, jadi tidak saya masukkan.
2. **Indeks dan keamanan** tetap seperti sebelumnya: `answerKey` dan `options.scoring` tidak boleh dikirim ke klien, dan konten yang sudah dipakai attempt tidak boleh diubah di tempat (buat versi baru).

Mau saya lanjutkan dengan **diagram relasi antar collection** atau **contoh dokumen nyata** untuk satu tes dari tiap kelompok?
