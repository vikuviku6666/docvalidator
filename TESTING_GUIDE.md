# Testing DocValidator with Real Spotify API

This guide shows you how to test DocValidator with the real Spotify Web API.

## 📋 Prerequisites

1. **Java 21** installed
2. **Maven** installed
3. **Spotify Developer Account** (free)
4. **OpenRouter API Key** (for AI features)

## 🔧 Step 1: Get Spotify API Credentials

### 1.1 Create Spotify Developer Account
1. Go to https://developer.spotify.com/dashboard
2. Log in with your Spotify account (or create one)
3. Accept the Terms of Service

### 1.2 Create an App
1. Click **"Create app"**
2. Fill in the details:
   - **App name:** DocValidator Test
   - **App description:** Testing API documentation validation
   - **Redirect URI:** `http://127.0.0.1:8080/callback`
   - **Which API/SDKs are you planning to use?** Select "Web API"
3. Click **"Save"**

### 1.3 Get Your Credentials
1. Click on your newly created app
2. Click **"Settings"**
3. You'll see:
   - **Client ID** (copy this)
   - **Client Secret** (click "View client secret" and copy)

## 🔑 Step 2: Get OpenRouter API Key

### 2.1 Sign Up for OpenRouter
1. Go to https://openrouter.ai/
2. Sign up for an account
3. Go to **Keys** section
4. Create a new API key
5. Copy the key (starts with `sk-or-...`)

## ⚙️ Step 3: Configure Environment Variables

### On macOS/Linux:
```bash
# Add to ~/.bashrc or ~/.zshrc
export OPENROUTER_API_KEY="sk-or-v1-your-key-here"
export SPOTIFY_CLIENT_ID="your-spotify-client-id"
export SPOTIFY_CLIENT_SECRET="your-spotify-client-secret"

# Reload shell
source ~/.bashrc  # or source ~/.zshrc
```

### On Windows (PowerShell):
```powershell
$env:OPENROUTER_API_KEY="sk-or-v1-your-key-here"
$env:SPOTIFY_CLIENT_ID="your-spotify-client-id"
$env:SPOTIFY_CLIENT_SECRET="your-spotify-client-secret"
```

### Verify Environment Variables:
```bash
echo $OPENROUTER_API_KEY
echo $SPOTIFY_CLIENT_ID
echo $SPOTIFY_CLIENT_SECRET
```

## 🚀 Step 4: Start the Application

```bash
cd /Users/viku/Dev_Projects/Java_Projects/tut_java/tut03
mvn spring-boot:run
```

Wait for the application to start. You should see:
```
Started DocValidatorApplication in X.XXX seconds
```

## 🧪 Step 5: Test with Real Data

### Option A: Using curl (Command Line)

#### 5.1 Test Health Check (No Auth)
```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "application": "DocValidator",
  "timestamp": 1234567890
}
```

#### 5.2 Start Validation (With Auth)
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
    "endpointPaths": [
      "/v1/albums/{id}",
      "/v1/artists/{id}",
      "/v1/tracks/{id}"
    ]
  }'
```

Expected response:
```json
{
  "status": "STARTED",
  "message": "Validation started successfully"
}
```

#### 5.3 Check Progress
```bash
curl -u admin:admin123 http://localhost:8080/api/v1/validation/progress
```

#### 5.4 Run Validation Synchronously (Get Full Report)
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/validation/run \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
    "endpointPaths": ["/v1/albums/{id}"]
  }' | jq '.'
```

Note: `| jq '.'` formats the JSON output (install jq if needed: `brew install jq`)

### Option B: Using Postman

#### 5.1 Import Collection
1. Open Postman
2. Create a new collection: "DocValidator Tests"

#### 5.2 Add Requests

**Request 1: Health Check**
- Method: GET
- URL: `http://localhost:8080/api/health`
- Auth: None

**Request 2: Start Validation**
- Method: POST
- URL: `http://localhost:8080/api/v1/validation/start`
- Auth: Basic Auth (username: `admin`, password: `admin123`)
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
  "endpointPaths": [
    "/v1/albums/{id}",
    "/v1/artists/{id}"
  ]
}
```

**Request 3: Get Progress**
- Method: GET
- URL: `http://localhost:8080/api/v1/validation/progress`
- Auth: Basic Auth (username: `admin`, password: `admin123`)

### Option C: Using CLI Runner

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--run-validation=true"
```

This will:
1. Load the Spotify OpenAPI spec
2. Generate tests for all endpoints
3. Execute tests against live Spotify API
4. Generate a validation report
5. Save report to `reports/` directory

## 📊 Step 6: View Results

### Check Console Output
The application will log:
- Parsing OpenAPI specification
- Generating test cases
- Executing tests
- Validation results
- Discrepancies found

### Check Report Files
Reports are saved in the `reports/` directory:
```bash
ls -la reports/
```

### Check Logs
Detailed logs are in `logs/docvalidator.log`:
```bash
tail -f logs/docvalidator.log
```

## 🎯 Example Test Scenarios

### Scenario 1: Test Single Endpoint
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/validation/run \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
    "endpointPaths": ["/v1/albums/{id}"]
  }'
```

### Scenario 2: Test Multiple Endpoints
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/validation/run \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
    "endpointPaths": [
      "/v1/albums/{id}",
      "/v1/artists/{id}",
      "/v1/tracks/{id}",
      "/v1/playlists/{playlist_id}"
    ]
  }'
```

### Scenario 3: Test All Endpoints (Leave endpointPaths empty)
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
    "endpointPaths": []
  }'
```

## 🔍 What to Expect

### Successful Validation
```json
{
  "id": "uuid-here",
  "generatedAt": "2024-01-01T12:00:00",
  "summary": {
    "totalTests": 15,
    "passedTests": 13,
    "failedTests": 2,
    "totalDiscrepancies": 3,
    "criticalIssues": 0,
    "highIssues": 1,
    "mediumIssues": 2,
    "lowIssues": 0,
    "infoIssues": 0
  },
  "healthScore": 86.67,
  "validationResults": [...],
  "recommendations": [...]
}
```

### Common Discrepancies Found
1. **Schema Mismatches** - Response fields not matching documentation
2. **Missing Fields** - Fields in response but not documented
3. **Type Mismatches** - Field types different from documentation
4. **Status Code Mismatches** - Different status codes than documented

## 🐛 Troubleshooting

### Issue: "Authentication failed"
**Cause:** Invalid Spotify credentials
**Solution:** 
1. Check your Client ID and Client Secret
2. Verify environment variables are set
3. Restart the application

### Issue: "OpenAI API error"
**Cause:** Invalid OpenRouter API key
**Solution:**
1. Check your OpenRouter API key
2. Verify it starts with `sk-or-`
3. Check you have credits in your OpenRouter account

### Issue: "Connection timeout"
**Cause:** Network issues or Spotify API is down
**Solution:**
1. Check your internet connection
2. Verify Spotify API status: https://developer.spotify.com/status
3. Increase timeout in `application.yml`

### Issue: "Rate limit exceeded"
**Cause:** Too many requests to Spotify API
**Solution:**
1. Wait a few minutes
2. Reduce the number of endpoints being tested
3. Adjust rate limiting in configuration

## 📈 Performance Tips

### For Faster Testing
1. Test fewer endpoints at a time
2. Use async validation (`/start`) instead of sync (`/run`)
3. Increase parallel execution threads in config

### For More Accurate Results
1. Run tests multiple times
2. Test during different times of day
3. Compare results over time

## 🎓 Learning from Results

### Analyze Discrepancies
1. Review the `discrepanciesByType` section
2. Check `discrepanciesBySeverity` for priority
3. Read AI-generated `recommendations`

### Improve Documentation
1. Fix critical and high severity issues first
2. Update OpenAPI specification based on findings
3. Re-run validation to verify fixes

## 📚 Next Steps

1. **Explore Reports** - Check the generated reports in `reports/`
2. **Customize Tests** - Modify test generation settings in `application.yml`
3. **Add More APIs** - Test other APIs by changing the OpenAPI URL
4. **Automate** - Set up CI/CD to run validations automatically

## 🔗 Related Documentation

- [API_ENDPOINTS.md](API_ENDPOINTS.md) - Complete API reference
- [LOGIN_GUIDE.md](LOGIN_GUIDE.md) - Authentication guide
- [GETTING_STARTED.md](GETTING_STARTED.md) - Getting started guide
- [docs/SPOTIFY_SETUP.md](docs/SPOTIFY_SETUP.md) - Detailed Spotify setup