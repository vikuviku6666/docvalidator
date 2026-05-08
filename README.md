# DocValidator - AI-Powered API Documentation Testing Framework

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> Automatically validate API documentation against live API behavior using AI-powered agents

## 🎯 Overview

DocValidator is an intelligent framework that validates API documentation (OpenAPI/Swagger) against actual API behavior. It uses AI agents to generate comprehensive test cases, execute them against live APIs, detect discrepancies, and provide actionable recommendations for fixing documentation issues.

### Key Features

- 🤖 **AI-Powered Test Generation** - Automatically generates positive, negative, and edge case tests
- 🔍 **Semantic Validation** - Uses GPT-4 to detect logical inconsistencies beyond schema validation
- 📊 **Comprehensive Reporting** - Generates detailed reports with health scores and recommendations
- 🔐 **OAuth 2.0 Support** - Built-in authentication for secured APIs (Spotify, etc.)
- ⚡ **Parallel Execution** - Fast test execution with configurable concurrency
- 📈 **Real-time Progress** - Track validation progress via REST API or CLI
- 🎨 **Multiple Export Formats** - JSON, Markdown, and HTML reports

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    DocValidator Framework                    │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │   OpenAPI    │───▶│     Test     │───▶│     Test     │  │
│  │    Parser    │    │  Generator   │    │  Execution   │  │
│  │              │    │    Agent     │    │    Engine    │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│         │                    │                    │          │
│         │                    ▼                    ▼          │
│         │            ┌──────────────┐    ┌──────────────┐  │
│         └───────────▶│  Validator   │───▶│   Reporter   │  │
│                      │    Agent     │    │    Agent     │  │
│                      └──────────────┘    └──────────────┘  │
│                             │                    │          │
│                             ▼                    ▼          │
│                      ┌──────────────────────────────────┐  │
│                      │    Validation Report (JSON/MD)   │  │
│                      └──────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- OpenAI API Key
- Spotify API Credentials (for Spotify API validation)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/docvalidator.git
cd docvalidator
```

2. **Configure application**

Edit `src/main/resources/application.yml`:

```yaml
docvalidator:
  ai:
    api-key: ${OPENAI_API_KEY}  # Set via environment variable
    model: gpt-4
  
  target-api:
    base-url: https://api.spotify.com/v1
    auth:
      client-id: ${SPOTIFY_CLIENT_ID}
      client-secret: ${SPOTIFY_CLIENT_SECRET}
```

3. **Set environment variables**
```bash
export OPENAI_API_KEY="your-openai-api-key"
export SPOTIFY_CLIENT_ID="your-spotify-client-id"
export SPOTIFY_CLIENT_SECRET="your-spotify-client-secret"
```

4. **Build the project**
```bash
mvn clean install
```

### Running Validation

#### Option 1: Command Line

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--run-validation=true"
```

#### Option 2: REST API

Start the server:
```bash
mvn spring-boot:run
```

Trigger validation via API:
```bash
curl -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://developer.spotify.com/reference/web-api/open-api-schema.yml"
  }'
```

Check progress:
```bash
curl http://localhost:8080/api/v1/validation/progress
```

## 📖 Usage Examples

### Validate Specific Endpoints

```bash
curl -X POST http://localhost:8080/api/v1/validation/run \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://developer.spotify.com/reference/web-api/open-api-schema.yml",
    "endpointPaths": [
      "/albums/{id}",
      "/artists/{id}",
      "/tracks/{id}"
    ]
  }'
```

### Programmatic Usage

```java
@Autowired
private ValidationOrchestrator orchestrator;

// Run validation
ValidationReport report = orchestrator.runValidation(
    "https://developer.spotify.com/reference/web-api/open-api-schema.yml"
);

// Access results
System.out.println("Health Score: " + report.getHealthScore() + "%");
System.out.println("Total Tests: " + report.getSummary().getTotalTests());
System.out.println("Passed: " + report.getSummary().getPassedTests());
System.out.println("Failed: " + report.getSummary().getFailedTests());

// Get recommendations
report.getRecommendations().forEach(rec -> {
    System.out.println("Recommendation: " + rec.getTitle());
    System.out.println("Description: " + rec.getDescription());
});
```

## 📊 Sample Output

```
================================================================================
VALIDATION SUMMARY
================================================================================
Total Tests:      450
Passed:           423 (94.0%)
Failed:           27

Health Score:     91.5%

Issues by Severity:
  🔴 Critical:    2
  🟠 High:        8
  🟡 Medium:      12
  🟢 Low:         5
  ℹ️  Info:        0

Top Recommendations:
  • Fix STATUS_CODE_MISMATCH (Priority: 1)
  • Fix SCHEMA_MISMATCH (Priority: 2)
  • Fix MISSING_REQUIRED_FIELD (Priority: 2)

JSON report saved: reports/validation_report_20260508_091234.json
Markdown report saved: reports/validation_report_20260508_091234.md
================================================================================
```

## 🔧 Configuration

### AI Configuration

```yaml
docvalidator:
  ai:
    api-key: ${OPENAI_API_KEY}
    model: gpt-4                    # or gpt-3.5-turbo
    temperature: 0.3
    max-tokens: 2000
```

### Test Generation

```yaml
docvalidator:
  test-generation:
    max-tests-per-endpoint: 10
    include-positive-tests: true
    include-negative-tests: true
    include-edge-cases: true
```

### Validation Settings

```yaml
docvalidator:
  validation:
    enable-semantic-analysis: true
    enable-schema-validation: true
    enable-performance-checks: true
    max-response-time-ms: 5000
```

## 📁 Project Structure

```
docvalidator/
├── src/main/java/com/docvalidator/
│   ├── agent/                    # AI agents
│   │   ├── TestGeneratorAgent.java
│   │   ├── ValidatorAgent.java
│   │   └── ReporterAgent.java
│   ├── auth/                     # Authentication
│   │   └── SpotifyAuthManager.java
│   ├── cli/                      # Command-line interface
│   │   └── ValidationRunner.java
│   ├── config/                   # Configuration
│   │   └── DocValidatorConfig.java
│   ├── controller/               # REST controllers
│   │   └── ValidationController.java
│   ├── engine/                   # Test execution
│   │   └── TestExecutionEngine.java
│   ├── model/                    # Domain models
│   │   ├── ApiEndpoint.java
│   │   ├── TestCase.java
│   │   ├── ValidationResult.java
│   │   ├── Discrepancy.java
│   │   └── ValidationReport.java
│   ├── parser/                   # OpenAPI parsing
│   │   └── OpenApiParser.java
│   └── service/                  # Business logic
│       └── ValidationOrchestrator.java
├── src/main/resources/
│   └── application.yml
├── docs/                         # Documentation
├── pom.xml
└── README.md
```

## 🎯 Use Cases

### 1. Continuous Integration
Integrate DocValidator into your CI/CD pipeline to catch documentation drift:

```yaml
# .github/workflows/validate-docs.yml
name: Validate API Documentation
on: [push, pull_request]
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run DocValidator
        run: mvn spring-boot:run -Dspring-boot.run.arguments="--run-validation=true"
```

### 2. Pre-Release Validation
Validate documentation before releasing new API versions:

```bash
./validate-docs.sh --openapi-url=./openapi-v2.yml --fail-on-critical
```

### 3. Documentation Quality Monitoring
Track documentation quality over time with scheduled validations.

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) for details.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- OpenAI for GPT-4 API
- Spotify for comprehensive API documentation
- Spring Boot team for the excellent framework
- RestAssured for API testing capabilities

## 📞 Support

- 📧 Email: support@docvalidator.dev
- 💬 Discord: [Join our community](https://discord.gg/docvalidator)
- 🐛 Issues: [GitHub Issues](https://github.com/yourusername/docvalidator/issues)

## 🗺️ Roadmap

- [ ] Web UI Dashboard
- [ ] Support for GraphQL APIs
- [ ] Integration with popular API gateways
- [ ] Model Context Protocol (MCP) integration
- [ ] Multi-language support for reports
- [ ] Custom validation rules engine
- [ ] Performance benchmarking
- [ ] API versioning comparison

---

**Built with ❤️ for the API documentation community**