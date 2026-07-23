# Referral Code System - Implementation Guide

## Overview

Implementasi referral code system dengan fitur:

- ✅ Unique constraint di database (MongoDB unique index)
- ✅ Referral track ke userId, bukan ke code
- ✅ Archive kode lama untuk audit & backward compatibility
- ✅ Rate limiting untuk prevent abuse (MongoDB-based)
- ✅ Logging/audit trail untuk monitoring (Cloud Logging JSON)

## Files Created

### Core Services

| File                                                                                                                                | Purpose                                              |
| ----------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| [`RateLimitEntry.java`](../src/main/java/com/psycorp/psychapi/domain/model/RateLimitEntry.java)                                     | MongoDB entity untuk rate limiting                   |
| [`RateLimitExceededException.java`](../src/main/java/com/psycorp/psychapi/infrastructure/ratelimit/RateLimitExceededException.java) | Exception untuk rate limit exceeded                  |
| [`RateLimitService.java`](../src/main/java/com/psycorp/psychapi/infrastructure/ratelimit/RateLimitService.java)                     | Service untuk rate limiting (MongoDB-based)          |
| [`ReferralService.java`](../src/main/java/com/psycorp/psychapi/domain/service/ReferralService.java)                                 | Core referral logic (generate, validate, regenerate) |

### API Layer (Optional)

| File                                                                                                | Purpose                                  |
| --------------------------------------------------------------------------------------------------- | ---------------------------------------- |
| [`ReferralRequests.java`](../src/main/java/com/psycorp/psychapi/api/dto/ReferralRequests.java)      | Request DTOs untuk referral endpoints    |
| [`ReferralResource.java`](../src/main/java/com/psycorp/psychapi/api/resource/ReferralResource.java) | REST endpoints untuk referral management |

### Modified Files

| File                                                                                        | Changes                                       |
| ------------------------------------------------------------------------------------------- | --------------------------------------------- |
| [`UserService.java`](../src/main/java/com/psycorp/psychapi/domain/service/UserService.java) | Inject `ReferralService`, delegate validation |
| [`application.yml`](../src/main/resources/application.yml)                                  | Enable JSON logging, add category log levels  |

### Scripts

| File                                             | Purpose                                 |
| ------------------------------------------------ | --------------------------------------- |
| [`mongodb-migration.js`](./mongodb-migration.js) | Migration script untuk database indexes |

---

## Setup Instructions

### 1. Run Database Migration

Jalankan migration script untuk membuat indexes yang diperlukan:

```bash
# Menggunakan mongosh
mongosh mongodb://localhost:27017/psych scripts/mongodb-migration.js

# Atau via MongoDB Compass Shell
# Paste isi script ke Compass Shell
```

**Migration akan:**

1. Create unique index `idx_referral_code_unique` pada `users.referralCode`
2. Create collection `ratelimits` dengan TTL index pada `expiresAt`
3. Verify indexes dan collections

### 2. Configure Logging (Optional)

Update environment variables untuk custom logging:

```bash
# Enable JSON logging (default: true untuk Cloud Run)
export LOG_CONSOLE_JSON=true

# Custom log levels
export LOG_LEVEL=INFO
export LOG_LEVEL_REFERRAL=INFO
export LOG_LEVEL_RATELIMIT=WARN
```

### 3. Deploy to Cloud Run

Pastikan environment variables sudah di-set:

```bash
gcloud run deploy psych-api \
  --set-env-vars LOG_CONSOLE_JSON=true \
  --set-env-vars MONGODB_URI=mongodb+srv://... \
  --region us-central1
```

---

## API Endpoints

### Built-in (via UserService)

Registration endpoint sudah otomatis menggunakan referral system:

```bash
POST /api/v1/auth/register
{
  "email": "user@example.com",
  "password": "Password123!",
  "fullName": "John Doe",
  "accountType": "INDIVIDUAL",
  "referralCode": "JOHN2024"  // Optional
}
```

### New Referral Endpoints (Optional)

Jika `ReferralResource` di-deploy:

```bash
# Regenerate referral code (requires authentication)
POST /api/v1/referral/regenerate
Authorization: Bearer <token>
{
  "reason": "user_request"  // user_request | security | regenerated
}

# Validate referral code (requires authentication)
POST /api/v1/referral/validate
Authorization: Bearer <token>
{
  "referralCode": "JOHN2024"
}

# Get referral history (requires authentication)
GET /api/v1/referral/history
Authorization: Bearer <token>

# Get referral statistics (requires authentication)
GET /api/v1/referral/stats
Authorization: Bearer <token>
```

---

## Rate Limiting

### Configuration

| Type              | Limit       | Window   |
| ----------------- | ----------- | -------- |
| Code Validation   | 20 requests | 1 minute |
| Code Generation   | 5 requests  | 1 hour   |
| Code Regeneration | 3 requests  | 1 day    |

### How It Works

1. Request masuk → `RateLimitService` check counter di MongoDB
2. Jika counter < limit → increment dan allow request
3. Jika counter > limit → throw `RateLimitExceededException` (HTTP 429)
4. Counter auto-expire sesuai TTL window

### MongoDB Rate Limit Document

```javascript
{
  "_id": "ratelimit:validation:192.168.1.0",
  "count": 5,
  "createdAt": ISODate("2026-07-22T15:00:00Z"),
  "updatedAt": ISODate("2026-07-22T15:05:00Z"),
  "expiresAt": ISODate("2026-07-22T15:01:00Z")  // Auto-delete by TTL
}
```

---

## Backward Compatibility

### Referral Code Lookup Flow

```
1. Check current referralCode di users collection
   ↓ (not found)
2. Check referralCodeHistory array (archived codes)
   ↓ (not found)
3. Throw INVALID_REFERRAL_CODE exception
```

### Archive Format

```javascript
{
  "_id": ObjectId("507f1f77bcf86cd799439011"),
  "email": "user@example.com",
  "referralCode": "JOHN2024",  // Current active code
  "referralCodeHistory": [     // Archived codes
    {
      "code": "OLD2023",
      "archivedAt": ISODate("2026-07-20T08:00:00Z"),
      "reason": "user_request",
      "replacedBy": "JOHN2024"
    }
  ]
}
```

---

## Logging & Monitoring

### Log Format (JSON)

```json
{
  "severity": "INFO",
  "timestamp": "2026-07-22T15:00:00.000Z",
  "message": "Referral code validated successfully: code=JOH***, referrerId=507f..., source=current",
  "logger": "com.psycorp.psychapi.domain.service.ReferralService",
  "thread": "executor-thread-1",
  "event": "REFERRAL_CODE_VALIDATED",
  "referralCode": "JOH***",
  "referrerId": "507f1f77bcf86cd799439011",
  "source": "current"
}
```

### Cloud Logging Queries

```sql
-- Failed validation attempts
resource.type="cloud_run_revision"
jsonPayload.message:"Invalid referral code"

-- Rate limit exceeded
resource.type="cloud_run_revision"
jsonPayload.message:"Rate limit exceeded"

-- Successful referrals today
resource.type="cloud_run_revision"
jsonPayload.message:"Referral code validated successfully"
timestamp > "2026-07-22T00:00:00Z"
```

---

## Testing

### Unit Tests (Recommended)

```java
@QuarkusTest
public class ReferralServiceTest {

    @Inject
    ReferralService referralService;

    @Test
    public void testGenerateReferralCode() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setCreatedAt(Instant.now());

        String code = referralService.generateReferralCode(user);

        assertNotNull(code);
        assertEquals(8, code.length());
    }

    @Test
    public void testValidateReferralCode() {
        // Create referrer first
        User referrer = new User();
        referrer.setEmail("referrer@example.com");
        referrer.setReferralCode("TEST1234");
        referrer.persist();

        // Validate code
        User result = referralService.validateReferralCode("TEST1234", "192.168.1.1");

        assertEquals(referrer.getId(), result.getId());
    }
}
```

### Integration Test

```bash
# Test registration with referral code
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "Password123!",
    "fullName": "New User",
    "accountType": "INDIVIDUAL",
    "referralCode": "JOHN2024"
  }'
```

---

## Troubleshooting

### Issue: Duplicate referralCode error

**Cause**: Ada duplicate referralCode di database sebelum migration.

**Solution**:

```javascript
// Find duplicates
db.users.aggregate([
  { $match: { referralCode: { $exists: true, $ne: null } } },
  {
    $group: {
      _id: "$referralCode",
      count: { $sum: 1 },
      ids: { $push: "$_id" },
    },
  },
  { $match: { count: { $gt: 1 } } },
]);

// Fix duplicates (add random suffix)
db.users.find({ referralCode: "DUP123" }).forEach((user) => {
  const newCode = user.referralCode + Math.floor(Math.random() * 1000);
  db.users.updateOne({ _id: user._id }, { $set: { referralCode: newCode } });
});
```

### Issue: Rate limit too strict

**Solution**: Adjust limits di [`RateLimitService.java`](../src/main/java/com/psycorp/psychapi/infrastructure/ratelimit/RateLimitService.java):

```java
private static final int VALIDATION_LIMIT = 50;  // Increase from 20
private static final int VALIDATION_WINDOW = 120; // Increase from 60s
```

### Issue: Logs tidak muncul di Cloud Logging

**Check**:

1. Ensure `LOG_CONSOLE_JSON=true`
2. Check service account has `logging.logWriter` permission
3. Verify stdout/stderr tidak di-redirect

---

## Cost Estimate

| Service              | Tier                    | Cost/Month   |
| -------------------- | ----------------------- | ------------ |
| Google Cloud Run     | Free tier (2M requests) | $0           |
| Google Cloud Logging | 50GB/month free         | $0           |
| MongoDB Atlas        | M0 Sandbox (512MB)      | $0           |
| **Total**            |                         | **$0/month** |

---

## Next Steps

1. **Test locally** - Run migration script dan test registration flow
2. **Deploy to staging** - Deploy ke Cloud Run staging environment
3. **Monitor logs** - Check Cloud Logging untuk referral events
4. **Iterate** - Adjust rate limits atau logging berdasarkan traffic patterns

---

## Security Considerations

1. **Code masking** - Referral codes di-mask di logs (first 3 chars only)
2. **IP masking** - Last octet di-mask untuk privacy
3. **Self-referral prevention** - User tidak bisa gunakan code sendiri
4. **Rate limiting** - Prevent brute-force attacks
5. **Fail-open** - Jika MongoDB down, rate limiting fail-open (allow requests) untuk mencegah DoS

---

## Support

Untuk pertanyaan atau issues, hubungi support@psychapi.com atau buka issue di repository.
