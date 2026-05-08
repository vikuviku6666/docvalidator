# DocValidator API Endpoints Guide

## 🌐 Base URL
```
http://localhost:8080
```

## 📋 Public Endpoints (No Authentication Required)

### 1. Root / Home
**GET** `/`

Returns application information and available endpoints.

**Example:**
```bash
curl http://localhost:8080/
```

**Response:**
```json
{
  "application": "DocValidator",
  "version": "1.0.0-SNAPSHOT",
  "description": "AI-Powered API Documentation Testing Framework",
  "status": "running",
  "endpoints": {
    "health": "/api/health",
    "validation": "/api/v1/validation/*",
    "start_validation": "POST /api/v1/validation/start",
    "run_validation": "POST /api/v1/validation/run",
    "get_progress": "GET /api/v1/validation/progress"
  }
}
```

### 2. Health Check
**GET** `/api/health`

Check if the application is running.

**Example:**
```bash
curl http://localhost:8080/api/health
```

**Response:**
```json
{
  "status": "UP",
  "application": "DocValidator",
  "timestamp": 1234567890
}
```

### 3. API Information
**GET** `/api/info`

Get detailed API information and features.

**Example:**
```bash
curl http://localhost:8080/api/info
```

**Response:**
```json
{
  "name": "DocValidator API",
  "version": "1.0.0-SNAPSHOT",
  "description": "AI-Powered API Documentation Testing Framework",
  "features": [
    "OpenAPI Specification Parsing",
    "AI-Powered Test Generation",
    "Semantic Validation",
    "OAuth 2.0 Authentication",
    "Real-time Progress Tracking",
    "Comprehensive Reporting"
  ],
  "endpoints": {
    "POST /api/v1/validation/start": "Start validation (async)",
    "POST /api/v1/validation/run": "Run validation (sync)",
    "GET /api/v1/validation/progress": "Get validation progress",
    "GET /api/v1/validation/report/json": "Export report as JSON",
    "GET /api/v1/validation/report/markdown": "Export report as Markdown"
  }
}
```

## 🔐 Protected Endpoints (Authentication Required)

### Login Credentials
- **Admin:** username: `admin`, password: `admin123`
- **Viewer:** username: `viewer`, password: `viewer123`

### 4. Start Validation (Async)
**POST** `/api/v1/validation/start`

Start validation in the background. Returns immediately.

**Authentication:** Required

**Request Body:**
```json
{
  "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
  "endpointPaths": [
    "/v1/albums/{id}",
    "/v1/artists/{id}"
  ]
}
```

**Example with curl:**
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
    "endpointPaths": ["/v1/albums/{id}"]
  }'
```

**Response:**
```json
{
  "status": "STARTED",
  "message": "Validation started successfully"
}
```

### 5. Run Validation (Sync)
**POST** `/api/v1/validation/run`

Run validation synchronously. Waits for completion and returns the full report.

**Authentication:** Required

**Request Body:**
```json
{
  "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
  "endpointPaths": [
    "/v1/albums/{id}"
  ]
}
```

**Example with curl:**
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/validation/run \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
    "endpointPaths": ["/v1/albums/{id}"]
  }'
```

**Response:**
```json
{
  "id": "uuid-here",
  "generatedAt": "2024-01-01T12:00:00",
  "summary": {
    "totalTests": 10,
    "passedTests": 8,
    "failedTests": 2,
    "totalDiscrepancies": 5,
    "criticalIssues": 1,
    "highIssues": 2,
    "mediumIssues": 2,
    "lowIssues": 0,
    "infoIssues": 0
  },
  "validationResults": [...],
  "discrepanciesByType": {...},
  "discrepanciesBySeverity": {...},
  "recommendations": [...]
}
```

### 6. Get Validation Progress
**GET** `/api/v1/validation/progress`

Get the current progress of a running validation.

**Authentication:** Required

**Example:**
```bash
curl -u admin:admin123 http://localhost:8080/api/v1/validation/progress
```

**Response:**
```json
{
  "status": "IN_PROGRESS",
  "currentStep": "Generating tests",
  "progress": 45,
  "totalSteps": 100,
  "startTime": "2024-01-01T12:00:00",
  "estimatedTimeRemaining": "2 minutes"
}
```

### 7. Export Report as JSON
**GET** `/api/v1/validation/report/json`

Export the validation report in JSON format.

**Authentication:** Required

**Example:**
```bash
curl -u admin:admin123 http://localhost:8080/api/v1/validation/report/json
```

### 8. Export Report as Markdown
**GET** `/api/v1/validation/report/markdown`

Export the validation report in Markdown format.

**Authentication:** Required

**Example:**
```bash
curl -u admin:admin123 http://localhost:8080/api/v1/validation/report/markdown

## 🔌 MCP (Model Context Protocol) Endpoints

MCP endpoints provide AI agents with access to both OpenAPI documentation context and live API runtime behavior.

**Base URL:** `/api/mcp`

**Authentication:** Required (Basic Auth)

### MCP Server Information

**GET** `/api/mcp/info`

Get information about available MCP servers and their capabilities.

**Example:**
```bash
curl -u admin:admin123 http://localhost:8080/api/mcp/info
```

**Response:**
```json
{
  "servers": [
    {
      "name": "documentation",
      "type": "openapi",
      "description": "Provides OpenAPI specification and documentation context",
      "tools": ["get_openapi_spec", "get_endpoint_details", "get_schema_definition", "list_endpoints", "get_auth_requirements"]
    },
    {
      "name": "live-api",
      "type": "runtime",
      "description": "Provides live API runtime behavior and execution context",
      "tools": ["execute_api_call", "get_response_schema", "check_endpoint_availability", "validate_oauth_token", "get_api_metrics"]
    }
  ]
}
```

### Documentation MCP Endpoints

#### 9. Get OpenAPI Specification
**GET** `/api/mcp/documentation/spec`

Retrieve the complete OpenAPI specification.

**Authentication:** Required

**Example:**
```bash
curl -u admin:admin123 http://localhost:8080/api/mcp/documentation/spec
```

**Response:**
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

#### 10. Get Endpoint Details
**GET** `/api/mcp/documentation/endpoint`

Get detailed information about a specific endpoint.

**Authentication:** Required

**Query Parameters:**
- `path` (required): API endpoint path (e.g., "/v1/albums/{id}")
- `method` (required): HTTP method (GET, POST, PUT, DELETE, PATCH)

**Example:**
```bash
curl -u admin:admin123 \
  "http://localhost:8080/api/mcp/documentation/endpoint?path=/v1/albums/{id}&method=GET"
```

**Response:**
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

#### 11. Get Schema Definition
**GET** `/api/mcp/documentation/schema`

Retrieve a specific schema definition from the OpenAPI spec.

**Authentication:** Required

**Query Parameters:**
- `name` (required): Schema name (e.g., "AlbumObject")

**Example:**
```bash
curl -u admin:admin123 \
  "http://localhost:8080/api/mcp/documentation/schema?name=AlbumObject"
```

**Response:**
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

#### 12. List Endpoints
**GET** `/api/mcp/documentation/endpoints`

List all available endpoints, optionally filtered by tag.

**Authentication:** Required

**Query Parameters:**
- `tag` (optional): Filter by OpenAPI tag (e.g., "Albums", "Artists")

**Example:**
```bash
# List all endpoints
curl -u admin:admin123 http://localhost:8080/api/mcp/documentation/endpoints

# List endpoints by tag
curl -u admin:admin123 \
  "http://localhost:8080/api/mcp/documentation/endpoints?tag=Albums"
```

**Response:**
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

#### 13. Get Authentication Requirements
**GET** `/api/mcp/documentation/auth`

Get authentication requirements for an endpoint.

**Authentication:** Required

**Query Parameters:**
- `path` (required): API endpoint path
- `method` (required): HTTP method

**Example:**
```bash
curl -u admin:admin123 \
  "http://localhost:8080/api/mcp/documentation/auth?path=/v1/albums/{id}&method=GET"
```

**Response:**
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

### Live API MCP Endpoints

#### 14. Execute API Call
**POST** `/api/mcp/live/execute`

Execute a live API call and return the response.

**Authentication:** Required

**Request Body:**
```json
{
  "path": "/v1/albums/4aawyAB9vmqN3uQ7FjRGTy",
  "method": "GET",
  "headers": {
    "Authorization": "Bearer YOUR_TOKEN"
  },
  "queryParams": {
    "market": "US"
  }
}
```

**Example:**
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/mcp/live/execute \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/v1/albums/4aawyAB9vmqN3uQ7FjRGTy",
    "method": "GET",
    "headers": {"Authorization": "Bearer YOUR_TOKEN"}
  }'
```

**Response:**
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

#### 15. Get Response Schema
**GET** `/api/mcp/live/schema`

Analyze the actual response schema from a live API call.

**Authentication:** Required

**Query Parameters:**
- `path` (required): API endpoint path
- `method` (required): HTTP method

**Example:**
```bash
curl -u admin:admin123 \
  "http://localhost:8080/api/mcp/live/schema?path=/v1/albums/{id}&method=GET"
```

**Response:**
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

#### 16. Check Endpoint Availability
**GET** `/api/mcp/live/availability`

Check if an endpoint is available and responding.

**Authentication:** Required

**Query Parameters:**
- `path` (required): API endpoint path
- `method` (required): HTTP method

**Example:**
```bash
curl -u admin:admin123 \
  "http://localhost:8080/api/mcp/live/availability?path=/v1/albums/{id}&method=GET"
```

**Response:**
```json
{
  "success": true,
  "available": true,
  "statusCode": 200,
  "responseTime": 123
}
```

#### 17. Validate OAuth Token
**POST** `/api/mcp/live/validate-token`

Validate an OAuth token against the API.

**Authentication:** Required

**Request Body:**
```json
{
  "token": "your-access-token"
}
```

**Example:**
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/mcp/live/validate-token \
  -H "Content-Type: application/json" \
  -d '{"token": "your-access-token"}'
```

**Response:**
```json
{
  "success": true,
  "valid": true,
  "expiresIn": 3600,
  "scopes": ["user-read-private", "user-read-email"]
}
```

#### 18. Get API Metrics
**GET** `/api/mcp/live/metrics`

Retrieve performance metrics for API calls.

**Authentication:** Required

**Query Parameters:**
- `path` (optional): Filter by specific endpoint path

**Example:**
```bash
# Get all metrics
curl -u admin:admin123 http://localhost:8080/api/mcp/live/metrics

# Get metrics for specific endpoint
curl -u admin:admin123 \
  "http://localhost:8080/api/mcp/live/metrics?path=/v1/albums/{id}"
```

**Response:**
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

### MCP Usage with Python

```python
import requests
from requests.auth import HTTPBasicAuth

BASE_URL = "http://localhost:8080/api/mcp"
AUTH = HTTPBasicAuth("admin", "admin123")

# Get OpenAPI spec
response = requests.get(f"{BASE_URL}/documentation/spec", auth=AUTH)
spec = response.json()

# Get endpoint details
response = requests.get(
    f"{BASE_URL}/documentation/endpoint",
    params={"path": "/v1/albums/{id}", "method": "GET"},
    auth=AUTH
)
endpoint = response.json()

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

# Get API metrics
response = requests.get(f"{BASE_URL}/live/metrics", auth=AUTH)
metrics = response.json()
```

```

## 🔧 Using with Postman

### 1. Import Collection
Create a new Postman collection with these endpoints.

### 2. Set Authorization
- Type: Basic Auth
- Username: `admin`
- Password: `admin123`

### 3. Test Endpoints
Start with the public endpoints, then try the protected ones.

## 🐍 Using with Python

```python
import requests
from requests.auth import HTTPBasicAuth

# Base URL
base_url = "http://localhost:8080"

# Authentication
auth = HTTPBasicAuth('admin', 'admin123')

# Health check (no auth)
response = requests.get(f"{base_url}/api/health")
print(response.json())

# Start validation (with auth)
validation_request = {
    "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
    "endpointPaths": ["/v1/albums/{id}"]
}

response = requests.post(
    f"{base_url}/api/v1/validation/start",
    json=validation_request,
    auth=auth
)
print(response.json())

# Get progress
response = requests.get(
    f"{base_url}/api/v1/validation/progress",
    auth=auth
)
print(response.json())
```

## 🌐 Using with JavaScript/Fetch

```javascript
// Health check (no auth)
fetch('http://localhost:8080/api/health')
  .then(response => response.json())
  .then(data => console.log(data));

// Start validation (with auth)
const credentials = btoa('admin:admin123');

fetch('http://localhost:8080/api/v1/validation/start', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Basic ${credentials}`
  },
  body: JSON.stringify({
    openApiUrl: 'https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml',
    endpointPaths: ['/v1/albums/{id}']
  })
})
.then(response => response.json())
.then(data => console.log(data));
```

## 📝 Notes

### CSRF Protection
CSRF is disabled for API endpoints to allow easy testing. In production, you should enable it.

### CORS
CORS is enabled for all origins (`*`) in the ValidationController. Adjust this for production.

### Rate Limiting
Currently, there's no rate limiting. Consider adding it for production use.

## 🔍 Troubleshooting

### Issue: 401 Unauthorized
**Solution:** Make sure you're including authentication credentials:
```bash
curl -u admin:admin123 http://localhost:8080/api/v1/validation/progress
```

### Issue: 404 Not Found
**Solution:** Check the endpoint path. Remember:
- Validation endpoints are at `/api/v1/validation/*`
- Not `/api/validate` (this doesn't exist)

### Issue: Connection Refused
**Solution:** Make sure the application is running:
```bash
mvn spring-boot:run
```

## 📚 Related Documentation

- [LOGIN_GUIDE.md](LOGIN_GUIDE.md) - Login credentials and authentication
- [README.md](README.md) - Main project documentation
- [GETTING_STARTED.md](GETTING_STARTED.md) - Getting started guide