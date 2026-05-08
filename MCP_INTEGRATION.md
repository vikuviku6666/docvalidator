# MCP (Model Context Protocol) Integration Guide

## Overview

DocValidator implements MCP (Model Context Protocol) to enable AI agents to access both OpenAPI documentation context and live API runtime behavior. This creates a powerful feedback loop where AI agents can:

1. **Understand Documentation** - Access OpenAPI specifications, endpoint details, schemas, and authentication requirements
2. **Observe Runtime Behavior** - Execute API calls, analyze responses, validate tokens, and monitor performance
3. **Generate Intelligent Tests** - Use both contexts to create comprehensive test cases
4. **Validate Semantically** - Compare documented behavior against actual runtime behavior

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     AI Agents Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Test         │  │ Validator    │  │ Reporter     │      │
│  │ Generator    │  │ Agent        │  │ Agent        │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         └──────────────────┴──────────────────┘              │
│                            │                                 │
└────────────────────────────┼─────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │  MCP Controller │
                    │  (REST API)     │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
    ┌─────────▼──────────┐      ┌─────────▼──────────┐
    │ Documentation MCP  │      │  Live API MCP      │
    │ Server             │      │  Server            │
    │                    │      │                    │
    │ • OpenAPI Spec     │      │ • API Execution    │
    │ • Endpoint Details │      │ • Response Schema  │
    │ • Schema Defs      │      │ • Availability     │
    │ • Auth Reqs        │      │ • Token Validation │
    │ • Endpoint List    │      │ • API Metrics      │
    └────────────────────┘      └────────────────────┘
```

## MCP Servers

### 1. Documentation MCP Server

**Purpose**: Provides access to OpenAPI specification and documentation context.

**Location**: `src/main/java/com/docvalidator/mcp/DocumentationMcpServer.java`

**Tools**:

#### `get_openapi_spec`
Retrieves the complete OpenAPI specification.

**Parameters**: None

**Returns**:
```json
{
  "success": true,
  "spec": {
    "openapi": "3.0.0",
    "info": {...},
    "servers": [...],
    "paths": {...}
  }
}
```

#### `get_endpoint_details`
Gets detailed information about a specific endpoint.

**Parameters**:
- `path` (required): API endpoint path (e.g., "/v1/albums/{id}")
- `method` (required): HTTP method (GET, POST, PUT, DELETE, PATCH)

**Returns**:
```json
{
  "success": true,
  "endpoint": {
    "path": "/v1/albums/{id}",
    "method": "GET",
    "summary": "Get Album",
    "description": "Get Spotify catalog information for a single album",
    "operationId": "get-an-album",
    "tags": ["Albums"],
    "parameters": [...],
    "requestBody": {...},
    "responses": {...},
    "requiredScopes": ["user-read-private"],
    "deprecated": false
  }
}
```

#### `get_schema_definition`
Retrieves a specific schema definition from the OpenAPI spec.

**Parameters**:
- `schemaName` (required): Name of the schema (e.g., "AlbumObject")

**Returns**:
```json
{
  "success": true,
  "schema": {
    "type": "object",
    "properties": {...},
    "required": [...]
  }
}
```

#### `list_endpoints`
Lists all available endpoints, optionally filtered by tag.

**Parameters**:
- `tag` (optional): Filter by OpenAPI tag (e.g., "Albums", "Artists")

**Returns**:
```json
{
  "success": true,
  "endpoints": [
    {
      "path": "/v1/albums/{id}",
      "method": "GET",
      "summary": "Get Album",
      "tags": ["Albums"]
    }
  ],
  "count": 1
}
```

#### `get_auth_requirements`
Gets authentication requirements for an endpoint.

**Parameters**:
- `path` (required): API endpoint path
- `method` (required): HTTP method

**Returns**:
```json
{
  "success": true,
  "authentication": {
    "required": true,
    "type": "oauth2",
    "scopes": ["user-read-private"],
    "flows": {
      "authorizationCode": {...}
    }
  }
}
```

### 2. Live API MCP Server

**Purpose**: Provides access to live API runtime behavior and execution context.

**Location**: `src/main/java/com/docvalidator/mcp/LiveApiMcpServer.java`

**Tools**:

#### `execute_api_call`
Executes a live API call and returns the response.

**Parameters**:
- `path` (required): API endpoint path
- `method` (required): HTTP method
- `headers` (optional): Request headers as JSON object
- `body` (optional): Request body as JSON string
- `queryParams` (optional): Query parameters as JSON object

**Returns**:
```json
{
  "success": true,
  "response": {
    "statusCode": 200,
    "headers": {...},
    "body": {...},
    "responseTime": 245
  }
}
```

#### `get_response_schema`
Analyzes the actual response schema from a live API call.

**Parameters**:
- `path` (required): API endpoint path
- `method` (required): HTTP method

**Returns**:
```json
{
  "success": true,
  "schema": {
    "type": "object",
    "properties": {
      "id": {"type": "string"},
      "name": {"type": "string"}
    },
    "actualFields": ["id", "name", "extra_field"]
  }
}
```

#### `check_endpoint_availability`
Checks if an endpoint is available and responding.

**Parameters**:
- `path` (required): API endpoint path
- `method` (required): HTTP method

**Returns**:
```json
{
  "success": true,
  "available": true,
  "statusCode": 200,
  "responseTime": 123
}
```

#### `validate_oauth_token`
Validates an OAuth token against the API.

**Parameters**:
- `token` (required): OAuth access token

**Returns**:
```json
{
  "success": true,
  "valid": true,
  "expiresIn": 3600,
  "scopes": ["user-read-private", "user-read-email"]
}
```

#### `get_api_metrics`
Retrieves performance metrics for API calls.

**Parameters**:
- `path` (optional): Filter by specific endpoint path

**Returns**:
```json
{
  "success": true,
  "metrics": {
    "totalCalls": 150,
    "successRate": 98.5,
    "averageResponseTime": 234,
    "errorRate": 1.5,
    "endpoints": {
      "/v1/albums/{id}": {
        "calls": 50,
        "avgResponseTime": 200,
        "successRate": 100.0
      }
    }
  }
}
```

## REST API Endpoints

The MCP servers are exposed via REST API through the `McpController`.

**Base URL**: `http://localhost:8080/api/mcp`

**Authentication**: Basic Auth (admin/admin123 or viewer/viewer123)

### Documentation Endpoints

```bash
# Get OpenAPI specification
GET /api/mcp/documentation/spec

# Get endpoint details
GET /api/mcp/documentation/endpoint?path=/v1/albums/{id}&method=GET

# Get schema definition
GET /api/mcp/documentation/schema?name=AlbumObject

# List all endpoints
GET /api/mcp/documentation/endpoints

# List endpoints by tag
GET /api/mcp/documentation/endpoints?tag=Albums

# Get authentication requirements
GET /api/mcp/documentation/auth?path=/v1/albums/{id}&method=GET
```

### Live API Endpoints

```bash
# Execute API call
POST /api/mcp/live/execute
Content-Type: application/json
{
  "path": "/v1/albums/{id}",
  "method": "GET",
  "headers": {"Authorization": "Bearer token"},
  "queryParams": {"market": "US"}
}

# Get response schema
GET /api/mcp/live/schema?path=/v1/albums/{id}&method=GET

# Check endpoint availability
GET /api/mcp/live/availability?path=/v1/albums/{id}&method=GET

# Validate OAuth token
POST /api/mcp/live/validate-token
Content-Type: application/json
{
  "token": "your-access-token"
}

# Get API metrics
GET /api/mcp/live/metrics

# Get metrics for specific endpoint
GET /api/mcp/live/metrics?path=/v1/albums/{id}
```

### Info Endpoint

```bash
# Get MCP server information
GET /api/mcp/info
```

## Usage Examples

### Example 1: AI Agent Generating Tests

```java
// 1. Get endpoint details from Documentation MCP
Map<String, Object> endpointDetails = documentationMcp.getEndpointDetails(
    "/v1/albums/{id}", "GET"
);

// 2. Check if endpoint is available from Live API MCP
Map<String, Object> availability = liveApiMcp.checkEndpointAvailability(
    "/v1/albums/{id}", "GET"
);

// 3. Execute test call
Map<String, Object> response = liveApiMcp.executeApiCall(
    "/v1/albums/{id}",
    "GET",
    Map.of("Authorization", "Bearer " + token),
    null,
    Map.of("id", "4aawyAB9vmqN3uQ7FjRGTy")
);

// 4. Compare documented vs actual schema
Map<String, Object> actualSchema = liveApiMcp.getResponseSchema(
    "/v1/albums/{id}", "GET"
);
```

### Example 2: Using REST API with curl

```bash
# Get endpoint details
curl -u admin:admin123 \
  "http://localhost:8080/api/mcp/documentation/endpoint?path=/v1/albums/{id}&method=GET"

# Execute API call
curl -u admin:admin123 \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/v1/albums/4aawyAB9vmqN3uQ7FjRGTy",
    "method": "GET",
    "headers": {"Authorization": "Bearer YOUR_TOKEN"}
  }' \
  "http://localhost:8080/api/mcp/live/execute"

# Get API metrics
curl -u admin:admin123 \
  "http://localhost:8080/api/mcp/live/metrics"
```

### Example 3: Python Integration

```python
import requests
from requests.auth import HTTPBasicAuth

BASE_URL = "http://localhost:8080/api/mcp"
AUTH = HTTPBasicAuth("admin", "admin123")

# Get OpenAPI spec
response = requests.get(f"{BASE_URL}/documentation/spec", auth=AUTH)
spec = response.json()

# List all endpoints
response = requests.get(f"{BASE_URL}/documentation/endpoints", auth=AUTH)
endpoints = response.json()

# Execute API call
response = requests.post(
    f"{BASE_URL}/live/execute",
    auth=AUTH,
    json={
        "path": "/v1/albums/4aawyAB9vmqN3uQ7FjRGTy",
        "method": "GET",
        "headers": {"Authorization": f"Bearer {token}"}
    }
)
result = response.json()
```

## Configuration

MCP servers are configured in `application.yml`:

```yaml
docvalidator:
  mcp:
    enabled: true
    servers:
      - name: "documentation"
        type: "openapi"
        url: "http://localhost:8080/api/mcp/documentation"
      - name: "live-api"
        type: "runtime"
        url: "http://localhost:8080/api/mcp/live"
```

## Security

- All MCP endpoints require authentication (Basic Auth)
- Two user roles available:
  - **admin** (admin123): Full access to all MCP tools
  - **viewer** (viewer123): Read-only access
- OAuth tokens are validated before API execution
- Sensitive data (tokens, credentials) are not logged

## Benefits

1. **Context-Aware Testing**: AI agents have access to both documentation and runtime behavior
2. **Intelligent Test Generation**: Tests are generated based on actual API behavior, not just documentation
3. **Semantic Validation**: Compare what the API should do (docs) vs what it actually does (runtime)
4. **Performance Monitoring**: Track API performance and reliability over time
5. **Automated Discovery**: Detect undocumented endpoints, fields, or behaviors
6. **Continuous Validation**: Monitor API changes and breaking changes automatically

## Troubleshooting

### MCP Server Not Responding

1. Check if the application is running: `curl http://localhost:8080/actuator/health`
2. Verify authentication: `curl -u admin:admin123 http://localhost:8080/api/mcp/info`
3. Check logs: `tail -f logs/docvalidator.log`

### Authentication Errors

- Ensure you're using correct credentials (admin/admin123 or viewer/viewer123)
- Check if Basic Auth header is properly formatted
- Verify Spring Security configuration in `SecurityConfig.java`

### API Execution Failures

- Verify OAuth token is valid: Use `/api/mcp/live/validate-token`
- Check endpoint availability: Use `/api/mcp/live/availability`
- Review API metrics: Use `/api/mcp/live/metrics`
- Check Spotify API credentials in `application.yml`

## Next Steps

1. **Integrate with AI Agents**: Update TestGeneratorAgent, ValidatorAgent, and ReporterAgent to use MCP context
2. **Add More Tools**: Extend MCP servers with additional tools as needed
3. **Implement Caching**: Cache OpenAPI specs and frequently accessed data
4. **Add Monitoring**: Implement detailed logging and monitoring for MCP operations
5. **Create Dashboard**: Build a web UI to visualize MCP data and metrics

## References

- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Spotify Web API Documentation](https://developer.spotify.com/documentation/web-api)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)