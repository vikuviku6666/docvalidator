# DocValidator: AI-Powered API Documentation Testing Framework

## Project Overview

An intelligent testing framework that programmatically validates API documentation against live systems using AI agents and Model Context Protocol (MCP). The solution ensures documentation accuracy by generating executable test cases from API specifications, running them against live APIs, and producing actionable reports when discrepancies are found.

## Problem Statement

**Documentation Drift**: API documentation often becomes outdated, leading to:
- Developer confusion and wasted time
- Integration failures
- Reduced AI agent reliability (agents depend on accurate documentation)
- Loss of trust in documentation

**Current Gap**: No automated way to continuously validate that documented API behavior matches actual system responses.

## Solution

A comprehensive testing framework that treats **documentation as testable code** by:

1. **Parsing API Specifications**: Extracts endpoints, schemas, and contracts from OpenAPI/Swagger specs
2. **Generating Tests with AI**: Intelligent agents create comprehensive test suites including edge cases
3. **Validating Against Live Systems**: Executes tests against real APIs (Spotify, internal APIs, etc.)
4. **Detecting Discrepancies**: AI-powered semantic analysis identifies documentation-reality gaps
5. **Producing Actionable Reports**: Clear reports with specific fix recommendations

## Architecture

### High-Level Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Documentation Sources                     │
│  OpenAPI Specs • Service Contracts • API Examples           │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                      MCP Layer                               │
│  ┌──────────────────────┐  ┌──────────────────────┐        │
│  │ MCP Server 1         │  │ MCP Server 2         │        │
│  │ Documentation Context│  │ Live API Context     │        │
│  └──────────────────────┘  └──────────────────────┘        │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                    AI Agent Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │Test Generator│  │  Validator  │  │  Reporter   │        │
│  │    Agent     │  │    Agent    │  │    Agent    │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│              Testing Framework Core                          │
│  Parser • Test Engine • Validator • Reporter                │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                    Target APIs                               │
│  Spotify API • Any OpenAPI-documented API                    │
└─────────────────────────────────────────────────────────────┘
```

### Key Technologies

- **Language**: Java 21
- **Framework**: Spring Boot 3.x
- **Testing**: JUnit 5, RestAssured
- **API Parsing**: Swagger Parser, OpenAPI 3.0
- **MCP**: Model Context Protocol for efficient context sharing
- **AI Integration**: OpenAI/Claude for intelligent test generation and analysis
- **Authentication**: OAuth 2.0, API Keys, Basic Auth support
- **Build**: Maven

## Core Features

### 1. Intelligent Test Generation
- AI analyzes OpenAPI specifications
- Generates comprehensive test scenarios (positive, negative, edge cases)
- Creates realistic test data
- Understands REST semantics and common patterns

### 2. MCP-Powered Context Management
- **MCP Server 1**: Exposes documentation context (specs, schemas, contracts)
- **MCP Server 2**: Provides live API context (responses, metrics, behavior)
- Efficient context sharing between components
- Real-time API monitoring

### 3. Semantic Validation
- Goes beyond syntax checking
- Understands intent and context
- Identifies logical inconsistencies
- Tolerates acceptable variations

### 4. Actionable Reporting
- Clear identification of discrepancies
- Severity classification (Critical, High, Medium, Low)
- Specific fix recommendations
- Documentation patch suggestions
- Impact analysis

## Demonstration: Spotify API

The framework is demonstrated using Spotify's Web API, showcasing validation across:

### Endpoint Categories (150+ endpoints)
- **Playlists**: Create, read, update, delete, add/remove tracks
- **Tracks**: Get track details, audio features, audio analysis
- **Albums**: Get album info, tracks, multiple albums
- **Artists**: Get artist details, albums, top tracks
- **Search**: Search tracks, artists, albums, playlists
- **User Profile**: Get current user, user playlists
- **Player**: Playback control, queue management

### Example Test Scenarios

#### Track Retrieval
```
Endpoint: GET /v1/tracks/{id}
✓ Valid track ID returns 200 with complete schema
✓ Invalid track ID returns 404
✓ Response includes all documented fields
✗ DISCREPANCY: popularity field sometimes null (not documented)
```

#### Playlist Creation
```
Endpoint: POST /v1/users/{user_id}/playlists
✓ Valid OAuth token required
✓ Name field is required
✓ Public/private flags work
✗ DISCREPANCY: Collaborative flag requires additional scope (not documented)
```

#### Search Functionality
```
Endpoint: GET /v1/search
✓ Query parameter required
✓ Type parameter accepts multiple values
✓ Pagination works correctly
✗ DISCREPANCY: Market parameter behavior differs from documentation
```

## Project Structure

```
doc-testing-framework/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/doctest/
│   │   │   │   ├── agent/          # AI agents
│   │   │   │   │   ├── TestGeneratorAgent.java
│   │   │   │   │   ├── ValidatorAgent.java
│   │   │   │   │   └── ReporterAgent.java
│   │   │   │   ├── mcp/            # MCP servers
│   │   │   │   │   ├── DocumentationServer.java
│   │   │   │   │   └── LiveApiServer.java
│   │   │   │   ├── parser/         # OpenAPI parsing
│   │   │   │   │   └── OpenAPIParser.java
│   │   │   │   ├── engine/         # Test execution
│   │   │   │   │   └── TestExecutionEngine.java
│   │   │   │   ├── validator/      # Schema validation
│   │   │   │   │   └── SchemaValidator.java
│   │   │   │   └── reporter/       # Report generation
│   │   │   │       └── ReportGenerator.java
│   │   └── resources/
│   │       ├── application.yml     # Configuration
│   │       └── schemas/            # JSON schemas
│   └── test/
│       └── java/
│           └── com/doctest/
│               └── spotify/        # Spotify API tests
├── docs/
│   ├── ARCHITECTURE.md             # System architecture
│   ├── SPOTIFY_ARCHITECTURE.md     # Spotify-specific design
│   └── TEST_GENERATION_STRATEGY.md # Test generation details
├── pom.xml                         # Maven configuration
└── README.md                       # Getting started guide
```

## Use Cases

### 1. Continuous Integration
- Run in CI/CD pipeline
- Validate documentation on every commit
- Prevent documentation drift
- Gate deployments on documentation accuracy

### 2. API Development
- Validate implementation against specification
- Ensure consistency across versions
- Catch breaking changes early
- Maintain API contract integrity

### 3. Third-Party API Integration
- Validate external API documentation
- Detect undocumented changes
- Ensure integration reliability
- Monitor API evolution

### 4. AI Agent Development
- Provide accurate context for AI agents
- Ensure agents have correct API information
- Improve agent reliability
- Reduce hallucinations from incorrect docs

## Benefits

### For Developers
- **Time Savings**: No more debugging due to incorrect documentation
- **Confidence**: Trust that documentation matches reality
- **Productivity**: Faster integration with accurate specs
- **Quality**: Catch issues before they reach production

### For Organizations
- **Reliability**: Consistent API behavior
- **Maintainability**: Automated documentation validation
- **Developer Experience**: Better onboarding and integration
- **Cost Reduction**: Fewer support tickets from documentation issues

### For AI Systems
- **Accuracy**: Correct context for AI agents
- **Reliability**: Agents can trust documentation
- **Performance**: Fewer errors from incorrect information
- **Scalability**: Automated validation at scale

## Success Metrics

- **Coverage**: 150+ endpoints tested across multiple API categories
- **Accuracy**: 95%+ discrepancy detection rate
- **Performance**: Complete test suite execution in < 5 minutes
- **Usability**: Clear, actionable reports with specific recommendations

## Future Enhancements

1. **Auto-Fix**: Automatically generate documentation patches
2. **Continuous Monitoring**: Real-time validation in production
3. **Multi-API Support**: Test multiple APIs simultaneously
4. **ML-Based Prediction**: Predict potential documentation issues
5. **Integration Hub**: Connect with Postman, Swagger UI, Stoplight
6. **Version Comparison**: Track documentation changes across versions
7. **Performance Testing**: Validate response times against SLAs
8. **Security Testing**: Validate authentication and authorization

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.9+
- API credentials (for target APIs)
- OpenAI/Claude API key (for AI features)

### Quick Start
```bash
# Clone repository
git clone <repository-url>
cd docvalidator

# Configure API credentials
cp src/main/resources/application.yml.example src/main/resources/application.yml
# Edit application.yml with your credentials

# Build project
mvn clean install

# Run tests against Spotify API
mvn test -Dtest=SpotifyApiValidationTest

# Generate validation report
mvn exec:java -Dexec.mainClass="com.docvalidator.Main"
```

### Configuration
```yaml
doc-validator:
  target-api:
    base-url: https://api.spotify.com/v1
    openapi-spec: https://developer.spotify.com/openapi
  mcp:
    doc-server:
      enabled: true
    api-server:
      enabled: true
  ai:
    provider: openai
    model: gpt-4
    api-key: ${OPENAI_API_KEY}
  authentication:
    type: oauth2
    client-id: ${SPOTIFY_CLIENT_ID}
    client-secret: ${SPOTIFY_CLIENT_SECRET}
```

## Documentation

- **[ARCHITECTURE.md](ARCHITECTURE.md)**: Detailed system architecture
- **[SPOTIFY_ARCHITECTURE.md](SPOTIFY_ARCHITECTURE.md)**: Spotify API demonstration design
- **[TEST_GENERATION_STRATEGY.md](TEST_GENERATION_STRATEGY.md)**: Test generation methodology

## Contributing

Contributions welcome! Areas for contribution:
- Additional API integrations
- New test generation strategies
- Enhanced AI agent capabilities
- Performance optimizations
- Documentation improvements

## License

MIT License - See LICENSE file for details

## Contact

For questions, issues, or collaboration opportunities, please open an issue on GitHub.

---

**Built for Hackathon**: Demonstrating how AI and MCP can solve real-world API documentation challenges at scale.