# Swagger / OpenAPI Documentation

This project integrates Springdoc OpenAPI to auto-generate API docs and serve Swagger UI.

## What was added
- Dependency: `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0`
- Config: `OpenApiConfig` providing API metadata.
- OpenAPI annotations on `OrderController` for operations, parameters, and responses.

## How to use

- Start the app, then open the Swagger UI in a browser:
  - Swagger UI: http://localhost:8080/swagger-ui.html
  - OpenAPI JSON: http://localhost:8080/v3/api-docs
  - OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml

If your server port differs, adjust the host/port accordingly.

## Notes
- Endpoints are grouped under tag: "Orders"
- Models are derived from DTOs: `OrderCreateRequest`, `OrderStatusUpdateRequest`, `OrderResponse`
- Works alongside HATEOAS responses; schemas document payloads while hypermedia links may also appear in responses.

