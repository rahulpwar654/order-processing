# Plan - Order Processing System

## 1. Scope and assumptions
- Implement core features from `doc/Assignment.md`.
- Tech: Java 21, Spring Boot, Maven, Spring Web, Spring Data JPA, Bean Validation, H2 (dev).
- Status lifecycle: `PENDING -> PROCESSING -> SHIPPED -> DELIVERED`.
- Cancel allowed only when status is `PENDING`.
- Scheduler auto-updates all `PENDING` orders to `PROCESSING` every 5 minutes.

## 2. Architecture overview
- Layered: Controller -> Service -> Repository.
- DTOs for requests/responses; entities for persistence.
- Validation at DTO level; business rules in service.
- Global exception handling with `@ControllerAdvice`.
- Scheduler with `@EnableScheduling`.

## 3. Data model
- Entity: `Order`
  - `id` (UUID), `customerId` (String), `status` (enum), `totalAmount` (BigDecimal)
  - `createdAt`, `updatedAt`, `canceledAt` (timestamps)
  - `items` (OneToMany OrderItem, cascade all)
- Entity: `OrderItem`
  - `id` (UUID), `order` (ManyToOne), `productId` (String), `quantity` (int > 0), `unitPrice` (BigDecimal >= 0), `lineTotal`
- Enum: `OrderStatus { PENDING, PROCESSING, SHIPPED, DELIVERED }`
- Derived field: `totalAmount = sum(items.lineTotal)`; compute in service.

## 4. API design
- `POST /api/orders`
  - Create order with items.
  - Request: `{ customerId, items:[{ productId, quantity, unitPrice }] }`
  - Response: full order with `id`, `status=PENDING`.
- `GET /api/orders/{id}`
  - Fetch order by id.
- `GET /api/orders?status={STATUS}&page=&size=`
  - List all orders; optional filter by status; paginated.
- `PATCH /api/orders/{id}/status`
  - Update status (allowed transitions: `PROCESSING->SHIPPED`, `SHIPPED->DELIVERED`).
  - Request: `{ status: "SHIPPED" }`
- `POST /api/orders/{id}/cancel`
  - Cancel only if current status is `PENDING`.
- Error responses: JSON with `timestamp`, `path`, `code`, `message`, `details`.

## 5. Business rules
- On create:
  - Validate items non-empty; each quantity > 0, unitPrice >= 0.
  - Set status `PENDING`.
  - Compute `lineTotal = quantity * unitPrice`; compute `totalAmount`.
- On cancel:
  - Only if status is `PENDING`; set `canceledAt` and keep status `PENDING` or mark as `CANCELLED`? (Per assignment, keep `PENDING` but block further transitions and mark `canceledAt`.)
- On manual status update:
  - Disallow updates for `PENDING` and `DELIVERED`.
  - Allowed transitions:
    - `PROCESSING -> SHIPPED`
    - `SHIPPED -> DELIVERED`
- On scheduled update:
  - Every 5 minutes, set all non-canceled `PENDING` orders to `PROCESSING`.

## 6. Scheduler
- Enable scheduling with `@EnableScheduling`.
- `@Scheduled(fixedRateString = "PT5M")` or `@Scheduled(cron = "0 */5 * * * *")`.
- Efficient update:
  - Repository method to bulk update: `update status to PROCESSING where status=PENDING and canceledAt is null`.
  - Return count and log for observability.

## 7. Validation and error handling
- Bean Validation on DTOs: `@NotBlank customerId`, `@NotEmpty items`, `@Positive quantity`, `@DecimalMin("0.0") unitPrice`.
- Map validation errors to 400 with clear messages.
- Domain errors:
  - `ORDER_NOT_FOUND` -> 404
  - `INVALID_STATUS_TRANSITION` -> 409
  - `ORDER_ALREADY_CANCELED` -> 409
  - `CANNOT_CANCEL_NON_PENDING` -> 409
- Global handler with `@ControllerAdvice`.

## 8. Persistence and configuration
- Spring Data JPA repositories for `Order` and `OrderItem`.
- H2 in-memory for dev; schema auto via JPA.
- `application.yml`:
  - H2 console enabled for dev.
  - `spring.jpa.hibernate.ddl-auto=update` (dev only).
  - Logging SQL (dev).
- Use UUID generation for ids.

## 9. Project structure
- `src/main/java/com/example/orders/`
  - `controller/OrderController.java`
  - `service/OrderService.java`
  - `service/impl/OrderServiceImpl.java`
  - `repository/OrderRepository.java`, `OrderItemRepository.java`
  - `domain/Order.java`, `OrderItem.java`, `OrderStatus.java`
  - `dto/OrderCreateRequest.java`, `OrderItemRequest.java`, `OrderResponse.java`, `OrderStatusUpdateRequest.java`, `PageResponse.java`
  - `mapper/OrderMapper.java` (manual or MapStruct)
  - `scheduler/OrderStatusScheduler.java`
  - `exception/*` (custom exceptions, error model, advice)
  - `config/SchedulingConfig.java`
- `src/test/java/...`
  - `service/OrderServiceTest.java`
  - `controller/OrderControllerIT.java`
  - `scheduler/OrderStatusSchedulerIT.java`

## 10. Testing strategy
- Unit tests:
  - `OrderService` create, cancel, invalid transitions, total calculations.
- Integration tests (MockMvc + H2):
  - Create, get, list with filter and pagination.
  - Status update success/failure flows.
  - Cancel endpoint behavior.
- Scheduler test:
  - Seed `PENDING` orders, run scheduler, assert updated to `PROCESSING`.
- Edge cases:
  - Empty items, negative quantity/price.
  - Idempotency for cancel (second cancel should 409).
  - Large item counts.

## 11. Build and run
- Maven dependencies:
  - `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `h2`, `spring-boot-starter-test`.
- Run locally:
  - `mvn spring-boot:run`
  - H2 console at `/h2-console` (dev).
- Java version set in Maven `maven-compiler-plugin`.

## 12. API examples
- Create order request:
  - `{ "customerId": "cust-123", "items": [ { "productId": "sku-1", "quantity": 2, "unitPrice": 10.50 }, { "productId": "sku-2", "quantity": 1, "unitPrice": 5.00 } ] }`
- Create order response:
  - `{ "id":"<uuid>", "status":"PENDING", "totalAmount":26.00, "items":[...], "createdAt":"...", "updatedAt":"..." }`
- Update status to shipped:
  - `PATCH /api/orders/{id}/status` with `{ "status":"SHIPPED" }`
- Cancel:
  - `POST /api/orders/{id}/cancel`

## 13. Delivery checklist
- Endpoints implemented and documented.
- Validation and error handling complete.
- Scheduler executes every 5 minutes and logs updates.
- Tests passing with coverage on core flows.
- README updated with run instructions and API summary.

## 14. Future enhancements (optional)
- Separate `CANCELLED` status.
- Outbox/event publishing on status changes.
- Idempotency keys for create/cancel.
- Authentication/authorization.
- OpenAPI via `springdoc-openapi`.