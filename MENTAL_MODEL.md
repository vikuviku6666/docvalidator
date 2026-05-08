# DocValidator - Mental Model & How It Works

## 🎯 What Is DocValidator?

DocValidator is an **AI-powered testing framework** that automatically validates API documentation against live API behavior. Think of it as a "spell checker" for API docs, but instead of checking spelling, it verifies that what the documentation says matches what the API actually does.

---

## 🧠 Core Concept: Documentation as Testable Code

The fundamental insight is: **API documentation should be treated like code** - it can drift from reality, so it needs automated testing.

```
Documentation Says: "Returns 200 with user object"
Reality Check: Does the API actually return 200? Does it return a user object?
DocValidator: Automatically tests this and reports discrepancies
```

---

## 🏗️ High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER INTERACTION                         │
│  Web UI / REST API / CLI - Trigger validation runs              │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│                   VALIDATION ORCHESTRATOR                        │
│  Coordinates the entire workflow (4 phases)                      │
└────────────────────────┬────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
┌───────▼──────┐  ┌──────▼──────┐  ┌─────▼──────┐
│   Phase 1    │  │   Phase 2   │  │  Phase 3   │  ┌──────────┐
│   PARSE      │─▶│  GENERATE   │─▶│  EXECUTE   │─▶│ Phase 4  │
│   OpenAPI    │  │   Tests     │  │   Tests    │  │  REPORT  │
└──────────────┘  └─────────────┘  └────────────┘  └──────────┘
```

---

## 🔄 The 4-Phase Validation Workflow

### Phase 1: PARSE 📖
**What happens:** Read and understand the API documentation

```java
OpenApiParser.parseFromUrl(openApiUrl)
  ↓
Extracts:
  • Endpoints (GET /tracks/{id}, POST /playlists, etc.)
  • Parameters (path params, query params, headers)
  • Request/Response schemas
  • Authentication requirements
  • Expected status codes
```

**Output:** List of [`ApiEndpoint`](backend/src/main/java/com/docvalidator/model/ApiEndpoint.java) objects

---

### Phase 2: GENERATE 🤖
**What happens:** AI creates comprehensive test scenarios

```java
TestGeneratorAgent.generateTestCases(endpoint)
  ↓
For each endpoint, generates:
  • Positive tests (happy path)
  • Negative tests (error cases)
  • Edge cases (boundary conditions)
  • Performance tests
  • Schema validation tests
```

**Intelligence:**
- Uses AI (GPT-4/Claude) to understand endpoint semantics
- Accesses MCP (Model Context Protocol) for live API context
- Creates realistic test data
- Considers authentication requirements

**Output:** List of [`TestCase`](backend/src/main/java/com/docvalidator/model/TestCase.java) objects

---

### Phase 3: EXECUTE ⚡
**What happens:** Run tests against the live API

```java
TestExecutionEngine.executeTests(testCases)
  ↓
For each test:
  1. Build HTTP request (RestAssured)
  2. Add authentication (OAuth 2.0 for Spotify)
  3. Execute request against live API
  4. Capture response (status, headers, body, timing)
  5. Validate against expectations
```

**Key Features:**
- Parallel execution (10 threads)
- Real-time progress tracking
- Automatic retry on failures
- Response time measurement

**Output:** List of [`ValidationResult`](backend/src/main/java/com/docvalidator/model/ValidationResult.java) objects

---

### Phase 4: REPORT 📊
**What happens:** AI analyzes results and generates actionable insights

```java
ReporterAgent.generateReport(validationResults)
  ↓
Produces:
  • Summary statistics (pass/fail rates)
  • Health score (0-100%)
  • Discrepancies grouped by type and severity
  • AI-generated fix recommendations
  • Export formats (JSON, Markdown, HTML)
```

**AI-Powered Analysis:**
- Identifies patterns in failures
- Suggests root causes
- Provides specific fix recommendations
- Estimates effort required

**Output:** [`ValidationReport`](backend/src/main/java/com/docvalidator/model/ValidationReport.java)

---

## 🎭 The Three AI Agents

### 1. Test Generator Agent 🧪
**Role:** Creates intelligent test scenarios

**Capabilities:**
- Analyzes OpenAPI specs semantically
- Generates edge cases humans might miss
- Creates realistic test data
- Understands REST patterns

**Example:**
```
Endpoint: GET /tracks/{id}
Generates:
  ✓ Valid ID returns 200
  ✓ Invalid ID returns 404
  ✓ Missing auth returns 401
  ✓ Very long ID (edge case)
  ✓ Special characters in ID
```

---

### 2. Validator Agent ✅
**Role:** Compares expected vs actual behavior

**Checks:**
- Status codes match
- Response schemas match
- Required fields present
- Data types correct
- Response times acceptable
- Semantic correctness (AI-powered)

**Example:**
```
Expected: 200 OK with track object
Actual:   200 OK with track object
Result:   ✓ PASS

Expected: 404 Not Found
Actual:   400 Bad Request
Result:   ✗ FAIL - Status code mismatch
```

---

### 3. Reporter Agent 📝
**Role:** Generates actionable reports

**Capabilities:**
- Aggregates all validation results
- Calculates health scores
- Groups issues by severity
- Uses AI to suggest fixes
- Exports in multiple formats

**Example Output:**
```
Health Score: 91.5%
Total Tests: 450
Passed: 423 (94%)
Failed: 27

Critical Issues: 2
  • Missing required field in /playlists response
  • Wrong status code for /search endpoint

Recommendations:
  1. Update OpenAPI spec to document 400 status
  2. Add 'popularity' field to track schema
```

---

## 🔌 MCP Integration (Model Context Protocol)

MCP provides **real-time context** from both documentation and live APIs.

### MCP Server 1: Documentation Context
```
Tools Exposed:
  • get_openapi_spec() - Full API specification
  • get_endpoint_details() - Specific endpoint info
  • get_schema_definition() - Data schemas
  • list_endpoints() - All available endpoints
```

### MCP Server 2: Live API Context
```
Tools Exposed:
  • execute_api_call() - Make live requests
  • get_response_schema() - Actual response structure
  • check_endpoint_availability() - Is endpoint accessible?
  • get_api_metrics() - Performance data
```

**Why MCP?**
- Efficient context sharing between components
- Real-time API behavior monitoring
- Reduces redundant API calls
- Enables intelligent test generation

---

## 🎯 Example: Validating Spotify's Track Endpoint

Let's walk through a complete example:

### 1. Parse Phase
```yaml
Endpoint: GET /v1/tracks/{id}
Documentation says:
  - Path param: id (required, string)
  - Returns: 200 with Track object
  - Auth: OAuth 2.0 required
  - Schema: { id, name, artists[], album, duration_ms }
```

### 2. Generate Phase
```
AI creates 8 test cases:
  1. Valid track ID → expect 200
  2. Invalid track ID → expect 404
  3. Missing auth → expect 401
  4. Very long ID → expect 400 or 404
  5. Special chars in ID → expect 400 or 404
  6. Performance test → expect < 500ms
  7. Schema validation → all fields present
  8. Multiple requests → rate limiting check
```

### 3. Execute Phase
```
Test 1: GET /v1/tracks/3n3Ppam7vgaVa1iaRUc9Lp
  → 200 OK in 234ms
  → Body: { id: "3n3...", name: "Mr. Brightside", ... }
  → ✓ PASS

Test 2: GET /v1/tracks/invalid_id
  → 400 Bad Request in 156ms
  → Expected: 404, Got: 400
  → ✗ FAIL - Status code mismatch

Test 3: GET /v1/tracks/3n3... (no auth)
  → 401 Unauthorized in 89ms
  → ✓ PASS
```

### 4. Report Phase
```markdown
## Validation Report

**Endpoint:** GET /v1/tracks/{id}
**Tests:** 8 total, 7 passed, 1 failed

### Discrepancy Found
- **Type:** STATUS_CODE_MISMATCH
- **Severity:** HIGH
- **Description:** Invalid track ID returns 400 instead of documented 404
- **Recommendation:** Update OpenAPI spec to document 400 Bad Request
  for malformed IDs, or update API to return 404 for consistency.
```

---

## 🔐 Authentication Flow (Spotify Example)

```
1. User provides credentials in application.yml:
   - SPOTIFY_CLIENT_ID
   - SPOTIFY_CLIENT_SECRET

2. SpotifyAuthManager handles OAuth 2.0:
   - Requests access token from Spotify
   - Caches token (valid for 1 hour)
   - Auto-refreshes when expired

3. TestExecutionEngine adds auth to requests:
   - Header: Authorization: Bearer {token}
   - Automatically applied to all authenticated endpoints
```

---

## 📊 Data Flow Diagram

```
┌──────────────┐
│   OpenAPI    │
│     Spec     │
└──────┬───────┘
       │
       ▼
┌──────────────┐     ┌──────────────┐
│   OpenAPI    │────▶│     Test     │
│    Parser    │     │  Generator   │
└──────────────┘     │    Agent     │
                     └──────┬───────┘
                            │
                            ▼
                     ┌──────────────┐
                     │  Test Cases  │
                     │   (List)     │
                     └──────┬───────┘
                            │
                            ▼
                     ┌──────────────┐     ┌──────────────┐
                     │     Test     │────▶│  Spotify API │
                     │  Execution   │◀────│  (Live)      │
                     │    Engine    │     └──────────────┘
                     └──────┬───────┘
                            │
                            ▼
                     ┌──────────────┐
                     │  Validator   │
                     │    Agent     │
                     └──────┬───────┘
                            │
                            ▼
                     ┌──────────────┐
                     │ Validation   │
                     │   Results    │
                     └──────┬───────┘
                            │
                            ▼
                     ┌──────────────┐
                     │   Reporter   │
                     │    Agent     │
                     └──────┬───────┘
                            │
                            ▼
                     ┌──────────────┐
                     │    Report    │
                     │ (JSON/MD/UI) │
                     └──────────────┘
```

---

## 🎨 Frontend Architecture

```
Next.js Frontend (Port 3000)
  │
  ├─ Dashboard Page
  │   └─ Shows validation history, health trends
  │
  ├─ Run Validation Page
  │   └─ Trigger new validation runs
  │
  ├─ Progress Page
  │   └─ Real-time progress tracking
  │
  └─ Reports Page
      └─ View detailed validation reports

All pages communicate with:
  Backend REST API (Port 8080)
```

---

## 🚀 How to Use It

### 1. Start the Application
```bash
# Backend
cd backend
mvn spring-boot:run

# Frontend
cd frontend
npm run dev
```

### 2. Trigger Validation (3 Ways)

**Option A: Web UI**
```
1. Open http://localhost:3000
2. Navigate to "Run Validation"
3. Enter OpenAPI URL
4. Click "Start Validation"
5. Watch real-time progress
6. View report when complete
```

**Option B: REST API**
```bash
curl -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{"openApiUrl": "https://api.example.com/openapi.json"}'
```

**Option C: CLI**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--run-validation=true"
```

### 3. View Results
```
- Web UI: http://localhost:3000/reports
- JSON: target/reports/validation_report_*.json
- Markdown: target/reports/validation_report_*.md
```

---

## 🎯 Key Benefits

### For Developers
- **Save Time:** No manual testing of documentation
- **Catch Drift:** Detect when docs become outdated
- **Confidence:** Trust that docs match reality
- **Better Onboarding:** New developers get accurate info

### For Organizations
- **Quality:** Maintain high documentation standards
- **Automation:** Integrate into CI/CD pipelines
- **Reliability:** Reduce support tickets from bad docs
- **Developer Experience:** Improve API consumer satisfaction

### For AI Systems
- **Accuracy:** AI agents get correct API information
- **Reliability:** Fewer errors from incorrect docs
- **Trust:** Agents can rely on documentation

---

## 🔧 Technology Stack

### Backend
- **Java 21** - Modern Java features
- **Spring Boot 3.x** - Application framework
- **RestAssured** - HTTP client for API testing
- **JUnit 5** - Test framework
- **Swagger Parser** - OpenAPI parsing
- **H2 Database** - In-memory storage for history

### Frontend
- **Next.js 14** - React framework
- **TypeScript** - Type safety
- **Tailwind CSS** - Styling
- **React Query** - Data fetching

### AI Integration
- **OpenAI GPT-4** - Test generation & analysis
- **OpenRouter** - Multi-model support
- **Claude** - Alternative AI provider

### MCP
- **Model Context Protocol** - Context sharing
- **Java MCP SDK** - MCP implementation

---

## 📈 Success Metrics

```
Coverage:     150+ Spotify API endpoints tested
Accuracy:     95%+ discrepancy detection rate
Performance:  Complete suite in < 5 minutes
Usability:    Clear, actionable reports
```

---

## 🎓 Mental Model Summary

Think of DocValidator as a **continuous integration system for API documentation**:

1. **Input:** API documentation (OpenAPI spec)
2. **Process:** AI generates and executes comprehensive tests
3. **Validation:** Compare documented behavior vs actual behavior
4. **Output:** Detailed report with fix recommendations

**Key Insight:** Just like code needs tests, documentation needs validation. DocValidator automates this process using AI to ensure your API docs always match reality.

---

## 🔮 Future Enhancements

- **Auto-Fix:** Generate documentation patches automatically
- **Continuous Monitoring:** Real-time validation in production
- **Multi-API Support:** Test multiple APIs simultaneously
- **ML Prediction:** Predict potential documentation issues
- **Integration Hub:** Connect with Postman, Swagger UI, etc.

---

**Built with ❤️ to solve the documentation drift problem**