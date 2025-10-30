# HATEOAS API Flow Example

This document demonstrates a complete order lifecycle using HATEOAS principles, showing how hypermedia links guide the client through available actions.

## Scenario: Creating and Processing an Order

### Step 1: Create a New Order

**Request:**
```http
POST /api/orders HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "customerId": "CUST123",
  "items": [
    {
      "productId": "PROD456",
      "quantity": 2,
      "unitPrice": 149.99
    }
  ]
}
```

**Response (Status: 201 Created):**
```json
{
  "id": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
  "customerId": "CUST123",
  "status": "PENDING",
  "totalAmount": 299.98,
  "items": [
    {
      "productId": "PROD456",
      "quantity": 2,
      "unitPrice": 149.99,
      "lineTotal": 299.98
    }
  ],
  "createdAt": "2025-10-30T10:00:00Z",
  "updatedAt": "2025-10-30T10:00:00Z",
  "canceledAt": null,
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6"
    },
    "orders": {
      "href": "http://localhost:8080/api/orders?page=0&size=20"
    },
    "customer-orders": {
      "href": "http://localhost:8080/api/orders/customer/CUST123?page=0&size=20"
    },
    "process": {
      "href": "http://localhost:8080/api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6/status"
    },
    "cancel": {
      "href": "http://localhost:8080/api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6/cancel"
    }
  }
}
```

**Available Actions (PENDING):**
- ✅ **process** - Move order to processing
- ✅ **cancel** - Cancel the order
- ℹ️ **self** - View order details
- ℹ️ **orders** - View all orders
- ℹ️ **customer-orders** - View this customer's orders

---

### Step 2: Process the Order

Client follows the `process` link to move the order forward.

**Request:**
```http
PATCH /api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6/status HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "status": "PROCESSING"
}
```

**Response (Status: 200 OK):**
```json
{
  "id": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
  "customerId": "CUST123",
  "status": "PROCESSING",
  "totalAmount": 299.98,
  "items": [...],
  "createdAt": "2025-10-30T10:00:00Z",
  "updatedAt": "2025-10-30T10:05:00Z",
  "canceledAt": null,
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6"
    },
    "orders": {
      "href": "http://localhost:8080/api/orders?page=0&size=20"
    },
    "customer-orders": {
      "href": "http://localhost:8080/api/orders/customer/CUST123?page=0&size=20"
    },
    "ship": {
      "href": "http://localhost:8080/api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6/status"
    },
    "cancel": {
      "href": "http://localhost:8080/api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6/cancel"
    }
  }
}
```

**Available Actions (PROCESSING):**
- ✅ **ship** - Ship the order
- ✅ **cancel** - Cancel the order
- ❌ **process** - No longer available (already processing)

---

### Step 3: Ship the Order

Client follows the `ship` link.

**Request:**
```http
PATCH /api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6/status HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "status": "SHIPPED"
}
```

**Response (Status: 200 OK):**
```json
{
  "id": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
  "customerId": "CUST123",
  "status": "SHIPPED",
  "totalAmount": 299.98,
  "items": [...],
  "createdAt": "2025-10-30T10:00:00Z",
  "updatedAt": "2025-10-30T10:15:00Z",
  "canceledAt": null,
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6"
    },
    "orders": {
      "href": "http://localhost:8080/api/orders?page=0&size=20"
    },
    "customer-orders": {
      "href": "http://localhost:8080/api/orders/customer/CUST123?page=0&size=20"
    },
    "deliver": {
      "href": "http://localhost:8080/api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6/status"
    }
  }
}
```

**Available Actions (SHIPPED):**
- ✅ **deliver** - Mark as delivered
- ❌ **cancel** - No longer available (already shipped)
- ❌ **ship** - No longer available (already shipped)

---

### Step 4: Deliver the Order

Client follows the `deliver` link.

**Request:**
```http
PATCH /api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6/status HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "status": "DELIVERED"
}
```

**Response (Status: 200 OK):**
```json
{
  "id": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
  "customerId": "CUST123",
  "status": "DELIVERED",
  "totalAmount": 299.98,
  "items": [...],
  "createdAt": "2025-10-30T10:00:00Z",
  "updatedAt": "2025-10-30T11:00:00Z",
  "canceledAt": null,
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6"
    },
    "orders": {
      "href": "http://localhost:8080/api/orders?page=0&size=20"
    },
    "customer-orders": {
      "href": "http://localhost:8080/api/orders/customer/CUST123?page=0&size=20"
    }
  }
}
```

**Available Actions (DELIVERED - Terminal State):**
- ❌ **No action links** - Order lifecycle is complete
- ℹ️ Only navigation links remain

---

## Alternative Flow: Cancelling an Order

### Cancel from PENDING State

**Request:**
```http
POST /api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6/cancel HTTP/1.1
Host: localhost:8080
```

**Response (Status: 200 OK):**
```json
{
  "id": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
  "customerId": "CUST123",
  "status": "CANCELLED",
  "totalAmount": 299.98,
  "items": [...],
  "createdAt": "2025-10-30T10:00:00Z",
  "updatedAt": "2025-10-30T10:02:00Z",
  "canceledAt": "2025-10-30T10:02:00Z",
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6"
    },
    "orders": {
      "href": "http://localhost:8080/api/orders?page=0&size=20"
    },
    "customer-orders": {
      "href": "http://localhost:8080/api/orders/customer/CUST123?page=0&size=20"
    }
  }
}
```

**Available Actions (CANCELLED - Terminal State):**
- ❌ **No action links** - Order is permanently cancelled

---

## Pagination Example

### List All Orders

**Request:**
```http
GET /api/orders?page=0&size=2 HTTP/1.1
Host: localhost:8080
```

**Response (Status: 200 OK):**
```json
{
  "_embedded": {
    "orderResponseList": [
      {
        "id": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
        "customerId": "CUST123",
        "status": "PENDING",
        "totalAmount": 299.98,
        "_links": {
          "self": { "href": "http://localhost:8080/api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6" },
          "orders": { "href": "http://localhost:8080/api/orders?page=0&size=20" },
          "customer-orders": { "href": "http://localhost:8080/api/orders/customer/CUST123?page=0&size=20" },
          "process": { "href": "http://localhost:8080/api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6/status" },
          "cancel": { "href": "http://localhost:8080/api/orders/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6/cancel" }
        }
      },
      {
        "id": "b2c3d4e5-f6g7-48h9-i0j1-k2l3m4n5o6p7",
        "customerId": "CUST456",
        "status": "SHIPPED",
        "totalAmount": 599.99,
        "_links": {
          "self": { "href": "http://localhost:8080/api/orders/b2c3d4e5-f6g7-48h9-i0j1-k2l3m4n5o6p7" },
          "orders": { "href": "http://localhost:8080/api/orders?page=0&size=20" },
          "customer-orders": { "href": "http://localhost:8080/api/orders/customer/CUST456?page=0&size=20" },
          "deliver": { "href": "http://localhost:8080/api/orders/b2c3d4e5-f6g7-48h9-i0j1-k2l3m4n5o6p7/status" }
        }
      }
    ]
  },
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/orders?page=0&size=2"
    },
    "first": {
      "href": "http://localhost:8080/api/orders?page=0&size=2"
    },
    "next": {
      "href": "http://localhost:8080/api/orders?page=1&size=2"
    },
    "last": {
      "href": "http://localhost:8080/api/orders?page=4&size=2"
    }
  },
  "page": {
    "size": 2,
    "totalElements": 10,
    "totalPages": 5,
    "number": 0
  }
}
```

**Pagination Links:**
- **self** - Current page
- **first** - First page
- **next** - Next page
- **last** - Last page

---

## Key Advantages Demonstrated

### 1. **Discoverability**
Clients don't need external documentation - they discover available actions from the response itself.

### 2. **State Machine Enforcement**
The server controls which transitions are valid:
- Can't ship before processing
- Can't cancel after shipping
- Can't perform actions on completed orders

### 3. **Decoupling**
Clients never hardcode URLs (except the entry point). They follow links using relation names:
```javascript
// Good - Using HATEOAS
const processUrl = order._links.process.href;
fetch(processUrl, { method: 'PATCH', ... });

// Bad - Hardcoded URL
fetch(`/api/orders/${orderId}/status`, { ... });
```

### 4. **API Evolution**
URLs can change without breaking clients:
- `/api/orders/{id}/status` could become `/api/v2/orders/{id}/transitions`
- As long as the `process` relation exists, clients still work

### 5. **Self-Documentation**
The presence or absence of links tells the client:
- What actions are currently possible
- What state the resource is in
- Where to go next

---

## Client Implementation Pattern

```javascript
async function processOrder(orderResponse) {
  // Check if the action is available
  if (orderResponse._links.process) {
    // Follow the link
    const response = await fetch(orderResponse._links.process.href, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: 'PROCESSING' })
    });
    
    const updatedOrder = await response.json();
    
    // Check new available actions
    console.log('Available actions:', Object.keys(updatedOrder._links));
    
    return updatedOrder;
  } else {
    console.log('Cannot process order in current state');
  }
}
```

## Conclusion

HATEOAS transforms the API from a set of disconnected endpoints into a **navigable state machine** where the server guides the client through the available transitions. This makes the API more robust, maintainable, and easier to evolve over time.

