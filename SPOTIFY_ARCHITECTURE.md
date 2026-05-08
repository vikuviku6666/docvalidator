# Spotify API Documentation Testing Framework - Architecture

## Hackathon Demonstration: AI-Powered API Documentation Validation

This framework validates Spotify's API documentation against live responses, demonstrating how AI agents and MCP can ensure documentation accuracy for complex, real-world APIs.

---

## System Architecture Overview

```mermaid
graph TB
    subgraph "External API"
        SPOTIFY[Spotify Web API<br/>api.spotify.com]
        OAUTH[OAuth 2.0<br/>Authentication]
    end
    
    subgraph "Documentation Sources"
        SPEC[Spotify OpenAPI Spec<br/>developer.spotify.com]
        DOCS[API Reference Docs<br/>Endpoints & Schemas]
    end
    
    subgraph "MCP Layer - Context Providers"
        MCP1[MCP Server 1<br/>Documentation Context<br/>- OpenAPI specs<br/>- Endpoint definitions<br/>- Schema contracts]
        MCP2[MCP Server 2<br/>Live API Context<br/>- Real responses<br/>- OAuth tokens<br/>- Rate limits]
    end
    
    subgraph "AI Agent Layer"
        AG1[Test Generator Agent<br/>- Parse API specs<br/>- Generate test scenarios<br/>- Create edge cases]
        AG2[Validator Agent<br/>- Semantic analysis<br/>- Schema validation<br/>- Response comparison]
        AG3[Reporter Agent<br/>- Discrepancy detection<br/>- Fix recommendations<br/>- Impact analysis]
    end
    
    subgraph "Testing Framework Core"
        PARSER[OpenAPI Parser<br/>Extract endpoints & schemas]
        ENGINE[Test Execution Engine<br/>RestAssured + JUnit 5]
        VALIDATOR[Schema Validator<br/>JSON Schema validation]
        REPORTER[Report Generator<br/>HTML/JSON reports]
    end
    
    subgraph "Test Outputs"
        TESTS[Generated Test Suite<br/>- Positive tests<br/>- Negative tests<br/>- Edge cases<br/>- Workflows]
        REPORTS[Validation Reports<br/>- Discrepancies<br/>- Recommendations<br/>- Metrics]
    end
    
    SPEC --> MCP1
    DOCS --> MCP1
    SPOTIFY --> MCP2
    OAUTH --> MCP2
    
    MCP1 --> AG1
    MCP2 --> AG2
    MCP1 --> AG2
    
    AG1 --> PARSER
    PARSER --> ENGINE
    ENGINE --> SPOTIFY
    SPOTIFY --> ENGINE
    ENGINE --> VALIDATOR
    AG2 --> VALIDATOR
    VALIDATOR --> AG3
    AG3 --> REPORTER
    
    ENGINE --> TESTS
    REPORTER --> REPORTS
    
    style SPOTIFY fill:#1DB954
    style MCP1 fill:#4A90E2
    style MCP2 fill:#4A90E2
    style AG1 fill:#F5A623
    style AG2 fill:#F5A623
    style AG3 fill:#F5A623
```

---

## Detailed Component Architecture

### 1. MCP Servers - Context Management

```mermaid
graph LR
    subgraph "MCP Server 1: Documentation Context"
        DOC_TOOLS[Exposed Tools]
        DOC_TOOLS --> T1[get_openapi_spec]
        DOC_TOOLS --> T2[get_endpoint_details]
        DOC_TOOLS --> T3[get_schema_definition]
        DOC_TOOLS --> T4[list_all_endpoints]
        DOC_TOOLS --> T5[get_auth_requirements]
    end
    
    subgraph "MCP Server 2: Live API Context"
        API_TOOLS[Exposed Tools]
        API_TOOLS --> T6[execute_api_call]
        API_TOOLS --> T7[get_response_schema]
        API_TOOLS --> T8[check_rate_limits]
        API_TOOLS --> T9[validate_oauth_token]
        API_TOOLS --> T10[get_api_metrics]
    end
    
    style DOC_TOOLS fill:#4A90E2
    style API_TOOLS fill:#4A90E2
```

### 2. AI Agent Workflow

```mermaid
sequenceDiagram
    participant TF as Testing Framework
    participant MCP1 as MCP Doc Server
    participant AG1 as Test Generator
    participant MCP2 as MCP API Server
    participant AG2 as Validator Agent
    participant SPOT as Spotify API
    participant AG3 as Reporter Agent
    
    Note over TF,AG3: Phase 1: Test Generation
    TF->>MCP1: Request Spotify API specs
    MCP1->>TF: Return OpenAPI specification
    TF->>AG1: Generate tests from specs
    AG1->>AG1: Analyze endpoints, schemas, auth
    AG1->>TF: Return test scenarios
    
    Note over TF,AG3: Phase 2: Test Execution
    loop For each endpoint test
        TF->>MCP2: Request OAuth token
        MCP2->>TF: Return valid token
        TF->>MCP2: Execute API call
        MCP2->>SPOT: HTTP Request with OAuth
        SPOT->>MCP2: HTTP Response
        MCP2->>TF: Return response data
    end
    
    Note over TF,AG3: Phase 3: Validation
    TF->>AG2: Validate responses
    AG2->>MCP1: Get expected schemas
    MCP1->>AG2: Return schema definitions
    AG2->>AG2: Semantic comparison
    AG2->>TF: Return validation results
    
    Note over TF,AG3: Phase 4: Reporting
    TF->>AG3: Generate report
    AG3->>AG3: Analyze discrepancies
    AG3->>AG3: Generate recommendations
    AG3->>TF: Return actionable report
```

---

## Spotify API Endpoints for Demonstration

### Endpoint Categories

```mermaid
mindmap
  root((Spotify API<br/>Testing))
    Playlists
      Get Playlist
      Create Playlist
      Update Playlist
      Add Items
      Remove Items
    Tracks
      Get Track
      Get Multiple Tracks
      Get Audio Features
      Get Audio Analysis
    Albums
      Get Album
      Get Album Tracks
      Get Multiple Albums
    Artists
      Get Artist
      Get Artist Albums
      Get Artist Top Tracks
    Search
      Search Tracks
      Search Artists
      Search Albums
      Search Playlists
    User Profile
      Get Current User
      Get User Profile
      Get User Playlists
    Player
      Get Playback State
      Start Playback
      Pause Playback
      Skip Track
```

### Example Test Scenarios

#### 1. Get Track Endpoint
```
Endpoint: GET /v1/tracks/{id}
Documentation: Returns track details including name, artists, album
Test Scenarios:
  ✓ Valid track ID returns 200 with complete schema
  ✓ Invalid track ID returns 404
  ✓ Response includes all documented fields
  ✓ Audio features link is valid
  ✗ DISCREPANCY: popularity field sometimes null (not documented)
```

#### 2. Search Endpoint
```
Endpoint: GET /v1/search
Documentation: Search for tracks, artists, albums, playlists
Test Scenarios:
  ✓ Query parameter required
  ✓ Type parameter accepts multiple values
  ✓ Limit parameter works (1-50)
  ✓ Offset parameter for pagination
  ✗ DISCREPANCY: Market parameter behavior differs from docs
```

#### 3. Create Playlist Endpoint
```
Endpoint: POST /v1/users/{user_id}/playlists
Documentation: Creates a playlist for a user
Test Scenarios:
  ✓ Valid OAuth token required
  ✓ Name field is required
  ✓ Public/private flags work
  ✓ Description is optional
  ✗ DISCREPANCY: Collaborative flag requires additional scope (not documented)
```

---

## Test Generation Strategy for Spotify API

### Contract-Based Test Generation

```mermaid
graph TB
    subgraph "Input Sources"
        OAS[OpenAPI Spec]
        AUTH[OAuth 2.0 Scopes]
        RATE[Rate Limits]
        EXAMPLES[API Examples]
    end
    
    subgraph "AI Analysis"
        PARSE[Parse Specifications]
        ANALYZE[Analyze Contracts]
        GENERATE[Generate Scenarios]
    end
    
    subgraph "Generated Tests"
        AUTH_TESTS[Authentication Tests<br/>- Valid tokens<br/>- Expired tokens<br/>- Missing scopes]
        CRUD_TESTS[CRUD Tests<br/>- Create playlist<br/>- Read track<br/>- Update playlist<br/>- Delete playlist]
        SEARCH_TESTS[Search Tests<br/>- Query variations<br/>- Filter combinations<br/>- Pagination]
        EDGE_TESTS[Edge Cases<br/>- Rate limiting<br/>- Invalid IDs<br/>- Boundary values]
        WORKFLOW_TESTS[Workflows<br/>- Create & populate playlist<br/>- Search & play track<br/>- Follow & unfollow]
    end
    
    OAS --> PARSE
    AUTH --> PARSE
    RATE --> PARSE
    EXAMPLES --> PARSE
    
    PARSE --> ANALYZE
    ANALYZE --> GENERATE
    
    GENERATE --> AUTH_TESTS
    GENERATE --> CRUD_TESTS
    GENERATE --> SEARCH_TESTS
    GENERATE --> EDGE_TESTS
    GENERATE --> WORKFLOW_TESTS
```

---

## Validation & Reporting Flow

```mermaid
graph TB
    subgraph "Test Execution"
        EXEC[Execute Test Suite]
        CAPTURE[Capture Responses]
    end
    
    subgraph "Validation"
        SCHEMA[Schema Validation]
        STATUS[Status Code Check]
        HEADERS[Header Validation]
        SEMANTIC[Semantic Analysis]
    end
    
    subgraph "Discrepancy Detection"
        COMPARE[Compare Doc vs Reality]
        CLASSIFY[Classify Issues]
        SEVERITY[Assess Severity]
    end
    
    subgraph "Report Generation"
        SUMMARY[Executive Summary]
        DETAILS[Detailed Findings]
        FIXES[Recommended Fixes]
        METRICS[Test Metrics]
    end
    
    EXEC --> CAPTURE
    CAPTURE --> SCHEMA
    CAPTURE --> STATUS
    CAPTURE --> HEADERS
    CAPTURE --> SEMANTIC
    
    SCHEMA --> COMPARE
    STATUS --> COMPARE
    HEADERS --> COMPARE
    SEMANTIC --> COMPARE
    
    COMPARE --> CLASSIFY
    CLASSIFY --> SEVERITY
    
    SEVERITY --> SUMMARY
    SEVERITY --> DETAILS
    SEVERITY --> FIXES
    SEVERITY --> METRICS
    
    style COMPARE fill:#FF6B6B
    style CLASSIFY fill:#FF6B6B
    style SEVERITY fill:#FF6B6B
```

---

## Example Validation Report Structure

```mermaid
graph LR
    REPORT[Validation Report]
    
    REPORT --> EXEC_SUM[Executive Summary<br/>- Total tests: 150<br/>- Passed: 142<br/>- Failed: 8<br/>- Discrepancies: 12]
    
    REPORT --> DISC[Discrepancies Found]
    DISC --> D1[Critical: 2<br/>- Missing required field<br/>- Wrong status code]
    DISC --> D2[High: 4<br/>- Schema mismatch<br/>- Undocumented behavior]
    DISC --> D3[Medium: 6<br/>- Optional field issues<br/>- Format differences]
    
    REPORT --> RECS[Recommendations]
    RECS --> R1[Update OpenAPI spec<br/>- Add missing fields<br/>- Fix schema definitions]
    RECS --> R2[Update documentation<br/>- Clarify OAuth scopes<br/>- Document edge cases]
    RECS --> R3[Fix API implementation<br/>- Align with spec<br/>- Add validation]
    
    style D1 fill:#FF6B6B
    style D2 fill:#FFA500
    style D3 fill:#FFD700
```

---

## Technology Stack

### Core Framework
- **Language**: Java 21
- **Framework**: Spring Boot 3.x
- **Testing**: JUnit 5, RestAssured
- **Build Tool**: Maven

### Key Dependencies
```xml
<!-- API Testing -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
</dependency>

<!-- OpenAPI Parser -->
<dependency>
    <groupId>io.swagger.parser.v3</groupId>
    <artifactId>swagger-parser</artifactId>
</dependency>

<!-- OAuth 2.0 -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-client</artifactId>
</dependency>

<!-- JSON Schema Validation -->
<dependency>
    <groupId>com.networknt</groupId>
    <artifactId>json-schema-validator</artifactId>
</dependency>

<!-- MCP SDK -->
<dependency>
    <groupId>io.modelcontextprotocol</groupId>
    <artifactId>mcp-java-sdk</artifactId>
</dependency>

<!-- AI Integration -->
<dependency>
    <groupId>com.theokanning.openai-gpt3-java</groupId>
    <artifactId>service</artifactId>
</dependency>
```

---

## Hackathon Demo Flow

```mermaid
graph TB
    START[Start Demo] --> INTRO[Introduce Problem<br/>Documentation drift]
    INTRO --> SHOW_ARCH[Show Architecture<br/>MCP + AI Agents]
    SHOW_ARCH --> DEMO1[Demo 1: Test Generation<br/>AI generates tests from Spotify API spec]
    DEMO1 --> DEMO2[Demo 2: Live Validation<br/>Execute tests against Spotify API]
    DEMO2 --> DEMO3[Demo 3: Discrepancy Detection<br/>Show found issues]
    DEMO3 --> DEMO4[Demo 4: Actionable Reports<br/>AI-generated fix recommendations]
    DEMO4 --> IMPACT[Show Impact<br/>- Developer productivity<br/>- API reliability<br/>- Agent accuracy]
    IMPACT --> QA[Q&A]
    
    style START fill:#1DB954
    style DEMO1 fill:#4A90E2
    style DEMO2 fill:#4A90E2
    style DEMO3 fill:#FF6B6B
    style DEMO4 fill:#F5A623
    style IMPACT fill:#1DB954
```

---

## Key Differentiators for Hackathon

1. **Real-World API**: Using Spotify's production API (not a toy example)
2. **AI-Powered**: Intelligent test generation and semantic validation
3. **MCP Integration**: Modern context protocol for efficient data sharing
4. **Actionable Insights**: Not just finding problems, but suggesting solutions
5. **Scalable**: Works with any OpenAPI-documented API
6. **Developer-Focused**: Solves real pain points in API development

---

## Success Metrics

```mermaid
graph LR
    METRICS[Success Metrics]
    
    METRICS --> M1[Coverage<br/>- 150+ endpoints tested<br/>- All HTTP methods<br/>- Auth scenarios]
    METRICS --> M2[Accuracy<br/>- 95%+ discrepancy detection<br/>- Low false positives<br/>- Semantic understanding]
    METRICS --> M3[Performance<br/>- Test suite in < 5 min<br/>- Real-time validation<br/>- Parallel execution]
    METRICS --> M4[Usability<br/>- Clear reports<br/>- Actionable recommendations<br/>- Easy integration]
    
    style M1 fill:#4A90E2
    style M2 fill:#1DB954
    style M3 fill:#F5A623
    style M4 fill:#9B59B6
```

---

## Future Enhancements

- **Auto-Fix**: Automatically generate documentation patches
- **Continuous Monitoring**: Real-time validation in CI/CD
- **Multi-API Support**: Test multiple APIs simultaneously
- **ML-Based Prediction**: Predict potential documentation issues
- **Integration Hub**: Connect with Postman, Swagger UI, etc.

---

This architecture demonstrates a production-ready solution for ensuring API documentation accuracy using cutting-edge AI and MCP technology, perfect for a hackathon showcase with Spotify's comprehensive API.