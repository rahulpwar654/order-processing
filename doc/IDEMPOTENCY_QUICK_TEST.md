# Quick Test Guide - Idempotency Feature

## What Was Implemented

✅ **Idempotency for Order Creation** - Prevents duplicate orders from the same request

## Three Ways to Use

### 1. Via HTTP Header (Recommended)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: my-unique-key-123" \
  -d '{
    "customerId": "CUST123",
    "items": [
      {
        "productId": "PROD456",
        "quantity": 2,
        "unitPrice": 149.99
      }
    ]
  }'
```

### 2. Via Request Body
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST123",
    "idempotencyKey": "order-2025-10-30-001",
    "items": [
      {
        "productId": "PROD456",
        "quantity": 2,
        "unitPrice": 149.99
      }
    ]
  }'
```

### 3. Auto-Generated (No Key Provided)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST123",
    "items": [
      {
        "productId": "PROD456",
        "quantity": 2,
        "unitPrice": 149.99
      }
    ]
  }'
```
**System auto-generates SHA-256 hash from request content**

## Quick Test

### Step 1: Create First Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-duplicate-001" \
  -d '{
    "customerId": "CUST999",
    "items": [
      {
        "productId": "PROD123",
        "quantity": 1,
        "unitPrice": 99.99
      }
    ]
  }'
```

**Note the Order ID** (e.g., `550e8400-e29b-41d4-a716-446655440000`)

### Step 2: Try to Create Same Order Again
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-duplicate-001" \
  -d '{
    "customerId": "CUST999",
    "items": [
      {
        "productId": "PROD123",
        "quantity": 1,
        "unitPrice": 99.99
      }
    ]
  }'
```

**Result:** ✅ Returns the SAME Order ID (no duplicate created!)

### Step 3: Verify in Logs
Check application logs for:
```
"Order already exists with idempotency key: test-duplicate-001, returning existing order: 550e8400-e29b-41d4-a716-446655440000"
```

## Test Auto-Generation

### Send Identical Requests (No Key)
```bash
# Request 1
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST888",
    "items": [{"productId": "PROD777", "quantity": 5, "unitPrice": 50.00}]
  }'

# Request 2 (IDENTICAL content)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST888",
    "items": [{"productId": "PROD777", "quantity": 5, "unitPrice": 50.00}]
  }'
```

**Result:** ✅ Returns the SAME Order ID (auto-generated key matches!)

## Verify in Swagger

1. Open http://localhost:8080/swagger-ui.html
2. Find `POST /api/orders`
3. Notice new `Idempotency-Key` header parameter
4. Try creating orders with same key

## Database Verification

```sql
-- Check idempotency keys in database
SELECT id, customer_id, idempotency_key, status, total_amount, created_at 
FROM orders 
ORDER BY created_at DESC 
LIMIT 10;
```

## What Changed

### Files Modified
1. `OrderCreateRequest.java` - Added `idempotencyKey` field
2. `Order.java` - Added `idempotencyKey` column with unique constraint
3. `OrderRepository.java` - Added `findByIdempotencyKey()` method
4. `OrderServiceImpl.java` - Added duplicate detection logic
5. `OrderController.java` - Added `Idempotency-Key` header support

### Files Created
1. `IdempotencyKeyGenerator.java` - SHA-256 hash generator
2. `IDEMPOTENCY_IMPLEMENTATION.md` - Full documentation

## Expected Behavior

| Scenario | Result |
|----------|--------|
| First request with key | Creates new order |
| Duplicate request with same key | Returns existing order (same ID) |
| Identical content, no key | Returns existing order (auto-generated key matches) |
| Different key | Creates new order |
| Different content, no key | Creates new order (different hash) |

## Use Cases

✅ **Network Retry** - Client retries on timeout  
✅ **User Double-Click** - User clicks submit multiple times  
✅ **Mobile App** - Offline queue sends same request multiple times  
✅ **API Client** - Automatic retry logic  

## Monitoring

Check logs for:
- `"Generated idempotency key: {key}"`
- `"Using provided idempotency key: {key}"`
- `"Order already exists with idempotency key: {key}"`

## Success Criteria

- ✅ Same idempotency key = Same order ID
- ✅ HTTP 201 returned for both new and existing orders
- ✅ No database constraint violations
- ✅ Works with header, body field, and auto-generation
- ✅ Documented in Swagger UI

## Full Documentation

See `doc/IDEMPOTENCY_IMPLEMENTATION.md` for:
- Complete implementation details
- Best practices for clients
- Code examples
- Advanced scenarios
- Monitoring and metrics

