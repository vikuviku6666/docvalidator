# Getting Started with DocValidator

This guide will help you get DocValidator up and running in 5 minutes.

## Prerequisites

Before you begin, ensure you have:

- ✅ Java 21 or higher installed
- ✅ Maven 3.8+ installed
- ✅ OpenAI API key ([Get one here](https://platform.openai.com/api-keys))
- ✅ Spotify API credentials ([Register here](https://developer.spotify.com/dashboard))

## Step 1: Clone and Setup

```bash
# Clone the repository
git clone https://github.com/yourusername/docvalidator.git
cd docvalidator

# Verify Java version
java -version  # Should be 21+

# Verify Maven
mvn -version   # Should be 3.8+
```

## Step 2: Configure Credentials

Create a `.env` file in the project root:

```bash
# .env
OPENAI_API_KEY=sk-your-openai-api-key-here
SPOTIFY_CLIENT_ID=your-spotify-client-id
SPOTIFY_CLIENT_SECRET=your-spotify-client-secret
```

Or export as environment variables:

```bash
export OPENAI_API_KEY="sk-your-openai-api-key-here"
export SPOTIFY_CLIENT_ID="your-spotify-client-id"
export SPOTIFY_CLIENT_SECRET="your-spotify-client-secret"
```

## Step 3: Build the Project

```bash
mvn clean install
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: 45.123 s
```

## Step 4: Run Your First Validation

### Option A: Command Line (Recommended for first run)

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--run-validation=true"
```

This will:
1. Parse the Spotify OpenAPI specification
2. Generate test cases for all endpoints
3. Execute tests against the live Spotify API
4. Generate a comprehensive validation report

### Option B: REST API

Start the server:
```bash
mvn spring-boot:run
```

In another terminal, trigger validation:
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

## Step 5: View Results

After validation completes, check the `reports/` directory:

```bash
ls -la reports/
```

You'll find:
- `validation_report_YYYYMMDD_HHMMSS.json` - Detailed JSON report
- `validation_report_YYYYMMDD_HHMMSS.md` - Human-readable Markdown report

### Sample Report Structure

```markdown
# API Documentation Validation Report

**Generated:** 2026-05-08T09:12:34

## Summary
- **Total Tests:** 450
- **Passed:** 423 (94.0%)
- **Failed:** 27
- **Health Score:** 91.5%

## Issues by Severity
- 🔴 **Critical:** 2
- 🟠 **High:** 8
- 🟡 **Medium:** 12
- 🟢 **Low:** 5
- ℹ️ **Info:** 0

## Recommendations
### Fix STATUS_CODE_MISMATCH
**Priority:** 1 | **Effort:** Low
...
```

## Understanding the Output

### Health Score
- **90-100%**: Excellent - Documentation is highly accurate
- **75-89%**: Good - Minor discrepancies exist
- **60-74%**: Fair - Several issues need attention
- **Below 60%**: Poor - Significant documentation problems

### Discrepancy Types

| Type | Description | Typical Cause |
|------|-------------|---------------|
| STATUS_CODE_MISMATCH | API returns different status code | Outdated documentation |
| SCHEMA_MISMATCH | Response structure differs | API changes not documented |
| MISSING_FIELD | Expected field not in response | Breaking change |
| EXTRA_FIELD | Unexpected field in response | New feature not documented |
| TYPE_MISMATCH | Field has wrong data type | Schema error |

## Common Issues and Solutions

### Issue 1: "Authentication failed"

**Solution:** Verify your Spotify credentials:
```bash
# Test credentials
curl -X POST https://accounts.spotify.com/api/token \
  -H "Authorization: Basic $(echo -n $SPOTIFY_CLIENT_ID:$SPOTIFY_CLIENT_SECRET | base64)" \
  -d "grant_type=client_credentials"
```

### Issue 2: "OpenAI API rate limit exceeded"

**Solution:** Reduce test generation in `application.yml`:
```yaml
docvalidator:
  test-generation:
    max-tests-per-endpoint: 5  # Reduce from 10
```

### Issue 3: "Connection timeout"

**Solution:** Increase timeout in `application.yml`:
```yaml
docvalidator:
  target-api:
    timeout: 10000  # Increase to 10 seconds
```

## Next Steps

### 1. Validate Specific Endpoints

Instead of validating all 150+ Spotify endpoints, focus on specific ones:

```bash
curl -X POST http://localhost:8080/api/v1/validation/run \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://developer.spotify.com/reference/web-api/open-api-schema.yml",
    "endpointPaths": [
      "/albums/{id}",
      "/artists/{id}"
    ]
  }'
```

### 2. Customize Configuration

Edit `src/main/resources/application.yml` to:
- Adjust AI model (gpt-4 vs gpt-3.5-turbo)
- Configure test generation settings
- Enable/disable specific validation types
- Set custom thresholds

### 3. Integrate with CI/CD

Add to your GitHub Actions workflow:

```yaml
# .github/workflows/validate-docs.yml
name: Validate API Documentation
on: [push, pull_request]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up Java
        uses: actions/setup-java@v2
        with:
          java-version: '21'
          
      - name: Run DocValidator
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
          SPOTIFY_CLIENT_ID: ${{ secrets.SPOTIFY_CLIENT_ID }}
          SPOTIFY_CLIENT_SECRET: ${{ secrets.SPOTIFY_CLIENT_SECRET }}
        run: |
          mvn spring-boot:run -Dspring-boot.run.arguments="--run-validation=true"
          
      - name: Upload Reports
        uses: actions/upload-artifact@v2
        with:
          name: validation-reports
          path: reports/
```

### 4. Explore Advanced Features

- **Semantic Analysis**: AI-powered logical consistency checks
- **Performance Validation**: Response time monitoring
- **Custom Rules**: Define your own validation rules
- **Batch Processing**: Validate multiple APIs in sequence

## Troubleshooting

### Enable Debug Logging

Add to `application.yml`:
```yaml
logging:
  level:
    com.docvalidator: DEBUG
```

### Check System Requirements

```bash
# Verify Java
java -version

# Verify Maven
mvn -version

# Check available memory
free -h

# Test network connectivity
curl -I https://api.spotify.com/v1/albums/4aawyAB9vmqN3uQ7FjRGTy
```

### Get Help

- 📖 [Full Documentation](docs/)
- 💬 [Discord Community](https://discord.gg/docvalidator)
- 🐛 [Report Issues](https://github.com/yourusername/docvalidator/issues)
- 📧 Email: support@docvalidator.dev

## What's Next?

Now that you have DocValidator running, explore:

1. **[Architecture Guide](docs/ARCHITECTURE.md)** - Understand how it works
2. **[Configuration Reference](docs/CONFIGURATION.md)** - All configuration options
3. **[API Reference](docs/API_REFERENCE.md)** - REST API documentation
4. **[Examples](docs/EXAMPLES.md)** - Real-world use cases

---

**Happy Validating! 🚀**