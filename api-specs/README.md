# QAWave API Specifications

This directory contains OpenAPI specifications for the QAWave platform.

## Files

| File | Description |
|------|-------------|
| `qawave-api.yaml` | Core QAWave API specification (OpenAPI 3.0) |

## Usage

### View Documentation

The backend automatically serves Swagger UI at runtime:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api-docs`
- **OpenAPI YAML**: `http://localhost:8080/api-docs.yaml`

### Validate Specification

```bash
# Install spectral (OpenAPI linter)
npm install -g @stoplight/spectral-cli

# Lint the specification
spectral lint api-specs/qawave-api.yaml
```

### Generate TypeScript Types (Frontend)

```bash
# Install openapi-typescript
npm install -D openapi-typescript

# Generate types
npx openapi-typescript api-specs/qawave-api.yaml -o frontend/src/types/api.d.ts
```

### Generate API Client

```bash
# Using openapi-generator
npx @openapitools/openapi-generator-cli generate \
  -i api-specs/qawave-api.yaml \
  -g typescript-fetch \
  -o frontend/src/api/generated
```

## API Overview

### Authentication

All endpoints require JWT authentication via Keycloak:

```bash
# Get token (example with password grant - use PKCE in production)
curl -X POST "https://auth.qawave.local/realms/qawave/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=qawave-backend" \
  -d "client_secret=YOUR_SECRET"

# Use token
curl -H "Authorization: Bearer $TOKEN" https://api.qawave.local/api/qa/packages
```

### Endpoints Summary

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/qa/packages` | List all QA packages |
| POST | `/api/qa/packages` | Create a new QA package |
| GET | `/api/qa/packages/{id}` | Get package by ID |
| PUT | `/api/qa/packages/{id}` | Update a package |
| DELETE | `/api/qa/packages/{id}` | Delete a package |
| PATCH | `/api/qa/packages/{id}/status` | Update package status |
| GET | `/api/qa/packages/count` | Get package count |
| GET | `/api/health` | Health check |

### Example: Create Package

```bash
curl -X POST "http://localhost:8080/api/qa/packages" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Pet Store API Tests",
    "specUrl": "https://petstore3.swagger.io/api/v3/openapi.json",
    "baseUrl": "https://petstore3.swagger.io/api/v3",
    "requirements": "Test all CRUD operations for pets"
  }'
```

## Versioning

The API follows semantic versioning. Breaking changes will increment the major version.

Current version: **1.0.0**

### Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-01-30 | Initial release |

## Contributing

When adding new endpoints:

1. Update `qawave-api.yaml` with the new endpoint
2. Ensure all request/response schemas are defined
3. Add examples for complex requests
4. Run linter: `spectral lint api-specs/qawave-api.yaml`
5. Regenerate TypeScript types if needed

## References

- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.3)
- [ADR-006: Test Scenario JSON Contract](../docs/architecture/decisions/ADR-006-test-scenario-json-contract.md)
